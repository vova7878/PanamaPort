package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Stack.getStackClass1;
import static com.v7878.unsafe.Utils.newIllegalArgumentException;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrame;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.invoke.Transformers.makeTransformer;

import android.util.ArrayMap;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.Transformers.TransformerI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Map;
import java.util.Objects;

import dalvik.system.DexFile;

public class MethodHandlesFixes {

    private static final long RTYPE_OFFSET = fieldOffset(
            getDeclaredField(MethodType.class, "rtype"));
    private static final long PTYPES_OFFSET = fieldOffset(
            getDeclaredField(MethodType.class, "ptypes"));

    private static Class<?> rtype(MethodType type) {
        return (Class<?>) getObject(Objects.requireNonNull(type), RTYPE_OFFSET);
    }

    private static Class<?>[] ptypes(MethodType type) {
        return (Class<?>[]) getObject(Objects.requireNonNull(type), PTYPES_OFFSET);
    }

    private static class CollectArguments implements TransformerI {
        private final MethodHandle target;
        private final MethodHandle collector;
        private final int pos;
        private final int collector_count;

        CollectArguments(MethodHandle target, MethodHandle collector, int pos) {
            this.target = target;
            this.collector = collector;
            this.pos = pos;
            this.collector_count = collector.type().parameterCount();
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor this_accessor = stack.createAccessor();

            // First invoke the collector.
            EmulatedStackFrame collectorFrame = EmulatedStackFrame.create(collector.type());
            StackFrameAccessor collector_accessor = collectorFrame.createAccessor();
            EmulatedStackFrame.copyArguments(this_accessor, pos,
                    collector_accessor, 0, collector_count);
            invokeExactWithFrameNoChecks(collector, collectorFrame);

            // Start constructing the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            StackFrameAccessor target_accessor = targetFrame.createAccessor();
            EmulatedStackFrame.copyArguments(this_accessor, 0,
                    target_accessor, 0, pos);

            // If return type of collector is not void, we have a return value to copy.
            target_accessor.moveTo(pos);
            if (rtype(collector.type()) != void.class) {
                EmulatedStackFrame.copyNext(collector_accessor.moveToReturn(),
                        target_accessor, rtype(collector.type()));
            }

            // Finish constructing the target frame.
            int this_pos = pos + collector_count;
            EmulatedStackFrame.copyArguments(this_accessor, this_pos,
                    target_accessor, target_accessor.argumentIdx,
                    stack.type().parameterCount() - this_pos);

            // Invoke the target.
            invokeExactWithFrameNoChecks(target, targetFrame);
            targetFrame.copyReturnValueTo(stack);
        }
    }

    // fix for PLATFORM-BUG!
    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        MethodType newType = collectArgumentsChecks(target, pos, filter);
        return makeTransformer(newType, new CollectArguments(target, filter, pos));
    }

    private static MethodType collectArgumentsChecks(MethodHandle target, int pos, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = rtype(filterType);
        Class<?>[] filterArgs = ptypes(filterType);
        if (rtype == void.class) {
            return targetType.insertParameterTypes(pos, filterArgs);
        }
        if (rtype != targetType.parameterType(pos)) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        return targetType.dropParameterTypes(pos, pos + 1).insertParameterTypes(pos, filterArgs);
    }

    // fix for PLATFORM-BUG!
    public static MethodHandle identity(Class<?> type) {
        Objects.requireNonNull(type);
        return makeTransformer(MethodType.methodType(type, type), (TransformerI) stack ->
                EmulatedStackFrame.copyNext(stack.createAccessor().moveTo(0),
                        stack.createAccessor().moveToReturn(), type));
    }

    private static void addClass(Map<String, Class<?>> map, Class<?> clazz) {
        Class<?> component = clazz.getComponentType();
        while (component != null) {
            clazz = component;
            component = clazz.getComponentType();
        }

        if (clazz.getClassLoader() != null && clazz.getClassLoader() != Object.class.getClassLoader()) {
            map.put(clazz.getName(), clazz);
        }
    }

    private static ClassLoader getInvokerClassLoader(MethodType type) {
        ArrayMap<String, Class<?>> map = new ArrayMap<>(type.parameterCount() + 1);
        for (int i = 0; i < type.parameterCount(); i++) {
            addClass(map, type.parameterType(i));
        }
        addClass(map, rtype(type));

        if (map.size() == 0) {
            return Utils.newEmptyClassLoader();
        }

        // new every time, needed for GC
        return new ClassLoader(getStackClass1().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> out = map.get(name);
                if (out != null) {
                    return out;
                }
                return super.loadClass(name, resolve);
            }

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return loadClass(name, false);
            }
        };
    }

    private static String getInvokerName(ProtoId proto) {
        return MethodHandlesFixes.class.getName() + "$$$Invoker_" + proto.getShorty();
    }

    private static MethodHandle newInvoker(MethodType type, boolean exact) {
        ProtoId proto = ProtoId.of(type);
        MethodType itype = type.insertParameterTypes(0, MethodHandle.class);

        String invoker_name = getInvokerName(proto);
        TypeId invoker_id = TypeId.of(invoker_name);
        ClassDef invoker_def = new ClassDef(invoker_id);
        invoker_def.setSuperClass(TypeId.of(Object.class));

        MethodId invoke = new MethodId(TypeId.of(MethodHandle.class),
                new ProtoId(TypeId.of(Object.class), TypeId.of(Object[].class)),
                exact ? "invokeExact" : "invoke");

        MethodId iid = new MethodId(invoker_id, ProtoId.of(itype), "invoke");
        invoker_def.getClassData().getDirectMethods().add(new EncodedMethod(
                iid, ACC_STATIC).withCode(2 /* locals for wide result */, b -> {
                    b.invoke_polymorphic_range(invoke, proto,
                            proto.getInputRegistersCount() + 1, b.p(0));
                    switch (proto.getReturnType().getShorty()) {
                        case 'V' -> b.return_void();
                        case 'L' -> b.move_result_object(b.l(0))
                                .return_object(b.l(0));
                        case 'J', 'D' -> b.move_result_wide(b.l(0))
                                .return_wide(b.l(0));
                        default -> b.move_result(b.l(0))
                                .return_(b.l(0));
                    }
                }
        ));

        DexFile dex = openDexFile(new Dex(invoker_def).compile());
        Class<?> invoker = loadClass(dex, invoker_name, getInvokerClassLoader(type));

        return unreflect(getDeclaredMethod(invoker, "invoke", itype.parameterArray()));
    }

    private static final Utils.SoftReferenceCache<MethodType, MethodHandle>
            invokers_cache = new Utils.SoftReferenceCache<>();

    public static MethodHandle invoker(MethodType type) {
        Objects.requireNonNull(type);
        return invokers_cache.get(type, t -> newInvoker(t, false));
    }

    private static final Utils.SoftReferenceCache<MethodType, MethodHandle>
            exact_invokers_cache = new Utils.SoftReferenceCache<>();

    public static MethodHandle exactInvoker(MethodType type) {
        Objects.requireNonNull(type);
        return exact_invokers_cache.get(type, t -> newInvoker(t, true));
    }

    private static class JustInvoke implements TransformerI {
        private final MethodHandle target;

        JustInvoke(MethodHandle target) {
            this.target = target;
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            // Invoke the target with checks
            invokeExactWithFrame(target, stack);
        }
    }

    public static MethodHandle protectHandle(MethodHandle target) {
        return makeTransformer(target.type(), new JustInvoke(target));
    }

    static class ExplicitCastArguments implements TransformerI {
        private final MethodHandle target;
        private final MethodType type;

        ExplicitCastArguments(MethodHandle target, MethodType type) {
            this.target = target;
            this.type = type;
        }

        @Override
        public void transform(EmulatedStackFrame callerFrame) throws Throwable {
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            StackFrameAccessor callerAccessor = callerFrame.createAccessor();
            StackFrameAccessor targetAccessor = targetFrame.createAccessor();

            explicitCastArguments(callerAccessor, targetAccessor);

            invokeExactWithFrameNoChecks(target, targetFrame);

            explicitCastReturnValue(targetAccessor.moveToReturn(), callerAccessor.moveToReturn());
        }

        private void explicitCastArguments(StackFrameAccessor reader, StackFrameAccessor writer) {
            Class<?>[] fromTypes = ptypes(type);
            Class<?>[] toTypes = ptypes(target.type());
            for (int i = 0; i < fromTypes.length; ++i) {
                explicitCast(reader, fromTypes[i], writer, toTypes[i]);
            }
        }

        private void explicitCastReturnValue(StackFrameAccessor reader, StackFrameAccessor writer) {
            Class<?> from = rtype(target.type());
            Class<?> to = rtype(type);
            if (to != void.class) {
                if (from == void.class) {
                    if (to.isPrimitive()) {
                        unboxNull(writer, to);
                    } else {
                        writer.putNextReference(null, to);
                    }
                } else {
                    explicitCast(reader, from, writer, to);
                }
            }
        }

        private static RuntimeException unexpectedType(Class<?> unexpectedType) {
            throw new InternalError("Unexpected type: " + unexpectedType);
        }

        private static RuntimeException badCast(Class<?> from, Class<?> to) {
            throw new ClassCastException("Cannot cast " + from.getName() + " to " + to.getName());
        }

        private static boolean toBoolean(byte value) {
            return (value & 1) == 1;
        }

        private static byte readPrimitiveAsByte(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> reader.nextByte();
                case 'C' -> (byte) reader.nextChar();
                case 'S' -> (byte) reader.nextShort();
                case 'I' -> (byte) reader.nextInt();
                case 'J' -> (byte) reader.nextLong();
                case 'F' -> (byte) reader.nextFloat();
                case 'D' -> (byte) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? (byte) 1 : (byte) 0;
                default -> throw unexpectedType(from);
            };
        }

        private static char readPrimitiveAsChar(StackFrameAccessor reader, Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (char) reader.nextByte();
                case 'C' -> reader.nextChar();
                case 'S' -> (char) reader.nextShort();
                case 'I' -> (char) reader.nextInt();
                case 'J' -> (char) reader.nextLong();
                case 'F' -> (char) reader.nextFloat();
                case 'D' -> (char) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? (char) 1 : (char) 0;
                default -> throw unexpectedType(from);
            };
        }

        private static short readPrimitiveAsShort(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (short) reader.nextByte();
                case 'C' -> (short) reader.nextChar();
                case 'S' -> reader.nextShort();
                case 'I' -> (short) reader.nextInt();
                case 'J' -> (short) reader.nextLong();
                case 'F' -> (short) reader.nextFloat();
                case 'D' -> (short) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? (short) 1 : (short) 0;
                default -> throw unexpectedType(from);
            };
        }

        private static int readPrimitiveAsInt(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (int) reader.nextByte();
                case 'C' -> (int) reader.nextChar();
                case 'S' -> (int) reader.nextShort();
                case 'I' -> reader.nextInt();
                case 'J' -> (int) reader.nextLong();
                case 'F' -> (int) reader.nextFloat();
                case 'D' -> (int) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? 1 : 0;
                default -> throw unexpectedType(from);
            };
        }

        private static long readPrimitiveAsLong(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (long) reader.nextByte();
                case 'C' -> (long) reader.nextChar();
                case 'S' -> (long) reader.nextShort();
                case 'I' -> (long) reader.nextInt();
                case 'J' -> reader.nextLong();
                case 'F' -> (long) reader.nextFloat();
                case 'D' -> (long) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? 1L : 0L;
                default -> throw unexpectedType(from);
            };
        }

        private static float readPrimitiveAsFloat(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (float) reader.nextByte();
                case 'C' -> (float) reader.nextChar();
                case 'S' -> (float) reader.nextShort();
                case 'I' -> (float) reader.nextInt();
                case 'J' -> (float) reader.nextLong();
                case 'F' -> reader.nextFloat();
                case 'D' -> (float) reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? 1.0f : 0.0f;
                default -> throw unexpectedType(from);
            };
        }

        private static double readPrimitiveAsDouble(StackFrameAccessor reader, final Class<?> from) {
            return switch (TypeId.of(from).getShorty()) {
                case 'B' -> (double) reader.nextByte();
                case 'C' -> (double) reader.nextChar();
                case 'S' -> (double) reader.nextShort();
                case 'I' -> (double) reader.nextInt();
                case 'J' -> (double) reader.nextLong();
                case 'F' -> (double) reader.nextFloat();
                case 'D' -> reader.nextDouble();
                case 'Z' -> reader.nextBoolean() ? 1.0 : 0.0;
                default -> throw unexpectedType(from);
            };
        }

        private static void explicitCastPrimitives(
                StackFrameAccessor reader, Class<?> from,
                StackFrameAccessor writer, Class<?> to) {
            switch (TypeId.of(to).getShorty()) {
                case 'B' -> writer.putNextByte(readPrimitiveAsByte(reader, from));
                case 'C' -> writer.putNextChar(readPrimitiveAsChar(reader, from));
                case 'S' -> writer.putNextShort(readPrimitiveAsShort(reader, from));
                case 'I' -> writer.putNextInt(readPrimitiveAsInt(reader, from));
                case 'J' -> writer.putNextLong(readPrimitiveAsLong(reader, from));
                case 'F' -> writer.putNextFloat(readPrimitiveAsFloat(reader, from));
                case 'D' -> writer.putNextDouble(readPrimitiveAsDouble(reader, from));
                case 'Z' -> writer.putNextBoolean(toBoolean(readPrimitiveAsByte(reader, from)));
                default -> throw unexpectedType(to);
            }
        }

        private static void unboxNull(StackFrameAccessor writer, final Class<?> to) {
            switch (TypeId.of(to).getShorty()) {
                case 'Z' -> writer.putNextBoolean(false);
                case 'B' -> writer.putNextByte((byte) 0);
                case 'C' -> writer.putNextChar((char) 0);
                case 'S' -> writer.putNextShort((short) 0);
                case 'I' -> writer.putNextInt(0);
                case 'J' -> writer.putNextLong(0);
                case 'F' -> writer.putNextFloat((float) 0);
                case 'D' -> writer.putNextDouble(0);
                default -> throw unexpectedType(to);
            }
        }

        private static char asPrimitiveChar(Class<?> boxed) {
            if (boxed == Boolean.class) {
                return 'Z';
            } else if (boxed == Byte.class) {
                return 'B';
            } else if (boxed == Short.class) {
                return 'S';
            } else if (boxed == Character.class) {
                return 'C';
            } else if (boxed == Integer.class) {
                return 'I';
            } else if (boxed == Float.class) {
                return 'F';
            } else if (boxed == Long.class) {
                return 'J';
            } else if (boxed == Double.class) {
                return 'D';
            }
            throw unexpectedType(boxed);
        }

        private static void unboxNonNull(
                Object ref, StackFrameAccessor writer, Class<?> to) {
            Class<?> from = ref.getClass();
            char unboxed_char = asPrimitiveChar(from);
            char to_char = TypeId.of(to).getShorty();
            switch (unboxed_char) {
                case 'Z' -> {
                    boolean z = (boolean) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean(z);
                        case 'B' -> writer.putNextByte(z ? (byte) 1 : (byte) 0);
                        case 'S' -> writer.putNextShort(z ? (short) 1 : (short) 0);
                        case 'C' -> writer.putNextChar(z ? (char) 1 : (char) 0);
                        case 'I' -> writer.putNextInt(z ? 1 : 0);
                        case 'J' -> writer.putNextLong(z ? 1L : 0L);
                        case 'F' -> writer.putNextFloat(z ? 1.0f : 0.0f);
                        case 'D' -> writer.putNextDouble(z ? 1.0 : 0.0);
                        default -> throw badCast(from, to);
                    }
                }
                case 'B' -> {
                    byte b = (byte) ref;
                    switch (to_char) {
                        case 'B' -> writer.putNextByte(b);
                        case 'Z' -> writer.putNextBoolean(toBoolean(b));
                        case 'S' -> writer.putNextShort(b);
                        case 'C' -> writer.putNextChar((char) b);
                        case 'I' -> writer.putNextInt(b);
                        case 'J' -> writer.putNextLong(b);
                        case 'F' -> writer.putNextFloat(b);
                        case 'D' -> writer.putNextDouble(b);
                        default -> throw badCast(from, to);
                    }
                }
                case 'S' -> {
                    short s = (short) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean((s & 1) == 1);
                        case 'B' -> writer.putNextByte((byte) s);
                        case 'S' -> writer.putNextShort(s);
                        case 'C' -> writer.putNextChar((char) s);
                        case 'I' -> writer.putNextInt(s);
                        case 'J' -> writer.putNextLong(s);
                        case 'F' -> writer.putNextFloat(s);
                        case 'D' -> writer.putNextDouble(s);
                        default -> throw badCast(from, to);
                    }
                }
                case 'C' -> {
                    char c = (char) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean((c & (char) 1) == (char) 1);
                        case 'B' -> writer.putNextByte((byte) c);
                        case 'S' -> writer.putNextShort((short) c);
                        case 'C' -> writer.putNextChar(c);
                        case 'I' -> writer.putNextInt(c);
                        case 'J' -> writer.putNextLong(c);
                        case 'F' -> writer.putNextFloat(c);
                        case 'D' -> writer.putNextDouble(c);
                        default -> throw badCast(from, to);
                    }
                }
                case 'I' -> {
                    int i = (int) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean((i & 1) == 1);
                        case 'B' -> writer.putNextByte((byte) i);
                        case 'S' -> writer.putNextShort((short) i);
                        case 'C' -> writer.putNextChar((char) i);
                        case 'I' -> writer.putNextInt(i);
                        case 'J' -> writer.putNextLong(i);
                        case 'F' -> writer.putNextFloat((float) i);
                        case 'D' -> writer.putNextDouble(i);
                        default -> throw badCast(from, to);
                    }
                }
                case 'J' -> {
                    long j = (long) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean((j & 1L) == 1L);
                        case 'B' -> writer.putNextByte((byte) j);
                        case 'S' -> writer.putNextShort((short) j);
                        case 'C' -> writer.putNextChar((char) j);
                        case 'I' -> writer.putNextInt((int) j);
                        case 'J' -> writer.putNextLong(j);
                        case 'F' -> writer.putNextFloat((float) j);
                        case 'D' -> writer.putNextDouble((double) j);
                        default -> throw badCast(from, to);
                    }
                }
                case 'F' -> {
                    float f = (float) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean(((byte) f & 1) != 0);
                        case 'B' -> writer.putNextByte((byte) f);
                        case 'S' -> writer.putNextShort((short) f);
                        case 'C' -> writer.putNextChar((char) f);
                        case 'I' -> writer.putNextInt((int) f);
                        case 'J' -> writer.putNextLong((long) f);
                        case 'F' -> writer.putNextFloat(f);
                        case 'D' -> writer.putNextDouble(f);
                        default -> throw badCast(from, to);
                    }
                }
                case 'D' -> {
                    double d = (double) ref;
                    switch (to_char) {
                        case 'Z' -> writer.putNextBoolean(((byte) d & 1) != 0);
                        case 'B' -> writer.putNextByte((byte) d);
                        case 'S' -> writer.putNextShort((short) d);
                        case 'C' -> writer.putNextChar((char) d);
                        case 'I' -> writer.putNextInt((int) d);
                        case 'J' -> writer.putNextLong((long) d);
                        case 'F' -> writer.putNextFloat((float) d);
                        case 'D' -> writer.putNextDouble(d);
                        default -> throw badCast(from, to);
                    }
                }
                default -> throw badCast(from, to);
            }
        }

        private static void unbox(
                Object ref, StackFrameAccessor writer, Class<?> to) {
            if (ref == null) {
                unboxNull(writer, to);
            } else {
                unboxNonNull(ref, writer, to);
            }
        }

        private static void box(
                StackFrameAccessor reader, Class<?> from,
                StackFrameAccessor writer, Class<?> to) {
            Object boxed;
            switch (TypeId.of(from).getShorty()) {
                case 'Z' -> boxed = reader.nextBoolean();
                case 'B' -> boxed = reader.nextByte();
                case 'C' -> boxed = reader.nextChar();
                case 'S' -> boxed = reader.nextShort();
                case 'I' -> boxed = reader.nextInt();
                case 'J' -> boxed = reader.nextLong();
                case 'F' -> boxed = reader.nextFloat();
                case 'D' -> boxed = reader.nextDouble();
                default -> throw unexpectedType(from);
            }
            writer.putNextReference(to.cast(boxed), to);
        }

        private static void explicitCast(
                StackFrameAccessor reader, Class<?> from,
                StackFrameAccessor writer, Class<?> to) {
            if (from.equals(to)) {
                EmulatedStackFrame.copyNext(reader, writer, from);
                return;
            }
            if (from.isPrimitive()) {
                if (to.isPrimitive()) {
                    // |from| and |to| are primitive types.
                    explicitCastPrimitives(reader, from, writer, to);
                } else {
                    // |from| is a primitive type, |to| is a reference type.
                    box(reader, from, writer, to);
                }
            } else {
                // |from| is a reference type.
                Object ref = reader.nextReference(from);
                if (to.isPrimitive()) {
                    // |from| is a reference type, |to| is a primitive type,
                    unbox(ref, writer, to);
                } else if (to.isInterface()) {
                    // Pass from without a cast according to description for
                    // {@link java.lang.invoke.MethodHandles#explicitCastArguments()}.
                    writer.putNextReference(ref, to);
                } else {
                    // |to| and from |from| are reference types, perform class cast check.
                    writer.putNextReference(to.cast(ref), to);
                }
            }
        }
    }

    private static void explicitCastArgumentsChecks(MethodHandle target, MethodType newType) {
        if (target.type().parameterCount() != newType.parameterCount()) {
            throw new WrongMethodTypeException("cannot explicitly cast " + target +
                    " to " + newType);
        }
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed)
    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        target = target.asFixedArity();
        explicitCastArgumentsChecks(target, newType);
        // use the asTypeCache when possible:
        MethodType oldType = target.type();
        if (oldType.equals(newType)) return target;
        //TODO!: check fast path
        //if (oldType.explicitCastEquivalentToAsType(newType) &&
        //        !INVOKE_TRANSFORMER.isInstance(target)) {
        //    return target.asType(newType);
        //}
        return Transformers.makeTransformer(newType, new ExplicitCastArguments(target, newType));
    }
}

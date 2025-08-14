package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Utils.badCast;
import static com.v7878.unsafe.Utils.newIllegalArgumentException;
import static com.v7878.unsafe.Utils.newWrongMethodTypeException;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.Utils.unexpectedType;
import static com.v7878.unsafe.access.InvokeAccess.rtype;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;
import static com.v7878.unsafe.invoke.Transformers.invokeExactNoChecks;
import static com.v7878.unsafe.invoke.Transformers.makeTransformer;

import com.v7878.invoke.Handles;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.InvokeAccess;
import com.v7878.unsafe.invoke.EmulatedStackFrame.RelativeStackFrameAccessor;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.Transformers.AbstractTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;
import java.util.Objects;

//TODO: use codegen to improve performance
class MHUtils {
    private static class CollectArguments extends AbstractTransformer {
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
        public void transform(MethodHandle ignored, EmulatedStackFrame stackFrame) throws Throwable {
            MethodType collector_type = collector.type();
            StackFrameAccessor this_accessor = stackFrame.accessor();

            // First invoke the collector.
            EmulatedStackFrame collectorFrame = EmulatedStackFrame.create(collector_type);
            StackFrameAccessor collector_accessor = collectorFrame.accessor();
            EmulatedStackFrame.copyArguments(this_accessor, pos,
                    collector_accessor, 0, collector_count);
            invokeExactNoChecks(collector, collectorFrame);

            // Start constructing the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            StackFrameAccessor target_accessor = targetFrame.accessor();
            EmulatedStackFrame.copyArguments(this_accessor, 0,
                    target_accessor, 0, pos);

            // If return type of collector is not void, we have a return value to copy.
            int target_pos = pos;
            if (rtype(collector_type) != void.class) {
                EmulatedStackFrame.copyValue(collector_accessor,
                        RETURN_VALUE_IDX, target_accessor, target_pos);
                target_pos++;
            }

            // Finish constructing the target frame.
            int this_pos = pos + collector_count;
            EmulatedStackFrame.copyArguments(this_accessor, this_pos,
                    target_accessor, target_pos,
                    stackFrame.type().parameterCount() - this_pos);

            // Invoke the target.
            invokeExactNoChecks(target, targetFrame);
            EmulatedStackFrame.copyReturnValue(target_accessor, this_accessor);
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
        Class<?>[] filterArgs = InvokeAccess.ptypes(filterType);
        if (rtype == void.class) {
            return targetType.insertParameterTypes(pos, filterArgs);
        }
        if (rtype != targetType.parameterType(pos)) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        return targetType.dropParameterTypes(pos, pos + 1).insertParameterTypes(pos, filterArgs);
    }

    private static class Identity extends AbstractTransformer {
        Identity() {
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) {
            var accessor = stack.accessor();
            EmulatedStackFrame.copyValue(accessor, 0, accessor, RETURN_VALUE_IDX);
        }
    }

    // fix for PLATFORM-BUG!
    public static MethodHandle identity(Class<?> type) {
        Objects.requireNonNull(type);
        return makeTransformer(MethodType.methodType(type, type), new Identity());
    }

    private static class Empty extends AbstractTransformer {
        Empty() {
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) {
            // nop
        }
    }

    public static MethodHandle empty(MethodType type) {
        Objects.requireNonNull(type);
        return makeTransformer(type, new Empty());
    }

    public static MethodHandle zero(Class<?> type) {
        Objects.requireNonNull(type);
        return empty(MethodType.methodType(type));
    }

    private static class Invoker extends AbstractTransformer {
        private final MethodType targetType;
        private final boolean isExactInvoker;
        private final int args_count;

        Invoker(MethodType targetType, boolean isExactInvoker) {
            this.targetType = targetType;
            this.isExactInvoker = isExactInvoker;
            this.args_count = targetType.parameterCount();
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stackFrame) throws Throwable {
            // The first argument to the stack frame is the handle that needs to be invoked.
            StackFrameAccessor thisAccessor = stackFrame.accessor();
            MethodHandle target = thisAccessor.getReference(0);

            // All other arguments must be copied to the target frame.
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(targetType);
            StackFrameAccessor targetAccessor = targetFrame.accessor();
            EmulatedStackFrame.copyArguments(thisAccessor, 1, targetAccessor, 0, args_count);

            // Finally, invoke the handle and copy the return value.
            if (isExactInvoker) {
                Transformers.invokeExact(target, targetFrame);
            } else {
                Transformers.invoke(target, targetFrame);
            }
            EmulatedStackFrame.copyReturnValue(targetAccessor, thisAccessor);
        }
    }

    public static MethodHandle invoker(MethodType type) {
        Objects.requireNonNull(type);
        MethodType transformer_type = type.insertParameterTypes(0, MethodHandle.class);
        return Transformers.makeTransformer(transformer_type, new Invoker(type, false));
    }

    public static MethodHandle exactInvoker(MethodType type) {
        Objects.requireNonNull(type);
        MethodType transformer_type = type.insertParameterTypes(0, MethodHandle.class);
        return Transformers.makeTransformer(transformer_type, new Invoker(type, true));
    }

    private static class ExplicitCastArguments extends AbstractTransformer {
        private final MethodHandle target;
        private final MethodType type;

        ExplicitCastArguments(MethodHandle target, MethodType type) {
            this.target = target;
            this.type = type;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame callerFrame) throws Throwable {
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            var callerAccessor = callerFrame.relativeAccessor();
            var targetAccessor = targetFrame.relativeAccessor();

            explicitCastArguments(callerAccessor, targetAccessor);

            invokeExactNoChecks(target, targetFrame);

            explicitCastReturnValue(targetAccessor.moveToReturn(), callerAccessor.moveToReturn());
        }

        private void explicitCastArguments(RelativeStackFrameAccessor reader, RelativeStackFrameAccessor writer) {
            Class<?>[] fromTypes = InvokeAccess.ptypes(type);
            Class<?>[] toTypes = InvokeAccess.ptypes(target.type());
            for (int i = 0; i < fromTypes.length; ++i) {
                explicitCast(reader, fromTypes[i], writer, toTypes[i]);
            }
        }

        private void explicitCastReturnValue(RelativeStackFrameAccessor reader, RelativeStackFrameAccessor writer) {
            Class<?> from = rtype(target.type());
            Class<?> to = rtype(type);
            if (to != void.class) {
                if (from == void.class) {
                    if (to.isPrimitive()) {
                        unboxNull(writer, to);
                    } else {
                        writer.putNextReference(null);
                    }
                } else {
                    explicitCast(reader, from, writer, to);
                }
            }
        }

        private static boolean toBoolean(byte value) {
            return (value & 1) == 1;
        }

        private static byte readPrimitiveAsByte(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static char readPrimitiveAsChar(RelativeStackFrameAccessor reader, Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static short readPrimitiveAsShort(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static int readPrimitiveAsInt(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static long readPrimitiveAsLong(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static float readPrimitiveAsFloat(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static double readPrimitiveAsDouble(RelativeStackFrameAccessor reader, final Class<?> from) {
            return switch (Wrapper.basicTypeChar(from)) {
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

        private static void explicitCastPrimitives(RelativeStackFrameAccessor reader, Class<?> from,
                                                   RelativeStackFrameAccessor writer, Class<?> to) {
            switch (Wrapper.basicTypeChar(to)) {
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

        private static void unboxNull(RelativeStackFrameAccessor writer, final Class<?> to) {
            switch (Wrapper.basicTypeChar(to)) {
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

        private static void unboxNonNull(Object ref, RelativeStackFrameAccessor writer, Class<?> to) {
            Class<?> from = ref.getClass();
            char from_char = Wrapper.basicTypeChar(Wrapper.asPrimitiveType(from));
            char to_char = Wrapper.basicTypeChar(to);
            switch (from_char) {
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

        private static void unbox(Object ref, RelativeStackFrameAccessor writer, Class<?> to) {
            if (ref == null) {
                unboxNull(writer, to);
            } else {
                unboxNonNull(ref, writer, to);
            }
        }

        private static void box(RelativeStackFrameAccessor reader, Class<?> from,
                                RelativeStackFrameAccessor writer, Class<?> to) {
            Object boxed = switch (Wrapper.basicTypeChar(from)) {
                case 'Z' -> reader.nextBoolean();
                case 'B' -> reader.nextByte();
                case 'C' -> reader.nextChar();
                case 'S' -> reader.nextShort();
                case 'I' -> reader.nextInt();
                case 'J' -> reader.nextLong();
                case 'F' -> reader.nextFloat();
                case 'D' -> reader.nextDouble();
                default -> throw unexpectedType(from);
            };
            writer.putNextReference(to.cast(boxed));
        }

        private static void explicitCast(RelativeStackFrameAccessor reader, Class<?> from,
                                         RelativeStackFrameAccessor writer, Class<?> to) {
            if (from == to) {
                EmulatedStackFrame.copyNextValue(reader, writer);
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
                Object ref = reader.nextReference();
                if (to.isPrimitive()) {
                    // |from| is a reference type, |to| is a primitive type,
                    unbox(ref, writer, to);
                } else if (to.isInterface()) {
                    // Pass from without a cast according to description for
                    // {@link java.lang.invoke.MethodHandles#explicitCastArguments()}.
                    writer.putNextReference(ref);
                } else {
                    // |to| and from |from| are reference types, perform class cast check.
                    writer.putNextReference(to.cast(ref));
                }
            }
        }
    }

    private static void explicitCastArgumentsChecks(MethodType oldType, MethodType newType) {
        if (oldType.parameterCount() != newType.parameterCount()) {
            throw new WrongMethodTypeException(
                    "cannot explicitly cast " + oldType + " to " + newType);
        }
    }

    public static MethodHandle explicitCastArgumentsAdapter(MethodHandle target, MethodType newType) {
        explicitCastArgumentsChecks(target.type(), newType);
        return Transformers.makeTransformer(newType, new ExplicitCastArguments(target, newType));
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed)
    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        target = target.asFixedArity();
        MethodType oldType = target.type();
        if (oldType.equals(newType)) return target;
        explicitCastArgumentsChecks(oldType, newType);
        if ((ART_SDK_INT >= 33 || !Transformers.isTransformer(target)) &&
                InvokeAccess.explicitCastEquivalentToAsType(oldType, newType)) {
            return target.asType(newType);
        }
        return Transformers.makeTransformer(newType, new ExplicitCastArguments(target, newType));
    }

    private static class PermuteArguments extends AbstractTransformer {
        private final MethodHandle target;
        private final int[] reorder;

        PermuteArguments(MethodHandle target, int[] reorder) {
            this.target = target;
            this.reorder = reorder;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stackFrame) throws Throwable {
            MethodType type = target.type();
            StackFrameAccessor reader = stackFrame.accessor();
            EmulatedStackFrame calleeFrame = EmulatedStackFrame.create(type);
            StackFrameAccessor writer = calleeFrame.accessor();
            for (int i = 0; i < reorder.length; i++) {
                EmulatedStackFrame.copyValue(reader, reorder[i], writer, i);
            }
            invokeExactNoChecks(target, calleeFrame);
            // copy from writer to reader as this is return value
            EmulatedStackFrame.copyReturnValue(writer, reader);
        }
    }

    static void permuteArgumentChecks(int[] reorder, MethodType newType, MethodType oldType) {
        if (newType.returnType() != oldType.returnType())
            throw newIllegalArgumentException("return types do not match",
                    oldType, newType);
        if (reorder.length != oldType.parameterCount())
            throw newIllegalArgumentException("old type parameter count and reorder array length do not match",
                    oldType, Arrays.toString(reorder));

        int limit = newType.parameterCount();
        for (int j = 0; j < reorder.length; j++) {
            int i = reorder[j];
            if (i < 0 || i >= limit) {
                throw newIllegalArgumentException("index is out of bounds for new type",
                        i, newType);
            }
            Class<?> src = newType.parameterType(i);
            Class<?> dst = oldType.parameterType(j);
            if (src != dst)
                throw newIllegalArgumentException("parameter types do not match after reorder",
                        oldType, newType);
        }
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed x2)
    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        reorder = reorder.clone();  // get a private copy
        MethodType oldType = target.type();
        permuteArgumentChecks(reorder, newType, oldType);
        return Transformers.makeTransformer(newType, new PermuteArguments(target, reorder));
    }

    private static MethodType reorderArgumentChecks(int[] reorder, MethodType type) {
        int limit;
        if ((limit = reorder.length) != type.parameterCount())
            throw newIllegalArgumentException("type parameter count and reorder array length do not match",
                    type, Arrays.toString(reorder));

        Class<?> rtype = rtype(type);
        Class<?>[] atypes = new Class[limit];

        for (int j = 0; j < reorder.length; j++) {
            int i = reorder[j];
            if (i < 0 || i >= limit) {
                throw newIllegalArgumentException("index is out of bounds for type", i, type);
            }
            atypes[i] = type.parameterType(j);
        }

        return MethodType.methodType(rtype, atypes);
    }

    public static MethodHandle reorderArguments(MethodHandle target, int... reorder) {
        reorder = reorder.clone();  // get a private copy
        MethodType newType = reorderArgumentChecks(reorder, target.type());
        return Transformers.makeTransformer(newType, new PermuteArguments(target, reorder));
    }

    private static class AsTypeAdapter extends AbstractTransformer {
        private final MethodHandle target;
        private final MethodType type;

        AsTypeAdapter(MethodHandle target, MethodType type) {
            this.target = target;
            this.type = type;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame callerFrame) throws Throwable {
            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());

            var callerAccessor = callerFrame.relativeAccessor();
            var targetAccessor = targetFrame.relativeAccessor();

            adaptArguments(callerAccessor, targetAccessor);

            invokeExactNoChecks(target, targetFrame);

            adaptReturnValue(targetAccessor.moveToReturn(), callerAccessor.moveToReturn());
        }

        private void adaptArguments(RelativeStackFrameAccessor reader, RelativeStackFrameAccessor writer) {
            Class<?>[] fromTypes = InvokeAccess.ptypes(type);
            Class<?>[] toTypes = InvokeAccess.ptypes(target.type());
            for (int i = 0; i < fromTypes.length; ++i) {
                adaptArgument(reader, fromTypes[i], writer, toTypes[i]);
            }
        }

        private void adaptReturnValue(RelativeStackFrameAccessor reader, RelativeStackFrameAccessor writer) {
            Class<?> fromType = rtype(target.type());
            Class<?> toType = rtype(type);
            adaptArgument(reader, fromType, writer, toType);
        }

        private RuntimeException wrongType() {
            throw newWrongMethodTypeException(type, target.type());
        }

        private void writePrimitiveByteAs(RelativeStackFrameAccessor writer, char baseType, byte value) {
            switch (baseType) {
                case 'B' -> writer.putNextByte(value);
                case 'S' -> writer.putNextShort(value);
                case 'I' -> writer.putNextInt(value);
                case 'J' -> writer.putNextLong(value);
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveShortAs(RelativeStackFrameAccessor writer, char baseType, short value) {
            switch (baseType) {
                case 'S' -> writer.putNextShort(value);
                case 'I' -> writer.putNextInt(value);
                case 'J' -> writer.putNextLong(value);
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveCharAs(RelativeStackFrameAccessor writer, char baseType, char value) {
            switch (baseType) {
                case 'C' -> writer.putNextChar(value);
                case 'I' -> writer.putNextInt(value);
                case 'J' -> writer.putNextLong(value);
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveIntAs(RelativeStackFrameAccessor writer, char baseType, int value) {
            switch (baseType) {
                case 'I' -> writer.putNextInt(value);
                case 'J' -> writer.putNextLong(value);
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveLongAs(RelativeStackFrameAccessor writer, char baseType, long value) {
            switch (baseType) {
                case 'J' -> writer.putNextLong(value);
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveFloatAs(RelativeStackFrameAccessor writer, char baseType, float value) {
            switch (baseType) {
                case 'F' -> writer.putNextFloat(value);
                case 'D' -> writer.putNextDouble(value);
                default -> throw wrongType();
            }
        }

        private void writePrimitiveDoubleAs(RelativeStackFrameAccessor writer, char baseType, double value) {
            if (baseType == 'D') {
                writer.putNextDouble(value);
            } else {
                throw wrongType();
            }
        }

        private void writePrimitiveVoidAs(RelativeStackFrameAccessor writer, char baseType) {
            switch (baseType) {
                case 'Z' -> writer.putNextBoolean(false);
                case 'B' -> writer.putNextByte((byte) 0);
                case 'S' -> writer.putNextShort((short) 0);
                case 'C' -> writer.putNextChar((char) 0);
                case 'I' -> writer.putNextInt(0);
                case 'J' -> writer.putNextLong(0L);
                case 'F' -> writer.putNextFloat(0.0f);
                case 'D' -> writer.putNextDouble(0.0);
                default -> throw wrongType();
            }
        }

        private static Class<?> getBoxedPrimitiveClass(char baseType) {
            return switch (baseType) {
                case 'Z' -> Boolean.class;
                case 'B' -> Byte.class;
                case 'S' -> Short.class;
                case 'C' -> Character.class;
                case 'I' -> Integer.class;
                case 'J' -> Long.class;
                case 'F' -> Float.class;
                case 'D' -> Double.class;
                default -> null;
            };
        }

        private void adaptArgument(RelativeStackFrameAccessor reader, Class<?> from,
                                   RelativeStackFrameAccessor writer, Class<?> to) {
            if (from.equals(to)) {
                EmulatedStackFrame.copyNextValue(reader, writer);
                return;
            }
            if (to.isPrimitive()) {
                char toBaseType = Wrapper.basicTypeChar(to);
                if (from.isPrimitive()) {
                    char fromBaseType = Wrapper.basicTypeChar(from);
                    switch (fromBaseType) {
                        case 'B' -> writePrimitiveByteAs(writer, toBaseType, reader.nextByte());
                        case 'S' -> writePrimitiveShortAs(writer, toBaseType, reader.nextShort());
                        case 'C' -> writePrimitiveCharAs(writer, toBaseType, reader.nextChar());
                        case 'I' -> writePrimitiveIntAs(writer, toBaseType, reader.nextInt());
                        case 'J' -> writePrimitiveLongAs(writer, toBaseType, reader.nextLong());
                        case 'F' -> writePrimitiveFloatAs(writer, toBaseType, reader.nextFloat());
                        case 'V' -> writePrimitiveVoidAs(writer, toBaseType);
                        default -> throw wrongType(); // 'Z', 'D'
                    }
                } else {
                    if (to == void.class) {
                        return;
                    }
                    Object value = reader.nextReference();
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    from = value.getClass();

                    if (!Wrapper.isWrapperType(from)) {
                        throw badCast(value.getClass(), to);
                    }
                    final Wrapper fromWrapper = Wrapper.forWrapperType(from);
                    final Wrapper toWrapper = Wrapper.forPrimitiveType(to);
                    if (!toWrapper.isConvertibleFrom(fromWrapper)) {
                        throw badCast(from, to);
                    }

                    switch (fromWrapper.basicTypeChar()) {
                        case 'Z' -> writer.putNextBoolean((Boolean) value);
                        case 'B' -> writePrimitiveByteAs(writer, toBaseType, (Byte) value);
                        case 'S' -> writePrimitiveShortAs(writer, toBaseType, (Short) value);
                        case 'C' -> writePrimitiveCharAs(writer, toBaseType, (Character) value);
                        case 'I' -> writePrimitiveIntAs(writer, toBaseType, (Integer) value);
                        case 'J' -> writePrimitiveLongAs(writer, toBaseType, (Long) value);
                        case 'F' -> writePrimitiveFloatAs(writer, toBaseType, (Float) value);
                        case 'D' -> writePrimitiveDoubleAs(writer, toBaseType, (Double) value);
                        default -> throw shouldNotReachHere();
                    }
                }
            } else {
                if (from.isPrimitive()) {
                    // Boxing conversion
                    char fromBaseType = Wrapper.basicTypeChar(from);
                    Class<?> fromBoxed = getBoxedPrimitiveClass(fromBaseType);
                    // 'to' maybe a super class of the boxed `from` type, e.g. Number.
                    if (fromBoxed != null && !to.isAssignableFrom(fromBoxed)) {
                        throw wrongType();
                    }
                    Object boxed = switch (fromBaseType) {
                        case 'Z' -> reader.nextBoolean();
                        case 'B' -> reader.nextByte();
                        case 'S' -> reader.nextShort();
                        case 'C' -> reader.nextChar();
                        case 'I' -> reader.nextInt();
                        case 'J' -> reader.nextLong();
                        case 'F' -> reader.nextFloat();
                        case 'D' -> reader.nextDouble();
                        case 'V' -> null;
                        default -> shouldNotReachHere();
                    };
                    writer.putNextReference(boxed);
                } else {
                    // Cast
                    Object value = reader.nextReference();
                    if (value != null && !to.isAssignableFrom(value.getClass())) {
                        throw badCast(value.getClass(), to);
                    }
                    writer.putNextReference(value);
                }
            }
        }
    }

    public static MethodHandle asTypeAdapter(MethodHandle target, MethodType newType) {
        if (!InvokeAccess.isConvertibleTo(target.type(), newType)) {
            throw new WrongMethodTypeException("cannot convert " + target + " to " + newType);
        }
        return Transformers.makeTransformer(newType, new AsTypeAdapter(target, newType));
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed x3)
    public static MethodHandle asType(MethodHandle target, MethodType newType) {
        MethodType oldType = target.type();
        if (newType.equals(oldType)) return target;
        return asTypeAdapter(target, newType);
    }

    private static class ReinterpretInvoker extends AbstractTransformer {
        private final MethodHandle target;

        ReinterpretInvoker(MethodHandle target) {
            this.target = target;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) throws Throwable {
            var backup = stack.type();
            stack.type(target.type());
            Transformers.invokeTransform(target, stack);
            stack.type(backup);
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static MethodHandle reinterptetHandle(MethodHandle handle, MethodType type) {
        // TODO: check android 8-12L nominalType and type behavior
        var old_type = handle.type();
        if (old_type.equals(type)) {
            return handle;
        }
        if (Transformers.isTransformer(handle)) {
            return makeTransformer(type, new ReinterpretInvoker(handle));
        }
        handle = InvokeAccess.duplicateHandle(handle);
        InvokeAccess.setRealType(handle, type);
        return handle;
    }

    private static class CollectReturnValue extends AbstractTransformer {
        private final MethodHandle target;
        private final MethodHandle collector;
        private final int target_count;
        private final int collector_count;

        CollectReturnValue(MethodHandle target, MethodHandle collector, int collector_count) {
            this.target = target;
            this.collector = collector;
            this.target_count = target.type().parameterCount();
            this.collector_count = collector_count;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stackFrame) throws Throwable {
            StackFrameAccessor this_accessor = stackFrame.accessor();

            // Constructing the target frame.
            MethodType target_type = target.type();
            EmulatedStackFrame target_frame = EmulatedStackFrame.create(target_type);
            StackFrameAccessor target_accessor = target_frame.accessor();
            EmulatedStackFrame.copyArguments(this_accessor, 0,
                    target_accessor, 0, target_count);

            // Invoke the target
            invokeExactNoChecks(target, target_frame);

            // Constructing the collector frame.
            EmulatedStackFrame collectorFrame = EmulatedStackFrame.create(collector.type());
            StackFrameAccessor collector_accessor = collectorFrame.accessor();
            EmulatedStackFrame.copyArguments(this_accessor, target_count,
                    collector_accessor, 0, collector_count);

            // If return type of target is not void, we have a return value to copy.
            if (rtype(target_type) != void.class) {
                EmulatedStackFrame.copyValue(target_accessor, RETURN_VALUE_IDX,
                        collector_accessor, collector_count);
            }

            // Invoke the collector and copy its return value back to the original frame.
            invokeExactNoChecks(collector, collectorFrame);
            EmulatedStackFrame.copyReturnValue(collector_accessor, this_accessor);
        }
    }

    /**
     * The adaptation works as follows:
     * <blockquote><pre>{@code
     * T target(A...)
     * V filter(B... , T)
     * V adapter(A... a, B... b) {
     *     T t = target(a...);
     *     return filter(b..., t);
     * }
     * // and if the target has a void return:
     * void target2(A...);
     * V filter2(B...);
     * V adapter2(A... a, B... b) {
     *   target2(a...);
     *   return filter2(b...);
     * }
     * // and if the filter has a void return:
     * T target3(A...);
     * void filter3(B... , T);
     * void adapter3(A... a, B... b) {
     *   T t = target3(a...);
     *   filter3(b..., t);
     * }
     * }</pre></blockquote>
     *
     * @param target the target method handle
     * @param filter the filter method handle
     * @return the adapter method handle
     */
    // Backport from Hotspot
    public static MethodHandle collectReturnValue(MethodHandle target, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        int value = target.type().returnType() == void.class ? 0 : 1;
        int parameters = filterType.parameterCount() - value;
        if (parameters == 0) {
            return Handles.filterReturnValue(target, filter);
        }
        collectReturnValueChecks(targetType, filterType);
        MethodType newType = targetType.changeReturnType(filterType.returnType());
        newType = newType.appendParameterTypes(filterType.parameterList().subList(0, parameters));
        return makeTransformer(newType, new CollectReturnValue(target, filter, parameters));
    }

    private static void collectReturnValueChecks(MethodType targetType, MethodType filterType) {
        Class<?> rtype = targetType.returnType();
        int filterValues = filterType.parameterCount();
        if (rtype != void.class && (filterValues < 1 || rtype != filterType.parameterType(filterValues - 1)))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
    }
}

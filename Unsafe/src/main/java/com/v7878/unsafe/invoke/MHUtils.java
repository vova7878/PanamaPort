package com.v7878.unsafe.invoke;

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
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.InvokeAccess;
import com.v7878.unsafe.invoke.EmulatedStackFrame.RelativeStackFrameAccessor;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.Transformers.AbstractTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

//TODO: use codegen to improve performance
class MHUtils {
    static class AsTypeAdapter extends AbstractTransformer {
        private final MethodHandle target;
        private final MethodType target_type;

        AsTypeAdapter(MethodHandle target, MethodType target_type) {
            this.target = target;
            this.target_type = target_type;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame callerFrame) throws Throwable {
            apply(target, target_type, callerFrame);
        }

        public static void apply(MethodHandle target, MethodType target_type,
                                 EmulatedStackFrame caller_frame) throws Throwable {
            EmulatedStackFrame target_frame = EmulatedStackFrame.create(target_type);

            var caller_accessor = caller_frame.accessor();
            var target_accessor = target_frame.accessor();

            adaptArguments(caller_accessor, target_accessor);

            Transformers.invokeExactPlain(target, target_frame);

            adaptReturnValue(target_accessor, caller_accessor);
        }

        @AlwaysInline
        private static void adaptArguments(StackFrameAccessor reader, StackFrameAccessor writer) {
            int args = reader.getArgCount();
            for (int i = 0; i < args; ++i) {
                adapt(reader, writer, i);
            }
        }

        @AlwaysInline
        private static void adaptReturnValue(StackFrameAccessor reader, StackFrameAccessor writer) {
            adapt(reader, writer, RETURN_VALUE_IDX);
        }

        @AlwaysInline
        private static RuntimeException wrongType(StackFrameAccessor reader,
                                                  StackFrameAccessor writer, int index) {
            var from = reader.frame().type();
            var to = writer.frame().type();
            if (index == RETURN_VALUE_IDX) {
                throw newWrongMethodTypeException(to, from);
            }
            throw newWrongMethodTypeException(from, to);
        }

        @AlwaysInline
        private static void writeByteAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                        int index, char shorty, byte value) {
            switch (shorty) {
                case 'B' -> writer.setByte(index, value);
                case 'S' -> writer.setShort(index, value);
                case 'I' -> writer.setInt(index, value);
                case 'J' -> writer.setLong(index, value);
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeShortAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                         int index, char shorty, short value) {
            switch (shorty) {
                case 'S' -> writer.setShort(index, value);
                case 'I' -> writer.setInt(index, value);
                case 'J' -> writer.setLong(index, value);
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeCharAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                        int index, char shorty, char value) {
            switch (shorty) {
                case 'C' -> writer.setChar(index, value);
                case 'I' -> writer.setInt(index, value);
                case 'J' -> writer.setLong(index, value);
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeIntAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                       int index, char shorty, int value) {
            switch (shorty) {
                case 'I' -> writer.setInt(index, value);
                case 'J' -> writer.setLong(index, value);
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeLongAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                        int index, char shorty, long value) {
            switch (shorty) {
                case 'J' -> writer.setLong(index, value);
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeFloatAs(StackFrameAccessor reader, StackFrameAccessor writer,
                                         int index, char shorty, float value) {
            switch (shorty) {
                case 'F' -> writer.setFloat(index, value);
                case 'D' -> writer.setDouble(index, value);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static void writeVoidAs(
                StackFrameAccessor reader, StackFrameAccessor writer, int index, char shorty) {
            switch (shorty) {
                case 'Z' -> writer.setBoolean(index, false);
                case 'B' -> writer.setByte(index, (byte) 0);
                case 'S' -> writer.setShort(index, (short) 0);
                case 'C' -> writer.setChar(index, (char) 0);
                case 'I' -> writer.setInt(index, 0);
                case 'J' -> writer.setLong(index, 0L);
                case 'F' -> writer.setFloat(index, 0.0f);
                case 'D' -> writer.setDouble(index, 0.0);
                default -> throw wrongType(reader, writer, index);
            }
        }

        @AlwaysInline
        private static Class<?> boxedPrimitive(char baseType) {
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

        private static void adapt(StackFrameAccessor reader, StackFrameAccessor writer, int index) {
            char from = reader.getArgumentShorty(index), to = writer.getArgumentShorty(index);
            if (from == to) {
                if (from == 'L') {
                    // Cast
                    var to_type = writer.getArgumentType(index);
                    Object value = reader.getReference(index);
                    writer.setReference(index, to_type.cast(value));
                    return;
                }
                EmulatedStackFrame.copyValue(reader, index, writer, index);
                return;
            }
            if (to == 'L') {
                // Note: to != from -> from is primitive
                // Boxing conversion
                var to_type = writer.getArgumentType(index);
                Class<?> from_boxed = boxedPrimitive(from);
                // 'to' maybe a super class of the boxed `from` type, e.g. Number.
                if (from_boxed != null && !to_type.isAssignableFrom(from_boxed)) {
                    throw wrongType(reader, writer, index);
                }
                Object boxed = switch (from) {
                    case 'Z' -> reader.getBoolean(index);
                    case 'B' -> reader.getByte(index);
                    case 'S' -> reader.getShort(index);
                    case 'C' -> reader.getChar(index);
                    case 'I' -> reader.getInt(index);
                    case 'J' -> reader.getLong(index);
                    case 'F' -> reader.getFloat(index);
                    case 'D' -> reader.getDouble(index);
                    case 'V' -> null;
                    default -> shouldNotReachHere();
                };
                writer.setReference(index, boxed);
            } else {
                if (from == 'L') {
                    if (to == 'V') {
                        return;
                    }
                    Object value = reader.getReference(index);
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    var to_type = writer.getArgumentType(index);
                    var from_type = value.getClass();

                    if (!Wrapper.isWrapperType(from_type)) {
                        throw badCast(value.getClass(), to_type);
                    }
                    final Wrapper from_wrapper = Wrapper.forWrapperType(from_type);
                    final Wrapper to_wrapper = Wrapper.forPrimitiveType(to);
                    if (!to_wrapper.isConvertibleFrom(from_wrapper)) {
                        throw badCast(from_type, to_type);
                    }

                    switch (from_wrapper.basicTypeChar()) {
                        case 'Z' -> writer.setBoolean(index, (Boolean) value);
                        case 'B' -> writeByteAs(reader, writer, index, to, (Byte) value);
                        case 'S' -> writeShortAs(reader, writer, index, to, (Short) value);
                        case 'C' -> writeCharAs(reader, writer, index, to, (Character) value);
                        case 'I' -> writeIntAs(reader, writer, index, to, (Integer) value);
                        case 'J' -> writeLongAs(reader, writer, index, to, (Long) value);
                        case 'F' -> writeFloatAs(reader, writer, index, to, (Float) value);
                        case 'D' -> writer.setDouble(index, (Double) value);
                        default -> throw shouldNotReachHere();
                    }
                } else {
                    switch (from) {
                        case 'B' -> writeByteAs(reader, writer, index, to, reader.getByte(index));
                        case 'S' -> writeShortAs(reader, writer, index, to, reader.getShort(index));
                        case 'C' -> writeCharAs(reader, writer, index, to, reader.getChar(index));
                        case 'I' -> writeIntAs(reader, writer, index, to, reader.getInt(index));
                        case 'J' -> writeLongAs(reader, writer, index, to, reader.getLong(index));
                        case 'F' -> writeFloatAs(reader, writer, index, to, reader.getFloat(index));
                        case 'V' -> writeVoidAs(reader, writer, index, to);
                        default -> throw wrongType(reader, writer, index); // 'Z', 'D'
                    }
                }
            }
        }
    }

    public static MethodHandle asTypeAdapter(MethodHandle target, MethodType new_type) {
        if (!InvokeAccess.isConvertibleTo(target.type(), new_type)) {
            throw new WrongMethodTypeException("cannot convert " + target + " to " + new_type);
        }
        MethodType real_type = InvokeAccess.realType(target);
        return makeTransformer(new_type, new AsTypeAdapter(target, real_type));
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed x3)
    public static MethodHandle asType(MethodHandle target, MethodType new_type) {
        if (new_type.equals(InvokeAccess.realType(target))) {
            return InvokeAccess.realTypeHandle(target);
        }
        return asTypeAdapter(target, new_type);
    }

    // TODO: refactor
    private static class ExplicitCastArguments extends AbstractTransformer {
        private final MethodHandle target;
        private final MethodType type;

        ExplicitCastArguments(MethodHandle target, MethodType type) {
            this.target = target;
            this.type = type;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame caller_frame) throws Throwable {
            EmulatedStackFrame target_frame = EmulatedStackFrame.create(InvokeAccess.realType(target));

            var caller_accessor = caller_frame.relativeAccessor();
            var target_accessor = target_frame.relativeAccessor();

            explicitCastArguments(caller_accessor, target_accessor);

            Transformers.invokeExactPlain(target, target_frame);

            explicitCastReturnValue(target_accessor.moveToReturn(), caller_accessor.moveToReturn());
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

    private static void explicitCastArgumentsChecks(MethodType old_type, MethodType new_type) {
        if (old_type.parameterCount() != new_type.parameterCount()) {
            throw new WrongMethodTypeException(
                    "Cannot explicitly cast " + old_type + " to " + new_type);
        }
    }

    public static MethodHandle explicitCastArgumentsAdapter(MethodHandle target, MethodType new_type) {
        explicitCastArgumentsChecks(target.type(), new_type);
        return Transformers.makeTransformer(new_type, new ExplicitCastArguments(target, new_type));
    }

    // fix for PLATFORM-BUG! (Again... Android's MethodHandle API is cursed)
    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType new_type) {
        target = target.asFixedArity();
        if (new_type.equals(InvokeAccess.realType(target))) {
            return InvokeAccess.realTypeHandle(target);
        }
        MethodType old_type = target.type();
        explicitCastArgumentsChecks(old_type, new_type);
        if (InvokeAccess.explicitCastEquivalentToAsType(old_type, new_type)) {
            return asType(target, new_type);
        }
        return Transformers.makeTransformer(new_type, new ExplicitCastArguments(target, new_type));
    }

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

    private static class Collector extends AbstractTransformer {
        private final MethodHandle target;
        private final int arrayOffset;
        private final int arrayLength;
        private final Class<?> componentType;
        private final char shorty;

        Collector(MethodHandle delegate, Class<?> componentType, int start, int length) {
            this.target = delegate;
            this.arrayOffset = start;
            this.arrayLength = length;
            this.componentType = componentType;
            this.shorty = Wrapper.basicTypeChar(componentType);
        }

        @Override
        public void transform(MethodHandle thiz, EmulatedStackFrame callerFrame) throws Throwable {
            StackFrameAccessor caller_accessor = callerFrame.accessor();

            EmulatedStackFrame targetFrame = EmulatedStackFrame.create(target.type());
            var target_accessor = targetFrame.accessor();

            // Copy arguments before the collector array.
            EmulatedStackFrame.copyArguments(caller_accessor, 0,
                    target_accessor, 0, arrayOffset);

            // Copy arguments after the collector array.
            EmulatedStackFrame.copyArguments(caller_accessor, arrayOffset + arrayLength,
                    target_accessor, arrayOffset + 1,
                    caller_accessor.getArgCount() - (arrayOffset + arrayLength));

            switch (shorty) {
                case 'Z' -> {
                    boolean[] array = new boolean[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getBoolean(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'B' -> {
                    byte[] array = new byte[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getByte(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'S' -> {
                    short[] array = new short[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getShort(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'C' -> {
                    char[] array = new char[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getChar(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'I' -> {
                    int[] array = new int[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getInt(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'J' -> {
                    long[] array = new long[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getLong(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'F' -> {
                    float[] array = new float[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getFloat(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'D' -> {
                    double[] array = new double[arrayLength];
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getDouble(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                case 'L' -> {
                    Object[] array = (Object[]) Array.newInstance(componentType, arrayLength);
                    for (int i = 0; i < arrayLength; ++i) {
                        array[i] = caller_accessor.getReference(arrayOffset + i);
                    }
                    target_accessor.setReference(arrayOffset, array);
                }
                default -> shouldNotReachHere();
            }

            Transformers.invokeExactNoChecks(target, targetFrame);
            EmulatedStackFrame.copyReturnValue(target_accessor, caller_accessor);
        }
    }

    public static MethodHandle asCollector(MethodHandle target, int collectArgPos,
                                           Class<?> arrayType, int arrayLength) {
        asCollectorChecks(target.type(), arrayType, collectArgPos, arrayLength);
        var type = target.type().dropParameterTypes(collectArgPos, 1);
        type = type.insertParameterTypes(collectArgPos, Collections.nCopies(arrayLength, arrayType));
        return Transformers.makeTransformer(type, new Collector(target,
                arrayType.getComponentType(), collectArgPos, arrayLength));
    }

    private static void spreadArrayChecks(Class<?> arrayType, int arrayLength) {
        Class<?> arrayElement = arrayType.getComponentType();
        if (arrayElement == null)
            throw newIllegalArgumentException("not an array type", arrayType);
        if ((arrayLength & 0x7F) != arrayLength) {
            if ((arrayLength & 0xFF) != arrayLength)
                throw newIllegalArgumentException("array length is not legal", arrayLength);
            assert (arrayLength >= 128);
            if (arrayElement == long.class ||
                    arrayElement == double.class)
                throw newIllegalArgumentException("array length is not legal for long[] or double[]", arrayLength);
        }
    }

    private static void asCollectorChecks(MethodType targetType, Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        if (pos < 0 || pos >= targetType.parameterCount()) {
            throw newIllegalArgumentException("bad collect position");
        }
        Class<?> param = targetType.parameterType(pos);
        if (param == arrayType || param.isAssignableFrom(arrayType)) return;
        throw newIllegalArgumentException("array type not assignable to argument", targetType, arrayType);
    }

    private static class Spreader extends AbstractTransformer {
        private final MethodHandle target;
        private final int array_offset;
        private final int array_length;
        private final char shorty;

        Spreader(MethodHandle target, Class<?> componentType, int start, int length) {
            this.target = target;
            this.array_offset = start;
            this.array_length = length;
            this.shorty = Wrapper.basicTypeChar(componentType);
        }

        @Override
        public void transform(MethodHandle thiz, EmulatedStackFrame caller_frame) throws Throwable {
            StackFrameAccessor caller_accessor = caller_frame.accessor();

            Object raw_array = caller_accessor.getReference(array_offset);
            // The incoming array may be null if the expected number of array arguments is zero
            int raw_length = (array_length == 0 && raw_array == null) ?
                    0 : Array.getLength(raw_array);
            if (raw_length != array_length) {
                throw new IllegalArgumentException("Invalid array length " +
                        raw_length + " expected " + array_length);
            }

            // Create a new stack frame for the callee
            EmulatedStackFrame target_frame = EmulatedStackFrame.create(target.type());
            StackFrameAccessor target_accessor = target_frame.accessor();

            // Copy arguments before the array
            EmulatedStackFrame.copyArguments(caller_accessor, 0,
                    target_accessor, 0, array_offset);

            // Copy arguments after the array
            EmulatedStackFrame.copyArguments(caller_accessor, array_offset + 1,
                    target_accessor, array_offset + array_length,
                    target_accessor.getArgCount() - (array_offset + array_length));

            if (raw_length != 0) {
                switch (shorty) {
                    case 'Z' -> {
                        boolean[] array = (boolean[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setBoolean(array_offset + i, array[i]);
                        }
                    }
                    case 'B' -> {
                        byte[] array = (byte[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setByte(array_offset + i, array[i]);
                        }
                    }
                    case 'S' -> {
                        short[] array = (short[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setShort(array_offset + i, array[i]);
                        }
                    }
                    case 'C' -> {
                        char[] array = (char[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setChar(array_offset + i, array[i]);
                        }
                    }
                    case 'I' -> {
                        int[] array = (int[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setInt(array_offset + i, array[i]);
                        }
                    }
                    case 'J' -> {
                        long[] array = (long[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setLong(array_offset + i, array[i]);
                        }
                    }
                    case 'F' -> {
                        float[] array = (float[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setFloat(array_offset + i, array[i]);
                        }
                    }
                    case 'D' -> {
                        double[] array = (double[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setDouble(array_offset + i, array[i]);
                        }
                    }
                    case 'L' -> {
                        Object[] array = (Object[]) raw_array;
                        for (int i = 0; i < array.length; ++i) {
                            target_accessor.setReference(array_offset + i, array[i]);
                        }
                    }
                    default -> shouldNotReachHere();
                }
            }

            Transformers.invokeExactNoChecks(target, target_frame);
            EmulatedStackFrame.copyReturnValue(target_accessor, caller_accessor);
        }
    }

    public static MethodHandle asSpreader(MethodHandle target, int spreadArgPos,
                                          Class<?> arrayType, int arrayLength) {
        MethodType partType = asSpreaderChecks(target.type(), arrayType, spreadArgPos, arrayLength);
        MethodType adapterType = partType.insertParameterTypes(spreadArgPos, arrayType);
        var componentType = arrayType.getComponentType();
        MethodType needType = partType.insertParameterTypes(spreadArgPos,
                Collections.nCopies(arrayLength, componentType));
        target = Handles.asType(target, needType);
        return Transformers.makeTransformer(adapterType, new Spreader(target,
                componentType, spreadArgPos, arrayLength));
    }

    private static MethodType asSpreaderChecks(MethodType targetType, Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = targetType.parameterCount();
        if (nargs < arrayLength || arrayLength < 0)
            throw newIllegalArgumentException("bad spread array length");
        if (pos < 0 || pos + arrayLength > nargs) {
            throw newIllegalArgumentException("bad spread position");
        }
        return targetType.dropParameterTypes(pos, pos + arrayLength);
    }
}

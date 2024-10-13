package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.objectFieldOffset;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.assertEq;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public final class EmulatedStackFrame {
    public static final int RETURN_VALUE_IDX = -2;

    @AlwaysInline
    private static boolean is64BitPrimitive(Class<?> type) {
        return type == double.class || type == long.class;
    }

    @AlwaysInline
    static int getSize(Class<?> type) {
        assert type.isPrimitive();
        // NOTE: size of void is 4
        return is64BitPrimitive(type) ? 8 : 4;
    }

    @AlwaysInline
    private static void checkAssignable(Class<?> expected, Class<?> actual) {
        if (expected == actual) {
            return;
        }
        throw new IllegalArgumentException(
                String.format("Incorrect type: %s, expected: %s", actual, expected));
    }

    static final Class<?> esf_class = nothrows_run(() ->
            Class.forName("dalvik.system.EmulatedStackFrame"));

    @DoNotShrink
    @DoNotObfuscate
    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static EmulatedStackFrame wrap(Object esf) {
        assert esf_class.isAssignableFrom(esf.getClass());
        return new EmulatedStackFrame(esf);
    }

    private static final MethodHandle esf_create =
            unreflect(getDeclaredMethod(esf_class, "create", MethodType.class));

    public static EmulatedStackFrame create(MethodType frameType) {
        return new EmulatedStackFrame(nothrows_run(() -> esf_create.invoke(frameType)));
    }

    final Object esf;

    private EmulatedStackFrame(Object esf) {
        this.esf = esf;
    }

    private static final long references_offset =
            objectFieldOffset(getDeclaredField(esf_class, "references"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public Object[] references() {
        return (Object[]) getObject(esf, references_offset);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void setReferences(Object[] references) {
        Objects.requireNonNull(references);
        putObject(esf, references_offset, references);
    }

    private static final long stackFrame_offset =
            objectFieldOffset(getDeclaredField(esf_class, "stackFrame"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public byte[] stackFrame() {
        return (byte[]) getObject(esf, stackFrame_offset);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void setStackFrame(byte[] stackFrame) {
        Objects.requireNonNull(stackFrame);
        putObject(esf, stackFrame_offset, stackFrame);
    }

    private static final long type_offset =
            objectFieldOffset(getDeclaredField(esf_class, "type"));

    public MethodType type() {
        return (MethodType) getObject(esf, type_offset);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void setType(MethodType type) {
        Objects.requireNonNull(type);
        putObject(esf, type_offset, type);
    }

    public StackFrameAccessor createAccessor() {
        return new StackFrameAccessor(this);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void copyArguments(StackFrameAccessor reader, int reader_start_idx,
                                     StackFrameAccessor writer, int writer_start_idx, int count) {
        Objects.checkFromIndexSize(reader_start_idx, count, reader.ptypes.length);
        Objects.checkFromIndexSize(writer_start_idx, count, writer.ptypes.length);

        if (count == 0) return;

        int reader_frame_start = reader.frameOffsets[reader_start_idx];
        int reader_ref_start = reader.referencesOffsets[reader_start_idx];

        int writer_frame_start = writer.frameOffsets[writer_start_idx];
        int writer_ref_start = writer.referencesOffsets[writer_start_idx];

        int frame_count = assertEq(reader.frameOffsets[reader_start_idx + count] - reader_frame_start,
                writer.frameOffsets[writer_start_idx + count] - writer_frame_start);
        int ref_count = assertEq(reader.referencesOffsets[reader_start_idx + count] - reader_ref_start,
                writer.referencesOffsets[writer_start_idx + count] - writer_ref_start);

        System.arraycopy(reader.frame.stackFrame(), reader_frame_start,
                writer.frame.stackFrame(), writer_frame_start, frame_count);
        System.arraycopy(reader.frame.references(), reader_ref_start,
                writer.frame.references(), writer_ref_start, ref_count);
    }

    public void copyReturnValueTo(EmulatedStackFrame other) {
        final Class<?> returnType = type().erase().returnType();
        checkAssignable(other.type().erase().returnType(), returnType);
        if (returnType.isPrimitive()) {
            byte[] this_stack = stackFrame();
            byte[] other_stack = other.stackFrame();
            int size = getSize(returnType);
            System.arraycopy(this_stack, this_stack.length - size,
                    other_stack, other_stack.length - size, size);
        } else {
            Object[] this_references = references();
            Object[] other_references = other.references();
            other_references[other_references.length - 1]
                    = this_references[this_references.length - 1];
        }
    }

    public static void copyNext(StackFrameAccessor reader,
                                StackFrameAccessor writer, Class<?> type) {
        switch (Wrapper.basicTypeChar(type)) {
            case 'L' -> writer.putNextReference(reader.nextReference());
            case 'Z' -> writer.putNextBoolean(reader.nextBoolean());
            case 'B' -> writer.putNextByte(reader.nextByte());
            case 'C' -> writer.putNextChar(reader.nextChar());
            case 'S' -> writer.putNextShort(reader.nextShort());
            case 'I' -> writer.putNextInt(reader.nextInt());
            case 'J' -> writer.putNextLong(reader.nextLong());
            case 'F' -> writer.putNextFloat(reader.nextFloat());
            case 'D' -> writer.putNextDouble(reader.nextDouble());
            default -> throw shouldNotReachHere();
        }
    }

    public static final class StackFrameAccessor {
        private final int[] frameOffsets;
        private final int[] referencesOffsets;

        private final ByteBuffer frameBuf;
        private final Object[] references;
        private int currentReference;

        private final EmulatedStackFrame frame;
        private final Class<?>[] ptypes;
        private final Class<?> rtype;

        private int argumentIdx;

        public StackFrameAccessor(EmulatedStackFrame stackFrame) {
            frame = stackFrame;
            MethodType type = stackFrame.type().erase();
            rtype = InvokeAccess.rtype(type);
            ptypes = InvokeAccess.ptypes(type);
            frameBuf = ByteBuffer.wrap(stackFrame.stackFrame())
                    .order(ByteOrder.nativeOrder());
            references = frame.references();
            MethodTypeForm form = MethodTypeHacks.getForm(type);
            frameOffsets = form.frameOffsets();
            referencesOffsets = form.referencesOffsets();
        }

        @AlwaysInline
        public EmulatedStackFrame frame() {
            return frame;
        }

        @AlwaysInline
        public int currentArgument() {
            return argumentIdx;
        }

        @AlwaysInline
        private void checkIndex(int index) {
            if ((index < 0 || index >= ptypes.length) && (index != RETURN_VALUE_IDX)) {
                throw new IllegalArgumentException("Invalid argument index: " + index);
            }
        }

        @AlwaysInline
        private int toArrayIndex(int index) {
            return index == RETURN_VALUE_IDX ? ptypes.length : index;
        }

        @AlwaysInline
        private int toFrameOffset(int index) {
            return frameOffsets[toArrayIndex(index)];
        }

        @AlwaysInline
        private int toReferencesOffset(int index) {
            return referencesOffsets[toArrayIndex(index)];
        }

        @AlwaysInline
        public Class<?> getArgumentType(int index) {
            checkIndex(index);
            return (index == RETURN_VALUE_IDX) ? rtype : ptypes[index];
        }

        @AlwaysInline
        private void checkWriteType(int index, Class<?> expected) {
            checkAssignable(getArgumentType(index), expected);
        }

        @AlwaysInline
        private void checkWriteType(Class<?> expected) {
            checkAssignable(getArgumentType(argumentIdx), expected);
        }

        @AlwaysInline
        private void checkReadType(int index, Class<?> expected) {
            checkAssignable(expected, getArgumentType(index));
        }

        @AlwaysInline
        private void checkReadType(Class<?> expected) {
            checkAssignable(expected, getArgumentType(argumentIdx));
        }

        public StackFrameAccessor moveTo(int index) {
            checkIndex(index);
            int array_index = toArrayIndex(index);
            currentReference = referencesOffsets[array_index];
            frameBuf.position(frameOffsets[array_index]);
            argumentIdx = index;
            return this;
        }

        @DoNotShrink
        @DoNotObfuscate
        public StackFrameAccessor moveToReturn() {
            return moveTo(RETURN_VALUE_IDX);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextBoolean(boolean value) {
            checkWriteType(boolean.class);
            argumentIdx++;
            frameBuf.putInt(value ? 1 : 0);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextByte(byte value) {
            checkWriteType(byte.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextChar(char value) {
            checkWriteType(char.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextShort(short value) {
            checkWriteType(short.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextInt(int value) {
            checkWriteType(int.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextFloat(float value) {
            checkWriteType(float.class);
            argumentIdx++;
            frameBuf.putFloat(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextLong(long value) {
            checkWriteType(long.class);
            argumentIdx++;
            frameBuf.putLong(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextDouble(double value) {
            checkWriteType(double.class);
            argumentIdx++;
            frameBuf.putDouble(value);
        }

        @DoNotShrink
        @DoNotObfuscate
        public void putNextReference(Object value) {
            checkWriteType(Object.class);
            argumentIdx++;
            references[currentReference++] = value;
        }

        public void putNextValue(Object value) {
            char shorty = Wrapper.basicTypeChar(getArgumentType(argumentIdx));
            argumentIdx++;
            switch (shorty) {
                case 'V' -> {
                }
                case 'L' -> references[currentReference++] = value;
                case 'Z' -> frameBuf.putInt((boolean) value ? 1 : 0);
                case 'B' -> frameBuf.putInt((byte) value);
                case 'C' -> frameBuf.putInt((char) value);
                case 'S' -> frameBuf.putInt((short) value);
                case 'I' -> frameBuf.putInt((int) value);
                case 'F' -> frameBuf.putFloat((float) value);
                case 'J' -> frameBuf.putLong((long) value);
                case 'D' -> frameBuf.putDouble((double) value);
                default -> throw shouldNotReachHere();
            }
        }

        public void setBoolean(int index, boolean value) {
            checkWriteType(index, boolean.class);
            frameBuf.putInt(toFrameOffset(index), value ? 1 : 0);
        }

        public void setByte(int index, byte value) {
            checkWriteType(index, byte.class);
            frameBuf.putInt(toFrameOffset(index), value);
        }

        public void setChar(int index, char value) {
            checkWriteType(index, char.class);
            frameBuf.putInt(toFrameOffset(index), value);
        }

        public void setShort(int index, short value) {
            checkWriteType(index, short.class);
            frameBuf.putInt(toFrameOffset(index), value);
        }

        public void setInt(int index, int value) {
            checkWriteType(index, int.class);
            frameBuf.putInt(toFrameOffset(index), value);
        }

        public void setFloat(int index, float value) {
            checkWriteType(index, float.class);
            frameBuf.putFloat(toFrameOffset(index), value);
        }

        public void setLong(int index, long value) {
            checkWriteType(index, long.class);
            frameBuf.putLong(toFrameOffset(index), value);
        }

        public void setDouble(int index, double value) {
            checkWriteType(index, double.class);
            frameBuf.putDouble(toFrameOffset(index), value);
        }

        public void setReference(int index, Object value) {
            checkWriteType(index, Object.class);
            references[toReferencesOffset(index)] = value;
        }

        public void setValue(int index, Object value) {
            char shorty = Wrapper.basicTypeChar(getArgumentType(index));
            switch (shorty) {
                case 'V' -> {
                }
                case 'L' -> references[toReferencesOffset(index)] = value;
                case 'Z' -> frameBuf.putInt(toFrameOffset(index), (boolean) value ? 1 : 0);
                case 'B' -> frameBuf.putInt(toFrameOffset(index), (byte) value);
                case 'C' -> frameBuf.putInt(toFrameOffset(index), (char) value);
                case 'S' -> frameBuf.putInt(toFrameOffset(index), (short) value);
                case 'I' -> frameBuf.putInt(toFrameOffset(index), (int) value);
                case 'F' -> frameBuf.putFloat(toFrameOffset(index), (float) value);
                case 'J' -> frameBuf.putLong(toFrameOffset(index), (long) value);
                case 'D' -> frameBuf.putDouble(toFrameOffset(index), (double) value);
                default -> throw shouldNotReachHere();
            }
        }

        @DoNotShrink
        @DoNotObfuscate
        public boolean nextBoolean() {
            checkReadType(boolean.class);
            argumentIdx++;
            return frameBuf.getInt() != 0;
        }

        @DoNotShrink
        @DoNotObfuscate
        public byte nextByte() {
            checkReadType(byte.class);
            argumentIdx++;
            return (byte) frameBuf.getInt();
        }

        @DoNotShrink
        @DoNotObfuscate
        public char nextChar() {
            checkReadType(char.class);
            argumentIdx++;
            return (char) frameBuf.getInt();
        }

        @DoNotShrink
        @DoNotObfuscate
        public short nextShort() {
            checkReadType(short.class);
            argumentIdx++;
            return (short) frameBuf.getInt();
        }

        @DoNotShrink
        @DoNotObfuscate
        public int nextInt() {
            checkReadType(int.class);
            argumentIdx++;
            return frameBuf.getInt();
        }

        @DoNotShrink
        @DoNotObfuscate
        public float nextFloat() {
            checkReadType(float.class);
            argumentIdx++;
            return frameBuf.getFloat();
        }

        @DoNotShrink
        @DoNotObfuscate
        public long nextLong() {
            checkReadType(long.class);
            argumentIdx++;
            return frameBuf.getLong();
        }

        @DoNotShrink
        @DoNotObfuscate
        public double nextDouble() {
            checkReadType(double.class);
            argumentIdx++;
            return frameBuf.getDouble();
        }

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unchecked")
        public <T> T nextReference() {
            checkReadType(Object.class);
            argumentIdx++;
            return (T) references[currentReference++];
        }

        public Object nextValue() {
            char shorty = Wrapper.basicTypeChar(getArgumentType(argumentIdx));
            argumentIdx++;
            return switch (shorty) {
                case 'L' -> references[currentReference++];
                case 'Z' -> (frameBuf.getInt() != 0);
                case 'B' -> (byte) frameBuf.getInt();
                case 'C' -> (char) frameBuf.getInt();
                case 'S' -> (short) frameBuf.getInt();
                case 'I' -> frameBuf.getInt();
                case 'F' -> frameBuf.getFloat();
                case 'J' -> frameBuf.getLong();
                case 'D' -> frameBuf.getDouble();
                /* 'V' */
                default -> throw shouldNotReachHere();
            };
        }

        public boolean getBoolean(int index) {
            checkReadType(index, boolean.class);
            return frameBuf.getInt(toFrameOffset(index)) != 0;
        }

        public byte getByte(int index) {
            checkReadType(index, byte.class);
            return (byte) frameBuf.getInt(toFrameOffset(index));
        }

        public char getChar(int index) {
            checkReadType(index, char.class);
            return (char) frameBuf.getInt(toFrameOffset(index));
        }

        public short getShort(int index) {
            checkReadType(index, short.class);
            return (short) frameBuf.getInt(toFrameOffset(index));
        }

        public int getInt(int index) {
            checkReadType(index, int.class);
            return frameBuf.getInt(toFrameOffset(index));
        }

        public float getFloat(int index) {
            checkReadType(index, float.class);
            return frameBuf.getFloat(toFrameOffset(index));
        }

        public long getLong(int index) {
            checkReadType(index, long.class);
            return frameBuf.getLong(toFrameOffset(index));
        }

        public double getDouble(int index) {
            checkReadType(index, double.class);
            return frameBuf.getDouble(toFrameOffset(index));
        }

        @SuppressWarnings("unchecked")
        public <T> T getReference(int index) {
            checkReadType(index, Object.class);
            return (T) references[toReferencesOffset(index)];
        }

        public Object getValue(int index) {
            char shorty = Wrapper.basicTypeChar(getArgumentType(index));
            return switch (shorty) {
                case 'L' -> references[toReferencesOffset(index)];
                case 'Z' -> (frameBuf.getInt(toFrameOffset(index)) != 0);
                case 'B' -> (byte) frameBuf.getInt(toFrameOffset(index));
                case 'C' -> (char) frameBuf.getInt(toFrameOffset(index));
                case 'S' -> (short) frameBuf.getInt(toFrameOffset(index));
                case 'I' -> frameBuf.getInt(toFrameOffset(index));
                case 'F' -> frameBuf.getFloat(toFrameOffset(index));
                case 'J' -> frameBuf.getLong(toFrameOffset(index));
                case 'D' -> frameBuf.getDouble(toFrameOffset(index));
                /* 'V' */
                default -> throw shouldNotReachHere();
            };
        }
    }

    @Override
    public String toString() {
        return "EmulatedStackFrame{type=" + type() + ", stackFrame=[" + Arrays.toString(stackFrame())
                + "], references=" + Arrays.toString(references()) + "}";
    }
}

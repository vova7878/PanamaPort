package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.objectFieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;

import androidx.annotation.Keep;

import com.v7878.dex.TypeId;
import com.v7878.misc.Checks;
import com.v7878.unsafe.DangerLevel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class EmulatedStackFrame {
    public static final int RETURN_VALUE_IDX = -2;

    private static boolean is64BitPrimitive(Class<?> type) {
        return type == double.class || type == long.class;
    }

    public static int getSize(Class<?> type) {
        if (!type.isPrimitive()) {
            throw new IllegalArgumentException("type.isPrimitive() == false: " + type);
        }
        return is64BitPrimitive(type) ? 8 : 4;
    }

    private static void checkAssignable(Class<?> expectedType, Class<?> actualType) {
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new IllegalArgumentException("Incorrect type: " + actualType
                    + ", expected: " + expectedType);
        }
    }

    static final Class<?> esf_class = nothrows_run(
            () -> Class.forName("dalvik.system.EmulatedStackFrame"));

    @Keep
    public static EmulatedStackFrame wrap(Object esf) {
        //null + class check
        esf.getClass().asSubclass(esf_class);
        return new EmulatedStackFrame(esf);
    }

    private static final MethodHandle esf_create = nothrows_run(
            () -> unreflect(getDeclaredMethod(esf_class, "create", MethodType.class)));

    public static EmulatedStackFrame create(MethodType frameType) {
        return new EmulatedStackFrame(nothrows_run(() -> esf_create.invoke(frameType)));
    }

    final Object esf;

    private EmulatedStackFrame(Object esf) {
        this.esf = esf;
    }

    private static final long references_offset = nothrows_run(
            () -> objectFieldOffset(getDeclaredField(esf_class, "references")));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public Object[] references() {
        return (Object[]) getObject(esf, references_offset);
    }

    private static final long stackFrame_offset = nothrows_run(
            () -> objectFieldOffset(getDeclaredField(esf_class, "stackFrame")));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public byte[] stackFrame() {
        return (byte[]) getObject(esf, stackFrame_offset);
    }

    private static final long type_offset = nothrows_run(
            () -> objectFieldOffset(getDeclaredField(esf_class, "type")));

    public MethodType type() {
        return (MethodType) getObject(esf, type_offset);
    }

    public StackFrameAccessor createAccessor() {
        StackFrameAccessor out = new StackFrameAccessor();
        out.attach(this);
        return out;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void copyArguments(StackFrameAccessor reader, int reader_start_idx,
                                     StackFrameAccessor writer, int writer_start_idx, int count) {
        Checks.checkFromIndexSize(reader_start_idx, count,
                reader.frame().type().parameterCount());
        Checks.checkFromIndexSize(writer_start_idx, count,
                writer.frame().type().parameterCount());

        if (count == 0) return;

        int reader_frame_start = reader.frameOffsets[reader_start_idx];
        int reader_ref_start = reader.referencesOffsets[reader_start_idx];

        int writer_frame_start = writer.frameOffsets[writer_start_idx];
        int writer_ref_start = writer.referencesOffsets[writer_start_idx];

        int frame_count = reader.frameOffsets[reader_start_idx + count] - reader_frame_start;
        int ref_count = reader.referencesOffsets[reader_start_idx + count] - reader_ref_start;

        if (frame_count != (writer.frameOffsets[writer_start_idx + count] - writer_frame_start)) {
            throw new IllegalArgumentException("reader and writer have different stack frame range size");
        }
        if (ref_count != (writer.referencesOffsets[writer_start_idx + count] - writer_ref_start)) {
            throw new IllegalArgumentException("reader and writer have different stack ref range size");
        }

        System.arraycopy(reader.frame.stackFrame(), reader_frame_start,
                writer.frame.stackFrame(), writer_frame_start, frame_count);
        System.arraycopy(reader.frame.references(), reader_ref_start,
                writer.frame.references(), writer_ref_start, ref_count);
    }

    public void copyReturnValueTo(EmulatedStackFrame other) {
        final Class<?> returnType = type().returnType();
        checkAssignable(other.type().returnType(), returnType);
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
        switch (TypeId.of(type).getShorty()) {
            case 'L' -> writer.putNextReference(reader.nextReference(type), type);
            case 'Z' -> writer.putNextBoolean(reader.nextBoolean());
            case 'B' -> writer.putNextByte(reader.nextByte());
            case 'C' -> writer.putNextChar(reader.nextChar());
            case 'S' -> writer.putNextShort(reader.nextShort());
            case 'I' -> writer.putNextInt(reader.nextInt());
            case 'J' -> writer.putNextLong(reader.nextLong());
            case 'F' -> writer.putNextFloat(reader.nextFloat());
            case 'D' -> writer.putNextDouble(reader.nextDouble());
            default -> throw new AssertionError("Cannot get here");
        }
    }

    public static class StackFrameAccessor {

        protected int referencesOffset;
        protected int argumentIdx;

        int[] frameOffsets;
        int[] referencesOffsets;

        protected ByteBuffer frameBuf;
        protected EmulatedStackFrame frame;

        public StackFrameAccessor() {
            referencesOffset = 0;
            argumentIdx = 0;
            frameBuf = null;
        }

        public void attach(EmulatedStackFrame stackFrame) {
            if (frame != stackFrame) {
                // Re-initialize storage if not re-attaching to the same stackFrame.
                frame = stackFrame;
                frameBuf = ByteBuffer.wrap(frame.stackFrame())
                        .order(ByteOrder.LITTLE_ENDIAN);
                buildTables(stackFrame.type());
            }
            referencesOffset = 0;
            argumentIdx = 0;
        }

        public EmulatedStackFrame frame() {
            return frame;
        }

        private void buildTables(MethodType methodType) {
            final Class<?>[] ptypes = methodType.parameterArray();
            frameOffsets = new int[ptypes.length + 1];
            referencesOffsets = new int[ptypes.length + 1];
            int frameOffset = 0;
            int referenceOffset = 0;
            for (int i = 0; i < ptypes.length; ++i) {
                final Class<?> ptype = ptypes[i];
                if (ptype.isPrimitive()) {
                    frameOffset += getSize(ptype);
                } else {
                    referenceOffset++;
                }
                frameOffsets[i + 1] = frameOffset;
                referencesOffsets[i + 1] = referenceOffset;
            }
        }

        public Class<?> getCurrentArgumentType() {
            if (argumentIdx >= frame.type().parameterCount()
                    || argumentIdx == (RETURN_VALUE_IDX + 1)) {
                throw new IllegalArgumentException("Invalid argument index: " + argumentIdx);
            }
            MethodType type = frame.type();
            return (argumentIdx == RETURN_VALUE_IDX)
                    ? type.returnType() : type.parameterType(argumentIdx);
        }

        public void checkWriteType(Class<?> expectedType) {
            checkAssignable(getCurrentArgumentType(), expectedType);
        }

        public void checkReadType(Class<?> expectedType) {
            checkAssignable(expectedType, getCurrentArgumentType());
        }

        public StackFrameAccessor moveTo(int argumentIndex) {
            if (argumentIndex == RETURN_VALUE_IDX) {
                return moveToReturn();
            }
            referencesOffset = referencesOffsets[argumentIndex];
            frameBuf.position(frameOffsets[argumentIndex]);
            argumentIdx = argumentIndex;
            return this;
        }

        public StackFrameAccessor moveToReturn() {
            Class<?> rtype = frame.type().returnType();
            argumentIdx = RETURN_VALUE_IDX;
            // Position the cursor appropriately. The return value is either the last element
            // of the references array, or the last 4 or 8 bytes of the stack frame.
            if (rtype.isPrimitive()) {
                frameBuf.position(frameBuf.capacity() - getSize(rtype));
            } else {
                referencesOffset = frame.references().length - 1;
            }
            return this;
        }

        public void putNextBoolean(boolean value) {
            checkWriteType(boolean.class);
            argumentIdx++;
            frameBuf.putInt(value ? 1 : 0);
        }

        public void putNextByte(byte value) {
            checkWriteType(byte.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        public void putNextChar(char value) {
            checkWriteType(char.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        public void putNextShort(short value) {
            checkWriteType(short.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        public void putNextInt(int value) {
            checkWriteType(int.class);
            argumentIdx++;
            frameBuf.putInt(value);
        }

        public void putNextFloat(float value) {
            checkWriteType(float.class);
            argumentIdx++;
            frameBuf.putFloat(value);
        }

        public void putNextLong(long value) {
            checkWriteType(long.class);
            argumentIdx++;
            frameBuf.putLong(value);
        }

        public void putNextDouble(double value) {
            checkWriteType(double.class);
            argumentIdx++;
            frameBuf.putDouble(value);
        }

        public void putNextReference(Object value, Class<?> expectedType) {
            checkWriteType(expectedType);
            argumentIdx++;
            frame.references()[referencesOffset++] = value;
        }

        public void putNextValue(Object value) {
            char shorty = TypeId.of(getCurrentArgumentType()).getShorty();
            argumentIdx++;
            switch (shorty) {
                case 'V':
                    break;
                case 'L':
                    frame.references()[referencesOffset++] = value;
                    break;
                case 'Z':
                    frameBuf.putInt((boolean) value ? 1 : 0);
                    break;
                case 'B':
                    frameBuf.putInt((byte) value);
                    break;
                case 'C':
                    frameBuf.putInt((char) value);
                    break;
                case 'S':
                    frameBuf.putInt((short) value);
                    break;
                case 'I':
                    frameBuf.putInt((int) value);
                    break;
                case 'F':
                    frameBuf.putFloat((float) value);
                    break;
                case 'J':
                    frameBuf.putLong((long) value);
                    break;
                case 'D':
                    frameBuf.putDouble((double) value);
                    break;
                default:
                    throw new AssertionError("Cannot get here");
            }
        }

        public boolean nextBoolean() {
            checkReadType(boolean.class);
            argumentIdx++;
            return (frameBuf.getInt() != 0);
        }

        public byte nextByte() {
            checkReadType(byte.class);
            argumentIdx++;
            return (byte) frameBuf.getInt();
        }

        public char nextChar() {
            checkReadType(char.class);
            argumentIdx++;
            return (char) frameBuf.getInt();
        }

        public short nextShort() {
            checkReadType(short.class);
            argumentIdx++;
            return (short) frameBuf.getInt();
        }

        public int nextInt() {
            checkReadType(int.class);
            argumentIdx++;
            return frameBuf.getInt();
        }

        public float nextFloat() {
            checkReadType(float.class);
            argumentIdx++;
            return frameBuf.getFloat();
        }

        public long nextLong() {
            checkReadType(long.class);
            argumentIdx++;
            return frameBuf.getLong();
        }

        public double nextDouble() {
            checkReadType(double.class);
            argumentIdx++;
            return frameBuf.getDouble();
        }

        public <T> T nextReference(Class<T> expectedType) {
            checkReadType(expectedType);
            argumentIdx++;
            //noinspection unchecked
            return (T) frame.references()[referencesOffset++];
        }

        public Object nextValue() {
            char shorty = TypeId.of(getCurrentArgumentType()).getShorty();
            argumentIdx++;
            return switch (shorty) {
                case 'L' -> frame.references()[referencesOffset++];
                case 'Z' -> (frameBuf.getInt() != 0);
                case 'B' -> (byte) frameBuf.getInt();
                case 'C' -> (char) frameBuf.getInt();
                case 'S' -> (short) frameBuf.getInt();
                case 'I' -> frameBuf.getInt();
                case 'F' -> frameBuf.getFloat();
                case 'J' -> frameBuf.getLong();
                case 'D' -> frameBuf.getDouble();
                /* 'V' */
                default -> throw new AssertionError("Cannot get here");
            };
        }
    }

    @Override
    public String toString() {
        return "EmulatedStackFrame{type=" + type() + ", stackFrame=[" + Arrays.toString(stackFrame())
                + "], references=" + Arrays.toString(references()) + "}";
    }
}

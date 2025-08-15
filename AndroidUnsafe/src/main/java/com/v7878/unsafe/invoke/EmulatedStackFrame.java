package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getHiddenInstanceFields;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;
import static com.v7878.unsafe.access.InvokeAccess.MT_ID;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ArtFieldUtils;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.AccessLinker.FieldAccess;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;

import dalvik.system.DexFile;

public final class EmulatedStackFrame {
    static final Class<?> ESF_CLASS = ClassUtils.sysClass(
            "dalvik.system.EmulatedStackFrame");
    private static final AccessI ACCESS;

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @DoNotShrink
        @DoNotObfuscate
        abstract Object create(MethodType type, Object[] references, byte[] stackFrame);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "dalvik.system.EmulatedStackFrame", name = "type")
        abstract MethodType type(Object frame);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "dalvik.system.EmulatedStackFrame", name = "type")
        abstract void type(Object frame, MethodType type);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "dalvik.system.EmulatedStackFrame", name = "references")
        abstract Object[] references(Object frame);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "dalvik.system.EmulatedStackFrame", name = "references")
        abstract void references(Object frame, Object[] references);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "dalvik.system.EmulatedStackFrame", name = "stackFrame")
        abstract byte[] primitives(Object frame);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "dalvik.system.EmulatedStackFrame", name = "stackFrame")
        abstract void primitives(Object frame, byte[] primitives);
    }

    static {
        for (var field : getHiddenInstanceFields(ESF_CLASS)) {
            ArtFieldUtils.makeFieldPublic(field);
            ArtFieldUtils.makeFieldNonFinal(field);
        }

        Class<?> partial_impl = AccessLinker.generateImplClass(AccessI.class);

        TypeId esf = TypeId.of(ESF_CLASS);

        String access_name = EmulatedStackFrame.class.getName() + "$Access";
        TypeId access_id = TypeId.ofName(access_name);

        var type_id = FieldId.of(esf, "type", MT_ID);
        var callsite_id = FieldId.of(esf, "callsiteType", MT_ID);
        var references_id = FieldId.of(esf, "references", TypeId.OBJECT.array());
        var primitives_id = FieldId.of(esf, "stackFrame", TypeId.B.array());

        ClassDef access_def = ClassBuilder.build(access_id, cb -> cb
                .withSuperClass(TypeId.of(partial_impl))
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("create")
                        .withReturnType(TypeId.OBJECT)
                        .withParameterTypes(MT_ID, TypeId.OBJECT.array(), TypeId.B.array())
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.new_instance(ib.this_(), esf);
                            ib.iop(PUT_OBJECT, ib.p(0), ib.this_(), type_id);
                            if (ART_SDK_INT <= 32) {
                                ib.iop(PUT_OBJECT, ib.p(0), ib.this_(), callsite_id);
                            }
                            ib.iop(PUT_OBJECT, ib.p(1), ib.this_(), references_id);
                            ib.iop(PUT_OBJECT, ib.p(2), ib.this_(), primitives_id);
                            ib.return_object(ib.this_());
                        })
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(access_def)));
        setTrusted(dex);

        ClassLoader loader = EmulatedStackFrame.class.getClassLoader();

        Class<?> invoker_class = loadClass(dex, access_name, loader);
        ClassUtils.forceClassVerified(invoker_class);
        ACCESS = (AccessI) allocateInstance(invoker_class);
    }

    public static final int RETURN_VALUE_IDX = -2;
    private static final int SREG = 4;
    private static final int DREG = 8;

    @AlwaysInline
    private static boolean is64BitPrimitive(Class<?> type) {
        return type == double.class || type == long.class;
    }

    @AlwaysInline
    static int getSize(Class<?> type) {
        assert type.isPrimitive();
        // NOTE: size of void is 4
        return is64BitPrimitive(type) ? DREG : SREG;
    }

    @AlwaysInline
    private static char checkAssignable(char expected, char actual) {
        if (expected == actual) {
            return expected;
        }
        throw new IllegalArgumentException(String.format(
                "Incorrect shorty: %s, expected: %s", actual, expected));
    }

    @DoNotShrink
    @DoNotObfuscate
    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static EmulatedStackFrame wrap(Object esf) {
        assert ESF_CLASS.isAssignableFrom(esf.getClass());
        return new EmulatedStackFrame(esf);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static EmulatedStackFrame create(MethodType frameType, Object[] references, byte[] stackFrame) {
        Objects.requireNonNull(frameType);
        Objects.requireNonNull(references);
        Objects.requireNonNull(stackFrame);
        return wrap(ACCESS.create(frameType, references, stackFrame));
    }

    public static EmulatedStackFrame create(MethodType frameType) {
        var form = MethodTypeHacks.getForm(frameType);
        return wrap(ACCESS.create(frameType,
                new Object[form.referencesCount()],
                new byte[form.primitivesCount()]));
    }

    final Object esf;

    private EmulatedStackFrame(Object esf) {
        this.esf = esf;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public Object[] references() {
        return ACCESS.references(esf);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void references(Object[] references) {
        Objects.requireNonNull(references);
        ACCESS.references(esf, references);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public byte[] primitives() {
        return ACCESS.primitives(esf);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void primitives(byte[] primitives) {
        Objects.requireNonNull(primitives);
        ACCESS.primitives(esf, primitives);
    }

    public MethodType type() {
        return ACCESS.type(esf);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public void type(MethodType type) {
        Objects.requireNonNull(type);
        ACCESS.type(esf, type);
    }

    public StackFrameAccessor accessor() {
        return new StackFrameAccessor(this);
    }

    public RelativeStackFrameAccessor relativeAccessor() {
        return new RelativeStackFrameAccessor(this);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void copyArguments(StackFrameAccessor reader, int reader_start_idx,
                                     StackFrameAccessor writer, int writer_start_idx, int count) {
        Objects.checkFromIndexSize(reader_start_idx, count, reader.getArgCount());
        Objects.checkFromIndexSize(writer_start_idx, count, writer.getArgCount());

        if (count == 0) return;

        // debug check of subshorty
        assert reader.rshorty.substring(reader_start_idx, reader_start_idx + count).equals(
                writer.rshorty.substring(writer_start_idx, writer_start_idx + count));

        int reader_frame_start = reader.primitivesOffsets[reader_start_idx];
        int reader_ref_start = reader.referencesOffsets[reader_start_idx];

        int writer_frame_start = writer.primitivesOffsets[writer_start_idx];
        int writer_ref_start = writer.referencesOffsets[writer_start_idx];

        int frame_count = reader.primitivesOffsets[reader_start_idx + count] - reader_frame_start;
        int ref_count = reader.referencesOffsets[reader_start_idx + count] - reader_ref_start;

        System.arraycopy(reader.primitives, reader_frame_start,
                writer.primitives, writer_frame_start, frame_count);
        System.arraycopy(reader.references, reader_ref_start,
                writer.references, writer_ref_start, ref_count);
    }

    public static void copyNextValue(RelativeStackFrameAccessor reader,
                                     RelativeStackFrameAccessor writer) {
        switch (checkAssignable(reader.currentShorty(), writer.currentShorty())) {
            case 'L' -> writer.putNextRSLOT(reader.getNextRSLOT());
            case 'Z', 'B', 'C', 'S', 'I', 'F' -> writer.putNextSSLOT(reader.getNextSSLOT());
            case 'J', 'D' -> writer.putNextDSLOT(reader.getNextDSLOT());
            default -> throw shouldNotReachHere();
        }
    }

    public static void copyValue(StackFrameAccessor reader, int reader_idx,
                                 StackFrameAccessor writer, int writer_idx) {
        switch (checkAssignable(reader.getArgumentShorty(reader_idx),
                writer.getArgumentShorty(writer_idx))) {
            case 'V' -> { /* nop */ }
            case 'L' -> writer.putRSLOT(writer_idx, reader.getRSLOT(reader_idx));
            case 'Z', 'B', 'C', 'S', 'I', 'F' ->
                    writer.putSSLOT(writer_idx, reader.getSSLOT(reader_idx));
            case 'J', 'D' -> writer.putDSLOT(writer_idx, reader.getDSLOT(reader_idx));
            default -> throw shouldNotReachHere();
        }
    }

    @AlwaysInline
    public static void copyReturnValue(StackFrameAccessor reader, StackFrameAccessor writer) {
        copyValue(reader, RETURN_VALUE_IDX, writer, RETURN_VALUE_IDX);
    }

    public static sealed class StackFrameAccessor permits RelativeStackFrameAccessor {
        private static final long BASE = ARRAY_BYTE_BASE_OFFSET;

        final int[] primitivesOffsets;
        final int[] referencesOffsets;

        final byte[] primitives;
        final Object[] references;

        final EmulatedStackFrame frame;
        final String rshorty;
        final int argCount;

        public StackFrameAccessor(EmulatedStackFrame stackFrame) {
            frame = stackFrame;
            primitives = stackFrame.primitives();
            references = stackFrame.references();

            MethodType type = stackFrame.type();
            argCount = type.parameterCount();

            MethodTypeForm form = MethodTypeHacks.getForm(type);
            primitivesOffsets = form.primitivesOffsets();
            referencesOffsets = form.referencesOffsets();
            rshorty = form.rshorty();
        }

        @AlwaysInline
        public EmulatedStackFrame frame() {
            return frame;
        }

        @AlwaysInline
        public int getArgCount() {
            return argCount;
        }

        @AlwaysInline
        protected void checkIndex(int index) {
            if ((index >= 0 && index <= argCount) || index == RETURN_VALUE_IDX) {
                return;
            }
            throw new IllegalArgumentException("Invalid argument index: " + index);
        }

        @AlwaysInline
        protected int toArrayIndex(int index) {
            return index == RETURN_VALUE_IDX ? argCount : index;
        }

        @AlwaysInline
        private int toPrimitivesOffset(int index) {
            return primitivesOffsets[toArrayIndex(index)];
        }

        @AlwaysInline
        private int toReferencesOffset(int index) {
            return referencesOffsets[toArrayIndex(index)];
        }

        @AlwaysInline
        public char getArgumentShorty(int index) {
            checkIndex(index);
            return rshorty.charAt(index == RETURN_VALUE_IDX ? argCount : index);
        }

        @AlwaysInline
        private void checkWriteType(int index, char expected) {
            checkAssignable(getArgumentShorty(index), expected);
        }

        @AlwaysInline
        private void checkReadType(int index, char expected) {
            checkAssignable(expected, getArgumentShorty(index));
        }

        @AlwaysInline
        void putSSLOTRaw(int offset, int value) {
            AndroidUnsafe.putIntO(primitives, BASE + offset, value);
        }

        @AlwaysInline
        void putSSLOT(int index, int value) {
            putSSLOTRaw(toPrimitivesOffset(index), value);
        }

        @AlwaysInline
        void putDSLOTRaw(int offset, long value) {
            long array_offset = BASE + offset;
            if ((array_offset & 0x7) == 0) {
                AndroidUnsafe.putLongO(primitives, array_offset, value);
            } else {
                // Note: android is always little-endian
                AndroidUnsafe.putIntO(primitives, array_offset, (int) value);
                AndroidUnsafe.putIntO(primitives, array_offset + 4, (int) (value >> 32));
            }
        }

        @AlwaysInline
        void putDSLOT(int index, long value) {
            putDSLOTRaw(toPrimitivesOffset(index), value);
        }

        @AlwaysInline
        void putRSLOTRaw(int offset, Object value) {
            references[offset] = value;
        }

        @AlwaysInline
        void putRSLOT(int index, Object value) {
            putRSLOTRaw(toReferencesOffset(index), value);
        }

        @AlwaysInline
        int getSSLOTRaw(int offset) {
            return AndroidUnsafe.getIntO(primitives, BASE + offset);
        }

        @AlwaysInline
        int getSSLOT(int index) {
            return getSSLOTRaw(toPrimitivesOffset(index));
        }

        @AlwaysInline
        long getDSLOTRaw(int offset) {
            long array_offset = BASE + offset;
            if ((array_offset & 0x7) == 0) {
                return AndroidUnsafe.getLongO(primitives, array_offset);
            } else {
                // Note: android is always little-endian
                int lo = AndroidUnsafe.getIntO(primitives, array_offset);
                int hi = AndroidUnsafe.getIntO(primitives, array_offset + 4);
                return (lo & 0xffffffffL) | ((long) hi << 32);
            }
        }

        @AlwaysInline
        long getDSLOT(int index) {
            return getDSLOTRaw(toPrimitivesOffset(index));
        }

        @AlwaysInline
        @SuppressWarnings("unchecked")
        <T> T getRSLOTRaw(int offset) {
            return (T) references[offset];
        }

        @AlwaysInline
        <T> T getRSLOT(int index) {
            return getRSLOTRaw(toReferencesOffset(index));
        }

        public void setBoolean(int index, boolean value) {
            checkWriteType(index, 'Z');
            putSSLOT(index, value ? 1 : 0);
        }

        public void setByte(int index, byte value) {
            checkWriteType(index, 'B');
            putSSLOT(index, value);
        }

        public void setChar(int index, char value) {
            checkWriteType(index, 'C');
            putSSLOT(index, value);
        }

        public void setShort(int index, short value) {
            checkWriteType(index, 'S');
            putSSLOT(index, value);
        }

        public void setInt(int index, int value) {
            checkWriteType(index, 'I');
            putSSLOT(index, value);
        }

        public void setFloat(int index, float value) {
            checkWriteType(index, 'F');
            putSSLOT(index, Float.floatToRawIntBits(value));
        }

        public void setLong(int index, long value) {
            checkWriteType(index, 'J');
            putDSLOT(index, value);
        }

        public void setDouble(int index, double value) {
            checkWriteType(index, 'D');
            putDSLOT(index, Double.doubleToRawLongBits(value));
        }

        public void setReference(int index, Object value) {
            checkWriteType(index, 'L');
            putRSLOT(index, value);
        }

        public void setValue(int index, Object value) {
            switch (getArgumentShorty(index)) {
                case 'V' -> { /* nop */ }
                case 'L' -> putRSLOT(index, value);
                case 'Z' -> putSSLOT(index, (boolean) value ? 1 : 0);
                case 'B' -> putSSLOT(index, (byte) value);
                case 'C' -> putSSLOT(index, (char) value);
                case 'S' -> putSSLOT(index, (short) value);
                case 'I' -> putSSLOT(index, (int) value);
                case 'F' -> putSSLOT(index, Float.floatToRawIntBits((float) value));
                case 'J' -> putDSLOT(index, (long) value);
                case 'D' -> putDSLOT(index, Double.doubleToRawLongBits((double) value));
                default -> throw shouldNotReachHere();
            }
        }

        public boolean getBoolean(int index) {
            checkReadType(index, 'Z');
            return getSSLOT(index) != 0;
        }

        public byte getByte(int index) {
            checkReadType(index, 'B');
            return (byte) getSSLOT(index);
        }

        public char getChar(int index) {
            checkReadType(index, 'C');
            return (char) getSSLOT(index);
        }

        public short getShort(int index) {
            checkReadType(index, 'S');
            return (short) getSSLOT(index);
        }

        public int getInt(int index) {
            checkReadType(index, 'I');
            return getSSLOT(index);
        }

        public float getFloat(int index) {
            checkReadType(index, 'F');
            return Float.intBitsToFloat(getSSLOT(index));
        }

        public long getLong(int index) {
            checkReadType(index, 'J');
            return getDSLOT(index);
        }

        public double getDouble(int index) {
            checkReadType(index, 'D');
            return Double.longBitsToDouble(getDSLOT(index));
        }

        public <T> T getReference(int index) {
            checkReadType(index, 'L');
            return getRSLOT(index);
        }

        public Object getValue(int index) {
            return switch (getArgumentShorty(index)) {
                case 'V' -> null;
                case 'L' -> getRSLOT(index);
                case 'Z' -> getSSLOT(index) != 0;
                case 'B' -> (byte) getSSLOT(index);
                case 'C' -> (char) getSSLOT(index);
                case 'S' -> (short) getSSLOT(index);
                case 'I' -> getSSLOT(index);
                case 'F' -> Float.intBitsToFloat(getSSLOT(index));
                case 'J' -> getDSLOT(index);
                case 'D' -> Double.longBitsToDouble(getDSLOT(index));
                default -> throw shouldNotReachHere();
            };
        }
    }

    public static final class RelativeStackFrameAccessor extends StackFrameAccessor {
        private int currentPrimitive;
        private int currentReference;
        private int argumentIdx;

        public RelativeStackFrameAccessor(EmulatedStackFrame stackFrame) {
            super(stackFrame);
            currentPrimitive = 0;
            currentReference = 0;
            argumentIdx = 0;
        }

        @AlwaysInline
        public int currentArgument() {
            return argumentIdx;
        }

        @AlwaysInline
        public char currentShorty() {
            return getArgumentShorty(argumentIdx);
        }

        @AlwaysInline
        private void checkWriteType(char expected) {
            checkAssignable(currentShorty(), expected);
        }

        @AlwaysInline
        private void checkReadType(char expected) {
            checkAssignable(expected, currentShorty());
        }

        public RelativeStackFrameAccessor moveTo(int index) {
            checkIndex(index);
            int array_index = toArrayIndex(index);
            currentReference = referencesOffsets[array_index];
            currentPrimitive = primitivesOffsets[array_index];
            argumentIdx = index;
            return this;
        }

        @AlwaysInline
        public RelativeStackFrameAccessor moveToReturn() {
            return moveTo(RETURN_VALUE_IDX);
        }

        @AlwaysInline
        private void putNextSSLOT(int value) {
            putSSLOTRaw(currentPrimitive, value);
            argumentIdx++;
            currentPrimitive += SREG;
        }

        @AlwaysInline
        private void putNextDSLOT(long value) {
            putDSLOTRaw(currentPrimitive, value);
            argumentIdx++;
            currentPrimitive += DREG;
        }

        @AlwaysInline
        private void putNextRSLOT(Object value) {
            putRSLOTRaw(currentReference, value);
            argumentIdx++;
            currentReference++;
        }

        @AlwaysInline
        private int getNextSSLOT() {
            int value = getSSLOTRaw(currentPrimitive);
            argumentIdx++;
            currentPrimitive += SREG;
            return value;
        }

        @AlwaysInline
        private long getNextDSLOT() {
            long value = getDSLOTRaw(currentPrimitive);
            argumentIdx++;
            currentPrimitive += DREG;
            return value;
        }

        @AlwaysInline
        private <T> T getNextRSLOT() {
            T value = getRSLOTRaw(currentReference);
            argumentIdx++;
            currentReference++;
            return value;
        }

        public void putNextBoolean(boolean value) {
            checkWriteType('Z');
            putNextSSLOT(value ? 1 : 0);
        }

        public void putNextByte(byte value) {
            checkWriteType('B');
            putNextSSLOT(value);
        }

        public void putNextChar(char value) {
            checkWriteType('C');
            putNextSSLOT(value);
        }

        public void putNextShort(short value) {
            checkWriteType('S');
            putNextSSLOT(value);
        }

        public void putNextInt(int value) {
            checkWriteType('I');
            putNextSSLOT(value);
        }

        public void putNextFloat(float value) {
            checkWriteType('F');
            putNextSSLOT(Float.floatToRawIntBits(value));
        }

        public void putNextLong(long value) {
            checkWriteType('J');
            putNextDSLOT(value);
        }

        public void putNextDouble(double value) {
            checkWriteType('D');
            putNextDSLOT(Double.doubleToRawLongBits(value));
        }

        public void putNextReference(Object value) {
            checkWriteType('L');
            putNextRSLOT(value);
        }

        public void putNextValue(Object value) {
            switch (currentShorty()) {
                case 'V' -> argumentIdx++;
                case 'L' -> putNextRSLOT(value);
                case 'Z' -> putNextSSLOT((boolean) value ? 1 : 0);
                case 'B' -> putNextSSLOT((byte) value);
                case 'C' -> putNextSSLOT((char) value);
                case 'S' -> putNextSSLOT((short) value);
                case 'I' -> putNextSSLOT((int) value);
                case 'F' -> putNextSSLOT(Float.floatToRawIntBits((float) value));
                case 'J' -> putNextDSLOT((long) value);
                case 'D' -> putNextDSLOT(Double.doubleToRawLongBits((double) value));
                default -> throw shouldNotReachHere();
            }
        }

        public boolean nextBoolean() {
            checkReadType('Z');
            return getNextSSLOT() != 0;
        }

        public byte nextByte() {
            checkReadType('B');
            return (byte) getNextSSLOT();
        }

        public char nextChar() {
            checkReadType('C');
            return (char) getNextSSLOT();
        }

        public short nextShort() {
            checkReadType('S');
            return (short) getNextSSLOT();
        }

        public int nextInt() {
            checkReadType('I');
            return getNextSSLOT();
        }

        public float nextFloat() {
            checkReadType('F');
            return Float.intBitsToFloat(getNextSSLOT());
        }

        public long nextLong() {
            checkReadType('J');
            return getNextDSLOT();
        }

        public double nextDouble() {
            checkReadType('D');
            return Double.longBitsToDouble(getNextDSLOT());
        }

        public <T> T nextReference() {
            checkReadType('L');
            return getNextRSLOT();
        }

        public Object nextValue() {
            return switch (currentShorty()) {
                case 'V' -> {
                    argumentIdx++;
                    yield null;
                }
                case 'L' -> getNextRSLOT();
                case 'Z' -> getNextSSLOT() != 0;
                case 'B' -> (byte) getNextSSLOT();
                case 'C' -> (char) getNextSSLOT();
                case 'S' -> (short) getNextSSLOT();
                case 'I' -> getNextSSLOT();
                case 'F' -> Float.intBitsToFloat(getNextSSLOT());
                case 'J' -> getNextDSLOT();
                case 'D' -> Double.longBitsToDouble(getNextDSLOT());
                default -> throw shouldNotReachHere();
            };
        }
    }

    @Override
    public String toString() {
        return "EmulatedStackFrame{type=" + type() +
                ", primitives=" + Arrays.toString(primitives()) +
                ", references=" + Arrays.toString(references()) + "}";
    }
}

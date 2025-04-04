package com.v7878.unsafe.invoke;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getHiddenInstanceFields;
import static com.v7878.unsafe.Utils.assertEq;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ArtFieldUtils;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;

import dalvik.system.DexFile;

public final class EmulatedStackFrame {
    static final Class<?> ESF_CLASS = nothrows_run(() ->
            Class.forName("dalvik.system.EmulatedStackFrame"));
    private static final AccessI ACCESS;

    @DoNotShrink
    @DoNotObfuscate
    private abstract static class AccessI {
        abstract Object create(MethodType type, Object[] references, byte[] stackFrame);

        abstract MethodType type(Object frame);

        abstract void type(Object frame, MethodType type);

        abstract Object[] references(Object frame);

        abstract void references(Object frame, Object[] references);

        abstract byte[] primitives(Object frame);

        abstract void primitives(Object frame, byte[] primitives);
    }

    static {
        for (var field : getHiddenInstanceFields(ESF_CLASS)) {
            ArtFieldUtils.makeFieldPublic(field);
            ArtFieldUtils.makeFieldNonFinal(field);
        }

        TypeId mt = TypeId.of(MethodType.class);
        TypeId esf = TypeId.of(ESF_CLASS);

        String access_name = EmulatedStackFrame.class.getName() + "$Access";
        TypeId access_id = TypeId.ofName(access_name);

        var type_id = FieldId.of(esf, "type", mt);
        var callsite_id = FieldId.of(esf, "callsiteType", mt);
        var references_id = FieldId.of(esf, "references", TypeId.OBJECT.array());
        var primitives_id = FieldId.of(esf, "stackFrame", TypeId.B.array());

        ClassDef access_def = ClassBuilder.build(access_id, cb -> cb
                .withSuperClass(TypeId.of(AccessI.class))
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("create")
                        .withReturnType(TypeId.OBJECT)
                        .withParameterTypes(mt, TypeId.OBJECT.array(), TypeId.B.array())
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
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("type")
                        .withReturnType(mt)
                        .withParameterTypes(TypeId.OBJECT)
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(GET_OBJECT, ib.this_(), ib.p(0), type_id);
                            ib.return_object(ib.this_());
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("type")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(TypeId.OBJECT, mt)
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(PUT_OBJECT, ib.p(1), ib.p(0), type_id);
                            ib.return_void();
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("references")
                        .withReturnType(TypeId.OBJECT.array())
                        .withParameterTypes(TypeId.OBJECT)
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(GET_OBJECT, ib.this_(), ib.p(0), references_id);
                            ib.return_object(ib.this_());
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("references")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(TypeId.OBJECT, TypeId.OBJECT.array())
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(PUT_OBJECT, ib.p(1), ib.p(0), references_id);
                            ib.return_void();
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("primitives")
                        .withReturnType(TypeId.B.array())
                        .withParameterTypes(TypeId.OBJECT)
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(GET_OBJECT, ib.this_(), ib.p(0), primitives_id);
                            ib.return_object(ib.this_());
                        })
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("primitives")
                        .withReturnType(TypeId.V)
                        .withParameterTypes(TypeId.OBJECT, TypeId.B.array())
                        .withCode(0, ib -> {
                            ib.generate_lines();
                            ib.iop(PUT_OBJECT, ib.p(1), ib.p(0), primitives_id);
                            ib.return_void();
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
    private static void checkAssignable(Class<?> expected, Class<?> actual) {
        if (expected == actual) {
            return;
        }
        throw new IllegalArgumentException(
                String.format("Incorrect type: %s, expected: %s", actual, expected));
    }

    @DoNotShrink
    @DoNotObfuscate
    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static EmulatedStackFrame wrap(Object esf) {
        assert ESF_CLASS.isAssignableFrom(esf.getClass());
        return new EmulatedStackFrame(esf);
    }

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
    // TODO: simplify
    public static void copyArguments(StackFrameAccessor reader, int reader_start_idx,
                                     StackFrameAccessor writer, int writer_start_idx, int count) {
        Objects.checkFromIndexSize(reader_start_idx, count, reader.ptypes.length);
        Objects.checkFromIndexSize(writer_start_idx, count, writer.ptypes.length);

        if (count == 0) return;

        int reader_frame_start = reader.primitivesOffsets[reader_start_idx];
        int reader_ref_start = reader.referencesOffsets[reader_start_idx];

        int writer_frame_start = writer.primitivesOffsets[writer_start_idx];
        int writer_ref_start = writer.referencesOffsets[writer_start_idx];

        int frame_count = assertEq(reader.primitivesOffsets[reader_start_idx + count] - reader_frame_start,
                writer.primitivesOffsets[writer_start_idx + count] - writer_frame_start);
        int ref_count = assertEq(reader.referencesOffsets[reader_start_idx + count] - reader_ref_start,
                writer.referencesOffsets[writer_start_idx + count] - writer_ref_start);

        System.arraycopy(reader.frame.primitives(), reader_frame_start,
                writer.frame.primitives(), writer_frame_start, frame_count);
        System.arraycopy(reader.frame.references(), reader_ref_start,
                writer.frame.references(), writer_ref_start, ref_count);
    }

    public static void copyReturnValue(EmulatedStackFrame src, EmulatedStackFrame dst) {
        final Class<?> returnType = src.type().erase().returnType();
        checkAssignable(dst.type().erase().returnType(), returnType);
        if (returnType.isPrimitive()) {
            byte[] this_primitives = src.primitives();
            byte[] other_primitives = dst.primitives();
            int size = getSize(returnType);
            System.arraycopy(this_primitives, this_primitives.length - size,
                    other_primitives, other_primitives.length - size, size);
        } else {
            Object[] this_references = src.references();
            Object[] other_references = dst.references();
            other_references[other_references.length - 1]
                    = this_references[this_references.length - 1];
        }
    }

    public static void copyNextValue(RelativeStackFrameAccessor reader,
                                     RelativeStackFrameAccessor writer, Class<?> type) {
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

    public static void copyValue(StackFrameAccessor reader, int reader_idx,
                                 StackFrameAccessor writer, int writer_idx, Class<?> type) {
        switch (Wrapper.basicTypeChar(type)) {
            case 'L' -> writer.setReference(writer_idx, reader.getReference(reader_idx));
            case 'Z' -> writer.setBoolean(writer_idx, reader.getBoolean(reader_idx));
            case 'B' -> writer.setByte(writer_idx, reader.getByte(reader_idx));
            case 'C' -> writer.setChar(writer_idx, reader.getChar(reader_idx));
            case 'S' -> writer.setShort(writer_idx, reader.getShort(reader_idx));
            case 'I' -> writer.setInt(writer_idx, reader.getInt(reader_idx));
            case 'J' -> writer.setLong(writer_idx, reader.getLong(reader_idx));
            case 'F' -> writer.setFloat(writer_idx, reader.getFloat(reader_idx));
            case 'D' -> writer.setDouble(writer_idx, reader.getDouble(reader_idx));
            default -> throw shouldNotReachHere();
        }
    }

    public static sealed class StackFrameAccessor permits RelativeStackFrameAccessor {
        private static final long BASE = ARRAY_BYTE_BASE_OFFSET;

        final int[] primitivesOffsets;
        final int[] referencesOffsets;

        final byte[] primitives;
        final Object[] references;

        final EmulatedStackFrame frame;
        final Class<?>[] ptypes;
        final Class<?> rtype;

        public StackFrameAccessor(EmulatedStackFrame stackFrame) {
            frame = stackFrame;
            primitives = stackFrame.primitives();
            references = stackFrame.references();

            MethodType type = stackFrame.type().erase();
            rtype = InvokeAccess.rtype(type);
            ptypes = InvokeAccess.ptypes(type);

            MethodTypeForm form = MethodTypeHacks.getForm(type);
            primitivesOffsets = form.primitivesOffsets();
            referencesOffsets = form.referencesOffsets();
        }

        @AlwaysInline
        public EmulatedStackFrame frame() {
            return frame;
        }

        @AlwaysInline
        protected void checkIndex(int index) {
            if ((index < 0 || index >= ptypes.length) && (index != RETURN_VALUE_IDX)) {
                throw new IllegalArgumentException("Invalid argument index: " + index);
            }
        }

        @AlwaysInline
        protected int toArrayIndex(int index) {
            return index == RETURN_VALUE_IDX ? ptypes.length : index;
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
        public Class<?> getArgumentType(int index) {
            checkIndex(index);
            return (index == RETURN_VALUE_IDX) ? rtype : ptypes[index];
        }

        @AlwaysInline
        private void checkWriteType(int index, Class<?> expected) {
            checkAssignable(getArgumentType(index), expected);
        }

        @AlwaysInline
        private void checkReadType(int index, Class<?> expected) {
            checkAssignable(expected, getArgumentType(index));
        }

        @AlwaysInline
        protected void putSSLOT(int offset, int value) {
            AndroidUnsafe.putIntO(primitives, BASE + offset, value);
        }

        @AlwaysInline
        protected void putDSLOT(int offset, long value) {
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
        protected void putRef(int index, Object value) {
            references[index] = value;
        }

        @AlwaysInline
        protected int getSSLOT(int offset) {
            return AndroidUnsafe.getIntO(primitives, BASE + offset);
        }

        @AlwaysInline
        protected long getDSLOT(int offset) {
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
        @SuppressWarnings("unchecked")
        protected <T> T getRef(int index) {
            return (T) references[index];
        }

        public void setBoolean(int index, boolean value) {
            checkWriteType(index, boolean.class);
            putSSLOT(toPrimitivesOffset(index), value ? 1 : 0);
        }

        public void setByte(int index, byte value) {
            checkWriteType(index, byte.class);
            putSSLOT(toPrimitivesOffset(index), value);
        }

        public void setChar(int index, char value) {
            checkWriteType(index, char.class);
            putSSLOT(toPrimitivesOffset(index), value);
        }

        public void setShort(int index, short value) {
            checkWriteType(index, short.class);
            putSSLOT(toPrimitivesOffset(index), value);
        }

        public void setInt(int index, int value) {
            checkWriteType(index, int.class);
            putSSLOT(toPrimitivesOffset(index), value);
        }

        public void setFloat(int index, float value) {
            checkWriteType(index, float.class);
            putSSLOT(toPrimitivesOffset(index), Float.floatToRawIntBits(value));
        }

        public void setLong(int index, long value) {
            checkWriteType(index, long.class);
            putDSLOT(toPrimitivesOffset(index), value);
        }

        public void setDouble(int index, double value) {
            checkWriteType(index, double.class);
            putDSLOT(toPrimitivesOffset(index), Double.doubleToRawLongBits(value));
        }

        public void setReference(int index, Object value) {
            checkWriteType(index, Object.class);
            putRef(toReferencesOffset(index), value);
        }

        public void setValue(int index, Object value) {
            switch (Wrapper.basicTypeChar(getArgumentType(index))) {
                case 'V' -> { /* nop */ }
                case 'L' -> putRef(toReferencesOffset(index), value);
                case 'Z' -> putSSLOT(toPrimitivesOffset(index), (boolean) value ? 1 : 0);
                case 'B' -> putSSLOT(toPrimitivesOffset(index), (byte) value);
                case 'C' -> putSSLOT(toPrimitivesOffset(index), (char) value);
                case 'S' -> putSSLOT(toPrimitivesOffset(index), (short) value);
                case 'I' -> putSSLOT(toPrimitivesOffset(index), (int) value);
                case 'F' -> putSSLOT(toPrimitivesOffset(index),
                        Float.floatToRawIntBits((float) value));
                case 'J' -> putDSLOT(toPrimitivesOffset(index), (long) value);
                case 'D' -> putDSLOT(toPrimitivesOffset(index),
                        Double.doubleToRawLongBits((double) value));
                default -> throw shouldNotReachHere();
            }
        }

        public boolean getBoolean(int index) {
            checkReadType(index, boolean.class);
            return getSSLOT(toPrimitivesOffset(index)) != 0;
        }

        public byte getByte(int index) {
            checkReadType(index, byte.class);
            return (byte) getSSLOT(toPrimitivesOffset(index));
        }

        public char getChar(int index) {
            checkReadType(index, char.class);
            return (char) getSSLOT(toPrimitivesOffset(index));
        }

        public short getShort(int index) {
            checkReadType(index, short.class);
            return (short) getSSLOT(toPrimitivesOffset(index));
        }

        public int getInt(int index) {
            checkReadType(index, int.class);
            return getSSLOT(toPrimitivesOffset(index));
        }

        public float getFloat(int index) {
            checkReadType(index, float.class);
            return Float.intBitsToFloat(getSSLOT(toPrimitivesOffset(index)));
        }

        public long getLong(int index) {
            checkReadType(index, long.class);
            return getDSLOT(toPrimitivesOffset(index));
        }

        public double getDouble(int index) {
            checkReadType(index, double.class);
            return Double.longBitsToDouble(getDSLOT(toPrimitivesOffset(index)));
        }

        public <T> T getReference(int index) {
            checkReadType(index, Object.class);
            return getRef(toReferencesOffset(index));
        }

        public Object getValue(int index) {
            return switch (Wrapper.basicTypeChar(getArgumentType(index))) {
                case 'V' -> null;
                case 'L' -> getRef(toReferencesOffset(index));
                case 'Z' -> getSSLOT(toPrimitivesOffset(index)) != 0;
                case 'B' -> (byte) getSSLOT(toPrimitivesOffset(index));
                case 'C' -> (char) getSSLOT(toPrimitivesOffset(index));
                case 'S' -> (short) getSSLOT(toPrimitivesOffset(index));
                case 'I' -> getSSLOT(toPrimitivesOffset(index));
                case 'F' -> Float.intBitsToFloat(getSSLOT(toPrimitivesOffset(index)));
                case 'J' -> getDSLOT(toPrimitivesOffset(index));
                case 'D' -> Double.longBitsToDouble(getDSLOT(toPrimitivesOffset(index)));
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
        private void checkWriteType(Class<?> expected) {
            checkAssignable(getArgumentType(argumentIdx), expected);
        }

        @AlwaysInline
        private void checkReadType(Class<?> expected) {
            checkAssignable(expected, getArgumentType(argumentIdx));
        }

        public RelativeStackFrameAccessor moveTo(int index) {
            checkIndex(index);
            int array_index = toArrayIndex(index);
            currentReference = referencesOffsets[array_index];
            currentPrimitive = primitivesOffsets[array_index];
            argumentIdx = index;
            return this;
        }

        public RelativeStackFrameAccessor moveToReturn() {
            return moveTo(RETURN_VALUE_IDX);
        }

        @AlwaysInline
        private void putNextSSLOT(int value) {
            putSSLOT(currentPrimitive, value);
            argumentIdx++;
            currentPrimitive += SREG;
        }

        @AlwaysInline
        private void putNextDSLOT(long value) {
            putDSLOT(currentPrimitive, value);
            argumentIdx++;
            currentPrimitive += DREG;
        }

        @AlwaysInline
        private void putNextRef(Object value) {
            putRef(currentReference, value);
            argumentIdx++;
            currentReference++;
        }

        @AlwaysInline
        private int getNextSSLOT() {
            int value = getSSLOT(currentPrimitive);
            argumentIdx++;
            currentPrimitive += SREG;
            return value;
        }

        @AlwaysInline
        private long getNextDSLOT() {
            long value = getDSLOT(currentPrimitive);
            argumentIdx++;
            currentPrimitive += DREG;
            return value;
        }

        @AlwaysInline
        private <T> T getNextRef() {
            T value = getRef(currentReference);
            argumentIdx++;
            currentReference++;
            return value;
        }

        public void putNextBoolean(boolean value) {
            checkWriteType(boolean.class);
            putNextSSLOT(value ? 1 : 0);
        }

        public void putNextByte(byte value) {
            checkWriteType(byte.class);
            putNextSSLOT(value);
        }

        public void putNextChar(char value) {
            checkWriteType(char.class);
            putNextSSLOT(value);
        }

        public void putNextShort(short value) {
            checkWriteType(short.class);
            putNextSSLOT(value);
        }

        public void putNextInt(int value) {
            checkWriteType(int.class);
            putNextSSLOT(value);
        }

        public void putNextFloat(float value) {
            checkWriteType(float.class);
            putNextSSLOT(Float.floatToRawIntBits(value));
        }

        public void putNextLong(long value) {
            checkWriteType(long.class);
            putNextDSLOT(value);
        }

        public void putNextDouble(double value) {
            checkWriteType(double.class);
            putNextDSLOT(Double.doubleToRawLongBits(value));
        }

        public void putNextReference(Object value) {
            checkWriteType(Object.class);
            putNextRef(value);
        }

        public void putNextValue(Object value) {
            switch (Wrapper.basicTypeChar(getArgumentType(argumentIdx))) {
                case 'V' -> argumentIdx++;
                case 'L' -> putNextRef(value);
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
            checkReadType(boolean.class);
            return getNextSSLOT() != 0;
        }

        public byte nextByte() {
            checkReadType(byte.class);
            return (byte) getNextSSLOT();
        }

        public char nextChar() {
            checkReadType(char.class);
            return (char) getNextSSLOT();
        }

        public short nextShort() {
            checkReadType(short.class);
            return (short) getNextSSLOT();
        }

        public int nextInt() {
            checkReadType(int.class);
            return getNextSSLOT();
        }

        public float nextFloat() {
            checkReadType(float.class);
            return Float.intBitsToFloat(getNextSSLOT());
        }

        public long nextLong() {
            checkReadType(long.class);
            return getNextDSLOT();
        }

        public double nextDouble() {
            checkReadType(double.class);
            return Double.longBitsToDouble(getNextDSLOT());
        }

        public <T> T nextReference() {
            checkReadType(Object.class);
            return getNextRef();
        }

        public Object nextValue() {
            return switch (Wrapper.basicTypeChar(getArgumentType(argumentIdx))) {
                case 'V' -> {
                    argumentIdx++;
                    yield null;
                }
                case 'L' -> getNextRef();
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

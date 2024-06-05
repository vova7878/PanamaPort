package com.v7878.foreign;

import static com.v7878.misc.Math.convEndian;
import static com.v7878.misc.Math.d2l;
import static com.v7878.misc.Math.f2i;
import static com.v7878.misc.Math.i2f;
import static com.v7878.misc.Math.l2d;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.invoke.VarHandleImpl.isReadOnly;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.VarHandleImpl;
import com.v7878.unsafe.invoke.VarHandleImpl.VarHandleTransformer;
import com.v7878.unsafe.invoke.Wrapper;

import java.util.Objects;

abstract sealed class _VarHandleSegmentViewBase implements VarHandleTransformer {

    /**
     * Creates a memory segment view var handle.
     * <p>
     * The resulting var handle will take a memory segment as first argument (the segment to be dereferenced),
     * and a {@code long} as second argument (the offset into the segment).
     * <p>
     * Note: the returned var handle does not perform any size or alignment check. It is up to clients
     * to adapt the returned var handle and insert the appropriate checks.
     *
     * @param carrier       the Java carrier type.
     * @param alignmentMask alignment requirement to be checked upon access. In bytes. Expressed as a mask.
     * @return the created VarHandle.
     */
    public static VarHandle rawMemorySegmentViewHandle(Class<?> carrier, long alignmentMask, boolean swap) {
        if (!carrier.isPrimitive() || carrier == void.class || carrier == boolean.class) {
            throw new IllegalArgumentException("Invalid carrier: " + carrier.getName());
        }

        _VarHandleSegmentViewBase transformer;
        if (carrier == byte.class) {
            transformer = new VarHandleSegmentAsBytes(swap);
        } else if (carrier == short.class) {
            transformer = new VarHandleSegmentAsShorts(swap);
        } else if (carrier == char.class) {
            transformer = new VarHandleSegmentAsChars(swap);
        } else if (carrier == int.class) {
            transformer = new VarHandleSegmentAsInts(swap);
        } else if (carrier == float.class) {
            transformer = new VarHandleSegmentAsFloats(swap);
        } else if (carrier == long.class) {
            transformer = new VarHandleSegmentAsLongs(swap);
        } else if (carrier == double.class) {
            transformer = new VarHandleSegmentAsDoubles(swap);
        } else {
            throw shouldNotReachHere();
        }

        long min_align_mask = Wrapper.forPrimitiveType(carrier).byteWidth() - 1;
        boolean allowAtomicAccess = (alignmentMask & min_align_mask) == min_align_mask;
        int modesMask = VarHandleImpl.accessModesBitMask(carrier, allowAtomicAccess);
        return VarHandleImpl.newVarHandle(modesMask, transformer,
                carrier, MemorySegment.class, long.class);
    }

    /**
     * endianness
     **/
    final boolean swap;

    _VarHandleSegmentViewBase(boolean swap) {
        this.swap = swap;
    }

    void checkAddress(_AbstractMemorySegmentImpl ms, boolean ro) {
        Objects.requireNonNull(ms).checkReadOnly(ro);
    }

    long getOffset(_AbstractMemorySegmentImpl bb, long offset) {
        long base = bb.unsafeGetOffset();
        return base + offset;
    }

    private static final class VarHandleSegmentAsBytes extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsBytes(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextByte(
                        _ScopedMemoryAccess.getByte(session, base, offset));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextByte(
                        _ScopedMemoryAccess.getByteVolatile(session, base, offset));
                case SET -> _ScopedMemoryAccess.putByte(session, base, offset, accessor.nextByte());
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putByteVolatile(
                        session, base, offset, accessor.nextByte());
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetByte(session,
                            base, offset, accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeByte(session, base,
                            offset, accessor.nextByte(), accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetByte(session, base,
                            offset, accessor.nextByte(), accessor.nextByte());
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_AND_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseAndByte(session,
                            base, offset, accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE, GET_AND_BITWISE_OR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseOrByte(session,
                            base, offset, accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE, GET_AND_BITWISE_XOR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseXorByte(session,
                            base, offset, accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddByteWithCAS(session,
                            base, offset, accessor.nextByte());
                    accessor.moveToReturn().putNextByte(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsShorts extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsShorts(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextShort(
                        _ScopedMemoryAccess.getShortUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextShort(
                        convEndian(_ScopedMemoryAccess.getShortVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putShortUnaligned(
                        session, base, offset, accessor.nextShort(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putShortVolatile(
                        session, base, offset, convEndian(accessor.nextShort(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetShort(session, base,
                            offset, convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextShort(convEndian(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeShort(session, base, offset,
                            convEndian(accessor.nextShort(), swap), convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextShort(convEndian(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetShort(session, base, offset,
                            convEndian(accessor.nextShort(), swap), convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_AND_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseAndShort(session, base,
                            offset, convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextShort(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE, GET_AND_BITWISE_OR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseOrShort(session, base,
                            offset, convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextShort(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE, GET_AND_BITWISE_XOR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseXorShort(session, base,
                            offset, convEndian(accessor.nextShort(), swap));
                    accessor.moveToReturn().putNextShort(convEndian(tmp, swap));
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddShortWithCAS(session,
                            base, offset, accessor.nextShort(), swap);
                    accessor.moveToReturn().putNextShort(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsChars extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsChars(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextChar(
                        _ScopedMemoryAccess.getCharUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextChar(
                        (char) convEndian(_ScopedMemoryAccess.getShortVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putCharUnaligned(
                        session, base, offset, accessor.nextChar(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putShortVolatile(
                        session, base, offset, convEndian((short) accessor.nextChar(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetShort(session, base,
                            offset, convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextChar((char) convEndian(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeShort(session, base, offset,
                            convEndian((short) accessor.nextChar(), swap),
                            convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextChar((char) convEndian(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetShort(session, base, offset,
                            convEndian((short) accessor.nextChar(), swap),
                            convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_AND_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseAndShort(session, base,
                            offset, convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextChar((char) convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE, GET_AND_BITWISE_OR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseOrShort(session, base,
                            offset, convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextChar((char) convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE, GET_AND_BITWISE_XOR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseXorShort(session, base,
                            offset, convEndian((short) accessor.nextChar(), swap));
                    accessor.moveToReturn().putNextChar((char) convEndian(tmp, swap));
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddShortWithCAS(session,
                            base, offset, (short) accessor.nextChar(), swap);
                    accessor.moveToReturn().putNextChar((char) tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsInts extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsInts(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextInt(
                        _ScopedMemoryAccess.getIntUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextInt(
                        convEndian(_ScopedMemoryAccess.getIntVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putIntUnaligned(
                        session, base, offset, accessor.nextInt(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putIntVolatile(
                        session, base, offset, convEndian(accessor.nextInt(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetInt(session, base,
                            offset, convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextInt(convEndian(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeInt(session, base, offset,
                            convEndian(accessor.nextInt(), swap), convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextInt(convEndian(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetInt(session, base, offset,
                            convEndian(accessor.nextInt(), swap), convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_AND_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseAndInt(session, base,
                            offset, convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextInt(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE, GET_AND_BITWISE_OR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseOrInt(session, base,
                            offset, convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextInt(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE, GET_AND_BITWISE_XOR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseXorInt(session, base,
                            offset, convEndian(accessor.nextInt(), swap));
                    accessor.moveToReturn().putNextInt(convEndian(tmp, swap));
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddIntWithCAS(session,
                            base, offset, accessor.nextInt(), swap);
                    accessor.moveToReturn().putNextInt(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsFloats extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsFloats(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextFloat(
                        _ScopedMemoryAccess.getFloatUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextFloat(
                        i2f(_ScopedMemoryAccess.getIntVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putFloatUnaligned(
                        session, base, offset, accessor.nextFloat(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putIntVolatile(
                        session, base, offset, f2i(accessor.nextFloat(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetInt(session, base,
                            offset, f2i(accessor.nextFloat(), swap));
                    accessor.moveToReturn().putNextFloat(i2f(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeInt(session, base, offset,
                            f2i(accessor.nextFloat(), swap), f2i(accessor.nextFloat(), swap));
                    accessor.moveToReturn().putNextFloat(i2f(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetInt(session, base, offset,
                            f2i(accessor.nextFloat(), swap), f2i(accessor.nextFloat(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddFloatWithCAS(session,
                            base, offset, accessor.nextFloat(), swap);
                    accessor.moveToReturn().putNextFloat(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsLongs extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsLongs(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextLong(
                        _ScopedMemoryAccess.getLongUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextLong(
                        convEndian(_ScopedMemoryAccess.getLongVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putLongUnaligned(
                        session, base, offset, accessor.nextLong(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putLongVolatile(
                        session, base, offset, convEndian(accessor.nextLong(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetLong(session, base,
                            offset, convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextLong(convEndian(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeLong(session, base, offset,
                            convEndian(accessor.nextLong(), swap), convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextLong(convEndian(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetLong(session, base, offset,
                            convEndian(accessor.nextLong(), swap), convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_AND_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseAndLong(session, base,
                            offset, convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextLong(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE, GET_AND_BITWISE_OR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseOrLong(session, base,
                            offset, convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextLong(convEndian(tmp, swap));
                }
                case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE, GET_AND_BITWISE_XOR_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndBitwiseXorLong(session, base,
                            offset, convEndian(accessor.nextLong(), swap));
                    accessor.moveToReturn().putNextLong(convEndian(tmp, swap));
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddLongWithCAS(session,
                            base, offset, accessor.nextLong(), swap);
                    accessor.moveToReturn().putNextLong(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }

    private static final class VarHandleSegmentAsDoubles extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsDoubles(boolean swap) {
            super(swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextDouble(
                        _ScopedMemoryAccess.getDoubleUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextDouble(
                        l2d(_ScopedMemoryAccess.getLongVolatile(session, base, offset), swap));
                case SET -> _ScopedMemoryAccess.putDoubleUnaligned(
                        session, base, offset, accessor.nextDouble(), swap);
                case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> _ScopedMemoryAccess.putLongVolatile(
                        session, base, offset, d2l(accessor.nextDouble(), swap));
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.getAndSetLong(session, base,
                            offset, d2l(accessor.nextDouble(), swap));
                    accessor.moveToReturn().putNextDouble(l2d(tmp, swap));
                }
                case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE, COMPARE_AND_EXCHANGE_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndExchangeLong(session, base, offset,
                            d2l(accessor.nextDouble(), swap), d2l(accessor.nextDouble(), swap));
                    accessor.moveToReturn().putNextDouble(l2d(tmp, swap));
                }
                case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                        WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> {
                    var tmp = _ScopedMemoryAccess.compareAndSetLong(session, base, offset,
                            d2l(accessor.nextDouble(), swap), d2l(accessor.nextDouble(), swap));
                    accessor.moveToReturn().putNextBoolean(tmp);
                }
                case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> {
                    var tmp = _ScopedMemoryAccess.getAndAddDoubleWithCAS(session,
                            base, offset, accessor.nextDouble(), swap);
                    accessor.moveToReturn().putNextDouble(tmp);
                }
                default -> shouldNotReachHere();
            }
        }
    }
}

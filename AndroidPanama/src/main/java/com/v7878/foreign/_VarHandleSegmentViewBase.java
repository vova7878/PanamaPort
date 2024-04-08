package com.v7878.foreign;

import static com.v7878.misc.Math.convEndian;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.invoke.VarHandleImpl.isReadOnly;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.VarHandleImpl;
import com.v7878.unsafe.invoke.VarHandleImpl.VarHandleTransformer;

import java.util.Objects;

abstract sealed class _VarHandleSegmentViewBase implements VarHandleTransformer {

    public static VarHandle makeRawSegmentViewVarHandle(Class<?> carrier, long alignmentMask, boolean swap) {
        if (!carrier.isPrimitive() || carrier == void.class || carrier == boolean.class) {
            throw new IllegalArgumentException("Invalid carrier: " + carrier.getName());
        }

        _VarHandleSegmentViewBase transformer;
        if (carrier == byte.class) {
            transformer = new VarHandleSegmentAsBytes(swap, alignmentMask);
        } else if (carrier == short.class) {
            transformer = new VarHandleSegmentAsShorts(swap, alignmentMask);
        } else if (carrier == char.class) {
            transformer = new VarHandleSegmentAsChars(swap, alignmentMask);
        } else if (carrier == int.class) {
            transformer = new VarHandleSegmentAsInts(swap, alignmentMask);
        } else if (carrier == float.class) {
            transformer = new VarHandleSegmentAsFloats(swap, alignmentMask);
        } else if (carrier == long.class) {
            transformer = new VarHandleSegmentAsLongs(swap, alignmentMask);
        } else if (carrier == double.class) {
            transformer = new VarHandleSegmentAsDoubles(swap, alignmentMask);
        } else {
            throw shouldNotReachHere();
        }

        long min_align_mask = transformer.length - 1;
        boolean allowAtomicAccess = (alignmentMask & min_align_mask) == min_align_mask;
        int modesMask = VarHandleImpl.accessModesBitMask(carrier, allowAtomicAccess);
        return VarHandleImpl.newVarHandle(modesMask, transformer,
                carrier, MemorySegment.class, long.class);
    }

    /**
     * endianness
     **/
    final boolean swap;

    /**
     * access size (in bytes, computed from var handle carrier type)
     **/
    final long length;

    /**
     * alignment constraint (in bytes, expressed as a bit mask)
     **/
    final long alignmentMask;

    _VarHandleSegmentViewBase(boolean swap, long length, long alignmentMask) {
        this.swap = swap;
        this.length = length;
        this.alignmentMask = alignmentMask;
    }

    static IllegalArgumentException newIllegalArgumentExceptionForMisalignedAccess(long address) {
        return new IllegalArgumentException("Misaligned access at address: " + _Utils.toHexString(address));
    }

    void checkAddress(_AbstractMemorySegmentImpl ms, long offset, boolean ro) {
        Objects.requireNonNull(ms).checkAccess(offset, length, ro);
    }

    long getOffset(_AbstractMemorySegmentImpl bb, long offset) {
        long base = bb.unsafeGetOffset();
        long address = base + offset;
        long maxAlignMask = bb.maxAlignMask();
        if (((address | maxAlignMask) & alignmentMask) != 0) {
            throw newIllegalArgumentExceptionForMisalignedAccess(address);
        }
        return address;
    }

    private static final class VarHandleSegmentAsBytes extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsBytes(boolean swap, long alignmentMask) {
            super(swap, 1, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextByte(
                        _ScopedMemoryAccess.getByte(session, base, offset));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextByte(
                        _ScopedMemoryAccess.getByteVolatile(session, base, offset));
                case SET -> _ScopedMemoryAccess.putByte(session, base, offset, accessor.nextByte());
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsShorts extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsShorts(boolean swap, long alignmentMask) {
            super(swap, 2, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
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
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsChars extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsChars(boolean swap, long alignmentMask) {
            super(swap, 2, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
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
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsInts extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsInts(boolean swap, long alignmentMask) {
            super(swap, 4, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
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
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsFloats extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsFloats(boolean swap, long alignmentMask) {
            super(swap, 4, alignmentMask);
        }

        static float i2f(boolean swap, int n) {
            return Float.intBitsToFloat(convEndian(n, swap));
        }

        static int f2i(boolean swap, float n) {
            return convEndian(Float.floatToRawIntBits(n), swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextFloat(
                        _ScopedMemoryAccess.getFloatUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextFloat(
                        i2f(swap, _ScopedMemoryAccess.getIntVolatile(session, base, offset)));
                case SET -> _ScopedMemoryAccess.putFloatUnaligned(
                        session, base, offset, accessor.nextFloat(), swap);
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsLongs extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsLongs(boolean swap, long alignmentMask) {
            super(swap, 8, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
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
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }

    private static final class VarHandleSegmentAsDoubles extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsDoubles(boolean swap, long alignmentMask) {
            super(swap, 8, alignmentMask);
        }

        static double l2d(boolean swap, long n) {
            return Double.longBitsToDouble(convEndian(n, swap));
        }

        static long d2l(boolean swap, double n) {
            return convEndian(Double.doubleToRawLongBits(n), swap);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
            offset = getOffset(ms, offset);
            Object base = ms.unsafeGetBase();
            var session = ms.sessionImpl();
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextDouble(
                        _ScopedMemoryAccess.getDoubleUnaligned(session, base, offset, swap));
                case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> accessor.moveToReturn().putNextDouble(
                        l2d(swap, _ScopedMemoryAccess.getLongVolatile(session, base, offset)));
                case SET -> _ScopedMemoryAccess.putDoubleUnaligned(
                        session, base, offset, accessor.nextDouble(), swap);
                default -> throw new UnsupportedOperationException("TODO");
            }
        }
    }
}

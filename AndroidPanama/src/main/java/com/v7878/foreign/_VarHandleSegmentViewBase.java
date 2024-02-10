package com.v7878.foreign;

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
        //TODO: all modes
        int accessModesBitMask = (1 << AccessMode.GET.ordinal()) | (1 << AccessMode.SET.ordinal());

        VarHandleTransformer transformer;
        if (carrier == byte.class) {
            transformer = new VarHandleSegmentAsBytes(swap, alignmentMask);
        } else if (carrier == char.class) {
            transformer = new VarHandleSegmentAsChars(swap, alignmentMask);
        } else if (carrier == short.class) {
            transformer = new VarHandleSegmentAsShorts(swap, alignmentMask);
        } else if (carrier == int.class) {
            transformer = new VarHandleSegmentAsInts(swap, alignmentMask);
        } else if (carrier == float.class) {
            transformer = new VarHandleSegmentAsFloats(swap, alignmentMask);
        } else if (carrier == long.class) {
            transformer = new VarHandleSegmentAsLongs(swap, alignmentMask);
        } else if (carrier == double.class) {
            transformer = new VarHandleSegmentAsDoubles(swap, alignmentMask);
        } else {
            throw new IllegalStateException("Cannot get here");
        }

        return VarHandleImpl.newVarHandle(accessModesBitMask,
                transformer, carrier, MemorySegment.class, long.class);
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

    static UnsupportedOperationException newUnsupportedAccessModeForAlignment(long alignment) {
        return new UnsupportedOperationException("Unsupported access mode for alignment: " + alignment);
    }

    void checkAddress(_AbstractMemorySegmentImpl ms, long offset, boolean ro) {
        Objects.requireNonNull(ms).checkAccess(offset, length, ro);
    }

    long offsetNonPlain(_AbstractMemorySegmentImpl bb, long offset) {
        final long min_align_mask = length - 1;
        if ((alignmentMask & min_align_mask) != min_align_mask) {
            throw newUnsupportedAccessModeForAlignment(alignmentMask + 1);
        }
        return offsetPlain(bb, offset);
    }

    long offsetPlain(_AbstractMemorySegmentImpl bb, long offset) {
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
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextByte(
                        _ScopedMemoryAccess.getByte(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset)));
                case SET -> _ScopedMemoryAccess.putByte(ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextByte());
                default -> throw new AssertionError("Cannot get here");
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
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextChar(
                        _ScopedMemoryAccess.getCharUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putCharUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextChar(), swap);
                default -> throw new AssertionError("Cannot get here");
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
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextShort(
                        _ScopedMemoryAccess.getShortUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putShortUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextShort(), swap);
                default -> throw new AssertionError("Cannot get here");
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
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextInt(
                        _ScopedMemoryAccess.getIntUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putIntUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextInt(), swap);
                default -> throw new AssertionError("Cannot get here");
            }
        }
    }

    private static final class VarHandleSegmentAsFloats extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsFloats(boolean swap, long alignmentMask) {
            super(swap, 4, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextFloat(
                        _ScopedMemoryAccess.getFloatUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putFloatUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextFloat(), swap);
                default -> throw new AssertionError("Cannot get here");
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
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextLong(
                        _ScopedMemoryAccess.getLongUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putLongUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextLong(), swap);
                default -> throw new AssertionError("Cannot get here");
            }
        }
    }

    private static final class VarHandleSegmentAsDoubles extends _VarHandleSegmentViewBase {

        VarHandleSegmentAsDoubles(boolean swap, long alignmentMask) {
            super(swap, 8, alignmentMask);
        }

        @Override
        public void transform(VarHandleImpl handle, AccessMode mode, EmulatedStackFrame stack) {
            StackFrameAccessor accessor = stack.createAccessor();
            _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl)
                    accessor.nextReference(MemorySegment.class);
            long offset = accessor.nextLong();
            checkAddress(ms, offset, isReadOnly(mode));
            switch (mode) {
                case GET -> accessor.moveToReturn().putNextDouble(
                        _ScopedMemoryAccess.getDoubleUnaligned(ms.sessionImpl(), ms.unsafeGetBase(),
                                offsetPlain(ms, offset), swap));
                case SET -> _ScopedMemoryAccess.putDoubleUnaligned(
                        ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetPlain(ms, offset), accessor.nextDouble(), swap);
                default -> throw new AssertionError("Cannot get here");
            }
        }
    }
}

package java.lang.foreign;

import static com.v7878.unsafe.invoke.VarHandleImpl.isReadOnly;

import com.v7878.foreign.VarHandle;
import com.v7878.foreign.VarHandle.AccessMode;
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
        int accessModesBitMask = (1 << AccessMode.GET.ordinal()) | (1 << AccessMode.SET.ordinal());

        VarHandleTransformer transformer;
        if (carrier == byte.class) {
            transformer = new VarHandleSegmentAsBytes(swap, alignmentMask);
        } else if (carrier == char.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        } else if (carrier == short.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        } else if (carrier == int.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        } else if (carrier == float.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        } else if (carrier == long.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        } else if (carrier == double.class) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
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
        return new IllegalArgumentException("Misaligned access at address: " + address);
    }

    void checkAddress(_AbstractMemorySegmentImpl ms, long offset, boolean ro) {
        Objects.requireNonNull(ms).checkAccess(offset, length, ro);
    }

    long offset(_AbstractMemorySegmentImpl bb, long offset) {
        long address = offsetNoVMAlignCheck(bb, offset);
        if ((address & (length - 1)) != 0) {
            throw newIllegalArgumentExceptionForMisalignedAccess(address);
        }
        return address;
    }

    long offsetNoVMAlignCheck(_AbstractMemorySegmentImpl bb, long offset) {
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
                                offsetNoVMAlignCheck(ms, offset)));
                case SET -> _ScopedMemoryAccess.putByte(ms.sessionImpl(), ms.unsafeGetBase(),
                        offsetNoVMAlignCheck(ms, offset), accessor.nextByte());
                default -> throw new AssertionError("Cannot get here");
            }
        }
    }
}

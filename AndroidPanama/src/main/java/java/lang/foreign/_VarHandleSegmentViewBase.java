package java.lang.foreign;

import com.v7878.unsafe.invoke.VarHandleImpl.VarHandleTransformer;

import java.util.Objects;

abstract class _VarHandleSegmentViewBase implements VarHandleTransformer {
    /**
     * endianness
     **/
    final boolean no;

    /**
     * access size (in bytes, computed from var handle carrier type)
     **/
    final long length;

    /**
     * alignment constraint (in bytes, expressed as a bit mask)
     **/
    final long alignmentMask;

    _VarHandleSegmentViewBase(boolean no, long length, long alignmentMask) {
        this.no = no;
        this.length = length;
        this.alignmentMask = alignmentMask;
    }

    static IllegalArgumentException newIllegalArgumentExceptionForMisalignedAccess(long address) {
        return new IllegalArgumentException("Misaligned access at address: " + address);
    }

    _AbstractMemorySegmentImpl checkAddress(Object obb, long offset, boolean ro) {
        _AbstractMemorySegmentImpl ms = (_AbstractMemorySegmentImpl) Objects.requireNonNull(obb);
        ms.checkAccess(offset, length, ro);
        return ms;
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
}

package com.v7878.foreign;

import static com.v7878.unsafe.ExtraMemoryAccess.LOG2_ARRAY_BYTE_INDEX_SCALE;

import com.v7878.r8.annotations.DoNotShrink;

/**
 * This class contains optimized bulk operation methods that operate on one or several
 * memory segments.
 * <p>
 * Generally, the methods attempt to work with as-large-as-possible units of memory at
 * a time.
 * <p>
 * It should be noted that when invoking scoped memory access get/set operations, it
 * is imperative from a performance perspective to convey the sharp types from the
 * call site in order for the compiler to pick the correct Unsafe access variant.
 */
final class _SegmentBulkOperations {
    private _SegmentBulkOperations() {
    }
    // All the threshold values below MUST be a power of two and should preferably be
    // greater or equal to 2^3.

    // TODO: choose the best values for android
    private static final int NATIVE_THRESHOLD_MISMATCH = 1 << 4;

    @DoNotShrink // TODO: DoNotInline
    public static long mismatch(_AbstractMemorySegmentImpl src, long srcFromOffset, long srcToOffset,
                                _AbstractMemorySegmentImpl dst, long dstFromOffset, long dstToOffset) {
        final long srcBytes = srcToOffset - srcFromOffset;
        final long dstBytes = dstToOffset - dstFromOffset;
        src.checkAccess(srcFromOffset, srcBytes, true);
        dst.checkAccess(dstFromOffset, dstBytes, true);

        final long length = Math.min(srcBytes, dstBytes);
        final boolean srcAndDstBytesDiffer = srcBytes != dstBytes;

        if (length == 0) {
            return srcAndDstBytesDiffer ? 0 : -1;
        } else if (length < NATIVE_THRESHOLD_MISMATCH) {
            return mismatch(src, srcFromOffset, dst, dstFromOffset, 0, (int) length, srcAndDstBytesDiffer);
        } else {
            if (_ScopedMemoryAccess.getByte(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset) !=
                    _ScopedMemoryAccess.getByte(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset)) {
                return 0;
            }
            long i = vectorizedMismatchLargeForBytes(src.sessionImpl(), dst.sessionImpl(),
                    src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset,
                    dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset,
                    length);
            if (i >= 0) {
                return i;
            }
            final long remaining = ~i;
            assert remaining < 8 : "remaining greater than 7: " + remaining;
            i = length - remaining;
            return mismatch(src, srcFromOffset + i, dst, dstFromOffset + i, i, (int) remaining, srcAndDstBytesDiffer);
        }
    }

    // Mismatch is handled in chunks of 64 (unroll of eight 8s), 8, 4, 2, and 1 byte(s).
    private static long mismatch(_AbstractMemorySegmentImpl src, long srcFromOffset,
                                 _AbstractMemorySegmentImpl dst, long dstFromOffset,
                                 long start, int length, boolean srcAndDstBytesDiffer) {
        int offset = 0;
        final int limit = length & (NATIVE_THRESHOLD_MISMATCH - Long.BYTES);
        for (; offset < limit; offset += 8) {
            final long s = _ScopedMemoryAccess.getLongUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, false);
            final long d = _ScopedMemoryAccess.getLongUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, false);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
        }
        int remaining = length - offset;
        // 0...0X00
        if (remaining >= Integer.BYTES) {
            final int s = _ScopedMemoryAccess.getIntUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, false);
            final int d = _ScopedMemoryAccess.getIntUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, false);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
            offset += Integer.BYTES;
            remaining -= Integer.BYTES;
        }
        // 0...00X0
        if (remaining >= Short.BYTES) {
            final short s = _ScopedMemoryAccess.getShortUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, false);
            final short d = _ScopedMemoryAccess.getShortUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, false);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
            offset += Short.BYTES;
            remaining -= Short.BYTES;
        }
        // 0...000X
        if (remaining == 1) {
            final byte s = _ScopedMemoryAccess.getByte(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset);
            final byte d = _ScopedMemoryAccess.getByte(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset);
            if (s != d) {
                return start + offset;
            }
        }
        return srcAndDstBytesDiffer ? (start + length) : -1;
        // We have now fully handled 0...0X...XXXX
    }

    private static int mismatch(long first, long second) {
        final long x = first ^ second;
        return Long.numberOfTrailingZeros(x) / 8;
    }

    private static int mismatch(int first, int second) {
        final int x = first ^ second;
        return Integer.numberOfTrailingZeros(x) / 8;
    }

    private static int mismatch(short first, short second) {
        return ((0xff & first) == (0xff & second)) ? 1 : 0;
    }

    /**
     * Mismatch over long lengths.
     */
    private static long vectorizedMismatchLargeForBytes(_MemorySessionImpl aSession, _MemorySessionImpl bSession,
                                                        Object a, long aOffset,
                                                        Object b, long bOffset,
                                                        long length) {
        long off = 0;
        long remaining = length;
        int i, size;
        boolean lastSubRange = false;
        while (remaining > 7 && !lastSubRange) {
            if (remaining > Integer.MAX_VALUE) {
                size = Integer.MAX_VALUE;
            } else {
                size = (int) remaining;
                lastSubRange = true;
            }
            i = _ScopedMemoryAccess.vectorizedMismatch(aSession, bSession,
                    a, aOffset + off,
                    b, bOffset + off,
                    size, LOG2_ARRAY_BYTE_INDEX_SCALE);
            if (i >= 0)
                return off + i;

            i = size - ~i;
            off += i;
            remaining -= i;
        }
        return ~remaining;
    }
}

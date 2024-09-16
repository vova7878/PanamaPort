package com.v7878.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS_BIG_ENDIAN;
import static com.v7878.unsafe.ExtraMemoryAccess.LOG2_ARRAY_BYTE_INDEX_SCALE;

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
    private static final int NATIVE_THRESHOLD_FILL = 1 << 4;
    private static final int NATIVE_THRESHOLD_MISMATCH = 1 << 4;
    private static final int NATIVE_THRESHOLD_COPY = 1 << 4;

    public static MemorySegment fill(_AbstractMemorySegmentImpl dst, byte value) {
        dst.checkReadOnly(false);
        if (dst.length == 0) {
            // Implicit state check
            dst.checkValidState();
        } else if (dst.length < NATIVE_THRESHOLD_FILL) {
            // 0 <= length < FILL_NATIVE_LIMIT : 0...0X...XXXX

            // Handle smaller segments directly without transitioning to native code
            final long u = Byte.toUnsignedLong(value);
            final long longValue = u << 56 | u << 48 | u << 40 | u << 32 | u << 24 | u << 16 | u << 8 | u;

            int offset = 0;
            // 0...0X...X000
            final int limit = (int) (dst.length & (NATIVE_THRESHOLD_FILL - 8));
            for (; offset < limit; offset += 8) {
                _ScopedMemoryAccess.putLongUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + offset, longValue, IS_BIG_ENDIAN);
            }
            int remaining = (int) dst.length - offset;
            // 0...0X00
            if (remaining >= 4) {
                _ScopedMemoryAccess.putIntUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + offset, (int) longValue, IS_BIG_ENDIAN);
                offset += 4;
                remaining -= 4;
            }
            // 0...00X0
            if (remaining >= 2) {
                _ScopedMemoryAccess.putShortUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + offset, (short) longValue, IS_BIG_ENDIAN);
                offset += 2;
                remaining -= 2;
            }
            // 0...000X
            if (remaining == 1) {
                _ScopedMemoryAccess.putByte(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + offset, value);
            }
            // We have now fully handled 0...0X...XXXX
        } else {
            // Handle larger segments via native calls
            _ScopedMemoryAccess.setMemory(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset(), dst.length, value);
        }
        return dst;
    }

    public static void copy(_AbstractMemorySegmentImpl src, long srcOffset,
                            _AbstractMemorySegmentImpl dst, long dstOffset,
                            long size) {
        _Utils.checkNonNegativeIndex(size, "size");
        // Implicit null check for src and dst
        src.checkAccess(srcOffset, size, true);
        dst.checkAccess(dstOffset, size, false);

        if (size <= 0) {
            // Do nothing
        } else if (size < NATIVE_THRESHOLD_COPY && !src.overlaps(dst)) {
            // 0 < size < FILL_NATIVE_LIMIT : 0...0X...XXXX
            //
            // Strictly, we could check for !src.asSlice(srcOffset, size).overlaps(dst.asSlice(dstOffset, size) but
            // this is a bit slower and it likely very unusual there is any difference in the outcome. Also, if there
            // is an overlap, we could tolerate one particular direction of overlap (but not the other).

            // 0...0X...X000
            final int limit = (int) (size & (NATIVE_THRESHOLD_COPY - 8));
            int offset = 0;
            for (; offset < limit; offset += 8) {
                final long v = _ScopedMemoryAccess.getLongUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcOffset + offset, IS_BIG_ENDIAN);
                _ScopedMemoryAccess.putLongUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstOffset + offset, v, IS_BIG_ENDIAN);
            }
            int remaining = (int) size - offset;
            // 0...0X00
            if (remaining >= 4) {
                final int v = _ScopedMemoryAccess.getIntUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcOffset + offset, IS_BIG_ENDIAN);
                _ScopedMemoryAccess.putIntUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstOffset + offset, v, IS_BIG_ENDIAN);
                offset += 4;
                remaining -= 4;
            }
            // 0...00X0
            if (remaining >= 2) {
                final short v = _ScopedMemoryAccess.getShortUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcOffset + offset, IS_BIG_ENDIAN);
                _ScopedMemoryAccess.putShortUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstOffset + offset, v, IS_BIG_ENDIAN);
                offset += 2;
                remaining -= 2;
            }
            // 0...000X
            if (remaining == 1) {
                final byte v = _ScopedMemoryAccess.getByte(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcOffset + offset);
                _ScopedMemoryAccess.putByte(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstOffset + offset, v);
            }
            // We have now fully handled 0...0X...XXXX
        } else {
            // For larger sizes, the transition to native code pays off
            _ScopedMemoryAccess.copyMemory(src.sessionImpl(), dst.sessionImpl(),
                    src.unsafeGetBase(), src.unsafeGetOffset() + srcOffset,
                    dst.unsafeGetBase(), dst.unsafeGetOffset() + dstOffset, size);
        }
    }

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
        final int limit = length & (NATIVE_THRESHOLD_MISMATCH - 8);
        for (; offset < limit; offset += 8) {
            final long s = _ScopedMemoryAccess.getLongUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, IS_BIG_ENDIAN);
            final long d = _ScopedMemoryAccess.getLongUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, IS_BIG_ENDIAN);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
        }
        int remaining = length - offset;
        // 0...0X00
        if (remaining >= 4) {
            final int s = _ScopedMemoryAccess.getIntUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, IS_BIG_ENDIAN);
            final int d = _ScopedMemoryAccess.getIntUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, IS_BIG_ENDIAN);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
            offset += 4;
            remaining -= 4;
        }
        // 0...00X0
        if (remaining >= 2) {
            final short s = _ScopedMemoryAccess.getShortUnaligned(src.sessionImpl(), src.unsafeGetBase(), src.unsafeGetOffset() + srcFromOffset + offset, IS_BIG_ENDIAN);
            final short d = _ScopedMemoryAccess.getShortUnaligned(dst.sessionImpl(), dst.unsafeGetBase(), dst.unsafeGetOffset() + dstFromOffset + offset, IS_BIG_ENDIAN);
            if (s != d) {
                return start + offset + mismatch(s, d);
            }
            offset += 2;
            remaining -= 2;
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
        return (!IS_BIG_ENDIAN
                ? Long.numberOfTrailingZeros(x)
                : Long.numberOfLeadingZeros(x)) / 8;
    }

    private static int mismatch(int first, int second) {
        final int x = first ^ second;
        return (!IS_BIG_ENDIAN
                ? Integer.numberOfTrailingZeros(x)
                : Integer.numberOfLeadingZeros(x)) / 8;
    }

    private static int mismatch(short first, short second) {
        if (!IS_BIG_ENDIAN) {
            return ((0xff & first) == (0xff & second)) ? 1 : 0;
        } else {
            return ((0xff & first) == (0xff & second)) ? 0 : 1;
        }
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

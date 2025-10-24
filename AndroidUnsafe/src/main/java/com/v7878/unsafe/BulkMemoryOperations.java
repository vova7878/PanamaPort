package com.v7878.unsafe;

import com.v7878.r8.annotations.DoNotShrink;

public class BulkMemoryOperations {
    private BulkMemoryOperations() {
    }

    // All the threshold values below MUST be a power of two and should preferably be
    // greater or equal to 2^3.

    // TODO: choose the best values for android
    private static final int NATIVE_THRESHOLD_FILL = 1 << 4;
    private static final int NATIVE_THRESHOLD_COPY = 1 << 4;
    private static final int NATIVE_THRESHOLD_MISMATCH = 1 << 4;

    @DoNotShrink // TODO: DoNotInline
    private static void setMemoryJava(Object base, long offset, int bytes, byte value) {
        final long u1 = value & 0xffL;
        final long u2 = u1 | (u1 << 8);
        final long u4 = u2 | (u2 << 16);
        final long u8 = u4 | (u4 << 32);

        int remaining = bytes;

        if (remaining >= Byte.BYTES && (offset & Byte.BYTES) != 0) {
            AndroidUnsafe.putByte(base, offset, (byte) u1);
            offset += Byte.BYTES;
            remaining -= Byte.BYTES;
        }

        if (remaining >= Short.BYTES && (offset & Short.BYTES) != 0) {
            AndroidUnsafe.putShort(base, offset, (short) u2);
            offset += Short.BYTES;
            remaining -= Short.BYTES;
        }

        if (remaining >= Integer.BYTES && (offset & Integer.BYTES) != 0) {
            AndroidUnsafe.putInt(base, offset, (int) u4);
            offset += Integer.BYTES;
            remaining -= Integer.BYTES;
        }

        for (; remaining >= Long.BYTES; offset += Long.BYTES, remaining -= Long.BYTES) {
            AndroidUnsafe.putLong(base, offset, u8);
        }

        if (remaining >= Integer.BYTES) {
            AndroidUnsafe.putInt(base, offset, (int) u4);
            offset += Integer.BYTES;
            remaining -= Integer.BYTES;
        }

        if (remaining >= Short.BYTES) {
            AndroidUnsafe.putShort(base, offset, (short) u2);
            offset += Short.BYTES;
            remaining -= Short.BYTES;
        }

        if (remaining >= Byte.BYTES) {
            AndroidUnsafe.putByte(base, offset, (byte) u1);
        }
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes <= 0) {
            return;
        }
        if (bytes < NATIVE_THRESHOLD_FILL) {
            // 0 <= length < FILL_NATIVE_LIMIT : 0...0X...XXXX
            // Handle smaller segments directly without transitioning to native code
            setMemoryJava(base, offset, (int) bytes, value);
        } else {
            // Handle larger segments via native calls
            ExtraMemoryAccess.setMemory(base, offset, bytes, value);
        }
    }

    // TODO: use aligned memory access
    @DoNotShrink // TODO: DoNotInline
    private static void copyMemoryJava(Object srcBase, long srcOffset,
                                       Object dstBase, long dstOffset, int bytes) {
        long position = 0;
        int remaining = bytes;

        for (; remaining >= Long.BYTES; position += Long.BYTES, remaining -= Long.BYTES) {
            final long v = AndroidUnsafe.getLongUnaligned(srcBase, srcOffset + position);
            AndroidUnsafe.putLongUnaligned(dstBase, dstOffset + position, v);
        }

        if (remaining >= Integer.BYTES) {
            final int v = AndroidUnsafe.getIntUnaligned(srcBase, srcOffset + position);
            AndroidUnsafe.putIntUnaligned(dstBase, dstOffset + position, v);
            position += Integer.BYTES;
            remaining -= Integer.BYTES;
        }

        if (remaining >= Short.BYTES) {
            final short v = AndroidUnsafe.getShortUnaligned(srcBase, srcOffset + position);
            AndroidUnsafe.putShortUnaligned(dstBase, dstOffset + position, v);
            position += Short.BYTES;
            remaining -= Short.BYTES;
        }

        if (remaining == 1) {
            final byte v = AndroidUnsafe.getByte(srcBase, srcOffset + position);
            AndroidUnsafe.putByte(dstBase, dstOffset + position, v);
        }
    }

    private static boolean backwards(Object srcBase, long srcOffset,
                                     Object dstBase, long dstOffset, long size) {
        if (srcBase == dstBase) {  // both either native or the same heap segment
            final long srcEnd = srcOffset + size;
            final long destEnd = dstOffset + size;

            boolean overlaps = (srcOffset < destEnd && srcEnd > dstOffset);
            //  one direction overlap
            return overlaps && (srcOffset < dstOffset);
        }
        return false;
    }

    public static void copyMemory(Object srcBase, long srcOffset,
                                  Object dstBase, long dstOffset, long bytes) {
        if (bytes <= 0) {
            return;
        }
        if (bytes < NATIVE_THRESHOLD_COPY && !backwards(srcBase, srcOffset, dstBase, dstOffset, bytes)) {
            // 0 < size < FILL_NATIVE_LIMIT : 0...0X...XXXX
            // Handle smaller segments directly without transitioning to native code
            copyMemoryJava(srcBase, srcOffset, dstBase, dstOffset, (int) bytes);
        } else {
            // For larger sizes, the transition to native code pays off
            ExtraMemoryAccess.copyMemory(srcBase, srcOffset, dstBase, dstOffset, bytes);
        }
    }

    public static void copySwapMemory(Object srcBase, long srcOffset,
                                      Object dstBase, long dstOffset,
                                      long bytes, long elemSize) {
        // TODO? java version
        ExtraMemoryAccess.copySwapMemory(srcBase, srcOffset, dstBase, dstOffset, bytes, elemSize);
    }

    /*
    Bits 63 and N * 8 (N = 1..7) of this number are zero.  Call these bits
    the "holes".  Note that there is a hole just to the left of
    each byte, with an extra at the end:

    bits:  01111110 11111110 11111110 11111110 11111110 11111110 11111110 11111111
    bytes: AAAAAAAA BBBBBBBB CCCCCCCC DDDDDDDD EEEEEEEE FFFFFFFF GGGGGGGG HHHHHHHH

    The 1-bits make sure that carries propagate to the next 0-bit.
    The 0-bits provide holes for carries to fall into.
    */
    private static final long HIMAGIC_FOR_BYTES = 0x8080_8080_8080_8080L;
    private static final long LOMAGIC_FOR_BYTES = 0x0101_0101_0101_0101L;

    private static boolean mightContainZeroByte(long l) {
        return ((l - LOMAGIC_FOR_BYTES) & (~l) & HIMAGIC_FOR_BYTES) != 0;
    }

    private static final long HIMAGIC_FOR_SHORTS = 0x8000_8000_8000_8000L;
    private static final long LOMAGIC_FOR_SHORTS = 0x0001_0001_0001_0001L;

    private static boolean mightContainZeroShort(long l) {
        return ((l - LOMAGIC_FOR_SHORTS) & (~l) & HIMAGIC_FOR_SHORTS) != 0;
    }

    private static final long HIMAGIC_FOR_INTS = 0x8000_0000_8000_0000L;
    private static final long LOMAGIC_FOR_INTS = 0x0000_0001_0000_0001L;

    private static boolean mightContainZeroInt(long l) {
        return ((l - LOMAGIC_FOR_INTS) & (~l) & HIMAGIC_FOR_INTS) != 0;
    }

    @DoNotShrink // TODO: DoNotInline
    public static long strlenByte(Object base, long offset, long bytes) {
        if (bytes < Byte.BYTES) {
            // There can be no null terminator present
            return -1;
        }

        long position = 0;
        long remaining = bytes;

        // Align memory
        for (; (offset + position & 0x7) != 0; position += Byte.BYTES, remaining -= Byte.BYTES) {
            byte val = AndroidUnsafe.getByte(base, offset + position);
            if (val == 0) {
                return position;
            }
        }
        for (; remaining >= Long.BYTES; position += Long.BYTES, remaining -= Long.BYTES) {
            long val = AndroidUnsafe.getLong(base, offset + position);
            if (mightContainZeroByte(val)) {
                for (int j = 0; j < Long.BYTES; j += Byte.BYTES) {
                    if ((val & 0xffL) == 0L) {
                        return position + j;
                    }
                    val >>>= Byte.SIZE;
                }
            }
        }
        // Handle the tail
        for (; remaining >= Byte.BYTES; position += Byte.BYTES, remaining -= Byte.BYTES) {
            byte val = AndroidUnsafe.getByte(base, offset + position);
            if (val == 0) {
                return position;
            }
        }
        return -1;
    }

    // TODO: use aligned memory access
    @DoNotShrink // TODO: DoNotInline
    public static long strlenShort(Object base, long offset, long bytes) {
        if (bytes < Short.BYTES) {
            // There can be no null terminator present
            return -1;
        }

        long position = 0;
        long remaining = bytes;

        for (; remaining >= Long.BYTES; position += Long.BYTES, remaining -= Long.BYTES) {
            long val = AndroidUnsafe.getLongUnaligned(base, offset + position);
            if (mightContainZeroShort(val)) {
                for (int j = 0; j < Long.BYTES; j += Short.BYTES) {
                    if ((val & 0xffffL) == 0L) {
                        return position + j;
                    }
                    val >>>= Short.SIZE;
                }
            }
        }
        // Handle the tail
        for (; remaining >= Short.BYTES; position += Short.BYTES, remaining -= Short.BYTES) {
            short val = AndroidUnsafe.getShortUnaligned(base, offset + position);
            if (val == 0) {
                return position;
            }
        }
        return -1;
    }

    // TODO: use aligned memory access
    @DoNotShrink // TODO: DoNotInline
    public static long strlenInt(Object base, long offset, long bytes) {
        if (bytes < Integer.BYTES) {
            // There can be no null terminator present
            return -1;
        }

        long position = 0;
        long remaining = bytes;

        for (; remaining >= Long.BYTES; position += Long.BYTES, remaining -= Long.BYTES) {
            long val = AndroidUnsafe.getLongUnaligned(base, offset + position);
            if (mightContainZeroInt(val)) {
                for (int j = 0; j < Long.BYTES; j += Integer.BYTES) {
                    if ((val & 0xffffffffL) == 0L) {
                        return position + j;
                    }
                    val >>>= Integer.SIZE;
                }
            }
        }
        // Handle the tail
        for (; remaining >= Integer.BYTES; position += Integer.BYTES, remaining -= Integer.BYTES) {
            int val = AndroidUnsafe.getIntUnaligned(base, offset + position);
            if (val == 0) {
                return position;
            }
        }
        return -1;
    }
}

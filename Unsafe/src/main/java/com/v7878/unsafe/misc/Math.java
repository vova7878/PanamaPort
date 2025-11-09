package com.v7878.unsafe.misc;

import com.v7878.r8.annotations.AlwaysInline;

public class Math {
    @AlwaysInline
    public static short convEndian16(short value, boolean swap) {
        return swap ? Short.reverseBytes(value) : value;
    }

    @AlwaysInline
    public static int convEndian32(int value, boolean swap) {
        return swap ? Integer.reverseBytes(value) : value;
    }

    @AlwaysInline
    public static long convEndian64(long value, boolean swap) {
        return swap ? Long.reverseBytes(value) : value;
    }

    @AlwaysInline
    public static float i2f(int n, boolean swap) {
        return Float.intBitsToFloat(convEndian32(n, swap));
    }

    @AlwaysInline
    public static int f2i(float n, boolean swap) {
        return convEndian32(Float.floatToRawIntBits(n), swap);
    }

    @AlwaysInline
    public static double l2d(long n, boolean swap) {
        return Double.longBitsToDouble(convEndian64(n, swap));
    }

    @AlwaysInline
    public static long d2l(double n, boolean swap) {
        return convEndian64(Double.doubleToRawLongBits(n), swap);
    }

    @AlwaysInline
    public static boolean byte2bool(byte b) {
        return b != 0;
    }

    @AlwaysInline
    public static byte bool2byte(boolean b) {
        return b ? (byte) 1 : (byte) 0;
    }

    @AlwaysInline
    public static long maxUL(long a, long b) {
        return Long.compareUnsigned(a, b) > 0 ? a : b;
    }

    @AlwaysInline
    public static long minUL(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? a : b;
    }

    @AlwaysInline
    public static int maxU(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0 ? a : b;
    }

    @AlwaysInline
    public static int minU(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? a : b;
    }

    @AlwaysInline
    public static int ceilDiv(int x, int y) {
        final int q = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }

    @AlwaysInline
    public static long ceilDiv(long x, int y) {
        return ceilDiv(x, (long) y);
    }

    @AlwaysInline
    public static long ceilDiv(long x, long y) {
        final long q = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }

    public static long addExactUL(long x, long y) {
        long tmp = x + y;
        if (Long.compareUnsigned(tmp, x) < 0) {
            throw new ArithmeticException("unsigned long overflow");
        }
        return tmp;
    }

    public static int addExactU(int x, int y) {
        int tmp = x + y;
        if (Integer.compareUnsigned(tmp, x) < 0) {
            throw new ArithmeticException("unsigned int overflow");
        }
        return tmp;
    }

    @AlwaysInline
    public static boolean is32Bit(long value) {
        return value >>> 32 == 0;
    }

    @AlwaysInline
    public static boolean isSigned32Bit(long value) {
        return (((value >> 32) + 1) & ~1) == 0;
    }

    @AlwaysInline
    public static boolean isPowerOfTwoUL(long x) {
        return (x != 0) && (x & (x - 1)) == 0;
    }

    @AlwaysInline
    public static boolean isPowerOfTwoU(int x) {
        return (x != 0) && (x & (x - 1)) == 0;
    }

    @AlwaysInline
    public static boolean isAlignedUL(long x, long alignment) {
        assert isPowerOfTwoUL(alignment);
        return (x & (alignment - 1)) == 0;
    }

    @AlwaysInline
    public static boolean isAlignedU(int x, int alignment) {
        assert isPowerOfTwoU(alignment);
        return (x & (alignment - 1)) == 0;
    }

    @AlwaysInline
    public static long roundDownUL(long x, long alignment) {
        assert isPowerOfTwoUL(alignment);
        return x & -alignment;
    }

    @AlwaysInline
    public static int roundDownU(int x, int alignment) {
        assert isPowerOfTwoU(alignment);
        return x & -alignment;
    }

    @AlwaysInline
    public static long roundUpUL(long x, long alignment) {
        assert isPowerOfTwoUL(alignment);
        return roundDownUL(addExactUL(x, alignment - 1), alignment);
    }

    @AlwaysInline
    public static int roundUpU(int x, int alignment) {
        assert isPowerOfTwoU(alignment);
        return roundDownU(addExactU(x, alignment - 1), alignment);
    }

    @AlwaysInline
    public static int log2(int value) {
        return 31 - Integer.numberOfLeadingZeros(value);
    }

    @AlwaysInline
    public static int log2(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }

    @AlwaysInline
    public static int toUnsignedInt(byte n) {
        return n & 0xff;
    }

    @AlwaysInline
    public static int toUnsignedInt(short n) {
        return n & 0xffff;
    }

    @AlwaysInline
    public static long toUnsignedLong(byte n) {
        return n & 0xffL;
    }

    @AlwaysInline
    public static long toUnsignedLong(short n) {
        return n & 0xffffL;
    }

    @AlwaysInline
    public static long toUnsignedLong(int n) {
        return n & 0xffffffffL;
    }
}

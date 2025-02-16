package com.v7878.unsafe.misc;

public class Math {
    public static short convEndian(short value, boolean swap) {
        return swap ? Short.reverseBytes(value) : value;
    }

    public static int convEndian(int value, boolean swap) {
        return swap ? Integer.reverseBytes(value) : value;
    }

    public static long convEndian(long value, boolean swap) {
        return swap ? Long.reverseBytes(value) : value;
    }

    public static float i2f(int n, boolean swap) {
        return Float.intBitsToFloat(convEndian(n, swap));
    }

    public static int f2i(float n, boolean swap) {
        return convEndian(Float.floatToRawIntBits(n), swap);
    }

    public static double l2d(long n, boolean swap) {
        return Double.longBitsToDouble(convEndian(n, swap));
    }

    public static long d2l(double n, boolean swap) {
        return convEndian(Double.doubleToRawLongBits(n), swap);
    }

    public static long maxUL(long a, long b) {
        return Long.compareUnsigned(a, b) > 0 ? a : b;
    }

    public static long minUL(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? a : b;
    }

    public static int maxU(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0 ? a : b;
    }

    public static int minU(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? a : b;
    }

    public static int ceilDiv(int x, int y) {
        final int q = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }

    public static long ceilDiv(long x, int y) {
        return ceilDiv(x, (long) y);
    }

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

    public static boolean is32Bit(long value) {
        return value >>> 32 == 0;
    }

    public static boolean isSigned32Bit(long value) {
        return (((value >> 32) + 1) & ~1) == 0;
    }

    public static boolean isPowerOfTwoUL(long x) {
        return (x != 0) && (x & (x - 1)) == 0;
    }

    public static boolean isPowerOfTwoL(long x) {
        return (x > 0) && isPowerOfTwoUL(x);
    }

    public static boolean isPowerOfTwoU(int x) {
        return (x != 0) && (x & (x - 1)) == 0;
    }

    public static boolean isPowerOfTwo(int x) {
        return (x > 0) && isPowerOfTwoU(x);
    }

    public static boolean isAlignedL(long x, long alignment) {
        if (!isPowerOfTwoUL(alignment)) {
            throw new IllegalArgumentException("alignment(" +
                    Long.toUnsignedString(alignment) + ") must be power of two");
        }
        return (x & (alignment - 1)) == 0;
    }

    public static boolean isAligned(int x, int alignment) {
        if (!isPowerOfTwoU(alignment)) {
            throw new IllegalArgumentException("alignment(" +
                    Integer.toUnsignedString(alignment) + ") must be power of two");
        }
        return (x & (alignment - 1)) == 0;
    }

    public static long roundDownUL(long x, long alignment) {
        if (!isPowerOfTwoUL(alignment)) {
            throw new IllegalArgumentException("alignment(" +
                    Long.toUnsignedString(alignment) + ") must be power of two");
        }
        return x & -alignment;
    }

    public static long roundDownL(long x, long alignment) {
        if (x < 0 || alignment < 0) {
            throw new IllegalArgumentException(
                    "x(" + x + ") or alignment(" + alignment + ") is negative");
        }
        return roundDownUL(x, alignment);
    }

    public static long roundUpUL(long x, long alignment) {
        return roundDownUL(addExactUL(x, alignment - 1), alignment);
    }

    public static long roundUpL(long x, long alignment) {
        return roundDownL(java.lang.Math.addExact(x, alignment - 1), alignment);
    }

    public static int roundDownU(int x, int alignment) {
        if (!isPowerOfTwoU(alignment)) {
            throw new IllegalArgumentException("alignment(" +
                    Integer.toUnsignedString(alignment) + ") must be power of two");
        }
        return x & -alignment;
    }

    public static int roundDown(int x, int alignment) {
        if (x < 0 || alignment < 0) {
            throw new IllegalArgumentException(
                    "x(" + x + ") or alignment(" + alignment + ") is negative");
        }
        return roundDownU(x, alignment);
    }

    public static int roundUpU(int x, int alignment) {
        return roundDownU(addExactU(x, alignment - 1), alignment);
    }

    public static int roundUp(int x, int alignment) {
        return roundDown(java.lang.Math.addExact(x, alignment - 1), alignment);
    }

    public static int log2(int value) {
        return 31 - Integer.numberOfLeadingZeros(value);
    }

    public static int log2(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }

    public static int toUnsignedInt(byte n) {
        return n & 0xff;
    }

    public static int toUnsignedInt(short n) {
        return n & 0xffff;
    }

    public static long toUnsignedLong(byte n) {
        return n & 0xffL;
    }

    public static long toUnsignedLong(short n) {
        return n & 0xffffL;
    }

    public static long toUnsignedLong(int n) {
        return n & 0xffffffffL;
    }
}

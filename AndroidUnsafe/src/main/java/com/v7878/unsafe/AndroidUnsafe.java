package com.v7878.unsafe;

import static com.v7878.misc.Math.convEndian;
import static com.v7878.misc.Math.toUnsignedInt;
import static com.v7878.misc.Math.toUnsignedLong;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.Objects;

public class AndroidUnsafe {

    public static final int ADDRESS_SIZE = SunUnsafe.addressSize();
    public static final int PAGE_SIZE = SunUnsafe.pageSize();

    static {
        assert_((ADDRESS_SIZE == 4) || (ADDRESS_SIZE == 8), AssertionError::new);
    }

    public static final boolean IS64BIT = ADDRESS_SIZE == 8;
    public static final boolean IS_BIG_ENDIAN =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    public static final boolean UNALIGNED_ACCESS;

    static {
        String arch = System.getProperty("os.arch");
        UNALIGNED_ACCESS = arch != null && (arch.equals("i386") || arch.equals("x86")
                || arch.equals("amd64") || arch.equals("x86_64"));
    }

    public static final int ARRAY_BOOLEAN_BASE_OFFSET = arrayBaseOffset(boolean[].class);
    public static final int ARRAY_BYTE_BASE_OFFSET = arrayBaseOffset(byte[].class);
    public static final int ARRAY_SHORT_BASE_OFFSET = arrayBaseOffset(short[].class);
    public static final int ARRAY_CHAR_BASE_OFFSET = arrayBaseOffset(char[].class);
    public static final int ARRAY_INT_BASE_OFFSET = arrayBaseOffset(int[].class);
    public static final int ARRAY_LONG_BASE_OFFSET = arrayBaseOffset(long[].class);
    public static final int ARRAY_FLOAT_BASE_OFFSET = arrayBaseOffset(float[].class);
    public static final int ARRAY_DOUBLE_BASE_OFFSET = arrayBaseOffset(double[].class);
    public static final int ARRAY_OBJECT_BASE_OFFSET = arrayBaseOffset(Object[].class);

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = arrayIndexScale(boolean[].class);
    public static final int ARRAY_BYTE_INDEX_SCALE = arrayIndexScale(byte[].class);
    public static final int ARRAY_SHORT_INDEX_SCALE = arrayIndexScale(short[].class);
    public static final int ARRAY_CHAR_INDEX_SCALE = arrayIndexScale(char[].class);
    public static final int ARRAY_INT_INDEX_SCALE = arrayIndexScale(int[].class);
    public static final int ARRAY_LONG_INDEX_SCALE = arrayIndexScale(long[].class);
    public static final int ARRAY_FLOAT_INDEX_SCALE = arrayIndexScale(float[].class);
    public static final int ARRAY_DOUBLE_INDEX_SCALE = arrayIndexScale(double[].class);
    public static final int ARRAY_OBJECT_INDEX_SCALE = arrayIndexScale(Object[].class);

    public static int addressSize() {
        return ADDRESS_SIZE;
    }

    public static int pageSize() {
        return PAGE_SIZE;
    }

    public static boolean unalignedAccess() {
        return UNALIGNED_ACCESS;
    }

    public static boolean isBigEndian() {
        return IS_BIG_ENDIAN;
    }

    private static <E extends Throwable, T> T throwException0(Throwable th) throws E {
        //noinspection unchecked
        throw (E) th;
    }

    public static <T> T throwException(Throwable th) {
        return throwException0(th);
    }

    public static void park(boolean absolute, long time) {
        SunUnsafe.park(absolute, time);
    }

    public static void unpark(Object obj) {
        SunUnsafe.unpark(obj);
    }

    public static void loadFence() {
        SunUnsafe.loadFence();
    }

    public static void storeFence() {
        SunUnsafe.storeFence();
    }

    public static void fullFence() {
        SunUnsafe.fullFence();
    }

    public static long allocateMemory(long bytes) {
        return SunUnsafe.allocateMemory(bytes);
    }

    public static void freeMemory(long address) {
        SunUnsafe.freeMemory(address);
    }

    public static void setMemory(long address, long bytes, byte value) {
        SunUnsafe.setMemory(address, bytes, value);
    }

    public static void copyMemory(long srcAddr, long dstAddr, long bytes) {
        SunUnsafe.copyMemory(srcAddr, dstAddr, bytes);
    }

    public static <T> T allocateInstance(Class<T> clazz) {
        //noinspection unchecked
        return (T) nothrows_run(() -> SunUnsafe.allocateInstance(clazz));
    }

    public static long objectFieldOffset(Field field) {
        return SunUnsafe.objectFieldOffset(field);
    }

    public static int arrayBaseOffset(Class<?> clazz) {
        int out = SunUnsafe.arrayBaseOffset(clazz);
        assert_(out != 0, IllegalStateException::new);
        return out;
    }

    public static int arrayIndexScale(Class<?> clazz) {
        int out = SunUnsafe.arrayIndexScale(clazz);
        assert_(out != 0, IllegalStateException::new);
        return out;
    }

    public static boolean getBooleanO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getBoolean(obj, offset);
    }

    public static void putBooleanO(Object obj, long offset, boolean value) {
        assert obj != null;
        SunUnsafe.putBoolean(obj, offset, value);
    }

    public static byte getByteO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getByte(obj, offset);
    }

    public static void putByteO(Object obj, long offset, byte value) {
        assert obj != null;
        SunUnsafe.putByte(obj, offset, value);
    }

    public static char getCharO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getChar(obj, offset);
    }

    public static void putCharO(Object obj, long offset, char value) {
        assert obj != null;
        SunUnsafe.putChar(obj, offset, value);
    }

    public static short getShortO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getShort(obj, offset);
    }

    public static void putShortO(Object obj, long offset, short value) {
        assert obj != null;
        SunUnsafe.putShort(obj, offset, value);
    }

    public static int getIntO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getInt(obj, offset);
    }

    public static void putIntO(Object obj, long offset, int value) {
        assert obj != null;
        SunUnsafe.putInt(obj, offset, value);
    }

    public static float getFloatO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getFloat(obj, offset);
    }

    public static void putFloatO(Object obj, long offset, float value) {
        assert obj != null;
        SunUnsafe.putFloat(obj, offset, value);
    }

    public static long getLongO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getLong(obj, offset);
    }

    public static void putLongO(Object obj, long offset, long value) {
        assert obj != null;
        SunUnsafe.putLong(obj, offset, value);
    }

    public static double getDoubleO(Object obj, long offset) {
        assert obj != null;
        return SunUnsafe.getDouble(obj, offset);
    }

    public static void putDoubleO(Object obj, long offset, double value) {
        assert obj != null;
        SunUnsafe.putDouble(obj, offset, value);
    }

    public static boolean getBooleanN(long address) {
        return SunUnsafe.getBoolean(address);
    }

    public static void putBooleanN(long address, boolean value) {
        SunUnsafe.putBoolean(address, value);
    }

    public static byte getByteN(long address) {
        return SunUnsafe.getByte(address);
    }

    public static void putByteN(long address, byte value) {
        SunUnsafe.putByte(address, value);
    }

    public static char getCharN(long address) {
        return SunUnsafe.getChar(address);
    }

    public static void putCharN(long address, char value) {
        SunUnsafe.putChar(address, value);
    }

    public static short getShortN(long address) {
        return SunUnsafe.getShort(address);
    }

    public static void putShortN(long address, short value) {
        SunUnsafe.putShort(address, value);
    }

    public static int getIntN(long address) {
        return SunUnsafe.getInt(address);
    }

    public static void putIntN(long address, int value) {
        SunUnsafe.putInt(address, value);
    }

    public static float getFloatN(long address) {
        return SunUnsafe.getFloat(address);
    }

    public static void putFloatN(long address, float value) {
        SunUnsafe.putFloat(address, value);
    }

    public static long getLongN(long address) {
        return SunUnsafe.getLong(address);
    }

    public static void putLongN(long address, long value) {
        SunUnsafe.putLong(address, value);
    }

    public static double getDoubleN(long address) {
        return SunUnsafe.getDouble(address);
    }

    public static void putDoubleN(long address, double value) {
        SunUnsafe.putDouble(address, value);
    }

    public static boolean getBoolean(Object obj, long offset) {
        return SunUnsafe.getBoolean(obj, offset);
    }

    public static void putBoolean(Object obj, long offset, boolean value) {
        SunUnsafe.putBoolean(obj, offset, value);
    }

    public static byte getByte(Object obj, long offset) {
        return SunUnsafe.getByte(obj, offset);
    }

    public static void putByte(Object obj, long offset, byte value) {
        SunUnsafe.putByte(obj, offset, value);
    }

    public static char getChar(Object obj, long offset) {
        return SunUnsafe.getChar(obj, offset);
    }

    public static void putChar(Object obj, long offset, char value) {
        SunUnsafe.putChar(obj, offset, value);
    }

    public static short getShort(Object obj, long offset) {
        return SunUnsafe.getShort(obj, offset);
    }

    public static void putShort(Object obj, long offset, short value) {
        SunUnsafe.putShort(obj, offset, value);
    }

    public static int getInt(Object obj, long offset) {
        return SunUnsafe.getInt(obj, offset);
    }

    public static void putInt(Object obj, long offset, int value) {
        SunUnsafe.putInt(obj, offset, value);
    }

    public static float getFloat(Object obj, long offset) {
        return SunUnsafe.getFloat(obj, offset);
    }

    public static void putFloat(Object obj, long offset, float value) {
        SunUnsafe.putFloat(obj, offset, value);
    }

    public static long getLong(Object obj, long offset) {
        return SunUnsafe.getLong(obj, offset);
    }

    public static void putLong(Object obj, long offset, long value) {
        SunUnsafe.putLong(obj, offset, value);
    }

    public static double getDouble(Object obj, long offset) {
        return SunUnsafe.getDouble(obj, offset);
    }

    public static void putDouble(Object obj, long offset, double value) {
        SunUnsafe.putDouble(obj, offset, value);
    }

    public static long getWordO(Object obj, long offset) {
        return IS64BIT ? getLongO(obj, offset) : getIntO(obj, offset) & 0xffffffffL;
    }

    public static void putWordO(Object obj, long offset, long value) {
        if (IS64BIT) {
            putLongO(obj, offset, value);
        } else {
            putIntO(obj, offset, (int) value);
        }
    }

    public static long getWordN(long address) {
        return IS64BIT ? getLongN(address) : getIntN(address) & 0xffffffffL;
    }

    public static void putWordN(long address, long value) {
        if (IS64BIT) {
            putLongN(address, value);
        } else {
            putIntN(address, (int) value);
        }
    }

    public static long getWord(Object obj, long offset) {
        return IS64BIT ? getLong(obj, offset) : getInt(obj, offset) & 0xffffffffL;
    }

    public static void putWord(Object obj, long offset, long value) {
        if (IS64BIT) {
            putLong(obj, offset, value);
        } else {
            putInt(obj, offset, (int) value);
        }
    }

    public static Object getObject(Object obj, long offset) {
        Objects.requireNonNull(obj);
        return SunUnsafe.getObject(obj, offset);
    }

    public static void putObject(Object obj, long offset, Object value) {
        Objects.requireNonNull(obj);
        SunUnsafe.putObject(obj, offset, value);
    }

    private static int pickPos(int top, int pos) {
        return isBigEndian() ? top - pos : pos;
    }

    private static long makeLong(int i0, int i1) {
        return (toUnsignedLong(i0) << pickPos(32, 0))
                | (toUnsignedLong(i1) << pickPos(32, 32));
    }

    private static long makeLong(short i0, short i1, short i2, short i3) {
        return ((toUnsignedLong(i0) << pickPos(48, 0))
                | (toUnsignedLong(i1) << pickPos(48, 16))
                | (toUnsignedLong(i2) << pickPos(48, 32))
                | (toUnsignedLong(i3) << pickPos(48, 48)));
    }

    private static long makeLong(byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        return ((toUnsignedLong(i0) << pickPos(56, 0))
                | (toUnsignedLong(i1) << pickPos(56, 8))
                | (toUnsignedLong(i2) << pickPos(56, 16))
                | (toUnsignedLong(i3) << pickPos(56, 24))
                | (toUnsignedLong(i4) << pickPos(56, 32))
                | (toUnsignedLong(i5) << pickPos(56, 40))
                | (toUnsignedLong(i6) << pickPos(56, 48))
                | (toUnsignedLong(i7) << pickPos(56, 56)));
    }

    private static int makeInt(short i0, short i1) {
        return (toUnsignedInt(i0) << pickPos(16, 0))
                | (toUnsignedInt(i1) << pickPos(16, 16));
    }

    private static int makeInt(byte i0, byte i1, byte i2, byte i3) {
        return ((toUnsignedInt(i0) << pickPos(24, 0))
                | (toUnsignedInt(i1) << pickPos(24, 8))
                | (toUnsignedInt(i2) << pickPos(24, 16))
                | (toUnsignedInt(i3) << pickPos(24, 24)));
    }

    private static short makeShort(byte i0, byte i1) {
        return (short) ((toUnsignedInt(i0) << pickPos(8, 0))
                | (toUnsignedInt(i1) << pickPos(8, 8)));
    }

    public static long getLongUnaligned(Object obj, long offset) {
        if (UNALIGNED_ACCESS || ((offset & 7) == 0)) {
            return getLong(obj, offset);
        } else if ((offset & 3) == 0) {
            return makeLong(getInt(obj, offset),
                    getInt(obj, offset + 4));
        } else if ((offset & 1) == 0) {
            return makeLong(getShort(obj, offset),
                    getShort(obj, offset + 2),
                    getShort(obj, offset + 4),
                    getShort(obj, offset + 6));
        } else {
            return makeLong(getByte(obj, offset),
                    getByte(obj, offset + 1),
                    getByte(obj, offset + 2),
                    getByte(obj, offset + 3),
                    getByte(obj, offset + 4),
                    getByte(obj, offset + 5),
                    getByte(obj, offset + 6),
                    getByte(obj, offset + 7));
        }
    }

    public static long getLongUnaligned(Object obj, long offset, boolean swap) {
        return convEndian(getLongUnaligned(obj, offset), swap);
    }

    public static double getDoubleUnaligned(Object obj, long offset) {
        return Double.longBitsToDouble(getLongUnaligned(obj, offset));
    }

    public static double getDoubleUnaligned(Object obj, long offset, boolean swap) {
        return Double.longBitsToDouble(getLongUnaligned(obj, offset, swap));
    }

    public static int getIntUnaligned(Object obj, long offset) {
        if (UNALIGNED_ACCESS || ((offset & 3) == 0)) {
            return getInt(obj, offset);
        } else if ((offset & 1) == 0) {
            return makeInt(getShort(obj, offset),
                    getShort(obj, offset + 2));
        } else {
            return makeInt(getByte(obj, offset),
                    getByte(obj, offset + 1),
                    getByte(obj, offset + 2),
                    getByte(obj, offset + 3));
        }
    }

    public static int getIntUnaligned(Object obj, long offset, boolean swap) {
        return convEndian(getIntUnaligned(obj, offset), swap);
    }

    public static float getFloatUnaligned(Object obj, long offset) {
        return Float.intBitsToFloat(getIntUnaligned(obj, offset));
    }

    public static float getFloatUnaligned(Object obj, long offset, boolean swap) {
        return Float.intBitsToFloat(getIntUnaligned(obj, offset, swap));
    }

    public static short getShortUnaligned(Object obj, long offset) {
        if (UNALIGNED_ACCESS || ((offset & 1) == 0)) {
            return getShort(obj, offset);
        } else {
            return makeShort(getByte(obj, offset),
                    getByte(obj, offset + 1));
        }
    }

    public static short getShortUnaligned(Object obj, long offset, boolean swap) {
        return convEndian(getShortUnaligned(obj, offset), swap);
    }

    public static char getCharUnaligned(Object obj, long offset) {
        return (char) getShortUnaligned(obj, offset);
    }

    public static char getCharUnaligned(Object obj, long offset, boolean swap) {
        return (char) getShortUnaligned(obj, offset, swap);
    }

    public static long getWordUnaligned(Object obj, long offset, boolean swap) {
        return IS64BIT ? getLongUnaligned(obj, offset, swap)
                : getIntUnaligned(obj, offset, swap) & 0xffffffffL;
    }

    public static long getWordUnaligned(Object obj, long offset) {
        return IS64BIT ? getLongUnaligned(obj, offset)
                : getIntUnaligned(obj, offset) & 0xffffffffL;
    }

    private static byte pick(byte le, byte be) {
        return isBigEndian() ? be : le;
    }

    private static short pick(short le, short be) {
        return isBigEndian() ? be : le;
    }

    private static int pick(int le, int be) {
        return isBigEndian() ? be : le;
    }

    private static void putLongParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        putByte(o, offset, pick(i0, i7));
        putByte(o, offset + 1, pick(i1, i6));
        putByte(o, offset + 2, pick(i2, i5));
        putByte(o, offset + 3, pick(i3, i4));
        putByte(o, offset + 4, pick(i4, i3));
        putByte(o, offset + 5, pick(i5, i2));
        putByte(o, offset + 6, pick(i6, i1));
        putByte(o, offset + 7, pick(i7, i0));
    }

    private static void putLongParts(Object o, long offset, short i0, short i1, short i2, short i3) {
        putShort(o, offset, pick(i0, i3));
        putShort(o, offset + 2, pick(i1, i2));
        putShort(o, offset + 4, pick(i2, i1));
        putShort(o, offset + 6, pick(i3, i0));
    }

    private static void putLongParts(Object o, long offset, int i0, int i1) {
        putInt(o, offset, pick(i0, i1));
        putInt(o, offset + 4, pick(i1, i0));
    }

    private static void putIntParts(Object o, long offset, short i0, short i1) {
        putShort(o, offset, pick(i0, i1));
        putShort(o, offset + 2, pick(i1, i0));
    }

    private static void putIntParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3) {
        putByte(o, offset, pick(i0, i3));
        putByte(o, offset + 1, pick(i1, i2));
        putByte(o, offset + 2, pick(i2, i1));
        putByte(o, offset + 3, pick(i3, i0));
    }

    private static void putShortParts(Object o, long offset, byte i0, byte i1) {
        putByte(o, offset, pick(i0, i1));
        putByte(o, offset + 1, pick(i1, i0));
    }

    public static void putLongUnaligned(Object o, long offset, long value) {
        if (UNALIGNED_ACCESS || ((offset & 7) == 0)) {
            putLong(o, offset, value);
        } else if ((offset & 3) == 0) {
            putLongParts(o, offset,
                    (int) (value),
                    (int) (value >>> 32));
        } else if ((offset & 1) == 0) {
            putLongParts(o, offset,
                    (short) (value),
                    (short) (value >>> 16),
                    (short) (value >>> 32),
                    (short) (value >>> 48));
        } else {
            putLongParts(o, offset,
                    (byte) (value),
                    (byte) (value >>> 8),
                    (byte) (value >>> 16),
                    (byte) (value >>> 24),
                    (byte) (value >>> 32),
                    (byte) (value >>> 40),
                    (byte) (value >>> 48),
                    (byte) (value >>> 56));
        }
    }

    public static void putLongUnaligned(Object o, long offset, long value, boolean swap) {
        putLongUnaligned(o, offset, convEndian(value, swap));
    }

    public static void putDoubleUnaligned(Object o, long offset, double value) {
        putLongUnaligned(o, offset, Double.doubleToRawLongBits(value));
    }

    public static void putDoubleUnaligned(Object o, long offset, double value, boolean swap) {
        putLongUnaligned(o, offset, Double.doubleToRawLongBits(value), swap);
    }

    public static void putIntUnaligned(Object o, long offset, int value) {
        if (UNALIGNED_ACCESS || ((offset & 3) == 0)) {
            putInt(o, offset, value);
        } else if ((offset & 1) == 0) {
            putIntParts(o, offset,
                    (short) (value),
                    (short) (value >>> 16));
        } else {
            putIntParts(o, offset,
                    (byte) (value),
                    (byte) (value >>> 8),
                    (byte) (value >>> 16),
                    (byte) (value >>> 24));
        }
    }

    public static void putIntUnaligned(Object o, long offset, int value, boolean swap) {
        putIntUnaligned(o, offset, convEndian(value, swap));
    }

    public static void putFloatUnaligned(Object o, long offset, float value) {
        putIntUnaligned(o, offset, Float.floatToRawIntBits(value));
    }

    public static void putFloatUnaligned(Object o, long offset, float value, boolean swap) {
        putIntUnaligned(o, offset, Float.floatToRawIntBits(value), swap);
    }

    public static void putShortUnaligned(Object o, long offset, short value) {
        if (UNALIGNED_ACCESS || ((offset & 1) == 0)) {
            putShort(o, offset, value);
        } else {
            putShortParts(o, offset,
                    (byte) (value),
                    (byte) (value >>> 8));
        }
    }

    public static void putShortUnaligned(Object o, long offset, short value, boolean swap) {
        putShortUnaligned(o, offset, convEndian(value, swap));
    }

    public static void putCharUnaligned(Object o, long offset, char value) {
        putShortUnaligned(o, offset, (short) value);
    }

    public static void putCharUnaligned(Object o, long offset, char value, boolean swap) {
        putShortUnaligned(o, offset, (short) value, swap);
    }

    public static void putWordUnaligned(Object obj, long offset, long value, boolean swap) {
        if (IS64BIT) {
            putLongUnaligned(obj, offset, value, swap);
        } else {
            putIntUnaligned(obj, offset, (int) value, swap);
        }
    }

    public static void putWordUnaligned(Object obj, long offset, long value) {
        if (IS64BIT) {
            putLongUnaligned(obj, offset, value);
        } else {
            putIntUnaligned(obj, offset, (int) value);
        }
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }
        if (srcBase == null) {
            if (destBase == null) {
                copyMemory(srcOffset, destOffset, bytes);
                return;
            }
        }
        for (long i = 0; i < bytes; i++) {
            putByteO(destBase, destOffset + i, getByteO(srcBase, srcOffset + i));
        }
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }
        if (base == null) {
            setMemory(offset, bytes, value);
            return;
        }
        for (long i = 0; i < bytes; i++) {
            putByteO(base, offset + i, value);
        }
    }

    public static int getIntVolatile(Object obj, long offset) {
        return SunUnsafe.getIntVolatile(obj, offset);
    }

    public static void putIntVolatile(Object obj, long offset, int value) {
        SunUnsafe.putIntVolatile(obj, offset, value);
    }

    public static long getLongVolatile(Object obj, long offset) {
        return SunUnsafe.getLongVolatile(obj, offset);
    }

    public static void putLongVolatile(Object obj, long offset, long value) {
        SunUnsafe.putLongVolatile(obj, offset, value);
    }

    public static Object getObjectVolatile(Object obj, long offset) {
        return SunUnsafe.getObjectVolatile(obj, offset);
    }

    public static void putObjectVolatile(Object obj, long offset, Object value) {
        SunUnsafe.putObjectVolatile(obj, offset, value);
    }

    public static boolean compareAndSetInt(Object obj, long offset,
                                           int expectedValue, int newValue) {
        return SunUnsafe.compareAndSwapInt(obj, offset, expectedValue, newValue);
    }

    public static boolean compareAndSetLong(Object obj, long offset,
                                            long expectedValue, long newValue) {
        return SunUnsafe.compareAndSwapLong(obj, offset, expectedValue, newValue);
    }

    public static boolean compareAndSetObject(Object obj, long offset,
                                              Object expectedValue, Object newValue) {
        return SunUnsafe.compareAndSwapObject(obj, offset, expectedValue, newValue);
    }

    public static int compareAndExchangeInt(Object obj, long offset,
                                            int expectedValue, int newValue) {
        int v;
        do {
            v = SunUnsafe.getIntVolatile(obj, offset);
        } while (!SunUnsafe.compareAndSwapInt(obj, offset, v, v == expectedValue ? newValue : v));
        return v;
    }

    public static long compareAndExchangeLong(Object obj, long offset,
                                              long expectedValue, long newValue) {
        long v;
        do {
            v = SunUnsafe.getLongVolatile(obj, offset);
        } while (!SunUnsafe.compareAndSwapLong(obj, offset, v, v == expectedValue ? newValue : v));
        return v;
    }

    public static Object compareAndExchangeObject(Object obj, long offset,
                                                  Object expectedValue, Object newValue) {
        Object v;
        do {
            v = SunUnsafe.getObjectVolatile(obj, offset);
        } while (!SunUnsafe.compareAndSwapObject(obj, offset, v, v == expectedValue ? newValue : v));
        return v;
    }

    public static boolean compareAndSwapInt(Object obj, long offset, int expectedValue, int value) {
        return SunUnsafe.compareAndSwapInt(obj, offset, expectedValue, value);
    }

    public static boolean compareAndSwapLong(Object obj, long offset, long expectedValue, long value) {
        return SunUnsafe.compareAndSwapLong(obj, offset, expectedValue, value);
    }

    public static boolean compareAndSwapObject(Object obj, long offset, Object expectedValue, Object value) {
        return SunUnsafe.compareAndSwapObject(obj, offset, expectedValue, value);
    }

    public static int getAndSetInt(Object obj, long offset, int newValue) {
        return SunUnsafe.getAndSetInt(obj, offset, newValue);
    }

    public static long getAndSetLong(Object obj, long offset, long newValue) {
        return SunUnsafe.getAndSetLong(obj, offset, newValue);
    }

    public static Object getAndSetObject(Object obj, long offset, Object newValue) {
        return SunUnsafe.getAndSetObject(obj, offset, newValue);
    }

    public static int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    public static long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }
}

package com.v7878.foreign;

import static com.v7878.unsafe.misc.Math.bool2byte;
import static com.v7878.unsafe.misc.Math.byte2bool;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.CheckDiscard;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.BulkMemoryOperations;
import com.v7878.unsafe.ExtraMemoryAccess;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.JavaNioAccess;

import java.io.FileDescriptor;

final class _ScopedMemoryAccess {
    private _ScopedMemoryAccess() {
    }

    record SessionLock(_MemorySessionImpl session) implements FineClosable {
        @AlwaysInline
        @CheckDiscard
        SessionLock {
            if (session != null) {
                session.acquire0();
            }
        }

        @AlwaysInline
        @CheckDiscard
        @Override
        public void close() {
            if (session != null) {
                session.release0();
            }
        }
    }

    @AlwaysInline
    @CheckDiscard
    static SessionLock lock(_MemorySessionImpl session) {
        return new SessionLock(session);
    }

    @AlwaysInline
    @CheckDiscard
    static void check(_MemorySessionImpl session) {
        if (session != null) {
            session.checkValidState();
        }
    }

    @AlwaysInline
    public static void copyMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                  Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        if (bytes <= 0) {
            // Implicit state check
            check(srcSession);
            check(dstSession);
            return;
        }
        try (var ignored1 = lock(srcSession);
             var ignored2 = lock(dstSession)) {
            BulkMemoryOperations.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    @AlwaysInline
    public static void copySwapMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                      Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        if (bytes <= 0) {
            // Implicit state check
            check(srcSession);
            check(dstSession);
            return;
        }
        try (var ignored1 = lock(srcSession);
             var ignored2 = lock(dstSession)) {
            BulkMemoryOperations.copySwapMemory(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
        }
    }

    @AlwaysInline
    public static void setMemory(_MemorySessionImpl session, Object base, long offset, long bytes, byte value) {
        if (bytes <= 0) {
            // Implicit state check
            check(session);
            return;
        }
        try (var ignored = lock(session)) {
            BulkMemoryOperations.setMemory(base, offset, bytes, value);
        }
    }

    @AlwaysInline
    public static long mismatch(_MemorySessionImpl aSession, _MemorySessionImpl bSession,
                                Object aBase, long aOffset,
                                Object bBase, long bOffset, long length) {
        if (length <= 0) {
            // Implicit state check
            check(aSession);
            check(bSession);
            return -1;
        }
        try (var ignored1 = lock(aSession);
             var ignored2 = lock(bSession)) {
            return BulkMemoryOperations.mismatch(aBase, aOffset, bBase, bOffset, length);
        }
    }

    @AlwaysInline
    public static boolean isLoaded(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            return JavaNioAccess.isLoaded(address, length);
        }
    }

    @AlwaysInline
    public static void load(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.load(address, length);
        }
    }

    @AlwaysInline
    public static void unload(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.unload(address, length);
        }
    }

    @AlwaysInline
    public static void force(_MemorySessionImpl session, FileDescriptor fd, long address, long offset, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.force(fd, address, offset, length);
        }
    }

    @AlwaysInline
    public static boolean getBoolean(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getBoolean(base, offset);
        }
    }

    @AlwaysInline
    public static void putBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putBoolean(base, offset, value);
        }
    }

    @AlwaysInline
    public static byte getByte(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getByte(base, offset);
        }
    }

    @AlwaysInline
    public static void putByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putByte(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getShort(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getShort(base, offset);
        }
    }

    @AlwaysInline
    public static void putShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putShort(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getInt(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getInt(base, offset);
        }
    }

    @AlwaysInline
    public static void putInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putInt(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getLong(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getLong(base, offset);
        }
    }

    @AlwaysInline
    public static void putLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putLong(base, offset, value);
        }
    }

    @AlwaysInline
    public static char getCharUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getCharUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putCharUnaligned(_MemorySessionImpl session, Object base, long offset, char value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putCharUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static short getShortUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getShortUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putShortUnaligned(_MemorySessionImpl session, Object base, long offset, short value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putShortUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static int getIntUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getIntUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putIntUnaligned(_MemorySessionImpl session, Object base, long offset, int value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putIntUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static float getFloatUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getFloatUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putFloatUnaligned(_MemorySessionImpl session, Object base, long offset, float value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putFloatUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static long getLongUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getLongUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putLongUnaligned(_MemorySessionImpl session, Object base, long offset, long value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putLongUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static double getDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getDoubleUnaligned(base, offset, swap);
        }
    }

    @AlwaysInline
    public static void putDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, double value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putDoubleUnaligned(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static boolean getBooleanVolatile(_MemorySessionImpl session, Object base, long offset) {
        return byte2bool(getByteVolatile(session, base, offset));
    }

    @AlwaysInline
    public static void putBooleanVolatile(_MemorySessionImpl session, Object base, long offset, boolean value) {
        putByteVolatile(session, base, offset, bool2byte(value));
    }

    @AlwaysInline
    public static byte getByteVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadByteAtomic(base, offset);
        }
    }

    @AlwaysInline
    public static void putByteVolatile(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeByteAtomic(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getShortVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadShortAtomic(base, offset);
        }
    }

    @AlwaysInline
    public static void putShortVolatile(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeShortAtomic(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getIntVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadIntAtomic(base, offset);
        }
    }

    @AlwaysInline
    public static void putIntVolatile(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeIntAtomic(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getLongVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadLongAtomic(base, offset);
        }
    }

    @AlwaysInline
    public static void putLongVolatile(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeLongAtomic(base, offset, value);
        }
    }

    @AlwaysInline
    public static boolean getAndSetBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        return byte2bool(getAndSetByte(session, base, offset, bool2byte(value)));
    }

    @AlwaysInline
    public static byte getAndSetByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeByte(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getAndSetShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeShort(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getAndSetInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeInt(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getAndSetLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeLong(base, offset, value);
        }
    }

    @AlwaysInline
    public static byte getAndAddByteWithCAS(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddByteWithCAS(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getAndAddShortWithCAS(_MemorySessionImpl session, Object base, long offset, short value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddShortWithCAS(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static int getAndAddIntWithCAS(_MemorySessionImpl session, Object base, long offset, int value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddIntWithCAS(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static float getAndAddFloatWithCAS(_MemorySessionImpl session, Object base, long offset, float value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddFloatWithCAS(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static long getAndAddLongWithCAS(_MemorySessionImpl session, Object base, long offset, long value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddLongWithCAS(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static double getAndAddDoubleWithCAS(_MemorySessionImpl session, Object base, long offset, double value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddDoubleWithCAS(base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static boolean getAndBitwiseAndBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        return byte2bool(getAndBitwiseAndByte(session, base, offset, bool2byte(value)));
    }

    @AlwaysInline
    public static byte getAndBitwiseAndByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndByte(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getAndBitwiseAndShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndShort(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getAndBitwiseAndInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndInt(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getAndBitwiseAndLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndLong(base, offset, value);
        }
    }

    @AlwaysInline
    public static boolean getAndBitwiseOrBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        return byte2bool(getAndBitwiseOrByte(session, base, offset, bool2byte(value)));
    }

    @AlwaysInline
    public static byte getAndBitwiseOrByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrByte(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getAndBitwiseOrShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrShort(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getAndBitwiseOrInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrInt(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getAndBitwiseOrLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrLong(base, offset, value);
        }
    }

    @AlwaysInline
    public static boolean getAndBitwiseXorBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        return byte2bool(getAndBitwiseXorByte(session, base, offset, bool2byte(value)));
    }

    @AlwaysInline
    public static byte getAndBitwiseXorByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorByte(base, offset, value);
        }
    }

    @AlwaysInline
    public static short getAndBitwiseXorShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorShort(base, offset, value);
        }
    }

    @AlwaysInline
    public static int getAndBitwiseXorInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorInt(base, offset, value);
        }
    }

    @AlwaysInline
    public static long getAndBitwiseXorLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorLong(base, offset, value);
        }
    }

    @AlwaysInline
    public static boolean compareAndExchangeBoolean(_MemorySessionImpl session, Object base, long offset, boolean expected, boolean desired) {
        return byte2bool(compareAndExchangeByte(session, base, offset, bool2byte(expected), bool2byte(desired)));
    }

    @AlwaysInline
    public static byte compareAndExchangeByte(_MemorySessionImpl session, Object base, long offset, byte expected, byte desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeByte(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static short compareAndExchangeShort(_MemorySessionImpl session, Object base, long offset, short expected, short desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeShort(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static int compareAndExchangeInt(_MemorySessionImpl session, Object base, long offset, int expected, int desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeInt(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static long compareAndExchangeLong(_MemorySessionImpl session, Object base, long offset, long expected, long desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeLong(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static boolean compareAndSetBoolean(_MemorySessionImpl session, Object base, long offset, boolean expected, boolean desired) {
        return compareAndSetByte(session, base, offset, bool2byte(expected), bool2byte(desired));
    }

    @AlwaysInline
    public static boolean compareAndSetByte(_MemorySessionImpl session, Object base, long offset, byte expected, byte desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetByte(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static boolean compareAndSetShort(_MemorySessionImpl session, Object base, long offset, short expected, short desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetShort(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static boolean compareAndSetInt(_MemorySessionImpl session, Object base, long offset, int expected, int desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetInt(base, offset, expected, desired);
        }
    }

    @AlwaysInline
    public static boolean compareAndSetLong(_MemorySessionImpl session, Object base, long offset, long expected, long desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetLong(base, offset, expected, desired);
        }
    }
}

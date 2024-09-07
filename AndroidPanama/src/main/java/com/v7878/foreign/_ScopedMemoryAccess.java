package com.v7878.foreign;

import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ExtraMemoryAccess;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.JavaNioAccess;

import java.io.FileDescriptor;
import java.util.function.Supplier;

final class _ScopedMemoryAccess {

    /**
     * This class manages the temporal bounds associated with a memory segment as well
     * as thread confinement. A session has a liveness bit, which is updated when the session is closed
     * (this operation is triggered by {@link _MemorySessionImpl#close()}). This bit is consulted prior
     * to memory access (see {@link _MemorySessionImpl#checkValidStateRaw()}).
     * There are two kinds of memory session: confined memory session and shared memory session.
     * A confined memory session has an associated owner thread that confines some operations to
     * associated owner thread such as {@link _MemorySessionImpl#close()} or {@link _MemorySessionImpl#checkValidStateRaw()}.
     * Shared sessions do not feature an owner thread - meaning their operations can be called, in a racy
     * manner, by multiple threads. To guarantee temporal safety in the presence of concurrent thread,
     * shared sessions use a more sophisticated synchronization mechanism, which guarantees that no concurrent
     * access is possible when a session is being closed (see {@link _ScopedMemoryAccess}).
     */
    static final class ScopedAccessError extends Error {

        private final Supplier<RuntimeException> runtimeExceptionSupplier;

        public ScopedAccessError(Supplier<RuntimeException> runtimeExceptionSupplier) {
            super("Invalid memory access", null, false, false);
            this.runtimeExceptionSupplier = runtimeExceptionSupplier;
        }

        public RuntimeException newRuntimeException() {
            return runtimeExceptionSupplier.get();
        }
    }

    private _ScopedMemoryAccess() {
    }

    private record SessionScopedLock(_MemorySessionImpl session) implements FineClosable {
        private SessionScopedLock {
            session.acquire0();
        }

        @Override
        public void close() {
            session.release0();
        }
    }

    private static SessionScopedLock lock(_MemorySessionImpl session) {
        return session == null ? null : new SessionScopedLock(session);
    }

    // COPY_NATIVE_THRESHOLD must be a power of two and should be greater than 2^3
    private static final long COPY_NATIVE_THRESHOLD = 1 << 6;

    public static void copyMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                  Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        try (var ignored1 = lock(srcSession);
             var ignored2 = lock(dstSession)) {
            // TODO: Will this work on android?
            //if (bytes < COPY_NATIVE_THRESHOLD && !_Utils.overlaps(
            //        srcBase, srcOffset, bytes, destBase, destOffset, bytes)) {
            if (false) {
                // 0 < size < FILL_NATIVE_LIMIT : 0...0X...XXXX
                //
                // Strictly, we could check for !src.asSlice(srcOffset, size).overlaps(dst.asSlice(dstOffset, size) but
                // this is a bit slower and it likely very unusual there is any difference in the outcome. Also, if there
                // is an overlap, we could tolerate one particular direction of overlap (but not the other).

                // 0...0X...X000
                final int limit = (int) (bytes & (COPY_NATIVE_THRESHOLD - 8));
                int copied = 0;
                for (; copied < limit; copied += 8) {
                    final long v = AndroidUnsafe.getLongUnaligned(srcBase, srcOffset + copied);
                    AndroidUnsafe.putLongUnaligned(destBase, destOffset + copied, v);
                }
                int remaining = (int) bytes - copied;
                // 0...0X00
                if (remaining >= 4) {
                    final int v = AndroidUnsafe.getIntUnaligned(srcBase, srcOffset + copied);
                    AndroidUnsafe.putIntUnaligned(destBase, destOffset + copied, v);
                    copied += 4;
                    remaining -= 4;
                }
                // 0...00X0
                if (remaining >= 2) {
                    final short v = AndroidUnsafe.getShortUnaligned(srcBase, srcOffset + copied);
                    AndroidUnsafe.putShortUnaligned(destBase, destOffset + copied, v);
                    copied += 2;
                    remaining -= 2;
                }
                // 0...000X
                if (remaining == 1) {
                    final byte v = AndroidUnsafe.getByte(srcBase, srcOffset + copied);
                    AndroidUnsafe.putByte(destBase, destOffset + copied, v);
                }
                // We have now fully handled 0...0X...XXXX
            } else {
                // For larger sizes, the transition to native code pays off
                ExtraMemoryAccess.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
            }
        }
    }

    public static void copySwapMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                      Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        try (var ignored1 = lock(srcSession);
             var ignored2 = lock(dstSession)) {
            ExtraMemoryAccess.copySwapMemory(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
        }
    }

    // FILL_NATIVE_THRESHOLD must be a power of two and should be greater than 2^3
    // Update the value for Aarch64 once 8338975 is fixed.
    private static final long FILL_NATIVE_THRESHOLD = 1L << (CURRENT_INSTRUCTION_SET == ARM64 ? 10 : 5);

    public static void setMemory(_MemorySessionImpl session, Object base, long offset, long bytes, byte value) {
        try (var ignored = lock(session)) {
            // TODO: Will this work on android?
            //if (bytes < FILL_NATIVE_THRESHOLD) {
            if (false) {
                // 0 <= length < FILL_NATIVE_LIMIT : 0...0X...XXXX

                // Handle smaller segments directly without transitioning to native code
                final long u = Byte.toUnsignedLong(value);
                final long longValue = u << 56 | u << 48 | u << 40 | u << 32 | u << 24 | u << 16 | u << 8 | u;

                int filled = 0;
                // 0...0X...X000
                final int limit = (int) (bytes & (FILL_NATIVE_THRESHOLD - 8));
                for (; filled < limit; filled += 8) {
                    AndroidUnsafe.putLongUnaligned(base, offset + filled, longValue);
                }
                int remaining = (int) bytes - filled;
                // 0...0X00
                if (remaining >= 4) {
                    AndroidUnsafe.putIntUnaligned(base, offset + filled, (int) longValue);
                    filled += 4;
                    remaining -= 4;
                }
                // 0...00X0
                if (remaining >= 2) {
                    AndroidUnsafe.putShortUnaligned(base, offset + filled, (short) longValue);
                    filled += 2;
                    remaining -= 2;
                }
                // 0...000X
                if (remaining == 1) {
                    AndroidUnsafe.putByte(base, offset + filled, value);
                }
                // We have now fully handled 0...0X...XXXX
            } else {
                // Handle larger segments via native calls
                ExtraMemoryAccess.setMemory(base, offset, bytes, value);
            }
        }
    }

    public static int vectorizedMismatch(_MemorySessionImpl aSession, _MemorySessionImpl bSession,
                                         Object aBase, long aOffset,
                                         Object bBase, long bOffset,
                                         int length, int log2ArrayIndexScale) {
        try (var ignored1 = lock(aSession);
             var ignored2 = lock(bSession)) {
            return ExtraMemoryAccess.vectorizedMismatch(
                    aBase, aOffset, bBase, bOffset, length, log2ArrayIndexScale);
        }
    }

    public static boolean isLoaded(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            return JavaNioAccess.isLoaded(address, length);
        }
    }

    public static void load(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.load(address, length);
        }
    }

    public static void unload(_MemorySessionImpl session, long address, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.unload(address, length);
        }
    }

    public static void force(_MemorySessionImpl session, FileDescriptor fd, long address, long offset, long length) {
        try (var ignored = lock(session)) {
            JavaNioAccess.force(fd, address, offset, length);
        }
    }

    public static boolean getBoolean(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getBoolean(base, offset);
        }
    }

    public static void putBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putBoolean(base, offset, value);
        }
    }

    public static byte getByte(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getByte(base, offset);
        }
    }

    public static void putByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putByte(base, offset, value);
        }
    }

    public static char getChar(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getChar(base, offset);
        }
    }

    public static void putChar(_MemorySessionImpl session, Object base, long offset, char value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putChar(base, offset, value);
        }
    }

    public static short getShort(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getShort(base, offset);
        }
    }

    public static void putShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putShort(base, offset, value);
        }
    }

    public static int getInt(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getInt(base, offset);
        }
    }

    public static void putInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putInt(base, offset, value);
        }
    }

    public static float getFloat(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getFloat(base, offset);
        }
    }

    public static void putFloat(_MemorySessionImpl session, Object base, long offset, float value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putFloat(base, offset, value);
        }
    }

    public static long getLong(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getLong(base, offset);
        }
    }

    public static void putLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putLong(base, offset, value);
        }
    }

    public static double getDouble(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getDouble(base, offset);
        }
    }

    public static void putDouble(_MemorySessionImpl session, Object base, long offset, double value) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putDouble(base, offset, value);
        }
    }

    public static char getCharUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getCharUnaligned(base, offset, swap);
        }
    }

    public static void putCharUnaligned(_MemorySessionImpl session, Object base, long offset, char value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putCharUnaligned(base, offset, value, swap);
        }
    }

    public static short getShortUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getShortUnaligned(base, offset, swap);
        }
    }

    public static void putShortUnaligned(_MemorySessionImpl session, Object base, long offset, short value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putShortUnaligned(base, offset, value, swap);
        }
    }

    public static int getIntUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getIntUnaligned(base, offset, swap);
        }
    }

    public static void putIntUnaligned(_MemorySessionImpl session, Object base, long offset, int value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putIntUnaligned(base, offset, value, swap);
        }
    }

    public static float getFloatUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getFloatUnaligned(base, offset, swap);
        }
    }

    public static void putFloatUnaligned(_MemorySessionImpl session, Object base, long offset, float value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putFloatUnaligned(base, offset, value, swap);
        }
    }

    public static long getLongUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getLongUnaligned(base, offset, swap);
        }
    }

    public static void putLongUnaligned(_MemorySessionImpl session, Object base, long offset, long value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putLongUnaligned(base, offset, value, swap);
        }
    }

    public static double getDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (var ignored = lock(session)) {
            return AndroidUnsafe.getDoubleUnaligned(base, offset, swap);
        }
    }

    public static void putDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, double value, boolean swap) {
        try (var ignored = lock(session)) {
            AndroidUnsafe.putDoubleUnaligned(base, offset, value, swap);
        }
    }

    public static byte getByteVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadByteAtomic(base, offset);
        }
    }

    public static void putByteVolatile(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeByteAtomic(base, offset, value);
        }
    }

    public static short getShortVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadShortAtomic(base, offset);
        }
    }

    public static void putShortVolatile(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeShortAtomic(base, offset, value);
        }
    }

    public static int getIntVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadIntAtomic(base, offset);
        }
    }

    public static void putIntVolatile(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeIntAtomic(base, offset, value);
        }
    }

    public static long getLongVolatile(_MemorySessionImpl session, Object base, long offset) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.loadLongAtomic(base, offset);
        }
    }

    public static void putLongVolatile(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            ExtraMemoryAccess.storeLongAtomic(base, offset, value);
        }
    }

    public static byte getAndSetByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeByte(base, offset, value);
        }
    }

    public static short getAndSetShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeShort(base, offset, value);
        }
    }

    public static int getAndSetInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeInt(base, offset, value);
        }
    }

    public static long getAndSetLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicExchangeLong(base, offset, value);
        }
    }

    public static byte getAndAddByteWithCAS(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddByteWithCAS(base, offset, value);
        }
    }

    public static short getAndAddShortWithCAS(_MemorySessionImpl session, Object base, long offset, short value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddShortWithCAS(base, offset, value, swap);
        }
    }

    public static int getAndAddIntWithCAS(_MemorySessionImpl session, Object base, long offset, int value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddIntWithCAS(base, offset, value, swap);
        }
    }

    public static float getAndAddFloatWithCAS(_MemorySessionImpl session, Object base, long offset, float value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddFloatWithCAS(base, offset, value, swap);
        }
    }

    public static long getAndAddLongWithCAS(_MemorySessionImpl session, Object base, long offset, long value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddLongWithCAS(base, offset, value, swap);
        }
    }

    public static double getAndAddDoubleWithCAS(_MemorySessionImpl session, Object base, long offset, double value, boolean swap) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAddDoubleWithCAS(base, offset, value, swap);
        }
    }

    public static byte getAndBitwiseAndByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndByte(base, offset, value);
        }
    }

    public static short getAndBitwiseAndShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndShort(base, offset, value);
        }
    }

    public static int getAndBitwiseAndInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndInt(base, offset, value);
        }
    }

    public static long getAndBitwiseAndLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchAndLong(base, offset, value);
        }
    }

    public static byte getAndBitwiseOrByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrByte(base, offset, value);
        }
    }

    public static short getAndBitwiseOrShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrShort(base, offset, value);
        }
    }

    public static int getAndBitwiseOrInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrInt(base, offset, value);
        }
    }

    public static long getAndBitwiseOrLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchOrLong(base, offset, value);
        }
    }

    public static byte getAndBitwiseXorByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorByte(base, offset, value);
        }
    }

    public static short getAndBitwiseXorShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorShort(base, offset, value);
        }
    }

    public static int getAndBitwiseXorInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorInt(base, offset, value);
        }
    }

    public static long getAndBitwiseXorLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicFetchXorLong(base, offset, value);
        }
    }

    public static byte compareAndExchangeByte(_MemorySessionImpl session, Object base, long offset, byte expected, byte desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeByte(base, offset, expected, desired);
        }
    }

    public static short compareAndExchangeShort(_MemorySessionImpl session, Object base, long offset, short expected, short desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeShort(base, offset, expected, desired);
        }
    }

    public static int compareAndExchangeInt(_MemorySessionImpl session, Object base, long offset, int expected, int desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeInt(base, offset, expected, desired);
        }
    }

    public static long compareAndExchangeLong(_MemorySessionImpl session, Object base, long offset, long expected, long desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndExchangeLong(base, offset, expected, desired);
        }
    }

    public static boolean compareAndSetByte(_MemorySessionImpl session, Object base, long offset, byte expected, byte desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetByte(base, offset, expected, desired);
        }
    }

    public static boolean compareAndSetShort(_MemorySessionImpl session, Object base, long offset, short expected, short desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetShort(base, offset, expected, desired);
        }
    }

    public static boolean compareAndSetInt(_MemorySessionImpl session, Object base, long offset, int expected, int desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetInt(base, offset, expected, desired);
        }
    }

    public static boolean compareAndSetLong(_MemorySessionImpl session, Object base, long offset, long expected, long desired) {
        try (var ignored = lock(session)) {
            return ExtraMemoryAccess.atomicCompareAndSetLong(base, offset, expected, desired);
        }
    }
}

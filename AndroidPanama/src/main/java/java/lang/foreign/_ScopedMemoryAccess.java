package java.lang.foreign;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ExtraMemoryAccess;
import com.v7878.unsafe.access.JavaNioAccess;

import java.io.FileDescriptor;
import java.lang.foreign._MemorySessionImpl.SessionScopedLock;

final class _ScopedMemoryAccess {
    private _ScopedMemoryAccess() {
    }

    private static SessionScopedLock lock(_MemorySessionImpl session) {
        return session == null ? null : session.lock();
    }

    public static void copyMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                  Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        try (SessionScopedLock ignored1 = lock(srcSession);
             SessionScopedLock ignored2 = lock(dstSession)) {
            ExtraMemoryAccess.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    public static void copySwapMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                      Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        try (SessionScopedLock ignored1 = lock(srcSession);
             SessionScopedLock ignored2 = lock(dstSession)) {
            ExtraMemoryAccess.copySwapMemory(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
        }
    }

    public static void setMemory(_MemorySessionImpl session, Object base, long offset, long bytes, byte value) {
        //TODO
        //ExtraMemoryAccess.setMemory(base, offset, bytes, value);
        throw new UnsupportedOperationException("Not supported yet");
    }

    public static int vectorizedMismatch(_MemorySessionImpl aSession, _MemorySessionImpl bSession,
                                         Object aBase, long aOffset,
                                         Object bBase, long bOffset,
                                         int length, int log2ArrayIndexScale) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public static boolean isLoaded(_MemorySessionImpl session, long address, long length) {
        try (SessionScopedLock ignored = lock(session)) {
            return JavaNioAccess.isLoaded(address, length);
        }
    }

    public static void load(_MemorySessionImpl session, long address, long length) {
        try (SessionScopedLock ignored = lock(session)) {
            JavaNioAccess.load(address, length);
        }
    }

    public static void unload(_MemorySessionImpl session, long address, long length) {
        try (SessionScopedLock ignored = lock(session)) {
            JavaNioAccess.unload(address, length);
        }
    }

    public static void force(_MemorySessionImpl session, FileDescriptor fd, long address, long offset, long length) {
        try (SessionScopedLock ignored = lock(session)) {
            JavaNioAccess.force(fd, address, offset, length);
        }
    }

    public static boolean getBoolean(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getBoolean(base, offset);
        }
    }

    public static void putBoolean(_MemorySessionImpl session, Object base, long offset, boolean value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putBoolean(base, offset, value);
        }
    }

    public static byte getByte(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getByte(base, offset);
        }
    }

    public static void putByte(_MemorySessionImpl session, Object base, long offset, byte value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putByte(base, offset, value);
        }
    }

    public static char getChar(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getChar(base, offset);
        }
    }

    public static void putChar(_MemorySessionImpl session, Object base, long offset, char value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putChar(base, offset, value);
        }
    }

    public static short getShort(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getShort(base, offset);
        }
    }

    public static void putShort(_MemorySessionImpl session, Object base, long offset, short value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putShort(base, offset, value);
        }
    }

    public static int getInt(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getInt(base, offset);
        }
    }

    public static void putInt(_MemorySessionImpl session, Object base, long offset, int value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putInt(base, offset, value);
        }
    }

    public static float getFloat(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getFloat(base, offset);
        }
    }

    public static void putFloat(_MemorySessionImpl session, Object base, long offset, float value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putFloat(base, offset, value);
        }
    }

    public static long getLong(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getLong(base, offset);
        }
    }

    public static void putLong(_MemorySessionImpl session, Object base, long offset, long value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putLong(base, offset, value);
        }
    }

    public static double getDouble(_MemorySessionImpl session, Object base, long offset) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getDouble(base, offset);
        }
    }

    public static void putDouble(_MemorySessionImpl session, Object base, long offset, double value) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putDouble(base, offset, value);
        }
    }

    public static char getCharUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getCharUnaligned(base, offset, swap);
        }
    }

    public static void putCharUnaligned(_MemorySessionImpl session, Object base, long offset, char value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putCharUnaligned(base, offset, value, swap);
        }
    }

    public static short getShortUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getShortUnaligned(base, offset, swap);
        }
    }

    public static void putShortUnaligned(_MemorySessionImpl session, Object base, long offset, short value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putShortUnaligned(base, offset, value, swap);
        }
    }

    public static int getIntUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getIntUnaligned(base, offset, swap);
        }
    }

    public static void putIntUnaligned(_MemorySessionImpl session, Object base, long offset, int value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putIntUnaligned(base, offset, value, swap);
        }
    }

    public static float getFloatUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getFloatUnaligned(base, offset, swap);
        }
    }

    public static void putFloatUnaligned(_MemorySessionImpl session, Object base, long offset, float value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putFloatUnaligned(base, offset, value, swap);
        }
    }

    public static long getLongUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getLongUnaligned(base, offset, swap);
        }
    }

    public static void putLongUnaligned(_MemorySessionImpl session, Object base, long offset, long value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putLongUnaligned(base, offset, value, swap);
        }
    }

    public static double getDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            return AndroidUnsafe.getDoubleUnaligned(base, offset, swap);
        }
    }

    public static void putDoubleUnaligned(_MemorySessionImpl session, Object base, long offset, double value, boolean swap) {
        try (SessionScopedLock ignored = lock(session)) {
            AndroidUnsafe.putDoubleUnaligned(base, offset, value, swap);
        }
    }
}

package java.lang.foreign;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.access.JavaNioAccess;

import java.io.FileDescriptor;
import java.lang.foreign._MemorySessionImpl.SessionScopedLock;

class _ScopedMemoryAccess {
    private _ScopedMemoryAccess() {
    }

    public static void copyMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                  Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        try (SessionScopedLock ignored1 = srcSession == null ? null : srcSession.lock();
             SessionScopedLock ignored2 = dstSession == null ? null : dstSession.lock()) {
            AndroidUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
            //TODO
            //ExtraMemoryAccess.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    public static void copySwapMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                      Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        //TODO
        //ExtraMemoryAccess.copySwapMemory(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
        throw new UnsupportedOperationException("Not supported yet");
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
        try (SessionScopedLock ignored = session == null ? null : session.lock()) {
            return JavaNioAccess.isLoaded(address, length);
        }
    }

    public static void load(_MemorySessionImpl session, long address, long length) {
        try (SessionScopedLock ignored = session == null ? null : session.lock()) {
            JavaNioAccess.load(address, length);
        }
    }

    public static void unload(_MemorySessionImpl session, long address, long length) {
        try (SessionScopedLock ignored = session == null ? null : session.lock()) {
            JavaNioAccess.unload(address, length);
        }
    }

    public static void force(_MemorySessionImpl session, FileDescriptor fd, long address, long offset, long length) {
        try (SessionScopedLock ignored = session == null ? null : session.lock()) {
            JavaNioAccess.force(fd, address, offset, length);
        }
    }
}

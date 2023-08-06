package java.lang.foreign;

class _ScopedMemoryAccess {
    private _ScopedMemoryAccess() {
    }

    public static void copyMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                  Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public static void copySwapMemory(_MemorySessionImpl srcSession, _MemorySessionImpl dstSession,
                                      Object srcBase, long srcOffset,
                                      Object destBase, long destOffset,
                                      long bytes, long elemSize) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public static void setMemory(_MemorySessionImpl session, Object base, long offset, long bytes, byte value) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public static int vectorizedMismatch(_MemorySessionImpl aSession, _MemorySessionImpl bSession,
                                         Object aBase, long aOffset,
                                         Object bBase, long bOffset,
                                         int length, int log2ArrayIndexScale) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }
}

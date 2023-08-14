package java.nio;

public class DirectByteBuffer$MemoryRef {
    public byte[] buffer;
    public long allocatedAddress;
    public final int offset;
    public boolean isAccessible;
    public boolean isFreed;

    // api 27+, TODO?
    //public final Object originalBufferObject;

    public DirectByteBuffer$MemoryRef() {
        throw new UnsupportedOperationException("Stub!");
    }

    public void free() {
        throw new UnsupportedOperationException("Stub!");
    }
}

package com.v7878.unsafe;

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class HeapByteBuffer extends CommonByteBuffer {
    public HeapByteBuffer(byte[] buf, int mark, int pos, int lim, int cap, int off,
                          boolean isReadOnly) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public abstract HeapByteBuffer slice();

    @Override
    public abstract HeapByteBuffer slice(int index, int length);

    @Override
    public abstract HeapByteBuffer duplicate();

    @Override
    public HeapByteBuffer compact() {
        throw new UnsupportedOperationException("Stub!");
    }
}

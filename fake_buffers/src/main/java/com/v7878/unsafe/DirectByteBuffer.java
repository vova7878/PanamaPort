package com.v7878.unsafe;

import java.io.FileDescriptor;

import sun.misc.Cleaner;

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class DirectByteBuffer extends CommonByteBuffer {
    public static class MemoryRef {

        // from java.nio.DirectByteBuffer$MemoryRef
        public byte[] buffer;
        public long allocatedAddress;
        public final int offset;
        public boolean isAccessible;
        public boolean isFreed;

        // use the "originalBufferObject" field from java.nio.DirectByteBuffer
        // if it exists, else generate it
        public final Object originalBufferObject;

        public MemoryRef(long address, Object originalBufferObject) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void free() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    // from java.nio.MappedByteBuffer
    public final FileDescriptor fd;

    public final Cleaner cleaner;
    public final MemoryRef memoryRef;

    public DirectByteBuffer(MemoryRef memoryRef,
                            int mark, int pos, int lim, int cap,
                            int off, boolean isReadOnly) {
        throw new UnsupportedOperationException("Stub!");
    }

    public Object attachment() {
        throw new UnsupportedOperationException("Stub!");
    }

    public Cleaner cleaner() {
        throw new UnsupportedOperationException("Stub!");
    }

    public long address() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public abstract DirectByteBuffer slice();

    @Override
    public abstract DirectByteBuffer slice(int index, int length);

    @Override
    public abstract DirectByteBuffer duplicate();

    @Override
    public DirectByteBuffer compact() {
        throw new UnsupportedOperationException("Stub!");
    }
}

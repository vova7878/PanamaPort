package com.v7878.unsafe;

import java.io.FileDescriptor;
import java.nio.MappedByteBuffer;

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class DirectByteBuffer extends MappedByteBufferBase {
    public static class MemoryRef {

        // from java.nio.DirectByteBuffer$MemoryRef
        public byte[] buffer;
        public long allocatedAddress;
        public final int offset;
        public boolean isAccessible;
        public boolean isFreed;

        // Will be generated if does not exist
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

    public DirectByteBuffer(MemoryRef memoryRef,
                            int mark, int pos, int lim, int cap,
                            int off, boolean isReadOnly) {
        throw new UnsupportedOperationException("Stub!");
    }

    public MemoryRef attachment() {
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
    public abstract DirectByteBuffer asReadOnlyBuffer();

    // Runtime generated methods to bypass presence of the final modifier

    protected abstract boolean isLoadedImpl();

    protected abstract MappedByteBuffer loadImpl();

    protected abstract MappedByteBuffer forceImpl();

    protected abstract MappedByteBuffer forceImpl(int index, int length);

    protected final boolean isLoadedSuper() {
        throw new UnsupportedOperationException("Stub!");
    }

    protected final MappedByteBuffer loadSuper() {
        throw new UnsupportedOperationException("Stub!");
    }

    protected final MappedByteBuffer forceSuper() {
        throw new UnsupportedOperationException("Stub!");
    }

    protected final MappedByteBuffer forceSuper(int index, int length) {
        throw new UnsupportedOperationException("Stub!");
    }
}

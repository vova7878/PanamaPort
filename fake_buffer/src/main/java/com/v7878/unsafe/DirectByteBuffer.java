package com.v7878.unsafe;

import java.io.FileDescriptor;
import java.nio.DirectByteBuffer$MemoryRef;

import sun.misc.Cleaner;

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class DirectByteBuffer extends CommonByteBuffer {
    public static class MemoryRef extends DirectByteBuffer$MemoryRef {

        public MemoryRef(long address) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    // from java.nio.MappedByteBuffer
    public final FileDescriptor fd;

    public final Cleaner cleaner;
    public final DirectByteBuffer$MemoryRef memoryRef;

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

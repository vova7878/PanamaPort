package com.v7878.unsafe;

import static com.v7878.unsafe.DexFileUtils.getDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;

import androidx.annotation.Keep;

import com.v7878.misc.Checks;

import java.lang.foreign.MemorySegment.Scope;
import java.nio.ByteBuffer;

class DirectSegmentByteBuffer extends DirectByteBuffer {

    static {
        setTrusted(getDexFile(DirectSegmentByteBuffer.class));
    }

    static class SegmentMemoryRef extends MemoryRef {

        @Keep
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        //TODO: use the "originalBufferObject" field if it exists, else generate it
        private final Object att;

        public SegmentMemoryRef(long allocatedAddress, Object obj) {
            super(allocatedAddress);
            this.att = obj;
        }
    }

    public final Scope scope;

    DirectSegmentByteBuffer(SegmentMemoryRef memoryRef, int mark, int pos, int lim,
                            int cap, int off, boolean isReadOnly, Scope scope) {
        super(memoryRef, mark, pos, lim, cap, off, isReadOnly);
        this.scope = scope;
    }

    @Override
    public final DirectByteBuffer slice() {
        if (!memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        int pos = position();
        int lim = limit();
        int rem = lim - pos;
        int off = pos + offset;
        return new DirectSegmentByteBuffer((SegmentMemoryRef) memoryRef,
                -1, 0, rem, rem, off, isReadOnly, scope);
    }

    @Override
    public final DirectByteBuffer slice(int index, int length) {
        if (!memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Checks.checkFromIndexSize(index, length, limit());
        return new DirectSegmentByteBuffer((SegmentMemoryRef) memoryRef,
                -1, 0, length, length, index, isReadOnly, scope);
    }

    @Override
    public final DirectByteBuffer duplicate() {
        if (memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        return new DirectSegmentByteBuffer((SegmentMemoryRef) memoryRef, markValue(),
                position(), limit(), capacity(), offset, isReadOnly, scope);
    }

    @Override
    public final ByteBuffer asReadOnlyBuffer() {
        if (memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        return new DirectSegmentByteBuffer((SegmentMemoryRef) memoryRef, markValue(),
                position(), limit(), capacity(), offset, true, scope);
    }

    //TODO: lock the scope while performing manipulations
}

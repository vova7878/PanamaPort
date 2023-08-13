package com.v7878.unsafe;

import static com.v7878.unsafe.DexFileUtils.getDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;

import androidx.annotation.Keep;

import java.lang.foreign.MemorySegment.Scope;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

class SegmentByteBuffer extends DirectByteBuffer {

    static {
        setTrusted(getDexFile(SegmentByteBuffer.class));
    }

    static class SegmentMemoryRef extends MemoryRef {

        @Keep
        @SuppressWarnings("unused")
        //TODO: use the "originalBufferObject" field if it exists, else generate it
        private final Object att;
        final Scope scope;

        public SegmentMemoryRef(long allocatedAddress, Object obj, Scope scope) {
            super(allocatedAddress);
            this.att = obj;
            this.scope = scope;
        }
    }

    SegmentByteBuffer(SegmentMemoryRef memoryRef, int mark, int pos, int lim, int cap, int off, boolean isReadOnly) {
        super(memoryRef, mark, pos, lim, cap, off, isReadOnly);
    }

    @Override
    public final MappedByteBuffer slice() {
            /*if (!memoryRef.isAccessible) {
                throw new IllegalStateException("buffer is inaccessible");
            }
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            int off = pos + offset;
            assert (off >= 0);
            return new SegmentByteBuffer(memoryRef, -1, 0, rem, rem, off, isReadOnly);*/
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public final MappedByteBuffer slice(int index, int length) {
            /*if (!memoryRef.isAccessible) {
                throw new IllegalStateException("buffer is inaccessible");
            }
            Objects.checkFromIndexSize(index, length, limit());
            return new SegmentByteBuffer(memoryRef,
                    -1,
                    0,
                    length,
                    length,
                    index << 0,
                    isReadOnly);*/
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public final MappedByteBuffer duplicate() {
            /*if (memoryRef.isFreed) {
                throw new IllegalStateException("buffer has been freed");
            }
            return new SegmentByteBuffer(memoryRef,
                    this.markValue(),
                    this.position(),
                    this.limit(),
                    this.capacity(),
                    offset,
                    isReadOnly);*/
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public final ByteBuffer asReadOnlyBuffer() {
            /*if (memoryRef.isFreed) {
                throw new IllegalStateException("buffer has been freed");
            }
            return new SegmentByteBuffer(memoryRef,
                    this.markValue(),
                    this.position(),
                    this.limit(),
                    this.capacity(),
                    offset,
                    true);*/
        throw new UnsupportedOperationException("Not supported yet");
    }

    //TODO: lock the scope while performing manipulations
}

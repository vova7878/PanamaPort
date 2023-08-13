package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

class SegmentByteBuffer extends DirectByteBuffer {

    SegmentByteBuffer(MemoryRef memoryRef, int mark, int pos, int lim, int cap, int off, boolean isReadOnly) {
        super(memoryRef, mark, pos, lim, cap, off, isReadOnly);
    }

    @Override
    public final MappedByteBuffer slice() {
        attachment();
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
}

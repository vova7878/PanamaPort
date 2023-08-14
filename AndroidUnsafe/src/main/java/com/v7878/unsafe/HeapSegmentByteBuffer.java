package com.v7878.unsafe;

import static com.v7878.unsafe.DexFileUtils.getDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;

import com.v7878.misc.Checks;

import java.lang.foreign.MemorySegment.Scope;
import java.nio.ByteBuffer;

public class HeapSegmentByteBuffer extends HeapByteBuffer {

    static {
        setTrusted(getDexFile(HeapSegmentByteBuffer.class));
    }

    public final Scope scope;

    public HeapSegmentByteBuffer(byte[] buf, int mark, int pos, int lim, int cap,
                                 int off, boolean isReadOnly, Scope scope) {
        super(buf, mark, pos, lim, cap, off, isReadOnly);
        this.scope = scope;
    }

    @Override
    public HeapByteBuffer slice() {
        return new HeapSegmentByteBuffer(hb, -1, 0, remaining(),
                remaining(), position() + offset, isReadOnly, scope);
    }

    @Override
    public HeapByteBuffer slice(int index, int length) {
        Checks.checkFromIndexSize(index, length, limit());
        return new HeapSegmentByteBuffer(hb, -1, 0,
                length, length, index + offset, isReadOnly, scope);
    }

    @Override
    public HeapByteBuffer duplicate() {
        return new HeapSegmentByteBuffer(hb, markValue(), position(), limit(),
                capacity(), offset, isReadOnly, scope);
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        return new HeapSegmentByteBuffer(hb, markValue(), position(), limit(),
                capacity(), offset, true, scope);
    }

    //TODO: lock the scope while performing manipulations
}

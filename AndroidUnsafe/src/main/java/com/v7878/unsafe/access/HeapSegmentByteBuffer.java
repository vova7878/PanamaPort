package com.v7878.unsafe.access;

import static com.v7878.unsafe.access.JavaForeignAccess.lock;

import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.unsafe.HeapByteBuffer;
import com.v7878.unsafe.Utils.FineClosable;

import java.nio.ByteBuffer;
import java.util.Objects;

public class HeapSegmentByteBuffer extends HeapByteBuffer {
    public final Scope scope;

    public HeapSegmentByteBuffer(byte[] buf, int mark, int pos, int lim, int cap,
                                 int off, boolean isReadOnly, Scope scope) {
        super(buf, mark, pos, lim, cap, off, isReadOnly);
        this.scope = scope;
    }

    @Override
    public HeapSegmentByteBuffer slice() {
        int pos = position();
        int lim = Math.max(limit(), pos);
        int rem = lim - pos;
        int off = pos + offset;
        return new HeapSegmentByteBuffer(hb, -1, 0, rem,
                rem, off, isReadOnly, scope);
    }

    @Override
    public HeapSegmentByteBuffer slice(int index, int length) {
        Objects.checkFromIndexSize(index, length, limit());
        return new HeapSegmentByteBuffer(hb, -1, 0,
                length, length, index + offset, isReadOnly, scope);
    }

    @Override
    public HeapSegmentByteBuffer duplicate() {
        return new HeapSegmentByteBuffer(hb, markValue(), position(), limit(),
                capacity(), offset, isReadOnly, scope);
    }

    @Override
    public HeapSegmentByteBuffer asReadOnlyBuffer() {
        return new HeapSegmentByteBuffer(hb, markValue(), position(), limit(),
                capacity(), offset, true, scope);
    }

    @Override
    public byte get() {
        try (FineClosable ignored = lock(scope)) {
            return super.get();
        }
    }

    @Override
    public byte get(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.get(i);
        }
    }

    @Override
    public ByteBuffer get(byte[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            return super.get(dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer put(ByteBuffer src) {
        try (FineClosable ignored = lock(scope)) {
            return super.put(src);
        }
    }

    @Override
    public ByteBuffer put(byte x) {
        try (FineClosable ignored = lock(scope)) {
            return super.put(x);
        }
    }

    @Override
    public ByteBuffer put(int i, byte x) {
        try (FineClosable ignored = lock(scope)) {
            return super.put(i, x);
        }
    }

    @Override
    public ByteBuffer put(byte[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            return super.put(src, srcOffset, length);
        }
    }

    @Override
    public byte _get(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super._get(i);
        }
    }

    @Override
    public void _put(int i, byte b) {
        try (FineClosable ignored = lock(scope)) {
            super._put(i, b);
        }
    }

    @Override
    public char getChar() {
        try (FineClosable ignored = lock(scope)) {
            return super.getChar();
        }
    }

    @Override
    public char getChar(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getChar(i);
        }
    }

    @Override
    public char getCharUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getCharUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, char[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putChar(char x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putChar(x);
        }
    }

    @Override
    public ByteBuffer putChar(int i, char x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putChar(i, x);
        }
    }

    @Override
    public void putCharUnchecked(int i, char x) {
        try (FineClosable ignored = lock(scope)) {
            super.putCharUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, char[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }

    @Override
    public short getShort() {
        try (FineClosable ignored = lock(scope)) {
            return super.getShort();
        }
    }

    @Override
    public short getShort(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getShort(i);
        }
    }

    @Override
    public short getShortUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getShortUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, short[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putShort(short x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putShort(x);
        }
    }

    @Override
    public ByteBuffer putShort(int i, short x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putShort(i, x);
        }
    }

    @Override
    public void putShortUnchecked(int i, short x) {
        try (FineClosable ignored = lock(scope)) {
            super.putShortUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, short[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }

    @Override
    public int getInt() {
        try (FineClosable ignored = lock(scope)) {
            return super.getInt();
        }
    }

    @Override
    public int getInt(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getInt(i);
        }
    }

    @Override
    public int getIntUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getIntUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, int[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putInt(int x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putInt(x);
        }
    }

    @Override
    public ByteBuffer putInt(int i, int x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putInt(i, x);
        }
    }

    @Override
    public void putIntUnchecked(int i, int x) {
        try (FineClosable ignored = lock(scope)) {
            super.putIntUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, int[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }

    @Override
    public long getLong() {
        try (FineClosable ignored = lock(scope)) {
            return super.getLong();
        }
    }

    @Override
    public long getLong(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getLong(i);
        }
    }

    @Override
    public long getLongUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getLongUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, long[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putLong(long x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putLong(x);
        }
    }

    @Override
    public ByteBuffer putLong(int i, long x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putLong(i, x);
        }
    }

    @Override
    public void putLongUnchecked(int i, long x) {
        try (FineClosable ignored = lock(scope)) {
            super.putLongUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, long[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }

    @Override
    public float getFloat() {
        try (FineClosable ignored = lock(scope)) {
            return super.getFloat();
        }
    }

    @Override
    public float getFloat(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getFloat(i);
        }
    }

    @Override
    public float getFloatUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getFloatUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, float[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putFloat(float x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putFloat(x);
        }
    }

    @Override
    public ByteBuffer putFloat(int i, float x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putFloat(i, x);
        }
    }

    @Override
    public void putFloatUnchecked(int i, float x) {
        try (FineClosable ignored = lock(scope)) {
            super.putFloatUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, float[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }

    @Override
    public double getDouble() {
        try (FineClosable ignored = lock(scope)) {
            return super.getDouble();
        }
    }

    @Override
    public double getDouble(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getDouble(i);
        }
    }

    @Override
    public double getDoubleUnchecked(int i) {
        try (FineClosable ignored = lock(scope)) {
            return super.getDoubleUnchecked(i);
        }
    }

    @Override
    public void getUnchecked(int pos, double[] dst, int dstOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.getUnchecked(pos, dst, dstOffset, length);
        }
    }

    @Override
    public ByteBuffer putDouble(double x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putDouble(x);
        }
    }

    @Override
    public ByteBuffer putDouble(int i, double x) {
        try (FineClosable ignored = lock(scope)) {
            return super.putDouble(i, x);
        }
    }

    @Override
    public void putDoubleUnchecked(int i, double x) {
        try (FineClosable ignored = lock(scope)) {
            super.putDoubleUnchecked(i, x);
        }
    }

    @Override
    public void putUnchecked(int pos, double[] src, int srcOffset, int length) {
        try (FineClosable ignored = lock(scope)) {
            super.putUnchecked(pos, src, srcOffset, length);
        }
    }
}

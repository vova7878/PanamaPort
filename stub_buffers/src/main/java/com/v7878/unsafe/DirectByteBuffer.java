package com.v7878.unsafe;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class DirectByteBuffer extends FakeMappedByteBuffer {

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

    // from java.nio.MappedByteBuffer

    public final FileDescriptor fd;

    // from java.nio.Buffer

    public int mark;
    public int position;
    public int limit;
    public int capacity;
    public long address;

    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean isDirect() {
        throw new UnsupportedOperationException("Stub!");
    }

    // from java.nio.ByteBuffer

    public byte[] hb;
    public int offset;
    public boolean isReadOnly;

    public boolean bigEndian;
    public boolean nativeByteOrder;

    public ByteBuffer put(ByteBuffer src) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer get(int index, byte[] dst, int offset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(int index, byte[] src, int offset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public CharBuffer asCharBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public ShortBuffer asShortBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public IntBuffer asIntBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public LongBuffer asLongBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public FloatBuffer asFloatBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public DoubleBuffer asDoubleBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    // from java.nio.DirectByteBuffer

    public abstract MappedByteBuffer slice();

    public abstract MappedByteBuffer slice(int index, int length);

    public abstract MappedByteBuffer duplicate();

    public abstract MappedByteBuffer asReadOnlyBuffer();

    public MappedByteBuffer compact() {
        throw new UnsupportedOperationException("Stub!");
    }

    public byte get() {
        throw new UnsupportedOperationException("Stub!");
    }

    public byte get(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(int i, byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public byte _get(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void _put(int i, byte b) {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getChar() {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getChar(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putChar(char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getCharUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, char[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putChar(int i, char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putCharUnchecked(int i, char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, char[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public short getShort() {
        throw new UnsupportedOperationException("Stub!");
    }

    public short getShort(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public short getShortUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, short[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putShort(short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putShort(int i, short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putShortUnchecked(int i, short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, short[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public int getInt() {
        throw new UnsupportedOperationException("Stub!");
    }

    public int getInt(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public int getIntUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, int[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putInt(int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putInt(int i, int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putIntUnchecked(int i, int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, int[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public long getLong() {
        throw new UnsupportedOperationException("Stub!");
    }

    public long getLong(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public long getLongUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, long[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putLong(long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putLong(int i, long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putLongUnchecked(int i, long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, long[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public float getFloat() {
        throw new UnsupportedOperationException("Stub!");
    }

    public float getFloat(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public float getFloatUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, float[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putFloat(float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putFloat(int i, float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putFloatUnchecked(int i, float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, float[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public double getDouble() {
        throw new UnsupportedOperationException("Stub!");
    }

    public double getDouble(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public double getDoubleUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, double[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putDouble(double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putDouble(int i, double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putDoubleUnchecked(int i, double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, double[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }
}

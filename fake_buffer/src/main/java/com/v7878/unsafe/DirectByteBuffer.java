package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;

import sun.misc.Cleaner;

//TODO: jasmin gradle plugin?

/* ByteBuffer is
.class public Lcom/v7878/unsafe/FakeByteBuffer;
.super Ljava/nio/MappedByteBuffer;

.method public constructor <init>()V
    return-void
.end method
*/

// Compile-time stub, real DirectByteBuffer will be generated at runtime
public abstract class DirectByteBuffer extends FakeByteBuffer {
    public static class MemoryRef {

        public MemoryRef(long allocatedAddress) {
        }

        public void free() {
        }
    }

    public DirectByteBuffer(MemoryRef memoryRef,
                            int mark, int pos, int lim, int cap,
                            int off, boolean isReadOnly) {
    }

    public Object attachment() {
        throw new UnsupportedOperationException("Stub!");
    }

    public Cleaner cleaner() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public abstract MappedByteBuffer slice();

    public abstract MappedByteBuffer slice(int index, int length);

    @Override
    public abstract MappedByteBuffer duplicate();

    @Override
    public abstract ByteBuffer asReadOnlyBuffer();

    public long address() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public byte get() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public byte get(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer get(byte[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer put(ByteBuffer src) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer put(byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer put(int i, byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer put(byte[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public MappedByteBuffer compact() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public boolean isDirect() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Stub!");
    }

    // Used by java.nio.Bits
    public byte _get(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    // Used by java.nio.Bits
    public void _put(int i, byte b) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public char getChar() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public char getChar(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getCharUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, char[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putChar(char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putChar(int i, char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putCharUnchecked(int i, char x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, char[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public CharBuffer asCharBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public short getShort() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public short getShort(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public short getShortUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, short[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putShort(short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putShort(int i, short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putShortUnchecked(int i, short x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, short[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ShortBuffer asShortBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public int getInt() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public int getInt(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public int getIntUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, int[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putInt(int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putInt(int i, int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putIntUnchecked(int i, int x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, int[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public IntBuffer asIntBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public long getLong(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public long getLongUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, long[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putLong(long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putLong(int i, long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putLongUnchecked(int i, long x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, long[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public LongBuffer asLongBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public float getFloat() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public float getFloat(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public float getFloatUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, float[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putFloat(float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putFloat(int i, float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putFloatUnchecked(int i, float x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, float[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public double getDouble() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public double getDouble(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public double getDoubleUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, double[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putDouble(double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public ByteBuffer putDouble(int i, double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putDoubleUnchecked(int i, double x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void putUnchecked(int pos, double[] src, int srcOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean isAccessible() {
        throw new UnsupportedOperationException("Stub!");
    }

    public void setAccessible(boolean value) {
        throw new UnsupportedOperationException("Stub!");
    }
}

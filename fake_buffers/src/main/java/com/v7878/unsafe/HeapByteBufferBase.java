package com.v7878.unsafe;

//TODO: generate with jasmin gradle plugin?

/* FakeHeapByteBuffer is
.class public Lcom/v7878/unsafe/FakeHeapByteBuffer;
.super Ljava/nio/ByteBuffer;

.method public constructor <init>()V
    return-void
.end method
*/

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public abstract class HeapByteBufferBase extends FakeHeapByteBuffer {
    public HeapByteBufferBase() {
        throw new UnsupportedOperationException("Stub!");
    }

    // from java.nio.Buffer
    public long address;

    // from java.nio.ByteBuffer
    public final byte[] hb;
    public final int offset;
    public boolean isReadOnly;

    public abstract ByteBuffer slice();

    public abstract ByteBuffer slice(int index, int length);

    public abstract ByteBuffer duplicate();

    public abstract ByteBuffer asReadOnlyBuffer();

    public ByteBuffer compact() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean isDirect() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean isAccessible() {
        throw new UnsupportedOperationException("Stub!");
    }

    public void setAccessible(boolean value) {
        throw new UnsupportedOperationException("Stub!");
    }

    // from java.nio.Buffer
    public int markValue() {
        throw new UnsupportedOperationException("Stub!");
    }

    public byte get() {
        throw new UnsupportedOperationException("Stub!");
    }

    public byte get(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer get(byte[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(ByteBuffer src) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(int i, byte x) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer put(byte[] src, int srcOffset, int length) {
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

    public char getChar() {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getChar(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public char getCharUnchecked(int i) {
        throw new UnsupportedOperationException("Stub!");
    }

    public void getUnchecked(int pos, char[] dst, int dstOffset, int length) {
        throw new UnsupportedOperationException("Stub!");
    }

    public ByteBuffer putChar(char x) {
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

    public CharBuffer asCharBuffer() {
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

    public ShortBuffer asShortBuffer() {
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

    public IntBuffer asIntBuffer() {
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

    public LongBuffer asLongBuffer() {
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

    public FloatBuffer asFloatBuffer() {
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

    public DoubleBuffer asDoubleBuffer() {
        throw new UnsupportedOperationException("Stub!");
    }
}

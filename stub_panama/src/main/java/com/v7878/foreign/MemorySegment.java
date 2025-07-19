/*
 * Copyright (c) 2024 Vladimir Kozelkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.OfBoolean;
import static com.v7878.foreign.ValueLayout.OfByte;
import static com.v7878.foreign.ValueLayout.OfChar;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfFloat;
import static com.v7878.foreign.ValueLayout.OfInt;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.foreign.ValueLayout.OfShort;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface MemorySegment {

    long address();

    long nativeAddress();

    Optional<Object> heapBase();

    Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout);

    Stream<MemorySegment> elements(MemoryLayout elementLayout);

    Scope scope();

    boolean isAccessibleBy(Thread thread);

    long byteSize();

    MemorySegment asSlice(long offset, long newSize);

    MemorySegment asSlice(long offset, long newSize, long byteAlignment);

    MemorySegment asSlice(long offset, MemoryLayout layout);

    MemorySegment asSlice(long offset);

    MemorySegment reinterpret(long newSize);

    MemorySegment reinterpret(Arena arena, Consumer<MemorySegment> cleanup);

    MemorySegment reinterpret(long newSize,
                              Arena arena,
                              Consumer<MemorySegment> cleanup);

    boolean isReadOnly();

    MemorySegment asReadOnly();

    boolean isNative();

    boolean isMapped();

    Optional<MemorySegment> asOverlappingSlice(MemorySegment other);

    MemorySegment fill(byte value);

    MemorySegment copyFrom(MemorySegment src);

    long mismatch(MemorySegment other);

    boolean isLoaded();

    void load();

    void unload();

    void force();

    ByteBuffer asByteBuffer();

    byte[] toArray(OfByte elementLayout);

    short[] toArray(OfShort elementLayout);

    char[] toArray(OfChar elementLayout);

    int[] toArray(OfInt elementLayout);

    float[] toArray(OfFloat elementLayout);

    long[] toArray(OfLong elementLayout);

    double[] toArray(OfDouble elementLayout);

    String getString(long offset);

    String getString(long offset, Charset charset);

    void setString(long offset, String str);

    void setString(long offset, String str, Charset charset);

    static MemorySegment ofBuffer(Buffer buffer) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(byte[] byteArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(char[] charArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(short[] shortArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(int[] intArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(float[] floatArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(long[] longArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(double[] doubleArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    MemorySegment NULL = MemorySegment.ofAddress(0L);

    static MemorySegment ofAddress(long address) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(MemorySegment srcSegment, long srcOffset,
                     MemorySegment dstSegment, long dstOffset, long bytes) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset,
                     MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset,
                     long elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    byte get(OfByte layout, long offset);

    void set(OfByte layout, long offset, byte value);

    boolean get(OfBoolean layout, long offset);

    void set(OfBoolean layout, long offset, boolean value);

    char get(OfChar layout, long offset);

    void set(OfChar layout, long offset, char value);

    short get(OfShort layout, long offset);

    void set(OfShort layout, long offset, short value);

    int get(OfInt layout, long offset);

    void set(OfInt layout, long offset, int value);

    float get(OfFloat layout, long offset);

    void set(OfFloat layout, long offset, float value);

    long get(OfLong layout, long offset);

    void set(OfLong layout, long offset, long value);

    double get(OfDouble layout, long offset);

    void set(OfDouble layout, long offset, double value);

    MemorySegment get(AddressLayout layout, long offset);

    void set(AddressLayout layout, long offset, MemorySegment value);

    byte getAtIndex(OfByte layout, long index);

    boolean getAtIndex(OfBoolean layout, long index);

    char getAtIndex(OfChar layout, long index);

    void setAtIndex(OfChar layout, long index, char value);

    short getAtIndex(OfShort layout, long index);

    void setAtIndex(OfByte layout, long index, byte value);

    void setAtIndex(OfBoolean layout, long index, boolean value);

    void setAtIndex(OfShort layout, long index, short value);

    int getAtIndex(OfInt layout, long index);

    void setAtIndex(OfInt layout, long index, int value);

    float getAtIndex(OfFloat layout, long index);

    void setAtIndex(OfFloat layout, long index, float value);

    long getAtIndex(OfLong layout, long index);

    void setAtIndex(OfLong layout, long index, long value);

    double getAtIndex(OfDouble layout, long index);

    void setAtIndex(OfDouble layout, long index, double value);

    MemorySegment getAtIndex(AddressLayout layout, long index);

    void setAtIndex(AddressLayout layout, long index, MemorySegment value);

    @Override
    boolean equals(Object that);

    @Override
    int hashCode();

    static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
                     Object dstArray, int dstIndex,
                     int elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(Object srcArray, int srcIndex,
                     MemorySegment dstSegment, ValueLayout dstLayout, long dstOffset,
                     int elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    static long mismatch(MemorySegment srcSegment, long srcFromOffset, long srcToOffset,
                         MemorySegment dstSegment, long dstFromOffset, long dstToOffset) {
        throw new UnsupportedOperationException("Stub!");
    }

    interface Scope {
        boolean isAlive();

        @Override
        boolean equals(Object that);

        @Override
        int hashCode();
    }
}

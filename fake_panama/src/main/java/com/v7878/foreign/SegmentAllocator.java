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

import static com.v7878.foreign.ValueLayout.OfByte;
import static com.v7878.foreign.ValueLayout.OfChar;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfFloat;
import static com.v7878.foreign.ValueLayout.OfInt;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.foreign.ValueLayout.OfShort;

import java.nio.charset.Charset;

@FunctionalInterface
public interface SegmentAllocator {

    default MemorySegment allocateFrom(String str) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(String str, Charset charset) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfByte layout, byte value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfChar layout, char value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfShort layout, short value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfInt layout, int value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfFloat layout, float value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfLong layout, long value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfDouble layout, double value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(AddressLayout layout, MemorySegment value) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(ValueLayout elementLayout,
                                       MemorySegment source,
                                       ValueLayout sourceElementLayout,
                                       long sourceOffset,
                                       long elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfByte elementLayout, byte... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfShort elementLayout, short... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfChar elementLayout, char... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfInt elementLayout, int... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfFloat elementLayout, float... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfLong elementLayout, long... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocateFrom(OfDouble elementLayout, double... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocate(MemoryLayout layout) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocate(MemoryLayout elementLayout, long count) {
        throw new UnsupportedOperationException("Stub!");
    }

    default MemorySegment allocate(long byteSize) {
        throw new UnsupportedOperationException("Stub!");
    }

    MemorySegment allocate(long byteSize, long byteAlignment);

    static SegmentAllocator slicingAllocator(MemorySegment segment) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SegmentAllocator prefixAllocator(MemorySegment segment) {
        throw new UnsupportedOperationException("Stub!");
    }
}

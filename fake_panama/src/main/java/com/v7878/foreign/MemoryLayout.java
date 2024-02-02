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

import java.lang.invoke.MethodHandle;
import java.util.Optional;

public interface MemoryLayout {
    long byteSize();

    Optional<String> name();

    MemoryLayout withName(String name);

    MemoryLayout withoutName();

    long byteAlignment();

    MemoryLayout withByteAlignment(long byteAlignment);

    long scale(long offset, long index);

    MethodHandle scaleHandle();

    long byteOffset(PathElement... elements);

    MethodHandle byteOffsetHandle(PathElement... elements);

    VarHandle varHandle(PathElement... elements);

    VarHandle arrayElementVarHandle(PathElement... elements);

    MethodHandle sliceHandle(PathElement... elements);

    MemoryLayout select(PathElement... elements);

    interface PathElement {

        static PathElement groupElement(String name) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement groupElement(long index) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement(long index) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement(long start, long step) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement() {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement dereferenceElement() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    boolean equals(Object other);

    int hashCode();

    @Override
    String toString();

    static PaddingLayout paddingLayout(long byteSize) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SequenceLayout sequenceLayout(long elementCount, MemoryLayout elementLayout) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SequenceLayout sequenceLayout(MemoryLayout elementLayout) {
        throw new UnsupportedOperationException("Stub!");
    }

    static StructLayout structLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    static StructLayout paddedStructLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    static UnionLayout unionLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }
}

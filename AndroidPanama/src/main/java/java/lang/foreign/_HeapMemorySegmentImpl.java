/*
 *  Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package java.lang.foreign;

import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.access.JavaNioAccess;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation for heap memory segments. A heap memory segment is composed by an offset and
 * a base object (typically an array). To enhance performances, the access to the base object needs to feature
 * sharp type information, as well as sharp null-check information. For this reason, many concrete subclasses
 * of {@link _HeapMemorySegmentImpl} are defined (e.g. {@link OfFloat}, so that each subclass can override the
 * {@link _HeapMemorySegmentImpl#unsafeGetBase()} method so that it returns an array of the correct (sharp) type. Note that
 * the field type storing the 'base' coordinate is just Object; similarly, all the constructor in the subclasses
 * accept an Object 'base' parameter instead of a sharper type (e.g. {@code byte[]}). This is deliberate, as
 * using sharper types would require use of type-conversions, which in turn would inhibit some C2 optimizations,
 * such as the elimination of store barriers in methods like {@link _HeapMemorySegmentImpl#dup(long, long, boolean, _MemorySessionImpl)}.
 */
abstract sealed class _HeapMemorySegmentImpl extends _AbstractMemorySegmentImpl {

    // Port-changed: use AndroidUnsafe
    //private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int BYTE_ARR_BASE = AndroidUnsafe.arrayBaseOffset(byte[].class);

    private static final long MAX_ALIGN_1 = ValueLayout.JAVA_BYTE.byteAlignment();
    private static final long MAX_ALIGN_2 = ValueLayout.JAVA_SHORT.byteAlignment();
    private static final long MAX_ALIGN_4 = ValueLayout.JAVA_INT.byteAlignment();
    private static final long MAX_ALIGN_8 = ValueLayout.JAVA_LONG.byteAlignment();

    final long offset;
    final Object base;

    @Override
    public Optional<Object> heapBase() {
        return readOnly ?
                Optional.empty() :
                Optional.of(base);
    }

    _HeapMemorySegmentImpl(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
        super(length, readOnly, session);
        this.offset = offset;
        this.base = base;
    }

    @Override
    public long unsafeGetOffset() {
        return offset;
    }

    @Override
    abstract _HeapMemorySegmentImpl dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope);

    @Override
    ByteBuffer makeByteBuffer() {
        if (!(base instanceof byte[] baseByte)) {
            // TODO: allow it?
            throw new UnsupportedOperationException("Not an address to an heap-allocated byte array");
        }
        // Port-changed: different JavaNioAccess.newHeapByteBuffer implementation
        return JavaNioAccess.newHeapByteBuffer(baseByte, (int) offset - BYTE_ARR_BASE, (int) byteSize(), scope);
    }

    // factories

    public static final class OfByte extends _HeapMemorySegmentImpl {

        OfByte(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfByte dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfByte(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public byte[] unsafeGetBase() {
            return (byte[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(byte[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_BYTE_INDEX_SCALE;
            return new OfByte(AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_1;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
        }
    }

    public static final class OfChar extends _HeapMemorySegmentImpl {

        OfChar(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfChar dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfChar(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public char[] unsafeGetBase() {
            return (char[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(char[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_CHAR_INDEX_SCALE;
            return new OfChar(AndroidUnsafe.ARRAY_CHAR_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_2;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_CHAR_BASE_OFFSET;
        }
    }

    public static final class OfShort extends _HeapMemorySegmentImpl {

        OfShort(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfShort dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfShort(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public short[] unsafeGetBase() {
            return (short[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(short[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_SHORT_INDEX_SCALE;
            return new OfShort(AndroidUnsafe.ARRAY_SHORT_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_2;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_SHORT_BASE_OFFSET;
        }
    }

    public static final class OfInt extends _HeapMemorySegmentImpl {

        OfInt(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfInt dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfInt(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public int[] unsafeGetBase() {
            return (int[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(int[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_INT_INDEX_SCALE;
            return new OfInt(AndroidUnsafe.ARRAY_INT_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_4;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
        }
    }

    public static final class OfLong extends _HeapMemorySegmentImpl {

        OfLong(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfLong dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfLong(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public long[] unsafeGetBase() {
            return (long[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(long[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_LONG_INDEX_SCALE;
            return new OfLong(AndroidUnsafe.ARRAY_LONG_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_8;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_LONG_BASE_OFFSET;
        }
    }

    public static final class OfFloat extends _HeapMemorySegmentImpl {

        OfFloat(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfFloat dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfFloat(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public float[] unsafeGetBase() {
            return (float[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(float[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_FLOAT_INDEX_SCALE;
            return new OfFloat(AndroidUnsafe.ARRAY_FLOAT_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_4;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_FLOAT_BASE_OFFSET;
        }
    }

    public static final class OfDouble extends _HeapMemorySegmentImpl {

        OfDouble(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfDouble dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfDouble(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public double[] unsafeGetBase() {
            return (double[]) Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(double[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long) arr.length * AndroidUnsafe.ARRAY_DOUBLE_INDEX_SCALE;
            return new OfDouble(AndroidUnsafe.ARRAY_DOUBLE_BASE_OFFSET, arr, byteSize, false,
                    _MemorySessionImpl.heapSession(arr));
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_8;
        }

        @Override
        public long address() {
            return offset - AndroidUnsafe.ARRAY_DOUBLE_BASE_OFFSET;
        }
    }

    //TODO: ofRawObject
}

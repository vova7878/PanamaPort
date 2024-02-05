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

// Port-changed: Extensive modifications made throughout the class for Android.

package com.v7878.foreign;

import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.JavaNioAccess;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation for heap memory segments. A heap memory segment is composed by an offset and
 * a base object (typically an array). To enhance performances, the access to the base object needs to feature
 * sharp type information, as well as sharp null-check information. For this reason, many concrete subclasses
 * of {@link _HeapMemorySegmentImpl} are defined (e.g. {@link OfFloat}), so that each subclass can override the
 * {@link _HeapMemorySegmentImpl#unsafeGetBase()} method so that it returns an array of the correct (sharp) type. Note that
 * the field type storing the 'base' coordinate is just Object; similarly, all the constructor in the subclasses
 * accept an Object 'base' parameter instead of a sharper type (e.g. {@code byte[]}). This is deliberate, as
 * using sharper types would require use of type-conversions, which in turn would inhibit some C2 optimizations,
 * such as the elimination of store barriers in methods like {@link _HeapMemorySegmentImpl#dup(long, long, boolean, _MemorySessionImpl)}.
 */
abstract sealed class _HeapMemorySegmentImpl extends _AbstractMemorySegmentImpl {

    private static final int BYTE_ARRAY_BASE = _Utils.BaseAndScale.BYTE.base();

    // Constants defining the maximum alignment supported by various kinds of heap arrays.
    // While for most arrays, the maximum alignment is constant (the size, in bytes, of the array elements),
    // note that the alignment of a long[]/double[] depends on the platform: it's 4-byte on x86, but 8 bytes on x64
    // (as specified by the JAVA_LONG layout constant).

    private static final long MAX_ALIGN_OBJECT = VM.OBJECT_ALIGNMENT;
    private static final long MAX_ALIGN_BYTE_ARRAY = ValueLayout.JAVA_BYTE.byteAlignment();
    private static final long MAX_ALIGN_SHORT_ARRAY = ValueLayout.JAVA_SHORT.byteAlignment();
    private static final long MAX_ALIGN_INT_ARRAY = ValueLayout.JAVA_INT.byteAlignment();
    private static final long MAX_ALIGN_LONG_ARRAY = ValueLayout.JAVA_LONG.byteAlignment();

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
        if (!(base instanceof byte[] baseByte && this instanceof OfByte)) {
            throw new UnsupportedOperationException("Not an address to an heap-allocated byte array");
        }
        return JavaNioAccess.newHeapByteBuffer(baseByte, (int) offset - BYTE_ARRAY_BASE, (int) byteSize(), scope);
    }

    // factories

    // Port-added
    public static final class OfObject extends _HeapMemorySegmentImpl {

        OfObject(long offset, Object base, long length, boolean readOnly, _MemorySessionImpl session) {
            super(offset, base, length, readOnly, session);
        }

        @Override
        OfObject dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope) {
            return new OfObject(this.offset + offset, base, size, readOnly, scope);
        }

        @Override
        public Object unsafeGetBase() {
            return Objects.requireNonNull(base);
        }

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_OBJECT;
        }

        @Override
        public long address() {
            return offset;
        }
    }

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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_BYTE_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.BYTE.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_SHORT_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.CHAR.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_SHORT_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.SHORT.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_INT_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.INT.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_LONG_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.LONG.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_INT_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.FLOAT.base();
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

        @Override
        public long maxAlignMask() {
            return MAX_ALIGN_LONG_ARRAY;
        }

        @Override
        public long address() {
            return offset - _Utils.BaseAndScale.DOUBLE.base();
        }
    }

}

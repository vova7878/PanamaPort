/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// Port-changed: Extensive modifications made throughout the class for Android.

package com.v7878.foreign;

import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import android.annotation.SuppressLint;

import com.v7878.unsafe.Utils;
import com.v7878.unsafe.access.JavaNioAccess;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This abstract class provides an immutable implementation for the {@code MemorySegment} interface. This class contains information
 * about the segment's spatial and temporal bounds; each memory segment implementation is associated with an owner thread which is set at creation time.
 * Access to certain sensitive operations on the memory segment will fail with {@code IllegalStateException} if the
 * segment is either in an invalid state (e.g. it has already been closed) or if access occurs from a thread other
 * than the owner thread. See {@link _MemorySessionImpl} for more details on management of temporal bounds. Subclasses
 * are defined for each memory segment kind, see {@link _NativeMemorySegmentImpl}, {@link _HeapMemorySegmentImpl} and
 * {@link _MappedMemorySegmentImpl}.
 */
abstract sealed class _AbstractMemorySegmentImpl
        implements MemorySegment, SegmentAllocator, BiFunction<String, List<Number>, RuntimeException>
        permits _HeapMemorySegmentImpl, _NativeMemorySegmentImpl {

    final long length;
    final boolean readOnly;
    final _MemorySessionImpl scope;

    _AbstractMemorySegmentImpl(long length, boolean readOnly, _MemorySessionImpl scope) {
        this.length = length;
        this.readOnly = readOnly;
        this.scope = scope;
    }

    abstract _AbstractMemorySegmentImpl dup(long offset, long size, boolean readOnly, _MemorySessionImpl scope);

    abstract ByteBuffer makeByteBuffer();

    @Override
    public final long nativeAddress() {
        if (!isNative()) throw new UnsupportedOperationException("Not a native segment");
        return address();
    }

    @Override
    public _AbstractMemorySegmentImpl asReadOnly() {
        return dup(0, length, true, scope);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public _AbstractMemorySegmentImpl asSlice(long offset, long newSize) {
        checkBounds(offset, newSize);
        return asSliceNoCheck(offset, newSize);
    }

    @Override
    public _AbstractMemorySegmentImpl asSlice(long offset) {
        checkBounds(offset, 0);
        return asSliceNoCheck(offset, length - offset);
    }

    @Override
    public MemorySegment asSlice(long offset, long newSize, long byteAlignment) {
        checkBounds(offset, newSize);
        _Utils.checkAlign(byteAlignment);

        if (!isAlignedForElement(offset, byteAlignment)) {
            throw new IllegalArgumentException("Target offset incompatible with alignment constraints");
        }
        return asSliceNoCheck(offset, newSize);
    }

    @Override
    public MemorySegment asSlice(long offset, MemoryLayout layout) {
        Objects.requireNonNull(layout);
        return asSlice(offset, layout.byteSize(), layout.byteAlignment());
    }

    @Override
    public final MemorySegment reinterpret(long newSize, Arena arena, Consumer<MemorySegment> cleanup) {
        Objects.requireNonNull(arena);
        return reinterpretInternal(newSize, _MemorySessionImpl.toMemorySession(arena), cleanup);
    }

    @Override
    public final MemorySegment reinterpret(long newSize) {
        return reinterpretInternal(newSize, scope, null);
    }

    @Override
    public final MemorySegment reinterpret(Arena arena, Consumer<MemorySegment> cleanup) {
        Objects.requireNonNull(arena);
        return reinterpretInternal(byteSize(), _MemorySessionImpl.toMemorySession(arena), cleanup);
    }

    public MemorySegment reinterpretInternal(long newSize, Scope scope, Consumer<MemorySegment> cleanup) {
        _Utils.checkNonNegativeArgument(newSize, "newSize");
        if (!isNative()) throw new UnsupportedOperationException("Not a native segment");
        Runnable action = cleanup == null ? null : () -> cleanup.accept(
                _SegmentFactories.makeNativeSegmentUnchecked(address(), newSize));
        return _SegmentFactories.makeNativeSegmentUnchecked(address(), newSize,
                (_MemorySessionImpl) scope, readOnly, action);
    }

    private _AbstractMemorySegmentImpl asSliceNoCheck(long offset, long newSize) {
        return dup(offset, newSize, readOnly, scope);
    }

    @Override
    public Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout) {
        Objects.requireNonNull(elementLayout);
        if (elementLayout.byteSize() == 0) {
            throw new IllegalArgumentException("Element layout size cannot be zero");
        }
        _Utils.checkElementAlignment(elementLayout, "Element layout size is not multiple of alignment");
        if (!isAlignedForElement(0, elementLayout)) {
            throw new IllegalArgumentException("Incompatible alignment constraints");
        }
        if ((byteSize() % elementLayout.byteSize()) != 0) {
            throw new IllegalArgumentException("Segment size is not a multiple of layout size");
        }
        return new SegmentSplitter(elementLayout.byteSize(), byteSize() / elementLayout.byteSize(),
                this);
    }

    @Override
    public Stream<MemorySegment> elements(MemoryLayout elementLayout) {
        return StreamSupport.stream(spliterator(elementLayout), false);
    }

    @Override
    public final MemorySegment fill(byte value) {
        return _SegmentBulkOperations.fill(this, value);
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        _Utils.checkAllocationSizeAndAlign(byteSize, byteAlignment);
        return asSlice(0, byteSize, byteAlignment);
    }

    @Override
    public final ByteBuffer asByteBuffer() {
        checkArraySize("ByteBuffer", 1);
        ByteBuffer _bb = makeByteBuffer();
        if (readOnly) {
            //session is IMMUTABLE - obtain a RO byte buffer
            _bb = _bb.asReadOnlyBuffer();
        }
        return _bb;
    }

    @Override
    public final long byteSize() {
        return length;
    }

    @Override
    public boolean isMapped() {
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public final Optional<MemorySegment> asOverlappingSlice(MemorySegment other) {
        final _AbstractMemorySegmentImpl that = (_AbstractMemorySegmentImpl) Objects.requireNonNull(other);
        if (overlaps(that)) {
            final long offsetToThat = that.address() - this.address();
            final long newOffset = offsetToThat >= 0 ? offsetToThat : 0;
            return Optional.of(asSlice(newOffset, Math.min(this.byteSize() - newOffset, that.byteSize() + offsetToThat)));
        }
        return Optional.empty();
    }

    boolean overlaps(_AbstractMemorySegmentImpl that) {
        return _Utils.overlaps(this.unsafeGetBase(), this.unsafeGetOffset(), this.byteSize(),
                that.unsafeGetBase(), that.unsafeGetOffset(), that.byteSize());
    }

    @Override
    public MemorySegment copyFrom(MemorySegment src) {
        MemorySegment.copy(src, 0, this, 0, src.byteSize());
        return this;
    }

    @Override
    public long mismatch(MemorySegment other) {
        Objects.requireNonNull(other);
        return _SegmentBulkOperations.mismatch(this, 0, byteSize(),
                (_AbstractMemorySegmentImpl) other, 0, other.byteSize());
    }

    @Override
    public void load() {
        throw notAMappedSegment();
    }

    @Override
    public void unload() {
        throw notAMappedSegment();
    }

    @Override
    public boolean isLoaded() {
        throw notAMappedSegment();
    }

    @Override
    public void force() {
        throw notAMappedSegment();
    }

    private static UnsupportedOperationException notAMappedSegment() {
        throw new UnsupportedOperationException("Not a mapped segment");
    }

    @Override
    public final byte[] toArray(ValueLayout.OfByte elementLayout) {
        return toArray(byte[].class, elementLayout, byte[]::new, MemorySegment::ofArray);
    }

    @Override
    public final short[] toArray(ValueLayout.OfShort elementLayout) {
        return toArray(short[].class, elementLayout, short[]::new, MemorySegment::ofArray);
    }

    @Override
    public final char[] toArray(ValueLayout.OfChar elementLayout) {
        return toArray(char[].class, elementLayout, char[]::new, MemorySegment::ofArray);
    }

    @Override
    public final int[] toArray(ValueLayout.OfInt elementLayout) {
        return toArray(int[].class, elementLayout, int[]::new, MemorySegment::ofArray);
    }

    @Override
    public final float[] toArray(ValueLayout.OfFloat elementLayout) {
        return toArray(float[].class, elementLayout, float[]::new, MemorySegment::ofArray);
    }

    @Override
    public final long[] toArray(ValueLayout.OfLong elementLayout) {
        return toArray(long[].class, elementLayout, long[]::new, MemorySegment::ofArray);
    }

    @Override
    public final double[] toArray(ValueLayout.OfDouble elementLayout) {
        return toArray(double[].class, elementLayout, double[]::new, MemorySegment::ofArray);
    }

    private <Z> Z toArray(Class<Z> arrayClass, ValueLayout elemLayout, IntFunction<Z> arrayFactory, Function<Z, MemorySegment> segmentFactory) {
        int size = checkArraySize(arrayClass.getSimpleName(), (int) elemLayout.byteSize());
        Z arr = arrayFactory.apply(size);
        MemorySegment arrSegment = segmentFactory.apply(arr);
        MemorySegment.copy(this, elemLayout, 0, arrSegment, elemLayout.withOrder(ByteOrder.nativeOrder()), 0, size);
        return arr;
    }

    public void checkReadOnly(boolean readOnly) {
        if (!readOnly && this.readOnly) {
            throw new IllegalArgumentException("Attempt to write a read-only segment");
        }
    }

    public void checkAccess(long offset, long length, boolean readOnly) {
        checkReadOnly(readOnly);
        checkBounds(offset, length);
    }

    public void checkValidState() {
        sessionImpl().checkValidState();
    }

    public final void checkEnclosingLayout(long offset, MemoryLayout enclosing, boolean readOnly) {
        checkAccess(offset, enclosing.byteSize(), readOnly);
        if (!isAlignedForElement(offset, enclosing)) {
            throw new IllegalArgumentException(String.format(
                    "Target offset %d is incompatible with alignment constraint %d (of %s) for segment %s"
                    , offset, enclosing.byteAlignment(), enclosing, this));
        }
    }

    public abstract long unsafeGetOffset();

    public abstract Object unsafeGetBase();

    // Helper methods

    public abstract long maxAlignMask();

    public final boolean isAlignedForElement(long offset, MemoryLayout layout) {
        return isAlignedForElement(offset, layout.byteAlignment());
    }

    public final boolean isAlignedForElement(long offset, long byteAlignment) {
        return (((unsafeGetOffset() + offset) | maxAlignMask()) & (byteAlignment - 1)) == 0;
    }

    private int checkArraySize(String typeName, int elemSize) {
        // elemSize is guaranteed to be a power of two, so we can use an alignment check
        if (!_Utils.isAligned(length, elemSize)) {
            throw new IllegalStateException(String.format("Segment size is not a multiple of %d. Size: %d", elemSize, length));
        }
        long arraySize = length / elemSize;
        if (arraySize > (Integer.MAX_VALUE - 8)) { //conservative check
            throw new IllegalStateException(String.format("Segment is too large to wrap as %s. Size: %d", typeName, length));
        }
        return (int) arraySize;
    }

    void checkBounds(long offset, long length) {
        if (length > 0) {
            Objects.checkIndex(offset, this.length - length + 1);
        } else if (length < 0 || offset < 0 ||
                offset > this.length - length) {
            throw outOfBoundException(offset, length);
        }
    }

    @Override
    public RuntimeException apply(String s, List<Number> numbers) {
        long offset = numbers.get(0).longValue();
        long length = byteSize() - numbers.get(1).longValue() + 1;
        return outOfBoundException(offset, length);
    }

    @Override
    public Scope scope() {
        return scope;
    }

    @Override
    public boolean isAccessibleBy(Thread thread) {
        return sessionImpl().isAccessibleBy(thread);
    }

    public final _MemorySessionImpl sessionImpl() {
        return scope;
    }

    @SuppressLint("DefaultLocale")
    private IndexOutOfBoundsException outOfBoundException(long offset, long length) {
        return new IndexOutOfBoundsException(
                String.format("Out of bound access on segment %s; new offset = %d; new length = %d",
                        this, offset, length));
    }

    static class SegmentSplitter implements Spliterator<MemorySegment> {
        _AbstractMemorySegmentImpl segment;
        long elemCount;
        final long elementSize;
        long currentIndex;

        SegmentSplitter(long elementSize, long elemCount, _AbstractMemorySegmentImpl segment) {
            this.segment = segment;
            this.elementSize = elementSize;
            this.elemCount = elemCount;
        }

        @Override
        public SegmentSplitter trySplit() {
            if (currentIndex == 0 && elemCount > 1) {
                _AbstractMemorySegmentImpl parent = segment;
                long rem = elemCount % 2;
                long split = elemCount / 2;
                long lobound = split * elementSize;
                long hibound = lobound + (rem * elementSize);
                elemCount = split + rem;
                segment = parent.asSliceNoCheck(lobound, hibound);
                return new SegmentSplitter(elementSize, split, parent.asSliceNoCheck(0, lobound));
            } else {
                return null;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super MemorySegment> action) {
            Objects.requireNonNull(action);
            if (currentIndex < elemCount) {
                _AbstractMemorySegmentImpl acquired = segment;
                try {
                    action.accept(acquired.asSliceNoCheck(currentIndex * elementSize, elementSize));
                } finally {
                    currentIndex++;
                    if (currentIndex == elemCount) {
                        segment = null;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super MemorySegment> action) {
            Objects.requireNonNull(action);
            if (currentIndex < elemCount) {
                _AbstractMemorySegmentImpl acquired = segment;
                try {
                    for (long i = currentIndex; i < elemCount; i++) {
                        action.accept(acquired.asSliceNoCheck(i * elementSize, elementSize));
                    }
                } finally {
                    currentIndex = elemCount;
                    segment = null;
                }
            }
        }

        @Override
        public long estimateSize() {
            return elemCount;
        }

        @Override
        public int characteristics() {
            return NONNULL | SUBSIZED | SIZED | IMMUTABLE | ORDERED;
        }
    }

    // Object methods

    @Override
    public String toString() {
        return "MemorySegment{ " +
                heapBase().map(hb -> "heapBase: " + hb + ", ").orElse("") +
                "address: " + Utils.toHexString(address()) +
                ", byteSize: " + length +
                " }";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof _AbstractMemorySegmentImpl that &&
                unsafeGetBase() == that.unsafeGetBase() &&
                unsafeGetOffset() == that.unsafeGetOffset();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                unsafeGetOffset(),
                unsafeGetBase());
    }

    public static _AbstractMemorySegmentImpl ofBuffer(Buffer bb) {
        //TODO: check buffer.isAccessible or isFreed in segments?
        Objects.requireNonNull(bb);
        Object base = JavaNioAccess.getBufferBase(bb);
        if (!bb.isDirect() && base == null) {
            throw new IllegalArgumentException("The provided heap buffer is not backed by an array.");
        }
        long bbAddress = JavaNioAccess.getBufferAddress(bb);
        UnmapperProxy unmapper = JavaNioAccess.unmapper(bb);

        int pos = bb.position();
        int limit = bb.limit();
        int size = limit - pos;

        boolean readOnly = bb.isReadOnly();
        int scaleFactor = getScaleFactor(bb);
        _MemorySessionImpl bufferScope = (_MemorySessionImpl) JavaNioAccess.getBufferScope(bb);
        if (bufferScope == null) {
            bufferScope = _MemorySessionImpl.createGlobalHolder(bufferRef(bb));
        }
        long off = bbAddress + ((long) pos << scaleFactor);
        long len = (long) size << scaleFactor;

        if (base != null) heap:{
            if (bb.isDirect()) {
                assert_(base instanceof byte[], AssertionError::new);
                break heap;
            } else if (base instanceof byte[]) {
                return new _HeapMemorySegmentImpl.OfByte(off, base, len, readOnly, bufferScope);
            } else if (base instanceof short[]) {
                return new _HeapMemorySegmentImpl.OfShort(off, base, len, readOnly, bufferScope);
            } else if (base instanceof char[]) {
                return new _HeapMemorySegmentImpl.OfChar(off, base, len, readOnly, bufferScope);
            } else if (base instanceof int[]) {
                return new _HeapMemorySegmentImpl.OfInt(off, base, len, readOnly, bufferScope);
            } else if (base instanceof float[]) {
                return new _HeapMemorySegmentImpl.OfFloat(off, base, len, readOnly, bufferScope);
            } else if (base instanceof long[]) {
                return new _HeapMemorySegmentImpl.OfLong(off, base, len, readOnly, bufferScope);
            } else if (base instanceof double[]) {
                return new _HeapMemorySegmentImpl.OfDouble(off, base, len, readOnly, bufferScope);
            }
            throw shouldNotReachHere();
        }
        if (unmapper == null) {
            return new _NativeMemorySegmentImpl(off, len, readOnly, bufferScope);
        } else {
            return new _MappedMemorySegmentImpl(off, unmapper, len, readOnly, bufferScope);
        }
    }

    private static Object bufferRef(Buffer buffer) {
        if (buffer.isDirect()) {
            // direct buffer, return the buffer attachment
            return Objects.requireNonNull(JavaNioAccess.attachment(buffer));
        } else {
            // heap buffer, return the underlying array
            return JavaNioAccess.getBufferBase(buffer);
        }
    }

    private static int getScaleFactor(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 0;
        } else if (buffer instanceof CharBuffer || buffer instanceof ShortBuffer) {
            return 1;
        } else if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) {
            return 2;
        } else if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) {
            return 3;
        } else {
            throw shouldNotReachHere();
        }
    }

    public static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset,
                            MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset,
                            long elementCount) {

        _Utils.checkNonNegativeIndex(elementCount, "elementCount");
        _AbstractMemorySegmentImpl srcImpl = (_AbstractMemorySegmentImpl) srcSegment;
        _AbstractMemorySegmentImpl dstImpl = (_AbstractMemorySegmentImpl) dstSegment;
        if (srcElementLayout.byteSize() != dstElementLayout.byteSize()) {
            throw new IllegalArgumentException("Source and destination layouts must have same size");
        }
        _Utils.checkElementAlignment(srcElementLayout, "Source layout alignment greater than its size");
        _Utils.checkElementAlignment(dstElementLayout, "Destination layout alignment greater than its size");
        if (!srcImpl.isAlignedForElement(srcOffset, srcElementLayout)) {
            throw new IllegalArgumentException("Source segment incompatible with alignment constraints");
        }
        if (!dstImpl.isAlignedForElement(dstOffset, dstElementLayout)) {
            throw new IllegalArgumentException("Destination segment incompatible with alignment constraints");
        }
        long size = elementCount * srcElementLayout.byteSize();
        srcImpl.checkAccess(srcOffset, size, true);
        dstImpl.checkAccess(dstOffset, size, false);
        if (srcElementLayout.byteSize() == 1 || srcElementLayout.order() == dstElementLayout.order()) {
            _ScopedMemoryAccess.copyMemory(srcImpl.sessionImpl(), dstImpl.sessionImpl(),
                    srcImpl.unsafeGetBase(), srcImpl.unsafeGetOffset() + srcOffset,
                    dstImpl.unsafeGetBase(), dstImpl.unsafeGetOffset() + dstOffset, size);
        } else {
            _ScopedMemoryAccess.copySwapMemory(srcImpl.sessionImpl(), dstImpl.sessionImpl(),
                    srcImpl.unsafeGetBase(), srcImpl.unsafeGetOffset() + srcOffset,
                    dstImpl.unsafeGetBase(), dstImpl.unsafeGetOffset() + dstOffset, size, srcElementLayout.byteSize());
        }
    }

    public static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
                            Object dstArray, int dstIndex, int elementCount) {
        _Utils.checkNonNegativeIndex(elementCount, "elementCount");
        var dstInfo = _Utils.BaseAndScale.of(dstArray);
        if (dstArray.getClass().getComponentType() != srcLayout.carrier()) {
            throw new IllegalArgumentException("Incompatible value layout: " + srcLayout);
        }
        _AbstractMemorySegmentImpl srcImpl = (_AbstractMemorySegmentImpl) srcSegment;
        _Utils.checkElementAlignment(srcLayout, "Source layout alignment greater than its size");
        if (!srcImpl.isAlignedForElement(srcOffset, srcLayout)) {
            throw new IllegalArgumentException("Source segment incompatible with alignment constraints");
        }
        srcImpl.checkAccess(srcOffset, elementCount * dstInfo.scale(), true);
        Objects.checkFromIndexSize(dstIndex, elementCount, Array.getLength(dstArray));
        if (dstInfo.scale() == 1 || srcLayout.order() == ByteOrder.nativeOrder()) {
            _ScopedMemoryAccess.copyMemory(srcImpl.sessionImpl(), null,
                    srcImpl.unsafeGetBase(), srcImpl.unsafeGetOffset() + srcOffset,
                    dstArray, dstInfo.base() + (dstIndex * dstInfo.scale()), elementCount * dstInfo.scale());
        } else {
            _ScopedMemoryAccess.copySwapMemory(srcImpl.sessionImpl(), null,
                    srcImpl.unsafeGetBase(), srcImpl.unsafeGetOffset() + srcOffset,
                    dstArray, dstInfo.base() + (dstIndex * dstInfo.scale()), elementCount * dstInfo.scale(), dstInfo.scale());
        }
    }

    public static void copy(Object srcArray, int srcIndex,
                            MemorySegment dstSegment, ValueLayout dstLayout, long dstOffset,
                            int elementCount) {
        var srcInfo = _Utils.BaseAndScale.of(srcArray);
        if (srcArray.getClass().getComponentType() != dstLayout.carrier()) {
            throw new IllegalArgumentException("Incompatible value layout: " + dstLayout);
        }
        Objects.checkFromIndexSize(srcIndex, elementCount, Array.getLength(srcArray));
        _AbstractMemorySegmentImpl destImpl = (_AbstractMemorySegmentImpl) dstSegment;
        _Utils.checkElementAlignment(dstLayout, "Destination layout alignment greater than its size");
        if (!destImpl.isAlignedForElement(dstOffset, dstLayout)) {
            throw new IllegalArgumentException("Destination segment incompatible with alignment constraints");
        }
        destImpl.checkAccess(dstOffset, elementCount * srcInfo.scale(), false);
        if (srcInfo.scale() == 1 || dstLayout.order() == ByteOrder.nativeOrder()) {
            _ScopedMemoryAccess.copyMemory(null, destImpl.sessionImpl(),
                    srcArray, srcInfo.base() + (srcIndex * srcInfo.scale()),
                    destImpl.unsafeGetBase(), destImpl.unsafeGetOffset() + dstOffset, elementCount * srcInfo.scale());
        } else {
            _ScopedMemoryAccess.copySwapMemory(null, destImpl.sessionImpl(),
                    srcArray, srcInfo.base() + (srcIndex * srcInfo.scale()),
                    destImpl.unsafeGetBase(), destImpl.unsafeGetOffset() + dstOffset, elementCount * srcInfo.scale(), srcInfo.scale());
        }
    }

    // accessors

    @Override
    public byte get(ValueLayout.OfByte layout, long offset) {
        return (byte) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfByte layout, long offset, byte value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public boolean get(ValueLayout.OfBoolean layout, long offset) {
        return (boolean) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfBoolean layout, long offset, boolean value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public char get(ValueLayout.OfChar layout, long offset) {
        return (char) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfChar layout, long offset, char value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public short get(ValueLayout.OfShort layout, long offset) {
        return (short) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfShort layout, long offset, short value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public int get(ValueLayout.OfInt layout, long offset) {
        return (int) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfInt layout, long offset, int value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public float get(ValueLayout.OfFloat layout, long offset) {
        return (float) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfFloat layout, long offset, float value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public long get(ValueLayout.OfLong layout, long offset) {
        return (long) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfLong layout, long offset, long value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public double get(ValueLayout.OfDouble layout, long offset) {
        return (double) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(ValueLayout.OfDouble layout, long offset, double value) {
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public MemorySegment get(AddressLayout layout, long offset) {
        return (MemorySegment) layout.varHandle().get(this, offset);
    }

    @Override
    public void set(AddressLayout layout, long offset, MemorySegment value) {
        Objects.requireNonNull(value);
        layout.varHandle().set(this, offset, value);
    }

    @Override
    public byte getAtIndex(ValueLayout.OfByte layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (byte) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public boolean getAtIndex(ValueLayout.OfBoolean layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (boolean) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public char getAtIndex(ValueLayout.OfChar layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (char) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfChar layout, long index, char value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public short getAtIndex(ValueLayout.OfShort layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (short) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfByte layout, long index, byte value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public void setAtIndex(ValueLayout.OfShort layout, long index, short value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public int getAtIndex(ValueLayout.OfInt layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (int) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfInt layout, long index, int value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public float getAtIndex(ValueLayout.OfFloat layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (float) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfFloat layout, long index, float value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public long getAtIndex(ValueLayout.OfLong layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (long) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfLong layout, long index, long value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public double getAtIndex(ValueLayout.OfDouble layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (double) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(ValueLayout.OfDouble layout, long index, double value) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public MemorySegment getAtIndex(AddressLayout layout, long index) {
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        return (MemorySegment) layout.varHandle().get(this, index * layout.byteSize());
    }

    @Override
    public void setAtIndex(AddressLayout layout, long index, MemorySegment value) {
        Objects.requireNonNull(value);
        _Utils.checkElementAlignment(layout, "Layout alignment greater than its size");
        layout.varHandle().set(this, index * layout.byteSize(), value);
    }

    @Override
    public String getString(long offset) {
        return getString(offset, StandardCharsets.UTF_8);
    }

    @Override
    public String getString(long offset, Charset charset) {
        Objects.requireNonNull(charset);
        return _StringSupport.read(this, offset, charset);
    }

    @Override
    public void setString(long offset, String str) {
        Objects.requireNonNull(str);
        setString(offset, str, StandardCharsets.UTF_8);
    }

    @Override
    public void setString(long offset, String str, Charset charset) {
        Objects.requireNonNull(charset);
        Objects.requireNonNull(str);
        _StringSupport.write(this, offset, charset, str);
    }
}

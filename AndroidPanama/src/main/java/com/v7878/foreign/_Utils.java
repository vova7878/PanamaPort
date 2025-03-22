/*
 *  Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.toHexString;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandles;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.NoSideEffects;
import com.v7878.unsafe.invoke.MethodHandlesFixes;
import com.v7878.unsafe.invoke.Wrapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class contains misc helper functions to support creation of memory segments.
 */
final class _Utils {

    // Suppresses default constructor, ensuring non-instantiability.
    private _Utils() {
    }

    @NoSideEffects
    private static final Class<?> ADDRESS_CARRIER_TYPE;
    @NoSideEffects
    private static final MethodHandle UNBOX_SEGMENT;
    @NoSideEffects
    private static final MethodHandle MAKE_SEGMENT_TARGET;
    @NoSideEffects
    private static final MethodHandle MAKE_SEGMENT_NO_TARGET;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String unboxSegmentName;
        Class<?> rawAddressType;
        if (IS64BIT) {
            unboxSegmentName = "unboxSegment";
            rawAddressType = long.class;
        } else {
            unboxSegmentName = "unboxSegment32";
            rawAddressType = int.class;
        }
        ADDRESS_CARRIER_TYPE = rawAddressType;
        UNBOX_SEGMENT = _MhUtil.findStatic(lookup, _Utils.class, unboxSegmentName,
                MethodType.methodType(rawAddressType, MemorySegment.class));
        MAKE_SEGMENT_TARGET = _MhUtil.findStatic(lookup, _Utils.class, "makeSegment",
                MethodType.methodType(MemorySegment.class, rawAddressType, AddressLayout.class));
        MAKE_SEGMENT_NO_TARGET = _MhUtil.findStatic(lookup, _Utils.class, "makeSegment",
                MethodType.methodType(MemorySegment.class, rawAddressType));
    }

    public static long requireByteSizeValid(long byteSize, boolean allowZero) {
        if ((byteSize == 0 && !allowZero) || byteSize < 0) {
            throw new IllegalArgumentException("Invalid byte size: " + byteSize);
        }
        return byteSize;
    }

    public static long alignUp(long n, long alignment) {
        return (n + alignment - 1) & -alignment;
    }

    // Port-added: specific implementation
    private static VarHandle memorySegmentViewHandle(
            Class<?> carrier, long alignmentMask, ByteOrder order) {
        boolean swap = carrier != byte.class && carrier != boolean.class
                && !ByteOrder.nativeOrder().equals(order);
        return _VarHandleSegmentView.memorySegmentViewHandle(carrier, alignmentMask, swap);
    }

    /**
     * This method returns a var handle that accesses a target layout in an enclosing layout, taking the memory offset
     * and the base offset of the enclosing layout in the segment.
     * <p>
     * The coordinates are (MS, long, long).
     * The trailing long is a pre-validated, variable extra offset, which the var handle does not perform any size or
     * alignment checks against. Such checks are added (using adaptation) by {@link _LayoutPath#dereferenceHandle()}.
     * <p>
     * We provide two level of caching of the generated var handles. First, the var handle associated
     * with a {@link ValueLayout#varHandle()} call is cached inside a stable field of the value layout implementation.
     * This optimizes common code idioms like {@code JAVA_INT.varHandle().getInt(...)}. A second layer of caching
     * is then provided by this method, so different value layouts with same effects can reuse var handle instances.
     * (The 2nd layer may be redundant in the long run)
     *
     * @param layout the value layout for which a raw memory segment var handle is to be created
     */
    public static VarHandle makeRawSegmentViewVarHandle(ValueLayout layout) {
        record VarHandleCache() implements Function<ValueLayout, VarHandle> {
            private static final Map<ValueLayout, VarHandle> HANDLE_MAP = new ConcurrentHashMap<>();
            private static final VarHandleCache INSTANCE = new VarHandleCache();

            @Override
            public VarHandle apply(ValueLayout valueLayout) {
                return makeRawSegmentViewVarHandleInternal(valueLayout);
            }
        }
        return VarHandleCache.HANDLE_MAP.computeIfAbsent(layout.withoutName(), VarHandleCache.INSTANCE);
    }

    private static VarHandle makeRawSegmentViewVarHandleInternal(ValueLayout layout) {
        Class<?> baseCarrier = layout.carrier();
        if (layout.carrier() == MemorySegment.class) {
            baseCarrier = ADDRESS_CARRIER_TYPE;
        }

        VarHandle handle = memorySegmentViewHandle(baseCarrier,
                layout.byteAlignment() - 1, layout.order());

        if (layout instanceof AddressLayout addressLayout) {
            MethodHandle longToAddressAdapter = addressLayout.targetLayout().isPresent() ?
                    MethodHandlesFixes.insertArguments(MAKE_SEGMENT_TARGET, 1, addressLayout) :
                    MAKE_SEGMENT_NO_TARGET;
            handle = VarHandles.filterValue(handle, UNBOX_SEGMENT, longToAddressAdapter);
        }
        return handle;
    }

    public static void checkSymbol(MemorySegment symbol) {
        Objects.requireNonNull(symbol);
        if (symbol.equals(MemorySegment.NULL))
            throw new IllegalArgumentException("Symbol is NULL: " + symbol);
    }

    public static MemorySegment checkCaptureSegment(MemorySegment captureSegment) {
        Objects.requireNonNull(captureSegment);
        if (captureSegment.equals(MemorySegment.NULL)) {
            throw new IllegalArgumentException("Capture segment is NULL: " + captureSegment);
        }
        return captureSegment.asSlice(0, _CapturableState.LAYOUT);
    }

    public static boolean overlaps(Object srcBase, long srcOffset, long srcSize,
                                   Object destBase, long destOffset, long destSize) {
        if (srcBase == destBase) {  // both either native or the same heap segment
            final long srcEnd = srcOffset + srcSize;
            final long destEnd = destOffset + destSize;

            return (srcOffset < destEnd && srcEnd > destOffset); //overlap occurs?
        }
        return false;
    }

    public static void checkNative(MemorySegment segment) {
        if (!segment.isNative()) {
            throw new IllegalArgumentException("Heap segment not allowed: " + segment);
        }
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long unboxSegment(MemorySegment segment) {
        checkNative(segment);
        return segment.address();
    }

    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    public static int unboxSegment32(MemorySegment segment) {
        // This cast to 'int' is safe, because we only call this method on 32-bit
        // platforms, where we know the address of a segment is truncated to 32-bits.
        // There's a similar cast for 4-byte addresses in Unsafe.putAddress.
        return (int) unboxSegment(segment);
    }


    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    public static MemorySegment makeSegment(long addr) {
        return makeSegment(addr, 0, 1);
    }

    // 32 bit
    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    public static MemorySegment makeSegment(int addr) {
        return makeSegment(addr, 0, 1);
    }

    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    public static MemorySegment makeSegment(long addr, AddressLayout layout) {
        return makeSegment(addr, pointeeByteSize(layout), pointeeByteAlign(layout));
    }

    // 32 bit
    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    public static MemorySegment makeSegment(int addr, AddressLayout layout) {
        return makeSegment(addr, pointeeByteSize(layout), pointeeByteAlign(layout));
    }

    public static MemorySegment makeSegment(long addr, long size, long align) {
        return makeSegment(addr, size, align, _GlobalSession.INSTANCE);
    }

    public static MemorySegment makeSegment(long addr, long size, long align, _MemorySessionImpl scope) {
        if (!isAligned(addr, align)) {
            throw new IllegalArgumentException("Invalid alignment constraint for address: " + toHexString(addr));
        }
        return _SegmentFactories.makeNativeSegmentUnchecked(addr, size, scope);
    }

    public static boolean isAligned(long offset, long align) {
        return (offset & (align - 1)) == 0;
    }

    public static boolean isPowerOfTwo(long value) {
        return (value & (value - 1)) == 0L;
    }

    public static boolean isElementAligned(ValueLayout layout) {
        // Fast-path: if both size and alignment are powers of two, we can just
        // check if one is greater than the other.
        assert isPowerOfTwo(layout.byteSize());
        return layout.byteAlignment() <= layout.byteSize();
    }

    public static void checkElementAlignment(ValueLayout layout, String msg) {
        if (!isElementAligned(layout)) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void checkElementAlignment(MemoryLayout layout, String msg) {
        if (layout.byteSize() % layout.byteAlignment() != 0) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static long pointeeByteSize(AddressLayout addressLayout) {
        return addressLayout.targetLayout()
                .map(MemoryLayout::byteSize)
                .orElse(0L);
    }

    public static long pointeeByteAlign(AddressLayout addressLayout) {
        return addressLayout.targetLayout()
                .map(MemoryLayout::byteAlignment)
                .orElse(1L);
    }

    public static void checkAllocationSizeAndAlign(long byteSize, long byteAlignment) {
        // byteSize should be >= 0
        _Utils.checkNonNegativeArgument(byteSize, "allocation size");
        checkAlign(byteAlignment);
    }

    public static void checkAlign(long byteAlignment) {
        // alignment should be > 0, and power of two
        if (byteAlignment <= 0 || !isPowerOfTwo(byteAlignment)) {
            throw new IllegalArgumentException("Invalid alignment constraint: " + byteAlignment);
        }
    }

    public static void checkNonNegativeArgument(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("The provided " + name + " is negative: " + value);
        }
    }

    public static void checkNonNegativeIndex(long value, String name) {
        if (value < 0) {
            throw new IndexOutOfBoundsException("The provided " + name + " is negative: " + value);
        }
    }

    private static long computePadding(long offset, long align) {
        boolean isAligned = offset == 0 || offset % align == 0;
        if (isAligned) {
            return 0;
        } else {
            long gap = offset % align;
            return align - gap;
        }
    }

    /**
     * {@return return a struct layout constructed from the given elements, with padding
     * computed automatically so that they are naturally aligned}.
     *
     * @param elements the structs' fields
     */
    public static StructLayout computePaddedStructLayout(MemoryLayout... elements) {
        long offset = 0L;
        List<MemoryLayout> layouts = new ArrayList<>();
        long align = 0;
        for (MemoryLayout l : elements) {
            long padding = computePadding(offset, l.byteAlignment());
            if (padding != 0) {
                layouts.add(MemoryLayout.paddingLayout(padding));
                offset += padding;
            }
            layouts.add(l);
            align = Math.max(align, l.byteAlignment());
            offset += l.byteSize();
        }
        long padding = computePadding(offset, align);
        if (padding != 0) {
            layouts.add(MemoryLayout.paddingLayout(padding));
        }
        return MemoryLayout.structLayout(layouts.toArray(new MemoryLayout[0]));
    }

    public static int byteWidthOfPrimitive(Class<?> primitive) {
        return Wrapper.forPrimitiveType(primitive).byteWidth();
    }

    public static <L extends MemoryLayout> L wrapOverflow(Supplier<L> layoutSupplier) {
        try {
            return layoutSupplier.get();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Layout size exceeds Long.MAX_VALUE");
        }
    }

    public enum BaseAndScale {
        BYTE(ARRAY_BYTE_BASE_OFFSET, ARRAY_BYTE_INDEX_SCALE),
        CHAR(ARRAY_CHAR_BASE_OFFSET, ARRAY_CHAR_INDEX_SCALE),
        SHORT(ARRAY_SHORT_BASE_OFFSET, ARRAY_SHORT_INDEX_SCALE),
        INT(ARRAY_INT_BASE_OFFSET, ARRAY_INT_INDEX_SCALE),
        FLOAT(ARRAY_FLOAT_BASE_OFFSET, ARRAY_FLOAT_INDEX_SCALE),
        LONG(ARRAY_LONG_BASE_OFFSET, ARRAY_LONG_INDEX_SCALE),
        DOUBLE(ARRAY_DOUBLE_BASE_OFFSET, ARRAY_DOUBLE_INDEX_SCALE);

        private final long base;
        private final long scale;

        BaseAndScale(long base, long scale) {
            this.base = base;
            this.scale = scale;
        }

        public static BaseAndScale of(Object array) {
            if (array instanceof byte[]) {
                return BaseAndScale.BYTE;
            } else if (array instanceof char[]) {
                return BaseAndScale.CHAR;
            } else if (array instanceof short[]) {
                return BaseAndScale.SHORT;
            } else if (array instanceof int[]) {
                return BaseAndScale.INT;
            } else if (array instanceof float[]) {
                return BaseAndScale.FLOAT;
            } else if (array instanceof long[]) {
                return BaseAndScale.LONG;
            } else if (array instanceof double[]) {
                return BaseAndScale.DOUBLE;
            }
            throw new IllegalArgumentException("Not a supported array class: "
                    + array.getClass().getSimpleName());
        }

        public long base() {
            return base;
        }

        public long scale() {
            return scale;
        }
    }

    public static Arena newBoundedArena(long size) {
        return new Arena() {
            final Arena arena = Arena.ofConfined();
            final SegmentAllocator slicingAllocator = SegmentAllocator
                    .slicingAllocator(arena.allocate(size));

            @Override
            public MemorySegment.Scope scope() {
                return arena.scope();
            }

            @Override
            public void close() {
                arena.close();
            }

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                return slicingAllocator.allocate(byteSize, byteAlignment);
            }
        };
    }
}

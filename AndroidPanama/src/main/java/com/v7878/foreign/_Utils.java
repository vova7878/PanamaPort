/*
 *  Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

import androidx.annotation.Keep;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandles;
import com.v7878.unsafe.invoke.MethodHandlesFixes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * This class contains misc helper functions to support creation of memory segments.
 */
final class _Utils {

    // Suppresses default constructor, ensuring non-instantiability.
    private _Utils() {
    }

    private static final MethodHandle BYTE_TO_BOOL;
    private static final MethodHandle BOOL_TO_BYTE;
    private static final MethodHandle ADDRESS_TO_LONG;
    private static final MethodHandle LONG_TO_ADDRESS;
    private static final MethodHandle MH_CHECK_CAPTURE_SEGMENT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            BYTE_TO_BOOL = lookup.findStatic(_Utils.class, "byteToBoolean",
                    MethodType.methodType(boolean.class, byte.class));
            BOOL_TO_BYTE = lookup.findStatic(_Utils.class, "booleanToByte",
                    MethodType.methodType(byte.class, boolean.class));
            ADDRESS_TO_LONG = lookup.findStatic(_Utils.class, "unboxSegment",
                    MethodType.methodType(long.class, MemorySegment.class));
            LONG_TO_ADDRESS = lookup.findStatic(_Utils.class, "longToAddress",
                    MethodType.methodType(MemorySegment.class, long.class, long.class, long.class));
            MH_CHECK_CAPTURE_SEGMENT = lookup.findStatic(_Utils.class, "checkCaptureSegment",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
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

    public static long remainsToAlignment(long addr, long alignment) {
        return alignUp(addr, alignment) - addr;
    }

    // Port-added: specific implementation
    private static VarHandle makeRawSegmentViewVarHandle(
            Class<?> carrier, long alignmentMask, ByteOrder order) {
        return _VarHandleSegmentViewBase.makeRawSegmentViewVarHandle(carrier,
                alignmentMask, !ByteOrder.nativeOrder().equals(order));
    }

    public static VarHandle makeSegmentViewVarHandle(ValueLayout layout) {
        final class VarHandleCache {
            private static final Map<ValueLayout, VarHandle> HANDLE_MAP = new ConcurrentHashMap<>();

            static VarHandle put(ValueLayout layout, VarHandle handle) {
                VarHandle prev = HANDLE_MAP.putIfAbsent(layout, handle);
                return prev != null ? prev : handle;
            }

            static VarHandle get(ValueLayout layout) {
                return HANDLE_MAP.get(layout);
            }
        }
        layout = layout.withoutName(); // name doesn't matter
        // keep the addressee layout as it's used below

        VarHandle handle = VarHandleCache.get(layout);
        if (handle != null) {
            return handle;
        }

        Class<?> baseCarrier = layout.carrier();
        if (layout.carrier() == MemorySegment.class) {
            baseCarrier = IS64BIT ? long.class : int.class;
        } else if (layout.carrier() == boolean.class) {
            //TODO: disallow NUMERIC_ATOMIC_UPDATE_ACCESS_MODES?
            baseCarrier = byte.class;
        }

        handle = makeRawSegmentViewVarHandle(baseCarrier,
                layout.byteAlignment() - 1, layout.order());

        if (layout.carrier() == boolean.class) {
            handle = VarHandles.filterValue(handle, BOOL_TO_BYTE, BYTE_TO_BOOL);
        } else if (layout instanceof AddressLayout addressLayout) {
            handle = VarHandles.filterValue(handle,
                    MethodHandlesFixes.explicitCastArguments(ADDRESS_TO_LONG, MethodType.methodType(baseCarrier, MemorySegment.class)),
                    MethodHandlesFixes.explicitCastArguments(MethodHandles.insertArguments(LONG_TO_ADDRESS, 1,
                                    pointeeByteSize(addressLayout), pointeeByteAlign(addressLayout)),
                            MethodType.methodType(MemorySegment.class, baseCarrier)));
        }
        return VarHandleCache.put(layout, handle);
    }

    @Keep
    public static boolean byteToBoolean(byte b) {
        return b != 0;
    }

    @Keep
    private static byte booleanToByte(boolean b) {
        return b ? (byte) 1 : (byte) 0;
    }

    public static void checkSymbol(MemorySegment symbol) {
        Objects.requireNonNull(symbol);
        if (symbol.equals(MemorySegment.NULL))
            throw new IllegalArgumentException("Symbol is NULL: " + symbol);
    }

    @Keep
    public static MemorySegment checkCaptureSegment(MemorySegment captureSegment) {
        Objects.requireNonNull(captureSegment);
        if (captureSegment.equals(MemorySegment.NULL)) {
            throw new IllegalArgumentException("Capture segment is NULL: " + captureSegment);
        }
        return captureSegment.asSlice(0, _CapturableState.LAYOUT);
    }

    public static MethodHandle maybeCheckCaptureSegment(MethodHandle handle, _LinkerOptions options) {
        if (options.hasCapturedCallState()) {
            int index = options.isReturnInMemory() ? 2 : 1;
            // (<target address>, ?<allocator>, <capture segment>, ...) -> ...
            handle = MethodHandles.filterArguments(handle, index, MH_CHECK_CAPTURE_SEGMENT);
        }
        return handle;
    }

    private static int distance(int a, int b) {
        return a < b ? b - a : a - b;
    }

    public static MethodHandle moveArgument(MethodHandle mh, int fromIndex, int toIndex) {
        int[] perms = IntStream.range(0, mh.type().parameterCount()).toArray();
        int length = distance(toIndex, fromIndex);
        if (fromIndex < toIndex) {
            System.arraycopy(perms, fromIndex, perms, fromIndex + 1, length);
        } else {
            System.arraycopy(perms, toIndex + 1, perms, toIndex, length);
        }
        perms[fromIndex] = toIndex;
        return MethodHandlesFixes.reorderArguments(mh, perms);
    }

    public static void checkNative(MemorySegment segment) {
        if (!segment.isNative()) {
            throw new IllegalArgumentException("Heap segment not allowed: " + segment);
        }
    }

    @Keep
    public static long unboxSegment(MemorySegment segment) {
        checkNative(segment);
        return segment.address();
    }

    public static MemorySegment longToAddress(long addr, long size, long align, _MemorySessionImpl scope) {
        if (!isAligned(addr, align)) {
            throw new IllegalArgumentException("Invalid alignment constraint for address: " + toHexString(addr));
        }
        return _SegmentFactories.makeNativeSegmentUnchecked(addr, size, scope);
    }

    @Keep
    public static MemorySegment longToAddress(long addr, long size, long align) {
        return longToAddress(addr, size, align, _GlobalSession.INSTANCE);
    }

    public static boolean isAligned(long offset, long align) {
        return (offset & (align - 1)) == 0;
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
        if (byteAlignment <= 0 ||
                ((byteAlignment & (byteAlignment - 1)) != 0L)) {
            throw new IllegalArgumentException("Invalid alignment constraint : " + byteAlignment);
        }
    }

    public static void checkNonNegativeArgument(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("The provided " + name + " is negative: " + value);
        }
    }

    public static void checkPositiveArgument(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("The provided " + name + " is negative: " + value);
        }
        if (value == 0) {
            throw new IllegalArgumentException("The provided " + name + " is equals to zero");
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
        // Port-changed: just check all primitive types
        //return Wrapper.forPrimitiveType(primitive).bitWidth() / 8;
        if (primitive == byte.class || primitive == boolean.class) {
            return 1;
        } else if (primitive == short.class || primitive == char.class) {
            return 2;
        } else if (primitive == int.class || primitive == float.class) {
            return 4;
        } else if (primitive == long.class || primitive == double.class) {
            return 8;
        }
        throw new IllegalArgumentException("Unsupported primitive class: " + primitive);
    }

    public static boolean isPowerOfTwo(long value) {
        return (value & (value - 1)) == 0L;
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

        private final int base;
        private final long scale;

        BaseAndScale(int base, long scale) {
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

        public int base() {
            return base;
        }

        public long scale() {
            return scale;
        }
    }
}

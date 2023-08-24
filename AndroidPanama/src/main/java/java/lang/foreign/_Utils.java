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

package java.lang.foreign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * This class contains misc helper functions to support creation of memory segments.
 */
final class _Utils {

    // Port-removed: always Android
    //public static final boolean IS_WINDOWS = privilegedGetProperty("os.name").startsWith("Windows");

    // Suppresses default constructor, ensuring non-instantiability.
    private _Utils() {
    }

    // Port-removed: unused
    //private static final MethodHandle BYTE_TO_BOOL;
    //private static final MethodHandle BOOL_TO_BYTE;
    //private static final MethodHandle ADDRESS_TO_LONG;
    //private static final MethodHandle LONG_TO_ADDRESS;
    //
    //static {
    //    try {
    //        MethodHandles.Lookup lookup = MethodHandles.lookup();
    //        BYTE_TO_BOOL = lookup.findStatic(_Utils.class, "byteToBoolean",
    //                MethodType.methodType(boolean.class, byte.class));
    //        BOOL_TO_BYTE = lookup.findStatic(_Utils.class, "booleanToByte",
    //                MethodType.methodType(byte.class, boolean.class));
    //        ADDRESS_TO_LONG = lookup.findStatic(SharedUtils.class, "unboxSegment",
    //                MethodType.methodType(long.class, MemorySegment.class));
    //        LONG_TO_ADDRESS = lookup.findStatic(_Utils.class, "longToAddress",
    //                MethodType.methodType(MemorySegment.class, long.class, long.class, long.class));
    //    } catch (Throwable ex) {
    //        throw new ExceptionInInitializerError(ex);
    //    }
    //}

    public static long alignUp(long n, long alignment) {
        return (n + alignment - 1) & -alignment;
    }

    public static MemorySegment alignUp(MemorySegment ms, long alignment) {
        long offset = ms.address();
        return ms.asSlice(alignUp(offset, alignment) - offset);
    }

    // Port-changed: Use MemoryVarHandle instead VarHandle
    public static MemoryVarHandle makeSegmentViewVarHandle(ValueLayout layout) {
        // Port-removed: TODO
        //final class VarHandleCache {
        //    private static final Map<ValueLayout, VarHandle> HANDLE_MAP = new ConcurrentHashMap<>();
        //
        //    static VarHandle put(ValueLayout layout, VarHandle handle) {
        //        VarHandle prev = HANDLE_MAP.putIfAbsent(layout, handle);
        //        return prev != null ? prev : handle;
        //    }
        //}
        //Class<?> baseCarrier = layout.carrier();
        //if (layout.carrier() == MemorySegment.class) {
        //    baseCarrier = switch ((int) ValueLayout.ADDRESS.byteSize()) {
        //        case Long.BYTES -> long.class;
        //        case Integer.BYTES -> int.class;
        //        default -> throw new UnsupportedOperationException("Unsupported address layout");
        //    };
        //} else if (layout.carrier() == boolean.class) {
        //    baseCarrier = byte.class;
        //}
        //
        //VarHandle handle = SharedSecrets.getJavaLangInvokeAccess().memorySegmentViewHandle(baseCarrier,
        //        layout.byteAlignment() - 1, layout.order());
        //
        //if (layout.carrier() == boolean.class) {
        //    handle = MethodHandles.filterValue(handle, BOOL_TO_BYTE, BYTE_TO_BOOL);
        //} else if (layout instanceof AddressLayout addressLayout) {
        //    handle = MethodHandles.filterValue(handle,
        //            MethodHandles.explicitCastArguments(ADDRESS_TO_LONG, MethodType.methodType(baseCarrier, MemorySegment.class)),
        //            MethodHandles.explicitCastArguments(MethodHandles.insertArguments(LONG_TO_ADDRESS, 1,
        //                            pointeeByteSize(addressLayout), pointeeByteAlign(addressLayout)),
        //                    MethodType.methodType(MemorySegment.class, baseCarrier)));
        //}
        //return VarHandleCache.put(layout, handle);
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Port-removed: unused
    //public static boolean byteToBoolean(byte b) {
    //    return b != 0;
    //}
    //
    //private static byte booleanToByte(boolean b) {
    //    return b ? (byte) 1 : (byte) 0;
    //}

    public static MemorySegment longToAddress(long addr, long size, long align) {
        if (!isAligned(addr, align)) {
            throw new IllegalArgumentException("Invalid alignment constraint for address: " + addr);
        }
        return _NativeMemorySegmentImpl.makeNativeSegmentUnchecked(addr, size);
    }

    public static MemorySegment longToAddress(long addr, long size, long align, _MemorySessionImpl scope) {
        if (!isAligned(addr, align)) {
            throw new IllegalArgumentException("Invalid alignment constraint for address: " + addr);
        }
        return _NativeMemorySegmentImpl.makeNativeSegmentUnchecked(addr, size, scope);
    }

    public static long unboxSegment(MemorySegment segment) {
        if (!segment.isNative()) {
            throw new IllegalArgumentException("Heap segment not allowed: " + segment);
        }
        return segment.address();
    }

    public static void checkSymbol(MemorySegment symbol) {
        Objects.requireNonNull(symbol);
        if (symbol.equals(MemorySegment.NULL))
            throw new IllegalArgumentException("Symbol is NULL: " + symbol);
    }

    public static void copy(MemorySegment addr, byte[] bytes) {
        // Port-changed: TODO?
        //var heapSegment = MemorySegment.ofArray(bytes);
        //addr.copyFrom(heapSegment);
        //addr.set(JAVA_BYTE, bytes.length, (byte) 0);
        var heapSegment = MemorySegment.ofArray(Arrays.copyOf(bytes, bytes.length + 1));
        addr.copyFrom(heapSegment);
    }

    public static MemorySegment toCString(byte[] bytes, SegmentAllocator allocator) {
        MemorySegment addr = allocator.allocate(bytes.length + 1);
        copy(addr, bytes);
        return addr;
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
        // size should be >= 0
        if (byteSize < 0) {
            throw new IllegalArgumentException("Invalid allocation size : " + byteSize);
        }

        checkAlign(byteAlignment);
    }

    public static void checkAlign(long byteAlignment) {
        // alignment should be > 0, and power of two
        if (byteAlignment <= 0 ||
                ((byteAlignment & (byteAlignment - 1)) != 0L)) {
            throw new IllegalArgumentException("Invalid alignment constraint : " + byteAlignment);
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
        // Port-removed: TODO?
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
}

/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_CHAR;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.foreign.ValueLayout.OfByte;
import static com.v7878.foreign.ValueLayout.OfChar;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfFloat;
import static com.v7878.foreign.ValueLayout.OfInt;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.foreign.ValueLayout.OfShort;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * An object that may be used to allocate {@linkplain MemorySegment memory segments}.
 * Clients implementing this interface must implement the {@link #allocate(long, long)}
 * method. A segment allocator defines several methods which can be useful to create
 * segments from several kinds of Java values such as primitives and arrays.
 * <p>
 * {@code SegmentAllocator} is a {@linkplain FunctionalInterface functional interface}.
 * Clients can easily obtain a new segment allocator by using either a lambda expression
 * or a method reference:
 * <p>
 * {@snippet lang = java:
 * SegmentAllocator autoAllocator = (byteSize, byteAlignment) -> Arena.ofAuto().allocate(byteSize, byteAlignment);
 *}
 * <p>
 * This interface defines factories for commonly used allocators:
 * <ul>
 *     <li>{@link #slicingAllocator(MemorySegment)} obtains an efficient slicing
 *         allocator, where memory is allocated by repeatedly slicing the provided
 *         memory segment;</li>
 *     <li>{@link #prefixAllocator(MemorySegment)} obtains an allocator which wraps a
 *         segment and recycles its content upon each new allocation request.</li>
 * </ul>
 * <p>
 * Passing a segment allocator to an API can be especially useful in circumstances where
 * a client wants to communicate <em>where</em> the results of a certain operation
 * (performed by the API) should be stored, as a memory segment. For instance,
 * {@linkplain Linker#downcallHandle(FunctionDescriptor, Linker.Option...) downcall method handles}
 * can accept an additional {@link SegmentAllocator} parameter if the underlying
 * foreign function is known to return a struct by-value. Effectively, the allocator
 * parameter tells the linker where to store the return value of the foreign function.
 *
 * @apiNote Unless otherwise specified, the {@link #allocate(long, long)} method is
 * not thread-safe. Furthermore, memory segments allocated by a segment
 * allocator can be associated with different lifetimes, and can even be backed
 * by overlapping regions of memory. For these reasons, clients should
 * generally only interact with a segment allocator they own.
 * <p>
 * Clients should consider using an {@linkplain Arena arena} instead, which, provides
 * strong thread-safety, lifetime and non-overlapping guarantees.
 */
@FunctionalInterface
public interface SegmentAllocator {

    /**
     * Converts a Java string into a null-terminated C string using the
     * {@linkplain StandardCharsets#UTF_8 UTF-8} charset, storing the result into a
     * memory segment.
     * <p>
     * Calling this method is equivalent to the following code:
     * {@snippet lang = java:
     * allocateFrom(str, StandardCharsets.UTF_8);
     *}
     *
     * @param str the Java string to be converted into a C string
     * @return a new native segment containing the converted C string
     */

    default MemorySegment allocateFrom(String str) {
        Objects.requireNonNull(str);
        return allocateFrom(str, StandardCharsets.UTF_8);
    }

    /**
     * Converts a Java string into a null-terminated C string using the provided charset,
     * and storing the result into a memory segment.
     * <p>
     * This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array. The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     * <p>
     * If the given string contains any {@code '\0'} characters, they will be
     * copied as well. This means that, depending on the method used to read
     * the string, such as {@link MemorySegment#getString(long)}, the string
     * will appear truncated when read again.
     *
     * @param str     the Java string to be converted into a C string
     * @param charset the charset used to {@linkplain Charset#newEncoder() encode} the
     *                string bytes
     * @return a new native segment containing the converted C string
     * @throws IllegalArgumentException if {@code charset} is not a
     *                                  {@linkplain StandardCharsets standard charset}
     * @implSpec The default implementation for this method copies the contents of the
     * provided Java string into a new memory segment obtained by calling
     * {@code this.allocate(B + N)}, where:
     * <ul>
     *     <li>{@code B} is the size, in bytes, of the string encoded using the
     *         provided charset (e.g. {@code str.getBytes(charset).length});</li>
     *     <li>{@code N} is the size (in bytes) of the terminator char according to the
     *         provided charset. For instance, this is 1 for {@link StandardCharsets#US_ASCII}
     *         and 2 for {@link StandardCharsets#UTF_16}.</li>
     * </ul>
     */

    default MemorySegment allocateFrom(String str, Charset charset) {
        Objects.requireNonNull(charset);
        Objects.requireNonNull(str);
        int termCharSize = _StringSupport.CharsetKind.of(charset).terminatorCharSize();
        MemorySegment segment;
        int length;
        if (_StringSupport.bytesCompatible(str, charset)) {
            length = str.length();
            segment = allocateNoInit((long) length + termCharSize);
            _StringSupport.copyToSegmentRaw(str, segment, 0);
        } else {
            byte[] bytes = str.getBytes(charset);
            length = bytes.length;
            segment = allocateNoInit((long) bytes.length + termCharSize);
            MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, 0, bytes.length);
        }
        for (int i = 0; i < termCharSize; i++) {
            segment.set(JAVA_BYTE, length + i, (byte) 0);
        }
        return segment;
    }

    /**
     * {@return a new memory segment initialized with the provided byte value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfByte layout, byte value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided char value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout.
     * The given value is written into the segment according to the byte order and
     * alignment constraint of the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfChar layout, char value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided short value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfShort layout, short value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided int value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfInt layout, int value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided float value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfFloat layout, float value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided long value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfLong layout, long value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the provided double value}
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(OfDouble layout, double value) {
        Objects.requireNonNull(layout);
        MemorySegment seg = allocateNoInit(layout);
        seg.set(layout, 0, value);
        return seg;
    }

    /**
     * {@return a new memory segment initialized with the
     * {@linkplain MemorySegment#address() address} of the provided memory segment}
     * <p>
     * The address value might be narrowed according to the platform address size
     * (see {@link ValueLayout#ADDRESS}).
     * <p>
     * The size of the allocated memory segment is the
     * {@linkplain MemoryLayout#byteSize() size} of the given layout. The given value is
     * written into the segment according to the byte order and alignment constraint of
     * the given layout.
     *
     * @param layout the layout of the block of memory to be allocated
     * @param value  the value to be set in the newly allocated memory segment
     * @throws IllegalArgumentException if {@code value} is not
     *                                  a {@linkplain MemorySegment#isNative() native} segment
     * @implSpec The default implementation is equivalent to:
     * {@snippet lang = java:
     *  Objects.requireNonNull(value);
     *  MemorySegment seg = allocate(Objects.requireNonNull(layout));
     *  seg.set(layout, 0, value);
     *  return seg;
     *}
     */
    default MemorySegment allocateFrom(AddressLayout layout, MemorySegment value) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(layout);
        MemorySegment segment = allocateNoInit(layout);
        segment.set(layout, 0, value);
        return segment;
    }

    /**
     * {@return a new memory segment initialized with the contents of the provided segment}
     * <p>
     * The size of the allocated memory segment is the
     * {@code elementLayout.byteSize() * elementCount}. The contents of the
     * source segment is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout       the element layout of the allocated array
     * @param source              the source segment
     * @param sourceElementLayout the element layout of the source segment
     * @param sourceOffset        the starting offset, in bytes, of the source segment
     * @param elementCount        the number of elements in the source segment to be copied
     * @throws IllegalArgumentException  if {@code elementLayout.byteSize() != sourceElementLayout.byteSize()}
     * @throws IllegalArgumentException  if the source segment/offset
     *                                   are <a href="MemorySegment.html#segment-alignment">incompatible with the alignment constraint</a>
     *                                   in the source element layout
     * @throws IllegalArgumentException  if {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @throws IllegalArgumentException  if {@code sourceElementLayout.byteAlignment() > sourceElementLayout.byteSize()}
     * @throws IllegalStateException     if the {@linkplain MemorySegment#scope() scope} associated
     *                                   with {@code source} is not {@linkplain MemorySegment.Scope#isAlive() alive}
     * @throws WrongThreadException      if this method is called from a thread {@code T},
     *                                   such that {@code source.isAccessibleBy(T) == false}
     * @throws IllegalArgumentException  if {@code elementCount * sourceElementLayout.byteSize()} overflows
     * @throws IllegalArgumentException  if {@code elementCount < 0}
     * @throws IndexOutOfBoundsException if {@code sourceOffset > source.byteSize() - (elementCount * sourceElementLayout.byteSize())}
     * @throws IndexOutOfBoundsException if {@code sourceOffset < 0}
     * @implSpec The default implementation for this method is equivalent to the following code:
     * {@snippet lang = java:
     * MemorySegment dest = this.allocate(elementLayout, elementCount);
     * MemorySegment.copy(source, sourceElementLayout, sourceOffset, dest, elementLayout, 0, elementCount);
     * return dest;
     *}
     */

    default MemorySegment allocateFrom(ValueLayout elementLayout,
                                       MemorySegment source,
                                       ValueLayout sourceElementLayout,
                                       long sourceOffset,
                                       long elementCount) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(sourceElementLayout);
        Objects.requireNonNull(elementLayout);
        MemorySegment dest = allocateNoInit(elementLayout, elementCount);
        MemorySegment.copy(source, sourceElementLayout, sourceOffset, dest, elementLayout, 0, elementCount);
        return dest;
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * byte array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of the
     * source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the byte elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_BYTE, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfByte elementLayout, byte... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_BYTE, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * short array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of the
     * source array are copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the short elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_SHORT, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfShort elementLayout, short... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_SHORT, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * char array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of the
     * source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the char elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_CHAR, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfChar elementLayout, char... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_CHAR, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * int array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of the
     * source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the int elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_INT, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfInt elementLayout, int... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_INT, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * float array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of
     * the source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the float elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_FLOAT, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfFloat elementLayout, float... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_FLOAT, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * long array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of
     * the source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the long elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_LONG, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfLong elementLayout, long... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_LONG, 0, elements.length);
    }

    /**
     * {@return a new memory segment initialized with the elements in the provided
     * double array}
     * <p>
     * The size of the allocated memory segment is
     * {@code elementLayout.byteSize() * elements.length}. The contents of
     * the source array is copied into the result segment element by element, according
     * to the byte order and alignment constraint of the given element layout.
     *
     * @param elementLayout the element layout of the array to be allocated
     * @param elements      the double elements to be copied to the newly allocated
     *                      memory block
     * @throws IllegalArgumentException if
     *                                  {@code elementLayout.byteAlignment() > elementLayout.byteSize()}
     * @implSpec The default implementation for this method is equivalent to the
     * following code:
     * {@snippet lang = java:
     * this.allocateFrom(layout, MemorySegment.ofArray(array),
     *                   ValueLayout.JAVA_DOUBLE, 0, array.length)
     *}
     */

    default MemorySegment allocateFrom(OfDouble elementLayout, double... elements) {
        return allocateFrom(elementLayout, MemorySegment.ofArray(elements),
                JAVA_DOUBLE, 0, elements.length);
    }

    /**
     * {@return a new memory segment with the given layout}
     *
     * @param layout the layout of the block of memory to be allocated
     * @implSpec The default implementation for this method calls
     * {@code this.allocate(layout.byteSize(), layout.byteAlignment())}.
     */
    default MemorySegment allocate(MemoryLayout layout) {
        Objects.requireNonNull(layout);
        return allocate(layout.byteSize(), layout.byteAlignment());
    }

    /**
     * {@return a new memory segment with the given {@code elementLayout} and {@code count}}
     *
     * @param elementLayout the array element layout
     * @param count         the array element count
     * @throws IllegalArgumentException if {@code elementLayout.byteSize() * count}
     *                                  overflows
     * @throws IllegalArgumentException if {@code count < 0}
     * @implSpec The default implementation for this method calls
     * {@code this.allocate(MemoryLayout.sequenceLayout(count, elementLayout))}.
     */
    default MemorySegment allocate(MemoryLayout elementLayout, long count) {
        Objects.requireNonNull(elementLayout);
        if (count < 0) {
            throw new IllegalArgumentException("Negative array size");
        }
        return allocate(MemoryLayout.sequenceLayout(count, elementLayout));
    }

    /**
     * {@return a new memory segment with the given {@code byteSize}}
     *
     * @param byteSize the size (in bytes) of the block of memory to be allocated
     * @throws IllegalArgumentException if {@code byteSize < 0}
     * @implSpec The default implementation for this method calls
     * {@code this.allocate(byteSize, 1)}.
     */
    default MemorySegment allocate(long byteSize) {
        return allocate(byteSize, 1);
    }

    /**
     * {@return a new memory segment with the given {@code byteSize} and
     * {@code byteAlignment}}
     *
     * @param byteSize      the size (in bytes) of the block of memory
     *                      to be allocated
     * @param byteAlignment the alignment (in bytes) of the block of memory
     *                      to be allocated
     * @throws IllegalArgumentException if {@code byteSize < 0},
     *                                  {@code byteAlignment <= 0},
     *                                  or if {@code byteAlignment} is not a power of 2
     */
    MemorySegment allocate(long byteSize, long byteAlignment);

    /**
     * Returns a segment allocator that responds to allocation requests by returning
     * consecutive slices obtained from the provided segment. Each new allocation
     * request will return a new slice starting at the current offset (modulo additional
     * padding to satisfy alignment constraint), with given size.
     * <p>
     * The returned allocator throws {@link IndexOutOfBoundsException} when a slice of
     * the provided segment with the requested size and alignment cannot be found.
     *
     * @param segment the segment from which the returned allocator should slice from
     * @return a new slicing allocator
     * @throws IllegalArgumentException if the {@code segment} is
     *                                  {@linkplain MemorySegment#isReadOnly() read-only}
     * @implNote A slicing allocator is not <em>thread-safe</em>.
     */
    static SegmentAllocator slicingAllocator(MemorySegment segment) {
        assertWritable(segment);
        return new _SlicingAllocator(segment);
    }

    /**
     * Returns a segment allocator that responds to allocation requests by recycling a
     * single segment. Each new allocation request will return a new slice starting at
     * the segment offset {@code 0}, hence the name <em>prefix allocator</em>.
     * <p>
     * Equivalent to (but likely more efficient than) the following code:
     * {@snippet lang = java:
     * MemorySegment segment = ...
     * SegmentAllocator prefixAllocator = (size, align) -> segment.asSlice(0, size, align);
     *}
     * The returned allocator throws {@link IndexOutOfBoundsException} when a slice of
     * the provided segment with the requested size and alignment cannot be found.
     *
     * @param segment the memory segment to be recycled by the returned allocator
     * @return an allocator that recycles an existing segment upon each new
     * allocation request
     * @throws IllegalArgumentException if the {@code segment} is
     *                                  {@linkplain MemorySegment#isReadOnly() read-only}
     * @apiNote A prefix allocator can be useful to limit allocation requests in case a
     * client knows that they have fully processed the contents of the allocated
     * segment before the subsequent allocation request takes place.
     * @implNote While a prefix allocator is <em>thread-safe</em>, concurrent access on
     * the same recycling allocator might cause a thread to overwrite contents
     * written to the underlying segment by a different thread.
     */
    static SegmentAllocator prefixAllocator(MemorySegment segment) {
        assertWritable(segment);
        return (_AbstractMemorySegmentImpl) segment;
    }

    private static void assertWritable(MemorySegment segment) {
        // Implicit null check
        if (segment.isReadOnly()) {
            throw new IllegalArgumentException("read-only segment");
        }
    }

    private MemorySegment allocateNoInit(long byteSize) {
        return this instanceof _ArenaImpl arenaImpl ?
                arenaImpl.allocateNoInit(byteSize, 1) :
                allocate(byteSize);
    }


    private MemorySegment allocateNoInit(MemoryLayout layout) {
        return this instanceof _ArenaImpl arenaImpl ?
                arenaImpl.allocateNoInit(layout.byteSize(), layout.byteAlignment()) :
                allocate(layout);
    }


    private MemorySegment allocateNoInit(MemoryLayout layout, long size) {
        return this instanceof _ArenaImpl arenaImpl ?
                arenaImpl.allocateNoInit(layout.byteSize() * size, layout.byteAlignment()) :
                allocate(layout, size);
    }
}

/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.v7878.invoke.VarHandle;
import com.v7878.r8.annotations.NoSideEffects;

import java.nio.ByteOrder;

/**
 * A layout that models values of basic data types. Examples of values modeled by
 * a value layout are <em>integral</em> values (either signed or unsigned),
 * <em>floating-point</em> values and <em>address</em> values.
 * <p>
 * Each value layout has a size, an alignment (both expressed in bytes),
 * a {@linkplain ByteOrder byte order}, and a <em>carrier</em>, that is, the Java type
 * that should be used when {@linkplain MemorySegment#get(OfInt, long) accessing} a
 * region of memory using the value layout.
 * <p>
 * <p>
 * This class defines useful value layout constants for Java primitive types and
 * addresses.
 *
 * @apiNote Some characteristics of the Java layout constants are platform-dependent.
 * For instance, the byte order of these constants is set to the
 * {@linkplain ByteOrder#nativeOrder() native byte order}, thus making it easy
 * to work with other APIs, such as arrays and {@link java.nio.ByteBuffer}.
 * @implSpec implementing classes and subclasses are immutable, thread-safe and
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
public sealed interface ValueLayout extends MemoryLayout
        permits ValueLayout.OfBoolean, ValueLayout.OfByte, ValueLayout.OfChar,
        ValueLayout.OfShort, ValueLayout.OfInt, ValueLayout.OfFloat,
        ValueLayout.OfLong, ValueLayout.OfDouble, AddressLayout {

    /**
     * {@return the value's byte order}
     */
    ByteOrder order();

    /**
     * {@return a value layout with the same characteristics as this layout, but with the given byte order}
     *
     * @param order the desired byte order
     */
    ValueLayout withOrder(ByteOrder order);

    /**
     * {@inheritDoc}
     */
    @Override
    ValueLayout withoutName();

    /**
     * {@return the carrier associated with this value layout}
     */
    Class<?> carrier();

    /**
     * {@inheritDoc}
     */
    @Override
    ValueLayout withName(String name);

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    ValueLayout withByteAlignment(long byteAlignment);

    /**
     * {@return a var handle which can be used to access values described by this value
     * layout, in a given memory segment}
     * <p>
     * The returned var handle's {@linkplain VarHandle#varType() var type} is the
     * {@linkplain ValueLayout#carrier() carrier type} of this value layout, and the
     * list of coordinate types is {@code (MemorySegment, long)}, where the
     * memory segment coordinate corresponds to the memory segment to be accessed, and
     * the {@code long} coordinate corresponds to the byte offset into the accessed
     * memory segment at which the access occurs.
     * <p>
     * The returned var handle checks that accesses are aligned according to
     * this value layout's {@linkplain MemoryLayout#byteAlignment() alignment constraint}.
     *
     * @apiNote This method is similar, but more efficient than calling
     * {@code MemoryLayout#varHandle(PathElement...)} with an empty path
     * element array, as it avoids the creation of the var args array.
     * @apiNote The returned var handle features certain
     * <a href="MemoryLayout.html#access-mode-restrictions">access mode restrictions</a>
     * common to all memory access var handles derived from memory layouts.
     * @see MemoryLayout#varHandle(PathElement...)
     */
    VarHandle varHandle();

    /**
     * A value layout whose carrier is {@code boolean.class}.
     *
     * @see #JAVA_BOOLEAN
     */
    sealed interface OfBoolean extends ValueLayout permits _ValueLayouts.OfBooleanImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfBoolean withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfBoolean withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfBoolean withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfBoolean withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code byte.class}.
     *
     * @see #JAVA_BYTE
     */
    sealed interface OfByte extends ValueLayout permits _ValueLayouts.OfByteImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfByte withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfByte withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfByte withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfByte withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code char.class}.
     *
     * @see #JAVA_CHAR
     * @see #JAVA_CHAR_UNALIGNED
     */
    sealed interface OfChar extends ValueLayout permits _ValueLayouts.OfCharImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfChar withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfChar withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfChar withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfChar withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code short.class}.
     *
     * @see #JAVA_SHORT
     * @see #JAVA_SHORT_UNALIGNED
     */
    sealed interface OfShort extends ValueLayout permits _ValueLayouts.OfShortImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfShort withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfShort withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfShort withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfShort withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code int.class}.
     *
     * @see #JAVA_INT
     * @see #JAVA_INT_UNALIGNED
     */
    sealed interface OfInt extends ValueLayout permits _ValueLayouts.OfIntImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfInt withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfInt withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfInt withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfInt withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code float.class}.
     *
     * @see #JAVA_FLOAT
     * @see #JAVA_FLOAT_UNALIGNED
     */
    sealed interface OfFloat extends ValueLayout permits _ValueLayouts.OfFloatImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfFloat withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfFloat withoutName();

        /**
         * {@inheritDoc}
         */
        @Override
        OfFloat withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfFloat withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code long.class}.
     *
     * @see #JAVA_LONG
     * @see #JAVA_LONG_UNALIGNED
     */
    sealed interface OfLong extends ValueLayout permits _ValueLayouts.OfLongImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfLong withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfLong withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfLong withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfLong withOrder(ByteOrder order);

    }

    /**
     * A value layout whose carrier is {@code double.class}.
     *
     * @see #JAVA_DOUBLE
     * @see #JAVA_DOUBLE_UNALIGNED
     */
    sealed interface OfDouble extends ValueLayout permits _ValueLayouts.OfDoubleImpl {

        /**
         * {@inheritDoc}
         */
        @Override
        OfDouble withName(String name);

        /**
         * {@inheritDoc}
         */
        @Override
        OfDouble withoutName();

        /**
         * {@inheritDoc}
         *
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        OfDouble withByteAlignment(long byteAlignment);

        /**
         * {@inheritDoc}
         */
        @Override
        OfDouble withOrder(ByteOrder order);

    }

    /**
     * An address layout constant whose size is the same as that of a
     * machine address ({@code size_t}), byte alignment set to {@code sizeof(size_t)},
     * byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    AddressLayout ADDRESS = _ValueLayouts.OfAddressImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code byte},
     * byte alignment set to 1, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfByte JAVA_BYTE = _ValueLayouts.OfByteImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code boolean},
     * byte alignment set to 1, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfBoolean JAVA_BOOLEAN = _ValueLayouts.OfBooleanImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code char},
     * byte alignment set to 2, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfChar JAVA_CHAR = _ValueLayouts.OfCharImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code short},
     * byte alignment set to 2, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfShort JAVA_SHORT = _ValueLayouts.OfShortImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code int},
     * byte alignment set to 4, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfInt JAVA_INT = _ValueLayouts.OfIntImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code long},
     * byte alignment set to 8, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfLong JAVA_LONG = _ValueLayouts.OfLongImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code float},
     * byte alignment set to 4, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfFloat JAVA_FLOAT = _ValueLayouts.OfFloatImpl.of(ByteOrder.nativeOrder());

    /**
     * A value layout constant whose size is the same as that of a Java {@code double},
     * byte alignment set to 8, and byte order set to {@link ByteOrder#nativeOrder()}.
     */
    @NoSideEffects
    OfDouble JAVA_DOUBLE = _ValueLayouts.OfDoubleImpl.of(ByteOrder.nativeOrder());

    /**
     * An unaligned address layout constant whose size is the same as that of a
     * machine address ({@code size_t}), and byte order set to
     * {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * ADDRESS.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    AddressLayout ADDRESS_UNALIGNED = ADDRESS.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code char}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_CHAR.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfChar JAVA_CHAR_UNALIGNED = JAVA_CHAR.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code short}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_SHORT.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfShort JAVA_SHORT_UNALIGNED = JAVA_SHORT.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code int}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_INT.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfInt JAVA_INT_UNALIGNED = JAVA_INT.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code long}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_LONG.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfLong JAVA_LONG_UNALIGNED = JAVA_LONG.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code float}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_FLOAT.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfFloat JAVA_FLOAT_UNALIGNED = JAVA_FLOAT.withByteAlignment(1);

    /**
     * An unaligned value layout constant whose size is the same as that of a Java {@code double}
     * and byte order set to {@link ByteOrder#nativeOrder()}.
     * Equivalent to the following code:
     * {@snippet lang = java:
     * JAVA_DOUBLE.withByteAlignment(1);
     *}
     *
     * @apiNote Care should be taken when using unaligned value layouts as they may
     * induce performance and portability issues.
     */
    @NoSideEffects
    OfDouble JAVA_DOUBLE_UNALIGNED = JAVA_DOUBLE.withByteAlignment(1);

}

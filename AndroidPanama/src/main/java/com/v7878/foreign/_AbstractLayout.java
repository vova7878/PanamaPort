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

import android.annotation.SuppressLint;

import com.v7878.foreign.MemoryLayout.PathElement;
import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandles;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

abstract sealed class _AbstractLayout<L extends _AbstractLayout<L> & MemoryLayout>
        permits _AbstractGroupLayout, _PaddingLayoutImpl, _SequenceLayoutImpl, _ValueLayouts.AbstractValueLayout {

    private final long byteSize;
    private final long byteAlignment;
    private final String name;

    _AbstractLayout(long byteSize, long byteAlignment, String name) {
        this.byteSize = _Utils.requireByteSizeValid(byteSize, /* Port-changed */ false);
        this.byteAlignment = requirePowerOfTwoAndGreaterOrEqualToOne(byteAlignment);
        this.name = name;
    }

    public final L withName(String name) {
        return dup(byteAlignment(), name);
    }

    @SuppressWarnings("unchecked")
    public final L withoutName() {
        return name == null ? (L) this : dup(byteAlignment(), null);
    }

    protected final String plain_name() {
        return name;
    }

    public final Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public L withByteAlignment(long byteAlignment) {
        return dup(byteAlignment, name);
    }

    public final long byteAlignment() {
        return byteAlignment;
    }

    public final long byteSize() {
        return byteSize;
    }

    public boolean hasNaturalAlignment() {
        return byteSize == byteAlignment;
    }

    // the following methods have to copy the same Javadoc as in MemoryLayout, or subclasses will just show
    // the Object methods javadoc

    /**
     * {@return the hash code value for this layout}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, byteSize, byteAlignment);
    }

    /**
     * Compares the specified object with this layout for equality. Returns {@code true} if and only if the specified
     * object is also a layout, and it is equal to this layout. Two layouts are considered equal if they are of
     * the same kind, have the same size, name and alignment constraints. Furthermore, depending on the layout kind, additional
     * conditions must be satisfied:
     * <ul>
     *     <li>two value layouts are considered equal if they have the same {@linkplain ValueLayout#order() order},
     *     and {@linkplain ValueLayout#carrier() carrier}</li>
     *     <li>two sequence layouts are considered equal if they have the same element count (see {@link SequenceLayout#elementCount()}), and
     *     if their element layouts (see {@link SequenceLayout#elementLayout()}) are also equal</li>
     *     <li>two group layouts are considered equal if they are of the same type (see {@link StructLayout},
     *     {@link UnionLayout}) and if their member layouts (see {@link GroupLayout#memberLayouts()}) are also equal</li>
     * </ul>
     *
     * @param other the object to be compared for equality with this layout.
     * @return {@code true} if the specified object is equal to this layout.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof _AbstractLayout<?> otherLayout &&
                Objects.equals(name, otherLayout.name) &&
                byteSize == otherLayout.byteSize &&
                byteAlignment == otherLayout.byteAlignment;
    }

    /**
     * {@return the string representation of this layout}
     */
    @Override
    public abstract String toString();

    abstract L dup(long byteAlignment, String name);

    // Port-changed
    @SuppressLint("DefaultLocale")
    protected String decorateLayoutString(String s) {
        s = String.format("%s%d", s, byteSize());
        if (!hasNaturalAlignment()) {
            s = String.format("%s%%%d", s, byteAlignment());
        }
        if (name != null) {
            s = String.format("%s(%s)", s, name);
        }
        return s;
    }

    private static long requirePowerOfTwoAndGreaterOrEqualToOne(long value) {
        if (!_Utils.isPowerOfTwo(value) || // value must be a power of two
                value < 1) { // value must be greater or equal to 1
            throw new IllegalArgumentException("Invalid alignment: " + value);
        }
        return value;
    }

    public long scale(long offset, long index) {
        _Utils.checkNonNegativeArgument(offset, "offset");
        _Utils.checkNonNegativeArgument(index, "index");
        return Math.addExact(offset, Math.multiplyExact(byteSize(), index));
    }

    public MethodHandle scaleHandle() {
        class Holder {
            static final MethodHandle MH_SCALE;

            static {
                try {
                    MH_SCALE = MethodHandles.lookup().findVirtual(MemoryLayout.class, "scale",
                            MethodType.methodType(long.class, long.class, long.class));
                } catch (ReflectiveOperationException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }
        }
        return Holder.MH_SCALE.bindTo(this);
    }


    public long byteOffset(PathElement... elements) {
        return computePathOp(_LayoutPath.rootPath((MemoryLayout) this), _LayoutPath::offset,
                Set.of(_LayoutPath.SequenceElement.class, _LayoutPath.SequenceElementByRange.class,
                        _LayoutPath.DereferenceElement.class), elements);
    }

    public MethodHandle byteOffsetHandle(PathElement... elements) {
        return computePathOp(_LayoutPath.rootPath((MemoryLayout) this), _LayoutPath::offsetHandle,
                Set.of(_LayoutPath.DereferenceElement.class), elements);
    }

    public VarHandle varHandle(PathElement... elements) {
        Objects.requireNonNull(elements);
        if (this instanceof ValueLayout vl && elements.length == 0) {
            return vl.varHandle(); // fast path
        }
        return computePathOp(_LayoutPath.rootPath((MemoryLayout) this), _LayoutPath::dereferenceHandle,
                Set.of(), elements);
    }

    public VarHandle arrayElementVarHandle(PathElement... elements) {
        return VarHandles.collectCoordinates(varHandle(elements), 1, scaleHandle());
    }

    public MethodHandle sliceHandle(PathElement... elements) {
        return computePathOp(_LayoutPath.rootPath((MemoryLayout) this), _LayoutPath::sliceHandle,
                Set.of(_LayoutPath.DereferenceElement.class), elements);
    }

    public MemoryLayout select(PathElement... elements) {
        return computePathOp(_LayoutPath.rootPath((MemoryLayout) this), _LayoutPath::layout,
                Set.of(_LayoutPath.SequenceElementByIndex.class, _LayoutPath.SequenceElementByRange.class,
                        _LayoutPath.DereferenceElement.class), elements);
    }

    private static <Z> Z computePathOp(_LayoutPath path, Function<_LayoutPath, Z> finalizer,
                                       Set<Class<?>> badTypes, PathElement... elements) {
        Objects.requireNonNull(elements);
        for (PathElement e : elements) {
            Objects.requireNonNull(e);
            if (badTypes.contains(e.getClass())) {
                throw new IllegalArgumentException("Invalid selection in layout path: " + e);
            }
            @SuppressWarnings("unchecked")
            UnaryOperator<_LayoutPath> pathOp = (UnaryOperator<_LayoutPath>) e;
            path = pathOp.apply(path);
        }
        return finalizer.apply(path);
    }
}

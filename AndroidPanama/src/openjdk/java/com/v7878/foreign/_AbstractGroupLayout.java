/*
 * Copyright (c) 2019 - 2025 Oracle and/or its affiliates. All rights reserved.
 * Modifications Copyright (c) 2025 Vladimir Kozelkov.
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A compound layout that aggregates multiple <em>member layouts</em>. There are two ways in which member layouts
 * can be combined: if member layouts are laid out one after the other, the resulting group layout is said to be a <em>struct</em>
 * (see {@link MemoryLayout#structLayout(MemoryLayout...)}); conversely, if all member layouts are laid out at the same starting offset,
 * the resulting group layout is said to be a <em>union</em> (see {@link MemoryLayout#unionLayout(MemoryLayout...)}).
 *
 * @implSpec This class is immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
abstract sealed class _AbstractGroupLayout<L extends _AbstractGroupLayout<L> & MemoryLayout>
        extends _AbstractLayout<L>
        permits _StructLayoutImpl, _UnionLayoutImpl {

    private final Kind kind;
    private final List<MemoryLayout> elements;
    final long minByteAlignment;

    _AbstractGroupLayout(Kind kind, List<MemoryLayout> elements, long byteSize, long byteAlignment, long minByteAlignment, String name) {
        super(byteSize, byteAlignment, name); // Subclassing creates toctou problems here
        this.kind = kind;
        this.elements = List.copyOf(elements);
        this.minByteAlignment = minByteAlignment;
    }

    /**
     * Returns the member layouts associated with this group.
     *
     * @return the member layouts associated with this group.
     * @apiNote the order in which member layouts are returned is the same order in which member layouts have
     * been passed to one of the group layout factory methods (see {@link MemoryLayout#structLayout(MemoryLayout...)},
     * {@link MemoryLayout#unionLayout(MemoryLayout...)}).
     */
    public final List<MemoryLayout> memberLayouts() {
        return elements; // "elements" are already unmodifiable.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return decorateLayoutString(elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(kind.delimTag, "[", "]")));
    }

    @Override
    public L withByteAlignment(long byteAlignment) {
        if (byteAlignment < minByteAlignment) {
            throw new IllegalArgumentException("Invalid alignment constraint");
        }
        return super.withByteAlignment(byteAlignment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object other) {
        return this == other ||
                other instanceof _AbstractGroupLayout<?> otherGroup &&
                        super.equals(other) &&
                        kind == otherGroup.kind &&
                        elements.equals(otherGroup.elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return Objects.hash(super.hashCode(), kind, elements);
    }

    @Override
    public final boolean hasNaturalAlignment() {
        return byteAlignment() == minByteAlignment;
    }

    /**
     * The group kind.
     */
    enum Kind {
        /**
         * A 'struct' kind.
         */
        STRUCT(""),
        /**
         * A 'union' kind.
         */
        UNION("|");

        final String delimTag;

        Kind(String delimTag) {
            this.delimTag = delimTag;
        }
    }
}

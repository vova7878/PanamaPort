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

import java.util.Objects;

final class _PaddingLayoutImpl extends _AbstractLayout<_PaddingLayoutImpl> implements PaddingLayout {

    private _PaddingLayoutImpl(long byteSize) {
        this(byteSize, 1, null);
    }

    private _PaddingLayoutImpl(long byteSize, long byteAlignment, String name) {
        super(byteSize, byteAlignment, name);
    }

    // Port-changed
    @Override
    public String toString() {
        return decorateLayoutString("x");
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
                other instanceof _PaddingLayoutImpl otherPadding &&
                        super.equals(other) &&
                        byteSize() == otherPadding.byteSize();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), byteSize());
    }

    @Override
    _PaddingLayoutImpl dup(long byteAlignment, String name) {
        return new _PaddingLayoutImpl(byteSize(), byteAlignment, name);
    }

    @Override
    public boolean hasNaturalAlignment() {
        return true;
    }

    public static PaddingLayout of(long byteSize) {
        return new _PaddingLayoutImpl(byteSize);
    }

}

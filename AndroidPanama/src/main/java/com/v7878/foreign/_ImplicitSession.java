/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.v7878.r8.annotations.DoNotShrinkMembers;
import com.v7878.r8.annotations.KeepCodeAttribute;
import com.v7878.unsafe.Utils;

import java.util.Objects;

//TODO
//import sun.nio.ch.DirectBuffer;

/**
 * This is an implicit, GC-backed memory session. Implicit sessions cannot be closed explicitly.
 * While it would be possible to model an implicit session as a non-closeable view of a shared
 * session, it is better to capture the fact that an implicit session is not just a non-closeable
 * view of some session which might be closeable. This is useful e.g. in the implementations of
 * {@link DirectBuffer#address()}, where obtaining an address of a buffer instance associated
 * with a potentially closeable session is forbidden.
 */
sealed class _ImplicitSession extends _SharedSession {

    public _ImplicitSession() {
        super();
        // Port-changed: Use sun.misc.Cleaner
        //cleaner.register(this, resourceList);
        //TODO?: it`s not normal
        sun.misc.Cleaner.create(this, resourceList);
    }

    @Override
    public void release0() {
        Utils.reachabilityFence(this);
    }

    @Override
    public void acquire0() {
        // do nothing
    }

    @Override
    public boolean isCloseable() {
        return false;
    }

    @Override
    public void justClose() {
        throw nonCloseable();
    }

    /**
     * This is an implicit session that wraps a heap object.
     * Possible objects are: Java arrays, buffers and class loaders.
     */
    static final class ImplicitHeapSession extends _ImplicitSession {
        @DoNotShrinkMembers
        final Object ref;

        @DoNotShrinkMembers
        @KeepCodeAttribute
        public ImplicitHeapSession(Object ref) {
            this.ref = Objects.requireNonNull(ref);
        }
    }
}

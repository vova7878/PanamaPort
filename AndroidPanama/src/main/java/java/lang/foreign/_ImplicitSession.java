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

package java.lang.foreign;

// Port-removed: TODO
//import sun.nio.ch.DirectBuffer;

import com.v7878.unsafe.Utils;

/**
 * This is an implicit, GC-backed memory session. Implicit sessions cannot be closed explicitly.
 * While it would be possible to model an implicit session as a non-closeable view of a shared
 * session, it is better to capture the fact that an implicit session is not just a non-closeable
 * view of some session which might be closeable. This is useful e.g. in the implementations of
 * {@link DirectBuffer#address()}, where obtaining an address of a buffer instance associated
 * with a potentially closeable session is forbidden.
 */
final class _ImplicitSession extends _SharedSession {

    // Port-changed: Use sun.misc.Cleaner
    //public _ImplicitSession(Cleaner cleaner) {
    //    super();
    //    cleaner.register(this, resourceList);
    //}

    public _ImplicitSession() {
        super();
        sun.misc.Cleaner.create(this, resourceList);
    }

    @Override
    protected void release0() {
        // Port-changed: Use Utils instead Reference
        Utils.reachabilityFence(this);
    }

    @Override
    protected void acquire0() {
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
}

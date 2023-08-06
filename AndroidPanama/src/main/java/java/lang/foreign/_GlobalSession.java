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

/**
 * The global, non-closeable, shared session. Similar to a shared session, but its {@link #close()} method throws unconditionally.
 * Adding new resources to the global session, does nothing: as the session can never become not-alive, there is nothing to track.
 * Acquiring and or releasing a memory session similarly does nothing.
 */
final class _GlobalSession extends _MemorySessionImpl {

    // Port-added
    public static final _GlobalSession INSTANCE = new _GlobalSession(null);

    final Object ref;

    public _GlobalSession(Object ref) {
        // Port-changed: Move owner Thread to ConfinedSession
        //super(null, null);
        super(null);
        this.ref = ref;
    }

    @Override
    protected void release0() {
        // do nothing
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

    @Override
    void addInternal(ResourceList.ResourceCleanup resource) {
        // do nothing
    }
}

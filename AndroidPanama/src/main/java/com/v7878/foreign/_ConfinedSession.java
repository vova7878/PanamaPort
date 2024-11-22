/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static com.v7878.unsafe.Reflection.getDeclaredField;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AndroidUnsafe;

/**
 * A confined session, which features an owner thread. The liveness check features an additional
 * confinement check - that is, calling any operation on this session from a thread other than the
 * owner thread will result in an exception. Because of this restriction, checking the liveness bit
 * can be performed in plain mode.
 */
final class _ConfinedSession extends _MemorySessionImpl {

    static final long ASYNC_RELEASE_COUNT_OFFSET = AndroidUnsafe.objectFieldOffset(
            getDeclaredField(_ConfinedSession.class, "asyncReleaseCount"));

    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private int asyncReleaseCount = 0;

    public _ConfinedSession(Thread owner) {
        super(owner, new ConfinedResourceList());
    }

    @Override
    public void acquire0() {
        checkValidState();
        if (acquireCount == MAX_FORKS) {
            throw tooManyAcquires();
        }
        acquireCount++;
    }

    @Override
    public void release0() {
        if (Thread.currentThread() == owner) {
            acquireCount--;
        } else {
            // It is possible to end up here in two cases: this session was kept alive by some other confined session
            // which is implicitly released (in which case the release call comes from the cleaner thread). Or,
            // this session might be kept alive by a shared session, which means the release call can come from any
            // thread.
            AndroidUnsafe.getAndAddIntO(this, ASYNC_RELEASE_COUNT_OFFSET, 1);
        }
    }

    void justClose() {
        checkValidState();
        int asyncCount = AndroidUnsafe.getIntVolatileO(this, ASYNC_RELEASE_COUNT_OFFSET);
        int acquire = acquireCount - asyncCount;
        if (acquire == 0) {
            state = CLOSED;
        } else {
            throw alreadyAcquired(acquire);
        }
    }

    /**
     * A confined resource list; no races are possible here.
     */
    static final class ConfinedResourceList extends ResourceList {
        @Override
        void add(ResourceCleanup cleanup) {
            if (fst != ResourceCleanup.CLOSED_LIST) {
                cleanup.next = fst;
                fst = cleanup;
            } else {
                throw alreadyClosed();
            }
        }

        @Override
        void cleanup() {
            if (fst != ResourceCleanup.CLOSED_LIST) {
                ResourceCleanup prev = fst;
                fst = ResourceCleanup.CLOSED_LIST;
                cleanup(prev);
            } else {
                throw alreadyClosed();
            }
        }
    }
}

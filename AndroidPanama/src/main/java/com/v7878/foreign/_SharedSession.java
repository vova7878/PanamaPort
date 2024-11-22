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

import static com.v7878.unsafe.AndroidUnsafe.compareAndExchangeIntO;
import static com.v7878.unsafe.AndroidUnsafe.compareAndSetIntO;
import static com.v7878.unsafe.AndroidUnsafe.compareAndSetObject;
import static com.v7878.unsafe.AndroidUnsafe.getAndSetObject;
import static com.v7878.unsafe.AndroidUnsafe.getIntVolatileO;
import static com.v7878.unsafe.AndroidUnsafe.getObjectVolatile;
import static com.v7878.unsafe.AndroidUnsafe.putIntVolatileO;
import static com.v7878.unsafe.Reflection.getDeclaredField;

import android.os.Build;

import com.v7878.unsafe.AndroidUnsafe;

/**
 * A shared session, which can be shared across multiple threads. Closing a shared session has to ensure that
 * (i) only one thread can successfully close a session (e.g. in a close vs. close race) and that
 * (ii) no other thread is accessing the memory associated with this session while the segment is being
 * closed. To ensure the former condition, a CAS is performed on the liveness bit. Ensuring the latter
 * is trickier, and require a complex synchronization protocol (see {@link _ScopedMemoryAccess}).
 * Since it is the responsibility of the closing thread to make sure that no concurrent access is possible,
 * checking the liveness bit upon access can be performed in plain mode, as in the confined case.
 */
sealed class _SharedSession extends _MemorySessionImpl permits _ImplicitSession {
    static final long STATE_OFFSET = AndroidUnsafe.objectFieldOffset(
            getDeclaredField(_MemorySessionImpl.class, "state"));
    static final long ACQUIRE_COUNT_OFFSET = AndroidUnsafe.objectFieldOffset(
            getDeclaredField(_MemorySessionImpl.class, "acquireCount"));
    private static final int CLOSED_ACQUIRE_COUNT = -1;

    _SharedSession() {
        super(null, new SharedResourceList());
    }

    @Override
    public void acquire0() {
        int value;
        do {
            value = getIntVolatileO(this, ACQUIRE_COUNT_OFFSET);
            if (value < 0) {
                //segment is not open!
                throw sharedSessionAlreadyClosed();
            } else if (value == MAX_FORKS) {
                //overflow
                throw tooManyAcquires();
            }
        } while (!compareAndSetIntO(this, ACQUIRE_COUNT_OFFSET, value, value + 1));
    }

    @Override
    public void release0() {
        int value;
        do {
            value = getIntVolatileO(this, ACQUIRE_COUNT_OFFSET);
            if (value <= 0) {
                //cannot get here - we can't close segment twice
                throw sharedSessionAlreadyClosed();
            }
        } while (!compareAndSetIntO(this, ACQUIRE_COUNT_OFFSET, value, value - 1));
    }

    void justClose() {
        int acquireCount = compareAndExchangeIntO(this, ACQUIRE_COUNT_OFFSET, 0, CLOSED_ACQUIRE_COUNT);
        if (acquireCount < 0) {
            throw sharedSessionAlreadyClosed();
        } else if (acquireCount > 0) {
            throw alreadyAcquired(acquireCount);
        }
        putIntVolatileO(this, STATE_OFFSET, CLOSED);
    }

    private IllegalStateException sharedSessionAlreadyClosed() {
        // To avoid the situation where a scope fails to be acquired or closed but still reports as
        // alive afterward, we wait for the state to change before throwing the exception
        while (getIntVolatileO(this, STATE_OFFSET) == OPEN) {
            // TODO: Is there any way to improve this code?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Thread.onSpinWait();
            }
        }
        return alreadyClosed();
    }

    /**
     * A shared resource list; this implementation has to handle add vs. add races, as well as add vs. cleanup races.
     */
    static class SharedResourceList extends ResourceList {

        static final long FST_OFFSET = AndroidUnsafe.objectFieldOffset(
                getDeclaredField(ResourceList.class, "fst"));

        @Override
        void add(ResourceCleanup cleanup) {
            while (true) {
                ResourceCleanup prev = (ResourceCleanup) getObjectVolatile(this, FST_OFFSET);
                if (prev == ResourceCleanup.CLOSED_LIST) {
                    // too late
                    throw alreadyClosed();
                }
                cleanup.next = prev;
                if (compareAndSetObject(this, FST_OFFSET, prev, cleanup)) {
                    return; //victory
                }
                // keep trying
            }
        }

        void cleanup() {
            ResourceCleanup prev = (ResourceCleanup) getAndSetObject(
                    this, FST_OFFSET, ResourceCleanup.CLOSED_LIST);
            if (prev == ResourceCleanup.CLOSED_LIST) {
                throw alreadyClosed();
            }
            cleanup(prev);
        }
    }
}

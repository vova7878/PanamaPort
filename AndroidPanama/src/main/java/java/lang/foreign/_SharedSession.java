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

package java.lang.foreign;

import static com.v7878.unsafe.AndroidUnsafe.compareAndExchangeIntO;
import static com.v7878.unsafe.AndroidUnsafe.compareAndSetIntO;
import static com.v7878.unsafe.AndroidUnsafe.compareAndSetObject;
import static com.v7878.unsafe.AndroidUnsafe.getAndSetObject;
import static com.v7878.unsafe.AndroidUnsafe.getIntVolatileO;
import static com.v7878.unsafe.AndroidUnsafe.getObjectVolatile;
import static com.v7878.unsafe.Reflection.getDeclaredField;

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

    // Port-romeved: unused
    //private static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    // Port-added
    static final long STATE_OFFSET = AndroidUnsafe.objectFieldOffset(
            getDeclaredField(_MemorySessionImpl.class, "state"));

    _SharedSession() {
        // Port-changed: Move owner Thread to ConfinedSession
        //super(null, new SharedResourceList());
        super(new SharedResourceList());
    }

    @Override
    protected void acquire0() {
        int value;
        do {
            value = getIntVolatileO(this, STATE_OFFSET);
            if (value < OPEN) {
                //segment is not open!
                throw alreadyClosed();
            } else if (value == MAX_FORKS) {
                //overflow
                throw tooManyAcquires();
            }
        } while (!compareAndSetIntO(this, STATE_OFFSET, value, value + 1));
    }

    @Override
    protected void release0() {
        int value;
        do {
            value = getIntVolatileO(this, STATE_OFFSET);
            if (value <= OPEN) {
                //cannot get here - we can't close segment twice
                throw alreadyClosed();
            }
        } while (!compareAndSetIntO(this, STATE_OFFSET, value, value - 1));
    }

    void justClose() {
        // Port-changed
        //int prevState = (int) STATE.compareAndExchange(this, OPEN, CLOSING);
        //if (prevState < 0) {
        //    throw alreadyClosed();
        //} else if (prevState != OPEN) {
        //    throw alreadyAcquired(prevState);
        //}
        //boolean success = SCOPED_MEMORY_ACCESS.closeScope(this);
        //STATE.setVolatile(this, success ? CLOSED : OPEN);
        //if (!success) {
        //    throw alreadyAcquired(1);
        //}

        int prevState = compareAndExchangeIntO(this, STATE_OFFSET, OPEN, CLOSED);
        if (prevState < OPEN) {
            throw alreadyClosed();
        } else if (prevState != OPEN) {
            throw alreadyAcquired(prevState);
        }
    }

    /**
     * A shared resource list; this implementation has to handle add vs. add races, as well as add vs. cleanup races.
     */
    static class SharedResourceList extends ResourceList {

        // Port-changed: use unsafe
        //static final VarHandle FST;
        //
        //static {
        //    try {
        //        FST = MethodHandles.lookup().findVarHandle(ResourceList.class, "fst", ResourceCleanup.class);
        //    } catch (Throwable ex) {
        //        throw new ExceptionInInitializerError();
        //    }
        //}

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
            // Port-changed
            //// At this point we are only interested about add vs. close races - not close vs. close
            //// (because _MemorySessionImpl::justClose ensured that this thread won the race to close the session).
            //// So, the only "bad" thing that could happen is that some other thread adds to this list
            //// while we're closing it.
            //if (FST.getAcquire(this) != ResourceCleanup.CLOSED_LIST) {
            //    //ok now we're really closing down
            //    ResourceCleanup prev = null;
            //    while (true) {
            //        prev = (ResourceCleanup) FST.getVolatile(this);
            //        // no need to check for DUMMY, since only one thread can get here!
            //        if (FST.compareAndSet(this, prev, ResourceCleanup.CLOSED_LIST)) {
            //            break;
            //        }
            //    }
            //    cleanup(prev);
            //} else {
            //    throw alreadyClosed();
            //}

            ResourceCleanup prev = (ResourceCleanup) getAndSetObject(
                    this, FST_OFFSET, ResourceCleanup.CLOSED_LIST);
            if (prev == ResourceCleanup.CLOSED_LIST) {
                throw alreadyClosed();
            }
            cleanup(prev);
        }
    }
}

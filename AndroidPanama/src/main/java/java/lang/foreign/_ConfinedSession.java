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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A confined session, which features an owner thread. The liveness check features an additional
 * confinement check - that is, calling any operation on this session from a thread other than the
 * owner thread will result in an exception. Because of this restriction, checking the liveness bit
 * can be performed in plain mode.
 */
final class _ConfinedSession extends _MemorySessionImpl {

    // Port-added: Move owner Thread to ConfinedSession
    private final Thread owner;

    // Port-changed: Use AtomicInteger
    //private int asyncReleaseCount = 0;
    //
    //static final VarHandle ASYNC_RELEASE_COUNT;
    //
    //static {
    //    try {
    //        ASYNC_RELEASE_COUNT = MethodHandles.lookup().findVarHandle(_ConfinedSession.class, "asyncReleaseCount", int.class);
    //    } catch (Throwable ex) {
    //        throw new ExceptionInInitializerError(ex);
    //    }
    //}

    private final AtomicInteger ASYNC_RELEASE_COUNT = new AtomicInteger(0);

    public _ConfinedSession(Thread owner) {
        // Port-changed: Move owner Thread to ConfinedSession
        //super(owner, new ConfinedResourceList());
        super(new ConfinedResourceList());
        this.owner = owner;
    }

    // Port-added: Move owner Thread to ConfinedSession
    @Override
    public Thread ownerThread() {
        return owner;
    }

    // Port-added
    @Override
    public void checkStateForAccess() {
        if (owner != null && owner != Thread.currentThread()) {
            throw wrongThread();
        }
        super.checkStateForAccess();
    }

    @Override
    protected void acquire0() {
        checkStateForAccess();
        if (state == MAX_FORKS) {
            throw tooManyAcquires();
        }
        state++;
    }

    @Override
    protected void release0() {
        if (Thread.currentThread() == owner) {
            state--;
        } else {
            // It is possible to end up here in two cases: this session was kept alive by some other confined session
            // which is implicitly released (in which case the release call comes from the cleaner thread). Or,
            // this session might be kept alive by a shared session, which means the release call can come from any
            // thread.
            ASYNC_RELEASE_COUNT.getAndAdd(1);
        }
    }

    void justClose() {
        checkStateForAccess();
        int asyncCount = ASYNC_RELEASE_COUNT.get();
        if ((state - asyncCount) == 0) {
            state = CLOSED;
        } else {
            throw alreadyAcquired(state - asyncCount);
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

    // Port-added: Move owner Thread to ConfinedSession
    static WrongThreadException wrongThread() {
        return new WrongThreadException("Attempted access outside owning thread");
    }
}

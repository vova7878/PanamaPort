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

package java.lang.foreign;

import java.lang.foreign.MemorySegment.Scope;
import java.util.Objects;

/**
 * This class manages the temporal bounds associated with a memory segment as well
 * as thread confinement. A session has a liveness bit, which is updated when the session is closed
 * (this operation is triggered by {@link _MemorySessionImpl#close()}). This bit is consulted prior
 * to memory access (see {@link #checkValidStateRaw()}).
 * There are two kinds of memory session: confined memory session and shared memory session.
 * A confined memory session has an associated owner thread that confines some operations to
 * associated owner thread such as {@link #close()} or {@link #checkValidStateRaw()}.
 * Shared sessions do not feature an owner thread - meaning their operations can be called, in a racy
 * manner, by multiple threads. To guarantee temporal safety in the presence of concurrent thread,
 * shared sessions use a more sophisticated synchronization mechanism, which guarantees that no concurrent
 * access is possible when a session is being closed (see {@link _ScopedMemoryAccess}).
 */
abstract sealed class _MemorySessionImpl
        implements Scope
        permits _ConfinedSession, _GlobalSession, _SharedSession {
    static final int OPEN = 0;
    static final int CLOSING = -1;
    static final int CLOSED = -2;

    // Port-removed: Move to SharedSession
    //static final VarHandle STATE;
    //
    //static {
    //    try {
    //        STATE = MethodHandles.lookup().findVarHandle(MemorySessionImpl.class, "state", int.class);
    //    } catch (Exception ex) {
    //        throw new ExceptionInInitializerError(ex);
    //    }
    //}

    // Port-removed: Move to GlobalSession
    //public static final MemorySessionImpl GLOBAL = new GlobalSession(null);


    // Port-removed: unused
    //static final ScopedAccessError ALREADY_CLOSED = new ScopedAccessError(_MemorySessionImpl::alreadyClosed);
    //static final ScopedAccessError WRONG_THREAD = new ScopedAccessError(MemorySessionImpl::wrongThread);

    // Port-removed: Move owner Thread to ConfinedSession
    //static final int MAX_FORKS = Integer.MAX_VALUE;

    final ResourceList resourceList;
    // Port-removed: Move owner Thread to ConfinedSession
    //final Thread owner;
    int state = OPEN;

    public Arena asArena() {
        return new Arena() {
            @Override
            public Scope scope() {
                return _MemorySessionImpl.this;
            }

            @Override
            public void close() {
                _MemorySessionImpl.this.close();
            }
        };
    }

    public static _MemorySessionImpl toMemorySession(Arena arena) {
        return (_MemorySessionImpl) arena.scope();
    }

    // Port-changed: unused
    //public final boolean isCloseableBy(Thread thread) {
    //    Objects.requireNonNull(thread);
    //    return isCloseable() &&
    //            (owner == null || owner == thread);
    //}

    public void addCloseAction(Runnable runnable) {
        Objects.requireNonNull(runnable);
        addInternal(ResourceList.ResourceCleanup.ofRunnable(runnable));
    }

    /**
     * Add a cleanup action. If a failure occurred (because of a add vs. close race), call the cleanup action.
     * This semantics is useful when allocating new memory segments, since we first do a malloc/mmap and _then_
     * we register the cleanup (free/munmap) against the session; so, if registration fails, we still have to
     * cleanup memory. From the perspective of the client, such a failure would manifest as a factory
     * returning a segment that is already "closed" - which is always possible anyway (e.g. if the session
     * is closed _after_ the cleanup for the segment is registered but _before_ the factory returns the
     * new segment to the client). For this reason, it's not worth adding extra complexity to the segment
     * initialization logic here - and using an optimistic logic works well in practice.
     */
    public void addOrCleanupIfFail(ResourceList.ResourceCleanup resource) {
        try {
            addInternal(resource);
        } catch (Throwable ex) {
            resource.cleanup();
            throw ex;
        }
    }

    void addInternal(ResourceList.ResourceCleanup resource) {
        // Port-changed: Use lock()
        //checkValidState();
        //// Note: from here on we no longer check the session state. Two cases are possible: either the resource cleanup
        //// is added to the list when the session is still open, in which case everything works ok; or the resource
        //// cleanup is added while the session is being closed. In this latter case, what matters is whether we have already
        //// called `ResourceList::cleanup` to run all the cleanup actions. If not, we can still add this resource
        //// to the list (and, in case of an add vs. close race, it might happen that the cleanup action will be
        //// called immediately after).
        //resourceList.add(resource);
        try (SessionScopedLock tmp = lock()) {
            resourceList.add(resource);
        }
    }

    // Port-changed: Move owner Thread to ConfinedSession
    //protected _MemorySessionImpl(Thread owner, ResourceList resourceList) {
    //    this.owner = owner;
    //    this.resourceList = resourceList;
    //}

    protected _MemorySessionImpl(ResourceList resourceList) {
        this.resourceList = resourceList;
    }

    public static _MemorySessionImpl createConfined(Thread thread) {
        return new _ConfinedSession(thread);
    }

    public static _MemorySessionImpl createShared() {
        return new _SharedSession();
    }

    // Port-changed: Use sun.misc.Cleaner
    //public static _MemorySessionImpl createImplicit(Cleaner cleaner) {
    //    return new _ImplicitSession(cleaner);
    //}

    public static _MemorySessionImpl createImplicit() {
        return new _ImplicitSession();
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        _Utils.checkAllocationSizeAndAlign(byteSize, byteAlignment);
        return _NativeMemorySegmentImpl.makeNativeSegment(byteSize, byteAlignment, this);
    }

    // Port-changed: make protected
    //public abstract void acquire0();
    //public abstract void release0();

    protected abstract void acquire0();

    protected abstract void release0();

    // Port-added
    public final class SessionScopedLock implements AutoCloseable {
        SessionScopedLock() {
            acquire0();
        }

        @Override
        public void close() {
            release0();
        }
    }

    public final SessionScopedLock lock() {
        return new SessionScopedLock();
    }

    // Port-changed: unused
    //public void whileAlive(Runnable action) {
    //    Objects.requireNonNull(action);
    //    acquire0();
    //    try {
    //        action.run();
    //    } finally {
    //        release0();
    //    }
    //}

    // Port-changed: Move owner Thread to ConfinedSession
    //public final Thread ownerThread() {
    //    return owner;
    //}

    public Thread ownerThread() {
        return null;
    }

    public final boolean isAccessibleBy(Thread thread) {
        Objects.requireNonNull(thread);
        // Port-added
        Thread owner = ownerThread();
        return owner == null || owner == thread;
    }

    /**
     * Returns true, if this session is still open. This method may be called in any thread.
     *
     * @return {@code true} if this session is not closed yet.
     */
    public boolean isAlive() {
        return state >= OPEN;
    }

    // Port-added
    public void checkStateForAccess() {
        if (state < OPEN) {
            throw alreadyClosed();
        }
    }

    // Port-removed: unused
    ///**
    // * This is a faster version of {@link #checkValidState()}, which is called upon memory access, and which
    // * relies on invariants associated with the memory session implementations (volatile access
    // * to the closed state bit is replaced with plain access). This method should be monomorphic,
    // * to avoid virtual calls in the memory access hot path. This method is not intended as general purpose method
    // * and should only be used in the memory access handle hot path; for liveness checks triggered by other API methods,
    // * please use {@link #checkValidState()}.
    // */
    //public void checkValidStateRaw() {
    //    if (owner != null && owner != Thread.currentThread()) {
    //        throw WRONG_THREAD;
    //    }
    //    if (state < OPEN) {
    //        throw ALREADY_CLOSED;
    //    }
    //}
    //
    ///**
    // * Checks that this session is still alive (see {@link #isAlive()}).
    // *
    // * @throws IllegalStateException if this session is already closed or if this is
    // *                               a confined session and this method is called outside of the owner thread.
    // */
    //public void checkValidState() {
    //    try {
    //        checkValidStateRaw();
    //    } catch (ScopedMemoryAccess.ScopedAccessError error) {
    //        throw error.newRuntimeException();
    //    }
    //}
    //
    //public static void checkValidState(MemorySegment segment) {
    //    ((_AbstractMemorySegmentImpl) segment).sessionImpl().checkValidState();
    //}

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public boolean isCloseable() {
        return true;
    }

    /**
     * Closes this session, executing any cleanup action (where provided).
     *
     * @throws IllegalStateException if this session is already closed or if this is
     *                               a confined session and this method is called outside of the owner thread.
     */
    public void close() {
        justClose();
        resourceList.cleanup();
    }

    abstract void justClose();

    public static _MemorySessionImpl heapSession(Object ref) {
        return new _GlobalSession(ref);
    }

    /**
     * A list of all cleanup actions associated with a memory session. Cleanup actions are modelled as instances
     * of the {@link ResourceCleanup} class, and, together, form a linked list. Depending on whether a session
     * is shared or confined, different implementations of this class will be used, see {@link _ConfinedSession.ConfinedResourceList}
     * and {@link _SharedSession.SharedResourceList}.
     */
    public abstract static class ResourceList implements Runnable {
        ResourceCleanup fst;

        abstract void add(ResourceCleanup cleanup);

        abstract void cleanup();

        public final void run() {
            cleanup(); // cleaner interop
        }

        static void cleanup(ResourceCleanup first) {
            ResourceCleanup current = first;
            while (current != null) {
                current.cleanup();
                current = current.next;
            }
        }

        public abstract static class ResourceCleanup {
            ResourceCleanup next;

            public abstract void cleanup();

            static final ResourceCleanup CLOSED_LIST = new ResourceCleanup() {
                @Override
                public void cleanup() {
                    throw new IllegalStateException("This resource list has already been closed!");
                }
            };

            static ResourceCleanup ofRunnable(Runnable cleanupAction) {
                return new ResourceCleanup() {
                    @Override
                    public void cleanup() {
                        cleanupAction.run();
                    }
                };
            }
        }
    }

    // helper functions to centralize error handling

    // Port-removed: Move owner Thread to ConfinedSession
    //static IllegalStateException tooManyAcquires() {
    //    return new IllegalStateException("Session acquire limit exceeded");
    //}

    static IllegalStateException alreadyAcquired(int acquires) {
        return new IllegalStateException(String.format("Session is acquired by %d clients", acquires));
    }

    static IllegalStateException alreadyClosed() {
        return new IllegalStateException("Already closed");
    }

    // Port-removed: Move owner Thread to ConfinedSession
    //static WrongThreadException wrongThread() {
    //    return new WrongThreadException("Attempted access outside owning thread");
    //}

    static UnsupportedOperationException nonCloseable() {
        return new UnsupportedOperationException("Attempted to close a non-closeable session");
    }

}

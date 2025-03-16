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

import android.annotation.SuppressLint;

import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.foreign._GlobalSession.GlobalHolderSession;
import com.v7878.foreign._ImplicitSession.ImplicitHolderSession;
import com.v7878.foreign._ScopedMemoryAccess.ScopedAccessError;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.sun.cleaner.SunCleaner;

import java.util.Objects;

abstract sealed class _MemorySessionImpl implements Scope
        permits _ConfinedSession, _GlobalSession, _SharedSession {
    /**
     * The value of the {@code state} of a {@code MemorySessionImpl}. The only possible transition
     * is OPEN -> CLOSED. As a result, the states CLOSED and NONCLOSEABLE are stable. This allows
     * us to annotate {@code state} with {@link Stable} and elide liveness check on non-closeable
     * constant scopes, such as {@code GLOBAL_SESSION}.
     */
    static final int OPEN = 0;
    static final int CLOSED = -1;
    static final int NONCLOSEABLE = 1;

    static final int MAX_FORKS = Integer.MAX_VALUE;

    static final ScopedAccessError ALREADY_CLOSED = new ScopedAccessError(_MemorySessionImpl::alreadyClosed);
    static final ScopedAccessError WRONG_THREAD = new ScopedAccessError(_MemorySessionImpl::wrongThread);

    // This is the session of all zero-length memory segments

    final ResourceList resourceList;
    final Thread owner;

    @DoNotShrink
    @DoNotObfuscate
    //@Stable
    int state;
    @DoNotShrink
    @DoNotObfuscate
    int acquireCount;

    public _ArenaImpl asArena() {
        return new _ArenaImpl(this);
    }

    public static _MemorySessionImpl toMemorySession(Arena arena) {
        return (_MemorySessionImpl) arena.scope();
    }

    public final boolean isCloseableBy(Thread thread) {
        Objects.requireNonNull(thread);
        return isCloseable() && (owner == null || owner == thread);
    }

    public void addCloseAction(Runnable runnable) {
        Objects.requireNonNull(runnable);
        addInternal(ResourceList.ResourceCleanup.ofRunnable(runnable));
    }

    /**
     * Add a cleanup action. If a failure occurred (because of an add vs. close race), call the cleanup action.
     * This semantics is useful when allocating new memory segments, since we first do a malloc/mmap and _then_
     * we register the cleanup (free/munmap) against the session; so, if registration fails, we still have to
     * clean up memory. From the perspective of the client, such a failure would manifest as a factory
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
        checkValidState();
        // Note: from here on we no longer check the session state. Two cases are possible: either the resource cleanup
        // is added to the list when the session is still open, in which case everything works ok; or the resource
        // cleanup is added while the session is being closed. In this latter case, what matters is whether we have already
        // called `ResourceList::cleanup` to run all the cleanup actions. If not, we can still add this resource
        // to the list (and, in case of an add vs. close race, it might happen that the cleanup action will be
        // called immediately after).
        resourceList.add(resource);
    }

    protected _MemorySessionImpl(Thread owner, ResourceList resourceList) {
        this.owner = owner;
        this.resourceList = resourceList;
    }

    public static _MemorySessionImpl createConfined(Thread thread) {
        return new _ConfinedSession(thread);
    }

    public static _MemorySessionImpl createShared() {
        return new _SharedSession();
    }

    public static _MemorySessionImpl createImplicit(SunCleaner cleaner) {
        return new _ImplicitSession(cleaner);
    }

    public static _MemorySessionImpl createGlobalHolder(Object ref) {
        return new GlobalHolderSession(ref);
    }

    public static _MemorySessionImpl createImplicitHolder(Object ref) {
        return new ImplicitHolderSession(SunCleaner.systemCleaner(), ref);
    }

    public abstract void release0();

    public abstract void acquire0();

    public final Thread ownerThread() {
        return owner;
    }

    public final boolean isAccessibleBy(Thread thread) {
        Objects.requireNonNull(thread);
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

    /**
     * This is a faster version of {@link #checkValidState()}, which is called upon memory access, and which
     * relies on invariants associated with the memory session implementations (volatile access
     * to the closed state bit is replaced with plain access). This method should be monomorphic,
     * to avoid virtual calls in the memory access hot path. This method is not intended as general purpose method
     * and should only be used in the memory access handle hot path; for liveness checks triggered by other API methods,
     * please use {@link #checkValidState()}.
     */
    public final void checkValidStateRaw() {
        if (owner != null && owner != Thread.currentThread()) {
            throw WRONG_THREAD;
        }
        if (state < OPEN) {
            throw ALREADY_CLOSED;
        }
    }

    /**
     * Checks that this session is still alive (see {@link #isAlive()}).
     *
     * @throws IllegalStateException if this session is already closed or if this is
     *                               a confined session and this method is called outside the owner thread.
     */
    public final void checkValidState() {
        try {
            checkValidStateRaw();
        } catch (ScopedAccessError error) {
            throw error.newRuntimeException();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public final boolean isCloseable() {
        return state <= OPEN;
    }

    /**
     * Closes this session, executing any cleanup action (where provided).
     *
     * @throws IllegalStateException if this session is already closed or if this is
     *                               a confined session and this method is called outside the owner thread.
     */
    public void close() {
        justClose();
        resourceList.cleanup();
    }

    abstract void justClose();

    /**
     * A list of all cleanup actions associated with a memory session. Cleanup actions are modelled as instances
     * of the {@link ResourceCleanup} class, and, together, form a linked list. Depending on whether a session
     * is shared or confined, different implementations of this class will be used, see {@link _ConfinedSession.ConfinedResourceList}
     * and {@link _SharedSession.SharedResourceList}.
     */
    public abstract static class ResourceList implements Runnable {
        @DoNotShrink
        @DoNotObfuscate
        ResourceCleanup fst;

        abstract void add(ResourceCleanup cleanup);

        abstract void cleanup();

        public final void run() {
            cleanup(); // cleaner interop
        }

        static void cleanup(ResourceCleanup first) {
            cleanup(first, null);
        }

        static void cleanup(ResourceCleanup first, RuntimeException pendingException) {
            ResourceCleanup current = first;
            while (current != null) {
                pendingException = cleanupSingle(current, pendingException);
                current = current.next;
            }
            if (pendingException != null) {
                throw pendingException;
            }
        }

        static RuntimeException cleanupSingle(ResourceCleanup resource, RuntimeException pendingException) {
            try {
                resource.cleanup();
            } catch (RuntimeException ex) {
                if (pendingException == null) {
                    pendingException = ex;
                } else if (ex != pendingException) {
                    // note: self-suppression is not supported
                    pendingException.addSuppressed(ex);
                }
            }
            return pendingException;
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

    static IllegalStateException tooManyAcquires() {
        return new IllegalStateException("Session acquire limit exceeded");
    }

    @SuppressLint("DefaultLocale")
    static IllegalStateException alreadyAcquired(int acquires) {
        return new IllegalStateException(String.format("Session is acquired by %d clients", acquires));
    }

    static IllegalStateException alreadyClosed() {
        return new IllegalStateException("Already closed");
    }

    static WrongThreadException wrongThread() {
        return new WrongThreadException("Attempted access outside owning thread");
    }

    static UnsupportedOperationException nonCloseable() {
        return new UnsupportedOperationException("Attempted to close a non-closeable session");
    }

}

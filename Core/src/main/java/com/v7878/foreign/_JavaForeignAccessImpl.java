package com.v7878.foreign;

import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.foreign._MemorySessionImpl.ResourceList.ResourceCleanup;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;

import java.util.Objects;

@SuppressWarnings("unused")
final class _JavaForeignAccessImpl {
    @AlwaysInline
    public static boolean isThreadConfined(Scope scope) {
        return ((_MemorySessionImpl) scope).ownerThread() != null;
    }

    @AlwaysInline
    public static FineClosable lock(Scope scope) {
        record SessionLock(_MemorySessionImpl session) implements FineClosable {
            SessionLock {
                session.acquire0();
            }

            @Override
            public void close() {
                session.release0();
            }
        }
        return scope == null ? FineClosable.NOP :
                new SessionLock((_MemorySessionImpl) scope);
    }

    @AlwaysInline
    public static void checkValidState(Scope scope) {
        ((_MemorySessionImpl) scope).checkValidState();
    }

    @AlwaysInline
    public static void addCloseAction(Scope scope, Runnable cleanup) {
        ((_MemorySessionImpl) scope).addCloseAction(cleanup);
    }

    @AlwaysInline
    public static void addOrCleanupIfFail(Scope scope, Runnable cleanup) {
        ((_MemorySessionImpl) scope).addOrCleanupIfFail(ResourceCleanup.ofRunnable(cleanup));
    }

    @AlwaysInline
    public static MemorySegment objectSegment(Object obj) {
        return _SegmentFactories.fromObject(obj);
    }

    @AlwaysInline
    public static Arena createGlobalHolderArena(Object ref) {
        return _MemorySessionImpl.createGlobalHolder(ref).asArena();
    }

    @AlwaysInline
    public static Arena createImplicitHolderArena(Object ref) {
        return _MemorySessionImpl.createImplicitHolder(ref).asArena();
    }

    @AlwaysInline
    public static MemorySegment makeNativeSegmentUnchecked(
            long min, long byteSize, boolean readOnly, Arena scope, Runnable action) {
        Objects.requireNonNull(scope);
        _Utils.checkNonNegativeArgument(byteSize, "byteSize");
        return _SegmentFactories.makeNativeSegmentUnchecked(min, byteSize,
                _MemorySessionImpl.toMemorySession(scope), readOnly, action);
    }

    @AlwaysInline
    public static MemorySegment allocateNativeSegment(
            long byteSize, long byteAlignment, Arena scope, boolean init) {
        return _SegmentFactories.allocateNativeSegment(byteSize, byteAlignment,
                _MemorySessionImpl.toMemorySession(scope), init);
    }

    @AlwaysInline
    public static MemorySegment mapSegment(
            UnmapperProxy unmapper, long size, boolean readOnly, Arena scope) {
        return _SegmentFactories.mapSegment(unmapper, size, readOnly,
                _MemorySessionImpl.toMemorySession(scope));
    }

    @AlwaysInline
    public static boolean hasNaturalAlignment(MemoryLayout layout) {
        return ((_AbstractLayout<?>) layout).hasNaturalAlignment();
    }

    @AlwaysInline
    public static long unsafeGetOffset(MemorySegment segment) {
        return ((_AbstractMemorySegmentImpl) segment).unsafeGetOffset();
    }

    @AlwaysInline
    public static Object unsafeGetBase(MemorySegment segment) {
        return ((_AbstractMemorySegmentImpl) segment).unsafeGetBase();
    }
}

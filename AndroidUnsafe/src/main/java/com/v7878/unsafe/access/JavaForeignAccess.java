package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.foreign.NativeLibrary;
import com.v7878.unsafe.foreign.RawNativeLibraries;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public abstract class JavaForeignAccess {
    private static final JavaForeignAccess INSTANCE = (JavaForeignAccess) nothrows_run(() -> {
        Method init = getDeclaredMethod(MemorySegment.class, "initAccess");
        return init.invoke(null);
    });

    protected abstract FineClosable _lock(Scope scope);

    public static FineClosable lock(Scope scope) {
        return INSTANCE._lock(scope);
    }

    protected abstract void _addCloseAction(Scope scope, Runnable cleanup);

    public static void addCloseAction(Scope scope, Runnable cleanup) {
        INSTANCE._addCloseAction(scope, cleanup);
    }

    protected abstract void _addOrCleanupIfFail(Scope scope, Runnable cleanup);

    public static void addOrCleanupIfFail(Scope scope, Runnable cleanup) {
        INSTANCE._addOrCleanupIfFail(scope, cleanup);
    }

    protected abstract MemorySegment _objectSegment(Object obj);

    public static MemorySegment objectSegment(Object obj) {
        return INSTANCE._objectSegment(obj);
    }

    protected abstract Arena _createGlobalHolderArena(Object ref);

    public static Arena createGlobalHolderArena(Object ref) {
        return INSTANCE._createGlobalHolderArena(ref);
    }

    protected abstract Arena _createImplicitHolderArena(Object ref);

    public static Arena createImplicitHolderArena(Object ref) {
        return INSTANCE._createImplicitHolderArena(ref);
    }

    protected abstract MemorySegment _makeNativeSegmentUnchecked(
            long min, long byteSize, boolean readOnly, Arena scope, Runnable action);

    public static MemorySegment makeNativeSegment(long min, long byteSize, boolean readOnly,
                                                  Arena scope, Runnable action) {
        //TODO: minimal checks
        return INSTANCE._makeNativeSegmentUnchecked(min, byteSize, readOnly, scope, action);
    }

    protected abstract MemorySegment _allocateSegment(
            long byteSize, long byteAlignment, Arena scope,
            boolean use_operator_new_instead_of_malloc);

    public static MemorySegment allocateSegment(
            long byteSize, long byteAlignment, Arena scope,
            boolean use_operator_new_instead_of_malloc) {
        //TODO: minimal checks
        return INSTANCE._allocateSegment(byteSize, byteAlignment,
                scope, use_operator_new_instead_of_malloc);
    }

    public static MemorySegment allocateSegment(long byteSize, long byteAlignment, Arena scope) {
        return allocateSegment(byteSize, byteAlignment, scope, false);
    }

    protected abstract MemorySegment _mapSegment(UnmapperProxy unmapper, long size, boolean readOnly, Arena scope);

    public static MemorySegment mapSegment(UnmapperProxy unmapper, long size, boolean readOnly, Arena scope) {
        return INSTANCE._mapSegment(unmapper, size, readOnly, scope);
    }

    public static SymbolLookup libraryLookup(NativeLibrary library, Arena libArena) {
        Objects.requireNonNull(library);
        Objects.requireNonNull(libArena);

        // register hook to unload library when 'libScope' becomes not alive
        addOrCleanupIfFail(libArena.scope(), () -> RawNativeLibraries.unload(library));

        return name -> {
            long addr = library.find(name);
            return addr == 0L ? Optional.empty() : Optional.of(
                    MemorySegment.ofAddress(addr).reinterpret(libArena, null));
        };
    }

    protected abstract boolean _hasNaturalAlignment(MemoryLayout layout);

    public static boolean hasNaturalAlignment(MemoryLayout layout) {
        return INSTANCE._hasNaturalAlignment(layout);
    }

    protected abstract long _unsafeGetOffset(MemorySegment segment);

    public static long unsafeGetOffset(MemorySegment segment) {
        return INSTANCE._unsafeGetOffset(segment);
    }

    protected abstract Object _unsafeGetBase(MemorySegment segment);

    public static Object unsafeGetBase(MemorySegment segment) {
        return INSTANCE._unsafeGetBase(segment);
    }
}

package com.v7878.unsafe.access;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.foreign.RawJavaForeignAccess;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.foreign.NativeLibrary;
import com.v7878.unsafe.foreign.RawNativeLibraries;

import java.util.Objects;
import java.util.Optional;

public final class JavaForeignAccess {
    @AlwaysInline
    public static boolean isThreadConfined(Scope scope) {
        return RawJavaForeignAccess.isThreadConfined(scope);
    }

    @AlwaysInline
    public static FineClosable lock(Scope scope) {
        return RawJavaForeignAccess.lock(scope);
    }

    @AlwaysInline
    public static void checkValidState(Scope scope) {
        RawJavaForeignAccess.checkValidState(scope);
    }

    @AlwaysInline
    public static void addCloseAction(Scope scope, Runnable cleanup) {
        RawJavaForeignAccess.addCloseAction(scope, cleanup);
    }

    @AlwaysInline
    public static void addOrCleanupIfFail(Scope scope, Runnable cleanup) {
        RawJavaForeignAccess.addOrCleanupIfFail(scope, cleanup);
    }

    @AlwaysInline
    public static MemorySegment objectSegment(Object obj) {
        return RawJavaForeignAccess.objectSegment(obj);
    }

    @AlwaysInline
    public static Arena createGlobalHolderArena(Object ref) {
        return RawJavaForeignAccess.createGlobalHolderArena(ref);
    }

    @AlwaysInline
    public static Arena createImplicitHolderArena(Object ref) {
        return RawJavaForeignAccess.createImplicitHolderArena(ref);
    }

    // NOTE: no @AlwaysInline
    public static MemorySegment makeNativeSegment(long min, long byteSize, boolean readOnly,
                                                  Arena scope, Runnable action) {
        return RawJavaForeignAccess.makeNativeSegment(min, byteSize, readOnly, scope, action);
    }

    @AlwaysInline
    public static MemorySegment allocateNativeSegment(
            long byteSize, long byteAlignment, Arena scope, boolean init) {
        return RawJavaForeignAccess.allocateNativeSegment(byteSize,
                byteAlignment, scope, init);
    }

    @AlwaysInline
    public static MemorySegment allocateNoInit(long byteSize, long byteAlignment, Arena scope) {
        return allocateNativeSegment(byteSize, byteAlignment, scope, false);
    }

    @AlwaysInline
    public static MemorySegment mapSegment(UnmapperProxy unmapper, long size, boolean readOnly, Arena scope) {
        return RawJavaForeignAccess.mapSegment(unmapper, size, readOnly, scope);
    }

    @AlwaysInline
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

    @AlwaysInline
    public static boolean hasNaturalAlignment(MemoryLayout layout) {
        return RawJavaForeignAccess.hasNaturalAlignment(layout);
    }

    @AlwaysInline
    public static long unsafeGetOffset(MemorySegment segment) {
        return RawJavaForeignAccess.unsafeGetOffset(segment);
    }

    @AlwaysInline
    public static Object unsafeGetBase(MemorySegment segment) {
        return RawJavaForeignAccess.unsafeGetBase(segment);
    }
}

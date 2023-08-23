package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.nothrows_run;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

public class RawNativeLibraries {
    public static final int RTLD_LOCAL = 0;
    public static final int RTLD_LAZY = 0x1;
    public static final int RTLD_NOW = IS64BIT ? 0x2 : 0x0;
    public static final int RTLD_GLOBAL = IS64BIT ? 0x100 : 0x2;
    public static final int RTLD_NOLOAD = 0x00004;
    public static final int RTLD_NODELETE = 0x01000;

    public static final long RTLD_DEFAULT = IS64BIT ? 0L : -1L;
    public static final long RTLD_NEXT = IS64BIT ? -1L : -2L;

    private static final ValueLayout WORD = IS64BIT ? JAVA_LONG : JAVA_INT;

    private static final MethodHandle dlopen = Linker.nativeLinker()
            .downcallHandle(LibDL.dlopen, FunctionDescriptor.of(WORD, WORD, JAVA_INT)
                    /*TODO: , isTrivial()*/);

    public static long dlopen(String path, int flags) {
        Objects.requireNonNull(path);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_path = arena.allocateUtf8String(path);
            return nothrows_run(() -> IS64BIT ?
                    (long) dlopen.invokeExact(c_path.address(), flags) :
                    (int) dlopen.invokeExact((int) c_path.address(), flags));
        }
    }

    public static long dlopen(String path) {
        return dlopen(path, RTLD_NOW);
    }

    private static final MethodHandle dlsym = Linker.nativeLinker()
            .downcallHandle(LibDL.dlsym, FunctionDescriptor.of(WORD, WORD, WORD)
                    /*TODO: , isTrivial()*/);

    public static long dlsym(long handle, String name) {
        Objects.requireNonNull(name);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateUtf8String(name);
            return nothrows_run(() -> IS64BIT ?
                    (long) dlsym.invokeExact(handle, c_name.address()) :
                    (int) dlsym.invokeExact((int) handle, (int) c_name.address()));
        }
    }

    private static final MethodHandle dlclose = Linker.nativeLinker()
            .downcallHandle(LibDL.dlclose, FunctionDescriptor.of(JAVA_INT, WORD)
                    /*TODO: , isTrivial()*/);

    public static void dlclose(long handle) {
        //TODO: check result?
        Object ignore = nothrows_run(() -> IS64BIT ?
                (int) dlclose.invokeExact(handle) :
                (int) dlclose.invokeExact((int) handle));
    }

    // TODO
    //private static final Supplier<MethodHandle> dlerror = runOnce(
    //        () -> SymbolLookup.defaultLookup().lookupHandle("dlerror",
    //                FunctionDescriptor.of(ADDRESS)));
    //
    //static String dlerror() {
    //    Pointer msg = (Pointer) nothrows_run(() -> dlerror.get().invoke());
    //    return msg.isNull() ? null : msg.getCString();
    //}
}

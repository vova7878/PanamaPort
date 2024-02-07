package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemorySegment.NULL;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.Utils;

import java.nio.file.Path;
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

    public static long dlopen(String path, int flags) {
        Objects.requireNonNull(path);
        if (Utils.containsNullChars(path)) return 0;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_path = arena.allocateFrom(path);
            return LibDL.dlopen(c_path.address(), flags);
        }
    }

    public static long dlopen(String path) {
        return dlopen(path, RTLD_NOW);
    }

    public static long dlsym(long handle, String name) {
        Objects.requireNonNull(name);
        if (Utils.containsNullChars(name)) return 0;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            return LibDL.dlsym(handle, c_name.address());
        }
    }

    public static void dlclose(long handle) {
        //TODO: check result?
        int ignore = LibDL.dlclose(handle);
    }

    public static String dlerror() {
        MemorySegment msg = MemorySegment.ofAddress(LibDL.dlerror())
                .reinterpret(MemoryLayout.sequenceLayout(JAVA_BYTE).byteSize());
        return NULL.equals(msg) ? null : msg.getString(0);
    }

    public static long findNative(ClassLoader loader, String name) {
        //TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static NativeLibrary load(Path path) {
        return load(path.toFile().toString());
    }

    public static NativeLibrary load(String pathname) {
        long handle = dlopen(pathname);
        //TODO: check dlerror
        if (handle == 0) return null;
        return new RawNativeLibraryImpl(handle, pathname);
    }

    public static void unload(NativeLibrary lib) {
        Objects.requireNonNull(lib);
        RawNativeLibraryImpl nl = (RawNativeLibraryImpl) lib;
        nl.unload();
    }


    private static class RawNativeLibraryImpl extends NativeLibrary {
        private final String name;
        private long handle;

        RawNativeLibraryImpl(long handle, String name) {
            this.handle = handle;
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        private long handle() {
            if (handle == 0) {
                throw new IllegalStateException("library is closed");
            }
            return handle;
        }

        @Override
        public long find(String symbol_name) {
            synchronized (this) {
                return dlsym(handle(), symbol_name);
            }
        }

        public void unload() {
            synchronized (this) {
                dlclose(handle());
                handle = 0;
            }
        }
    }
}

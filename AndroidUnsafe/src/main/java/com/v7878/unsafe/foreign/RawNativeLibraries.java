package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.foreign.LibDL.RTLD_DEFAULT;
import static com.v7878.unsafe.foreign.LibDL.dlclose;
import static com.v7878.unsafe.foreign.LibDL.dlerror;
import static com.v7878.unsafe.foreign.LibDL.dlopen;
import static com.v7878.unsafe.foreign.LibDL.dlsym;
import static com.v7878.unsafe.foreign.LibDL.dlsym_nochecks;

import com.v7878.unsafe.Utils;

import java.nio.file.Path;
import java.util.Objects;

public class RawNativeLibraries {

    public static final NativeLibrary DEFAULT = load(RTLD_DEFAULT);

    public static long findNative(ClassLoader loader, String name) {
        Objects.requireNonNull(name);
        if (Utils.containsNullChars(name)) return 0;

        long[] out = new long[1];
        if (loader != null) {
            JniLibraries.forEachHandlesInClassLoader(loader, library -> {
                long res = dlsym_nochecks(library.address(), name);
                if (res != 0) {
                    out[0] = res;
                    return true;
                }
                return false;
            });
        }
        if (out[0] != 0) {
            return out[0];
        }
        return dlsym_nochecks(RTLD_DEFAULT, name);
    }

    private static String format_dlerror(String msg) {
        StringBuilder out = new StringBuilder();
        out.append(msg);
        String err = LibDL.dlerror();
        if (err == null) {
            out.append("; no dlerror message");
        } else {
            out.append("; ");
            out.append(err);
        }
        return out.toString();
    }

    public static NativeLibrary load(Path path) {
        return load(path.toFile().toString());
    }

    public static NativeLibrary load(String pathname) {
        if (Utils.containsNullChars(pathname)) {
            throw new IllegalArgumentException("Cannot open library: " + pathname);
        }

        dlerror(); // clear dlerror state before loading

        long handle = dlopen(pathname);

        if (handle == 0) {
            throw new IllegalArgumentException(format_dlerror("Cannot open library: " + pathname));
        }

        return new RawNativeLibraryImpl(handle, pathname);
    }

    public static NativeLibrary load(long handle) {
        return new RawNativeLibraryImpl(handle, "(generic)");
    }

    public static void unload(NativeLibrary lib) {
        Objects.requireNonNull(lib);
        RawNativeLibraryImpl nl = (RawNativeLibraryImpl) lib;
        nl.unload();
    }

    private static class RawNativeLibraryImpl extends NativeLibrary {
        private final String name;
        private final long handle;
        private boolean closed;

        RawNativeLibraryImpl(long handle, String name) {
            this.handle = handle;
            this.name = name;
            this.closed = false;
        }

        @Override
        public String name() {
            return name;
        }

        private long handle() {
            if (closed) {
                throw new IllegalStateException("library is closed");
            }
            return handle;
        }

        @Override
        public long find(String symbol_name) {
            Objects.requireNonNull(symbol_name);
            if (Utils.containsNullChars(symbol_name)) return 0;
            synchronized (this) {
                return dlsym(handle(), symbol_name);
            }
        }

        public void unload() {
            synchronized (this) {
                dlclose(handle());
                closed = true;
            }
        }
    }
}

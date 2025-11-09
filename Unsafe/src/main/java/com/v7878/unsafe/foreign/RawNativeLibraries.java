package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.foreign.LibDL.DEFAULT_CALLER;
import static com.v7878.unsafe.foreign.LibDL.RTLD_LOCAL;
import static com.v7878.unsafe.foreign.LibDL.RTLD_NOW;
import static com.v7878.unsafe.foreign.LibDL.cdlopen;
import static com.v7878.unsafe.foreign.LibDL.cdlsym;
import static com.v7878.unsafe.foreign.LibDL.dlclose;
import static com.v7878.unsafe.foreign.LibDL.dlerror;
import static com.v7878.unsafe.foreign.LibDL.dlsym;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.LibDLExt.Namespace;

import java.nio.file.Path;
import java.util.Objects;

public class RawNativeLibraries {
    public static final NativeLibrary DEFAULT = load("libc.so");
    public static final SymbolLookup DEFAULT_LOOKUP = JavaForeignAccess
            .libraryLookup(RawNativeLibraries.DEFAULT, Arena.global());

    public static long findNative(ClassLoader loader, String name) {
        Objects.requireNonNull(name);
        if (Utils.containsNullChars(name)) return 0;

        long[] out = new long[1];
        if (loader != null) {
            JniLibraries.forEachHandlesInClassLoader(loader, library -> {
                long res = dlsym(library.nativeAddress(), name);
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
        return DEFAULT.find(name);
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

    public static NativeLibrary cload(String pathname, Namespace namespace, int flags, long caller_addr) {
        if (Utils.containsNullChars(pathname)) {
            throw new IllegalArgumentException("Cannot open library: " + pathname);
        }

        dlerror(); // clear dlerror state before loading

        long handle = namespace == null ? cdlopen(pathname, flags, caller_addr) :
                LibDLExt.cdlopen_ext(pathname, namespace, flags, caller_addr);

        if (handle == 0) {
            throw new IllegalArgumentException(format_dlerror("Cannot open library: " + pathname));
        }

        return new RawNativeLibraryImpl(handle, caller_addr, pathname);
    }

    public static NativeLibrary load(String pathname, Namespace namespace, int flags) {
        return cload(pathname, namespace, flags, DEFAULT_CALLER);
    }

    public static NativeLibrary cload(String pathname, Namespace namespace, long caller_addr) {
        return cload(pathname, namespace, RTLD_NOW | RTLD_LOCAL, caller_addr);
    }

    public static NativeLibrary load(String pathname, Namespace namespace) {
        return cload(pathname, namespace, DEFAULT_CALLER);
    }

    public static NativeLibrary cload(String pathname, long caller_addr) {
        return cload(pathname, null, caller_addr);
    }

    public static NativeLibrary load(String pathname) {
        return cload(pathname, DEFAULT_CALLER);
    }

    public static NativeLibrary load(Path path) {
        return load(path.toFile().toString());
    }

    public static NativeLibrary cload(long handle, long caller_addr) {
        return new RawNativeLibraryImpl(handle, caller_addr, "(generic)");
    }

    public static NativeLibrary load(long handle) {
        return cload(handle, DEFAULT_CALLER);
    }

    public static void unload(NativeLibrary lib) {
        Objects.requireNonNull(lib);
        RawNativeLibraryImpl nl = (RawNativeLibraryImpl) lib;
        nl.unload();
    }

    private static class RawNativeLibraryImpl extends NativeLibrary {
        private final String name;
        private final long handle;
        private final long caller;
        private boolean closed;

        RawNativeLibraryImpl(long handle, long caller, String name) {
            this.name = name;
            this.handle = handle;
            this.caller = caller;
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
                return cdlsym(handle(), symbol_name, caller);
            }
        }

        public void unload() {
            synchronized (this) {
                dlclose(handle());
                closed = true;
            }
        }

        @Override
        public String toString() {
            return "RawNativeLibraryImpl{" +
                    "name='" + name + '\'' +
                    ", handle=" + handle +
                    ", closed=" + closed + '}';
        }
    }
}

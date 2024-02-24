package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.foreign.SimpleBulkLinker.WORD_CLASS;
import static com.v7878.unsafe.io.IOUtils.getDescriptorValue;

import android.system.ErrnoException;
import android.system.Os;

import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.invoke.VarHandle;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder2;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class LibDLExt {

    public static final Arena DLEXT_SCOPE = JavaForeignAccess.createHeapArena(LibDLExt.class);
    public static final SymbolLookup DLEXT = SymbolLookup.libraryLookup(
            CORRECT_SDK_INT < 29 ? "libdl.so" : "libdl_android.so", DLEXT_SCOPE);

    public static class AndroidNamespace {
        public static final AndroidNamespace NULL = new AndroidNamespace(0);

        private final long value;

        AndroidNamespace(long value) {
            this.value = value;
        }

        long value() {
            return value;
        }

        @Override
        public String toString() {
            return "AndroidNamespace{" +
                    "value=" + value +
                    '}';
        }
    }

    /**
     * Bitfield definitions for `android_dlextinfo::flags`.
     */
    private static class dlextinfo_flags {
        /**
         * When set, the `reserved_addr` and `reserved_size` fields must point to an
         * already-reserved region of address space which will be used to load the
         * library if it fits.
         * <p>
         * If the reserved region is not large enough, loading will fail.
         */
        public static final long ANDROID_DLEXT_RESERVED_ADDRESS = 0x1;
        /**
         * Like `ANDROID_DLEXT_RESERVED_ADDRESS`, but if the reserved region is not large enough,
         * the linker will choose an available address instead.
         */
        public static final long ANDROID_DLEXT_RESERVED_ADDRESS_HINT = 0x2;
        /**
         * When set, write the GNU RELRO section of the mapped library to `relro_fd`
         * after relocation has been performed, to allow it to be reused by another
         * process loading the same library at the same address. This implies
         * `ANDROID_DLEXT_USE_RELRO`.
         * <p>
         * This is mainly useful for the system WebView implementation.
         */
        public static final long ANDROID_DLEXT_WRITE_RELRO = 0x4;
        /**
         * When set, compare the GNU RELRO section of the mapped library to `relro_fd`
         * after relocation has been performed, and replace any relocated pages that
         * are identical with a version mapped from the file.
         * <p>
         * This is mainly useful for the system WebView implementation.
         */
        public static final long ANDROID_DLEXT_USE_RELRO = 0x8;
        /**
         * Use `library_fd` instead of opening the file by name.
         * The filename parameter is still used to identify the library.
         */
        public static final long ANDROID_DLEXT_USE_LIBRARY_FD = 0x10;
        /**
         * If opening a library using `library_fd` read it starting at `library_fd_offset`.
         * This is mainly useful for loading a library stored within another file (such as uncompressed
         * inside a ZIP archive).
         * This flag is only valid when `ANDROID_DLEXT_USE_LIBRARY_FD` is set.
         */
        public static final long ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET = 0x20;
        /**
         * When set, do not use `stat(2)` to check if the library has already been loaded.
         * <p>
         * This flag allows forced loading of the library in the case when for some
         * reason multiple ELF files share the same filename (because the already-loaded
         * library has been removed and overwritten, for example).
         * <p>
         * Note that if the library has the same `DT_SONAME` as an old one and some other
         * library has the soname in its `DT_NEEDED` list, the first one will be used to resolve any
         * dependencies.
         */
        public static final long ANDROID_DLEXT_FORCE_LOAD = 0x40;
        // Historically we had two other options for ART.
        // They were last available in Android P.
        // Reuse these bits last!
        // ANDROID_DLEXT_FORCE_FIXED_VADDR = 0x80
        // ANDROID_DLEXT_LOAD_AT_FIXED_ADDRESS = 0x100
        /**
         * This flag used to load library in a different namespace. The namespace is
         * specified in `library_namespace`.
         * <p>
         * This flag is for internal use only (since there is no NDK API for namespaces).
         */
        public static final long ANDROID_DLEXT_USE_NAMESPACE = 0x200;
        /**
         * Instructs dlopen to apply `ANDROID_DLEXT_RESERVED_ADDRESS`,
         * `ANDROID_DLEXT_RESERVED_ADDRESS_HINT`, `ANDROID_DLEXT_WRITE_RELRO` and
         * `ANDROID_DLEXT_USE_RELRO` to any libraries loaded as dependencies of the
         * main library as well.
         * <p>
         * This means that if the main library depends on one or more not-already-loaded libraries, they
         * will be loaded consecutively into the region starting at `reserved_addr`, and `reserved_size`
         * must be large enough to contain all of the libraries. The libraries will be loaded in the
         * deterministic order constructed from the DT_NEEDED entries, rather than the more secure random
         * order used by default.
         * <p>
         * Each library's GNU RELRO sections will be written out to `relro_fd` in the same order they were
         * loaded. This will mean that the resulting file is dependent on which of the libraries were
         * already loaded, as only the newly loaded libraries will be included, not any already-loaded
         * dependencies. The caller should ensure that the set of libraries newly loaded is consistent
         * for this to be effective.
         * <p>
         * This is mainly useful for the system WebView implementation.
         */
        public static final long ANDROID_DLEXT_RESERVED_ADDRESS_RECURSIVE = 0x400;
        /**
         * Mask of valid bits.
         */
        public static final long ANDROID_DLEXT_VALID_FLAG_BITS = ANDROID_DLEXT_RESERVED_ADDRESS |
                ANDROID_DLEXT_RESERVED_ADDRESS_HINT |
                ANDROID_DLEXT_WRITE_RELRO |
                ANDROID_DLEXT_USE_RELRO |
                ANDROID_DLEXT_USE_LIBRARY_FD |
                ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET |
                ANDROID_DLEXT_FORCE_LOAD |
                ANDROID_DLEXT_USE_NAMESPACE |
                ANDROID_DLEXT_RESERVED_ADDRESS_RECURSIVE;
    }

    public static class NamespaceType {
        /**
         * A regular namespace is the namespace with a custom search path that does
         * not impose any restrictions on the location of native libraries.
         */
        public static final long REGULAR = 0;
        /**
         * An isolated namespace requires all the libraries to be on the search path
         * or under permitted_when_isolated_path. The search path is the union of
         * ld_library_path and default_library_path.
         */
        public static final long ISOLATED = 1;
        /**
         * The shared namespace clones the list of libraries of the caller namespace upon creation
         * which means that they are shared between namespaces - the caller namespace and the new one
         * will use the same copy of a library if it was loaded prior to android_create_namespace call.
         * <p>
         * Note that libraries loaded after the namespace is created will not be shared.
         * <p>
         * Shared namespaces can be isolated or regular. Note that they do not inherit the search path nor
         * permitted_path from the caller's namespace.
         */
        public static final long SHARED = 2;
        /**
         * This flag instructs linker to enable exempt-list workaround for the namespace.
         * See http://b/26394120 for details.
         */
        public static final long EXEMPT_LIST_ENABLED = 0x08000000;
        /**
         * This flag instructs linker to use this namespace as the anonymous
         * namespace. There can be only one anonymous namespace in a process. If there
         * already an anonymous namespace in the process, using this flag when
         * creating a new namespace causes an error
         */
        public static final long TYPE_ALSO_USED_AS_ANONYMOUS = 0x10000000;

        public static final long TYPE_SHARED_ISOLATED = SHARED | ISOLATED;
    }

    private static final GroupLayout android_dlextinfo_layout = paddedStructLayout(
            JAVA_LONG.withName("flags"),
            ADDRESS.withName("reserved_addr"),
            WORD.withName("reserved_size"),
            JAVA_INT.withName("relro_fd"),
            JAVA_INT.withName("library_fd"),
            JAVA_LONG.withName("library_fd_offset"),
            ADDRESS.withName("library_namespace")
    );

    private static class android_dlextinfo {
        final long flags;
        final int library_fd;
        final long library_fd_offset;
        final long library_namespace;

        android_dlextinfo(long flags, int library_fd,
                          long library_fd_offset, long library_namespace) {
            this.flags = flags;
            this.library_fd = library_fd;
            this.library_fd_offset = library_fd_offset;
            this.library_namespace = library_namespace;
        }
    }

    private enum Function implements SymbolHolder2 {
        android_get_LD_LIBRARY_PATH(void.class, WORD_CLASS, WORD_CLASS),
        android_update_LD_LIBRARY_PATH(void.class, WORD_CLASS),
        android_init_anonymous_namespace(boolean.class, WORD_CLASS, WORD_CLASS),
        android_create_namespace(WORD_CLASS, WORD_CLASS, WORD_CLASS, WORD_CLASS, long.class, WORD_CLASS, WORD_CLASS),
        android_link_namespaces(boolean.class, WORD_CLASS, WORD_CLASS, WORD_CLASS),
        android_get_exported_namespace(WORD_CLASS, WORD_CLASS);

        static {
            SimpleBulkLinker.processSymbols(DLEXT, DLEXT_SCOPE, Function.values());
        }

        private final MethodType type;

        private LongSupplier symbol;
        private Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(LongSupplier symbol) {
            this.symbol = symbol;
        }

        @Override
        public void setHandle(Supplier<MethodHandle> handle) {
            this.handle = handle;
        }

        public long symbol() {
            return symbol.getAsLong();
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + symbol() +
                    ", handle=" + handle() + '}';
        }
    }

    // TODO: void android_get_LD_LIBRARY_PATH(char* buffer, size_t buffer_size)

    public static void android_update_LD_LIBRARY_PATH(String ld_library_path) {
        Objects.requireNonNull(ld_library_path);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ld_library_path = arena.allocateFrom(ld_library_path);
            nothrows_run(() -> Function.android_update_LD_LIBRARY_PATH.handle().invoke(c_ld_library_path.nativeAddress()));
        }
    }

    private static final VarHandle flags_handle = android_dlextinfo_layout.varHandle(groupElement("flags"));
    private static final VarHandle fd_handle = android_dlextinfo_layout.varHandle(groupElement("library_fd"));
    private static final VarHandle fd_offset_handle = android_dlextinfo_layout.varHandle(groupElement("library_fd_offset"));
    private static final VarHandle namespace_handle = android_dlextinfo_layout.varHandle(groupElement("library_namespace"));

    // TODO: make it public api
    private static long android_dlopen_ext(String filename, int flags, android_dlextinfo extinfo) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = filename == null ?
                    MemorySegment.NULL : arena.allocateFrom(filename);
            MemorySegment c_extinfo = arena.allocate(android_dlextinfo_layout);
            flags_handle.set(c_extinfo, 0, extinfo.flags);
            fd_handle.set(c_extinfo, 0, extinfo.library_fd);
            fd_offset_handle.set(c_extinfo, 0, extinfo.library_fd_offset);
            namespace_handle.set(c_extinfo, 0, MemorySegment.ofAddress(extinfo.library_namespace));
            return nothrows_run(() -> (long) LibDL.Function.android_dlopen_ext.handle()
                    .invokeExact(c_filename.nativeAddress(), flags, c_extinfo.nativeAddress()));
        }
    }

    public static long android_dlopen_ext(FileDescriptor fd, long fd_offset, int flags) {
        Objects.requireNonNull(fd);
        android_dlextinfo info = new android_dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_USE_LIBRARY_FD |
                        dlextinfo_flags.ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET,
                getDescriptorValue(fd), fd_offset, 0);
        return android_dlopen_ext(null, flags, info);
    }

    public static long android_dlopen_ext(String filename, AndroidNamespace namespace,
                                          int flags, boolean force_load) {
        Objects.requireNonNull(filename);
        Objects.requireNonNull(namespace);
        android_dlextinfo info = new android_dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_USE_NAMESPACE
                        | (force_load ? dlextinfo_flags.ANDROID_DLEXT_FORCE_LOAD : 0),
                0, 0, namespace.value());
        return android_dlopen_ext(filename, flags, info);
    }

    public static long android_dlopen_ext(String filename, AndroidNamespace namespace, int flags) {
        return android_dlopen_ext(filename, namespace, flags, false);
    }

    public static long android_dlopen_force(String filename, int flags) {
        Objects.requireNonNull(filename);
        android_dlextinfo info = new android_dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_FORCE_LOAD,
                0, 0, 0);
        return android_dlopen_ext(filename, flags, info);
    }

    public static boolean android_init_anonymous_namespace(String shared_libs_sonames,
                                                           String library_search_path) {
        Objects.requireNonNull(shared_libs_sonames);
        Objects.requireNonNull(library_search_path);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_shared_libs_sonames = arena.allocateFrom(shared_libs_sonames);
            MemorySegment c_library_search_path = arena.allocateFrom(library_search_path);
            return nothrows_run(() -> (boolean) Function.android_init_anonymous_namespace.handle()
                    .invoke(c_shared_libs_sonames.nativeAddress(), c_library_search_path.nativeAddress()));
        }
    }

    public static AndroidNamespace android_create_namespace(
            String name, String ld_library_path, String default_library_path,
            long type, String permitted_when_isolated_path, AndroidNamespace parent) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(ld_library_path);
        Objects.requireNonNull(default_library_path);
        Objects.requireNonNull(permitted_when_isolated_path);
        Objects.requireNonNull(parent);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            MemorySegment c_ld_library_path = arena.allocateFrom(ld_library_path);
            MemorySegment c_default_library_path = arena.allocateFrom(default_library_path);
            MemorySegment c_permitted_when_isolated_path = arena.allocateFrom(permitted_when_isolated_path);
            return nothrows_run(() -> new AndroidNamespace((long) Function.android_create_namespace.handle()
                    .invoke(c_name.nativeAddress(), c_ld_library_path.nativeAddress(), c_default_library_path.nativeAddress(),
                            type, c_permitted_when_isolated_path.nativeAddress(), parent.value())));
        }
    }

    public static boolean android_link_namespaces(AndroidNamespace namespace_from,
                                                  AndroidNamespace namespace_to,
                                                  String shared_libs_sonames) {
        Objects.requireNonNull(shared_libs_sonames);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_shared_libs_sonames = arena.allocateFrom(shared_libs_sonames);
            return nothrows_run(() -> (boolean) Function.android_link_namespaces.handle().invoke(
                    namespace_from.value(), namespace_to.value(), c_shared_libs_sonames.nativeAddress()));
        }
    }

    public static AndroidNamespace android_get_exported_namespace(String name) {
        Objects.requireNonNull(name);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            long value = nothrows_run(() -> (long) Function
                    .android_get_exported_namespace.handle().invoke(c_name.nativeAddress()));
            return value == 0 ? null : new AndroidNamespace(value);
        }
    }

    public static long mem_dlopen(MemorySegment segment, int flags) {
        long length = segment.byteSize();
        try {
            FileDescriptor fd = IOUtils.ashmem_create_region(
                    "(mem_dlopen)", length);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment target = IOUtils.map(fd, 0, length, arena);
                target.copyFrom(segment);
                target.force();
                return android_dlopen_ext(fd, 0, flags);
            } finally {
                try {
                    Os.close(fd);
                } catch (ErrnoException e) { /* swallow exception */ }
            }
        } catch (ErrnoException e) {
            return 0;
        }
    }

    //TODO
    //public static final AndroidNamespace DEFAULT_NAMESPACE = ...;
}

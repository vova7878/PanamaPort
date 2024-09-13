package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.foreign.LibDL.RTLD_NOW;
import static com.v7878.unsafe.io.IOUtils.getDescriptorValue;

import android.system.ErrnoException;
import android.system.Os;

import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.invoke.VarHandle;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;
import java.util.Objects;

public class LibDLExt {
    private static final GroupLayout dlextinfo_layout = paddedStructLayout(
            JAVA_LONG.withName("flags"),
            ADDRESS.withName("reserved_addr"),
            WORD.withName("reserved_size"),
            JAVA_INT.withName("relro_fd"),
            JAVA_INT.withName("library_fd"),
            JAVA_LONG.withName("library_fd_offset"),
            ADDRESS.withName("library_namespace")
    );

    private static final VarHandle VH_FLAGS = dlextinfo_layout.varHandle(groupElement("flags"));
    private static final VarHandle VH_LIBRARY_FD = dlextinfo_layout.varHandle(groupElement("library_fd"));
    private static final VarHandle VH_FD_OFFSET = dlextinfo_layout.varHandle(groupElement("library_fd_offset"));
    private static final VarHandle VH_NAMESPACE = dlextinfo_layout.varHandle(groupElement("library_namespace"));

    public static class Namespace {
        public static final Namespace NULL = new Namespace(0);

        private final long value;

        Namespace(long value) {
            this.value = value;
        }

        long value() {
            return value;
        }

        @Override
        public String toString() {
            return "Namespace{" +
                    "value=" + value +
                    '}';
        }
    }

    /**
     * Bitfield definitions for `dlextinfo::flags`.
     */
    @SuppressWarnings("unused")
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
         * See b/26394120 for details.
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

    private record dlextinfo(long flags, int library_fd,
                             long library_fd_offset, long library_namespace) {
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();
        @ApiSensitive
        private static final SymbolLookup DLEXT = SymbolLookup.libraryLookup(
                CORRECT_SDK_INT < 29 ? "libdl.so" : "libdl_android.so", SCOPE);

        @SymbolGenerator(method = "s_android_dlopen_ext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long dlopen_ext(long filename, int flags, long info);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static MemorySegment s_android_dlopen_ext() {
            return LibDL.s_android_dlopen_ext;
        }

        @LibrarySymbol(name = "android_update_LD_LIBRARY_PATH")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void update_LD_LIBRARY_PATH(long ld_library_path);

        @LibrarySymbol(name = "android_init_anonymous_namespace")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean init_anonymous_namespace(
                long shared_libs_sonames, long library_search_path);

        @LibrarySymbol(name = "android_create_namespace")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {
                LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG, LONG_AS_WORD, LONG_AS_WORD})
        abstract long create_namespace(
                long name, long ld_library_path, long default_library_path,
                long type, long permitted_when_isolated_path, long parent);

        @LibrarySymbol(name = "android_link_namespaces")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean link_namespaces(
                long namespace_from, long namespace_to, long shared_libs_sonames);

        @LibrarySymbol(name = "android_get_exported_namespace")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long get_exported_namespace(long name);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, DLEXT));
    }

    public static void update_LD_LIBRARY_PATH(String ld_library_path) {
        Objects.requireNonNull(ld_library_path);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ld_library_path = arena.allocateFrom(ld_library_path);
            Native.INSTANCE.update_LD_LIBRARY_PATH(c_ld_library_path.nativeAddress());
        }
    }

    // TODO: make it public api
    private static long dlopen_ext(String filename, int flags, dlextinfo extinfo) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = filename == null ?
                    MemorySegment.NULL : arena.allocateFrom(filename);
            MemorySegment c_extinfo = arena.allocate(dlextinfo_layout);
            VH_FLAGS.set(c_extinfo, 0, extinfo.flags);
            VH_LIBRARY_FD.set(c_extinfo, 0, extinfo.library_fd);
            VH_FD_OFFSET.set(c_extinfo, 0, extinfo.library_fd_offset);
            VH_NAMESPACE.set(c_extinfo, 0, MemorySegment.ofAddress(extinfo.library_namespace));
            return Native.INSTANCE.dlopen_ext(c_filename.nativeAddress(), flags, c_extinfo.nativeAddress());
        }
    }

    public static long dlopen_ext(FileDescriptor fd, long fd_offset, int flags) {
        Objects.requireNonNull(fd);
        dlextinfo info = new dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_USE_LIBRARY_FD |
                        dlextinfo_flags.ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET,
                getDescriptorValue(fd), fd_offset, 0);
        return dlopen_ext(null, flags, info);
    }

    public static long dlopen_ext(String filename, Namespace namespace,
                                  int flags, boolean force_load) {
        Objects.requireNonNull(filename);
        Objects.requireNonNull(namespace);
        dlextinfo info = new dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_USE_NAMESPACE
                        | (force_load ? dlextinfo_flags.ANDROID_DLEXT_FORCE_LOAD : 0),
                0, 0, namespace.value());
        return dlopen_ext(filename, flags, info);
    }

    public static long dlopen_ext(String filename, Namespace namespace, int flags) {
        return dlopen_ext(filename, namespace, flags, false);
    }

    public static long dlopen_ext(String filename, Namespace namespace) {
        return dlopen_ext(filename, namespace, RTLD_NOW);
    }

    public static long dlopen_force(String filename, int flags) {
        Objects.requireNonNull(filename);
        dlextinfo info = new dlextinfo(
                dlextinfo_flags.ANDROID_DLEXT_FORCE_LOAD,
                0, 0, 0);
        return dlopen_ext(filename, flags, info);
    }

    public static boolean init_anonymous_namespace(String shared_libs_sonames,
                                                   String library_search_path) {
        Objects.requireNonNull(shared_libs_sonames);
        Objects.requireNonNull(library_search_path);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_shared_libs_sonames = arena.allocateFrom(shared_libs_sonames);
            MemorySegment c_library_search_path = arena.allocateFrom(library_search_path);
            return Native.INSTANCE.init_anonymous_namespace(
                    c_shared_libs_sonames.nativeAddress(), c_library_search_path.nativeAddress());
        }
    }

    public static Namespace create_namespace(
            String name, String ld_library_path, String default_library_path,
            long type, String permitted_when_isolated_path, Namespace parent) {
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
            return new Namespace(Native.INSTANCE.create_namespace(
                    c_name.nativeAddress(), c_ld_library_path.nativeAddress(), c_default_library_path.nativeAddress(),
                    type, c_permitted_when_isolated_path.nativeAddress(), parent.value()));
        }
    }

    public static boolean link_namespaces(Namespace namespace_from,
                                          Namespace namespace_to,
                                          String shared_libs_sonames) {
        Objects.requireNonNull(shared_libs_sonames);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_shared_libs_sonames = arena.allocateFrom(shared_libs_sonames);
            return Native.INSTANCE.link_namespaces(namespace_from.value(),
                    namespace_to.value(), c_shared_libs_sonames.nativeAddress());
        }
    }

    public static Namespace get_exported_namespace(String name) {
        Objects.requireNonNull(name);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            long value = Native.INSTANCE.get_exported_namespace(c_name.nativeAddress());
            return value == 0 ? null : new Namespace(value);
        }
    }

    public static long mem_dlopen(MemorySegment segment, int flags) {
        long length = segment.byteSize();
        try {
            FileDescriptor fd = IOUtils.ashmem_create_region(
                    "(mem_dlopen)", length);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment target = IOUtils.mmap(fd, 0, length, arena);
                target.copyFrom(segment);
                target.force();
                return dlopen_ext(fd, 0, flags);
            } finally {
                try {
                    Os.close(fd);
                } catch (ErrnoException e) { /* swallow exception */ }
            }
        } catch (ErrnoException e) {
            return 0;
        }
    }

    public static Namespace systemNamespace() {
        class Holder {
            static final Namespace default_namespace;

            static {
                String path = System.getProperty("java.library.path");
                assert path != null;
                default_namespace = create_namespace("panama_default", path, "",
                        NamespaceType.SHARED, "", Namespace.NULL);
            }
        }
        return Holder.default_namespace;
    }

    public static SymbolLookup systemLibraryLookup(String name, Arena arena) {
        return JavaForeignAccess.libraryLookup(RawNativeLibraries.load(name, systemNamespace()), arena);
    }
}

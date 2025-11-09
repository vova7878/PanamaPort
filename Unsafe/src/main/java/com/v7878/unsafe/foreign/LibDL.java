package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.EarlyNativeUtils.allocString;
import static com.v7878.unsafe.EarlyNativeUtils.segmentToString;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.invoke.VarHandle;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.ArtMethodUtils;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;
import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;

import java.util.Objects;
import java.util.Optional;

public class LibDL {
    public static final int RTLD_LOCAL = 0;
    public static final int RTLD_LAZY = 0x1;
    public static final int RTLD_NOW = IS64BIT ? 0x2 : 0x0;
    public static final int RTLD_GLOBAL = IS64BIT ? 0x100 : 0x2;
    public static final int RTLD_NOLOAD = 0x00004;
    public static final int RTLD_NODELETE = 0x01000;

    public static final long RTLD_DEFAULT = IS64BIT ? 0L : -1L;
    public static final long RTLD_NEXT = IS64BIT ? -1L : -2L;

    public static class DLInfo {
        /**
         * Pathname of shared object that contains address.
         */
        public final String fname;
        /**
         * Address at which shared object is loaded.
         */
        public final long fbase;
        /**
         * Name of nearest symbol with address lower than addr.
         */
        public final String sname;
        /**
         * Exact address of symbol named in sname.
         */
        public final long saddr;

        DLInfo(String fname, long fbase, String sname, long saddr) {
            this.fname = fname;
            this.fbase = fbase;
            this.sname = sname;
            this.saddr = saddr;
        }

        @Override
        public String toString() {
            return "DLInfo{" +
                    "fname=" + (fname == null ? null : '\'' + fname + '\'') +
                    ", fbase=" + fbase +
                    ", sname=" + (sname == null ? null : '\'' + sname + '\'') +
                    ", saddr=" + saddr +
                    '}';
        }
    }

    private static final GroupLayout dlinfo_layout = paddedStructLayout(
            ADDRESS.withName("fname"),
            ADDRESS.withName("fbase"),
            ADDRESS.withName("sname"),
            ADDRESS.withName("saddr")
    );

    @DoNotShrink
    @DoNotObfuscate
    static class LinkerSymbols {
        // for LibDL
        static final MemorySegment s_dladdr;
        static final MemorySegment s_dlclose;
        static final MemorySegment s_dlerror;
        static final MemorySegment s_dlopen;
        static final MemorySegment s_dlvsym;

        // for LibDLExt
        static final MemorySegment s_android_create_namespace;
        static final MemorySegment s_android_dlopen_ext;

        static {
            String linker_name = IS64BIT ? "linker64" : "linker";
            MMapEntry linker = MMap.findFirstByPath("/\\S+/" + linker_name);
            SymTab symbols = ELF.readSymTab(linker.path(), ART_INDEX >= A9);

            s_dladdr = symbols.findFunction(ART_INDEX < A9 ? "__dl__Z8__dladdrPKvP7Dl_info" : "__loader_dladdr", linker.start());
            s_dlclose = symbols.findFunction(ART_INDEX < A9 ? "__dl__Z9__dlclosePv" : "__loader_dlclose", linker.start());
            s_dlerror = symbols.findFunction(ART_INDEX < A9 ? "__dl__Z9__dlerrorv" : "__loader_dlerror", linker.start());
            s_dlopen = symbols.findFunction(ART_INDEX < A9 ? "__dl__Z8__dlopenPKciPKv" : "__loader_dlopen", linker.start());
            s_dlvsym = symbols.findFunction(ART_INDEX < A9 ? "__dl__Z8__dlvsymPvPKcS1_PKv" : "__loader_dlvsym", linker.start());

            s_android_create_namespace = symbols.findFunction(ART_INDEX < A9 ?
                    "__dl__Z26__android_create_namespacePKcS0_S0_yS0_P19android_namespace_tPKv" :
                    "__loader_android_create_namespace", linker.start());
            s_android_dlopen_ext = symbols.findFunction(ART_INDEX < A9 ?
                    "__dl__Z20__android_dlopen_extPKciPK17android_dlextinfoPKv" :
                    "__loader_android_dlopen_ext", linker.start());
        }
    }

    public static final long ART_CALLER;
    public static final long DEFAULT_CALLER;

    static {
        // Just any symbols belonging to libart and libc
        ART_CALLER = ArtMethodUtils.getExecutableData(getDeclaredMethod(System.class, "currentTimeMillis"));
        DEFAULT_CALLER = cdlsym(RTLD_DEFAULT, "malloc", ART_CALLER);
        assert DEFAULT_CALLER != 0;
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @SymbolGenerator(klass = LinkerSymbols.class, field = "s_dlopen")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long dlopen(long filename, int flags, long caller_addr);

        @SymbolGenerator(klass = LinkerSymbols.class, field = "s_dlclose")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int dlclose(long handle);

        @SymbolGenerator(klass = LinkerSymbols.class, field = "s_dlerror")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long dlerror();

        @SymbolGenerator(klass = LinkerSymbols.class, field = "s_dlvsym")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long dlvsym(long handle, long symbol, long version, long caller_addr);

        @SymbolGenerator(klass = LinkerSymbols.class, field = "s_dladdr")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int dladdr(long addr, long info);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE,
                Native.class, unused -> Optional.empty());
    }

    public static long cdlopen(String filename, int flags, long caller_addr) {
        Objects.requireNonNull(filename);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = allocString(arena, filename);
            return Native.INSTANCE.dlopen(c_filename.nativeAddress(), flags, caller_addr);
        }
    }

    public static long dlopen(String filename, int flags) {
        return cdlopen(filename, flags, DEFAULT_CALLER);
    }

    public static void dlclose(long handle) {
        //TODO: check result?
        int ignore = Native.INSTANCE.dlclose(handle);
    }

    public static String dlerror() {
        long msg = Native.INSTANCE.dlerror();
        if (msg == 0) return null;
        return segmentToString(MemorySegment.ofAddress(msg));
    }

    public static long cdlvsym(long handle, String symbol, String version, long caller_addr) {
        Objects.requireNonNull(symbol);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_symbol = allocString(arena, symbol);
            // Note: version is nullable
            MemorySegment c_version = allocString(arena, version);
            return Native.INSTANCE.dlvsym(handle, c_symbol.nativeAddress(), c_version.nativeAddress(), caller_addr);
        }
    }

    public static long dlvsym(long handle, String symbol, String version) {
        return cdlvsym(handle, symbol, version, DEFAULT_CALLER);
    }

    public static long cdlsym(long handle, String symbol, long caller_addr) {
        return cdlvsym(handle, symbol, null, caller_addr);
    }

    public static long dlsym(long handle, String symbol) {
        return cdlsym(handle, symbol, DEFAULT_CALLER);
    }

    public static DLInfo dladdr(long addr) {
        class Holder {
            static final VarHandle fname = dlinfo_layout.varHandle(groupElement("fname"));
            static final VarHandle fbase = dlinfo_layout.varHandle(groupElement("fbase"));
            static final VarHandle sname = dlinfo_layout.varHandle(groupElement("sname"));
            static final VarHandle saddr = dlinfo_layout.varHandle(groupElement("saddr"));
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(dlinfo_layout);

            int ignore = Native.INSTANCE.dladdr(addr, info.nativeAddress());

            return new DLInfo(segmentToString((MemorySegment) Holder.fname.get(info, 0)),
                    ((MemorySegment) Holder.fbase.get(info, 0)).nativeAddress(),
                    segmentToString((MemorySegment) Holder.sname.get(info, 0)),
                    ((MemorySegment) Holder.saddr.get(info, 0)).nativeAddress());
        }
    }
}

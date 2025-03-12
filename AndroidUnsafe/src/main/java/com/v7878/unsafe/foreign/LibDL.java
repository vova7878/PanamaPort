package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.VM.vmLibrary;
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
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ExtraMemoryAccess;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;
import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    @SuppressWarnings("unused")
    static class LinkerSymbols {
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
            SymTab symbols = ELF.readSymTab(linker.path(), ART_SDK_INT >= 28);

            s_dladdr = symbols.findFunction(ART_SDK_INT <= 27 ? "__dl__Z8__dladdrPKvP7Dl_info" : "__loader_dladdr", linker.start());
            s_dlclose = symbols.findFunction(ART_SDK_INT <= 27 ? "__dl__Z9__dlclosePv" : "__loader_dlclose", linker.start());
            s_dlerror = symbols.findFunction(ART_SDK_INT <= 27 ? "__dl__Z9__dlerrorv" : "__loader_dlerror", linker.start());
            s_dlopen = symbols.findFunction(ART_SDK_INT <= 27 ? "__dl__Z8__dlopenPKciPKv" : "__loader_dlopen", linker.start());
            s_dlvsym = symbols.findFunction(ART_SDK_INT <= 27 ? "__dl__Z8__dlvsymPvPKcS1_PKv" : "__loader_dlvsym", linker.start());

            s_android_create_namespace = symbols.findFunction(ART_SDK_INT <= 27 ?
                    "__dl__Z26__android_create_namespacePKcS0_S0_yS0_P19android_namespace_tPKv" : "__loader_android_create_namespace", linker.start());
            s_android_dlopen_ext = symbols.findFunction(ART_SDK_INT <= 27 ?
                    "__dl__Z20__android_dlopen_extPKciPK17android_dlextinfoPKv" : "__loader_android_dlopen_ext", linker.start());
        }
    }

    // TODO: Maybe some other symbol from libart?
    static final long ART_CALLER = MMap.findFirstByPath("/\\S+/" + vmLibrary()).start();

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

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, unused -> Optional.empty()));
    }

    // TODO: refactor?
    private static MemorySegment allocString(Arena arena, String value) {
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            return arena.allocateFrom(value);
        }

        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        data = Arrays.copyOf(data, data.length + 1);
        MemorySegment out = JavaForeignAccess.allocateNoInit(data.length, 1, arena);
        AndroidUnsafe.copyMemory(data, ARRAY_BYTE_BASE_OFFSET,
                null, out.nativeAddress(), data.length);
        return out;
    }

    // TODO: refactor?
    private static String segmentToString(MemorySegment segment) {
        if (MemorySegment.NULL.equals(segment)) return null;
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            return segment.reinterpret(Long.MAX_VALUE).getString(0);
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        var base = JavaForeignAccess.unsafeGetBase(segment);
        var offset = JavaForeignAccess.unsafeGetOffset(segment);

        for (int i = 0; ; i++) {
            byte value = AndroidUnsafe.getByte(base, offset + i);
            if (value == 0) break;
            data.write(value);
        }

        return data.toString();
    }

    public static long dlopen(String filename, int flags) {
        Objects.requireNonNull(filename);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = allocString(arena, filename);
            return Native.INSTANCE.dlopen(c_filename.nativeAddress(), flags, ART_CALLER);
        }
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

    static long dlsym_nochecks(long handle, String symbol) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_symbol = allocString(arena, symbol);
            return Native.INSTANCE.dlvsym(handle, c_symbol.nativeAddress(), 0, ART_CALLER);
        }
    }

    public static long dlsym(long handle, String symbol) {
        Objects.requireNonNull(symbol);
        return dlsym_nochecks(handle, symbol);
    }

    public static long dlvsym(long handle, String symbol, String version) {
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(version);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_symbol = allocString(arena, symbol);
            MemorySegment c_version = allocString(arena, version);
            return Native.INSTANCE.dlvsym(handle, c_symbol.nativeAddress(), c_version.nativeAddress(), ART_CALLER);
        }
    }

    private static final VarHandle fname_handle = dlinfo_layout.varHandle(groupElement("fname"));
    private static final VarHandle fbase_handle = dlinfo_layout.varHandle(groupElement("fbase"));
    private static final VarHandle sname_handle = dlinfo_layout.varHandle(groupElement("sname"));
    private static final VarHandle saddr_handle = dlinfo_layout.varHandle(groupElement("saddr"));

    public static DLInfo dladdr(long addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(dlinfo_layout);

            //TODO: check result?
            int ignore = Native.INSTANCE.dladdr(addr, info.nativeAddress());

            return new DLInfo(segmentToString((MemorySegment) fname_handle.get(info, 0)),
                    ((MemorySegment) fbase_handle.get(info, 0)).nativeAddress(),
                    segmentToString((MemorySegment) sname_handle.get(info, 0)),
                    ((MemorySegment) saddr_handle.get(info, 0)).nativeAddress());
        }
    }
}

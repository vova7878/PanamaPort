package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.invoke.VarHandle;
import com.v7878.unsafe.AndroidUnsafe;
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

    private static final MemorySegment s_dladdr;
    private static final MemorySegment s_dlclose;
    private static final MemorySegment s_dlerror;
    private static final MemorySegment s_dlopen;
    private static final MemorySegment s_dlvsym;
    private static final MemorySegment s_dlsym;

    // for LibDLExt
    static final MemorySegment s_android_dlopen_ext;

    static {
        MMapEntry libdl = MMap.findFirstByPath("/\\S+/libdl.so");
        SymTab symbols = ELF.readSymTab(libdl.path, true);

        s_dladdr = symbols.findFunction("dladdr", libdl.start);
        s_dlclose = symbols.findFunction("dlclose", libdl.start);
        s_dlerror = symbols.findFunction("dlerror", libdl.start);
        s_dlopen = symbols.findFunction("dlopen", libdl.start);
        s_dlvsym = symbols.findFunction("dlvsym", libdl.start);
        s_dlsym = symbols.findFunction("dlsym", libdl.start);

        // for LibDLExt
        s_android_dlopen_ext = symbols.findFunction("android_dlopen_ext", libdl.start);
    }

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @SymbolGenerator(method = "s_dlopen")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long dlopen(long filename, int flags);

        @SuppressWarnings("unused")
        private static MemorySegment s_dlopen() {
            return s_dlopen;
        }

        @SymbolGenerator(method = "s_dlclose")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int dlclose(long handle);

        @SuppressWarnings("unused")
        private static MemorySegment s_dlclose() {
            return s_dlclose;
        }

        @SymbolGenerator(method = "s_dlerror")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long dlerror();

        @SuppressWarnings("unused")
        private static MemorySegment s_dlerror() {
            return s_dlerror;
        }

        @SymbolGenerator(method = "s_dlsym")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long dlsym(long handle, long symbol);

        @SuppressWarnings("unused")
        private static MemorySegment s_dlsym() {
            return s_dlsym;
        }

        @SymbolGenerator(method = "s_dlvsym")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long dlvsym(long handle, long symbol, long version);

        @SuppressWarnings("unused")
        private static MemorySegment s_dlvsym() {
            return s_dlvsym;
        }

        @SymbolGenerator(method = "s_dladdr")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int dladdr(long addr, long info);

        @SuppressWarnings("unused")
        private static MemorySegment s_dladdr() {
            return s_dladdr;
        }

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, unused -> Optional.empty()));
    }

    public static long dlopen(String filename, int flags) {
        Objects.requireNonNull(filename);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = arena.allocateFrom(filename);
            return Native.INSTANCE.dlopen(c_filename.nativeAddress(), flags);
        }
    }

    public static long dlopen(String filename) {
        return dlopen(filename, RTLD_NOW);
    }

    public static void dlclose(long handle) {
        //TODO: check result?
        int ignore = Native.INSTANCE.dlclose(handle);
    }

    public static String dlerror() {
        long msg = Native.INSTANCE.dlerror();
        if (msg == 0) return null;
        return MemorySegment.ofAddress(msg).reinterpret(Long.MAX_VALUE).getString(0);
    }

    static long dlsym_nochecks(long handle, String symbol) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_symbol = arena.allocateFrom(symbol);
            return Native.INSTANCE.dlsym(handle, c_symbol.nativeAddress());
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
            MemorySegment c_symbol = arena.allocateFrom(symbol);
            MemorySegment c_version = arena.allocateFrom(version);
            return Native.INSTANCE.dlvsym(handle, c_symbol.nativeAddress(), c_version.nativeAddress());
        }
    }

    private static final VarHandle fname_handle = dlinfo_layout.varHandle(groupElement("fname"));
    private static final VarHandle fbase_handle = dlinfo_layout.varHandle(groupElement("fbase"));
    private static final VarHandle sname_handle = dlinfo_layout.varHandle(groupElement("sname"));
    private static final VarHandle saddr_handle = dlinfo_layout.varHandle(groupElement("saddr"));

    private static String segmentToString(MemorySegment segment) {
        if (MemorySegment.NULL.equals(segment)) return null;
        return segment.reinterpret(Long.MAX_VALUE).getString(0);
    }

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

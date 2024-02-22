package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleBulkLinker.WORD_CLASS;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.invoke.VarHandle;
import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Supplier;

import dalvik.annotation.optimization.CriticalNative;

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
    private static final MemorySegment s_android_dlopen_ext;

    static {
        MMapEntry libdl = findLibDLEntry();
        SymTab symbols = getSymTab(libdl);

        s_dladdr = symbols.findFunction("dladdr", libdl.start);
        s_dlclose = symbols.findFunction("dlclose", libdl.start);
        s_dlerror = symbols.findFunction("dlerror", libdl.start);
        s_dlopen = symbols.findFunction("dlopen", libdl.start);
        s_dlvsym = symbols.findFunction("dlvsym", libdl.start);
        s_dlsym = symbols.findFunction("dlsym", libdl.start);

        // for LibDLExt
        s_android_dlopen_ext = symbols.findFunction("android_dlopen_ext", libdl.start);
    }

    private static MMapEntry findLibDLEntry() {
        return MMap.findFirstByPath("/\\S+/libdl.so");
    }

    private static SymTab getSymTab(MMapEntry libdl) {
        byte[] tmp;
        try {
            tmp = Files.readAllBytes(new File(libdl.path).toPath());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return ELF.readSymTab(ByteBuffer.wrap(tmp).order(ByteOrder.nativeOrder()), true);
    }

    enum Function implements SymbolHolder {
        dlopen(s_dlopen, WORD_CLASS, WORD_CLASS, int.class),
        dlclose(s_dlclose, int.class, WORD_CLASS),
        dlerror(s_dlerror, WORD_CLASS),
        //FIXME!!! (SIGSEGV on api levels [26, 28])
        //dlsym(s_dlsym, WORD_CLASS, WORD_CLASS, WORD_CLASS),
        dlvsym(s_dlvsym, WORD_CLASS, WORD_CLASS, WORD_CLASS, WORD_CLASS),
        dladdr(s_dladdr, int.class, WORD_CLASS, WORD_CLASS),

        // for LibDLExt
        android_dlopen_ext(s_android_dlopen_ext, WORD_CLASS, WORD_CLASS, int.class, WORD_CLASS);

        static {
            SimpleBulkLinker.processSymbols(Arena.global(), Function.values());
        }

        private final MemorySegment symbol;
        private final MethodType type;

        private Supplier<MethodHandle> handle;

        Function(MemorySegment symbol, Class<?> rtype, Class<?>... atypes) {
            this.symbol = symbol;
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setHandle(Supplier<MethodHandle> handle) {
            this.handle = handle;
        }

        public long symbol() {
            return symbol.nativeAddress();
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

    private static long raw_dlopen(long filename, int flags) {
        return nothrows_run(() -> (long) Function.dlopen.handle().invokeExact(filename, flags));
    }

    private static int raw_dlclose(long handle) {
        return nothrows_run(() -> (int) Function.dlclose.handle().invokeExact(handle));
    }

    private static long raw_dlerror() {
        return nothrows_run(() -> (long) Function.dlerror.handle().invokeExact());
    }

    //FIXME!!! (SIGSEGV on api levels [26, 28])
    //private static long raw_dlsym(long handle, long symbol) {
    //    return nothrows_run(() -> (long) Function.dlsym.handle().invokeExact(handle, symbol));
    //}

    static {
        String suffix = IS64BIT ? "64" : "32";
        Class<?> word = IS64BIT ? long.class : int.class;

        Method symbol = getDeclaredMethod(LibDL.class, "raw_dlsym" + suffix, word, word);
        registerNativeMethod(symbol, s_dlsym.nativeAddress());
    }

    @Keep
    @CriticalNative
    private static native long raw_dlsym64(long handle, long symbol);

    @Keep
    @CriticalNative
    private static native int raw_dlsym32(int handle, int symbol);

    public static long raw_dlsym(long handle, long symbol) {
        return IS64BIT ? raw_dlsym64(handle, symbol) : raw_dlsym32((int) handle, (int) symbol) & 0xffffffffL;
    }

    private static long raw_dlvsym(long handle, long symbol, long version) {
        return nothrows_run(() -> (long) Function.dlvsym.handle().invokeExact(handle, symbol, version));
    }

    private static int raw_dladdr(long addr, long info) {
        return nothrows_run(() -> (int) Function.dladdr.handle().invokeExact(addr, info));
    }

    public static long dlopen(String filename, int flags) {
        Objects.requireNonNull(filename);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_filename = arena.allocateFrom(filename);
            return raw_dlopen(c_filename.nativeAddress(), flags);
        }
    }

    public static long dlopen(String filename) {
        return dlopen(filename, RTLD_NOW);
    }

    public static void dlclose(long handle) {
        //TODO: check result?
        int ignore = raw_dlclose(handle);
    }

    public static String dlerror() {
        long msg = raw_dlerror();
        if (msg == 0) return null;
        return MemorySegment.ofAddress(msg).reinterpret(Long.MAX_VALUE).getString(0);
    }

    static long dlsym_nochecks(long handle, String symbol) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_symbol = arena.allocateFrom(symbol);
            return raw_dlsym(handle, c_symbol.nativeAddress());
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
            return raw_dlvsym(handle, c_symbol.nativeAddress(), c_version.nativeAddress());
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
            int ignore = raw_dladdr(addr, info.nativeAddress());

            return new DLInfo(segmentToString((MemorySegment) fname_handle.get(info, 0)),
                    ((MemorySegment) fbase_handle.get(info, 0)).nativeAddress(),
                    segmentToString((MemorySegment) sname_handle.get(info, 0)),
                    ((MemorySegment) saddr_handle.get(info, 0)).nativeAddress());
        }
    }
}

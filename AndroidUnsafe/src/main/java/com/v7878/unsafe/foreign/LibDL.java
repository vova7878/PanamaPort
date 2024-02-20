package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleBulkLinker.WORD_CLASS;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Supplier;

public class LibDL {

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
        dladdr(s_dladdr, int.class, WORD_CLASS, WORD_CLASS),
        dlclose(s_dlclose, int.class, WORD_CLASS),
        dlerror(s_dlerror, WORD_CLASS),
        dlopen(s_dlopen, WORD_CLASS, WORD_CLASS, int.class),
        dlsym(s_dlsym, WORD_CLASS, WORD_CLASS, WORD_CLASS),
        dlvsym(s_dlvsym, WORD_CLASS, WORD_CLASS, WORD_CLASS, WORD_CLASS),

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
            return symbol.address();
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

    public static long dlopen(long filename, int flags) {
        return nothrows_run(() -> (long) Function.dlopen.handle().invokeExact(filename, flags));
    }

    public static int dlclose(long handle) {
        return nothrows_run(() -> (int) Function.dlclose.handle().invokeExact(handle));
    }

    public static long dlerror() {
        return nothrows_run(() -> (long) Function.dlerror.handle().invokeExact());
    }

    public static long dlsym(long handle, long symbol) {
        return nothrows_run(() -> (long) Function.dlsym.handle().invokeExact(handle, symbol));
    }

    public static long dlvsym(long handle, long symbol, long version) {
        return nothrows_run(() -> (long) Function.dlvsym.handle().invokeExact(handle, symbol, version));
    }

    public static int dladdr(long addr, long info) {
        return nothrows_run(() -> (int) Function.dladdr.handle().invokeExact(addr, info));
    }
}

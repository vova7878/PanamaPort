package com.v7878.unsafe.foreign;

import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class LibDL {

    //TODO?: android_handle_signal android_get_LD_LIBRARY_PATH android_dlopen_ext
    public static final MemorySegment dladdr;
    public static final MemorySegment dlclose;
    public static final MemorySegment dlerror;
    public static final MemorySegment dlopen;
    public static final MemorySegment dlvsym;
    public static final MemorySegment dlsym;

    static {
        MMapEntry libdl = findLibDLEntry();
        SymTab symbols = getSymTab(libdl);
        dladdr = symbols.findFunction("dladdr", libdl.start);
        dlclose = symbols.findFunction("dlclose", libdl.start);
        dlerror = symbols.findFunction("dlerror", libdl.start);
        dlopen = symbols.findFunction("dlopen", libdl.start);
        dlvsym = symbols.findFunction("dlvsym", libdl.start);
        dlsym = symbols.findFunction("dlsym", libdl.start);
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
}

package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;

import androidx.annotation.Keep;

import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.foreign.ELF.SymTab;
import com.v7878.unsafe.foreign.MMap.MMapEntry;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import dalvik.annotation.optimization.CriticalNative;

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

    static {
        String suffix = IS64BIT ? "64" : "32";
        Class<?> word = IS64BIT ? long.class : int.class;

        Method symbol = getDeclaredMethod(LibDL.class, "dlopen" + suffix, word, int.class);
        setExecutableData(symbol, dlopen.address());

        symbol = getDeclaredMethod(LibDL.class, "dlerror" + suffix);
        setExecutableData(symbol, dlerror.address());

        symbol = getDeclaredMethod(LibDL.class, "dlsym" + suffix, word, word);
        setExecutableData(symbol, dlsym.address());

        symbol = getDeclaredMethod(LibDL.class, "dlclose" + suffix, word);
        setExecutableData(symbol, dlclose.address());

        //TODO: dladdr, dlvsym
    }

    @Keep
    @CriticalNative
    private static native long dlopen64(long filename, int flags);

    @Keep
    @CriticalNative
    private static native int dlopen32(int filename, int flags);

    public static long dlopen(long filename, int flags) {
        return IS64BIT ? dlopen64(filename, flags) : dlopen32((int) filename, flags) & 0xffffffffL;
    }

    @Keep
    @CriticalNative
    private static native long dlerror64();

    @Keep
    @CriticalNative
    private static native int dlerror32();

    public static long dlerror() {
        return IS64BIT ? dlerror64() : dlerror32() & 0xffffffffL;
    }

    @Keep
    @CriticalNative
    private static native long dlsym64(long handle, long symbol);

    @Keep
    @CriticalNative
    private static native int dlsym32(int handle, int symbol);

    public static long dlsym(long handle, long symbol) {
        return IS64BIT ? dlsym64(handle, symbol) : dlsym32((int) handle, (int) symbol) & 0xffffffffL;
    }

    @Keep
    @CriticalNative
    private static native int dlclose64(long handle);

    @Keep
    @CriticalNative
    private static native int dlclose32(int handle);

    public static int dlclose(long handle) {
        return IS64BIT ? dlclose64(handle) : dlclose32((int) handle);
    }
}

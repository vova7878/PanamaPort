package com.v7878.unsafe.foreign;

import com.v7878.foreign.Linker;
import com.v7878.foreign.SymbolLookup;

public class LibArt {
    //TODO: open libart.so with art namespace via android_dlopen_ext
    public static final SymbolLookup ART = Linker.nativeLinker().defaultLookup();
}

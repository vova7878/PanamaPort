package com.v7878.unsafe.foreign;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;

public class LibArt {
    //TODO: open libart.so with art namespace via android_dlopen_ext
    public static final SymbolLookup ART = SymbolLookup
            .libraryLookup("libart.so", Arena.global());
}

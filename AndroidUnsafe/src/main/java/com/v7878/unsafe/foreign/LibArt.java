package com.v7878.unsafe.foreign;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;

public class LibArt {
    //TODO: heap session
    public static final Arena ART_SCOPE = Arena.global();
    //TODO: open libart.so with art namespace via android_dlopen_ext
    public static final SymbolLookup ART = SymbolLookup
            .libraryLookup("libart.so", ART_SCOPE);
}

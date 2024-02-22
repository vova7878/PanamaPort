package com.v7878.unsafe.foreign;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.access.JavaForeignAccess;

public class LibArt {
    public static final Arena ART_SCOPE = JavaForeignAccess.createHeapArena(LibArt.class);
    //TODO: open libart.so with art namespace via android_dlopen_ext
    public static final SymbolLookup ART = SymbolLookup
            .libraryLookup("libart.so", ART_SCOPE);
}

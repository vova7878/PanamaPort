package com.v7878.unsafe.foreign;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.access.JavaForeignAccess;

public class LibArt {
    public static final Arena ART_SCOPE = JavaForeignAccess.createImplicitHeapArena(LibArt.class);
    public static final SymbolLookup ART = LibDLExt.systemLibraryLookup("libart.so", ART_SCOPE);
}

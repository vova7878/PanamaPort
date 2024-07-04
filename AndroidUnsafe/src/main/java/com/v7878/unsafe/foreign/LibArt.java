package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.VM.vmLibrary;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.access.JavaForeignAccess;

public class LibArt {
    private static final Arena ART_SCOPE = JavaForeignAccess.createImplicitHeapArena(LibArt.class);
    public static final SymbolLookup ART = LibDLExt.systemLibraryLookup(vmLibrary(), ART_SCOPE);
}

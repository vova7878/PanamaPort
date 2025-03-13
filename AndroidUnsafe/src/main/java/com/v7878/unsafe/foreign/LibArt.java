package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.VM.vmLibrary;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.access.JavaForeignAccess;

public class LibArt {
    @DoNotShrink
    private static final Arena ART_SCOPE = Arena.ofAuto();
    public static final SymbolLookup ART = JavaForeignAccess.libraryLookup(
            RawNativeLibraries.cload(vmLibrary(), LibDL.ART_CALLER), ART_SCOPE);
}

package com.v7878.unsafe.cpp_std;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.LibDLExt;

public class LibCpp {
    private static final Arena CPP_SCOPE = JavaForeignAccess.createImplicitHeapArena(LibCpp.class);
    public static final SymbolLookup CPP = LibDLExt.systemLibraryLookup("libc++.so", CPP_SCOPE);
}

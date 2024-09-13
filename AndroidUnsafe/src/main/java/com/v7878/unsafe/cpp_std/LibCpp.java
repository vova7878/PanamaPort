package com.v7878.unsafe.cpp_std;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.foreign.LibDLExt;

public class LibCpp {
    @DoNotShrink
    private static final Arena CPP_SCOPE = Arena.ofAuto();
    public static final SymbolLookup CPP = LibDLExt.systemLibraryLookup("libc++.so", CPP_SCOPE);
}

package com.v7878.llvm;

import static com.v7878.misc.Version.CORRECT_SDK_INT;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.LibDLExt;

class _LibLLVM {
    public static final Arena LLVM_SCOPE = JavaForeignAccess.createImplicitHeapArena(_LibLLVM.class);
    @ApiSensitive
    public static final SymbolLookup LLVM = LibDLExt.systemLibraryLookup(
            CORRECT_SDK_INT < 28 ? "libLLVM.so" : "libLLVM_android.so", LLVM_SCOPE);
}

package com.v7878.llvm;

import static com.v7878.misc.Version.CORRECT_SDK_INT;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;

public class LibLLVM {
    //TODO: heap session
    public static final Arena LLVM_SCOPE = Arena.global();
    public static final SymbolLookup LLVM = SymbolLookup.libraryLookup(
            CORRECT_SDK_INT < 28 ? "libLLVM.so" : "libLLVM_android.so", LLVM_SCOPE);
}

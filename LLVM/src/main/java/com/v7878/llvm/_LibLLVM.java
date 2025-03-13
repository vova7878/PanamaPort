package com.v7878.llvm;

import static com.v7878.misc.Version.CORRECT_SDK_INT;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.ApiSensitive;

final class _LibLLVM {
    private _LibLLVM() {
    }

    @DoNotShrink
    private static final Arena LLVM_SCOPE = Arena.ofAuto();
    @ApiSensitive
    public static final SymbolLookup LLVM = SymbolLookup.libraryLookup(
            CORRECT_SDK_INT < 28 ? "libLLVM.so" : "libLLVM_android.so", LLVM_SCOPE);
}

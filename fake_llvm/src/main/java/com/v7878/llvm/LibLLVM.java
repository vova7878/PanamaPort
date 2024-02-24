package com.v7878.llvm;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;

import java.util.function.Supplier;

public class LibLLVM {
    public static final Arena LLVM_SCOPE = ((Supplier<Arena>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    public static final SymbolLookup LLVM = ((Supplier<SymbolLookup>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
}

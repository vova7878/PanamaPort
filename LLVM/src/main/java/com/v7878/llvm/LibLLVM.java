package com.v7878.llvm;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.foreign.Arena;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.access.JavaForeignAccess;

public class LibLLVM {
    public static final Arena LLVM_SCOPE = JavaForeignAccess.createHeapArena(LibLLVM.class);
    //TODO: open with system namespace via android_dlopen_ext
    public static final SymbolLookup LLVM = SymbolLookup.libraryLookup(
            CORRECT_SDK_INT < 28 ? "libLLVM.so" :
                    "/system/lib" + (IS64BIT ? "64" : "") + "/libLLVM_android.so", LLVM_SCOPE);
}

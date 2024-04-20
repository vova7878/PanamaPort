package com.v7878.llvm;

import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===-- llvm-c/Linker.h - Module Linker C Interface -------------*- C++ -*-===*\
|*                                                                            *|
|* This file defines the C interface to the module/file/archive linker.       *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Linker {
    private Linker() {
    }

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMLinkModules2")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMLinkModules2(long Dest, long Src);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    /**
     * Links the source module into the destination module. The source module is destroyed.
     * The return value is true if an error occurred, false otherwise.
     * Use the diagnostic handler to get any diagnostic message.
     */
    public static boolean LLVMLinkModules2(LLVMModuleRef Dest, LLVMModuleRef Src) {
        return Native.INSTANCE.LLVMLinkModules2(Dest.value(), Src.value());
    }
}

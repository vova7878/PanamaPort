package com.v7878.llvm;

import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===---------------------------Vectorize.h --------------------- -*- C -*-===*\
|*===----------- Vectorization Transformation Library C Interface ---------===*|
|*                                                                            *|
|* This header declares the C interface to libLLVMVectorize.a, which          *|
|* implements various vectorization transformations of the LLVM IR.           *|
|*                                                                            *|
|* Many exotic languages can interoperate with C code but have a harder time  *|
|* with C++ due to name mangling. So in addition to C, this interface enables *|
|* tools written in such languages.                                           *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Vectorize {
    private Vectorize() {
    }

    /*
     * @defgroup LLVMCTransformsVectorize Vectorization transformations
     * @ingroup LLVMCTransforms
     */

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMAddBBVectorizePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddBBVectorizePass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopVectorizePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopVectorizePass(long PM);

        @LibrarySymbol(name = "LLVMAddSLPVectorizePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddSLPVectorizePass(long PM);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMAddBBVectorizePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddBBVectorizePass(PM.value());
    }

    public static void LLVMAddLoopVectorizePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopVectorizePass(PM.value());
    }

    public static void LLVMAddSLPVectorizePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddSLPVectorizePass(PM.value());
    }
}

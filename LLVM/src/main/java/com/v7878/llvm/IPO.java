package com.v7878.llvm;

import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
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

/*===-- IPO.h - Interprocedural Transformations C Interface -----*- C++ -*-===*\
|*                                                                            *|
|* This header declares the C interface to libLLVMIPO.a, which implements     *|
|* various interprocedural transformations of the LLVM IR.                    *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class IPO {
    private IPO() {
    }

    /*
     * @defgroup LLVMCTransformsIPO Interprocedural transformations
     * @ingroup LLVMCTransforms
     */

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMAddArgumentPromotionPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddArgumentPromotionPass(long PM);

        @LibrarySymbol(name = "LLVMAddConstantMergePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddConstantMergePass(long PM);

        @LibrarySymbol(name = "LLVMAddDeadArgEliminationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddDeadArgEliminationPass(long PM);

        @LibrarySymbol(name = "LLVMAddFunctionAttrsPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddFunctionAttrsPass(long PM);

        @LibrarySymbol(name = "LLVMAddFunctionInliningPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddFunctionInliningPass(long PM);

        @LibrarySymbol(name = "LLVMAddAlwaysInlinerPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddAlwaysInlinerPass(long PM);

        @LibrarySymbol(name = "LLVMAddGlobalDCEPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddGlobalDCEPass(long PM);

        @LibrarySymbol(name = "LLVMAddGlobalOptimizerPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddGlobalOptimizerPass(long PM);

        @LibrarySymbol(name = "LLVMAddIPConstantPropagationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddIPConstantPropagationPass(long PM);

        @LibrarySymbol(name = "LLVMAddPruneEHPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddPruneEHPass(long PM);

        @LibrarySymbol(name = "LLVMAddIPSCCPPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddIPSCCPPass(long PM);

        @LibrarySymbol(name = "LLVMAddInternalizePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMAddInternalizePass(long PM, int AllButMain);

        @LibrarySymbol(name = "LLVMAddStripDeadPrototypesPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddStripDeadPrototypesPass(long PM);

        @LibrarySymbol(name = "LLVMAddStripSymbolsPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddStripSymbolsPass(long PM);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMAddArgumentPromotionPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddArgumentPromotionPass(PM.value());
    }

    public static void LLVMAddConstantMergePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddConstantMergePass(PM.value());
    }

    public static void LLVMAddDeadArgEliminationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddDeadArgEliminationPass(PM.value());
    }

    public static void LLVMAddFunctionAttrsPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddFunctionAttrsPass(PM.value());
    }

    public static void LLVMAddFunctionInliningPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddFunctionInliningPass(PM.value());
    }

    public static void LLVMAddAlwaysInlinerPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddAlwaysInlinerPass(PM.value());
    }

    public static void LLVMAddGlobalDCEPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddGlobalDCEPass(PM.value());
    }

    public static void LLVMAddGlobalOptimizerPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddGlobalOptimizerPass(PM.value());
    }

    public static void LLVMAddIPConstantPropagationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddIPConstantPropagationPass(PM.value());
    }

    public static void LLVMAddPruneEHPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddPruneEHPass(PM.value());
    }

    public static void LLVMAddIPSCCPPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddIPSCCPPass(PM.value());
    }

    public static void LLVMAddInternalizePass(LLVMPassManagerRef PM, int /* unsigned */ AllButMain) {
        Native.INSTANCE.LLVMAddInternalizePass(PM.value(), AllButMain);
    }

    public static void LLVMAddStripDeadPrototypesPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddStripDeadPrototypesPass(PM.value());
    }

    public static void LLVMAddStripSymbolsPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddStripSymbolsPass(PM.value());
    }
}

package com.v7878.llvm;

import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===-- Scalar.h - Scalar Transformation Library C Interface ----*- C++ -*-===*\
|*                                                                            *|
|* This header declares the C interface to libLLVMScalarOpts.a, which         *|
|* implements various scalar transformations of the LLVM IR.                  *|
|*                                                                            *|
|* Many exotic languages can interoperate with C code but have a harder time  *|
|* with C++ due to name mangling. So in addition to C, this interface enables *|
|* tools written in such languages.                                           *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Scalar {
    private Scalar() {
    }

    /*
     * @defgroup LLVMCTransformsScalar Scalar transformations
     * @ingroup LLVMCTransforms
     */

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMAddAggressiveDCEPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddAggressiveDCEPass(long PM);

        @LibrarySymbol(name = "LLVMAddBitTrackingDCEPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddBitTrackingDCEPass(long PM);

        @LibrarySymbol(name = "LLVMAddAlignmentFromAssumptionsPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddAlignmentFromAssumptionsPass(long PM);

        @LibrarySymbol(name = "LLVMAddCFGSimplificationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddCFGSimplificationPass(long PM);

        @LibrarySymbol(name = "LLVMAddDeadStoreEliminationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddDeadStoreEliminationPass(long PM);

        @LibrarySymbol(name = "LLVMAddScalarizerPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddScalarizerPass(long PM);

        @LibrarySymbol(name = "LLVMAddMergedLoadStoreMotionPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddMergedLoadStoreMotionPass(long PM);

        @LibrarySymbol(name = "LLVMAddGVNPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddGVNPass(long PM);

        @LibrarySymbol(name = "LLVMAddIndVarSimplifyPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddIndVarSimplifyPass(long PM);

        @LibrarySymbol(name = "LLVMAddInstructionCombiningPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddInstructionCombiningPass(long PM);

        @LibrarySymbol(name = "LLVMAddJumpThreadingPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddJumpThreadingPass(long PM);

        @LibrarySymbol(name = "LLVMAddLICMPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLICMPass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopDeletionPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopDeletionPass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopIdiomPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopIdiomPass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopRotatePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopRotatePass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopRerollPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopRerollPass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopUnrollPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopUnrollPass(long PM);

        @LibrarySymbol(name = "LLVMAddLoopUnswitchPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLoopUnswitchPass(long PM);

        @LibrarySymbol(name = "LLVMAddMemCpyOptPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddMemCpyOptPass(long PM);

        @LibrarySymbol(name = "LLVMAddPartiallyInlineLibCallsPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddPartiallyInlineLibCallsPass(long PM);

        @LibrarySymbol(name = "LLVMAddLowerSwitchPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLowerSwitchPass(long PM);

        @LibrarySymbol(name = "LLVMAddPromoteMemoryToRegisterPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddPromoteMemoryToRegisterPass(long PM);

        @LibrarySymbol(name = "LLVMAddReassociatePass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddReassociatePass(long PM);

        @LibrarySymbol(name = "LLVMAddSCCPPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddSCCPPass(long PM);

        @LibrarySymbol(name = "LLVMAddScalarReplAggregatesPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddScalarReplAggregatesPass(long PM);

        @LibrarySymbol(name = "LLVMAddScalarReplAggregatesPassSSA")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddScalarReplAggregatesPassSSA(long PM);

        @LibrarySymbol(name = "LLVMAddScalarReplAggregatesPassWithThreshold")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMAddScalarReplAggregatesPassWithThreshold(long PM, int Threshold);

        @LibrarySymbol(name = "LLVMAddSimplifyLibCallsPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddSimplifyLibCallsPass(long PM);

        @LibrarySymbol(name = "LLVMAddTailCallEliminationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddTailCallEliminationPass(long PM);

        @LibrarySymbol(name = "LLVMAddConstantPropagationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddConstantPropagationPass(long PM);

        @LibrarySymbol(name = "LLVMAddDemoteMemoryToRegisterPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddDemoteMemoryToRegisterPass(long PM);

        @LibrarySymbol(name = "LLVMAddVerifierPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddVerifierPass(long PM);

        @LibrarySymbol(name = "LLVMAddCorrelatedValuePropagationPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddCorrelatedValuePropagationPass(long PM);

        @LibrarySymbol(name = "LLVMAddEarlyCSEPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddEarlyCSEPass(long PM);

        @LibrarySymbol(name = "LLVMAddLowerExpectIntrinsicPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddLowerExpectIntrinsicPass(long PM);

        @LibrarySymbol(name = "LLVMAddTypeBasedAliasAnalysisPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddTypeBasedAliasAnalysisPass(long PM);

        @LibrarySymbol(name = "LLVMAddScopedNoAliasAAPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddScopedNoAliasAAPass(long PM);

        @LibrarySymbol(name = "LLVMAddBasicAliasAnalysisPass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMAddBasicAliasAnalysisPass(long PM);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMAddAggressiveDCEPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddAggressiveDCEPass(PM.value());
    }

    public static void LLVMAddBitTrackingDCEPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddBitTrackingDCEPass(PM.value());
    }

    public static void LLVMAddAlignmentFromAssumptionsPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddAlignmentFromAssumptionsPass(PM.value());
    }

    public static void LLVMAddCFGSimplificationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddCFGSimplificationPass(PM.value());
    }

    public static void LLVMAddDeadStoreEliminationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddDeadStoreEliminationPass(PM.value());
    }

    public static void LLVMAddScalarizerPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddScalarizerPass(PM.value());
    }

    public static void LLVMAddMergedLoadStoreMotionPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddMergedLoadStoreMotionPass(PM.value());
    }

    public static void LLVMAddGVNPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddGVNPass(PM.value());
    }

    public static void LLVMAddIndVarSimplifyPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddIndVarSimplifyPass(PM.value());
    }

    public static void LLVMAddInstructionCombiningPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddInstructionCombiningPass(PM.value());
    }

    public static void LLVMAddJumpThreadingPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddJumpThreadingPass(PM.value());
    }

    public static void LLVMAddLICMPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLICMPass(PM.value());
    }

    public static void LLVMAddLoopDeletionPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopDeletionPass(PM.value());
    }

    public static void LLVMAddLoopIdiomPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopIdiomPass(PM.value());
    }

    public static void LLVMAddLoopRotatePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopRotatePass(PM.value());
    }

    public static void LLVMAddLoopRerollPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopRerollPass(PM.value());
    }

    public static void LLVMAddLoopUnrollPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopUnrollPass(PM.value());
    }

    public static void LLVMAddLoopUnswitchPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLoopUnswitchPass(PM.value());
    }

    public static void LLVMAddMemCpyOptPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddMemCpyOptPass(PM.value());
    }

    public static void LLVMAddPartiallyInlineLibCallsPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddPartiallyInlineLibCallsPass(PM.value());
    }

    public static void LLVMAddLowerSwitchPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLowerSwitchPass(PM.value());
    }

    public static void LLVMAddPromoteMemoryToRegisterPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddPromoteMemoryToRegisterPass(PM.value());
    }

    public static void LLVMAddReassociatePass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddReassociatePass(PM.value());
    }

    public static void LLVMAddSCCPPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddSCCPPass(PM.value());
    }

    public static void LLVMAddScalarReplAggregatesPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddScalarReplAggregatesPass(PM.value());
    }

    public static void LLVMAddScalarReplAggregatesPassSSA(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddScalarReplAggregatesPassSSA(PM.value());
    }

    public static void LLVMAddScalarReplAggregatesPassWithThreshold(LLVMPassManagerRef PM, int Threshold) {
        Native.INSTANCE.LLVMAddScalarReplAggregatesPassWithThreshold(PM.value(), Threshold);
    }

    public static void LLVMAddSimplifyLibCallsPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddSimplifyLibCallsPass(PM.value());
    }

    public static void LLVMAddTailCallEliminationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddTailCallEliminationPass(PM.value());
    }

    public static void LLVMAddConstantPropagationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddConstantPropagationPass(PM.value());
    }

    public static void LLVMAddDemoteMemoryToRegisterPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddDemoteMemoryToRegisterPass(PM.value());
    }

    public static void LLVMAddVerifierPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddVerifierPass(PM.value());
    }

    public static void LLVMAddCorrelatedValuePropagationPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddCorrelatedValuePropagationPass(PM.value());
    }

    public static void LLVMAddEarlyCSEPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddEarlyCSEPass(PM.value());
    }

    public static void LLVMAddLowerExpectIntrinsicPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddLowerExpectIntrinsicPass(PM.value());
    }

    public static void LLVMAddTypeBasedAliasAnalysisPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddTypeBasedAliasAnalysisPass(PM.value());
    }

    public static void LLVMAddScopedNoAliasAAPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddScopedNoAliasAAPass(PM.value());
    }

    public static void LLVMAddBasicAliasAnalysisPass(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddBasicAliasAnalysisPass(PM.value());
    }
}

package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBasicBlockRef;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import com.v7878.llvm.Types.LLVMModuleProviderRef;
import com.v7878.llvm.Types.LLVMPassManagerRef;

import java.util.function.Supplier;

public class Core {
    @FunctionalInterface
    public interface LLVMDiagnosticHandler {
        void invoke(LLVMDiagnosticInfoRef info);
    }

    @FunctionalInterface
    public interface LLVMYieldCallback {
        void invoke(LLVMContextRef context);
    }

    public static class LLVMAttributeIndex {
        public static final int LLVMAttributeFirstArgIndex = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMAttributeFunctionIndex = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMAttributeReturnIndex = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
    }

    public static class LLVMAttribute {
        public static final int LLVMAlignment = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMAlwaysInlineAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMByValAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMInRegAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMInlineHintAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNakedAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNestAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoAliasAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoCaptureAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoImplicitFloatAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoInlineAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoRedZoneAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoReturnAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNoUnwindAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMNonLazyBind = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMOptimizeForSizeAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMReadNoneAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMReadOnlyAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMReturnsTwice = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMSExtAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMStackAlignment = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMStackProtectAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMStackProtectReqAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMStructRetAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMUWTable = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
        public static final int LLVMZExtAttribute = ((Supplier<Integer>) () -> {
            throw new UnsupportedOperationException("Stub!");
        }).get();
    }

    public enum LLVMOpcode {
        LLVMRet,
        LLVMBr,
        LLVMSwitch,
        LLVMIndirectBr,
        LLVMInvoke,
        LLVMUnreachable,
        LLVMAdd,
        LLVMFAdd,
        LLVMSub,
        LLVMFSub,
        LLVMMul,
        LLVMFMul,
        LLVMUDiv,
        LLVMSDiv,
        LLVMFDiv,
        LLVMURem,
        LLVMSRem,
        LLVMFRem,
        LLVMShl,
        LLVMLShr,
        LLVMAShr,
        LLVMAnd,
        LLVMOr,
        LLVMXor,
        LLVMAlloca,
        LLVMLoad,
        LLVMStore,
        LLVMGetElementPtr,
        LLVMTrunc,
        LLVMZExt,
        LLVMSExt,
        LLVMFPToUI,
        LLVMFPToSI,
        LLVMUIToFP,
        LLVMSIToFP,
        LLVMFPTrunc,
        LLVMFPExt,
        LLVMPtrToInt,
        LLVMIntToPtr,
        LLVMBitCast,
        LLVMAddrSpaceCast,
        LLVMICmp,
        LLVMFCmp,
        LLVMPHI,
        LLVMCall,
        LLVMSelect,
        LLVMUserOp1,
        LLVMUserOp2,
        LLVMVAArg,
        LLVMExtractElement,
        LLVMInsertElement,
        LLVMShuffleVector,
        LLVMExtractValue,
        LLVMInsertValue,
        LLVMFence,
        LLVMAtomicCmpXchg,
        LLVMAtomicRMW,
        LLVMResume,
        LLVMLandingPad,
        LLVMCleanupRet,
        LLVMCatchRet,
        LLVMCatchPad,
        LLVMCleanupPad,
        LLVMCatchSwitch;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMOpcode of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }


    public enum LLVMTypeKind {
        LLVMVoidTypeKind,
        LLVMHalfTypeKind,
        LLVMFloatTypeKind,
        LLVMDoubleTypeKind,
        LLVMX86_FP80TypeKind,
        LLVMFP128TypeKind,
        LLVMPPC_FP128TypeKind,
        LLVMLabelTypeKind,
        LLVMIntegerTypeKind,
        LLVMFunctionTypeKind,
        LLVMStructTypeKind,
        LLVMArrayTypeKind,
        LLVMPointerTypeKind,
        LLVMVectorTypeKind,
        LLVMMetadataTypeKind,
        LLVMX86_MMXTypeKind,
        LLVMTokenTypeKind;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMTypeKind of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }


    public enum LLVMLinkage {
        LLVMExternalLinkage,
        LLVMAvailableExternallyLinkage,
        LLVMLinkOnceAnyLinkage,
        LLVMLinkOnceODRLinkage,
        LLVMLinkOnceODRAutoHideLinkage,
        LLVMWeakAnyLinkage,
        LLVMWeakODRLinkage,
        LLVMAppendingLinkage,
        LLVMInternalLinkage,
        LLVMPrivateLinkage,
        LLVMDLLImportLinkage,
        LLVMDLLExportLinkage,
        LLVMExternalWeakLinkage,
        LLVMGhostLinkage,
        LLVMCommonLinkage,
        LLVMLinkerPrivateLinkage,
        LLVMLinkerPrivateWeakLinkage;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMLinkage of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMVisibility {
        LLVMDefaultVisibility,
        LLVMHiddenVisibility,
        LLVMProtectedVisibility;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMVisibility of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMDLLStorageClass {
        LLVMDefaultStorageClass,
        LLVMDLLImportStorageClass,
        LLVMDLLExportStorageClass;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMDLLStorageClass of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMCallConv {
        LLVMCCallConv,
        LLVMFastCallConv,
        LLVMColdCallConv,
        LLVMWebKitJSCallConv,
        LLVMAnyRegCallConv,
        LLVMX86StdcallCallConv,
        LLVMX86FastcallCallConv;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMCallConv of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMValueKind {
        LLVMArgumentValueKind,
        LLVMBasicBlockValueKind,
        LLVMMemoryUseValueKind,
        LLVMMemoryDefValueKind,
        LLVMMemoryPhiValueKind,
        LLVMFunctionValueKind,
        LLVMGlobalAliasValueKind,
        LLVMGlobalIFuncValueKind,
        LLVMGlobalVariableValueKind,
        LLVMBlockAddressValueKind,
        LLVMConstantExprValueKind,
        LLVMConstantArrayValueKind,
        LLVMConstantStructValueKind,
        LLVMConstantVectorValueKind,
        LLVMUndefValueValueKind,
        LLVMConstantAggregateZeroValueKind,
        LLVMConstantDataArrayValueKind,
        LLVMConstantDataVectorValueKind,
        LLVMConstantIntValueKind,
        LLVMConstantFPValueKind,
        LLVMConstantPointerNullValueKind,
        LLVMConstantTokenNoneValueKind,
        LLVMMetadataAsValueValueKind,
        LLVMInlineAsmValueKind,
        LLVMInstructionValueKind;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMValueKind of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMIntPredicate {
        LLVMIntEQ,
        LLVMIntNE,
        LLVMIntUGT,
        LLVMIntUGE,
        LLVMIntULT,
        LLVMIntULE,
        LLVMIntSGT,
        LLVMIntSGE,
        LLVMIntSLT,
        LLVMIntSLE;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMIntPredicate of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMRealPredicate {
        LLVMRealPredicateFalse,
        LLVMRealOEQ,
        LLVMRealOGT,
        LLVMRealOGE,
        LLVMRealOLT,
        LLVMRealOLE,
        LLVMRealORD,
        LLVMRealONE,
        LLVMRealUNO,
        LLVMRealUEQ,
        LLVMRealUGT,
        LLVMRealUGE,
        LLVMRealULT,
        LLVMRealULE,
        LLVMRealUNE,
        LLVMRealPredicateTrue;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMRealPredicate of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMLandingPadClauseTy {
        LLVMLandingPadCatch,
        LLVMLandingPadFilter;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMLandingPadClauseTy of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMThreadLocalMode {
        LLVMNotThreadLocal,
        LLVMGeneralDynamicTLSModel,
        LLVMLocalDynamicTLSModel,
        LLVMInitialExecTLSModel,
        LLVMLocalExecTLSModel;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMThreadLocalMode of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMAtomicOrdering {
        LLVMAtomicOrderingNotAtomic,
        LLVMAtomicOrderingUnordered,
        LLVMAtomicOrderingMonotonic,
        LLVMAtomicOrderingAcquire,
        LLVMAtomicOrderingRelease,
        LLVMAtomicOrderingAcquireRelease,
        LLVMAtomicOrderingSequentiallyConsistent;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMAtomicOrdering of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMAtomicRMWBinOp {
        LLVMAtomicRMWBinOpXchg,
        LLVMAtomicRMWBinOpAdd,
        LLVMAtomicRMWBinOpSub,
        LLVMAtomicRMWBinOpAnd,
        LLVMAtomicRMWBinOpNand,
        LLVMAtomicRMWBinOpOr,
        LLVMAtomicRMWBinOpXor,
        LLVMAtomicRMWBinOpMax,
        LLVMAtomicRMWBinOpMin,
        LLVMAtomicRMWBinOpUMax,
        LLVMAtomicRMWBinOpUMin;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMAtomicRMWBinOp of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMDiagnosticSeverity {
        LLVMDSError,
        LLVMDSWarning,
        LLVMDSRemark,
        LLVMDSNote;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMDiagnosticSeverity of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMAddAttribute(LLVMValueRef Arg, int PA) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddAttributeAtIndex(LLVMValueRef F, int Idx, LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal, LLVMBasicBlockRef Dest) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMAddFunction(LLVMModuleRef M, String Name, LLVMTypeRef FunctionTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddFunctionAttr(LLVMValueRef Fn, int PA) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef IncomingValue, LLVMBasicBlockRef IncomingBlock) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef[] IncomingValues, LLVMBasicBlockRef[] IncomingBlocks) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddInstrAttribute(LLVMValueRef Instr, int index, int PA) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMAlignOf(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C, LLVMValueRef Fn, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, int ElementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBasicBlockAsValue(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef B, LLVMValueRef[] RetVals) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAnd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAtomicCmpXchg(LLVMBuilderRef B, LLVMValueRef Ptr, LLVMValueRef Cmp, LLVMValueRef New, LLVMAtomicOrdering SuccessOrdering, LLVMAtomicOrdering FailureOrdering, boolean SingleThread) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAtomicRMW(LLVMBuilderRef B, LLVMAtomicRMWBinOp Op, LLVMValueRef Ptr, LLVMValueRef Val, LLVMAtomicOrdering Ordering, boolean SingleThread) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildBr(LLVMBuilderRef B, LLVMBasicBlockRef Dest) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildCall(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef B, LLVMValueRef If, LLVMBasicBlockRef Then, LLVMBasicBlockRef Else) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef B, LLVMValueRef AggVal, int Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef B, LLVMRealPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering Ordering, boolean SingleThread, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFree(LLVMBuilderRef B, LLVMValueRef Ptr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildICmp(LLVMBuilderRef B, LLVMIntPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr, int NumDests) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef EltVal, LLVMValueRef Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef B, LLVMValueRef AggVal, LLVMValueRef EltVal, int Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildLShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef PersFn, int NumClauses, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildLoad(LLVMBuilderRef B, LLVMValueRef Ptr, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNot(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildOr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPhi(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildRet(LLVMBuilderRef Builder, LLVMValueRef V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSelect(LLVMBuilderRef B, LLVMValueRef If, LLVMValueRef Then, LLVMValueRef Else, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildShl(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef B, LLVMValueRef V1, LLVMValueRef V2, LLVMValueRef Mask, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildStore(LLVMBuilderRef B, LLVMValueRef Val, LLVMValueRef Ptr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer, int Idx, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef B, LLVMValueRef V, LLVMBasicBlockRef Else, int NumCases) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildURem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef B) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef B, LLVMValueRef List, LLVMTypeRef Ty, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildXor(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildZExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMClearInsertionPosition(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMCloneModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstInt(LLVMTypeRef IntTy, long N, boolean SignExtend) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy, long... Words) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstNull(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstString(String Str, boolean DontNullTerminate) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstStringInContext(LLVMContextRef C, String Str, boolean DontNullTerminate) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMContextCreate() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMContextDispose(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMContextSetDiagnosticHandler(LLVMContextRef C, Arena arena, LLVMDiagnosticHandler Handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMContextSetYieldCallback(LLVMContextRef C, Arena arena, LLVMYieldCallback Callback) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMCountIncoming(LLVMValueRef PhiNode) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMCountParams(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, int KindID, long Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Deprecated
    public static LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegment(MemorySegment InputData, String BufferName, boolean RequiresNullTerminator) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegmentCopy(MemorySegment InputData, String BufferName) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleProviderRef LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMPassManagerRef LLVMCreatePassManager() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAttributeRef LLVMCreateStringAttribute(LLVMContextRef C, String K, String V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDeleteBasicBlock(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeBuilder(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeModuleProvider(LLVMModuleProviderRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposePassManager(LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMDoubleType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMDoubleTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDumpModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDumpType(LLVMTypeRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFP128Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFP128TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMFinalizeFunctionPassManager(LLVMPassManagerRef FPM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFloatType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFloatTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFunctionType(LLVMTypeRef ReturnType, LLVMTypeRef[] ParamTypes, boolean IsVarArg) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetAlignment(LLVMValueRef V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMGetAllocatedType(LLVMValueRef Alloca) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetArrayLength(LLVMTypeRef ArrayTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetAttribute(LLVMValueRef Arg) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetBasicBlockName(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetBasicBlockTerminator(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetBufferSegment(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, int Idx) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAtomicOrdering LLVMGetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAtomicOrdering LLVMGetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMOpcode LLVMGetConstOpcode(LLVMValueRef ConstantVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDataLayout(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDataLayoutStr(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDiagInfoDescription(LLVMDiagnosticInfoRef DI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMDiagnosticSeverity LLVMGetDiagInfoSeverity(LLVMDiagnosticInfoRef DI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMGetElementType(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetEnumAttributeKind(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetEnumAttributeKindForName(String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetEnumAttributeValue(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetFirstFunction(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMCallConv LLVMGetFunctionCallConv(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetGC(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMGetGlobalContext() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMPassRegistryRef LLVMGetGlobalPassRegistry() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, int Index) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetIncomingValue(LLVMValueRef PhiNode, int Index) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetIntTypeWidth(LLVMTypeRef IntegerTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetLastEnumAttributeKind() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetLastFunction(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMLinkage LLVMGetLinkage(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetMDKindID(String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetMDKindIDInContext(LLVMContextRef C, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMGetModuleContext(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetModuleIdentifier(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef M, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetNextFunction(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetNumClauses(LLVMValueRef LandingPad) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetParam(LLVMValueRef Fn, int Index) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetParamParent(LLVMValueRef Inst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef[] LLVMGetParams(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetPointerAddressSpace(LLVMTypeRef PointerTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetPreviousFunction(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMGetReturnType(LLVMTypeRef FunctionTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSection(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetStringAttributeKind(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetStringAttributeValue(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetStructName(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTarget(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMGetTypeByName(LLVMModuleRef M, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMGetTypeContext(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeKind LLVMGetTypeKind(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGetVectorSize(LLVMTypeRef VectorTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMVisibility LLVMGetVisibility(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMGetVolatile(LLVMValueRef MemoryAccessInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMHalfType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMHalfTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMHasUnnamedAddr(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMInitializeFunctionPassManager(LLVMPassManagerRef FPM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMInsertBasicBlockInContext(LLVMContextRef C, LLVMBasicBlockRef BB, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInsertIntoBuilder(LLVMBuilderRef Builder, LLVMValueRef Instr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInsertIntoBuilderWithName(LLVMBuilderRef Builder, LLVMValueRef Instr, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt128Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt16Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt1Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt1TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt32Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt64Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt8Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntType(int NumBits) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, int NumBits) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsAtomicSingleThread(LLVMValueRef AtomicInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsCleanup(LLVMValueRef LandingPad) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsDeclaration(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsEnumAttribute(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsFunctionVarArg(LLVMTypeRef FunctionTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsInBounds(LLVMValueRef GEP) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Deprecated
    public static boolean LLVMIsMultithreaded() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsNull(LLVMValueRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsOpaqueStruct(LLVMTypeRef StructTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsPackedStruct(LLVMTypeRef StructTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsStringAttribute(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMLabelType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMModuleCreateWithName(String ModuleID) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMModuleCreateWithNameInContext(String ModuleID, LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMPPCFP128Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMPPCFP128TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMPointerType(LLVMTypeRef ElementType, int AddressSpace) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPositionBuilder(LLVMBuilderRef Builder, LLVMBasicBlockRef Block, LLVMValueRef Instr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPositionBuilderAtEnd(LLVMBuilderRef Builder, LLVMBasicBlockRef Block) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPositionBuilderBefore(LLVMBuilderRef Builder, LLVMValueRef Instr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMPrintModuleToString(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMPrintTypeToString(LLVMTypeRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMPrintValueToString(LLVMValueRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRemoveAttribute(LLVMValueRef Arg, int PA) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRemoveInstrAttribute(LLVMValueRef Instr, int index, int PA) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMRunFunctionPassManager(LLVMPassManagerRef FPM, LLVMValueRef F) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMRunPassManager(LLVMPassManagerRef PM, LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetAlignment(LLVMValueRef V, int Bytes) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetAtomicSingleThread(LLVMValueRef AtomicInst, boolean SingleThread) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetCleanup(LLVMValueRef LandingPad, boolean Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetDataLayout(LLVMModuleRef M, String DataLayoutStr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetFunctionCallConv(LLVMValueRef Fn, LLVMCallConv CC) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetGC(LLVMValueRef Fn, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetInstrParamAlignment(LLVMValueRef Instr, int index, int Align) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetIsInBounds(LLVMValueRef GEP, boolean InBounds) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetModuleIdentifier(LLVMModuleRef M, String Ident) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetModuleInlineAsm(LLVMModuleRef M, String Asm) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetParamAlignment(LLVMValueRef Arg, int Align) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetSection(LLVMValueRef Global, String Section) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetTarget(LLVMModuleRef M, String Triple) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetUnnamedAddr(LLVMValueRef Global, boolean HasUnnamedAddr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, boolean IsVolatile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMShutdown() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMSizeOf(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Deprecated
    public static boolean LLVMStartMultithreaded() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Deprecated
    public static void LLVMStopMultithreaded() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMStructCreateNamed(LLVMContextRef C, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMStructSetBody(LLVMTypeRef StructTy, LLVMTypeRef[] ElementTypes, boolean Packed) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMStructType(LLVMTypeRef[] ElementTypes, boolean Packed) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMStructTypeInContext(LLVMContextRef C, LLVMTypeRef[] ElementTypes, boolean Packed) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTypeIsSized(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMValueIsBasicBlock(LLVMValueRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVectorType(LLVMTypeRef ElementType, int ElementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVoidType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVoidTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86FP80Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86FP80TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86MMXType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }
}

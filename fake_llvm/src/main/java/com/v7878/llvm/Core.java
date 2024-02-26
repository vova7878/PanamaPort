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

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import com.v7878.llvm.Types.LLVMModuleProviderRef;

public class Core {

    private static int stub() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static class LLVMAttribute {
        public static final int LLVMZExtAttribute = stub();
        public static final int LLVMSExtAttribute = stub();
        public static final int LLVMNoReturnAttribute = stub();
        public static final int LLVMInRegAttribute = stub();
        public static final int LLVMStructRetAttribute = stub();
        public static final int LLVMNoUnwindAttribute = stub();
        public static final int LLVMNoAliasAttribute = stub();
        public static final int LLVMByValAttribute = stub();
        public static final int LLVMNestAttribute = stub();
        public static final int LLVMReadNoneAttribute = stub();
        public static final int LLVMReadOnlyAttribute = stub();
        public static final int LLVMNoInlineAttribute = stub();
        public static final int LLVMAlwaysInlineAttribute = stub();
        public static final int LLVMOptimizeForSizeAttribute = stub();
        public static final int LLVMStackProtectAttribute = stub();
        public static final int LLVMStackProtectReqAttribute = stub();
        public static final int LLVMAlignment = stub();
        public static final int LLVMNoCaptureAttribute = stub();
        public static final int LLVMNoRedZoneAttribute = stub();
        public static final int LLVMNoImplicitFloatAttribute = stub();
        public static final int LLVMNakedAttribute = stub();
        public static final int LLVMInlineHintAttribute = stub();
        public static final int LLVMStackAlignment = stub();
        public static final int LLVMReturnsTwice = stub();
        public static final int LLVMUWTable = stub();
        public static final int LLVMNonLazyBind = stub();
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

    public enum LLVMAttributeIndex {
        LLVMAttributeReturnIndex,
        LLVMAttributeFunctionIndex;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMAttributeIndex of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMShutdown() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMContextCreate() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMGetGlobalContext() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMContextDispose(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDiagInfoDescription(LLVMDiagnosticInfoRef DI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMDiagnosticSeverity LLVMGetDiagInfoSeverity(LLVMDiagnosticInfoRef DI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetMDKindIDInContext(LLVMContextRef C, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetMDKindID(String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetEnumAttributeKindForName(String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetLastEnumAttributeKind() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, int /* unsigned */ KindID, long /* uint64_t */ Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetEnumAttributeKind(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetEnumAttributeValue(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAttributeRef LLVMCreateStringAttribute(LLVMContextRef C, String K, String V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsEnumAttribute(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsStringAttribute(LLVMAttributeRef A) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMModuleCreateWithName(String ModuleID) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMModuleCreateWithNameInContext(String ModuleID, LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMCloneModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetModuleIdentifier(LLVMModuleRef M, String Ident) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDataLayoutStr(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetDataLayout(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetDataLayout(LLVMModuleRef M, String DataLayoutStr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTarget(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetTarget(LLVMModuleRef M, String Triple) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDumpModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMPrintModuleToString(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMAddFunction(LLVMModuleRef M, String Name, LLVMTypeRef FunctionTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeKind LLVMGetTypeKind(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTypeIsSized(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMContextRef LLVMGetTypeContext(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDumpType(LLVMTypeRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMPrintTypeToString(LLVMTypeRef Val) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt1TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, int /* unsigned */ NumBits) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt1Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt8Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt16Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt32Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt64Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMInt128Type() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntType(int /* unsigned */ NumBits) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetIntTypeWidth(LLVMTypeRef IntegerTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMFunctionType(LLVMTypeRef ReturnType, LLVMTypeRef[] ParamTypes, boolean IsVarArg) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMStructTypeInContext(LLVMContextRef C, LLVMTypeRef[] ElementTypes, boolean Packed) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMStructType(LLVMTypeRef[] ElementTypes, boolean Packed) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsPackedStruct(LLVMTypeRef StructTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsOpaqueStruct(LLVMTypeRef StructTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMGetElementType(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetArrayLength(LLVMTypeRef ArrayTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMPointerType(LLVMTypeRef ElementType, int /* unsigned */ AddressSpace) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetPointerAddressSpace(LLVMTypeRef PointerTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVectorType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetVectorSize(LLVMTypeRef VectorTy) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVoidTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMVoidType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMLabelType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMX86MMXType() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstNull(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstInt(LLVMTypeRef IntTy, long /* unsigned long long */ N, boolean SignExtend) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy, long... /* uint64_t */ Words) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstStringInContext(LLVMContextRef C, String Str, boolean DontNullTerminate) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMConstString(String Str, boolean DontNullTerminate) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMLinkage LLVMGetLinkage(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSection(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetSection(LLVMValueRef Global, String Section) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMVisibility LLVMGetVisibility(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGetAlignment(LLVMValueRef V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetAlignment(LLVMValueRef V, int /* unsigned */ Bytes) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C, LLVMValueRef Fn, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPositionBuilderAtEnd(LLVMBuilderRef Builder, LLVMBasicBlockRef Block) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeBuilder(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef Builder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildRet(LLVMBuilderRef Builder, LLVMValueRef V) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildURem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildShl(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildLShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAnd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildOr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildXor(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildNot(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMGetVolatile(LLVMValueRef MemoryAccessInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, boolean IsVolatile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildZExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildCall(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef B, LLVMValueRef AggVal, int /* unsigned */ Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef B, LLVMValueRef AggVal, LLVMValueRef EltVal, int /* unsigned */ Index, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleProviderRef LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeModuleProvider(LLVMModuleProviderRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegment(
            MemorySegment InputData, String BufferName, boolean RequiresNullTerminator) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegmentCopy(
            MemorySegment InputData, String BufferName) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* size_t */ LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetBufferSegment(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMPassRegistryRef LLVMGetGlobalPassRegistry() {
        throw new UnsupportedOperationException("Stub!");
    }
}

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


    public static class LLVMAttribute {
        public static final int LLVMZExtAttribute = 1;
        public static final int LLVMSExtAttribute = 1 << 1;
        public static final int LLVMNoReturnAttribute = 1 << 2;
        public static final int LLVMInRegAttribute = 1 << 3;
        public static final int LLVMStructRetAttribute = 1 << 4;
        public static final int LLVMNoUnwindAttribute = 1 << 5;
        public static final int LLVMNoAliasAttribute = 1 << 6;
        public static final int LLVMByValAttribute = 1 << 7;
        public static final int LLVMNestAttribute = 1 << 8;
        public static final int LLVMReadNoneAttribute = 1 << 9;
        public static final int LLVMReadOnlyAttribute = 1 << 10;
        public static final int LLVMNoInlineAttribute = 1 << 11;
        public static final int LLVMAlwaysInlineAttribute = 1 << 12;
        public static final int LLVMOptimizeForSizeAttribute = 1 << 13;
        public static final int LLVMStackProtectAttribute = 1 << 14;
        public static final int LLVMStackProtectReqAttribute = 1 << 15;
        public static final int LLVMAlignment = 31 << 16;
        public static final int LLVMNoCaptureAttribute = 1 << 21;
        public static final int LLVMNoRedZoneAttribute = 1 << 22;
        public static final int LLVMNoImplicitFloatAttribute = 1 << 23;
        public static final int LLVMNakedAttribute = 1 << 24;
        public static final int LLVMInlineHintAttribute = 1 << 25;
        public static final int LLVMStackAlignment = 7 << 26;
        public static final int LLVMReturnsTwice = 1 << 29;
        public static final int LLVMUWTable = 1 << 30;
        public static final int LLVMNonLazyBind = 1 << 31;

        // FIXME: These attributes are currently not included in the C API as
        // a temporary measure until the API/ABI impact to the C API is understood
        // and the path forward agreed upon.

        // LLVMSanitizeAddressAttribute = 1ULL << 32,
        // LLVMStackProtectStrongAttribute = 1ULL<<35,
        // LLVMColdAttribute = 1ULL << 40,
        // LLVMOptimizeNoneAttribute = 1ULL << 42,
        // LLVMInAllocaAttribute = 1ULL << 43,
        // LLVMNonNullAttribute = 1ULL << 44,
        // LLVMJumpTableAttribute = 1ULL << 45,
        // LLVMConvergentAttribute = 1ULL << 46,
        // LLVMSafeStackAttribute = 1ULL << 47,
        // LLVMSwiftSelfAttribute = 1ULL << 48,
        // LLVMSwiftErrorAttribute = 1ULL << 49,
    }

    public enum LLVMOpcode {
        LLVMRet(),
        LLVMBr(),
        LLVMSwitch(),
        LLVMIndirectBr(),
        LLVMInvoke(),
        LLVMUnreachable(),
        LLVMAdd(),
        LLVMFAdd(),
        LLVMSub(),
        LLVMFSub(),
        LLVMMul(),
        LLVMFMul(),
        LLVMUDiv(),
        LLVMSDiv(),
        LLVMFDiv(),
        LLVMURem(),
        LLVMSRem(),
        LLVMFRem(),
        LLVMShl(),
        LLVMLShr(),
        LLVMAShr(),
        LLVMAnd(),
        LLVMOr(),
        LLVMXor(),
        LLVMAlloca(),
        LLVMLoad(),
        LLVMStore(),
        LLVMGetElementPtr(),
        LLVMTrunc(),
        LLVMZExt(),
        LLVMSExt(),
        LLVMFPToUI(),
        LLVMFPToSI(),
        LLVMUIToFP(),
        LLVMSIToFP(),
        LLVMFPTrunc(),
        LLVMFPExt(),
        LLVMPtrToInt(),
        LLVMIntToPtr(),
        LLVMBitCast(),
        LLVMAddrSpaceCast(),
        LLVMICmp(),
        LLVMFCmp(),
        LLVMPHI(),
        LLVMCall(),
        LLVMSelect(),
        LLVMUserOp1(),
        LLVMUserOp2(),
        LLVMVAArg(),
        LLVMExtractElement(),
        LLVMInsertElement(),
        LLVMShuffleVector(),
        LLVMExtractValue(),
        LLVMInsertValue(),
        LLVMFence(),
        LLVMAtomicCmpXchg(),
        LLVMAtomicRMW(),
        LLVMResume(),
        LLVMLandingPad(),
        LLVMCleanupRet(),
        LLVMCatchRet(),
        LLVMCatchPad(),
        LLVMCleanupPad(),
        LLVMCatchSwitch();

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

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
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

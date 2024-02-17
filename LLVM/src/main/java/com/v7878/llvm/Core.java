package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONTS_CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.unsafe.Utils.nothrows_run;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;

public class Core {

    static final Class<?> LLVMAttribute = ENUM;

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

    static final Class<?> LLVMOpcode = ENUM;

    public enum LLVMOpcode {
        /* Terminator Instructions */
        LLVMRet(1),
        LLVMBr(2),
        LLVMSwitch(3),
        LLVMIndirectBr(4),
        LLVMInvoke(5),
        /* removed 6 due to API changes */
        LLVMUnreachable(7),
        /* Standard Binary Operators */
        LLVMAdd(8),
        LLVMFAdd(9),
        LLVMSub(10),
        LLVMFSub(11),
        LLVMMul(12),
        LLVMFMul(13),
        LLVMUDiv(14),
        LLVMSDiv(15),
        LLVMFDiv(16),
        LLVMURem(17),
        LLVMSRem(18),
        LLVMFRem(19),
        /* Logical Operators */
        LLVMShl(20),
        LLVMLShr(21),
        LLVMAShr(22),
        LLVMAnd(23),
        LLVMOr(24),
        LLVMXor(25),
        /* Memory Operators */
        LLVMAlloca(26),
        LLVMLoad(27),
        LLVMStore(28),
        LLVMGetElementPtr(29),
        /* Cast Operators */
        LLVMTrunc(30),
        LLVMZExt(31),
        LLVMSExt(32),
        LLVMFPToUI(33),
        LLVMFPToSI(34),
        LLVMUIToFP(35),
        LLVMSIToFP(36),
        LLVMFPTrunc(37),
        LLVMFPExt(38),
        LLVMPtrToInt(39),
        LLVMIntToPtr(40),
        LLVMBitCast(41),
        LLVMAddrSpaceCast(60),
        /* Other Operators */
        LLVMICmp(42),
        LLVMFCmp(43),
        LLVMPHI(44),
        LLVMCall(45),
        LLVMSelect(46),
        LLVMUserOp1(47),
        LLVMUserOp2(48),
        LLVMVAArg(49),
        LLVMExtractElement(50),
        LLVMInsertElement(51),
        LLVMShuffleVector(52),
        LLVMExtractValue(53),
        LLVMInsertValue(54),
        /* Atomic operators */
        LLVMFence(55),
        LLVMAtomicCmpXchg(56),
        LLVMAtomicRMW(57),
        /* Exception Handling Operators */
        LLVMResume(58),
        LLVMLandingPad(59),
        LLVMCleanupRet(61),
        LLVMCatchRet(62),
        LLVMCatchPad(63),
        LLVMCleanupPad(64),
        LLVMCatchSwitch(65);

        private final int value;

        LLVMOpcode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    static final Class<?> LLVMTypeKind = ENUM;

    public enum LLVMTypeKind {
        /**
         * < type with no size
         */
        LLVMVoidTypeKind,
        /**
         * < 16 bit floating point type
         */
        LLVMHalfTypeKind,
        /**
         * < 32 bit floating point type
         */
        LLVMFloatTypeKind,
        /**
         * < 64 bit floating point type
         */
        LLVMDoubleTypeKind,
        /**
         * < 80 bit floating point type (X87)
         */
        LLVMX86_FP80TypeKind,
        /**
         * < 128 bit floating point type (112-bit mantissa)
         */
        LLVMFP128TypeKind,
        /**
         * < 128 bit floating point type (two 64-bits)
         */
        LLVMPPC_FP128TypeKind,
        /**
         * < Labels
         */
        LLVMLabelTypeKind,
        /**
         * < Arbitrary bit width integers
         */
        LLVMIntegerTypeKind,
        /**
         * < Functions
         */
        LLVMFunctionTypeKind,
        /**
         * < Structures
         */
        LLVMStructTypeKind,
        /**
         * < Arrays
         */
        LLVMArrayTypeKind,
        /**
         * < Pointers
         */
        LLVMPointerTypeKind,
        /**
         * < SIMD 'packed' format, or other vector type
         */
        LLVMVectorTypeKind,
        /**
         * < Metadata
         */
        LLVMMetadataTypeKind,
        /**
         * < X86 MMX
         */
        LLVMX86_MMXTypeKind,
        /**
         * < Tokens
         */
        LLVMTokenTypeKind;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMLinkage = ENUM;

    public enum LLVMLinkage {
        /**
         * < Externally visible function
         */
        LLVMExternalLinkage,
        LLVMAvailableExternallyLinkage,
        /**
         * < Keep one copy of function when linking (inline)
         */
        LLVMLinkOnceAnyLinkage,
        /**
         * < Same, but only replaced by something
         * equivalent.
         */
        LLVMLinkOnceODRLinkage,
        /**
         * < Obsolete
         */
        LLVMLinkOnceODRAutoHideLinkage,
        /**
         * < Keep one copy of function when linking (weak)
         */
        LLVMWeakAnyLinkage,
        /**
         * < Same, but only replaced by something
         * equivalent.
         */
        LLVMWeakODRLinkage,
        /**
         * < Special purpose, only applies to global arrays
         */
        LLVMAppendingLinkage,
        /**
         * < Rename collisions when linking (static
         * functions)
         */
        LLVMInternalLinkage,
        /**
         * < Like Internal, but omit from symbol table
         */
        LLVMPrivateLinkage,
        /**
         * < Obsolete
         */
        LLVMDLLImportLinkage,
        /**
         * < Obsolete
         */
        LLVMDLLExportLinkage,
        /**
         * < ExternalWeak linkage description
         */
        LLVMExternalWeakLinkage,
        /**
         * < Obsolete
         */
        LLVMGhostLinkage,
        /**
         * < Tentative definitions
         */
        LLVMCommonLinkage,
        /**
         * < Like Private, but linker removes.
         */
        LLVMLinkerPrivateLinkage,
        /**
         * < Like LinkerPrivate, but is weak.
         */
        LLVMLinkerPrivateWeakLinkage;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMVisibility = ENUM;

    public enum LLVMVisibility {
        /**
         * < The GV is visible
         */
        LLVMDefaultVisibility,
        /**
         * < The GV is hidden
         */
        LLVMHiddenVisibility,
        /**
         * < The GV is protected
         */
        LLVMProtectedVisibility;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMDLLStorageClass = ENUM;

    public enum LLVMDLLStorageClass {
        LLVMDefaultStorageClass(0),
        /**
         * < Function to be imported from DLL.
         */
        LLVMDLLImportStorageClass(1),
        /**
         * < Function to be accessible from DLL.
         */
        LLVMDLLExportStorageClass(2);

        private final int value;

        LLVMDLLStorageClass(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    static final Class<?> LLVMCallConv = ENUM;

    public enum LLVMCallConv {
        LLVMCCallConv(0),
        LLVMFastCallConv(8),
        LLVMColdCallConv(9),
        LLVMWebKitJSCallConv(12),
        LLVMAnyRegCallConv(13),
        LLVMX86StdcallCallConv(64),
        LLVMX86FastcallCallConv(65);

        private final int value;

        LLVMCallConv(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    static final Class<?> LLVMValueKind = ENUM;

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
            return ordinal();
        }
    }

    static final Class<?> LLVMIntPredicate = ENUM;

    public enum LLVMIntPredicate {
        /**
         * < equal
         */
        LLVMIntEQ,
        /**
         * < not equal
         */
        LLVMIntNE,
        /**
         * < unsigned greater than
         */
        LLVMIntUGT,
        /**
         * < unsigned greater or equal
         */
        LLVMIntUGE,
        /**
         * < unsigned less than
         */
        LLVMIntULT,
        /**
         * < unsigned less or equal
         */
        LLVMIntULE,
        /**
         * < signed greater than
         */
        LLVMIntSGT,
        /**
         * < signed greater or equal
         */
        LLVMIntSGE,
        /**
         * < signed less than
         */
        LLVMIntSLT,

        /**
         * < signed less or equal
         */
        LLVMIntSLE;

        public int value() {
            return ordinal() + 32;
        }
    }

    static final Class<?> LLVMRealPredicate = ENUM;

    public enum LLVMRealPredicate {
        /**
         * < Always false (always folded)
         */
        LLVMRealPredicateFalse,
        /**
         * < True if ordered and equal
         */
        LLVMRealOEQ,
        /**
         * < True if ordered and greater than
         */
        LLVMRealOGT,
        /**
         * < True if ordered and greater than or equal
         */
        LLVMRealOGE,
        /**
         * < True if ordered and less than
         */
        LLVMRealOLT,
        /**
         * < True if ordered and less than or equal
         */
        LLVMRealOLE,
        /**
         * < True if ordered and operands are unequal
         */
        LLVMRealORD,
        /**
         * < True if ordered (no nans)
         */
        LLVMRealONE,
        /**
         * < True if unordered: isnan(X) | isnan(Y)
         */
        LLVMRealUNO,
        /**
         * < True if unordered or equal
         */
        LLVMRealUEQ,
        /**
         * < True if unordered or greater than
         */
        LLVMRealUGT,
        /**
         * < True if unordered, greater than, or equal
         */
        LLVMRealUGE,
        /**
         * < True if unordered or less than
         */
        LLVMRealULT,
        /**
         * < True if unordered, less than, or equal
         */
        LLVMRealULE,
        /**
         * < True if unordered or not equal
         */
        LLVMRealUNE,
        /**
         * < Always true (always folded)
         */
        LLVMRealPredicateTrue;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMLandingPadClauseTy = ENUM;

    public enum LLVMLandingPadClauseTy {
        /**
         * < A catch clause
         */
        LLVMLandingPadCatch,
        /**
         * < A filter clause
         */
        LLVMLandingPadFilter;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMThreadLocalMode = ENUM;

    public enum LLVMThreadLocalMode {
        LLVMNotThreadLocal,
        LLVMGeneralDynamicTLSModel,
        LLVMLocalDynamicTLSModel,
        LLVMInitialExecTLSModel,
        LLVMLocalExecTLSModel;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMAtomicOrdering = ENUM;

    public enum LLVMAtomicOrdering {
        /**
         * < A load or store which is not atomic
         */
        LLVMAtomicOrderingNotAtomic(0),
        /**
         * < Lowest level of atomicity, guarantees
         * somewhat sane results, lock free.
         */
        LLVMAtomicOrderingUnordered(1),
        /**
         * < guarantees that if you take all the
         * operations affecting a specific address,
         * a consistent ordering exists
         */
        LLVMAtomicOrderingMonotonic(2),
        /**
         * < Acquire provides a barrier of the sort
         * necessary to acquire a lock to access other
         * memory with normal loads and stores.
         */
        LLVMAtomicOrderingAcquire(4),
        /**
         * < Release is similar to Acquire, but with
         * a barrier of the sort necessary to release
         * a lock.
         */
        LLVMAtomicOrderingRelease(5),
        /**
         * < provides both an Acquire and a
         * Release barrier (for fences and
         * operations which both read and write
         * memory).
         */
        LLVMAtomicOrderingAcquireRelease(6),
        /**
         * < provides Acquire semantics
         * for loads and Release
         * semantics for stores.
         * Additionally, it guarantees
         * that a total ordering exists
         * between all
         * SequentiallyConsistent
         * operations.
         */
        LLVMAtomicOrderingSequentiallyConsistent(7);

        private final int value;

        LLVMAtomicOrdering(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    static final Class<?> LLVMAtomicRMWBinOp = ENUM;

    public enum LLVMAtomicRMWBinOp {
        /**
         * < Set the new value and return the one old
         */
        LLVMAtomicRMWBinOpXchg,
        /**
         * < Add a value and return the old one
         */
        LLVMAtomicRMWBinOpAdd,
        /**
         * < Subtract a value and return the old one
         */
        LLVMAtomicRMWBinOpSub,
        /**
         * < And a value and return the old one
         */
        LLVMAtomicRMWBinOpAnd,
        /**
         * < Not-And a value and return the old one
         */
        LLVMAtomicRMWBinOpNand,
        /**
         * < OR a value and return the old one
         */
        LLVMAtomicRMWBinOpOr,
        /**
         * < Xor a value and return the old one
         */
        LLVMAtomicRMWBinOpXor,
        /**
         * < Sets the value if it's greater than the
         * original using a signed comparison and return
         * the old one
         */
        LLVMAtomicRMWBinOpMax,
        /**
         * < Sets the value if it's Smaller than the
         * original using a signed comparison and return
         * the old one
         */
        LLVMAtomicRMWBinOpMin,
        /**
         * < Sets the value if it's greater than the
         * original using an unsigned comparison and return
         * the old one
         */
        LLVMAtomicRMWBinOpUMax,
        /**
         * < Sets the value if it's greater than the
         * original using an unsigned comparison  and return
         * the old one
         */
        LLVMAtomicRMWBinOpUMin;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMDiagnosticSeverity = ENUM;

    public enum LLVMDiagnosticSeverity {
        LLVMDSError,
        LLVMDSWarning,
        LLVMDSRemark,
        LLVMDSNote;

        public int value() {
            return ordinal();
        }
    }

    static final Class<?> LLVMAttributeIndex = UNSIGNED_INT;

    /**
     * Attribute index are either LLVMAttributeReturnIndex,
     * LLVMAttributeFunctionIndex or a parameter number from 1 to N.
     */
    public enum LLVMAttributeIndex {
        LLVMAttributeReturnIndex(0),
        // ISO C restricts enumerator values to range of 'int'
        // (4294967295 is too large)
        // LLVMAttributeFunctionIndex = ~0U,
        LLVMAttributeFunctionIndex(-1);

        private final int value;

        LLVMAttributeIndex(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    //TODO
    //typedef void (*LLVMDiagnosticHandler)(LLVMDiagnosticInfoRef, void *);
    //typedef void (*LLVMYieldCallback)(LLVMContextRef, void *);

    private enum Function implements _Utils.Symbol {
        LLVMInitializeCore(void.class, LLVMPassRegistryRef),
        LLVMShutdown(void.class),
        LLVMCreateMessage(CHAR_PTR, CONTS_CHAR_PTR),
        LLVMDisposeMessage(void.class, CHAR_PTR),
        LLVMContextCreate(LLVMContextRef),
        LLVMGetGlobalContext(LLVMContextRef),
        //void LLVMContextSetDiagnosticHandler(LLVMContextRef C,
        //                                     LLVMDiagnosticHandler Handler,
        //                                     void *DiagnosticContext);
        //LLVMDiagnosticHandler LLVMContextGetDiagnosticHandler(LLVMContextRef C);
        //void *LLVMContextGetDiagnosticContext(LLVMContextRef C);
        //void LLVMContextSetYieldCallback(LLVMContextRef C, LLVMYieldCallback Callback,
        //                                 void *OpaqueHandle);
        //void LLVMContextDispose(LLVMContextRef C);
        //char *LLVMGetDiagInfoDescription(LLVMDiagnosticInfoRef DI);
        //LLVMDiagnosticSeverity LLVMGetDiagInfoSeverity(LLVMDiagnosticInfoRef DI);
        //unsigned LLVMGetMDKindIDInContext(LLVMContextRef C, const char *Name,
        //                                  unsigned SLen);
        //unsigned LLVMGetMDKindID(const char *Name, unsigned SLen);
        //unsigned LLVMGetEnumAttributeKindForName(const char *Name, size_t SLen);
        //unsigned LLVMGetLastEnumAttributeKind(void);
        //LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, unsigned KindID,
        //                                         uint64_t Val);
        //unsigned LLVMGetEnumAttributeKind(LLVMAttributeRef A);
        //uint64_t LLVMGetEnumAttributeValue(LLVMAttributeRef A);
        //LLVMAttributeRef LLVMCreateStringAttribute(LLVMContextRef C,
        //                                           const char *K, unsigned KLength,
        //                                           const char *V, unsigned VLength);
        //const char *LLVMGetStringAttributeKind(LLVMAttributeRef A, unsigned *Length);
        //const char *LLVMGetStringAttributeValue(LLVMAttributeRef A, unsigned *Length);
        //LLVMBool LLVMIsEnumAttribute(LLVMAttributeRef A);
        //LLVMBool LLVMIsStringAttribute(LLVMAttributeRef A);
        //LLVMModuleRef LLVMModuleCreateWithName(const char *ModuleID);
        //LLVMModuleRef LLVMModuleCreateWithNameInContext(const char *ModuleID,
        //                                                LLVMContextRef C);
        //LLVMModuleRef LLVMCloneModule(LLVMModuleRef M);
        //void LLVMDisposeModule(LLVMModuleRef M);
        //const char *LLVMGetModuleIdentifier(LLVMModuleRef M, size_t *Len);
        //void LLVMSetModuleIdentifier(LLVMModuleRef M, const char *Ident, size_t Len);
        //const char *LLVMGetDataLayoutStr(LLVMModuleRef M);
        //const char *LLVMGetDataLayout(LLVMModuleRef M);
        //void LLVMSetDataLayout(LLVMModuleRef M, const char *DataLayoutStr);
        //const char *LLVMGetTarget(LLVMModuleRef M);
        //void LLVMSetTarget(LLVMModuleRef M, const char *Triple);
        //void LLVMDumpModule(LLVMModuleRef M);
        //LLVMBool LLVMPrintModuleToFile(LLVMModuleRef M, const char *Filename,
        //                               char **ErrorMessage);
        //char *LLVMPrintModuleToString(LLVMModuleRef M);
        //void LLVMSetModuleInlineAsm(LLVMModuleRef M, const char *Asm);
        //LLVMContextRef LLVMGetModuleContext(LLVMModuleRef M);
        //LLVMTypeRef LLVMGetTypeByName(LLVMModuleRef M, const char *Name);
        //unsigned LLVMGetNamedMetadataNumOperands(LLVMModuleRef M, const char *Name);
        //void LLVMGetNamedMetadataOperands(LLVMModuleRef M, const char *Name,
        //                                  LLVMValueRef *Dest);
        //void LLVMAddNamedMetadataOperand(LLVMModuleRef M, const char *Name,
        //                                 LLVMValueRef Val);
        //LLVMValueRef LLVMAddFunction(LLVMModuleRef M, const char *Name,
        //                             LLVMTypeRef FunctionTy);
        //LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef M, const char *Name);
        //LLVMValueRef LLVMGetFirstFunction(LLVMModuleRef M);
        //LLVMValueRef LLVMGetLastFunction(LLVMModuleRef M);
        //LLVMValueRef LLVMGetNextFunction(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetPreviousFunction(LLVMValueRef Fn);
        //LLVMTypeKind LLVMGetTypeKind(LLVMTypeRef Ty);
        //LLVMBool LLVMTypeIsSized(LLVMTypeRef Ty);
        //LLVMContextRef LLVMGetTypeContext(LLVMTypeRef Ty);
        //void LLVMDumpType(LLVMTypeRef Val);
        //char *LLVMPrintTypeToString(LLVMTypeRef Val);
        //LLVMTypeRef LLVMInt1TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, unsigned NumBits);
        //LLVMTypeRef LLVMInt1Type(void);
        //LLVMTypeRef LLVMInt8Type(void);
        //LLVMTypeRef LLVMInt16Type(void);
        //LLVMTypeRef LLVMInt32Type(void);
        //LLVMTypeRef LLVMInt64Type(void);
        //LLVMTypeRef LLVMInt128Type(void);
        //LLVMTypeRef LLVMIntType(unsigned NumBits);
        //unsigned LLVMGetIntTypeWidth(LLVMTypeRef IntegerTy);
        //LLVMTypeRef LLVMHalfTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMFloatTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMDoubleTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMX86FP80TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMFP128TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMPPCFP128TypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMHalfType(void);
        //LLVMTypeRef LLVMFloatType(void);
        //LLVMTypeRef LLVMDoubleType(void);
        //LLVMTypeRef LLVMX86FP80Type(void);
        //LLVMTypeRef LLVMFP128Type(void);
        //LLVMTypeRef LLVMPPCFP128Type(void);
        //LLVMTypeRef LLVMFunctionType(LLVMTypeRef ReturnType,
        //                             LLVMTypeRef *ParamTypes, unsigned ParamCount,
        //                             LLVMBool IsVarArg);
        //LLVMBool LLVMIsFunctionVarArg(LLVMTypeRef FunctionTy);
        //LLVMTypeRef LLVMGetReturnType(LLVMTypeRef FunctionTy);
        //unsigned LLVMCountParamTypes(LLVMTypeRef FunctionTy);
        //void LLVMGetParamTypes(LLVMTypeRef FunctionTy, LLVMTypeRef *Dest);
        //LLVMTypeRef LLVMStructTypeInContext(LLVMContextRef C, LLVMTypeRef *ElementTypes,
        //                                    unsigned ElementCount, LLVMBool Packed);
        //LLVMTypeRef LLVMStructType(LLVMTypeRef *ElementTypes, unsigned ElementCount,
        //                           LLVMBool Packed);
        //LLVMTypeRef LLVMStructCreateNamed(LLVMContextRef C, const char *Name);
        //const char *LLVMGetStructName(LLVMTypeRef Ty);
        //void LLVMStructSetBody(LLVMTypeRef StructTy, LLVMTypeRef *ElementTypes,
        //                       unsigned ElementCount, LLVMBool Packed);
        //unsigned LLVMCountStructElementTypes(LLVMTypeRef StructTy);
        //void LLVMGetStructElementTypes(LLVMTypeRef StructTy, LLVMTypeRef *Dest);
        //LLVMTypeRef LLVMStructGetTypeAtIndex(LLVMTypeRef StructTy, unsigned i);
        //LLVMBool LLVMIsPackedStruct(LLVMTypeRef StructTy);
        //LLVMBool LLVMIsOpaqueStruct(LLVMTypeRef StructTy);
        //LLVMTypeRef LLVMGetElementType(LLVMTypeRef Ty);
        //LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, unsigned ElementCount);
        //unsigned LLVMGetArrayLength(LLVMTypeRef ArrayTy);
        //LLVMTypeRef LLVMPointerType(LLVMTypeRef ElementType, unsigned AddressSpace);
        //unsigned LLVMGetPointerAddressSpace(LLVMTypeRef PointerTy);
        //LLVMTypeRef LLVMVectorType(LLVMTypeRef ElementType, unsigned ElementCount);
        //unsigned LLVMGetVectorSize(LLVMTypeRef VectorTy);
        //LLVMTypeRef LLVMVoidTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C);
        //LLVMTypeRef LLVMVoidType(void);
        //LLVMTypeRef LLVMLabelType(void);
        //LLVMTypeRef LLVMX86MMXType(void);

        //TODO:
        //#define LLVM_FOR_EACH_VALUE_SUBCLASS(macro) \
        //  macro(Argument)                           \
        //  macro(BasicBlock)                         \
        //  macro(InlineAsm)                          \
        //  macro(User)                               \
        //    macro(Constant)                         \
        //      macro(BlockAddress)                   \
        //      macro(ConstantAggregateZero)          \
        //      macro(ConstantArray)                  \
        //      macro(ConstantDataSequential)         \
        //        macro(ConstantDataArray)            \
        //        macro(ConstantDataVector)           \
        //      macro(ConstantExpr)                   \
        //      macro(ConstantFP)                     \
        //      macro(ConstantInt)                    \
        //      macro(ConstantPointerNull)            \
        //      macro(ConstantStruct)                 \
        //      macro(ConstantTokenNone)              \
        //      macro(ConstantVector)                 \
        //      macro(GlobalValue)                    \
        //        macro(GlobalAlias)                  \
        //        macro(GlobalObject)                 \
        //          macro(Function)                   \
        //          macro(GlobalVariable)             \
        //      macro(UndefValue)                     \
        //    macro(Instruction)                      \
        //      macro(BinaryOperator)                 \
        //      macro(CallInst)                       \
        //        macro(IntrinsicInst)                \
        //          macro(DbgInfoIntrinsic)           \
        //            macro(DbgDeclareInst)           \
        //          macro(MemIntrinsic)               \
        //            macro(MemCpyInst)               \
        //            macro(MemMoveInst)              \
        //            macro(MemSetInst)               \
        //      macro(CmpInst)                        \
        //        macro(FCmpInst)                     \
        //        macro(ICmpInst)                     \
        //      macro(ExtractElementInst)             \
        //      macro(GetElementPtrInst)              \
        //      macro(InsertElementInst)              \
        //      macro(InsertValueInst)                \
        //      macro(LandingPadInst)                 \
        //      macro(PHINode)                        \
        //      macro(SelectInst)                     \
        //      macro(ShuffleVectorInst)              \
        //      macro(StoreInst)                      \
        //      macro(TerminatorInst)                 \
        //        macro(BranchInst)                   \
        //        macro(IndirectBrInst)               \
        //        macro(InvokeInst)                   \
        //        macro(ReturnInst)                   \
        //        macro(SwitchInst)                   \
        //        macro(UnreachableInst)              \
        //        macro(ResumeInst)                   \
        //        macro(CleanupReturnInst)            \
        //        macro(CatchReturnInst)              \
        //      macro(FuncletPadInst)                 \
        //        macro(CatchPadInst)                 \
        //        macro(CleanupPadInst)               \
        //      macro(UnaryInstruction)               \
        //        macro(AllocaInst)                   \
        //        macro(CastInst)                     \
        //          macro(AddrSpaceCastInst)          \
        //          macro(BitCastInst)                \
        //          macro(FPExtInst)                  \
        //          macro(FPToSIInst)                 \
        //          macro(FPToUIInst)                 \
        //          macro(FPTruncInst)                \
        //          macro(IntToPtrInst)               \
        //          macro(PtrToIntInst)               \
        //          macro(SExtInst)                   \
        //          macro(SIToFPInst)                 \
        //          macro(TruncInst)                  \
        //          macro(UIToFPInst)                 \
        //          macro(ZExtInst)                   \
        //        macro(ExtractValueInst)             \
        //        macro(LoadInst)                     \
        //        macro(VAArgInst)

        //LLVMTypeRef LLVMTypeOf(LLVMValueRef Val);
        //LLVMValueKind LLVMGetValueKind(LLVMValueRef Val);
        //const char *LLVMGetValueName(LLVMValueRef Val);
        //void LLVMSetValueName(LLVMValueRef Val, const char *Name);
        //void LLVMDumpValue(LLVMValueRef Val);
        //char *LLVMPrintValueToString(LLVMValueRef Val);
        //void LLVMReplaceAllUsesWith(LLVMValueRef OldVal, LLVMValueRef NewVal);
        //LLVMBool LLVMIsConstant(LLVMValueRef Val);
        //LLVMBool LLVMIsUndef(LLVMValueRef Val);

        //TODO:
        //#define LLVM_DECLARE_VALUE_CAST(name) \
        //  LLVMValueRef LLVMIsA##name(LLVMValueRef Val);
        //LLVM_FOR_EACH_VALUE_SUBCLASS(LLVM_DECLARE_VALUE_CAST)

        //LLVMValueRef LLVMIsAMDNode(LLVMValueRef Val);
        //LLVMValueRef LLVMIsAMDString(LLVMValueRef Val);
        //LLVMUseRef LLVMGetFirstUse(LLVMValueRef Val);
        //LLVMUseRef LLVMGetNextUse(LLVMUseRef U);
        //LLVMValueRef LLVMGetUser(LLVMUseRef U);
        //LLVMValueRef LLVMGetUsedValue(LLVMUseRef U);
        //LLVMValueRef LLVMGetOperand(LLVMValueRef Val, unsigned Index);
        //LLVMUseRef LLVMGetOperandUse(LLVMValueRef Val, unsigned Index);
        //void LLVMSetOperand(LLVMValueRef User, unsigned Index, LLVMValueRef Val);
        //int LLVMGetNumOperands(LLVMValueRef Val);
        //LLVMValueRef LLVMConstNull(LLVMTypeRef Ty);
        //LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty);
        //LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty);
        //LLVMBool LLVMIsNull(LLVMValueRef Val);
        //LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty);
        //LLVMValueRef LLVMConstInt(LLVMTypeRef IntTy, unsigned long long N,
        //                          LLVMBool SignExtend);
        //LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy,
        //                                              unsigned NumWords,
        //                                              const uint64_t Words[]);
        //LLVMValueRef LLVMConstIntOfString(LLVMTypeRef IntTy, const char *Text,
        //                                  uint8_t Radix);
        //LLVMValueRef LLVMConstIntOfStringAndSize(LLVMTypeRef IntTy, const char *Text,
        //                                         unsigned SLen, uint8_t Radix);
        //LLVMValueRef LLVMConstReal(LLVMTypeRef RealTy, double N);
        //LLVMValueRef LLVMConstRealOfString(LLVMTypeRef RealTy, const char *Text);
        //LLVMValueRef LLVMConstRealOfStringAndSize(LLVMTypeRef RealTy, const char *Text,
        //                                          unsigned SLen);
        //unsigned long long LLVMConstIntGetZExtValue(LLVMValueRef ConstantVal);
        //long long LLVMConstIntGetSExtValue(LLVMValueRef ConstantVal);
        //double LLVMConstRealGetDouble(LLVMValueRef ConstantVal, LLVMBool *losesInfo);
        //LLVMValueRef LLVMConstStringInContext(LLVMContextRef C, const char *Str,
        //                                      unsigned Length, LLVMBool DontNullTerminate);
        //LLVMValueRef LLVMConstString(const char *Str, unsigned Length,
        //                             LLVMBool DontNullTerminate);
        //LLVMBool LLVMIsConstantString(LLVMValueRef c);
        //const char *LLVMGetAsString(LLVMValueRef c, size_t *Length);
        //LLVMValueRef LLVMConstStructInContext(LLVMContextRef C,
        //                                      LLVMValueRef *ConstantVals,
        //                                      unsigned Count, LLVMBool Packed);
        //LLVMValueRef LLVMConstStruct(LLVMValueRef *ConstantVals, unsigned Count,
        //                             LLVMBool Packed);
        //LLVMValueRef LLVMConstArray(LLVMTypeRef ElementTy,
        //                            LLVMValueRef *ConstantVals, unsigned Length);
        //LLVMValueRef LLVMConstNamedStruct(LLVMTypeRef StructTy,
        //                                  LLVMValueRef *ConstantVals,
        //                                  unsigned Count);
        //LLVMValueRef LLVMGetElementAsConstant(LLVMValueRef C, unsigned idx);
        //LLVMValueRef LLVMConstVector(LLVMValueRef *ScalarConstantVals, unsigned Size);
        //LLVMOpcode LLVMGetConstOpcode(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMAlignOf(LLVMTypeRef Ty);
        //LLVMValueRef LLVMSizeOf(LLVMTypeRef Ty);
        //LLVMValueRef LLVMConstNeg(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMConstNSWNeg(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMConstNUWNeg(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMConstFNeg(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMConstNot(LLVMValueRef ConstantVal);
        //LLVMValueRef LLVMConstAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNSWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNUWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNSWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNUWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNSWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstNUWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstUDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstExactSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstURem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstSRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstAnd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstOr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstXor(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstICmp(LLVMIntPredicate Predicate,
        //                           LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstFCmp(LLVMRealPredicate Predicate,
        //                           LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstShl(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstLShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstAShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMValueRef LLVMConstGEP(LLVMValueRef ConstantVal,
        //                          LLVMValueRef *ConstantIndices, unsigned NumIndices);
        //LLVMValueRef LLVMConstInBoundsGEP(LLVMValueRef ConstantVal,
        //                                  LLVMValueRef *ConstantIndices,
        //                                  unsigned NumIndices);
        //LLVMValueRef LLVMConstTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstSExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstZExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstFPTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstFPExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstUIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstSIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstFPToUI(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstFPToSI(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstPtrToInt(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstIntToPtr(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstAddrSpaceCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstZExtOrBitCast(LLVMValueRef ConstantVal,
        //                                    LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstSExtOrBitCast(LLVMValueRef ConstantVal,
        //                                    LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstTruncOrBitCast(LLVMValueRef ConstantVal,
        //                                     LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstPointerCast(LLVMValueRef ConstantVal,
        //                                  LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstIntCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType,
        //                              LLVMBool isSigned);
        //LLVMValueRef LLVMConstFPCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMValueRef LLVMConstSelect(LLVMValueRef ConstantCondition,
        //                             LLVMValueRef ConstantIfTrue,
        //                             LLVMValueRef ConstantIfFalse);
        //LLVMValueRef LLVMConstExtractElement(LLVMValueRef VectorConstant,
        //                                     LLVMValueRef IndexConstant);
        //LLVMValueRef LLVMConstInsertElement(LLVMValueRef VectorConstant,
        //                                    LLVMValueRef ElementValueConstant,
        //                                    LLVMValueRef IndexConstant);
        //LLVMValueRef LLVMConstShuffleVector(LLVMValueRef VectorAConstant,
        //                                    LLVMValueRef VectorBConstant,
        //                                    LLVMValueRef MaskConstant);
        //LLVMValueRef LLVMConstExtractValue(LLVMValueRef AggConstant, unsigned *IdxList,
        //                                   unsigned NumIdx);
        //LLVMValueRef LLVMConstInsertValue(LLVMValueRef AggConstant,
        //                                  LLVMValueRef ElementValueConstant,
        //                                  unsigned *IdxList, unsigned NumIdx);
        //LLVMValueRef LLVMConstInlineAsm(LLVMTypeRef Ty,
        //                                const char *AsmString, const char *Constraints,
        //                                LLVMBool HasSideEffects, LLVMBool IsAlignStack);
        //LLVMValueRef LLVMBlockAddress(LLVMValueRef F, LLVMBasicBlockRef BB);
        //LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global);
        //LLVMBool LLVMIsDeclaration(LLVMValueRef Global);
        //LLVMLinkage LLVMGetLinkage(LLVMValueRef Global);
        //void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage);
        //const char *LLVMGetSection(LLVMValueRef Global);
        //void LLVMSetSection(LLVMValueRef Global, const char *Section);
        //LLVMVisibility LLVMGetVisibility(LLVMValueRef Global);
        //void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz);
        //LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global);
        //void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class);
        //LLVMBool LLVMHasUnnamedAddr(LLVMValueRef Global);
        //void LLVMSetUnnamedAddr(LLVMValueRef Global, LLVMBool HasUnnamedAddr);
        //unsigned LLVMGetAlignment(LLVMValueRef V);
        //void LLVMSetAlignment(LLVMValueRef V, unsigned Bytes);
        //LLVMValueRef LLVMAddGlobal(LLVMModuleRef M, LLVMTypeRef Ty, const char *Name);
        //LLVMValueRef LLVMAddGlobalInAddressSpace(LLVMModuleRef M, LLVMTypeRef Ty,
        //                                         const char *Name,
        //                                         unsigned AddressSpace);
        //LLVMValueRef LLVMGetNamedGlobal(LLVMModuleRef M, const char *Name);
        //LLVMValueRef LLVMGetFirstGlobal(LLVMModuleRef M);
        //LLVMValueRef LLVMGetLastGlobal(LLVMModuleRef M);
        //LLVMValueRef LLVMGetNextGlobal(LLVMValueRef GlobalVar);
        //LLVMValueRef LLVMGetPreviousGlobal(LLVMValueRef GlobalVar);
        //void LLVMDeleteGlobal(LLVMValueRef GlobalVar);
        //LLVMValueRef LLVMGetInitializer(LLVMValueRef GlobalVar);
        //void LLVMSetInitializer(LLVMValueRef GlobalVar, LLVMValueRef ConstantVal);
        //LLVMBool LLVMIsThreadLocal(LLVMValueRef GlobalVar);
        //void LLVMSetThreadLocal(LLVMValueRef GlobalVar, LLVMBool IsThreadLocal);
        //LLVMBool LLVMIsGlobalConstant(LLVMValueRef GlobalVar);
        //void LLVMSetGlobalConstant(LLVMValueRef GlobalVar, LLVMBool IsConstant);
        //LLVMThreadLocalMode LLVMGetThreadLocalMode(LLVMValueRef GlobalVar);
        //void LLVMSetThreadLocalMode(LLVMValueRef GlobalVar, LLVMThreadLocalMode Mode);
        //LLVMBool LLVMIsExternallyInitialized(LLVMValueRef GlobalVar);
        //void LLVMSetExternallyInitialized(LLVMValueRef GlobalVar, LLVMBool IsExtInit);
        //LLVMValueRef LLVMAddAlias(LLVMModuleRef M, LLVMTypeRef Ty, LLVMValueRef Aliasee,
        //                          const char *Name);
        //void LLVMDeleteFunction(LLVMValueRef Fn);
        //LLVMBool LLVMHasPersonalityFn(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetPersonalityFn(LLVMValueRef Fn);
        //void LLVMSetPersonalityFn(LLVMValueRef Fn, LLVMValueRef PersonalityFn);
        //unsigned LLVMGetIntrinsicID(LLVMValueRef Fn);
        //unsigned LLVMGetFunctionCallConv(LLVMValueRef Fn);
        //void LLVMSetFunctionCallConv(LLVMValueRef Fn, unsigned CC);
        //const char *LLVMGetGC(LLVMValueRef Fn);
        //void LLVMSetGC(LLVMValueRef Fn, const char *Name);
        //void LLVMAddFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA);
        //void LLVMAddAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                             LLVMAttributeRef A);
        //LLVMAttributeRef LLVMGetEnumAttributeAtIndex(LLVMValueRef F,
        //                                             LLVMAttributeIndex Idx,
        //                                             unsigned KindID);
        //LLVMAttributeRef LLVMGetStringAttributeAtIndex(LLVMValueRef F,
        //                                               LLVMAttributeIndex Idx,
        //                                               const char *K, unsigned KLen);
        //void LLVMRemoveEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                    unsigned KindID);
        //void LLVMRemoveStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                      const char *K, unsigned KLen);
        //void LLVMAddTargetDependentFunctionAttr(LLVMValueRef Fn, const char *A,
        //                                        const char *V);
        //LLVMAttribute LLVMGetFunctionAttr(LLVMValueRef Fn);
        //void LLVMRemoveFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA);
        //unsigned LLVMCountParams(LLVMValueRef Fn);
        //void LLVMGetParams(LLVMValueRef Fn, LLVMValueRef *Params);
        //LLVMValueRef LLVMGetParam(LLVMValueRef Fn, unsigned Index);
        //LLVMValueRef LLVMGetParamParent(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg);
        //LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg);
        //void LLVMAddAttribute(LLVMValueRef Arg, LLVMAttribute PA);
        //void LLVMRemoveAttribute(LLVMValueRef Arg, LLVMAttribute PA);
        //LLVMAttribute LLVMGetAttribute(LLVMValueRef Arg);
        //void LLVMSetParamAlignment(LLVMValueRef Arg, unsigned Align);
        //LLVMValueRef LLVMMDStringInContext(LLVMContextRef C, const char *Str,
        //                                   unsigned SLen);
        //LLVMValueRef LLVMMDString(const char *Str, unsigned SLen);
        //LLVMValueRef LLVMMDNodeInContext(LLVMContextRef C, LLVMValueRef *Vals,
        //                                 unsigned Count);
        //LLVMValueRef LLVMMDNode(LLVMValueRef *Vals, unsigned Count);
        //const char *LLVMGetMDString(LLVMValueRef V, unsigned *Length);
        //unsigned LLVMGetMDNodeNumOperands(LLVMValueRef V);
        //void LLVMGetMDNodeOperands(LLVMValueRef V, LLVMValueRef *Dest);
        //LLVMValueRef LLVMBasicBlockAsValue(LLVMBasicBlockRef BB);
        //LLVMBool LLVMValueIsBasicBlock(LLVMValueRef Val);
        //LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val);
        //const char *LLVMGetBasicBlockName(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetBasicBlockTerminator(LLVMBasicBlockRef BB);
        //unsigned LLVMCountBasicBlocks(LLVMValueRef Fn);
        //void LLVMGetBasicBlocks(LLVMValueRef Fn, LLVMBasicBlockRef *BasicBlocks);
        //LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB);
        //LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB);
        //LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C,
        //                                                LLVMValueRef Fn,
        //                                                const char *Name);
        //LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, const char *Name);
        //LLVMBasicBlockRef LLVMInsertBasicBlockInContext(LLVMContextRef C,
        //                                                LLVMBasicBlockRef BB,
        //                                                const char *Name);
        //LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB,
        //                                       const char *Name);
        //void LLVMDeleteBasicBlock(LLVMBasicBlockRef BB);
        //void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB);
        //void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB);
        //int LLVMHasMetadata(LLVMValueRef Val);
        //LLVMValueRef LLVMGetMetadata(LLVMValueRef Val, unsigned KindID);
        //void LLVMSetMetadata(LLVMValueRef Val, unsigned KindID, LLVMValueRef Node);
        //LLVMBasicBlockRef LLVMGetInstructionParent(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetNextInstruction(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetPreviousInstruction(LLVMValueRef Inst);
        //void LLVMInstructionRemoveFromParent(LLVMValueRef Inst);
        //void LLVMInstructionEraseFromParent(LLVMValueRef Inst);
        //LLVMOpcode LLVMGetInstructionOpcode(LLVMValueRef Inst);
        //LLVMIntPredicate LLVMGetICmpPredicate(LLVMValueRef Inst);
        //LLVMRealPredicate LLVMGetFCmpPredicate(LLVMValueRef Inst);
        //LLVMValueRef LLVMInstructionClone(LLVMValueRef Inst);
        //unsigned LLVMGetNumArgOperands(LLVMValueRef Instr);
        //void LLVMSetInstructionCallConv(LLVMValueRef Instr, unsigned CC);
        //unsigned LLVMGetInstructionCallConv(LLVMValueRef Instr);
        //void LLVMAddInstrAttribute(LLVMValueRef Instr, unsigned index, LLVMAttribute);
        //void LLVMRemoveInstrAttribute(LLVMValueRef Instr, unsigned index,
        //                              LLVMAttribute);
        //void LLVMSetInstrParamAlignment(LLVMValueRef Instr, unsigned index,
        //                                unsigned Align);
        //void LLVMAddCallSiteAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                              LLVMAttributeRef A);
        //LLVMAttributeRef LLVMGetCallSiteEnumAttribute(LLVMValueRef C,
        //                                              LLVMAttributeIndex Idx,
        //                                              unsigned KindID);
        //LLVMAttributeRef LLVMGetCallSiteStringAttribute(LLVMValueRef C,
        //                                                LLVMAttributeIndex Idx,
        //                                                const char *K, unsigned KLen);
        //void LLVMRemoveCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                     unsigned KindID);
        //void LLVMRemoveCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                       const char *K, unsigned KLen);
        //LLVMValueRef LLVMGetCalledValue(LLVMValueRef Instr);
        //LLVMBool LLVMIsTailCall(LLVMValueRef CallInst);
        //void LLVMSetTailCall(LLVMValueRef CallInst, LLVMBool IsTailCall);
        //LLVMBasicBlockRef LLVMGetNormalDest(LLVMValueRef InvokeInst);
        //LLVMBasicBlockRef LLVMGetUnwindDest(LLVMValueRef InvokeInst);
        //void LLVMSetNormalDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //void LLVMSetUnwindDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //unsigned LLVMGetNumSuccessors(LLVMValueRef Term);
        //LLVMBasicBlockRef LLVMGetSuccessor(LLVMValueRef Term, unsigned i);
        //void LLVMSetSuccessor(LLVMValueRef Term, unsigned i, LLVMBasicBlockRef block);
        //LLVMBool LLVMIsConditional(LLVMValueRef Branch);
        //LLVMValueRef LLVMGetCondition(LLVMValueRef Branch);
        //void LLVMSetCondition(LLVMValueRef Branch, LLVMValueRef Cond);
        //LLVMBasicBlockRef LLVMGetSwitchDefaultDest(LLVMValueRef SwitchInstr);
        //LLVMTypeRef LLVMGetAllocatedType(LLVMValueRef Alloca);
        //LLVMBool LLVMIsInBounds(LLVMValueRef GEP);
        //void LLVMSetIsInBounds(LLVMValueRef GEP, LLVMBool InBounds);
        //void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef *IncomingValues,
        //                     LLVMBasicBlockRef *IncomingBlocks, unsigned Count);
        //unsigned LLVMCountIncoming(LLVMValueRef PhiNode);
        //LLVMValueRef LLVMGetIncomingValue(LLVMValueRef PhiNode, unsigned Index);
        //LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, unsigned Index);
        //unsigned LLVMGetNumIndices(LLVMValueRef Inst);
        //const unsigned *LLVMGetIndices(LLVMValueRef Inst);
        LLVMCreateBuilderInContext(LLVMBuilderRef, LLVMContextRef),
        LLVMCreateBuilder(LLVMBuilderRef),
        //void LLVMPositionBuilder(LLVMBuilderRef Builder, LLVMBasicBlockRef Block,
        //                         LLVMValueRef Instr);
        //void LLVMPositionBuilderBefore(LLVMBuilderRef Builder, LLVMValueRef Instr);
        //void LLVMPositionBuilderAtEnd(LLVMBuilderRef Builder, LLVMBasicBlockRef Block);
        //LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef Builder);
        //void LLVMClearInsertionPosition(LLVMBuilderRef Builder);
        //void LLVMInsertIntoBuilder(LLVMBuilderRef Builder, LLVMValueRef Instr);
        //void LLVMInsertIntoBuilderWithName(LLVMBuilderRef Builder, LLVMValueRef Instr,
        //                                   const char *Name);
        LLVMDisposeBuilder(void.class, LLVMBuilderRef),
        //void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L);
        //LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder);
        //void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst);
        //LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef);
        //LLVMValueRef LLVMBuildRet(LLVMBuilderRef, LLVMValueRef V);
        //LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef, LLVMValueRef *RetVals,
        //                                   unsigned N);
        //LLVMValueRef LLVMBuildBr(LLVMBuilderRef, LLVMBasicBlockRef Dest);
        //LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Else);
        //LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef, LLVMValueRef V,
        //                             LLVMBasicBlockRef Else, unsigned NumCases);
        //LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr,
        //                                 unsigned NumDests);
        //LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef, LLVMValueRef Fn,
        //                             LLVMValueRef *Args, unsigned NumArgs,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty,
        //                                 LLVMValueRef PersFn, unsigned NumClauses,
        //                                 const char *Name);
        //LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn);
        //LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef);
        //void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal,
        //                 LLVMBasicBlockRef Dest);
        //void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest);
        //unsigned LLVMGetNumClauses(LLVMValueRef LandingPad);
        //LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, unsigned Idx);
        //void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal);
        //LLVMBool LLVMIsCleanup(LLVMValueRef LandingPad);
        //void LLVMSetCleanup(LLVMValueRef LandingPad, LLVMBool Val);
        //LLVMValueRef LLVMBuildAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildFSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildFMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                                const char *Name);
        //LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildURem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildSRem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildFRem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildShl(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildLShr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildAShr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildAnd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildOr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildXor(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op,
        //                            LLVMValueRef LHS, LLVMValueRef RHS,
        //                            const char *Name);
        //LLVMValueRef LLVMBuildNeg(LLVMBuilderRef, LLVMValueRef V, const char *Name);
        //LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef, LLVMValueRef V, const char *Name);
        //LLVMValueRef LLVMBuildNot(LLVMBuilderRef, LLVMValueRef V, const char *Name);
        //LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef, LLVMTypeRef Ty, const char *Name);
        //LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, const char *Name);
        //LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef, LLVMTypeRef Ty, const char *Name);
        //LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, const char *Name);
        //LLVMValueRef LLVMBuildFree(LLVMBuilderRef, LLVMValueRef PointerVal);
        //LLVMValueRef LLVMBuildLoad(LLVMBuilderRef, LLVMValueRef PointerVal,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildStore(LLVMBuilderRef, LLVMValueRef Val, LLVMValueRef Ptr);
        //LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                          LLVMValueRef *Indices, unsigned NumIndices,
        //                          const char *Name);
        //LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                  LLVMValueRef *Indices, unsigned NumIndices,
        //                                  const char *Name);
        //LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                unsigned Idx, const char *Name);
        //LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, const char *Str,
        //                                   const char *Name);
        //LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, const char *Str,
        //                                      const char *Name);
        //LLVMBool LLVMGetVolatile(LLVMValueRef MemoryAccessInst);
        //void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, LLVMBool IsVolatile);
        //LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst);
        //void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering);
        //LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildZExt(LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildSExt(LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                     LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                  LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef, LLVMValueRef Val, /*Signed cast!*/
        //                              LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, const char *Name);
        //LLVMValueRef LLVMBuildICmp(LLVMBuilderRef, LLVMIntPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef, LLVMRealPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildPhi(LLVMBuilderRef, LLVMTypeRef Ty, const char *Name);
        //LLVMValueRef LLVMBuildCall(LLVMBuilderRef, LLVMValueRef Fn,
        //                           LLVMValueRef *Args, unsigned NumArgs,
        //                           const char *Name);
        //LLVMValueRef LLVMBuildSelect(LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMValueRef Then, LLVMValueRef Else,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef, LLVMValueRef List, LLVMTypeRef Ty,
        //                            const char *Name);
        //LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef, LLVMValueRef VecVal,
        //                                     LLVMValueRef Index, const char *Name);
        //LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef, LLVMValueRef VecVal,
        //                                    LLVMValueRef EltVal, LLVMValueRef Index,
        //                                    const char *Name);
        //LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef, LLVMValueRef V1,
        //                                    LLVMValueRef V2, LLVMValueRef Mask,
        //                                    const char *Name);
        //LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef, LLVMValueRef AggVal,
        //                                   unsigned Index, const char *Name);
        //LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef, LLVMValueRef AggVal,
        //                                  LLVMValueRef EltVal, unsigned Index,
        //                                  const char *Name);
        //LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef, LLVMValueRef Val,
        //                             const char *Name);
        //LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef, LLVMValueRef Val,
        //                                const char *Name);
        //LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef, LLVMValueRef LHS,
        //                              LLVMValueRef RHS, const char *Name);
        //LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering ordering,
        //                            LLVMBool singleThread, const char *Name);
        //LLVMValueRef LLVMBuildAtomicRMW(LLVMBuilderRef B, LLVMAtomicRMWBinOp op,
        //                                LLVMValueRef PTR, LLVMValueRef Val,
        //                                LLVMAtomicOrdering ordering,
        //                                LLVMBool singleThread);
        //LLVMValueRef LLVMBuildAtomicCmpXchg(LLVMBuilderRef B, LLVMValueRef Ptr,
        //                                    LLVMValueRef Cmp, LLVMValueRef New,
        //                                    LLVMAtomicOrdering SuccessOrdering,
        //                                    LLVMAtomicOrdering FailureOrdering,
        //                                    LLVMBool SingleThread);
        //LLVMBool LLVMIsAtomicSingleThread(LLVMValueRef AtomicInst);
        //void LLVMSetAtomicSingleThread(LLVMValueRef AtomicInst, LLVMBool SingleThread);
        //LLVMAtomicOrdering LLVMGetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst);
        //void LLVMSetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst,
        //                                   LLVMAtomicOrdering Ordering);
        //LLVMAtomicOrdering LLVMGetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst);
        //void LLVMSetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst,
        //                                   LLVMAtomicOrdering Ordering);
        //LLVMModuleProviderRef
        //LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M);
        //void LLVMDisposeModuleProvider(LLVMModuleProviderRef M);
        //LLVMBool LLVMCreateMemoryBufferWithContentsOfFile(const char *Path,
        //                                                  LLVMMemoryBufferRef *OutMemBuf,
        //                                                  char **OutMessage);
        //LLVMBool LLVMCreateMemoryBufferWithSTDIN(LLVMMemoryBufferRef *OutMemBuf,
        //                                         char **OutMessage);
        //LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRange(const char *InputData,
        //                                                          size_t InputDataLength,
        //                                                          const char *BufferName,
        //                                                          LLVMBool RequiresNullTerminator);
        //LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRangeCopy(const char *InputData,
        //                                                              size_t InputDataLength,
        //                                                              const char *BufferName);
        //const char *LLVMGetBufferStart(LLVMMemoryBufferRef MemBuf);
        //size_t LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf);
        //void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf);
        LLVMGetGlobalPassRegistry(LLVMPassRegistryRef),
        //LLVMPassManagerRef LLVMCreatePassManager(void);
        //LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M);
        //LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP);
        //LLVMBool LLVMRunPassManager(LLVMPassManagerRef PM, LLVMModuleRef M);
        //LLVMBool LLVMInitializeFunctionPassManager(LLVMPassManagerRef FPM);
        //LLVMBool LLVMRunFunctionPassManager(LLVMPassManagerRef FPM, LLVMValueRef F);
        //LLVMBool LLVMFinalizeFunctionPassManager(LLVMPassManagerRef FPM);
        //void LLVMDisposePassManager(LLVMPassManagerRef PM);
        //LLVMBool LLVMStartMultithreaded(void);
        //void LLVMStopMultithreaded(void);
        //LLVMBool LLVMIsMultithreaded(void);
        ;

        static {
            _Utils.processSymbols(LLVM, LLVM_SCOPE, Function.values());
        }

        private final MethodType type;

        private long native_symbol;
        private MethodHandle handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(long native_symbol) {
            this.native_symbol = native_symbol;
        }

        @Override
        public void setHandle(MethodHandle handle) {
            this.handle = handle;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle);
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + native_symbol +
                    ", handle=" + handle + '}';
        }
    }

    public static LLVMContextRef LLVMContextCreate() {
        return nothrows_run(() -> new LLVMContextRef((long) Function.LLVMContextCreate.handle().invoke()));
    }

    public static LLVMContextRef LLVMGetGlobalContext() {
        return nothrows_run(() -> new LLVMContextRef((long) Function.LLVMGetGlobalContext.handle().invoke()));
    }

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        return nothrows_run(() -> new LLVMBuilderRef((long) Function.LLVMCreateBuilderInContext.handle().invoke(C.value())));
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        return nothrows_run(() -> new LLVMBuilderRef((long) Function.LLVMCreateBuilder.handle().invoke()));
    }

    public static void LLVMDisposeBuilder(LLVMBuilderRef Builder) {
        nothrows_run(() -> Function.LLVMDisposeBuilder.handle().invoke(Builder.value()));
    }

    public static LLVMPassRegistryRef LLVMGetGlobalPassRegistry() {
        return nothrows_run(() -> new LLVMPassRegistryRef((long) Function.LLVMGetGlobalPassRegistry.handle().invoke()));
    }
}

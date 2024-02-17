package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBasicBlockRef;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleProviderRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMUseRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.DOUBLE;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.INT;
import static com.v7878.llvm._Utils.LONG_LONG;
import static com.v7878.llvm._Utils.SIZE_T;
import static com.v7878.llvm._Utils.UINT64_T;
import static com.v7878.llvm._Utils.UINT8_T;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.llvm._Utils.UNSIGNED_LONG_LONG;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.llvm._Utils.const_ptr;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm._Utils.Symbol;

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
    static final Class<?> LLVMDiagnosticHandler = VOID_PTR; // void (*LLVMDiagnosticHandler)(LLVMDiagnosticInfoRef, void*);
    static final Class<?> LLVMYieldCallback = VOID_PTR; // void (*LLVMYieldCallback)(LLVMContextRef, void*);

    private enum Function implements Symbol {
        LLVMInitializeCore(void.class, LLVMPassRegistryRef),
        LLVMShutdown(void.class),
        LLVMCreateMessage(CHAR_PTR, CONST_CHAR_PTR),
        LLVMDisposeMessage(void.class, CHAR_PTR),
        LLVMContextCreate(LLVMContextRef),
        LLVMGetGlobalContext(LLVMContextRef),
        LLVMContextSetDiagnosticHandler(void.class, LLVMContextRef, LLVMDiagnosticHandler, VOID_PTR),
        LLVMContextGetDiagnosticHandler(LLVMDiagnosticHandler, LLVMContextRef),
        LLVMContextGetDiagnosticContext(VOID_PTR, LLVMContextRef),
        LLVMContextSetYieldCallback(void.class, LLVMContextRef, LLVMYieldCallback, VOID_PTR),
        LLVMContextDispose(void.class, LLVMContextRef),
        LLVMGetDiagInfoDescription(CHAR_PTR, LLVMDiagnosticInfoRef),
        LLVMGetDiagInfoSeverity(LLVMDiagnosticSeverity, LLVMDiagnosticInfoRef),
        LLVMGetMDKindIDInContext(UNSIGNED_INT, LLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetMDKindID(UNSIGNED_INT, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetEnumAttributeKindForName(UNSIGNED_INT, CONST_CHAR_PTR, SIZE_T),
        LLVMGetLastEnumAttributeKind(UNSIGNED_INT),
        LLVMCreateEnumAttribute(LLVMAttributeRef, LLVMContextRef, UNSIGNED_INT, UINT64_T),
        LLVMGetEnumAttributeKind(UNSIGNED_INT, LLVMAttributeRef),
        LLVMGetEnumAttributeValue(UINT64_T, LLVMAttributeRef),
        LLVMCreateStringAttribute(LLVMAttributeRef, LLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetStringAttributeKind(CONST_CHAR_PTR, LLVMAttributeRef, ptr(UNSIGNED_INT)),
        LLVMGetStringAttributeValue(CONST_CHAR_PTR, LLVMAttributeRef, ptr(UNSIGNED_INT)),
        LLVMIsEnumAttribute(LLVMBool, LLVMAttributeRef),
        LLVMIsStringAttribute(LLVMBool, LLVMAttributeRef),
        LLVMModuleCreateWithName(LLVMModuleRef, CONST_CHAR_PTR),
        LLVMModuleCreateWithNameInContext(LLVMModuleRef, CONST_CHAR_PTR, LLVMContextRef),
        LLVMCloneModule(LLVMModuleRef, LLVMModuleRef),
        LLVMDisposeModule(void.class, LLVMModuleRef),
        LLVMGetModuleIdentifier(CONST_CHAR_PTR, LLVMModuleRef, ptr(SIZE_T)),
        LLVMSetModuleIdentifier(void.class, LLVMModuleRef, CONST_CHAR_PTR, SIZE_T),
        LLVMGetDataLayoutStr(CONST_CHAR_PTR, LLVMModuleRef),
        LLVMGetDataLayout(CONST_CHAR_PTR, LLVMModuleRef),
        LLVMSetDataLayout(void.class, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetTarget(CONST_CHAR_PTR, LLVMModuleRef),
        LLVMSetTarget(void.class, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMDumpModule(void.class, LLVMModuleRef),
        LLVMPrintModuleToFile(LLVMBool, LLVMModuleRef, CONST_CHAR_PTR, ptr(CHAR_PTR)),
        LLVMPrintModuleToString(CHAR_PTR, LLVMModuleRef),
        LLVMSetModuleInlineAsm(void.class, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetModuleContext(LLVMContextRef, LLVMModuleRef),
        LLVMGetTypeByName(LLVMTypeRef, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetNamedMetadataNumOperands(UNSIGNED_INT, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetNamedMetadataOperands(void.class, LLVMModuleRef, CONST_CHAR_PTR, ptr(LLVMValueRef)),
        LLVMAddNamedMetadataOperand(void.class, LLVMModuleRef, CONST_CHAR_PTR, LLVMValueRef),
        LLVMAddFunction(LLVMValueRef, LLVMModuleRef, CONST_CHAR_PTR, LLVMTypeRef),
        LLVMGetNamedFunction(LLVMValueRef, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetFirstFunction(LLVMValueRef, LLVMModuleRef),
        LLVMGetLastFunction(LLVMValueRef, LLVMModuleRef),
        LLVMGetNextFunction(LLVMValueRef, LLVMValueRef),
        LLVMGetPreviousFunction(LLVMValueRef, LLVMValueRef),
        LLVMGetTypeKind(LLVMTypeKind, LLVMTypeRef),
        LLVMTypeIsSized(LLVMBool, LLVMTypeRef),
        LLVMGetTypeContext(LLVMContextRef, LLVMTypeRef),
        LLVMDumpType(void.class, LLVMTypeRef),
        LLVMPrintTypeToString(CHAR_PTR, LLVMTypeRef),
        LLVMInt1TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMInt8TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMInt16TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMInt32TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMInt64TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMInt128TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMIntTypeInContext(LLVMTypeRef, LLVMContextRef, UNSIGNED_INT),
        LLVMInt1Type(LLVMTypeRef),
        LLVMInt8Type(LLVMTypeRef),
        LLVMInt16Type(LLVMTypeRef),
        LLVMInt32Type(LLVMTypeRef),
        LLVMInt64Type(LLVMTypeRef),
        LLVMInt128Type(LLVMTypeRef),
        LLVMIntType(LLVMTypeRef, UNSIGNED_INT),
        LLVMGetIntTypeWidth(UNSIGNED_INT, LLVMTypeRef),
        LLVMHalfTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMFloatTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMDoubleTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMX86FP80TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMFP128TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMPPCFP128TypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMHalfType(LLVMTypeRef),
        LLVMFloatType(LLVMTypeRef),
        LLVMDoubleType(LLVMTypeRef),
        LLVMX86FP80Type(LLVMTypeRef),
        LLVMFP128Type(LLVMTypeRef),
        LLVMPPCFP128Type(LLVMTypeRef),
        LLVMFunctionType(LLVMTypeRef, LLVMTypeRef, ptr(LLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMIsFunctionVarArg(LLVMBool, LLVMTypeRef),
        LLVMGetReturnType(LLVMTypeRef, LLVMTypeRef),
        LLVMCountParamTypes(UNSIGNED_INT, LLVMTypeRef),
        LLVMGetParamTypes(void.class, LLVMTypeRef, ptr(LLVMTypeRef)),
        LLVMStructTypeInContext(LLVMTypeRef, LLVMContextRef, ptr(LLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMStructType(LLVMTypeRef, ptr(LLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMStructCreateNamed(LLVMTypeRef, LLVMContextRef, CONST_CHAR_PTR),
        LLVMGetStructName(CONST_CHAR_PTR, LLVMTypeRef),
        LLVMStructSetBody(void.class, LLVMTypeRef, ptr(LLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMCountStructElementTypes(UNSIGNED_INT, LLVMTypeRef),
        LLVMGetStructElementTypes(void.class, LLVMTypeRef, ptr(LLVMTypeRef)),
        LLVMStructGetTypeAtIndex(LLVMTypeRef, LLVMTypeRef, UNSIGNED_INT),
        LLVMIsPackedStruct(LLVMBool, LLVMTypeRef),
        LLVMIsOpaqueStruct(LLVMBool, LLVMTypeRef),
        LLVMGetElementType(LLVMTypeRef, LLVMTypeRef),
        LLVMArrayType(LLVMTypeRef, LLVMTypeRef, UNSIGNED_INT),
        LLVMGetArrayLength(UNSIGNED_INT, LLVMTypeRef),
        LLVMPointerType(LLVMTypeRef, LLVMTypeRef, UNSIGNED_INT),
        LLVMGetPointerAddressSpace(UNSIGNED_INT, LLVMTypeRef),
        LLVMVectorType(LLVMTypeRef, LLVMTypeRef, UNSIGNED_INT),
        LLVMGetVectorSize(UNSIGNED_INT, LLVMTypeRef),
        LLVMVoidTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMLabelTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMX86MMXTypeInContext(LLVMTypeRef, LLVMContextRef),
        LLVMVoidType(LLVMTypeRef),
        LLVMLabelType(LLVMTypeRef),
        LLVMX86MMXType(LLVMTypeRef),

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

        LLVMTypeOf(LLVMTypeRef, LLVMValueRef),
        LLVMGetValueKind(LLVMValueKind, LLVMValueRef),
        LLVMGetValueName(CONST_CHAR_PTR, LLVMValueRef),
        LLVMSetValueName(void.class, LLVMValueRef, CONST_CHAR_PTR),
        LLVMDumpValue(void.class, LLVMValueRef),
        LLVMPrintValueToString(CHAR_PTR, LLVMValueRef),
        LLVMReplaceAllUsesWith(void.class, LLVMValueRef, LLVMValueRef),
        LLVMIsConstant(LLVMBool, LLVMValueRef),
        LLVMIsUndef(LLVMBool, LLVMValueRef),

        //TODO:
        //#define LLVM_DECLARE_VALUE_CAST(name) \
        //  LLVMValueRef LLVMIsA##name(LLVMValueRef Val);
        //LLVM_FOR_EACH_VALUE_SUBCLASS(LLVM_DECLARE_VALUE_CAST)

        LLVMIsAMDNode(LLVMValueRef, LLVMValueRef),
        LLVMIsAMDString(LLVMValueRef, LLVMValueRef),
        LLVMGetFirstUse(LLVMUseRef, LLVMValueRef),
        LLVMGetNextUse(LLVMUseRef, LLVMUseRef),
        LLVMGetUser(LLVMValueRef, LLVMUseRef),
        LLVMGetUsedValue(LLVMValueRef, LLVMUseRef),
        LLVMGetOperand(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMGetOperandUse(LLVMUseRef, LLVMValueRef, UNSIGNED_INT),
        LLVMSetOperand(void.class, LLVMValueRef, UNSIGNED_INT, LLVMValueRef),
        LLVMGetNumOperands(INT, LLVMValueRef),
        LLVMConstNull(LLVMValueRef, LLVMTypeRef),
        LLVMConstAllOnes(LLVMValueRef, LLVMTypeRef),
        LLVMGetUndef(LLVMValueRef, LLVMTypeRef),
        LLVMIsNull(LLVMBool, LLVMValueRef),
        LLVMConstPointerNull(LLVMValueRef, LLVMTypeRef),
        LLVMConstInt(LLVMValueRef, LLVMTypeRef, UNSIGNED_LONG_LONG, LLVMBool),
        LLVMConstIntOfArbitraryPrecision(LLVMValueRef, LLVMTypeRef, UNSIGNED_INT, const_ptr(UINT64_T)),
        LLVMConstIntOfString(LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR, UINT8_T),
        LLVMConstIntOfStringAndSize(LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT, UINT8_T),
        LLVMConstReal(LLVMValueRef, LLVMTypeRef, DOUBLE),
        LLVMConstRealOfString(LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMConstRealOfStringAndSize(LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMConstIntGetZExtValue(UNSIGNED_LONG_LONG, LLVMValueRef),
        LLVMConstIntGetSExtValue(LONG_LONG, LLVMValueRef),
        LLVMConstRealGetDouble(DOUBLE, LLVMValueRef, ptr(LLVMBool)),
        LLVMConstStringInContext(LLVMValueRef, LLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT, LLVMBool),
        LLVMConstString(LLVMValueRef, CONST_CHAR_PTR, UNSIGNED_INT, LLVMBool),
        LLVMIsConstantString(LLVMBool, LLVMValueRef),
        LLVMGetAsString(CONST_CHAR_PTR, LLVMValueRef, ptr(SIZE_T)),
        LLVMConstStructInContext(LLVMValueRef, LLVMContextRef, ptr(LLVMValueRef), UNSIGNED_INT, LLVMBool),
        LLVMConstStruct(LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT, LLVMBool),
        LLVMConstArray(LLVMValueRef, LLVMTypeRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMConstNamedStruct(LLVMValueRef, LLVMTypeRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMGetElementAsConstant(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMConstVector(LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMGetConstOpcode(LLVMOpcode, LLVMValueRef),
        LLVMAlignOf(LLVMValueRef, LLVMTypeRef),
        LLVMSizeOf(LLVMValueRef, LLVMTypeRef),
        LLVMConstNeg(LLVMValueRef, LLVMValueRef),
        LLVMConstNSWNeg(LLVMValueRef, LLVMValueRef),
        LLVMConstNUWNeg(LLVMValueRef, LLVMValueRef),
        LLVMConstFNeg(LLVMValueRef, LLVMValueRef),
        LLVMConstNot(LLVMValueRef, LLVMValueRef),
        LLVMConstAdd(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNSWAdd(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNUWAdd(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstFAdd(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstSub(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNSWSub(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNUWSub(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstFSub(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstMul(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNSWMul(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstNUWMul(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstFMul(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstUDiv(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstSDiv(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstExactSDiv(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstFDiv(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstURem(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstSRem(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstFRem(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstAnd(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstOr(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstXor(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstICmp(LLVMValueRef, LLVMIntPredicate, LLVMValueRef, LLVMValueRef),
        LLVMConstFCmp(LLVMValueRef, LLVMRealPredicate, LLVMValueRef, LLVMValueRef),
        LLVMConstShl(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstLShr(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstAShr(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstGEP(LLVMValueRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMConstInBoundsGEP(LLVMValueRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMConstTrunc(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstSExt(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstZExt(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstFPTrunc(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstFPExt(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstUIToFP(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstSIToFP(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstFPToUI(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstFPToSI(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstPtrToInt(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstIntToPtr(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstBitCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstAddrSpaceCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstZExtOrBitCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstSExtOrBitCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstTruncOrBitCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstPointerCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstIntCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef, LLVMBool),
        LLVMConstFPCast(LLVMValueRef, LLVMValueRef, LLVMTypeRef),
        LLVMConstSelect(LLVMValueRef, LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstExtractElement(LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstInsertElement(LLVMValueRef, LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstShuffleVector(LLVMValueRef, LLVMValueRef, LLVMValueRef, LLVMValueRef),
        LLVMConstExtractValue(LLVMValueRef, LLVMValueRef, ptr(UNSIGNED_INT), UNSIGNED_INT),
        LLVMConstInsertValue(LLVMValueRef, LLVMValueRef, LLVMValueRef, ptr(UNSIGNED_INT), UNSIGNED_INT),
        LLVMConstInlineAsm(LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR, CONST_CHAR_PTR, LLVMBool, LLVMBool),
        LLVMBlockAddress(LLVMValueRef, LLVMValueRef, LLVMBasicBlockRef),
        LLVMGetGlobalParent(LLVMModuleRef, LLVMValueRef),
        LLVMIsDeclaration(LLVMBool, LLVMValueRef),
        LLVMGetLinkage(LLVMLinkage, LLVMValueRef),
        LLVMSetLinkage(void.class, LLVMValueRef, LLVMLinkage),
        LLVMGetSection(CONST_CHAR_PTR, LLVMValueRef),
        LLVMSetSection(void.class, LLVMValueRef, CONST_CHAR_PTR),
        LLVMGetVisibility(LLVMVisibility, LLVMValueRef),
        LLVMSetVisibility(void.class, LLVMValueRef, LLVMVisibility),
        LLVMGetDLLStorageClass(LLVMDLLStorageClass, LLVMValueRef),
        LLVMSetDLLStorageClass(void.class, LLVMValueRef, LLVMDLLStorageClass),
        LLVMHasUnnamedAddr(LLVMBool, LLVMValueRef),
        LLVMSetUnnamedAddr(void.class, LLVMValueRef, LLVMBool),
        LLVMGetAlignment(UNSIGNED_INT, LLVMValueRef),
        LLVMSetAlignment(void.class, LLVMValueRef, UNSIGNED_INT),
        LLVMAddGlobal(LLVMValueRef, LLVMModuleRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMAddGlobalInAddressSpace(LLVMValueRef, LLVMModuleRef, LLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetNamedGlobal(LLVMValueRef, LLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetFirstGlobal(LLVMValueRef, LLVMModuleRef),
        LLVMGetLastGlobal(LLVMValueRef, LLVMModuleRef),
        LLVMGetNextGlobal(LLVMValueRef, LLVMValueRef),
        LLVMGetPreviousGlobal(LLVMValueRef, LLVMValueRef),
        LLVMDeleteGlobal(void.class, LLVMValueRef),
        LLVMGetInitializer(LLVMValueRef, LLVMValueRef),
        LLVMSetInitializer(void.class, LLVMValueRef, LLVMValueRef),
        LLVMIsThreadLocal(LLVMBool, LLVMValueRef),
        LLVMSetThreadLocal(void.class, LLVMValueRef, LLVMBool),
        LLVMIsGlobalConstant(LLVMBool, LLVMValueRef),
        LLVMSetGlobalConstant(void.class, LLVMValueRef, LLVMBool),
        LLVMGetThreadLocalMode(LLVMThreadLocalMode, LLVMValueRef),
        LLVMSetThreadLocalMode(void.class, LLVMValueRef, LLVMThreadLocalMode),
        LLVMIsExternallyInitialized(LLVMBool, LLVMValueRef),
        LLVMSetExternallyInitialized(void.class, LLVMValueRef, LLVMBool),
        LLVMAddAlias(LLVMValueRef, LLVMModuleRef, LLVMTypeRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMDeleteFunction(void.class, LLVMValueRef),
        LLVMHasPersonalityFn(LLVMBool, LLVMValueRef),
        LLVMGetPersonalityFn(LLVMValueRef, LLVMValueRef),
        LLVMSetPersonalityFn(void.class, LLVMValueRef, LLVMValueRef),
        LLVMGetIntrinsicID(UNSIGNED_INT, LLVMValueRef),
        LLVMGetFunctionCallConv(UNSIGNED_INT, LLVMValueRef),
        LLVMSetFunctionCallConv(void.class, LLVMValueRef, UNSIGNED_INT),
        LLVMGetGC(CONST_CHAR_PTR, LLVMValueRef),
        LLVMSetGC(void.class, LLVMValueRef, CONST_CHAR_PTR),
        LLVMAddFunctionAttr(void.class, LLVMValueRef, LLVMAttribute),
        LLVMAddAttributeAtIndex(void.class, LLVMValueRef, LLVMAttributeIndex, LLVMAttributeRef),
        LLVMGetEnumAttributeAtIndex(LLVMAttributeRef, LLVMValueRef, LLVMAttributeIndex, UNSIGNED_INT),
        LLVMGetStringAttributeAtIndex(LLVMAttributeRef, LLVMValueRef, LLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMRemoveEnumAttributeAtIndex(void.class, LLVMValueRef, LLVMAttributeIndex, UNSIGNED_INT),
        LLVMRemoveStringAttributeAtIndex(void.class, LLVMValueRef, LLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMAddTargetDependentFunctionAttr(void.class, LLVMValueRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMGetFunctionAttr(LLVMAttribute, LLVMValueRef),
        LLVMRemoveFunctionAttr(void.class, LLVMValueRef, LLVMAttribute),
        LLVMCountParams(UNSIGNED_INT, LLVMValueRef),
        LLVMGetParams(void.class, LLVMValueRef, ptr(LLVMValueRef)),
        LLVMGetParam(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMGetParamParent(LLVMValueRef, LLVMValueRef),
        LLVMGetFirstParam(LLVMValueRef, LLVMValueRef),
        LLVMGetLastParam(LLVMValueRef, LLVMValueRef),
        LLVMGetNextParam(LLVMValueRef, LLVMValueRef),
        LLVMGetPreviousParam(LLVMValueRef, LLVMValueRef),
        LLVMAddAttribute(void.class, LLVMValueRef, LLVMAttribute),
        LLVMRemoveAttribute(void.class, LLVMValueRef, LLVMAttribute),
        LLVMGetAttribute(LLVMAttribute, LLVMValueRef),
        LLVMSetParamAlignment(void.class, LLVMValueRef, UNSIGNED_INT),
        LLVMMDStringInContext(LLVMValueRef, LLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMMDString(LLVMValueRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMMDNodeInContext(LLVMValueRef, LLVMContextRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMMDNode(LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMGetMDString(CONST_CHAR_PTR, LLVMValueRef, ptr(UNSIGNED_INT)),
        LLVMGetMDNodeNumOperands(UNSIGNED_INT, LLVMValueRef),
        LLVMGetMDNodeOperands(void.class, LLVMValueRef, ptr(LLVMValueRef)),
        LLVMBasicBlockAsValue(LLVMValueRef, LLVMBasicBlockRef),
        LLVMValueIsBasicBlock(LLVMBool, LLVMValueRef),
        LLVMValueAsBasicBlock(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetBasicBlockName(CONST_CHAR_PTR, LLVMBasicBlockRef),
        LLVMGetBasicBlockParent(LLVMValueRef, LLVMBasicBlockRef),
        LLVMGetBasicBlockTerminator(LLVMValueRef, LLVMBasicBlockRef),
        LLVMCountBasicBlocks(UNSIGNED_INT, LLVMValueRef),
        LLVMGetBasicBlocks(void.class, LLVMValueRef, ptr(LLVMBasicBlockRef)),
        LLVMGetFirstBasicBlock(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetLastBasicBlock(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetNextBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef),
        LLVMGetPreviousBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef),
        LLVMGetEntryBasicBlock(LLVMBasicBlockRef, LLVMValueRef),
        LLVMAppendBasicBlockInContext(LLVMBasicBlockRef, LLVMContextRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMAppendBasicBlock(LLVMBasicBlockRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMInsertBasicBlockInContext(LLVMBasicBlockRef, LLVMContextRef, LLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMInsertBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMDeleteBasicBlock(void.class, LLVMBasicBlockRef),
        LLVMRemoveBasicBlockFromParent(void.class, LLVMBasicBlockRef),
        LLVMMoveBasicBlockBefore(void.class, LLVMBasicBlockRef, LLVMBasicBlockRef),
        LLVMMoveBasicBlockAfter(void.class, LLVMBasicBlockRef, LLVMBasicBlockRef),
        LLVMGetFirstInstruction(LLVMValueRef, LLVMBasicBlockRef),
        LLVMGetLastInstruction(LLVMValueRef, LLVMBasicBlockRef),
        LLVMHasMetadata(INT, LLVMValueRef),
        LLVMGetMetadata(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMSetMetadata(void.class, LLVMValueRef, UNSIGNED_INT, LLVMValueRef),
        LLVMGetInstructionParent(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetNextInstruction(LLVMValueRef, LLVMValueRef),
        LLVMGetPreviousInstruction(LLVMValueRef, LLVMValueRef),
        LLVMInstructionRemoveFromParent(void.class, LLVMValueRef),
        LLVMInstructionEraseFromParent(void.class, LLVMValueRef),
        LLVMGetInstructionOpcode(LLVMOpcode, LLVMValueRef),
        LLVMGetICmpPredicate(LLVMIntPredicate, LLVMValueRef),
        LLVMGetFCmpPredicate(LLVMRealPredicate, LLVMValueRef),
        LLVMInstructionClone(LLVMValueRef, LLVMValueRef),
        LLVMGetNumArgOperands(UNSIGNED_INT, LLVMValueRef),
        LLVMSetInstructionCallConv(void.class, LLVMValueRef, UNSIGNED_INT),
        LLVMGetInstructionCallConv(UNSIGNED_INT, LLVMValueRef),
        LLVMAddInstrAttribute(void.class, LLVMValueRef, UNSIGNED_INT, LLVMAttribute),
        LLVMRemoveInstrAttribute(void.class, LLVMValueRef, UNSIGNED_INT, LLVMAttribute),
        LLVMSetInstrParamAlignment(void.class, LLVMValueRef, UNSIGNED_INT, UNSIGNED_INT),
        LLVMAddCallSiteAttribute(void.class, LLVMValueRef, LLVMAttributeIndex, LLVMAttributeRef),
        LLVMGetCallSiteEnumAttribute(LLVMAttributeRef, LLVMValueRef, LLVMAttributeIndex, UNSIGNED_INT),
        LLVMGetCallSiteStringAttribute(LLVMAttributeRef, LLVMValueRef, LLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMRemoveCallSiteEnumAttribute(void.class, LLVMValueRef, LLVMAttributeIndex, UNSIGNED_INT),
        LLVMRemoveCallSiteStringAttribute(void.class, LLVMValueRef, LLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetCalledValue(LLVMValueRef, LLVMValueRef),
        LLVMIsTailCall(LLVMBool, LLVMValueRef),
        LLVMSetTailCall(void.class, LLVMValueRef, LLVMBool),
        LLVMGetNormalDest(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetUnwindDest(LLVMBasicBlockRef, LLVMValueRef),
        LLVMSetNormalDest(void.class, LLVMValueRef, LLVMBasicBlockRef),
        LLVMSetUnwindDest(void.class, LLVMValueRef, LLVMBasicBlockRef),
        LLVMGetNumSuccessors(UNSIGNED_INT, LLVMValueRef),
        LLVMGetSuccessor(LLVMBasicBlockRef, LLVMValueRef, UNSIGNED_INT),
        LLVMSetSuccessor(void.class, LLVMValueRef, UNSIGNED_INT, LLVMBasicBlockRef),
        LLVMIsConditional(LLVMBool, LLVMValueRef),
        LLVMGetCondition(LLVMValueRef, LLVMValueRef),
        LLVMSetCondition(void.class, LLVMValueRef, LLVMValueRef),
        LLVMGetSwitchDefaultDest(LLVMBasicBlockRef, LLVMValueRef),
        LLVMGetAllocatedType(LLVMTypeRef, LLVMValueRef),
        LLVMIsInBounds(LLVMBool, LLVMValueRef),
        LLVMSetIsInBounds(void.class, LLVMValueRef, LLVMBool),
        LLVMAddIncoming(void.class, LLVMValueRef, ptr(LLVMValueRef), ptr(LLVMBasicBlockRef), UNSIGNED_INT),
        LLVMCountIncoming(UNSIGNED_INT, LLVMValueRef),
        LLVMGetIncomingValue(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMGetIncomingBlock(LLVMBasicBlockRef, LLVMValueRef, UNSIGNED_INT),
        LLVMGetNumIndices(UNSIGNED_INT, LLVMValueRef),
        LLVMGetIndices(const_ptr(UNSIGNED_INT), LLVMValueRef),
        LLVMCreateBuilderInContext(LLVMBuilderRef, LLVMContextRef),
        LLVMCreateBuilder(LLVMBuilderRef),
        LLVMPositionBuilder(void.class, LLVMBuilderRef, LLVMBasicBlockRef, LLVMValueRef),
        LLVMPositionBuilderBefore(void.class, LLVMBuilderRef, LLVMValueRef),
        LLVMPositionBuilderAtEnd(void.class, LLVMBuilderRef, LLVMBasicBlockRef),
        LLVMGetInsertBlock(LLVMBasicBlockRef, LLVMBuilderRef),
        LLVMClearInsertionPosition(void.class, LLVMBuilderRef),
        LLVMInsertIntoBuilder(void.class, LLVMBuilderRef, LLVMValueRef),
        LLVMInsertIntoBuilderWithName(void.class, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMDisposeBuilder(void.class, LLVMBuilderRef),
        LLVMSetCurrentDebugLocation(void.class, LLVMBuilderRef, LLVMValueRef),
        LLVMGetCurrentDebugLocation(LLVMValueRef, LLVMBuilderRef),
        LLVMSetInstDebugLocation(void.class, LLVMBuilderRef, LLVMValueRef),
        LLVMBuildRetVoid(LLVMValueRef, LLVMBuilderRef),
        LLVMBuildRet(LLVMValueRef, LLVMBuilderRef, LLVMValueRef),
        LLVMBuildAggregateRet(LLVMValueRef, LLVMBuilderRef, ptr(LLVMValueRef), UNSIGNED_INT),
        LLVMBuildBr(LLVMValueRef, LLVMBuilderRef, LLVMBasicBlockRef),
        LLVMBuildCondBr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMBasicBlockRef, LLVMBasicBlockRef),
        LLVMBuildSwitch(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMBasicBlockRef, UNSIGNED_INT),
        LLVMBuildIndirectBr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, UNSIGNED_INT),
        LLVMBuildInvoke(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT, LLVMBasicBlockRef, LLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMBuildLandingPad(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, LLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildResume(LLVMValueRef, LLVMBuilderRef, LLVMValueRef),
        LLVMBuildUnreachable(LLVMValueRef, LLVMBuilderRef),
        LLVMAddCase(void.class, LLVMValueRef, LLVMValueRef, LLVMBasicBlockRef),
        LLVMAddDestination(void.class, LLVMValueRef, LLVMBasicBlockRef),
        LLVMGetNumClauses(UNSIGNED_INT, LLVMValueRef),
        LLVMGetClause(LLVMValueRef, LLVMValueRef, UNSIGNED_INT),
        LLVMAddClause(void.class, LLVMValueRef, LLVMValueRef),
        LLVMIsCleanup(LLVMBool, LLVMValueRef),
        LLVMSetCleanup(void.class, LLVMValueRef, LLVMBool),
        LLVMBuildAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildUDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildExactSDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildURem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSRem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFRem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildShl(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildLShr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAShr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAnd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildOr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildXor(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildBinOp(LLVMValueRef, LLVMBuilderRef, LLVMOpcode, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNot(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildMalloc(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildArrayMalloc(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAlloca(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildArrayAlloca(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFree(LLVMValueRef, LLVMBuilderRef, LLVMValueRef),
        LLVMBuildLoad(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildStore(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef),
        LLVMBuildGEP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildInBoundsGEP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildStructGEP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildGlobalString(LLVMValueRef, LLVMBuilderRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMBuildGlobalStringPtr(LLVMValueRef, LLVMBuilderRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMGetVolatile(LLVMBool, LLVMValueRef),
        LLVMSetVolatile(void.class, LLVMValueRef, LLVMBool),
        LLVMGetOrdering(LLVMAtomicOrdering, LLVMValueRef),
        LLVMSetOrdering(void.class, LLVMValueRef, LLVMAtomicOrdering),
        LLVMBuildTrunc(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildZExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPToUI(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPToSI(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildUIToFP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSIToFP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPTrunc(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildPtrToInt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildIntToPtr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildAddrSpaceCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildZExtOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSExtOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildTruncOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildCast(LLVMValueRef, LLVMBuilderRef, LLVMOpcode, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildPointerCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildIntCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, /*Signed cast!*/ LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildICmp(LLVMValueRef, LLVMBuilderRef, LLVMIntPredicate, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFCmp(LLVMValueRef, LLVMBuilderRef, LLVMRealPredicate, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildPhi(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildCall(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, ptr(LLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildSelect(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildVAArg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildExtractElement(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildInsertElement(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildShuffleVector(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildExtractValue(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildInsertValue(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildIsNull(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildIsNotNull(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildPtrDiff(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFence(LLVMValueRef, LLVMBuilderRef, LLVMAtomicOrdering, LLVMBool, CONST_CHAR_PTR),
        LLVMBuildAtomicRMW(LLVMValueRef, LLVMBuilderRef, LLVMAtomicRMWBinOp, LLVMValueRef, LLVMValueRef, LLVMAtomicOrdering, LLVMBool),
        LLVMBuildAtomicCmpXchg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef, LLVMValueRef, LLVMValueRef, LLVMAtomicOrdering, LLVMAtomicOrdering, LLVMBool),
        LLVMIsAtomicSingleThread(LLVMBool, LLVMValueRef),
        LLVMSetAtomicSingleThread(void.class, LLVMValueRef, LLVMBool),
        LLVMGetCmpXchgSuccessOrdering(LLVMAtomicOrdering, LLVMValueRef),
        LLVMSetCmpXchgSuccessOrdering(void.class, LLVMValueRef, LLVMAtomicOrdering),
        LLVMGetCmpXchgFailureOrdering(LLVMAtomicOrdering, LLVMValueRef),
        LLVMSetCmpXchgFailureOrdering(void.class, LLVMValueRef, LLVMAtomicOrdering),
        LLVMCreateModuleProviderForExistingModule(LLVMModuleProviderRef, LLVMModuleRef),
        LLVMDisposeModuleProvider(void.class, LLVMModuleProviderRef),
        LLVMCreateMemoryBufferWithContentsOfFile(LLVMBool, CONST_CHAR_PTR, ptr(LLVMMemoryBufferRef), ptr(CHAR_PTR)),
        LLVMCreateMemoryBufferWithSTDIN(LLVMBool, ptr(LLVMMemoryBufferRef), ptr(CHAR_PTR)),
        LLVMCreateMemoryBufferWithMemoryRange(LLVMMemoryBufferRef, CONST_CHAR_PTR, SIZE_T, CONST_CHAR_PTR, LLVMBool),
        LLVMCreateMemoryBufferWithMemoryRangeCopy(LLVMMemoryBufferRef, CONST_CHAR_PTR, SIZE_T, CONST_CHAR_PTR),
        LLVMGetBufferStart(CONST_CHAR_PTR, LLVMMemoryBufferRef),
        LLVMGetBufferSize(SIZE_T, LLVMMemoryBufferRef),
        LLVMDisposeMemoryBuffer(void.class, LLVMMemoryBufferRef),
        LLVMGetGlobalPassRegistry(LLVMPassRegistryRef),
        LLVMCreatePassManager(LLVMPassManagerRef),
        LLVMCreateFunctionPassManagerForModule(LLVMPassManagerRef, LLVMModuleRef),
        LLVMCreateFunctionPassManager(LLVMPassManagerRef, LLVMModuleProviderRef),
        LLVMRunPassManager(LLVMBool, LLVMPassManagerRef, LLVMModuleRef),
        LLVMInitializeFunctionPassManager(LLVMBool, LLVMPassManagerRef),
        LLVMRunFunctionPassManager(LLVMBool, LLVMPassManagerRef, LLVMValueRef),
        LLVMFinalizeFunctionPassManager(LLVMBool, LLVMPassManagerRef),
        LLVMDisposePassManager(void.class, LLVMPassManagerRef),
        LLVMStartMultithreaded(LLVMBool),
        LLVMStopMultithreaded(void.class),
        LLVMIsMultithreaded(LLVMBool);

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

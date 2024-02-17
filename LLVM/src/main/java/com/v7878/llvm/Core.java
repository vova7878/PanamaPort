package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.SIZE_T;
import static com.v7878.llvm._Utils.UINT64_T;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.llvm._Utils.VOID_PTR;
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

        //LLVMTypeOf(LLVMTypeRef, LLVMValueRef Val);
        //LLVMGetValueKind(LLVMValueKind, LLVMValueRef Val);
        //LLVMGetValueName(CONST_CHAR_PTR, LLVMValueRef Val);
        //LLVMSetValueName(void, LLVMValueRef Val, CONST_CHAR_PTR Name);
        //LLVMDumpValue(void, LLVMValueRef Val);
        //LLVMPrintValueToString(CHAR_PTR, LLVMValueRef Val);
        //LLVMReplaceAllUsesWith(void, LLVMValueRef OldVal, LLVMValueRef NewVal);
        //LLVMIsConstant(LLVMBool, LLVMValueRef Val);
        //LLVMIsUndef(LLVMBool, LLVMValueRef Val);

        //TODO:
        //#define LLVM_DECLARE_VALUE_CAST(name) \
        //  LLVMValueRef LLVMIsA##name(LLVMValueRef Val);
        //LLVM_FOR_EACH_VALUE_SUBCLASS(LLVM_DECLARE_VALUE_CAST)

        //LLVMIsAMDNode(LLVMValueRef, LLVMValueRef Val);
        //LLVMIsAMDString(LLVMValueRef, LLVMValueRef Val);
        //LLVMGetFirstUse(LLVMUseRef, LLVMValueRef Val);
        //LLVMGetNextUse(LLVMUseRef, LLVMUseRef U);
        //LLVMGetUser(LLVMValueRef, LLVMUseRef U);
        //LLVMGetUsedValue(LLVMValueRef, LLVMUseRef U);
        //LLVMGetOperand(LLVMValueRef, LLVMValueRef Val, UNSIGNED_INT Index);
        //LLVMGetOperandUse(LLVMUseRef, LLVMValueRef Val, UNSIGNED_INT Index);
        //LLVMSetOperand(void, LLVMValueRef User, UNSIGNED_INT Index, LLVMValueRef Val);
        //LLVMGetNumOperands(int, LLVMValueRef Val);
        //LLVMConstNull(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMConstAllOnes(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMGetUndef(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMIsNull(LLVMBool, LLVMValueRef Val);
        //LLVMConstPointerNull(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMConstInt(LLVMValueRef, LLVMTypeRef IntTy, UNSIGNED_LONG_LONG N,
        //                          LLVMBool SignExtend);
        //LLVMConstIntOfArbitraryPrecision(LLVMValueRef, LLVMTypeRef IntTy,
        //                                              UNSIGNED_INT NumWords,
        //                                              const UINT64_T Words[]);
        //LLVMConstIntOfString(LLVMValueRef, LLVMTypeRef IntTy, CONST_CHAR_PTR Text,
        //                                  uint8_t Radix);
        //LLVMConstIntOfStringAndSize(LLVMValueRef, LLVMTypeRef IntTy, CONST_CHAR_PTR Text,
        //                                         UNSIGNED_INT SLen, uint8_t Radix);
        //LLVMConstReal(LLVMValueRef, LLVMTypeRef RealTy, double N);
        //LLVMConstRealOfString(LLVMValueRef, LLVMTypeRef RealTy, CONST_CHAR_PTR Text);
        //LLVMConstRealOfStringAndSize(LLVMValueRef, LLVMTypeRef RealTy, CONST_CHAR_PTR Text,
        //                                          UNSIGNED_INT SLen);
        //LLVMConstIntGetZExtValue(UNSIGNED_LONG_LONG, LLVMValueRef ConstantVal);
        //LLVMConstIntGetSExtValue(LONG_LONG, LLVMValueRef ConstantVal);
        //LLVMConstRealGetDouble(double, LLVMValueRef ConstantVal, LLVMBool *losesInfo);
        //LLVMConstStringInContext(LLVMValueRef, LLVMContextRef C, CONST_CHAR_PTR Str,
        //                                      UNSIGNED_INT Length, LLVMBool DontNullTerminate);
        //LLVMConstString(LLVMValueRef, CONST_CHAR_PTR Str, UNSIGNED_INT Length,
        //                             LLVMBool DontNullTerminate);
        //LLVMIsConstantString(LLVMBool, LLVMValueRef c);
        //LLVMGetAsString(CONST_CHAR_PTR, LLVMValueRef c, SIZE_T *Length);
        //LLVMConstStructInContext(LLVMValueRef, LLVMContextRef C,
        //                                      LLVMValueRef *ConstantVals,
        //                                      UNSIGNED_INT Count, LLVMBool Packed);
        //LLVMConstStruct(LLVMValueRef, LLVMValueRef *ConstantVals, UNSIGNED_INT Count,
        //                             LLVMBool Packed);
        //LLVMConstArray(LLVMValueRef, LLVMTypeRef ElementTy,
        //                            LLVMValueRef *ConstantVals, UNSIGNED_INT Length);
        //LLVMConstNamedStruct(LLVMValueRef, LLVMTypeRef StructTy,
        //                                  LLVMValueRef *ConstantVals,
        //                                  UNSIGNED_INT Count);
        //LLVMGetElementAsConstant(LLVMValueRef, LLVMValueRef C, UNSIGNED_INT idx);
        //LLVMConstVector(LLVMValueRef, LLVMValueRef *ScalarConstantVals, UNSIGNED_INT Size);
        //LLVMGetConstOpcode(LLVMOpcode, LLVMValueRef ConstantVal);
        //LLVMAlignOf(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMSizeOf(LLVMValueRef, LLVMTypeRef Ty);
        //LLVMConstNeg(LLVMValueRef, LLVMValueRef ConstantVal);
        //LLVMConstNSWNeg(LLVMValueRef, LLVMValueRef ConstantVal);
        //LLVMConstNUWNeg(LLVMValueRef, LLVMValueRef ConstantVal);
        //LLVMConstFNeg(LLVMValueRef, LLVMValueRef ConstantVal);
        //LLVMConstNot(LLVMValueRef, LLVMValueRef ConstantVal);
        //LLVMConstAdd(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNSWAdd(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNUWAdd(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFAdd(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstSub(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNSWSub(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNUWSub(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFSub(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstMul(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNSWMul(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstNUWMul(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFMul(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstUDiv(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstSDiv(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstExactSDiv(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFDiv(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstURem(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstSRem(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFRem(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstAnd(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstOr(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstXor(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstICmp(LLVMValueRef, LLVMIntPredicate Predicate,
        //                           LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstFCmp(LLVMValueRef, LLVMRealPredicate Predicate,
        //                           LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstShl(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstLShr(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstAShr(LLVMValueRef, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant);
        //LLVMConstGEP(LLVMValueRef, LLVMValueRef ConstantVal,
        //                          LLVMValueRef *ConstantIndices, UNSIGNED_INT NumIndices);
        //LLVMConstInBoundsGEP(LLVMValueRef, LLVMValueRef ConstantVal,
        //                                  LLVMValueRef *ConstantIndices,
        //                                  UNSIGNED_INT NumIndices);
        //LLVMConstTrunc(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstSExt(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstZExt(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstFPTrunc(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstFPExt(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstUIToFP(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstSIToFP(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstFPToUI(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstFPToSI(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstPtrToInt(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstIntToPtr(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstBitCast(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstAddrSpaceCast(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstZExtOrBitCast(LLVMValueRef, LLVMValueRef ConstantVal,
        //                                    LLVMTypeRef ToType);
        //LLVMConstSExtOrBitCast(LLVMValueRef, LLVMValueRef ConstantVal,
        //                                    LLVMTypeRef ToType);
        //LLVMConstTruncOrBitCast(LLVMValueRef, LLVMValueRef ConstantVal,
        //                                     LLVMTypeRef ToType);
        //LLVMConstPointerCast(LLVMValueRef, LLVMValueRef ConstantVal,
        //                                  LLVMTypeRef ToType);
        //LLVMConstIntCast(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType,
        //                              LLVMBool isSigned);
        //LLVMConstFPCast(LLVMValueRef, LLVMValueRef ConstantVal, LLVMTypeRef ToType);
        //LLVMConstSelect(LLVMValueRef, LLVMValueRef ConstantCondition,
        //                             LLVMValueRef ConstantIfTrue,
        //                             LLVMValueRef ConstantIfFalse);
        //LLVMConstExtractElement(LLVMValueRef, LLVMValueRef VectorConstant,
        //                                     LLVMValueRef IndexConstant);
        //LLVMConstInsertElement(LLVMValueRef, LLVMValueRef VectorConstant,
        //                                    LLVMValueRef ElementValueConstant,
        //                                    LLVMValueRef IndexConstant);
        //LLVMConstShuffleVector(LLVMValueRef, LLVMValueRef VectorAConstant,
        //                                    LLVMValueRef VectorBConstant,
        //                                    LLVMValueRef MaskConstant);
        //LLVMConstExtractValue(LLVMValueRef, LLVMValueRef AggConstant, UNSIGNED_INT *IdxList,
        //                                   UNSIGNED_INT NumIdx);
        //LLVMConstInsertValue(LLVMValueRef, LLVMValueRef AggConstant,
        //                                  LLVMValueRef ElementValueConstant,
        //                                  UNSIGNED_INT *IdxList, UNSIGNED_INT NumIdx);
        //LLVMConstInlineAsm(LLVMValueRef, LLVMTypeRef Ty,
        //                                CONST_CHAR_PTR AsmString, CONST_CHAR_PTR Constraints,
        //                                LLVMBool HasSideEffects, LLVMBool IsAlignStack);
        //LLVMBlockAddress(LLVMValueRef, LLVMValueRef F, LLVMBasicBlockRef BB);
        //LLVMGetGlobalParent(LLVMModuleRef, LLVMValueRef Global);
        //LLVMIsDeclaration(LLVMBool, LLVMValueRef Global);
        //LLVMGetLinkage(LLVMLinkage, LLVMValueRef Global);
        //LLVMSetLinkage(void, LLVMValueRef Global, LLVMLinkage Linkage);
        //LLVMGetSection(CONST_CHAR_PTR, LLVMValueRef Global);
        //LLVMSetSection(void, LLVMValueRef Global, CONST_CHAR_PTR Section);
        //LLVMGetVisibility(LLVMVisibility, LLVMValueRef Global);
        //LLVMSetVisibility(void, LLVMValueRef Global, LLVMVisibility Viz);
        //LLVMGetDLLStorageClass(LLVMDLLStorageClass, LLVMValueRef Global);
        //LLVMSetDLLStorageClass(void, LLVMValueRef Global, LLVMDLLStorageClass Class);
        //LLVMHasUnnamedAddr(LLVMBool, LLVMValueRef Global);
        //LLVMSetUnnamedAddr(void, LLVMValueRef Global, LLVMBool HasUnnamedAddr);
        //LLVMGetAlignment(UNSIGNED_INT, LLVMValueRef V);
        //LLVMSetAlignment(void, LLVMValueRef V, UNSIGNED_INT Bytes);
        //LLVMAddGlobal(LLVMValueRef, LLVMModuleRef M, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMAddGlobalInAddressSpace(LLVMValueRef, LLVMModuleRef M, LLVMTypeRef Ty,
        //                                         CONST_CHAR_PTR Name,
        //                                         UNSIGNED_INT AddressSpace);
        //LLVMGetNamedGlobal(LLVMValueRef, LLVMModuleRef M, CONST_CHAR_PTR Name);
        //LLVMGetFirstGlobal(LLVMValueRef, LLVMModuleRef M);
        //LLVMGetLastGlobal(LLVMValueRef, LLVMModuleRef M);
        //LLVMGetNextGlobal(LLVMValueRef, LLVMValueRef GlobalVar);
        //LLVMGetPreviousGlobal(LLVMValueRef, LLVMValueRef GlobalVar);
        //LLVMDeleteGlobal(void, LLVMValueRef GlobalVar);
        //LLVMGetInitializer(LLVMValueRef, LLVMValueRef GlobalVar);
        //LLVMSetInitializer(void, LLVMValueRef GlobalVar, LLVMValueRef ConstantVal);
        //LLVMIsThreadLocal(LLVMBool, LLVMValueRef GlobalVar);
        //LLVMSetThreadLocal(void, LLVMValueRef GlobalVar, LLVMBool IsThreadLocal);
        //LLVMIsGlobalConstant(LLVMBool, LLVMValueRef GlobalVar);
        //LLVMSetGlobalConstant(void, LLVMValueRef GlobalVar, LLVMBool IsConstant);
        //LLVMGetThreadLocalMode(LLVMThreadLocalMode, LLVMValueRef GlobalVar);
        //LLVMSetThreadLocalMode(void, LLVMValueRef GlobalVar, LLVMThreadLocalMode Mode);
        //LLVMIsExternallyInitialized(LLVMBool, LLVMValueRef GlobalVar);
        //LLVMSetExternallyInitialized(void, LLVMValueRef GlobalVar, LLVMBool IsExtInit);
        //LLVMAddAlias(LLVMValueRef, LLVMModuleRef M, LLVMTypeRef Ty, LLVMValueRef Aliasee,
        //                          CONST_CHAR_PTR Name);
        //LLVMDeleteFunction(void, LLVMValueRef Fn);
        //LLVMHasPersonalityFn(LLVMBool, LLVMValueRef Fn);
        //LLVMGetPersonalityFn(LLVMValueRef, LLVMValueRef Fn);
        //LLVMSetPersonalityFn(void, LLVMValueRef Fn, LLVMValueRef PersonalityFn);
        //LLVMGetIntrinsicID(UNSIGNED_INT, LLVMValueRef Fn);
        //LLVMGetFunctionCallConv(UNSIGNED_INT, LLVMValueRef Fn);
        //LLVMSetFunctionCallConv(void, LLVMValueRef Fn, UNSIGNED_INT CC);
        //LLVMGetGC(CONST_CHAR_PTR, LLVMValueRef Fn);
        //LLVMSetGC(void, LLVMValueRef Fn, CONST_CHAR_PTR Name);
        //LLVMAddFunctionAttr(void, LLVMValueRef Fn, LLVMAttribute PA);
        //LLVMAddAttributeAtIndex(void, LLVMValueRef F, LLVMAttributeIndex Idx,
        //                             LLVMAttributeRef A);
        //LLVMGetEnumAttributeAtIndex(LLVMAttributeRef, LLVMValueRef F,
        //                                             LLVMAttributeIndex Idx,
        //                                             UNSIGNED_INT KindID);
        //LLVMGetStringAttributeAtIndex(LLVMAttributeRef, LLVMValueRef F,
        //                                               LLVMAttributeIndex Idx,
        //                                               CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //LLVMRemoveEnumAttributeAtIndex(void, LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                    UNSIGNED_INT KindID);
        //LLVMRemoveStringAttributeAtIndex(void, LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                      CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //LLVMAddTargetDependentFunctionAttr(void, LLVMValueRef Fn, CONST_CHAR_PTR A,
        //                                        CONST_CHAR_PTR V);
        //LLVMGetFunctionAttr(LLVMAttribute, LLVMValueRef Fn);
        //LLVMRemoveFunctionAttr(void, LLVMValueRef Fn, LLVMAttribute PA);
        //LLVMCountParams(UNSIGNED_INT, LLVMValueRef Fn);
        //LLVMGetParams(void, LLVMValueRef Fn, LLVMValueRef *Params);
        //LLVMGetParam(LLVMValueRef, LLVMValueRef Fn, UNSIGNED_INT Index);
        //LLVMGetParamParent(LLVMValueRef, LLVMValueRef Inst);
        //LLVMGetFirstParam(LLVMValueRef, LLVMValueRef Fn);
        //LLVMGetLastParam(LLVMValueRef, LLVMValueRef Fn);
        //LLVMGetNextParam(LLVMValueRef, LLVMValueRef Arg);
        //LLVMGetPreviousParam(LLVMValueRef, LLVMValueRef Arg);
        //LLVMAddAttribute(void, LLVMValueRef Arg, LLVMAttribute PA);
        //LLVMRemoveAttribute(void, LLVMValueRef Arg, LLVMAttribute PA);
        //LLVMGetAttribute(LLVMAttribute, LLVMValueRef Arg);
        //LLVMSetParamAlignment(void, LLVMValueRef Arg, UNSIGNED_INT Align);
        //LLVMMDStringInContext(LLVMValueRef, LLVMContextRef C, CONST_CHAR_PTR Str,
        //                                   UNSIGNED_INT SLen);
        //LLVMMDString(LLVMValueRef, CONST_CHAR_PTR Str, UNSIGNED_INT SLen);
        //LLVMMDNodeInContext(LLVMValueRef, LLVMContextRef C, LLVMValueRef *Vals,
        //                                 UNSIGNED_INT Count);
        //LLVMMDNode(LLVMValueRef, LLVMValueRef *Vals, UNSIGNED_INT Count);
        //LLVMGetMDString(CONST_CHAR_PTR, LLVMValueRef V, UNSIGNED_INT *Length);
        //LLVMGetMDNodeNumOperands(UNSIGNED_INT, LLVMValueRef V);
        //LLVMGetMDNodeOperands(void, LLVMValueRef V, LLVMValueRef *Dest);
        //LLVMBasicBlockAsValue(LLVMValueRef, LLVMBasicBlockRef BB);
        //LLVMValueIsBasicBlock(LLVMBool, LLVMValueRef Val);
        //LLVMValueAsBasicBlock(LLVMBasicBlockRef, LLVMValueRef Val);
        //LLVMGetBasicBlockName(CONST_CHAR_PTR, LLVMBasicBlockRef BB);
        //LLVMGetBasicBlockParent(LLVMValueRef, LLVMBasicBlockRef BB);
        //LLVMGetBasicBlockTerminator(LLVMValueRef, LLVMBasicBlockRef BB);
        //LLVMCountBasicBlocks(UNSIGNED_INT, LLVMValueRef Fn);
        //LLVMGetBasicBlocks(void, LLVMValueRef Fn, LLVMBasicBlockRef *BasicBlocks);
        //LLVMGetFirstBasicBlock(LLVMBasicBlockRef, LLVMValueRef Fn);
        //LLVMGetLastBasicBlock(LLVMBasicBlockRef, LLVMValueRef Fn);
        //LLVMGetNextBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef BB);
        //LLVMGetPreviousBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef BB);
        //LLVMGetEntryBasicBlock(LLVMBasicBlockRef, LLVMValueRef Fn);
        //LLVMAppendBasicBlockInContext(LLVMBasicBlockRef, LLVMContextRef C,
        //                                                LLVMValueRef Fn,
        //                                                CONST_CHAR_PTR Name);
        //LLVMAppendBasicBlock(LLVMBasicBlockRef, LLVMValueRef Fn, CONST_CHAR_PTR Name);
        //LLVMInsertBasicBlockInContext(LLVMBasicBlockRef, LLVMContextRef C,
        //                                                LLVMBasicBlockRef BB,
        //                                                CONST_CHAR_PTR Name);
        //LLVMInsertBasicBlock(LLVMBasicBlockRef, LLVMBasicBlockRef InsertBeforeBB,
        //                                       CONST_CHAR_PTR Name);
        //LLVMDeleteBasicBlock(void, LLVMBasicBlockRef BB);
        //LLVMRemoveBasicBlockFromParent(void, LLVMBasicBlockRef BB);
        //LLVMMoveBasicBlockBefore(void, LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //LLVMMoveBasicBlockAfter(void, LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //LLVMGetFirstInstruction(LLVMValueRef, LLVMBasicBlockRef BB);
        //LLVMGetLastInstruction(LLVMValueRef, LLVMBasicBlockRef BB);
        //LLVMHasMetadata(int, LLVMValueRef Val);
        //LLVMGetMetadata(LLVMValueRef, LLVMValueRef Val, UNSIGNED_INT KindID);
        //LLVMSetMetadata(void, LLVMValueRef Val, UNSIGNED_INT KindID, LLVMValueRef Node);
        //LLVMGetInstructionParent(LLVMBasicBlockRef, LLVMValueRef Inst);
        //LLVMGetNextInstruction(LLVMValueRef, LLVMValueRef Inst);
        //LLVMGetPreviousInstruction(LLVMValueRef, LLVMValueRef Inst);
        //LLVMInstructionRemoveFromParent(void, LLVMValueRef Inst);
        //LLVMInstructionEraseFromParent(void, LLVMValueRef Inst);
        //LLVMGetInstructionOpcode(LLVMOpcode, LLVMValueRef Inst);
        //LLVMGetICmpPredicate(LLVMIntPredicate, LLVMValueRef Inst);
        //LLVMGetFCmpPredicate(LLVMRealPredicate, LLVMValueRef Inst);
        //LLVMInstructionClone(LLVMValueRef, LLVMValueRef Inst);
        //LLVMGetNumArgOperands(UNSIGNED_INT, LLVMValueRef Instr);
        //LLVMSetInstructionCallConv(void, LLVMValueRef Instr, UNSIGNED_INT CC);
        //LLVMGetInstructionCallConv(UNSIGNED_INT, LLVMValueRef Instr);
        //LLVMAddInstrAttribute(void, LLVMValueRef Instr, UNSIGNED_INT index, LLVMAttribute);
        //LLVMRemoveInstrAttribute(void, LLVMValueRef Instr, UNSIGNED_INT index,
        //                              LLVMAttribute);
        //LLVMSetInstrParamAlignment(void, LLVMValueRef Instr, UNSIGNED_INT index,
        //                                UNSIGNED_INT Align);
        //LLVMAddCallSiteAttribute(void, LLVMValueRef C, LLVMAttributeIndex Idx,
        //                              LLVMAttributeRef A);
        //LLVMGetCallSiteEnumAttribute(LLVMAttributeRef, LLVMValueRef C,
        //                                              LLVMAttributeIndex Idx,
        //                                              UNSIGNED_INT KindID);
        //LLVMGetCallSiteStringAttribute(LLVMAttributeRef, LLVMValueRef C,
        //                                                LLVMAttributeIndex Idx,
        //                                                CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //LLVMRemoveCallSiteEnumAttribute(void, LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                     UNSIGNED_INT KindID);
        //LLVMRemoveCallSiteStringAttribute(void, LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                       CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //LLVMGetCalledValue(LLVMValueRef, LLVMValueRef Instr);
        //LLVMIsTailCall(LLVMBool, LLVMValueRef CallInst);
        //LLVMSetTailCall(void, LLVMValueRef CallInst, LLVMBool IsTailCall);
        //LLVMGetNormalDest(LLVMBasicBlockRef, LLVMValueRef InvokeInst);
        //LLVMGetUnwindDest(LLVMBasicBlockRef, LLVMValueRef InvokeInst);
        //LLVMSetNormalDest(void, LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //LLVMSetUnwindDest(void, LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //LLVMGetNumSuccessors(UNSIGNED_INT, LLVMValueRef Term);
        //LLVMGetSuccessor(LLVMBasicBlockRef, LLVMValueRef Term, UNSIGNED_INT i);
        //LLVMSetSuccessor(void, LLVMValueRef Term, UNSIGNED_INT i, LLVMBasicBlockRef block);
        //LLVMIsConditional(LLVMBool, LLVMValueRef Branch);
        //LLVMGetCondition(LLVMValueRef, LLVMValueRef Branch);
        //LLVMSetCondition(void, LLVMValueRef Branch, LLVMValueRef Cond);
        //LLVMGetSwitchDefaultDest(LLVMBasicBlockRef, LLVMValueRef SwitchInstr);
        //LLVMGetAllocatedType(LLVMTypeRef, LLVMValueRef Alloca);
        //LLVMIsInBounds(LLVMBool, LLVMValueRef GEP);
        //LLVMSetIsInBounds(void, LLVMValueRef GEP, LLVMBool InBounds);
        //LLVMAddIncoming(void, LLVMValueRef PhiNode, LLVMValueRef *IncomingValues,
        //                     LLVMBasicBlockRef *IncomingBlocks, UNSIGNED_INT Count);
        //LLVMCountIncoming(UNSIGNED_INT, LLVMValueRef PhiNode);
        //LLVMGetIncomingValue(LLVMValueRef, LLVMValueRef PhiNode, UNSIGNED_INT Index);
        //LLVMGetIncomingBlock(LLVMBasicBlockRef, LLVMValueRef PhiNode, UNSIGNED_INT Index);
        //LLVMGetNumIndices(UNSIGNED_INT, LLVMValueRef Inst);
        //LLVMGetIndices(const UNSIGNED_INT*, LLVMValueRef Inst);
        LLVMCreateBuilderInContext(LLVMBuilderRef, LLVMContextRef),
        LLVMCreateBuilder(LLVMBuilderRef),
        //LLVMPositionBuilder(void, LLVMBuilderRef Builder, LLVMBasicBlockRef Block,
        //                         LLVMValueRef Instr);
        //LLVMPositionBuilderBefore(void, LLVMBuilderRef Builder, LLVMValueRef Instr);
        //LLVMPositionBuilderAtEnd(void, LLVMBuilderRef Builder, LLVMBasicBlockRef Block);
        //LLVMGetInsertBlock(LLVMBasicBlockRef, LLVMBuilderRef Builder);
        //LLVMClearInsertionPosition(void, LLVMBuilderRef Builder);
        //LLVMInsertIntoBuilder(void, LLVMBuilderRef Builder, LLVMValueRef Instr);
        //LLVMInsertIntoBuilderWithName(void, LLVMBuilderRef Builder, LLVMValueRef Instr,
        //                                   CONST_CHAR_PTR Name);
        LLVMDisposeBuilder(void.class, LLVMBuilderRef),
        //LLVMSetCurrentDebugLocation(void, LLVMBuilderRef Builder, LLVMValueRef L);
        //LLVMGetCurrentDebugLocation(LLVMValueRef, LLVMBuilderRef Builder);
        //LLVMSetInstDebugLocation(void, LLVMBuilderRef Builder, LLVMValueRef Inst);
        //LLVMBuildRetVoid(LLVMValueRef, LLVMBuilderRef);
        //LLVMBuildRet(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V);
        //LLVMBuildAggregateRet(LLVMValueRef, LLVMBuilderRef, LLVMValueRef *RetVals,
        //                                   UNSIGNED_INT N);
        //LLVMBuildBr(LLVMValueRef, LLVMBuilderRef, LLVMBasicBlockRef Dest);
        //LLVMBuildCondBr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Else);
        //LLVMBuildSwitch(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V,
        //                             LLVMBasicBlockRef Else, UNSIGNED_INT NumCases);
        //LLVMBuildIndirectBr(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Addr,
        //                                 UNSIGNED_INT NumDests);
        //LLVMBuildInvoke(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Fn,
        //                             LLVMValueRef *Args, UNSIGNED_INT NumArgs,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildLandingPad(LLVMValueRef, LLVMBuilderRef B, LLVMTypeRef Ty,
        //                                 LLVMValueRef PersFn, UNSIGNED_INT NumClauses,
        //                                 CONST_CHAR_PTR Name);
        //LLVMBuildResume(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Exn);
        //LLVMBuildUnreachable(LLVMValueRef, LLVMBuilderRef);
        //LLVMAddCase(void, LLVMValueRef Switch, LLVMValueRef OnVal,
        //                 LLVMBasicBlockRef Dest);
        //LLVMAddDestination(void, LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest);
        //LLVMGetNumClauses(UNSIGNED_INT, LLVMValueRef LandingPad);
        //LLVMGetClause(LLVMValueRef, LLVMValueRef LandingPad, UNSIGNED_INT Idx);
        //LLVMAddClause(void, LLVMValueRef LandingPad, LLVMValueRef ClauseVal);
        //LLVMIsCleanup(LLVMBool, LLVMValueRef LandingPad);
        //LLVMSetCleanup(void, LLVMValueRef LandingPad, LLVMBool Val);
        //LLVMBuildAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildNSWAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildNUWAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildFAdd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildNSWSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildNUWSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildFSub(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildNSWMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildNUWMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildFMul(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildUDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildSDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildExactSDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                                CONST_CHAR_PTR Name);
        //LLVMBuildFDiv(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildURem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildSRem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildFRem(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildShl(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildLShr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildAShr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildAnd(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildOr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildXor(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildBinOp(LLVMValueRef, LLVMBuilderRef B, LLVMOpcode Op,
        //                            LLVMValueRef LHS, LLVMValueRef RHS,
        //                            CONST_CHAR_PTR Name);
        //LLVMBuildNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMBuildNSWNeg(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef V,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildNUWNeg(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef V,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildFNeg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMBuildNot(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMBuildMalloc(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMBuildArrayMalloc(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, CONST_CHAR_PTR Name);
        //LLVMBuildAlloca(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMBuildArrayAlloca(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, CONST_CHAR_PTR Name);
        //LLVMBuildFree(LLVMValueRef, LLVMBuilderRef, LLVMValueRef PointerVal);
        //LLVMBuildLoad(LLVMValueRef, LLVMBuilderRef, LLVMValueRef PointerVal,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildStore(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val, LLVMValueRef Ptr);
        //LLVMBuildGEP(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Pointer,
        //                          LLVMValueRef *Indices, UNSIGNED_INT NumIndices,
        //                          CONST_CHAR_PTR Name);
        //LLVMBuildInBoundsGEP(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                  LLVMValueRef *Indices, UNSIGNED_INT NumIndices,
        //                                  CONST_CHAR_PTR Name);
        //LLVMBuildStructGEP(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                UNSIGNED_INT Idx, CONST_CHAR_PTR Name);
        //LLVMBuildGlobalString(LLVMValueRef, LLVMBuilderRef B, CONST_CHAR_PTR Str,
        //                                   CONST_CHAR_PTR Name);
        //LLVMBuildGlobalStringPtr(LLVMValueRef, LLVMBuilderRef B, CONST_CHAR_PTR Str,
        //                                      CONST_CHAR_PTR Name);
        //LLVMGetVolatile(LLVMBool, LLVMValueRef MemoryAccessInst);
        //LLVMSetVolatile(void, LLVMValueRef MemoryAccessInst, LLVMBool IsVolatile);
        //LLVMGetOrdering(LLVMAtomicOrdering, LLVMValueRef MemoryAccessInst);
        //LLVMSetOrdering(void, LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering);
        //LLVMBuildTrunc(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildZExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildSExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildFPToUI(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildFPToSI(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildUIToFP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildSIToFP(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildFPTrunc(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildFPExt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildPtrToInt(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildIntToPtr(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildAddrSpaceCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildZExtOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildSExtOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildTruncOrBitCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                     LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildCast(LLVMValueRef, LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildPointerCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                  LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildIntCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val, /*Signed cast!*/
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildFPCast(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMBuildICmp(LLVMValueRef, LLVMBuilderRef, LLVMIntPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildFCmp(LLVMValueRef, LLVMBuilderRef, LLVMRealPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildPhi(LLVMValueRef, LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMBuildCall(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Fn,
        //                           LLVMValueRef *Args, UNSIGNED_INT NumArgs,
        //                           CONST_CHAR_PTR Name);
        //LLVMBuildSelect(LLVMValueRef, LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMValueRef Then, LLVMValueRef Else,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildVAArg(LLVMValueRef, LLVMBuilderRef, LLVMValueRef List, LLVMTypeRef Ty,
        //                            CONST_CHAR_PTR Name);
        //LLVMBuildExtractElement(LLVMValueRef, LLVMBuilderRef, LLVMValueRef VecVal,
        //                                     LLVMValueRef Index, CONST_CHAR_PTR Name);
        //LLVMBuildInsertElement(LLVMValueRef, LLVMBuilderRef, LLVMValueRef VecVal,
        //                                    LLVMValueRef EltVal, LLVMValueRef Index,
        //                                    CONST_CHAR_PTR Name);
        //LLVMBuildShuffleVector(LLVMValueRef, LLVMBuilderRef, LLVMValueRef V1,
        //                                    LLVMValueRef V2, LLVMValueRef Mask,
        //                                    CONST_CHAR_PTR Name);
        //LLVMBuildExtractValue(LLVMValueRef, LLVMBuilderRef, LLVMValueRef AggVal,
        //                                   UNSIGNED_INT Index, CONST_CHAR_PTR Name);
        //LLVMBuildInsertValue(LLVMValueRef, LLVMBuilderRef, LLVMValueRef AggVal,
        //                                  LLVMValueRef EltVal, UNSIGNED_INT Index,
        //                                  CONST_CHAR_PTR Name);
        //LLVMBuildIsNull(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                             CONST_CHAR_PTR Name);
        //LLVMBuildIsNotNull(LLVMValueRef, LLVMBuilderRef, LLVMValueRef Val,
        //                                CONST_CHAR_PTR Name);
        //LLVMBuildPtrDiff(LLVMValueRef, LLVMBuilderRef, LLVMValueRef LHS,
        //                              LLVMValueRef RHS, CONST_CHAR_PTR Name);
        //LLVMBuildFence(LLVMValueRef, LLVMBuilderRef B, LLVMAtomicOrdering ordering,
        //                            LLVMBool singleThread, CONST_CHAR_PTR Name);
        //LLVMBuildAtomicRMW(LLVMValueRef, LLVMBuilderRef B, LLVMAtomicRMWBinOp op,
        //                                LLVMValueRef PTR, LLVMValueRef Val,
        //                                LLVMAtomicOrdering ordering,
        //                                LLVMBool singleThread);
        //LLVMBuildAtomicCmpXchg(LLVMValueRef, LLVMBuilderRef B, LLVMValueRef Ptr,
        //                                    LLVMValueRef Cmp, LLVMValueRef New,
        //                                    LLVMAtomicOrdering SuccessOrdering,
        //                                    LLVMAtomicOrdering FailureOrdering,
        //                                    LLVMBool SingleThread);
        //LLVMIsAtomicSingleThread(LLVMBool, LLVMValueRef AtomicInst);
        //LLVMSetAtomicSingleThread(void, LLVMValueRef AtomicInst, LLVMBool SingleThread);
        //LLVMGetCmpXchgSuccessOrdering(LLVMAtomicOrdering, LLVMValueRef CmpXchgInst);
        //LLVMSetCmpXchgSuccessOrdering(void, LLVMValueRef CmpXchgInst,
        //                                   LLVMAtomicOrdering Ordering);
        //LLVMGetCmpXchgFailureOrdering(LLVMAtomicOrdering, LLVMValueRef CmpXchgInst);
        //LLVMSetCmpXchgFailureOrdering(void, LLVMValueRef CmpXchgInst,
        //                                   LLVMAtomicOrdering Ordering);
        //LLVMModuleProviderRef
        //LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M);
        //LLVMDisposeModuleProvider(void, LLVMModuleProviderRef M);
        //LLVMCreateMemoryBufferWithContentsOfFile(LLVMBool, CONST_CHAR_PTR Path,
        //                                                  LLVMMemoryBufferRef *OutMemBuf,
        //                                                  CHAR_PTR *OutMessage);
        //LLVMCreateMemoryBufferWithSTDIN(LLVMBool, LLVMMemoryBufferRef *OutMemBuf,
        //                                         CHAR_PTR *OutMessage);
        //LLVMCreateMemoryBufferWithMemoryRange(LLVMMemoryBufferRef, CONST_CHAR_PTR InputData,
        //                                                          SIZE_T InputDataLength,
        //                                                          CONST_CHAR_PTR BufferName,
        //                                                          LLVMBool RequiresNullTerminator);
        //LLVMCreateMemoryBufferWithMemoryRangeCopy(LLVMMemoryBufferRef, CONST_CHAR_PTR InputData,
        //                                                              SIZE_T InputDataLength,
        //                                                              CONST_CHAR_PTR BufferName);
        //LLVMGetBufferStart(CONST_CHAR_PTR, LLVMMemoryBufferRef MemBuf);
        //LLVMGetBufferSize(SIZE_T, LLVMMemoryBufferRef MemBuf);
        //LLVMDisposeMemoryBuffer(void, LLVMMemoryBufferRef MemBuf);
        LLVMGetGlobalPassRegistry(LLVMPassRegistryRef),
        //LLVMCreatePassManager(LLVMPassManagerRef, );
        //LLVMCreateFunctionPassManagerForModule(LLVMPassManagerRef, LLVMModuleRef M);
        //LLVMCreateFunctionPassManager(LLVMPassManagerRef, LLVMModuleProviderRef MP);
        //LLVMRunPassManager(LLVMBool, LLVMPassManagerRef PM, LLVMModuleRef M);
        //LLVMInitializeFunctionPassManager(LLVMBool, LLVMPassManagerRef FPM);
        //LLVMRunFunctionPassManager(LLVMBool, LLVMPassManagerRef FPM, LLVMValueRef F);
        //LLVMFinalizeFunctionPassManager(LLVMBool, LLVMPassManagerRef FPM);
        //LLVMDisposePassManager(void, LLVMPassManagerRef PM);
        //LLVMStartMultithreaded(LLVMBool, );
        //LLVMStopMultithreaded(void, );
        //LLVMIsMultithreaded(LLVMBool, );
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

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

        //LLVMTypeRef LLVMTypeOf(LLVMValueRef Val);
        //LLVMValueKind LLVMGetValueKind(LLVMValueRef Val);
        //CONST_CHAR_PTR LLVMGetValueName(LLVMValueRef Val);
        //void LLVMSetValueName(LLVMValueRef Val, CONST_CHAR_PTR Name);
        //void LLVMDumpValue(LLVMValueRef Val);
        //CHAR_PTR LLVMPrintValueToString(LLVMValueRef Val);
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
        //LLVMValueRef LLVMGetOperand(LLVMValueRef Val, UNSIGNED_INT Index);
        //LLVMUseRef LLVMGetOperandUse(LLVMValueRef Val, UNSIGNED_INT Index);
        //void LLVMSetOperand(LLVMValueRef User, UNSIGNED_INT Index, LLVMValueRef Val);
        //int LLVMGetNumOperands(LLVMValueRef Val);
        //LLVMValueRef LLVMConstNull(LLVMTypeRef Ty);
        //LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty);
        //LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty);
        //LLVMBool LLVMIsNull(LLVMValueRef Val);
        //LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty);
        //LLVMValueRef LLVMConstInt(LLVMTypeRef IntTy, UNSIGNED_INT long long N,
        //                          LLVMBool SignExtend);
        //LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy,
        //                                              UNSIGNED_INT NumWords,
        //                                              const UINT64_T Words[]);
        //LLVMValueRef LLVMConstIntOfString(LLVMTypeRef IntTy, CONST_CHAR_PTR Text,
        //                                  uint8_t Radix);
        //LLVMValueRef LLVMConstIntOfStringAndSize(LLVMTypeRef IntTy, CONST_CHAR_PTR Text,
        //                                         UNSIGNED_INT SLen, uint8_t Radix);
        //LLVMValueRef LLVMConstReal(LLVMTypeRef RealTy, double N);
        //LLVMValueRef LLVMConstRealOfString(LLVMTypeRef RealTy, CONST_CHAR_PTR Text);
        //LLVMValueRef LLVMConstRealOfStringAndSize(LLVMTypeRef RealTy, CONST_CHAR_PTR Text,
        //                                          UNSIGNED_INT SLen);
        //UNSIGNED_INT long long LLVMConstIntGetZExtValue(LLVMValueRef ConstantVal);
        //long long LLVMConstIntGetSExtValue(LLVMValueRef ConstantVal);
        //double LLVMConstRealGetDouble(LLVMValueRef ConstantVal, LLVMBool *losesInfo);
        //LLVMValueRef LLVMConstStringInContext(LLVMContextRef C, CONST_CHAR_PTR Str,
        //                                      UNSIGNED_INT Length, LLVMBool DontNullTerminate);
        //LLVMValueRef LLVMConstString(CONST_CHAR_PTR Str, UNSIGNED_INT Length,
        //                             LLVMBool DontNullTerminate);
        //LLVMBool LLVMIsConstantString(LLVMValueRef c);
        //CONST_CHAR_PTR LLVMGetAsString(LLVMValueRef c, SIZE_T *Length);
        //LLVMValueRef LLVMConstStructInContext(LLVMContextRef C,
        //                                      LLVMValueRef *ConstantVals,
        //                                      UNSIGNED_INT Count, LLVMBool Packed);
        //LLVMValueRef LLVMConstStruct(LLVMValueRef *ConstantVals, UNSIGNED_INT Count,
        //                             LLVMBool Packed);
        //LLVMValueRef LLVMConstArray(LLVMTypeRef ElementTy,
        //                            LLVMValueRef *ConstantVals, UNSIGNED_INT Length);
        //LLVMValueRef LLVMConstNamedStruct(LLVMTypeRef StructTy,
        //                                  LLVMValueRef *ConstantVals,
        //                                  UNSIGNED_INT Count);
        //LLVMValueRef LLVMGetElementAsConstant(LLVMValueRef C, UNSIGNED_INT idx);
        //LLVMValueRef LLVMConstVector(LLVMValueRef *ScalarConstantVals, UNSIGNED_INT Size);
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
        //                          LLVMValueRef *ConstantIndices, UNSIGNED_INT NumIndices);
        //LLVMValueRef LLVMConstInBoundsGEP(LLVMValueRef ConstantVal,
        //                                  LLVMValueRef *ConstantIndices,
        //                                  UNSIGNED_INT NumIndices);
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
        //LLVMValueRef LLVMConstExtractValue(LLVMValueRef AggConstant, UNSIGNED_INT *IdxList,
        //                                   UNSIGNED_INT NumIdx);
        //LLVMValueRef LLVMConstInsertValue(LLVMValueRef AggConstant,
        //                                  LLVMValueRef ElementValueConstant,
        //                                  UNSIGNED_INT *IdxList, UNSIGNED_INT NumIdx);
        //LLVMValueRef LLVMConstInlineAsm(LLVMTypeRef Ty,
        //                                CONST_CHAR_PTR AsmString, CONST_CHAR_PTR Constraints,
        //                                LLVMBool HasSideEffects, LLVMBool IsAlignStack);
        //LLVMValueRef LLVMBlockAddress(LLVMValueRef F, LLVMBasicBlockRef BB);
        //LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global);
        //LLVMBool LLVMIsDeclaration(LLVMValueRef Global);
        //LLVMLinkage LLVMGetLinkage(LLVMValueRef Global);
        //void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage);
        //CONST_CHAR_PTR LLVMGetSection(LLVMValueRef Global);
        //void LLVMSetSection(LLVMValueRef Global, CONST_CHAR_PTR Section);
        //LLVMVisibility LLVMGetVisibility(LLVMValueRef Global);
        //void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz);
        //LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global);
        //void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class);
        //LLVMBool LLVMHasUnnamedAddr(LLVMValueRef Global);
        //void LLVMSetUnnamedAddr(LLVMValueRef Global, LLVMBool HasUnnamedAddr);
        //UNSIGNED_INT LLVMGetAlignment(LLVMValueRef V);
        //void LLVMSetAlignment(LLVMValueRef V, UNSIGNED_INT Bytes);
        //LLVMValueRef LLVMAddGlobal(LLVMModuleRef M, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMAddGlobalInAddressSpace(LLVMModuleRef M, LLVMTypeRef Ty,
        //                                         CONST_CHAR_PTR Name,
        //                                         UNSIGNED_INT AddressSpace);
        //LLVMValueRef LLVMGetNamedGlobal(LLVMModuleRef M, CONST_CHAR_PTR Name);
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
        //                          CONST_CHAR_PTR Name);
        //void LLVMDeleteFunction(LLVMValueRef Fn);
        //LLVMBool LLVMHasPersonalityFn(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetPersonalityFn(LLVMValueRef Fn);
        //void LLVMSetPersonalityFn(LLVMValueRef Fn, LLVMValueRef PersonalityFn);
        //UNSIGNED_INT LLVMGetIntrinsicID(LLVMValueRef Fn);
        //UNSIGNED_INT LLVMGetFunctionCallConv(LLVMValueRef Fn);
        //void LLVMSetFunctionCallConv(LLVMValueRef Fn, UNSIGNED_INT CC);
        //CONST_CHAR_PTR LLVMGetGC(LLVMValueRef Fn);
        //void LLVMSetGC(LLVMValueRef Fn, CONST_CHAR_PTR Name);
        //void LLVMAddFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA);
        //void LLVMAddAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                             LLVMAttributeRef A);
        //LLVMAttributeRef LLVMGetEnumAttributeAtIndex(LLVMValueRef F,
        //                                             LLVMAttributeIndex Idx,
        //                                             UNSIGNED_INT KindID);
        //LLVMAttributeRef LLVMGetStringAttributeAtIndex(LLVMValueRef F,
        //                                               LLVMAttributeIndex Idx,
        //                                               CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //void LLVMRemoveEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                    UNSIGNED_INT KindID);
        //void LLVMRemoveStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx,
        //                                      CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //void LLVMAddTargetDependentFunctionAttr(LLVMValueRef Fn, CONST_CHAR_PTR A,
        //                                        CONST_CHAR_PTR V);
        //LLVMAttribute LLVMGetFunctionAttr(LLVMValueRef Fn);
        //void LLVMRemoveFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA);
        //UNSIGNED_INT LLVMCountParams(LLVMValueRef Fn);
        //void LLVMGetParams(LLVMValueRef Fn, LLVMValueRef *Params);
        //LLVMValueRef LLVMGetParam(LLVMValueRef Fn, UNSIGNED_INT Index);
        //LLVMValueRef LLVMGetParamParent(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn);
        //LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg);
        //LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg);
        //void LLVMAddAttribute(LLVMValueRef Arg, LLVMAttribute PA);
        //void LLVMRemoveAttribute(LLVMValueRef Arg, LLVMAttribute PA);
        //LLVMAttribute LLVMGetAttribute(LLVMValueRef Arg);
        //void LLVMSetParamAlignment(LLVMValueRef Arg, UNSIGNED_INT Align);
        //LLVMValueRef LLVMMDStringInContext(LLVMContextRef C, CONST_CHAR_PTR Str,
        //                                   UNSIGNED_INT SLen);
        //LLVMValueRef LLVMMDString(CONST_CHAR_PTR Str, UNSIGNED_INT SLen);
        //LLVMValueRef LLVMMDNodeInContext(LLVMContextRef C, LLVMValueRef *Vals,
        //                                 UNSIGNED_INT Count);
        //LLVMValueRef LLVMMDNode(LLVMValueRef *Vals, UNSIGNED_INT Count);
        //CONST_CHAR_PTR LLVMGetMDString(LLVMValueRef V, UNSIGNED_INT *Length);
        //UNSIGNED_INT LLVMGetMDNodeNumOperands(LLVMValueRef V);
        //void LLVMGetMDNodeOperands(LLVMValueRef V, LLVMValueRef *Dest);
        //LLVMValueRef LLVMBasicBlockAsValue(LLVMBasicBlockRef BB);
        //LLVMBool LLVMValueIsBasicBlock(LLVMValueRef Val);
        //LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val);
        //CONST_CHAR_PTR LLVMGetBasicBlockName(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetBasicBlockTerminator(LLVMBasicBlockRef BB);
        //UNSIGNED_INT LLVMCountBasicBlocks(LLVMValueRef Fn);
        //void LLVMGetBasicBlocks(LLVMValueRef Fn, LLVMBasicBlockRef *BasicBlocks);
        //LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB);
        //LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB);
        //LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn);
        //LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C,
        //                                                LLVMValueRef Fn,
        //                                                CONST_CHAR_PTR Name);
        //LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, CONST_CHAR_PTR Name);
        //LLVMBasicBlockRef LLVMInsertBasicBlockInContext(LLVMContextRef C,
        //                                                LLVMBasicBlockRef BB,
        //                                                CONST_CHAR_PTR Name);
        //LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB,
        //                                       CONST_CHAR_PTR Name);
        //void LLVMDeleteBasicBlock(LLVMBasicBlockRef BB);
        //void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB);
        //void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos);
        //LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB);
        //LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB);
        //int LLVMHasMetadata(LLVMValueRef Val);
        //LLVMValueRef LLVMGetMetadata(LLVMValueRef Val, UNSIGNED_INT KindID);
        //void LLVMSetMetadata(LLVMValueRef Val, UNSIGNED_INT KindID, LLVMValueRef Node);
        //LLVMBasicBlockRef LLVMGetInstructionParent(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetNextInstruction(LLVMValueRef Inst);
        //LLVMValueRef LLVMGetPreviousInstruction(LLVMValueRef Inst);
        //void LLVMInstructionRemoveFromParent(LLVMValueRef Inst);
        //void LLVMInstructionEraseFromParent(LLVMValueRef Inst);
        //LLVMOpcode LLVMGetInstructionOpcode(LLVMValueRef Inst);
        //LLVMIntPredicate LLVMGetICmpPredicate(LLVMValueRef Inst);
        //LLVMRealPredicate LLVMGetFCmpPredicate(LLVMValueRef Inst);
        //LLVMValueRef LLVMInstructionClone(LLVMValueRef Inst);
        //UNSIGNED_INT LLVMGetNumArgOperands(LLVMValueRef Instr);
        //void LLVMSetInstructionCallConv(LLVMValueRef Instr, UNSIGNED_INT CC);
        //UNSIGNED_INT LLVMGetInstructionCallConv(LLVMValueRef Instr);
        //void LLVMAddInstrAttribute(LLVMValueRef Instr, UNSIGNED_INT index, LLVMAttribute);
        //void LLVMRemoveInstrAttribute(LLVMValueRef Instr, UNSIGNED_INT index,
        //                              LLVMAttribute);
        //void LLVMSetInstrParamAlignment(LLVMValueRef Instr, UNSIGNED_INT index,
        //                                UNSIGNED_INT Align);
        //void LLVMAddCallSiteAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                              LLVMAttributeRef A);
        //LLVMAttributeRef LLVMGetCallSiteEnumAttribute(LLVMValueRef C,
        //                                              LLVMAttributeIndex Idx,
        //                                              UNSIGNED_INT KindID);
        //LLVMAttributeRef LLVMGetCallSiteStringAttribute(LLVMValueRef C,
        //                                                LLVMAttributeIndex Idx,
        //                                                CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //void LLVMRemoveCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                     UNSIGNED_INT KindID);
        //void LLVMRemoveCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx,
        //                                       CONST_CHAR_PTR K, UNSIGNED_INT KLen);
        //LLVMValueRef LLVMGetCalledValue(LLVMValueRef Instr);
        //LLVMBool LLVMIsTailCall(LLVMValueRef CallInst);
        //void LLVMSetTailCall(LLVMValueRef CallInst, LLVMBool IsTailCall);
        //LLVMBasicBlockRef LLVMGetNormalDest(LLVMValueRef InvokeInst);
        //LLVMBasicBlockRef LLVMGetUnwindDest(LLVMValueRef InvokeInst);
        //void LLVMSetNormalDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //void LLVMSetUnwindDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B);
        //UNSIGNED_INT LLVMGetNumSuccessors(LLVMValueRef Term);
        //LLVMBasicBlockRef LLVMGetSuccessor(LLVMValueRef Term, UNSIGNED_INT i);
        //void LLVMSetSuccessor(LLVMValueRef Term, UNSIGNED_INT i, LLVMBasicBlockRef block);
        //LLVMBool LLVMIsConditional(LLVMValueRef Branch);
        //LLVMValueRef LLVMGetCondition(LLVMValueRef Branch);
        //void LLVMSetCondition(LLVMValueRef Branch, LLVMValueRef Cond);
        //LLVMBasicBlockRef LLVMGetSwitchDefaultDest(LLVMValueRef SwitchInstr);
        //LLVMTypeRef LLVMGetAllocatedType(LLVMValueRef Alloca);
        //LLVMBool LLVMIsInBounds(LLVMValueRef GEP);
        //void LLVMSetIsInBounds(LLVMValueRef GEP, LLVMBool InBounds);
        //void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef *IncomingValues,
        //                     LLVMBasicBlockRef *IncomingBlocks, UNSIGNED_INT Count);
        //UNSIGNED_INT LLVMCountIncoming(LLVMValueRef PhiNode);
        //LLVMValueRef LLVMGetIncomingValue(LLVMValueRef PhiNode, UNSIGNED_INT Index);
        //LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, UNSIGNED_INT Index);
        //UNSIGNED_INT LLVMGetNumIndices(LLVMValueRef Inst);
        //const UNSIGNED_INT *LLVMGetIndices(LLVMValueRef Inst);
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
        //                                   CONST_CHAR_PTR Name);
        LLVMDisposeBuilder(void.class, LLVMBuilderRef),
        //void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L);
        //LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder);
        //void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst);
        //LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef);
        //LLVMValueRef LLVMBuildRet(LLVMBuilderRef, LLVMValueRef V);
        //LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef, LLVMValueRef *RetVals,
        //                                   UNSIGNED_INT N);
        //LLVMValueRef LLVMBuildBr(LLVMBuilderRef, LLVMBasicBlockRef Dest);
        //LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Else);
        //LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef, LLVMValueRef V,
        //                             LLVMBasicBlockRef Else, UNSIGNED_INT NumCases);
        //LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr,
        //                                 UNSIGNED_INT NumDests);
        //LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef, LLVMValueRef Fn,
        //                             LLVMValueRef *Args, UNSIGNED_INT NumArgs,
        //                             LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty,
        //                                 LLVMValueRef PersFn, UNSIGNED_INT NumClauses,
        //                                 CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn);
        //LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef);
        //void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal,
        //                 LLVMBasicBlockRef Dest);
        //void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest);
        //UNSIGNED_INT LLVMGetNumClauses(LLVMValueRef LandingPad);
        //LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, UNSIGNED_INT Idx);
        //void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal);
        //LLVMBool LLVMIsCleanup(LLVMValueRef LandingPad);
        //void LLVMSetCleanup(LLVMValueRef LandingPad, LLVMBool Val);
        //LLVMValueRef LLVMBuildAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFSub(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFMul(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                                CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildURem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSRem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFRem(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildShl(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildLShr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildAShr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildAnd(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildOr(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildXor(LLVMBuilderRef, LLVMValueRef LHS, LLVMValueRef RHS,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op,
        //                            LLVMValueRef LHS, LLVMValueRef RHS,
        //                            CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNeg(LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildNot(LLVMBuilderRef, LLVMValueRef V, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef, LLVMTypeRef Ty,
        //                                  LLVMValueRef Val, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFree(LLVMBuilderRef, LLVMValueRef PointerVal);
        //LLVMValueRef LLVMBuildLoad(LLVMBuilderRef, LLVMValueRef PointerVal,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildStore(LLVMBuilderRef, LLVMValueRef Val, LLVMValueRef Ptr);
        //LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                          LLVMValueRef *Indices, UNSIGNED_INT NumIndices,
        //                          CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                  LLVMValueRef *Indices, UNSIGNED_INT NumIndices,
        //                                  CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer,
        //                                UNSIGNED_INT Idx, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, CONST_CHAR_PTR Str,
        //                                   CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, CONST_CHAR_PTR Str,
        //                                      CONST_CHAR_PTR Name);
        //LLVMBool LLVMGetVolatile(LLVMValueRef MemoryAccessInst);
        //void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, LLVMBool IsVolatile);
        //LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst);
        //void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering);
        //LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildZExt(LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSExt(LLVMBuilderRef, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef, LLVMValueRef Val,
        //                            LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef, LLVMValueRef Val,
        //                               LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                    LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                     LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val,
        //                           LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef, LLVMValueRef Val,
        //                                  LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef, LLVMValueRef Val, /*Signed cast!*/
        //                              LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef, LLVMValueRef Val,
        //                             LLVMTypeRef DestTy, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildICmp(LLVMBuilderRef, LLVMIntPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef, LLVMRealPredicate Op,
        //                           LLVMValueRef LHS, LLVMValueRef RHS,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildPhi(LLVMBuilderRef, LLVMTypeRef Ty, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildCall(LLVMBuilderRef, LLVMValueRef Fn,
        //                           LLVMValueRef *Args, UNSIGNED_INT NumArgs,
        //                           CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildSelect(LLVMBuilderRef, LLVMValueRef If,
        //                             LLVMValueRef Then, LLVMValueRef Else,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef, LLVMValueRef List, LLVMTypeRef Ty,
        //                            CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef, LLVMValueRef VecVal,
        //                                     LLVMValueRef Index, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef, LLVMValueRef VecVal,
        //                                    LLVMValueRef EltVal, LLVMValueRef Index,
        //                                    CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef, LLVMValueRef V1,
        //                                    LLVMValueRef V2, LLVMValueRef Mask,
        //                                    CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef, LLVMValueRef AggVal,
        //                                   UNSIGNED_INT Index, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef, LLVMValueRef AggVal,
        //                                  LLVMValueRef EltVal, UNSIGNED_INT Index,
        //                                  CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef, LLVMValueRef Val,
        //                             CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef, LLVMValueRef Val,
        //                                CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef, LLVMValueRef LHS,
        //                              LLVMValueRef RHS, CONST_CHAR_PTR Name);
        //LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering ordering,
        //                            LLVMBool singleThread, CONST_CHAR_PTR Name);
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
        //LLVMBool LLVMCreateMemoryBufferWithContentsOfFile(CONST_CHAR_PTR Path,
        //                                                  LLVMMemoryBufferRef *OutMemBuf,
        //                                                  CHAR_PTR *OutMessage);
        //LLVMBool LLVMCreateMemoryBufferWithSTDIN(LLVMMemoryBufferRef *OutMemBuf,
        //                                         CHAR_PTR *OutMessage);
        //LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRange(CONST_CHAR_PTR InputData,
        //                                                          SIZE_T InputDataLength,
        //                                                          CONST_CHAR_PTR BufferName,
        //                                                          LLVMBool RequiresNullTerminator);
        //LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRangeCopy(CONST_CHAR_PTR InputData,
        //                                                              SIZE_T InputDataLength,
        //                                                              CONST_CHAR_PTR BufferName);
        //CONST_CHAR_PTR LLVMGetBufferStart(LLVMMemoryBufferRef MemBuf);
        //SIZE_T LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf);
        //void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf);
        LLVMGetGlobalPassRegistry(LLVMPassRegistryRef),
        //LLVMPassManagerRef LLVMCreatePassManager();
        //LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M);
        //LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP);
        //LLVMBool LLVMRunPassManager(LLVMPassManagerRef PM, LLVMModuleRef M);
        //LLVMBool LLVMInitializeFunctionPassManager(LLVMPassManagerRef FPM);
        //LLVMBool LLVMRunFunctionPassManager(LLVMPassManagerRef FPM, LLVMValueRef F);
        //LLVMBool LLVMFinalizeFunctionPassManager(LLVMPassManagerRef FPM);
        //void LLVMDisposePassManager(LLVMPassManagerRef PM);
        //LLVMBool LLVMStartMultithreaded();
        //void LLVMStopMultithreaded();
        //LLVMBool LLVMIsMultithreaded();
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

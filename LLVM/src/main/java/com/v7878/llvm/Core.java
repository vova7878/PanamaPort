package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBasicBlockRef;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm.Types.cLLVMAttributeRef;
import static com.v7878.llvm.Types.cLLVMBasicBlockRef;
import static com.v7878.llvm.Types.cLLVMBuilderRef;
import static com.v7878.llvm.Types.cLLVMContextRef;
import static com.v7878.llvm.Types.cLLVMDiagnosticInfoRef;
import static com.v7878.llvm.Types.cLLVMMemoryBufferRef;
import static com.v7878.llvm.Types.cLLVMModuleProviderRef;
import static com.v7878.llvm.Types.cLLVMModuleRef;
import static com.v7878.llvm.Types.cLLVMPassManagerRef;
import static com.v7878.llvm.Types.cLLVMPassRegistryRef;
import static com.v7878.llvm.Types.cLLVMTypeRef;
import static com.v7878.llvm.Types.cLLVMUseRef;
import static com.v7878.llvm.Types.cLLVMValueRef;
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
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.allocPointerArray;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.llvm._Utils.arrayLength;
import static com.v7878.llvm._Utils.const_ptr;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.llvm._Utils.readPointerArray;
import static com.v7878.llvm._Utils.stringLength;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleLinker.processSymbol;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import com.v7878.llvm.Types.LLVMModuleProviderRef;
import com.v7878.llvm.Types.LLVMPassManagerRef;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Supplier;

public class Core {

    static final Class<?> cLLVMAttribute = ENUM;

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

    static final Class<?> cLLVMOpcode = ENUM;

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

        public static LLVMOpcode of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMTypeKind = ENUM;

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

        public static LLVMTypeKind of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMLinkage = ENUM;

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

        public static LLVMLinkage of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMVisibility = ENUM;

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

        public static LLVMVisibility of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMDLLStorageClass = ENUM;

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

        public static LLVMDLLStorageClass of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMCallConv = ENUM;

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

        public static LLVMCallConv of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMValueKind = ENUM;

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

        public static LLVMValueKind of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMIntPredicate = ENUM;

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

        public static LLVMIntPredicate of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMRealPredicate = ENUM;

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

        public static LLVMRealPredicate of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMLandingPadClauseTy = ENUM;

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

        public static LLVMLandingPadClauseTy of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMThreadLocalMode = ENUM;

    public enum LLVMThreadLocalMode {
        LLVMNotThreadLocal,
        LLVMGeneralDynamicTLSModel,
        LLVMLocalDynamicTLSModel,
        LLVMInitialExecTLSModel,
        LLVMLocalExecTLSModel;

        public int value() {
            return ordinal();
        }

        public static LLVMThreadLocalMode of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMAtomicOrdering = ENUM;

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

        public static LLVMAtomicOrdering of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMAtomicRMWBinOp = ENUM;

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

        public static LLVMAtomicRMWBinOp of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMDiagnosticSeverity = ENUM;

    public enum LLVMDiagnosticSeverity {
        LLVMDSError,
        LLVMDSWarning,
        LLVMDSRemark,
        LLVMDSNote;

        public int value() {
            return ordinal();
        }

        public static LLVMDiagnosticSeverity of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMAttributeIndex = UNSIGNED_INT;

    /**
     * Attribute index are either LLVMAttributeReturnIndex,
     * LLVMAttributeFunctionIndex or a parameter number from 1 to N.
     */
    public static class LLVMAttributeIndex {
        public static final int LLVMAttributeReturnIndex = 0;
        // ISO C restricts enumerator values to range of 'int'
        // (4294967295 is too large)
        // LLVMAttributeFunctionIndex = ~0U,
        public static final int LLVMAttributeFunctionIndex = -1;
    }

    //TODO
    static final Class<?> LLVMDiagnosticHandler = VOID_PTR; // void (*LLVMDiagnosticHandler)(LLVMDiagnosticInfoRef, void*);
    static final Class<?> LLVMYieldCallback = VOID_PTR; // void (*LLVMYieldCallback)(LLVMContextRef, void*);

    private enum Function {
        LLVMInitializeCore(void.class, cLLVMPassRegistryRef),
        LLVMShutdown(void.class),
        LLVMCreateMessage(CHAR_PTR, CONST_CHAR_PTR),
        LLVMDisposeMessage(void.class, CHAR_PTR),
        LLVMContextCreate(cLLVMContextRef),
        LLVMGetGlobalContext(cLLVMContextRef),
        LLVMContextSetDiagnosticHandler(void.class, cLLVMContextRef, LLVMDiagnosticHandler, VOID_PTR),
        LLVMContextGetDiagnosticHandler(LLVMDiagnosticHandler, cLLVMContextRef),
        LLVMContextGetDiagnosticContext(VOID_PTR, cLLVMContextRef),
        LLVMContextSetYieldCallback(void.class, cLLVMContextRef, LLVMYieldCallback, VOID_PTR),
        LLVMContextDispose(void.class, cLLVMContextRef),
        LLVMGetDiagInfoDescription(CHAR_PTR, cLLVMDiagnosticInfoRef),
        LLVMGetDiagInfoSeverity(cLLVMDiagnosticSeverity, cLLVMDiagnosticInfoRef),
        LLVMGetMDKindIDInContext(UNSIGNED_INT, cLLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetMDKindID(UNSIGNED_INT, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetEnumAttributeKindForName(UNSIGNED_INT, CONST_CHAR_PTR, SIZE_T),
        LLVMGetLastEnumAttributeKind(UNSIGNED_INT),
        LLVMCreateEnumAttribute(cLLVMAttributeRef, cLLVMContextRef, UNSIGNED_INT, UINT64_T),
        LLVMGetEnumAttributeKind(UNSIGNED_INT, cLLVMAttributeRef),
        LLVMGetEnumAttributeValue(UINT64_T, cLLVMAttributeRef),
        LLVMCreateStringAttribute(cLLVMAttributeRef, cLLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetStringAttributeKind(CONST_CHAR_PTR, cLLVMAttributeRef, ptr(UNSIGNED_INT)),
        LLVMGetStringAttributeValue(CONST_CHAR_PTR, cLLVMAttributeRef, ptr(UNSIGNED_INT)),
        LLVMIsEnumAttribute(LLVMBool, cLLVMAttributeRef),
        LLVMIsStringAttribute(LLVMBool, cLLVMAttributeRef),
        LLVMModuleCreateWithName(cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMModuleCreateWithNameInContext(cLLVMModuleRef, CONST_CHAR_PTR, cLLVMContextRef),
        LLVMCloneModule(cLLVMModuleRef, cLLVMModuleRef),
        LLVMDisposeModule(void.class, cLLVMModuleRef),
        LLVMGetModuleIdentifier(CONST_CHAR_PTR, cLLVMModuleRef, ptr(SIZE_T)),
        LLVMSetModuleIdentifier(void.class, cLLVMModuleRef, CONST_CHAR_PTR, SIZE_T),
        LLVMGetDataLayoutStr(CONST_CHAR_PTR, cLLVMModuleRef),
        LLVMGetDataLayout(CONST_CHAR_PTR, cLLVMModuleRef),
        LLVMSetDataLayout(void.class, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetTarget(CONST_CHAR_PTR, cLLVMModuleRef),
        LLVMSetTarget(void.class, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMDumpModule(void.class, cLLVMModuleRef),
        LLVMPrintModuleToFile(LLVMBool, cLLVMModuleRef, CONST_CHAR_PTR, ptr(CHAR_PTR)),
        LLVMPrintModuleToString(CHAR_PTR, cLLVMModuleRef),
        LLVMSetModuleInlineAsm(void.class, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetModuleContext(cLLVMContextRef, cLLVMModuleRef),
        LLVMGetTypeByName(cLLVMTypeRef, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetNamedMetadataNumOperands(UNSIGNED_INT, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetNamedMetadataOperands(void.class, cLLVMModuleRef, CONST_CHAR_PTR, ptr(cLLVMValueRef)),
        LLVMAddNamedMetadataOperand(void.class, cLLVMModuleRef, CONST_CHAR_PTR, cLLVMValueRef),
        LLVMAddFunction(cLLVMValueRef, cLLVMModuleRef, CONST_CHAR_PTR, cLLVMTypeRef),
        LLVMGetNamedFunction(cLLVMValueRef, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetFirstFunction(cLLVMValueRef, cLLVMModuleRef),
        LLVMGetLastFunction(cLLVMValueRef, cLLVMModuleRef),
        LLVMGetNextFunction(cLLVMValueRef, cLLVMValueRef),
        LLVMGetPreviousFunction(cLLVMValueRef, cLLVMValueRef),
        LLVMGetTypeKind(cLLVMTypeKind, cLLVMTypeRef),
        LLVMTypeIsSized(LLVMBool, cLLVMTypeRef),
        LLVMGetTypeContext(cLLVMContextRef, cLLVMTypeRef),
        LLVMDumpType(void.class, cLLVMTypeRef),
        LLVMPrintTypeToString(CHAR_PTR, cLLVMTypeRef),
        LLVMInt1TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMInt8TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMInt16TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMInt32TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMInt64TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMInt128TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMIntTypeInContext(cLLVMTypeRef, cLLVMContextRef, UNSIGNED_INT),
        LLVMInt1Type(cLLVMTypeRef),
        LLVMInt8Type(cLLVMTypeRef),
        LLVMInt16Type(cLLVMTypeRef),
        LLVMInt32Type(cLLVMTypeRef),
        LLVMInt64Type(cLLVMTypeRef),
        LLVMInt128Type(cLLVMTypeRef),
        LLVMIntType(cLLVMTypeRef, UNSIGNED_INT),
        LLVMGetIntTypeWidth(UNSIGNED_INT, cLLVMTypeRef),
        LLVMHalfTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMFloatTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMDoubleTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMX86FP80TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMFP128TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMPPCFP128TypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMHalfType(cLLVMTypeRef),
        LLVMFloatType(cLLVMTypeRef),
        LLVMDoubleType(cLLVMTypeRef),
        LLVMX86FP80Type(cLLVMTypeRef),
        LLVMFP128Type(cLLVMTypeRef),
        LLVMPPCFP128Type(cLLVMTypeRef),
        LLVMFunctionType(cLLVMTypeRef, cLLVMTypeRef, ptr(cLLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMIsFunctionVarArg(LLVMBool, cLLVMTypeRef),
        LLVMGetReturnType(cLLVMTypeRef, cLLVMTypeRef),
        LLVMCountParamTypes(UNSIGNED_INT, cLLVMTypeRef),
        LLVMGetParamTypes(void.class, cLLVMTypeRef, ptr(cLLVMTypeRef)),
        LLVMStructTypeInContext(cLLVMTypeRef, cLLVMContextRef, ptr(cLLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMStructType(cLLVMTypeRef, ptr(cLLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMStructCreateNamed(cLLVMTypeRef, cLLVMContextRef, CONST_CHAR_PTR),
        LLVMGetStructName(CONST_CHAR_PTR, cLLVMTypeRef),
        LLVMStructSetBody(void.class, cLLVMTypeRef, ptr(cLLVMTypeRef), UNSIGNED_INT, LLVMBool),
        LLVMCountStructElementTypes(UNSIGNED_INT, cLLVMTypeRef),
        LLVMGetStructElementTypes(void.class, cLLVMTypeRef, ptr(cLLVMTypeRef)),
        LLVMStructGetTypeAtIndex(cLLVMTypeRef, cLLVMTypeRef, UNSIGNED_INT),
        LLVMIsPackedStruct(LLVMBool, cLLVMTypeRef),
        LLVMIsOpaqueStruct(LLVMBool, cLLVMTypeRef),
        LLVMGetElementType(cLLVMTypeRef, cLLVMTypeRef),
        LLVMArrayType(cLLVMTypeRef, cLLVMTypeRef, UNSIGNED_INT),
        LLVMGetArrayLength(UNSIGNED_INT, cLLVMTypeRef),
        LLVMPointerType(cLLVMTypeRef, cLLVMTypeRef, UNSIGNED_INT),
        LLVMGetPointerAddressSpace(UNSIGNED_INT, cLLVMTypeRef),
        LLVMVectorType(cLLVMTypeRef, cLLVMTypeRef, UNSIGNED_INT),
        LLVMGetVectorSize(UNSIGNED_INT, cLLVMTypeRef),
        LLVMVoidTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMLabelTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMX86MMXTypeInContext(cLLVMTypeRef, cLLVMContextRef),
        LLVMVoidType(cLLVMTypeRef),
        LLVMLabelType(cLLVMTypeRef),
        LLVMX86MMXType(cLLVMTypeRef),

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

        LLVMTypeOf(cLLVMTypeRef, cLLVMValueRef),
        LLVMGetValueKind(cLLVMValueKind, cLLVMValueRef),
        LLVMGetValueName(CONST_CHAR_PTR, cLLVMValueRef),
        LLVMSetValueName(void.class, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMDumpValue(void.class, cLLVMValueRef),
        LLVMPrintValueToString(CHAR_PTR, cLLVMValueRef),
        LLVMReplaceAllUsesWith(void.class, cLLVMValueRef, cLLVMValueRef),
        LLVMIsConstant(LLVMBool, cLLVMValueRef),
        LLVMIsUndef(LLVMBool, cLLVMValueRef),

        //TODO:
        //#define LLVM_DECLARE_VALUE_CAST(name) \
        //  LLVMValueRef LLVMIsA##name(LLVMValueRef Val);
        //LLVM_FOR_EACH_VALUE_SUBCLASS(LLVM_DECLARE_VALUE_CAST)

        LLVMIsAMDNode(cLLVMValueRef, cLLVMValueRef),
        LLVMIsAMDString(cLLVMValueRef, cLLVMValueRef),
        LLVMGetFirstUse(cLLVMUseRef, cLLVMValueRef),
        LLVMGetNextUse(cLLVMUseRef, cLLVMUseRef),
        LLVMGetUser(cLLVMValueRef, cLLVMUseRef),
        LLVMGetUsedValue(cLLVMValueRef, cLLVMUseRef),
        LLVMGetOperand(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetOperandUse(cLLVMUseRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMSetOperand(void.class, cLLVMValueRef, UNSIGNED_INT, cLLVMValueRef),
        LLVMGetNumOperands(INT, cLLVMValueRef),
        LLVMConstNull(cLLVMValueRef, cLLVMTypeRef),
        LLVMConstAllOnes(cLLVMValueRef, cLLVMTypeRef),
        LLVMGetUndef(cLLVMValueRef, cLLVMTypeRef),
        LLVMIsNull(LLVMBool, cLLVMValueRef),
        LLVMConstPointerNull(cLLVMValueRef, cLLVMTypeRef),
        LLVMConstInt(cLLVMValueRef, cLLVMTypeRef, UNSIGNED_LONG_LONG, LLVMBool),
        LLVMConstIntOfArbitraryPrecision(cLLVMValueRef, cLLVMTypeRef, UNSIGNED_INT, const_ptr(UINT64_T)),
        LLVMConstIntOfString(cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR, UINT8_T),
        LLVMConstIntOfStringAndSize(cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT, UINT8_T),
        LLVMConstReal(cLLVMValueRef, cLLVMTypeRef, DOUBLE),
        LLVMConstRealOfString(cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMConstRealOfStringAndSize(cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMConstIntGetZExtValue(UNSIGNED_LONG_LONG, cLLVMValueRef),
        LLVMConstIntGetSExtValue(LONG_LONG, cLLVMValueRef),
        LLVMConstRealGetDouble(DOUBLE, cLLVMValueRef, ptr(LLVMBool)),
        LLVMConstStringInContext(cLLVMValueRef, cLLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT, LLVMBool),
        LLVMConstString(cLLVMValueRef, CONST_CHAR_PTR, UNSIGNED_INT, LLVMBool),
        LLVMIsConstantString(LLVMBool, cLLVMValueRef),
        LLVMGetAsString(CONST_CHAR_PTR, cLLVMValueRef, ptr(SIZE_T)),
        LLVMConstStructInContext(cLLVMValueRef, cLLVMContextRef, ptr(cLLVMValueRef), UNSIGNED_INT, LLVMBool),
        LLVMConstStruct(cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT, LLVMBool),
        LLVMConstArray(cLLVMValueRef, cLLVMTypeRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMConstNamedStruct(cLLVMValueRef, cLLVMTypeRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMGetElementAsConstant(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMConstVector(cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMGetConstOpcode(cLLVMOpcode, cLLVMValueRef),
        LLVMAlignOf(cLLVMValueRef, cLLVMTypeRef),
        LLVMSizeOf(cLLVMValueRef, cLLVMTypeRef),
        LLVMConstNeg(cLLVMValueRef, cLLVMValueRef),
        LLVMConstNSWNeg(cLLVMValueRef, cLLVMValueRef),
        LLVMConstNUWNeg(cLLVMValueRef, cLLVMValueRef),
        LLVMConstFNeg(cLLVMValueRef, cLLVMValueRef),
        LLVMConstNot(cLLVMValueRef, cLLVMValueRef),
        LLVMConstAdd(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNSWAdd(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNUWAdd(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFAdd(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstSub(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNSWSub(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNUWSub(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFSub(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstMul(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNSWMul(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstNUWMul(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFMul(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstUDiv(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstSDiv(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstExactSDiv(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFDiv(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstURem(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstSRem(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFRem(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstAnd(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstOr(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstXor(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstICmp(cLLVMValueRef, cLLVMIntPredicate, cLLVMValueRef, cLLVMValueRef),
        LLVMConstFCmp(cLLVMValueRef, cLLVMRealPredicate, cLLVMValueRef, cLLVMValueRef),
        LLVMConstShl(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstLShr(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstAShr(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstGEP(cLLVMValueRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMConstInBoundsGEP(cLLVMValueRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMConstTrunc(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstSExt(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstZExt(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstFPTrunc(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstFPExt(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstUIToFP(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstSIToFP(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstFPToUI(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstFPToSI(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstPtrToInt(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstIntToPtr(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstBitCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstAddrSpaceCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstZExtOrBitCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstSExtOrBitCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstTruncOrBitCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstPointerCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstIntCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef, LLVMBool),
        LLVMConstFPCast(cLLVMValueRef, cLLVMValueRef, cLLVMTypeRef),
        LLVMConstSelect(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstExtractElement(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstInsertElement(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstShuffleVector(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef),
        LLVMConstExtractValue(cLLVMValueRef, cLLVMValueRef, ptr(UNSIGNED_INT), UNSIGNED_INT),
        LLVMConstInsertValue(cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, ptr(UNSIGNED_INT), UNSIGNED_INT),
        LLVMConstInlineAsm(cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR, CONST_CHAR_PTR, LLVMBool, LLVMBool),
        LLVMBlockAddress(cLLVMValueRef, cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMGetGlobalParent(cLLVMModuleRef, cLLVMValueRef),
        LLVMIsDeclaration(LLVMBool, cLLVMValueRef),
        LLVMGetLinkage(cLLVMLinkage, cLLVMValueRef),
        LLVMSetLinkage(void.class, cLLVMValueRef, cLLVMLinkage),
        LLVMGetSection(CONST_CHAR_PTR, cLLVMValueRef),
        LLVMSetSection(void.class, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMGetVisibility(cLLVMVisibility, cLLVMValueRef),
        LLVMSetVisibility(void.class, cLLVMValueRef, cLLVMVisibility),
        LLVMGetDLLStorageClass(cLLVMDLLStorageClass, cLLVMValueRef),
        LLVMSetDLLStorageClass(void.class, cLLVMValueRef, cLLVMDLLStorageClass),
        LLVMHasUnnamedAddr(LLVMBool, cLLVMValueRef),
        LLVMSetUnnamedAddr(void.class, cLLVMValueRef, LLVMBool),
        LLVMGetAlignment(UNSIGNED_INT, cLLVMValueRef),
        LLVMSetAlignment(void.class, cLLVMValueRef, UNSIGNED_INT),
        LLVMAddGlobal(cLLVMValueRef, cLLVMModuleRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMAddGlobalInAddressSpace(cLLVMValueRef, cLLVMModuleRef, cLLVMTypeRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetNamedGlobal(cLLVMValueRef, cLLVMModuleRef, CONST_CHAR_PTR),
        LLVMGetFirstGlobal(cLLVMValueRef, cLLVMModuleRef),
        LLVMGetLastGlobal(cLLVMValueRef, cLLVMModuleRef),
        LLVMGetNextGlobal(cLLVMValueRef, cLLVMValueRef),
        LLVMGetPreviousGlobal(cLLVMValueRef, cLLVMValueRef),
        LLVMDeleteGlobal(void.class, cLLVMValueRef),
        LLVMGetInitializer(cLLVMValueRef, cLLVMValueRef),
        LLVMSetInitializer(void.class, cLLVMValueRef, cLLVMValueRef),
        LLVMIsThreadLocal(LLVMBool, cLLVMValueRef),
        LLVMSetThreadLocal(void.class, cLLVMValueRef, LLVMBool),
        LLVMIsGlobalConstant(LLVMBool, cLLVMValueRef),
        LLVMSetGlobalConstant(void.class, cLLVMValueRef, LLVMBool),
        LLVMGetThreadLocalMode(cLLVMThreadLocalMode, cLLVMValueRef),
        LLVMSetThreadLocalMode(void.class, cLLVMValueRef, cLLVMThreadLocalMode),
        LLVMIsExternallyInitialized(LLVMBool, cLLVMValueRef),
        LLVMSetExternallyInitialized(void.class, cLLVMValueRef, LLVMBool),
        LLVMAddAlias(cLLVMValueRef, cLLVMModuleRef, cLLVMTypeRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMDeleteFunction(void.class, cLLVMValueRef),
        LLVMHasPersonalityFn(LLVMBool, cLLVMValueRef),
        LLVMGetPersonalityFn(cLLVMValueRef, cLLVMValueRef),
        LLVMSetPersonalityFn(void.class, cLLVMValueRef, cLLVMValueRef),
        LLVMGetIntrinsicID(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetFunctionCallConv(UNSIGNED_INT, cLLVMValueRef),
        LLVMSetFunctionCallConv(void.class, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetGC(CONST_CHAR_PTR, cLLVMValueRef),
        LLVMSetGC(void.class, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMAddFunctionAttr(void.class, cLLVMValueRef, cLLVMAttribute),
        LLVMAddAttributeAtIndex(void.class, cLLVMValueRef, cLLVMAttributeIndex, cLLVMAttributeRef),
        LLVMGetEnumAttributeAtIndex(cLLVMAttributeRef, cLLVMValueRef, cLLVMAttributeIndex, UNSIGNED_INT),
        LLVMGetStringAttributeAtIndex(cLLVMAttributeRef, cLLVMValueRef, cLLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMRemoveEnumAttributeAtIndex(void.class, cLLVMValueRef, cLLVMAttributeIndex, UNSIGNED_INT),
        LLVMRemoveStringAttributeAtIndex(void.class, cLLVMValueRef, cLLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMAddTargetDependentFunctionAttr(void.class, cLLVMValueRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMGetFunctionAttr(cLLVMAttribute, cLLVMValueRef),
        LLVMRemoveFunctionAttr(void.class, cLLVMValueRef, cLLVMAttribute),
        LLVMCountParams(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetParams(void.class, cLLVMValueRef, ptr(cLLVMValueRef)),
        LLVMGetParam(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetParamParent(cLLVMValueRef, cLLVMValueRef),
        LLVMGetFirstParam(cLLVMValueRef, cLLVMValueRef),
        LLVMGetLastParam(cLLVMValueRef, cLLVMValueRef),
        LLVMGetNextParam(cLLVMValueRef, cLLVMValueRef),
        LLVMGetPreviousParam(cLLVMValueRef, cLLVMValueRef),
        LLVMAddAttribute(void.class, cLLVMValueRef, cLLVMAttribute),
        LLVMRemoveAttribute(void.class, cLLVMValueRef, cLLVMAttribute),
        LLVMGetAttribute(cLLVMAttribute, cLLVMValueRef),
        LLVMSetParamAlignment(void.class, cLLVMValueRef, UNSIGNED_INT),
        LLVMMDStringInContext(cLLVMValueRef, cLLVMContextRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMMDString(cLLVMValueRef, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMMDNodeInContext(cLLVMValueRef, cLLVMContextRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMMDNode(cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMGetMDString(CONST_CHAR_PTR, cLLVMValueRef, ptr(UNSIGNED_INT)),
        LLVMGetMDNodeNumOperands(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetMDNodeOperands(void.class, cLLVMValueRef, ptr(cLLVMValueRef)),
        LLVMBasicBlockAsValue(cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMValueIsBasicBlock(LLVMBool, cLLVMValueRef),
        LLVMValueAsBasicBlock(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetBasicBlockName(CONST_CHAR_PTR, cLLVMBasicBlockRef),
        LLVMGetBasicBlockParent(cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMGetBasicBlockTerminator(cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMCountBasicBlocks(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetBasicBlocks(void.class, cLLVMValueRef, ptr(cLLVMBasicBlockRef)),
        LLVMGetFirstBasicBlock(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetLastBasicBlock(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetNextBasicBlock(cLLVMBasicBlockRef, cLLVMBasicBlockRef),
        LLVMGetPreviousBasicBlock(cLLVMBasicBlockRef, cLLVMBasicBlockRef),
        LLVMGetEntryBasicBlock(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMAppendBasicBlockInContext(cLLVMBasicBlockRef, cLLVMContextRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMAppendBasicBlock(cLLVMBasicBlockRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMInsertBasicBlockInContext(cLLVMBasicBlockRef, cLLVMContextRef, cLLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMInsertBasicBlock(cLLVMBasicBlockRef, cLLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMDeleteBasicBlock(void.class, cLLVMBasicBlockRef),
        LLVMRemoveBasicBlockFromParent(void.class, cLLVMBasicBlockRef),
        LLVMMoveBasicBlockBefore(void.class, cLLVMBasicBlockRef, cLLVMBasicBlockRef),
        LLVMMoveBasicBlockAfter(void.class, cLLVMBasicBlockRef, cLLVMBasicBlockRef),
        LLVMGetFirstInstruction(cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMGetLastInstruction(cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMHasMetadata(INT, cLLVMValueRef),
        LLVMGetMetadata(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMSetMetadata(void.class, cLLVMValueRef, UNSIGNED_INT, cLLVMValueRef),
        LLVMGetInstructionParent(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetNextInstruction(cLLVMValueRef, cLLVMValueRef),
        LLVMGetPreviousInstruction(cLLVMValueRef, cLLVMValueRef),
        LLVMInstructionRemoveFromParent(void.class, cLLVMValueRef),
        LLVMInstructionEraseFromParent(void.class, cLLVMValueRef),
        LLVMGetInstructionOpcode(cLLVMOpcode, cLLVMValueRef),
        LLVMGetICmpPredicate(cLLVMIntPredicate, cLLVMValueRef),
        LLVMGetFCmpPredicate(cLLVMRealPredicate, cLLVMValueRef),
        LLVMInstructionClone(cLLVMValueRef, cLLVMValueRef),
        LLVMGetNumArgOperands(UNSIGNED_INT, cLLVMValueRef),
        LLVMSetInstructionCallConv(void.class, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetInstructionCallConv(UNSIGNED_INT, cLLVMValueRef),
        LLVMAddInstrAttribute(void.class, cLLVMValueRef, UNSIGNED_INT, cLLVMAttribute),
        LLVMRemoveInstrAttribute(void.class, cLLVMValueRef, UNSIGNED_INT, cLLVMAttribute),
        LLVMSetInstrParamAlignment(void.class, cLLVMValueRef, UNSIGNED_INT, UNSIGNED_INT),
        LLVMAddCallSiteAttribute(void.class, cLLVMValueRef, cLLVMAttributeIndex, cLLVMAttributeRef),
        LLVMGetCallSiteEnumAttribute(cLLVMAttributeRef, cLLVMValueRef, cLLVMAttributeIndex, UNSIGNED_INT),
        LLVMGetCallSiteStringAttribute(cLLVMAttributeRef, cLLVMValueRef, cLLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMRemoveCallSiteEnumAttribute(void.class, cLLVMValueRef, cLLVMAttributeIndex, UNSIGNED_INT),
        LLVMRemoveCallSiteStringAttribute(void.class, cLLVMValueRef, cLLVMAttributeIndex, CONST_CHAR_PTR, UNSIGNED_INT),
        LLVMGetCalledValue(cLLVMValueRef, cLLVMValueRef),
        LLVMIsTailCall(LLVMBool, cLLVMValueRef),
        LLVMSetTailCall(void.class, cLLVMValueRef, LLVMBool),
        LLVMGetNormalDest(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetUnwindDest(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMSetNormalDest(void.class, cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMSetUnwindDest(void.class, cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMGetNumSuccessors(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetSuccessor(cLLVMBasicBlockRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMSetSuccessor(void.class, cLLVMValueRef, UNSIGNED_INT, cLLVMBasicBlockRef),
        LLVMIsConditional(LLVMBool, cLLVMValueRef),
        LLVMGetCondition(cLLVMValueRef, cLLVMValueRef),
        LLVMSetCondition(void.class, cLLVMValueRef, cLLVMValueRef),
        LLVMGetSwitchDefaultDest(cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMGetAllocatedType(cLLVMTypeRef, cLLVMValueRef),
        LLVMIsInBounds(LLVMBool, cLLVMValueRef),
        LLVMSetIsInBounds(void.class, cLLVMValueRef, LLVMBool),
        LLVMAddIncoming(void.class, cLLVMValueRef, ptr(cLLVMValueRef), ptr(cLLVMBasicBlockRef), UNSIGNED_INT),
        LLVMCountIncoming(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetIncomingValue(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetIncomingBlock(cLLVMBasicBlockRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMGetNumIndices(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetIndices(const_ptr(UNSIGNED_INT), cLLVMValueRef),
        LLVMCreateBuilderInContext(cLLVMBuilderRef, cLLVMContextRef),
        LLVMCreateBuilder(cLLVMBuilderRef),
        LLVMPositionBuilder(void.class, cLLVMBuilderRef, cLLVMBasicBlockRef, cLLVMValueRef),
        LLVMPositionBuilderBefore(void.class, cLLVMBuilderRef, cLLVMValueRef),
        LLVMPositionBuilderAtEnd(void.class, cLLVMBuilderRef, cLLVMBasicBlockRef),
        LLVMGetInsertBlock(cLLVMBasicBlockRef, cLLVMBuilderRef),
        LLVMClearInsertionPosition(void.class, cLLVMBuilderRef),
        LLVMInsertIntoBuilder(void.class, cLLVMBuilderRef, cLLVMValueRef),
        LLVMInsertIntoBuilderWithName(void.class, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMDisposeBuilder(void.class, cLLVMBuilderRef),
        LLVMSetCurrentDebugLocation(void.class, cLLVMBuilderRef, cLLVMValueRef),
        LLVMGetCurrentDebugLocation(cLLVMValueRef, cLLVMBuilderRef),
        LLVMSetInstDebugLocation(void.class, cLLVMBuilderRef, cLLVMValueRef),
        LLVMBuildRetVoid(cLLVMValueRef, cLLVMBuilderRef),
        LLVMBuildRet(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef),
        LLVMBuildAggregateRet(cLLVMValueRef, cLLVMBuilderRef, ptr(cLLVMValueRef), UNSIGNED_INT),
        LLVMBuildBr(cLLVMValueRef, cLLVMBuilderRef, cLLVMBasicBlockRef),
        LLVMBuildCondBr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMBasicBlockRef, cLLVMBasicBlockRef),
        LLVMBuildSwitch(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMBasicBlockRef, UNSIGNED_INT),
        LLVMBuildIndirectBr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMBuildInvoke(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT, cLLVMBasicBlockRef, cLLVMBasicBlockRef, CONST_CHAR_PTR),
        LLVMBuildLandingPad(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, cLLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildResume(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef),
        LLVMBuildUnreachable(cLLVMValueRef, cLLVMBuilderRef),
        LLVMAddCase(void.class, cLLVMValueRef, cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMAddDestination(void.class, cLLVMValueRef, cLLVMBasicBlockRef),
        LLVMGetNumClauses(UNSIGNED_INT, cLLVMValueRef),
        LLVMGetClause(cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT),
        LLVMAddClause(void.class, cLLVMValueRef, cLLVMValueRef),
        LLVMIsCleanup(LLVMBool, cLLVMValueRef),
        LLVMSetCleanup(void.class, cLLVMValueRef, LLVMBool),
        LLVMBuildAdd(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWAdd(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWAdd(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFAdd(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSub(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWSub(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWSub(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFSub(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildMul(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWMul(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWMul(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFMul(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildUDiv(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSDiv(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildExactSDiv(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFDiv(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildURem(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildSRem(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFRem(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildShl(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildLShr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAShr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAnd(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildOr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildXor(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildBinOp(cLLVMValueRef, cLLVMBuilderRef, cLLVMOpcode, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNeg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNSWNeg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNUWNeg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFNeg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildNot(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildMalloc(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildArrayMalloc(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildAlloca(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildArrayAlloca(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFree(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef),
        LLVMBuildLoad(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildStore(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef),
        LLVMBuildGEP(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildInBoundsGEP(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildStructGEP(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildGlobalString(cLLVMValueRef, cLLVMBuilderRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMBuildGlobalStringPtr(cLLVMValueRef, cLLVMBuilderRef, CONST_CHAR_PTR, CONST_CHAR_PTR),
        LLVMGetVolatile(LLVMBool, cLLVMValueRef),
        LLVMSetVolatile(void.class, cLLVMValueRef, LLVMBool),
        LLVMGetOrdering(cLLVMAtomicOrdering, cLLVMValueRef),
        LLVMSetOrdering(void.class, cLLVMValueRef, cLLVMAtomicOrdering),
        LLVMBuildTrunc(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildZExt(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSExt(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPToUI(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPToSI(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildUIToFP(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSIToFP(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPTrunc(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPExt(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildPtrToInt(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildIntToPtr(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildBitCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildAddrSpaceCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildZExtOrBitCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildSExtOrBitCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildTruncOrBitCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMOpcode, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildPointerCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildIntCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, /*Signed cast!*/ cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildFPCast(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildICmp(cLLVMValueRef, cLLVMBuilderRef, cLLVMIntPredicate, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFCmp(cLLVMValueRef, cLLVMBuilderRef, cLLVMRealPredicate, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildPhi(cLLVMValueRef, cLLVMBuilderRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildCall(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, ptr(cLLVMValueRef), UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildSelect(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildVAArg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMTypeRef, CONST_CHAR_PTR),
        LLVMBuildExtractElement(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildInsertElement(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildShuffleVector(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildExtractValue(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildInsertValue(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, UNSIGNED_INT, CONST_CHAR_PTR),
        LLVMBuildIsNull(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildIsNotNull(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildPtrDiff(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, CONST_CHAR_PTR),
        LLVMBuildFence(cLLVMValueRef, cLLVMBuilderRef, cLLVMAtomicOrdering, LLVMBool, CONST_CHAR_PTR),
        LLVMBuildAtomicRMW(cLLVMValueRef, cLLVMBuilderRef, cLLVMAtomicRMWBinOp, cLLVMValueRef, cLLVMValueRef, cLLVMAtomicOrdering, LLVMBool),
        LLVMBuildAtomicCmpXchg(cLLVMValueRef, cLLVMBuilderRef, cLLVMValueRef, cLLVMValueRef, cLLVMValueRef, cLLVMAtomicOrdering, cLLVMAtomicOrdering, LLVMBool),
        LLVMIsAtomicSingleThread(LLVMBool, cLLVMValueRef),
        LLVMSetAtomicSingleThread(void.class, cLLVMValueRef, LLVMBool),
        LLVMGetCmpXchgSuccessOrdering(cLLVMAtomicOrdering, cLLVMValueRef),
        LLVMSetCmpXchgSuccessOrdering(void.class, cLLVMValueRef, cLLVMAtomicOrdering),
        LLVMGetCmpXchgFailureOrdering(cLLVMAtomicOrdering, cLLVMValueRef),
        LLVMSetCmpXchgFailureOrdering(void.class, cLLVMValueRef, cLLVMAtomicOrdering),
        LLVMCreateModuleProviderForExistingModule(cLLVMModuleProviderRef, cLLVMModuleRef),
        LLVMDisposeModuleProvider(void.class, cLLVMModuleProviderRef),
        LLVMCreateMemoryBufferWithContentsOfFile(LLVMBool, CONST_CHAR_PTR, ptr(cLLVMMemoryBufferRef), ptr(CHAR_PTR)),
        LLVMCreateMemoryBufferWithSTDIN(LLVMBool, ptr(cLLVMMemoryBufferRef), ptr(CHAR_PTR)),
        LLVMCreateMemoryBufferWithMemoryRange(cLLVMMemoryBufferRef, CONST_CHAR_PTR, SIZE_T, CONST_CHAR_PTR, LLVMBool),
        LLVMCreateMemoryBufferWithMemoryRangeCopy(cLLVMMemoryBufferRef, CONST_CHAR_PTR, SIZE_T, CONST_CHAR_PTR),
        LLVMGetBufferStart(CONST_CHAR_PTR, cLLVMMemoryBufferRef),
        LLVMGetBufferSize(SIZE_T, cLLVMMemoryBufferRef),
        LLVMDisposeMemoryBuffer(void.class, cLLVMMemoryBufferRef),
        LLVMGetGlobalPassRegistry(cLLVMPassRegistryRef),
        LLVMCreatePassManager(cLLVMPassManagerRef),
        LLVMCreateFunctionPassManagerForModule(cLLVMPassManagerRef, cLLVMModuleRef),
        LLVMCreateFunctionPassManager(cLLVMPassManagerRef, cLLVMModuleProviderRef),
        LLVMRunPassManager(LLVMBool, cLLVMPassManagerRef, cLLVMModuleRef),
        LLVMInitializeFunctionPassManager(LLVMBool, cLLVMPassManagerRef),
        LLVMRunFunctionPassManager(LLVMBool, cLLVMPassManagerRef, cLLVMValueRef),
        LLVMFinalizeFunctionPassManager(LLVMBool, cLLVMPassManagerRef),
        LLVMDisposePassManager(void.class, cLLVMPassManagerRef),
        LLVMStartMultithreaded(LLVMBool),
        LLVMStopMultithreaded(void.class),
        LLVMIsMultithreaded(LLVMBool);

        private final MethodType type;
        private final Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
            this.handle = processSymbol(LLVM, LLVM_SCOPE, name(), type());
        }

        public MethodType type() {
            return type;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", handle=" + handle() + '}';
        }
    }

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeCore.handle().invoke(R.value()));
    }

    /**
     * Deallocate and destroy all ManagedStatic variables.
     */
    public static void LLVMShutdown() {
        nothrows_run(() -> Function.LLVMShutdown.handle().invoke());
    }

    /*===-- Error handling ----------------------------------------------------===*/

    /* package-private */
    static long LLVMCreateMessage(String Message) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Message = allocString(arena, Message);
            return nothrows_run(() -> (long) Function.LLVMCreateMessage.handle().invoke(c_Message.nativeAddress()));
        }
    }

    /* package-private */
    static void LLVMDisposeMessage(long Message) {
        nothrows_run(() -> Function.LLVMDisposeMessage.handle().invoke(Message));
    }

    /*
     * @defgroup LLVMCCoreContext Contexts
     *
     * Contexts are execution states for the core LLVM IR system.
     *
     * Most types are tied to a context instance. Multiple contexts can
     * exist simultaneously. A single context is not thread safe. However,
     * different contexts can execute on different threads simultaneously.
     */

    /**
     * Create a new context.
     * <p>
     * Every call to this function should be paired with a call to
     * LLVMContextDispose() or the context will leak memory.
     */
    public static LLVMContextRef LLVMContextCreate() {
        return nothrows_run(() -> LLVMContextRef.ofNullable((long) Function.LLVMContextCreate.handle().invoke()));
    }

    /**
     * Obtain the global context instance.
     */
    public static LLVMContextRef LLVMGetGlobalContext() {
        return nothrows_run(() -> LLVMContextRef.ofNullable((long) Function.LLVMGetGlobalContext.handle().invoke()));
    }

    //TODO:
    ///**
    // * Set the diagnostic handler for this context.
    // */
    //void LLVMContextSetDiagnosticHandler(LLVMContextRef C, LLVMDiagnosticHandler Handler, void *DiagnosticContext);
    ///**
    // * Get the diagnostic handler of this context.
    // */
    //LLVMDiagnosticHandler LLVMContextGetDiagnosticHandler(LLVMContextRef C);
    ///**
    // * Get the diagnostic context of this context.
    // */
    //void *LLVMContextGetDiagnosticContext(LLVMContextRef C);
    ///**
    // * Set the yield callback function for this context.
    // *
    // * @see LLVMContext::setYieldCallback()
    // */
    //void LLVMContextSetYieldCallback(LLVMContextRef C, LLVMYieldCallback Callback, void *OpaqueHandle);

    /**
     * Destroy a context instance.
     * <p>
     * This should be called for every call to LLVMContextCreate() or memory
     * will be leaked.
     */
    public static void LLVMContextDispose(LLVMContextRef C) {
        nothrows_run(() -> Function.LLVMContextDispose.handle().invoke(C.value()));
    }

    /**
     * Return a string representation of the DiagnosticInfo.
     */
    public static String LLVMGetDiagInfoDescription(LLVMDiagnosticInfoRef DI) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetDiagInfoDescription.handle().invoke(DI.value())));
    }

    /**
     * Return an enum LLVMDiagnosticSeverity.
     */
    public static LLVMDiagnosticSeverity LLVMGetDiagInfoSeverity(LLVMDiagnosticInfoRef DI) {
        return nothrows_run(() -> LLVMDiagnosticSeverity.of((int) Function.LLVMGetDiagInfoSeverity.handle().invoke(DI.value())));
    }

    public static int /* unsigned */ LLVMGetMDKindIDInContext(LLVMContextRef C, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            int /* unsigned */ SLen = Math.toIntExact(stringLength(c_Name));
            return nothrows_run(() -> (int) Function.LLVMGetMDKindIDInContext.handle().invoke(C.value(), c_Name.nativeAddress(), SLen));
        }
    }

    public static int /* unsigned */ LLVMGetMDKindID(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            int /* unsigned */ SLen = Math.toIntExact(stringLength(c_Name));
            return nothrows_run(() -> (int) Function.LLVMGetMDKindID.handle().invoke(c_Name.nativeAddress(), SLen));
        }
    }

    /**
     * Return an unique id given the name of a enum attribute,
     * or 0 if no attribute by that name exists.
     * <p>
     * See <a href="http://llvm.org/docs/LangRef.html#parameter-attributes">parameter-attributes</a>
     * and <a href="http://llvm.org/docs/LangRef.html#function-attributes">function-attributes</a>
     * for the list of available attributes.
     * <p>
     * NB: Attribute names and/or id are subject to change without
     * going through the C API deprecation cycle.
     */
    public static int /* unsigned */ LLVMGetEnumAttributeKindForName(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            long /* size_t */ SLen = stringLength(c_Name);
            return nothrows_run(() -> (int) Function.LLVMGetEnumAttributeKindForName.handle().invoke(c_Name.nativeAddress(), SLen));
        }
    }

    public static int /* unsigned */ LLVMGetLastEnumAttributeKind() {
        return nothrows_run(() -> (int) Function.LLVMGetLastEnumAttributeKind.handle().invoke());
    }

    /**
     * Create an enum attribute.
     */
    public static LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, int /* unsigned */ KindID, long /* uint64_t */ Val) {
        return nothrows_run(() -> LLVMAttributeRef.ofNullable((long) Function.LLVMCreateEnumAttribute.handle().invoke(C.value(), KindID, Val)));
    }

    /**
     * Get the unique id corresponding to the enum attribute
     * passed as argument.
     */
    public static int /* unsigned */ LLVMGetEnumAttributeKind(LLVMAttributeRef A) {
        return nothrows_run(() -> (int) Function.LLVMGetEnumAttributeKind.handle().invoke(A.value()));
    }

    /**
     * Get the enum attribute's value. 0 is returned if none exists.
     */
    public static long /* uint64_t */ LLVMGetEnumAttributeValue(LLVMAttributeRef A) {
        return nothrows_run(() -> (int) Function.LLVMGetEnumAttributeValue.handle().invoke(A.value()));
    }

    /**
     * Create a string attribute.
     */
    public static LLVMAttributeRef LLVMCreateStringAttribute(LLVMContextRef C, String K, String V) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_K = allocString(arena, K);
            int /* unsigned */ KLength = Math.toIntExact(stringLength(c_K));
            MemorySegment c_V = allocString(arena, V);
            int /* unsigned */ VLength = Math.toIntExact(stringLength(c_V));
            return nothrows_run(() -> LLVMAttributeRef.ofNullable((long) Function.LLVMCreateStringAttribute.handle()
                    .invoke(C.value(), c_K.nativeAddress(), KLength, c_V.nativeAddress(), VLength)));
        }
    }

    /**
     * Get the string attribute's kind.
     */
    public static String LLVMGetStringAttributeKind(LLVMAttributeRef A) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Length = arena.allocate(JAVA_INT);
            long ptr = nothrows_run(() -> (long) Function.LLVMGetStringAttributeKind.handle().invoke(A.value(), c_Length.nativeAddress()));
            int /* unsigned */ Length = c_Length.get(JAVA_INT, 0);
            return addressToString(ptr, Length);
        }
    }

    /**
     * Get the string attribute's value.
     */
    public static String LLVMGetStringAttributeValue(LLVMAttributeRef A) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Length = arena.allocate(JAVA_INT);
            long ptr = nothrows_run(() -> (long) Function.LLVMGetStringAttributeValue.handle().invoke(A.value(), c_Length.nativeAddress()));
            int /* unsigned */ Length = c_Length.get(JAVA_INT, 0);
            return addressToString(ptr, Length);
        }
    }

    /**
     * Check for the different types of attributes.
     */
    public static boolean LLVMIsEnumAttribute(LLVMAttributeRef A) {
        return nothrows_run(() -> (boolean) Function.LLVMIsEnumAttribute.handle().invoke(A.value()));
    }

    public static boolean LLVMIsStringAttribute(LLVMAttributeRef A) {
        return nothrows_run(() -> (boolean) Function.LLVMIsStringAttribute.handle().invoke(A.value()));
    }

    /*
     * @defgroup LLVMCCoreModule Modules
     *
     * Modules represent the top-level structure in an LLVM program. An LLVM
     * module is effectively a translation unit or a collection of
     * translation units merged together.
     */

    /**
     * Create a new, empty module in the global context.
     * <p>
     * This is equivalent to calling LLVMModuleCreateWithNameInContext with
     * LLVMGetGlobalContext() as the context parameter.
     * <p>
     * Every invocation should be paired with LLVMDisposeModule() or memory
     * will be leaked.
     */
    public static LLVMModuleRef LLVMModuleCreateWithName(String ModuleID) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ModuleID = allocString(arena, ModuleID);
            return nothrows_run(() -> LLVMModuleRef.ofNullable((long) Function.LLVMModuleCreateWithName.handle().invoke(c_ModuleID.nativeAddress())));
        }
    }

    /**
     * Create a new, empty module in a specific context.
     * <p>
     * Every invocation should be paired with LLVMDisposeModule() or memory
     * will be leaked.
     */
    public static LLVMModuleRef LLVMModuleCreateWithNameInContext(String ModuleID, LLVMContextRef C) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ModuleID = allocString(arena, ModuleID);
            return nothrows_run(() -> LLVMModuleRef.ofNullable((long) Function.LLVMModuleCreateWithNameInContext.handle().invoke(c_ModuleID.nativeAddress(), C.value())));
        }
    }

    /**
     * Return an exact copy of the specified module.
     */
    public static LLVMModuleRef LLVMCloneModule(LLVMModuleRef M) {
        return nothrows_run(() -> LLVMModuleRef.ofNullable((long) Function.LLVMCloneModule.handle().invoke(M.value())));
    }

    /**
     * Destroy a module instance.
     * <p>
     * This must be called for every created module or memory will be
     * leaked.
     */
    public static void LLVMDisposeModule(LLVMModuleRef M) {
        nothrows_run(() -> Function.LLVMDisposeModule.handle().invoke(M.value()));
    }

    //TODO
    ///**
    // * Obtain the identifier of a module.
    // *
    // * @param M Module to obtain identifier of
    // * @param Len Out parameter which holds the length of the returned string.
    // * @return The identifier of M.
    // * @see Module::getModuleIdentifier()
    // */
    //String LLVMGetModuleIdentifier(LLVMModuleRef M, long /* size_t */ *Len) {
    //    return nothrows_run(() -> Function.LLVMGetModuleIdentifier.handle().invoke());
    //}

    /**
     * Set the identifier of a module to a string Ident.
     *
     * @param M     The module to set identifier
     * @param Ident The string to set M's identifier to
     */
    public static void LLVMSetModuleIdentifier(LLVMModuleRef M, String Ident) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Ident = allocString(arena, Ident);
            long /* size_t */ Len = stringLength(c_Ident);
            nothrows_run(() -> Function.LLVMSetModuleIdentifier.handle().invoke(c_Ident.nativeAddress(), Len));
        }
    }

    /**
     * Obtain the data layout for a module.
     */
    public static String LLVMGetDataLayoutStr(LLVMModuleRef M) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetDataLayoutStr.handle().invoke(M.value())));
    }

    /**
     * Obtain the data layout for a module.
     * <p>
     * LLVMGetDataLayout is DEPRECATED, as the name is not only incorrect,
     * but match the name of another method on the module. Prefer the use
     * of LLVMGetDataLayoutStr, which is not ambiguous.
     */
    public static String LLVMGetDataLayout(LLVMModuleRef M) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetDataLayout.handle().invoke(M.value())));
    }

    /**
     * Set the data layout for a module.
     */
    public static void LLVMSetDataLayout(LLVMModuleRef M, String DataLayoutStr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_DataLayoutStr = allocString(arena, DataLayoutStr);
            nothrows_run(() -> Function.LLVMSetDataLayout.handle().invoke(M.value(), c_DataLayoutStr.nativeAddress()));
        }
    }

    /**
     * Obtain the target triple for a module.
     */
    public static String LLVMGetTarget(LLVMModuleRef M) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetTarget.handle().invoke(M.value())));
    }

    /**
     * Set the target triple for a module.
     */
    public static void LLVMSetTarget(LLVMModuleRef M, String Triple) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Triple = allocString(arena, Triple);
            nothrows_run(() -> Function.LLVMSetTarget.handle().invoke(M.value(), c_Triple.nativeAddress()));
        }
    }

    /**
     * Dump a representation of a module to stderr.
     */
    public static void LLVMDumpModule(LLVMModuleRef M) {
        nothrows_run(() -> Function.LLVMDumpModule.handle().invoke(M.value()));
    }

    //TODO
    ///**
    // * Print a representation of a module to a file. The ErrorMessage needs to be
    // * disposed with LLVMDisposeMessage. Returns 0 on success, 1 otherwise.
    // */
    //boolean LLVMPrintModuleToFile(LLVMModuleRef M, String Filename, LLVMString *ErrorMessage) {
    //    return nothrows_run(() -> Function.LLVMPrintModuleToFile.handle().invoke());
    //}

    /**
     * Return a string representation of the module.
     */
    public static String LLVMPrintModuleToString(LLVMModuleRef M) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMPrintModuleToString.handle().invoke(M.value())));
    }

    ///**
    // * Set inline assembly for a module.
    // *
    // * @see Module::setModuleInlineAsm()
    // */
    //void LLVMSetModuleInlineAsm(LLVMModuleRef M, String Asm) {
    //    return nothrows_run(() -> Function.LLVMSetModuleInlineAsm.handle().invoke());
    //}

    /**
     * Obtain the context to which this module is associated.
     */
    public static LLVMContextRef LLVMGetModuleContext(LLVMModuleRef M) {
        return nothrows_run(() -> LLVMContextRef.ofNullable((long) Function.LLVMGetModuleContext.handle().invoke(M.value())));
    }

    ///**
    // * Obtain a Type from a module by its registered name.
    // */
    //LLVMTypeRef LLVMGetTypeByName(LLVMModuleRef M, String Name) {
    //    return nothrows_run(() -> Function.LLVMGetTypeByName.handle().invoke());
    //}
    ///**
    // * Obtain the number of operands for named metadata in a module.
    // *
    // * @see llvm::Module::getNamedMetadata()
    // */
    //int /* unsigned */ LLVMGetNamedMetadataNumOperands(LLVMModuleRef M, String Name) {
    //    return nothrows_run(() -> Function.LLVMGetNamedMetadataNumOperands.handle().invoke());
    //}
    ///**
    // * Obtain the named metadata operands for a module.
    // *
    // * The passed LLVMValueRef pointer should refer to an array of
    // * LLVMValueRef at least LLVMGetNamedMetadataNumOperands long. This
    // * array will be populated with the LLVMValueRef instances. Each
    // * instance corresponds to a llvm::MDNode.
    // *
    // * @see llvm::Module::getNamedMetadata()
    // * @see llvm::MDNode::getOperand()
    // */
    //void LLVMGetNamedMetadataOperands(LLVMModuleRef M, String Name, LLVMValueRef *Dest) {
    //    return nothrows_run(() -> Function.LLVMGetNamedMetadataOperands.handle().invoke());
    //}
    ///**
    // * Add an operand to named metadata.
    // *
    // * @see llvm::Module::getNamedMetadata()
    // * @see llvm::MDNode::addOperand()
    // */
    //void LLVMAddNamedMetadataOperand(LLVMModuleRef M, String Name, LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMAddNamedMetadataOperand.handle().invoke());
    //}

    /**
     * Add a function to a module under a specified name.
     */
    public static LLVMValueRef LLVMAddFunction(LLVMModuleRef M, String Name, LLVMTypeRef FunctionTy) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMAddFunction.handle()
                    .invoke(M.value(), c_Name.nativeAddress(), FunctionTy.value())));
        }
    }

    /**
     * Obtain a Function value from a Module by its name.
     * <p>
     * The returned value corresponds to a llvm::Function value.
     */
    public static LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef M, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMGetNamedFunction.handle()
                    .invoke(M.value(), c_Name.nativeAddress())));
        }
    }

    ///**
    // * Obtain an iterator to the first Function in a Module.
    // *
    // * @see llvm::Module::begin()
    // */
    //LLVMValueRef LLVMGetFirstFunction(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMGetFirstFunction.handle().invoke());
    //}
    ///**
    // * Obtain an iterator to the last Function in a Module.
    // *
    // * @see llvm::Module::end()
    // */
    //LLVMValueRef LLVMGetLastFunction(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMGetLastFunction.handle().invoke());
    //}
    ///**
    // * Advance a Function iterator to the next Function.
    // *
    // * Returns NULL if the iterator was already at the end and there are no more
    // * functions.
    // */
    //LLVMValueRef LLVMGetNextFunction(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetNextFunction.handle().invoke());
    //}
    ///**
    // * Decrement a Function iterator to the previous Function.
    // *
    // * Returns NULL if the iterator was already at the beginning and there are
    // * no previous functions.
    // */
    //LLVMValueRef LLVMGetPreviousFunction(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetPreviousFunction.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreType Types
     *
     * Types represent the type of a value.
     *
     * Types are associated with a context instance. The context internally
     * deduplicates types so there is only 1 instance of a specific type
     * alive at a time. In other words, a unique type is shared among all
     * consumers within a context.
     *
     * A Type in the C API corresponds to llvm::Type.
     *
     * Types have the following hierarchy:
     *
     *   types:
     *     integer type
     *     real type
     *     function type
     *     sequence types:
     *       array type
     *       pointer type
     *       vector type
     *     void type
     *     label type
     *     opaque type
     */

    /**
     * Obtain the enumerated type of a Type instance.
     */
    public static LLVMTypeKind LLVMGetTypeKind(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMTypeKind.of((int) Function.LLVMGetTypeKind.handle().invoke(Ty.value())));
    }

    /**
     * Whether the type has a known size.
     * <p>
     * Things that don't have a size are abstract types, labels, and void.a
     */
    public static boolean LLVMTypeIsSized(LLVMTypeRef Ty) {
        return nothrows_run(() -> (boolean) Function.LLVMTypeIsSized.handle().invoke(Ty.value()));
    }

    /**
     * Obtain the context to which this type instance is associated.
     */
    public static LLVMContextRef LLVMGetTypeContext(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMContextRef.ofNullable((long) Function.LLVMGetTypeContext.handle().invoke(Ty.value())));
    }

    /**
     * Dump a representation of a type to stderr.
     */
    public static void LLVMDumpType(LLVMTypeRef Val) {
        nothrows_run(() -> Function.LLVMDumpType.handle().invoke(Val.value()));
    }

    /**
     * Return a string representation of the type.
     */
    public static String LLVMPrintTypeToString(LLVMTypeRef Val) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMPrintTypeToString.handle().invoke(Val.value())));
    }

    /*
     * @defgroup LLVMCCoreTypeInt Integer Types
     *
     * Functions in this section operate on integer types.
     */

    /**
     * Obtain an integer type from a context with specified bit width.
     */
    public static LLVMTypeRef LLVMInt1TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt1TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt8TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt16TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt32TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt64TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt128TypeInContext.handle().invoke(C.value())));
    }

    public static LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, int /* unsigned */ NumBits) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMIntTypeInContext.handle().invoke(C.value(), NumBits)));
    }

    /**
     * Obtain an integer type from the global context with a specified bit
     * width.
     */
    public static LLVMTypeRef LLVMInt1Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt1Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMInt8Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt8Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMInt16Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt16Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMInt32Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt32Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMInt64Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt64Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMInt128Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMInt128Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMIntType(int /* unsigned */ NumBits) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMIntType.handle().invoke(NumBits)));
    }

    public static int /* unsigned */ LLVMGetIntTypeWidth(LLVMTypeRef IntegerTy) {
        return nothrows_run(() -> (int) Function.LLVMGetIntTypeWidth.handle().invoke(IntegerTy.value()));
    }

    /*
     * @defgroup LLVMCCoreTypeFloat Floating Point Types
     */

    /**
     * Obtain a 16-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMHalfTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMHalfTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a 32-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMFloatTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMFloatTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a 64-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMDoubleTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMDoubleTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a 80-bit floating point type (X87) from a context.
     */
    public static LLVMTypeRef LLVMX86FP80TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMX86FP80TypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a 128-bit floating point type (112-bit mantissa) from a
     * context.
     */
    public static LLVMTypeRef LLVMFP128TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMFP128TypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a 128-bit floating point type (two 64-bits) from a context.
     */
    public static LLVMTypeRef LLVMPPCFP128TypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMPPCFP128TypeInContext.handle().invoke(C.value())));
    }

    /**
     * Obtain a floating point type from the global context.
     * <p>
     * These map to the functions in this group of the same name.
     */
    public static LLVMTypeRef LLVMHalfType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMHalfType.handle().invoke()));
    }

    public static LLVMTypeRef LLVMFloatType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMFloatType.handle().invoke()));
    }

    public static LLVMTypeRef LLVMDoubleType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMDoubleType.handle().invoke()));
    }

    public static LLVMTypeRef LLVMX86FP80Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMX86FP80Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMFP128Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMFP128Type.handle().invoke()));
    }

    public static LLVMTypeRef LLVMPPCFP128Type() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMPPCFP128Type.handle().invoke()));
    }

    /*
     * @defgroup LLVMCCoreTypeFunction Function Types
     */

    /**
     * Obtain a function type consisting of a specified signature.
     * <p>
     * The function is defined as a tuple of a return Type, a list of
     * parameter types, and whether the function is variadic.
     */
    public static LLVMTypeRef LLVMFunctionType(LLVMTypeRef ReturnType, LLVMTypeRef[] ParamTypes, boolean IsVarArg) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ParamTypes = allocArray(arena, ParamTypes);
            int /* unsigned */ ParamCount = arrayLength(ParamTypes);
            return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMFunctionType.handle()
                    .invoke(ReturnType.value(), c_ParamTypes.nativeAddress(), ParamCount, IsVarArg)));
        }
    }

    /**
     * Returns whether a function type is variadic.
     */
    public static boolean LLVMIsFunctionVarArg(LLVMTypeRef FunctionTy) {
        return nothrows_run(() -> (boolean) Function.LLVMIsFunctionVarArg.handle().invoke(FunctionTy.value()));
    }

    /**
     * Obtain the Type this function Type returns.
     */
    public static LLVMTypeRef LLVMGetReturnType(LLVMTypeRef FunctionTy) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMGetReturnType.handle().invoke(FunctionTy.value())));
    }

    ///**
    // * Obtain the number of parameters this function accepts.
    // */
    //int /* unsigned */ LLVMCountParamTypes(LLVMTypeRef FunctionTy) {
    //    return nothrows_run(() -> Function.LLVMCountParamTypes.handle().invoke());
    //}
    ///**
    // * Obtain the types of a function's parameters.
    // *
    // * The Dest parameter should point to a pre-allocated array of
    // * LLVMTypeRef at least LLVMCountParamTypes() large. On return, the
    // * first LLVMCountParamTypes() entries in the array will be populated
    // * with LLVMTypeRef instances.
    // *
    // * @param FunctionTy The function type to operate on.
    // * @param Dest Memory address of an array to be filled with result.
    // */
    //void LLVMGetParamTypes(LLVMTypeRef FunctionTy, LLVMTypeRef *Dest) {
    //    return nothrows_run(() -> Function.LLVMGetParamTypes.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreTypeStruct Structure Types
     *
     * These functions relate to LLVMTypeRef instances.
     */

    /**
     * Create a new structure type in a context.
     * <p>
     * A structure is specified by a list of inner elements/types and
     * whether these can be packed together.
     */
    public static LLVMTypeRef LLVMStructTypeInContext(LLVMContextRef C, LLVMTypeRef[] ElementTypes, boolean Packed) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ElementTypes = allocArray(arena, ElementTypes);
            int /* unsigned */ ElementCount = arrayLength(ElementTypes);
            return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMStructTypeInContext.handle()
                    .invoke(C.value(), c_ElementTypes.nativeAddress(), ElementCount, Packed)));
        }
    }

    /**
     * Create a new structure type in the global context.
     */
    public static LLVMTypeRef LLVMStructType(LLVMTypeRef[] ElementTypes, boolean Packed) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ElementTypes = allocArray(arena, ElementTypes);
            int /* unsigned */ ElementCount = arrayLength(ElementTypes);
            return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMStructType.handle()
                    .invoke(c_ElementTypes.nativeAddress(), ElementCount, Packed)));
        }
    }

    /**
     * Create an empty structure in a context having a specified name.
     */
    public static LLVMTypeRef LLVMStructCreateNamed(LLVMContextRef C, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMStructCreateNamed
                    .handle().invoke(C.value(), c_Name.nativeAddress())));
        }
    }

    /**
     * Obtain the name of a structure.
     */
    public static String LLVMGetStructName(LLVMTypeRef Ty) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetStructName.handle().invoke(Ty.value())));
    }

    /**
     * Set the contents of a structure type.
     */
    public static void LLVMStructSetBody(LLVMTypeRef StructTy, LLVMTypeRef[] ElementTypes, boolean Packed) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ElementTypes = allocArray(arena, ElementTypes);
            int /* unsigned */ ElementCount = arrayLength(ElementTypes);
            nothrows_run(() -> Function.LLVMStructSetBody.handle().invoke(
                    StructTy.value(), c_ElementTypes.nativeAddress(), ElementCount, Packed));
        }
    }

    ///**
    // * Get the number of elements defined inside the structure.
    // *
    // * @see llvm::StructType::getNumElements()
    // */
    //int /* unsigned */ LLVMCountStructElementTypes(LLVMTypeRef StructTy) {
    //    return nothrows_run(() -> Function.LLVMCountStructElementTypes.handle().invoke());
    //}
    ///**
    // * Get the elements within a structure.
    // *
    // * The function is passed the address of a pre-allocated array of
    // * LLVMTypeRef at least LLVMCountStructElementTypes() long. After
    // * invocation, this array will be populated with the structure's
    // * elements. The objects in the destination array will have a lifetime
    // * of the structure type itself, which is the lifetime of the context it
    // * is contained in.
    // */
    //void LLVMGetStructElementTypes(LLVMTypeRef StructTy, LLVMTypeRef *Dest) {
    //    return nothrows_run(() -> Function.LLVMGetStructElementTypes.handle().invoke());
    //}
    ///**
    // * Get the type of the element at a given index in the structure.
    // *
    // * @see llvm::StructType::getTypeAtIndex()
    // */
    //LLVMTypeRef LLVMStructGetTypeAtIndex(LLVMTypeRef StructTy, int /* unsigned */ i) {
    //    return nothrows_run(() -> Function.LLVMStructGetTypeAtIndex.handle().invoke());
    //}

    /**
     * Determine whether a structure is packed.
     */
    public static boolean LLVMIsPackedStruct(LLVMTypeRef StructTy) {
        return nothrows_run(() -> (boolean) Function.LLVMIsPackedStruct.handle().invoke(StructTy.value()));
    }

    /**
     * Determine whether a structure is opaque.
     */
    public static boolean LLVMIsOpaqueStruct(LLVMTypeRef StructTy) {
        return nothrows_run(() -> (boolean) Function.LLVMIsOpaqueStruct.handle().invoke(StructTy.value()));
    }

    /*
     * @defgroup LLVMCCoreTypeSequential Sequential Types
     *
     * Sequential types represents "arrays" of types. This is a super class
     * for array, vector, and pointer types.
     */

    /**
     * Obtain the type of elements within a sequential type.
     * <p>
     * This works on array, vector, and pointer types.
     */
    public static LLVMTypeRef LLVMGetElementType(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMGetElementType.handle().invoke(Ty.value())));
    }

    /**
     * Create a fixed size array type that refers to a specific type.
     * <p>
     * The created type will exist in the context that its element type
     * exists in.
     */
    public static LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMArrayType.handle().invoke(ElementType.value(), ElementCount)));
    }

    /**
     * Obtain the length of an array type.
     * <p>
     * This only works on types that represent arrays.
     */
    public static int /* unsigned */ LLVMGetArrayLength(LLVMTypeRef ArrayTy) {
        return nothrows_run(() -> (int) Function.LLVMGetArrayLength.handle().invoke(ArrayTy.value()));
    }

    /**
     * Create a pointer type that points to a defined type.
     * <p>
     * The created type will exist in the context that its pointee type
     * exists in.
     */
    public static LLVMTypeRef LLVMPointerType(LLVMTypeRef ElementType, int /* unsigned */ AddressSpace) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMPointerType.handle().invoke(ElementType.value(), AddressSpace)));
    }

    /**
     * Obtain the address space of a pointer type.
     * <p>
     * This only works on types that represent pointers.
     */
    public static int /* unsigned */ LLVMGetPointerAddressSpace(LLVMTypeRef PointerTy) {
        return nothrows_run(() -> (int) Function.LLVMGetPointerAddressSpace.handle().invoke(PointerTy.value()));
    }

    /**
     * Create a vector type that contains a defined type and has a specific
     * number of elements.
     * <p>
     * The created type will exist in the context thats its element type
     * exists in.
     */
    public static LLVMTypeRef LLVMVectorType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMVectorType.handle().invoke(ElementType.value(), ElementCount)));
    }

    /**
     * Obtain the number of elements in a vector type.
     * <p>
     * This only works on types that represent vectors.
     */
    public static int /* unsigned */ LLVMGetVectorSize(LLVMTypeRef VectorTy) {
        return nothrows_run(() -> (int) Function.LLVMGetVectorSize.handle().invoke(VectorTy.value()));
    }

    /*
     * @defgroup LLVMCCoreTypeOther Other Types
     */

    /**
     * Create a void type in a context.
     */
    public static LLVMTypeRef LLVMVoidTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMVoidTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Create a label type in a context.
     */
    public static LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMLabelTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Create a X86 MMX type in a context.
     */
    public static LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMX86MMXTypeInContext.handle().invoke(C.value())));
    }

    /**
     * Create a void type in the global context.
     */
    public static LLVMTypeRef LLVMVoidType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMVoidType.handle().invoke()));
    }

    /**
     * Create a label type in the global context.
     */
    public static LLVMTypeRef LLVMLabelType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMLabelType.handle().invoke()));
    }

    /**
     * Create a X86 MMX type in the global context.
     */
    public static LLVMTypeRef LLVMX86MMXType() {
        return nothrows_run(() -> LLVMTypeRef.ofNullable((long) Function.LLVMX86MMXType.handle().invoke()));
    }

    /*
     * @defgroup LLVMCCoreValueGeneral General APIs
     *
     * Functions in this section work on all LLVMValueRef instances,
     * regardless of their sub-type. They correspond to functions available
     * on llvm::Value.
     */

    ///**
    // * Obtain the type of a value.
    // *
    // * @see llvm::Value::getType()
    // */
    //LLVMTypeRef LLVMTypeOf(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMTypeOf.handle().invoke());
    //}
    ///**
    // * Obtain the enumerated type of a Value instance.
    // *
    // * @see llvm::Value::getValueID()
    // */
    //LLVMValueKind LLVMGetValueKind(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMGetValueKind.handle().invoke());
    //}
    ///**
    // * Obtain the string name of a value.
    // *
    // * @see llvm::Value::getName()
    // */
    //String LLVMGetValueName(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMGetValueName.handle().invoke());
    //}
    ///**
    // * Set the string name of a value.
    // *
    // * @see llvm::Value::setName()
    // */
    //void LLVMSetValueName(LLVMValueRef Val, String Name) {
    //    return nothrows_run(() -> Function.LLVMSetValueName.handle().invoke());
    //}
    ///**
    // * Dump a representation of a value to stderr.
    // *
    // * @see llvm::Value::dump()
    // */
    //void LLVMDumpValue(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMDumpValue.handle().invoke());
    //}

    /**
     * Return a string representation of the value. Use
     * LLVMDisposeMessage to free the string.
     */
    public static String LLVMPrintValueToString(LLVMValueRef Val) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMPrintValueToString.handle().invoke(Val.value())));
    }

    ///**
    // * Replace all uses of a value with another one.
    // *
    // * @see llvm::Value::replaceAllUsesWith()
    // */
    //void LLVMReplaceAllUsesWith(LLVMValueRef OldVal, LLVMValueRef NewVal) {
    //    return nothrows_run(() -> Function.LLVMReplaceAllUsesWith.handle().invoke());
    //}
    ///**
    // * Determine whether the specified value instance is constant.
    // */
    //boolean LLVMIsConstant(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMIsConstant.handle().invoke());
    //}
    ///**
    // * Determine whether a value instance is undefined.
    // */
    //boolean LLVMIsUndef(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMIsUndef.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueUses Usage
     *
     * This module defines functions that allow you to inspect the uses of a
     * LLVMValueRef.
     *
     * It is possible to obtain an LLVMUseRef for any LLVMValueRef instance.
     * Each LLVMUseRef (which corresponds to a llvm::Use instance) holds a
     * llvm::User and llvm::Value.
     */

    ///**
    // * Obtain the first use of a value.
    // *
    // * Uses are obtained in an iterator fashion. First, call this function
    // * to obtain a reference to the first use. Then, call LLVMGetNextUse()
    // * on that instance and all subsequently obtained instances until
    // * LLVMGetNextUse() returns NULL.
    // *
    // * @see llvm::Value::use_begin()
    // */
    //LLVMUseRef LLVMGetFirstUse(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMGetFirstUse.handle().invoke());
    //}
    ///**
    // * Obtain the next use of a value.
    // *
    // * This effectively advances the iterator. It returns NULL if you are on
    // * the final use and no more are available.
    // */
    //LLVMUseRef LLVMGetNextUse(LLVMUseRef U) {
    //    return nothrows_run(() -> Function.LLVMGetNextUse.handle().invoke());
    //}
    ///**
    // * Obtain the user value for a user.
    // *
    // * The returned value corresponds to a llvm::User type.
    // *
    // * @see llvm::Use::getUser()
    // */
    //LLVMValueRef LLVMGetUser(LLVMUseRef U) {
    //    return nothrows_run(() -> Function.LLVMGetUser.handle().invoke());
    //}
    ///**
    // * Obtain the value this use corresponds to.
    // *
    // * @see llvm::Use::get().
    // */
    //LLVMValueRef LLVMGetUsedValue(LLVMUseRef U) {
    //    return nothrows_run(() -> Function.LLVMGetUsedValue.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueUser User value
     *
     * Function in this group pertain to LLVMValueRef instances that descent
     * from llvm::User. This includes constants, instructions, and
     * operators.
     */

    ///**
    // * Obtain an operand at a specific index in a llvm::User value.
    // *
    // * @see llvm::User::getOperand()
    // */
    //LLVMValueRef LLVMGetOperand(LLVMValueRef Val, int /* unsigned */ Index) {
    //    return nothrows_run(() -> Function.LLVMGetOperand.handle().invoke());
    //}
    ///**
    // * Obtain the use of an operand at a specific index in a llvm::User value.
    // *
    // * @see llvm::User::getOperandUse()
    // */
    //LLVMUseRef LLVMGetOperandUse(LLVMValueRef Val, int /* unsigned */ Index) {
    //    return nothrows_run(() -> Function.LLVMGetOperandUse.handle().invoke());
    //}
    ///**
    // * Set an operand at a specific index in a llvm::User value.
    // *
    // * @see llvm::User::setOperand()
    // */
    //void LLVMSetOperand(LLVMValueRef User, int /* unsigned */ Index, LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMSetOperand.handle().invoke());
    //}
    ///**
    // * Obtain the number of operands in a llvm::User value.
    // *
    // * @see llvm::User::getNumOperands()
    // */
    //int LLVMGetNumOperands(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMGetNumOperands.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueConstant Constants
     *
     * This section contains APIs for interacting with LLVMValueRef that
     * correspond to llvm::Constant instances.
     *
     * These functions will work for any LLVMValueRef in the llvm::Constant
     * class hierarchy.
     */

    /**
     * Obtain a constant value referring to the null instance of a type.
     */
    public static LLVMValueRef LLVMConstNull(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstNull.handle().invoke(Ty.value())));
    }

    /**
     * Obtain a constant value referring to the instance of a type
     * consisting of all ones.
     * <p>
     * This is only valid for integer types.
     */
    public static LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstAllOnes.handle().invoke(Ty.value())));
    }

    /**
     * Obtain a constant value referring to an undefined value of a type.
     */
    public static LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMGetUndef.handle().invoke(Ty.value())));
    }

    ///**
    // * Determine whether a value instance is null.
    // *
    // * @see llvm::Constant::isNullValue()
    // */
    //boolean LLVMIsNull(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMIsNull.handle().invoke());
    //}
    ///**
    // * Obtain a constant that is a constant pointer pointing to NULL for a
    // * specified type.
    // */
    //LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMConstPointerNull.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueConstantScalar Scalar constants
     *
     * Functions in this group model LLVMValueRef instances that correspond
     * to constants referring to scalar types.
     *
     * For integer types, the LLVMTypeRef parameter should correspond to a
     * llvm::IntegerType instance and the returned LLVMValueRef will
     * correspond to a llvm::ConstantInt.
     *
     * For floating point types, the LLVMTypeRef returned corresponds to a
     * llvm::ConstantFP.
     */

    /**
     * Obtain a constant value for an integer type.
     * <p>
     * The returned value corresponds to a llvm::ConstantInt.
     *
     * @param IntTy      Integer type to obtain value of.
     * @param N          The value the returned instance should refer to.
     * @param SignExtend Whether to sign extend the produced value.
     */
    public static LLVMValueRef LLVMConstInt(LLVMTypeRef IntTy, long /* unsigned long long */ N, boolean SignExtend) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstInt.handle().invoke(IntTy.value(), N, SignExtend)));
    }

    /**
     * Obtain a constant value for an integer of arbitrary precision.
     */
    public static LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy, long... /* uint64_t */ Words) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Words = allocArray(arena, Words);
            int /* unsigned */ NumWords = arrayLength(Words);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstIntOfArbitraryPrecision
                    .handle().invoke(IntTy.value(), NumWords, c_Words.nativeAddress())));
        }
    }

    ///**
    // * Obtain a constant value for an integer parsed from a string.
    // *
    // * A similar API, LLVMConstIntOfStringAndSize is also available. If the
    // * string's length is available, it is preferred to call that function
    // * instead.
    // *
    // * @see llvm::ConstantInt::get()
    // */
    //LLVMValueRef LLVMConstIntOfString(LLVMTypeRef IntTy, String Text, byte /* uint8_t */ Radix) {
    //    return nothrows_run(() -> Function.LLVMConstIntOfString.handle().invoke());
    //}
    ///**
    // * Obtain a constant value for an integer parsed from a string with
    // * specified length.
    // *
    // * @see llvm::ConstantInt::get()
    // */
    //LLVMValueRef LLVMConstIntOfStringAndSize(LLVMTypeRef IntTy, String Text, int /* unsigned */ SLen, byte /* uint8_t */ Radix) {
    //    return nothrows_run(() -> Function.LLVMConstIntOfStringAndSize.handle().invoke());
    //}
    ///**
    // * Obtain a constant value referring to a double floating point value.
    // */
    //LLVMValueRef LLVMConstReal(LLVMTypeRef RealTy, double N) {
    //    return nothrows_run(() -> Function.LLVMConstReal.handle().invoke());
    //}
    ///**
    // * Obtain a constant for a floating point value parsed from a string.
    // *
    // * A similar API, LLVMConstRealOfStringAndSize is also available. It
    // * should be used if the input string's length is known.
    // */
    //LLVMValueRef LLVMConstRealOfString(LLVMTypeRef RealTy, String Text) {
    //    return nothrows_run(() -> Function.LLVMConstRealOfString.handle().invoke());
    //}
    ///**
    // * Obtain a constant for a floating point value parsed from a string.
    // */
    //LLVMValueRef LLVMConstRealOfStringAndSize(LLVMTypeRef RealTy, String Text, int /* unsigned */ SLen) {
    //    return nothrows_run(() -> Function.LLVMConstRealOfStringAndSize.handle().invoke());
    //}
    ///**
    // * Obtain the zero extended value for an integer constant value.
    // *
    // * @see llvm::ConstantInt::getZExtValue()
    // */
    //long /* unsigned long long */ LLVMConstIntGetZExtValue(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstIntGetZExtValue.handle().invoke());
    //}
    ///**
    // * Obtain the sign extended value for an integer constant value.
    // *
    // * @see llvm::ConstantInt::getSExtValue()
    // */
    //long /* long long */ LLVMConstIntGetSExtValue(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstIntGetSExtValue.handle().invoke());
    //}
    ///**
    // * Obtain the double value for an floating point constant value.
    // * losesInfo indicates if some precision was lost in the conversion.
    // *
    // * @see llvm::ConstantFP::getDoubleValue
    // */
    //double LLVMConstRealGetDouble(LLVMValueRef ConstantVal, boolean *losesInfo) {
    //    return nothrows_run(() -> Function.LLVMConstRealGetDouble.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueConstantComposite Composite Constants
     *
     * Functions in this group operate on composite constants.
     */

    /**
     * Create a ConstantDataSequential and initialize it with a string.
     */
    public static LLVMValueRef LLVMConstStringInContext(LLVMContextRef C, String Str, boolean DontNullTerminate) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            int /* unsigned */ Length = Math.toIntExact(stringLength(c_Str));
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstStringInContext.handle()
                    .invoke(C.value(), c_Str.nativeAddress(), Length, DontNullTerminate)));
        }
    }

    /**
     * Create a ConstantDataSequential with string content in the global context.
     * <p>
     * This is the same as LLVMConstStringInContext except it operates on the
     * global context.
     */
    public static LLVMValueRef LLVMConstString(String Str, boolean DontNullTerminate) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            int /* unsigned */ Length = Math.toIntExact(stringLength(c_Str));
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMConstString.handle()
                    .invoke(c_Str.nativeAddress(), Length, DontNullTerminate)));
        }
    }

    ///**
    // * Returns true if the specified constant is an array of i8.
    // *
    // * @see ConstantDataSequential::getAsString()
    // */
    //boolean LLVMIsConstantString(LLVMValueRef c) {
    //    return nothrows_run(() -> Function.LLVMIsConstantString.handle().invoke());
    //}
    ///**
    // * Get the given constant data sequential as a string.
    // *
    // * @see ConstantDataSequential::getAsString()
    // */
    //String LLVMGetAsString(LLVMValueRef c, long /* size_t */ *Length) {
    //    return nothrows_run(() -> Function.LLVMGetAsString.handle().invoke());
    //}
    ///**
    // * Create an anonymous ConstantStruct with the specified values.
    // *
    // * @see llvm::ConstantStruct::getAnon()
    // */
    //LLVMValueRef LLVMConstStructInContext(LLVMContextRef C, LLVMValueRef *ConstantVals, int /* unsigned */ Count, boolean Packed) {
    //    return nothrows_run(() -> Function.LLVMConstStructInContext.handle().invoke());
    //}
    ///**
    // * Create a ConstantStruct in the global Context.
    // *
    // * This is the same as LLVMConstStructInContext except it operates on the
    // * global Context.
    // *
    // * @see LLVMConstStructInContext()
    // */
    //LLVMValueRef LLVMConstStruct(LLVMValueRef *ConstantVals, int /* unsigned */ Count, boolean Packed) {
    //    return nothrows_run(() -> Function.LLVMConstStruct.handle().invoke());
    //}
    ///**
    // * Create a ConstantArray from values.
    // *
    // * @see llvm::ConstantArray::get()
    // */
    //LLVMValueRef LLVMConstArray(LLVMTypeRef ElementTy, LLVMValueRef *ConstantVals, int /* unsigned */ Length) {
    //    return nothrows_run(() -> Function.LLVMConstArray.handle().invoke());
    //}
    ///**
    // * Create a non-anonymous ConstantStruct from values.
    // *
    // * @see llvm::ConstantStruct::get()
    // */
    //LLVMValueRef LLVMConstNamedStruct(LLVMTypeRef StructTy, LLVMValueRef *ConstantVals, int /* unsigned */ Count) {
    //    return nothrows_run(() -> Function.LLVMConstNamedStruct.handle().invoke());
    //}
    ///**
    // * Get an element at specified index as a constant.
    // *
    // * @see ConstantDataSequential::getElementAsConstant()
    // */
    //LLVMValueRef LLVMGetElementAsConstant(LLVMValueRef C, int /* unsigned */ idx) {
    //    return nothrows_run(() -> Function.LLVMGetElementAsConstant.handle().invoke());
    //}
    ///**
    // * Create a ConstantVector from values.
    // *
    // * @see llvm::ConstantVector::get()
    // */
    //LLVMValueRef LLVMConstVector(LLVMValueRef *ScalarConstantVals, int /* unsigned */ Size) {
    //    return nothrows_run(() -> Function.LLVMConstVector.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueConstantExpressions Constant Expressions
    // *
    // * Functions in this group correspond to APIs on llvm::ConstantExpr.
    // *
    // * @see llvm::ConstantExpr.
    // *
    // * @{
    // */
    //LLVMOpcode LLVMGetConstOpcode(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMGetConstOpcode.handle().invoke());
    //}
    //LLVMValueRef LLVMAlignOf(LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMAlignOf.handle().invoke());
    //}
    //LLVMValueRef LLVMSizeOf(LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMSizeOf.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNeg(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstNeg.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNSWNeg(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstNSWNeg.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNUWNeg(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstNUWNeg.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFNeg(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstFNeg.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNot(LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMConstNot.handle().invoke());
    //}
    //LLVMValueRef LLVMConstAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstAdd.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNSWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNSWAdd.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNUWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNUWAdd.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFAdd.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstSub.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNSWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNSWSub.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNUWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNUWSub.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFSub.handle().invoke());
    //}
    //LLVMValueRef LLVMConstMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstMul.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNSWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNSWMul.handle().invoke());
    //}
    //LLVMValueRef LLVMConstNUWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstNUWMul.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFMul.handle().invoke());
    //}
    //LLVMValueRef LLVMConstUDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstUDiv.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstSDiv.handle().invoke());
    //}
    //LLVMValueRef LLVMConstExactSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstExactSDiv.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFDiv.handle().invoke());
    //}
    //LLVMValueRef LLVMConstURem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstURem.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstSRem.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFRem.handle().invoke());
    //}
    //LLVMValueRef LLVMConstAnd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstAnd.handle().invoke());
    //}
    //LLVMValueRef LLVMConstOr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstOr.handle().invoke());
    //}
    //LLVMValueRef LLVMConstXor(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstXor.handle().invoke());
    //}
    //LLVMValueRef LLVMConstICmp(LLVMIntPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstICmp.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFCmp(LLVMRealPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstFCmp.handle().invoke());
    //}
    //LLVMValueRef LLVMConstShl(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstShl.handle().invoke());
    //}
    //LLVMValueRef LLVMConstLShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstLShr.handle().invoke());
    //}
    //LLVMValueRef LLVMConstAShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return nothrows_run(() -> Function.LLVMConstAShr.handle().invoke());
    //}
    //LLVMValueRef LLVMConstGEP(LLVMValueRef ConstantVal, LLVMValueRef *ConstantIndices, int /* unsigned */ NumIndices) {
    //    return nothrows_run(() -> Function.LLVMConstGEP.handle().invoke());
    //}
    //LLVMValueRef LLVMConstInBoundsGEP(LLVMValueRef ConstantVal, LLVMValueRef *ConstantIndices, int /* unsigned */ NumIndices) {
    //    return nothrows_run(() -> Function.LLVMConstInBoundsGEP.handle().invoke());
    //}
    //LLVMValueRef LLVMConstTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstTrunc.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstSExt.handle().invoke());
    //}
    //LLVMValueRef LLVMConstZExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstZExt.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFPTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstFPTrunc.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFPExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstFPExt.handle().invoke());
    //}
    //LLVMValueRef LLVMConstUIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstUIToFP.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstSIToFP.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFPToUI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstFPToUI.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFPToSI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstFPToSI.handle().invoke());
    //}
    //LLVMValueRef LLVMConstPtrToInt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstPtrToInt.handle().invoke());
    //}
    //LLVMValueRef LLVMConstIntToPtr(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstIntToPtr.handle().invoke());
    //}
    //LLVMValueRef LLVMConstBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstBitCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstAddrSpaceCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstAddrSpaceCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstZExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstZExtOrBitCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstSExtOrBitCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstTruncOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstTruncOrBitCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstPointerCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstPointerCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstIntCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType, boolean isSigned) {
    //    return nothrows_run(() -> Function.LLVMConstIntCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstFPCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return nothrows_run(() -> Function.LLVMConstFPCast.handle().invoke());
    //}
    //LLVMValueRef LLVMConstSelect(LLVMValueRef ConstantCondition, LLVMValueRef ConstantIfTrue, LLVMValueRef ConstantIfFalse) {
    //    return nothrows_run(() -> Function.LLVMConstSelect.handle().invoke());
    //}
    //LLVMValueRef LLVMConstExtractElement(LLVMValueRef VectorConstant, LLVMValueRef IndexConstant) {
    //    return nothrows_run(() -> Function.LLVMConstExtractElement.handle().invoke());
    //}
    //LLVMValueRef LLVMConstInsertElement(LLVMValueRef VectorConstant, LLVMValueRef ElementValueConstant, LLVMValueRef IndexConstant) {
    //    return nothrows_run(() -> Function.LLVMConstInsertElement.handle().invoke());
    //}
    //LLVMValueRef LLVMConstShuffleVector(LLVMValueRef VectorAConstant, LLVMValueRef VectorBConstant, LLVMValueRef MaskConstant) {
    //    return nothrows_run(() -> Function.LLVMConstShuffleVector.handle().invoke());
    //}
    //LLVMValueRef LLVMConstExtractValue(LLVMValueRef AggConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return nothrows_run(() -> Function.LLVMConstExtractValue.handle().invoke());
    //}
    //LLVMValueRef LLVMConstInsertValue(LLVMValueRef AggConstant, LLVMValueRef ElementValueConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return nothrows_run(() -> Function.LLVMConstInsertValue.handle().invoke());
    //}
    //LLVMValueRef LLVMConstInlineAsm(LLVMTypeRef Ty, String AsmString, String Constraints, boolean HasSideEffects, boolean IsAlignStack) {
    //    return nothrows_run(() -> Function.LLVMConstInlineAsm.handle().invoke());
    //}
    //LLVMValueRef LLVMBlockAddress(LLVMValueRef F, LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMBlockAddress.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueConstantGlobals Global Values
     *
     * This group contains functions that operate on global values. Functions in
     * this group relate to functions in the llvm::GlobalValue class tree.
     */

    public static LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global) {
        return nothrows_run(() -> LLVMModuleRef.ofNullable((long) Function.LLVMGetGlobalParent.handle().invoke(Global.value())));
    }

    public static boolean LLVMIsDeclaration(LLVMValueRef Global) {
        return nothrows_run(() -> (boolean) Function.LLVMIsDeclaration.handle().invoke(Global.value()));
    }

    public static LLVMLinkage LLVMGetLinkage(LLVMValueRef Global) {
        return nothrows_run(() -> LLVMLinkage.of((int) Function.LLVMGetLinkage.handle().invoke(Global.value())));
    }

    public static void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage) {
        nothrows_run(() -> Function.LLVMSetLinkage.handle().invoke(Global.value(), Linkage.value()));
    }

    public static String LLVMGetSection(LLVMValueRef Global) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetSection.handle().invoke(Global.value())));
    }

    public static void LLVMSetSection(LLVMValueRef Global, String Section) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Section = allocString(arena, Section);
            nothrows_run(() -> Function.LLVMSetSection.handle().invoke(Global.value(), c_Section.nativeAddress()));
        }
    }

    public static LLVMVisibility LLVMGetVisibility(LLVMValueRef Global) {
        return nothrows_run(() -> LLVMVisibility.of((int) Function.LLVMGetVisibility.handle().invoke(Global.value())));
    }

    public static void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz) {
        nothrows_run(() -> Function.LLVMSetVisibility.handle().invoke(Global.value(), Viz.value()));
    }

    public static LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global) {
        return nothrows_run(() -> LLVMDLLStorageClass.of((int) Function.LLVMGetDLLStorageClass.handle().invoke(Global.value())));
    }

    public static void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class) {
        nothrows_run(() -> Function.LLVMSetDLLStorageClass.handle().invoke(Global.value(), Class.value()));
    }

    //boolean LLVMHasUnnamedAddr(LLVMValueRef Global) {
    //    return nothrows_run(() -> Function.LLVMHasUnnamedAddr.handle().invoke());
    //}
    //void LLVMSetUnnamedAddr(LLVMValueRef Global, boolean HasUnnamedAddr) {
    //    return nothrows_run(() -> Function.LLVMSetUnnamedAddr.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueWithAlignment Values with alignment
     *
     * Functions in this group only apply to values with alignment, i.e.
     * global variables, load and store instructions.
     */

    /**
     * Obtain the preferred alignment of the value.
     */
    public static int /* unsigned */ LLVMGetAlignment(LLVMValueRef V) {
        return nothrows_run(() -> (int) Function.LLVMGetAlignment.handle().invoke(V.value()));
    }

    /**
     * Set the preferred alignment of the value.
     */
    public static void LLVMSetAlignment(LLVMValueRef V, int /* unsigned */ Bytes) {
        nothrows_run(() -> Function.LLVMSetAlignment.handle().invoke(V.value(), Bytes));
    }

    /*
     * @defgroup LLVMCoreValueConstantGlobalVariable Global Variables
     *
     * This group contains functions that operate on global variable values.
     */

    //LLVMValueRef LLVMAddGlobal(LLVMModuleRef M, LLVMTypeRef Ty, String Name) {
    //    return nothrows_run(() -> Function.LLVMAddGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMAddGlobalInAddressSpace(LLVMModuleRef M, LLVMTypeRef Ty, String Name, int /* unsigned */ AddressSpace) {
    //    return nothrows_run(() -> Function.LLVMAddGlobalInAddressSpace.handle().invoke());
    //}
    //LLVMValueRef LLVMGetNamedGlobal(LLVMModuleRef M, String Name) {
    //    return nothrows_run(() -> Function.LLVMGetNamedGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMGetFirstGlobal(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMGetFirstGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMGetLastGlobal(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMGetLastGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMGetNextGlobal(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMGetNextGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMGetPreviousGlobal(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMGetPreviousGlobal.handle().invoke());
    //}
    //void LLVMDeleteGlobal(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMDeleteGlobal.handle().invoke());
    //}
    //LLVMValueRef LLVMGetInitializer(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMGetInitializer.handle().invoke());
    //}
    //void LLVMSetInitializer(LLVMValueRef GlobalVar, LLVMValueRef ConstantVal) {
    //    return nothrows_run(() -> Function.LLVMSetInitializer.handle().invoke());
    //}
    //boolean LLVMIsThreadLocal(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMIsThreadLocal.handle().invoke());
    //}
    //void LLVMSetThreadLocal(LLVMValueRef GlobalVar, boolean IsThreadLocal) {
    //    return nothrows_run(() -> Function.LLVMSetThreadLocal.handle().invoke());
    //}
    //boolean LLVMIsGlobalConstant(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMIsGlobalConstant.handle().invoke());
    //}
    //void LLVMSetGlobalConstant(LLVMValueRef GlobalVar, boolean IsConstant) {
    //    return nothrows_run(() -> Function.LLVMSetGlobalConstant.handle().invoke());
    //}
    //LLVMThreadLocalMode LLVMGetThreadLocalMode(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMGetThreadLocalMode.handle().invoke());
    //}
    //void LLVMSetThreadLocalMode(LLVMValueRef GlobalVar, LLVMThreadLocalMode Mode) {
    //    return nothrows_run(() -> Function.LLVMSetThreadLocalMode.handle().invoke());
    //}
    //boolean LLVMIsExternallyInitialized(LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMIsExternallyInitialized.handle().invoke());
    //}
    //void LLVMSetExternallyInitialized(LLVMValueRef GlobalVar, boolean IsExtInit) {
    //    return nothrows_run(() -> Function.LLVMSetExternallyInitialized.handle().invoke());
    //}

    /*
     * @defgroup LLVMCoreValueConstantGlobalAlias Global Aliases
     *
     * This group contains function that operate on global alias values.
     */

    //LLVMValueRef LLVMAddAlias(LLVMModuleRef M, LLVMTypeRef Ty, LLVMValueRef Aliasee, String Name) {
    //    return nothrows_run(() -> Function.LLVMAddAlias.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueFunction Function values
    // *
    // * Functions in this group operate on LLVMValueRef instances that
    // * correspond to llvm::Function instances.
    // *
    // * @see llvm::Function
    // *
    // * @{
    // */
    ///**
    // * Remove a function from its containing module and deletes it.
    // *
    // * @see llvm::Function::eraseFromParent()
    // */
    //void LLVMDeleteFunction(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMDeleteFunction.handle().invoke());
    //}
    ///**
    // * Check whether the given function has a personality function.
    // *
    // * @see llvm::Function::hasPersonalityFn()
    // */
    //boolean LLVMHasPersonalityFn(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMHasPersonalityFn.handle().invoke());
    //}
    ///**
    // * Obtain the personality function attached to the function.
    // *
    // * @see llvm::Function::getPersonalityFn()
    // */
    //LLVMValueRef LLVMGetPersonalityFn(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetPersonalityFn.handle().invoke());
    //}
    ///**
    // * Set the personality function attached to the function.
    // *
    // * @see llvm::Function::setPersonalityFn()
    // */
    //void LLVMSetPersonalityFn(LLVMValueRef Fn, LLVMValueRef PersonalityFn) {
    //    return nothrows_run(() -> Function.LLVMSetPersonalityFn.handle().invoke());
    //}
    ///**
    // * Obtain the ID number from a function instance.
    // *
    // * @see llvm::Function::getIntrinsicID()
    // */
    //int /* unsigned */ LLVMGetIntrinsicID(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetIntrinsicID.handle().invoke());
    //}

    /**
     * Obtain the calling function of a function.
     * <p>
     * The returned value corresponds to the LLVMCallConv enumeration.
     */
    public static LLVMCallConv LLVMGetFunctionCallConv(LLVMValueRef Fn) {
        return nothrows_run(() -> LLVMCallConv.of((int) Function.LLVMGetFunctionCallConv.handle().invoke(Fn.value())));
    }

    /**
     * Set the calling convention of a function.
     *
     * @param Fn Function to operate on
     * @param CC LLVMCallConv to set calling convention to
     */
    public static void LLVMSetFunctionCallConv(LLVMValueRef Fn, LLVMCallConv CC) {
        nothrows_run(() -> Function.LLVMSetFunctionCallConv.handle().invoke(Fn.value(), CC.value()));
    }

    ///**
    // * Obtain the name of the garbage collector to use during code
    // * generation.
    // *
    // * @see llvm::Function::getGC()
    // */
    //String LLVMGetGC(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetGC.handle().invoke());
    //}
    ///**
    // * Define the garbage collector to use during code generation.
    // *
    // * @see llvm::Function::setGC()
    // */
    //void LLVMSetGC(LLVMValueRef Fn, String Name) {
    //    return nothrows_run(() -> Function.LLVMSetGC.handle().invoke());
    //}

    /**
     * Add an attribute to a function.
     */
    public static void LLVMAddFunctionAttr(LLVMValueRef Fn, int /* LLVMAttribute */ PA) {
        nothrows_run(() -> Function.LLVMAddFunctionAttr.handle().invoke(Fn.value(), PA));
    }

    public static void LLVMAddAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, LLVMAttributeRef A) {
        nothrows_run(() -> Function.LLVMAddAttributeAtIndex.handle().invoke(F.value(), Idx, A.value()));
    }

    //LLVMAttributeRef LLVMGetEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return nothrows_run(() -> Function.LLVMGetEnumAttributeAtIndex.handle().invoke());
    //}
    //LLVMAttributeRef LLVMGetStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return nothrows_run(() -> Function.LLVMGetStringAttributeAtIndex.handle().invoke());
    //}
    //void LLVMRemoveEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return nothrows_run(() -> Function.LLVMRemoveEnumAttributeAtIndex.handle().invoke());
    //}
    //void LLVMRemoveStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return nothrows_run(() -> Function.LLVMRemoveStringAttributeAtIndex.handle().invoke());
    //}
    ///**
    // * Add a target-dependent attribute to a function
    // * @see llvm::AttrBuilder::addAttribute()
    // */
    //void LLVMAddTargetDependentFunctionAttr(LLVMValueRef Fn, String A, String V) {
    //    return nothrows_run(() -> Function.LLVMAddTargetDependentFunctionAttr.handle().invoke());
    //}
    ///**
    // * Obtain an attribute from a function.
    // *
    // * @see llvm::Function::getAttributes()
    // */
    //LLVMAttribute LLVMGetFunctionAttr(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetFunctionAttr.handle().invoke());
    //}
    ///**
    // * Remove an attribute from a function.
    // */
    //void LLVMRemoveFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA) {
    //    return nothrows_run(() -> Function.LLVMRemoveFunctionAttr.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreValueFunctionParameters Function Parameters
     *
     * Functions in this group relate to arguments/parameters on functions.
     *
     * Functions in this group expect LLVMValueRef instances that correspond
     * to llvm::Function instances.
     */

    /**
     * Obtain the number of parameters in a function.
     */
    public static int /* unsigned */ LLVMCountParams(LLVMValueRef Fn) {
        return nothrows_run(() -> (int) Function.LLVMCountParams.handle().invoke(Fn.value()));
    }

    /**
     * Obtain the parameters in a function.
     * <p>
     * The takes a pointer to a pre-allocated array of LLVMValueRef that is
     * at least LLVMCountParams() long. This array will be filled with
     * LLVMValueRef instances which correspond to the parameters the
     * function receives. Each LLVMValueRef corresponds to a llvm::Argument
     * instance.
     */
    /* package-private */
    static void LLVMGetParams(LLVMValueRef Fn, long Params) {
        nothrows_run(() -> Function.LLVMGetParams.handle().invoke(Fn.value(), Params));
    }

    /**
     * Obtain the parameters in a function.
     * <p>
     * Return array will be filled with LLVMValueRef instances which correspond
     * to the parameters the function receives. Each LLVMValueRef corresponds
     * to a llvm::Argument instance.
     */
    // Port-added
    public static LLVMValueRef[] LLVMGetParams(LLVMValueRef Fn) {
        int /* unsigned */ count = LLVMCountParams(Fn);
        if (count == 0) {
            return new LLVMValueRef[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Params = allocPointerArray(arena, count);
            LLVMGetParams(Fn, c_Params.nativeAddress());
            return readPointerArray(c_Params, LLVMValueRef.class, LLVMValueRef::ofNullable);
        }
    }

    /**
     * Obtain the parameter at the specified index.
     * <p>
     * Parameters are indexed from 0.
     */
    public static LLVMValueRef LLVMGetParam(LLVMValueRef Fn, int /* unsigned */ Index) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMGetParam.handle().invoke(Fn.value(), Index)));
    }

    ///**
    // * Obtain the function to which this argument belongs.
    // *
    // * Unlike other functions in this group, this one takes an LLVMValueRef
    // * that corresponds to a llvm::Attribute.
    // *
    // * The returned LLVMValueRef is the llvm::Function to which this
    // * argument belongs.
    // */
    //LLVMValueRef LLVMGetParamParent(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetParamParent.handle().invoke());
    //}
    ///**
    // * Obtain the first parameter to a function.
    // *
    // * @see llvm::Function::arg_begin()
    // */
    //LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetFirstParam.handle().invoke());
    //}
    ///**
    // * Obtain the last parameter to a function.
    // *
    // * @see llvm::Function::arg_end()
    // */
    //LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetLastParam.handle().invoke());
    //}
    ///**
    // * Obtain the next parameter to a function.
    // *
    // * This takes an LLVMValueRef obtained from LLVMGetFirstParam() (which is
    // * actually a wrapped iterator) and obtains the next parameter from the
    // * underlying iterator.
    // */
    //LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg) {
    //    return nothrows_run(() -> Function.LLVMGetNextParam.handle().invoke());
    //}
    ///**
    // * Obtain the previous parameter to a function.
    // *
    // * This is the opposite of LLVMGetNextParam().
    // */
    //LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg) {
    //    return nothrows_run(() -> Function.LLVMGetPreviousParam.handle().invoke());
    //}

    /**
     * Add an attribute to a function argument.
     */
    public static void LLVMAddAttribute(LLVMValueRef Arg, int /* LLVMAttribute */ PA) {
        nothrows_run(() -> Function.LLVMAddAttribute.handle().invoke(Arg.value(), PA));
    }

    ///**
    // * Remove an attribute from a function argument.
    // *
    // * @see llvm::Argument::removeAttr()
    // */
    //void LLVMRemoveAttribute(LLVMValueRef Arg, LLVMAttribute PA) {
    //    return nothrows_run(() -> Function.LLVMRemoveAttribute.handle().invoke());
    //}

    /**
     * Get an attribute from a function argument.
     */
    public static int /* LLVMAttribute */ LLVMGetAttribute(LLVMValueRef Arg) {
        return nothrows_run(() -> (int) Function.LLVMGetAttribute.handle().invoke(Arg.value()));
    }

    /**
     * Set the alignment for a function parameter.
     */
    public static void LLVMSetParamAlignment(LLVMValueRef Arg, int /* unsigned */ Align) {
        nothrows_run(() -> Function.LLVMSetParamAlignment.handle().invoke(Arg.value(), Align));
    }

    /*
     * @defgroup LLVMCCoreValueMetadata Metadata
     */

    ///**
    // * Obtain a MDString value from a context.
    // *
    // * The returned instance corresponds to the llvm::MDString class.
    // *
    // * The instance is specified by string data of a specified length. The
    // * string content is copied, so the backing memory can be freed after
    // * this function returns.
    // */
    //LLVMValueRef LLVMMDStringInContext(LLVMContextRef C, String Str, int /* unsigned */ SLen) {
    //    return nothrows_run(() -> Function.LLVMMDStringInContext.handle().invoke());
    //}
    ///**
    // * Obtain a MDString value from the global context.
    // */
    //LLVMValueRef LLVMMDString(String Str, int /* unsigned */ SLen) {
    //    return nothrows_run(() -> Function.LLVMMDString.handle().invoke());
    //}
    ///**
    // * Obtain a MDNode value from a context.
    // *
    // * The returned value corresponds to the llvm::MDNode class.
    // */
    //LLVMValueRef LLVMMDNodeInContext(LLVMContextRef C, LLVMValueRef *Vals, int /* unsigned */ Count) {
    //    return nothrows_run(() -> Function.LLVMMDNodeInContext.handle().invoke());
    //}
    ///**
    // * Obtain a MDNode value from the global context.
    // */
    //LLVMValueRef LLVMMDNode(LLVMValueRef *Vals, int /* unsigned */ Count) {
    //    return nothrows_run(() -> Function.LLVMMDNode.handle().invoke());
    //}
    ///**
    // * Obtain the underlying string from a MDString value.
    // *
    // * @param V Instance to obtain string from.
    // * @param Length Memory address which will hold length of returned string.
    // * @return String data in MDString.
    // */
    //String LLVMGetMDString(LLVMValueRef V, int /* unsigned */ *Length) {
    //    return nothrows_run(() -> Function.LLVMGetMDString.handle().invoke());
    //}
    ///**
    // * Obtain the number of operands from an MDNode value.
    // *
    // * @param V MDNode to get number of operands from.
    // * @return Number of operands of the MDNode.
    // */
    //int /* unsigned */ LLVMGetMDNodeNumOperands(LLVMValueRef V) {
    //    return nothrows_run(() -> Function.LLVMGetMDNodeNumOperands.handle().invoke());
    //}
    ///**
    // * Obtain the given MDNode's operands.
    // *
    // * The passed LLVMValueRef pointer should point to enough memory to hold all of
    // * the operands of the given MDNode (see LLVMGetMDNodeNumOperands) as
    // * LLVMValueRefs. This memory will be populated with the LLVMValueRefs of the
    // * MDNode's operands.
    // *
    // * @param V MDNode to get the operands from.
    // * @param Dest Destination array for operands.
    // */
    //void LLVMGetMDNodeOperands(LLVMValueRef V, LLVMValueRef *Dest) {
    //    return nothrows_run(() -> Function.LLVMGetMDNodeOperands.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueBasicBlock Basic Block
    // *
    // * A basic block represents a single entry single exit section of code.
    // * Basic blocks contain a list of instructions which form the body of
    // * the block.
    // *
    // * Basic blocks belong to functions. They have the type of label.
    // *
    // * Basic blocks are themselves values. However, the C API models them as
    // * LLVMBasicBlockRef.
    // *
    // * @see llvm::BasicBlock
    // *
    // * @{
    // */
    ///**
    // * Convert a basic block instance to a value type.
    // */
    //LLVMValueRef LLVMBasicBlockAsValue(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMBasicBlockAsValue.handle().invoke());
    //}
    ///**
    // * Determine whether an LLVMValueRef is itself a basic block.
    // */
    //boolean LLVMValueIsBasicBlock(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMValueIsBasicBlock.handle().invoke());
    //}
    ///**
    // * Convert an LLVMValueRef to an LLVMBasicBlockRef instance.
    // */
    //LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMValueAsBasicBlock.handle().invoke());
    //}
    ///**
    // * Obtain the string name of a basic block.
    // */
    //String LLVMGetBasicBlockName(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetBasicBlockName.handle().invoke());
    //}

    /**
     * Obtain the function to which a basic block belongs.
     */
    public static LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMGetBasicBlockParent.handle().invoke(BB.value())));
    }

    ///**
    // * Obtain the terminator instruction for a basic block.
    // *
    // * If the basic block does not have a terminator (it is not well-formed
    // * if it doesn't), then NULL is returned.
    // *
    // * The returned LLVMValueRef corresponds to a llvm::TerminatorInst.
    // *
    // * @see llvm::BasicBlock::getTerminator()
    // */
    //LLVMValueRef LLVMGetBasicBlockTerminator(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetBasicBlockTerminator.handle().invoke());
    //}
    ///**
    // * Obtain the number of basic blocks in a function.
    // *
    // * @param Fn Function value to operate on.
    // */
    //int /* unsigned */ LLVMCountBasicBlocks(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMCountBasicBlocks.handle().invoke());
    //}
    ///**
    // * Obtain all of the basic blocks in a function.
    // *
    // * This operates on a function value. The BasicBlocks parameter is a
    // * pointer to a pre-allocated array of LLVMBasicBlockRef of at least
    // * LLVMCountBasicBlocks() in length. This array is populated with
    // * LLVMBasicBlockRef instances.
    // */
    //void LLVMGetBasicBlocks(LLVMValueRef Fn, LLVMBasicBlockRef *BasicBlocks) {
    //    return nothrows_run(() -> Function.LLVMGetBasicBlocks.handle().invoke());
    //}
    ///**
    // * Obtain the first basic block in a function.
    // *
    // * The returned basic block can be used as an iterator. You will likely
    // * eventually call into LLVMGetNextBasicBlock() with it.
    // *
    // * @see llvm::Function::begin()
    // */
    //LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetFirstBasicBlock.handle().invoke());
    //}
    ///**
    // * Obtain the last basic block in a function.
    // *
    // * @see llvm::Function::end()
    // */
    //LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.LLVMGetLastBasicBlock.handle().invoke());
    //}
    ///**
    // * Advance a basic block iterator.
    // */
    //LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetNextBasicBlock.handle().invoke());
    //}
    ///**
    // * Go backwards in a basic block iterator.
    // */
    //LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetPreviousBasicBlock.handle().invoke());
    //}

    /**
     * Obtain the basic block that corresponds to the entry point of a
     * function.
     */
    public static LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn) {
        return nothrows_run(() -> LLVMBasicBlockRef.ofNullable((long) Function.LLVMGetEntryBasicBlock.handle().invoke(Fn.value())));
    }

    /**
     * Append a basic block to the end of a function.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C, LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMBasicBlockRef.ofNullable((long) Function.LLVMAppendBasicBlockInContext.handle()
                    .invoke(C.value(), Fn.value(), c_Name.nativeAddress())));
        }
    }

    /**
     * Append a basic block to the end of a function using the global
     * context.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMBasicBlockRef.ofNullable((long) Function.LLVMAppendBasicBlock.handle().invoke(Fn.value(), c_Name.nativeAddress())));
        }
    }

    ///**
    // * Insert a basic block in a function before another basic block.
    // *
    // * The function to add to is determined by the function of the
    // * passed basic block.
    // *
    // * @see llvm::BasicBlock::Create()
    // */
    //LLVMBasicBlockRef LLVMInsertBasicBlockInContext(LLVMContextRef C, LLVMBasicBlockRef BB, String Name) {
    //    return nothrows_run(() -> Function.LLVMInsertBasicBlockInContext.handle().invoke());
    //}
    ///**
    // * Insert a basic block in a function using the global context.
    // *
    // * @see llvm::BasicBlock::Create()
    // */
    //LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB, String Name) {
    //    return nothrows_run(() -> Function.LLVMInsertBasicBlock.handle().invoke());
    //}
    ///**
    // * Remove a basic block from a function and delete it.
    // *
    // * This deletes the basic block from its containing function and deletes
    // * the basic block itself.
    // *
    // * @see llvm::BasicBlock::eraseFromParent()
    // */
    //void LLVMDeleteBasicBlock(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMDeleteBasicBlock.handle().invoke());
    //}
    ///**
    // * Remove a basic block from a function.
    // *
    // * This deletes the basic block from its containing function but keep
    // * the basic block alive.
    // *
    // * @see llvm::BasicBlock::removeFromParent()
    // */
    //void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMRemoveBasicBlockFromParent.handle().invoke());
    //}
    ///**
    // * Move a basic block to before another one.
    // *
    // * @see llvm::BasicBlock::moveBefore()
    // */
    //void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
    //    return nothrows_run(() -> Function.LLVMMoveBasicBlockBefore.handle().invoke());
    //}
    ///**
    // * Move a basic block to after another one.
    // *
    // * @see llvm::BasicBlock::moveAfter()
    // */
    //void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
    //    return nothrows_run(() -> Function.LLVMMoveBasicBlockAfter.handle().invoke());
    //}
    ///**
    // * Obtain the first instruction in a basic block.
    // *
    // * The returned LLVMValueRef corresponds to a llvm::Instruction
    // * instance.
    // */
    //LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetFirstInstruction.handle().invoke());
    //}
    ///**
    // * Obtain the last instruction in a basic block.
    // *
    // * The returned LLVMValueRef corresponds to an LLVM:Instruction.
    // */
    //LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB) {
    //    return nothrows_run(() -> Function.LLVMGetLastInstruction.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstruction Instructions
    // *
    // * Functions in this group relate to the inspection and manipulation of
    // * individual instructions.
    // *
    // * In the C++ API, an instruction is modeled by llvm::Instruction. This
    // * class has a large number of descendents. llvm::Instruction is a
    // * llvm::Value and in the C API, instructions are modeled by
    // * LLVMValueRef.
    // *
    // * This group also contains sub-groups which operate on specific
    // * llvm::Instruction types, e.g. llvm::CallInst.
    // *
    // * @{
    // */
    ///**
    // * Determine whether an instruction has any metadata attached.
    // */
    //int LLVMHasMetadata(LLVMValueRef Val) {
    //    return nothrows_run(() -> Function.LLVMHasMetadata.handle().invoke());
    //}
    ///**
    // * Return metadata associated with an instruction value.
    // */
    //LLVMValueRef LLVMGetMetadata(LLVMValueRef Val, int /* unsigned */ KindID) {
    //    return nothrows_run(() -> Function.LLVMGetMetadata.handle().invoke());
    //}
    ///**
    // * Set metadata associated with an instruction value.
    // */
    //void LLVMSetMetadata(LLVMValueRef Val, int /* unsigned */ KindID, LLVMValueRef Node) {
    //    return nothrows_run(() -> Function.LLVMSetMetadata.handle().invoke());
    //}
    ///**
    // * Obtain the basic block to which an instruction belongs.
    // *
    // * @see llvm::Instruction::getParent()
    // */
    //LLVMBasicBlockRef LLVMGetInstructionParent(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetInstructionParent.handle().invoke());
    //}
    ///**
    // * Obtain the instruction that occurs after the one specified.
    // *
    // * The next instruction will be from the same basic block.
    // *
    // * If this is the last instruction in a basic block, NULL will be
    // * returned.
    // */
    //LLVMValueRef LLVMGetNextInstruction(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetNextInstruction.handle().invoke());
    //}
    ///**
    // * Obtain the instruction that occurred before this one.
    // *
    // * If the instruction is the first instruction in a basic block, NULL
    // * will be returned.
    // */
    //LLVMValueRef LLVMGetPreviousInstruction(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetPreviousInstruction.handle().invoke());
    //}
    ///**
    // * Remove and delete an instruction.
    // *
    // * The instruction specified is removed from its containing building
    // * block but is kept alive.
    // *
    // * @see llvm::Instruction::removeFromParent()
    // */
    //void LLVMInstructionRemoveFromParent(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMInstructionRemoveFromParent.handle().invoke());
    //}
    ///**
    // * Remove and delete an instruction.
    // *
    // * The instruction specified is removed from its containing building
    // * block and then deleted.
    // *
    // * @see llvm::Instruction::eraseFromParent()
    // */
    //void LLVMInstructionEraseFromParent(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMInstructionEraseFromParent.handle().invoke());
    //}
    ///**
    // * Obtain the code opcode for an individual instruction.
    // *
    // * @see llvm::Instruction::getOpCode()
    // */
    //LLVMOpcode LLVMGetInstructionOpcode(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetInstructionOpcode.handle().invoke());
    //}
    ///**
    // * Obtain the predicate of an instruction.
    // *
    // * This is only valid for instructions that correspond to llvm::ICmpInst
    // * or llvm::ConstantExpr whose opcode is llvm::Instruction::ICmp.
    // *
    // * @see llvm::ICmpInst::getPredicate()
    // */
    //LLVMIntPredicate LLVMGetICmpPredicate(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetICmpPredicate.handle().invoke());
    //}
    ///**
    // * Obtain the float predicate of an instruction.
    // *
    // * This is only valid for instructions that correspond to llvm::FCmpInst
    // * or llvm::ConstantExpr whose opcode is llvm::Instruction::FCmp.
    // *
    // * @see llvm::FCmpInst::getPredicate()
    // */
    //LLVMRealPredicate LLVMGetFCmpPredicate(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetFCmpPredicate.handle().invoke());
    //}
    ///**
    // * Create a copy of 'this' instruction that is identical in all ways
    // * except the following:
    // *   * The instruction has no parent
    // *   * The instruction has no name
    // *
    // * @see llvm::Instruction::clone()
    // */
    //LLVMValueRef LLVMInstructionClone(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMInstructionClone.handle().invoke());
    //}
    ///**
    // * @defgroup LLVMCCoreValueInstructionCall Call Sites and Invocations
    // *
    // * Functions in this group apply to instructions that refer to call
    // * sites and invocations. These correspond to C++ types in the
    // * llvm::CallInst class tree.
    // *
    // * @{
    // */
    ///**
    // * Obtain the argument count for a call instruction.
    // *
    // * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
    // * llvm::InvokeInst.
    // *
    // * @see llvm::CallInst::getNumArgOperands()
    // * @see llvm::InvokeInst::getNumArgOperands()
    // */
    //int /* unsigned */ LLVMGetNumArgOperands(LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMGetNumArgOperands.handle().invoke());
    //}
    ///**
    // * Set the calling convention for a call instruction.
    // *
    // * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
    // * llvm::InvokeInst.
    // *
    // * @see llvm::CallInst::setCallingConv()
    // * @see llvm::InvokeInst::setCallingConv()
    // */
    //void LLVMSetInstructionCallConv(LLVMValueRef Instr, int /* unsigned */ CC) {
    //    return nothrows_run(() -> Function.LLVMSetInstructionCallConv.handle().invoke());
    //}
    ///**
    // * Obtain the calling convention for a call instruction.
    // *
    // * This is the opposite of LLVMSetInstructionCallConv(). Reads its
    // * usage.
    // *
    // * @see LLVMSetInstructionCallConv()
    // */
    //int /* unsigned */ LLVMGetInstructionCallConv(LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMGetInstructionCallConv.handle().invoke());
    //}

    public static void LLVMAddInstrAttribute(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* LLVMAttribute */ PA) {
        nothrows_run(() -> Function.LLVMAddInstrAttribute.handle().invoke(Instr.value(), index, PA));
    }

    public static void LLVMRemoveInstrAttribute(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* LLVMAttribute */ PA) {
        nothrows_run(() -> Function.LLVMRemoveInstrAttribute.handle().invoke(Instr.value(), index, PA));
    }

    public static void LLVMSetInstrParamAlignment(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* unsigned */ Align) {
        nothrows_run(() -> Function.LLVMSetInstrParamAlignment.handle().invoke(Instr.value(), index, Align));
    }

    //void LLVMAddCallSiteAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, LLVMAttributeRef A) {
    //    return nothrows_run(() -> Function.LLVMAddCallSiteAttribute.handle().invoke());
    //}
    //LLVMAttributeRef LLVMGetCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return nothrows_run(() -> Function.LLVMGetCallSiteEnumAttribute.handle().invoke());
    //}
    //LLVMAttributeRef LLVMGetCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return nothrows_run(() -> Function.LLVMGetCallSiteStringAttribute.handle().invoke());
    //}
    //void LLVMRemoveCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return nothrows_run(() -> Function.LLVMRemoveCallSiteEnumAttribute.handle().invoke());
    //}
    //void LLVMRemoveCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return nothrows_run(() -> Function.LLVMRemoveCallSiteStringAttribute.handle().invoke());
    //}
    ///**
    // * Obtain the pointer to the function invoked by this instruction.
    // *
    // * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
    // * llvm::InvokeInst.
    // *
    // * @see llvm::CallInst::getCalledValue()
    // * @see llvm::InvokeInst::getCalledValue()
    // */
    //LLVMValueRef LLVMGetCalledValue(LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMGetCalledValue.handle().invoke());
    //}
    ///**
    // * Obtain whether a call instruction is a tail call.
    // *
    // * This only works on llvm::CallInst instructions.
    // *
    // * @see llvm::CallInst::isTailCall()
    // */
    //boolean LLVMIsTailCall(LLVMValueRef CallInst) {
    //    return nothrows_run(() -> Function.LLVMIsTailCall.handle().invoke());
    //}
    ///**
    // * Set whether a call instruction is a tail call.
    // *
    // * This only works on llvm::CallInst instructions.
    // *
    // * @see llvm::CallInst::setTailCall()
    // */
    //void LLVMSetTailCall(LLVMValueRef CallInst, boolean IsTailCall) {
    //    return nothrows_run(() -> Function.LLVMSetTailCall.handle().invoke());
    //}
    ///**
    // * Return the normal destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::getNormalDest()
    // */
    //LLVMBasicBlockRef LLVMGetNormalDest(LLVMValueRef InvokeInst) {
    //    return nothrows_run(() -> Function.LLVMGetNormalDest.handle().invoke());
    //}
    ///**
    // * Return the unwind destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::getUnwindDest()
    // */
    //LLVMBasicBlockRef LLVMGetUnwindDest(LLVMValueRef InvokeInst) {
    //    return nothrows_run(() -> Function.LLVMGetUnwindDest.handle().invoke());
    //}
    ///**
    // * Set the normal destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::setNormalDest()
    // */
    //void LLVMSetNormalDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
    //    return nothrows_run(() -> Function.LLVMSetNormalDest.handle().invoke());
    //}
    ///**
    // * Set the unwind destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::setUnwindDest()
    // */
    //void LLVMSetUnwindDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
    //    return nothrows_run(() -> Function.LLVMSetUnwindDest.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstructionTerminator Terminators
    // *
    // * Functions in this group only apply to instructions that map to
    // * llvm::TerminatorInst instances.
    // *
    // * @{
    // */
    ///**
    // * Return the number of successors that this terminator has.
    // *
    // * @see llvm::TerminatorInst::getNumSuccessors
    // */
    //int /* unsigned */ LLVMGetNumSuccessors(LLVMValueRef Term) {
    //    return nothrows_run(() -> Function.LLVMGetNumSuccessors.handle().invoke());
    //}
    ///**
    // * Return the specified successor.
    // *
    // * @see llvm::TerminatorInst::getSuccessor
    // */
    //LLVMBasicBlockRef LLVMGetSuccessor(LLVMValueRef Term, int /* unsigned */ i) {
    //    return nothrows_run(() -> Function.LLVMGetSuccessor.handle().invoke());
    //}
    ///**
    // * Update the specified successor to point at the provided block.
    // *
    // * @see llvm::TerminatorInst::setSuccessor
    // */
    //void LLVMSetSuccessor(LLVMValueRef Term, int /* unsigned */ i, LLVMBasicBlockRef block) {
    //    return nothrows_run(() -> Function.LLVMSetSuccessor.handle().invoke());
    //}
    ///**
    // * Return if a branch is conditional.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::isConditional
    // */
    //boolean LLVMIsConditional(LLVMValueRef Branch) {
    //    return nothrows_run(() -> Function.LLVMIsConditional.handle().invoke());
    //}
    ///**
    // * Return the condition of a branch instruction.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::getCondition
    // */
    //LLVMValueRef LLVMGetCondition(LLVMValueRef Branch) {
    //    return nothrows_run(() -> Function.LLVMGetCondition.handle().invoke());
    //}
    ///**
    // * Set the condition of a branch instruction.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::setCondition
    // */
    //void LLVMSetCondition(LLVMValueRef Branch, LLVMValueRef Cond) {
    //    return nothrows_run(() -> Function.LLVMSetCondition.handle().invoke());
    //}
    ///**
    // * Obtain the default destination basic block of a switch instruction.
    // *
    // * This only works on llvm::SwitchInst instructions.
    // *
    // * @see llvm::SwitchInst::getDefaultDest()
    // */
    //LLVMBasicBlockRef LLVMGetSwitchDefaultDest(LLVMValueRef SwitchInstr) {
    //    return nothrows_run(() -> Function.LLVMGetSwitchDefaultDest.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstructionAlloca Allocas
    // *
    // * Functions in this group only apply to instructions that map to
    // * llvm::AllocaInst instances.
    // *
    // * @{
    // */
    ///**
    // * Obtain the type that is being allocated by the alloca instruction.
    // */
    //LLVMTypeRef LLVMGetAllocatedType(LLVMValueRef Alloca) {
    //    return nothrows_run(() -> Function.LLVMGetAllocatedType.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstructionGetElementPointer GEPs
    // *
    // * Functions in this group only apply to instructions that map to
    // * llvm::GetElementPtrInst instances.
    // *
    // * @{
    // */
    ///**
    // * Check whether the given GEP instruction is inbounds.
    // */
    //boolean LLVMIsInBounds(LLVMValueRef GEP) {
    //    return nothrows_run(() -> Function.LLVMIsInBounds.handle().invoke());
    //}
    ///**
    // * Set the given GEP instruction to be inbounds or not.
    // */
    //void LLVMSetIsInBounds(LLVMValueRef GEP, boolean InBounds) {
    //    return nothrows_run(() -> Function.LLVMSetIsInBounds.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstructionPHINode PHI Nodes
    // *
    // * Functions in this group only apply to instructions that map to
    // * llvm::PHINode instances.
    // *
    // * @{
    // */
    ///**
    // * Add an incoming value to the end of a PHI list.
    // */
    //void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef *IncomingValues, LLVMBasicBlockRef *IncomingBlocks, int /* unsigned */ Count) {
    //    return nothrows_run(() -> Function.LLVMAddIncoming.handle().invoke());
    //}
    ///**
    // * Obtain the number of incoming basic blocks to a PHI node.
    // */
    //int /* unsigned */ LLVMCountIncoming(LLVMValueRef PhiNode) {
    //    return nothrows_run(() -> Function.LLVMCountIncoming.handle().invoke());
    //}
    ///**
    // * Obtain an incoming value to a PHI node as an LLVMValueRef.
    // */
    //LLVMValueRef LLVMGetIncomingValue(LLVMValueRef PhiNode, int /* unsigned */ Index) {
    //    return nothrows_run(() -> Function.LLVMGetIncomingValue.handle().invoke());
    //}
    ///**
    // * Obtain an incoming value to a PHI node as an LLVMBasicBlockRef.
    // */
    //LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, int /* unsigned */ Index) {
    //    return nothrows_run(() -> Function.LLVMGetIncomingBlock.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @defgroup LLVMCCoreValueInstructionExtractValue ExtractValue
    // * @defgroup LLVMCCoreValueInstructionInsertValue InsertValue
    // *
    // * Functions in this group only apply to instructions that map to
    // * llvm::ExtractValue and llvm::InsertValue instances.
    // *
    // * @{
    // */
    ///**
    // * Obtain the number of indices.
    // * NB: This also works on GEP.
    // */
    //int /* unsigned */ LLVMGetNumIndices(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.LLVMGetNumIndices.handle().invoke());
    //}
    ///**
    // * Obtain the indices as an array.
    // */
    //const int /* unsigned */ *LLVMGetIndices(LLVMValueRef Inst) {
    //    return nothrows_run(() -> Function.*LLVMGetIndices.handle().invoke());
    //}
    ///**
    // * @}
    // */
    ///**
    // * @}
    // */
    ///**
    // * @}
    // */

    /*
     * @defgroup LLVMCCoreInstructionBuilder Instruction Builders
     *
     * An instruction builder represents a point within a basic block and is
     * the exclusive means of building instructions using the C interface.
     */

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        return nothrows_run(() -> LLVMBuilderRef.ofNullable((long) Function.LLVMCreateBuilderInContext.handle().invoke(C.value())));
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        return nothrows_run(() -> LLVMBuilderRef.ofNullable((long) Function.LLVMCreateBuilder.handle().invoke()));
    }

    //void LLVMPositionBuilder(LLVMBuilderRef Builder, LLVMBasicBlockRef Block, LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMPositionBuilder.handle().invoke());
    //}
    //void LLVMPositionBuilderBefore(LLVMBuilderRef Builder, LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMPositionBuilderBefore.handle().invoke());
    //}

    public static void LLVMPositionBuilderAtEnd(LLVMBuilderRef Builder, LLVMBasicBlockRef Block) {
        nothrows_run(() -> Function.LLVMPositionBuilderAtEnd.handle().invoke(Builder.value(), Block.value()));
    }

    public static LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef Builder) {
        return nothrows_run(() -> LLVMBasicBlockRef.ofNullable((long) Function.LLVMGetInsertBlock.handle().invoke(Builder.value())));
    }

    //void LLVMClearInsertionPosition(LLVMBuilderRef Builder) {
    //    return nothrows_run(() -> Function.LLVMClearInsertionPosition.handle().invoke());
    //}
    //void LLVMInsertIntoBuilder(LLVMBuilderRef Builder, LLVMValueRef Instr) {
    //    return nothrows_run(() -> Function.LLVMInsertIntoBuilder.handle().invoke());
    //}
    //void LLVMInsertIntoBuilderWithName(LLVMBuilderRef Builder, LLVMValueRef Instr, String Name) {
    //    return nothrows_run(() -> Function.LLVMInsertIntoBuilderWithName.handle().invoke());
    //}

    public static void LLVMDisposeBuilder(LLVMBuilderRef Builder) {
        nothrows_run(() -> Function.LLVMDisposeBuilder.handle().invoke(Builder.value()));
    }

    /* Metadata */

    public static void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L) {
        nothrows_run(() -> Function.LLVMSetCurrentDebugLocation.handle().invoke(Builder.value(), L.value()));
    }

    public static LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMGetCurrentDebugLocation.handle().invoke(Builder.value())));
    }

    public static void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst) {
        nothrows_run(() -> Function.LLVMSetInstDebugLocation.handle().invoke(Builder.value(), Inst.value()));
    }

    /* Terminators */

    public static LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef Builder) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildRetVoid.handle().invoke(Builder.value())));
    }

    public static LLVMValueRef LLVMBuildRet(LLVMBuilderRef Builder, LLVMValueRef V) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildRet.handle().invoke(Builder.value(), V.value())));
    }

    //LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef B, LLVMValueRef *RetVals, int /* unsigned */ N) {
    //    return nothrows_run(() -> Function.LLVMBuildAggregateRet.handle().invoke());
    //}

    public static LLVMValueRef LLVMBuildBr(LLVMBuilderRef B, LLVMBasicBlockRef Dest) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildBr.handle().invoke(B.value(), Dest.value())));
    }

    public static LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef B, LLVMValueRef If, LLVMBasicBlockRef Then, LLVMBasicBlockRef Else) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildCondBr.handle().invoke(B.value(), If.value(), Then.value(), Else.value())));
    }

    //LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef B, LLVMValueRef V, LLVMBasicBlockRef Else, int /* unsigned */ NumCases) {
    //    return nothrows_run(() -> Function.LLVMBuildSwitch.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr, int /* unsigned */ NumDests) {
    //    return nothrows_run(() -> Function.LLVMBuildIndirectBr.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef *Args, int /* unsigned */ NumArgs, LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildInvoke.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef PersFn, int /* unsigned */ NumClauses, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildLandingPad.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn) {
    //    return nothrows_run(() -> Function.LLVMBuildResume.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef) {
    //    return nothrows_run(() -> Function.LLVMBuildUnreachable.handle().invoke());
    //}
    ///* Add a case to the switch instruction */
    //void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal, LLVMBasicBlockRef Dest) {
    //    return nothrows_run(() -> Function.LLVMAddCase.handle().invoke());
    //}
    ///* Add a destination to the indirectbr instruction */
    //void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest) {
    //    return nothrows_run(() -> Function.LLVMAddDestination.handle().invoke());
    //}
    ///* Get the number of clauses on the landingpad instruction */
    //int /* unsigned */ LLVMGetNumClauses(LLVMValueRef LandingPad) {
    //    return nothrows_run(() -> Function.LLVMGetNumClauses.handle().invoke());
    //}
    ///* Get the value of the clause at idnex Idx on the landingpad instruction */
    //LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, int /* unsigned */ Idx) {
    //    return nothrows_run(() -> Function.LLVMGetClause.handle().invoke());
    //}
    ///* Add a catch or filter clause to the landingpad instruction */
    //void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal) {
    //    return nothrows_run(() -> Function.LLVMAddClause.handle().invoke());
    //}
    ///* Get the 'cleanup' flag in the landingpad instruction */
    //boolean LLVMIsCleanup(LLVMValueRef LandingPad) {
    //    return nothrows_run(() -> Function.LLVMIsCleanup.handle().invoke());
    //}
    ///* Set the 'cleanup' flag in the landingpad instruction */
    //void LLVMSetCleanup(LLVMValueRef LandingPad, boolean Val) {
    //    return nothrows_run(() -> Function.LLVMSetCleanup.handle().invoke());
    //}

    /* Arithmetic */

    public static LLVMValueRef LLVMBuildAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildAdd.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNSWAdd.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNUWAdd.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFAdd.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSub.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNSWSub.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNUWSub.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFSub.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildMul.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNSWMul.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNUWMul.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFMul.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildUDiv.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSDiv.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildExactSDiv.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFDiv.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildURem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildURem.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSRem.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFRem.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildShl(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildShl.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildLShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildLShr.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildAShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildAShr.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildAnd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildAnd.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildOr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildOr.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildXor(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildXor.handle()
                    .invoke(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildBinOp.handle()
                    .invoke(B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNeg.handle()
                    .invoke(B.value(), V.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNSWNeg.handle()
                    .invoke(B.value(), V.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNUWNeg.handle()
                    .invoke(B.value(), V.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFNeg.handle()
                    .invoke(B.value(), V.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildNot(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildNot.handle()
                    .invoke(B.value(), V.value(), c_Name.nativeAddress())));
        }
    }

    /* Memory */

    //LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildMalloc.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildArrayMalloc.handle().invoke());
    //}

    public static LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildAlloca.handle()
                    .invoke(B.value(), Ty.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildArrayAlloca.handle()
                    .invoke(B.value(), Ty.value(), Val.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFree(LLVMBuilderRef B, LLVMValueRef Ptr) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFree.handle().invoke(B.value(), Ptr.value())));
    }

    public static LLVMValueRef LLVMBuildLoad(LLVMBuilderRef B, LLVMValueRef Ptr, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildLoad.handle().invoke(B.value(), Ptr.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildStore(LLVMBuilderRef B, LLVMValueRef Val, LLVMValueRef Ptr) {
        return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildStore.handle().invoke(B.value(), Val.value(), Ptr.value())));
    }

    //LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef *Indices, int /* unsigned */ NumIndices, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildGEP.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef *Indices, int /* unsigned */ NumIndices, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildInBoundsGEP.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer, int /* unsigned */ Idx, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildStructGEP.handle().invoke());
    //}

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildGlobalString.handle()
                    .invoke(B.value(), c_Str.nativeAddress(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildGlobalStringPtr.handle()
                    .invoke(B.value(), c_Str.nativeAddress(), c_Name.nativeAddress())));
        }
    }

    public static boolean LLVMGetVolatile(LLVMValueRef MemoryAccessInst) {
        return nothrows_run(() -> (boolean) Function.LLVMGetVolatile.handle().invoke(MemoryAccessInst.value()));
    }

    public static void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, boolean IsVolatile) {
        nothrows_run(() -> Function.LLVMSetVolatile.handle().invoke(MemoryAccessInst.value(), IsVolatile));
    }

    public static LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst) {
        return nothrows_run(() -> LLVMAtomicOrdering.of((int) Function.LLVMGetOrdering.handle().invoke(MemoryAccessInst.value())));
    }

    public static void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering) {
        nothrows_run(() -> Function.LLVMSetOrdering.handle().invoke(MemoryAccessInst.value(), Ordering.value()));
    }

    /* Casts */

    public static LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildTrunc.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildZExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildZExt.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSExt.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFPToUI.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFPToSI.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildUIToFP.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSIToFP.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFPTrunc.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFPExt.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildPtrToInt.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildIntToPtr.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildBitCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildAddrSpaceCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildZExtOrBitCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildSExtOrBitCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildTruncOrBitCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildPointerCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildIntCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFPCast.handle()
                    .invoke(B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress())));
        }
    }

    /* Comparisons */

    public static LLVMValueRef LLVMBuildICmp(LLVMBuilderRef B, LLVMIntPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildICmp.handle()
                    .invoke(B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef B, LLVMRealPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildFCmp.handle()
                    .invoke(B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress())));
        }
    }

    /* Miscellaneous instructions */

    public static LLVMValueRef LLVMBuildPhi(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildPhi.handle()
                    .invoke(B.value(), Ty.value(), c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildCall(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildCall.handle()
                    .invoke(B.value(), Fn.value(), c_Args.nativeAddress(), NumArgs, c_Name.nativeAddress())));
        }
    }

    //LLVMValueRef LLVMBuildSelect(LLVMBuilderRef B, LLVMValueRef If, LLVMValueRef Then, LLVMValueRef Else, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildSelect.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef B, LLVMValueRef List, LLVMTypeRef Ty, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildVAArg.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef Index, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildExtractElement.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef EltVal, LLVMValueRef Index, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildInsertElement.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef B, LLVMValueRef V1, LLVMValueRef V2, LLVMValueRef Mask, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildShuffleVector.handle().invoke());
    //}

    public static LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef B, LLVMValueRef AggVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildExtractValue.handle()
                    .invoke(B.value(), AggVal.value(), Index, c_Name.nativeAddress())));
        }
    }

    public static LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef B, LLVMValueRef AggVal, LLVMValueRef EltVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMValueRef.ofNullable((long) Function.LLVMBuildInsertValue.handle()
                    .invoke(B.value(), AggVal.value(), EltVal.value(), Index, c_Name.nativeAddress())));
        }
    }

    //LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildIsNull.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildIsNotNull.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildPtrDiff.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering ordering, boolean singleThread, String Name) {
    //    return nothrows_run(() -> Function.LLVMBuildFence.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildAtomicRMW(LLVMBuilderRef B, LLVMAtomicRMWBinOp op, LLVMValueRef PTR, LLVMValueRef Val, LLVMAtomicOrdering ordering, boolean singleThread) {
    //    return nothrows_run(() -> Function.LLVMBuildAtomicRMW.handle().invoke());
    //}
    //LLVMValueRef LLVMBuildAtomicCmpXchg(LLVMBuilderRef B, LLVMValueRef Ptr, LLVMValueRef Cmp, LLVMValueRef New, LLVMAtomicOrdering SuccessOrdering, LLVMAtomicOrdering FailureOrdering, boolean SingleThread) {
    //    return nothrows_run(() -> Function.LLVMBuildAtomicCmpXchg.handle().invoke());
    //}
    //boolean LLVMIsAtomicSingleThread(LLVMValueRef AtomicInst) {
    //    return nothrows_run(() -> Function.LLVMIsAtomicSingleThread.handle().invoke());
    //}
    //void LLVMSetAtomicSingleThread(LLVMValueRef AtomicInst, boolean SingleThread) {
    //    return nothrows_run(() -> Function.LLVMSetAtomicSingleThread.handle().invoke());
    //}
    //LLVMAtomicOrdering LLVMGetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst) {
    //    return nothrows_run(() -> Function.LLVMGetCmpXchgSuccessOrdering.handle().invoke());
    //}
    //void LLVMSetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
    //    return nothrows_run(() -> Function.LLVMSetCmpXchgSuccessOrdering.handle().invoke());
    //}
    //LLVMAtomicOrdering LLVMGetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst) {
    //    return nothrows_run(() -> Function.LLVMGetCmpXchgFailureOrdering.handle().invoke());
    //}
    //void LLVMSetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
    //    return nothrows_run(() -> Function.LLVMSetCmpXchgFailureOrdering.handle().invoke());
    //}

    /*
     * @defgroup LLVMCCoreModuleProvider Module Providers
     */

    /**
     * Changes the type of M so it can be passed to FunctionPassManagers and the
     * JIT. They take ModuleProviders for historical reasons.
     */
    public static LLVMModuleProviderRef LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M) {
        return nothrows_run(() -> LLVMModuleProviderRef.ofNullable((long) Function.LLVMCreateModuleProviderForExistingModule.handle().invoke(M.value())));
    }

    /**
     * Destroys the module M.
     */
    public static void LLVMDisposeModuleProvider(LLVMModuleProviderRef M) {
        nothrows_run(() -> Function.LLVMDisposeModuleProvider.handle().invoke(M.value()));
    }

    /*
     * @defgroup LLVMCCoreMemoryBuffers Memory Buffers
     */

    //boolean LLVMCreateMemoryBufferWithContentsOfFile(String Path, LLVMMemoryBufferRef *OutMemBuf, LLVMString *OutMessage) {
    //    return nothrows_run(() -> Function.LLVMCreateMemoryBufferWithContentsOfFile.handle().invoke());
    //}
    //boolean LLVMCreateMemoryBufferWithSTDIN(LLVMMemoryBufferRef *OutMemBuf, LLVMString *OutMessage) {
    //    return nothrows_run(() -> Function.LLVMCreateMemoryBufferWithSTDIN.handle().invoke());
    //}

    /* package-private */
    static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRange(
            long InputData, long /* size_t */ InputDataLength,
            String BufferName, boolean RequiresNullTerminator) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BufferName = allocString(arena, BufferName);
            return nothrows_run(() -> LLVMMemoryBufferRef.ofNullable((long) Function.LLVMCreateMemoryBufferWithMemoryRange
                    .handle().invoke(InputData, InputDataLength, c_BufferName.nativeAddress(), RequiresNullTerminator)));
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegment(
            MemorySegment InputData, String BufferName, boolean RequiresNullTerminator) {
        return LLVMCreateMemoryBufferWithMemoryRange(InputData.nativeAddress(),
                InputData.byteSize(), BufferName, RequiresNullTerminator);
    }

    /* package-private */
    static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRangeCopy(
            long InputData, long /* size_t */ InputDataLength, String BufferName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BufferName = allocString(arena, BufferName);
            return nothrows_run(() -> LLVMMemoryBufferRef.ofNullable(
                    (long) Function.LLVMCreateMemoryBufferWithMemoryRangeCopy.handle()
                            .invoke(InputData, InputDataLength, c_BufferName.nativeAddress())));
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegmentCopy(
            MemorySegment InputData, String BufferName) {
        return LLVMCreateMemoryBufferWithMemoryRangeCopy(
                InputData.nativeAddress(), InputData.byteSize(), BufferName);
    }

    /* package-private */
    static long LLVMGetBufferStart(LLVMMemoryBufferRef MemBuf) {
        return nothrows_run(() -> (long) Function.LLVMGetBufferStart.handle().invoke(MemBuf.value()));
    }

    public static long /* size_t */ LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf) {
        return nothrows_run(() -> (long) Function.LLVMGetBufferSize.handle().invoke(MemBuf.value()));
    }

    // Port-added
    public static MemorySegment LLVMGetBufferSegment(LLVMMemoryBufferRef MemBuf) {
        long address = LLVMGetBufferStart(MemBuf);
        long size = LLVMGetBufferSize(MemBuf);
        return MemorySegment.ofAddress(address).reinterpret(size).asReadOnly();
    }

    public static void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf) {
        nothrows_run(() -> Function.LLVMDisposeMemoryBuffer.handle().invoke(MemBuf.value()));
    }

    /*
     * @defgroup LLVMCCorePassRegistry Pass Registry
     */

    /**
     * Return the global pass registry, for use with initialization functions.
     */
    public static LLVMPassRegistryRef LLVMGetGlobalPassRegistry() {
        return nothrows_run(() -> LLVMPassRegistryRef.ofNullable((long) Function.LLVMGetGlobalPassRegistry.handle().invoke()));
    }

    /*
     * @defgroup LLVMCCorePassManagers Pass Managers
     */

    ///** Constructs a new whole-module pass pipeline. This type of pipeline is
    //    suitable for link-time optimization and whole-module transformations.
    //    @see llvm::PassManager::PassManager */
    //LLVMPassManagerRef LLVMCreatePassManager() {
    //    return nothrows_run(() -> Function.LLVMCreatePassManager.handle().invoke());
    //}
    ///** Constructs a new function-by-function pass pipeline over the module
    //    provider. It does not take ownership of the module provider. This type of
    //    pipeline is suitable for code generation and JIT compilation tasks.
    //    @see llvm::FunctionPassManager::FunctionPassManager */
    //LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMCreateFunctionPassManagerForModule.handle().invoke());
    //}
    ///** Deprecated: Use LLVMCreateFunctionPassManagerForModule instead. */
    //LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP) {
    //    return nothrows_run(() -> Function.LLVMCreateFunctionPassManager.handle().invoke());
    //}
    ///** Initializes, executes on the provided module, and finalizes all of the
    //    passes scheduled in the pass manager. Returns 1 if any of the passes
    //    modified the module, 0 otherwise.
    //    @see llvm::PassManager::run(Module&) */
    //boolean LLVMRunPassManager(LLVMPassManagerRef PM, LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMRunPassManager.handle().invoke());
    //}
    ///** Initializes all of the function passes scheduled in the function pass
    //    manager. Returns 1 if any of the passes modified the module, 0 otherwise.
    //    @see llvm::FunctionPassManager::doInitialization */
    //boolean LLVMInitializeFunctionPassManager(LLVMPassManagerRef FPM) {
    //    return nothrows_run(() -> Function.LLVMInitializeFunctionPassManager.handle().invoke());
    //}
    ///** Executes all of the function passes scheduled in the function pass manager
    //    on the provided function. Returns 1 if any of the passes modified the
    //    function, false otherwise.
    //    @see llvm::FunctionPassManager::run(Function&) */
    //boolean LLVMRunFunctionPassManager(LLVMPassManagerRef FPM, LLVMValueRef F) {
    //    return nothrows_run(() -> Function.LLVMRunFunctionPassManager.handle().invoke());
    //}
    ///** Finalizes all of the function passes scheduled in in the function pass
    //    manager. Returns 1 if any of the passes modified the module, 0 otherwise.
    //    @see llvm::FunctionPassManager::doFinalization */
    //boolean LLVMFinalizeFunctionPassManager(LLVMPassManagerRef FPM) {
    //    return nothrows_run(() -> Function.LLVMFinalizeFunctionPassManager.handle().invoke());
    //}

    /**
     * Frees the memory of a pass pipeline. For function pipelines, does not free
     * the module provider.
     */
    public static void LLVMDisposePassManager(LLVMPassManagerRef PM) {
        nothrows_run(() -> Function.LLVMDisposePassManager.handle().invoke(PM.value()));
    }

    /*
     * @defgroup LLVMCCoreThreading Threading
     *
     * Handle the structures needed to make LLVM safe for multithreading.
     */

    ///** Deprecated: Multi-threading can only be enabled/disabled with the compile
    //    time define LLVM_ENABLE_THREADS.  This function always returns
    //    LLVMIsMultithreaded(). */
    //boolean LLVMStartMultithreaded() {
    //    return nothrows_run(() -> Function.LLVMStartMultithreaded.handle().invoke());
    //}
    ///** Deprecated: Multi-threading can only be enabled/disabled with the compile
    //    time define LLVM_ENABLE_THREADS. */
    //void LLVMStopMultithreaded() {
    //    return nothrows_run(() -> Function.LLVMStopMultithreaded.handle().invoke());
    //}
    ///** Check whether LLVM is executing in thread-safe mode or not.
    //    @see llvm::llvm_is_multithreaded */
    //boolean LLVMIsMultithreaded() {
    //    return nothrows_run(() -> Function.LLVMIsMultithreaded.handle().invoke());
    //}
}

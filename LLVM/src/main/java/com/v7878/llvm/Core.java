package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBasicBlockRef;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.allocPointerArray;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.llvm._Utils.arrayLength;
import static com.v7878.llvm._Utils.readPointerArray;
import static com.v7878.llvm._Utils.stringLength;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import com.v7878.llvm.Types.LLVMModuleProviderRef;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

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
        // Port-added
        public static final int LLVMAttributeFirstArgIndex = 1;
    }

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMInitializeCore")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeCore(long R);

        @LibrarySymbol(name = "LLVMShutdown")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMShutdown();

        @LibrarySymbol(name = "LLVMCreateMessage")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateMessage(long Message);

        @LibrarySymbol(name = "LLVMDisposeMessage")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeMessage(long Message);

        @LibrarySymbol(name = "LLVMContextCreate")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMContextCreate();

        @LibrarySymbol(name = "LLVMGetGlobalContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMGetGlobalContext();

        /*@LibrarySymbol("LLVMContextSetDiagnosticHandler")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMContextSetDiagnosticHandler(long, long, long);

        @LibrarySymbol("LLVMContextGetDiagnosticHandler")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMContextGetDiagnosticHandler(long);

        @LibrarySymbol("LLVMContextGetDiagnosticContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMContextGetDiagnosticContext(long);

        @LibrarySymbol("LLVMContextSetYieldCallback")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMContextSetYieldCallback(long, long, long);*/

        @LibrarySymbol(name = "LLVMContextDispose")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMContextDispose(long C);

        @LibrarySymbol(name = "LLVMGetDiagInfoDescription")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetDiagInfoDescription(long DI);

        @LibrarySymbol(name = "LLVMGetDiagInfoSeverity")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetDiagInfoSeverity(long DI);

        @LibrarySymbol(name = "LLVMGetMDKindIDInContext")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int LLVMGetMDKindIDInContext(long C, long Name, int SLen);

        @LibrarySymbol(name = "LLVMGetMDKindID")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, INT})
        abstract int LLVMGetMDKindID(long Name, int SLen);

        @LibrarySymbol(name = "LLVMGetEnumAttributeKindForName")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMGetEnumAttributeKindForName(long Name, long SLen);

        @LibrarySymbol(name = "LLVMGetLastEnumAttributeKind")
        @CallSignature(type = CRITICAL, ret = INT, args = {})
        abstract int LLVMGetLastEnumAttributeKind();

        @LibrarySymbol(name = "LLVMCreateEnumAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG})
        abstract long LLVMCreateEnumAttribute(long C, int KindID, long Val);

        @LibrarySymbol(name = "LLVMGetEnumAttributeKind")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetEnumAttributeKind(long A);

        @LibrarySymbol(name = "LLVMGetEnumAttributeValue")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetEnumAttributeValue(long A);

        @LibrarySymbol(name = "LLVMCreateStringAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract long LLVMCreateStringAttribute(long C, long K, int KLength, long V, int VLength);

        @LibrarySymbol(name = "LLVMGetStringAttributeKind")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetStringAttributeKind(long A, long Length);

        @LibrarySymbol(name = "LLVMGetStringAttributeValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetStringAttributeValue(long A, long Length);

        @LibrarySymbol(name = "LLVMIsEnumAttribute")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsEnumAttribute(long A);

        @LibrarySymbol(name = "LLVMIsStringAttribute")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsStringAttribute(long A);

        @LibrarySymbol(name = "LLVMModuleCreateWithName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMModuleCreateWithName(long ModuleID);

        @LibrarySymbol(name = "LLVMModuleCreateWithNameInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMModuleCreateWithNameInContext(long ModuleID, long C);

        @LibrarySymbol(name = "LLVMCloneModule")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCloneModule(long M);

        @LibrarySymbol(name = "LLVMDisposeModule")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeModule(long M);

        /*@LibrarySymbol("LLVMGetModuleIdentifier")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetModuleIdentifier(long, long);*/

        @LibrarySymbol(name = "LLVMSetModuleIdentifier")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetModuleIdentifier(long M, long Ident, long Len);

        @LibrarySymbol(name = "LLVMGetDataLayoutStr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetDataLayoutStr(long M);

        @LibrarySymbol(name = "LLVMGetDataLayout")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetDataLayout(long M);

        @LibrarySymbol(name = "LLVMSetDataLayout")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetDataLayout(long M, long DataLayoutStr);

        @LibrarySymbol(name = "LLVMGetTarget")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTarget(long M);

        @LibrarySymbol(name = "LLVMSetTarget")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetTarget(long M, long Triple);

        @LibrarySymbol(name = "LLVMDumpModule")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDumpModule(long M);

        /*@LibrarySymbol("LLVMPrintModuleToFile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMPrintModuleToFile(long, long, long);*/

        @LibrarySymbol(name = "LLVMPrintModuleToString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPrintModuleToString(long M);

        /*@LibrarySymbol("LLVMSetModuleInlineAsm")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetModuleInlineAsm(long, long);*/

        @LibrarySymbol(name = "LLVMGetModuleContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetModuleContext(long M);

        /*@LibrarySymbol("LLVMGetTypeByName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetTypeByName(long, long);

        @LibrarySymbol("LLVMGetNamedMetadataNumOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMGetNamedMetadataNumOperands(long, long);

        @LibrarySymbol("LLVMGetNamedMetadataOperands")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetNamedMetadataOperands(long, long, long);

        @LibrarySymbol("LLVMAddNamedMetadataOperand")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddNamedMetadataOperand(long, long, long);*/

        @LibrarySymbol(name = "LLVMAddFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddFunction(long M, long Name, long FunctionTy);

        @LibrarySymbol(name = "LLVMGetNamedFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetNamedFunction(long M, long Name);

        /*@LibrarySymbol("LLVMGetFirstFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstFunction(long);

        @LibrarySymbol("LLVMGetLastFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastFunction(long);

        @LibrarySymbol("LLVMGetNextFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextFunction(long);

        @LibrarySymbol("LLVMGetPreviousFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousFunction(long);*/

        @LibrarySymbol(name = "LLVMGetTypeKind")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetTypeKind(long Ty);

        @LibrarySymbol(name = "LLVMTypeIsSized")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMTypeIsSized(long Ty);

        @LibrarySymbol(name = "LLVMGetTypeContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTypeContext(long Ty);

        @LibrarySymbol(name = "LLVMDumpType")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDumpType(long Val);

        @LibrarySymbol(name = "LLVMPrintTypeToString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPrintTypeToString(long Val);

        @LibrarySymbol(name = "LLVMInt1TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt1TypeInContext(long C);

        @LibrarySymbol(name = "LLVMInt8TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt8TypeInContext(long C);

        @LibrarySymbol(name = "LLVMInt16TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt16TypeInContext(long C);

        @LibrarySymbol(name = "LLVMInt32TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt32TypeInContext(long C);

        @LibrarySymbol(name = "LLVMInt64TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt64TypeInContext(long C);

        @LibrarySymbol(name = "LLVMInt128TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInt128TypeInContext(long C);

        @LibrarySymbol(name = "LLVMIntTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMIntTypeInContext(long C, int NumBits);

        @LibrarySymbol(name = "LLVMInt1Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt1Type();

        @LibrarySymbol(name = "LLVMInt8Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt8Type();

        @LibrarySymbol(name = "LLVMInt16Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt16Type();

        @LibrarySymbol(name = "LLVMInt32Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt32Type();

        @LibrarySymbol(name = "LLVMInt64Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt64Type();

        @LibrarySymbol(name = "LLVMInt128Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMInt128Type();

        @LibrarySymbol(name = "LLVMIntType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT})
        abstract long LLVMIntType(int NumBits);

        @LibrarySymbol(name = "LLVMGetIntTypeWidth")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetIntTypeWidth(long IntegerTy);

        @LibrarySymbol(name = "LLVMHalfTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMHalfTypeInContext(long C);

        @LibrarySymbol(name = "LLVMFloatTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMFloatTypeInContext(long C);

        @LibrarySymbol(name = "LLVMDoubleTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMDoubleTypeInContext(long C);

        @LibrarySymbol(name = "LLVMX86FP80TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMX86FP80TypeInContext(long C);

        @LibrarySymbol(name = "LLVMFP128TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMFP128TypeInContext(long C);

        @LibrarySymbol(name = "LLVMPPCFP128TypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPPCFP128TypeInContext(long C);

        @LibrarySymbol(name = "LLVMHalfType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMHalfType();

        @LibrarySymbol(name = "LLVMFloatType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMFloatType();

        @LibrarySymbol(name = "LLVMDoubleType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMDoubleType();

        @LibrarySymbol(name = "LLVMX86FP80Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMX86FP80Type();

        @LibrarySymbol(name = "LLVMFP128Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMFP128Type();

        @LibrarySymbol(name = "LLVMPPCFP128Type")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMPPCFP128Type();

        @LibrarySymbol(name = "LLVMFunctionType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMFunctionType(long ReturnType, long ParamTypes, int ParamCount, boolean IsVarArg);

        @LibrarySymbol(name = "LLVMIsFunctionVarArg")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsFunctionVarArg(long FunctionTy);

        @LibrarySymbol(name = "LLVMGetReturnType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetReturnType(long FunctionTy);

        /*@LibrarySymbol("LLVMCountParamTypes")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountParamTypes(long);

        @LibrarySymbol("LLVMGetParamTypes")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetParamTypes(long, long);*/

        @LibrarySymbol(name = "LLVMStructTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMStructTypeInContext(long C, long ElementTypes, int ElementCount, boolean Packed);

        @LibrarySymbol(name = "LLVMStructType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMStructType(long ElementTypes, int ElementCount, boolean Packed);

        @LibrarySymbol(name = "LLVMStructCreateNamed")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMStructCreateNamed(long C, long Name);

        @LibrarySymbol(name = "LLVMGetStructName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetStructName(long Ty);

        @LibrarySymbol(name = "LLVMStructSetBody")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract void LLVMStructSetBody(long StructTy, long ElementTypes, int ElementCount, boolean Packed);

        /*@LibrarySymbol("LLVMCountStructElementTypes")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountStructElementTypes(long);

        @LibrarySymbol("LLVMGetStructElementTypes")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetStructElementTypes(long, long);

        @LibrarySymbol("LLVMStructGetTypeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMStructGetTypeAtIndex(long, int);*/

        @LibrarySymbol(name = "LLVMIsPackedStruct")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsPackedStruct(long StructTy);

        @LibrarySymbol(name = "LLVMIsOpaqueStruct")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsOpaqueStruct(long StructTy);

        @LibrarySymbol(name = "LLVMGetElementType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetElementType(long Ty);

        @LibrarySymbol(name = "LLVMArrayType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMArrayType(long ElementType, int ElementCount);

        @LibrarySymbol(name = "LLVMGetArrayLength")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetArrayLength(long ArrayTy);

        @LibrarySymbol(name = "LLVMPointerType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMPointerType(long ElementType, int AddressSpace);

        @LibrarySymbol(name = "LLVMGetPointerAddressSpace")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetPointerAddressSpace(long PointerTy);

        @LibrarySymbol(name = "LLVMVectorType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMVectorType(long ElementType, int ElementCount);

        @LibrarySymbol(name = "LLVMGetVectorSize")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetVectorSize(long VectorTy);

        @LibrarySymbol(name = "LLVMVoidTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMVoidTypeInContext(long C);

        @LibrarySymbol(name = "LLVMLabelTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMLabelTypeInContext(long C);

        @LibrarySymbol(name = "LLVMX86MMXTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMX86MMXTypeInContext(long C);

        @LibrarySymbol(name = "LLVMVoidType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMVoidType();

        @LibrarySymbol(name = "LLVMLabelType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMLabelType();

        @LibrarySymbol(name = "LLVMX86MMXType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMX86MMXType();

        /*@LibrarySymbol("LLVMIsAArgument")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAArgument(long);

        @LibrarySymbol("LLVMIsABasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABasicBlock(long);

        @LibrarySymbol("LLVMIsAInlineAsm")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInlineAsm(long);

        @LibrarySymbol("LLVMIsAUser")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUser(long);

        @LibrarySymbol("LLVMIsAConstant")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstant(long);

        @LibrarySymbol("LLVMIsABlockAddress")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABlockAddress(long);

        @LibrarySymbol("LLVMIsAConstantAggregateZero")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantAggregateZero(long);

        @LibrarySymbol("LLVMIsAConstantArray")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantArray(long);

        @LibrarySymbol("LLVMIsAConstantDataSequential")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataSequential(long);

        @LibrarySymbol("LLVMIsAConstantDataArray")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataArray(long);

        @LibrarySymbol("LLVMIsAConstantDataVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataVector(long);

        @LibrarySymbol("LLVMIsAConstantExpr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantExpr(long);

        @LibrarySymbol("LLVMIsAConstantFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantFP(long);

        @LibrarySymbol("LLVMIsAConstantInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantInt(long);

        @LibrarySymbol("LLVMIsAConstantPointerNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantPointerNull(long);

        @LibrarySymbol("LLVMIsAConstantStruct")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantStruct(long);

        @LibrarySymbol("LLVMIsAConstantTokenNone")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantTokenNone(long);

        @LibrarySymbol("LLVMIsAConstantVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantVector(long);

        @LibrarySymbol("LLVMIsAGlobalValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalValue(long);

        @LibrarySymbol("LLVMIsAGlobalAlias")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalAlias(long);

        @LibrarySymbol("LLVMIsAGlobalObject")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalObject(long);

        @LibrarySymbol("LLVMIsAFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFunction(long);

        @LibrarySymbol("LLVMIsAGlobalVariable")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalVariable(long);

        @LibrarySymbol("LLVMIsAUndefValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUndefValue(long);

        @LibrarySymbol("LLVMIsAInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInstruction(long);

        @LibrarySymbol("LLVMIsABinaryOperator")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABinaryOperator(long);

        @LibrarySymbol("LLVMIsACallInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACallInst(long);

        @LibrarySymbol("LLVMIsAIntrinsicInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIntrinsicInst(long);

        @LibrarySymbol("LLVMIsADbgInfoIntrinsic")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsADbgInfoIntrinsic(long);

        @LibrarySymbol("LLVMIsADbgDeclareInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsADbgDeclareInst(long);

        @LibrarySymbol("LLVMIsAMemIntrinsic")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemIntrinsic(long);

        @LibrarySymbol("LLVMIsAMemCpyInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemCpyInst(long);

        @LibrarySymbol("LLVMIsAMemMoveInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemMoveInst(long);

        @LibrarySymbol("LLVMIsAMemSetInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemSetInst(long);

        @LibrarySymbol("LLVMIsACmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACmpInst(long);

        @LibrarySymbol("LLVMIsAFCmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFCmpInst(long);

        @LibrarySymbol("LLVMIsAICmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAICmpInst(long);

        @LibrarySymbol("LLVMIsAExtractElementInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAExtractElementInst(long);

        @LibrarySymbol("LLVMIsAGetElementPtrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGetElementPtrInst(long);

        @LibrarySymbol("LLVMIsAInsertElementInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInsertElementInst(long);

        @LibrarySymbol("LLVMIsAInsertValueInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInsertValueInst(long);

        @LibrarySymbol("LLVMIsALandingPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsALandingPadInst(long);

        @LibrarySymbol("LLVMIsAPHINode")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAPHINode(long);

        @LibrarySymbol("LLVMIsASelectInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASelectInst(long);

        @LibrarySymbol("LLVMIsAShuffleVectorInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAShuffleVectorInst(long);

        @LibrarySymbol("LLVMIsAStoreInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAStoreInst(long);

        @LibrarySymbol("LLVMIsATerminatorInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsATerminatorInst(long);

        @LibrarySymbol("LLVMIsABranchInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABranchInst(long);

        @LibrarySymbol("LLVMIsAIndirectBrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIndirectBrInst(long);

        @LibrarySymbol("LLVMIsAInvokeInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInvokeInst(long);

        @LibrarySymbol("LLVMIsAReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAReturnInst(long);

        @LibrarySymbol("LLVMIsASwitchInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASwitchInst(long);

        @LibrarySymbol("LLVMIsAUnreachableInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUnreachableInst(long);

        @LibrarySymbol("LLVMIsAResumeInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAResumeInst(long);

        @LibrarySymbol("LLVMIsACleanupReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACleanupReturnInst(long);

        @LibrarySymbol("LLVMIsACatchReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACatchReturnInst(long);

        @LibrarySymbol("LLVMIsAFuncletPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFuncletPadInst(long);

        @LibrarySymbol("LLVMIsACatchPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACatchPadInst(long);

        @LibrarySymbol("LLVMIsACleanupPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACleanupPadInst(long);

        @LibrarySymbol("LLVMIsAUnaryInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUnaryInstruction(long);

        @LibrarySymbol("LLVMIsAAllocaInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAAllocaInst(long);

        @LibrarySymbol("LLVMIsACastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACastInst(long);

        @LibrarySymbol("LLVMIsAAddrSpaceCastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAAddrSpaceCastInst(long);

        @LibrarySymbol("LLVMIsABitCastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABitCastInst(long);

        @LibrarySymbol("LLVMIsAFPExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPExtInst(long);

        @LibrarySymbol("LLVMIsAFPToSIInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPToSIInst(long);

        @LibrarySymbol("LLVMIsAFPToUIInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPToUIInst(long);

        @LibrarySymbol("LLVMIsAFPTruncInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPTruncInst(long);

        @LibrarySymbol("LLVMIsAIntToPtrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIntToPtrInst(long);

        @LibrarySymbol("LLVMIsAPtrToIntInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAPtrToIntInst(long);

        @LibrarySymbol("LLVMIsASExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASExtInst(long);

        @LibrarySymbol("LLVMIsASIToFPInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASIToFPInst(long);

        @LibrarySymbol("LLVMIsATruncInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsATruncInst(long);

        @LibrarySymbol("LLVMIsAUIToFPInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUIToFPInst(long);

        @LibrarySymbol("LLVMIsAZExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAZExtInst(long);

        @LibrarySymbol("LLVMIsAExtractValueInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAExtractValueInst(long);

        @LibrarySymbol("LLVMIsALoadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsALoadInst(long);

        @LibrarySymbol("LLVMIsAVAArgInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAVAArgInst(long);

        @LibrarySymbol("LLVMTypeOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMTypeOf(long);

        @LibrarySymbol("LLVMGetValueKind")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetValueKind(long);

        @LibrarySymbol("LLVMGetValueName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetValueName(long);

        @LibrarySymbol("LLVMSetValueName")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetValueName(long, long);

        @LibrarySymbol("LLVMDumpValue")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDumpValue(long);*/

        @LibrarySymbol(name = "LLVMPrintValueToString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPrintValueToString(long Val);

        /*@LibrarySymbol("LLVMReplaceAllUsesWith")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMReplaceAllUsesWith(long, long);

        @LibrarySymbol("LLVMIsConstant")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsConstant(long);

        @LibrarySymbol("LLVMIsUndef")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsUndef(long);

        @LibrarySymbol("LLVMIsAMDNode")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMDNode(long);

        @LibrarySymbol("LLVMIsAMDString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMDString(long);

        @LibrarySymbol("LLVMGetFirstUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstUse(long);

        @LibrarySymbol("LLVMGetNextUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextUse(long);

        @LibrarySymbol("LLVMGetUser")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUser(long);

        @LibrarySymbol("LLVMGetUsedValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUsedValue(long);

        @LibrarySymbol("LLVMGetOperand")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetOperand(long, int);

        @LibrarySymbol("LLVMGetOperandUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetOperandUse(long, int);

        @LibrarySymbol("LLVMSetOperand")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetOperand(long, int, long);

        @LibrarySymbol("LLVMGetNumOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumOperands(long);*/

        @LibrarySymbol(name = "LLVMConstNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNull(long Ty);

        @LibrarySymbol(name = "LLVMConstAllOnes")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstAllOnes(long Ty);

        @LibrarySymbol(name = "LLVMGetUndef")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUndef(long Ty);

        /*@LibrarySymbol("LLVMIsNull")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsNull(long);

        @LibrarySymbol("LLVMConstPointerNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstPointerNull(long);*/

        @LibrarySymbol(name = "LLVMConstInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG, BOOL_AS_INT})
        abstract long LLVMConstInt(long IntTy, long N, boolean SignExtend);

        @LibrarySymbol(name = "LLVMConstIntOfArbitraryPrecision")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMConstIntOfArbitraryPrecision(long IntTy, int NumWords, long Words);

        /*@LibrarySymbol("LLVMConstIntOfString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract long LLVMConstIntOfString(long, long, byte);

        @LibrarySymbol("LLVMConstIntOfStringAndSize")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BYTE})
        abstract long LLVMConstIntOfStringAndSize(long, long, int, byte);

        @LibrarySymbol("LLVMConstReal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, DOUBLE})
        abstract long LLVMConstReal(long, double);

        @LibrarySymbol("LLVMConstRealOfString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstRealOfString(long, long);

        @LibrarySymbol("LLVMConstRealOfStringAndSize")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstRealOfStringAndSize(long, long, int);

        @LibrarySymbol("LLVMConstIntGetZExtValue")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMConstIntGetZExtValue(long);

        @LibrarySymbol("LLVMConstIntGetSExtValue")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMConstIntGetSExtValue(long);

        @LibrarySymbol("LLVMConstRealGetDouble")
        @CallSignature(type = CRITICAL, ret = DOUBLE, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract double LLVMConstRealGetDouble(long, long);*/

        @LibrarySymbol(name = "LLVMConstStringInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMConstStringInContext(long C, long Str, int Length, boolean DontNullTerminate);

        @LibrarySymbol(name = "LLVMConstString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMConstString(long Str, int Length, boolean DontNullTerminate);

        /*@LibrarySymbol("LLVMIsConstantString")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsConstantString(long);

        @LibrarySymbol("LLVMGetAsString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetAsString(long, long);

        @LibrarySymbol("LLVMConstStructInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMConstStructInContext(long, long, int, boolean);

        @LibrarySymbol("LLVMConstStruct")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMConstStruct(long, int, boolean);

        @LibrarySymbol("LLVMConstArray")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstArray(long, long, int);

        @LibrarySymbol("LLVMConstNamedStruct")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstNamedStruct(long, long, int);

        @LibrarySymbol("LLVMGetElementAsConstant")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetElementAsConstant(long, int);

        @LibrarySymbol("LLVMConstVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMConstVector(long, int);

        @LibrarySymbol("LLVMGetConstOpcode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetConstOpcode(long);

        @LibrarySymbol("LLVMAlignOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMAlignOf(long);

        @LibrarySymbol("LLVMSizeOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMSizeOf(long);

        @LibrarySymbol("LLVMConstNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNeg(long);

        @LibrarySymbol("LLVMConstNSWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNSWNeg(long);

        @LibrarySymbol("LLVMConstNUWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNUWNeg(long);

        @LibrarySymbol("LLVMConstFNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstFNeg(long);

        @LibrarySymbol("LLVMConstNot")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNot(long);

        @LibrarySymbol("LLVMConstAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAdd(long, long);

        @LibrarySymbol("LLVMConstNSWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWAdd(long, long);

        @LibrarySymbol("LLVMConstNUWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWAdd(long, long);

        @LibrarySymbol("LLVMConstFAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFAdd(long, long);

        @LibrarySymbol("LLVMConstSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSub(long, long);

        @LibrarySymbol("LLVMConstNSWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWSub(long, long);

        @LibrarySymbol("LLVMConstNUWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWSub(long, long);

        @LibrarySymbol("LLVMConstFSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFSub(long, long);

        @LibrarySymbol("LLVMConstMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstMul(long, long);

        @LibrarySymbol("LLVMConstNSWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWMul(long, long);

        @LibrarySymbol("LLVMConstNUWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWMul(long, long);

        @LibrarySymbol("LLVMConstFMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFMul(long, long);

        @LibrarySymbol("LLVMConstUDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstUDiv(long, long);

        @LibrarySymbol("LLVMConstSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSDiv(long, long);

        @LibrarySymbol("LLVMConstExactSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstExactSDiv(long, long);

        @LibrarySymbol("LLVMConstFDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFDiv(long, long);

        @LibrarySymbol("LLVMConstURem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstURem(long, long);

        @LibrarySymbol("LLVMConstSRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSRem(long, long);

        @LibrarySymbol("LLVMConstFRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFRem(long, long);

        @LibrarySymbol("LLVMConstAnd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAnd(long, long);

        @LibrarySymbol("LLVMConstOr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstOr(long, long);

        @LibrarySymbol("LLVMConstXor")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstXor(long, long);

        @LibrarySymbol("LLVMConstICmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstICmp(int, long, long);

        @LibrarySymbol("LLVMConstFCmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFCmp(int, long, long);

        @LibrarySymbol("LLVMConstShl")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstShl(long, long);

        @LibrarySymbol("LLVMConstLShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstLShr(long, long);

        @LibrarySymbol("LLVMConstAShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAShr(long, long);

        @LibrarySymbol("LLVMConstGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstGEP(long, long, int);

        @LibrarySymbol("LLVMConstInBoundsGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstInBoundsGEP(long, long, int);

        @LibrarySymbol("LLVMConstTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstTrunc(long, long);

        @LibrarySymbol("LLVMConstSExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSExt(long, long);

        @LibrarySymbol("LLVMConstZExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstZExt(long, long);

        @LibrarySymbol("LLVMConstFPTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPTrunc(long, long);

        @LibrarySymbol("LLVMConstFPExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPExt(long, long);

        @LibrarySymbol("LLVMConstUIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstUIToFP(long, long);

        @LibrarySymbol("LLVMConstSIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSIToFP(long, long);

        @LibrarySymbol("LLVMConstFPToUI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPToUI(long, long);

        @LibrarySymbol("LLVMConstFPToSI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPToSI(long, long);

        @LibrarySymbol("LLVMConstPtrToInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstPtrToInt(long, long);

        @LibrarySymbol("LLVMConstIntToPtr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstIntToPtr(long, long);

        @LibrarySymbol("LLVMConstBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstBitCast(long, long);

        @LibrarySymbol("LLVMConstAddrSpaceCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAddrSpaceCast(long, long);

        @LibrarySymbol("LLVMConstZExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstZExtOrBitCast(long, long);

        @LibrarySymbol("LLVMConstSExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSExtOrBitCast(long, long);

        @LibrarySymbol("LLVMConstTruncOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstTruncOrBitCast(long, long);

        @LibrarySymbol("LLVMConstPointerCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstPointerCast(long, long);

        @LibrarySymbol("LLVMConstIntCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT})
        abstract long LLVMConstIntCast(long, long, boolean);

        @LibrarySymbol("LLVMConstFPCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPCast(long, long);

        @LibrarySymbol("LLVMConstSelect")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSelect(long, long, long);

        @LibrarySymbol("LLVMConstExtractElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstExtractElement(long, long);

        @LibrarySymbol("LLVMConstInsertElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstInsertElement(long, long, long);

        @LibrarySymbol("LLVMConstShuffleVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstShuffleVector(long, long, long);

        @LibrarySymbol("LLVMConstExtractValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstExtractValue(long, long, int);

        @LibrarySymbol("LLVMConstInsertValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstInsertValue(long, long, long, int);

        @LibrarySymbol("LLVMConstInlineAsm")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT, BOOL_AS_INT})
        abstract long LLVMConstInlineAsm(long, long, long, boolean, boolean);

        @LibrarySymbol("LLVMBlockAddress")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBlockAddress(long, long);*/

        @LibrarySymbol(name = "LLVMGetGlobalParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetGlobalParent(long Global);

        @LibrarySymbol(name = "LLVMIsDeclaration")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsDeclaration(long Global);

        @LibrarySymbol(name = "LLVMGetLinkage")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetLinkage(long Global);

        @LibrarySymbol(name = "LLVMSetLinkage")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetLinkage(long Global, int Linkage);

        @LibrarySymbol(name = "LLVMGetSection")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSection(long Global);

        @LibrarySymbol(name = "LLVMSetSection")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetSection(long Global, long Section);

        @LibrarySymbol(name = "LLVMGetVisibility")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetVisibility(long Global);

        @LibrarySymbol(name = "LLVMSetVisibility")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetVisibility(long Global, int Viz);

        @LibrarySymbol(name = "LLVMGetDLLStorageClass")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetDLLStorageClass(long Global);

        @LibrarySymbol(name = "LLVMSetDLLStorageClass")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetDLLStorageClass(long Global, int Class);

        /*@LibrarySymbol("LLVMHasUnnamedAddr")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMHasUnnamedAddr(long);

        @LibrarySymbol("LLVMSetUnnamedAddr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetUnnamedAddr(long, boolean);*/

        @LibrarySymbol(name = "LLVMGetAlignment")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetAlignment(long V);

        @LibrarySymbol(name = "LLVMSetAlignment")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetAlignment(long V, int Bytes);

        /*@LibrarySymbol("LLVMAddGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddGlobal(long, long, long);

        @LibrarySymbol("LLVMAddGlobalInAddressSpace")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMAddGlobalInAddressSpace(long, long, long, int);

        @LibrarySymbol("LLVMGetNamedGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetNamedGlobal(long, long);

        @LibrarySymbol("LLVMGetFirstGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstGlobal(long);

        @LibrarySymbol("LLVMGetLastGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastGlobal(long);

        @LibrarySymbol("LLVMGetNextGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextGlobal(long);

        @LibrarySymbol("LLVMGetPreviousGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousGlobal(long);

        @LibrarySymbol("LLVMDeleteGlobal")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteGlobal(long);

        @LibrarySymbol("LLVMGetInitializer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetInitializer(long);

        @LibrarySymbol("LLVMSetInitializer")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetInitializer(long, long);

        @LibrarySymbol("LLVMIsThreadLocal")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsThreadLocal(long);

        @LibrarySymbol("LLVMSetThreadLocal")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetThreadLocal(long, boolean);

        @LibrarySymbol("LLVMIsGlobalConstant")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsGlobalConstant(long);

        @LibrarySymbol("LLVMSetGlobalConstant")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetGlobalConstant(long, boolean);

        @LibrarySymbol("LLVMGetThreadLocalMode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetThreadLocalMode(long);

        @LibrarySymbol("LLVMSetThreadLocalMode")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetThreadLocalMode(long, int);

        @LibrarySymbol("LLVMIsExternallyInitialized")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsExternallyInitialized(long);

        @LibrarySymbol("LLVMSetExternallyInitialized")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetExternallyInitialized(long, boolean);

        @LibrarySymbol("LLVMAddAlias")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddAlias(long, long, long, long);

        @LibrarySymbol("LLVMDeleteFunction")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteFunction(long);

        @LibrarySymbol("LLVMHasPersonalityFn")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMHasPersonalityFn(long);

        @LibrarySymbol("LLVMGetPersonalityFn")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPersonalityFn(long);

        @LibrarySymbol("LLVMSetPersonalityFn")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetPersonalityFn(long, long);

        @LibrarySymbol("LLVMGetIntrinsicID")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetIntrinsicID(long);*/

        @LibrarySymbol(name = "LLVMGetFunctionCallConv")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetFunctionCallConv(long Fn);

        @LibrarySymbol(name = "LLVMSetFunctionCallConv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetFunctionCallConv(long Fn, int CC);

        /*@LibrarySymbol("LLVMGetGC")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetGC(long);

        @LibrarySymbol("LLVMSetGC")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetGC(long, long);*/

        @LibrarySymbol(name = "LLVMAddFunctionAttr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMAddFunctionAttr(long Fn, int PA);

        @LibrarySymbol(name = "LLVMAddAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMAddAttributeAtIndex(long F, int Idx, long A);

        /*@LibrarySymbol("LLVMGetEnumAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, INT})
        abstract long LLVMGetEnumAttributeAtIndex(long, int, int);

        @LibrarySymbol("LLVMGetStringAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract long LLVMGetStringAttributeAtIndex(long, int, long, int);

        @LibrarySymbol("LLVMRemoveEnumAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMRemoveEnumAttributeAtIndex(long, int, int);

        @LibrarySymbol("LLVMRemoveStringAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract void LLVMRemoveStringAttributeAtIndex(long, int, long, int);

        @LibrarySymbol("LLVMAddTargetDependentFunctionAttr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddTargetDependentFunctionAttr(long, long, long);

        @LibrarySymbol("LLVMGetFunctionAttr")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetFunctionAttr(long);

        @LibrarySymbol("LLVMRemoveFunctionAttr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMRemoveFunctionAttr(long, int);*/

        @LibrarySymbol(name = "LLVMCountParams")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountParams(long Fn);

        @LibrarySymbol(name = "LLVMGetParams")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetParams(long Fn, long Params);

        @LibrarySymbol(name = "LLVMGetParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetParam(long Fn, int Index);

        /*@LibrarySymbol("LLVMGetParamParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetParamParent(long);

        @LibrarySymbol("LLVMGetFirstParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstParam(long);

        @LibrarySymbol("LLVMGetLastParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastParam(long);

        @LibrarySymbol("LLVMGetNextParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextParam(long);

        @LibrarySymbol("LLVMGetPreviousParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousParam(long);*/

        @LibrarySymbol(name = "LLVMAddAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMAddAttribute(long Arg, int PA);

        /*@LibrarySymbol("LLVMRemoveAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMRemoveAttribute(long, int);*/

        @LibrarySymbol(name = "LLVMGetAttribute")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetAttribute(long Arg);

        @LibrarySymbol(name = "LLVMSetParamAlignment")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetParamAlignment(long Arg, int Align);

        /*@LibrarySymbol("LLVMMDStringInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMMDStringInContext(long, long, int);

        @LibrarySymbol("LLVMMDString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMMDString(long, int);

        @LibrarySymbol("LLVMMDNodeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMMDNodeInContext(long, long, int);

        @LibrarySymbol("LLVMMDNode")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMMDNode(long, int);

        @LibrarySymbol("LLVMGetMDString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetMDString(long, long);

        @LibrarySymbol("LLVMGetMDNodeNumOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetMDNodeNumOperands(long);

        @LibrarySymbol("LLVMGetMDNodeOperands")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetMDNodeOperands(long, long);

        @LibrarySymbol("LLVMBasicBlockAsValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMBasicBlockAsValue(long);

        @LibrarySymbol("LLVMValueIsBasicBlock")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMValueIsBasicBlock(long);

        @LibrarySymbol("LLVMValueAsBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMValueAsBasicBlock(long);

        @LibrarySymbol("LLVMGetBasicBlockName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockName(long);*/

        @LibrarySymbol(name = "LLVMGetBasicBlockParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockParent(long BB);

        /*@LibrarySymbol("LLVMGetBasicBlockTerminator")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockTerminator(long);

        @LibrarySymbol("LLVMCountBasicBlocks")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountBasicBlocks(long);

        @LibrarySymbol("LLVMGetBasicBlocks")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetBasicBlocks(long, long);

        @LibrarySymbol("LLVMGetFirstBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstBasicBlock(long);

        @LibrarySymbol("LLVMGetLastBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastBasicBlock(long);

        @LibrarySymbol("LLVMGetNextBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextBasicBlock(long);

        @LibrarySymbol("LLVMGetPreviousBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousBasicBlock(long);*/

        @LibrarySymbol(name = "LLVMGetEntryBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetEntryBasicBlock(long Fn);

        @LibrarySymbol(name = "LLVMAppendBasicBlockInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAppendBasicBlockInContext(long C, long Fn, long Name);

        @LibrarySymbol(name = "LLVMAppendBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAppendBasicBlock(long Fn, long Name);

        /*@LibrarySymbol("LLVMInsertBasicBlockInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMInsertBasicBlockInContext(long, long, long);

        @LibrarySymbol("LLVMInsertBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMInsertBasicBlock(long, long);

        @LibrarySymbol("LLVMDeleteBasicBlock")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteBasicBlock(long);

        @LibrarySymbol("LLVMRemoveBasicBlockFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRemoveBasicBlockFromParent(long);

        @LibrarySymbol("LLVMMoveBasicBlockBefore")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMMoveBasicBlockBefore(long, long);

        @LibrarySymbol("LLVMMoveBasicBlockAfter")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMMoveBasicBlockAfter(long, long);

        @LibrarySymbol("LLVMGetFirstInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstInstruction(long);

        @LibrarySymbol("LLVMGetLastInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastInstruction(long);

        @LibrarySymbol("LLVMHasMetadata")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMHasMetadata(long);

        @LibrarySymbol("LLVMGetMetadata")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetMetadata(long, int);

        @LibrarySymbol("LLVMSetMetadata")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetMetadata(long, int, long);

        @LibrarySymbol("LLVMGetInstructionParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetInstructionParent(long);

        @LibrarySymbol("LLVMGetNextInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextInstruction(long);

        @LibrarySymbol("LLVMGetPreviousInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousInstruction(long);

        @LibrarySymbol("LLVMInstructionRemoveFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInstructionRemoveFromParent(long);

        @LibrarySymbol("LLVMInstructionEraseFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInstructionEraseFromParent(long);

        @LibrarySymbol("LLVMGetInstructionOpcode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetInstructionOpcode(long);

        @LibrarySymbol("LLVMGetICmpPredicate")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetICmpPredicate(long);

        @LibrarySymbol("LLVMGetFCmpPredicate")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetFCmpPredicate(long);

        @LibrarySymbol("LLVMInstructionClone")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInstructionClone(long);

        @LibrarySymbol("LLVMGetNumArgOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumArgOperands(long);

        @LibrarySymbol("LLVMSetInstructionCallConv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetInstructionCallConv(long, int);

        @LibrarySymbol("LLVMGetInstructionCallConv")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetInstructionCallConv(long);*/

        @LibrarySymbol(name = "LLVMAddInstrAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMAddInstrAttribute(long Instr, int index, int PA);

        @LibrarySymbol(name = "LLVMRemoveInstrAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMRemoveInstrAttribute(long Instr, int index, int PA);

        @LibrarySymbol(name = "LLVMSetInstrParamAlignment")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMSetInstrParamAlignment(long Instr, int index, int Align);

        /*@LibrarySymbol("LLVMAddCallSiteAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMAddCallSiteAttribute(long, int, long);

        @LibrarySymbol("LLVMGetCallSiteEnumAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, INT})
        abstract long LLVMGetCallSiteEnumAttribute(long, int, int);

        @LibrarySymbol("LLVMGetCallSiteStringAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract long LLVMGetCallSiteStringAttribute(long, int, long, int);

        @LibrarySymbol("LLVMRemoveCallSiteEnumAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMRemoveCallSiteEnumAttribute(long, int, int);

        @LibrarySymbol("LLVMRemoveCallSiteStringAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract void LLVMRemoveCallSiteStringAttribute(long, int, long, int);

        @LibrarySymbol("LLVMGetCalledValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetCalledValue(long);

        @LibrarySymbol("LLVMIsTailCall")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsTailCall(long);

        @LibrarySymbol("LLVMSetTailCall")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetTailCall(long, boolean);

        @LibrarySymbol("LLVMGetNormalDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNormalDest(long);

        @LibrarySymbol("LLVMGetUnwindDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUnwindDest(long);

        @LibrarySymbol("LLVMSetNormalDest")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetNormalDest(long, long);

        @LibrarySymbol("LLVMSetUnwindDest")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetUnwindDest(long, long);

        @LibrarySymbol("LLVMGetNumSuccessors")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumSuccessors(long);

        @LibrarySymbol("LLVMGetSuccessor")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetSuccessor(long, int);

        @LibrarySymbol("LLVMSetSuccessor")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetSuccessor(long, int, long);

        @LibrarySymbol("LLVMIsConditional")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsConditional(long);

        @LibrarySymbol("LLVMGetCondition")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetCondition(long);

        @LibrarySymbol("LLVMSetCondition")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetCondition(long, long);

        @LibrarySymbol("LLVMGetSwitchDefaultDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSwitchDefaultDest(long);*/

        @LibrarySymbol(name = "LLVMGetAllocatedType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetAllocatedType(long Alloca);

        @LibrarySymbol(name = "LLVMIsInBounds")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsInBounds(long GEP);

        @LibrarySymbol(name = "LLVMSetIsInBounds")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetIsInBounds(long GEP, boolean InBounds);

        @LibrarySymbol(name = "LLVMAddIncoming")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract void LLVMAddIncoming(long PhiNode, long IncomingValues, long IncomingBlocks, int Count);

        @LibrarySymbol(name = "LLVMCountIncoming")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountIncoming(long PhiNode);

        @LibrarySymbol(name = "LLVMGetIncomingValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetIncomingValue(long PhiNode, int Index);

        @LibrarySymbol(name = "LLVMGetIncomingBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetIncomingBlock(long PhiNode, int Index);

        /*@LibrarySymbol("LLVMGetNumIndices")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumIndices(long);

        @LibrarySymbol("LLVMGetIndices")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetIndices(long);*/

        @LibrarySymbol(name = "LLVMCreateBuilderInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateBuilderInContext(long C);

        @LibrarySymbol(name = "LLVMCreateBuilder")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMCreateBuilder();

        @LibrarySymbol(name = "LLVMPositionBuilder")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMPositionBuilder(long Builder, long Block, long Instr);

        @LibrarySymbol(name = "LLVMPositionBuilderBefore")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMPositionBuilderBefore(long Builder, long Block);

        @LibrarySymbol(name = "LLVMPositionBuilderAtEnd")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMPositionBuilderAtEnd(long Builder, long Block);

        @LibrarySymbol(name = "LLVMGetInsertBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetInsertBlock(long Builder);

        @LibrarySymbol(name = "LLVMClearInsertionPosition")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMClearInsertionPosition(long Builder);

        @LibrarySymbol(name = "LLVMInsertIntoBuilder")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMInsertIntoBuilder(long Builder, long Instr);

        @LibrarySymbol(name = "LLVMInsertIntoBuilderWithName")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMInsertIntoBuilderWithName(long Builder, long Instr, long Name);

        @LibrarySymbol(name = "LLVMDisposeBuilder")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeBuilder(long Builder);

        @LibrarySymbol(name = "LLVMSetCurrentDebugLocation")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetCurrentDebugLocation(long Builder, long L);

        @LibrarySymbol(name = "LLVMGetCurrentDebugLocation")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetCurrentDebugLocation(long Builder);

        @LibrarySymbol(name = "LLVMSetInstDebugLocation")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetInstDebugLocation(long Builder, long Inst);

        @LibrarySymbol(name = "LLVMBuildRetVoid")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMBuildRetVoid(long Builder);

        @LibrarySymbol(name = "LLVMBuildRet")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildRet(long Builder, long V);

        /*@LibrarySymbol("LLVMBuildAggregateRet")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildAggregateRet(long, long, int);*/

        @LibrarySymbol(name = "LLVMBuildBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildBr(long B, long Dest);

        @LibrarySymbol(name = "LLVMBuildCondBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildCondBr(long B, long If, long Then, long Else);

        /*@LibrarySymbol("LLVMBuildSwitch")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildSwitch(long, long, long, int);

        @LibrarySymbol("LLVMBuildIndirectBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildIndirectBr(long, long, int);

        @LibrarySymbol("LLVMBuildInvoke")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildInvoke(long, long, long, int, long, long, long);

        @LibrarySymbol("LLVMBuildLandingPad")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildLandingPad(long, long, long, int, long);

        @LibrarySymbol("LLVMBuildResume")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildResume(long, long);

        @LibrarySymbol("LLVMBuildUnreachable")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMBuildUnreachable(long);

        @LibrarySymbol("LLVMAddCase")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddCase(long, long, long);

        @LibrarySymbol("LLVMAddDestination")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddDestination(long, long);

        @LibrarySymbol("LLVMGetNumClauses")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumClauses(long);

        @LibrarySymbol("LLVMGetClause")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetClause(long, int);

        @LibrarySymbol("LLVMAddClause")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddClause(long, long);

        @LibrarySymbol("LLVMIsCleanup")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsCleanup(long);

        @LibrarySymbol("LLVMSetCleanup")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetCleanup(long, boolean);*/

        @LibrarySymbol(name = "LLVMBuildAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildAdd(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNSWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNSWAdd(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNUWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNUWAdd(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFAdd(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSub(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNSWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNSWSub(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNUWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNUWSub(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFSub(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildMul(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNSWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNSWMul(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNUWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNUWMul(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFMul(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildUDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildUDiv(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSDiv(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildExactSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildExactSDiv(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFDiv(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildURem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildURem(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildSRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSRem(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFRem(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildShl")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildShl(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildLShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildLShr(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildAShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildAShr(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildAnd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildAnd(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildOr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildOr(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildXor")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildXor(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildBinOp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildBinOp(long B, int Op, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNeg(long B, long V, long Name);

        @LibrarySymbol(name = "LLVMBuildNSWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNSWNeg(long B, long V, long Name);

        @LibrarySymbol(name = "LLVMBuildNUWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNUWNeg(long B, long V, long Name);

        @LibrarySymbol(name = "LLVMBuildFNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFNeg(long B, long V, long Name);

        @LibrarySymbol(name = "LLVMBuildNot")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildNot(long B, long V, long Name);

        @LibrarySymbol(name = "LLVMBuildMalloc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildMalloc(long B, long Ty, long Name);

        @LibrarySymbol(name = "LLVMBuildArrayMalloc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildArrayMalloc(long B, long Ty, long Val, long Name);

        @LibrarySymbol(name = "LLVMBuildAlloca")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildAlloca(long B, long Ty, long Name);

        @LibrarySymbol(name = "LLVMBuildArrayAlloca")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildArrayAlloca(long B, long Ty, long Val, long Name);

        @LibrarySymbol(name = "LLVMBuildFree")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFree(long B, long Ptr);

        @LibrarySymbol(name = "LLVMBuildLoad")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildLoad(long B, long Ptr, long Name);

        @LibrarySymbol(name = "LLVMBuildStore")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildStore(long B, long Val, long Ptr);

        @LibrarySymbol(name = "LLVMBuildGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildGEP(long B, long Pointer, long Indices, int NumIndices, long Name);

        @LibrarySymbol(name = "LLVMBuildInBoundsGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildInBoundsGEP(long B, long Pointer, long Indices, int NumIndices, long Name);

        @LibrarySymbol(name = "LLVMBuildStructGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildStructGEP(long B, long Pointer, int Idx, long Name);

        @LibrarySymbol(name = "LLVMBuildGlobalString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildGlobalString(long B, long Str, long Name);

        @LibrarySymbol(name = "LLVMBuildGlobalStringPtr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildGlobalStringPtr(long B, long Str, long Name);

        @LibrarySymbol(name = "LLVMGetVolatile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMGetVolatile(long MemoryAccessInst);

        @LibrarySymbol(name = "LLVMSetVolatile")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetVolatile(long MemoryAccessInst, boolean IsVolatile);

        @LibrarySymbol(name = "LLVMGetOrdering")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetOrdering(long MemoryAccessInst);

        @LibrarySymbol(name = "LLVMSetOrdering")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetOrdering(long MemoryAccessInst, int Ordering);

        @LibrarySymbol(name = "LLVMBuildTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildTrunc(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildZExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildZExt(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildSExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSExt(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildFPToUI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFPToUI(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildFPToSI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFPToSI(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildUIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildUIToFP(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildSIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSIToFP(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildFPTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFPTrunc(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildFPExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFPExt(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildPtrToInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildPtrToInt(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildIntToPtr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIntToPtr(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildBitCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildAddrSpaceCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildAddrSpaceCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildZExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildZExtOrBitCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildSExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSExtOrBitCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildTruncOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildTruncOrBitCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildCast(long B, int Op, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildPointerCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildPointerCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildIntCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIntCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildFPCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFPCast(long B, long Val, long DestTy, long Name);

        @LibrarySymbol(name = "LLVMBuildICmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildICmp(long B, int Op, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFCmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildFCmp(long B, int Op, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildPhi")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildPhi(long B, long Ty, long Name);

        @LibrarySymbol(name = "LLVMBuildCall")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildCall(long B, long Fn, long Args, int NumArgs, long Name);

        @LibrarySymbol(name = "LLVMBuildSelect")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildSelect(long B, long If, long Then, long Else, long Name);

        /*@LibrarySymbol("LLVMBuildVAArg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildVAArg(long, long, long, long);

        @LibrarySymbol("LLVMBuildExtractElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildExtractElement(long, long, long, long);

        @LibrarySymbol("LLVMBuildInsertElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildInsertElement(long, long, long, long, long);

        @LibrarySymbol("LLVMBuildShuffleVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildShuffleVector(long, long, long, long, long);*/

        @LibrarySymbol(name = "LLVMBuildExtractValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildExtractValue(long B, long AggVal, int Index, long Name);

        @LibrarySymbol(name = "LLVMBuildInsertValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildInsertValue(long B, long AggVal, long EltVal, int Index, long Name);

        /*@LibrarySymbol("LLVMBuildIsNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIsNull(long, long, long);

        @LibrarySymbol("LLVMBuildIsNotNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIsNotNull(long, long, long);

        @LibrarySymbol("LLVMBuildPtrDiff")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildPtrDiff(long, long, long, long);

        @LibrarySymbol("LLVMBuildFence")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, BOOL_AS_INT, LONG_AS_WORD})
        abstract long LLVMBuildFence(long, int, boolean, long);

        @LibrarySymbol("LLVMBuildAtomicRMW")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMBuildAtomicRMW(long, int, long, long, int, boolean);

        @LibrarySymbol("LLVMBuildAtomicCmpXchg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, INT, BOOL_AS_INT})
        abstract long LLVMBuildAtomicCmpXchg(long, long, long, long, int, int, boolean);

        @LibrarySymbol("LLVMIsAtomicSingleThread")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsAtomicSingleThread(long);

        @LibrarySymbol("LLVMSetAtomicSingleThread")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetAtomicSingleThread(long, boolean);

        @LibrarySymbol("LLVMGetCmpXchgSuccessOrdering")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetCmpXchgSuccessOrdering(long);

        @LibrarySymbol("LLVMSetCmpXchgSuccessOrdering")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetCmpXchgSuccessOrdering(long, int);

        @LibrarySymbol("LLVMGetCmpXchgFailureOrdering")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetCmpXchgFailureOrdering(long);

        @LibrarySymbol("LLVMSetCmpXchgFailureOrdering")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetCmpXchgFailureOrdering(long, int);*/

        @LibrarySymbol(name = "LLVMCreateModuleProviderForExistingModule")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateModuleProviderForExistingModule(long M);

        @LibrarySymbol(name = "LLVMDisposeModuleProvider")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeModuleProvider(long M);

        /*@LibrarySymbol("LLVMCreateMemoryBufferWithContentsOfFile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMemoryBufferWithContentsOfFile(long, long, long);

        @LibrarySymbol("LLVMCreateMemoryBufferWithSTDIN")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMemoryBufferWithSTDIN(long, long);*/

        @LibrarySymbol(name = "LLVMCreateMemoryBufferWithMemoryRange")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT})
        abstract long LLVMCreateMemoryBufferWithMemoryRange(long InputData, long InputDataLength, long BufferName, boolean RequiresNullTerminator);

        @LibrarySymbol(name = "LLVMCreateMemoryBufferWithMemoryRangeCopy")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMCreateMemoryBufferWithMemoryRangeCopy(long InputData, long InputDataLength, long BufferName);

        @LibrarySymbol(name = "LLVMGetBufferStart")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBufferStart(long MemBuf);

        @LibrarySymbol(name = "LLVMGetBufferSize")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBufferSize(long MemBuf);

        @LibrarySymbol(name = "LLVMDisposeMemoryBuffer")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeMemoryBuffer(long MemBuf);

        @LibrarySymbol(name = "LLVMGetGlobalPassRegistry")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMGetGlobalPassRegistry();

        @LibrarySymbol(name = "LLVMCreatePassManager")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMCreatePassManager();

        @LibrarySymbol(name = "LLVMCreateFunctionPassManagerForModule")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateFunctionPassManagerForModule(long M);

        @LibrarySymbol(name = "LLVMCreateFunctionPassManager")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateFunctionPassManager(long MP);

        @LibrarySymbol(name = "LLVMRunPassManager")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMRunPassManager(long PM, long M);

        @LibrarySymbol(name = "LLVMInitializeFunctionPassManager")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMInitializeFunctionPassManager(long FPM);

        @LibrarySymbol(name = "LLVMRunFunctionPassManager")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMRunFunctionPassManager(long FPM, long F);

        @LibrarySymbol(name = "LLVMFinalizeFunctionPassManager")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMFinalizeFunctionPassManager(long FPM);

        @LibrarySymbol(name = "LLVMDisposePassManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposePassManager(long PM);

        /*@LibrarySymbol("LLVMStartMultithreaded")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {})
        abstract boolean LLVMStartMultithreaded();

        @LibrarySymbol("LLVMStopMultithreaded")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMStopMultithreaded();

        @LibrarySymbol("LLVMIsMultithreaded")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {})
        abstract boolean LLVMIsMultithreaded();*/

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeCore(R.value());
    }

    /**
     * Deallocate and destroy all ManagedStatic variables.
     */
    public static void LLVMShutdown() {
        Native.INSTANCE.LLVMShutdown();
    }

    /*===-- Error handling ----------------------------------------------------===*/

    /* package-private */
    static long LLVMCreateMessage(String Message) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Message = allocString(arena, Message);
            return Native.INSTANCE.LLVMCreateMessage(c_Message.nativeAddress());
        }
    }

    /* package-private */
    static void LLVMDisposeMessage(long Message) {
        Native.INSTANCE.LLVMDisposeMessage(Message);
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
        return LLVMContextRef.ofNullable(Native.INSTANCE.LLVMContextCreate());
    }

    /**
     * Obtain the global context instance.
     */
    public static LLVMContextRef LLVMGetGlobalContext() {
        return LLVMContextRef.ofNullable(Native.INSTANCE.LLVMGetGlobalContext());
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
        Native.INSTANCE.LLVMContextDispose(C.value());
    }

    /**
     * Return a string representation of the DiagnosticInfo.
     */
    public static String LLVMGetDiagInfoDescription(LLVMDiagnosticInfoRef DI) {
        return addressToLLVMString(Native.INSTANCE.LLVMGetDiagInfoDescription(DI.value()));
    }

    /**
     * Return an enum LLVMDiagnosticSeverity.
     */
    public static LLVMDiagnosticSeverity LLVMGetDiagInfoSeverity(LLVMDiagnosticInfoRef DI) {
        return LLVMDiagnosticSeverity.of(Native.INSTANCE.LLVMGetDiagInfoSeverity(DI.value()));
    }

    public static int /* unsigned */ LLVMGetMDKindIDInContext(LLVMContextRef C, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            int /* unsigned */ SLen = Math.toIntExact(stringLength(c_Name));
            return Native.INSTANCE.LLVMGetMDKindIDInContext(C.value(), c_Name.nativeAddress(), SLen);
        }
    }

    public static int /* unsigned */ LLVMGetMDKindID(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            int /* unsigned */ SLen = Math.toIntExact(stringLength(c_Name));
            return Native.INSTANCE.LLVMGetMDKindID(c_Name.nativeAddress(), SLen);
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
            return Native.INSTANCE.LLVMGetEnumAttributeKindForName(c_Name.nativeAddress(), SLen);
        }
    }

    public static int /* unsigned */ LLVMGetLastEnumAttributeKind() {
        return Native.INSTANCE.LLVMGetLastEnumAttributeKind();
    }

    /**
     * Create an enum attribute.
     */
    public static LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, int /* unsigned */ KindID, long /* uint64_t */ Val) {
        return LLVMAttributeRef.ofNullable(Native.INSTANCE.LLVMCreateEnumAttribute(C.value(), KindID, Val));
    }

    /**
     * Get the unique id corresponding to the enum attribute
     * passed as argument.
     */
    public static int /* unsigned */ LLVMGetEnumAttributeKind(LLVMAttributeRef A) {
        return Native.INSTANCE.LLVMGetEnumAttributeKind(A.value());
    }

    /**
     * Get the enum attribute's value. 0 is returned if none exists.
     */
    public static long /* uint64_t */ LLVMGetEnumAttributeValue(LLVMAttributeRef A) {
        return (int) Native.INSTANCE.LLVMGetEnumAttributeValue(A.value());
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
            return LLVMAttributeRef.ofNullable(Native.INSTANCE.LLVMCreateStringAttribute(
                    C.value(), c_K.nativeAddress(), KLength, c_V.nativeAddress(), VLength));
        }
    }

    /**
     * Get the string attribute's kind.
     */
    public static String LLVMGetStringAttributeKind(LLVMAttributeRef A) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Length = arena.allocate(JAVA_INT);
            long ptr = Native.INSTANCE.LLVMGetStringAttributeKind(A.value(), c_Length.nativeAddress());
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
            long ptr = Native.INSTANCE.LLVMGetStringAttributeValue(A.value(), c_Length.nativeAddress());
            int /* unsigned */ Length = c_Length.get(JAVA_INT, 0);
            return addressToString(ptr, Length);
        }
    }

    /**
     * Check for the different types of attributes.
     */
    public static boolean LLVMIsEnumAttribute(LLVMAttributeRef A) {
        return Native.INSTANCE.LLVMIsEnumAttribute(A.value());
    }

    public static boolean LLVMIsStringAttribute(LLVMAttributeRef A) {
        return Native.INSTANCE.LLVMIsStringAttribute(A.value());
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
            return LLVMModuleRef.ofNullable(Native.INSTANCE.LLVMModuleCreateWithName(c_ModuleID.nativeAddress()));
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
            return LLVMModuleRef.ofNullable(Native.INSTANCE.LLVMModuleCreateWithNameInContext(c_ModuleID.nativeAddress(), C.value()));
        }
    }

    /**
     * Return an exact copy of the specified module.
     */
    public static LLVMModuleRef LLVMCloneModule(LLVMModuleRef M) {
        return LLVMModuleRef.ofNullable(Native.INSTANCE.LLVMCloneModule(M.value()));
    }

    /**
     * Destroy a module instance.
     * <p>
     * This must be called for every created module or memory will be
     * leaked.
     */
    public static void LLVMDisposeModule(LLVMModuleRef M) {
        Native.INSTANCE.LLVMDisposeModule(M.value());
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
    //    return Native.INSTANCE.LLVMGetModuleIdentifier();
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
            Native.INSTANCE.LLVMSetModuleIdentifier(M.value(), c_Ident.nativeAddress(), Len);
        }
    }

    /**
     * Obtain the data layout for a module.
     */
    public static String LLVMGetDataLayoutStr(LLVMModuleRef M) {
        return addressToString(Native.INSTANCE.LLVMGetDataLayoutStr(M.value()));
    }

    /**
     * Obtain the data layout for a module.
     * <p>
     * LLVMGetDataLayout is DEPRECATED, as the name is not only incorrect,
     * but match the name of another method on the module. Prefer the use
     * of LLVMGetDataLayoutStr, which is not ambiguous.
     */
    public static String LLVMGetDataLayout(LLVMModuleRef M) {
        return addressToString(Native.INSTANCE.LLVMGetDataLayout(M.value()));
    }

    /**
     * Set the data layout for a module.
     */
    public static void LLVMSetDataLayout(LLVMModuleRef M, String DataLayoutStr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_DataLayoutStr = allocString(arena, DataLayoutStr);
            Native.INSTANCE.LLVMSetDataLayout(M.value(), c_DataLayoutStr.nativeAddress());
        }
    }

    /**
     * Obtain the target triple for a module.
     */
    public static String LLVMGetTarget(LLVMModuleRef M) {
        return addressToString(Native.INSTANCE.LLVMGetTarget(M.value()));
    }

    /**
     * Set the target triple for a module.
     */
    public static void LLVMSetTarget(LLVMModuleRef M, String Triple) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Triple = allocString(arena, Triple);
            Native.INSTANCE.LLVMSetTarget(M.value(), c_Triple.nativeAddress());
        }
    }

    /**
     * Dump a representation of a module to stderr.
     */
    public static void LLVMDumpModule(LLVMModuleRef M) {
        Native.INSTANCE.LLVMDumpModule(M.value());
    }

    //TODO
    ///**
    // * Print a representation of a module to a file. The ErrorMessage needs to be
    // * disposed with LLVMDisposeMessage. Returns 0 on success, 1 otherwise.
    // */
    //boolean LLVMPrintModuleToFile(LLVMModuleRef M, String Filename, LLVMString *ErrorMessage) {
    //    return Native.INSTANCE.LLVMPrintModuleToFile();
    //}

    /**
     * Return a string representation of the module.
     */
    public static String LLVMPrintModuleToString(LLVMModuleRef M) {
        return addressToLLVMString(Native.INSTANCE.LLVMPrintModuleToString(M.value()));
    }

    ///**
    // * Set inline assembly for a module.
    // *
    // * @see Module::setModuleInlineAsm()
    // */
    //void LLVMSetModuleInlineAsm(LLVMModuleRef M, String Asm) {
    //    return Native.INSTANCE.LLVMSetModuleInlineAsm();
    //}

    /**
     * Obtain the context to which this module is associated.
     */
    public static LLVMContextRef LLVMGetModuleContext(LLVMModuleRef M) {
        return LLVMContextRef.ofNullable(Native.INSTANCE.LLVMGetModuleContext(M.value()));
    }

    ///**
    // * Obtain a Type from a module by its registered name.
    // */
    //LLVMTypeRef LLVMGetTypeByName(LLVMModuleRef M, String Name) {
    //    return Native.INSTANCE.LLVMGetTypeByName();
    //}
    ///**
    // * Obtain the number of operands for named metadata in a module.
    // *
    // * @see llvm::Module::getNamedMetadata()
    // */
    //int /* unsigned */ LLVMGetNamedMetadataNumOperands(LLVMModuleRef M, String Name) {
    //    return Native.INSTANCE.LLVMGetNamedMetadataNumOperands();
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
    //    return Native.INSTANCE.LLVMGetNamedMetadataOperands();
    //}
    ///**
    // * Add an operand to named metadata.
    // *
    // * @see llvm::Module::getNamedMetadata()
    // * @see llvm::MDNode::addOperand()
    // */
    //void LLVMAddNamedMetadataOperand(LLVMModuleRef M, String Name, LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMAddNamedMetadataOperand();
    //}

    /**
     * Add a function to a module under a specified name.
     */
    public static LLVMValueRef LLVMAddFunction(LLVMModuleRef M, String Name, LLVMTypeRef FunctionTy) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMAddFunction(
                    M.value(), c_Name.nativeAddress(), FunctionTy.value()));
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
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetNamedFunction(
                    M.value(), c_Name.nativeAddress()));
        }
    }

    ///**
    // * Obtain an iterator to the first Function in a Module.
    // *
    // * @see llvm::Module::begin()
    // */
    //LLVMValueRef LLVMGetFirstFunction(LLVMModuleRef M) {
    //    return Native.INSTANCE.LLVMGetFirstFunction();
    //}
    ///**
    // * Obtain an iterator to the last Function in a Module.
    // *
    // * @see llvm::Module::end()
    // */
    //LLVMValueRef LLVMGetLastFunction(LLVMModuleRef M) {
    //    return Native.INSTANCE.LLVMGetLastFunction();
    //}
    ///**
    // * Advance a Function iterator to the next Native.INSTANCE.
    // *
    // * Returns NULL if the iterator was already at the end and there are no more
    // * functions.
    // */
    //LLVMValueRef LLVMGetNextFunction(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetNextFunction();
    //}
    ///**
    // * Decrement a Function iterator to the previous Native.INSTANCE.
    // *
    // * Returns NULL if the iterator was already at the beginning and there are
    // * no previous functions.
    // */
    //LLVMValueRef LLVMGetPreviousFunction(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetPreviousFunction();
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
        return LLVMTypeKind.of(Native.INSTANCE.LLVMGetTypeKind(Ty.value()));
    }

    /**
     * Whether the type has a known size.
     * <p>
     * Things that don't have a size are abstract types, labels, and void.a
     */
    public static boolean LLVMTypeIsSized(LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMTypeIsSized(Ty.value());
    }

    /**
     * Obtain the context to which this type instance is associated.
     */
    public static LLVMContextRef LLVMGetTypeContext(LLVMTypeRef Ty) {
        return LLVMContextRef.ofNullable(Native.INSTANCE.LLVMGetTypeContext(Ty.value()));
    }

    /**
     * Dump a representation of a type to stderr.
     */
    public static void LLVMDumpType(LLVMTypeRef Val) {
        Native.INSTANCE.LLVMDumpType(Val.value());
    }

    /**
     * Return a string representation of the type.
     */
    public static String LLVMPrintTypeToString(LLVMTypeRef Val) {
        return addressToLLVMString(Native.INSTANCE.LLVMPrintTypeToString(Val.value()));
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
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt1TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt8TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt16TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt32TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt64TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt128TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, int /* unsigned */ NumBits) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntTypeInContext(C.value(), NumBits));
    }

    /**
     * Obtain an integer type from the global context with a specified bit
     * width.
     */
    public static LLVMTypeRef LLVMInt1Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt1Type());
    }

    public static LLVMTypeRef LLVMInt8Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt8Type());
    }

    public static LLVMTypeRef LLVMInt16Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt16Type());
    }

    public static LLVMTypeRef LLVMInt32Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt32Type());
    }

    public static LLVMTypeRef LLVMInt64Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt64Type());
    }

    public static LLVMTypeRef LLVMInt128Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMInt128Type());
    }

    public static LLVMTypeRef LLVMIntType(int /* unsigned */ NumBits) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntType(NumBits));
    }

    public static int /* unsigned */ LLVMGetIntTypeWidth(LLVMTypeRef IntegerTy) {
        return Native.INSTANCE.LLVMGetIntTypeWidth(IntegerTy.value());
    }

    /*
     * @defgroup LLVMCCoreTypeFloat Floating Point Types
     */

    /**
     * Obtain a 16-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMHalfTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMHalfTypeInContext(C.value()));
    }

    /**
     * Obtain a 32-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMFloatTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMFloatTypeInContext(C.value()));
    }

    /**
     * Obtain a 64-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMDoubleTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMDoubleTypeInContext(C.value()));
    }

    /**
     * Obtain a 80-bit floating point type (X87) from a context.
     */
    public static LLVMTypeRef LLVMX86FP80TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMX86FP80TypeInContext(C.value()));
    }

    /**
     * Obtain a 128-bit floating point type (112-bit mantissa) from a
     * context.
     */
    public static LLVMTypeRef LLVMFP128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMFP128TypeInContext(C.value()));
    }

    /**
     * Obtain a 128-bit floating point type (two 64-bits) from a context.
     */
    public static LLVMTypeRef LLVMPPCFP128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMPPCFP128TypeInContext(C.value()));
    }

    /**
     * Obtain a floating point type from the global context.
     * <p>
     * These map to the functions in this group of the same name.
     */
    public static LLVMTypeRef LLVMHalfType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMHalfType());
    }

    public static LLVMTypeRef LLVMFloatType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMFloatType());
    }

    public static LLVMTypeRef LLVMDoubleType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMDoubleType());
    }

    public static LLVMTypeRef LLVMX86FP80Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMX86FP80Type());
    }

    public static LLVMTypeRef LLVMFP128Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMFP128Type());
    }

    public static LLVMTypeRef LLVMPPCFP128Type() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMPPCFP128Type());
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
            return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMFunctionType(
                    ReturnType.value(), c_ParamTypes.nativeAddress(), ParamCount, IsVarArg));
        }
    }

    /**
     * Returns whether a function type is variadic.
     */
    public static boolean LLVMIsFunctionVarArg(LLVMTypeRef FunctionTy) {
        return Native.INSTANCE.LLVMIsFunctionVarArg(FunctionTy.value());
    }

    /**
     * Obtain the Type this function Type returns.
     */
    public static LLVMTypeRef LLVMGetReturnType(LLVMTypeRef FunctionTy) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMGetReturnType(FunctionTy.value()));
    }

    ///**
    // * Obtain the number of parameters this function accepts.
    // */
    //int /* unsigned */ LLVMCountParamTypes(LLVMTypeRef FunctionTy) {
    //    return Native.INSTANCE.LLVMCountParamTypes();
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
    //    return Native.INSTANCE.LLVMGetParamTypes();
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
            return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMStructTypeInContext(
                    C.value(), c_ElementTypes.nativeAddress(), ElementCount, Packed));
        }
    }

    /**
     * Create a new structure type in the global context.
     */
    public static LLVMTypeRef LLVMStructType(LLVMTypeRef[] ElementTypes, boolean Packed) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ElementTypes = allocArray(arena, ElementTypes);
            int /* unsigned */ ElementCount = arrayLength(ElementTypes);
            return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMStructType(
                    c_ElementTypes.nativeAddress(), ElementCount, Packed));
        }
    }

    /**
     * Create an empty structure in a context having a specified name.
     */
    public static LLVMTypeRef LLVMStructCreateNamed(LLVMContextRef C, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMStructCreateNamed
                    (C.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Obtain the name of a structure.
     */
    public static String LLVMGetStructName(LLVMTypeRef Ty) {
        return addressToString(Native.INSTANCE.LLVMGetStructName(Ty.value()));
    }

    /**
     * Set the contents of a structure type.
     */
    public static void LLVMStructSetBody(LLVMTypeRef StructTy, LLVMTypeRef[] ElementTypes, boolean Packed) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ElementTypes = allocArray(arena, ElementTypes);
            int /* unsigned */ ElementCount = arrayLength(ElementTypes);
            Native.INSTANCE.LLVMStructSetBody(StructTy.value(), c_ElementTypes.nativeAddress(), ElementCount, Packed);
        }
    }

    ///**
    // * Get the number of elements defined inside the structure.
    // *
    // * @see llvm::StructType::getNumElements()
    // */
    //int /* unsigned */ LLVMCountStructElementTypes(LLVMTypeRef StructTy) {
    //    return Native.INSTANCE.LLVMCountStructElementTypes();
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
    //    return Native.INSTANCE.LLVMGetStructElementTypes();
    //}
    ///**
    // * Get the type of the element at a given index in the structure.
    // *
    // * @see llvm::StructType::getTypeAtIndex()
    // */
    //LLVMTypeRef LLVMStructGetTypeAtIndex(LLVMTypeRef StructTy, int /* unsigned */ i) {
    //    return Native.INSTANCE.LLVMStructGetTypeAtIndex();
    //}

    /**
     * Determine whether a structure is packed.
     */
    public static boolean LLVMIsPackedStruct(LLVMTypeRef StructTy) {
        return Native.INSTANCE.LLVMIsPackedStruct(StructTy.value());
    }

    /**
     * Determine whether a structure is opaque.
     */
    public static boolean LLVMIsOpaqueStruct(LLVMTypeRef StructTy) {
        return Native.INSTANCE.LLVMIsOpaqueStruct(StructTy.value());
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
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMGetElementType(Ty.value()));
    }

    /**
     * Create a fixed size array type that refers to a specific type.
     * <p>
     * The created type will exist in the context that its element type
     * exists in.
     */
    public static LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMArrayType(ElementType.value(), ElementCount));
    }

    /**
     * Obtain the length of an array type.
     * <p>
     * This only works on types that represent arrays.
     */
    public static int /* unsigned */ LLVMGetArrayLength(LLVMTypeRef ArrayTy) {
        return Native.INSTANCE.LLVMGetArrayLength(ArrayTy.value());
    }

    /**
     * Create a pointer type that points to a defined type.
     * <p>
     * The created type will exist in the context that its pointee type
     * exists in.
     */
    public static LLVMTypeRef LLVMPointerType(LLVMTypeRef ElementType, int /* unsigned */ AddressSpace) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMPointerType(ElementType.value(), AddressSpace));
    }

    /**
     * Obtain the address space of a pointer type.
     * <p>
     * This only works on types that represent pointers.
     */
    public static int /* unsigned */ LLVMGetPointerAddressSpace(LLVMTypeRef PointerTy) {
        return Native.INSTANCE.LLVMGetPointerAddressSpace(PointerTy.value());
    }

    /**
     * Create a vector type that contains a defined type and has a specific
     * number of elements.
     * <p>
     * The created type will exist in the context thats its element type
     * exists in.
     */
    public static LLVMTypeRef LLVMVectorType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMVectorType(ElementType.value(), ElementCount));
    }

    /**
     * Obtain the number of elements in a vector type.
     * <p>
     * This only works on types that represent vectors.
     */
    public static int /* unsigned */ LLVMGetVectorSize(LLVMTypeRef VectorTy) {
        return Native.INSTANCE.LLVMGetVectorSize(VectorTy.value());
    }

    /*
     * @defgroup LLVMCCoreTypeOther Other Types
     */

    /**
     * Create a void type in a context.
     */
    public static LLVMTypeRef LLVMVoidTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMVoidTypeInContext(C.value()));
    }

    /**
     * Create a label type in a context.
     */
    public static LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMLabelTypeInContext(C.value()));
    }

    /**
     * Create a X86 MMX type in a context.
     */
    public static LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMX86MMXTypeInContext(C.value()));
    }

    /**
     * Create a void type in the global context.
     */
    public static LLVMTypeRef LLVMVoidType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMVoidType());
    }

    /**
     * Create a label type in the global context.
     */
    public static LLVMTypeRef LLVMLabelType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMLabelType());
    }

    /**
     * Create a X86 MMX type in the global context.
     */
    public static LLVMTypeRef LLVMX86MMXType() {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMX86MMXType());
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
    //    return Native.INSTANCE.LLVMTypeOf();
    //}
    ///**
    // * Obtain the enumerated type of a Value instance.
    // *
    // * @see llvm::Value::getValueID()
    // */
    //LLVMValueKind LLVMGetValueKind(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMGetValueKind();
    //}
    ///**
    // * Obtain the string name of a value.
    // *
    // * @see llvm::Value::getName()
    // */
    //String LLVMGetValueName(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMGetValueName();
    //}
    ///**
    // * Set the string name of a value.
    // *
    // * @see llvm::Value::setName()
    // */
    //void LLVMSetValueName(LLVMValueRef Val, String Name) {
    //    return Native.INSTANCE.LLVMSetValueName();
    //}
    ///**
    // * Dump a representation of a value to stderr.
    // *
    // * @see llvm::Value::dump()
    // */
    //void LLVMDumpValue(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMDumpValue();
    //}

    /**
     * Return a string representation of the value. Use
     * LLVMDisposeMessage to free the string.
     */
    public static String LLVMPrintValueToString(LLVMValueRef Val) {
        return addressToLLVMString(Native.INSTANCE.LLVMPrintValueToString(Val.value()));
    }

    ///**
    // * Replace all uses of a value with another one.
    // *
    // * @see llvm::Value::replaceAllUsesWith()
    // */
    //void LLVMReplaceAllUsesWith(LLVMValueRef OldVal, LLVMValueRef NewVal) {
    //    return Native.INSTANCE.LLVMReplaceAllUsesWith();
    //}
    ///**
    // * Determine whether the specified value instance is constant.
    // */
    //boolean LLVMIsConstant(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMIsConstant();
    //}
    ///**
    // * Determine whether a value instance is undefined.
    // */
    //boolean LLVMIsUndef(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMIsUndef();
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
    //    return Native.INSTANCE.LLVMGetFirstUse();
    //}
    ///**
    // * Obtain the next use of a value.
    // *
    // * This effectively advances the iterator. It returns NULL if you are on
    // * the final use and no more are available.
    // */
    //LLVMUseRef LLVMGetNextUse(LLVMUseRef U) {
    //    return Native.INSTANCE.LLVMGetNextUse();
    //}
    ///**
    // * Obtain the user value for a user.
    // *
    // * The returned value corresponds to a llvm::User type.
    // *
    // * @see llvm::Use::getUser()
    // */
    //LLVMValueRef LLVMGetUser(LLVMUseRef U) {
    //    return Native.INSTANCE.LLVMGetUser();
    //}
    ///**
    // * Obtain the value this use corresponds to.
    // *
    // * @see llvm::Use::get().
    // */
    //LLVMValueRef LLVMGetUsedValue(LLVMUseRef U) {
    //    return Native.INSTANCE.LLVMGetUsedValue();
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
    //    return Native.INSTANCE.LLVMGetOperand();
    //}
    ///**
    // * Obtain the use of an operand at a specific index in a llvm::User value.
    // *
    // * @see llvm::User::getOperandUse()
    // */
    //LLVMUseRef LLVMGetOperandUse(LLVMValueRef Val, int /* unsigned */ Index) {
    //    return Native.INSTANCE.LLVMGetOperandUse();
    //}
    ///**
    // * Set an operand at a specific index in a llvm::User value.
    // *
    // * @see llvm::User::setOperand()
    // */
    //void LLVMSetOperand(LLVMValueRef User, int /* unsigned */ Index, LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMSetOperand();
    //}
    ///**
    // * Obtain the number of operands in a llvm::User value.
    // *
    // * @see llvm::User::getNumOperands()
    // */
    //int LLVMGetNumOperands(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMGetNumOperands();
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
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstNull(Ty.value()));
    }

    /**
     * Obtain a constant value referring to the instance of a type
     * consisting of all ones.
     * <p>
     * This is only valid for integer types.
     */
    public static LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstAllOnes(Ty.value()));
    }

    /**
     * Obtain a constant value referring to an undefined value of a type.
     */
    public static LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetUndef(Ty.value()));
    }

    ///**
    // * Determine whether a value instance is null.
    // *
    // * @see llvm::Constant::isNullValue()
    // */
    //boolean LLVMIsNull(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMIsNull();
    //}
    ///**
    // * Obtain a constant that is a constant pointer pointing to NULL for a
    // * specified type.
    // */
    //LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty) {
    //    return Native.INSTANCE.LLVMConstPointerNull();
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
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstInt(IntTy.value(), N, SignExtend));
    }

    /**
     * Obtain a constant value for an integer of arbitrary precision.
     */
    public static LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy, long... /* uint64_t */ Words) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Words = allocArray(arena, Words);
            int /* unsigned */ NumWords = arrayLength(Words);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstIntOfArbitraryPrecision(
                    IntTy.value(), NumWords, c_Words.nativeAddress()));
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
    //    return Native.INSTANCE.LLVMConstIntOfString();
    //}
    ///**
    // * Obtain a constant value for an integer parsed from a string with
    // * specified length.
    // *
    // * @see llvm::ConstantInt::get()
    // */
    //LLVMValueRef LLVMConstIntOfStringAndSize(LLVMTypeRef IntTy, String Text, int /* unsigned */ SLen, byte /* uint8_t */ Radix) {
    //    return Native.INSTANCE.LLVMConstIntOfStringAndSize();
    //}
    ///**
    // * Obtain a constant value referring to a double floating point value.
    // */
    //LLVMValueRef LLVMConstReal(LLVMTypeRef RealTy, double N) {
    //    return Native.INSTANCE.LLVMConstReal();
    //}
    ///**
    // * Obtain a constant for a floating point value parsed from a string.
    // *
    // * A similar API, LLVMConstRealOfStringAndSize is also available. It
    // * should be used if the input string's length is known.
    // */
    //LLVMValueRef LLVMConstRealOfString(LLVMTypeRef RealTy, String Text) {
    //    return Native.INSTANCE.LLVMConstRealOfString();
    //}
    ///**
    // * Obtain a constant for a floating point value parsed from a string.
    // */
    //LLVMValueRef LLVMConstRealOfStringAndSize(LLVMTypeRef RealTy, String Text, int /* unsigned */ SLen) {
    //    return Native.INSTANCE.LLVMConstRealOfStringAndSize();
    //}
    ///**
    // * Obtain the zero extended value for an integer constant value.
    // *
    // * @see llvm::ConstantInt::getZExtValue()
    // */
    //long /* unsigned long long */ LLVMConstIntGetZExtValue(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstIntGetZExtValue();
    //}
    ///**
    // * Obtain the sign extended value for an integer constant value.
    // *
    // * @see llvm::ConstantInt::getSExtValue()
    // */
    //long /* long long */ LLVMConstIntGetSExtValue(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstIntGetSExtValue();
    //}
    ///**
    // * Obtain the double value for an floating point constant value.
    // * losesInfo indicates if some precision was lost in the conversion.
    // *
    // * @see llvm::ConstantFP::getDoubleValue
    // */
    //double LLVMConstRealGetDouble(LLVMValueRef ConstantVal, boolean *losesInfo) {
    //    return Native.INSTANCE.LLVMConstRealGetDouble();
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
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstStringInContext(
                    C.value(), c_Str.nativeAddress(), Length, DontNullTerminate));
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
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMConstString(
                    c_Str.nativeAddress(), Length, DontNullTerminate));
        }
    }

    ///**
    // * Returns true if the specified constant is an array of i8.
    // *
    // * @see ConstantDataSequential::getAsString()
    // */
    //boolean LLVMIsConstantString(LLVMValueRef c) {
    //    return Native.INSTANCE.LLVMIsConstantString();
    //}
    ///**
    // * Get the given constant data sequential as a string.
    // *
    // * @see ConstantDataSequential::getAsString()
    // */
    //String LLVMGetAsString(LLVMValueRef c, long /* size_t */ *Length) {
    //    return Native.INSTANCE.LLVMGetAsString();
    //}
    ///**
    // * Create an anonymous ConstantStruct with the specified values.
    // *
    // * @see llvm::ConstantStruct::getAnon()
    // */
    //LLVMValueRef LLVMConstStructInContext(LLVMContextRef C, LLVMValueRef *ConstantVals, int /* unsigned */ Count, boolean Packed) {
    //    return Native.INSTANCE.LLVMConstStructInContext();
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
    //    return Native.INSTANCE.LLVMConstStruct();
    //}
    ///**
    // * Create a ConstantArray from values.
    // *
    // * @see llvm::ConstantArray::get()
    // */
    //LLVMValueRef LLVMConstArray(LLVMTypeRef ElementTy, LLVMValueRef *ConstantVals, int /* unsigned */ Length) {
    //    return Native.INSTANCE.LLVMConstArray();
    //}
    ///**
    // * Create a non-anonymous ConstantStruct from values.
    // *
    // * @see llvm::ConstantStruct::get()
    // */
    //LLVMValueRef LLVMConstNamedStruct(LLVMTypeRef StructTy, LLVMValueRef *ConstantVals, int /* unsigned */ Count) {
    //    return Native.INSTANCE.LLVMConstNamedStruct();
    //}
    ///**
    // * Get an element at specified index as a constant.
    // *
    // * @see ConstantDataSequential::getElementAsConstant()
    // */
    //LLVMValueRef LLVMGetElementAsConstant(LLVMValueRef C, int /* unsigned */ idx) {
    //    return Native.INSTANCE.LLVMGetElementAsConstant();
    //}
    ///**
    // * Create a ConstantVector from values.
    // *
    // * @see llvm::ConstantVector::get()
    // */
    //LLVMValueRef LLVMConstVector(LLVMValueRef *ScalarConstantVals, int /* unsigned */ Size) {
    //    return Native.INSTANCE.LLVMConstVector();
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
    //    return Native.INSTANCE.LLVMGetConstOpcode();
    //}
    //LLVMValueRef LLVMAlignOf(LLVMTypeRef Ty) {
    //    return Native.INSTANCE.LLVMAlignOf();
    //}
    //LLVMValueRef LLVMSizeOf(LLVMTypeRef Ty) {
    //    return Native.INSTANCE.LLVMSizeOf();
    //}
    //LLVMValueRef LLVMConstNeg(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstNeg();
    //}
    //LLVMValueRef LLVMConstNSWNeg(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstNSWNeg();
    //}
    //LLVMValueRef LLVMConstNUWNeg(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstNUWNeg();
    //}
    //LLVMValueRef LLVMConstFNeg(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstFNeg();
    //}
    //LLVMValueRef LLVMConstNot(LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMConstNot();
    //}
    //LLVMValueRef LLVMConstAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstAdd();
    //}
    //LLVMValueRef LLVMConstNSWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNSWAdd();
    //}
    //LLVMValueRef LLVMConstNUWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNUWAdd();
    //}
    //LLVMValueRef LLVMConstFAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFAdd();
    //}
    //LLVMValueRef LLVMConstSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstSub();
    //}
    //LLVMValueRef LLVMConstNSWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNSWSub();
    //}
    //LLVMValueRef LLVMConstNUWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNUWSub();
    //}
    //LLVMValueRef LLVMConstFSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFSub();
    //}
    //LLVMValueRef LLVMConstMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstMul();
    //}
    //LLVMValueRef LLVMConstNSWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNSWMul();
    //}
    //LLVMValueRef LLVMConstNUWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstNUWMul();
    //}
    //LLVMValueRef LLVMConstFMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFMul();
    //}
    //LLVMValueRef LLVMConstUDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstUDiv();
    //}
    //LLVMValueRef LLVMConstSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstSDiv();
    //}
    //LLVMValueRef LLVMConstExactSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstExactSDiv();
    //}
    //LLVMValueRef LLVMConstFDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFDiv();
    //}
    //LLVMValueRef LLVMConstURem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstURem();
    //}
    //LLVMValueRef LLVMConstSRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstSRem();
    //}
    //LLVMValueRef LLVMConstFRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFRem();
    //}
    //LLVMValueRef LLVMConstAnd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstAnd();
    //}
    //LLVMValueRef LLVMConstOr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstOr();
    //}
    //LLVMValueRef LLVMConstXor(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstXor();
    //}
    //LLVMValueRef LLVMConstICmp(LLVMIntPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstICmp();
    //}
    //LLVMValueRef LLVMConstFCmp(LLVMRealPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstFCmp();
    //}
    //LLVMValueRef LLVMConstShl(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstShl();
    //}
    //LLVMValueRef LLVMConstLShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstLShr();
    //}
    //LLVMValueRef LLVMConstAShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
    //    return Native.INSTANCE.LLVMConstAShr();
    //}
    //LLVMValueRef LLVMConstGEP(LLVMValueRef ConstantVal, LLVMValueRef *ConstantIndices, int /* unsigned */ NumIndices) {
    //    return Native.INSTANCE.LLVMConstGEP();
    //}
    //LLVMValueRef LLVMConstInBoundsGEP(LLVMValueRef ConstantVal, LLVMValueRef *ConstantIndices, int /* unsigned */ NumIndices) {
    //    return Native.INSTANCE.LLVMConstInBoundsGEP();
    //}
    //LLVMValueRef LLVMConstTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstTrunc();
    //}
    //LLVMValueRef LLVMConstSExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstSExt();
    //}
    //LLVMValueRef LLVMConstZExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstZExt();
    //}
    //LLVMValueRef LLVMConstFPTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstFPTrunc();
    //}
    //LLVMValueRef LLVMConstFPExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstFPExt();
    //}
    //LLVMValueRef LLVMConstUIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstUIToFP();
    //}
    //LLVMValueRef LLVMConstSIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstSIToFP();
    //}
    //LLVMValueRef LLVMConstFPToUI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstFPToUI();
    //}
    //LLVMValueRef LLVMConstFPToSI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstFPToSI();
    //}
    //LLVMValueRef LLVMConstPtrToInt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstPtrToInt();
    //}
    //LLVMValueRef LLVMConstIntToPtr(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstIntToPtr();
    //}
    //LLVMValueRef LLVMConstBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstBitCast();
    //}
    //LLVMValueRef LLVMConstAddrSpaceCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstAddrSpaceCast();
    //}
    //LLVMValueRef LLVMConstZExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstZExtOrBitCast();
    //}
    //LLVMValueRef LLVMConstSExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstSExtOrBitCast();
    //}
    //LLVMValueRef LLVMConstTruncOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstTruncOrBitCast();
    //}
    //LLVMValueRef LLVMConstPointerCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstPointerCast();
    //}
    //LLVMValueRef LLVMConstIntCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType, boolean isSigned) {
    //    return Native.INSTANCE.LLVMConstIntCast();
    //}
    //LLVMValueRef LLVMConstFPCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
    //    return Native.INSTANCE.LLVMConstFPCast();
    //}
    //LLVMValueRef LLVMConstSelect(LLVMValueRef ConstantCondition, LLVMValueRef ConstantIfTrue, LLVMValueRef ConstantIfFalse) {
    //    return Native.INSTANCE.LLVMConstSelect();
    //}
    //LLVMValueRef LLVMConstExtractElement(LLVMValueRef VectorConstant, LLVMValueRef IndexConstant) {
    //    return Native.INSTANCE.LLVMConstExtractElement();
    //}
    //LLVMValueRef LLVMConstInsertElement(LLVMValueRef VectorConstant, LLVMValueRef ElementValueConstant, LLVMValueRef IndexConstant) {
    //    return Native.INSTANCE.LLVMConstInsertElement();
    //}
    //LLVMValueRef LLVMConstShuffleVector(LLVMValueRef VectorAConstant, LLVMValueRef VectorBConstant, LLVMValueRef MaskConstant) {
    //    return Native.INSTANCE.LLVMConstShuffleVector();
    //}
    //LLVMValueRef LLVMConstExtractValue(LLVMValueRef AggConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return Native.INSTANCE.LLVMConstExtractValue();
    //}
    //LLVMValueRef LLVMConstInsertValue(LLVMValueRef AggConstant, LLVMValueRef ElementValueConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return Native.INSTANCE.LLVMConstInsertValue();
    //}
    //LLVMValueRef LLVMConstInlineAsm(LLVMTypeRef Ty, String AsmString, String Constraints, boolean HasSideEffects, boolean IsAlignStack) {
    //    return Native.INSTANCE.LLVMConstInlineAsm();
    //}
    //LLVMValueRef LLVMBlockAddress(LLVMValueRef F, LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMBlockAddress();
    //}

    /*
     * @defgroup LLVMCCoreValueConstantGlobals Global Values
     *
     * This group contains functions that operate on global values. Functions in
     * this group relate to functions in the llvm::GlobalValue class tree.
     */

    public static LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global) {
        return LLVMModuleRef.ofNullable(Native.INSTANCE.LLVMGetGlobalParent(Global.value()));
    }

    public static boolean LLVMIsDeclaration(LLVMValueRef Global) {
        return Native.INSTANCE.LLVMIsDeclaration(Global.value());
    }

    public static LLVMLinkage LLVMGetLinkage(LLVMValueRef Global) {
        return LLVMLinkage.of(Native.INSTANCE.LLVMGetLinkage(Global.value()));
    }

    public static void LLVMSetLinkage(LLVMValueRef Global, LLVMLinkage Linkage) {
        Native.INSTANCE.LLVMSetLinkage(Global.value(), Linkage.value());
    }

    public static String LLVMGetSection(LLVMValueRef Global) {
        return addressToString(Native.INSTANCE.LLVMGetSection(Global.value()));
    }

    public static void LLVMSetSection(LLVMValueRef Global, String Section) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Section = allocString(arena, Section);
            Native.INSTANCE.LLVMSetSection(Global.value(), c_Section.nativeAddress());
        }
    }

    public static LLVMVisibility LLVMGetVisibility(LLVMValueRef Global) {
        return LLVMVisibility.of(Native.INSTANCE.LLVMGetVisibility(Global.value()));
    }

    public static void LLVMSetVisibility(LLVMValueRef Global, LLVMVisibility Viz) {
        Native.INSTANCE.LLVMSetVisibility(Global.value(), Viz.value());
    }

    public static LLVMDLLStorageClass LLVMGetDLLStorageClass(LLVMValueRef Global) {
        return LLVMDLLStorageClass.of(Native.INSTANCE.LLVMGetDLLStorageClass(Global.value()));
    }

    public static void LLVMSetDLLStorageClass(LLVMValueRef Global, LLVMDLLStorageClass Class) {
        Native.INSTANCE.LLVMSetDLLStorageClass(Global.value(), Class.value());
    }

    //boolean LLVMHasUnnamedAddr(LLVMValueRef Global) {
    //    return Native.INSTANCE.LLVMHasUnnamedAddr();
    //}
    //void LLVMSetUnnamedAddr(LLVMValueRef Global, boolean HasUnnamedAddr) {
    //    return Native.INSTANCE.LLVMSetUnnamedAddr();
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
        return Native.INSTANCE.LLVMGetAlignment(V.value());
    }

    /**
     * Set the preferred alignment of the value.
     */
    public static void LLVMSetAlignment(LLVMValueRef V, int /* unsigned */ Bytes) {
        Native.INSTANCE.LLVMSetAlignment(V.value(), Bytes);
    }

    /*
     * @defgroup LLVMCoreValueConstantGlobalVariable Global Variables
     *
     * This group contains functions that operate on global variable values.
     */

    //LLVMValueRef LLVMAddGlobal(LLVMModuleRef M, LLVMTypeRef Ty, String Name) {
    //    return Native.INSTANCE.LLVMAddGlobal();
    //}
    //LLVMValueRef LLVMAddGlobalInAddressSpace(LLVMModuleRef M, LLVMTypeRef Ty, String Name, int /* unsigned */ AddressSpace) {
    //    return Native.INSTANCE.LLVMAddGlobalInAddressSpace();
    //}
    //LLVMValueRef LLVMGetNamedGlobal(LLVMModuleRef M, String Name) {
    //    return Native.INSTANCE.LLVMGetNamedGlobal();
    //}
    //LLVMValueRef LLVMGetFirstGlobal(LLVMModuleRef M) {
    //    return Native.INSTANCE.LLVMGetFirstGlobal();
    //}
    //LLVMValueRef LLVMGetLastGlobal(LLVMModuleRef M) {
    //    return Native.INSTANCE.LLVMGetLastGlobal();
    //}
    //LLVMValueRef LLVMGetNextGlobal(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMGetNextGlobal();
    //}
    //LLVMValueRef LLVMGetPreviousGlobal(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMGetPreviousGlobal();
    //}
    //void LLVMDeleteGlobal(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMDeleteGlobal();
    //}
    //LLVMValueRef LLVMGetInitializer(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMGetInitializer();
    //}
    //void LLVMSetInitializer(LLVMValueRef GlobalVar, LLVMValueRef ConstantVal) {
    //    return Native.INSTANCE.LLVMSetInitializer();
    //}
    //boolean LLVMIsThreadLocal(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMIsThreadLocal();
    //}
    //void LLVMSetThreadLocal(LLVMValueRef GlobalVar, boolean IsThreadLocal) {
    //    return Native.INSTANCE.LLVMSetThreadLocal();
    //}
    //boolean LLVMIsGlobalConstant(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMIsGlobalConstant();
    //}
    //void LLVMSetGlobalConstant(LLVMValueRef GlobalVar, boolean IsConstant) {
    //    return Native.INSTANCE.LLVMSetGlobalConstant();
    //}
    //LLVMThreadLocalMode LLVMGetThreadLocalMode(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMGetThreadLocalMode();
    //}
    //void LLVMSetThreadLocalMode(LLVMValueRef GlobalVar, LLVMThreadLocalMode Mode) {
    //    return Native.INSTANCE.LLVMSetThreadLocalMode();
    //}
    //boolean LLVMIsExternallyInitialized(LLVMValueRef GlobalVar) {
    //    return Native.INSTANCE.LLVMIsExternallyInitialized();
    //}
    //void LLVMSetExternallyInitialized(LLVMValueRef GlobalVar, boolean IsExtInit) {
    //    return Native.INSTANCE.LLVMSetExternallyInitialized();
    //}

    /*
     * @defgroup LLVMCoreValueConstantGlobalAlias Global Aliases
     *
     * This group contains function that operate on global alias values.
     */

    //LLVMValueRef LLVMAddAlias(LLVMModuleRef M, LLVMTypeRef Ty, LLVMValueRef Aliasee, String Name) {
    //    return Native.INSTANCE.LLVMAddAlias();
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
    //    return Native.INSTANCE.LLVMDeleteFunction();
    //}
    ///**
    // * Check whether the given function has a personality Native.INSTANCE.
    // *
    // * @see llvm::Function::hasPersonalityFn()
    // */
    //boolean LLVMHasPersonalityFn(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMHasPersonalityFn();
    //}
    ///**
    // * Obtain the personality function attached to the Native.INSTANCE.
    // *
    // * @see llvm::Function::getPersonalityFn()
    // */
    //LLVMValueRef LLVMGetPersonalityFn(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetPersonalityFn();
    //}
    ///**
    // * Set the personality function attached to the Native.INSTANCE.
    // *
    // * @see llvm::Function::setPersonalityFn()
    // */
    //void LLVMSetPersonalityFn(LLVMValueRef Fn, LLVMValueRef PersonalityFn) {
    //    return Native.INSTANCE.LLVMSetPersonalityFn();
    //}
    ///**
    // * Obtain the ID number from a function instance.
    // *
    // * @see llvm::Function::getIntrinsicID()
    // */
    //int /* unsigned */ LLVMGetIntrinsicID(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetIntrinsicID();
    //}

    /**
     * Obtain the calling function of a Native.INSTANCE.
     * <p>
     * The returned value corresponds to the LLVMCallConv enumeration.
     */
    public static LLVMCallConv LLVMGetFunctionCallConv(LLVMValueRef Fn) {
        return LLVMCallConv.of(Native.INSTANCE.LLVMGetFunctionCallConv(Fn.value()));
    }

    /**
     * Set the calling convention of a Native.INSTANCE.
     *
     * @param Fn Function to operate on
     * @param CC LLVMCallConv to set calling convention to
     */
    public static void LLVMSetFunctionCallConv(LLVMValueRef Fn, LLVMCallConv CC) {
        Native.INSTANCE.LLVMSetFunctionCallConv(Fn.value(), CC.value());
    }

    ///**
    // * Obtain the name of the garbage collector to use during code
    // * generation.
    // *
    // * @see llvm::Function::getGC()
    // */
    //String LLVMGetGC(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetGC();
    //}
    ///**
    // * Define the garbage collector to use during code generation.
    // *
    // * @see llvm::Function::setGC()
    // */
    //void LLVMSetGC(LLVMValueRef Fn, String Name) {
    //    return Native.INSTANCE.LLVMSetGC();
    //}

    /**
     * Add an attribute to a Native.INSTANCE.
     */
    public static void LLVMAddFunctionAttr(LLVMValueRef Fn, int /* LLVMAttribute */ PA) {
        Native.INSTANCE.LLVMAddFunctionAttr(Fn.value(), PA);
    }

    public static void LLVMAddAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, LLVMAttributeRef A) {
        Native.INSTANCE.LLVMAddAttributeAtIndex(F.value(), Idx, A.value());
    }

    //LLVMAttributeRef LLVMGetEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return Native.INSTANCE.LLVMGetEnumAttributeAtIndex();
    //}
    //LLVMAttributeRef LLVMGetStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return Native.INSTANCE.LLVMGetStringAttributeAtIndex();
    //}
    //void LLVMRemoveEnumAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return Native.INSTANCE.LLVMRemoveEnumAttributeAtIndex();
    //}
    //void LLVMRemoveStringAttributeAtIndex(LLVMValueRef F, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return Native.INSTANCE.LLVMRemoveStringAttributeAtIndex();
    //}
    ///**
    // * Add a target-dependent attribute to a function
    // * @see llvm::AttrBuilder::addAttribute()
    // */
    //void LLVMAddTargetDependentFunctionAttr(LLVMValueRef Fn, String A, String V) {
    //    return Native.INSTANCE.LLVMAddTargetDependentFunctionAttr();
    //}
    ///**
    // * Obtain an attribute from a Native.INSTANCE.
    // *
    // * @see llvm::Function::getAttributes()
    // */
    //LLVMAttribute LLVMGetFunctionAttr(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetFunctionAttr();
    //}
    ///**
    // * Remove an attribute from a Native.INSTANCE.
    // */
    //void LLVMRemoveFunctionAttr(LLVMValueRef Fn, LLVMAttribute PA) {
    //    return Native.INSTANCE.LLVMRemoveFunctionAttr();
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
     * Obtain the number of parameters in a Native.INSTANCE.
     */
    public static int /* unsigned */ LLVMCountParams(LLVMValueRef Fn) {
        return Native.INSTANCE.LLVMCountParams(Fn.value());
    }

    /**
     * Obtain the parameters in a Native.INSTANCE.
     * <p>
     * The takes a pointer to a pre-allocated array of LLVMValueRef that is
     * at least LLVMCountParams() long. This array will be filled with
     * LLVMValueRef instances which correspond to the parameters the
     * function receives. Each LLVMValueRef corresponds to a llvm::Argument
     * instance.
     */
    /* package-private */
    static void LLVMGetParams(LLVMValueRef Fn, long Params) {
        Native.INSTANCE.LLVMGetParams(Fn.value(), Params);
    }

    /**
     * Obtain the parameters in a Native.INSTANCE.
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
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetParam(Fn.value(), Index));
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
    //    return Native.INSTANCE.LLVMGetParamParent();
    //}
    ///**
    // * Obtain the first parameter to a Native.INSTANCE.
    // *
    // * @see llvm::Function::arg_begin()
    // */
    //LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetFirstParam();
    //}
    ///**
    // * Obtain the last parameter to a Native.INSTANCE.
    // *
    // * @see llvm::Function::arg_end()
    // */
    //LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetLastParam();
    //}
    ///**
    // * Obtain the next parameter to a Native.INSTANCE.
    // *
    // * This takes an LLVMValueRef obtained from LLVMGetFirstParam() (which is
    // * actually a wrapped iterator) and obtains the next parameter from the
    // * underlying iterator.
    // */
    //LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg) {
    //    return Native.INSTANCE.LLVMGetNextParam();
    //}
    ///**
    // * Obtain the previous parameter to a Native.INSTANCE.
    // *
    // * This is the opposite of LLVMGetNextParam().
    // */
    //LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg) {
    //    return Native.INSTANCE.LLVMGetPreviousParam();
    //}

    /**
     * Add an attribute to a function argument.
     */
    public static void LLVMAddAttribute(LLVMValueRef Arg, int /* LLVMAttribute */ PA) {
        Native.INSTANCE.LLVMAddAttribute(Arg.value(), PA);
    }

    ///**
    // * Remove an attribute from a function argument.
    // *
    // * @see llvm::Argument::removeAttr()
    // */
    //void LLVMRemoveAttribute(LLVMValueRef Arg, LLVMAttribute PA) {
    //    return Native.INSTANCE.LLVMRemoveAttribute();
    //}

    /**
     * Get an attribute from a function argument.
     */
    public static int /* LLVMAttribute */ LLVMGetAttribute(LLVMValueRef Arg) {
        return Native.INSTANCE.LLVMGetAttribute(Arg.value());
    }

    /**
     * Set the alignment for a function parameter.
     */
    public static void LLVMSetParamAlignment(LLVMValueRef Arg, int /* unsigned */ Align) {
        Native.INSTANCE.LLVMSetParamAlignment(Arg.value(), Align);
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
    //    return Native.INSTANCE.LLVMMDStringInContext();
    //}
    ///**
    // * Obtain a MDString value from the global context.
    // */
    //LLVMValueRef LLVMMDString(String Str, int /* unsigned */ SLen) {
    //    return Native.INSTANCE.LLVMMDString();
    //}
    ///**
    // * Obtain a MDNode value from a context.
    // *
    // * The returned value corresponds to the llvm::MDNode class.
    // */
    //LLVMValueRef LLVMMDNodeInContext(LLVMContextRef C, LLVMValueRef *Vals, int /* unsigned */ Count) {
    //    return Native.INSTANCE.LLVMMDNodeInContext();
    //}
    ///**
    // * Obtain a MDNode value from the global context.
    // */
    //LLVMValueRef LLVMMDNode(LLVMValueRef *Vals, int /* unsigned */ Count) {
    //    return Native.INSTANCE.LLVMMDNode();
    //}
    ///**
    // * Obtain the underlying string from a MDString value.
    // *
    // * @param V Instance to obtain string from.
    // * @param Length Memory address which will hold length of returned string.
    // * @return String data in MDString.
    // */
    //String LLVMGetMDString(LLVMValueRef V, int /* unsigned */ *Length) {
    //    return Native.INSTANCE.LLVMGetMDString();
    //}
    ///**
    // * Obtain the number of operands from an MDNode value.
    // *
    // * @param V MDNode to get number of operands from.
    // * @return Number of operands of the MDNode.
    // */
    //int /* unsigned */ LLVMGetMDNodeNumOperands(LLVMValueRef V) {
    //    return Native.INSTANCE.LLVMGetMDNodeNumOperands();
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
    //    return Native.INSTANCE.LLVMGetMDNodeOperands();
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
    //    return Native.INSTANCE.LLVMBasicBlockAsValue();
    //}
    ///**
    // * Determine whether an LLVMValueRef is itself a basic block.
    // */
    //boolean LLVMValueIsBasicBlock(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMValueIsBasicBlock();
    //}
    ///**
    // * Convert an LLVMValueRef to an LLVMBasicBlockRef instance.
    // */
    //LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val) {
    //    return Native.INSTANCE.LLVMValueAsBasicBlock();
    //}
    ///**
    // * Obtain the string name of a basic block.
    // */
    //String LLVMGetBasicBlockName(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMGetBasicBlockName();
    //}

    /**
     * Obtain the function to which a basic block belongs.
     */
    public static LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetBasicBlockParent(BB.value()));
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
    //    return Native.INSTANCE.LLVMGetBasicBlockTerminator();
    //}
    ///**
    // * Obtain the number of basic blocks in a Native.INSTANCE.
    // *
    // * @param Fn Function value to operate on.
    // */
    //int /* unsigned */ LLVMCountBasicBlocks(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMCountBasicBlocks();
    //}
    ///**
    // * Obtain all of the basic blocks in a Native.INSTANCE.
    // *
    // * This operates on a function value. The BasicBlocks parameter is a
    // * pointer to a pre-allocated array of LLVMBasicBlockRef of at least
    // * LLVMCountBasicBlocks() in length. This array is populated with
    // * LLVMBasicBlockRef instances.
    // */
    //void LLVMGetBasicBlocks(LLVMValueRef Fn, LLVMBasicBlockRef *BasicBlocks) {
    //    return Native.INSTANCE.LLVMGetBasicBlocks();
    //}
    ///**
    // * Obtain the first basic block in a Native.INSTANCE.
    // *
    // * The returned basic block can be used as an iterator. You will likely
    // * eventually call into LLVMGetNextBasicBlock() with it.
    // *
    // * @see llvm::Function::begin()
    // */
    //LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetFirstBasicBlock();
    //}
    ///**
    // * Obtain the last basic block in a Native.INSTANCE.
    // *
    // * @see llvm::Function::end()
    // */
    //LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn) {
    //    return Native.INSTANCE.LLVMGetLastBasicBlock();
    //}
    ///**
    // * Advance a basic block iterator.
    // */
    //LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMGetNextBasicBlock();
    //}
    ///**
    // * Go backwards in a basic block iterator.
    // */
    //LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMGetPreviousBasicBlock();
    //}

    /**
     * Obtain the basic block that corresponds to the entry point of a
     * Native.INSTANCE.
     */
    public static LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn) {
        return LLVMBasicBlockRef.ofNullable(Native.INSTANCE.LLVMGetEntryBasicBlock(Fn.value()));
    }

    /**
     * Append a basic block to the end of a Native.INSTANCE.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C, LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.ofNullable(Native.INSTANCE.LLVMAppendBasicBlockInContext(
                    C.value(), Fn.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Append a basic block to the end of a function using the global
     * context.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.ofNullable(Native.INSTANCE.LLVMAppendBasicBlock(
                    Fn.value(), c_Name.nativeAddress()));
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
    //    return Native.INSTANCE.LLVMInsertBasicBlockInContext();
    //}
    ///**
    // * Insert a basic block in a function using the global context.
    // *
    // * @see llvm::BasicBlock::Create()
    // */
    //LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB, String Name) {
    //    return Native.INSTANCE.LLVMInsertBasicBlock();
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
    //    return Native.INSTANCE.LLVMDeleteBasicBlock();
    //}
    ///**
    // * Remove a basic block from a Native.INSTANCE.
    // *
    // * This deletes the basic block from its containing function but keep
    // * the basic block alive.
    // *
    // * @see llvm::BasicBlock::removeFromParent()
    // */
    //void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMRemoveBasicBlockFromParent();
    //}
    ///**
    // * Move a basic block to before another one.
    // *
    // * @see llvm::BasicBlock::moveBefore()
    // */
    //void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
    //    return Native.INSTANCE.LLVMMoveBasicBlockBefore();
    //}
    ///**
    // * Move a basic block to after another one.
    // *
    // * @see llvm::BasicBlock::moveAfter()
    // */
    //void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
    //    return Native.INSTANCE.LLVMMoveBasicBlockAfter();
    //}
    ///**
    // * Obtain the first instruction in a basic block.
    // *
    // * The returned LLVMValueRef corresponds to a llvm::Instruction
    // * instance.
    // */
    //LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMGetFirstInstruction();
    //}
    ///**
    // * Obtain the last instruction in a basic block.
    // *
    // * The returned LLVMValueRef corresponds to an LLVM:Instruction.
    // */
    //LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB) {
    //    return Native.INSTANCE.LLVMGetLastInstruction();
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
    //    return Native.INSTANCE.LLVMHasMetadata();
    //}
    ///**
    // * Return metadata associated with an instruction value.
    // */
    //LLVMValueRef LLVMGetMetadata(LLVMValueRef Val, int /* unsigned */ KindID) {
    //    return Native.INSTANCE.LLVMGetMetadata();
    //}
    ///**
    // * Set metadata associated with an instruction value.
    // */
    //void LLVMSetMetadata(LLVMValueRef Val, int /* unsigned */ KindID, LLVMValueRef Node) {
    //    return Native.INSTANCE.LLVMSetMetadata();
    //}
    ///**
    // * Obtain the basic block to which an instruction belongs.
    // *
    // * @see llvm::Instruction::getParent()
    // */
    //LLVMBasicBlockRef LLVMGetInstructionParent(LLVMValueRef Inst) {
    //    return Native.INSTANCE.LLVMGetInstructionParent();
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
    //    return Native.INSTANCE.LLVMGetNextInstruction();
    //}
    ///**
    // * Obtain the instruction that occurred before this one.
    // *
    // * If the instruction is the first instruction in a basic block, NULL
    // * will be returned.
    // */
    //LLVMValueRef LLVMGetPreviousInstruction(LLVMValueRef Inst) {
    //    return Native.INSTANCE.LLVMGetPreviousInstruction();
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
    //    return Native.INSTANCE.LLVMInstructionRemoveFromParent();
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
    //    return Native.INSTANCE.LLVMInstructionEraseFromParent();
    //}
    ///**
    // * Obtain the code opcode for an individual instruction.
    // *
    // * @see llvm::Instruction::getOpCode()
    // */
    //LLVMOpcode LLVMGetInstructionOpcode(LLVMValueRef Inst) {
    //    return Native.INSTANCE.LLVMGetInstructionOpcode();
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
    //    return Native.INSTANCE.LLVMGetICmpPredicate();
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
    //    return Native.INSTANCE.LLVMGetFCmpPredicate();
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
    //    return Native.INSTANCE.LLVMInstructionClone();
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
    //    return Native.INSTANCE.LLVMGetNumArgOperands();
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
    //    return Native.INSTANCE.LLVMSetInstructionCallConv();
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
    //    return Native.INSTANCE.LLVMGetInstructionCallConv();
    //}

    public static void LLVMAddInstrAttribute(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* LLVMAttribute */ PA) {
        Native.INSTANCE.LLVMAddInstrAttribute(Instr.value(), index, PA);
    }

    public static void LLVMRemoveInstrAttribute(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* LLVMAttribute */ PA) {
        Native.INSTANCE.LLVMRemoveInstrAttribute(Instr.value(), index, PA);
    }

    public static void LLVMSetInstrParamAlignment(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* unsigned */ Align) {
        Native.INSTANCE.LLVMSetInstrParamAlignment(Instr.value(), index, Align);
    }

    //void LLVMAddCallSiteAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, LLVMAttributeRef A) {
    //    return Native.INSTANCE.LLVMAddCallSiteAttribute();
    //}
    //LLVMAttributeRef LLVMGetCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return Native.INSTANCE.LLVMGetCallSiteEnumAttribute();
    //}
    //LLVMAttributeRef LLVMGetCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return Native.INSTANCE.LLVMGetCallSiteStringAttribute();
    //}
    //void LLVMRemoveCallSiteEnumAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, int /* unsigned */ KindID) {
    //    return Native.INSTANCE.LLVMRemoveCallSiteEnumAttribute();
    //}
    //void LLVMRemoveCallSiteStringAttribute(LLVMValueRef C, LLVMAttributeIndex Idx, String K, int /* unsigned */ KLen) {
    //    return Native.INSTANCE.LLVMRemoveCallSiteStringAttribute();
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
    //    return Native.INSTANCE.LLVMGetCalledValue();
    //}
    ///**
    // * Obtain whether a call instruction is a tail call.
    // *
    // * This only works on llvm::CallInst instructions.
    // *
    // * @see llvm::CallInst::isTailCall()
    // */
    //boolean LLVMIsTailCall(LLVMValueRef CallInst) {
    //    return Native.INSTANCE.LLVMIsTailCall();
    //}
    ///**
    // * Set whether a call instruction is a tail call.
    // *
    // * This only works on llvm::CallInst instructions.
    // *
    // * @see llvm::CallInst::setTailCall()
    // */
    //void LLVMSetTailCall(LLVMValueRef CallInst, boolean IsTailCall) {
    //    return Native.INSTANCE.LLVMSetTailCall();
    //}
    ///**
    // * Return the normal destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::getNormalDest()
    // */
    //LLVMBasicBlockRef LLVMGetNormalDest(LLVMValueRef InvokeInst) {
    //    return Native.INSTANCE.LLVMGetNormalDest();
    //}
    ///**
    // * Return the unwind destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::getUnwindDest()
    // */
    //LLVMBasicBlockRef LLVMGetUnwindDest(LLVMValueRef InvokeInst) {
    //    return Native.INSTANCE.LLVMGetUnwindDest();
    //}
    ///**
    // * Set the normal destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::setNormalDest()
    // */
    //void LLVMSetNormalDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
    //    return Native.INSTANCE.LLVMSetNormalDest();
    //}
    ///**
    // * Set the unwind destination basic block.
    // *
    // * This only works on llvm::InvokeInst instructions.
    // *
    // * @see llvm::InvokeInst::setUnwindDest()
    // */
    //void LLVMSetUnwindDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
    //    return Native.INSTANCE.LLVMSetUnwindDest();
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
    //    return Native.INSTANCE.LLVMGetNumSuccessors();
    //}
    ///**
    // * Return the specified successor.
    // *
    // * @see llvm::TerminatorInst::getSuccessor
    // */
    //LLVMBasicBlockRef LLVMGetSuccessor(LLVMValueRef Term, int /* unsigned */ i) {
    //    return Native.INSTANCE.LLVMGetSuccessor();
    //}
    ///**
    // * Update the specified successor to point at the provided block.
    // *
    // * @see llvm::TerminatorInst::setSuccessor
    // */
    //void LLVMSetSuccessor(LLVMValueRef Term, int /* unsigned */ i, LLVMBasicBlockRef block) {
    //    return Native.INSTANCE.LLVMSetSuccessor();
    //}
    ///**
    // * Return if a branch is conditional.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::isConditional
    // */
    //boolean LLVMIsConditional(LLVMValueRef Branch) {
    //    return Native.INSTANCE.LLVMIsConditional();
    //}
    ///**
    // * Return the condition of a branch instruction.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::getCondition
    // */
    //LLVMValueRef LLVMGetCondition(LLVMValueRef Branch) {
    //    return Native.INSTANCE.LLVMGetCondition();
    //}
    ///**
    // * Set the condition of a branch instruction.
    // *
    // * This only works on llvm::BranchInst instructions.
    // *
    // * @see llvm::BranchInst::setCondition
    // */
    //void LLVMSetCondition(LLVMValueRef Branch, LLVMValueRef Cond) {
    //    return Native.INSTANCE.LLVMSetCondition();
    //}
    ///**
    // * Obtain the default destination basic block of a switch instruction.
    // *
    // * This only works on llvm::SwitchInst instructions.
    // *
    // * @see llvm::SwitchInst::getDefaultDest()
    // */
    //LLVMBasicBlockRef LLVMGetSwitchDefaultDest(LLVMValueRef SwitchInstr) {
    //    return Native.INSTANCE.LLVMGetSwitchDefaultDest();
    //}

    /*
     * @defgroup LLVMCCoreValueInstructionAlloca Allocas
     *
     * Functions in this group only apply to instructions that map to
     * llvm::AllocaInst instances.
     */

    /**
     * Obtain the type that is being allocated by the alloca instruction.
     */
    public static LLVMTypeRef LLVMGetAllocatedType(LLVMValueRef Alloca) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMGetAllocatedType(Alloca.value()));
    }

    /*
     * @defgroup LLVMCCoreValueInstructionGetElementPointer GEPs
     *
     * Functions in this group only apply to instructions that map to
     * llvm::GetElementPtrInst instances.
     */

    /**
     * Check whether the given GEP instruction is inbounds.
     */
    public static boolean LLVMIsInBounds(LLVMValueRef GEP) {
        return Native.INSTANCE.LLVMIsInBounds(GEP.value());
    }

    /**
     * Set the given GEP instruction to be inbounds or not.
     */
    public static void LLVMSetIsInBounds(LLVMValueRef GEP, boolean InBounds) {
        Native.INSTANCE.LLVMSetIsInBounds(GEP.value(), InBounds);
    }

    /*
     * @defgroup LLVMCCoreValueInstructionPHINode PHI Nodes
     *
     * Functions in this group only apply to instructions that map to
     * llvm::PHINode instances.
     */

    /**
     * Add an incoming value to the end of a PHI list.
     */
    public static void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef[] IncomingValues, LLVMBasicBlockRef[] IncomingBlocks) {
        // TODO: maybe cleanup code?
        if (IncomingValues == null) {
            IncomingValues = new LLVMValueRef[0];
        }
        if (IncomingBlocks == null) {
            IncomingBlocks = new LLVMBasicBlockRef[0];
        }
        int /* unsigned */ Count;
        if ((Count = IncomingValues.length) != IncomingBlocks.length) {
            String msg = "IncomingValues.length(%s) != IncomingBlocks.length(%s)";
            throw new IllegalArgumentException(String.format(msg,
                    IncomingValues.length, IncomingBlocks.length));
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_IncomingValues = allocArray(arena, IncomingValues);
            MemorySegment c_IncomingBlocks = allocArray(arena, IncomingBlocks);
            Native.INSTANCE.LLVMAddIncoming(PhiNode.value(),
                    c_IncomingValues.nativeAddress(), c_IncomingBlocks.nativeAddress(), Count);
        }
    }

    /**
     * Add an incoming value to the end of a PHI list.
     */
    // Port-added
    public static void LLVMAddIncoming(LLVMValueRef PhiNode, LLVMValueRef IncomingValue, LLVMBasicBlockRef IncomingBlock) {
        LLVMAddIncoming(PhiNode, new LLVMValueRef[]{IncomingValue}, new LLVMBasicBlockRef[]{IncomingBlock});
    }

    /**
     * Obtain the number of incoming basic blocks to a PHI node.
     */
    public static int /* unsigned */ LLVMCountIncoming(LLVMValueRef PhiNode) {
        return Native.INSTANCE.LLVMCountIncoming(PhiNode.value());
    }

    /**
     * Obtain an incoming value to a PHI node as an LLVMValueRef.
     */
    public static LLVMValueRef LLVMGetIncomingValue(LLVMValueRef PhiNode, int /* unsigned */ Index) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetIncomingValue(PhiNode.value(), Index));
    }

    /**
     * Obtain an incoming value to a PHI node as an LLVMBasicBlockRef.
     */
    public static LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, int /* unsigned */ Index) {
        return LLVMBasicBlockRef.ofNullable(Native.INSTANCE.LLVMGetIncomingBlock(PhiNode.value(), Index));
    }

    /*
     * @defgroup LLVMCCoreValueInstructionExtractValue ExtractValue
     * @defgroup LLVMCCoreValueInstructionInsertValue InsertValue
     *
     * Functions in this group only apply to instructions that map to
     * llvm::ExtractValue and llvm::InsertValue instances.
     */

    ///**
    // * Obtain the number of indices.
    // * NB: This also works on GEP.
    // */
    //int /* unsigned */ LLVMGetNumIndices(LLVMValueRef Inst) {
    //    return Native.INSTANCE.LLVMGetNumIndices();
    //}
    ///**
    // * Obtain the indices as an array.
    // */
    //const int /* unsigned */ *LLVMGetIndices(LLVMValueRef Inst) {
    //    return Native.INSTANCE.*LLVMGetIndices();
    //}

    /*
     * @defgroup LLVMCCoreInstructionBuilder Instruction Builders
     *
     * An instruction builder represents a point within a basic block and is
     * the exclusive means of building instructions using the C interface.
     */

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        return LLVMBuilderRef.ofNullable(Native.INSTANCE.LLVMCreateBuilderInContext(C.value()));
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        return LLVMBuilderRef.ofNullable(Native.INSTANCE.LLVMCreateBuilder());
    }

    public static void LLVMPositionBuilder(LLVMBuilderRef Builder, LLVMBasicBlockRef Block, LLVMValueRef Instr) {
        Native.INSTANCE.LLVMPositionBuilder(Builder.value(), Block.value(), Instr.value());
    }

    public static void LLVMPositionBuilderBefore(LLVMBuilderRef Builder, LLVMValueRef Instr) {
        Native.INSTANCE.LLVMPositionBuilderBefore(Builder.value(), Instr.value());
    }

    public static void LLVMPositionBuilderAtEnd(LLVMBuilderRef Builder, LLVMBasicBlockRef Block) {
        Native.INSTANCE.LLVMPositionBuilderAtEnd(Builder.value(), Block.value());
    }

    public static LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef Builder) {
        return LLVMBasicBlockRef.ofNullable(Native.INSTANCE.LLVMGetInsertBlock(Builder.value()));
    }

    public static void LLVMClearInsertionPosition(LLVMBuilderRef Builder) {
        Native.INSTANCE.LLVMClearInsertionPosition(Builder.value());
    }

    public static void LLVMInsertIntoBuilder(LLVMBuilderRef Builder, LLVMValueRef Instr) {
        Native.INSTANCE.LLVMInsertIntoBuilder(Builder.value(), Instr.value());
    }

    public static void LLVMInsertIntoBuilderWithName(LLVMBuilderRef Builder, LLVMValueRef Instr, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            Native.INSTANCE.LLVMInsertIntoBuilderWithName(Builder.value(), Instr.value(), c_Name.nativeAddress());
        }
    }

    public static void LLVMDisposeBuilder(LLVMBuilderRef Builder) {
        Native.INSTANCE.LLVMDisposeBuilder(Builder.value());
    }

    /* Metadata */

    public static void LLVMSetCurrentDebugLocation(LLVMBuilderRef Builder, LLVMValueRef L) {
        Native.INSTANCE.LLVMSetCurrentDebugLocation(Builder.value(), L.value());
    }

    public static LLVMValueRef LLVMGetCurrentDebugLocation(LLVMBuilderRef Builder) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMGetCurrentDebugLocation(Builder.value()));
    }

    public static void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst) {
        Native.INSTANCE.LLVMSetInstDebugLocation(Builder.value(), Inst.value());
    }

    /* Terminators */

    public static LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef Builder) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildRetVoid(Builder.value()));
    }

    public static LLVMValueRef LLVMBuildRet(LLVMBuilderRef Builder, LLVMValueRef V) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildRet(Builder.value(), V.value()));
    }

    //LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef B, LLVMValueRef *RetVals, int /* unsigned */ N) {
    //    return Native.INSTANCE.LLVMBuildAggregateRet();
    //}

    public static LLVMValueRef LLVMBuildBr(LLVMBuilderRef B, LLVMBasicBlockRef Dest) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildBr(B.value(), Dest.value()));
    }

    public static LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef B, LLVMValueRef If, LLVMBasicBlockRef Then, LLVMBasicBlockRef Else) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildCondBr(
                B.value(), If.value(), Then.value(), Else.value()));
    }

    //LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef B, LLVMValueRef V, LLVMBasicBlockRef Else, int /* unsigned */ NumCases) {
    //    return Native.INSTANCE.LLVMBuildSwitch();
    //}
    //LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr, int /* unsigned */ NumDests) {
    //    return Native.INSTANCE.LLVMBuildIndirectBr();
    //}
    //LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef *Args, int /* unsigned */ NumArgs, LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch, String Name) {
    //    return Native.INSTANCE.LLVMBuildInvoke();
    //}
    //LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef PersFn, int /* unsigned */ NumClauses, String Name) {
    //    return Native.INSTANCE.LLVMBuildLandingPad();
    //}
    //LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn) {
    //    return Native.INSTANCE.LLVMBuildResume();
    //}
    //LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef) {
    //    return Native.INSTANCE.LLVMBuildUnreachable();
    //}
    ///* Add a case to the switch instruction */
    //void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal, LLVMBasicBlockRef Dest) {
    //    return Native.INSTANCE.LLVMAddCase();
    //}
    ///* Add a destination to the indirectbr instruction */
    //void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest) {
    //    return Native.INSTANCE.LLVMAddDestination();
    //}
    ///* Get the number of clauses on the landingpad instruction */
    //int /* unsigned */ LLVMGetNumClauses(LLVMValueRef LandingPad) {
    //    return Native.INSTANCE.LLVMGetNumClauses();
    //}
    ///* Get the value of the clause at idnex Idx on the landingpad instruction */
    //LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, int /* unsigned */ Idx) {
    //    return Native.INSTANCE.LLVMGetClause();
    //}
    ///* Add a catch or filter clause to the landingpad instruction */
    //void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal) {
    //    return Native.INSTANCE.LLVMAddClause();
    //}
    ///* Get the 'cleanup' flag in the landingpad instruction */
    //boolean LLVMIsCleanup(LLVMValueRef LandingPad) {
    //    return Native.INSTANCE.LLVMIsCleanup();
    //}
    ///* Set the 'cleanup' flag in the landingpad instruction */
    //void LLVMSetCleanup(LLVMValueRef LandingPad, boolean Val) {
    //    return Native.INSTANCE.LLVMSetCleanup();
    //}

    /* Arithmetic */

    public static LLVMValueRef LLVMBuildAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNSWAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNUWAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNSWSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNUWSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNSWMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNUWMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildUDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildExactSDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildURem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildURem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSRem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFRem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildShl(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildShl(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildLShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildLShr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildAShr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAnd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildAnd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildOr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildOr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildXor(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildXor(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildBinOp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNSWNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNUWNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNot(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildNot(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    /* Memory */

    public static LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildMalloc(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildArrayMalloc(
                    B.value(), Ty.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildAlloca(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildArrayAlloca(
                    B.value(), Ty.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFree(LLVMBuilderRef B, LLVMValueRef Ptr) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFree(B.value(), Ptr.value()));
    }

    public static LLVMValueRef LLVMBuildLoad(LLVMBuilderRef B, LLVMValueRef Ptr, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildLoad(
                    B.value(), Ptr.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildStore(LLVMBuilderRef B, LLVMValueRef Val, LLVMValueRef Ptr) {
        return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildStore(B.value(), Val.value(), Ptr.value()));
    }

    public static LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Indices = allocArray(arena, Indices);
            int /* unsigned */ NumIndices = arrayLength(Indices);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildGEP(B.value(),
                    Pointer.value(), c_Indices.nativeAddress(), NumIndices, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Indices = allocArray(arena, Indices);
            int /* unsigned */ NumIndices = arrayLength(Indices);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildInBoundsGEP(B.value(),
                    Pointer.value(), c_Indices.nativeAddress(), NumIndices, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer, int /* unsigned */ Idx, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildStructGEP(
                    B.value(), Pointer.value(), Idx, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildGlobalString(
                    B.value(), c_Str.nativeAddress(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildGlobalStringPtr(
                    B.value(), c_Str.nativeAddress(), c_Name.nativeAddress()));
        }
    }

    public static boolean LLVMGetVolatile(LLVMValueRef MemoryAccessInst) {
        return Native.INSTANCE.LLVMGetVolatile(MemoryAccessInst.value());
    }

    public static void LLVMSetVolatile(LLVMValueRef MemoryAccessInst, boolean IsVolatile) {
        Native.INSTANCE.LLVMSetVolatile(MemoryAccessInst.value(), IsVolatile);
    }

    public static LLVMAtomicOrdering LLVMGetOrdering(LLVMValueRef MemoryAccessInst) {
        return LLVMAtomicOrdering.of(Native.INSTANCE.LLVMGetOrdering(MemoryAccessInst.value()));
    }

    public static void LLVMSetOrdering(LLVMValueRef MemoryAccessInst, LLVMAtomicOrdering Ordering) {
        Native.INSTANCE.LLVMSetOrdering(MemoryAccessInst.value(), Ordering.value());
    }

    /* Casts */

    public static LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildTrunc(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildZExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildZExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFPToUI(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFPToSI(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildUIToFP(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSIToFP(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFPTrunc(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFPExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildPtrToInt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildIntToPtr(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildAddrSpaceCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildZExtOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSExtOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildTruncOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildCast(
                    B.value(), Op.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildPointerCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildIntCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFPCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    /* Comparisons */

    public static LLVMValueRef LLVMBuildICmp(LLVMBuilderRef B, LLVMIntPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildICmp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef B, LLVMRealPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildFCmp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    /* Miscellaneous instructions */

    public static LLVMValueRef LLVMBuildPhi(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildPhi(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildCall(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildCall(B.value(),
                    Fn.value(), c_Args.nativeAddress(), NumArgs, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSelect(LLVMBuilderRef B, LLVMValueRef If, LLVMValueRef Then, LLVMValueRef Else, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildSelect(B.value(),
                    If.value(), Then.value(), Else.value(), c_Name.nativeAddress()));
        }
    }

    //LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef B, LLVMValueRef List, LLVMTypeRef Ty, String Name) {
    //    return Native.INSTANCE.LLVMBuildVAArg();
    //}
    //LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef Index, String Name) {
    //    return Native.INSTANCE.LLVMBuildExtractElement();
    //}
    //LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef EltVal, LLVMValueRef Index, String Name) {
    //    return Native.INSTANCE.LLVMBuildInsertElement();
    //}
    //LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef B, LLVMValueRef V1, LLVMValueRef V2, LLVMValueRef Mask, String Name) {
    //    return Native.INSTANCE.LLVMBuildShuffleVector();
    //}

    public static LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef B, LLVMValueRef AggVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildExtractValue(
                    B.value(), AggVal.value(), Index, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef B, LLVMValueRef AggVal, LLVMValueRef EltVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.ofNullable(Native.INSTANCE.LLVMBuildInsertValue(
                    B.value(), AggVal.value(), EltVal.value(), Index, c_Name.nativeAddress()));
        }
    }

    //LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
    //    return Native.INSTANCE.LLVMBuildIsNull();
    //}
    //LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
    //    return Native.INSTANCE.LLVMBuildIsNotNull();
    //}
    //LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
    //    return Native.INSTANCE.LLVMBuildPtrDiff();
    //}
    //LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering ordering, boolean singleThread, String Name) {
    //    return Native.INSTANCE.LLVMBuildFence();
    //}
    //LLVMValueRef LLVMBuildAtomicRMW(LLVMBuilderRef B, LLVMAtomicRMWBinOp op, LLVMValueRef PTR, LLVMValueRef Val, LLVMAtomicOrdering ordering, boolean singleThread) {
    //    return Native.INSTANCE.LLVMBuildAtomicRMW();
    //}
    //LLVMValueRef LLVMBuildAtomicCmpXchg(LLVMBuilderRef B, LLVMValueRef Ptr, LLVMValueRef Cmp, LLVMValueRef New, LLVMAtomicOrdering SuccessOrdering, LLVMAtomicOrdering FailureOrdering, boolean SingleThread) {
    //    return Native.INSTANCE.LLVMBuildAtomicCmpXchg();
    //}
    //boolean LLVMIsAtomicSingleThread(LLVMValueRef AtomicInst) {
    //    return Native.INSTANCE.LLVMIsAtomicSingleThread();
    //}
    //void LLVMSetAtomicSingleThread(LLVMValueRef AtomicInst, boolean SingleThread) {
    //    return Native.INSTANCE.LLVMSetAtomicSingleThread();
    //}
    //LLVMAtomicOrdering LLVMGetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst) {
    //    return Native.INSTANCE.LLVMGetCmpXchgSuccessOrdering();
    //}
    //void LLVMSetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
    //    return Native.INSTANCE.LLVMSetCmpXchgSuccessOrdering();
    //}
    //LLVMAtomicOrdering LLVMGetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst) {
    //    return Native.INSTANCE.LLVMGetCmpXchgFailureOrdering();
    //}
    //void LLVMSetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
    //    return Native.INSTANCE.LLVMSetCmpXchgFailureOrdering();
    //}

    /*
     * @defgroup LLVMCCoreModuleProvider Module Providers
     */

    /**
     * Changes the type of M so it can be passed to FunctionPassManagers and the
     * JIT. They take ModuleProviders for historical reasons.
     */
    public static LLVMModuleProviderRef LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M) {
        return LLVMModuleProviderRef.ofNullable(Native.INSTANCE.LLVMCreateModuleProviderForExistingModule(M.value()));
    }

    /**
     * Destroys the module M.
     */
    public static void LLVMDisposeModuleProvider(LLVMModuleProviderRef M) {
        Native.INSTANCE.LLVMDisposeModuleProvider(M.value());
    }

    /*
     * @defgroup LLVMCCoreMemoryBuffers Memory Buffers
     */

    //boolean LLVMCreateMemoryBufferWithContentsOfFile(String Path, LLVMMemoryBufferRef *OutMemBuf, LLVMString *OutMessage) {
    //    return Native.INSTANCE.LLVMCreateMemoryBufferWithContentsOfFile();
    //}
    //boolean LLVMCreateMemoryBufferWithSTDIN(LLVMMemoryBufferRef *OutMemBuf, LLVMString *OutMessage) {
    //    return Native.INSTANCE.LLVMCreateMemoryBufferWithSTDIN();
    //}

    /* package-private */
    static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithMemoryRange(
            long InputData, long /* size_t */ InputDataLength,
            String BufferName, boolean RequiresNullTerminator) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BufferName = allocString(arena, BufferName);
            return LLVMMemoryBufferRef.ofNullable(Native.INSTANCE.LLVMCreateMemoryBufferWithMemoryRange(
                    InputData, InputDataLength, c_BufferName.nativeAddress(), RequiresNullTerminator));
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
            return LLVMMemoryBufferRef.ofNullable(Native.INSTANCE.LLVMCreateMemoryBufferWithMemoryRangeCopy(
                    InputData, InputDataLength, c_BufferName.nativeAddress()));
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
        return Native.INSTANCE.LLVMGetBufferStart(MemBuf.value());
    }

    public static long /* size_t */ LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf) {
        return Native.INSTANCE.LLVMGetBufferSize(MemBuf.value());
    }

    // Port-added
    public static MemorySegment LLVMGetBufferSegment(LLVMMemoryBufferRef MemBuf) {
        long address = LLVMGetBufferStart(MemBuf);
        long size = LLVMGetBufferSize(MemBuf);
        return MemorySegment.ofAddress(address).reinterpret(size).asReadOnly();
    }

    public static void LLVMDisposeMemoryBuffer(LLVMMemoryBufferRef MemBuf) {
        Native.INSTANCE.LLVMDisposeMemoryBuffer(MemBuf.value());
    }

    /*
     * @defgroup LLVMCCorePassRegistry Pass Registry
     */

    /**
     * Return the global pass registry, for use with initialization functions.
     */
    public static LLVMPassRegistryRef LLVMGetGlobalPassRegistry() {
        return LLVMPassRegistryRef.ofNullable(Native.INSTANCE.LLVMGetGlobalPassRegistry());
    }

    /*
     * @defgroup LLVMCCorePassManagers Pass Managers
     */

    /**
     * Constructs a new whole-module pass pipeline. This type of pipeline is
     * suitable for link-time optimization and whole-module transformations.
     */
    public static LLVMPassManagerRef LLVMCreatePassManager() {
        return LLVMPassManagerRef.ofNullable(Native.INSTANCE.LLVMCreatePassManager());
    }

    /**
     * Constructs a new function-by-function pass pipeline over the module
     * provider. It does not take ownership of the module provider. This type of
     * pipeline is suitable for code generation and JIT compilation tasks.
     */
    public static LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M) {
        return LLVMPassManagerRef.ofNullable(Native.INSTANCE.LLVMCreateFunctionPassManagerForModule(M.value()));
    }

    /**
     * Deprecated: Use LLVMCreateFunctionPassManagerForModule instead.
     */
    @Deprecated
    public static LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP) {
        return LLVMPassManagerRef.ofNullable(Native.INSTANCE.LLVMCreateFunctionPassManager(MP.value()));
    }

    /**
     * Initializes, executes on the provided module, and finalizes all of the
     * passes scheduled in the pass manager. Returns 1 if any of the passes
     * modified the module, 0 otherwise.
     */
    public static boolean LLVMRunPassManager(LLVMPassManagerRef PM, LLVMModuleRef M) {
        return Native.INSTANCE.LLVMRunPassManager(PM.value(), M.value());
    }

    /**
     * Initializes all of the function passes scheduled in the function pass
     * manager. Returns 1 if any of the passes modified the module, 0 otherwise.
     */
    public static boolean LLVMInitializeFunctionPassManager(LLVMPassManagerRef FPM) {
        return Native.INSTANCE.LLVMInitializeFunctionPassManager(FPM.value());
    }

    /**
     * Executes all of the function passes scheduled in the function pass manager
     * on the provided Native.INSTANCE. Returns 1 if any of the passes modified the
     * function, false otherwise.
     */
    public static boolean LLVMRunFunctionPassManager(LLVMPassManagerRef FPM, LLVMValueRef F) {
        return Native.INSTANCE.LLVMRunFunctionPassManager(FPM.value(), F.value());
    }

    /**
     * Finalizes all of the function passes scheduled in in the function pass
     * manager. Returns 1 if any of the passes modified the module, 0 otherwise.
     */
    public static boolean LLVMFinalizeFunctionPassManager(LLVMPassManagerRef FPM) {
        return Native.INSTANCE.LLVMFinalizeFunctionPassManager(FPM.value());
    }

    /**
     * Frees the memory of a pass pipeline. For function pipelines, does not free
     * the module provider.
     */
    public static void LLVMDisposePassManager(LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMDisposePassManager(PM.value());
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
    //    return Native.INSTANCE.LLVMStartMultithreaded();
    //}
    ///** Deprecated: Multi-threading can only be enabled/disabled with the compile
    //    time define LLVM_ENABLE_THREADS. */
    //void LLVMStopMultithreaded() {
    //    return Native.INSTANCE.LLVMStopMultithreaded();
    //}
    ///** Check whether LLVM is executing in thread-safe mode or not.
    //    @see llvm::llvm_is_multithreaded */
    //boolean LLVMIsMultithreaded() {
    //    return Native.INSTANCE.LLVMIsMultithreaded();
    //}
}

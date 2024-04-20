package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.llvm.Types.LLVMAttributeRef;
import static com.v7878.llvm.Types.LLVMBasicBlockRef;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.allocPointerArray;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.llvm._Utils.arrayLength;
import static com.v7878.llvm._Utils.readPointerArray;
import static com.v7878.llvm._Utils.stringLength;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMDiagnosticInfoRef;
import com.v7878.llvm.Types.LLVMModuleProviderRef;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.llvm.Types.LLVMUseRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.unsafe.invoke.Transformers.TransformerI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/*===-- llvm-c/Core.h - Core Library C Interface ------------------*- C -*-===*\
|*                                                                            *|
|* This header declares the C interface to libLLVMCore.a, which implements    *|
|* the LLVM intermediate representation.                                      *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Core {
    private Core() {
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

        int value() {
            return value;
        }

        static LLVMOpcode of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMTypeKind of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMLinkage of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMVisibility of(int value) {
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

        int value() {
            return value;
        }

        static LLVMDLLStorageClass of(int value) {
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

        int value() {
            return value;
        }

        static LLVMCallConv of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMValueKind of(int value) {
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

        int value() {
            return ordinal() + 32;
        }

        static LLVMIntPredicate of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMRealPredicate of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMLandingPadClauseTy of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMThreadLocalMode of(int value) {
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

        int value() {
            return value;
        }

        static LLVMAtomicOrdering of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMAtomicRMWBinOp of(int value) {
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

        int value() {
            return ordinal();
        }

        static LLVMDiagnosticSeverity of(int value) {
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
    public static final class LLVMAttributeIndex {
        private LLVMAttributeIndex() {
        }

        public static final int LLVMAttributeReturnIndex = 0;
        // ISO C restricts enumerator values to range of 'int'
        // (4294967295 is too large)
        // LLVMAttributeFunctionIndex = ~0U,
        public static final int LLVMAttributeFunctionIndex = -1;
        // Port-added
        public static final int LLVMAttributeFirstArgIndex = 1;
    }

    @FunctionalInterface
    public interface LLVMDiagnosticHandler {
        void invoke(LLVMDiagnosticInfoRef info);
    }

    private static final FunctionDescriptor DIAGNOSTIC_HANDLER_DESCRIPTOR =
            FunctionDescriptor.ofVoid(WORD, WORD);

    private static MethodHandle invoker(LLVMDiagnosticHandler handler) {
        return Transformers.makeTransformer(MethodType.methodType(
                void.class, WORD.carrier(), WORD.carrier()), (TransformerI) stack -> {
            var accessor = stack.createAccessor();
            var value = IS64BIT ? accessor.nextLong() : accessor.nextInt() & 0xffffffffL;
            handler.invoke(LLVMDiagnosticInfoRef.of(value));
        });
    }

    @FunctionalInterface
    public interface LLVMYieldCallback {
        void invoke(LLVMContextRef context);
    }

    private static final FunctionDescriptor YIELD_CALLBACK_DESCRIPTOR =
            FunctionDescriptor.ofVoid(WORD, WORD);

    private static MethodHandle invoker(LLVMYieldCallback callback) {
        return Transformers.makeTransformer(MethodType.methodType(
                void.class, WORD.carrier(), WORD.carrier()), (TransformerI) stack -> {
            var accessor = stack.createAccessor();
            var value = IS64BIT ? accessor.nextLong() : accessor.nextInt() & 0xffffffffL;
            callback.invoke(LLVMContextRef.of(value));
        });
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

        @LibrarySymbol(name = "LLVMContextSetDiagnosticHandler")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMContextSetDiagnosticHandler(long C, long Handler, long DiagnosticContext);

        @LibrarySymbol(name = "LLVMContextGetDiagnosticHandler")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMContextGetDiagnosticHandler(long C);

        @LibrarySymbol(name = "LLVMContextGetDiagnosticContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMContextGetDiagnosticContext(long C);

        @LibrarySymbol(name = "LLVMContextSetYieldCallback")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMContextSetYieldCallback(long C, long Callback, long OpaqueHandle);

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

        @LibrarySymbol(name = "LLVMGetModuleIdentifier")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetModuleIdentifier(long M, long Len);

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

        @LibrarySymbol(name = "LLVMPrintModuleToFile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMPrintModuleToFile(long M, long Filename, long ErrorMessage);

        @LibrarySymbol(name = "LLVMPrintModuleToString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPrintModuleToString(long M);

        @LibrarySymbol(name = "LLVMSetModuleInlineAsm")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetModuleInlineAsm(long M, long Asm);

        @LibrarySymbol(name = "LLVMGetModuleContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetModuleContext(long M);

        @LibrarySymbol(name = "LLVMGetTypeByName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetTypeByName(long M, long Name);

        @LibrarySymbol(name = "LLVMGetNamedMetadataNumOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMGetNamedMetadataNumOperands(long M, long Name);

        @LibrarySymbol(name = "LLVMGetNamedMetadataOperands")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetNamedMetadataOperands(long M, long Name, long Dest);

        @LibrarySymbol(name = "LLVMAddNamedMetadataOperand")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddNamedMetadataOperand(long M, long Name, long Val);

        @LibrarySymbol(name = "LLVMAddFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddFunction(long M, long Name, long FunctionTy);

        @LibrarySymbol(name = "LLVMGetNamedFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetNamedFunction(long M, long Name);

        @LibrarySymbol(name = "LLVMGetFirstFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstFunction(long M);

        @LibrarySymbol(name = "LLVMGetLastFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastFunction(long M);

        @LibrarySymbol(name = "LLVMGetNextFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextFunction(long Fn);

        @LibrarySymbol(name = "LLVMGetPreviousFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousFunction(long Fn);

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

        @LibrarySymbol(name = "LLVMCountParamTypes")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountParamTypes(long FunctionTy);

        @LibrarySymbol(name = "LLVMGetParamTypes")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetParamTypes(long FunctionTy, long Dest);

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

        @LibrarySymbol(name = "LLVMCountStructElementTypes")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountStructElementTypes(long StructTy);

        @LibrarySymbol(name = "LLVMGetStructElementTypes")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetStructElementTypes(long StructTy, long Dest);

        @LibrarySymbol(name = "LLVMStructGetTypeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMStructGetTypeAtIndex(long StructTy, int Idx);

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

        @LibrarySymbol(name = "LLVMTypeOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMTypeOf(long Val);

        @LibrarySymbol(name = "LLVMGetValueKind")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetValueKind(long Val);

        @LibrarySymbol(name = "LLVMGetValueName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetValueName(long Val);

        @LibrarySymbol(name = "LLVMSetValueName")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetValueName(long Val, long Name);

        @LibrarySymbol(name = "LLVMDumpValue")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDumpValue(long Val);

        @LibrarySymbol(name = "LLVMPrintValueToString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMPrintValueToString(long Val);

        @LibrarySymbol(name = "LLVMReplaceAllUsesWith")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMReplaceAllUsesWith(long OldVal, long NewVal);

        @LibrarySymbol(name = "LLVMIsConstant")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsConstant(long Val);

        @LibrarySymbol(name = "LLVMIsUndef")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsUndef(long Val);

        @LibrarySymbol(name = "LLVMIsAArgument")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAArgument(long Val);

        @LibrarySymbol(name = "LLVMIsABasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABasicBlock(long Val);

        @LibrarySymbol(name = "LLVMIsAInlineAsm")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInlineAsm(long Val);

        @LibrarySymbol(name = "LLVMIsAUser")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUser(long Val);

        @LibrarySymbol(name = "LLVMIsAConstant")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstant(long Val);

        @LibrarySymbol(name = "LLVMIsABlockAddress")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABlockAddress(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantAggregateZero")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantAggregateZero(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantArray")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantArray(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantDataSequential")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataSequential(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantDataArray")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataArray(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantDataVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantDataVector(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantExpr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantExpr(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantFP(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantInt(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantPointerNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantPointerNull(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantStruct")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantStruct(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantTokenNone")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantTokenNone(long Val);

        @LibrarySymbol(name = "LLVMIsAConstantVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAConstantVector(long Val);

        @LibrarySymbol(name = "LLVMIsAGlobalValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalValue(long Val);

        @LibrarySymbol(name = "LLVMIsAGlobalAlias")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalAlias(long Val);

        @LibrarySymbol(name = "LLVMIsAGlobalObject")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalObject(long Val);

        @LibrarySymbol(name = "LLVMIsAFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFunction(long Val);

        @LibrarySymbol(name = "LLVMIsAGlobalVariable")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGlobalVariable(long Val);

        @LibrarySymbol(name = "LLVMIsAUndefValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUndefValue(long Val);

        @LibrarySymbol(name = "LLVMIsAInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInstruction(long Val);

        @LibrarySymbol(name = "LLVMIsABinaryOperator")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABinaryOperator(long Val);

        @LibrarySymbol(name = "LLVMIsACallInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACallInst(long Val);

        @LibrarySymbol(name = "LLVMIsAIntrinsicInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIntrinsicInst(long Val);

        @LibrarySymbol(name = "LLVMIsADbgInfoIntrinsic")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsADbgInfoIntrinsic(long Val);

        @LibrarySymbol(name = "LLVMIsADbgDeclareInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsADbgDeclareInst(long Val);

        @LibrarySymbol(name = "LLVMIsAMemIntrinsic")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemIntrinsic(long Val);

        @LibrarySymbol(name = "LLVMIsAMemCpyInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemCpyInst(long Val);

        @LibrarySymbol(name = "LLVMIsAMemMoveInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemMoveInst(long Val);

        @LibrarySymbol(name = "LLVMIsAMemSetInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMemSetInst(long Val);

        @LibrarySymbol(name = "LLVMIsACmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACmpInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFCmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFCmpInst(long Val);

        @LibrarySymbol(name = "LLVMIsAICmpInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAICmpInst(long Val);

        @LibrarySymbol(name = "LLVMIsAExtractElementInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAExtractElementInst(long Val);

        @LibrarySymbol(name = "LLVMIsAGetElementPtrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAGetElementPtrInst(long Val);

        @LibrarySymbol(name = "LLVMIsAInsertElementInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInsertElementInst(long Val);

        @LibrarySymbol(name = "LLVMIsAInsertValueInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInsertValueInst(long Val);

        @LibrarySymbol(name = "LLVMIsALandingPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsALandingPadInst(long Val);

        @LibrarySymbol(name = "LLVMIsAPHINode")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAPHINode(long Val);

        @LibrarySymbol(name = "LLVMIsASelectInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASelectInst(long Val);

        @LibrarySymbol(name = "LLVMIsAShuffleVectorInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAShuffleVectorInst(long Val);

        @LibrarySymbol(name = "LLVMIsAStoreInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAStoreInst(long Val);

        @LibrarySymbol(name = "LLVMIsATerminatorInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsATerminatorInst(long Val);

        @LibrarySymbol(name = "LLVMIsABranchInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABranchInst(long Val);

        @LibrarySymbol(name = "LLVMIsAIndirectBrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIndirectBrInst(long Val);

        @LibrarySymbol(name = "LLVMIsAInvokeInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAInvokeInst(long Val);

        @LibrarySymbol(name = "LLVMIsAReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAReturnInst(long Val);

        @LibrarySymbol(name = "LLVMIsASwitchInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASwitchInst(long Val);

        @LibrarySymbol(name = "LLVMIsAUnreachableInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUnreachableInst(long Val);

        @LibrarySymbol(name = "LLVMIsAResumeInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAResumeInst(long Val);

        @LibrarySymbol(name = "LLVMIsACleanupReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACleanupReturnInst(long Val);

        @LibrarySymbol(name = "LLVMIsACatchReturnInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACatchReturnInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFuncletPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFuncletPadInst(long Val);

        @LibrarySymbol(name = "LLVMIsACatchPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACatchPadInst(long Val);

        @LibrarySymbol(name = "LLVMIsACleanupPadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACleanupPadInst(long Val);

        @LibrarySymbol(name = "LLVMIsAUnaryInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUnaryInstruction(long Val);

        @LibrarySymbol(name = "LLVMIsAAllocaInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAAllocaInst(long Val);

        @LibrarySymbol(name = "LLVMIsACastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsACastInst(long Val);

        @LibrarySymbol(name = "LLVMIsAAddrSpaceCastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAAddrSpaceCastInst(long Val);

        @LibrarySymbol(name = "LLVMIsABitCastInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsABitCastInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFPExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPExtInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFPToSIInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPToSIInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFPToUIInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPToUIInst(long Val);

        @LibrarySymbol(name = "LLVMIsAFPTruncInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAFPTruncInst(long Val);

        @LibrarySymbol(name = "LLVMIsAIntToPtrInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAIntToPtrInst(long Val);

        @LibrarySymbol(name = "LLVMIsAPtrToIntInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAPtrToIntInst(long Val);

        @LibrarySymbol(name = "LLVMIsASExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASExtInst(long Val);

        @LibrarySymbol(name = "LLVMIsASIToFPInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsASIToFPInst(long Val);

        @LibrarySymbol(name = "LLVMIsATruncInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsATruncInst(long Val);

        @LibrarySymbol(name = "LLVMIsAUIToFPInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAUIToFPInst(long Val);

        @LibrarySymbol(name = "LLVMIsAZExtInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAZExtInst(long Val);

        @LibrarySymbol(name = "LLVMIsAExtractValueInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAExtractValueInst(long Val);

        @LibrarySymbol(name = "LLVMIsALoadInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsALoadInst(long Val);

        @LibrarySymbol(name = "LLVMIsAVAArgInst")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAVAArgInst(long Val);

        @LibrarySymbol(name = "LLVMIsAMDNode")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMDNode(long Val);

        @LibrarySymbol(name = "LLVMIsAMDString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIsAMDString(long Val);

        @LibrarySymbol(name = "LLVMGetFirstUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstUse(long Val);

        @LibrarySymbol(name = "LLVMGetNextUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextUse(long U);

        @LibrarySymbol(name = "LLVMGetUser")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUser(long U);

        @LibrarySymbol(name = "LLVMGetUsedValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUsedValue(long U);

        @LibrarySymbol(name = "LLVMGetOperand")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetOperand(long Val, int Index);

        @LibrarySymbol(name = "LLVMGetOperandUse")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetOperandUse(long Val, int Index);

        @LibrarySymbol(name = "LLVMSetOperand")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetOperand(long User, int Index, long Val);

        @LibrarySymbol(name = "LLVMGetNumOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumOperands(long Val);

        @LibrarySymbol(name = "LLVMConstNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNull(long Ty);

        @LibrarySymbol(name = "LLVMConstAllOnes")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstAllOnes(long Ty);

        @LibrarySymbol(name = "LLVMGetUndef")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUndef(long Ty);

        @LibrarySymbol(name = "LLVMIsNull")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsNull(long Val);

        @LibrarySymbol(name = "LLVMConstPointerNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstPointerNull(long Ty);

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
        abstract long LLVMConstVector(long, int);*/

        @LibrarySymbol(name = "LLVMGetConstOpcode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetConstOpcode(long ConstantVal);

        @LibrarySymbol(name = "LLVMAlignOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMAlignOf(long Ty);

        @LibrarySymbol(name = "LLVMSizeOf")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMSizeOf(long Ty);

        @LibrarySymbol(name = "LLVMConstNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNeg(long ConstantVal);

        @LibrarySymbol(name = "LLVMConstNSWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNSWNeg(long ConstantVal);

        @LibrarySymbol(name = "LLVMConstNUWNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNUWNeg(long ConstantVal);

        @LibrarySymbol(name = "LLVMConstFNeg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstFNeg(long ConstantVal);

        @LibrarySymbol(name = "LLVMConstNot")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMConstNot(long ConstantVal);

        @LibrarySymbol(name = "LLVMConstAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAdd(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNSWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWAdd(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNUWAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWAdd(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFAdd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFAdd(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSub(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNSWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWSub(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNUWSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWSub(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFSub")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFSub(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstMul(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNSWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNSWMul(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstNUWMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstNUWMul(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFMul")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFMul(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstUDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstUDiv(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSDiv(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstExactSDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstExactSDiv(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFDiv")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFDiv(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstURem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstURem(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstSRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSRem(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFRem")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFRem(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstAnd")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAnd(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstOr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstOr(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstXor")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstXor(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstICmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstICmp(int Predicate, long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstFCmp")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFCmp(int Predicate, long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstShl")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstShl(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstLShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstLShr(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstAShr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAShr(long LHSConstant, long RHSConstant);

        @LibrarySymbol(name = "LLVMConstGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstGEP(long ConstantVal, long ConstantIndices, int NumIndices);

        @LibrarySymbol(name = "LLVMConstInBoundsGEP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstInBoundsGEP(long ConstantVal, long ConstantIndices, int NumIndices);

        @LibrarySymbol(name = "LLVMConstTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstTrunc(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstSExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSExt(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstZExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstZExt(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstFPTrunc")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPTrunc(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstFPExt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPExt(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstUIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstUIToFP(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstSIToFP")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSIToFP(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstFPToUI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPToUI(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstFPToSI")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPToSI(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstPtrToInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstPtrToInt(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstIntToPtr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstIntToPtr(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstBitCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstAddrSpaceCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstAddrSpaceCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstZExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstZExtOrBitCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstSExtOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSExtOrBitCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstTruncOrBitCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstTruncOrBitCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstPointerCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstPointerCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstIntCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT})
        abstract long LLVMConstIntCast(long ConstantVal, long ToType, boolean IsSigned);

        @LibrarySymbol(name = "LLVMConstFPCast")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstFPCast(long ConstantVal, long ToType);

        @LibrarySymbol(name = "LLVMConstSelect")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstSelect(long ConstantCondition, long ConstantIfTrue, long ConstantIfFalse);

        @LibrarySymbol(name = "LLVMConstExtractElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstExtractElement(long VectorConstant, long IndexConstant);

        @LibrarySymbol(name = "LLVMConstInsertElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstInsertElement(long VectorConstant, long ElementValueConstant, long IndexConstant);

        @LibrarySymbol(name = "LLVMConstShuffleVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMConstShuffleVector(long VectorAConstant, long VectorBConstant, long MaskConstant);

        /*@LibrarySymbol(name = "LLVMConstExtractValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstExtractValue(long, long, int);

        @LibrarySymbol(name = "LLVMConstInsertValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMConstInsertValue(long, long, long, int);*/

        @LibrarySymbol(name = "LLVMConstInlineAsm")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT, BOOL_AS_INT})
        abstract long LLVMConstInlineAsm(long Ty, long AsmString, long Constraints, boolean HasSideEffects, boolean IsAlignStack);

        @LibrarySymbol(name = "LLVMBlockAddress")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBlockAddress(long F, long BB);

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

        @LibrarySymbol(name = "LLVMHasUnnamedAddr")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMHasUnnamedAddr(long Global);

        @LibrarySymbol(name = "LLVMSetUnnamedAddr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetUnnamedAddr(long Global, boolean HasUnnamedAddr);

        @LibrarySymbol(name = "LLVMGetAlignment")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetAlignment(long V);

        @LibrarySymbol(name = "LLVMSetAlignment")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetAlignment(long V, int Bytes);

        @LibrarySymbol(name = "LLVMAddGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddGlobal(long M, long Ty, long Name);

        @LibrarySymbol(name = "LLVMAddGlobalInAddressSpace")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMAddGlobalInAddressSpace(long M, long Ty, long Name, int AddressSpace);

        @LibrarySymbol(name = "LLVMGetNamedGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetNamedGlobal(long M, long Name);

        @LibrarySymbol(name = "LLVMGetFirstGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstGlobal(long M);

        @LibrarySymbol(name = "LLVMGetLastGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastGlobal(long M);

        @LibrarySymbol(name = "LLVMGetNextGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextGlobal(long GlobalVar);

        @LibrarySymbol(name = "LLVMGetPreviousGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousGlobal(long GlobalVar);

        @LibrarySymbol(name = "LLVMDeleteGlobal")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteGlobal(long GlobalVar);

        @LibrarySymbol(name = "LLVMGetInitializer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetInitializer(long GlobalVar);

        @LibrarySymbol(name = "LLVMSetInitializer")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetInitializer(long GlobalVar, long ConstantVal);

        @LibrarySymbol(name = "LLVMIsThreadLocal")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsThreadLocal(long GlobalVar);

        @LibrarySymbol(name = "LLVMSetThreadLocal")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetThreadLocal(long GlobalVar, boolean IsThreadLocal);

        @LibrarySymbol(name = "LLVMIsGlobalConstant")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsGlobalConstant(long GlobalVar);

        @LibrarySymbol(name = "LLVMSetGlobalConstant")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetGlobalConstant(long GlobalVar, boolean IsConstant);

        @LibrarySymbol(name = "LLVMGetThreadLocalMode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetThreadLocalMode(long GlobalVar);

        @LibrarySymbol(name = "LLVMSetThreadLocalMode")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetThreadLocalMode(long GlobalVar, int Mode);

        @LibrarySymbol(name = "LLVMIsExternallyInitialized")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsExternallyInitialized(long GlobalVar);

        @LibrarySymbol(name = "LLVMSetExternallyInitialized")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetExternallyInitialized(long GlobalVar, boolean IsExtInit);

        @LibrarySymbol(name = "LLVMAddAlias")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAddAlias(long M, long Ty, long Name, long Aliasee);

        @LibrarySymbol(name = "LLVMDeleteFunction")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteFunction(long Fn);

        @LibrarySymbol(name = "LLVMHasPersonalityFn")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMHasPersonalityFn(long Fn);

        @LibrarySymbol(name = "LLVMGetPersonalityFn")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPersonalityFn(long Fn);

        @LibrarySymbol(name = "LLVMSetPersonalityFn")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetPersonalityFn(long Fn, long PersonalityFn);

        @LibrarySymbol(name = "LLVMGetIntrinsicID")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetIntrinsicID(long Fn);

        @LibrarySymbol(name = "LLVMGetFunctionCallConv")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetFunctionCallConv(long Fn);

        @LibrarySymbol(name = "LLVMSetFunctionCallConv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetFunctionCallConv(long Fn, int CC);

        @LibrarySymbol(name = "LLVMGetGC")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetGC(long Fn);

        @LibrarySymbol(name = "LLVMSetGC")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetGC(long Fn, long Name);

        @LibrarySymbol(name = "LLVMAddAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMAddAttributeAtIndex(long F, int Idx, long A);

        @LibrarySymbol(name = "LLVMGetEnumAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, INT})
        abstract long LLVMGetEnumAttributeAtIndex(long F, int Idx, int KindID);

        @LibrarySymbol(name = "LLVMGetStringAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract long LLVMGetStringAttributeAtIndex(long F, int Idx, long K, int KLen);

        @LibrarySymbol(name = "LLVMRemoveEnumAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMRemoveEnumAttributeAtIndex(long F, int Idx, int KindID);

        @LibrarySymbol(name = "LLVMRemoveStringAttributeAtIndex")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract void LLVMRemoveStringAttributeAtIndex(long F, int Idx, long K, int KLen);

        @LibrarySymbol(name = "LLVMAddTargetDependentFunctionAttr")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddTargetDependentFunctionAttr(long Fn, long A, long V);

        @LibrarySymbol(name = "LLVMCountParams")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountParams(long Fn);

        @LibrarySymbol(name = "LLVMGetParams")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetParams(long Fn, long Params);

        @LibrarySymbol(name = "LLVMGetParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetParam(long Fn, int Index);

        @LibrarySymbol(name = "LLVMGetParamParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetParamParent(long Inst);

        @LibrarySymbol(name = "LLVMGetFirstParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstParam(long Fn);

        @LibrarySymbol(name = "LLVMGetLastParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastParam(long Fn);

        @LibrarySymbol(name = "LLVMGetNextParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextParam(long Arg);

        @LibrarySymbol(name = "LLVMGetPreviousParam")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousParam(long Arg);

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
        abstract void LLVMGetMDNodeOperands(long, long);*/

        @LibrarySymbol(name = "LLVMBasicBlockAsValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMBasicBlockAsValue(long BB);

        @LibrarySymbol(name = "LLVMValueIsBasicBlock")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMValueIsBasicBlock(long Val);

        @LibrarySymbol(name = "LLVMValueAsBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMValueAsBasicBlock(long Val);

        @LibrarySymbol(name = "LLVMGetBasicBlockName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockName(long BB);

        @LibrarySymbol(name = "LLVMGetBasicBlockParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockParent(long BB);

        @LibrarySymbol(name = "LLVMGetBasicBlockTerminator")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetBasicBlockTerminator(long BB);

        @LibrarySymbol(name = "LLVMCountBasicBlocks")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMCountBasicBlocks(long Fn);

        @LibrarySymbol(name = "LLVMGetBasicBlocks")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMGetBasicBlocks(long Fn, long BasicBlocks);

        @LibrarySymbol(name = "LLVMGetFirstBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstBasicBlock(long Fn);

        @LibrarySymbol(name = "LLVMGetLastBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastBasicBlock(long Fn);

        @LibrarySymbol(name = "LLVMGetNextBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextBasicBlock(long BB);

        @LibrarySymbol(name = "LLVMGetPreviousBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousBasicBlock(long BB);

        @LibrarySymbol(name = "LLVMGetEntryBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetEntryBasicBlock(long Fn);

        @LibrarySymbol(name = "LLVMAppendBasicBlockInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAppendBasicBlockInContext(long C, long Fn, long Name);

        @LibrarySymbol(name = "LLVMAppendBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMAppendBasicBlock(long Fn, long Name);

        @LibrarySymbol(name = "LLVMInsertBasicBlockInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMInsertBasicBlockInContext(long C, long BB, long Name);

        @LibrarySymbol(name = "LLVMInsertBasicBlock")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMInsertBasicBlock(long InsertBeforeBB, long Name);

        @LibrarySymbol(name = "LLVMDeleteBasicBlock")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDeleteBasicBlock(long BB);

        @LibrarySymbol(name = "LLVMRemoveBasicBlockFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRemoveBasicBlockFromParent(long BB);

        @LibrarySymbol(name = "LLVMMoveBasicBlockBefore")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMMoveBasicBlockBefore(long BB, long MovePos);

        @LibrarySymbol(name = "LLVMMoveBasicBlockAfter")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMMoveBasicBlockAfter(long BB, long MovePos);

        @LibrarySymbol(name = "LLVMGetFirstInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetFirstInstruction(long BB);

        @LibrarySymbol(name = "LLVMGetLastInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetLastInstruction(long BB);

        @LibrarySymbol(name = "LLVMHasMetadata")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMHasMetadata(long Val);

        @LibrarySymbol(name = "LLVMGetMetadata")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetMetadata(long Val, int KindID);

        @LibrarySymbol(name = "LLVMSetMetadata")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetMetadata(long Val, int KindID, long Node);

        @LibrarySymbol(name = "LLVMGetInstructionParent")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetInstructionParent(long Inst);

        @LibrarySymbol(name = "LLVMGetNextInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextInstruction(long Inst);

        @LibrarySymbol(name = "LLVMGetPreviousInstruction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetPreviousInstruction(long Inst);

        @LibrarySymbol(name = "LLVMInstructionRemoveFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInstructionRemoveFromParent(long Inst);

        @LibrarySymbol(name = "LLVMInstructionEraseFromParent")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInstructionEraseFromParent(long Inst);

        @LibrarySymbol(name = "LLVMGetInstructionOpcode")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetInstructionOpcode(long Inst);

        @LibrarySymbol(name = "LLVMGetICmpPredicate")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetICmpPredicate(long Inst);

        @LibrarySymbol(name = "LLVMGetFCmpPredicate")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetFCmpPredicate(long Inst);

        @LibrarySymbol(name = "LLVMInstructionClone")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMInstructionClone(long Inst);

        @LibrarySymbol(name = "LLVMGetNumArgOperands")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumArgOperands(long Instr);

        @LibrarySymbol(name = "LLVMSetInstructionCallConv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetInstructionCallConv(long Instr, int CC);

        @LibrarySymbol(name = "LLVMGetInstructionCallConv")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetInstructionCallConv(long Instr);

        @LibrarySymbol(name = "LLVMSetInstrParamAlignment")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMSetInstrParamAlignment(long Instr, int index, int Align);

        @LibrarySymbol(name = "LLVMAddCallSiteAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMAddCallSiteAttribute(long C, int Idx, long A);

        @LibrarySymbol(name = "LLVMGetCallSiteEnumAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, INT})
        abstract long LLVMGetCallSiteEnumAttribute(long C, int Idx, int KindID);

        @LibrarySymbol(name = "LLVMGetCallSiteStringAttribute")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract long LLVMGetCallSiteStringAttribute(long C, int Idx, long K, int KLen);

        @LibrarySymbol(name = "LLVMRemoveCallSiteEnumAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, INT})
        abstract void LLVMRemoveCallSiteEnumAttribute(long C, int Idx, int KindID);

        @LibrarySymbol(name = "LLVMRemoveCallSiteStringAttribute")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD, INT})
        abstract void LLVMRemoveCallSiteStringAttribute(long C, int Idx, long K, int KLen);

        @LibrarySymbol(name = "LLVMGetCalledValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetCalledValue(long Instr);

        @LibrarySymbol(name = "LLVMIsTailCall")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsTailCall(long CallInst);

        @LibrarySymbol(name = "LLVMSetTailCall")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetTailCall(long CallInst, boolean IsTailCall);

        @LibrarySymbol(name = "LLVMGetNormalDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNormalDest(long InvokeInst);

        @LibrarySymbol(name = "LLVMGetUnwindDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetUnwindDest(long InvokeInst);

        @LibrarySymbol(name = "LLVMSetNormalDest")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetNormalDest(long InvokeInst, long B);

        @LibrarySymbol(name = "LLVMSetUnwindDest")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetUnwindDest(long InvokeInst, long B);

        @LibrarySymbol(name = "LLVMGetNumSuccessors")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumSuccessors(long Term);

        @LibrarySymbol(name = "LLVMGetSuccessor")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetSuccessor(long Term, int Idx);

        @LibrarySymbol(name = "LLVMSetSuccessor")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract void LLVMSetSuccessor(long Term, int Idx, long Block);

        @LibrarySymbol(name = "LLVMIsConditional")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsConditional(long Branch);

        @LibrarySymbol(name = "LLVMGetCondition")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetCondition(long Branch);

        @LibrarySymbol(name = "LLVMSetCondition")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetCondition(long Branch, long Cond);

        @LibrarySymbol(name = "LLVMGetSwitchDefaultDest")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSwitchDefaultDest(long SwitchInstr);

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

        @LibrarySymbol(name = "LLVMGetNumIndices")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumIndices(long Inst);

        @LibrarySymbol(name = "LLVMGetIndices")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetIndices(long Inst);

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

        @LibrarySymbol(name = "LLVMBuildAggregateRet")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildAggregateRet(long B, long RetVals, int N);

        @LibrarySymbol(name = "LLVMBuildBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildBr(long B, long Dest);

        @LibrarySymbol(name = "LLVMBuildCondBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildCondBr(long B, long If, long Then, long Else);

        @LibrarySymbol(name = "LLVMBuildSwitch")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildSwitch(long B, long V, long Else, int NumCases);

        @LibrarySymbol(name = "LLVMBuildIndirectBr")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMBuildIndirectBr(long B, long Addr, int NumCases);

        @LibrarySymbol(name = "LLVMBuildInvoke")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildInvoke(long B, long Fn, long Args, int NumArgs, long Than, long Catch, long Name);

        @LibrarySymbol(name = "LLVMBuildLandingPad")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildLandingPad(long B, long Ty, long PersFn, int NumClauses, long Name);

        @LibrarySymbol(name = "LLVMBuildResume")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildResume(long B, long Exn);

        @LibrarySymbol(name = "LLVMBuildUnreachable")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMBuildUnreachable(long B);

        @LibrarySymbol(name = "LLVMAddCase")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddCase(long Switch, long OnVal, long Dest);

        @LibrarySymbol(name = "LLVMAddDestination")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddDestination(long IndirectBr, long Dest);

        @LibrarySymbol(name = "LLVMGetNumClauses")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetNumClauses(long LandingPad);

        @LibrarySymbol(name = "LLVMGetClause")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMGetClause(long LandingPad, int Idx);

        @LibrarySymbol(name = "LLVMAddClause")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddClause(long LandingPad, long ClauseVal);

        @LibrarySymbol(name = "LLVMIsCleanup")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsCleanup(long LandingPad);

        @LibrarySymbol(name = "LLVMSetCleanup")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetCleanup(long LandingPad, boolean Val);

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

        @LibrarySymbol(name = "LLVMBuildVAArg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildVAArg(long B, long List, long Ty, long Name);

        @LibrarySymbol(name = "LLVMBuildExtractElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildExtractElement(long B, long VecVal, long Index, long Name);

        @LibrarySymbol(name = "LLVMBuildInsertElement")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildInsertElement(long B, long VecVal, long EltVal, long Index, long Name);

        @LibrarySymbol(name = "LLVMBuildShuffleVector")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildShuffleVector(long B, long V1, long V2, long Mask, long Name);

        @LibrarySymbol(name = "LLVMBuildExtractValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildExtractValue(long B, long AggVal, int Index, long Name);

        @LibrarySymbol(name = "LLVMBuildInsertValue")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMBuildInsertValue(long B, long AggVal, long EltVal, int Index, long Name);

        @LibrarySymbol(name = "LLVMBuildIsNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIsNull(long B, long Val, long Name);

        @LibrarySymbol(name = "LLVMBuildIsNotNull")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildIsNotNull(long B, long Val, long Name);

        @LibrarySymbol(name = "LLVMBuildPtrDiff")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMBuildPtrDiff(long B, long LHS, long RHS, long Name);

        @LibrarySymbol(name = "LLVMBuildFence")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, BOOL_AS_INT, LONG_AS_WORD})
        abstract long LLVMBuildFence(long B, int Ordering, boolean SingleThread, long Name);

        @LibrarySymbol(name = "LLVMBuildAtomicRMW")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD, INT, BOOL_AS_INT})
        abstract long LLVMBuildAtomicRMW(long B, int Op, long Ptr, long Val, int Ordering, boolean SingleThread);

        @LibrarySymbol(name = "LLVMBuildAtomicCmpXchg")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, INT, BOOL_AS_INT})
        abstract long LLVMBuildAtomicCmpXchg(long B, long Ptr, long Cmp, long New, int SuccessOrdering, int FailureOrdering, boolean SingleThread);

        @LibrarySymbol(name = "LLVMIsAtomicSingleThread")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMIsAtomicSingleThread(long AtomicInst);

        @LibrarySymbol(name = "LLVMSetAtomicSingleThread")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetAtomicSingleThread(long AtomicInst, boolean SingleThread);

        @LibrarySymbol(name = "LLVMGetCmpXchgSuccessOrdering")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetCmpXchgSuccessOrdering(long CmpXchgInst);

        @LibrarySymbol(name = "LLVMSetCmpXchgSuccessOrdering")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetCmpXchgSuccessOrdering(long CmpXchgInst, int Ordering);

        @LibrarySymbol(name = "LLVMGetCmpXchgFailureOrdering")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGetCmpXchgFailureOrdering(long CmpXchgInst);

        @LibrarySymbol(name = "LLVMSetCmpXchgFailureOrdering")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMSetCmpXchgFailureOrdering(long CmpXchgInst, int Ordering);

        @LibrarySymbol(name = "LLVMCreateModuleProviderForExistingModule")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateModuleProviderForExistingModule(long M);

        @LibrarySymbol(name = "LLVMDisposeModuleProvider")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeModuleProvider(long M);

        @LibrarySymbol(name = "LLVMCreateMemoryBufferWithContentsOfFile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMemoryBufferWithContentsOfFile(long Path, long OutMemBuf, long OutMessage);

        @LibrarySymbol(name = "LLVMCreateMemoryBufferWithSTDIN")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMemoryBufferWithSTDIN(long OutMemBuf, long OutMessage);

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

        @LibrarySymbol(name = "LLVMStartMultithreaded")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {})
        abstract boolean LLVMStartMultithreaded();

        @LibrarySymbol(name = "LLVMStopMultithreaded")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMStopMultithreaded();

        @LibrarySymbol(name = "LLVMIsMultithreaded")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {})
        abstract boolean LLVMIsMultithreaded();

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
    @SuppressWarnings("unused")
    static long nLLVMCreateMessage(String Message) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Message = allocString(arena, Message);
            return Native.INSTANCE.LLVMCreateMessage(c_Message.nativeAddress());
        }
    }

    /* package-private */
    static void nLLVMDisposeMessage(long Message) {
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
        return LLVMContextRef.of(Native.INSTANCE.LLVMContextCreate());
    }

    /**
     * Obtain the global context instance.
     */
    public static LLVMContextRef LLVMGetGlobalContext() {
        return LLVMContextRef.of(Native.INSTANCE.LLVMGetGlobalContext());
    }

    /**
     * Set the diagnostic handler for this context.
     */
    /* package-private */
    @SuppressWarnings("SameParameterValue")
    static void nLLVMContextSetDiagnosticHandler(LLVMContextRef C, long /* LLVMDiagnosticHandler */ Handler, long /* void* */ DiagnosticContext) {
        Native.INSTANCE.LLVMContextSetDiagnosticHandler(C.value(), Handler, DiagnosticContext);
    }

    /**
     * Set the diagnostic handler for this context.
     */
    // Port-added
    public static void LLVMContextSetDiagnosticHandler(LLVMContextRef C, Arena arena, LLVMDiagnosticHandler Handler) {
        if (Handler == null) {
            nLLVMContextSetDiagnosticHandler(C, 0, 0);
            return;
        }
        var linker = Linker.nativeLinker();
        var native_handler = linker.upcallStub(invoker(Handler), DIAGNOSTIC_HANDLER_DESCRIPTOR, arena);
        nLLVMContextSetDiagnosticHandler(C, native_handler.nativeAddress(), 0);
    }

    /**
     * Get the diagnostic handler of this context.
     */
    /* package-private */
    //TODO: make public version
    @SuppressWarnings("unused")
    static long /* LLVMDiagnosticHandler */ nLLVMContextGetDiagnosticHandler(LLVMContextRef C) {
        return Native.INSTANCE.LLVMContextGetDiagnosticHandler(C.value());
    }

    /**
     * Get the diagnostic context of this context.
     */
    /* package-private */
    //TODO: make public version
    @SuppressWarnings("unused")
    static long /* void* */ nLLVMContextGetDiagnosticContext(LLVMContextRef C) {
        return Native.INSTANCE.LLVMContextGetDiagnosticContext(C.value());
    }

    /**
     * Set the yield callback function for this context.
     */
    /* package-private */
    @SuppressWarnings("SameParameterValue")
    static void nLLVMContextSetYieldCallback(LLVMContextRef C, long /* LLVMYieldCallback */ Callback, long /* void* */ OpaqueHandle) {
        Native.INSTANCE.LLVMContextSetYieldCallback(C.value(), Callback, OpaqueHandle);
    }

    /**
     * Set the yield callback function for this context.
     */
    // Port-added
    public static void LLVMContextSetYieldCallback(LLVMContextRef C, Arena arena, LLVMYieldCallback Callback) {
        if (Callback == null) {
            nLLVMContextSetYieldCallback(C, 0, 0);
            return;
        }
        var linker = Linker.nativeLinker();
        var native_callback = linker.upcallStub(invoker(Callback), YIELD_CALLBACK_DESCRIPTOR, arena);
        nLLVMContextSetYieldCallback(C, native_callback.nativeAddress(), 0);
    }

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
        return LLVMAttributeRef.of(Native.INSTANCE.LLVMCreateEnumAttribute(C.value(), KindID, Val));
    }

    /**
     * Create an enum attribute.
     */
    // Port-added
    public static LLVMAttributeRef LLVMCreateEnumAttribute(LLVMContextRef C, String KindName, long /* uint64_t */ Val) {
        int kind = LLVMGetEnumAttributeKindForName(KindName);
        if (kind == 0) {
            throw new IllegalArgumentException("Can`t find enum attribute with name \"" + KindName + "\"");
        }
        return LLVMCreateEnumAttribute(C, kind, Val);
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
            return LLVMAttributeRef.of(Native.INSTANCE.LLVMCreateStringAttribute(
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
            return LLVMModuleRef.of(Native.INSTANCE.LLVMModuleCreateWithName(c_ModuleID.nativeAddress()));
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
            return LLVMModuleRef.of(Native.INSTANCE.LLVMModuleCreateWithNameInContext(c_ModuleID.nativeAddress(), C.value()));
        }
    }

    /**
     * Return an exact copy of the specified module.
     */
    public static LLVMModuleRef LLVMCloneModule(LLVMModuleRef M) {
        return LLVMModuleRef.of(Native.INSTANCE.LLVMCloneModule(M.value()));
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

    /**
     * Obtain the identifier of a module.
     *
     * @param M Module to obtain identifier of
     * @return The identifier of M.
     */
    public static String LLVMGetModuleIdentifier(LLVMModuleRef M) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Len = arena.allocate(WORD);
            long ptr = Native.INSTANCE.LLVMGetModuleIdentifier(M.value(), c_Len.nativeAddress());
            long /* size_t */ Len = IS64BIT ? c_Len.get(JAVA_LONG, 0) :
                    c_Len.get(JAVA_INT, 0) & 0xffffffffL;
            return addressToString(ptr, Len);
        }
    }

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

    /**
     * Print a representation of a module to a file. The ErrorMessage needs to be
     * disposed with LLVMDisposeMessage. Returns 0 on success, 1 otherwise.
     */
    /* package-private */
    static boolean nLLVMPrintModuleToFile(LLVMModuleRef M, String Filename, Consumer<String> ErrorMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Filename = allocString(arena, Filename);
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMPrintModuleToFile(M.value(),
                    c_Filename.nativeAddress(), c_ErrorMessage.nativeAddress());
            if (err) {
                ErrorMessage.accept(addressToLLVMString(c_ErrorMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Print a representation of a module to a file.
     */
    // Port-added
    public static void LLVMPrintModuleToFile(LLVMModuleRef M, String Filename) throws LLVMException {
        String[] err = new String[1];
        if (nLLVMPrintModuleToFile(M, Filename, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
    }

    /**
     * Return a string representation of the module.
     */
    public static String LLVMPrintModuleToString(LLVMModuleRef M) {
        return addressToLLVMString(Native.INSTANCE.LLVMPrintModuleToString(M.value()));
    }

    /**
     * Set inline assembly for a module.
     */
    public static void LLVMSetModuleInlineAsm(LLVMModuleRef M, String Asm) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Asm = allocString(arena, Asm);
            Native.INSTANCE.LLVMSetModuleInlineAsm(M.value(), c_Asm.nativeAddress());
        }
    }

    /**
     * Obtain the context to which this module is associated.
     */
    public static LLVMContextRef LLVMGetModuleContext(LLVMModuleRef M) {
        return LLVMContextRef.of(Native.INSTANCE.LLVMGetModuleContext(M.value()));
    }

    /**
     * Obtain a Type from a module by its registered name.
     */
    public static LLVMTypeRef LLVMGetTypeByName(LLVMModuleRef M, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMTypeRef.of(Native.INSTANCE.LLVMGetTypeByName(M.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Obtain the number of operands for named metadata in a module.
     */
    public static int /* unsigned */ LLVMGetNamedMetadataNumOperands(LLVMModuleRef M, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return Native.INSTANCE.LLVMGetNamedMetadataNumOperands(M.value(), c_Name.nativeAddress());
        }
    }

    /**
     * Obtain the named metadata operands for a module.
     * <p>
     * The passed LLVMValueRef pointer should refer to an array of
     * LLVMValueRef at least LLVMGetNamedMetadataNumOperands long. This
     * array will be populated with the LLVMValueRef instances. Each
     * instance corresponds to a llvm::MDNode.
     */
    /* package-private */
    static void nLLVMGetNamedMetadataOperands(LLVMModuleRef M, String Name, long Dest) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            Native.INSTANCE.LLVMGetNamedMetadataOperands(M.value(), c_Name.nativeAddress(), Dest);
        }
    }

    /**
     * Obtain the named metadata operands for a module.
     * <p>
     * Return array will be populated with the LLVMValueRef instances.
     * Each instance corresponds to a llvm::MDNode.
     */
    // Port-added
    public static LLVMValueRef[] LLVMGetNamedMetadataOperands(LLVMModuleRef M, String Name) {
        int /* unsigned */ count = LLVMGetNamedMetadataNumOperands(M, Name);
        if (count == 0) {
            return new LLVMValueRef[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Dest = allocPointerArray(arena, count);
            nLLVMGetNamedMetadataOperands(M, Name, c_Dest.nativeAddress());
            return readPointerArray(c_Dest, LLVMValueRef.class, LLVMValueRef::of);
        }
    }

    /**
     * Add an operand to named metadata.
     */
    public static void LLVMAddNamedMetadataOperand(LLVMModuleRef M, String Name, LLVMValueRef Val) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            Native.INSTANCE.LLVMAddNamedMetadataOperand(M.value(), c_Name.nativeAddress(), Val.value());
        }
    }

    /**
     * Add a function to a module under a specified name.
     */
    public static LLVMValueRef LLVMAddFunction(LLVMModuleRef M, String Name, LLVMTypeRef FunctionTy) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMAddFunction(
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
            return LLVMValueRef.of(Native.INSTANCE.LLVMGetNamedFunction(
                    M.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Obtain an iterator to the first Function in a Module.
     */
    public static LLVMValueRef LLVMGetFirstFunction(LLVMModuleRef M) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetFirstFunction(M.value()));
    }

    /**
     * Obtain an iterator to the last Function in a Module.
     */
    public static LLVMValueRef LLVMGetLastFunction(LLVMModuleRef M) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetLastFunction(M.value()));
    }

    /**
     * Advance a Function iterator to the next function.
     * <p>
     * Returns NULL if the iterator was already at the end and there are no more
     * functions.
     */
    public static LLVMValueRef LLVMGetNextFunction(LLVMValueRef Fn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetNextFunction(Fn.value()));
    }

    /**
     * Decrement a Function iterator to the previous function.
     * <p>
     * Returns NULL if the iterator was already at the beginning and there are
     * no previous functions.
     */
    public static LLVMValueRef LLVMGetPreviousFunction(LLVMValueRef Fn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetPreviousFunction(Fn.value()));
    }

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
        return LLVMContextRef.of(Native.INSTANCE.LLVMGetTypeContext(Ty.value()));
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt1TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt8TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt8TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt16TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt16TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt32TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt32TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt64TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt64TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMInt128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt128TypeInContext(C.value()));
    }

    public static LLVMTypeRef LLVMIntTypeInContext(LLVMContextRef C, int /* unsigned */ NumBits) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntTypeInContext(C.value(), NumBits));
    }

    /**
     * Obtain an integer type from the global context with a specified bit
     * width.
     */
    public static LLVMTypeRef LLVMInt1Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt1Type());
    }

    public static LLVMTypeRef LLVMInt8Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt8Type());
    }

    public static LLVMTypeRef LLVMInt16Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt16Type());
    }

    public static LLVMTypeRef LLVMInt32Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt32Type());
    }

    public static LLVMTypeRef LLVMInt64Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt64Type());
    }

    public static LLVMTypeRef LLVMInt128Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMInt128Type());
    }

    public static LLVMTypeRef LLVMIntType(int /* unsigned */ NumBits) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntType(NumBits));
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMHalfTypeInContext(C.value()));
    }

    /**
     * Obtain a 32-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMFloatTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMFloatTypeInContext(C.value()));
    }

    /**
     * Obtain a 64-bit floating point type from a context.
     */
    public static LLVMTypeRef LLVMDoubleTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMDoubleTypeInContext(C.value()));
    }

    /**
     * Obtain a 80-bit floating point type (X87) from a context.
     */
    public static LLVMTypeRef LLVMX86FP80TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMX86FP80TypeInContext(C.value()));
    }

    /**
     * Obtain a 128-bit floating point type (112-bit mantissa) from a
     * context.
     */
    public static LLVMTypeRef LLVMFP128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMFP128TypeInContext(C.value()));
    }

    /**
     * Obtain a 128-bit floating point type (two 64-bits) from a context.
     */
    public static LLVMTypeRef LLVMPPCFP128TypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMPPCFP128TypeInContext(C.value()));
    }

    /**
     * Obtain a floating point type from the global context.
     * <p>
     * These map to the functions in this group of the same name.
     */
    public static LLVMTypeRef LLVMHalfType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMHalfType());
    }

    public static LLVMTypeRef LLVMFloatType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMFloatType());
    }

    public static LLVMTypeRef LLVMDoubleType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMDoubleType());
    }

    public static LLVMTypeRef LLVMX86FP80Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMX86FP80Type());
    }

    public static LLVMTypeRef LLVMFP128Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMFP128Type());
    }

    public static LLVMTypeRef LLVMPPCFP128Type() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMPPCFP128Type());
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
            return LLVMTypeRef.of(Native.INSTANCE.LLVMFunctionType(
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMGetReturnType(FunctionTy.value()));
    }

    /**
     * Obtain the number of parameters this function accepts.
     */
    public static int /* unsigned */ LLVMCountParamTypes(LLVMTypeRef FunctionTy) {
        return Native.INSTANCE.LLVMCountParamTypes(FunctionTy.value());
    }

    /**
     * Obtain the types of a function's parameters.
     * <p>
     * The Dest parameter should point to a pre-allocated array of
     * LLVMTypeRef at least LLVMCountParamTypes() large. On return, the
     * first LLVMCountParamTypes() entries in the array will be populated
     * with LLVMTypeRef instances.
     *
     * @param FunctionTy The function type to operate on.
     * @param Dest       Memory address of an array to be filled with result.
     */
    /* package-private */
    static void nLLVMGetParamTypes(LLVMTypeRef FunctionTy, long Dest) {
        Native.INSTANCE.LLVMGetParamTypes(FunctionTy.value(), Dest);
    }

    /**
     * Obtain the types of a function's parameters.
     * <p>
     * Return array will be populated with LLVMTypeRef instances.
     *
     * @param FunctionTy The function type to operate on.
     */
    // Port-added
    public static LLVMTypeRef[] LLVMGetParamTypes(LLVMTypeRef FunctionTy) {
        int /* unsigned */ count = LLVMCountParamTypes(FunctionTy);
        if (count == 0) {
            return new LLVMTypeRef[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Dest = allocPointerArray(arena, count);
            nLLVMGetParamTypes(FunctionTy, c_Dest.nativeAddress());
            return readPointerArray(c_Dest, LLVMTypeRef.class, LLVMTypeRef::of);
        }
    }

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
            return LLVMTypeRef.of(Native.INSTANCE.LLVMStructTypeInContext(
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
            return LLVMTypeRef.of(Native.INSTANCE.LLVMStructType(
                    c_ElementTypes.nativeAddress(), ElementCount, Packed));
        }
    }

    /**
     * Create an empty structure in a context having a specified name.
     */
    public static LLVMTypeRef LLVMStructCreateNamed(LLVMContextRef C, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMTypeRef.of(Native.INSTANCE.LLVMStructCreateNamed
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

    /**
     * Get the number of elements defined inside the structure.
     */
    public static int /* unsigned */ LLVMCountStructElementTypes(LLVMTypeRef StructTy) {
        return Native.INSTANCE.LLVMCountStructElementTypes(StructTy.value());
    }

    /**
     * Get the elements within a structure.
     * <p>
     * The function is passed the address of a pre-allocated array of
     * LLVMTypeRef at least LLVMCountStructElementTypes() long. After
     * invocation, this array will be populated with the structure's
     * elements. The objects in the destination array will have a lifetime
     * of the structure type itself, which is the lifetime of the context it
     * is contained in.
     */
    /* package-private */
    static void nLLVMGetStructElementTypes(LLVMTypeRef StructTy, long Dest) {
        Native.INSTANCE.LLVMGetStructElementTypes(StructTy.value(), Dest);
    }

    /**
     * Get the elements within a structure.
     * <p>
     * Return array will be populated with the structure's elements.
     * The objects in the destination array will have a lifetime of the structure type
     * itself, which is the lifetime of the context it is contained in.
     */
    // Port-added
    public static LLVMTypeRef[] LLVMGetStructElementTypes(LLVMTypeRef StructTy) {
        int /* unsigned */ count = LLVMCountStructElementTypes(StructTy);
        if (count == 0) {
            return new LLVMTypeRef[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Dest = allocPointerArray(arena, count);
            nLLVMGetStructElementTypes(StructTy, c_Dest.nativeAddress());
            return readPointerArray(c_Dest, LLVMTypeRef.class, LLVMTypeRef::of);
        }
    }

    /**
     * Get the type of the element at a given index in the structure.
     */
    public static LLVMTypeRef LLVMStructGetTypeAtIndex(LLVMTypeRef StructTy, int /* unsigned */ Idx) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMStructGetTypeAtIndex(StructTy.value(), Idx));
    }

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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMGetElementType(Ty.value()));
    }

    /**
     * Create a fixed size array type that refers to a specific type.
     * <p>
     * The created type will exist in the context that its element type
     * exists in.
     */
    public static LLVMTypeRef LLVMArrayType(LLVMTypeRef ElementType, int /* unsigned */ ElementCount) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMArrayType(ElementType.value(), ElementCount));
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMPointerType(ElementType.value(), AddressSpace));
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMVectorType(ElementType.value(), ElementCount));
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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMVoidTypeInContext(C.value()));
    }

    /**
     * Create a label type in a context.
     */
    public static LLVMTypeRef LLVMLabelTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMLabelTypeInContext(C.value()));
    }

    /**
     * Create a X86 MMX type in a context.
     */
    public static LLVMTypeRef LLVMX86MMXTypeInContext(LLVMContextRef C) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMX86MMXTypeInContext(C.value()));
    }

    /**
     * Create a void type in the global context.
     */
    public static LLVMTypeRef LLVMVoidType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMVoidType());
    }

    /**
     * Create a label type in the global context.
     */
    public static LLVMTypeRef LLVMLabelType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMLabelType());
    }

    /**
     * Create a X86 MMX type in the global context.
     */
    public static LLVMTypeRef LLVMX86MMXType() {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMX86MMXType());
    }

    /*
     * @defgroup LLVMCCoreValueGeneral General APIs
     *
     * Functions in this section work on all LLVMValueRef instances,
     * regardless of their sub-type. They correspond to functions available
     * on llvm::Value.
     */

    /**
     * Obtain the type of a value.
     */
    public static LLVMTypeRef LLVMTypeOf(LLVMValueRef Val) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMTypeOf(Val.value()));
    }

    /**
     * Obtain the enumerated type of a Value instance.
     */
    public static LLVMValueKind LLVMGetValueKind(LLVMValueRef Val) {
        return LLVMValueKind.of(Native.INSTANCE.LLVMGetValueKind(Val.value()));
    }

    /**
     * Obtain the string name of a value.
     */
    public static String LLVMGetValueName(LLVMValueRef Val) {
        return addressToString(Native.INSTANCE.LLVMGetValueName(Val.value()));
    }

    /**
     * Set the string name of a value.
     */
    public static void LLVMSetValueName(LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            Native.INSTANCE.LLVMSetValueName(Val.value(), c_Name.nativeAddress());
        }
    }

    /**
     * Dump a representation of a value to stderr.
     */
    public static void LLVMDumpValue(LLVMValueRef Val) {
        Native.INSTANCE.LLVMDumpValue(Val.value());
    }

    /**
     * Return a string representation of the value. Use
     * LLVMDisposeMessage to free the string.
     */
    public static String LLVMPrintValueToString(LLVMValueRef Val) {
        return addressToLLVMString(Native.INSTANCE.LLVMPrintValueToString(Val.value()));
    }

    /**
     * Replace all uses of a value with another one.
     */
    public static void LLVMReplaceAllUsesWith(LLVMValueRef OldVal, LLVMValueRef NewVal) {
        Native.INSTANCE.LLVMReplaceAllUsesWith(OldVal.value(), NewVal.value());
    }

    /**
     * Determine whether the specified value instance is constant.
     */
    public static boolean LLVMIsConstant(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMIsConstant(Val.value());
    }

    /**
     * Determine whether a value instance is undefined.
     */
    public static boolean LLVMIsUndef(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMIsUndef(Val.value());
    }

    /**
     * Convert value instances between types.
     * <p>
     * Internally, an LLVMValueRef is "pinned" to a specific type. This
     * series of functions allows you to cast an instance to a specific
     * type.
     * <p>
     * If the cast is not valid for the specified type, NULL is returned.
     */
    public static LLVMValueRef LLVMIsAArgument(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAArgument(Val.value()));
    }

    public static LLVMValueRef LLVMIsABasicBlock(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsABasicBlock(Val.value()));
    }

    public static LLVMValueRef LLVMIsAInlineAsm(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAInlineAsm(Val.value()));
    }

    public static LLVMValueRef LLVMIsAUser(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAUser(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstant(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstant(Val.value()));
    }

    public static LLVMValueRef LLVMIsABlockAddress(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsABlockAddress(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantAggregateZero(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantAggregateZero(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantArray(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantArray(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantDataSequential(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantDataSequential(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantDataArray(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantDataArray(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantDataVector(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantDataVector(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantExpr(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantExpr(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantFP(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantFP(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantInt(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantInt(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantPointerNull(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantPointerNull(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantStruct(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantStruct(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantTokenNone(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantTokenNone(Val.value()));
    }

    public static LLVMValueRef LLVMIsAConstantVector(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAConstantVector(Val.value()));
    }

    public static LLVMValueRef LLVMIsAGlobalValue(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAGlobalValue(Val.value()));
    }

    public static LLVMValueRef LLVMIsAGlobalAlias(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAGlobalAlias(Val.value()));
    }

    public static LLVMValueRef LLVMIsAGlobalObject(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAGlobalObject(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFunction(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFunction(Val.value()));
    }

    public static LLVMValueRef LLVMIsAGlobalVariable(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAGlobalVariable(Val.value()));
    }

    public static LLVMValueRef LLVMIsAUndefValue(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAUndefValue(Val.value()));
    }

    public static LLVMValueRef LLVMIsAInstruction(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAInstruction(Val.value()));
    }

    public static LLVMValueRef LLVMIsABinaryOperator(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsABinaryOperator(Val.value()));
    }

    public static LLVMValueRef LLVMIsACallInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACallInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAIntrinsicInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAIntrinsicInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsADbgInfoIntrinsic(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsADbgInfoIntrinsic(Val.value()));
    }

    public static LLVMValueRef LLVMIsADbgDeclareInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsADbgDeclareInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMemIntrinsic(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMemIntrinsic(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMemCpyInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMemCpyInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMemMoveInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMemMoveInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMemSetInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMemSetInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACmpInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACmpInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFCmpInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFCmpInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAICmpInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAICmpInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAExtractElementInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAExtractElementInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAGetElementPtrInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAGetElementPtrInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAInsertElementInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAInsertElementInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAInsertValueInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAInsertValueInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsALandingPadInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsALandingPadInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAPHINode(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAPHINode(Val.value()));
    }

    public static LLVMValueRef LLVMIsASelectInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsASelectInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAShuffleVectorInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAShuffleVectorInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAStoreInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAStoreInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsATerminatorInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsATerminatorInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsABranchInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsABranchInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAIndirectBrInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAIndirectBrInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAInvokeInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAInvokeInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAReturnInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAReturnInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsASwitchInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsASwitchInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAUnreachableInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAUnreachableInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAResumeInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAResumeInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACleanupReturnInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACleanupReturnInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACatchReturnInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACatchReturnInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFuncletPadInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFuncletPadInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACatchPadInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACatchPadInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACleanupPadInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACleanupPadInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAUnaryInstruction(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAUnaryInstruction(Val.value()));
    }

    public static LLVMValueRef LLVMIsAAllocaInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAAllocaInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsACastInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsACastInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAAddrSpaceCastInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAAddrSpaceCastInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsABitCastInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsABitCastInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFPExtInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFPExtInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFPToSIInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFPToSIInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFPToUIInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFPToUIInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAFPTruncInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAFPTruncInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAIntToPtrInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAIntToPtrInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAPtrToIntInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAPtrToIntInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsASExtInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsASExtInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsASIToFPInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsASIToFPInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsATruncInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsATruncInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAUIToFPInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAUIToFPInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAZExtInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAZExtInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAExtractValueInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAExtractValueInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsALoadInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsALoadInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAVAArgInst(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAVAArgInst(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMDNode(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMDNode(Val.value()));
    }

    public static LLVMValueRef LLVMIsAMDString(LLVMValueRef Val) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMIsAMDString(Val.value()));
    }

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

    /**
     * Obtain the first use of a value.
     * <p>
     * Uses are obtained in an iterator fashion. First, call this function
     * to obtain a reference to the first use. Then, call LLVMGetNextUse()
     * on that instance and all subsequently obtained instances until
     * LLVMGetNextUse() returns NULL.
     */
    public static LLVMUseRef LLVMGetFirstUse(LLVMValueRef Val) {
        return LLVMUseRef.of(Native.INSTANCE.LLVMGetFirstUse(Val.value()));
    }

    /**
     * Obtain the next use of a value.
     * <p>
     * This effectively advances the iterator. It returns NULL if you are on
     * the final use and no more are available.
     */
    public static LLVMUseRef LLVMGetNextUse(LLVMUseRef U) {
        return LLVMUseRef.of(Native.INSTANCE.LLVMGetNextUse(U.value()));
    }

    /**
     * Obtain the user value for a user.
     * <p>
     * The returned value corresponds to a llvm::User type.
     */
    public static LLVMValueRef LLVMGetUser(LLVMUseRef U) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetUser(U.value()));
    }

    /**
     * Obtain the value this use corresponds to.
     */
    public static LLVMValueRef LLVMGetUsedValue(LLVMUseRef U) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetUsedValue(U.value()));
    }

    /*
     * @defgroup LLVMCCoreValueUser User value
     *
     * Function in this group pertain to LLVMValueRef instances that descent
     * from llvm::User. This includes constants, instructions, and
     * operators.
     */

    /**
     * Obtain an operand at a specific index in a llvm::User value.
     */
    public static LLVMValueRef LLVMGetOperand(LLVMValueRef Val, int /* unsigned */ Index) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetOperand(Val.value(), Index));
    }

    /**
     * Obtain the use of an operand at a specific index in a llvm::User value.
     */
    public static LLVMUseRef LLVMGetOperandUse(LLVMValueRef Val, int /* unsigned */ Index) {
        return LLVMUseRef.of(Native.INSTANCE.LLVMGetOperandUse(Val.value(), Index));
    }

    /**
     * Set an operand at a specific index in a llvm::User value.
     */
    public static void LLVMSetOperand(LLVMValueRef User, int /* unsigned */ Index, LLVMValueRef Val) {
        Native.INSTANCE.LLVMSetOperand(User.value(), Index, Val.value());
    }

    /**
     * Obtain the number of operands in a llvm::User value.
     */
    public static int LLVMGetNumOperands(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMGetNumOperands(Val.value());
    }

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
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNull(Ty.value()));
    }

    /**
     * Obtain a constant value referring to the instance of a type
     * consisting of all ones.
     * <p>
     * This is only valid for integer types.
     */
    public static LLVMValueRef LLVMConstAllOnes(LLVMTypeRef Ty) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstAllOnes(Ty.value()));
    }

    /**
     * Obtain a constant value referring to an undefined value of a type.
     */
    public static LLVMValueRef LLVMGetUndef(LLVMTypeRef Ty) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetUndef(Ty.value()));
    }

    /**
     * Determine whether a value instance is null.
     */
    public static boolean LLVMIsNull(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMIsNull(Val.value());
    }

    /**
     * Obtain a constant that is a constant pointer pointing to NULL for a
     * specified type.
     */
    public static LLVMValueRef LLVMConstPointerNull(LLVMTypeRef Ty) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstPointerNull(Ty.value()));
    }

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
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstInt(IntTy.value(), N, SignExtend));
    }

    /**
     * Obtain a constant value for an integer of arbitrary precision.
     */
    public static LLVMValueRef LLVMConstIntOfArbitraryPrecision(LLVMTypeRef IntTy, long... /* uint64_t */ Words) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Words = allocArray(arena, Words);
            int /* unsigned */ NumWords = arrayLength(Words);
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstIntOfArbitraryPrecision(
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
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstStringInContext(
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
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstString(
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

    /*
     * @defgroup LLVMCCoreValueConstantExpressions Constant Expressions
     *
     * Functions in this group correspond to APIs on llvm::ConstantExpr.
     */

    public static LLVMOpcode LLVMGetConstOpcode(LLVMValueRef ConstantVal) {
        return LLVMOpcode.of(Native.INSTANCE.LLVMGetConstOpcode(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMAlignOf(LLVMTypeRef Ty) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMAlignOf(Ty.value()));
    }

    public static LLVMValueRef LLVMSizeOf(LLVMTypeRef Ty) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMSizeOf(Ty.value()));
    }

    public static LLVMValueRef LLVMConstNeg(LLVMValueRef ConstantVal) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNeg(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMConstNSWNeg(LLVMValueRef ConstantVal) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNSWNeg(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMConstNUWNeg(LLVMValueRef ConstantVal) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNUWNeg(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMConstFNeg(LLVMValueRef ConstantVal) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFNeg(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMConstNot(LLVMValueRef ConstantVal) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNot(ConstantVal.value()));
    }

    public static LLVMValueRef LLVMConstAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstAdd(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNSWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNSWAdd(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNUWAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNUWAdd(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFAdd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFAdd(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSub(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNSWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNSWSub(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNUWSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNUWSub(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFSub(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFSub(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstMul(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNSWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNSWMul(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstNUWMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstNUWMul(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFMul(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFMul(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstUDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstUDiv(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSDiv(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstExactSDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstExactSDiv(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFDiv(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFDiv(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstURem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstURem(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstSRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSRem(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFRem(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFRem(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstAnd(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstAnd(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstOr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstOr(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstXor(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstXor(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstICmp(LLVMIntPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstICmp(Predicate.value(), LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstFCmp(LLVMRealPredicate Predicate, LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFCmp(Predicate.value(), LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstShl(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstShl(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstLShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstLShr(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstAShr(LLVMValueRef LHSConstant, LLVMValueRef RHSConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstAShr(LHSConstant.value(), RHSConstant.value()));
    }

    public static LLVMValueRef LLVMConstGEP(LLVMValueRef ConstantVal, LLVMValueRef[] ConstantIndices) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ConstantIndices = allocArray(arena, ConstantIndices);
            int /* unsigned */ NumIndices = arrayLength(ConstantIndices);
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstGEP(ConstantVal.value(), c_ConstantIndices.nativeAddress(), NumIndices));
        }
    }

    public static LLVMValueRef LLVMConstInBoundsGEP(LLVMValueRef ConstantVal, LLVMValueRef[] ConstantIndices) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ConstantIndices = allocArray(arena, ConstantIndices);
            int /* unsigned */ NumIndices = arrayLength(ConstantIndices);
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstInBoundsGEP(ConstantVal.value(), c_ConstantIndices.nativeAddress(), NumIndices));
        }
    }

    public static LLVMValueRef LLVMConstTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstTrunc(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstSExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSExt(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstZExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstZExt(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstFPTrunc(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFPTrunc(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstFPExt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFPExt(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstUIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstUIToFP(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstSIToFP(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSIToFP(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstFPToUI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFPToUI(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstFPToSI(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFPToSI(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstPtrToInt(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstPtrToInt(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstIntToPtr(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstIntToPtr(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstBitCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstAddrSpaceCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstAddrSpaceCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstZExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstZExtOrBitCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstSExtOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSExtOrBitCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstTruncOrBitCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstTruncOrBitCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstPointerCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstPointerCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstIntCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType, boolean isSigned) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstIntCast(ConstantVal.value(), ToType.value(), isSigned));
    }

    public static LLVMValueRef LLVMConstFPCast(LLVMValueRef ConstantVal, LLVMTypeRef ToType) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstFPCast(ConstantVal.value(), ToType.value()));
    }

    public static LLVMValueRef LLVMConstSelect(LLVMValueRef ConstantCondition, LLVMValueRef ConstantIfTrue, LLVMValueRef ConstantIfFalse) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstSelect(ConstantCondition.value(), ConstantIfTrue.value(), ConstantIfFalse.value()));
    }

    public static LLVMValueRef LLVMConstExtractElement(LLVMValueRef VectorConstant, LLVMValueRef IndexConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstExtractElement(VectorConstant.value(), IndexConstant.value()));
    }

    public static LLVMValueRef LLVMConstInsertElement(LLVMValueRef VectorConstant, LLVMValueRef ElementValueConstant, LLVMValueRef IndexConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstInsertElement(VectorConstant.value(), ElementValueConstant.value(), IndexConstant.value()));
    }

    public static LLVMValueRef LLVMConstShuffleVector(LLVMValueRef VectorAConstant, LLVMValueRef VectorBConstant, LLVMValueRef MaskConstant) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMConstShuffleVector(VectorAConstant.value(), VectorBConstant.value(), MaskConstant.value()));
    }

    //LLVMValueRef LLVMConstExtractValue(LLVMValueRef AggConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return Native.INSTANCE.LLVMConstExtractValue();
    //}
    //LLVMValueRef LLVMConstInsertValue(LLVMValueRef AggConstant, LLVMValueRef ElementValueConstant, int /* unsigned */ *IdxList, int /* unsigned */ NumIdx) {
    //    return Native.INSTANCE.LLVMConstInsertValue();
    //}

    public static LLVMValueRef LLVMConstInlineAsm(LLVMTypeRef Ty, String AsmString, String Constraints, boolean HasSideEffects, boolean IsAlignStack) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_AsmString = allocString(arena, AsmString);
            MemorySegment c_Constraints = allocString(arena, Constraints);
            return LLVMValueRef.of(Native.INSTANCE.LLVMConstInlineAsm(Ty.value(), c_AsmString.nativeAddress(), c_Constraints.nativeAddress(), HasSideEffects, IsAlignStack));
        }
    }

    public static LLVMValueRef LLVMBlockAddress(LLVMValueRef F, LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBlockAddress(F.value(), BB.value()));
    }

    /*
     * @defgroup LLVMCCoreValueConstantGlobals Global Values
     *
     * This group contains functions that operate on global values. Functions in
     * this group relate to functions in the llvm::GlobalValue class tree.
     */

    public static LLVMModuleRef LLVMGetGlobalParent(LLVMValueRef Global) {
        return LLVMModuleRef.of(Native.INSTANCE.LLVMGetGlobalParent(Global.value()));
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

    public static boolean LLVMHasUnnamedAddr(LLVMValueRef Global) {
        return Native.INSTANCE.LLVMHasUnnamedAddr(Global.value());
    }

    public static void LLVMSetUnnamedAddr(LLVMValueRef Global, boolean HasUnnamedAddr) {
        Native.INSTANCE.LLVMSetUnnamedAddr(Global.value(), HasUnnamedAddr);
    }

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

    public static LLVMValueRef LLVMAddGlobal(LLVMModuleRef M, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMAddGlobal(M.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMAddGlobalInAddressSpace(LLVMModuleRef M, LLVMTypeRef Ty, String Name, int /* unsigned */ AddressSpace) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMAddGlobalInAddressSpace(M.value(), Ty.value(), c_Name.nativeAddress(), AddressSpace));
        }
    }

    public static LLVMValueRef LLVMGetNamedGlobal(LLVMModuleRef M, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMGetNamedGlobal(M.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMGetFirstGlobal(LLVMModuleRef M) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetFirstGlobal(M.value()));
    }

    public static LLVMValueRef LLVMGetLastGlobal(LLVMModuleRef M) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetLastGlobal(M.value()));
    }

    public static LLVMValueRef LLVMGetNextGlobal(LLVMValueRef GlobalVar) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetNextGlobal(GlobalVar.value()));
    }

    public static LLVMValueRef LLVMGetPreviousGlobal(LLVMValueRef GlobalVar) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetPreviousGlobal(GlobalVar.value()));
    }

    public static void LLVMDeleteGlobal(LLVMValueRef GlobalVar) {
        Native.INSTANCE.LLVMDeleteGlobal(GlobalVar.value());
    }

    public static LLVMValueRef LLVMGetInitializer(LLVMValueRef GlobalVar) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetInitializer(GlobalVar.value()));
    }

    public static void LLVMSetInitializer(LLVMValueRef GlobalVar, LLVMValueRef ConstantVal) {
        Native.INSTANCE.LLVMSetInitializer(GlobalVar.value(), ConstantVal.value());
    }

    public static boolean LLVMIsThreadLocal(LLVMValueRef GlobalVar) {
        return Native.INSTANCE.LLVMIsThreadLocal(GlobalVar.value());
    }

    public static void LLVMSetThreadLocal(LLVMValueRef GlobalVar, boolean IsThreadLocal) {
        Native.INSTANCE.LLVMSetThreadLocal(GlobalVar.value(), IsThreadLocal);
    }

    public static boolean LLVMIsGlobalConstant(LLVMValueRef GlobalVar) {
        return Native.INSTANCE.LLVMIsGlobalConstant(GlobalVar.value());
    }

    public static void LLVMSetGlobalConstant(LLVMValueRef GlobalVar, boolean IsConstant) {
        Native.INSTANCE.LLVMSetGlobalConstant(GlobalVar.value(), IsConstant);
    }

    public static LLVMThreadLocalMode LLVMGetThreadLocalMode(LLVMValueRef GlobalVar) {
        return LLVMThreadLocalMode.of(Native.INSTANCE.LLVMGetThreadLocalMode(GlobalVar.value()));
    }

    public static void LLVMSetThreadLocalMode(LLVMValueRef GlobalVar, LLVMThreadLocalMode Mode) {
        Native.INSTANCE.LLVMSetThreadLocalMode(GlobalVar.value(), Mode.value());
    }

    public static boolean LLVMIsExternallyInitialized(LLVMValueRef GlobalVar) {
        return Native.INSTANCE.LLVMIsExternallyInitialized(GlobalVar.value());
    }

    public static void LLVMSetExternallyInitialized(LLVMValueRef GlobalVar, boolean IsExtInit) {
        Native.INSTANCE.LLVMSetExternallyInitialized(GlobalVar.value(), IsExtInit);
    }

    /*
     * @defgroup LLVMCoreValueConstantGlobalAlias Global Aliases
     *
     * This group contains function that operate on global alias values.
     */

    public static LLVMValueRef LLVMAddAlias(LLVMModuleRef M, LLVMTypeRef Ty, LLVMValueRef Aliasee, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMAddAlias(M.value(), Ty.value(), Aliasee.value(), c_Name.nativeAddress()));
        }
    }

    /*
     * @defgroup LLVMCCoreValueFunction Function values
     *
     * Functions in this group operate on LLVMValueRef instances that
     * correspond to llvm::Function instances.
     */

    /**
     * Remove a function from its containing module and deletes it.
     */
    public static void LLVMDeleteFunction(LLVMValueRef Fn) {
        Native.INSTANCE.LLVMDeleteFunction(Fn.value());
    }

    /**
     * Check whether the given function has a personality function.
     */
    public static boolean LLVMHasPersonalityFn(LLVMValueRef Fn) {
        return Native.INSTANCE.LLVMHasPersonalityFn(Fn.value());
    }

    /**
     * Obtain the personality function attached to the function.
     */
    public static LLVMValueRef LLVMGetPersonalityFn(LLVMValueRef Fn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetPersonalityFn(Fn.value()));
    }

    /**
     * Set the personality function attached to the function.
     */
    public static void LLVMSetPersonalityFn(LLVMValueRef Fn, LLVMValueRef PersonalityFn) {
        Native.INSTANCE.LLVMSetPersonalityFn(Fn.value(), PersonalityFn.value());
    }

    /**
     * Obtain the ID number from a function instance.
     */
    public static int /* unsigned */ LLVMGetIntrinsicID(LLVMValueRef Fn) {
        return Native.INSTANCE.LLVMGetIntrinsicID(Fn.value());
    }

    /**
     * Obtain the calling function of a function.
     * <p>
     * The returned value corresponds to the LLVMCallConv enumeration.
     */
    public static LLVMCallConv LLVMGetFunctionCallConv(LLVMValueRef Fn) {
        return LLVMCallConv.of(Native.INSTANCE.LLVMGetFunctionCallConv(Fn.value()));
    }

    /**
     * Set the calling convention of a function.
     *
     * @param Fn Function to operate on
     * @param CC LLVMCallConv to set calling convention to
     */
    public static void LLVMSetFunctionCallConv(LLVMValueRef Fn, LLVMCallConv CC) {
        Native.INSTANCE.LLVMSetFunctionCallConv(Fn.value(), CC.value());
    }

    /**
     * Obtain the name of the garbage collector to use during code
     * generation.
     */
    public static String LLVMGetGC(LLVMValueRef Fn) {
        return addressToString(Native.INSTANCE.LLVMGetGC(Fn.value()));
    }

    /**
     * Define the garbage collector to use during code generation.
     */
    public static void LLVMSetGC(LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            Native.INSTANCE.LLVMSetGC(Fn.value(), c_Name.nativeAddress());
        }
    }

    public static void LLVMAddAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, LLVMAttributeRef A) {
        Native.INSTANCE.LLVMAddAttributeAtIndex(F.value(), Idx, A.value());
    }

    public static LLVMAttributeRef LLVMGetEnumAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, int /* unsigned */ KindID) {
        return LLVMAttributeRef.of(Native.INSTANCE.LLVMGetEnumAttributeAtIndex(F.value(), Idx, KindID));
    }

    public static LLVMAttributeRef LLVMGetStringAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, String K) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_K = allocString(arena, K);
            int /* unsigned */ KLen = Math.toIntExact(stringLength(c_K));
            return LLVMAttributeRef.of(Native.INSTANCE.LLVMGetStringAttributeAtIndex(F.value(), Idx, c_K.nativeAddress(), KLen));
        }
    }

    public static void LLVMRemoveEnumAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, int /* unsigned */ KindID) {
        Native.INSTANCE.LLVMRemoveEnumAttributeAtIndex(F.value(), Idx, KindID);
    }

    public static void LLVMRemoveStringAttributeAtIndex(LLVMValueRef F, int /* LLVMAttributeIndex */ Idx, String K) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_K = allocString(arena, K);
            int /* unsigned */ KLen = Math.toIntExact(stringLength(c_K));
            Native.INSTANCE.LLVMRemoveStringAttributeAtIndex(F.value(), Idx, c_K.nativeAddress(), KLen);
        }
    }

    /**
     * Add a target-dependent attribute to a function
     */
    public static void LLVMAddTargetDependentFunctionAttr(LLVMValueRef Fn, String A, String V) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_A = allocString(arena, A);
            MemorySegment c_V = allocString(arena, V);
            Native.INSTANCE.LLVMAddTargetDependentFunctionAttr(Fn.value(), c_A.nativeAddress(), c_V.nativeAddress());
        }
    }

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
        return Native.INSTANCE.LLVMCountParams(Fn.value());
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
    static void nLLVMGetParams(LLVMValueRef Fn, long Params) {
        Native.INSTANCE.LLVMGetParams(Fn.value(), Params);
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
            nLLVMGetParams(Fn, c_Params.nativeAddress());
            return readPointerArray(c_Params, LLVMValueRef.class, LLVMValueRef::of);
        }
    }

    /**
     * Obtain the parameter at the specified index.
     * <p>
     * Parameters are indexed from 0.
     */
    public static LLVMValueRef LLVMGetParam(LLVMValueRef Fn, int /* unsigned */ Index) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetParam(Fn.value(), Index));
    }

    /**
     * Obtain the function to which this argument belongs.
     * <p>
     * Unlike other functions in this group, this one takes an LLVMValueRef
     * that corresponds to a llvm::Attribute.
     * <p>
     * The returned LLVMValueRef is the llvm::Function to which this
     * argument belongs.
     */
    public static LLVMValueRef LLVMGetParamParent(LLVMValueRef Inst) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetParamParent(Inst.value()));
    }

    /**
     * Obtain the first parameter to a function.
     */
    public static LLVMValueRef LLVMGetFirstParam(LLVMValueRef Fn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetFirstParam(Fn.value()));
    }

    /**
     * Obtain the last parameter to a function.
     */
    public static LLVMValueRef LLVMGetLastParam(LLVMValueRef Fn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetLastParam(Fn.value()));
    }

    /**
     * Obtain the next parameter to a function.
     * <p>
     * This takes an LLVMValueRef obtained from LLVMGetFirstParam() (which is
     * actually a wrapped iterator) and obtains the next parameter from the
     * underlying iterator.
     */
    public static LLVMValueRef LLVMGetNextParam(LLVMValueRef Arg) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetNextParam(Arg.value()));
    }

    /**
     * Obtain the previous parameter to a function.
     * <p>
     * This is the opposite of LLVMGetNextParam().
     */
    public static LLVMValueRef LLVMGetPreviousParam(LLVMValueRef Arg) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetPreviousParam(Arg.value()));
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

    /*
     * @defgroup LLVMCCoreValueBasicBlock Basic Block
     *
     * A basic block represents a single entry single exit section of code.
     * Basic blocks contain a list of instructions which form the body of
     * the block.
     *
     * Basic blocks belong to functions. They have the type of label.
     *
     * Basic blocks are themselves values. However, the C API models them as
     * LLVMBasicBlockRef.
     */

    /**
     * Convert a basic block instance to a value type.
     */
    public static LLVMValueRef LLVMBasicBlockAsValue(LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBasicBlockAsValue(BB.value()));
    }

    /**
     * Determine whether an LLVMValueRef is itself a basic block.
     */
    public static boolean LLVMValueIsBasicBlock(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMValueIsBasicBlock(Val.value());
    }

    /**
     * Convert an LLVMValueRef to an LLVMBasicBlockRef instance.
     */
    public static LLVMBasicBlockRef LLVMValueAsBasicBlock(LLVMValueRef Val) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMValueAsBasicBlock(Val.value()));
    }

    /**
     * Obtain the string name of a basic block.
     */
    public static String LLVMGetBasicBlockName(LLVMBasicBlockRef BB) {
        return addressToString(Native.INSTANCE.LLVMGetBasicBlockName(BB.value()));
    }

    /**
     * Obtain the function to which a basic block belongs.
     */
    public static LLVMValueRef LLVMGetBasicBlockParent(LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetBasicBlockParent(BB.value()));
    }

    /**
     * Obtain the terminator instruction for a basic block.
     * <p>
     * If the basic block does not have a terminator (it is not well-formed
     * if it doesn't), then NULL is returned.
     * <p>
     * The returned LLVMValueRef corresponds to a llvm::TerminatorInst.
     */
    public static LLVMValueRef LLVMGetBasicBlockTerminator(LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetBasicBlockTerminator(BB.value()));
    }

    /**
     * Obtain the number of basic blocks in a function.
     *
     * @param Fn Function value to operate on.
     */
    public static int /* unsigned */ LLVMCountBasicBlocks(LLVMValueRef Fn) {
        return Native.INSTANCE.LLVMCountBasicBlocks(Fn.value());
    }

    /**
     * Obtain all of the basic blocks in a function.
     * <p>
     * This operates on a function value. The BasicBlocks parameter is a
     * pointer to a pre-allocated array of LLVMBasicBlockRef of at least
     * LLVMCountBasicBlocks() in length. This array is populated with
     * LLVMBasicBlockRef instances.
     */
    /* package-private */
    static void nLLVMGetBasicBlocks(LLVMValueRef Fn, long BasicBlocks) {
        Native.INSTANCE.LLVMGetBasicBlocks(Fn.value(), BasicBlocks);
    }

    /**
     * Obtain all of the basic blocks in a function.
     * <p>
     * Return array is populated with LLVMBasicBlockRef instances.
     */
    // Port-added
    public static LLVMBasicBlockRef[] LLVMGetBasicBlocks(LLVMValueRef Fn) {
        int /* unsigned */ count = LLVMCountBasicBlocks(Fn);
        if (count == 0) {
            return new LLVMBasicBlockRef[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BasicBlocks = allocPointerArray(arena, count);
            nLLVMGetBasicBlocks(Fn, c_BasicBlocks.nativeAddress());
            return readPointerArray(c_BasicBlocks, LLVMBasicBlockRef.class, LLVMBasicBlockRef::of);
        }
    }

    /**
     * Obtain the first basic block in a function.
     * <p>
     * The returned basic block can be used as an iterator. You will likely
     * eventually call into LLVMGetNextBasicBlock() with it.
     */
    public static LLVMBasicBlockRef LLVMGetFirstBasicBlock(LLVMValueRef Fn) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetFirstBasicBlock(Fn.value()));
    }

    /**
     * Obtain the last basic block in a function.
     */
    public static LLVMBasicBlockRef LLVMGetLastBasicBlock(LLVMValueRef Fn) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetLastBasicBlock(Fn.value()));
    }

    /**
     * Advance a basic block iterator.
     */
    public static LLVMBasicBlockRef LLVMGetNextBasicBlock(LLVMBasicBlockRef BB) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetNextBasicBlock(BB.value()));
    }

    /**
     * Go backwards in a basic block iterator.
     */
    public static LLVMBasicBlockRef LLVMGetPreviousBasicBlock(LLVMBasicBlockRef BB) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetPreviousBasicBlock(BB.value()));
    }

    /**
     * Obtain the basic block that corresponds to the entry point of a function.
     */
    public static LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef Fn) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetEntryBasicBlock(Fn.value()));
    }

    /**
     * Append a basic block to the end of a function.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlockInContext(LLVMContextRef C, LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMAppendBasicBlockInContext(
                    C.value(), Fn.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Append a basic block to the end of a function using the global context.
     */
    public static LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef Fn, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMAppendBasicBlock(
                    Fn.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Insert a basic block in a function before another basic block.
     * <p>
     * The function to add to is determined by the function of the
     * passed basic block.
     */
    public static LLVMBasicBlockRef LLVMInsertBasicBlockInContext(LLVMContextRef C, LLVMBasicBlockRef BB, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMInsertBasicBlockInContext(C.value(), BB.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Insert a basic block in a function using the global context.
     */
    public static LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef InsertBeforeBB, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMInsertBasicBlock(InsertBeforeBB.value(), c_Name.nativeAddress()));
        }
    }

    /**
     * Remove a basic block from a function and delete it.
     * <p>
     * This deletes the basic block from its containing function and deletes
     * the basic block itself.
     */
    public static void LLVMDeleteBasicBlock(LLVMBasicBlockRef BB) {
        Native.INSTANCE.LLVMDeleteBasicBlock(BB.value());
    }

    /**
     * Remove a basic block from a function.
     * <p>
     * This deletes the basic block from its containing function but keep
     * the basic block alive.
     */
    public static void LLVMRemoveBasicBlockFromParent(LLVMBasicBlockRef BB) {
        Native.INSTANCE.LLVMRemoveBasicBlockFromParent(BB.value());
    }

    /**
     * Move a basic block to before another one.
     */
    public static void LLVMMoveBasicBlockBefore(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
        Native.INSTANCE.LLVMMoveBasicBlockBefore(BB.value(), MovePos.value());
    }

    /**
     * Move a basic block to after another one.
     */
    public static void LLVMMoveBasicBlockAfter(LLVMBasicBlockRef BB, LLVMBasicBlockRef MovePos) {
        Native.INSTANCE.LLVMMoveBasicBlockAfter(BB.value(), MovePos.value());
    }

    /**
     * Obtain the first instruction in a basic block.
     * <p>
     * The returned LLVMValueRef corresponds to a llvm::Instruction
     * instance.
     */
    public static LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetFirstInstruction(BB.value()));
    }

    /**
     * Obtain the last instruction in a basic block.
     * <p>
     * The returned LLVMValueRef corresponds to an LLVM:Instruction.
     */
    public static LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef BB) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetLastInstruction(BB.value()));
    }

    /*
     * @defgroup LLVMCCoreValueInstruction Instructions
     *
     * Functions in this group relate to the inspection and manipulation of
     * individual instructions.
     *
     * In the C++ API, an instruction is modeled by llvm::Instruction. This
     * class has a large number of descendents. llvm::Instruction is a
     * llvm::Value and in the C API, instructions are modeled by
     * LLVMValueRef.
     *
     * This group also contains sub-groups which operate on specific
     * llvm::Instruction types, e.g. llvm::CallInst.
     */

    /**
     * Determine whether an instruction has any metadata attached.
     */
    public static boolean LLVMHasMetadata(LLVMValueRef Val) {
        return Native.INSTANCE.LLVMHasMetadata(Val.value());
    }

    /**
     * Return metadata associated with an instruction value.
     */
    public static LLVMValueRef LLVMGetMetadata(LLVMValueRef Val, int /* unsigned */ KindID) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetMetadata(Val.value(), KindID));
    }

    /**
     * Set metadata associated with an instruction value.
     */
    public static void LLVMSetMetadata(LLVMValueRef Val, int /* unsigned */ KindID, LLVMValueRef Node) {
        Native.INSTANCE.LLVMSetMetadata(Val.value(), KindID, Node.value());
    }

    /**
     * Obtain the basic block to which an instruction belongs.
     */
    public static LLVMBasicBlockRef LLVMGetInstructionParent(LLVMValueRef Inst) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetInstructionParent(Inst.value()));
    }

    /**
     * Obtain the instruction that occurs after the one specified.
     * <p>
     * The next instruction will be from the same basic block.
     * <p>
     * If this is the last instruction in a basic block, NULL will be
     * returned.
     */
    public static LLVMValueRef LLVMGetNextInstruction(LLVMValueRef Inst) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetNextInstruction(Inst.value()));
    }

    /**
     * Obtain the instruction that occurred before this one.
     * <p>
     * If the instruction is the first instruction in a basic block, NULL
     * will be returned.
     */
    public static LLVMValueRef LLVMGetPreviousInstruction(LLVMValueRef Inst) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetPreviousInstruction(Inst.value()));
    }

    /**
     * Remove and delete an instruction.
     * <p>
     * The instruction specified is removed from its containing building
     * block but is kept alive.
     */
    public static void LLVMInstructionRemoveFromParent(LLVMValueRef Inst) {
        Native.INSTANCE.LLVMInstructionRemoveFromParent(Inst.value());
    }

    /**
     * Remove and delete an instruction.
     * <p>
     * The instruction specified is removed from its containing building
     * block and then deleted.
     */
    public static void LLVMInstructionEraseFromParent(LLVMValueRef Inst) {
        Native.INSTANCE.LLVMInstructionEraseFromParent(Inst.value());
    }

    /**
     * Obtain the code opcode for an individual instruction.
     */
    public static LLVMOpcode LLVMGetInstructionOpcode(LLVMValueRef Inst) {
        return LLVMOpcode.of(Native.INSTANCE.LLVMGetInstructionOpcode(Inst.value()));
    }

    /**
     * Obtain the predicate of an instruction.
     * <p>
     * This is only valid for instructions that correspond to llvm::ICmpInst
     * or llvm::ConstantExpr whose opcode is llvm::Instruction::ICmp.
     */
    public static LLVMIntPredicate LLVMGetICmpPredicate(LLVMValueRef Inst) {
        return LLVMIntPredicate.of(Native.INSTANCE.LLVMGetICmpPredicate(Inst.value()));
    }

    /**
     * Obtain the float predicate of an instruction.
     * <p>
     * This is only valid for instructions that correspond to llvm::FCmpInst
     * or llvm::ConstantExpr whose opcode is llvm::Instruction::FCmp.
     */
    public static LLVMRealPredicate LLVMGetFCmpPredicate(LLVMValueRef Inst) {
        return LLVMRealPredicate.of(Native.INSTANCE.LLVMGetFCmpPredicate(Inst.value()));
    }

    /**
     * Create a copy of 'this' instruction that is identical in all ways
     * except the following:
     * * The instruction has no parent
     * * The instruction has no name
     */
    public static LLVMValueRef LLVMInstructionClone(LLVMValueRef Inst) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMInstructionClone(Inst.value()));
    }

    /*
     * @defgroup LLVMCCoreValueInstructionCall Call Sites and Invocations
     *
     * Functions in this group apply to instructions that refer to call
     * sites and invocations. These correspond to C++ types in the
     * llvm::CallInst class tree.
     */

    /**
     * Obtain the argument count for a call instruction.
     * <p>
     * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
     * llvm::InvokeInst.
     */
    public static int /* unsigned */ LLVMGetNumArgOperands(LLVMValueRef Instr) {
        return Native.INSTANCE.LLVMGetNumArgOperands(Instr.value());
    }

    /**
     * Set the calling convention for a call instruction.
     * <p>
     * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
     * llvm::InvokeInst.
     */
    public static void LLVMSetInstructionCallConv(LLVMValueRef Instr, LLVMCallConv CC) {
        Native.INSTANCE.LLVMSetInstructionCallConv(Instr.value(), CC.value());
    }

    /**
     * Obtain the calling convention for a call instruction.
     * <p>
     * This is the opposite of LLVMSetInstructionCallConv(). Reads its
     * usage.
     */
    public static LLVMCallConv LLVMGetInstructionCallConv(LLVMValueRef Instr) {
        return LLVMCallConv.of(Native.INSTANCE.LLVMGetInstructionCallConv(Instr.value()));
    }

    public static void LLVMSetInstrParamAlignment(LLVMValueRef Instr, int /* LLVMAttributeIndex */ index, int /* unsigned */ Align) {
        Native.INSTANCE.LLVMSetInstrParamAlignment(Instr.value(), index, Align);
    }

    public static void LLVMAddCallSiteAttribute(LLVMValueRef C, int /* LLVMAttributeIndex */ Idx, LLVMAttributeRef A) {
        Native.INSTANCE.LLVMAddCallSiteAttribute(C.value(), Idx, A.value());
    }

    public static LLVMAttributeRef LLVMGetCallSiteEnumAttribute(LLVMValueRef C, int /* LLVMAttributeIndex */ Idx, int /* unsigned */ KindID) {
        return LLVMAttributeRef.of(Native.INSTANCE.LLVMGetCallSiteEnumAttribute(C.value(), Idx, KindID));
    }

    public static LLVMAttributeRef LLVMGetCallSiteStringAttribute(LLVMValueRef C, int /* LLVMAttributeIndex */ Idx, String K) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_K = allocString(arena, K);
            int /* unsigned */ KLen = Math.toIntExact(stringLength(c_K));
            return LLVMAttributeRef.of(Native.INSTANCE.LLVMGetCallSiteStringAttribute(C.value(), Idx, c_K.nativeAddress(), KLen));
        }
    }

    public static void LLVMRemoveCallSiteEnumAttribute(LLVMValueRef C, int /* LLVMAttributeIndex */ Idx, int /* unsigned */ KindID) {
        Native.INSTANCE.LLVMRemoveCallSiteEnumAttribute(C.value(), Idx, KindID);
    }

    public static void LLVMRemoveCallSiteStringAttribute(LLVMValueRef C, int /* LLVMAttributeIndex */ Idx, String K) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_K = allocString(arena, K);
            int /* unsigned */ KLen = Math.toIntExact(stringLength(c_K));
            Native.INSTANCE.LLVMRemoveCallSiteStringAttribute(C.value(), Idx, c_K.nativeAddress(), KLen);
        }
    }

    /**
     * Obtain the pointer to the function invoked by this instruction.
     * <p>
     * This expects an LLVMValueRef that corresponds to a llvm::CallInst or
     * llvm::InvokeInst.
     */
    public static LLVMValueRef LLVMGetCalledValue(LLVMValueRef Instr) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetCalledValue(Instr.value()));
    }

    /**
     * Obtain whether a call instruction is a tail call.
     * <p>
     * This only works on llvm::CallInst instructions.
     */
    public static boolean LLVMIsTailCall(LLVMValueRef CallInst) {
        return Native.INSTANCE.LLVMIsTailCall(CallInst.value());
    }

    /**
     * Set whether a call instruction is a tail call.
     * <p>
     * This only works on llvm::CallInst instructions.
     */
    public static void LLVMSetTailCall(LLVMValueRef CallInst, boolean IsTailCall) {
        Native.INSTANCE.LLVMSetTailCall(CallInst.value(), IsTailCall);
    }

    /**
     * Return the normal destination basic block.
     * <p>
     * This only works on llvm::InvokeInst instructions.
     */
    public static LLVMBasicBlockRef LLVMGetNormalDest(LLVMValueRef InvokeInst) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetNormalDest(InvokeInst.value()));
    }

    /**
     * Return the unwind destination basic block.
     * <p>
     * This only works on llvm::InvokeInst instructions.
     */
    public static LLVMBasicBlockRef LLVMGetUnwindDest(LLVMValueRef InvokeInst) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetUnwindDest(InvokeInst.value()));
    }

    /**
     * Set the normal destination basic block.
     * <p>
     * This only works on llvm::InvokeInst instructions.
     */
    public static void LLVMSetNormalDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
        Native.INSTANCE.LLVMSetNormalDest(InvokeInst.value(), B.value());
    }

    /**
     * Set the unwind destination basic block.
     * <p>
     * This only works on llvm::InvokeInst instructions.
     */
    public static void LLVMSetUnwindDest(LLVMValueRef InvokeInst, LLVMBasicBlockRef B) {
        Native.INSTANCE.LLVMSetUnwindDest(InvokeInst.value(), B.value()/**/);
    }

    /*
     * @defgroup LLVMCCoreValueInstructionTerminator Terminators
     *
     * Functions in this group only apply to instructions that map to
     * llvm::TerminatorInst instances.
     */

    /**
     * Return the number of successors that this terminator has.
     */
    public static int /* unsigned */ LLVMGetNumSuccessors(LLVMValueRef Term) {
        return Native.INSTANCE.LLVMGetNumSuccessors(Term.value());
    }

    /**
     * Return the specified successor.
     */
    public static LLVMBasicBlockRef LLVMGetSuccessor(LLVMValueRef Term, int /* unsigned */ Idx) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetSuccessor(Term.value(), Idx));
    }

    /**
     * Update the specified successor to point at the provided block.
     */
    public static void LLVMSetSuccessor(LLVMValueRef Term, int /* unsigned */ Idx, LLVMBasicBlockRef Block) {
        Native.INSTANCE.LLVMSetSuccessor(Term.value(), Idx, Block.value());
    }

    /**
     * Return if a branch is conditional.
     * <p>
     * This only works on llvm::BranchInst instructions.
     */
    public static boolean LLVMIsConditional(LLVMValueRef Branch) {
        return Native.INSTANCE.LLVMIsConditional(Branch.value());
    }

    /**
     * Return the condition of a branch instruction.
     * <p>
     * This only works on llvm::BranchInst instructions.
     */
    public static LLVMValueRef LLVMGetCondition(LLVMValueRef Branch) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetCondition(Branch.value()));
    }

    /**
     * Set the condition of a branch instruction.
     * <p>
     * This only works on llvm::BranchInst instructions.
     */
    public static void LLVMSetCondition(LLVMValueRef Branch, LLVMValueRef Cond) {
        Native.INSTANCE.LLVMSetCondition(Branch.value(), Cond.value());
    }

    /**
     * Obtain the default destination basic block of a switch instruction.
     * <p>
     * This only works on llvm::SwitchInst instructions.
     */
    public static LLVMBasicBlockRef LLVMGetSwitchDefaultDest(LLVMValueRef SwitchInstr) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetSwitchDefaultDest(SwitchInstr.value()));
    }

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
        return LLVMTypeRef.of(Native.INSTANCE.LLVMGetAllocatedType(Alloca.value()));
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
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetIncomingValue(PhiNode.value(), Index));
    }

    /**
     * Obtain an incoming value to a PHI node as an LLVMBasicBlockRef.
     */
    public static LLVMBasicBlockRef LLVMGetIncomingBlock(LLVMValueRef PhiNode, int /* unsigned */ Index) {
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetIncomingBlock(PhiNode.value(), Index));
    }

    /*
     * @defgroup LLVMCCoreValueInstructionExtractValue ExtractValue
     * @defgroup LLVMCCoreValueInstructionInsertValue InsertValue
     *
     * Functions in this group only apply to instructions that map to
     * llvm::ExtractValue and llvm::InsertValue instances.
     */

    /**
     * Obtain the number of indices.
     * NB: This also works on GEP.
     */
    public static int /* unsigned */ LLVMGetNumIndices(LLVMValueRef Inst) {
        return Native.INSTANCE.LLVMGetNumIndices(Inst.value());
    }

    /**
     * Obtain the indices as an array.
     */
    public static int[] /* unsigned* */ LLVMGetIndices(LLVMValueRef Inst) {
        return _Utils.readIntArray(Native.INSTANCE.LLVMGetIndices(Inst.value()), LLVMGetNumIndices(Inst));
    }

    /*
     * @defgroup LLVMCCoreInstructionBuilder Instruction Builders
     *
     * An instruction builder represents a point within a basic block and is
     * the exclusive means of building instructions using the C interface.
     */

    public static LLVMBuilderRef LLVMCreateBuilderInContext(LLVMContextRef C) {
        return LLVMBuilderRef.of(Native.INSTANCE.LLVMCreateBuilderInContext(C.value()));
    }

    public static LLVMBuilderRef LLVMCreateBuilder() {
        return LLVMBuilderRef.of(Native.INSTANCE.LLVMCreateBuilder());
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
        return LLVMBasicBlockRef.of(Native.INSTANCE.LLVMGetInsertBlock(Builder.value()));
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
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetCurrentDebugLocation(Builder.value()));
    }

    public static void LLVMSetInstDebugLocation(LLVMBuilderRef Builder, LLVMValueRef Inst) {
        Native.INSTANCE.LLVMSetInstDebugLocation(Builder.value(), Inst.value());
    }

    /* Terminators */

    public static LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef Builder) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildRetVoid(Builder.value()));
    }

    public static LLVMValueRef LLVMBuildRet(LLVMBuilderRef Builder, LLVMValueRef V) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildRet(Builder.value(), V.value()));
    }

    public static LLVMValueRef LLVMBuildAggregateRet(LLVMBuilderRef B, LLVMValueRef[] RetVals) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_RetVals = allocArray(arena, RetVals);
            int /* unsigned */ N = arrayLength(RetVals);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAggregateRet(B.value(), c_RetVals.nativeAddress(), N));
        }
    }

    public static LLVMValueRef LLVMBuildBr(LLVMBuilderRef B, LLVMBasicBlockRef Dest) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildBr(B.value(), Dest.value()));
    }

    public static LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef B, LLVMValueRef If, LLVMBasicBlockRef Then, LLVMBasicBlockRef Else) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildCondBr(
                B.value(), If.value(), Then.value(), Else.value()));
    }

    public static LLVMValueRef LLVMBuildSwitch(LLVMBuilderRef B, LLVMValueRef V, LLVMBasicBlockRef Else, int /* unsigned */ NumCases) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSwitch(B.value(), V.value(), Else.value(), NumCases));
    }

    public static LLVMValueRef LLVMBuildIndirectBr(LLVMBuilderRef B, LLVMValueRef Addr, int /* unsigned */ NumDests) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildIndirectBr(B.value(), Addr.value(), NumDests));
    }

    public static LLVMValueRef LLVMBuildInvoke(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, LLVMBasicBlockRef Then, LLVMBasicBlockRef Catch, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildInvoke(B.value(), Fn.value(), c_Args.nativeAddress(), NumArgs, Then.value(), Catch.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildLandingPad(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef PersFn, int /* unsigned */ NumClauses, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildLandingPad(B.value(), Ty.value(), PersFn.value(), NumClauses, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildResume(LLVMBuilderRef B, LLVMValueRef Exn) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildResume(B.value(), Exn.value()));
    }

    public static LLVMValueRef LLVMBuildUnreachable(LLVMBuilderRef B) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildUnreachable(B.value()));
    }

    /**
     * Add a case to the switch instruction
     */
    public static void LLVMAddCase(LLVMValueRef Switch, LLVMValueRef OnVal, LLVMBasicBlockRef Dest) {
        Native.INSTANCE.LLVMAddCase(Switch.value(), OnVal.value(), Dest.value());
    }

    /**
     * Add a destination to the indirectbr instruction
     */
    public static void LLVMAddDestination(LLVMValueRef IndirectBr, LLVMBasicBlockRef Dest) {
        Native.INSTANCE.LLVMAddDestination(IndirectBr.value(), Dest.value());
    }

    /**
     * Get the number of clauses on the landingpad instruction
     */
    public static int /* unsigned */ LLVMGetNumClauses(LLVMValueRef LandingPad) {
        return Native.INSTANCE.LLVMGetNumClauses(LandingPad.value());
    }

    /**
     * Get the value of the clause at idnex Idx on the landingpad instruction
     */
    public static LLVMValueRef LLVMGetClause(LLVMValueRef LandingPad, int /* unsigned */ Idx) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMGetClause(LandingPad.value(), Idx));
    }

    /**
     * Add a catch or filter clause to the landingpad instruction
     */
    public static void LLVMAddClause(LLVMValueRef LandingPad, LLVMValueRef ClauseVal) {
        Native.INSTANCE.LLVMAddClause(LandingPad.value(), ClauseVal.value());
    }

    /**
     * Get the 'cleanup' flag in the landingpad instruction
     */
    public static boolean LLVMIsCleanup(LLVMValueRef LandingPad) {
        return Native.INSTANCE.LLVMIsCleanup(LandingPad.value());
    }

    /**
     * Set the 'cleanup' flag in the landingpad instruction
     */
    public static void LLVMSetCleanup(LLVMValueRef LandingPad, boolean Val) {
        Native.INSTANCE.LLVMSetCleanup(LandingPad.value(), Val);
    }

    /* Arithmetic */

    public static LLVMValueRef LLVMBuildAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNSWAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNUWAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFAdd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNSWSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNUWSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFSub(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFSub(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNSWMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNUWMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFMul(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFMul(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildUDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildExactSDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildExactSDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFDiv(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildURem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildURem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSRem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFRem(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFRem(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildShl(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildShl(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildLShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildLShr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAShr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAShr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAnd(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAnd(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildOr(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildOr(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildXor(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildXor(
                    B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildBinOp(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildBinOp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNSWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNSWNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNUWNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNUWNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFNeg(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildNot(LLVMBuilderRef B, LLVMValueRef V, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildNot(
                    B.value(), V.value(), c_Name.nativeAddress()));
        }
    }

    /* Memory */

    public static LLVMValueRef LLVMBuildMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildMalloc(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildArrayMalloc(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildArrayMalloc(
                    B.value(), Ty.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAlloca(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildArrayAlloca(LLVMBuilderRef B, LLVMTypeRef Ty, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildArrayAlloca(
                    B.value(), Ty.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFree(LLVMBuilderRef B, LLVMValueRef Ptr) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFree(B.value(), Ptr.value()));
    }

    public static LLVMValueRef LLVMBuildLoad(LLVMBuilderRef B, LLVMValueRef Ptr, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildLoad(
                    B.value(), Ptr.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildStore(LLVMBuilderRef B, LLVMValueRef Val, LLVMValueRef Ptr) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildStore(B.value(), Val.value(), Ptr.value()));
    }

    public static LLVMValueRef LLVMBuildGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Indices = allocArray(arena, Indices);
            int /* unsigned */ NumIndices = arrayLength(Indices);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildGEP(B.value(),
                    Pointer.value(), c_Indices.nativeAddress(), NumIndices, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildInBoundsGEP(LLVMBuilderRef B, LLVMValueRef Pointer, LLVMValueRef[] Indices, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Indices = allocArray(arena, Indices);
            int /* unsigned */ NumIndices = arrayLength(Indices);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildInBoundsGEP(B.value(),
                    Pointer.value(), c_Indices.nativeAddress(), NumIndices, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildStructGEP(LLVMBuilderRef B, LLVMValueRef Pointer, int /* unsigned */ Idx, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildStructGEP(
                    B.value(), Pointer.value(), Idx, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildGlobalString(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildGlobalString(
                    B.value(), c_Str.nativeAddress(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildGlobalStringPtr(LLVMBuilderRef B, String Str, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Str = allocString(arena, Str);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildGlobalStringPtr(
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
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildTrunc(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildZExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildZExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFPToUI(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFPToSI(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildUIToFP(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSIToFP(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPTrunc(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFPTrunc(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPExt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFPExt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildPtrToInt(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIntToPtr(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildIntToPtr(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAddrSpaceCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAddrSpaceCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildZExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildZExtOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSExtOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSExtOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildTruncOrBitCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildTruncOrBitCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildCast(LLVMBuilderRef B, LLVMOpcode Op, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildCast(
                    B.value(), Op.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildPointerCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildPointerCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIntCast(LLVMBuilderRef B, LLVMValueRef Val, /*Signed cast!*/ LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildIntCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef B, LLVMValueRef Val, LLVMTypeRef DestTy, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFPCast(
                    B.value(), Val.value(), DestTy.value(), c_Name.nativeAddress()));
        }
    }

    /* Comparisons */

    public static LLVMValueRef LLVMBuildICmp(LLVMBuilderRef B, LLVMIntPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildICmp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef B, LLVMRealPredicate Op, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFCmp(
                    B.value(), Op.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    /* Miscellaneous instructions */

    public static LLVMValueRef LLVMBuildPhi(LLVMBuilderRef B, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildPhi(
                    B.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildCall(LLVMBuilderRef B, LLVMValueRef Fn, LLVMValueRef[] Args, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildCall(B.value(),
                    Fn.value(), c_Args.nativeAddress(), NumArgs, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildSelect(LLVMBuilderRef B, LLVMValueRef If, LLVMValueRef Then, LLVMValueRef Else, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildSelect(B.value(),
                    If.value(), Then.value(), Else.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildVAArg(LLVMBuilderRef B, LLVMValueRef List, LLVMTypeRef Ty, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildVAArg(B.value(), List.value(), Ty.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildExtractElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildExtractElement(B.value(), VecVal.value(), Index.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildInsertElement(LLVMBuilderRef B, LLVMValueRef VecVal, LLVMValueRef EltVal, LLVMValueRef Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildInsertElement(B.value(), VecVal.value(), EltVal.value(), Index.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildShuffleVector(LLVMBuilderRef B, LLVMValueRef V1, LLVMValueRef V2, LLVMValueRef Mask, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildShuffleVector(B.value(), V1.value(), V2.value(), Mask.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef B, LLVMValueRef AggVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildExtractValue(
                    B.value(), AggVal.value(), Index, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef B, LLVMValueRef AggVal, LLVMValueRef EltVal, int /* unsigned */ Index, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildInsertValue(
                    B.value(), AggVal.value(), EltVal.value(), Index, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIsNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildIsNull(B.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildIsNotNull(LLVMBuilderRef B, LLVMValueRef Val, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildIsNotNull(B.value(), Val.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildPtrDiff(LLVMBuilderRef B, LLVMValueRef LHS, LLVMValueRef RHS, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildPtrDiff(B.value(), LHS.value(), RHS.value(), c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildFence(LLVMBuilderRef B, LLVMAtomicOrdering Ordering, boolean SingleThread, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMValueRef.of(Native.INSTANCE.LLVMBuildFence(B.value(), Ordering.value(), SingleThread, c_Name.nativeAddress()));
        }
    }

    public static LLVMValueRef LLVMBuildAtomicRMW(LLVMBuilderRef B, LLVMAtomicRMWBinOp Op, LLVMValueRef Ptr, LLVMValueRef Val, LLVMAtomicOrdering Ordering, boolean SingleThread) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAtomicRMW(B.value(), Op.value(), Ptr.value(), Val.value(), Ordering.value(), SingleThread));
    }

    public static LLVMValueRef LLVMBuildAtomicCmpXchg(LLVMBuilderRef B, LLVMValueRef Ptr, LLVMValueRef Cmp, LLVMValueRef New, LLVMAtomicOrdering SuccessOrdering, LLVMAtomicOrdering FailureOrdering, boolean SingleThread) {
        return LLVMValueRef.of(Native.INSTANCE.LLVMBuildAtomicCmpXchg(B.value(), Ptr.value(), Cmp.value(), New.value(), SuccessOrdering.value(), FailureOrdering.value(), SingleThread));
    }

    public static boolean LLVMIsAtomicSingleThread(LLVMValueRef AtomicInst) {
        return Native.INSTANCE.LLVMIsAtomicSingleThread(AtomicInst.value());
    }

    public static void LLVMSetAtomicSingleThread(LLVMValueRef AtomicInst, boolean SingleThread) {
        Native.INSTANCE.LLVMSetAtomicSingleThread(AtomicInst.value(), SingleThread);
    }

    public static LLVMAtomicOrdering LLVMGetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst) {
        return LLVMAtomicOrdering.of(Native.INSTANCE.LLVMGetCmpXchgSuccessOrdering(CmpXchgInst.value()));
    }

    public static void LLVMSetCmpXchgSuccessOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
        Native.INSTANCE.LLVMSetCmpXchgSuccessOrdering(CmpXchgInst.value(), Ordering.value());
    }

    public static LLVMAtomicOrdering LLVMGetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst) {
        return LLVMAtomicOrdering.of(Native.INSTANCE.LLVMGetCmpXchgFailureOrdering(CmpXchgInst.value()));
    }

    public static void LLVMSetCmpXchgFailureOrdering(LLVMValueRef CmpXchgInst, LLVMAtomicOrdering Ordering) {
        Native.INSTANCE.LLVMSetCmpXchgFailureOrdering(CmpXchgInst.value(), Ordering.value());
    }

    /*
     * @defgroup LLVMCCoreModuleProvider Module Providers
     */

    /**
     * Changes the type of M so it can be passed to FunctionPassManagers and the
     * JIT. They take ModuleProviders for historical reasons.
     */
    public static LLVMModuleProviderRef LLVMCreateModuleProviderForExistingModule(LLVMModuleRef M) {
        return LLVMModuleProviderRef.of(Native.INSTANCE.LLVMCreateModuleProviderForExistingModule(M.value()));
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

    /* package-private */
    static boolean nLLVMCreateMemoryBufferWithContentsOfFile(String Path, Consumer<LLVMMemoryBufferRef> OutMemBuf, Consumer<String> OutMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Path = allocString(arena, Path);
            MemorySegment c_OutMemBuf = arena.allocate(ADDRESS);
            MemorySegment c_OutMessage = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMCreateMemoryBufferWithContentsOfFile(c_Path.nativeAddress(), c_OutMemBuf.nativeAddress(), c_OutMessage.nativeAddress());
            if (!err) {
                OutMemBuf.accept(LLVMMemoryBufferRef.of(c_OutMemBuf.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutMessage.accept(addressToLLVMString(c_OutMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithContentsOfFile(String Path) throws LLVMException {
        String[] err = new String[1];
        LLVMMemoryBufferRef[] out = new LLVMMemoryBufferRef[1];
        if (nLLVMCreateMemoryBufferWithContentsOfFile(Path, O -> out[0] = O, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    /* package-private */
    static boolean nLLVMCreateMemoryBufferWithSTDIN(Consumer<LLVMMemoryBufferRef> OutMemBuf, Consumer<String> OutMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutMemBuf = arena.allocate(ADDRESS);
            MemorySegment c_OutMessage = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMCreateMemoryBufferWithSTDIN(c_OutMemBuf.nativeAddress(), c_OutMessage.nativeAddress());
            if (!err) {
                OutMemBuf.accept(LLVMMemoryBufferRef.of(c_OutMemBuf.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutMessage.accept(addressToLLVMString(c_OutMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSTDIN() throws LLVMException {
        String[] err = new String[1];
        LLVMMemoryBufferRef[] out = new LLVMMemoryBufferRef[1];
        if (nLLVMCreateMemoryBufferWithSTDIN(O -> out[0] = O, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    /* package-private */
    static LLVMMemoryBufferRef nLLVMCreateMemoryBufferWithMemoryRange(
            long InputData, long /* size_t */ InputDataLength,
            String BufferName, boolean RequiresNullTerminator) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BufferName = allocString(arena, BufferName);
            return LLVMMemoryBufferRef.of(Native.INSTANCE.LLVMCreateMemoryBufferWithMemoryRange(
                    InputData, InputDataLength, c_BufferName.nativeAddress(), RequiresNullTerminator));
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegment(
            MemorySegment InputData, String BufferName, boolean RequiresNullTerminator) {
        return nLLVMCreateMemoryBufferWithMemoryRange(InputData.nativeAddress(),
                InputData.byteSize(), BufferName, RequiresNullTerminator);
    }

    /* package-private */
    static LLVMMemoryBufferRef nLLVMCreateMemoryBufferWithMemoryRangeCopy(
            long InputData, long /* size_t */ InputDataLength, String BufferName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_BufferName = allocString(arena, BufferName);
            return LLVMMemoryBufferRef.of(Native.INSTANCE.LLVMCreateMemoryBufferWithMemoryRangeCopy(
                    InputData, InputDataLength, c_BufferName.nativeAddress()));
        }
    }

    // Port-added
    public static LLVMMemoryBufferRef LLVMCreateMemoryBufferWithSegmentCopy(
            MemorySegment InputData, String BufferName) {
        return nLLVMCreateMemoryBufferWithMemoryRangeCopy(
                InputData.nativeAddress(), InputData.byteSize(), BufferName);
    }

    /* package-private */
    static long nLLVMGetBufferStart(LLVMMemoryBufferRef MemBuf) {
        return Native.INSTANCE.LLVMGetBufferStart(MemBuf.value());
    }

    public static long /* size_t */ LLVMGetBufferSize(LLVMMemoryBufferRef MemBuf) {
        return Native.INSTANCE.LLVMGetBufferSize(MemBuf.value());
    }

    // Port-added
    public static MemorySegment LLVMGetBufferSegment(LLVMMemoryBufferRef MemBuf) {
        long address = nLLVMGetBufferStart(MemBuf);
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
        return LLVMPassRegistryRef.of(Native.INSTANCE.LLVMGetGlobalPassRegistry());
    }

    /*
     * @defgroup LLVMCCorePassManagers Pass Managers
     */

    /**
     * Constructs a new whole-module pass pipeline. This type of pipeline is
     * suitable for link-time optimization and whole-module transformations.
     */
    public static LLVMPassManagerRef LLVMCreatePassManager() {
        return LLVMPassManagerRef.of(Native.INSTANCE.LLVMCreatePassManager());
    }

    /**
     * Constructs a new function-by-function pass pipeline over the module
     * provider. It does not take ownership of the module provider. This type of
     * pipeline is suitable for code generation and JIT compilation tasks.
     */
    public static LLVMPassManagerRef LLVMCreateFunctionPassManagerForModule(LLVMModuleRef M) {
        return LLVMPassManagerRef.of(Native.INSTANCE.LLVMCreateFunctionPassManagerForModule(M.value()));
    }

    /**
     * Deprecated: Use LLVMCreateFunctionPassManagerForModule instead.
     */
    @Deprecated
    public static LLVMPassManagerRef LLVMCreateFunctionPassManager(LLVMModuleProviderRef MP) {
        return LLVMPassManagerRef.of(Native.INSTANCE.LLVMCreateFunctionPassManager(MP.value()));
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
     * on the provided function. Returns 1 if any of the passes modified the
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

    /**
     * Deprecated: Multi-threading can only be enabled/disabled with the compile
     * time define LLVM_ENABLE_THREADS.  This function always returns
     * LLVMIsMultithreaded().
     */
    @Deprecated
    public static boolean LLVMStartMultithreaded() {
        return Native.INSTANCE.LLVMStartMultithreaded();
    }

    /**
     * Deprecated: Multi-threading can only be enabled/disabled with the compile
     * time define LLVM_ENABLE_THREADS.
     */
    @Deprecated
    public static void LLVMStopMultithreaded() {
        Native.INSTANCE.LLVMStopMultithreaded();
    }

    /**
     * Check whether LLVM is executing in thread-safe mode or not.
     */
    @Deprecated
    public static boolean LLVMIsMultithreaded() {
        return Native.INSTANCE.LLVMIsMultithreaded();
    }
}

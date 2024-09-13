package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.util.function.Consumer;

/*===-- llvm-c/Analysis.h - Analysis Library C Interface --------*- C++ -*-===*\
|*                                                                            *|
|* This header declares the C interface to libLLVMAnalysis.a, which           *|
|* implements various analyses of the LLVM IR.                                *|
|*                                                                            *|
|* Many exotic languages can interoperate with C code but have a harder time  *|
|* with C++ due to name mangling. So in addition to C, this interface enables *|
|* tools written in such languages.                                           *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Analysis {
    private Analysis() {
    }

    /*
     * @defgroup LLVMCAnalysis Analysis
     * @ingroup LLVMC
     */

    public enum LLVMVerifierFailureAction {

        /**
         * verifier will print to stderr and abort()
         */
        LLVMAbortProcessAction,
        /**
         * verifier will print to stderr and return 1
         */
        LLVMPrintMessageAction,
        /**
         * verifier will just return 1
         */
        LLVMReturnStatusAction;

        int value() {
            return ordinal();
        }

        static LLVMVerifierFailureAction of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMVerifyModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract boolean LLVMVerifyModule(long M, int Action, long OutMessage);

        @LibrarySymbol(name = "LLVMVerifyFunction")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, INT})
        abstract boolean LLVMVerifyFunction(long M, int Action);

        @LibrarySymbol(name = "LLVMViewFunctionCFG")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMViewFunctionCFG(long V);

        @LibrarySymbol(name = "LLVMViewFunctionCFGOnly")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMViewFunctionCFGOnly(long V);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    /**
     * Verifies that a module is valid, taking the specified action if not.
     * Optionally returns a human-readable description of any invalid constructs.
     */
    /* package-private */
    static boolean nLLVMVerifyModule(LLVMModuleRef M, LLVMVerifierFailureAction Action, Consumer<String> OutMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutMessage = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMVerifyModule(M.value(), Action.value(), c_OutMessage.nativeAddress());
            if (err) {
                OutMessage.accept(addressToLLVMString(c_OutMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Verifies that a module is valid, taking the specified action if not.
     */
    // Port-added
    public static void LLVMVerifyModule(LLVMModuleRef M, LLVMVerifierFailureAction Action) throws LLVMException {
        String[] err = new String[1];
        if (nLLVMVerifyModule(M, Action, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
    }

    /**
     * Verifies that a module is valid.
     */
    // Port-added
    public static void LLVMVerifyModule(LLVMModuleRef M) throws LLVMException {
        LLVMVerifyModule(M, LLVMVerifierFailureAction.LLVMReturnStatusAction);
    }

    /**
     * Verifies that a single function is valid, taking the specified action. Useful
     * for debugging.
     */
    public static boolean nLLVMVerifyFunction(LLVMValueRef Fn, LLVMVerifierFailureAction Action) {
        return Native.INSTANCE.LLVMVerifyFunction(Fn.value(), Action.value());
    }

    /**
     * Verifies that a single function is valid, taking the specified action. Useful
     * for debugging.
     */
    // Port-added
    public static void LLVMVerifyFunction(LLVMValueRef Fn, LLVMVerifierFailureAction Action) throws LLVMException {
        if (nLLVMVerifyFunction(Fn, Action)) {
            throw new LLVMException("Failed to verify function");
        }
    }

    /**
     * Verifies that a single function is valid. Useful for debugging.
     */
    // Port-added
    public static void LLVMVerifyFunction(LLVMValueRef Fn) throws LLVMException {
        LLVMVerifyFunction(Fn, LLVMVerifierFailureAction.LLVMPrintMessageAction);
    }

    /**
     * Open up a ghostview window that displays the CFG of the current function.
     * Useful for debugging.
     */
    public static void LLVMViewFunctionCFG(LLVMValueRef Fn) {
        Native.INSTANCE.LLVMViewFunctionCFG(Fn.value());
    }

    public static void LLVMViewFunctionCFGOnly(LLVMValueRef Fn) {
        Native.INSTANCE.LLVMViewFunctionCFGOnly(Fn.value());
    }
}

package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.cLLVMModuleRef;
import static com.v7878.llvm.Types.cLLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleLinker.processSymbol;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMValueRef;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Analysis {

    /*
     * @defgroup LLVMCAnalysis Analysis
     * @ingroup LLVMC
     */

    static final Class<?> cLLVMVerifierFailureAction = ENUM;

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

        public int value() {
            return ordinal();
        }

        public static LLVMVerifierFailureAction of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    private enum Function {
        LLVMVerifyModule(LLVMBool, cLLVMModuleRef, cLLVMVerifierFailureAction, ptr(CHAR_PTR)),
        LLVMVerifyFunction(LLVMBool, cLLVMValueRef, cLLVMVerifierFailureAction),
        LLVMViewFunctionCFG(void.class, cLLVMValueRef),
        LLVMViewFunctionCFGOnly(void.class, cLLVMValueRef);

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


    /**
     * Verifies that a module is valid, taking the specified action if not.
     * Optionally returns a human-readable description of any invalid constructs.
     */
    public static boolean LLVMVerifyModule(LLVMModuleRef M, LLVMVerifierFailureAction Action, Consumer<String> OutMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutMessage = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMVerifyModule.handle()
                    .invoke(M.value(), Action.value(), c_OutMessage.nativeAddress()));
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
        if (LLVMVerifyModule(M, Action, E -> err[0] = E)) {
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
        return nothrows_run(() -> (boolean) Function.LLVMVerifyFunction.handle().invoke(Fn.value(), Action.value()));
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
        nothrows_run(() -> Function.LLVMViewFunctionCFG.handle().invoke(Fn.value()));
    }

    public static void LLVMViewFunctionCFGOnly(LLVMValueRef Fn) {
        nothrows_run(() -> Function.LLVMViewFunctionCFGOnly.handle().invoke(Fn.value()));
    }
}

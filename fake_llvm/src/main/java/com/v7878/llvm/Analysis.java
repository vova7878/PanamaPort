package com.v7878.llvm;

import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMValueRef;

import java.util.function.Consumer;

public class Analysis {
    public enum LLVMVerifierFailureAction {
        LLVMAbortProcessAction,
        LLVMPrintMessageAction,
        LLVMReturnStatusAction;

        public static LLVMVerifierFailureAction of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMVerifyFunction(LLVMValueRef Fn) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMVerifyFunction(LLVMValueRef Fn, LLVMVerifierFailureAction Action) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMVerifyModule(LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMVerifyModule(LLVMModuleRef M, LLVMVerifierFailureAction Action) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMVerifyModule(LLVMModuleRef M, LLVMVerifierFailureAction Action, Consumer<String> OutMessage) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMViewFunctionCFG(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMViewFunctionCFGOnly(LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean nLLVMVerifyFunction(LLVMValueRef Fn, LLVMVerifierFailureAction Action) {
        throw new UnsupportedOperationException("Stub!");
    }
}

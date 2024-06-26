package com.v7878.llvm;

import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMValueRef;

@SuppressWarnings("RedundantThrows")
public final class Analysis {
    private Analysis() {
        throw new UnsupportedOperationException("Stub!");
    }

    public enum LLVMVerifierFailureAction {
        LLVMAbortProcessAction,
        LLVMPrintMessageAction,
        LLVMReturnStatusAction
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

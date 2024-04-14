package com.v7878.llvm;

public class ErrorHandling {

    @FunctionalInterface
    public interface LLVMFatalErrorHandler {
        void handle(String Reason);
    }

    public static void LLVMInstallFatalErrorHandler(LLVMFatalErrorHandler Handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMResetFatalErrorHandler() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMEnablePrettyStackTrace() {
        throw new UnsupportedOperationException("Stub!");
    }
}

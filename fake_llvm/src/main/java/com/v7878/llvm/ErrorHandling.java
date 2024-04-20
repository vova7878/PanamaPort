package com.v7878.llvm;

public final class ErrorHandling {
    private ErrorHandling() {
        throw new UnsupportedOperationException("Stub!");
    }

    @FunctionalInterface
    public interface LLVMFatalErrorHandler {
        void invoke(String reason);
    }

    public static void LLVMEnablePrettyStackTrace() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInstallFatalErrorHandler(LLVMFatalErrorHandler Handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMResetFatalErrorHandler() {
        throw new UnsupportedOperationException("Stub!");
    }
}

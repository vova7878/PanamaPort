package com.v7878.llvm;

public class LLVMException extends Exception {
    public LLVMException() {
        super();
    }

    public LLVMException(String message) {
        super(message);
    }

    public LLVMException(String message, Throwable cause) {
        super(message, cause);
    }

    public LLVMException(Throwable cause) {
        super(cause);
    }
}

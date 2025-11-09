package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;

import com.v7878.llvm.Types.LLVMModuleRef;

public final class IRReader {
    private IRReader() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMParseIRInContext(LLVMContextRef ContextRef, LLVMMemoryBufferRef MemBuf) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMParseIRInContext(LLVMContextRef ContextRef, String Data) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }
}

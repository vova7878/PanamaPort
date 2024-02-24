package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;

public class Extra {

    public static String LLVMGetHostTriple() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetHostCPUName() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetHostCPUFeatures() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment[] getFunctionsCode(LLVMMemoryBufferRef obj, String... names) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment getFunctionCode(LLVMMemoryBufferRef obj, String name) {
        throw new UnsupportedOperationException("Stub!");
    }
}

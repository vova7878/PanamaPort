package com.v7878.llvm;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMTypeRef;

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

    public static LLVMTypeRef layoutToLLVMTypeInContext(LLVMContextRef context, MemoryLayout layout) {
        throw new UnsupportedOperationException("Stub!");
    }
}

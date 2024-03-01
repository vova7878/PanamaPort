package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;

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

    public static MemorySegment[] getFunctionsCode(LLVMObjectFileRef obj, String... names) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment getFunctionCode(LLVMObjectFileRef obj, String name) {
        throw new UnsupportedOperationException("Stub!");
    }
}

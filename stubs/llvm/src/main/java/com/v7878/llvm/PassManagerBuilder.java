package com.v7878.llvm;

import com.v7878.llvm.Types.LLVMPassManagerRef;

public final class PassManagerBuilder {
    private PassManagerBuilder() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static final class LLVMPassManagerBuilderRef implements AutoCloseable {
        private LLVMPassManagerBuilderRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static LLVMPassManagerBuilderRef LLVMPassManagerBuilderCreate() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderDispose(LLVMPassManagerBuilderRef PMB) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderPopulateFunctionPassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderPopulateLTOPassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM, boolean Internalize, boolean RunInliner) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderPopulateModulePassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderSetDisableSimplifyLibCalls(LLVMPassManagerBuilderRef PMB, boolean Value) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderSetDisableUnitAtATime(LLVMPassManagerBuilderRef PMB, boolean Value) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderSetDisableUnrollLoops(LLVMPassManagerBuilderRef PMB, boolean Value) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderSetOptLevel(LLVMPassManagerBuilderRef PMB, int OptLevel) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderSetSizeLevel(LLVMPassManagerBuilderRef PMB, int SizeLevel) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMPassManagerBuilderUseInlinerWithThreshold(LLVMPassManagerBuilderRef PMB, int Threshold) {
        throw new UnsupportedOperationException("Stub!");
    }
}

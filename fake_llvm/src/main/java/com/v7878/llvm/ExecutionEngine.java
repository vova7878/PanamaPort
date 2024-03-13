package com.v7878.llvm;

import android.annotation.TargetApi;
import android.os.Build;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;

import java.util.function.Consumer;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class ExecutionEngine {
    public static final class LLVMGenericValueRef implements AutoCloseable {

        private LLVMGenericValueRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMExecutionEngineRef implements AutoCloseable {
        private LLVMExecutionEngineRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMMCJITMemoryManagerRef implements AutoCloseable {
        private LLVMMCJITMemoryManagerRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMLinkInMCJIT() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMLinkInInterpreter() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfInt(LLVMTypeRef Ty, long /* unsigned long long */ N, boolean IsSigned) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfSegment(MemorySegment S) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfFloat(LLVMTypeRef Ty, double N) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMGenericValueIntWidth(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* unsigned long long */ LLVMGenericValueToInt(LLVMGenericValueRef GenVal, boolean IsSigned) {
        throw new UnsupportedOperationException("Stub!");
    }

    public MemorySegment LLVMGenericValueToSegment(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static double LLVMGenericValueToFloat(LLVMTypeRef Ty, LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeGenericValue(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMCreateExecutionEngineForModule(Consumer<LLVMExecutionEngineRef> OutEE, LLVMModuleRef M, Consumer<String> OutError) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateExecutionEngineForModule(LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMCreateInterpreterForModule(Consumer<LLVMExecutionEngineRef> OutInterp, LLVMModuleRef M, Consumer<String> OutError) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateInterpreterForModule(LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMCreateJITCompilerForModule(Consumer<LLVMExecutionEngineRef> OutJIT, LLVMModuleRef M, int /* unsigned */ OptLevel, Consumer<String> OutError) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateJITCompilerForModule(LLVMModuleRef M, int /* unsigned */ OptLevel) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeExecutionEngine(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRunStaticConstructors(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRunStaticDestructors(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMRunFunction(LLVMExecutionEngineRef EE, LLVMValueRef F, LLVMGenericValueRef[] Args) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeMCJITMemoryManager(LLVMMCJITMemoryManagerRef MM) {
        throw new UnsupportedOperationException("Stub!");
    }
}

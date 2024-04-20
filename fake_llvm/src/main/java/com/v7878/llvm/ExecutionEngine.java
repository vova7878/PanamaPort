package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;

@SuppressWarnings("RedundantThrows")
public final class ExecutionEngine {
    private ExecutionEngine() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static final class LLVMGenericValueRef implements AutoCloseable {
        private LLVMGenericValueRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMExecutionEngineRef implements AutoCloseable {
        private LLVMExecutionEngineRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMMCJITMemoryManagerRef implements AutoCloseable {
        private LLVMMCJITMemoryManagerRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMAddGlobalMapping(LLVMExecutionEngineRef EE, LLVMValueRef Global, MemorySegment Addr) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateExecutionEngineForModule(LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfFloat(LLVMTypeRef Ty, double N) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfInt(LLVMTypeRef Ty, long N, boolean IsSigned) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfSegment(MemorySegment S) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateInterpreterForModule(LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateJITCompilerForModule(LLVMModuleRef M, int OptLevel) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeExecutionEngine(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeGenericValue(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeMCJITMemoryManager(LLVMMCJITMemoryManagerRef MM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMValueRef LLVMFindFunction(LLVMExecutionEngineRef EE, String Name) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMFreeMachineCodeForFunction(LLVMExecutionEngineRef EE, LLVMValueRef F) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMGenericValueIntWidth(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static double LLVMGenericValueToFloat(LLVMTypeRef Ty, LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGenericValueToInt(LLVMGenericValueRef GenVal, boolean IsSigned) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMGetExecutionEngineTargetData(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetMachineRef LLVMGetExecutionEngineTargetMachine(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetFunctionAddress(LLVMExecutionEngineRef EE, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetGlobalValueAddress(LLVMExecutionEngineRef EE, String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetPointerToGlobal(LLVMExecutionEngineRef EE, LLVMValueRef Global) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMLinkInInterpreter() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMLinkInMCJIT() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMRecompileAndRelinkFunction(LLVMExecutionEngineRef EE, LLVMValueRef Fn) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMModuleRef LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMGenericValueRef LLVMRunFunction(LLVMExecutionEngineRef EE, LLVMValueRef F, LLVMGenericValueRef[] Args) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRunStaticConstructors(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMRunStaticDestructors(LLVMExecutionEngineRef EE) {
        throw new UnsupportedOperationException("Stub!");
    }

    public MemorySegment LLVMGenericValueToSegment(LLVMGenericValueRef GenVal) {
        throw new UnsupportedOperationException("Stub!");
    }
}

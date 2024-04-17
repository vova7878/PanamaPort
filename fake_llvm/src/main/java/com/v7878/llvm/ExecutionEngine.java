package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;

import java.util.function.Consumer;

public class ExecutionEngine {
    public static final class LLVMGenericValueRef implements AutoCloseable {
        public static LLVMGenericValueRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMGenericValueRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMExecutionEngineRef implements AutoCloseable {
        public static LLVMExecutionEngineRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMExecutionEngineRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMMCJITMemoryManagerRef implements AutoCloseable {
        public static LLVMMCJITMemoryManagerRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMMCJITMemoryManagerRef ofNullable(long value) {
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

    public static boolean LLVMCreateExecutionEngineForModule(Consumer<LLVMExecutionEngineRef> OutEE, LLVMModuleRef M, Consumer<String> OutError) {
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

    public static boolean LLVMCreateInterpreterForModule(Consumer<LLVMExecutionEngineRef> OutInterp, LLVMModuleRef M, Consumer<String> OutError) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMExecutionEngineRef LLVMCreateJITCompilerForModule(LLVMModuleRef M, int OptLevel) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMCreateJITCompilerForModule(Consumer<LLVMExecutionEngineRef> OutJIT, LLVMModuleRef M, int OptLevel, Consumer<String> OutError) {
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

    public static boolean LLVMFindFunction(LLVMExecutionEngineRef EE, String Name, Consumer<LLVMValueRef> OutFn) {
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

    public static boolean LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M, Consumer<LLVMModuleRef> OutMod, Consumer<String> OutError) {
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

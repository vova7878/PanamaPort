package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Target.cLLVMTargetDataRef;
import static com.v7878.llvm.TargetMachine.cLLVMTargetMachineRef;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.cLLVMModuleRef;
import static com.v7878.llvm.Types.cLLVMTypeRef;
import static com.v7878.llvm.Types.cLLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.DOUBLE;
import static com.v7878.llvm._Utils.INT;
import static com.v7878.llvm._Utils.SIZE_T;
import static com.v7878.llvm._Utils.UINT64_T;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.llvm._Utils.UNSIGNED_LONG_LONG;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.arrayLength;
import static com.v7878.llvm._Utils.const_ptr;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.unsafe.Utils.nothrows_run;

import android.annotation.TargetApi;
import android.os.Build;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.foreign.SimpleBulkLinker;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class ExecutionEngine {

    /*
     * @defgroup LLVMCExecutionEngine Execution Engine
     * @ingroup LLVMC
     */

    static final Class<?> cLLVMGenericValueRef = VOID_PTR;
    static final Class<?> cLLVMExecutionEngineRef = VOID_PTR;
    static final Class<?> cLLVMMCJITMemoryManagerRef = VOID_PTR;

    public static final class LLVMGenericValueRef extends AddressValue {

        LLVMGenericValueRef(long value) {
            super(value);
        }
    }

    public static final class LLVMExecutionEngineRef extends AddressValue {
        LLVMExecutionEngineRef(long value) {
            super(value);
        }
    }

    public static final class LLVMMCJITMemoryManagerRef extends AddressValue {
        LLVMMCJITMemoryManagerRef(long value) {
            super(value);
        }
    }

    //TODO
    //struct LLVMMCJITCompilerOptions {
    //    unsigned OptLevel;
    //    LLVMCodeModel CodeModel;
    //    LLVMBool NoFramePointerElim;
    //    LLVMBool EnableFastISel;
    //    LLVMMCJITMemoryManagerRef MCJMM;
    //};
    static final Class<?> LLVMMCJITCompilerOptions_PTR = VOID_PTR;

    //TODO
    static final Class<?> LLVMMemoryManagerAllocateCodeSectionCallback = VOID_PTR; // uint8_t *(*LLVMMemoryManagerAllocateCodeSectionCallback)(void *Opaque, uintptr_t Size, unsigned Alignment, unsigned SectionID, const char *SectionName);
    static final Class<?> LLVMMemoryManagerAllocateDataSectionCallback = VOID_PTR; // uint8_t *(*LLVMMemoryManagerAllocateDataSectionCallback)(void *Opaque, uintptr_t Size, unsigned Alignment, unsigned SectionID, const char *SectionName, LLVMBool IsReadOnly);
    static final Class<?> LLVMMemoryManagerFinalizeMemoryCallback = VOID_PTR; // LLVMBool (*LLVMMemoryManagerFinalizeMemoryCallback)(void *Opaque, char **ErrMsg);
    static final Class<?> LLVMMemoryManagerDestroyCallback = VOID_PTR; // void (*LLVMMemoryManagerDestroyCallback)(void *Opaque);

    private enum Function implements SymbolHolder2 {
        LLVMLinkInMCJIT(void.class),
        LLVMLinkInInterpreter(void.class),
        LLVMCreateGenericValueOfInt(cLLVMGenericValueRef, cLLVMTypeRef, UNSIGNED_LONG_LONG, LLVMBool),
        LLVMCreateGenericValueOfPointer(cLLVMGenericValueRef, VOID_PTR),
        LLVMCreateGenericValueOfFloat(cLLVMGenericValueRef, cLLVMTypeRef, DOUBLE),
        LLVMGenericValueIntWidth(UNSIGNED_INT, cLLVMGenericValueRef),
        LLVMGenericValueToInt(UNSIGNED_LONG_LONG, cLLVMGenericValueRef, LLVMBool),
        LLVMGenericValueToPointer(VOID_PTR, cLLVMGenericValueRef),
        LLVMGenericValueToFloat(DOUBLE, cLLVMTypeRef, cLLVMGenericValueRef),
        LLVMDisposeGenericValue(void.class, cLLVMGenericValueRef),
        LLVMCreateExecutionEngineForModule(LLVMBool, ptr(cLLVMExecutionEngineRef), cLLVMModuleRef, ptr(CHAR_PTR)),
        LLVMCreateInterpreterForModule(LLVMBool, ptr(cLLVMExecutionEngineRef), cLLVMModuleRef, ptr(CHAR_PTR)),
        LLVMCreateJITCompilerForModule(LLVMBool, ptr(cLLVMExecutionEngineRef), cLLVMModuleRef, UNSIGNED_INT, ptr(CHAR_PTR)),
        LLVMInitializeMCJITCompilerOptions(void.class, LLVMMCJITCompilerOptions_PTR, SIZE_T),
        LLVMCreateMCJITCompilerForModule(LLVMBool, ptr(cLLVMExecutionEngineRef), cLLVMModuleRef, LLVMMCJITCompilerOptions_PTR, SIZE_T, ptr(CHAR_PTR)),
        LLVMDisposeExecutionEngine(void.class, cLLVMExecutionEngineRef),
        LLVMRunStaticConstructors(void.class, cLLVMExecutionEngineRef),
        LLVMRunStaticDestructors(void.class, cLLVMExecutionEngineRef),
        LLVMRunFunctionAsMain(INT, cLLVMExecutionEngineRef, cLLVMValueRef, UNSIGNED_INT, const_ptr(CONST_CHAR_PTR), const_ptr(CONST_CHAR_PTR)),
        LLVMRunFunction(cLLVMGenericValueRef, cLLVMExecutionEngineRef, cLLVMValueRef, UNSIGNED_INT, ptr(cLLVMGenericValueRef)),
        LLVMFreeMachineCodeForFunction(void.class, cLLVMExecutionEngineRef, cLLVMValueRef),
        LLVMAddModule(void.class, cLLVMExecutionEngineRef, cLLVMModuleRef),
        LLVMRemoveModule(LLVMBool, cLLVMExecutionEngineRef, cLLVMModuleRef, ptr(cLLVMModuleRef), ptr(CHAR_PTR)),
        LLVMFindFunction(LLVMBool, cLLVMExecutionEngineRef, CONST_CHAR_PTR, ptr(cLLVMValueRef)),
        LLVMRecompileAndRelinkFunction(VOID_PTR, cLLVMExecutionEngineRef, cLLVMValueRef),
        LLVMGetExecutionEngineTargetData(cLLVMTargetDataRef, cLLVMExecutionEngineRef),
        LLVMGetExecutionEngineTargetMachine(cLLVMTargetMachineRef, cLLVMExecutionEngineRef),
        LLVMAddGlobalMapping(void.class, cLLVMExecutionEngineRef, cLLVMValueRef, VOID_PTR),
        LLVMGetPointerToGlobal(VOID_PTR, cLLVMExecutionEngineRef, cLLVMValueRef),
        LLVMGetGlobalValueAddress(UINT64_T, cLLVMExecutionEngineRef, CONST_CHAR_PTR),
        LLVMGetFunctionAddress(UINT64_T, cLLVMExecutionEngineRef, CONST_CHAR_PTR),
        LLVMCreateSimpleMCJITMemoryManager(cLLVMMCJITMemoryManagerRef, VOID_PTR, LLVMMemoryManagerAllocateCodeSectionCallback,
                LLVMMemoryManagerAllocateDataSectionCallback, LLVMMemoryManagerFinalizeMemoryCallback, LLVMMemoryManagerDestroyCallback),
        LLVMDisposeMCJITMemoryManager(void.class, cLLVMMCJITMemoryManagerRef);

        static {
            SimpleBulkLinker.processSymbols(LLVM, LLVM_SCOPE, Function.values());
        }

        private final MethodType type;

        private LongSupplier symbol;
        private Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(LongSupplier symbol) {
            this.symbol = symbol;
        }

        @Override
        public void setHandle(Supplier<MethodHandle> handle) {
            this.handle = handle;
        }

        public long symbol() {
            return symbol.getAsLong();
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + symbol() +
                    ", handle=" + handle() + '}';
        }
    }

    public static void LLVMLinkInMCJIT() {
        nothrows_run(() -> Function.LLVMLinkInMCJIT.handle().invoke());
    }

    public static void LLVMLinkInInterpreter() {
        nothrows_run(() -> Function.LLVMLinkInInterpreter.handle().invoke());
    }

    /*===-- Operations on generic values --------------------------------------===*/

    public static LLVMGenericValueRef LLVMCreateGenericValueOfInt(LLVMTypeRef Ty, long /* unsigned long long */ N, boolean IsSigned) {
        return nothrows_run(() -> new LLVMGenericValueRef((long) Function.LLVMCreateGenericValueOfInt.handle().invoke(Ty.value(), N, IsSigned)));
    }

    /* package-private */
    static LLVMGenericValueRef LLVMCreateGenericValueOfPointer(long P) {
        return nothrows_run(() -> new LLVMGenericValueRef((long) Function.LLVMCreateGenericValueOfPointer.handle().invoke(P)));
    }

    // Port-added
    public static LLVMGenericValueRef LLVMCreateGenericValueOfSegment(MemorySegment S) {
        return LLVMCreateGenericValueOfPointer(S.nativeAddress());
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfFloat(LLVMTypeRef Ty, double N) {
        return nothrows_run(() -> new LLVMGenericValueRef((long) Function.LLVMCreateGenericValueOfFloat.handle().invoke(Ty.value(), N)));
    }

    public static int /* unsigned */ LLVMGenericValueIntWidth(LLVMGenericValueRef GenVal) {
        return nothrows_run(() -> (int) Function.LLVMGenericValueIntWidth.handle().invoke(GenVal.value()));
    }

    public static long /* unsigned long long */ LLVMGenericValueToInt(LLVMGenericValueRef GenVal, boolean IsSigned) {
        return nothrows_run(() -> (long) Function.LLVMGenericValueToInt.handle().invoke(GenVal.value(), IsSigned));
    }

    /* package-private */
    static long LLVMGenericValueToPointer(LLVMGenericValueRef GenVal) {
        return nothrows_run(() -> (long) Function.LLVMGenericValueToPointer.handle().invoke(GenVal.value()));
    }

    // Port-added
    public MemorySegment LLVMGenericValueToSegment(LLVMGenericValueRef GenVal) {
        return MemorySegment.ofAddress(LLVMGenericValueToPointer(GenVal));
    }

    public static double LLVMGenericValueToFloat(LLVMTypeRef Ty, LLVMGenericValueRef GenVal) {
        return nothrows_run(() -> (double) Function.LLVMGenericValueToFloat.handle().invoke(Ty.value(), GenVal.value()));
    }

    public static void LLVMDisposeGenericValue(LLVMGenericValueRef GenVal) {
        nothrows_run(() -> Function.LLVMDisposeGenericValue.handle().invoke(GenVal.value()));
    }

    ///*===-- Operations on execution engines -----------------------------------===*/

    public static boolean LLVMCreateExecutionEngineForModule(Consumer<LLVMExecutionEngineRef> OutEE, LLVMModuleRef M, Consumer<String> OutError) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutEE = arena.allocate(ADDRESS);
            MemorySegment c_OutError = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMCreateExecutionEngineForModule.handle()
                    .invoke(c_OutEE.nativeAddress(), M.value(), c_OutError.nativeAddress()));
            if (!err) {
                OutEE.accept(new LLVMExecutionEngineRef(c_OutEE.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutError.accept(addressToLLVMString(c_OutError.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMExecutionEngineRef LLVMCreateExecutionEngineForModule(LLVMModuleRef M) throws LLVMException {
        String[] err = new String[1];
        LLVMExecutionEngineRef[] out = new LLVMExecutionEngineRef[1];
        if (LLVMCreateExecutionEngineForModule(O -> out[0] = O, M, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    public static boolean LLVMCreateInterpreterForModule(Consumer<LLVMExecutionEngineRef> OutInterp, LLVMModuleRef M, Consumer<String> OutError) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutInterp = arena.allocate(ADDRESS);
            MemorySegment c_OutError = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMCreateInterpreterForModule.handle()
                    .invoke(c_OutInterp.nativeAddress(), M.value(), c_OutError.nativeAddress()));
            if (!err) {
                OutInterp.accept(new LLVMExecutionEngineRef(c_OutInterp.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutError.accept(addressToLLVMString(c_OutError.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMExecutionEngineRef LLVMCreateInterpreterForModule(LLVMModuleRef M) throws LLVMException {
        String[] err = new String[1];
        LLVMExecutionEngineRef[] out = new LLVMExecutionEngineRef[1];
        if (LLVMCreateInterpreterForModule(O -> out[0] = O, M, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    public static boolean LLVMCreateJITCompilerForModule(Consumer<LLVMExecutionEngineRef> OutJIT, LLVMModuleRef M, int /* unsigned */ OptLevel, Consumer<String> OutError) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutJIT = arena.allocate(ADDRESS);
            MemorySegment c_OutError = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMCreateJITCompilerForModule.handle()
                    .invoke(c_OutJIT.nativeAddress(), M.value(), OptLevel, c_OutError.nativeAddress()));
            if (!err) {
                OutJIT.accept(new LLVMExecutionEngineRef(c_OutJIT.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutError.accept(addressToLLVMString(c_OutError.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMExecutionEngineRef LLVMCreateJITCompilerForModule(LLVMModuleRef M, int /* unsigned */ OptLevel) throws LLVMException {
        String[] err = new String[1];
        LLVMExecutionEngineRef[] out = new LLVMExecutionEngineRef[1];
        if (LLVMCreateJITCompilerForModule(O -> out[0] = O, M, OptLevel, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    //void LLVMInitializeMCJITCompilerOptions(struct LLVMMCJITCompilerOptions *Options, size_t SizeOfOptions) {
    //    return nothrows_run(() -> Function.LLVMInitializeMCJITCompilerOptions.handle().invoke());
    //}
    ///**
    // * Create an MCJIT execution engine for a module, with the given options. It is
    // * the responsibility of the caller to ensure that all fields in Options up to
    // * the given SizeOfOptions are initialized. It is correct to pass a smaller
    // * value of SizeOfOptions that omits some fields. The canonical way of using
    // * this is:
    // *
    // * LLVMMCJITCompilerOptions options;
    // * LLVMInitializeMCJITCompilerOptions(&options, sizeof(options));
    // * ... fill in those options you care about
    // * LLVMCreateMCJITCompilerForModule(&jit, mod, &options, sizeof(options),
    // *                                  &error);
    // *
    // * Note that this is also correct, though possibly suboptimal:
    // *
    // * LLVMCreateMCJITCompilerForModule(&jit, mod, 0, 0, &error);
    // */
    //LLVMBool LLVMCreateMCJITCompilerForModule(LLVMExecutionEngineRef *OutJIT, LLVMModuleRef M, struct LLVMMCJITCompilerOptions *Options, size_t SizeOfOptions, char **OutError) {
    //    return nothrows_run(() -> Function.LLVMCreateMCJITCompilerForModule.handle().invoke());
    //}

    public static void LLVMDisposeExecutionEngine(LLVMExecutionEngineRef EE) {
        nothrows_run(() -> Function.LLVMDisposeExecutionEngine.handle().invoke(EE.value()));
    }

    public static void LLVMRunStaticConstructors(LLVMExecutionEngineRef EE) {
        nothrows_run(() -> Function.LLVMRunStaticConstructors.handle().invoke(EE.value()));
    }

    public static void LLVMRunStaticDestructors(LLVMExecutionEngineRef EE) {
        nothrows_run(() -> Function.LLVMRunStaticDestructors.handle().invoke(EE.value()));
    }

    //int LLVMRunFunctionAsMain(LLVMExecutionEngineRef EE, LLVMValueRef F, unsigned ArgC, const char * const *ArgV, const char * const *EnvP) {
    //    return nothrows_run(() -> Function.LLVMRunFunctionAsMain.handle().invoke());
    //}

    public static LLVMGenericValueRef LLVMRunFunction(LLVMExecutionEngineRef EE, LLVMValueRef F, LLVMGenericValueRef[] Args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            return nothrows_run(() -> new LLVMGenericValueRef((long) Function.LLVMRunFunction.handle()
                    .invoke(EE.value(), F.value(), NumArgs, c_Args.nativeAddress())));
        }
    }

    //void LLVMFreeMachineCodeForFunction(LLVMExecutionEngineRef EE, LLVMValueRef F) {
    //    return nothrows_run(() -> Function.LLVMFreeMachineCodeForFunction.handle().invoke());
    //}
    //void LLVMAddModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMAddModule.handle().invoke());
    //}
    //LLVMBool LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M, LLVMModuleRef *OutMod, char **OutError) {
    //    return nothrows_run(() -> Function.LLVMRemoveModule.handle().invoke());
    //}
    //LLVMBool LLVMFindFunction(LLVMExecutionEngineRef EE, const char *Name, LLVMValueRef *OutFn) {
    //    return nothrows_run(() -> Function.LLVMFindFunction.handle().invoke());
    //}
    //void *LLVMRecompileAndRelinkFunction(LLVMExecutionEngineRef EE, LLVMValueRef Fn) {
    //    return nothrows_run(() -> Function.*LLVMRecompileAndRelinkFunction.handle().invoke());
    //}
    //LLVMTargetDataRef LLVMGetExecutionEngineTargetData(LLVMExecutionEngineRef EE) {
    //    return nothrows_run(() -> Function.LLVMGetExecutionEngineTargetData.handle().invoke());
    //}
    //LLVMTargetMachineRef LLVMGetExecutionEngineTargetMachine(LLVMExecutionEngineRef EE) {
    //    return nothrows_run(() -> Function.LLVMGetExecutionEngineTargetMachine.handle().invoke());
    //}
    //void LLVMAddGlobalMapping(LLVMExecutionEngineRef EE, LLVMValueRef Global, void* Addr) {
    //    return nothrows_run(() -> Function.LLVMAddGlobalMapping.handle().invoke());
    //}
    //void *LLVMGetPointerToGlobal(LLVMExecutionEngineRef EE, LLVMValueRef Global) {
    //    return nothrows_run(() -> Function.*LLVMGetPointerToGlobal.handle().invoke());
    //}
    //uint64_t LLVMGetGlobalValueAddress(LLVMExecutionEngineRef EE, const char *Name) {
    //    return nothrows_run(() -> Function.LLVMGetGlobalValueAddress.handle().invoke());
    //}
    //uint64_t LLVMGetFunctionAddress(LLVMExecutionEngineRef EE, const char *Name) {
    //    return nothrows_run(() -> Function.LLVMGetFunctionAddress.handle().invoke());
    //}

    //TODO
    ///*===-- Operations on memory managers -------------------------------------===*/
    ///**
    // * Create a simple custom MCJIT memory manager. This memory manager can
    // * intercept allocations in a module-oblivious way. This will return NULL
    // * if any of the passed functions are NULL.
    // *
    // * @param Opaque An opaque client object to pass back to the callbacks.
    // * @param AllocateCodeSection Allocate a block of memory for executable code.
    // * @param AllocateDataSection Allocate a block of memory for data.
    // * @param FinalizeMemory Set page permissions and flush cache. Return 0 on
    // *   success, 1 on error.
    // */
    //LLVMMCJITMemoryManagerRef LLVMCreateSimpleMCJITMemoryManager(void *Opaque, LLVMMemoryManagerAllocateCodeSectionCallback AllocateCodeSection, LLVMMemoryManagerAllocateDataSectionCallback AllocateDataSection, LLVMMemoryManagerFinalizeMemoryCallback FinalizeMemory, LLVMMemoryManagerDestroyCallback Destroy) {
    //    return nothrows_run(() -> Function.LLVMCreateSimpleMCJITMemoryManager.handle().invoke());
    //}
    //void LLVMDisposeMCJITMemoryManager(LLVMMCJITMemoryManagerRef MM) {
    //    return nothrows_run(() -> Function.LLVMDisposeMCJITMemoryManager.handle().invoke());
    //}
}

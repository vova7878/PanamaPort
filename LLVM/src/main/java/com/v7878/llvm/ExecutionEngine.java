package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.arrayLength;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.DOUBLE;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.util.function.Consumer;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class ExecutionEngine {

    /*
     * @defgroup LLVMCExecutionEngine Execution Engine
     * @ingroup LLVMC
     */

    public static final class LLVMGenericValueRef extends AddressValue implements FineClosable {

        private LLVMGenericValueRef(long value) {
            super(value);
        }

        public static LLVMGenericValueRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMGenericValueRef of 0");
            }
            return new LLVMGenericValueRef(value);
        }

        public static LLVMGenericValueRef ofNullable(long value) {
            return value == 0 ? null : new LLVMGenericValueRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeGenericValue(this);
        }
    }

    public static final class LLVMExecutionEngineRef extends AddressValue implements FineClosable {

        private LLVMExecutionEngineRef(long value) {
            super(value);
        }

        public static LLVMExecutionEngineRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMExecutionEngineRef of 0");
            }
            return new LLVMExecutionEngineRef(value);
        }

        public static LLVMExecutionEngineRef ofNullable(long value) {
            return value == 0 ? null : new LLVMExecutionEngineRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeExecutionEngine(this);
        }
    }

    public static final class LLVMMCJITMemoryManagerRef extends AddressValue implements FineClosable {

        private LLVMMCJITMemoryManagerRef(long value) {
            super(value);
        }

        public static LLVMMCJITMemoryManagerRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMMCJITMemoryManagerRef of 0");
            }
            return new LLVMMCJITMemoryManagerRef(value);
        }

        public static LLVMMCJITMemoryManagerRef ofNullable(long value) {
            return value == 0 ? null : new LLVMMCJITMemoryManagerRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeMCJITMemoryManager(this);
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

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol("LLVMLinkInMCJIT")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMLinkInMCJIT();

        @LibrarySymbol("LLVMLinkInInterpreter")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMLinkInInterpreter();

        @LibrarySymbol("LLVMCreateGenericValueOfInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG, BOOL_AS_INT})
        abstract long LLVMCreateGenericValueOfInt(long Ty, long N, boolean IsSigned);

        @LibrarySymbol("LLVMCreateGenericValueOfPointer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateGenericValueOfPointer(long P);

        @LibrarySymbol("LLVMCreateGenericValueOfFloat")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, DOUBLE})
        abstract long LLVMCreateGenericValueOfFloat(long Ty, double N);

        @LibrarySymbol("LLVMGenericValueIntWidth")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGenericValueIntWidth(long GenVal);

        @LibrarySymbol("LLVMGenericValueToInt")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract long LLVMGenericValueToInt(long GenVal, boolean IsSigned);

        @LibrarySymbol("LLVMGenericValueToPointer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGenericValueToPointer(long GenVal);

        @LibrarySymbol("LLVMGenericValueToFloat")
        @CallSignature(type = CRITICAL, ret = DOUBLE, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract double LLVMGenericValueToFloat(long Ty, long GenVal);

        @LibrarySymbol("LLVMDisposeGenericValue")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeGenericValue(long GenVal);

        @LibrarySymbol("LLVMCreateExecutionEngineForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateExecutionEngineForModule(long OutEE, long M, long OutError);

        @LibrarySymbol("LLVMCreateInterpreterForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateInterpreterForModule(long OutInterp, long M, long OutError);

        @LibrarySymbol("LLVMCreateJITCompilerForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract boolean LLVMCreateJITCompilerForModule(long OutJIT, long M, int OptLevel, long OutError);

        /*@LibrarySymbol("LLVMInitializeMCJITCompilerOptions")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMInitializeMCJITCompilerOptions(long, long);

        @LibrarySymbol("LLVMCreateMCJITCompilerForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMCJITCompilerForModule(long, long, long, long, long);*/

        @LibrarySymbol("LLVMDisposeExecutionEngine")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeExecutionEngine(long EE);

        @LibrarySymbol("LLVMRunStaticConstructors")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRunStaticConstructors(long EE);

        @LibrarySymbol("LLVMRunStaticDestructors")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRunStaticDestructors(long EE);

        /*@LibrarySymbol("LLVMRunFunctionAsMain")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMRunFunctionAsMain(long, long, int, long, long);*/

        @LibrarySymbol("LLVMRunFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMRunFunction(long EE, long F, int NumArgs, long Args);

        /*@LibrarySymbol("LLVMFreeMachineCodeForFunction")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMFreeMachineCodeForFunction(long, long);

        @LibrarySymbol("LLVMAddModule")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddModule(long, long);

        @LibrarySymbol("LLVMRemoveModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMRemoveModule(long, long, long, long);

        @LibrarySymbol("LLVMFindFunction")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMFindFunction(long, long, long);

        @LibrarySymbol("LLVMRecompileAndRelinkFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMRecompileAndRelinkFunction(long, long);

        @LibrarySymbol("LLVMGetExecutionEngineTargetData")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetExecutionEngineTargetData(long);

        @LibrarySymbol("LLVMGetExecutionEngineTargetMachine")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetExecutionEngineTargetMachine(long);

        @LibrarySymbol("LLVMAddGlobalMapping")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddGlobalMapping(long, long, long);

        @LibrarySymbol("LLVMGetPointerToGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetPointerToGlobal(long, long);

        @LibrarySymbol("LLVMGetGlobalValueAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetGlobalValueAddress(long, long);

        @LibrarySymbol("LLVMGetFunctionAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetFunctionAddress(long, long);

        @LibrarySymbol("LLVMCreateSimpleMCJITMemoryManager")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMCreateSimpleMCJITMemoryManager(long, long, long, long, long);*/

        @LibrarySymbol("LLVMDisposeMCJITMemoryManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeMCJITMemoryManager(long MM);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMLinkInMCJIT() {
        Native.INSTANCE.LLVMLinkInMCJIT();
    }

    public static void LLVMLinkInInterpreter() {
        Native.INSTANCE.LLVMLinkInInterpreter();
    }

    /*===-- Operations on generic values --------------------------------------===*/

    public static LLVMGenericValueRef LLVMCreateGenericValueOfInt(LLVMTypeRef Ty, long /* unsigned long long */ N, boolean IsSigned) {
        return LLVMGenericValueRef.ofNullable(Native.INSTANCE.LLVMCreateGenericValueOfInt(Ty.value(), N, IsSigned));
    }

    /* package-private */
    static LLVMGenericValueRef LLVMCreateGenericValueOfPointer(long P) {
        return LLVMGenericValueRef.ofNullable(Native.INSTANCE.LLVMCreateGenericValueOfPointer(P));
    }

    // Port-added
    public static LLVMGenericValueRef LLVMCreateGenericValueOfSegment(MemorySegment S) {
        return LLVMCreateGenericValueOfPointer(S.nativeAddress());
    }

    public static LLVMGenericValueRef LLVMCreateGenericValueOfFloat(LLVMTypeRef Ty, double N) {
        return LLVMGenericValueRef.ofNullable(Native.INSTANCE.LLVMCreateGenericValueOfFloat(Ty.value(), N));
    }

    public static int /* unsigned */ LLVMGenericValueIntWidth(LLVMGenericValueRef GenVal) {
        return Native.INSTANCE.LLVMGenericValueIntWidth(GenVal.value());
    }

    public static long /* unsigned long long */ LLVMGenericValueToInt(LLVMGenericValueRef GenVal, boolean IsSigned) {
        return Native.INSTANCE.LLVMGenericValueToInt(GenVal.value(), IsSigned);
    }

    /* package-private */
    static long LLVMGenericValueToPointer(LLVMGenericValueRef GenVal) {
        return Native.INSTANCE.LLVMGenericValueToPointer(GenVal.value());
    }

    // Port-added
    public MemorySegment LLVMGenericValueToSegment(LLVMGenericValueRef GenVal) {
        return MemorySegment.ofAddress(LLVMGenericValueToPointer(GenVal));
    }

    public static double LLVMGenericValueToFloat(LLVMTypeRef Ty, LLVMGenericValueRef GenVal) {
        return Native.INSTANCE.LLVMGenericValueToFloat(Ty.value(), GenVal.value());
    }

    public static void LLVMDisposeGenericValue(LLVMGenericValueRef GenVal) {
        Native.INSTANCE.LLVMDisposeGenericValue(GenVal.value());
    }

    /*===-- Operations on execution engines -----------------------------------===*/

    public static boolean LLVMCreateExecutionEngineForModule(Consumer<LLVMExecutionEngineRef> OutEE, LLVMModuleRef M, Consumer<String> OutError) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutEE = arena.allocate(ADDRESS);
            MemorySegment c_OutError = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMCreateExecutionEngineForModule(c_OutEE.nativeAddress(), M.value(), c_OutError.nativeAddress());
            if (!err) {
                OutEE.accept(LLVMExecutionEngineRef.ofNullable(c_OutEE.get(ADDRESS, 0).nativeAddress()));
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
            boolean err = Native.INSTANCE.LLVMCreateInterpreterForModule(c_OutInterp.nativeAddress(), M.value(), c_OutError.nativeAddress());
            if (!err) {
                OutInterp.accept(LLVMExecutionEngineRef.ofNullable(c_OutInterp.get(ADDRESS, 0).nativeAddress()));
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
            boolean err = Native.INSTANCE.LLVMCreateJITCompilerForModule(c_OutJIT.nativeAddress(), M.value(), OptLevel, c_OutError.nativeAddress());
            if (!err) {
                OutJIT.accept(LLVMExecutionEngineRef.ofNullable(c_OutJIT.get(ADDRESS, 0).nativeAddress()));
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
    //    return Native.INSTANCE.LLVMInitializeMCJITCompilerOptions();
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
    //    return Native.INSTANCE.LLVMCreateMCJITCompilerForModule();
    //}

    public static void LLVMDisposeExecutionEngine(LLVMExecutionEngineRef EE) {
        Native.INSTANCE.LLVMDisposeExecutionEngine(EE.value());
    }

    public static void LLVMRunStaticConstructors(LLVMExecutionEngineRef EE) {
        Native.INSTANCE.LLVMRunStaticConstructors(EE.value());
    }

    public static void LLVMRunStaticDestructors(LLVMExecutionEngineRef EE) {
        Native.INSTANCE.LLVMRunStaticDestructors(EE.value());
    }

    //int LLVMRunFunctionAsMain(LLVMExecutionEngineRef EE, LLVMValueRef F, unsigned ArgC, const char * const *ArgV, const char * const *EnvP) {
    //    return Native.INSTANCE.LLVMRunFunctionAsMain();
    //}

    public static LLVMGenericValueRef LLVMRunFunction(LLVMExecutionEngineRef EE, LLVMValueRef F, LLVMGenericValueRef[] Args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Args = allocArray(arena, Args);
            int /* unsigned */ NumArgs = arrayLength(Args);
            return LLVMGenericValueRef.ofNullable(Native.INSTANCE.LLVMRunFunction(EE.value(), F.value(), NumArgs, c_Args.nativeAddress()));
        }
    }

    //void LLVMFreeMachineCodeForFunction(LLVMExecutionEngineRef EE, LLVMValueRef F) {
    //    return Native.INSTANCE.LLVMFreeMachineCodeForFunction();
    //}
    //void LLVMAddModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) {
    //    return Native.INSTANCE.LLVMAddModule();
    //}
    //LLVMBool LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M, LLVMModuleRef *OutMod, char **OutError) {
    //    return Native.INSTANCE.LLVMRemoveModule();
    //}
    //LLVMBool LLVMFindFunction(LLVMExecutionEngineRef EE, const char *Name, LLVMValueRef *OutFn) {
    //    return Native.INSTANCE.LLVMFindFunction();
    //}
    //void *LLVMRecompileAndRelinkFunction(LLVMExecutionEngineRef EE, LLVMValueRef Fn) {
    //    return Native.INSTANCE.*LLVMRecompileAndRelinkFunction();
    //}
    //LLVMTargetDataRef LLVMGetExecutionEngineTargetData(LLVMExecutionEngineRef EE) {
    //    return Native.INSTANCE.LLVMGetExecutionEngineTargetData();
    //}
    //LLVMTargetMachineRef LLVMGetExecutionEngineTargetMachine(LLVMExecutionEngineRef EE) {
    //    return Native.INSTANCE.LLVMGetExecutionEngineTargetMachine();
    //}
    //void LLVMAddGlobalMapping(LLVMExecutionEngineRef EE, LLVMValueRef Global, void* Addr) {
    //    return Native.INSTANCE.LLVMAddGlobalMapping();
    //}
    //void *LLVMGetPointerToGlobal(LLVMExecutionEngineRef EE, LLVMValueRef Global) {
    //    return Native.INSTANCE.*LLVMGetPointerToGlobal();
    //}
    //uint64_t LLVMGetGlobalValueAddress(LLVMExecutionEngineRef EE, const char *Name) {
    //    return Native.INSTANCE.LLVMGetGlobalValueAddress();
    //}
    //uint64_t LLVMGetFunctionAddress(LLVMExecutionEngineRef EE, const char *Name) {
    //    return Native.INSTANCE.LLVMGetFunctionAddress();
    //}

    /*===-- Operations on memory managers -------------------------------------===*/

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
    //    return Native.INSTANCE.LLVMCreateSimpleMCJITMemoryManager();
    //}

    public static void LLVMDisposeMCJITMemoryManager(LLVMMCJITMemoryManagerRef MM) {
        Native.INSTANCE.LLVMDisposeMCJITMemoryManager(MM.value());
    }
}

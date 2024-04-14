package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocArray;
import static com.v7878.llvm._Utils.allocString;
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
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
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

/*===-- llvm-c/ExecutionEngine.h - ExecutionEngine Lib C Iface --*- C++ -*-===*\
|*                                                                            *|
|* This header declares the C interface to libLLVMExecutionEngine.o, which    *|
|* implements various analyses of the LLVM IR.                                *|
|*                                                                            *|
|* Many exotic languages can interoperate with C code but have a harder time  *|
|* with C++ due to name mangling. So in addition to C, this interface enables *|
|* tools written in such languages.                                           *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
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

        @LibrarySymbol(name = "LLVMLinkInMCJIT")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMLinkInMCJIT();

        @LibrarySymbol(name = "LLVMCreateGenericValueOfInt")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG, BOOL_AS_INT})
        abstract long LLVMCreateGenericValueOfInt(long Ty, long N, boolean IsSigned);

        @LibrarySymbol(name = "LLVMCreateGenericValueOfPointer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateGenericValueOfPointer(long P);

        @LibrarySymbol(name = "LLVMCreateGenericValueOfFloat")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, DOUBLE})
        abstract long LLVMCreateGenericValueOfFloat(long Ty, double N);

        @LibrarySymbol(name = "LLVMGenericValueIntWidth")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMGenericValueIntWidth(long GenVal);

        @LibrarySymbol(name = "LLVMGenericValueToInt")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract long LLVMGenericValueToInt(long GenVal, boolean IsSigned);

        @LibrarySymbol(name = "LLVMGenericValueToPointer")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGenericValueToPointer(long GenVal);

        @LibrarySymbol(name = "LLVMGenericValueToFloat")
        @CallSignature(type = CRITICAL, ret = DOUBLE, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract double LLVMGenericValueToFloat(long Ty, long GenVal);

        @LibrarySymbol(name = "LLVMDisposeGenericValue")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeGenericValue(long GenVal);

        @LibrarySymbol(name = "LLVMCreateExecutionEngineForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateExecutionEngineForModule(long OutEE, long M, long OutError);

        @LibrarySymbol(name = "LLVMCreateInterpreterForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateInterpreterForModule(long OutInterp, long M, long OutError);

        @LibrarySymbol(name = "LLVMCreateJITCompilerForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract boolean LLVMCreateJITCompilerForModule(long OutJIT, long M, int OptLevel, long OutError);

        /*@LibrarySymbol(name = "LLVMInitializeMCJITCompilerOptions")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMInitializeMCJITCompilerOptions(long, long);

        @LibrarySymbol(name = "LLVMCreateMCJITCompilerForModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMCreateMCJITCompilerForModule(long, long, long, long, long);*/

        @LibrarySymbol(name = "LLVMDisposeExecutionEngine")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeExecutionEngine(long EE);

        @LibrarySymbol(name = "LLVMRunStaticConstructors")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRunStaticConstructors(long EE);

        @LibrarySymbol(name = "LLVMRunStaticDestructors")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMRunStaticDestructors(long EE);

        /*@LibrarySymbol(name = "LLVMRunFunctionAsMain")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMRunFunctionAsMain(long, long, int, long, long);*/

        @LibrarySymbol(name = "LLVMRunFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract long LLVMRunFunction(long EE, long F, int NumArgs, long Args);

        @LibrarySymbol(name = "LLVMFreeMachineCodeForFunction")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMFreeMachineCodeForFunction(long EE, long F);

        @LibrarySymbol(name = "LLVMAddModule")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddModule(long EE, long M);

        @LibrarySymbol(name = "LLVMRemoveModule")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMRemoveModule(long EE, long M, long OutMod, long OutError);

        @LibrarySymbol(name = "LLVMFindFunction")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMFindFunction(long EE, long Name, long OutFn);

        @LibrarySymbol(name = "LLVMRecompileAndRelinkFunction")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMRecompileAndRelinkFunction(long EE, long Fn);

        @LibrarySymbol(name = "LLVMGetExecutionEngineTargetData")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetExecutionEngineTargetData(long EE);

        @LibrarySymbol(name = "LLVMGetExecutionEngineTargetMachine")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetExecutionEngineTargetMachine(long EE);

        @LibrarySymbol(name = "LLVMAddGlobalMapping")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddGlobalMapping(long EE, long Global, long Addr);

        @LibrarySymbol(name = "LLVMGetPointerToGlobal")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetPointerToGlobal(long EE, long Global);

        @LibrarySymbol(name = "LLVMGetGlobalValueAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetGlobalValueAddress(long EE, long Name);

        @LibrarySymbol(name = "LLVMGetFunctionAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMGetFunctionAddress(long EE, long Name);

        /*@LibrarySymbol(name = "LLVMCreateSimpleMCJITMemoryManager")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMCreateSimpleMCJITMemoryManager(long, long, long, long, long);*/

        @LibrarySymbol(name = "LLVMDisposeMCJITMemoryManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeMCJITMemoryManager(long MM);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    public static void LLVMLinkInMCJIT() {
        Native.INSTANCE.LLVMLinkInMCJIT();
    }

    public static void LLVMLinkInInterpreter() {
        throw new UnsupportedOperationException("LLVMLinkInInterpreter does not exist in android LLVM");
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

    //TODO
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

    //TODO
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

    public static void LLVMFreeMachineCodeForFunction(LLVMExecutionEngineRef EE, LLVMValueRef F) {
        Native.INSTANCE.LLVMFreeMachineCodeForFunction(EE.value(), F.value());
    }

    public static void LLVMAddModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) {
        Native.INSTANCE.LLVMAddModule(EE.value(), M.value());
    }

    public static boolean LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M, Consumer<LLVMModuleRef> OutMod, Consumer<String> OutError) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutMod = arena.allocate(ADDRESS);
            MemorySegment c_OutError = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMRemoveModule(EE.value(), M.value(),
                    c_OutMod.nativeAddress(), c_OutError.nativeAddress());
            if (!err) {
                OutMod.accept(LLVMModuleRef.ofNullable(c_OutMod.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutError.accept(addressToLLVMString(c_OutError.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMModuleRef LLVMRemoveModule(LLVMExecutionEngineRef EE, LLVMModuleRef M) throws LLVMException {
        String[] err = new String[1];
        LLVMModuleRef[] out = new LLVMModuleRef[1];
        if (LLVMRemoveModule(EE, M, O -> out[0] = O, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    public static boolean LLVMFindFunction(LLVMExecutionEngineRef EE, String Name, Consumer<LLVMValueRef> OutFn) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            MemorySegment c_OutFn = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMFindFunction(EE.value(), c_Name.nativeAddress(), c_OutFn.nativeAddress());
            if (!err) {
                OutFn.accept(LLVMValueRef.ofNullable(c_OutFn.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    // Port-added
    public static LLVMValueRef LLVMFindFunction(LLVMExecutionEngineRef EE, String Name) throws LLVMException {
        LLVMValueRef[] out = new LLVMValueRef[1];
        if (LLVMFindFunction(EE, Name, O -> out[0] = O)) {
            throw new LLVMException("Funcfion \"" + Name + "\" not found");
        }
        return out[0];
    }

    /* package-private */
    static long /* void* */ nLLVMRecompileAndRelinkFunction(LLVMExecutionEngineRef EE, LLVMValueRef Fn) {
        return Native.INSTANCE.LLVMRecompileAndRelinkFunction(EE.value(), Fn.value());
    }

    // Port-added
    public static MemorySegment LLVMRecompileAndRelinkFunction(LLVMExecutionEngineRef EE, LLVMValueRef Fn) {
        return MemorySegment.ofAddress(nLLVMRecompileAndRelinkFunction(EE, Fn));
    }

    public static LLVMTargetDataRef LLVMGetExecutionEngineTargetData(LLVMExecutionEngineRef EE) {
        return LLVMTargetDataRef.ofNullable(Native.INSTANCE.LLVMGetExecutionEngineTargetData(EE.value()));
    }

    public static LLVMTargetMachineRef LLVMGetExecutionEngineTargetMachine(LLVMExecutionEngineRef EE) {
        return LLVMTargetMachineRef.ofNullable(Native.INSTANCE.LLVMGetExecutionEngineTargetMachine(EE.value()));
    }

    /* package-private */
    static void nLLVMAddGlobalMapping(LLVMExecutionEngineRef EE, LLVMValueRef Global, long /* void* */ Addr) {
        Native.INSTANCE.LLVMAddGlobalMapping(EE.value(), Global.value(), Addr);
    }

    // Port-added
    public static void LLVMAddGlobalMapping(LLVMExecutionEngineRef EE, LLVMValueRef Global, MemorySegment Addr) {
        nLLVMAddGlobalMapping(EE, Global, Addr.nativeAddress());
    }

    /* package-private */
    static long /* void* */ nLLVMGetPointerToGlobal(LLVMExecutionEngineRef EE, LLVMValueRef Global) {
        return Native.INSTANCE.LLVMGetPointerToGlobal(EE.value(), Global.value());
    }

    // Port-added
    public static MemorySegment LLVMGetPointerToGlobal(LLVMExecutionEngineRef EE, LLVMValueRef Global) {
        return MemorySegment.ofAddress(nLLVMGetPointerToGlobal(EE, Global));
    }

    /* package-private */
    static long /* uint64_t */ nLLVMGetGlobalValueAddress(LLVMExecutionEngineRef EE, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return Native.INSTANCE.LLVMGetGlobalValueAddress(EE.value(), c_Name.nativeAddress());
        }
    }

    // Port-added
    public static MemorySegment LLVMGetGlobalValueAddress(LLVMExecutionEngineRef EE, String Name) {
        return MemorySegment.ofAddress(nLLVMGetGlobalValueAddress(EE, Name));
    }

    /* package-private */
    static long /* uint64_t */ nLLVMGetFunctionAddress(LLVMExecutionEngineRef EE, String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return Native.INSTANCE.LLVMGetFunctionAddress(EE.value(), c_Name.nativeAddress());
        }
    }

    // Port-added
    public static MemorySegment LLVMGetFunctionAddress(LLVMExecutionEngineRef EE, String Name) {
        return MemorySegment.ofAddress(nLLVMGetFunctionAddress(EE, Name));
    }

    /*===-- Operations on memory managers -------------------------------------===*/

    //TODO
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

package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm._Utils.Symbol;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;

public class Initialization {
    private enum Function implements Symbol {
        LLVMInitializeCore(void.class, LLVMPassRegistryRef),
        LLVMInitializeTransformUtils(void.class, LLVMPassRegistryRef),
        LLVMInitializeScalarOpts(void.class, LLVMPassRegistryRef),
        LLVMInitializeObjCARCOpts(void.class, LLVMPassRegistryRef),
        LLVMInitializeVectorization(void.class, LLVMPassRegistryRef),
        LLVMInitializeInstCombine(void.class, LLVMPassRegistryRef),
        LLVMInitializeIPO(void.class, LLVMPassRegistryRef),
        LLVMInitializeInstrumentation(void.class, LLVMPassRegistryRef),
        LLVMInitializeAnalysis(void.class, LLVMPassRegistryRef),
        LLVMInitializeIPA(void.class, LLVMPassRegistryRef),
        LLVMInitializeCodeGen(void.class, LLVMPassRegistryRef),
        LLVMInitializeTarget(void.class, LLVMPassRegistryRef);

        static {
            _Utils.processSymbols(LLVM, LLVM_SCOPE, Function.values());
        }

        private final MethodType type;

        private long native_symbol;
        private MethodHandle handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(long native_symbol) {
            this.native_symbol = native_symbol;
        }

        @Override
        public void setHandle(MethodHandle handle) {
            this.handle = handle;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle);
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + native_symbol +
                    ", handle=" + handle + '}';
        }
    }

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeCore.handle().invoke(R.value()));
    }

    public static void LLVMInitializeTransformUtils(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeTransformUtils.handle().invoke(R.value()));
    }

    public static void LLVMInitializeScalarOpts(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeScalarOpts.handle().invoke(R.value()));
    }

    public static void LLVMInitializeObjCARCOpts(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeObjCARCOpts.handle().invoke(R.value()));
    }

    public static void LLVMInitializeVectorization(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeVectorization.handle().invoke(R.value()));
    }

    public static void LLVMInitializeInstCombine(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeInstCombine.handle().invoke(R.value()));
    }

    public static void LLVMInitializeIPO(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeIPO.handle().invoke(R.value()));
    }

    public static void LLVMInitializeInstrumentation(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeInstrumentation.handle().invoke(R.value()));
    }

    public static void LLVMInitializeAnalysis(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeAnalysis.handle().invoke(R.value()));
    }

    public static void LLVMInitializeIPA(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeIPA.handle().invoke(R.value()));
    }

    public static void LLVMInitializeCodeGen(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeCodeGen.handle().invoke(R.value()));
    }

    public static void LLVMInitializeTarget(LLVMPassRegistryRef R) {
        nothrows_run(() -> Function.LLVMInitializeTarget.handle().invoke(R.value()));
    }
}

package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm.Types.cLLVMPassRegistryRef;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleLinker.processSymbol;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Supplier;

public class Initialization {
    private enum Function {
        LLVMInitializeCore(void.class, cLLVMPassRegistryRef),
        LLVMInitializeTransformUtils(void.class, cLLVMPassRegistryRef),
        LLVMInitializeScalarOpts(void.class, cLLVMPassRegistryRef),
        LLVMInitializeObjCARCOpts(void.class, cLLVMPassRegistryRef),
        LLVMInitializeVectorization(void.class, cLLVMPassRegistryRef),
        LLVMInitializeInstCombine(void.class, cLLVMPassRegistryRef),
        LLVMInitializeIPO(void.class, cLLVMPassRegistryRef),
        LLVMInitializeInstrumentation(void.class, cLLVMPassRegistryRef),
        LLVMInitializeAnalysis(void.class, cLLVMPassRegistryRef),
        LLVMInitializeIPA(void.class, cLLVMPassRegistryRef),
        LLVMInitializeCodeGen(void.class, cLLVMPassRegistryRef),
        LLVMInitializeTarget(void.class, cLLVMPassRegistryRef);

        private final MethodType type;
        private final Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
            this.handle = processSymbol(LLVM, LLVM_SCOPE, name(), type());
        }

        public MethodType type() {
            return type;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", handle=" + handle() + '}';
        }
    }

    /*
     * @defgroup LLVMCInitialization Initialization Routines
     * @ingroup LLVMC
     *
     * This module contains routines used to initialize the LLVM system.
     */

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

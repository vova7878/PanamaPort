package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMContextCreate;
import static com.v7878.llvm.Core.LLVMDoubleTypeInContext;
import static com.v7878.llvm.Core.LLVMFP128TypeInContext;
import static com.v7878.llvm.Core.LLVMFloatTypeInContext;
import static com.v7878.llvm.Core.LLVMHalfTypeInContext;
import static com.v7878.llvm.Core.LLVMInt128TypeInContext;
import static com.v7878.llvm.Core.LLVMInt16TypeInContext;
import static com.v7878.llvm.Core.LLVMInt1TypeInContext;
import static com.v7878.llvm.Core.LLVMInt32TypeInContext;
import static com.v7878.llvm.Core.LLVMInt64TypeInContext;
import static com.v7878.llvm.Core.LLVMInt8TypeInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMVoidTypeInContext;
import static com.v7878.llvm.Extra.LLVMGetHostCPUFeatures;
import static com.v7878.llvm.Extra.LLVMGetHostCPUName;
import static com.v7878.llvm.Extra.LLVMGetHostTriple;
import static com.v7878.llvm.Target.LLVMInitializeNativeAsmParser;
import static com.v7878.llvm.Target.LLVMInitializeNativeAsmPrinter;
import static com.v7878.llvm.Target.LLVMInitializeNativeDisassembler;
import static com.v7878.llvm.Target.LLVMInitializeNativeTarget;
import static com.v7878.llvm.Target.LLVMInitializeNativeTargetInfo;
import static com.v7878.llvm.Target.LLVMInitializeNativeTargetMC;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault;
import static com.v7878.llvm.TargetMachine.LLVMCodeModel.LLVMCodeModelDefault;
import static com.v7878.llvm.TargetMachine.LLVMCreateTargetMachine;
import static com.v7878.llvm.TargetMachine.LLVMGetTargetFromTriple;
import static com.v7878.llvm.TargetMachine.LLVMRelocMode.LLVMRelocDefault;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm.TargetMachine.LLVMCodeGenOptLevel;
import com.v7878.llvm.TargetMachine.LLVMCodeModel;
import com.v7878.llvm.TargetMachine.LLVMRelocMode;
import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
import com.v7878.llvm.TargetMachine.LLVMTargetRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;

public class LLVMGlobals {
    public static final String HOST_TRIPLE = LLVMGetHostTriple();
    public static final String HOST_CPU_NAME = LLVMGetHostCPUName();
    public static final String HOST_CPU_FEATURES = LLVMGetHostCPUFeatures();

    static {
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeTargetInfo();
        LLVMInitializeNativeTargetMC();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeDisassembler();
    }

    public static final LLVMTargetRef HOST_TARGET = nothrows_run(() -> LLVMGetTargetFromTriple(HOST_TRIPLE));

    public static LLVMContextRef newContext() {
        return LLVMContextCreate();
    }

    public static LLVMTargetMachineRef newHostMachine(
            LLVMCodeGenOptLevel Level, LLVMRelocMode Reloc, LLVMCodeModel CodeModel) {
        return LLVMCreateTargetMachine(HOST_TARGET, HOST_TRIPLE, HOST_CPU_NAME,
                HOST_CPU_FEATURES, Level, Reloc, CodeModel);
    }

    public static LLVMTargetMachineRef newDefaultMachine() {
        return newHostMachine(LLVMCodeGenLevelDefault, LLVMRelocDefault, LLVMCodeModelDefault);
    }

    public static LLVMTypeRef ptr_t(LLVMTypeRef type) {
        return LLVMPointerType(type, 0);
    }

    public static LLVMTypeRef void_t(LLVMContextRef context) {
        return LLVMVoidTypeInContext(context);
    }

    public static LLVMTypeRef void_ptr_t(LLVMContextRef context) {
        return ptr_t(void_t(context));
    }

    public static LLVMTypeRef int1_t(LLVMContextRef context) {
        return LLVMInt1TypeInContext(context);
    }

    public static LLVMTypeRef int8_t(LLVMContextRef context) {
        return LLVMInt8TypeInContext(context);
    }

    public static LLVMTypeRef int16_t(LLVMContextRef context) {
        return LLVMInt16TypeInContext(context);
    }

    public static LLVMTypeRef int32_t(LLVMContextRef context) {
        return LLVMInt32TypeInContext(context);
    }

    public static LLVMTypeRef int64_t(LLVMContextRef context) {
        return LLVMInt64TypeInContext(context);
    }

    public static LLVMTypeRef int128_t(LLVMContextRef context) {
        return LLVMInt128TypeInContext(context);
    }

    public static LLVMTypeRef intptr_t(LLVMContextRef context) {
        // TODO?: get from target
        return IS64BIT ? int64_t(context) : int32_t(context);
    }

    public static LLVMTypeRef half_t(LLVMContextRef context) {
        return LLVMHalfTypeInContext(context);
    }

    public static LLVMTypeRef float_t(LLVMContextRef context) {
        return LLVMFloatTypeInContext(context);
    }

    public static LLVMTypeRef double_t(LLVMContextRef context) {
        return LLVMDoubleTypeInContext(context);
    }

    public static LLVMTypeRef fp128_t(LLVMContextRef context) {
        return LLVMFP128TypeInContext(context);
    }
}

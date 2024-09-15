package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMContextCreate;
import static com.v7878.llvm.ErrorHandling.LLVMEnablePrettyStackTrace;
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
import static com.v7878.unsafe.Utils.DEBUG_BUILD;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm.Core;
import com.v7878.llvm.TargetMachine.LLVMCodeGenOptLevel;
import com.v7878.llvm.TargetMachine.LLVMCodeModel;
import com.v7878.llvm.TargetMachine.LLVMRelocMode;
import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
import com.v7878.llvm.TargetMachine.LLVMTargetRef;
import com.v7878.llvm.Types.LLVMContextRef;

public class LLVMGlobals {
    static {
        if (DEBUG_BUILD) {
            LLVMEnablePrettyStackTrace();
        }

        LLVMInitializeNativeTarget();
        LLVMInitializeNativeTargetInfo();
        LLVMInitializeNativeTargetMC();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeDisassembler();
    }

    public static final String HOST_TRIPLE = LLVMGetHostTriple();
    public static final String HOST_CPU_NAME = LLVMGetHostCPUName();
    public static final String HOST_CPU_FEATURES = LLVMGetHostCPUFeatures();
    public static final LLVMTargetRef HOST_TARGET = nothrows_run(() -> LLVMGetTargetFromTriple(HOST_TRIPLE));
    public static final LLVMContextRef GLOBAL_CONTEXT = nothrows_run(Core::LLVMGetGlobalContext);

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
}

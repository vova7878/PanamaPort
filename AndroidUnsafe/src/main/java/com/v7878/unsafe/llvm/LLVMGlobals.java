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
import static com.v7878.llvm.TargetMachine.LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive;
import static com.v7878.llvm.TargetMachine.LLVMCodeModel.LLVMCodeModelDefault;
import static com.v7878.llvm.TargetMachine.LLVMCreateTargetMachine;
import static com.v7878.llvm.TargetMachine.LLVMGetTargetFromTriple;
import static com.v7878.llvm.TargetMachine.LLVMRelocMode.LLVMRelocDefault;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm.TargetMachine.LLVMTargetMachineRef;
import com.v7878.llvm.TargetMachine.LLVMTargetRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;

// Note: Only for internal usage
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

    // Note: You shouldn`t dispose these objects
    public static final LLVMContextRef DEFAULT_CONTEXT = LLVMContextCreate();
    public static final LLVMTargetRef DEFAULT_TARGET = nothrows_run(() -> LLVMGetTargetFromTriple(HOST_TRIPLE));
    public static final LLVMTargetMachineRef DEFAULT_MACHINE = LLVMCreateTargetMachine(
            DEFAULT_TARGET, HOST_TRIPLE, HOST_CPU_NAME, HOST_CPU_FEATURES,
            LLVMCodeGenLevelAggressive, LLVMRelocDefault, LLVMCodeModelDefault);

    public static final LLVMTypeRef VOID_T = LLVMVoidTypeInContext(DEFAULT_CONTEXT);

    public static final LLVMTypeRef VOID_PTR_T = LLVMPointerType(VOID_T, 0);

    public static final LLVMTypeRef INT1_T = LLVMInt1TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INT8_T = LLVMInt8TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INT16_T = LLVMInt16TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INT32_T = LLVMInt32TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INT64_T = LLVMInt64TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INT128_T = LLVMInt128TypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef INTPTR_T = IS64BIT ? INT64_T : INT32_T;

    public static final LLVMTypeRef HALF_T = LLVMHalfTypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef FLOAT_T = LLVMFloatTypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef DOUBLE_T = LLVMDoubleTypeInContext(DEFAULT_CONTEXT);
    public static final LLVMTypeRef FP128_T = LLVMFP128TypeInContext(DEFAULT_CONTEXT);
}

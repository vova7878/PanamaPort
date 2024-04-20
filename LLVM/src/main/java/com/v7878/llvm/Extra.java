package com.v7878.llvm;

import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;

public final class Extra {
    private Extra() {
    }

    public static String LLVMGetHostTriple() {
        // TODO
        return LLVMGetHostCPUName() + "-unknown-linux-android";
    }

    public static String LLVMGetHostCPUName() {
        // TODO _ZN4llvm3sys14getHostCPUNameEv
        return switch (CURRENT_INSTRUCTION_SET) {
            case X86 -> "i386";
            case X86_64 -> "x86_64";
            case ARM -> "arm";
            case ARM64 -> "aarch64";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };
    }

    public static String LLVMGetHostCPUFeatures() {
        // TODO _ZN4llvm3sys18getHostCPUFeaturesERNS_9StringMapIbNS_15MallocAllocatorEEE
        if (CURRENT_INSTRUCTION_SET == X86)
            return "+x87,+mmx,+sse,+sse2,+sse3,+ssse3";
        if (CURRENT_INSTRUCTION_SET == X86_64)
            return "+x87,+mmx,+sse,+sse2,+sse3,+ssse3,+sse4.1,+sse4.2,+popcnt";
        if (CURRENT_INSTRUCTION_SET == ARM64)
            return "+fp-armv8,+neon,+v8a,+reserve-x18";
        if (CURRENT_INSTRUCTION_SET == ARM)
            return "+armv7-a,+neon,-thumb-mode,+soft-float-abi";
        return "";
    }
}

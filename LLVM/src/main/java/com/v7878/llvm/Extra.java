package com.v7878.llvm;

import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;

public final class Extra {
    private Extra() {
    }

    public static String LLVMGetHostTriple() {
        // TODO _ZN4llvm3sys16getProcessTripleEv
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
        return switch (CURRENT_INSTRUCTION_SET) {
            case X86 -> "+x87,+mmx,+sse,+sse2,+sse3,+ssse3";
            case X86_64 -> "+x87,+mmx,+sse,+sse2,+sse3,+ssse3,+sse4.1,+sse4.2,+popcnt";
            case ARM -> "+armv7-a,+neon,-thumb-mode,+soft-float-abi";
            case ARM64 -> "+fp-armv8,+neon,+v8a,+reserve-x18";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };
    }
}

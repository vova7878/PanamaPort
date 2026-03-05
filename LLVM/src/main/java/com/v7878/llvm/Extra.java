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
            case X86 -> "i686";
            case X86_64 -> "x86_64";
            case ARM -> "armv7a";
            case ARM64 -> "aarch64";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };
    }

    public static String LLVMGetHostCPUFeatures() {
        // TODO _ZN4llvm3sys18getHostCPUFeaturesERNS_9StringMapIbNS_15MallocAllocatorEEE
        return switch (CURRENT_INSTRUCTION_SET) {
            // +cmov,+cx8,+mmx,+sse,+sse2,+sse3,+ssse3,+x87
            case X86 -> "+x87,+mmx,+sse,+sse2,+sse3,+ssse3";
            // +cmov,+crc32,+cx16,+cx8,+fxsr,+mmx,+popcnt,+sse,+sse2,+sse3,+sse4.1,+sse4.2,+ssse3,+x87
            case X86_64 -> "+x87,+mmx,+sse,+sse2,+sse3,+ssse3,+sse4.1,+sse4.2,+popcnt";
            // +armv7-a,+d32,+dsp,+fp64,+neon,+read-tp-tpidruro,+vfp2,+vfp2sp,+vfp3,+vfp3d16,+vfp3d16sp,+vfp3sp,-aes,-fp-armv8,-fp-armv8d16,-fp-armv8d16sp,-fp-armv8sp,-fp16,-fp16fml,-fullfp16,-sha2,-thumb-mode,-vfp4,-vfp4d16,-vfp4d16sp,-vfp4sp
            case ARM -> "+armv7-a,+neon,-thumb-mode,+soft-float-abi";
            // +fix-cortex-a53-835769,+fp-armv8,+neon,+outline-atomics,+v8a
            case ARM64 -> "+fp-armv8,+neon,+v8a,+reserve-x18";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };
    }
}

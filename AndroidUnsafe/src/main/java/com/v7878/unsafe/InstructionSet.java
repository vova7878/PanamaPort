package com.v7878.unsafe;

import static com.v7878.unsafe.VM.getCurrentInstructionSet;

public enum InstructionSet {
    ARM(8),
    ARM64(16),
    X86(16),
    X86_64(16),
    RISCV64(16);

    public static final InstructionSet CURRENT_INSTRUCTION_SET;

    static {
        String iset = getCurrentInstructionSet();
        switch (iset) {
            case "arm" -> CURRENT_INSTRUCTION_SET = InstructionSet.ARM;
            case "arm64" -> CURRENT_INSTRUCTION_SET = InstructionSet.ARM64;
            case "x86" -> CURRENT_INSTRUCTION_SET = InstructionSet.X86;
            case "x86_64" -> CURRENT_INSTRUCTION_SET = InstructionSet.X86_64;
            case "riscv64" -> CURRENT_INSTRUCTION_SET = InstructionSet.RISCV64;
            default -> throw new IllegalStateException("unsupported instruction set: " + iset);
        }
    }

    private final int code_alignment;

    InstructionSet(int code_alignment) {
        this.code_alignment = code_alignment;
    }

    public int codeAlignment() {
        return code_alignment;
    }
}

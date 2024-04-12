package com.v7878.unsafe;

public enum InstructionSet {
    ARM(8, 8),
    ARM64(16, 8),
    X86(16, 4),
    X86_64(16, 8),
    RISCV64(16, 8);

    public static final InstructionSet CURRENT_INSTRUCTION_SET;

    static {
        String iset = VM.getCurrentInstructionSet();
        CURRENT_INSTRUCTION_SET = switch (iset) {
            case "arm" -> InstructionSet.ARM;
            case "arm64" -> InstructionSet.ARM64;
            case "x86" -> InstructionSet.X86;
            case "x86_64" -> InstructionSet.X86_64;
            case "riscv64" -> InstructionSet.RISCV64;
            default -> throw new IllegalStateException("unsupported instruction set: " + iset);
        };
    }

    private final int code_alignment;
    private final int alignof_long_long;
    private final int alignof_double;

    InstructionSet(int code_alignment, int alignof_ll_and_d) {
        this.code_alignment = code_alignment;
        this.alignof_long_long = alignof_ll_and_d;
        this.alignof_double = alignof_ll_and_d;
    }

    public int codeAlignment() {
        return code_alignment;
    }

    public int alignofLongLong() {
        return alignof_long_long;
    }

    public int alignofDouble() {
        return alignof_double;
    }
}

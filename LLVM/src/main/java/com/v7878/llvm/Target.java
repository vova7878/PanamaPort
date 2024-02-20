package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm.Types.cLLVMContextRef;
import static com.v7878.llvm.Types.cLLVMModuleRef;
import static com.v7878.llvm.Types.cLLVMPassManagerRef;
import static com.v7878.llvm.Types.cLLVMTypeRef;
import static com.v7878.llvm.Types.cLLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.llvm._Utils.UNSIGNED_LONG_LONG;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.unsafe.NativeCodeBlob.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM64;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86_64;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMString;
import com.v7878.unsafe.foreign.SimpleBulkLinker;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Target {

    /*
     * @defgroup LLVMCTarget Target information
     */

    static final Class<?> cLLVMTargetDataRef = VOID_PTR;
    static final Class<?> cLLVMTargetLibraryInfoRef = VOID_PTR;

    public static final class LLVMTargetDataRef extends Types.AddressValue {

        LLVMTargetDataRef(long value) {
            super(value);
        }
    }

    public static final class LLVMTargetLibraryInfoRef extends Types.AddressValue {
        LLVMTargetLibraryInfoRef(long value) {
            super(value);
        }
    }

    static final Class<?> cLLVMByteOrdering = ENUM;

    public enum LLVMByteOrdering {

        LLVMBigEndian,
        LLVMLittleEndian;

        public int value() {
            return ordinal();
        }

        public static LLVMByteOrdering of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    private enum Function implements SymbolHolder2 {
        // x86 and x86_64
        LLVMInitializeX86TargetInfo(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),
        LLVMInitializeX86TargetMC(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),
        LLVMInitializeX86Target(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),
        LLVMInitializeX86Disassembler(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),
        LLVMInitializeX86AsmParser(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),
        LLVMInitializeX86AsmPrinter(CURRENT_INSTRUCTION_SET == X86 || CURRENT_INSTRUCTION_SET == X86_64, void.class),

        // arm
        LLVMInitializeARMTargetInfo(CURRENT_INSTRUCTION_SET == ARM, void.class),
        LLVMInitializeARMTargetMC(CURRENT_INSTRUCTION_SET == ARM, void.class),
        LLVMInitializeARMTarget(CURRENT_INSTRUCTION_SET == ARM, void.class),
        LLVMInitializeARMDisassembler(CURRENT_INSTRUCTION_SET == ARM, void.class),
        LLVMInitializeARMAsmParser(CURRENT_INSTRUCTION_SET == ARM, void.class),
        LLVMInitializeARMAsmPrinter(CURRENT_INSTRUCTION_SET == ARM, void.class),

        // aarch64
        LLVMInitializeAArch64TargetInfo(CURRENT_INSTRUCTION_SET == ARM64, void.class),
        LLVMInitializeAArch64TargetMC(CURRENT_INSTRUCTION_SET == ARM64, void.class),
        LLVMInitializeAArch64Target(CURRENT_INSTRUCTION_SET == ARM64, void.class),
        LLVMInitializeAArch64Disassembler(CURRENT_INSTRUCTION_SET == ARM64, void.class),
        LLVMInitializeAArch64AsmParser(CURRENT_INSTRUCTION_SET == ARM64, void.class),
        LLVMInitializeAArch64AsmPrinter(CURRENT_INSTRUCTION_SET == ARM64, void.class),

        // common
        LLVMGetModuleDataLayout(cLLVMTargetDataRef, cLLVMModuleRef),
        LLVMSetModuleDataLayout(void.class, cLLVMModuleRef, cLLVMTargetDataRef),
        LLVMCreateTargetData(cLLVMTargetDataRef, CONST_CHAR_PTR),
        LLVMDisposeTargetData(void.class, cLLVMTargetDataRef),
        LLVMAddTargetLibraryInfo(void.class, cLLVMTargetLibraryInfoRef, cLLVMPassManagerRef),
        LLVMCopyStringRepOfTargetData(CHAR_PTR, cLLVMTargetDataRef),
        LLVMByteOrder(cLLVMByteOrdering, cLLVMTargetDataRef),
        LLVMPointerSize(UNSIGNED_INT, cLLVMTargetDataRef),
        LLVMPointerSizeForAS(UNSIGNED_INT, cLLVMTargetDataRef, UNSIGNED_INT),
        LLVMIntPtrType(cLLVMTypeRef, cLLVMTargetDataRef),
        LLVMIntPtrTypeForAS(cLLVMTypeRef, cLLVMTargetDataRef, UNSIGNED_INT),
        LLVMIntPtrTypeInContext(cLLVMTypeRef, cLLVMContextRef, cLLVMTargetDataRef),
        LLVMIntPtrTypeForASInContext(cLLVMTypeRef, cLLVMContextRef, cLLVMTargetDataRef, UNSIGNED_INT),
        LLVMSizeOfTypeInBits(UNSIGNED_LONG_LONG, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMStoreSizeOfType(UNSIGNED_LONG_LONG, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMABISizeOfType(UNSIGNED_LONG_LONG, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMABIAlignmentOfType(UNSIGNED_INT, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMCallFrameAlignmentOfType(UNSIGNED_INT, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMPreferredAlignmentOfType(UNSIGNED_INT, cLLVMTargetDataRef, cLLVMTypeRef),
        LLVMPreferredAlignmentOfGlobal(UNSIGNED_INT, cLLVMTargetDataRef, cLLVMValueRef),
        LLVMElementAtOffset(UNSIGNED_INT, cLLVMTargetDataRef, cLLVMTypeRef, UNSIGNED_LONG_LONG),
        LLVMOffsetOfElement(UNSIGNED_LONG_LONG, cLLVMTargetDataRef, cLLVMTypeRef, UNSIGNED_INT);

        static {
            SimpleBulkLinker.processSymbols(LLVM, LLVM_SCOPE, Arrays.stream(Function.values())
                    .filter(f -> f.is_supperted).toArray(Function[]::new));
        }

        private final boolean is_supperted;
        private final MethodType type;

        private LongSupplier symbol;
        private Supplier<MethodHandle> handle;

        Function(boolean is_supperted, Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
            this.is_supperted = is_supperted;
        }

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
            this.is_supperted = true;
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(LongSupplier symbol) {
            this.symbol = symbol;
        }

        @Override
        public void setHandle(Supplier<MethodHandle> handle) {
            this.handle = handle;
        }

        public long symbol() {
            return symbol.getAsLong();
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + symbol() +
                    ", handle=" + handle() + '}';
        }
    }


    public static void LLVMInitializeNativeTargetInfo() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86TargetInfo.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64TargetInfo.handle();
            case ARM -> tmp = Function.LLVMInitializeARMTargetInfo.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    public static void LLVMInitializeNativeTargetMC() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86TargetMC.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64TargetMC.handle();
            case ARM -> tmp = Function.LLVMInitializeARMTargetMC.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    public static void LLVMInitializeNativeTarget() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86Target.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64Target.handle();
            case ARM -> tmp = Function.LLVMInitializeARMTarget.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    public static void LLVMInitializeNativeDisassembler() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86Disassembler.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64Disassembler.handle();
            case ARM -> tmp = Function.LLVMInitializeARMDisassembler.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    public static void LLVMInitializeNativeAsmParser() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86AsmParser.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64AsmParser.handle();
            case ARM -> tmp = Function.LLVMInitializeARMAsmParser.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    public static void LLVMInitializeNativeAsmPrinter() {
        MethodHandle tmp;
        switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> tmp = Function.LLVMInitializeX86AsmPrinter.handle();
            case ARM64 -> tmp = Function.LLVMInitializeAArch64AsmPrinter.handle();
            case ARM -> tmp = Function.LLVMInitializeARMAsmPrinter.handle();
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        }
        nothrows_run(() -> tmp.invoke());
    }

    /*===-- Target Data -------------------------------------------------------===*/

    /**
     * Obtain the data layout for a module.
     */
    public static LLVMTargetDataRef LLVMGetModuleDataLayout(LLVMModuleRef M) {
        return nothrows_run(() -> new LLVMTargetDataRef((long) Function.LLVMGetModuleDataLayout.handle().invoke(M.value())));
    }

    /**
     * Set the data layout for a module.
     */
    public static void LLVMSetModuleDataLayout(LLVMModuleRef M, LLVMTargetDataRef DL) {
        nothrows_run(() -> Function.LLVMSetModuleDataLayout.handle().invoke(M.value(), DL.value()));
    }

    /**
     * Creates target data from a target layout string.
     */
    public static LLVMTargetDataRef LLVMCreateTargetData(String StringRep) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_StringRep = allocString(arena, StringRep);
            return nothrows_run(() -> new LLVMTargetDataRef((long) Function.LLVMCreateTargetData.handle().invoke(c_StringRep.address())));
        }
    }

    /**
     * Deallocates a TargetData.
     */
    public static void LLVMDisposeTargetData(LLVMTargetDataRef TD) {
        nothrows_run(() -> Function.LLVMDisposeTargetData.handle().invoke(TD.value()));
    }

    /**
     * Adds target library information to a pass manager. This does not take
     * ownership of the target library info.
     */
    public static void LLVMAddTargetLibraryInfo(LLVMTargetLibraryInfoRef TLI, LLVMPassManagerRef PM) {
        nothrows_run(() -> Function.LLVMAddTargetLibraryInfo.handle().invoke(TLI.value(), PM.value()));
    }

    /**
     * Converts target data to a target layout string. The string must be disposed
     * with LLVMDisposeMessage.
     */
    public static LLVMString LLVMCopyStringRepOfTargetData(LLVMTargetDataRef TD) {
        return nothrows_run(() -> new LLVMString((long) Function.LLVMCopyStringRepOfTargetData.handle().invoke(TD.value())));
    }

    /**
     * Returns the byte order of a target, either LLVMBigEndian or
     * LLVMLittleEndian.
     */
    public static LLVMByteOrdering LLVMByteOrder(LLVMTargetDataRef TD) {
        return nothrows_run(() -> LLVMByteOrdering.of((int) Function.LLVMByteOrder.handle().invoke(TD.value())));
    }

    /**
     * Returns the pointer size in bytes for a target.
     */
    public static int /* unsigned */ LLVMPointerSize(LLVMTargetDataRef TD) {
        return nothrows_run(() -> (int) Function.LLVMPointerSize.handle().invoke(TD.value()));
    }

    /**
     * Returns the pointer size in bytes for a target for a specified
     * address space.
     */
    public static int /* unsigned */ LLVMPointerSizeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return nothrows_run(() -> (int) Function.LLVMPointerSizeForAS.handle().invoke(TD.value(), AS));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     */
    public static LLVMTypeRef LLVMIntPtrType(LLVMTargetDataRef TD) {
        return nothrows_run(() -> new LLVMTypeRef((long) Function.LLVMIntPtrType.handle().invoke(TD.value())));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return nothrows_run(() -> new LLVMTypeRef((long) Function.LLVMIntPtrTypeForAS.handle().invoke(TD.value(), AS)));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     */
    public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
        return nothrows_run(() -> new LLVMTypeRef((long) Function.LLVMIntPtrTypeInContext.handle().invoke(C.value(), TD.value())));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return nothrows_run(() -> new LLVMTypeRef((long) Function.LLVMIntPtrTypeForASInContext.handle().invoke(C.value(), TD.value(), AS)));
    }

    /**
     * Computes the size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMSizeOfTypeInBits(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (long) Function.LLVMSizeOfTypeInBits.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the storage size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMStoreSizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (long) Function.LLVMStoreSizeOfType.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the ABI size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMABISizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (long) Function.LLVMABISizeOfType.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the ABI alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMABIAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (int) Function.LLVMABIAlignmentOfType.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the call frame alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMCallFrameAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (int) Function.LLVMCallFrameAlignmentOfType.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the preferred alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMPreferredAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return nothrows_run(() -> (int) Function.LLVMPreferredAlignmentOfType.handle().invoke(TD.value(), Ty.value()));
    }

    /**
     * Computes the preferred alignment of a global variable in bytes for a target.
     */
    public static int /* unsigned */ LLVMPreferredAlignmentOfGlobal(LLVMTargetDataRef TD, LLVMValueRef GlobalVar) {
        return nothrows_run(() -> (int) Function.LLVMPreferredAlignmentOfGlobal.handle().invoke(TD.value(), GlobalVar.value()));
    }

    /**
     * Computes the structure element that contains the byte offset for a target.
     */
    public static int /* unsigned */ LLVMElementAtOffset(LLVMTargetDataRef TD, LLVMTypeRef StructTy, long /* unsigned long long */ Offset) {
        return nothrows_run(() -> (int) Function.LLVMElementAtOffset.handle().invoke(TD.value(), StructTy.value(), Offset));
    }

    /**
     * Computes the byte offset of the indexed struct element for a target.
     */
    public static long /* unsigned long long */ LLVMOffsetOfElement(LLVMTargetDataRef TD, LLVMTypeRef StructTy, int /* unsigned */ Element) {
        return nothrows_run(() -> (long) Function.LLVMOffsetOfElement.handle().invoke(TD.value(), StructTy.value(), Element));
    }
}

package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.Symbol;
import static com.v7878.llvm._Utils.UNSIGNED_INT;
import static com.v7878.llvm._Utils.UNSIGNED_LONG_LONG;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.unsafe.NativeCodeBlob.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Utils.nothrows_run;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;

public class Target {

    /*
     * @defgroup LLVMCTarget Target information
     */

    static final Class<?> LLVMTargetDataRef = VOID_PTR;
    static final Class<?> LLVMTargetLibraryInfoRef = VOID_PTR;

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

    static final Class<?> LLVMByteOrdering = ENUM;

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

    private enum Function implements Symbol {
        // x86 and x86_64
        LLVMInitializeX86TargetInfo(void.class),
        LLVMInitializeX86TargetMC(void.class),
        LLVMInitializeX86Target(void.class),
        LLVMInitializeX86Disassembler(void.class),
        LLVMInitializeX86AsmParser(void.class),
        LLVMInitializeX86AsmPrinter(void.class),

        // arm
        LLVMInitializeARMTargetInfo(void.class),
        LLVMInitializeARMTargetMC(void.class),
        LLVMInitializeARMTarget(void.class),
        LLVMInitializeARMDisassembler(void.class),
        LLVMInitializeARMAsmParser(void.class),
        LLVMInitializeARMAsmPrinter(void.class),

        // aarch64
        LLVMInitializeAArch64TargetInfo(void.class),
        LLVMInitializeAArch64TargetMC(void.class),
        LLVMInitializeAArch64Target(void.class),
        LLVMInitializeAArch64Disassembler(void.class),
        LLVMInitializeAArch64AsmParser(void.class),
        LLVMInitializeAArch64AsmPrinter(void.class),

        // common
        LLVMGetModuleDataLayout(LLVMTargetDataRef, LLVMModuleRef),
        LLVMSetModuleDataLayout(void.class, LLVMModuleRef, LLVMTargetDataRef),
        LLVMCreateTargetData(LLVMTargetDataRef, CONST_CHAR_PTR),
        LLVMDisposeTargetData(void.class, LLVMTargetDataRef),
        LLVMAddTargetLibraryInfo(void.class, LLVMTargetLibraryInfoRef, LLVMPassManagerRef),
        LLVMCopyStringRepOfTargetData(CHAR_PTR, LLVMTargetDataRef),
        LLVMByteOrder(LLVMByteOrdering, LLVMTargetDataRef),
        LLVMPointerSize(UNSIGNED_INT, LLVMTargetDataRef),
        LLVMPointerSizeForAS(UNSIGNED_INT, LLVMTargetDataRef, UNSIGNED_INT),
        LLVMIntPtrType(LLVMTypeRef, LLVMTargetDataRef),
        LLVMIntPtrTypeForAS(LLVMTypeRef, LLVMTargetDataRef, UNSIGNED_INT),
        LLVMIntPtrTypeInContext(LLVMTypeRef, LLVMContextRef, LLVMTargetDataRef),
        LLVMIntPtrTypeForASInContext(LLVMTypeRef, LLVMContextRef, LLVMTargetDataRef, UNSIGNED_INT),
        LLVMSizeOfTypeInBits(UNSIGNED_LONG_LONG, LLVMTargetDataRef, LLVMTypeRef),
        LLVMStoreSizeOfType(UNSIGNED_LONG_LONG, LLVMTargetDataRef, LLVMTypeRef),
        LLVMABISizeOfType(UNSIGNED_LONG_LONG, LLVMTargetDataRef, LLVMTypeRef),
        LLVMABIAlignmentOfType(UNSIGNED_INT, LLVMTargetDataRef, LLVMTypeRef),
        LLVMCallFrameAlignmentOfType(UNSIGNED_INT, LLVMTargetDataRef, LLVMTypeRef),
        LLVMPreferredAlignmentOfType(UNSIGNED_INT, LLVMTargetDataRef, LLVMTypeRef),
        LLVMPreferredAlignmentOfGlobal(UNSIGNED_INT, LLVMTargetDataRef, LLVMValueRef),
        LLVMElementAtOffset(UNSIGNED_INT, LLVMTargetDataRef, LLVMTypeRef, UNSIGNED_LONG_LONG),
        LLVMOffsetOfElement(UNSIGNED_LONG_LONG, LLVMTargetDataRef, LLVMTypeRef, UNSIGNED_INT);

        static {
            _Utils.processSymbols(LLVM, LLVM_SCOPE, Function.values());
        }

        private final MethodType type;

        private long native_symbol;
        private MethodHandle handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(long native_symbol) {
            this.native_symbol = native_symbol;
        }

        @Override
        public void setHandle(MethodHandle handle) {
            this.handle = handle;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle);
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + native_symbol +
                    ", handle=" + handle + '}';
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

    ///**
    // * Obtain the data layout for a module.
    // */
    //public static LLVMTargetDataRef LLVMGetModuleDataLayout(LLVMModuleRef M) {
    //    return nothrows_run(() -> Function.LLVMGetModuleDataLayout.handle().invoke());
    //}
//
    ///**
    // * Set the data layout for a module.
    // */
    //public static void LLVMSetModuleDataLayout(LLVMModuleRef M, LLVMTargetDataRef DL) {
    //    return nothrows_run(() -> Function.LLVMSetModuleDataLayout.handle().invoke());
    //}
//
    ///**
    // * Creates target data from a target layout string.
    // */
    //public static LLVMTargetDataRef LLVMCreateTargetData(String StringRep) {
    //    return nothrows_run(() -> Function.LLVMCreateTargetData.handle().invoke());
    //}
//
    ///**
    // * Deallocates a TargetData.
    // */
    //public static void LLVMDisposeTargetData(LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMDisposeTargetData.handle().invoke());
    //}
//
    ///**
    // * Adds target library information to a pass manager. This does not take
    // * ownership of the target library info.
    // */
    //public static void LLVMAddTargetLibraryInfo(LLVMTargetLibraryInfoRef TLI, LLVMPassManagerRef PM) {
    //    return nothrows_run(() -> Function.LLVMAddTargetLibraryInfo.handle().invoke());
    //}
//
    ///**
    // * Converts target data to a target layout string. The string must be disposed
    // * with LLVMDisposeMessage.
    // */
    //public static LLVMString LLVMCopyStringRepOfTargetData(LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMCopyStringRepOfTargetData.handle().invoke());
    //}
//
    ///**
    // * Returns the byte order of a target, either LLVMBigEndian or
    // * LLVMLittleEndian.
    // */
    //public static LLVMByteOrdering LLVMByteOrder(LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMByteOrder.handle().invoke());
    //}
//
    ///**
    // * Returns the pointer size in bytes for a target.
    // */
    //public static int /* unsigned */ LLVMPointerSize(LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMPointerSize.handle().invoke());
    //}
//
    ///**
    // * Returns the pointer size in bytes for a target for a specified
    // * address space.
    // */
    //public static int /* unsigned */ LLVMPointerSizeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
    //    return nothrows_run(() -> Function.LLVMPointerSizeForAS.handle().invoke());
    //}
//
    ///**
    // * Returns the integer type that is the same size as a pointer on a target.
    // */
    //public static LLVMTypeRef LLVMIntPtrType(LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMIntPtrType.handle().invoke());
    //}
//
    ///**
    // * Returns the integer type that is the same size as a pointer on a target.
    // * This version allows the address space to be specified.
    // */
    //public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
    //    return nothrows_run(() -> Function.LLVMIntPtrTypeForAS.handle().invoke());
    //}
//
    ///**
    // * Returns the integer type that is the same size as a pointer on a target.
    // */
    //public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
    //    return nothrows_run(() -> Function.LLVMIntPtrTypeInContext.handle().invoke());
    //}
//
    ///**
    // * Returns the integer type that is the same size as a pointer on a target.
    // * This version allows the address space to be specified.
    // */
    //public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int /* unsigned */ AS) {
    //    return nothrows_run(() -> Function.LLVMIntPtrTypeForASInContext.handle().invoke());
    //}
//
    ///**
    // * Computes the size of a type in bytes for a target.
    // */
    //public static long /* unsigned long long */ LLVMSizeOfTypeInBits(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMSizeOfTypeInBits.handle().invoke());
    //}
//
    ///**
    // * Computes the storage size of a type in bytes for a target.
    // */
    //public static long /* unsigned long long */ LLVMStoreSizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMStoreSizeOfType.handle().invoke());
    //}
//
    ///**
    // * Computes the ABI size of a type in bytes for a target.
    // */
    //public static long /* unsigned long long */ LLVMABISizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMABISizeOfType.handle().invoke());
    //}
//
    ///**
    // * Computes the ABI alignment of a type in bytes for a target.
    // */
    //public static int /* unsigned */ LLVMABIAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMABIAlignmentOfType.handle().invoke());
    //}
//
    ///**
    // * Computes the call frame alignment of a type in bytes for a target.
    // */
    //public static int /* unsigned */ LLVMCallFrameAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMCallFrameAlignmentOfType.handle().invoke());
    //}
//
    ///**
    // * Computes the preferred alignment of a type in bytes for a target.
    // */
    //public static int /* unsigned */ LLVMPreferredAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
    //    return nothrows_run(() -> Function.LLVMPreferredAlignmentOfType.handle().invoke());
    //}
//
    ///**
    // * Computes the preferred alignment of a global variable in bytes for a target.
    // */
    //public static int /* unsigned */ LLVMPreferredAlignmentOfGlobal(LLVMTargetDataRef TD, LLVMValueRef GlobalVar) {
    //    return nothrows_run(() -> Function.LLVMPreferredAlignmentOfGlobal.handle().invoke());
    //}
//
    ///**
    // * Computes the structure element that contains the byte offset for a target.
    // */
    //public static int /* unsigned */ LLVMElementAtOffset(LLVMTargetDataRef TD, LLVMTypeRef StructTy, long /* unsigned long long */ Offset) {
    //    return nothrows_run(() -> Function.LLVMElementAtOffset.handle().invoke());
    //}
//
    ///**
    // * Computes the byte offset of the indexed struct element for a target.
    // */
    //public static long /* unsigned long long */ LLVMOffsetOfElement(LLVMTargetDataRef TD, LLVMTypeRef StructTy, int /* unsigned */ Element) {
    //    return nothrows_run(() -> Function.LLVMOffsetOfElement.handle().invoke());
    //}
}

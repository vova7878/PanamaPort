package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===-- llvm-c/Target.h - Target Lib C Iface --------------------*- C++ -*-===*/
/*                                                                            */
/* This header declares the C interface to libLLVMTarget.a, which             */
/* implements target information.                                             */
/*                                                                            */
/* Many exotic languages can interoperate with C code but have a harder time  */
/* with C++ due to name mangling. So in addition to C, this interface enables */
/* tools written in such languages.                                           */
/*                                                                            */
/*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Target {
    private Target() {
    }

    /*
     * @defgroup LLVMCTarget Target information
     */

    public static final class LLVMTargetDataRef extends AddressValue implements FineClosable {

        private LLVMTargetDataRef(long value) {
            super(value);
        }

        static LLVMTargetDataRef of(long value) {
            return value == 0 ? null : new LLVMTargetDataRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeTargetData(this);
        }
    }

    public static final class LLVMTargetLibraryInfoRef extends AddressValue implements FineClosable {

        private LLVMTargetLibraryInfoRef(long value) {
            super(value);
        }

        static LLVMTargetLibraryInfoRef of(long value) {
            return value == 0 ? null : new LLVMTargetLibraryInfoRef(value);
        }

        @Override
        public void close() {
            // TODO
            throw new UnsupportedOperationException("Not supported yet!");
        }
    }

    public enum LLVMByteOrdering {

        LLVMBigEndian,
        LLVMLittleEndian;

        int value() {
            return ordinal();
        }

        static LLVMByteOrdering of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86TargetInfo")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMTargetInfo")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64TargetInfo")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTargetInfo();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86TargetMC")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMTargetMC")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64TargetMC")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTargetMC();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86Target")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMTarget")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64Target")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTarget();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86Disassembler")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMDisassembler")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64Disassembler")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeDisassembler();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86AsmParser")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMAsmParser")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64AsmParser")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeAsmParser();

        @LibrarySymbol(conditions = @Conditions(arch = {X86, X86_64}), name = "LLVMInitializeX86AsmPrinter")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM}), name = "LLVMInitializeARMAsmPrinter")
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64}), name = "LLVMInitializeAArch64AsmPrinter")
        //TODO: @LibrarySymbol(conditions = @Conditions(arch = {RISCV64}), name = "")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeAsmPrinter();

        @LibrarySymbol(name = "LLVMGetModuleDataLayout")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetModuleDataLayout(long M);

        @LibrarySymbol(name = "LLVMSetModuleDataLayout")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMSetModuleDataLayout(long M, long DL);

        @LibrarySymbol(name = "LLVMCreateTargetData")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateTargetData(long StringRep);

        @LibrarySymbol(name = "LLVMDisposeTargetData")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeTargetData(long TD);

        @LibrarySymbol(name = "LLVMAddTargetLibraryInfo")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddTargetLibraryInfo(long TLI, long PM);

        @LibrarySymbol(name = "LLVMCopyStringRepOfTargetData")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCopyStringRepOfTargetData(long TD);

        @LibrarySymbol(name = "LLVMByteOrder")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMByteOrder(long TD);

        @LibrarySymbol(name = "LLVMPointerSize")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD})
        abstract int LLVMPointerSize(long TD);

        @LibrarySymbol(name = "LLVMPointerSizeForAS")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, INT})
        abstract int LLVMPointerSizeForAS(long TD, int AS);

        @LibrarySymbol(name = "LLVMIntPtrType")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMIntPtrType(long TD);

        @LibrarySymbol(name = "LLVMIntPtrTypeForAS")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, INT})
        abstract long LLVMIntPtrTypeForAS(long TD, int AS);

        @LibrarySymbol(name = "LLVMIntPtrTypeInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMIntPtrTypeInContext(long C, long TD);

        @LibrarySymbol(name = "LLVMIntPtrTypeForASInContext")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMIntPtrTypeForASInContext(long C, long TD, int AS);

        @LibrarySymbol(name = "LLVMSizeOfTypeInBits")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMSizeOfTypeInBits(long TD, long Ty);

        @LibrarySymbol(name = "LLVMStoreSizeOfType")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMStoreSizeOfType(long TD, long Ty);

        @LibrarySymbol(name = "LLVMABISizeOfType")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long LLVMABISizeOfType(long TD, long Ty);

        @LibrarySymbol(name = "LLVMABIAlignmentOfType")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMABIAlignmentOfType(long TD, long Ty);

        @LibrarySymbol(name = "LLVMCallFrameAlignmentOfType")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMCallFrameAlignmentOfType(long TD, long Ty);

        @LibrarySymbol(name = "LLVMPreferredAlignmentOfType")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMPreferredAlignmentOfType(long TD, long Ty);

        @LibrarySymbol(name = "LLVMPreferredAlignmentOfGlobal")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int LLVMPreferredAlignmentOfGlobal(long TD, long GlobalVar);

        @LibrarySymbol(name = "LLVMElementAtOffset")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG})
        abstract int LLVMElementAtOffset(long TD, long StructTy, long Offset);

        @LibrarySymbol(name = "LLVMOffsetOfElement")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract long LLVMOffsetOfElement(long TD, long StructTy, int Element);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, LLVM);
    }

    public static void LLVMInitializeNativeTargetInfo() {
        Native.INSTANCE.LLVMInitializeTargetInfo();
    }

    public static void LLVMInitializeNativeTargetMC() {
        Native.INSTANCE.LLVMInitializeTargetMC();
    }

    public static void LLVMInitializeNativeTarget() {
        Native.INSTANCE.LLVMInitializeTarget();
    }

    public static void LLVMInitializeNativeDisassembler() {
        Native.INSTANCE.LLVMInitializeDisassembler();
    }

    public static void LLVMInitializeNativeAsmParser() {
        Native.INSTANCE.LLVMInitializeAsmParser();
    }

    public static void LLVMInitializeNativeAsmPrinter() {
        Native.INSTANCE.LLVMInitializeAsmPrinter();
    }

    /*===-- Target Data -------------------------------------------------------===*/

    /**
     * Obtain the data layout for a module.
     */
    public static LLVMTargetDataRef LLVMGetModuleDataLayout(LLVMModuleRef M) {
        return LLVMTargetDataRef.of(Native.INSTANCE.LLVMGetModuleDataLayout(M.value()));
    }

    /**
     * Set the data layout for a module.
     */
    public static void LLVMSetModuleDataLayout(LLVMModuleRef M, LLVMTargetDataRef DL) {
        Native.INSTANCE.LLVMSetModuleDataLayout(M.value(), DL.value());
    }

    /**
     * Creates target data from a target layout string.
     */
    public static LLVMTargetDataRef LLVMCreateTargetData(String StringRep) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_StringRep = allocString(arena, StringRep);
            return LLVMTargetDataRef.of(Native.INSTANCE.LLVMCreateTargetData(c_StringRep.nativeAddress()));
        }
    }

    /**
     * Deallocates a TargetData.
     */
    public static void LLVMDisposeTargetData(LLVMTargetDataRef TD) {
        Native.INSTANCE.LLVMDisposeTargetData(TD.value());
    }

    /**
     * Adds target library information to a pass manager. This does not take
     * ownership of the target library info.
     */
    public static void LLVMAddTargetLibraryInfo(LLVMTargetLibraryInfoRef TLI, LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddTargetLibraryInfo(TLI.value(), PM.value());
    }

    /**
     * Converts target data to a target layout string.
     */
    public static String LLVMCopyStringRepOfTargetData(LLVMTargetDataRef TD) {
        return addressToLLVMString(Native.INSTANCE.LLVMCopyStringRepOfTargetData(TD.value()));
    }

    /**
     * Returns the byte order of a target, either LLVMBigEndian or
     * LLVMLittleEndian.
     */
    public static LLVMByteOrdering LLVMByteOrder(LLVMTargetDataRef TD) {
        return LLVMByteOrdering.of(Native.INSTANCE.LLVMByteOrder(TD.value()));
    }

    /**
     * Returns the pointer size in bytes for a target.
     */
    public static int /* unsigned */ LLVMPointerSize(LLVMTargetDataRef TD) {
        return Native.INSTANCE.LLVMPointerSize(TD.value());
    }

    /**
     * Returns the pointer size in bytes for a target for a specified
     * address space.
     */
    public static int /* unsigned */ LLVMPointerSizeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return Native.INSTANCE.LLVMPointerSizeForAS(TD.value(), AS);
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     */
    public static LLVMTypeRef LLVMIntPtrType(LLVMTargetDataRef TD) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntPtrType(TD.value()));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntPtrTypeForAS(TD.value(), AS));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     */
    public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntPtrTypeInContext(C.value(), TD.value()));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return LLVMTypeRef.of(Native.INSTANCE.LLVMIntPtrTypeForASInContext(C.value(), TD.value(), AS));
    }

    /**
     * Computes the size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMSizeOfTypeInBits(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMSizeOfTypeInBits(TD.value(), Ty.value());
    }

    /**
     * Computes the storage size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMStoreSizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMStoreSizeOfType(TD.value(), Ty.value());
    }

    /**
     * Computes the ABI size of a type in bytes for a target.
     */
    public static long /* unsigned long long */ LLVMABISizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMABISizeOfType(TD.value(), Ty.value());
    }

    /**
     * Computes the ABI alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMABIAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMABIAlignmentOfType(TD.value(), Ty.value());
    }

    /**
     * Computes the call frame alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMCallFrameAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMCallFrameAlignmentOfType(TD.value(), Ty.value());
    }

    /**
     * Computes the preferred alignment of a type in bytes for a target.
     */
    public static int /* unsigned */ LLVMPreferredAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        return Native.INSTANCE.LLVMPreferredAlignmentOfType(TD.value(), Ty.value());
    }

    /**
     * Computes the preferred alignment of a global variable in bytes for a target.
     */
    public static int /* unsigned */ LLVMPreferredAlignmentOfGlobal(LLVMTargetDataRef TD, LLVMValueRef GlobalVar) {
        return Native.INSTANCE.LLVMPreferredAlignmentOfGlobal(TD.value(), GlobalVar.value());
    }

    /**
     * Computes the structure element that contains the byte offset for a target.
     */
    public static int /* unsigned */ LLVMElementAtOffset(LLVMTargetDataRef TD, LLVMTypeRef StructTy, long /* unsigned long long */ Offset) {
        return Native.INSTANCE.LLVMElementAtOffset(TD.value(), StructTy.value(), Offset);
    }

    /**
     * Computes the byte offset of the indexed struct element for a target.
     */
    public static long /* unsigned long long */ LLVMOffsetOfElement(LLVMTargetDataRef TD, LLVMTypeRef StructTy, int /* unsigned */ Element) {
        return Native.INSTANCE.LLVMOffsetOfElement(TD.value(), StructTy.value(), Element);
    }
}

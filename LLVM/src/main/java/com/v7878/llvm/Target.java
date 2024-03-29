package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;

public class Target {

    /*
     * @defgroup LLVMCTarget Target information
     */

    public static final class LLVMTargetDataRef extends AddressValue implements FineClosable {

        private LLVMTargetDataRef(long value) {
            super(value);
        }

        public static LLVMTargetDataRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMTargetDataRef of 0");
            }
            return new LLVMTargetDataRef(value);
        }

        public static LLVMTargetDataRef ofNullable(long value) {
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

        public static LLVMTargetLibraryInfoRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMTargetLibraryInfoRef of 0");
            }
            return new LLVMTargetLibraryInfoRef(value);
        }

        public static LLVMTargetLibraryInfoRef ofNullable(long value) {
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

    @Keep
    private abstract static class Native {

        private static final String Arch = switch (CURRENT_INSTRUCTION_SET) {
            case X86, X86_64 -> "X86";
            case ARM64 -> "AArch64";
            case ARM -> "ARM";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };

        private static final Arena SCOPE = Arena.ofAuto();

        @SymbolGenerator(method = "genLLVMInitializeTargetInfo")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTargetInfo();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeTargetInfo() {
            return LLVM.find("LLVMInitialize" + Arch + "TargetInfo").orElseThrow(Utils::shouldNotReachHere);
        }

        @SymbolGenerator(method = "genLLVMInitializeTargetMC")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTargetMC();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeTargetMC() {
            return LLVM.find("LLVMInitialize" + Arch + "TargetMC").orElseThrow(Utils::shouldNotReachHere);
        }

        @SymbolGenerator(method = "genLLVMInitializeTarget")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeTarget();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeTarget() {
            return LLVM.find("LLVMInitialize" + Arch + "Target").orElseThrow(Utils::shouldNotReachHere);
        }

        @SymbolGenerator(method = "genLLVMInitializeDisassembler")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeDisassembler();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeDisassembler() {
            return LLVM.find("LLVMInitialize" + Arch + "Disassembler").orElseThrow(Utils::shouldNotReachHere);
        }

        @SymbolGenerator(method = "genLLVMInitializeAsmParser")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeAsmParser();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeAsmParser() {
            return LLVM.find("LLVMInitialize" + Arch + "AsmParser").orElseThrow(Utils::shouldNotReachHere);
        }

        @SymbolGenerator(method = "genLLVMInitializeAsmPrinter")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMInitializeAsmPrinter();

        @SuppressWarnings("unused")
        private static MemorySegment genLLVMInitializeAsmPrinter() {
            return LLVM.find("LLVMInitialize" + Arch + "AsmPrinter").orElseThrow(Utils::shouldNotReachHere);
        }

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

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
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
        return LLVMTargetDataRef.ofNullable(Native.INSTANCE.LLVMGetModuleDataLayout(M.value()));
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
            return LLVMTargetDataRef.ofNullable(Native.INSTANCE.LLVMCreateTargetData(c_StringRep.nativeAddress()));
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
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntPtrType(TD.value()));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntPtrTypeForAS(TD.value(), AS));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     */
    public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntPtrTypeInContext(C.value(), TD.value()));
    }

    /**
     * Returns the integer type that is the same size as a pointer on a target.
     * This version allows the address space to be specified.
     */
    public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int /* unsigned */ AS) {
        return LLVMTypeRef.ofNullable(Native.INSTANCE.LLVMIntPtrTypeForASInContext(C.value(), TD.value(), AS));
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

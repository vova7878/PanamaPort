package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;

public class Target {
    public static final class LLVMTargetDataRef implements AutoCloseable {
        private LLVMTargetDataRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMTargetLibraryInfoRef implements AutoCloseable {
        private LLVMTargetLibraryInfoRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }


    public enum LLVMByteOrdering {

        LLVMBigEndian,
        LLVMLittleEndian;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMByteOrdering of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static void LLVMInitializeNativeTargetInfo() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeTargetMC() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeTarget() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeDisassembler() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeAsmParser() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeAsmPrinter() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMGetModuleDataLayout(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetModuleDataLayout(LLVMModuleRef M, LLVMTargetDataRef DL) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMCreateTargetData(String StringRep) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeTargetData(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddTargetLibraryInfo(LLVMTargetLibraryInfoRef TLI, LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMCopyStringRepOfTargetData(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMByteOrdering LLVMByteOrder(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMPointerSize(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMPointerSizeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrType(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int /* unsigned */ AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int /* unsigned */ AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* unsigned long long */ LLVMSizeOfTypeInBits(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* unsigned long long */ LLVMStoreSizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* unsigned long long */ LLVMABISizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMABIAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMCallFrameAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMPreferredAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMPreferredAlignmentOfGlobal(LLVMTargetDataRef TD, LLVMValueRef GlobalVar) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int /* unsigned */ LLVMElementAtOffset(LLVMTargetDataRef TD, LLVMTypeRef StructTy, long /* unsigned long long */ Offset) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* unsigned long long */ LLVMOffsetOfElement(LLVMTargetDataRef TD, LLVMTypeRef StructTy, int /* unsigned */ Element) {
        throw new UnsupportedOperationException("Stub!");
    }
}

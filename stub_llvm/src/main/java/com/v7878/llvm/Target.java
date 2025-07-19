package com.v7878.llvm;

import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;

public final class Target {
    private Target() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static final class LLVMTargetDataRef implements AutoCloseable {
        private LLVMTargetDataRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMTargetLibraryInfoRef implements AutoCloseable {
        private LLVMTargetLibraryInfoRef() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMByteOrdering {
        LLVMBigEndian,
        LLVMLittleEndian
    }

    public static int LLVMABIAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMABISizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddTargetLibraryInfo(LLVMTargetLibraryInfoRef TLI, LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMByteOrdering LLVMByteOrder(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMCallFrameAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMCopyStringRepOfTargetData(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMCreateTargetData(String StringRep) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeTargetData(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMElementAtOffset(LLVMTargetDataRef TD, LLVMTypeRef StructTy, long Offset) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMGetModuleDataLayout(LLVMModuleRef M) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeAsmParser() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeAsmPrinter() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeDisassembler() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeTarget() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeTargetInfo() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMInitializeNativeTargetMC() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrType(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeForAS(LLVMTargetDataRef TD, int AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeForASInContext(LLVMContextRef C, LLVMTargetDataRef TD, int AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTypeRef LLVMIntPtrTypeInContext(LLVMContextRef C, LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMOffsetOfElement(LLVMTargetDataRef TD, LLVMTypeRef StructTy, int Element) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMPointerSize(LLVMTargetDataRef TD) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMPointerSizeForAS(LLVMTargetDataRef TD, int AS) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMPreferredAlignmentOfGlobal(LLVMTargetDataRef TD, LLVMValueRef GlobalVar) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int LLVMPreferredAlignmentOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetModuleDataLayout(LLVMModuleRef M, LLVMTargetDataRef DL) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMSizeOfTypeInBits(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMStoreSizeOfType(LLVMTargetDataRef TD, LLVMTypeRef Ty) {
        throw new UnsupportedOperationException("Stub!");
    }
}

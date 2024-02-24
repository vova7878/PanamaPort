package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;

import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;

import java.util.function.Consumer;

public class TargetMachine {

    public static final class LLVMTargetMachineRef {
        private LLVMTargetMachineRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMTargetRef {
        private LLVMTargetRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }


    public enum LLVMCodeGenOptLevel {

        LLVMCodeGenLevelNone,
        LLVMCodeGenLevelLess,
        LLVMCodeGenLevelDefault,
        LLVMCodeGenLevelAggressive;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMCodeGenOptLevel of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMRelocMode {

        LLVMRelocDefault,
        LLVMRelocStatic,
        LLVMRelocPIC,
        LLVMRelocDynamicNoPic;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMRelocMode of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMCodeModel {

        LLVMCodeModelDefault,
        LLVMCodeModelJITDefault,
        LLVMCodeModelSmall,
        LLVMCodeModelKernel,
        LLVMCodeModelMedium,
        LLVMCodeModelLarge;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMCodeModel of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public enum LLVMCodeGenFileType {

        LLVMAssemblyFile,
        LLVMObjectFile;

        public int value() {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMCodeGenFileType of(int value) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static LLVMTargetRef LLVMGetFirstTarget() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetRef LLVMGetNextTarget(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetRef LLVMGetTargetFromName(String Name) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMGetTargetFromTriple(String Triple, Consumer<LLVMTargetRef> T, Consumer<String> ErrorMessage) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetRef LLVMGetTargetFromTriple(String Triple) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTargetName(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTargetDescription(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTargetHasJIT(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTargetHasTargetMachine(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTargetHasAsmBackend(LLVMTargetRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetMachineRef LLVMCreateTargetMachine(
            LLVMTargetRef T, String Triple, String CPU, String Features,
            LLVMCodeGenOptLevel Level, LLVMRelocMode Reloc, LLVMCodeModel CodeModel) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeTargetMachine(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetRef LLVMGetTargetMachineTarget(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTargetMachineTriple(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTargetMachineCPU(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetTargetMachineFeatureString(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMTargetDataRef LLVMCreateTargetDataLayout(LLVMTargetMachineRef T) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMSetTargetMachineAsmVerbosity(LLVMTargetMachineRef T, boolean VerboseAsm) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTargetMachineEmitToFile(
            LLVMTargetMachineRef T, LLVMModuleRef M, String Filename,
            LLVMCodeGenFileType codegen, Consumer<String> ErrorMessage) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMTargetMachineEmitToFile(
            LLVMTargetMachineRef T, LLVMModuleRef M, String Filename,
            LLVMCodeGenFileType codegen) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMTargetMachineEmitToMemoryBuffer(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMCodeGenFileType codegen,
            Consumer<String> ErrorMessage, Consumer<LLVMMemoryBufferRef> OutMemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMMemoryBufferRef LLVMTargetMachineEmitToMemoryBuffer(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMCodeGenFileType codegen) throws LLVMException {
        throw new UnsupportedOperationException("Stub!");
    }

    @Deprecated // on Android it always returns "i386-unknown-linux"
    public static String LLVMGetDefaultTargetTriple() {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMAddAnalysisPasses(LLVMTargetMachineRef T, LLVMPassManagerRef PM) {
        throw new UnsupportedOperationException("Stub!");
    }
}

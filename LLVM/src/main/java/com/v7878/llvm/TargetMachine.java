package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Target.cLLVMTargetDataRef;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm.Types.cLLVMMemoryBufferRef;
import static com.v7878.llvm.Types.cLLVMModuleRef;
import static com.v7878.llvm.Types.cLLVMPassManagerRef;
import static com.v7878.llvm._Utils.CHAR_PTR;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.ENUM;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.foreign.SimpleLinker.processSymbol;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TargetMachine {
    static final Class<?> cLLVMTargetMachineRef = VOID_PTR;
    static final Class<?> cLLVMTargetRef = VOID_PTR;

    public static final class LLVMTargetMachineRef extends AddressValue {

        private LLVMTargetMachineRef(long value) {
            super(value);
        }

        public static LLVMTargetMachineRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMTargetMachineRef of 0");
            }
            return new LLVMTargetMachineRef(value);
        }

        public static LLVMTargetMachineRef ofNullable(long value) {
            return value == 0 ? null : new LLVMTargetMachineRef(value);
        }
    }

    public static final class LLVMTargetRef extends AddressValue {

        private LLVMTargetRef(long value) {
            super(value);
        }

        public static LLVMTargetRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMTargetRef of 0");
            }
            return new LLVMTargetRef(value);
        }

        public static LLVMTargetRef ofNullable(long value) {
            return value == 0 ? null : new LLVMTargetRef(value);
        }
    }

    static final Class<?> cLLVMCodeGenOptLevel = ENUM;

    public enum LLVMCodeGenOptLevel {

        LLVMCodeGenLevelNone,
        LLVMCodeGenLevelLess,
        LLVMCodeGenLevelDefault,
        LLVMCodeGenLevelAggressive;

        public int value() {
            return ordinal();
        }

        public static LLVMCodeGenOptLevel of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMRelocMode = ENUM;

    public enum LLVMRelocMode {

        LLVMRelocDefault,
        LLVMRelocStatic,
        LLVMRelocPIC,
        LLVMRelocDynamicNoPic;

        public int value() {
            return ordinal();
        }

        public static LLVMRelocMode of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMCodeModel = ENUM;

    public enum LLVMCodeModel {

        LLVMCodeModelDefault,
        LLVMCodeModelJITDefault,
        LLVMCodeModelSmall,
        LLVMCodeModelKernel,
        LLVMCodeModelMedium,
        LLVMCodeModelLarge;

        public int value() {
            return ordinal();
        }

        public static LLVMCodeModel of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    static final Class<?> cLLVMCodeGenFileType = ENUM;

    public enum LLVMCodeGenFileType {

        LLVMAssemblyFile,
        LLVMObjectFile;

        public int value() {
            return ordinal();
        }

        public static LLVMCodeGenFileType of(int value) {
            for (var e : values()) {
                if (e.value() == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("value: " + value + " is not found");
        }
    }

    private enum Function {
        LLVMGetFirstTarget(cLLVMTargetRef),
        LLVMGetNextTarget(cLLVMTargetRef, cLLVMTargetRef),
        LLVMGetTargetFromName(cLLVMTargetRef, CONST_CHAR_PTR),
        LLVMGetTargetFromTriple(LLVMBool, CONST_CHAR_PTR, ptr(cLLVMTargetRef), ptr(CHAR_PTR)),
        LLVMGetTargetName(CONST_CHAR_PTR, cLLVMTargetRef),
        LLVMGetTargetDescription(CONST_CHAR_PTR, cLLVMTargetRef),
        LLVMTargetHasJIT(LLVMBool, cLLVMTargetRef),
        LLVMTargetHasTargetMachine(LLVMBool, cLLVMTargetRef),
        LLVMTargetHasAsmBackend(LLVMBool, cLLVMTargetRef),
        LLVMCreateTargetMachine(cLLVMTargetMachineRef, cLLVMTargetRef, CONST_CHAR_PTR, CONST_CHAR_PTR, CONST_CHAR_PTR, cLLVMCodeGenOptLevel, cLLVMRelocMode, cLLVMCodeModel),
        LLVMDisposeTargetMachine(void.class, cLLVMTargetMachineRef),
        LLVMGetTargetMachineTarget(cLLVMTargetRef, cLLVMTargetMachineRef),
        LLVMGetTargetMachineTriple(CHAR_PTR, cLLVMTargetMachineRef),
        LLVMGetTargetMachineCPU(CHAR_PTR, cLLVMTargetMachineRef),
        LLVMGetTargetMachineFeatureString(CHAR_PTR, cLLVMTargetMachineRef),
        LLVMCreateTargetDataLayout(cLLVMTargetDataRef, cLLVMTargetMachineRef),
        LLVMSetTargetMachineAsmVerbosity(void.class, cLLVMTargetMachineRef, LLVMBool),
        LLVMTargetMachineEmitToFile(LLVMBool, cLLVMTargetMachineRef, cLLVMModuleRef, CHAR_PTR, cLLVMCodeGenFileType, ptr(CHAR_PTR)),
        LLVMTargetMachineEmitToMemoryBuffer(LLVMBool, cLLVMTargetMachineRef, cLLVMModuleRef, cLLVMCodeGenFileType, ptr(CHAR_PTR), ptr(cLLVMMemoryBufferRef)),
        LLVMGetDefaultTargetTriple(CHAR_PTR),
        LLVMAddAnalysisPasses(void.class, cLLVMTargetMachineRef, cLLVMPassManagerRef);

        private final MethodType type;
        private final Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
            this.handle = processSymbol(LLVM, LLVM_SCOPE, name(), type());
        }

        public MethodType type() {
            return type;
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", handle=" + handle() + '}';
        }
    }

    /**
     * Returns the first llvm::Target in the registered targets list.
     */
    public static LLVMTargetRef LLVMGetFirstTarget() {
        return nothrows_run(() -> LLVMTargetRef.ofNullable((long) Function.LLVMGetFirstTarget.handle().invoke()));
    }

    /**
     * Returns the next llvm::Target given a previous one (or null if there's none)
     */
    public static LLVMTargetRef LLVMGetNextTarget(LLVMTargetRef T) {
        return nothrows_run(() -> LLVMTargetRef.ofNullable((long) Function.LLVMGetNextTarget.handle().invoke(T.value())));
    }

    /*===-- Target ------------------------------------------------------------===*/

    /**
     * Finds the target corresponding to the given name and stores it in \p T.
     * Returns 0 on success.
     */
    public static LLVMTargetRef LLVMGetTargetFromName(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> LLVMTargetRef.ofNullable((long) Function.LLVMGetTargetFromName.handle().invoke(c_Name.nativeAddress())));
        }
    }

    /**
     * Finds the target corresponding to the given triple and stores it in \p T.
     * Returns 0 on success. Optionally returns any error in ErrorMessage.
     */
    public static boolean LLVMGetTargetFromTriple(String Triple, Consumer<LLVMTargetRef> T, Consumer<String> ErrorMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Triple = allocString(arena, Triple);
            MemorySegment c_T = arena.allocate(ADDRESS);
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMGetTargetFromTriple.handle()
                    .invoke(c_Triple.nativeAddress(), c_T.nativeAddress(), c_ErrorMessage.nativeAddress()));
            if (!err) {
                T.accept(LLVMTargetRef.ofNullable(c_T.get(ADDRESS, 0).nativeAddress()));
            } else {
                ErrorMessage.accept(addressToLLVMString(c_ErrorMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Finds the target corresponding to the given triple.
     */
    // Port-added
    public static LLVMTargetRef LLVMGetTargetFromTriple(String Triple) throws LLVMException {
        String[] err = new String[1];
        LLVMTargetRef[] out = new LLVMTargetRef[1];
        if (LLVMGetTargetFromTriple(Triple, O -> out[0] = O, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    /**
     * Returns the name of a target. See llvm::Target::getName
     */
    public static String LLVMGetTargetName(LLVMTargetRef T) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetTargetName.handle().invoke(T.value())));
    }

    /**
     * Returns the description  of a target. See llvm::Target::getDescription
     */
    public static String LLVMGetTargetDescription(LLVMTargetRef T) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetTargetDescription.handle().invoke(T.value())));
    }

    /**
     * Returns if the target has a JIT
     */
    public static boolean LLVMTargetHasJIT(LLVMTargetRef T) {
        return nothrows_run(() -> (boolean) Function.LLVMTargetHasJIT.handle().invoke(T.value()));
    }

    /**
     * Returns if the target has a TargetMachine associated
     */
    public static boolean LLVMTargetHasTargetMachine(LLVMTargetRef T) {
        return nothrows_run(() -> (boolean) Function.LLVMTargetHasTargetMachine.handle().invoke(T.value()));
    }

    /**
     * Returns if the target as an ASM backend (required for emitting output)
     */
    public static boolean LLVMTargetHasAsmBackend(LLVMTargetRef T) {
        return nothrows_run(() -> (boolean) Function.LLVMTargetHasAsmBackend.handle().invoke(T.value()));
    }

    /*===-- Target Machine ----------------------------------------------------===*/

    /**
     * Creates a new llvm::TargetMachine. See llvm::Target::createTargetMachine
     */
    public static LLVMTargetMachineRef LLVMCreateTargetMachine(
            LLVMTargetRef T, String Triple, String CPU, String Features,
            LLVMCodeGenOptLevel Level, LLVMRelocMode Reloc, LLVMCodeModel CodeModel) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Triple = allocString(arena, Triple);
            MemorySegment c_CPU = allocString(arena, CPU);
            MemorySegment c_Features = allocString(arena, Features);
            return nothrows_run(() -> LLVMTargetMachineRef.ofNullable((long) Function.LLVMCreateTargetMachine.handle()
                    .invoke(T.value(), c_Triple.nativeAddress(), c_CPU.nativeAddress(), c_Features.nativeAddress(),
                            Level.value(), Reloc.value(), CodeModel.value())));
        }
    }

    /**
     * Dispose the LLVMTargetMachineRef instance generated by
     * LLVMCreateTargetMachine.
     */
    public static void LLVMDisposeTargetMachine(LLVMTargetMachineRef T) {
        nothrows_run(() -> Function.LLVMDisposeTargetMachine.handle().invoke(T.value()));
    }

    /**
     * Returns the Target used in a TargetMachine
     */
    public static LLVMTargetRef LLVMGetTargetMachineTarget(LLVMTargetMachineRef T) {
        return nothrows_run(() -> LLVMTargetRef.ofNullable((long) Function.LLVMGetTargetMachineTarget.handle().invoke(T.value())));
    }

    /**
     * Returns the triple used creating this target machine. See
     * llvm::TargetMachine::getTriple.
     */
    public static String LLVMGetTargetMachineTriple(LLVMTargetMachineRef T) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetTargetMachineTriple.handle().invoke(T.value())));
    }

    /**
     * Returns the cpu used creating this target machine. See
     * llvm::TargetMachine::getCPU.
     */
    public static String LLVMGetTargetMachineCPU(LLVMTargetMachineRef T) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetTargetMachineCPU.handle().invoke(T.value())));
    }

    /**
     * Returns the feature string used creating this target machine. See
     * llvm::TargetMachine::getFeatureString.
     */
    public static String LLVMGetTargetMachineFeatureString(LLVMTargetMachineRef T) {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetTargetMachineFeatureString.handle().invoke(T.value())));
    }

    /**
     * Create a DataLayout based on the targetMachine.
     */
    public static LLVMTargetDataRef LLVMCreateTargetDataLayout(LLVMTargetMachineRef T) {
        return nothrows_run(() -> LLVMTargetDataRef.ofNullable((long) Function.LLVMCreateTargetDataLayout.handle().invoke(T.value())));
    }

    /**
     * Set the target machine's ASM verbosity.
     */
    public static void LLVMSetTargetMachineAsmVerbosity(LLVMTargetMachineRef T, boolean VerboseAsm) {
        nothrows_run(() -> Function.LLVMSetTargetMachineAsmVerbosity.handle().invoke(T.value(), VerboseAsm));
    }

    /**
     * Emits an asm or object file for the given module to the filename. This
     * wraps several c++ only classes (among them a file stream). Returns any
     * error in ErrorMessage.
     */
    public static boolean LLVMTargetMachineEmitToFile(
            LLVMTargetMachineRef T, LLVMModuleRef M, String Filename,
            LLVMCodeGenFileType codegen, Consumer<String> ErrorMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Filename = allocString(arena, Filename);
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMTargetMachineEmitToFile.handle()
                    .invoke(T.value(), M.value(), c_Filename.nativeAddress(), codegen.value(), c_ErrorMessage.nativeAddress()));
            if (err) {
                ErrorMessage.accept(addressToLLVMString(c_ErrorMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Emits an asm or object file for the given module to the filename. This
     * wraps several c++ only classes (among them a file stream).
     */
    // Port-added
    public static void LLVMTargetMachineEmitToFile(
            LLVMTargetMachineRef T, LLVMModuleRef M, String Filename,
            LLVMCodeGenFileType codegen) throws LLVMException {
        String[] err = new String[1];
        if (LLVMTargetMachineEmitToFile(T, M, Filename, codegen, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
    }

    /**
     * Compile the LLVM IR stored in \p M and store the result in \p OutMemBuf.
     */
    public static boolean LLVMTargetMachineEmitToMemoryBuffer(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMCodeGenFileType codegen,
            Consumer<String> ErrorMessage, Consumer<LLVMMemoryBufferRef> OutMemBuf) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            MemorySegment c_OutMemBuf = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMTargetMachineEmitToMemoryBuffer.handle()
                    .invoke(T.value(), M.value(), codegen.value(), c_ErrorMessage.nativeAddress(), c_OutMemBuf.nativeAddress()));
            if (!err) {
                OutMemBuf.accept(LLVMMemoryBufferRef.ofNullable(c_OutMemBuf.get(ADDRESS, 0).nativeAddress()));
            } else {
                ErrorMessage.accept(addressToLLVMString(c_ErrorMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Compile the LLVM IR stored in \p M
     */
    // Port-added
    public static LLVMMemoryBufferRef LLVMTargetMachineEmitToMemoryBuffer(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMCodeGenFileType codegen) throws LLVMException {
        String[] err = new String[1];
        LLVMMemoryBufferRef[] out = new LLVMMemoryBufferRef[1];
        if (LLVMTargetMachineEmitToMemoryBuffer(T, M, codegen, E -> err[0] = E, O -> out[0] = O)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    /*===-- Triple ------------------------------------------------------------===*/

    /**
     * Get a triple for the host machine as a string.
     */
    @Deprecated // on Android it always returns "i386-unknown-linux"
    public static String LLVMGetDefaultTargetTriple() {
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetDefaultTargetTriple.handle().invoke()));
    }

    /**
     * Adds the target-specific analysis passes to the pass manager.
     */
    public static void LLVMAddAnalysisPasses(LLVMTargetMachineRef T, LLVMPassManagerRef PM) {
        nothrows_run(() -> Function.LLVMAddAnalysisPasses.handle().invoke(T.value(), PM.value()));
    }
}

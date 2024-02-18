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
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.llvm._Utils.ptr;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMString;
import com.v7878.llvm._Utils.Symbol;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;

public class TargetMachine {
    static final Class<?> cLLVMTargetMachineRef = VOID_PTR;
    static final Class<?> cLLVMTargetRef = VOID_PTR;

    public static final class LLVMTargetMachineRef extends AddressValue {

        LLVMTargetMachineRef(long value) {
            super(value);
        }
    }

    public static final class LLVMTargetRef extends AddressValue {
        LLVMTargetRef(long value) {
            super(value);
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

    private enum Function implements Symbol {
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

    /**
     * Returns the first llvm::Target in the registered targets list.
     */
    public static LLVMTargetRef LLVMGetFirstTarget() {
        return nothrows_run(() -> new LLVMTargetRef((long) Function.LLVMGetFirstTarget.handle().invoke()));
    }

    /**
     * Returns the next llvm::Target given a previous one (or null if there's none)
     */
    public static LLVMTargetRef LLVMGetNextTarget(LLVMTargetRef T) {
        return nothrows_run(() -> new LLVMTargetRef((long) Function.LLVMGetNextTarget.handle().invoke(T.value())));
    }

    /*===-- Target ------------------------------------------------------------===*/

    /**
     * Finds the target corresponding to the given name and stores it in \p T.
     * Returns 0 on success.
     */
    public static LLVMTargetRef LLVMGetTargetFromName(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return nothrows_run(() -> new LLVMTargetRef((long) Function.LLVMGetTargetFromName.handle().invoke(c_Name.address())));
        }
    }

    /**
     * Finds the target corresponding to the given triple and stores it in \p T.
     * Returns 0 on success. Optionally returns any error in ErrorMessage.
     * Use LLVMDisposeMessage to dispose the message.
     */
    public static boolean LLVMGetTargetFromTriple(String Triple, Consumer<LLVMTargetRef> T, Consumer<LLVMString> ErrorMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Triple = allocString(arena, Triple);
            MemorySegment c_T = arena.allocate(ADDRESS);
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMGetTargetFromTriple.handle()
                    .invoke(c_Triple.address(), c_T.address(), c_ErrorMessage.address()));
            if (!err) {
                T.accept(new LLVMTargetRef(c_T.get(ADDRESS, 0).address()));
            } else {
                ErrorMessage.accept(new LLVMString(c_ErrorMessage.get(ADDRESS, 0).address()));
            }
            return err;
        }
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
            return nothrows_run(() -> new LLVMTargetMachineRef((long) Function.LLVMCreateTargetMachine.handle()
                    .invoke(T.value(), c_Triple.address(), c_CPU.address(), c_Features.address(),
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
        return nothrows_run(() -> new LLVMTargetRef((long) Function.LLVMGetTargetMachineTarget.handle().invoke(T.value())));
    }

    /**
     * Returns the triple used creating this target machine. See
     * llvm::TargetMachine::getTriple. The result needs to be disposed with
     * LLVMDisposeMessage.
     */
    public static LLVMString LLVMGetTargetMachineTriple(LLVMTargetMachineRef T) {
        return nothrows_run(() -> new LLVMString((long) Function.LLVMGetTargetMachineTriple.handle().invoke(T.value())));
    }

    /**
     * Returns the cpu used creating this target machine. See
     * llvm::TargetMachine::getCPU. The result needs to be disposed with
     * LLVMDisposeMessage.
     */
    public static LLVMString LLVMGetTargetMachineCPU(LLVMTargetMachineRef T) {
        return nothrows_run(() -> new LLVMString((long) Function.LLVMGetTargetMachineCPU.handle().invoke(T.value())));
    }

    /**
     * Returns the feature string used creating this target machine. See
     * llvm::TargetMachine::getFeatureString. The result needs to be disposed with
     * LLVMDisposeMessage.
     */
    public static LLVMString LLVMGetTargetMachineFeatureString(LLVMTargetMachineRef T) {
        return nothrows_run(() -> new LLVMString((long) Function.LLVMGetTargetMachineFeatureString.handle().invoke(T.value())));
    }

    /**
     * Create a DataLayout based on the targetMachine.
     */
    public static LLVMTargetDataRef LLVMCreateTargetDataLayout(LLVMTargetMachineRef T) {
        return nothrows_run(() -> new LLVMTargetDataRef((long) Function.LLVMCreateTargetDataLayout.handle().invoke(T.value())));
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
     * error in ErrorMessage. Use LLVMDisposeMessage to dispose the message.
     */
    public static boolean LLVMTargetMachineEmitToFile(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMString Filename,
            LLVMCodeGenFileType codegen, Consumer<LLVMString> ErrorMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMTargetMachineEmitToFile.handle()
                    .invoke(T.value(), M.value(), Filename.value(), codegen.value(), c_ErrorMessage.address()));
            if (err) {
                ErrorMessage.accept(new LLVMString(c_ErrorMessage.get(ADDRESS, 0).address()));
            }
            return err;
        }
    }

    /**
     * Compile the LLVM IR stored in \p M and store the result in \p OutMemBuf.
     */
    public static boolean LLVMTargetMachineEmitToMemoryBuffer(
            LLVMTargetMachineRef T, LLVMModuleRef M, LLVMCodeGenFileType codegen,
            Consumer<LLVMString> ErrorMessage, Consumer<LLVMMemoryBufferRef> OutMemBuf) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_ErrorMessage = arena.allocate(ADDRESS);
            MemorySegment c_OutMemBuf = arena.allocate(ADDRESS);
            boolean err = nothrows_run(() -> (boolean) Function.LLVMTargetMachineEmitToMemoryBuffer.handle()
                    .invoke(T.value(), M.value(), codegen.value(), c_ErrorMessage.address(), c_OutMemBuf.address()));
            if (!err) {
                OutMemBuf.accept(new LLVMMemoryBufferRef(c_OutMemBuf.get(ADDRESS, 0).address()));
            } else {
                ErrorMessage.accept(new LLVMString(c_ErrorMessage.get(ADDRESS, 0).address()));
            }
            return err;
        }
    }

    /*===-- Triple ------------------------------------------------------------===*/

    /**
     * Get a triple for the host machine as a string. The result needs to be
     * disposed with LLVMDisposeMessage.
     */
    public static LLVMString LLVMGetDefaultTargetTriple() {
        return nothrows_run(() -> new LLVMString((long) Function.LLVMGetDefaultTargetTriple.handle().invoke()));
    }

    /**
     * Adds the target-specific analysis passes to the pass manager.
     */
    public static void LLVMAddAnalysisPasses(LLVMTargetMachineRef T, LLVMPassManagerRef PM) {
        nothrows_run(() -> Function.LLVMAddAnalysisPasses.handle().invoke(T.value(), PM.value()));
    }
}

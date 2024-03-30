package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMPassManagerRef;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Target.LLVMTargetDataRef;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.util.function.Consumer;

public class TargetMachine {

    public static final class LLVMTargetMachineRef extends AddressValue implements FineClosable {

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

        @Override
        public void close() {
            LLVMDisposeTargetMachine(this);
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

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMGetFirstTarget")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMGetFirstTarget();

        @LibrarySymbol(name = "LLVMGetNextTarget")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetNextTarget(long T);

        @LibrarySymbol(name = "LLVMGetTargetFromName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetFromName(long Name);

        @LibrarySymbol(name = "LLVMGetTargetFromTriple")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMGetTargetFromTriple(long Triple, long T, long ErrorMessage);

        @LibrarySymbol(name = "LLVMGetTargetName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetName(long T);

        @LibrarySymbol(name = "LLVMGetTargetDescription")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetDescription(long T);

        @LibrarySymbol(name = "LLVMTargetHasJIT")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMTargetHasJIT(long T);

        @LibrarySymbol(name = "LLVMTargetHasTargetMachine")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMTargetHasTargetMachine(long T);

        @LibrarySymbol(name = "LLVMTargetHasAsmBackend")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD})
        abstract boolean LLVMTargetHasAsmBackend(long T);

        @LibrarySymbol(name = "LLVMCreateTargetMachine")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, INT, INT})
        abstract long LLVMCreateTargetMachine(long T, long Triple, long CPU, long Features, int Level, int Reloc, int CodeModel);

        @LibrarySymbol(name = "LLVMDisposeTargetMachine")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeTargetMachine(long T);

        @LibrarySymbol(name = "LLVMGetTargetMachineTarget")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetMachineTarget(long T);

        @LibrarySymbol(name = "LLVMGetTargetMachineTriple")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetMachineTriple(long T);

        @LibrarySymbol(name = "LLVMGetTargetMachineCPU")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetMachineCPU(long T);

        @LibrarySymbol(name = "LLVMGetTargetMachineFeatureString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetTargetMachineFeatureString(long T);

        @LibrarySymbol(name = "LLVMCreateTargetDataLayout")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateTargetDataLayout(long T);

        @LibrarySymbol(name = "LLVMSetTargetMachineAsmVerbosity")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMSetTargetMachineAsmVerbosity(long T, boolean VerboseAsm);

        @LibrarySymbol(name = "LLVMTargetMachineEmitToFile")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD})
        abstract boolean LLVMTargetMachineEmitToFile(long T, long M, long Filename, int codegen, long ErrorMessage);

        @LibrarySymbol(name = "LLVMTargetMachineEmitToMemoryBuffer")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMTargetMachineEmitToMemoryBuffer(long T, long M, int codegen, long ErrorMessage, long OutMemBuf);

        @LibrarySymbol(name = "LLVMGetDefaultTargetTriple")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMGetDefaultTargetTriple();

        @LibrarySymbol(name = "LLVMAddAnalysisPasses")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMAddAnalysisPasses(long T, long P);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    /**
     * Returns the first llvm::Target in the registered targets list.
     */
    public static LLVMTargetRef LLVMGetFirstTarget() {
        return LLVMTargetRef.ofNullable(Native.INSTANCE.LLVMGetFirstTarget());
    }

    /**
     * Returns the next llvm::Target given a previous one (or null if there's none)
     */
    public static LLVMTargetRef LLVMGetNextTarget(LLVMTargetRef T) {
        return LLVMTargetRef.ofNullable(Native.INSTANCE.LLVMGetNextTarget(T.value()));
    }

    /*===-- Target ------------------------------------------------------------===*/

    /**
     * Finds the target corresponding to the given name and stores it in \p T.
     * Returns 0 on success.
     */
    public static LLVMTargetRef LLVMGetTargetFromName(String Name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Name = allocString(arena, Name);
            return LLVMTargetRef.ofNullable(Native.INSTANCE.LLVMGetTargetFromName(c_Name.nativeAddress()));
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
            boolean err = Native.INSTANCE.LLVMGetTargetFromTriple(
                    c_Triple.nativeAddress(), c_T.nativeAddress(), c_ErrorMessage.nativeAddress());
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
        return addressToString(Native.INSTANCE.LLVMGetTargetName(T.value()));
    }

    /**
     * Returns the description  of a target. See llvm::Target::getDescription
     */
    public static String LLVMGetTargetDescription(LLVMTargetRef T) {
        return addressToString(Native.INSTANCE.LLVMGetTargetDescription(T.value()));
    }

    /**
     * Returns if the target has a JIT
     */
    public static boolean LLVMTargetHasJIT(LLVMTargetRef T) {
        return Native.INSTANCE.LLVMTargetHasJIT(T.value());
    }

    /**
     * Returns if the target has a TargetMachine associated
     */
    public static boolean LLVMTargetHasTargetMachine(LLVMTargetRef T) {
        return Native.INSTANCE.LLVMTargetHasTargetMachine(T.value());
    }

    /**
     * Returns if the target as an ASM backend (required for emitting output)
     */
    public static boolean LLVMTargetHasAsmBackend(LLVMTargetRef T) {
        return Native.INSTANCE.LLVMTargetHasAsmBackend(T.value());
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
            return LLVMTargetMachineRef.ofNullable(Native.INSTANCE.LLVMCreateTargetMachine(
                    T.value(), c_Triple.nativeAddress(), c_CPU.nativeAddress(), c_Features.nativeAddress(),
                    Level.value(), Reloc.value(), CodeModel.value()));
        }
    }

    /**
     * Dispose the LLVMTargetMachineRef instance generated by
     * LLVMCreateTargetMachine.
     */
    public static void LLVMDisposeTargetMachine(LLVMTargetMachineRef T) {
        Native.INSTANCE.LLVMDisposeTargetMachine(T.value());
    }

    /**
     * Returns the Target used in a TargetMachine
     */
    public static LLVMTargetRef LLVMGetTargetMachineTarget(LLVMTargetMachineRef T) {
        return LLVMTargetRef.ofNullable(Native.INSTANCE.LLVMGetTargetMachineTarget(T.value()));
    }

    /**
     * Returns the triple used creating this target machine. See
     * llvm::TargetMachine::getTriple.
     */
    public static String LLVMGetTargetMachineTriple(LLVMTargetMachineRef T) {
        return addressToLLVMString(Native.INSTANCE.LLVMGetTargetMachineTriple(T.value()));
    }

    /**
     * Returns the cpu used creating this target machine. See
     * llvm::TargetMachine::getCPU.
     */
    public static String LLVMGetTargetMachineCPU(LLVMTargetMachineRef T) {
        return addressToLLVMString(Native.INSTANCE.LLVMGetTargetMachineCPU(T.value()));
    }

    /**
     * Returns the feature string used creating this target machine. See
     * llvm::TargetMachine::getFeatureString.
     */
    public static String LLVMGetTargetMachineFeatureString(LLVMTargetMachineRef T) {
        return addressToLLVMString(Native.INSTANCE.LLVMGetTargetMachineFeatureString(T.value()));
    }

    /**
     * Create a DataLayout based on the targetMachine.
     */
    public static LLVMTargetDataRef LLVMCreateTargetDataLayout(LLVMTargetMachineRef T) {
        return LLVMTargetDataRef.ofNullable(Native.INSTANCE.LLVMCreateTargetDataLayout(T.value()));
    }

    /**
     * Set the target machine's ASM verbosity.
     */
    public static void LLVMSetTargetMachineAsmVerbosity(LLVMTargetMachineRef T, boolean VerboseAsm) {
        Native.INSTANCE.LLVMSetTargetMachineAsmVerbosity(T.value(), VerboseAsm);
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
            boolean err = Native.INSTANCE.LLVMTargetMachineEmitToFile(T.value(), M.value(),
                    c_Filename.nativeAddress(), codegen.value(), c_ErrorMessage.nativeAddress());
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
            boolean err = Native.INSTANCE.LLVMTargetMachineEmitToMemoryBuffer(T.value(), M.value(),
                    codegen.value(), c_ErrorMessage.nativeAddress(), c_OutMemBuf.nativeAddress());
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
        return addressToLLVMString(Native.INSTANCE.LLVMGetDefaultTargetTriple());
    }

    /**
     * Adds the target-specific analysis passes to the pass manager.
     */
    public static void LLVMAddAnalysisPasses(LLVMTargetMachineRef T, LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMAddAnalysisPasses(T.value(), PM.value());
    }
}

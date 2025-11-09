package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMCreatePassManager;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMRunPassManager;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.llvm.ObjectFile.LLVMGetSectionSegment;
import static com.v7878.llvm.ObjectFile.LLVMGetSections;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolAddress;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolName;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolSize;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbols;
import static com.v7878.llvm.ObjectFile.LLVMIsSymbolIteratorAtEnd;
import static com.v7878.llvm.ObjectFile.LLVMMoveToContainingSection;
import static com.v7878.llvm.ObjectFile.LLVMMoveToNextSymbol;
import static com.v7878.llvm.PassManagerBuilder.LLVMPassManagerBuilderCreate;
import static com.v7878.llvm.PassManagerBuilder.LLVMPassManagerBuilderPopulateModulePassManager;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.llvm.LLVMGlobals.newContext;
import static com.v7878.unsafe.llvm.LLVMGlobals.newDefaultMachine;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.unsafe.NativeCodeBlob;

import java.util.List;
import java.util.Objects;

public class LLVMUtils {
    public static MemorySegment[] getFunctionsCode(LLVMObjectFileRef obj, String... names) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(names);
        if (names.length == 0) {
            return new MemorySegment[0];
        }

        List<String> l_names = List.of(names);

        MemorySegment[] out = new MemorySegment[names.length];

        try (var sym = LLVMGetSymbols(obj); var section = LLVMGetSections(obj)) {
            for (; !LLVMIsSymbolIteratorAtEnd(obj, sym); LLVMMoveToNextSymbol(sym)) {
                String name = LLVMGetSymbolName(sym);
                int index = l_names.indexOf(name);
                if (index >= 0) {
                    long offset = LLVMGetSymbolAddress(sym);
                    long size = LLVMGetSymbolSize(sym);

                    LLVMMoveToContainingSection(section, sym);
                    out[index] = LLVMGetSectionSegment(section).asSlice(offset, size);
                }
            }
        }

        for (int i = 0; i < names.length; i++) {
            if (out[i] == null) {
                throw new IllegalArgumentException("Can`t find code for function: \"" + names[i] + "\"");
            }
        }

        return out;
    }

    public static MemorySegment getFunctionCode(LLVMObjectFileRef obj, String name) {
        return getFunctionsCode(obj, name)[0];
    }

    public interface Generator {
        void generate(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder);
    }

    public static LLVMMemoryBufferRef generateModuleToBuffer(Generator generator) throws LLVMException {
        Objects.requireNonNull(generator);

        try (var context = newContext(); var builder = LLVMCreateBuilderInContext(context);
             var module = LLVMModuleCreateWithNameInContext("generic", context)) {

            generator.generate(context, module, builder);

            LLVMVerifyModule(module);

            try (var pass_manager = LLVMCreatePassManager()) {
                try (var pmb = LLVMPassManagerBuilderCreate()) {
                    LLVMPassManagerBuilderPopulateModulePassManager(pmb, pass_manager);
                }
                LLVMRunPassManager(pass_manager, module);
            }

            try (var machine = newDefaultMachine()) {
                return LLVMTargetMachineEmitToMemoryBuffer(machine, module, LLVMObjectFile);
            }
        }
    }

    public static byte[] generateFunctionCodeArray(Generator generator, String name) {
        try {
            var buf = generateModuleToBuffer(generator);
            try (var of = LLVMCreateObjectFile(buf)) {
                return getFunctionCode(of, name).toArray(ValueLayout.JAVA_BYTE);
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MemorySegment generateFunctionCodeSegment(Generator generator, String name, Arena scope) {
        try {
            var buf = generateModuleToBuffer(generator);
            try (var of = LLVMCreateObjectFile(buf)) {
                MemorySegment code = getFunctionCode(of, name);
                return NativeCodeBlob.makeCodeBlob(scope, code)[0];
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }
}

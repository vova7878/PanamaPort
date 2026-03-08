package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMCreatePassManager;
import static com.v7878.llvm.Core.LLVMGetBufferSegment;
import static com.v7878.llvm.Core.LLVMGetValueName;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMPrintModuleToString;
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
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMAssemblyFile;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.llvm.Types.LLVMValueRef;
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
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.unsafe.NativeCodeBlob;

import java.nio.charset.StandardCharsets;
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
        LLVMValueRef generate(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder);
    }

    private static byte[] generateCode(Generator generator) throws LLVMException {
        Objects.requireNonNull(generator);

        try (var context = newContext(); var builder = LLVMCreateBuilderInContext(context);
             var module = LLVMModuleCreateWithNameInContext("generic", context)) {

            var function = generator.generate(context, module, builder);
            var name = LLVMGetValueName(function);

            try {
                LLVMVerifyModule(module);
            } catch (LLVMException e) {
                throw new LLVMException(e.getMessage() + "\n" + LLVMPrintModuleToString(module), e);
            }

            try (var pass_manager = LLVMCreatePassManager()) {
                try (var pmb = LLVMPassManagerBuilderCreate()) {
                    LLVMPassManagerBuilderPopulateModulePassManager(pmb, pass_manager);
                }
                LLVMRunPassManager(pass_manager, module);
            }

            System.out.println(LLVMPrintModuleToString(module));

            try (var machine = newDefaultMachine()) {
                try (var asm = LLVMTargetMachineEmitToMemoryBuffer(machine, module, LLVMAssemblyFile)) {
                    var segment = LLVMGetBufferSegment(asm);
                    System.out.println(segment.getString(0,
                            StandardCharsets.UTF_8, segment.byteSize()));
                }
                var buffer = LLVMTargetMachineEmitToMemoryBuffer(machine, module, LLVMObjectFile);
                try (var of = LLVMCreateObjectFile(buffer)) {
                    return getFunctionCode(of, name).toArray(ValueLayout.JAVA_BYTE);
                }
            }
        }
    }

    public static byte[] generateFunctionCodeArray(Generator generator) {
        try {
            return generateCode(generator);
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MemorySegment generateFunctionCodeSegment(Generator generator, Arena scope) {
        try {
            return NativeCodeBlob.makeCodeBlob(scope, generateCode(generator))[0];
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }
}

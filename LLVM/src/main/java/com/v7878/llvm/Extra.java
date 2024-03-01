package com.v7878.llvm;

import static com.v7878.llvm.ObjectFile.LLVMGetSectionSegment;
import static com.v7878.llvm.ObjectFile.LLVMGetSections;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolAddress;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolName;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolSize;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbols;
import static com.v7878.llvm.ObjectFile.LLVMIsSymbolIteratorAtEnd;
import static com.v7878.llvm.ObjectFile.LLVMMoveToContainingSection;
import static com.v7878.llvm.ObjectFile.LLVMMoveToNextSymbol;
import static com.v7878.unsafe.NativeCodeBlob.CURRENT_INSTRUCTION_SET;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;
import com.v7878.unsafe.Utils;

import java.util.List;
import java.util.Objects;

public class Extra {

    public static String LLVMGetHostTriple() {
        // TODO
        return LLVMGetHostCPUName() + "-unknown-linux-android";
    }

    public static String LLVMGetHostCPUName() {
        // TODO _ZN4llvm3sys14getHostCPUNameEv
        return switch (CURRENT_INSTRUCTION_SET) {
            case X86 -> "i386";
            case X86_64 -> "x86_64";
            case ARM -> "arm";
            case ARM64 -> "aarch64";
            //TODO: RISCV64
            default -> throw new IllegalStateException(
                    "unsupported instruction set: " + CURRENT_INSTRUCTION_SET);
        };
    }

    public static String LLVMGetHostCPUFeatures() {
        // TODO _ZN4llvm3sys18getHostCPUFeaturesERNS_9StringMapIbNS_15MallocAllocatorEEE
        return "";
    }

    public static MemorySegment[] getFunctionsCode(LLVMObjectFileRef obj, String... names) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(names);
        if (names.length == 0) {
            return new MemorySegment[0];
        }

        List<String> l_names = List.of(names);

        MemorySegment[] out = new MemorySegment[names.length];

        try (var sym = Utils.lock(LLVMGetSymbols(obj), ObjectFile::LLVMDisposeSymbolIterator);
             var section = Utils.lock(LLVMGetSections(obj), ObjectFile::LLVMDisposeSectionIterator)) {
            for (; !LLVMIsSymbolIteratorAtEnd(obj, sym.value());
                 LLVMMoveToNextSymbol(sym.value())) {
                String name = LLVMGetSymbolName(sym.value());
                int index = l_names.indexOf(name);
                if (index >= 0) {
                    long offset = LLVMGetSymbolAddress(sym.value());
                    long size = LLVMGetSymbolSize(sym.value());

                    LLVMMoveToContainingSection(section.value(), sym.value());
                    out[index] = LLVMGetSectionSegment(section.value()).asSlice(offset, size);
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
}

package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMGetBasicBlockParent;
import static com.v7878.llvm.Core.LLVMGetGlobalParent;
import static com.v7878.llvm.Core.LLVMGetInsertBlock;
import static com.v7878.llvm.Core.LLVMGetModuleContext;
import static com.v7878.llvm.ObjectFile.LLVMGetSectionSegment;
import static com.v7878.llvm.ObjectFile.LLVMGetSections;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolAddress;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolName;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolSize;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbols;
import static com.v7878.llvm.ObjectFile.LLVMIsSymbolIteratorAtEnd;
import static com.v7878.llvm.ObjectFile.LLVMMoveToContainingSection;
import static com.v7878.llvm.ObjectFile.LLVMMoveToNextSymbol;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMModuleRef;

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

    public static LLVMModuleRef getBuilderModule(LLVMBuilderRef builder) {
        return LLVMGetGlobalParent(LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder)));
    }

    public static LLVMContextRef getModuleContext(LLVMModuleRef module) {
        return LLVMGetModuleContext(module);
    }

    public static LLVMContextRef getBuilderContext(LLVMBuilderRef builder) {
        return LLVMGetModuleContext(getBuilderModule(builder));
    }
}

package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
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
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.LLVMGlobals.DOUBLE_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.FLOAT_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT16_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT1_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT32_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT64_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT8_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.VOID_T;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.PaddingLayout;
import com.v7878.foreign.SequenceLayout;
import com.v7878.foreign.StructLayout;
import com.v7878.foreign.UnionLayout;
import com.v7878.foreign.ValueLayout;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.access.JavaForeignAccess;

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

    public static LLVMTypeRef layoutToLLVMTypeInContext(LLVMContextRef context, MemoryLayout layout) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(layout);
        boolean natural = JavaForeignAccess.hasNaturalAlignment(layout);
        LLVMTypeRef out;
        if (layout instanceof ValueLayout.OfByte) {
            out = INT8_T;
        } else if (layout instanceof ValueLayout.OfBoolean) {
            out = INT1_T;
        } else if (layout instanceof ValueLayout.OfShort || layout instanceof ValueLayout.OfChar) {
            out = INT16_T;
        } else if (layout instanceof ValueLayout.OfInt) {
            out = INT32_T;
        } else if (layout instanceof ValueLayout.OfFloat) {
            out = FLOAT_T;
        } else if (layout instanceof ValueLayout.OfLong) {
            out = INT64_T;
            natural = layout.byteAlignment() == JAVA_LONG.byteAlignment();
        } else if (layout instanceof ValueLayout.OfDouble) {
            out = DOUBLE_T;
            natural = layout.byteAlignment() == JAVA_DOUBLE.byteAlignment();
        } else if (layout instanceof AddressLayout addressLayout) {
            out = LLVMPointerType(addressLayout.targetLayout()
                    .map(l -> layoutToLLVMTypeInContext(context, l))
                    .orElse(VOID_T), 0);
        } else if (layout instanceof PaddingLayout || layout instanceof UnionLayout) {
            out = LLVMArrayType(INT8_T, Math.toIntExact(layout.byteSize()));
            natural = true;
        } else if (layout instanceof SequenceLayout sequence) {
            out = LLVMArrayType(layoutToLLVMTypeInContext(context, sequence.elementLayout()),
                    Math.toIntExact(sequence.elementCount()));
        } else if (layout instanceof StructLayout struct) {
            List<MemoryLayout> members = struct.memberLayouts();
            LLVMTypeRef[] elements = new LLVMTypeRef[members.size()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = layoutToLLVMTypeInContext(context, members.get(i));
            }
            out = LLVMStructTypeInContext(context, elements, !natural);
            natural = true;
        } else {
            throw shouldNotReachHere();
        }
        if (!natural) {
            out = LLVMStructTypeInContext(context, new LLVMTypeRef[]{out}, true);
        }
        return out;
    }
}

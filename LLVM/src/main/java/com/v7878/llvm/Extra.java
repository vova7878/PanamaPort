package com.v7878.llvm;

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
import static com.v7878.unsafe.NativeCodeBlob.CURRENT_INSTRUCTION_SET;

import android.system.ErrnoException;
import android.system.Os;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.foreign.LibDLExt;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;
import java.util.List;
import java.util.Objects;

public class Extra {

    public static long mem_dlopen(MemorySegment segment, int flags) {
        long length = segment.byteSize();
        try {
            FileDescriptor fd = IOUtils.ashmem_create_region(
                    "(generic dlopen)", length);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment target = IOUtils.map(fd, 0, length, arena);
                target.copyFrom(segment);
                target.force();
                return LibDLExt.android_dlopen_ext(fd, 0, flags);
            } finally {
                try {
                    Os.close(fd);
                } catch (ErrnoException e) { /* swallow exception */ }
            }
        } catch (ErrnoException e) {
            return 0;
        }
    }

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

    public static MemorySegment[] copyFunctionsCode(LLVMMemoryBufferRef obj, String... names) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(names);
        if (names.length == 0) {
            return new MemorySegment[0];
        }

        List<String> l_names = List.of(names);

        MemorySegment[] out = new MemorySegment[names.length];

        try (var of = Utils.lock(LLVMCreateObjectFile(obj), ObjectFile::LLVMDisposeObjectFile);
             var sym = Utils.lock(LLVMGetSymbols(of.value()), ObjectFile::LLVMDisposeSymbolIterator);
             var section = Utils.lock(LLVMGetSections(of.value()), ObjectFile::LLVMDisposeSectionIterator)) {
            for (; !LLVMIsSymbolIteratorAtEnd(of.value(), sym.value());
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

        return out;
    }

    public static MemorySegment copyFunctionCode(LLVMMemoryBufferRef obj, String name) {
        return copyFunctionsCode(obj, name)[0];
    }
}

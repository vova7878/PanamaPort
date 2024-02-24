package com.v7878.llvm;

import static com.v7878.unsafe.NativeCodeBlob.CURRENT_INSTRUCTION_SET;

import android.system.ErrnoException;
import android.system.Os;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.foreign.LibDLExt;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;

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
}

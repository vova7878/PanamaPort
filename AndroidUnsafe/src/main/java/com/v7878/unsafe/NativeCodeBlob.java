package com.v7878.unsafe;

import static android.system.Os.mmap;
import static com.v7878.misc.Math.roundUpL;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.copyMemory;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.VM.getCurrentInstructionSet;

import android.system.OsConstants;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class NativeCodeBlob {
    private static final int CODE_PROT = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;
    private static final int MAP_ANONYMOUS = 0x20;
    private static final int CODE_FLAGS = OsConstants.MAP_PRIVATE | MAP_ANONYMOUS;

    private static final int CODE_ALIGNMENT;

    static {
        String iset = getCurrentInstructionSet();
        switch (iset) {
            case "arm":
            case "arm64":
            case "x86":
            case "x86_64":
            case "riscv64":
                //TODO
                CODE_ALIGNMENT = 16;
                break;
            default:
                throw new IllegalStateException("unsupported instruction set: " + iset);
        }
    }

    static MemorySegment[] makeCodeBlobInternal(Arena arena, byte[]... code) {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += code[i].length;
        }
        long finalSize = size;
        //TODO: use arena
        MemorySegment data = nothrows_run(() -> MemorySegment.ofAddress(
                        mmap(0, finalSize, CODE_PROT, CODE_FLAGS, null, 0))
                .reinterpret(finalSize));
        //Cleaner.create(lifetime, () -> nothrows_run(
        //        () -> munmap(data.address(), finalSize)));
        MemorySegment[] out = new MemorySegment[count];
        for (int i = 0; i < count; i++) {
            MemorySegment tmp = data.asSlice(offsets[i], code[i].length);
            copyMemory(code[i], ARRAY_BYTE_BASE_OFFSET,
                    null, tmp.address(), code[i].length);
            out[i] = tmp;
        }
        return out;
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, byte[]... code) {
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, code.clone());
    }
}

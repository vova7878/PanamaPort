package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.PAGE_SIZE;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.misc.Math.roundUpUL;

import android.system.ErrnoException;
import android.system.OsConstants;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.io.IOUtils;

import java.util.Arrays;
import java.util.Objects;

public class NativeCodeBlob {
    private static final int PROT_RW = OsConstants.PROT_READ | OsConstants.PROT_WRITE;
    private static final int PROT_RX = OsConstants.PROT_READ | OsConstants.PROT_EXEC;
    private static final int CODE_FLAGS = OsConstants.MAP_PRIVATE | IOUtils.MAP_ANONYMOUS;
    private static final int CODE_ALIGNMENT = CURRENT_INSTRUCTION_SET.codeAlignment();

    private static MemorySegment[] makeCodeBlobInternal(Arena arena, MemorySegment... code) throws ErrnoException {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count], sizes = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpUL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += sizes[i] = code[i].byteSize();
        }
        roundUpUL(size, PAGE_SIZE);

        MemorySegment data = IOUtils.mmap(0, null, 0, size, PROT_RW, CODE_FLAGS, arena);
        long data_address = data.nativeAddress();

        MemorySegment[] out = new MemorySegment[count];
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            for (int i = 0; i < count; i++) {
                MemorySegment tmp = data.asSlice(offsets[i], sizes[i]);
                MemorySegment.copy(code[i], 0, tmp, 0, sizes[i]);
                out[i] = tmp;
            }
        } else {
            for (int i = 0; i < count; i++) {
                Object code_base = JavaForeignAccess.unsafeGetBase(code[i]);
                long code_offset = JavaForeignAccess.unsafeGetOffset(code[i]);
                AndroidUnsafe.copyMemory(code_base, code_offset, null,
                        data_address + offsets[i], sizes[i]);
                out[i] = data.asSlice(offsets[i], sizes[i]);
            }
        }

        IOUtils.mprotect(data_address, size, PROT_RX);

        return out;
    }

    private static MemorySegment[] makeCodeBlobChecked(Arena arena, MemorySegment... code) {
        try {
            return makeCodeBlobInternal(arena, code);
        } catch (ErrnoException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, MemorySegment... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobChecked(arena, code.clone());
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, byte[]... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobChecked(arena, Arrays.stream(code)
                .map(MemorySegment::ofArray).toArray(MemorySegment[]::new));
    }
}

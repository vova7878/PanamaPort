package com.v7878.unsafe;

import static com.v7878.misc.Math.roundUpL;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.io.IOUtils.MAP_ANONYMOUS;

import android.system.ErrnoException;
import android.system.OsConstants;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.io.IOUtils;

import java.util.Arrays;
import java.util.Objects;

public class NativeCodeBlob {

    private static final int CODE_PROT = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;
    private static final int CODE_FLAGS = OsConstants.MAP_PRIVATE | MAP_ANONYMOUS;
    private static final int CODE_ALIGNMENT = CURRENT_INSTRUCTION_SET.codeAlignment();

    private static MemorySegment[] makeCodeBlobInternal(Arena arena, MemorySegment... code) {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += code[i].byteSize();
        }

        MemorySegment data;
        try {
            data = IOUtils.mmap(0, null, 0, size, CODE_PROT, CODE_FLAGS, arena);
        } catch (ErrnoException e) {
            throw shouldNotHappen(e);
        }

        MemorySegment[] out = new MemorySegment[count];
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            for (int i = 0; i < count; i++) {
                MemorySegment tmp = data.asSlice(offsets[i], code[i].byteSize());
                MemorySegment.copy(code[i], 0, tmp, 0, code[i].byteSize());
                out[i] = tmp;
            }
        } else {
            Object data_base = JavaForeignAccess.unsafeGetBase(data);
            long data_offset = JavaForeignAccess.unsafeGetOffset(data);
            for (int i = 0; i < count; i++) {
                Object code_base = JavaForeignAccess.unsafeGetBase(code[i]);
                long code_offset = JavaForeignAccess.unsafeGetOffset(code[i]);
                AndroidUnsafe.copyMemory(code_base, code_offset, data_base,
                        data_offset + offsets[i], code[i].byteSize());
                out[i] = data.asSlice(offsets[i], code[i].byteSize());
            }
        }
        return out;
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, MemorySegment... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, code.clone());
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, byte[]... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, Arrays.stream(code)
                .map(MemorySegment::ofArray).toArray(MemorySegment[]::new));
    }
}

package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.PAGE_SIZE;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.io.IOUtils.MAP_ANONYMOUS;
import static com.v7878.unsafe.misc.Math.roundUpL;

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
        roundUpL(size, PAGE_SIZE);

        MemorySegment data;
        try {
            data = IOUtils.mmap(0, null, 0, size, PROT_RW, CODE_FLAGS, arena);
        } catch (ErrnoException e) {
            throw shouldNotHappen(e);
        }
        long data_address = data.nativeAddress();

        MemorySegment[] out = new MemorySegment[count];
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            for (int i = 0; i < count; i++) {
                MemorySegment tmp = data.asSlice(offsets[i], code[i].byteSize());
                MemorySegment.copy(code[i], 0, tmp, 0, code[i].byteSize());
                out[i] = tmp;
            }
        } else {
            for (int i = 0; i < count; i++) {
                Object code_base = JavaForeignAccess.unsafeGetBase(code[i]);
                long code_offset = JavaForeignAccess.unsafeGetOffset(code[i]);
                AndroidUnsafe.copyMemory(code_base, code_offset, null,
                        data_address + offsets[i], code[i].byteSize());
                out[i] = data.asSlice(offsets[i], code[i].byteSize());
            }
        }
        try {
            IOUtils.mprotect(data_address, size, PROT_RX);
        } catch (ErrnoException e) {
            throw shouldNotHappen(e);
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

package com.v7878.unsafe;

import static android.system.OsConstants.PROT_READ;
import static com.v7878.unsafe.AndroidUnsafe.PAGE_SIZE;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.io.IOUtils.F_ADD_SEALS;
import static com.v7878.unsafe.io.IOUtils.F_SEAL_FUTURE_WRITE;
import static com.v7878.unsafe.io.IOUtils.F_SEAL_GROW;
import static com.v7878.unsafe.io.IOUtils.F_SEAL_SEAL;
import static com.v7878.unsafe.io.IOUtils.F_SEAL_SHRINK;
import static com.v7878.unsafe.io.IOUtils.MAP_ANONYMOUS;
import static com.v7878.unsafe.io.IOUtils.MFD_ALLOW_SEALING;
import static com.v7878.unsafe.misc.Math.roundUpUL;

import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.io.IOUtils;

import java.util.Arrays;
import java.util.Objects;

public class NativeCodeBlob {
    private static final int PROT_RW = PROT_READ | OsConstants.PROT_WRITE;
    private static final int PROT_RX = PROT_READ | OsConstants.PROT_EXEC;
    private static final int FD_MAP_FLAGS = OsConstants.MAP_SHARED;
    private static final int ANON_MAP_FLAGS = OsConstants.MAP_PRIVATE | MAP_ANONYMOUS;
    private static final int MEMFD_SEAL_FLAGS = F_SEAL_SEAL | F_SEAL_SHRINK | F_SEAL_GROW | F_SEAL_FUTURE_WRITE;
    private static final int CODE_ALIGNMENT = CURRENT_INSTRUCTION_SET.codeAlignment();
    private static final String MAP_NAME = "jit-cache";

    private static MemorySegment[] makeCodeBlobInternal(Arena arena, MemorySegment... code) throws ErrnoException {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count], sizes = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpUL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += sizes[i] = code[i].byteSize();
        }
        size = roundUpUL(size, PAGE_SIZE);

        MemorySegment data;
        // TODO: check for isolated processes
        if (Utils.isCoreUid(Process.myUid())) {
            data = IOUtils.mmap(0, null, 0, size, PROT_RW, ANON_MAP_FLAGS, arena);

            for (int i = 0; i < count; i++) {
                EarlyNativeUtils.copy(code[i], 0, data, offsets[i], sizes[i]);
            }

            IOUtils.mprotect(data.nativeAddress(), size, PROT_RX);
        } else if (IOUtils.isSealFutureWriteSupported()) {
            try (var fd = IOUtils.memfd_create_scoped(MAP_NAME, MFD_ALLOW_SEALING, size)) {
                try (var scope = Arena.ofConfined()) {
                    data = IOUtils.mmap(0, fd.value(), 0, size, PROT_RW, FD_MAP_FLAGS, scope);

                    for (int i = 0; i < count; i++) {
                        EarlyNativeUtils.copy(code[i], 0, data, offsets[i], sizes[i]);
                    }
                }

                Os.fsync(fd.value());

                IOUtils.fcntl_arg(fd.value(), F_ADD_SEALS, MEMFD_SEAL_FLAGS);

                data = IOUtils.mmap(0, fd.value(), 0, size, PROT_RX, FD_MAP_FLAGS, arena);
            }
        } else {
            try (var fd = IOUtils.ashmem_create_scoped_region(MAP_NAME, size)) {
                try (var scope = Arena.ofConfined()) {
                    data = IOUtils.mmap(0, fd.value(), 0, size, PROT_RW, FD_MAP_FLAGS, scope);

                    for (int i = 0; i < count; i++) {
                        EarlyNativeUtils.copy(code[i], 0, data, offsets[i], sizes[i]);
                    }

                    data.force();
                }

                IOUtils.ashmem_set_prot_region(fd.value(), PROT_RX);

                data = IOUtils.mmap(0, fd.value(), 0, size, PROT_RX, FD_MAP_FLAGS, arena);
            }
        }

        MemorySegment[] out = new MemorySegment[count];
        for (int i = 0; i < count; i++) {
            out[i] = data.asSlice(offsets[i], sizes[i]);
        }

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

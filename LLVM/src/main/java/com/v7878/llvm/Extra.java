package com.v7878.llvm;

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
}

package com.v7878.unsafe.access;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.io.IOUtils.MADV_DONTNEED;
import static com.v7878.unsafe.io.IOUtils.MADV_WILLNEED;
import static com.v7878.unsafe.io.IOUtils.madvise;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AndroidUnsafe;

import java.io.FileDescriptor;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

class MappedMemoryUtils {
    private static final int PAGE_SIZE = AndroidUnsafe.PAGE_SIZE;

    static boolean isLoaded(long address, long length) {
        if (address == 0 || length == 0) return true;

        long offset = mappingOffset(address);
        address -= offset;
        length += offset;
        long pc = pageCount(length);
        if (CORRECT_SDK_INT < 35) {
            return isLoaded0_before35(address, length, (int) pc);
        } else {
            return isLoaded0_after35(address, length, pc);
        }
    }

    // not used, but a potential target for a store, see load() for details.
    @DoNotShrink
    //TODO: check if keep rules work correctly
    private static byte unused;

    static void load(long address, long length) {
        if (address == 0 || length == 0) return;

        long offset = mappingOffset(address);
        address -= offset;
        length += offset;
        load0(address, length);

        // Read a byte from each page to bring it into memory. A checksum
        // is computed as we go along to prevent the compiler from otherwise
        // considering the loop as dead code.
        long count = pageCount(length);
        byte x = 0;
        for (long i = 0; i < count; i++) {
            x ^= AndroidUnsafe.getByteN(address + i * PAGE_SIZE);
        }
        if (unused != 0) unused = x;
    }

    static void unload(long address, long length) {
        if (address == 0 || length == 0) return;

        long offset = mappingOffset(address);
        unload0(address - offset, length + offset);
    }

    static void force(FileDescriptor fd, long address, long length) {
        // force writeback via file descriptor
        long offset = mappingOffset(address);
        force0(fd, address - offset, length + offset);
    }

    // utility methods

    private static long pageCount(long size) {
        return (size + PAGE_SIZE - 1L) / PAGE_SIZE;
    }

    // Returns the distance (in bytes) of the buffer start from the
    // largest page aligned address of the mapping less than or equal
    // to the start address.
    private static long mappingOffset(long address) {
        return address - alignPageDown(address);
    }

    // align address down to page size
    private static long alignPageDown(long address) {
        return address & -PAGE_SIZE;
    }

    // native methods

    private static void load0(long address, long length) {
        nothrows_run(() -> madvise(address, length, MADV_WILLNEED));
    }

    private static void unload0(long address, long length) {
        nothrows_run(() -> madvise(address, length, MADV_DONTNEED));
    }

    @DoNotShrink
    @DoNotObfuscate
    private static native boolean isLoaded0_before35(long address, long length, int pageCount);

    @DoNotShrink
    @DoNotObfuscate
    private static native boolean isLoaded0_after35(long address, long length, long pageCount);

    @DoNotShrink
    @DoNotObfuscate
    private static native void force0(FileDescriptor fd, long address, long length);

    static {
        Method[] tm = getDeclaredMethods(MappedMemoryUtils.class);
        Method[] mm = getDeclaredMethods(MappedByteBuffer.class);
        Class<?> pc_type = CORRECT_SDK_INT < 35 ? int.class : long.class;
        String suffix = CORRECT_SDK_INT < 35 ? "_before35" : "_after35";
        registerNativeMethod(searchMethod(tm, "isLoaded0" + suffix, long.class, long.class, pc_type),
                getExecutableData(searchMethod(mm, "isLoaded0", long.class, long.class, pc_type)));
        registerNativeMethod(searchMethod(tm, "force0", FileDescriptor.class, long.class, long.class),
                getExecutableData(searchMethod(mm, "force0", FileDescriptor.class, long.class, long.class)));
    }
}

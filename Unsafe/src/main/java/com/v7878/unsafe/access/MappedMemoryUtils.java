package com.v7878.unsafe.access;

import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.DIRECT_HOOK_VTABLE;
import static com.v7878.unsafe.io.IOUtils.MADV_DONTNEED;
import static com.v7878.unsafe.io.IOUtils.MADV_WILLNEED;
import static com.v7878.unsafe.io.IOUtils.madvise;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.access.AccessLinker.Conditions;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;

import java.io.FileDescriptor;

class MappedMemoryUtils {
    private static final int PAGE_SIZE = AndroidUnsafe.PAGE_SIZE;

    static boolean isLoaded(long address, long length) {
        if (address == 0 || length == 0) return true;

        long offset = mappingOffset(address);
        address -= offset;
        length += offset;

        return isLoaded0(address, length, pageCount(length));
    }

    // not used, but a potential target for a store, see load() for details.
    @DoNotShrink
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

    private static boolean isLoaded0(long address, long length, long pageCount) {
        if (ART_INDEX < A15) {
            return AccessI.INSTANCE.isLoaded0b35(address, length, (int) pageCount);
        } else {
            return AccessI.INSTANCE.isLoaded0a35(address, length, pageCount);
        }
    }

    private static void force0(FileDescriptor fd, long length, long pageCount) {
        AccessI.INSTANCE.force0(fd, length, pageCount);
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @ExecutableAccess(conditions = @Conditions(max_art = A14), kind = DIRECT_HOOK_VTABLE,
                klass = "java.nio.MappedByteBuffer", name = "isLoaded0", args = {"long", "long", "int"})
        abstract boolean isLoaded0b35(long address, long length, int pageCount);

        @ExecutableAccess(conditions = @Conditions(min_art = A15), kind = DIRECT_HOOK_VTABLE,
                klass = "java.nio.MappedByteBuffer", name = "isLoaded0", args = {"long", "long", "long"})
        abstract boolean isLoaded0a35(long address, long length, long pageCount);

        @ExecutableAccess(kind = DIRECT_HOOK_VTABLE, klass = "java.nio.MappedByteBuffer",
                name = "force0", args = {"java.io.FileDescriptor", "long", "long"})
        abstract void force0(FileDescriptor fd, long length, long pageCount);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }
}

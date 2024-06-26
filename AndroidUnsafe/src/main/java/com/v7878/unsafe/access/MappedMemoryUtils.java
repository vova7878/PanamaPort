package com.v7878.unsafe.access;

import static com.v7878.unsafe.AndroidUnsafe.throwException;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import android.system.ErrnoException;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.Errno;

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
        return isLoaded0(address, length, (int) pageCount(length));
    }

    // not used, but a potential target for a store, see load() for details.
    @Keep
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

    @Keep
    private static native boolean isLoaded0(long address, long length, int pageCount);

    @Keep
    private static native void force0(FileDescriptor fd, long address, long length);


    private static final int MADV_WILLNEED = 3;
    private static final int MADV_DONTNEED = 4;

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "madvise")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int madvise(long address, long length, int advice);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class));
    }

    private static int madvise(long address, long length, int advice) {
        return Native.INSTANCE.madvise(address, length, advice);
    }

    private static void load0(long address, long length) {
        int result = madvise(address, length, MADV_WILLNEED);
        if (result == -1) {
            throwException(new ErrnoException("madvise", Errno.errno()));
        }
    }

    private static void unload0(long address, long length) {
        int result = madvise(address, length, MADV_DONTNEED);
        if (result == -1) {
            throwException(new ErrnoException("madvise", Errno.errno()));
        }
    }

    static {
        Method[] tm = getDeclaredMethods(MappedMemoryUtils.class);
        Method[] mm = getDeclaredMethods(MappedByteBuffer.class);
        registerNativeMethod(searchMethod(tm, "isLoaded0", long.class, long.class, int.class),
                getExecutableData(searchMethod(mm, "isLoaded0", long.class, long.class, int.class)));
        registerNativeMethod(searchMethod(tm, "force0", FileDescriptor.class, long.class, long.class),
                getExecutableData(searchMethod(mm, "force0", FileDescriptor.class, long.class, long.class)));
    }
}

package com.v7878.unsafe.io;

import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.putIntO;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getHiddenInstanceField;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.foreign.LibDLExt;

import java.io.FileDescriptor;
import java.util.Objects;

public class IOUtils {
    private static final int file_descriptor_offset =
            fieldOffset(getHiddenInstanceField(FileDescriptor.class, "descriptor"));

    public static int getDescriptorValue(FileDescriptor fd) {
        Objects.requireNonNull(fd);
        return getIntO(fd, file_descriptor_offset);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setDescriptorValue(FileDescriptor fd, int value) {
        Objects.requireNonNull(fd);
        putIntO(fd, file_descriptor_offset, value);
    }

    public static FileDescriptor newFileDescriptor(int value) {
        FileDescriptor out = allocateInstance(FileDescriptor.class);
        setDescriptorValue(out, value);
        return out;
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();
        private static final SymbolLookup CUTILS =
                LibDLExt.systemLibraryLookup("libcutils.so", SCOPE);

        @LibrarySymbol(name = "ashmem_valid")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT})
        abstract int ashmem_valid(int fd);

        @LibrarySymbol(name = "ashmem_create_region")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int ashmem_create_region(long name, long size);

        @LibrarySymbol(name = "ashmem_set_prot_region")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, INT})
        abstract int ashmem_set_prot_region(int fd, int prot);

        @LibrarySymbol(name = "ashmem_pin_region")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract int ashmem_pin_region(int fd, long offset, long len);

        @LibrarySymbol(name = "ashmem_unpin_region")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract int ashmem_unpin_region(int fd, long offset, long len);

        @LibrarySymbol(name = "ashmem_get_size_region")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT})
        abstract int ashmem_get_size_region(int fd);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, CUTILS));
    }

    public static void ashmem_valid(FileDescriptor fd) throws ErrnoException {
        int value = Native.INSTANCE.ashmem_valid(getDescriptorValue(fd));
        if (value < 0) {
            throw new ErrnoException("ashmem_valid", Errno.errno());
        }
    }

    public static FileDescriptor ashmem_create_region(String name, long size) throws ErrnoException {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            int value = Native.INSTANCE.ashmem_create_region(c_name.nativeAddress(), size);
            if (value == -1) {
                throw new ErrnoException("ashmem_create_region", Errno.errno());
            }
            return newFileDescriptor(value);
        }
    }

    public static void ashmem_set_prot_region(FileDescriptor fd, int prot) throws ErrnoException {
        int value = Native.INSTANCE.ashmem_set_prot_region(getDescriptorValue(fd), prot);
        if (value < 0) {
            throw new ErrnoException("ashmem_set_prot_region", Errno.errno());
        }
    }

    public static void ashmem_pin_region(FileDescriptor fd, long offset, long len) throws ErrnoException {
        int value = Native.INSTANCE.ashmem_pin_region(getDescriptorValue(fd), offset, len);
        if (value < 0) {
            throw new ErrnoException("ashmem_pin_region", Errno.errno());
        }
    }

    public static void ashmem_unpin_region(FileDescriptor fd, long offset, long len) throws ErrnoException {
        int value = Native.INSTANCE.ashmem_unpin_region(getDescriptorValue(fd), offset, len);
        if (value < 0) {
            throw new ErrnoException("ashmem_unpin_region", Errno.errno());
        }
    }

    public static int ashmem_get_size_region(FileDescriptor fd) throws ErrnoException {
        int value = Native.INSTANCE.ashmem_get_size_region(getDescriptorValue(fd));
        if (value < 0) {
            throw new ErrnoException("ashmem_get_size_region", Errno.errno());
        }
        return value;
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class EarlyNative {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "mprotect")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int mprotect(long addr, long len, int prot);

        @LibrarySymbol(name = "madvise")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int madvise(long address, long length, int advice);

        static final EarlyNative INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, EarlyNative.class));
    }

    public static void mprotect(long address, long length, int prot) throws ErrnoException {
        int value = EarlyNative.INSTANCE.mprotect(address, length, prot);
        if (value < 0) {
            throw new ErrnoException("mprotect", Errno.errno());
        }
    }

    public static final int MADV_NORMAL = 0;
    public static final int MADV_RANDOM = 1;
    public static final int MADV_SEQUENTIAL = 2;
    public static final int MADV_WILLNEED = 3;
    public static final int MADV_DONTNEED = 4;

    public static void madvise(long address, long length, int advice) throws ErrnoException {
        int value = EarlyNative.INSTANCE.madvise(address, length, advice);
        if (value < 0) {
            throw new ErrnoException("madvise", Errno.errno());
        }
    }

    public static final int MAP_ANONYMOUS = 0x20;

    public static MemorySegment mmap(long address, FileDescriptor fd, long offset,
                                     long length, int prot, int flags, Arena scope) throws ErrnoException {
        Objects.requireNonNull(scope);
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        if ((flags & MAP_ANONYMOUS) != 0) {
            if (fd != null) {
                throw new IllegalArgumentException(
                        "FileDescriptor must be null if the MAP_ANONYMOUS flag is set");
            }
        } else if (!fd.valid()) {
            throw new IllegalStateException("FileDescriptor is not valid");
        }
        long mmap_address = Os.mmap(address, length, prot, flags, fd, offset);
        boolean readOnly = (prot & OsConstants.PROT_WRITE) == 0;
        return JavaForeignAccess.mapSegment(new UnmapperProxy() {
            @Override
            public long address() {
                return mmap_address;
            }

            @Override
            public FileDescriptor fileDescriptor() {
                return fd;
            }

            @Override
            public void unmap() {
                try {
                    Os.munmap(mmap_address, length);
                } catch (ErrnoException ignored) {
                    // swallow exception
                }
            }
        }, length, readOnly, scope);
    }

    public static MemorySegment mmap(FileDescriptor fd, int prot, long offset,
                                     long length, Arena scope) throws ErrnoException {
        return mmap(0, fd, offset, length, prot, OsConstants.MAP_PRIVATE, scope);
    }

    private static final int PROT_RWX = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;

    public static MemorySegment mmap(FileDescriptor fd, long offset, long length, Arena scope) throws ErrnoException {
        return mmap(fd, PROT_RWX, offset, length, scope);
    }
}

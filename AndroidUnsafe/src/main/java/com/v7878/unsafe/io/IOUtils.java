package com.v7878.unsafe.io;

import static android.system.OsConstants.EINTR;
import static android.system.OsConstants.F_GETFL;
import static android.system.OsConstants.O_APPEND;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.NEW_INSTANCE;
import static com.v7878.unsafe.access.AccessLinker.FieldAccess;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;
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
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.Errno;

import java.io.FileDescriptor;
import java.util.Objects;
import java.util.function.IntSupplier;

public class IOUtils {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.io.FileDescriptor", name = "descriptor")
        abstract int descriptor(FileDescriptor instance);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.io.FileDescriptor", name = "descriptor")
        abstract void descriptor(FileDescriptor instance, int value);

        @ExecutableAccess(kind = NEW_INSTANCE, klass = "java.io.FileDescriptor",
                name = "<init>", args = {"int"})
        abstract FileDescriptor newFileDescriptor(int value);

        public static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    @SuppressWarnings("SameParameterValue")
    private static int tempFailureRetry(String functionName, IntSupplier impl) throws ErrnoException {
        int rc;
        int errno = 0;
        do {
            rc = impl.getAsInt();
        } while (rc == -1 && (errno = Errno.errno()) == EINTR);
        if (rc == -1) {
            throw new ErrnoException(functionName, errno);
        }
        return rc;
    }

    public static int getDescriptorValue(FileDescriptor fd) {
        return AccessI.INSTANCE.descriptor(fd);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setDescriptorValue(FileDescriptor fd, int value) {
        AccessI.INSTANCE.descriptor(fd, value);
    }

    public static FileDescriptor newFileDescriptor(int value) {
        return AccessI.INSTANCE.newFileDescriptor(value);
    }

    public static boolean getAppendFlag(FileDescriptor fd) {
        try {
            int flags = fcntl_void(fd, F_GETFL);
            return (flags & O_APPEND) != 0;
        } catch (ErrnoException e) {
            return false;
        }
    }

    public record ScopedFD(FileDescriptor value) implements FineClosable {
        @Override
        public void close() {
            try {
                Os.close(value);
            } catch (ErrnoException e) { /* swallow exception */ }
        }
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();
        private static final SymbolLookup CUTILS =
                SymbolLookup.libraryLookup("libcutils.so", SCOPE);

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

        @LibrarySymbol(name = "mprotect")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int mprotect(long addr, long len, int prot);

        @LibrarySymbol(name = "madvise")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD, INT})
        abstract int madvise(long address, long length, int advice);

        @LibrarySymbol(name = "fcntl")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, INT, INT})
        abstract int fcntl_arg(int fd, int cmd, int arg);

        @LibrarySymbol(name = "fcntl")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, INT})
        abstract int fcntl_void(int fd, int cmd);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, CUTILS);
    }

    public static void ashmem_valid(FileDescriptor fd) throws ErrnoException {
        int value = IOUtils.Native.INSTANCE.ashmem_valid(getDescriptorValue(fd));
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
            int value = IOUtils.Native.INSTANCE.ashmem_create_region(c_name.nativeAddress(), size);
            if (value == -1) {
                throw new ErrnoException("ashmem_create_region", Errno.errno());
            }
            return newFileDescriptor(value);
        }
    }

    public static ScopedFD ashmem_create_scoped_region(String name, long size) throws ErrnoException {
        return new ScopedFD(ashmem_create_region(name, size));
    }

    public static void ashmem_set_prot_region(FileDescriptor fd, int prot) throws ErrnoException {
        int value = IOUtils.Native.INSTANCE.ashmem_set_prot_region(getDescriptorValue(fd), prot);
        if (value < 0) {
            throw new ErrnoException("ashmem_set_prot_region", Errno.errno());
        }
    }

    public static void ashmem_pin_region(FileDescriptor fd, long offset, long len) throws ErrnoException {
        int value = IOUtils.Native.INSTANCE.ashmem_pin_region(getDescriptorValue(fd), offset, len);
        if (value < 0) {
            throw new ErrnoException("ashmem_pin_region", Errno.errno());
        }
    }

    public static void ashmem_unpin_region(FileDescriptor fd, long offset, long len) throws ErrnoException {
        int value = IOUtils.Native.INSTANCE.ashmem_unpin_region(getDescriptorValue(fd), offset, len);
        if (value < 0) {
            throw new ErrnoException("ashmem_unpin_region", Errno.errno());
        }
    }

    public static int ashmem_get_size_region(FileDescriptor fd) throws ErrnoException {
        int value = IOUtils.Native.INSTANCE.ashmem_get_size_region(getDescriptorValue(fd));
        if (value < 0) {
            throw new ErrnoException("ashmem_get_size_region", Errno.errno());
        }
        return value;
    }

    public static void mprotect(long address, long length, int prot) throws ErrnoException {
        int value = Native.INSTANCE.mprotect(address, length, prot);
        if (value < 0) {
            throw new ErrnoException("mprotect", Errno.errno());
        }
    }

    public static int fcntl_arg(FileDescriptor fd, int cmd, int arg) throws ErrnoException {
        int fd_int = getDescriptorValue(fd);
        return tempFailureRetry("fcntl", () -> Native.INSTANCE.fcntl_arg(fd_int, cmd, arg));
    }

    public static int fcntl_void(FileDescriptor fd, int cmd) throws ErrnoException {
        int fd_int = getDescriptorValue(fd);
        return tempFailureRetry("fcntl", () -> Native.INSTANCE.fcntl_void(fd_int, cmd));
    }

    public static final int MADV_NORMAL = 0;
    public static final int MADV_RANDOM = 1;
    public static final int MADV_SEQUENTIAL = 2;
    public static final int MADV_WILLNEED = 3;
    public static final int MADV_DONTNEED = 4;

    public static void madvise(long address, long length, int advice) throws ErrnoException {
        int value = Native.INSTANCE.madvise(address, length, advice);
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

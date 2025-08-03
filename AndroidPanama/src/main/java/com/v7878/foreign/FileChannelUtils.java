package com.v7878.foreign;

import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.access.ChannelsAccess;
import com.v7878.unsafe.access.ChannelsAccess.NativeThreadSet;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.Objects;

@PortApi
public final class FileChannelUtils {
    private FileChannelUtils() {
    }

    @SuppressWarnings("unused")
    private static class IOStatus {
        public static final int EOF = -1;              // End of file
        public static final int UNAVAILABLE = -2;      // Nothing available (non-blocking)
        public static final int INTERRUPTED = -3;      // System call interrupted
        public static final int UNSUPPORTED = -4;      // Operation not supported
        public static final int THROWN = -5;           // Exception thrown in JNI code
        public static final int UNSUPPORTED_CASE = -6; // This case not supported

        // Return true iff n is not one of the IOStatus values
        public static boolean checkAll(long n) {
            return ((n > EOF) || (n < UNSUPPORTED_CASE));
        }
    }

    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int MAP_PV = 2;

    private static int toProt(MapMode mode) {
        if (mode == MapMode.READ_ONLY) {
            return MAP_RO;
        } else if (mode == MapMode.READ_WRITE) {
            return MAP_RW;
        } else if (mode == MapMode.PRIVATE) {
            return MAP_PV;
        }
        throw new UnsupportedOperationException();
    }

    private static void checkMode(FileChannel channel, MapMode mode) {
        if (mode != MapMode.READ_ONLY && !ChannelsAccess.isWritable(channel))
            throw new NonWritableChannelException();
        if (!ChannelsAccess.isReadable(channel))
            throw new NonReadableChannelException();
    }

    public static MemorySegment map(FileChannel channel, MapMode mode,
                                    long offset, long size, Arena arena) throws IOException {
        ChannelsAccess.ensureOpen(channel);
        Objects.requireNonNull(mode, "Mode is null");
        Objects.requireNonNull(arena, "Arena is null");
        JavaForeignAccess.checkValidState(arena.scope());
        if (offset < 0)
            throw new IllegalArgumentException("Requested bytes offset must be >= 0.");
        if (size < 0)
            throw new IllegalArgumentException("Requested bytes size must be >= 0.");
        if (offset + size < 0)
            throw new IllegalArgumentException("Position + size overflow");

        checkMode(channel, mode);

        int prot = toProt(mode);
        UnmapperProxy unmapper = mapInternal(channel, offset, size, prot);
        boolean readOnly = prot == MAP_RO;
        return JavaForeignAccess.mapSegment(unmapper, size, readOnly, arena);
    }

    private static UnmapperProxy mapInternal(
            FileChannel channel, long position,
            long size, int prot) throws IOException {
        long granularity = ChannelsAccess.allocationGranularity();
        FileDescriptor fd = ChannelsAccess.getFD(channel);
        NativeThreadSet threads = ChannelsAccess.getThreadSet(channel);

        long addr = -1;
        int ti = -1;
        try {
            ChannelsAccess.begin(channel);
            ti = threads.add();
            if (!channel.isOpen())
                return null;

            long mapSize;
            int pagePosition;
            synchronized (ChannelsAccess.positionLock(channel)) {
                long filesize;
                do {
                    filesize = size(fd);
                } while ((filesize == IOStatus.INTERRUPTED) && channel.isOpen());
                if (!channel.isOpen())
                    return null;

                if (filesize < position + size) { // Extend file size
                    if (!ChannelsAccess.isWritable(channel)) {
                        throw new IOException("Channel not open for writing " +
                                "- cannot extend file to required size");
                    }
                    int rv;
                    do {
                        rv = truncate(fd, position + size);
                    } while ((rv == IOStatus.INTERRUPTED) && channel.isOpen());
                    if (!channel.isOpen())
                        return null;
                }

                if (size == 0) {
                    return null;
                }

                pagePosition = (int) (position % granularity);
                long mapPosition = position - pagePosition;
                mapSize = size + pagePosition;
                try {
                    // If map did not throw an exception, the address is valid
                    addr = map(fd, prot, mapPosition, mapSize);
                } catch (OutOfMemoryError x) {
                    // An OutOfMemoryError may indicate that we've exhausted
                    // memory so force gc and re-attempt map
                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException y) {
                        Thread.currentThread().interrupt();
                    }
                    try {
                        addr = map(fd, prot, mapPosition, mapSize);
                    } catch (OutOfMemoryError y) {
                        // After a second OOME, fail
                        throw new IOException("Map failed", y);
                    }
                }
            } // synchronized

            // On Windows, and potentially other platforms, we need an open
            // file descriptor for some mapping operations.
            FileDescriptor mfd = duplicateForMapping(fd);

            assert (IOStatus.checkAll(addr));
            assert (addr % granularity == 0);
            return new Unmapper(mfd, addr, pagePosition, mapSize);
        } finally {
            threads.remove(ti);
            ChannelsAccess.end(channel, IOStatus.checkAll(addr));
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class Unmapper implements UnmapperProxy {
        private final long address;
        private final long size;
        private final FileDescriptor fd;
        private final int pagePosition;

        private Unmapper(FileDescriptor fd, long address, int pagePosition, long size) {
            assert (address != 0);
            this.fd = fd;
            this.address = address;
            this.pagePosition = pagePosition;
            this.size = size;
        }

        @Override
        public long address() {
            return address + pagePosition;
        }

        @Override
        public FileDescriptor fileDescriptor() {
            return fd;
        }

        @Override
        public void unmap() {
            try {
                unmap0(address, size);
            } catch (IOException ignored) {
                // swallow exception
            }
        }
    }

    private static long map(FileDescriptor fd, int prot, long position, long length) throws IOException {
        //BlockGuard.getThreadPolicy().onReadFromDisk();
        return map0(fd, prot, position, length);
    }

    private static long size(FileDescriptor fd) throws IOException {
        //BlockGuard.getThreadPolicy().onReadFromDisk();
        return size0(fd);
    }

    private static int truncate(FileDescriptor fd, long size) throws IOException {
        //BlockGuard.getThreadPolicy().onWriteToDisk();
        return truncate0(fd, size);
    }

    @SuppressWarnings("unused")
    private static FileDescriptor duplicateForMapping(FileDescriptor fd) {
        // file descriptor not required for mapping operations;
        // okay to return invalid file descriptor.
        return IOUtils.newFileDescriptor(-1);
    }

    private static IOException rethrowAsIOException(ErrnoException errno) throws IOException {
        throw new IOException(errno.getMessage(), errno);
    }

    private static long map0(FileDescriptor fd, int prot, long position, long length) throws IOException {
        int protections;
        int flags;

        switch (prot) {
            case MAP_RO -> {
                protections = OsConstants.PROT_READ;
                flags = OsConstants.MAP_SHARED;
            }
            case MAP_RW -> {
                protections = OsConstants.PROT_WRITE | OsConstants.PROT_READ;
                flags = OsConstants.MAP_SHARED;
            }
            case MAP_PV -> {
                protections = OsConstants.PROT_WRITE | OsConstants.PROT_READ;
                flags = OsConstants.MAP_PRIVATE;
            }
            default -> throw shouldNotReachHere();
        }

        try {
            return Os.mmap(0, length, protections, flags, fd, position);
        } catch (ErrnoException errno) {
            if (errno.errno == OsConstants.ENOMEM) {
                throw new OutOfMemoryError("Map failed");
            }
            throw rethrowAsIOException(errno);
        }
    }

    private static void unmap0(long address, long length) throws IOException {
        try {
            Os.munmap(address, length);
        } catch (ErrnoException errno) {
            throw rethrowAsIOException(errno);
        }
    }

    private static Class<?> fileDispatcherClass() {
        class Holder {
            static final Class<?> CLASS = ClassUtils.sysClass("sun.nio.ch.FileDispatcherImpl");
        }
        return Holder.CLASS;
    }

    @SuppressWarnings("RedundantThrows")
    private static int truncate0(FileDescriptor fd, long size) throws IOException {
        class Holder {
            static final MethodHandle TRUNCATE = unreflect(getHiddenMethod(
                    fileDispatcherClass(), "truncate0",
                    FileDescriptor.class, long.class));
        }
        return nothrows_run(() -> (int) Holder.TRUNCATE.invokeExact(fd, size));
    }

    @SuppressWarnings("RedundantThrows")
    private static long size0(FileDescriptor fd) throws IOException {
        class Holder {
            static final MethodHandle SIZE = unreflect(getHiddenMethod(
                    fileDispatcherClass(), "size0", FileDescriptor.class));
        }
        return nothrows_run(() -> (long) Holder.SIZE.invokeExact(fd));
    }
}

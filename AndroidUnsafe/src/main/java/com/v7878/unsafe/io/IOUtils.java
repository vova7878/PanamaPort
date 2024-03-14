package com.v7878.unsafe.io;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.putIntO;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;
import com.v7878.unsafe.foreign.Errno;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.function.Supplier;

import dalvik.annotation.optimization.CriticalNative;

public class IOUtils {

    private static final int file_descriptor_offset = nothrows_run(
            () -> fieldOffset(getDeclaredField(FileDescriptor.class, "descriptor")));

    private static final Supplier<Class<?>> file_channel_impl_class = runOnce(() ->
            nothrows_run(() -> Class.forName("sun.nio.ch.FileChannelImpl")));

    private static final Supplier<MethodHandle> file_channel_open =
            runOnce(() -> unreflect(getDeclaredMethod(file_channel_impl_class.get(),
                    "open", FileDescriptor.class, String.class, boolean.class,
                    boolean.class, boolean.class, Object.class)));

    public static FileChannel openFileChannel(FileDescriptor fd, String path,
                                              boolean readable, boolean writable,
                                              boolean append, Object parent) {
        return nothrows_run(() -> (FileChannel) file_channel_open.get()
                .invokeExact(fd, path, readable, writable, append, parent));
    }

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

    static {
        Class<?> word = IS64BIT ? long.class : int.class;
        String suffix = IS64BIT ? "64" : "32";
        MemorySegment ashmem_create_region = Linker.nativeLinker().defaultLookup()
                .find("ashmem_create_region").orElseThrow(ExceptionInInitializerError::new);
        Method ashmem_create_region_m = getDeclaredMethod(IOUtils.class,
                "raw_ashmem_create_region" + suffix, word, word);
        registerNativeMethod(ashmem_create_region_m, ashmem_create_region.nativeAddress());
    }

    @Keep
    @CriticalNative
    private static native int raw_ashmem_create_region64(long name, long size);

    @Keep
    @CriticalNative
    private static native int raw_ashmem_create_region32(int name, int size);

    private static int raw_ashmem_create_region(long name, long size) {
        return IS64BIT ? raw_ashmem_create_region64(name, size) :
                raw_ashmem_create_region32((int) name, (int) size);
    }

    public static FileDescriptor ashmem_create_region(String name, long size) throws ErrnoException {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_name = arena.allocateFrom(name);
            int value = raw_ashmem_create_region(c_name.address(), size);
            if (value == -1) {
                throw new ErrnoException("memfd_create", Errno.errno());
            }
            return newFileDescriptor(value);
        }
    }

    //TODO
    //int ashmem_set_prot_region(int fd, int prot);
    //int ashmem_pin_region(int fd, size_t offset, size_t len);
    //int ashmem_unpin_region(int fd, size_t offset, size_t len);
    //int ashmem_get_size_region(int fd);

    public static final int MAP_ANONYMOUS = 0x20;

    public static MemorySegment mmap(MemorySegment address, FileDescriptor fd, long offset,
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
                throw new IllegalArgumentException("FileDescriptor must be null if the MAP_ANONYMOUS flag is set");
            }
        } else if (!fd.valid()) {
            throw new IllegalStateException("FileDescriptor is not valid");
        }
        if (address == null) {
            address = MemorySegment.NULL;
        }
        long mmap_address = Os.mmap(address.nativeAddress(), length, prot, flags, fd, offset);
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
                } catch (ErrnoException e) { /* swallow exception */ }
            }
        }, length, readOnly, scope);
    }

    public static MemorySegment mmap(FileDescriptor fd, int prot, long offset,
                                     long length, Arena scope) throws ErrnoException {
        return mmap(null, fd, offset, length, prot, OsConstants.MAP_PRIVATE, scope);
    }

    private static final int PROT_RWX = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;

    public static MemorySegment mmap(FileDescriptor fd, long offset, long length, Arena scope) throws ErrnoException {
        return mmap(fd, PROT_RWX, offset, length, scope);
    }
}

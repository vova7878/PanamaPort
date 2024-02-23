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

    private static final int PROT_MASK = OsConstants.PROT_READ | OsConstants.PROT_WRITE
            | OsConstants.PROT_EXEC | OsConstants.PROT_NONE;
    private static final int PROT_RWX = OsConstants.PROT_READ | OsConstants.PROT_WRITE
            | OsConstants.PROT_EXEC;

    private static void validateProt(int prot) {
        if ((prot & ~PROT_MASK) != 0) {
            throw new IllegalArgumentException("Invalid prot value");
        }
    }

    public static MemorySegment map(FileDescriptor fd, int prot, long offset,
                                    long length, Arena scope) throws ErrnoException {
        if (!fd.valid()) {
            throw new IllegalStateException("FileDescriptor is not valid");
        }
        validateProt(prot);
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        long address = Os.mmap(0, length, prot, OsConstants.MAP_PRIVATE, fd, offset);
        boolean readOnly = (prot & OsConstants.PROT_WRITE) == 0;
        return JavaForeignAccess.mapSegment(new UnmapperProxy() {
            @Override
            public long address() {
                return address;
            }

            @Override
            public FileDescriptor fileDescriptor() {
                return fd;
            }

            @Override
            public void unmap() {
                try {
                    Os.munmap(address, length);
                } catch (ErrnoException e) { /* swallow exception */ }
            }
        }, length, readOnly, scope);
    }

    public static MemorySegment map(FileDescriptor fd, long offset,
                                    long length, Arena scope) throws ErrnoException {
        return map(fd, PROT_RWX, offset, length, scope);
    }
}

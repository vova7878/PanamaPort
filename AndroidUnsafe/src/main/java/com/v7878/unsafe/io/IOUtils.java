package com.v7878.unsafe.io;

import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.function.Supplier;

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
}

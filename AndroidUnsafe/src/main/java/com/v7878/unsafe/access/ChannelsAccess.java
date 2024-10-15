package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.unsafe.AndroidUnsafe;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.Objects;

public class ChannelsAccess {
    public static void ensureOpen(FileChannel channel) throws IOException {
        Objects.requireNonNull(channel);
        if (!channel.isOpen()) throw new ClosedChannelException();
    }

    public static void begin(AbstractInterruptibleChannel channel) {
        class Holder {
            static final MethodHandle BEGIN = unreflect(getDeclaredMethod(
                    AbstractInterruptibleChannel.class, "begin"));
        }
        Objects.requireNonNull(channel);
        nothrows_run(() -> Holder.BEGIN.invoke(channel));
    }

    public static void end(AbstractInterruptibleChannel channel, boolean completed) {
        class Holder {
            static final MethodHandle END = unreflect(getDeclaredMethod(
                    AbstractInterruptibleChannel.class, "end", boolean.class));
        }
        Objects.requireNonNull(channel);
        nothrows_run(() -> Holder.END.invoke(channel, completed));
    }

    private static Class<?> fileChannelClass() {
        class Holder {
            static final Class<?> CLASS = nothrows_run(() ->
                    Class.forName("sun.nio.ch.FileChannelImpl"));
        }
        return Holder.CLASS;
    }

    public static long allocationGranularity() {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "allocationGranularity"));
            static final long VALUE = AndroidUnsafe.getLongO(fileChannelClass(), OFFSET);
        }
        return Holder.VALUE;
    }

    public static FileChannel openFileChannel(FileDescriptor fd, String path,
                                              boolean readable, boolean writable,
                                              boolean append, Object parent) {
        class Holder {
            static final MethodHandle OPEN = unreflect(
                    getDeclaredMethod(fileChannelClass(), "open",
                            FileDescriptor.class, String.class, boolean.class,
                            boolean.class, boolean.class, Object.class));
        }
        return nothrows_run(() -> (FileChannel) Holder.OPEN
                .invokeExact(fd, path, readable, writable, append, parent));
    }


    public static boolean isReadable(FileChannel channel) {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "readable"));
        }
        Objects.requireNonNull(channel);
        return AndroidUnsafe.getBooleanO(channel, Holder.OFFSET);
    }

    public static boolean isWritable(FileChannel channel) {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "writable"));
        }
        Objects.requireNonNull(channel);
        return AndroidUnsafe.getBooleanO(channel, Holder.OFFSET);
    }

    public static Object positionLock(FileChannel channel) {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "positionLock"));
        }
        Objects.requireNonNull(channel);
        return AndroidUnsafe.getObject(channel, Holder.OFFSET);
    }

    public static FileDescriptor getFD(FileChannel channel) {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "fd"));
        }
        Objects.requireNonNull(channel);
        return (FileDescriptor) AndroidUnsafe.getObject(channel, Holder.OFFSET);
    }

    private static Class<?> threadSetClass() {
        class Holder {
            static final Class<?> CLASS = nothrows_run(() ->
                    Class.forName("sun.nio.ch.NativeThreadSet"));
        }
        return Holder.CLASS;
    }

    public interface NativeThreadSet {
        int add();

        void remove(int i);
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class NativeThreadSetImpl implements NativeThreadSet {
        private final Object instance;

        private NativeThreadSetImpl(Object instance) {
            this.instance = instance;
        }

        public int add() {
            class Holder {
                static final MethodHandle ADD = unreflect(
                        getDeclaredMethod(threadSetClass(), "add"));
            }
            return nothrows_run(() -> (int) Holder.ADD.invoke(instance));
        }

        public void remove(int i) {
            class Holder {
                static final MethodHandle REMOVE = unreflect(getDeclaredMethod(
                        threadSetClass(), "remove", int.class));
            }
            nothrows_run(() -> Holder.REMOVE.invoke(instance, i));
        }
    }

    public static NativeThreadSet getThreadSet(FileChannel channel) {
        class Holder {
            static final long OFFSET = fieldOffset(getDeclaredField(
                    fileChannelClass(), "threads"));
        }
        Objects.requireNonNull(channel);
        return new NativeThreadSetImpl(AndroidUnsafe.getObject(channel, Holder.OFFSET));
    }
}

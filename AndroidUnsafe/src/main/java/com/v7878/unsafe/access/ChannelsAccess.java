package com.v7878.unsafe.access;

import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.STATIC;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.STATIC_GETTER;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.access.AccessLinker.Conditions;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import com.v7878.unsafe.access.AccessLinker.FieldAccess;
import com.v7878.unsafe.io.IOUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.Objects;

public class ChannelsAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @ExecutableAccess(kind = VIRTUAL, klass = "java.nio.channels.spi.AbstractInterruptibleChannel",
                name = "begin", args = {})
        abstract void begin(AbstractInterruptibleChannel instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "java.nio.channels.spi.AbstractInterruptibleChannel",
                name = "end", args = {"boolean"})
        abstract void end(AbstractInterruptibleChannel instance, boolean completed);

        @FieldAccess(kind = STATIC_GETTER, klass = "sun.nio.ch.FileChannelImpl",
                name = "allocationGranularity")
        abstract long allocationGranularity();

        @ApiSensitive
        @ExecutableAccess(conditions = @Conditions(max_art = 34),
                kind = STATIC, klass = "sun.nio.ch.FileChannelImpl",
                name = "open", args = {"java.io.FileDescriptor", "java.lang.String",
                "boolean", "boolean", "boolean", "java.lang.Object"})
        abstract FileChannel openFileChannel(FileDescriptor fd, String path, boolean readable,
                                             boolean writable, boolean append, Object parent);

        @ApiSensitive
        @ExecutableAccess(conditions = @Conditions(min_art = 35),
                kind = STATIC, klass = "sun.nio.ch.FileChannelImpl",
                name = "open", args = {"java.io.FileDescriptor", "java.lang.String",
                "boolean", "boolean", "java.lang.Object"})
        abstract FileChannel openFileChannel(FileDescriptor fd, String path, boolean readable,
                                             boolean writable, Object parent);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "sun.nio.ch.FileChannelImpl", name = "readable")
        abstract boolean readable(FileChannel instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "sun.nio.ch.FileChannelImpl", name = "writable")
        abstract boolean writable(FileChannel instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "sun.nio.ch.FileChannelImpl", name = "positionLock")
        abstract Object positionLock(FileChannel instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "sun.nio.ch.FileChannelImpl", name = "fd")
        abstract FileDescriptor getFD(FileChannel instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "sun.nio.ch.FileChannelImpl", name = "threads")
        abstract Object threads(FileChannel instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "sun.nio.ch.NativeThreadSet", name = "add", args = {})
        abstract int add(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "sun.nio.ch.NativeThreadSet", name = "remove", args = {"int"})
        abstract void remove(Object instance, int n);

        @ExecutableAccess(kind = VIRTUAL, klass = "sun.nio.ch.NativeThreadSet", name = "signalAndWait", args = {})
        abstract void signalAndWait(Object instance);

        public static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    public static void ensureOpen(FileChannel channel) throws IOException {
        Objects.requireNonNull(channel);
        if (!channel.isOpen()) throw new ClosedChannelException();
    }

    public static void begin(AbstractInterruptibleChannel channel) {
        AccessI.INSTANCE.begin(channel);
    }

    public static void end(AbstractInterruptibleChannel channel, boolean completed) {
        AccessI.INSTANCE.end(channel, completed);
    }

    public static long allocationGranularity() {
        class Holder {
            static final long VALUE = AccessI.INSTANCE.allocationGranularity();
        }
        return Holder.VALUE;
    }

    public static FileChannel openFileChannel(
            FileDescriptor fd, String path, boolean readable, boolean writable, Object parent) {
        if (ART_SDK_INT >= 35) {
            return AccessI.INSTANCE.openFileChannel(fd, path, readable, writable, parent);
        } else {
            boolean append = IOUtils.getAppendFlag(fd);
            return AccessI.INSTANCE.openFileChannel(fd, path, readable, writable, append, parent);
        }
    }

    public static boolean isReadable(FileChannel channel) {
        // TODO: check channel is FileChannelImpl instance
        return AccessI.INSTANCE.readable(channel);
    }

    public static boolean isWritable(FileChannel channel) {
        // TODO: check channel is FileChannelImpl instance
        return AccessI.INSTANCE.writable(channel);
    }

    public static Object positionLock(FileChannel channel) {
        // TODO: check channel is FileChannelImpl instance
        return AccessI.INSTANCE.positionLock(channel);
    }

    public static FileDescriptor getFD(FileChannel channel) {
        // TODO: check channel is FileChannelImpl instance
        return AccessI.INSTANCE.getFD(channel);
    }

    public interface NativeThreadSet {
        int add();

        void remove(int i);

        void signalAndWait();
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class NativeThreadSetImpl implements NativeThreadSet {
        private final Object instance;

        private NativeThreadSetImpl(Object instance) {
            this.instance = instance;
        }

        public int add() {
            return AccessI.INSTANCE.add(instance);
        }

        public void remove(int i) {
            AccessI.INSTANCE.remove(instance, i);
        }

        public void signalAndWait() {
            AccessI.INSTANCE.signalAndWait(instance);
        }
    }

    public static NativeThreadSet getThreadSet(FileChannel channel) {
        // TODO: check channel is FileChannelImpl instance
        return new NativeThreadSetImpl(AccessI.INSTANCE.threads(channel));
    }
}

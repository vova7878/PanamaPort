package com.v7878.unsafe.access;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AsynchronousSocketChannelBase;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@DoNotShrink
@DoNotObfuscate
final class AsynchronousSocketChannelHook extends AsynchronousSocketChannelBase {
    @Override
    public <A> void read(ByteBuffer[] dsts,
                         int offset,
                         int length,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Long, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScopes(dsts, true)) {
            super.read(dsts, offset, length, timeout, unit, attachment, handler);
        }
    }

    @Override
    public <A> void read(ByteBuffer dst,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Integer, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScope(dst, true)) {
            super.read(dst, timeout, unit, attachment, handler);
        }
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        try (var ignored1 = JavaNioAccess.lockScope(dst, true)) {
            return super.read(dst);
        }
    }

    @Override
    public <A> void write(ByteBuffer[] srcs,
                          int offset,
                          int length,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Long, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScopes(srcs, true)) {
            super.write(srcs, offset, length, timeout, unit, attachment, handler);
        }
    }

    @Override
    public <A> void write(ByteBuffer src,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Integer, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScope(src, true)) {
            super.write(src, timeout, unit, attachment, handler);
        }
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        try (var ignored1 = JavaNioAccess.lockScope(src, true)) {
            return super.write(src);
        }
    }
}

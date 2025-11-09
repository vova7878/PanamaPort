package com.v7878.unsafe.access;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AsynchronousFileChannelBase;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

@DoNotShrink
@DoNotObfuscate
final class AsynchronousFileChannelHook extends AsynchronousFileChannelBase {
    @Override
    public <A> void read(ByteBuffer dst,
                         long position,
                         A attachment,
                         CompletionHandler<Integer, ? super A> handler) {
        JavaNioAccess.checkAsyncScope(dst);
        super.read(dst, position, attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst, long position) {
        JavaNioAccess.checkAsyncScope(dst);
        return super.read(dst, position);
    }

    @Override
    public <A> void write(ByteBuffer src,
                          long position,
                          A attachment,
                          CompletionHandler<Integer, ? super A> handler) {
        JavaNioAccess.checkAsyncScope(src);
        super.write(src, position, attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src, long position) {
        JavaNioAccess.checkAsyncScope(src);
        return super.write(src, position);
    }
}

package com.v7878.unsafe.access;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AsynchronousSocketChannelBase;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@DoNotShrink
@DoNotObfuscate
final class AsynchronousSocketChannelHook extends AsynchronousSocketChannelBase {
    private static ByteBuffer[] subsequence(ByteBuffer[] bs, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, bs.length);
        if ((offset == 0) && (length == bs.length)) {
            return bs;
        }
        ByteBuffer[] bs2 = new ByteBuffer[length];
        System.arraycopy(bs, offset, bs2, 0, length);
        return bs2;
    }

    @Override
    public <A> void read(ByteBuffer[] dsts,
                         int offset,
                         int length,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Long, ? super A> handler) {
        dsts = subsequence(dsts, offset, length);
        JavaNioAccess.checkAsyncScope(dsts);
        super.read(dsts, 0, dsts.length, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void read(ByteBuffer dst,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Integer, ? super A> handler) {
        JavaNioAccess.checkAsyncScope(dst);
        super.read(dst, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        JavaNioAccess.checkAsyncScope(dst);
        return super.read(dst);
    }

    @Override
    public <A> void write(ByteBuffer[] srcs,
                          int offset,
                          int length,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Long, ? super A> handler) {
        srcs = subsequence(srcs, offset, length);
        JavaNioAccess.checkAsyncScope(srcs);
        super.write(srcs, 0, srcs.length, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void write(ByteBuffer src,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Integer, ? super A> handler) {
        JavaNioAccess.checkAsyncScope(src);
        super.write(src, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        JavaNioAccess.checkAsyncScope(src);
        return super.write(src);
    }
}

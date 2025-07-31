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
    public <V extends Number, A> Future<V> implRead(boolean isScatteringRead,
                                                    ByteBuffer dst,
                                                    ByteBuffer[] dsts,
                                                    long timeout,
                                                    TimeUnit unit,
                                                    A attachment,
                                                    CompletionHandler<V, ? super A> handler) {
        if (isScatteringRead) {
            // TODO: check dsts
        } else {
            // TODO: check dst
        }
        return super.implRead(isScatteringRead, dst, dsts, timeout, unit, attachment, handler);
    }

    @Override
    public <V extends Number, A> Future<V> implWrite(boolean isGatheringWrite,
                                                     ByteBuffer src,
                                                     ByteBuffer[] srcs,
                                                     long timeout,
                                                     TimeUnit unit,
                                                     A attachment,
                                                     CompletionHandler<V, ? super A> handler) {
        if (isGatheringWrite) {
            // TODO: check srcs
        } else {
            // TODO: check src
        }
        return super.implWrite(isGatheringWrite, src, srcs, timeout, unit, attachment, handler);
    }
}

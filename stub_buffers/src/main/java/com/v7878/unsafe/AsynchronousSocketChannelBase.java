package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// Compile-time stub, real class will be generated at runtime
public class AsynchronousSocketChannelBase {
    public <V extends Number, A> Future<V> implRead(boolean isScatteringRead,
                                                    ByteBuffer dst,
                                                    ByteBuffer[] dsts,
                                                    long timeout,
                                                    TimeUnit unit,
                                                    A attachment,
                                                    CompletionHandler<V, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <V extends Number, A> Future<V> implWrite(boolean isGatheringWrite,
                                                     ByteBuffer src,
                                                     ByteBuffer[] srcs,
                                                     long timeout,
                                                     TimeUnit unit,
                                                     A attachment,
                                                     CompletionHandler<V, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }
}

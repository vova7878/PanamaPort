package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// Compile-time stub, real class will be generated at runtime
public class AsynchronousSocketChannelBase {
    public <A> void read(ByteBuffer dst,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public Future<Integer> read(ByteBuffer dst) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <A> void read(ByteBuffer[] dsts,
                         int offset,
                         int length,
                         long timeout,
                         TimeUnit unit,
                         A attachment,
                         CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <A> void write(ByteBuffer src,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public Future<Integer> write(ByteBuffer src) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <A> void write(ByteBuffer[] srcs,
                          int offset,
                          int length,
                          long timeout,
                          TimeUnit unit,
                          A attachment,
                          CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }
}

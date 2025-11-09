package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

// Compile-time stub, real class will be generated at runtime
public class AsynchronousFileChannelBase {
    public <A> void read(ByteBuffer dst,
                         long position,
                         A attachment,
                         CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public Future<Integer> read(ByteBuffer dst, long position) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <A> void write(ByteBuffer src,
                          long position,
                          A attachment,
                          CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public Future<Integer> write(ByteBuffer src, long position) {
        throw new UnsupportedOperationException("Stub!");
    }
}

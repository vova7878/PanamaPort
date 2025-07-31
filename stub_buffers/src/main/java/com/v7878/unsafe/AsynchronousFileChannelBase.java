package com.v7878.unsafe;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

// Compile-time stub, real class will be generated at runtime
public class AsynchronousFileChannelBase {
    public <A> Future<Integer> implRead(ByteBuffer dst,
                                        long position,
                                        A attachment,
                                        CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public <A> Future<Integer> implWrite(ByteBuffer src,
                                         long position,
                                         A attachment,
                                         CompletionHandler<Integer, ? super A> handler) {
        throw new UnsupportedOperationException("Stub!");
    }
}

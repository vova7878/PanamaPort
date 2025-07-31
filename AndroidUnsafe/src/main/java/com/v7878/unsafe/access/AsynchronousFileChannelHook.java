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
    public <A> Future<Integer> implRead(ByteBuffer dst,
                                        long position,
                                        A attachment,
                                        CompletionHandler<Integer, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScope(dst, true)) {
            return super.implRead(dst, position, attachment, handler);
        }
    }

    @Override
    public <A> Future<Integer> implWrite(ByteBuffer src,
                                         long position,
                                         A attachment,
                                         CompletionHandler<Integer, ? super A> handler) {
        try (var ignored1 = JavaNioAccess.lockScope(src, true)) {
            return super.implWrite(src, position, attachment, handler);
        }
    }
}

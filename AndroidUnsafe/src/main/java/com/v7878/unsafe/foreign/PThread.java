package com.v7878.unsafe.foreign;

import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

public class PThread {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "pthread_key_create")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int pthread_key_create(long key_ptr, long destructor);

        @LibrarySymbol(name = "pthread_key_delete")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT})
        abstract int pthread_key_delete(int key);

        @LibrarySymbol(name = "pthread_getspecific")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {INT})
        abstract long pthread_getspecific(int key);

        @LibrarySymbol(name = "pthread_setspecific")
        @CallSignature(type = CRITICAL, ret = INT, args = {INT, LONG_AS_WORD})
        abstract int pthread_setspecific(int key, long value);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class));
    }

    public static int pthread_key_create(long destructor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment key = arena.allocate(JAVA_INT);
            int err = Native.INSTANCE.pthread_key_create(key.nativeAddress(), destructor);
            if (err != 0) {
                throw new NativeCodeException("pthread_key_create", err);
            }
            return key.get(JAVA_INT, 0);
        }
    }

    public static void pthread_key_delete(int key) {
        int err = Native.INSTANCE.pthread_key_delete(key);
        if (err != 0) {
            throw new NativeCodeException("pthread_key_delete", err);
        }
    }

    public static long pthread_getspecific(int key) {
        return Native.INSTANCE.pthread_getspecific(key);
    }

    public static void pthread_setspecific(int key, long value) {
        int err = Native.INSTANCE.pthread_setspecific(key, value);
        if (err != 0) {
            throw new NativeCodeException("pthread_setspecific", err);
        }
    }
}

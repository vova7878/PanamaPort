package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import android.system.Os;

import com.v7878.foreign.Arena;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

public class Errno {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "__errno")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long __errno();

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class));
    }

    public static long __errno() {
        return Native.INSTANCE.__errno();
    }

    public static int errno() {
        return getIntN(__errno());
    }

    public static void errno(int value) {
        putIntN(__errno(), value);
    }

    public static String strerror(int errno) {
        return Os.strerror(errno);
    }
}

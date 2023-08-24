package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;

import android.system.Os;

import androidx.annotation.Keep;

import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

import dalvik.annotation.optimization.CriticalNative;

public class Errno {

    static {
        MemorySegment __errno = Linker.nativeLinker().defaultLookup()
                .find("__errno").orElseThrow(ExceptionInInitializerError::new);
        Method __errno_m = getDeclaredMethod(Errno.class, IS64BIT ? "__errno64" : "__errno32");
        setExecutableData(__errno_m, __errno.address());
    }

    @Keep
    @CriticalNative
    @SuppressWarnings("JavaJniMissingFunction")
    private static native long __errno64();

    @Keep
    @CriticalNative
    @SuppressWarnings("JavaJniMissingFunction")
    private static native int __errno32();

    public static long __errno() {
        return IS64BIT ? __errno64() : __errno32() & 0xffffffffL;
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

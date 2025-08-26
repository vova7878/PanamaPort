package com.v7878.unsafe;

import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getHiddenMethod;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;

public class Stack {
    static {
        registerNativeMethod(
                getHiddenMethod(Stack.class, "getStackClass2"),
                getExecutableData(getHiddenMethod(ClassUtils.sysClass(
                        "dalvik.system.VMStack"), "getStackClass2")));
    }

    @DoNotObfuscate
    @DoNotShrink
    public static native Class<?> getStackClass2();

    @DoNotOptimize
    public static Class<?> getStackClass1() {
        return getStackClass2();
    }
}

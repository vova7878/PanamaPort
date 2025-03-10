package com.v7878.unsafe;

import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;

//TODO: mark all methods that used this class as noinline
public class Stack {
    static {
        nothrows_run(() -> registerNativeMethod(
                getHiddenMethod(Stack.class, "getStackClass2"),
                getExecutableData(getHiddenMethod(Class.forName(
                        "dalvik.system.VMStack"), "getStackClass2"))));
    }

    @DoNotObfuscate
    @DoNotShrink
    public static native Class<?> getStackClass2();

    @DoNotOptimize
    public static Class<?> getStackClass1() {
        return getStackClass2();
    }
}

package com.v7878.unsafe;

import static com.v7878.unsafe.ArtMethod.getExecutableData;
import static com.v7878.unsafe.ArtMethod.setExecutableData;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import androidx.annotation.Keep;

public class Stack {
    static {
        nothrows_run(() -> setExecutableData(
                getDeclaredMethod(Stack.class, "getStackClass2"),
                getExecutableData(getDeclaredMethod(Class.forName(
                        "dalvik.system.VMStack"), "getStackClass2"))));
    }

    @Keep
    @SuppressWarnings("JavaJniMissingFunction")
    public static native Class<?> getStackClass2();

    @Keep
    public static Class<?> getStackClass1() {
        return getStackClass2();
    }
}

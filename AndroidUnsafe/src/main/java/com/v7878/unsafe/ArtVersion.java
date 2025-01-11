package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Reflection.getDeclaredFields;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchField;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.unsupportedSDK;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// TODO: maybe we can read manifest-art.json?
public class ArtVersion {
    @ApiSensitive
    public static final int ART_SDK_INT = computeSDKInt();

    private static boolean atLeast36() {
        return false; //FIXME!
    }

    private static boolean atLeast35() {
        Class<?> buf = nothrows_run(() -> Class.forName(
                "java.nio.ByteBufferAsIntBuffer"));
        Field offset = searchField(getDeclaredFields(buf),
                "byteOffset", false);
        return offset != null;
    }

    private static boolean atLeast34() {
        Method sealed = searchMethod(getDeclaredMethods(Class.class),
                "isSealed", false);
        return sealed != null;
    }

    private static boolean atLeast33() {
        Class<?> esf = nothrows_run(() -> Class.forName(
                "dalvik.system.EmulatedStackFrame"));
        Method invoke = searchMethod(getDeclaredMethods(MethodHandle.class),
                "invokeExactWithFrame", false, esf);
        return invoke != null;
    }

    private static int computeSDKInt() {
        int tmp = CORRECT_SDK_INT;

        // Android 12 introduces mainline project
        if (tmp <= 30) return tmp;
        if (tmp > 36) throw unsupportedSDK(tmp);

        // Art module version 32 does not exist
        // (After version 31 comes version 33)
        if (tmp == 32) tmp = 31;

        // At the moment, there is nothing above 36
        if (tmp == 36) return tmp;

        if (atLeast36()) return 36;
        // Not 36, but at least 35 -> 35
        if (tmp == 35) return 35;

        if (atLeast35()) return 35;
        // Not 35, but at least 34 -> 34
        if (tmp == 34) return 34;

        if (atLeast34()) return 34;
        // Not 34, but at least 33 -> 33
        if (tmp == 33) return 33;

        if (atLeast33()) return 33;
        // Not 33, but at least 31 (32 does not exist) -> 31
        return tmp;
    }
}

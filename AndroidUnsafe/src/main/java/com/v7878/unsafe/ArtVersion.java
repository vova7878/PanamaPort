package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.unsupportedSDK;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Duration;

// TODO: maybe we can read manifest-art.json?
public class ArtVersion {
    @ApiSensitive
    public static final int ART_SDK_INT = computeSDKInt();

    private static boolean is36() {
        Method method = searchMethod(Duration.class.getDeclaredMethods(),
                "isPositive", false);
        return method != null;
    }

    private static boolean is35() {
        Method method = searchMethod(ByteBuffer.class.getDeclaredMethods(),
                "get", false, int.class, byte[].class);
        return method != null;
    }

    private static boolean is34() {
        Method method = searchMethod(Class.class.getDeclaredMethods(),
                "isSealed", false);
        return method != null;
    }

    private static boolean is33() {
        Method method = searchMethod(String.class.getDeclaredMethods(),
                "isBlank", false);
        return method != null;
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

        if (is36()) return 36;
        // Not 36, but at least 35 -> 35
        if (tmp == 35) return 35;

        if (is35()) return 35;
        // Not 35, but at least 34 -> 34
        if (tmp == 34) return 34;

        if (is34()) return 34;
        // Not 34, but at least 33 -> 33
        if (tmp == 33) return 33;

        if (is33()) return 33;
        // Not 33, but at least 31 (32 does not exist) -> 31
        return tmp;
    }
}

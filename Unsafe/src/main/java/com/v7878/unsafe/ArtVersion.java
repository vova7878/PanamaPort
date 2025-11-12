package com.v7878.unsafe;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION.SDK_INT_FULL;
import static com.v7878.unsafe.Utils.LOG_TAG;
import static com.v7878.unsafe.Utils.searchMethod;

import android.util.Log;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class ArtVersion {
    @ApiSensitive
    public static final int ART_INDEX = computeIndex();

    public static final int A8p0 = 1;
    public static final int A8p1 = 2;
    public static final int A9 = 3;
    public static final int A10 = 4;
    public static final int A11 = 5;
    public static final int A12 = 6;
    public static final int A13 = 7;
    public static final int A14 = 8;
    public static final int A15 = 9;
    public static final int A16 = 10;
    public static final int A16p1 = 11;

    private static boolean is36p1() {
        try {
            Class.forName("java.lang.invoke.VirtualThread");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean is36() {
        try {
            Class.forName("java.lang.invoke.DirectMethodHandle");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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

    private static int computeIndex() {
        int tmp = SDK_INT;

        if (tmp < 26) {
            throw new UnsupportedOperationException("SDK versions below 26 are not supported");
        }

        // Android 12 introduces mainline project
        if (tmp <= 30) return tmp - 26 + A8p0;
        if (tmp > 36) {
            tmp = 36;
            Log.w(LOG_TAG, String.format("SDK version is too new: %s, maximum supported: %s", SDK_INT, 36));
        }

        // At the moment, there is nothing above 36.1
        if (SDK_INT_FULL > 3600000 || is36p1()) return A16p1;

        if (tmp == 36 || is36()) return A16;
        if (tmp == 35 || is35()) return A15;
        if (tmp == 34 || is34()) return A14;
        if (tmp == 33 || is33()) return A13;

        //noinspection ConstantValue
        assert tmp == 31 || tmp == 32;
        // Art module is the same for api 32 and 31
        return A12;
    }
}

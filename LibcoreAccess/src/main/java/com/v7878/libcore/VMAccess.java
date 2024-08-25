package com.v7878.libcore;

import static com.v7878.unsafe.DexFileUtils.setTrusted;

import dalvik.system.VMRuntime;

public class VMAccess {
    static {
        setTrusted(VMAccess.class);
    }

    private static VMRuntime getInstance() {
        return VMRuntime.getRuntime();
    }

    public static String[] properties() {
        return getInstance().properties();
    }

    public static String bootClassPath() {
        return getInstance().bootClassPath();
    }

    public static String classPath() {
        return getInstance().classPath();
    }

    public static String vmLibrary() {
        return getInstance().vmLibrary();
    }

    public static String getCurrentInstructionSet() {
        return VMRuntime.getCurrentInstructionSet();
    }

    public static boolean isCheckJniEnabled() {
        return getInstance().isCheckJniEnabled();
    }

    public static boolean isNativeDebuggable() {
        return getInstance().isNativeDebuggable();
    }

    public static boolean isJavaDebuggable() {
        return getInstance().isJavaDebuggable();
    }

    public static Object newNonMovableArray(Class<?> componentType, int length) {
        return getInstance().newNonMovableArray(componentType, length);
    }

    public static long addressOf(Object array) {
        return getInstance().addressOf(array);
    }
}

package com.v7878.unsafe.access;

import static com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.STATIC;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.access.AccessLinker.Conditions;

public class VMAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @ExecutableAccess(kind = STATIC, klass = "dalvik.system.VMRuntime", name = "getRuntime", args = {})
        abstract Object getRuntime();

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "properties", args = {})
        abstract String[] properties(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "bootClassPath", args = {})
        abstract String bootClassPath(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "classPath", args = {})
        abstract String classPath(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "vmLibrary", args = {})
        abstract String vmLibrary(Object instance);

        @ExecutableAccess(kind = STATIC, klass = "dalvik.system.VMRuntime", name = "getCurrentInstructionSet", args = {})
        abstract String getCurrentInstructionSet();

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "isCheckJniEnabled", args = {})
        abstract boolean isCheckJniEnabled(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "isNativeDebuggable", args = {})
        abstract boolean isNativeDebuggable(Object instance);

        @ApiSensitive
        @ExecutableAccess(conditions = @Conditions(min_art = 28),
                kind = VIRTUAL, klass = "dalvik.system.VMRuntime", name = "isJavaDebuggable", args = {})
        abstract boolean isJavaDebuggable(Object instance);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime",
                name = "newNonMovableArray", args = {"java.lang.Class", "int"})
        abstract Object newNonMovableArray(Object instance, Class<?> componentType, int length);

        @ExecutableAccess(kind = VIRTUAL, klass = "dalvik.system.VMRuntime",
                name = "addressOf", args = {"java.lang.Object"})
        abstract long addressOf(Object instance, Object array);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    private static Object getInstance() {
        class Holder {
            static final Object vm = AccessI.INSTANCE.getRuntime();
        }
        return Holder.vm;
    }

    public static String[] properties() {
        return AccessI.INSTANCE.properties(getInstance());
    }

    public static String bootClassPath() {
        return AccessI.INSTANCE.bootClassPath(getInstance());
    }

    public static String classPath() {
        return AccessI.INSTANCE.classPath(getInstance());
    }

    public static String vmLibrary() {
        return AccessI.INSTANCE.vmLibrary(getInstance());
    }

    public static String getCurrentInstructionSet() {
        return AccessI.INSTANCE.getCurrentInstructionSet();
    }

    public static boolean isCheckJniEnabled() {
        return AccessI.INSTANCE.isCheckJniEnabled(getInstance());
    }

    public static boolean isNativeDebuggable() {
        return AccessI.INSTANCE.isNativeDebuggable(getInstance());
    }

    public static boolean isJavaDebuggable() {
        return AccessI.INSTANCE.isJavaDebuggable(getInstance());
    }

    public static Object newNonMovableArray(Class<?> componentType, int length) {
        return AccessI.INSTANCE.newNonMovableArray(getInstance(), componentType, length);
    }

    public static long addressOf(Object array) {
        return AccessI.INSTANCE.addressOf(getInstance(), array);
    }
}

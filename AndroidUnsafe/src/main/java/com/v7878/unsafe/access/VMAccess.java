package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.getHiddenMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;

import com.v7878.unsafe.AndroidUnsafe;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class VMAccess {
    private static final Class<?> VM_CLASS = nothrows_run(() ->
            Class.forName("dalvik.system.VMRuntime"));
    private static final Method[] methods = getHiddenMethods(VM_CLASS);

    private static Object getInstance() {
        class Holder {
            static final Object vm;

            static {
                vm = AndroidUnsafe.allocateInstance(VM_CLASS);
            }
        }
        return Holder.vm;
    }

    public static String[] properties() {
        class Holder {
            static final String[] properties;

            static {
                MethodHandle mh_properties = unreflect(
                        searchMethod(methods, "properties"));
                properties = nothrows_run(() ->
                        (String[]) mh_properties.invoke(getInstance()));
            }
        }
        return Holder.properties.clone();
    }

    public static String bootClassPath() {
        class Holder {
            static final String boot_class_path;

            static {
                MethodHandle bootClassPath = unreflect(
                        searchMethod(methods, "bootClassPath"));
                boot_class_path = nothrows_run(() ->
                        (String) bootClassPath.invoke(getInstance()));
            }
        }
        return Holder.boot_class_path;
    }

    public static String classPath() {
        class Holder {
            static final String class_path;

            static {
                MethodHandle classPath = unreflect(
                        searchMethod(methods, "classPath"));
                class_path = nothrows_run(() ->
                        (String) classPath.invoke(getInstance()));
            }
        }
        return Holder.class_path;
    }

    public static String vmLibrary() {
        class Holder {
            static final String lib;

            static {
                MethodHandle vmLibrary = unreflect(
                        searchMethod(methods, "vmLibrary"));
                lib = nothrows_run(() -> (String) vmLibrary.invoke(getInstance()));
            }
        }
        return Holder.lib;
    }

    public static String getCurrentInstructionSet() {
        class Holder {
            static final String instruction_set;

            static {
                MethodHandle getCurrentInstructionSet = unreflect(
                        searchMethod(methods, "getCurrentInstructionSet"));
                instruction_set = nothrows_run(() -> (String) getCurrentInstructionSet.invoke());
            }
        }
        return Holder.instruction_set;
    }

    public static boolean isCheckJniEnabled() {
        class Holder {
            static final boolean check_jni;

            static {
                MethodHandle isNativeDebuggable = unreflect(
                        searchMethod(methods, "isCheckJniEnabled"));
                check_jni = nothrows_run(() ->
                        (boolean) isNativeDebuggable.invoke(getInstance()));
            }
        }
        return Holder.check_jni;
    }

    public static boolean isNativeDebuggable() {
        class Holder {
            static final boolean debuggable;

            static {
                MethodHandle isNativeDebuggable = unreflect(
                        searchMethod(methods, "isNativeDebuggable"));
                debuggable = nothrows_run(() ->
                        (boolean) isNativeDebuggable.invoke(getInstance()));
            }
        }
        return Holder.debuggable;
    }

    public static boolean isJavaDebuggable() {
        class Holder {
            static final boolean debuggable;

            static {
                MethodHandle isJavaDebuggable = unreflect(
                        searchMethod(methods, "isJavaDebuggable"));
                debuggable = nothrows_run(() ->
                        (boolean) isJavaDebuggable.invoke(getInstance()));
            }
        }
        return Holder.debuggable;
    }

    public static Object newNonMovableArray(Class<?> componentType, int length) {
        class Holder {
            static final MethodHandle new_array;

            static {
                new_array = unreflectDirect(searchMethod(methods,
                        "newNonMovableArray", Class.class, int.class));
            }
        }
        return nothrows_run(() -> Holder.new_array
                .invoke(getInstance(), componentType, length));
    }

    public static long addressOf(Object array) {
        class Holder {
            static final MethodHandle address_of;

            static {
                address_of = unreflectDirect(searchMethod(
                        methods, "addressOf", Object.class));
            }
        }
        return nothrows_run(() -> (long) Holder.address_of.invoke(getInstance(), array));
    }
}

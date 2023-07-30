package com.v7878.unsafe;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Utils {

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }
        if (a2 == null) {
            return a1.length == 0;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static Field searchField(Field[] fields, String name, boolean thw) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return thw ? AndroidUnsafe.throwException(new NoSuchFieldException(name)) : null;
    }

    public static Field searchField(Field[] fields, String name) {
        return searchField(fields, name, true);
    }

    private static String methodToString(String name, Class<?>[] argTypes) {
        return name + ((argTypes == null || argTypes.length == 0)
                ? "()" : Arrays.stream(argTypes)
                .map(c -> c == null ? "null" : c.getName())
                .collect(Collectors.joining(",", "(", ")")));
    }

    public static Method searchMethod(Method[] methods, String name,
                                      boolean thw, Class<?>... parameterTypes) {
        for (Method m : methods) {
            if (m.getName().equals(name) && arrayContentsEq(
                    parameterTypes, m.getParameterTypes())) {
                return m;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString(name, parameterTypes))) : null;
    }

    public static Method searchMethod(Method[] methods, String name, Class<?>... parameterTypes) {
        return searchMethod(methods, name, true, parameterTypes);
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, boolean thw, Class<?>... parameterTypes) {
        for (Constructor<T> c : constructors) {
            if (arrayContentsEq(parameterTypes, c.getParameterTypes())) {
                return c;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString("<init>", parameterTypes))) : null;
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, Class<?>... parameterTypes) {
        return searchConstructor(constructors, true, parameterTypes);
    }

    public static <T extends Throwable> void assert_(boolean value, Supplier<T> th) {
        if (!value) {
            AndroidUnsafe.throwException(th.get());
        }
    }

    @FunctionalInterface
    public interface TRun<T> {

        T run() throws Throwable;
    }

    @FunctionalInterface
    public interface VTRun {

        void run() throws Throwable;
    }

    public static <T> T nothrows_run(TRun<T> r) {
        try {
            return r.run();
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static void nothrows_run(VTRun r) {
        try {
            r.run();
        } catch (Throwable th) {
            AndroidUnsafe.throwException(th);
        }
    }

    public static <T> Supplier<T> runOnce(Supplier<T> task) {
        return new Supplier<T>() {
            volatile T value;

            @Override
            public T get() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.get();
                        }
                    }
                }
                return value;
            }
        };
    }
}

package com.v7878.unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Reflection {
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... params) {
        throw new UnsupportedOperationException("Stub!");
    }
}

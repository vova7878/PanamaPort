package com.v7878.foreign;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class _MhUtil {
    private _MhUtil() {
    }

    public static MethodHandle findVirtual(MethodHandles.Lookup lookup,
                                           Class<?> refc,
                                           String name,
                                           MethodType type) {
        try {
            return lookup.findVirtual(refc, name, type);
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    public static MethodHandle findStatic(MethodHandles.Lookup lookup,
                                          Class<?> refc,
                                          String name,
                                          MethodType type) {
        try {
            return lookup.findStatic(refc, name, type);
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }
}


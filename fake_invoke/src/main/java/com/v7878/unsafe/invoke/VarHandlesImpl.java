package com.v7878.unsafe.invoke;

import com.v7878.invoke.VarHandle;

import java.lang.invoke.MethodHandle;
import java.util.List;

public final class VarHandlesImpl {
    private VarHandlesImpl() {
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
        throw new UnsupportedOperationException("Stub!");
    }
}

package com.v7878.invoke;


import com.v7878.unsafe.invoke.VarHandlesImpl;

import java.lang.invoke.MethodHandle;
import java.util.List;

public final class VarHandles {
    private VarHandles() {
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        return VarHandlesImpl.filterValue(target, filterToTarget, filterFromTarget);
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        return VarHandlesImpl.filterCoordinates(target, pos, filters);
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        return VarHandlesImpl.collectCoordinates(target, pos, filter);
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        return VarHandlesImpl.insertCoordinates(target, pos, values);
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        return VarHandlesImpl.permuteCoordinates(target, newCoordinates, reorder);
    }
}

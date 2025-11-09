package com.v7878.invoke;


import java.lang.invoke.MethodHandle;
import java.util.List;

// Class for backward compatibility
public final class VarHandles {
    private VarHandles() {
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        return Handles.filterValue(target, filterToTarget, filterFromTarget);
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        return Handles.filterCoordinates(target, pos, filters);
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        return Handles.insertCoordinates(target, pos, values);
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        return Handles.permuteCoordinates(target, newCoordinates, reorder);
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        return Handles.collectCoordinates(target, pos, filter);
    }

    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
        return Handles.dropCoordinates(target, pos, valueTypes);
    }
}

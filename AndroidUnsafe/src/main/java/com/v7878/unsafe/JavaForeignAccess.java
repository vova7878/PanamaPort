package com.v7878.unsafe;

import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.unsafe.Utils.FineClosable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment.Scope;
import java.lang.reflect.Method;

public abstract class JavaForeignAccess {
    private static final JavaForeignAccess INSTANCE = (JavaForeignAccess) nothrows_run(() -> {
        Method init = getDeclaredMethod(MemorySegment.class, "initAccess");
        return init.invoke(null);
    });

    protected abstract FineClosable _lock(Scope scope);

    public static FineClosable lock(Scope scope) {
        return INSTANCE._lock(scope);
    }
}

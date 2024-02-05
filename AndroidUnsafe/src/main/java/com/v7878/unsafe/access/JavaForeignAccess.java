package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.unsafe.Utils.FineClosable;

import java.lang.reflect.Method;

//TODO: filechanel mmap segment
public abstract class JavaForeignAccess {
    private static final JavaForeignAccess INSTANCE = (JavaForeignAccess) nothrows_run(() -> {
        Method init = getDeclaredMethod(MemorySegment.class, "initAccess");
        return init.invoke(null);
    });

    protected abstract FineClosable _lock(Scope scope);

    public static FineClosable lock(Scope scope) {
        return INSTANCE._lock(scope);
    }

    protected abstract void _addCloseAction(Scope scope, Runnable cleanup);

    public static void addCloseAction(Scope scope, Runnable cleanup) {
        INSTANCE._addCloseAction(scope, cleanup);
    }

    protected abstract void _addOrCleanupIfFail(Scope scope, Runnable cleanup);

    public static void addOrCleanupIfFail(Scope scope, Runnable cleanup) {
        INSTANCE._addOrCleanupIfFail(scope, cleanup);
    }

    protected abstract MemorySegment _objectSegment(Object obj);

    public static MemorySegment objectSegment(Object obj) {
        return INSTANCE._objectSegment(obj);
    }
}

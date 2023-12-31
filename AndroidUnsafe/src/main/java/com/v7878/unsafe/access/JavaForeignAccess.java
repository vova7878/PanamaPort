package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.VarHandle;
import com.v7878.unsafe.Utils.FineClosable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment.Scope;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Method;

//TODO: filechanel mmap segment
public abstract class JavaForeignAccess {
    private static final JavaForeignAccess INSTANCE = (JavaForeignAccess) nothrows_run(() -> {
        Method init = getDeclaredMethod(MemorySegment.class, "initAccess");
        return init.invoke(null);
    });

    protected abstract VarHandle _accessHandle(ValueLayout layout);

    public static VarHandle accessHandle(ValueLayout layout) {
        return INSTANCE._accessHandle(layout);
    }

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
}

package com.v7878.invoke;

import android.annotation.TargetApi;
import android.os.Build;

import com.v7878.unsafe.invoke.MethodHandlesImpl;
import com.v7878.unsafe.invoke.VarHandlesImpl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;

public final class Handles {
    private Handles() {
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        return VarHandlesImpl.filterValue(target, filterToTarget, filterFromTarget);
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        return VarHandlesImpl.filterCoordinates(target, pos, filters);
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        return VarHandlesImpl.insertCoordinates(target, pos, values);
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        return VarHandlesImpl.permuteCoordinates(target, newCoordinates, reorder);
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        return VarHandlesImpl.collectCoordinates(target, pos, filter);
    }

    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
        return VarHandlesImpl.dropCoordinates(target, pos, valueTypes);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        return MethodHandlesImpl.arrayConstructor(arrayClass);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayLength(Class<?> arrayClass) {
        return MethodHandlesImpl.arrayLength(arrayClass);
    }

    public static MethodHandle arrayElementGetter(Class<?> arrayClass) {
        return MethodHandlesImpl.arrayElementGetter(arrayClass);
    }

    public static MethodHandle arrayElementSetter(Class<?> arrayClass) {
        return MethodHandlesImpl.arrayElementSetter(arrayClass);
    }

    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        return MethodHandlesImpl.spreadInvoker(type, leadingArgCount);
    }

    public static MethodHandle exactInvoker(MethodType type) {
        return MethodHandlesImpl.exactInvoker(type);
    }

    public static MethodHandle invoker(MethodType type) {
        return MethodHandlesImpl.invoker(type);
    }

    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        return MethodHandlesImpl.explicitCastArguments(target, newType);
    }

    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        return MethodHandlesImpl.permuteArguments(target, newType, reorder);
    }

    public static MethodHandle constant(Class<?> type, Object value) {
        return MethodHandlesImpl.constant(type, value);
    }

    public static MethodHandle identity(Class<?> type) {
        return MethodHandlesImpl.identity(type);
    }

    public static MethodHandle zero(Class<?> type) {
        return MethodHandlesImpl.zero(type);
    }

    public static MethodHandle empty(MethodType type) {
        return MethodHandlesImpl.empty(type);
    }

    public static MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        return MethodHandlesImpl.insertArguments(target, pos, values);
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        return MethodHandlesImpl.dropArguments(target, pos, valueTypes);
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        return MethodHandlesImpl.dropArguments(target, pos, valueTypes);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos) {
        return MethodHandlesImpl.dropArgumentsToMatch(target, pos, newTypes, pos);
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle dropReturn(MethodHandle target) {
        return MethodHandlesImpl.dropReturn(target);
    }

    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        return MethodHandlesImpl.filterArguments(target, pos, filters);
    }

    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        return MethodHandlesImpl.collectArguments(target, pos, filter);
    }

    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        return MethodHandlesImpl.filterReturnValue(target, filter);
    }

    public static MethodHandle collectReturnValue(MethodHandle target, MethodHandle filter) {
        return MethodHandlesImpl.collectReturnValue(target, filter);
    }

    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        return MethodHandlesImpl.foldArguments(target, combiner);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        return MethodHandlesImpl.foldArguments(target, pos, combiner);
    }

    public static MethodHandle guardWithTest(MethodHandle test, MethodHandle target, MethodHandle fallback) {
        return MethodHandlesImpl.guardWithTest(test, target, fallback);
    }

    public static MethodHandle catchException(MethodHandle target, Class<? extends Throwable> exType, MethodHandle handler) {
        return MethodHandlesImpl.catchException(target, exType, handler);
    }

    public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        return MethodHandlesImpl.throwException(returnType, exType);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle loop(MethodHandle[]... clauses) {
        return MethodHandlesImpl.loop(clauses);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        return MethodHandlesImpl.whileLoop(init, pred, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        return MethodHandlesImpl.doWhileLoop(init, body, pred);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
        return MethodHandlesImpl.countedLoop(iterations, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        return MethodHandlesImpl.countedLoop(start, end, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        return MethodHandlesImpl.iteratedLoop(iterator, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        return MethodHandlesImpl.tryFinally(target, cleanup);
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle tableSwitch(MethodHandle fallback, MethodHandle... targets) {
        return MethodHandlesImpl.tableSwitch(fallback, targets);
    }

    public static MethodHandle asType(MethodHandle handle, MethodType newType) {
        return MethodHandlesImpl.asType(handle, newType);
    }

    public static MethodHandle asSpreader(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        return MethodHandlesImpl.asSpreader(handle, arrayType, arrayLength);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle asSpreader(MethodHandle handle, int spreadArgPos, Class<?> arrayType, int arrayLength) {
        return MethodHandlesImpl.asSpreader(handle, spreadArgPos, arrayType, arrayLength);
    }

    public static MethodHandle asCollector(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        return MethodHandlesImpl.asCollector(handle, arrayType, arrayLength);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle asCollector(MethodHandle handle, int collectArgPos, Class<?> arrayType, int arrayLength) {
        return MethodHandlesImpl.asCollector(handle, collectArgPos, arrayType, arrayLength);
    }

    public static MethodHandle asVarargsCollector(MethodHandle handle, Class<?> arrayType) {
        return MethodHandlesImpl.asVarargsCollector(handle, arrayType);
    }

    public static MethodHandle bindTo(MethodHandle handle, Object x) {
        return MethodHandlesImpl.bindTo(handle, x);
    }

    public static Object invokeWithArguments(MethodHandle handle, Object... arguments) throws Throwable {
        return MethodHandlesImpl.invokeWithArguments(handle, arguments);
    }

    public static Object invokeWithArguments(MethodHandle handle, List<?> arguments) throws Throwable {
        return MethodHandlesImpl.invokeWithArguments(handle, arguments);
    }
}

package com.v7878.unsafe.invoke;

import android.annotation.TargetApi;
import android.os.Build;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class MethodHandlesImpl {
    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayLength(Class<?> arrayClass) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle arrayElementGetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle arrayElementSetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        return MethodHandles.spreadInvoker(type, leadingArgCount);
    }

    public static MethodHandle exactInvoker(MethodType type) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle invoker(MethodType type) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle constant(Class<?> type, Object value) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle identity(Class<?> type) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle zero(Class<?> type) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle empty(MethodType type) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle dropReturn(MethodHandle target) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle collectReturnValue(MethodHandle target, MethodHandle filter) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle guardWithTest(MethodHandle test, MethodHandle target, MethodHandle fallback) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle catchException(MethodHandle target, Class<? extends Throwable> exType, MethodHandle handler) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle loop(MethodHandle[]... clauses) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle tableSwitch(MethodHandle fallback, MethodHandle... targets) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle asType(MethodHandle handle, MethodType newType) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle asSpreader(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle asSpreader(MethodHandle handle, int spreadArgPos, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle asCollector(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("Stub!");
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle asCollector(MethodHandle handle, int collectArgPos, Class<?> arrayType, int arrayLength) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle asVarargsCollector(MethodHandle handle, Class<?> arrayType) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MethodHandle bindTo(MethodHandle handle, Object x) {
        throw new UnsupportedOperationException("Stub!");
    }

    @SuppressWarnings("RedundantThrows")
    public static Object invokeWithArguments(MethodHandle handle, Object... arguments) throws Throwable {
        throw new UnsupportedOperationException("Stub!");
    }

    @SuppressWarnings("RedundantThrows")
    public static Object invokeWithArguments(MethodHandle handle, List<?> arguments) throws Throwable {
        throw new UnsupportedOperationException("Stub!");
    }
}

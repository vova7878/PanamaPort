package com.v7878.unsafe.invoke;

import android.annotation.TargetApi;
import android.os.Build;

import com.v7878.unsafe.DangerLevel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class MethodHandlesImpl {
    // Note: not present in Handles
    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static MethodHandle reinterptetHandle(MethodHandle handle, MethodType type) {
        return MHUtils.reinterptetHandle(handle, type);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        return MethodHandles.arrayConstructor(arrayClass);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle arrayLength(Class<?> arrayClass) {
        return MethodHandles.arrayLength(arrayClass);
    }

    public static MethodHandle arrayElementGetter(Class<?> arrayClass) {
        return MethodHandles.arrayElementGetter(arrayClass);
    }

    public static MethodHandle arrayElementSetter(Class<?> arrayClass) {
        return MethodHandles.arrayElementSetter(arrayClass);
    }

    // TODO?
    // public static VarHandle arrayElementVarHandle(Class<?> arrayClass) {}
    // public static VarHandle byteArrayViewVarHandle(Class<?> viewArrayClass, ByteOrder byteOrder) {}
    // public static VarHandle byteBufferViewVarHandle(Class<?> viewArrayClass, ByteOrder byteOrder) {}

    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        return MethodHandles.spreadInvoker(type, leadingArgCount);
    }

    public static MethodHandle exactInvoker(MethodType type) {
        return MHUtils.exactInvoker(type);
    }

    public static MethodHandle invoker(MethodType type) {
        return MHUtils.invoker(type);
    }

    // TODO?
    // public static MethodHandle varHandleExactInvoker(VarHandle.AccessMode accessMode, MethodType type) {}
    // public static MethodHandle varHandleInvoker(VarHandle.AccessMode accessMode, MethodType type) {}

    // Note: not present in Handles
    public static MethodHandle explicitCastArgumentsAdapter(MethodHandle target, MethodType newType) {
        return MHUtils.explicitCastArgumentsAdapter(target, newType);
    }

    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        return MHUtils.explicitCastArguments(target, newType);
    }

    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        return MHUtils.permuteArguments(target, newType, reorder);
    }

    // Note: not present in Handles
    public static MethodHandle reorderArguments(MethodHandle target, int... reorder) {
        return MHUtils.reorderArguments(target, reorder);
    }

    public static MethodHandle constant(Class<?> type, Object value) {
        return MethodHandles.constant(type, value);
    }

    public static MethodHandle identity(Class<?> type) {
        return MHUtils.identity(type);
    }

    public static MethodHandle zero(Class<?> type) {
        return MHUtils.zero(type);
    }

    public static MethodHandle empty(MethodType type) {
        return MHUtils.empty(type);
    }

    public static MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        return MethodHandles.insertArguments(target, pos, values);
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        return MethodHandles.dropArguments(target, pos, valueTypes);
    }

    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        return MethodHandles.dropArguments(target, pos, valueTypes);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos) {
        return MethodHandles.dropArgumentsToMatch(target, pos, newTypes, pos);
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle dropReturn(MethodHandle target) {
        return MethodHandles.dropReturn(target);
    }

    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        return MethodHandles.filterArguments(target, pos, filters);
    }

    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        return MHUtils.collectArguments(target, pos, filter);
    }

    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        return MethodHandles.filterReturnValue(target, filter);
    }

    public static MethodHandle collectReturnValue(MethodHandle target, MethodHandle filter) {
        return MHUtils.collectReturnValue(target, filter);
    }

    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        return MethodHandles.foldArguments(target, combiner);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        return MethodHandles.foldArguments(target, pos, combiner);
    }

    public static MethodHandle guardWithTest(MethodHandle test, MethodHandle target, MethodHandle fallback) {
        return MethodHandles.guardWithTest(test, target, fallback);
    }

    public static MethodHandle catchException(MethodHandle target, Class<? extends Throwable> exType, MethodHandle handler) {
        return MethodHandles.catchException(target, exType, handler);
    }

    public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        return MethodHandles.throwException(returnType, exType);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle loop(MethodHandle[]... clauses) {
        return MethodHandles.loop(clauses);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        return MethodHandles.whileLoop(init, pred, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        return MethodHandles.doWhileLoop(init, body, pred);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
        return MethodHandles.countedLoop(iterations, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        return MethodHandles.countedLoop(start, end, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        return MethodHandles.iteratedLoop(iterator, init, body);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        return MethodHandles.tryFinally(target, cleanup);
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static MethodHandle tableSwitch(MethodHandle fallback, MethodHandle... targets) {
        return MethodHandles.tableSwitch(fallback, targets);
    }

    // Note: not present in Handles
    public static MethodHandle asTypeAdapter(MethodHandle target, MethodType newType) {
        return MHUtils.asTypeAdapter(target, newType);
    }

    public static MethodHandle asType(MethodHandle handle, MethodType newType) {
        return MHUtils.asType(handle, newType);
    }

    public static MethodHandle asSpreader(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        return handle.asSpreader(arrayType, arrayLength);
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static MethodHandle asSpreader(MethodHandle handle, int spreadArgPos, Class<?> arrayType, int arrayLength) {
        return handle.asSpreader(spreadArgPos, arrayType, arrayLength);
    }

    public static MethodHandle asCollector(MethodHandle handle, Class<?> arrayType, int arrayLength) {
        return asCollector(handle, handle.type().parameterCount() - 1, arrayType, arrayLength);
    }

    public static MethodHandle asCollector(MethodHandle handle, int collectArgPos, Class<?> arrayType, int arrayLength) {
        return MHUtils.asCollector(handle, collectArgPos, arrayType, arrayLength);
    }

    public static MethodHandle asVarargsCollector(MethodHandle handle, Class<?> arrayType) {
        return handle.asVarargsCollector(arrayType);
    }

    public static MethodHandle bindTo(MethodHandle handle, Object x) {
        return handle.bindTo(x);
    }

    public static Object invokeWithArguments(MethodHandle handle, Object... arguments) throws Throwable {
        return handle.invokeWithArguments(arguments);
    }

    public static Object invokeWithArguments(MethodHandle handle, List<?> arguments) throws Throwable {
        return handle.invokeWithArguments(arguments);
    }
}

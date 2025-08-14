package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.newIllegalArgumentException;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.invoke.VarHandleImpl.accessType;

import com.v7878.invoke.Handles;
import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public final class VarHandlesImpl {
    private VarHandlesImpl() {
    }

    private static MethodHandle checkExceptions(MethodHandle target) {
        Objects.requireNonNull(target);
        Class<?>[] exceptionTypes = InvokeAccess.exceptionTypes(target);
        if (exceptionTypes != null) { // exceptions known
            if (Stream.of(exceptionTypes).anyMatch(VarHandlesImpl::isCheckedException)) {
                throw newIllegalArgumentException("Cannot adapt a var handle with a method handle which throws checked exceptions");
            }
        }
        return target;
    }

    private static boolean isCheckedException(Class<?> clazz) {
        return Throwable.class.isAssignableFrom(clazz) &&
                !RuntimeException.class.isAssignableFrom(clazz) &&
                !Error.class.isAssignableFrom(clazz);
    }

    static void handleCheckedExceptions(Throwable th) {
        if (isCheckedException(th.getClass())) {
            throw new IllegalStateException("Adapter handle threw checked exception", th);
        }
        AndroidUnsafe.throwException(th);
    }

    @AlwaysInline
    private static Class<?> lastParameterType(MethodType type) {
        int len = type.parameterCount();
        return len == 0 ? void.class : type.parameterType(len - 1);
    }

    @AlwaysInline
    @SuppressWarnings("SameParameterValue")
    private static int assert_same(int a, int b, String message) {
        if (a == b) {
            return a;
        }
        throw newIllegalArgumentException(message, a, b);
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        Objects.requireNonNull(target);
        checkExceptions(filterToTarget);
        checkExceptions(filterFromTarget);

        List<Class<?>> newCoordinates = new ArrayList<>(target.coordinateTypes());

        MethodType to_type = filterToTarget.type();
        MethodType from_type = filterFromTarget.type();

        int args_count = assert_same(to_type.parameterCount(),
                from_type.parameterCount(), "Filter types have different arity");
        if (args_count < 1) {
            throw newIllegalArgumentException("Filter types has wrong arity", args_count);
        }
        int additional_args = args_count - 1;

        if (lastParameterType(from_type) != to_type.returnType() ||
                lastParameterType(to_type) != from_type.returnType()) {
            throw newIllegalArgumentException("filterFromTarget and filterToTarget filter types do not match", from_type, to_type);
        } else if (target.varType() != lastParameterType(from_type)) {
            throw newIllegalArgumentException("filterFromTarget filter type does not match target var handle type", from_type, target.varType());
        } else if (target.varType() != filterToTarget.type().returnType()) {
            throw newIllegalArgumentException("filterToTarget filter type does not match target var handle type", to_type, target.varType());
        } else for (int i = 0; i < additional_args; i++) {
            var from = from_type.parameterType(i);
            if (from != to_type.parameterType(i)) {
                throw newIllegalArgumentException("filterFromTarget and filterToTarget filter types do not match", from_type, to_type);
            } else {
                newCoordinates.add(from);
            }
        }

        return newIndirectVarHandle(target, filterFromTarget.type().returnType(),
                newCoordinates.toArray(new Class<?>[0]), (mode, modeHandle) -> {
                    int lastParameterPos = modeHandle.type().parameterCount() - 1;
                    switch (accessType(mode)) {
                        case GET, GET_ATOMIC -> {
                            return Handles.collectReturnValue(modeHandle, filterFromTarget);
                        }
                        case SET, SET_ATOMIC -> {
                            return Handles.collectArguments(modeHandle, lastParameterPos, filterToTarget);
                        }
                        case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE, GET_AND_UPDATE_NUMERIC -> {
                            MethodHandle adapter = Handles.collectReturnValue(modeHandle, filterFromTarget);
                            MethodHandle res = Handles.collectArguments(adapter, lastParameterPos, filterToTarget);
                            if (additional_args != 0) {
                                res = joinDuplicateArgs(res, lastParameterPos,
                                        lastParameterPos + additional_args + 1, additional_args);
                            }
                            return res;
                        }
                        case COMPARE_AND_EXCHANGE -> {
                            MethodHandle adapter = Handles.collectReturnValue(modeHandle, filterFromTarget);
                            adapter = Handles.collectArguments(adapter, lastParameterPos, filterToTarget);
                            if (additional_args != 0) {
                                adapter = joinDuplicateArgs(adapter, lastParameterPos,
                                        lastParameterPos + additional_args + 1, additional_args);
                            }
                            MethodHandle res = Handles.collectArguments(adapter, lastParameterPos - 1, filterToTarget);
                            if (additional_args != 0) {
                                res = joinDuplicateArgs(res, lastParameterPos - 1,
                                        lastParameterPos + additional_args, additional_args);
                            }
                            return res;
                        }
                        case COMPARE_AND_SET -> {
                            MethodHandle adapter = Handles.collectArguments(modeHandle, lastParameterPos, filterToTarget);
                            MethodHandle res = Handles.collectArguments(adapter, lastParameterPos - 1, filterToTarget);
                            if (additional_args != 0) {
                                res = joinDuplicateArgs(res, lastParameterPos - 1,
                                        lastParameterPos + additional_args, additional_args);
                            }
                            return res;
                        }
                    }
                    throw shouldNotReachHere();
                });
    }

    private static MethodHandle joinDuplicateArgs(MethodHandle handle, int originalStart, int dropStart, int length) {
        var type = handle.type();
        int[] perms = new int[type.parameterCount()];
        for (int i = 0; i < dropStart; i++) {
            perms[i] = i;
        }
        for (int i = 0; i < length; i++) {
            perms[dropStart + i] = originalStart + i;
        }
        for (int i = dropStart + length; i < perms.length; i++) {
            perms[i] = i - length;
        }
        return Handles.permuteArguments(handle,
                type.dropParameterTypes(dropStart, dropStart + length), perms);
    }

    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(filters);

        List<Class<?>> targetCoordinates = target.coordinateTypes();
        if (pos < 0 || pos >= targetCoordinates.size()) {
            throw newIllegalArgumentException("Invalid position " + pos + " for coordinate types", targetCoordinates);
        } else if (pos + filters.length > targetCoordinates.size()) {
            throw new IllegalArgumentException("Too many filters");
        }

        if (filters.length == 0) return target;

        List<Class<?>> newCoordinates = new ArrayList<>(targetCoordinates);
        for (int i = 0; i < filters.length; i++) {
            MethodHandle filter = checkExceptions(filters[i]);
            MethodType filterType = filter.type();
            if (filterType.parameterCount() != 1) {
                throw newIllegalArgumentException("Invalid filter type " + filterType);
            } else if (newCoordinates.get(pos + i) != filterType.returnType()) {
                throw newIllegalArgumentException("Invalid filter type " + filterType + " for coordinate type " + newCoordinates.get(i));
            }
            newCoordinates.set(pos + i, filters[i].type().parameterType(0));
        }

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> Handles.filterArguments(modeHandle, pos, filters));
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        Objects.requireNonNull(target);
        checkExceptions(filter);

        List<Class<?>> targetCoordinates = target.coordinateTypes();
        if (pos < 0 || pos >= targetCoordinates.size()) {
            throw newIllegalArgumentException("Invalid position " + pos + " for coordinate types", targetCoordinates);
        } else if (filter.type().returnType() != void.class && filter.type().returnType() != targetCoordinates.get(pos)) {
            throw newIllegalArgumentException("Invalid filter type " + filter.type() + " for coordinate type " + targetCoordinates.get(pos));
        }

        List<Class<?>> newCoordinates = new ArrayList<>(targetCoordinates);
        if (filter.type().returnType() != void.class) {
            newCoordinates.remove(pos);
        }
        newCoordinates.addAll(pos, filter.type().parameterList());

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> Handles.collectArguments(modeHandle, pos, filter));
    }

    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(values);

        List<Class<?>> targetCoordinates = target.coordinateTypes();
        if (pos < 0 || pos >= targetCoordinates.size()) {
            throw newIllegalArgumentException("Invalid position " + pos + " for coordinate types", targetCoordinates);
        } else if (pos + values.length > targetCoordinates.size()) {
            throw new IllegalArgumentException("Too many values");
        }

        if (values.length == 0) return target;

        List<Class<?>> newCoordinates = new ArrayList<>(targetCoordinates);
        for (Object value : values) {
            Class<?> pt = newCoordinates.get(pos);
            if (pt.isPrimitive()) {
                Wrapper w = Wrapper.forPrimitiveType(pt);
                w.convert(value, pt);
            } else {
                pt.cast(value);
            }
            newCoordinates.remove(pos);
        }

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> Handles.insertArguments(modeHandle, pos, values));
    }

    private static int numTrailingArgs(VarHandleImpl.AccessType at) {
        return switch (at) {
            case GET, GET_ATOMIC -> 0;
            case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE,
                 GET_AND_UPDATE_NUMERIC, SET, SET_ATOMIC -> 1;
            case COMPARE_AND_SET, COMPARE_AND_EXCHANGE -> 2;
        };
    }

    private static int[] reorderArrayFor(VarHandleImpl.AccessType at, List<Class<?>> newCoordinates, int[] reorder) {
        int numTrailingArgs = numTrailingArgs(at);
        int[] adjustedReorder = new int[reorder.length + numTrailingArgs];
        System.arraycopy(reorder, 0, adjustedReorder, 0, reorder.length);
        for (int i = 0; i < numTrailingArgs; i++) {
            adjustedReorder[reorder.length + i] = newCoordinates.size() + i;
        }
        return adjustedReorder;
    }

    private static MethodType methodTypeFor(VarHandleImpl.AccessType at, MethodType oldType,
                                            List<Class<?>> oldCoordinates, List<Class<?>> newCoordinates) {
        int numTrailingArgs = numTrailingArgs(at);
        MethodType adjustedType = MethodType.methodType(oldType.returnType(), newCoordinates);
        for (int i = 0; i < numTrailingArgs; i++) {
            adjustedType = adjustedType.appendParameterTypes(
                    oldType.parameterType(oldCoordinates.size() + i));
        }
        return adjustedType;
    }

    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(newCoordinates);
        Objects.requireNonNull(reorder);

        List<Class<?>> targetCoordinates = target.coordinateTypes();
        MHUtils.permuteArgumentChecks(reorder,
                MethodType.methodType(void.class, newCoordinates),
                MethodType.methodType(void.class, targetCoordinates));

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> Handles.permuteArguments(modeHandle,
                        methodTypeFor(accessType(mode), modeHandle.type(), targetCoordinates, newCoordinates),
                        reorderArrayFor(accessType(mode), newCoordinates, reorder)));
    }

    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(valueTypes);

        List<Class<?>> targetCoordinates = target.coordinateTypes();
        if (pos < 0 || pos > targetCoordinates.size()) {
            throw newIllegalArgumentException("Invalid position " + pos + " for coordinate types", targetCoordinates);
        }

        if (valueTypes.length == 0) return target;

        List<Class<?>> newCoordinates = new ArrayList<>(targetCoordinates);
        newCoordinates.addAll(pos, List.of(valueTypes));

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> Handles.dropArguments(modeHandle, pos, valueTypes));
    }

    private static VarHandle newIndirectVarHandle(
            VarHandle target, Class<?> varType, Class<?>[] coordinates,
            BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory) {
        VarHandleImpl impl = (VarHandleImpl) target;
        int modeMask = impl.getAccessModesBitMask();
        return VarHandleImpl.newVarHandle(modeMask, (mode, type) -> {
            MethodHandle targetHandle = target.toMethodHandle(mode);
            return handleFactory.apply(mode, targetHandle);
        }, varType, coordinates);
    }
}

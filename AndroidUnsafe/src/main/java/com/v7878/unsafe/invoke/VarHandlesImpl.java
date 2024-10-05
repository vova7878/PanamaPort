package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.newIllegalArgumentException;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.invoke.VarHandleImpl.accessType;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class VarHandlesImpl {
    private VarHandlesImpl() {
    }

    private static Class<?> lastParameterType(MethodType type) {
        int len = type.parameterCount();
        return len == 0 ? void.class : type.parameterType(len - 1);
    }

    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(filterToTarget);
        Objects.requireNonNull(filterFromTarget);

        //check that from/to filters have right signatures
        if (filterFromTarget.type().parameterCount() != 1) {
            throw newIllegalArgumentException("filterFromTarget filter type has wrong arity", filterFromTarget.type());
        } else if (filterToTarget.type().parameterCount() != 1) {
            throw newIllegalArgumentException("filterToTarget filter type has wrong arity", filterFromTarget.type());
        } else if (lastParameterType(filterFromTarget.type()) != filterToTarget.type().returnType() ||
                lastParameterType(filterToTarget.type()) != filterFromTarget.type().returnType()) {
            throw newIllegalArgumentException("filterFromTarget and filterToTarget filter types do not match", filterFromTarget.type(), filterToTarget.type());
        } else if (target.varType() != lastParameterType(filterFromTarget.type())) {
            throw newIllegalArgumentException("filterFromTarget filter type does not match target var handle type", filterFromTarget.type(), target.varType());
        } else if (target.varType() != filterToTarget.type().returnType()) {
            throw newIllegalArgumentException("filterFromTarget filter type does not match target var handle type", filterToTarget.type(), target.varType());
        }
        return newIndirectVarHandle(target, filterFromTarget.type().returnType(),
                target.coordinateTypes().toArray(new Class[0]), (mode, modeHandle) -> {
                    int lastParameterPos = modeHandle.type().parameterCount() - 1;
                    switch (accessType(mode)) {
                        case GET, GET_ATOMIC -> {
                            return MethodHandlesFixes.filterReturnValue(modeHandle, filterFromTarget);
                        }
                        case SET, SET_ATOMIC -> {
                            return MethodHandlesFixes.filterArguments(modeHandle, lastParameterPos, filterToTarget);
                        }
                        case COMPARE_AND_SET -> {
                            return MethodHandlesFixes.filterArguments(modeHandle, lastParameterPos - 1, filterToTarget, filterToTarget);
                        }
                        case COMPARE_AND_EXCHANGE -> {
                            MethodHandle adapter = MethodHandlesFixes.filterReturnValue(modeHandle, filterFromTarget);
                            return MethodHandlesFixes.filterArguments(adapter, lastParameterPos - 1, filterToTarget, filterToTarget);
                        }
                        case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE, GET_AND_UPDATE_NUMERIC -> {
                            MethodHandle adapter = MethodHandlesFixes.filterReturnValue(modeHandle, filterFromTarget);
                            return MethodHandlesFixes.filterArguments(adapter, lastParameterPos, filterToTarget);
                        }
                    }
                    throw shouldNotReachHere();
                });
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
            MethodHandle filter = Objects.requireNonNull(filters[i]);
            MethodType filterType = filter.type();
            if (filterType.parameterCount() != 1) {
                throw newIllegalArgumentException("Invalid filter type " + filterType);
            } else if (newCoordinates.get(pos + i) != filterType.returnType()) {
                throw newIllegalArgumentException("Invalid filter type " + filterType + " for coordinate type " + newCoordinates.get(i));
            }
            newCoordinates.set(pos + i, filters[i].type().parameterType(0));
        }

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> MethodHandlesFixes.filterArguments(modeHandle, pos, filters));
    }

    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(filter);

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
                (mode, modeHandle) -> MethodHandlesFixes.collectArguments(modeHandle, pos, filter));
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
                (mode, modeHandle) -> MethodHandlesFixes.insertArguments(modeHandle, pos, values));
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
        MethodHandlesFixes.permuteArgumentChecks(reorder,
                MethodType.methodType(void.class, newCoordinates),
                MethodType.methodType(void.class, targetCoordinates));

        return newIndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> MethodHandlesFixes.permuteArguments(modeHandle,
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
                (mode, modeHandle) -> MethodHandlesFixes.dropArguments(modeHandle, pos, valueTypes));
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

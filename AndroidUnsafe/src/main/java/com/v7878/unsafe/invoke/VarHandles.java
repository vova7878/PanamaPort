package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.newIllegalArgumentException;

import com.v7878.foreign.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class VarHandles {
    private VarHandles() {
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
        return IndirectVarHandle.filterValue(target, filterToTarget, filterFromTarget);
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

        return new IndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> MethodHandles.filterArguments(modeHandle, pos, filters));
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

        return new IndirectVarHandle(target, target.varType(), newCoordinates.toArray(new Class<?>[0]),
                (mode, modeHandle) -> MethodHandlesFixes.collectArguments(modeHandle, pos, filter));
    }

    private static class IndirectVarHandle extends AbstractVarHandle {
        private final VarHandle target;
        private final BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory;

        IndirectVarHandle(VarHandle target, Class<?> varType, Class<?>[] coordinates,
                          BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory) {
            super(varType, coordinates);
            this.target = target;
            this.handleFactory = handleFactory;
        }

        @Override
        protected MethodHandle getMethodHandleUncached(int modeOrdinal) {
            AccessMode mode = AccessMode.values()[modeOrdinal];
            MethodHandle targetHandle = target.toMethodHandle(mode);
            MethodHandle out = handleFactory.apply(mode, targetHandle);
            if (!out.type().equals(accessModeType(mode))) { // NPE check
                throw new IllegalStateException("handleFactory returned MethodHandle with wrong type: " + out.type());
            }
            return out;
        }

        @Override
        public boolean isAccessModeSupported(AccessMode accessMode) {
            return target.isAccessModeSupported(accessMode);
        }

        public static IndirectVarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
            return new IndirectVarHandle(target, filterFromTarget.type().returnType(),
                    target.coordinateTypes().toArray(new Class[0]), (mode, modeHandle) -> {
                int lastParameterPos = modeHandle.type().parameterCount() - 1;
                switch (accessType(mode)) {
                    case GET -> {
                        return MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                    }
                    case SET -> {
                        return MethodHandles.filterArguments(modeHandle, lastParameterPos, filterToTarget);
                    }
                    case COMPARE_AND_SET -> {
                        return MethodHandles.filterArguments(modeHandle, lastParameterPos - 1, filterToTarget, filterToTarget);
                    }
                    case COMPARE_AND_EXCHANGE -> {
                        MethodHandle adapter = MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                        return MethodHandles.filterArguments(adapter, lastParameterPos - 1, filterToTarget, filterToTarget);
                    }
                    case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE, GET_AND_UPDATE_NUMERIC -> {
                        MethodHandle adapter = MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                        return MethodHandles.filterArguments(adapter, lastParameterPos, filterToTarget);
                    }
                }
                throw new AssertionError("Cannot get here");
            });
        }
    }
}

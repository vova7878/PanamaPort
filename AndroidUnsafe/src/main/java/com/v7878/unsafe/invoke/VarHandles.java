package com.v7878.unsafe.invoke;

import com.v7878.foreign.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.BiFunction;

public final class VarHandles {
    private VarHandles() {
    }

    private static RuntimeException newIllegalArgumentException(String message, Object obj) {
        return new IllegalArgumentException(message + ": " + obj);
    }

    private static RuntimeException newIllegalArgumentException(String message, Object obj1, Object obj2) {
        return new IllegalArgumentException(message + ": " + obj1 + ", " + obj2);
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
                    case GET:
                        return MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                    case SET:
                        return MethodHandles.collectArguments(modeHandle, lastParameterPos, filterToTarget);
                    case COMPARE_AND_SET: {
                        MethodHandle adapter = MethodHandles.collectArguments(modeHandle, lastParameterPos, filterToTarget);
                        return MethodHandles.collectArguments(adapter, lastParameterPos - 1, filterToTarget);
                    }
                    case COMPARE_AND_EXCHANGE: {
                        MethodHandle adapter = MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                        adapter = MethodHandles.collectArguments(adapter, lastParameterPos, filterToTarget);
                        return MethodHandles.collectArguments(adapter, lastParameterPos - 1, filterToTarget);
                    }
                    case GET_AND_UPDATE:
                    case GET_AND_UPDATE_BITWISE:
                    case GET_AND_UPDATE_NUMERIC: {
                        MethodHandle adapter = MethodHandles.filterReturnValue(modeHandle, filterFromTarget);
                        return MethodHandles.collectArguments(adapter, lastParameterPos, filterToTarget);
                    }
                }
                throw new AssertionError("Cannot get here");
            });
        }
    }
}

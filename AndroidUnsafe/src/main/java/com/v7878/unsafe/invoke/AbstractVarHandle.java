package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.invoke.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractVarHandle extends VarHandle {
    public final Object get(Object... args) {
        return invoke(AccessMode.GET, args);
    }

    public final void set(Object... args) {
        invoke(AccessMode.SET, args);
    }

    // Volatile accessors

    public final Object getVolatile(Object... args) {
        return invoke(AccessMode.GET_VOLATILE, args);
    }

    public final void setVolatile(Object... args) {
        invoke(AccessMode.SET_VOLATILE, args);
    }

    public final Object getOpaque(Object... args) {
        return invoke(AccessMode.GET_OPAQUE, args);
    }

    public final void setOpaque(Object... args) {
        invoke(AccessMode.SET_OPAQUE, args);
    }

    // Lazy accessors

    public final Object getAcquire(Object... args) {
        return invoke(AccessMode.GET_ACQUIRE, args);
    }

    public final void setRelease(Object... args) {
        invoke(AccessMode.SET_RELEASE, args);
    }

    // Compare and set accessors

    public final boolean compareAndSet(Object... args) {
        return (boolean) invoke(AccessMode.COMPARE_AND_SET, args);
    }

    public final Object compareAndExchange(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE, args);
    }

    public final Object compareAndExchangeAcquire(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE_ACQUIRE, args);
    }

    public final Object compareAndExchangeRelease(Object... args) {
        return invoke(AccessMode.COMPARE_AND_EXCHANGE_RELEASE, args);
    }

    // Weak (spurious failures allowed)

    public final boolean weakCompareAndSetPlain(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_PLAIN, args);
    }

    public final boolean weakCompareAndSet(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET, args);
    }

    public final boolean weakCompareAndSetAcquire(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_ACQUIRE, args);
    }

    public final boolean weakCompareAndSetRelease(Object... args) {
        return (boolean) invoke(AccessMode.WEAK_COMPARE_AND_SET_RELEASE, args);
    }

    public final Object getAndSet(Object... args) {
        return invoke(AccessMode.GET_AND_SET, args);
    }

    public final Object getAndSetAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_SET_ACQUIRE, args);
    }

    public final Object getAndSetRelease(Object... args) {
        return invoke(AccessMode.GET_AND_SET_RELEASE, args);
    }

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    public final Object getAndAdd(Object... args) {
        return invoke(AccessMode.GET_AND_ADD, args);
    }

    public final Object getAndAddAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_ADD_ACQUIRE, args);
    }

    public final Object getAndAddRelease(Object... args) {
        return invoke(AccessMode.GET_AND_ADD_RELEASE, args);
    }

    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    public final Object getAndBitwiseOr(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR, args);
    }

    public final Object getAndBitwiseOrAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR_ACQUIRE, args);
    }

    public final Object getAndBitwiseOrRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_OR_RELEASE, args);
    }

    public final Object getAndBitwiseAnd(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND, args);
    }

    public final Object getAndBitwiseAndAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND_ACQUIRE, args);
    }

    public final Object getAndBitwiseAndRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_AND_RELEASE, args);
    }

    public final Object getAndBitwiseXor(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR, args);
    }

    public final Object getAndBitwiseXorAcquire(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR_ACQUIRE, args);
    }

    public final Object getAndBitwiseXorRelease(Object... args) {
        return invoke(AccessMode.GET_AND_BITWISE_XOR_RELEASE, args);
    }

    //

    public enum AccessType {
        GET,
        SET,

        GET_ATOMIC,
        SET_ATOMIC,

        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,

        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        public MethodType accessModeType(Class<?> value, Class<?>... coordinates) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET, GET_ATOMIC -> {
                    ps = allocateParameters(0, coordinates);
                    fillParameters(ps, coordinates);
                    return MethodType.methodType(value, ps);
                }
                case SET, SET_ATOMIC -> {
                    ps = allocateParameters(1, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i] = value;
                    return MethodType.methodType(void.class, ps);
                }
                case COMPARE_AND_SET -> {
                    ps = allocateParameters(2, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(boolean.class, ps);
                }
                case COMPARE_AND_EXCHANGE -> {
                    ps = allocateParameters(2, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                }
                case GET_AND_UPDATE, GET_AND_UPDATE_BITWISE, GET_AND_UPDATE_NUMERIC -> {
                    ps = allocateParameters(1, coordinates);
                    i = fillParameters(ps, coordinates);
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                }
                default -> throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int values, Class<?>... coordinates) {
            int size = coordinates.length + values;
            return new Class<?>[size];
        }

        private static int fillParameters(Class<?>[] ps, Class<?>... coordinates) {
            int i = 0;
            for (Class<?> arg : coordinates) ps[i++] = arg;
            return i;
        }
    }

    public static AccessType accessType(AccessMode accessMode) {
        return VarHandle.accessType(accessMode);
    }

    //

    public final Class<?> varType() {
        return varType;
    }

    public final List<Class<?>> coordinateTypes() {
        return List.of(coordinates);
    }

    public final MethodType accessModeType(AccessMode mode) {
        return accessModeType(accessType(mode));
    }

    public final MethodHandle toMethodHandle(AccessMode mode) {
        return getMethodHandle(mode);
    }

    // implementation details

    /**
     * The target type for accesses.
     */
    private final Class<?> varType;
    /**
     * Coordinates of this VarHandle.
     */
    private final Class<?>[] coordinates;

    AbstractVarHandle(Class<?> varType, Class<?>... coordinates) {
        this.varType = varType;
        this.coordinates = coordinates;
    }

    private static Class<?> requireNonVoid(Class<?> clazz) {
        if (clazz == void.class) {
            throw new IllegalArgumentException("parameter type cannot be void");
        }
        return clazz;
    }

    protected static void checkVarType(Class<?> varType) {
        Objects.requireNonNull(requireNonVoid(varType));
    }

    protected static void checkCoordinates(Class<?>... coordinates) {
        Stream.of(Objects.requireNonNull(coordinates))
                .map(Objects::requireNonNull)
                .forEach(AbstractVarHandle::requireNonVoid);
    }

    private MethodType[] methodTypeTable;
    private MethodHandle[] methodHandleTable;
    private MethodHandle[] invokerMethodHandleTable;

    private Object invoke(AccessMode mode, Object[] args) {
        return nothrows_run(() -> getInvokerHandle(accessType(mode))
                .invokeExact(toMethodHandle(mode), args));
    }

    private MethodType accessModeType(AccessType type) {
        int typeOrdinal = type.ordinal();
        MethodType[] mtTable = methodTypeTable;
        if (mtTable == null) {
            mtTable = methodTypeTable = new MethodType[AccessType.values().length];
        }
        MethodType mt = mtTable[typeOrdinal];
        if (mt == null) {
            mt = mtTable[typeOrdinal] = accessModeTypeUncached(type);
        }
        return mt;
    }

    private MethodType accessModeTypeUncached(AccessType type) {
        return type.accessModeType(varType, coordinates);
    }

    private MethodHandle getMethodHandle(AccessMode mode) {
        int modeOrdinal = mode.ordinal();
        MethodHandle[] mhTable = methodHandleTable;
        if (mhTable == null) {
            mhTable = methodHandleTable = new MethodHandle[AccessMode.values().length];
        }
        MethodHandle mh = mhTable[modeOrdinal];
        if (mh == null) {
            if (isAccessModeSupported(mode)) {
                mh = getMethodHandleUncached(mode);
            } else {
                mh = Transformers.makeTransformer(accessModeType(mode), (thiz, stackFrame) -> {
                    //TODO: correct message
                    throw new UnsupportedOperationException(mode + " is not supported");
                });
            }
            return mhTable[modeOrdinal] = mh;
        }
        return mh;
    }

    private MethodHandle getInvokerHandleUncached(AccessType accessType) {
        MethodType type = accessModeType(accessType);
        MethodHandle invoker = MethodHandlesFixes.exactInvoker(type);
        invoker = MethodHandlesFixes.asTypeAdapter(invoker,
                type.generic().insertParameterTypes(0, MethodHandle.class));
        return invoker.asSpreader(Object[].class, type.parameterCount());
    }

    private MethodHandle getInvokerHandle(AccessType type) {
        int typeOrdinal = type.ordinal();
        MethodHandle[] mhTable = invokerMethodHandleTable;
        if (mhTable == null) {
            mhTable = invokerMethodHandleTable = new MethodHandle[AccessType.values().length];
        }
        MethodHandle mh = mhTable[typeOrdinal];
        if (mh == null) {
            mh = mhTable[typeOrdinal] = getInvokerHandleUncached(type);
        }
        return mh;
    }

    protected abstract MethodHandle getMethodHandleUncached(AccessMode mode);
}

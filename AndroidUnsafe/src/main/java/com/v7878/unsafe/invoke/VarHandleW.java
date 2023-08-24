package com.v7878.unsafe.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VarHandleW {
    public final Object get(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final void set(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Volatile accessors

    public final Object getVolatile(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final void setVolatile(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getOpaque(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final void setOpaque(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Lazy accessors

    public final Object getAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final void setRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Compare and set accessors

    public final boolean compareAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchange(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchangeAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object compareAndExchangeRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Weak (spurious failures allowed)

    public final boolean weakCompareAndSetPlain(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSetAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean weakCompareAndSetRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSet(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSetAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndSetRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    public final Object getAndAdd(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndAddAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndAddRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    public final Object getAndBitwiseOr(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseOrAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseOrRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAnd(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAndAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseAndRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXor(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXorAcquire(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final Object getAndBitwiseXorRelease(Object... args) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    enum AccessType {
        GET,
        SET,
        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,

        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        MethodType accessModeType(Class<?> receiver, Class<?> value,
                                  Class<?>... intermediate) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET:
                    ps = allocateParameters(0, receiver, intermediate);
                    fillParameters(ps, receiver, intermediate);
                    return MethodType.methodType(value, ps);
                case SET:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(void.class, ps);
                case COMPARE_AND_SET:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(boolean.class, ps);
                case COMPARE_AND_EXCHANGE:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                case GET_AND_UPDATE:
                case GET_AND_UPDATE_BITWISE:
                case GET_AND_UPDATE_NUMERIC:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                default:
                    throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(
                int values, Class<?> receiver, Class<?>... intermediate) {
            int size = ((receiver != null) ? 1 : 0) + intermediate.length + values;
            return new Class<?>[size];
        }

        private static int fillParameters(
                Class<?>[] ps, Class<?> receiver, Class<?>... intermediate) {
            int i = 0;
            if (receiver != null) ps[i++] = receiver;
            for (Class<?> arg : intermediate) ps[i++] = arg;
            return i;
        }
    }

    public enum AccessMode {
        GET("get", AccessType.GET),
        SET("set", AccessType.SET),
        GET_VOLATILE("getVolatile", AccessType.GET),
        SET_VOLATILE("setVolatile", AccessType.SET),
        GET_ACQUIRE("getAcquire", AccessType.GET),
        SET_RELEASE("setRelease", AccessType.SET),
        GET_OPAQUE("getOpaque", AccessType.GET),
        SET_OPAQUE("setOpaque", AccessType.SET),
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SET),
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SET),
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE_BITWISE);

        static final Map<String, AccessMode> methodNameToAccessMode;

        static {
            AccessMode[] values = AccessMode.values();
            // Initial capacity of # values divided by the load factor is sufficient
            // to avoid resizes for the smallest table size (64)
            int initialCapacity = (int) (values.length / 0.75f) + 1;
            methodNameToAccessMode = new HashMap<>(initialCapacity);
            for (AccessMode am : values) {
                methodNameToAccessMode.put(am.methodName, am);
            }
        }

        final String methodName;
        final AccessType at;

        AccessMode(String methodName, AccessType at) {
            this.methodName = methodName;
            this.at = at;
        }

        public String methodName() {
            return methodName;
        }

        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode am = methodNameToAccessMode.get(methodName);
            if (am != null) return am;
            throw new IllegalArgumentException("No AccessMode value for method name " + methodName);
        }
    }

    public final Class<?> varType() {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final List<Class<?>> coordinateTypes() {
        MethodType typeGet = accessModeType(AccessMode.GET);
        return typeGet.parameterList();
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final boolean isAccessModeSupported(AccessMode accessMode) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        //TODO
        throw new UnsupportedOperationException("Not supported yet");
    }
}

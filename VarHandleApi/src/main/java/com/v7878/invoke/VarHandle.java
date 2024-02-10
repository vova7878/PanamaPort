/*
 * Copyright (c) 2024 Vladimir Kozelkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.v7878.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class VarHandle {
    public abstract Object get(Object... args);

    public abstract void set(Object... args);

    // Volatile accessors

    public abstract Object getVolatile(Object... args);

    public abstract void setVolatile(Object... args);

    public abstract Object getOpaque(Object... args);

    public abstract void setOpaque(Object... args);

    // Lazy accessors

    public abstract Object getAcquire(Object... args);

    public abstract void setRelease(Object... args);

    // Compare and set accessors

    public abstract boolean compareAndSet(Object... args);

    public abstract Object compareAndExchange(Object... args);

    public abstract Object compareAndExchangeAcquire(Object... args);

    public abstract Object compareAndExchangeRelease(Object... args);

    // Weak (spurious failures allowed)

    public abstract boolean weakCompareAndSetPlain(Object... args);

    public abstract boolean weakCompareAndSet(Object... args);

    public abstract boolean weakCompareAndSetAcquire(Object... args);

    public abstract boolean weakCompareAndSetRelease(Object... args);

    public abstract Object getAndSet(Object... args);

    public abstract Object getAndSetAcquire(Object... args);

    public abstract Object getAndSetRelease(Object... args);

    // Primitive adders
    // Throw UnsupportedOperationException for refs

    public abstract Object getAndAdd(Object... args);

    public abstract Object getAndAddAcquire(Object... args);

    public abstract Object getAndAddRelease(Object... args);

    // Bitwise operations
    // Throw UnsupportedOperationException for refs

    public abstract Object getAndBitwiseOr(Object... args);

    public abstract Object getAndBitwiseOrAcquire(Object... args);

    public abstract Object getAndBitwiseOrRelease(Object... args);

    public abstract Object getAndBitwiseAnd(Object... args);

    public abstract Object getAndBitwiseAndAcquire(Object... args);

    public abstract Object getAndBitwiseAndRelease(Object... args);

    public abstract Object getAndBitwiseXor(Object... args);

    public abstract Object getAndBitwiseXorAcquire(Object... args);

    public abstract Object getAndBitwiseXorRelease(Object... args);

    protected enum AccessType {
        GET,
        SET,
        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,

        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        public MethodType accessModeType(Class<?> value, Class<?>... coordinates) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET -> {
                    ps = allocateParameters(0, coordinates);
                    fillParameters(ps, coordinates);
                    return MethodType.methodType(value, ps);
                }
                case SET -> {
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

    protected static AccessType accessType(AccessMode accessMode) {
        return accessMode.at;
    }

    public abstract Class<?> varType();

    public abstract List<Class<?>> coordinateTypes();

    public abstract MethodType accessModeType(AccessMode accessMode);

    public abstract boolean isAccessModeSupported(AccessMode accessMode);

    public abstract MethodHandle toMethodHandle(AccessMode accessMode);

    @Override
    public final String toString() {
        return String.format("VarHandle[varType=%s, coord=%s]",
                varType().getName(),
                coordinateTypes());
    }
}

package com.v7878.unsafe.invoke;

import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractVarHandle extends VarHandle {
    public final Object get(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET).invokeWithArguments(args));
    }

    public final void set(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET).invokeWithArguments(args));
    }

    // Volatile accessors

    public final Object getVolatile(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_VOLATILE).invokeWithArguments(args));
    }

    public final void setVolatile(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_VOLATILE).invokeWithArguments(args));
    }

    public final Object getOpaque(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_OPAQUE).invokeWithArguments(args));
    }

    public final void setOpaque(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_OPAQUE).invokeWithArguments(args));
    }

    // Lazy accessors

    public final Object getAcquire(Object... args) {
        return nothrows_run(() -> toMethodHandle(AccessMode.GET_ACQUIRE).invokeWithArguments(args));
    }

    public final void setRelease(Object... args) {
        nothrows_run(() -> toMethodHandle(AccessMode.SET_RELEASE).invokeWithArguments(args));
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

    //

    public final Class<?> varType() {
        return varType;
    }

    public final List<Class<?>> coordinateTypes() {
        //noinspection Since15
        return List.of(coordinates);
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        return accessModeType(accessType(accessMode).ordinal());
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        if (isAccessModeSupported(accessMode)) {
            return getMethodHandle(accessMode.ordinal());
        } else {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }
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

    private MethodType accessModeType(int type) {
        MethodType[] mtTable = methodTypeTable;
        if (mtTable == null) {
            mtTable = methodTypeTable = new MethodType[AccessType.values().length];
        }
        MethodType mt = mtTable[type];
        if (mt == null) {
            mt = mtTable[type] = accessModeTypeUncached(type);
        }
        return mt;
    }

    private MethodType accessModeTypeUncached(int type) {
        return AccessType.values()[type].accessModeType(varType, coordinates);
    }

    private MethodHandle getMethodHandle(int mode) {
        MethodHandle[] mhTable = methodHandleTable;
        if (mhTable == null) {
            mhTable = methodHandleTable = new MethodHandle[AccessMode.values().length];
        }
        MethodHandle mh = mhTable[mode];
        if (mh == null) {
            mh = mhTable[mode] = getMethodHandleUncached(mode);
        }
        return mh;
    }

    protected abstract MethodHandle getMethodHandleUncached(int modeOrdinal);
}

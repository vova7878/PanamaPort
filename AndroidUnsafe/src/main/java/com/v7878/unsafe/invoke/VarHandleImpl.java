package com.v7878.unsafe.invoke;

import com.v7878.foreign.VarHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;

public class VarHandleImpl extends VarHandle {
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

    //

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

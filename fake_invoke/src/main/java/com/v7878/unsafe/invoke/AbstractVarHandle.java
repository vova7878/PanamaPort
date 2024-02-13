package com.v7878.unsafe.invoke;

import com.v7878.invoke.VarHandle;

import java.lang.invoke.MethodType;

public abstract class AbstractVarHandle extends VarHandle {

    public enum AccessType {
        GET,
        SET,
        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,

        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        public MethodType accessModeType(Class<?> value, Class<?>... coordinates) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}

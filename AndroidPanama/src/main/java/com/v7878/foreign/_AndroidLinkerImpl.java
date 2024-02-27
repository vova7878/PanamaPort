package com.v7878.foreign;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

final class _AndroidLinkerImpl extends _AbstractAndroidLinker {
    public static final Linker INSTANCE = new _AndroidLinkerImpl();

    @Override
    protected MethodHandle arrangeDowncall(MethodType inferredMethodType, FunctionDescriptor function, _LinkerOptions options) {
        //TODO
        throw new UnsupportedOperationException("Unsuppurted yet!");
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(MethodType targetType, FunctionDescriptor function, _LinkerOptions options) {
        //TODO
        throw new UnsupportedOperationException("Unsuppurted yet!");
    }
}

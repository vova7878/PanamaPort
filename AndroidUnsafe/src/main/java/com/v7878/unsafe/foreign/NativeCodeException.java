package com.v7878.unsafe.foreign;

public class NativeCodeException extends RuntimeException {
    private final String functionName;
    public final int code;

    public NativeCodeException(String functionName, int code) {
        this.functionName = functionName;
        this.code = code;
    }

    public NativeCodeException(String functionName, int code, Throwable cause) {
        super(cause);
        this.functionName = functionName;
        this.code = code;
    }

    @Override
    public String getMessage() {
        return functionName + " failed with code: " + code;
    }
}

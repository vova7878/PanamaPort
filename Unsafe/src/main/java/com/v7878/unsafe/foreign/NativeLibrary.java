package com.v7878.unsafe.foreign;

public abstract class NativeLibrary {
    public abstract String name();

    public abstract long find(String name);

    public final long lookup(String name) {
        long addr = find(name);
        if (0 == addr) {
            throw new IllegalArgumentException("Cannot find symbol " + name + " in library " + name());
        }
        return addr;
    }
}

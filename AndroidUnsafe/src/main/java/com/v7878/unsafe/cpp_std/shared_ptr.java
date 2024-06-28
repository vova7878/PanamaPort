package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;

import com.v7878.foreign.MemoryLayout;

//TODO: access methods
public class shared_ptr {
    private shared_ptr() {
    }

    public static final MemoryLayout LAYOUT = structLayout(sequenceLayout(2, ADDRESS));
}

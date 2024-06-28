package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;

import com.v7878.foreign.MemoryLayout;

//TODO: access methods
public class unique_ptr {
    private unique_ptr() {
    }

    public static final MemoryLayout LAYOUT = structLayout(ADDRESS);
}

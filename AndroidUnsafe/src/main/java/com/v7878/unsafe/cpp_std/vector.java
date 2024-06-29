package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.AndroidUnsafe;

public class vector {
    private vector() {
    }

    public static final MemoryLayout LAYOUT = structLayout(sequenceLayout(3, ADDRESS));

    private static MemorySegment to_vec(MemorySegment vec) {
        return vec.asSlice(0, LAYOUT);
    }

    private static MemorySegment begin0(MemorySegment vec) {
        return vec.get(ADDRESS, 0);
    }

    public static MemorySegment begin(MemorySegment vec) {
        return begin0(to_vec(vec));
    }

    private static MemorySegment end0(MemorySegment vec) {
        return vec.get(ADDRESS, ADDRESS.byteSize());
    }

    public static MemorySegment end(MemorySegment vec) {
        return end0(to_vec(vec));
    }

    private static MemorySegment end_of_storage0(MemorySegment vec) {
        return vec.get(ADDRESS, ADDRESS.byteSize() * 2);
    }

    public static MemorySegment end_of_storage(MemorySegment vec) {
        return end_of_storage0(to_vec(vec));
    }

    private static long byte_size0(MemorySegment vec) {
        return end0(vec).nativeAddress() - begin0(vec).nativeAddress();
    }

    public static long byte_size(MemorySegment vec) {
        return byte_size0(to_vec(vec));
    }

    private static long byte_capacity0(MemorySegment vec) {
        return end_of_storage0(vec).nativeAddress() - begin0(vec).nativeAddress();
    }

    public static long byte_capacity(MemorySegment vec) {
        return byte_capacity0(to_vec(vec));
    }

    private static MemorySegment data0(MemorySegment vec) {
        return begin0(vec).reinterpret(byte_size0(vec));
    }

    public static MemorySegment data(MemorySegment vec) {
        return data0(vec);
    }

    private static void destruct0(MemorySegment vec) {
        long data = begin0(vec).nativeAddress();
        if (data != 0) {
            AndroidUnsafe.freeMemory(data);
        }
    }

    public static void destruct(MemorySegment vec) {
        destruct0(to_vec(vec));
    }
}
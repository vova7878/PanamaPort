package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.foreign.ExtraLayouts.C_LONG_LONG;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.AndroidUnsafe;

import java.util.Objects;

public final class string {
    private string() {
    }

    public static final MemoryLayout LAYOUT = structLayout(sequenceLayout(3, ADDRESS));

    private static MemorySegment make_str(MemorySegment str) {
        return str.asSlice(0, LAYOUT);
    }

    private static long get_word(MemorySegment ptr, long offset) {
        return IS64BIT ? ptr.get(C_LONG_LONG, offset) : (ptr.get(JAVA_INT, offset) & 0xffffffffL);
    }

    private static boolean is_short0(MemorySegment str) {
        return (str.get(JAVA_BYTE, 0) & 1) == 0;
    }

    public static boolean is_short(MemorySegment str) {
        return is_short0(make_str(str));
    }

    private static MemorySegment data0(MemorySegment str) {
        return is_short0(str) ? str.asSlice(1, 0) : str.get(ADDRESS, ADDRESS.byteSize() * 2);
    }

    public static MemorySegment data(MemorySegment str) {
        return data0(make_str(str)).reinterpret(length0(str));
    }

    private static long capacity0(MemorySegment str) {
        return is_short0(str) ? 22 : (get_word(str, 0) & ~1) - 1;
    }

    public static long capacity(MemorySegment str) {
        return capacity0(make_str(str));
    }

    private static long length0(MemorySegment str) {
        return is_short0(str) ? str.get(JAVA_BYTE, 0) >> 1 : get_word(str, ADDRESS.byteSize());
    }

    public static long length(MemorySegment str) {
        return length0(make_str(str));
    }

    private static void destruct0(MemorySegment str) {
        if (!is_short0(str)) {
            AndroidUnsafe.freeMemory(get_word(str, ADDRESS.byteSize() * 2));
        }
    }

    public static void destruct(MemorySegment str) {
        destruct0(make_str(str));
    }

    private static void assign0(MemorySegment str, MemorySegment data) {
        long length = data.byteSize();
        MemorySegment dst;
        long offset;
        if (length < 23) {
            (dst = str).set(JAVA_BYTE, 0, (byte) (length << 1));
            offset = 1;
        } else {
            dst = Arena.global().allocate(length + 1, ADDRESS.byteAlignment());
            offset = 0;
        }
        MemorySegment.copy(data, 0, dst, offset, length);
        dst.set(JAVA_BYTE, offset + length, (byte) 0);
    }

    public static void assign(MemorySegment str, MemorySegment data) {
        assign0(make_str(str), Objects.requireNonNull(data));
    }
}

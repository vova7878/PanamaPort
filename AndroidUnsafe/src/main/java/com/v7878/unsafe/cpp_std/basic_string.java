package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemoryLayout.unionLayout;
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

public final class basic_string {
    public static final basic_string string = new basic_string(JAVA_BYTE);

    public final MemoryLayout ELEMENT;
    public final MemoryLayout LONG_LAYOUT;
    public final MemoryLayout SHORT_LAYOUT;
    public final MemoryLayout LAYOUT;

    private final long min_capacity;

    public basic_string(MemoryLayout element) {
        this.ELEMENT = Objects.requireNonNull(element);
        this.LONG_LAYOUT = structLayout(sequenceLayout(3, ADDRESS));
        long min_cap = (LONG_LAYOUT.byteSize() - 1) / ELEMENT.byteSize();
        min_cap = min_cap < 2 ? 2 : min_cap;
        this.SHORT_LAYOUT = structLayout(JAVA_BYTE,
                sequenceLayout(ELEMENT.byteSize() - 1, JAVA_BYTE),
                sequenceLayout(min_cap, ELEMENT));
        this.LAYOUT = unionLayout(LONG_LAYOUT, SHORT_LAYOUT);
        this.min_capacity = min_cap;
    }

    public class impl {
        private final MemorySegment str;

        public impl(MemorySegment str) {
            this.str = str.asSlice(0, LAYOUT);
        }

        private static long get_word(MemorySegment ptr, long offset) {
            return IS64BIT ? ptr.get(C_LONG_LONG, offset) : (ptr.get(JAVA_INT, offset) & 0xffffffffL);
        }

        public boolean is_short() {
            return (str.get(JAVA_BYTE, 0) & 1) == 0;
        }

        public MemorySegment data() {
            MemorySegment out = is_short() ?
                    str.asSlice(ELEMENT.byteSize(), 0) :
                    str.get(ADDRESS, ADDRESS.byteSize() * 2);
            return out.reinterpret(length() * ELEMENT.byteSize());
        }

        public long capacity() {
            return is_short() ? min_capacity - 1 : (get_word(str, 0) & ~1) - 1;
        }

        public long length() {
            return is_short() ? str.get(JAVA_BYTE, 0) >> 1 : get_word(str, ADDRESS.byteSize());
        }

        public void destruct() {
            if (!is_short()) {
                // TODO: aligned free
                AndroidUnsafe.freeMemory(get_word(str, ADDRESS.byteSize() * 2));
            }
        }

        public void assign(MemorySegment data) {
            long bytes = data.byteSize();
            if (bytes % ELEMENT.byteSize() != 0) {
                throw new IllegalArgumentException("data size is not multiple of element size");
            }
            long length = bytes / ELEMENT.byteSize();
            MemorySegment dst;
            long offset;
            if (length < min_capacity) {
                (dst = str).set(JAVA_BYTE, 0, (byte) (length << 1));
                offset = ELEMENT.byteSize();
            } else {
                // TODO: aligned alloc
                dst = Arena.global().allocate(bytes + ELEMENT.byteSize(), ADDRESS.byteAlignment());
                offset = 0;
            }
            MemorySegment.copy(data, 0, dst, offset, bytes);
            dst.asSlice(offset + bytes, ELEMENT.byteSize()).fill((byte) 0);
        }
    }
}

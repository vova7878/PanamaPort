package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.AndroidUnsafe;

import java.util.Objects;

public class vector_template {

    public final MemoryLayout ELEMENT;
    public final MemoryLayout LAYOUT;

    private vector_template(MemoryLayout element) {
        this.ELEMENT = Objects.requireNonNull(element);
        this.LAYOUT = structLayout(sequenceLayout(3, ADDRESS));
    }

    public class impl {
        private final MemorySegment vec;

        public impl(MemorySegment vec) {
            this.vec = vec.asSlice(0, LAYOUT);
        }

        public MemorySegment begin() {
            return vec.get(ADDRESS, 0);
        }

        public MemorySegment end() {
            return vec.get(ADDRESS, ADDRESS.byteSize());
        }

        public MemorySegment end_of_storage() {
            return vec.get(ADDRESS, ADDRESS.byteSize() * 2);
        }

        public long byte_size() {
            return end().nativeAddress() - begin().nativeAddress();
        }

        public long size() {
            return byte_size() / ELEMENT.byteSize();
        }

        public long byte_capacity() {
            return end_of_storage().nativeAddress() - begin().nativeAddress();
        }

        public long capacity() {
            return byte_capacity() / ELEMENT.byteSize();
        }

        public MemorySegment data() {
            return begin().reinterpret(byte_size());
        }

        public void destruct() {
            long data = begin().nativeAddress();
            if (data != 0) {
                // TODO: aligned free
                AndroidUnsafe.freeMemory(data);
            }
        }

        // TODO: public void assign(MemorySegment data) {}
    }
}
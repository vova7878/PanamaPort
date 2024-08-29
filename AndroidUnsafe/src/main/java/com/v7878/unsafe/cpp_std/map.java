package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemorySegment.NULL;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;

import java.util.Objects;

public class map {

    public final MemoryLayout ELEMENT1;
    public final MemoryLayout ELEMENT2;
    public final MemoryLayout PAIR;

    private final MemoryLayout TREE_NODE;
    private final AddressLayout TREE_NODE_PTR;

    private final long FIRST_OFFSET;
    private final long SECOND_OFFSET;

    public final MemoryLayout LAYOUT;

    public map(MemoryLayout element1, MemoryLayout element2) {
        this.ELEMENT1 = Objects.requireNonNull(element1);
        this.ELEMENT2 = Objects.requireNonNull(element2);
        this.PAIR = paddedStructLayout(element1.withName("first"), element2.withName("second"));
        this.TREE_NODE = paddedStructLayout(sequenceLayout(3, ADDRESS), JAVA_BYTE, PAIR.withName("pair"));
        this.TREE_NODE_PTR = ADDRESS.withTargetLayout(TREE_NODE);

        this.FIRST_OFFSET = TREE_NODE.byteOffset(groupElement("pair"), groupElement("first"));
        this.SECOND_OFFSET = TREE_NODE.byteOffset(groupElement("pair"), groupElement("second"));

        this.LAYOUT = structLayout(sequenceLayout(3, ADDRESS));
    }

    public class iterator {
        private final MemorySegment iter;

        iterator(MemorySegment iter) {
            this.iter = iter.reinterpret(TREE_NODE.byteSize());
        }

        public MemorySegment first() {
            return iter.asSlice(FIRST_OFFSET, ELEMENT1);
        }

        public MemorySegment second() {
            return iter.asSlice(SECOND_OFFSET, ELEMENT2);
        }

        public iterator next() {
            MemorySegment tmp1 = iter;
            MemorySegment tmp2 = tmp1.get(TREE_NODE_PTR, ADDRESS.byteSize());
            MemorySegment tmp3;
            if (tmp2.equals(NULL)) {
                while (true) {
                    tmp3 = tmp1.get(TREE_NODE_PTR, ADDRESS.byteSize() * 2L);
                    if (tmp3.get(ADDRESS, 0).equals(tmp1)) {
                        break;
                    }
                    tmp1 = tmp3;
                }
            } else {
                do {
                    tmp3 = tmp2;
                    tmp2 = tmp2.get(TREE_NODE_PTR, 0);
                } while (!tmp2.equals(NULL));
            }
            return new iterator(tmp3);
        }

        public iterator previous() {
            MemorySegment tmp1 = iter;
            MemorySegment tmp2 = tmp1.get(TREE_NODE_PTR, 0);
            MemorySegment tmp3;
            if (tmp2.equals(NULL)) {
                while (true) {
                    tmp3 = tmp1.get(TREE_NODE_PTR, ADDRESS.byteSize() * 2L);
                    if (tmp3.get(ADDRESS, 0).equals(tmp1)) {
                        break;
                    }
                    tmp1 = tmp3;
                }
            } else {
                do {
                    tmp3 = tmp2;
                    tmp2 = tmp2.get(TREE_NODE_PTR, ADDRESS.byteSize());
                } while (!tmp2.equals(NULL));
            }
            return new iterator(tmp3);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof iterator iobj)) {
                return false;
            }
            return iter.equals(iobj.iter);
        }
    }

    public class impl {
        private final MemorySegment map;

        public impl(MemorySegment map) {
            this.map = map.asSlice(0, LAYOUT);
        }

        private static long get_word(MemorySegment ptr, long offset) {
            return IS64BIT ? ptr.get(JAVA_LONG, offset) : (ptr.get(JAVA_INT, offset) & 0xffffffffL);
        }

        public iterator begin() {
            return new iterator(map.get(ADDRESS, 0));
        }

        public iterator end() {
            return new iterator(map.asSlice(ADDRESS.byteSize(), 0));
        }

        public long size() {
            return get_word(map, ADDRESS.byteSize() * 2);
        }

        //TODO: find
        //TODO: erase
        //TODO: insert
        //TODO: public void destruct() {}
    }
}

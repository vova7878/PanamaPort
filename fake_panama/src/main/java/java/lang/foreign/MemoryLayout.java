package java.lang.foreign;

import java.lang.invoke.MethodHandle;
import java.util.Optional;

public interface MemoryLayout {

    long byteSize();

    Optional<String> name();

    MemoryLayout withName(String name);

    MemoryLayout withoutName();

    long byteAlignment();

    MemoryLayout withByteAlignment(long byteAlignment);

    long byteOffset(PathElement... elements);

    MethodHandle byteOffsetHandle(PathElement... elements);

    MemoryVarHandle varHandle(PathElement... elements);

    MethodHandle sliceHandle(PathElement... elements);

    MemoryLayout select(PathElement... elements);

    interface PathElement {

        static PathElement groupElement(String name) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement groupElement(long index) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement(long index) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement(long start, long step) {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement sequenceElement() {
            throw new UnsupportedOperationException("Stub!");
        }

        static PathElement dereferenceElement() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    boolean equals(Object other);

    int hashCode();

    @Override
    String toString();

    static PaddingLayout paddingLayout(long byteSize) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SequenceLayout sequenceLayout(long elementCount, MemoryLayout elementLayout) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SequenceLayout sequenceLayout(MemoryLayout elementLayout) {
        throw new UnsupportedOperationException("Stub!");
    }

    static StructLayout structLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    static StructLayout paddedStructLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    static UnionLayout unionLayout(MemoryLayout... elements) {
        throw new UnsupportedOperationException("Stub!");
    }
}

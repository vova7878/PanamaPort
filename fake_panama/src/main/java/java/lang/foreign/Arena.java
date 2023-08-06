package java.lang.foreign;

import java.lang.foreign.MemorySegment.Scope;

public interface Arena extends SegmentAllocator, AutoCloseable {

    static Arena ofAuto() {
        throw new UnsupportedOperationException("Stub!");
    }

    static Arena global() {
        throw new UnsupportedOperationException("Stub!");
    }

    static Arena ofConfined() {
        throw new UnsupportedOperationException("Stub!");
    }

    static Arena ofShared() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    MemorySegment allocate(long byteSize, long byteAlignment);

    Scope scope();

    @Override
    void close();
}

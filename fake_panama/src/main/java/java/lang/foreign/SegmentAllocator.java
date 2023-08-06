package java.lang.foreign;

public interface SegmentAllocator {

    MemorySegment allocateUtf8String(String str);

    MemorySegment allocate(ValueLayout.OfByte layout, byte value);

    MemorySegment allocate(ValueLayout.OfChar layout, char value);

    MemorySegment allocate(ValueLayout.OfShort layout, short value);

    MemorySegment allocate(ValueLayout.OfInt layout, int value);

    MemorySegment allocate(ValueLayout.OfFloat layout, float value);

    MemorySegment allocate(ValueLayout.OfLong layout, long value);

    MemorySegment allocate(ValueLayout.OfDouble layout, double value);

    MemorySegment allocate(AddressLayout layout, MemorySegment value);

    MemorySegment allocateArray(ValueLayout.OfByte elementLayout, byte... elements);

    MemorySegment allocateArray(ValueLayout.OfShort elementLayout, short... elements);

    MemorySegment allocateArray(ValueLayout.OfChar elementLayout, char... elements);

    MemorySegment allocateArray(ValueLayout.OfInt elementLayout, int... elements);

    MemorySegment allocateArray(ValueLayout.OfFloat elementLayout, float... elements);

    MemorySegment allocateArray(ValueLayout.OfLong elementLayout, long... elements);

    MemorySegment allocateArray(ValueLayout.OfDouble elementLayout, double... elements);

    MemorySegment allocate(MemoryLayout layout);

    MemorySegment allocateArray(MemoryLayout elementLayout, long count);

    MemorySegment allocate(long byteSize);

    MemorySegment allocate(long byteSize, long byteAlignment);

    static SegmentAllocator slicingAllocator(MemorySegment segment) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SegmentAllocator prefixAllocator(MemorySegment segment) {
        throw new UnsupportedOperationException("Stub!");
    }
}

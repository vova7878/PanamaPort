package java.lang.foreign;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface MemorySegment {

    long address();

    Optional<Object> heapBase();

    Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout);

    Stream<MemorySegment> elements(MemoryLayout elementLayout);

    Scope scope();

    boolean isAccessibleBy(Thread thread);

    long byteSize();

    MemorySegment asSlice(long offset, long newSize);

    MemorySegment asSlice(long offset, long newSize, long byteAlignment);

    MemorySegment asSlice(long offset, MemoryLayout layout);

    MemorySegment asSlice(long offset);

    MemorySegment reinterpret(long newSize);

    MemorySegment reinterpret(Arena arena, Consumer<MemorySegment> cleanup);

    MemorySegment reinterpret(long newSize, Arena arena, Consumer<MemorySegment> cleanup);

    boolean isReadOnly();

    MemorySegment asReadOnly();

    boolean isNative();

    boolean isMapped();

    Optional<MemorySegment> asOverlappingSlice(MemorySegment other);

    long segmentOffset(MemorySegment other);

    MemorySegment fill(byte value);

    MemorySegment copyFrom(MemorySegment src);

    long mismatch(MemorySegment other);

    boolean isLoaded();

    void load();

    void unload();

    void force();

    ByteBuffer asByteBuffer();

    byte[] toArray(ValueLayout.OfByte elementLayout);

    short[] toArray(ValueLayout.OfShort elementLayout);

    char[] toArray(ValueLayout.OfChar elementLayout);

    int[] toArray(ValueLayout.OfInt elementLayout);

    float[] toArray(ValueLayout.OfFloat elementLayout);

    long[] toArray(ValueLayout.OfLong elementLayout);

    double[] toArray(ValueLayout.OfDouble elementLayout);

    String getUtf8String(long offset);

    void setUtf8String(long offset, String str);

    static MemorySegment ofBuffer(Buffer buffer) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(byte[] byteArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(char[] charArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(short[] shortArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(int[] intArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(float[] floatArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(long[] longArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    static MemorySegment ofArray(double[] doubleArray) {
        throw new UnsupportedOperationException("Stub!");
    }

    MemorySegment NULL = ((Supplier<MemorySegment>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();

    static MemorySegment ofAddress(long address) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(MemorySegment srcSegment, long srcOffset,
                     MemorySegment dstSegment, long dstOffset, long bytes) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset,
                     MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset,
                     long elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    byte get(ValueLayout.OfByte layout, long offset);

    void set(ValueLayout.OfByte layout, long offset, byte value);

    boolean get(ValueLayout.OfBoolean layout, long offset);

    void set(ValueLayout.OfBoolean layout, long offset, boolean value);

    char get(ValueLayout.OfChar layout, long offset);

    void set(ValueLayout.OfChar layout, long offset, char value);

    short get(ValueLayout.OfShort layout, long offset);

    void set(ValueLayout.OfShort layout, long offset, short value);

    int get(ValueLayout.OfInt layout, long offset);

    void set(ValueLayout.OfInt layout, long offset, int value);

    float get(ValueLayout.OfFloat layout, long offset);

    void set(ValueLayout.OfFloat layout, long offset, float value);

    long get(ValueLayout.OfLong layout, long offset);

    void set(ValueLayout.OfLong layout, long offset, long value);

    double get(ValueLayout.OfDouble layout, long offset);

    void set(ValueLayout.OfDouble layout, long offset, double value);

    MemorySegment get(AddressLayout layout, long offset);

    void set(AddressLayout layout, long offset, MemorySegment value);

    byte getAtIndex(ValueLayout.OfByte layout, long index);

    boolean getAtIndex(ValueLayout.OfBoolean layout, long index);

    char getAtIndex(ValueLayout.OfChar layout, long index);

    void setAtIndex(ValueLayout.OfChar layout, long index, char value);

    short getAtIndex(ValueLayout.OfShort layout, long index);

    void setAtIndex(ValueLayout.OfByte layout, long index, byte value);

    void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value);

    void setAtIndex(ValueLayout.OfShort layout, long index, short value);

    int getAtIndex(ValueLayout.OfInt layout, long index);

    void setAtIndex(ValueLayout.OfInt layout, long index, int value);

    float getAtIndex(ValueLayout.OfFloat layout, long index);

    void setAtIndex(ValueLayout.OfFloat layout, long index, float value);

    long getAtIndex(ValueLayout.OfLong layout, long index);

    void setAtIndex(ValueLayout.OfLong layout, long index, long value);

    double getAtIndex(ValueLayout.OfDouble layout, long index);

    void setAtIndex(ValueLayout.OfDouble layout, long index, double value);

    MemorySegment getAtIndex(AddressLayout layout, long index);

    void setAtIndex(AddressLayout layout, long index, MemorySegment value);

    @Override
    boolean equals(Object that);

    @Override
    int hashCode();

    static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
                     Object dstArray, int dstIndex, int elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    static void copy(Object srcArray, int srcIndex,
                     MemorySegment dstSegment, ValueLayout dstLayout,
                     long dstOffset, int elementCount) {
        throw new UnsupportedOperationException("Stub!");
    }

    static long mismatch(MemorySegment srcSegment, long srcFromOffset, long srcToOffset,
                         MemorySegment dstSegment, long dstFromOffset, long dstToOffset) {
        throw new UnsupportedOperationException("Stub!");
    }

    interface Scope {
        boolean isAlive();

        @Override
        boolean equals(Object that);

        @Override
        int hashCode();
    }
}

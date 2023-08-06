package java.lang.foreign;

public interface SequenceLayout extends MemoryLayout {

    MemoryLayout elementLayout();

    long elementCount();

    SequenceLayout withElementCount(long elementCount);

    SequenceLayout reshape(long... elementCounts);

    SequenceLayout flatten();

    @Override
    SequenceLayout withName(String name);

    @Override
    MemoryLayout withoutName();

    SequenceLayout withByteAlignment(long byteAlignment);
}

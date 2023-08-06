package java.lang.foreign;

public interface PaddingLayout extends MemoryLayout {

    @Override
    PaddingLayout withName(String name);

    @Override
    PaddingLayout withoutName();

    PaddingLayout withByteAlignment(long byteAlignment);
}

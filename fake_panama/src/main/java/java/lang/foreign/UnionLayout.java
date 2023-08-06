package java.lang.foreign;

public interface UnionLayout extends GroupLayout {

    @Override
    UnionLayout withName(String name);

    @Override
    UnionLayout withoutName();

    @Override
    UnionLayout withByteAlignment(long byteAlignment);
}

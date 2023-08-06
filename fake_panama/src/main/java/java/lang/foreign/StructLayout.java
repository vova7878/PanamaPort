package java.lang.foreign;

public interface StructLayout extends GroupLayout {

    @Override
    StructLayout withName(String name);

    @Override
    StructLayout withoutName();

    @Override
    StructLayout withByteAlignment(long byteAlignment);
}

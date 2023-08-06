package java.lang.foreign;

import java.util.List;

public interface GroupLayout extends MemoryLayout {

    List<MemoryLayout> memberLayouts();

    @Override
    GroupLayout withName(String name);

    @Override
    GroupLayout withoutName();

    @Override
    GroupLayout withByteAlignment(long byteAlignment);
}

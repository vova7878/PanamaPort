package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.Optional;

public interface AddressLayout extends ValueLayout {

    @Override
    AddressLayout withName(String name);

    @Override
    AddressLayout withoutName();

    @Override
    AddressLayout withByteAlignment(long byteAlignment);

    @Override
    AddressLayout withOrder(ByteOrder order);

    AddressLayout withTargetLayout(MemoryLayout layout);

    AddressLayout withoutTargetLayout();

    Optional<MemoryLayout> targetLayout();
}

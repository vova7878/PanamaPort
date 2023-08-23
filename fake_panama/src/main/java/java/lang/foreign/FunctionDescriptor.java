package java.lang.foreign;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;


public interface FunctionDescriptor {

    Optional<MemoryLayout> returnLayout();

    List<MemoryLayout> argumentLayouts();

    FunctionDescriptor appendArgumentLayouts(MemoryLayout... addedLayouts);

    FunctionDescriptor insertArgumentLayouts(int index, MemoryLayout... addedLayouts);

    FunctionDescriptor changeReturnLayout(MemoryLayout newReturn);

    FunctionDescriptor dropReturnLayout();

    MethodType toMethodType();

    static FunctionDescriptor of(MemoryLayout resLayout, MemoryLayout... argLayouts) {
        throw new UnsupportedOperationException("Stub!");
    }

    static FunctionDescriptor ofVoid(MemoryLayout... argLayouts) {
        throw new UnsupportedOperationException("Stub!");
    }
}

package java.lang.foreign;

import java.lang.invoke.MethodHandle;

final class _AndroidLinkerImpl implements Linker {
    public static final _AndroidLinkerImpl INSTANCE = new _AndroidLinkerImpl();

    private _AndroidLinkerImpl() {
    }

    @Override
    public MethodHandle downcallHandle(
            MemorySegment address, FunctionDescriptor function, Option... options) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public MethodHandle downcallHandle(FunctionDescriptor function, Option... options) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function,
                                    Arena arena, Option... options) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet");
    }
}

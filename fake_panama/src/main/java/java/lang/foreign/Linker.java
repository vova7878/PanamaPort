package java.lang.foreign;

import java.lang.invoke.MethodHandle;

public interface Linker {
    static Linker nativeLinker() {
        throw new UnsupportedOperationException("Stub!");
    }

    MethodHandle downcallHandle(MemorySegment address, FunctionDescriptor function, Option... options);

    MethodHandle downcallHandle(FunctionDescriptor function, Option... options);

    MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena, Linker.Option... options);

    SymbolLookup defaultLookup();

    interface Option {
        static Option firstVariadicArg(int index) {
            throw new UnsupportedOperationException("Stub!");
        }

        static Option captureCallState(String... capturedState) {
            throw new UnsupportedOperationException("Stub!");
        }

        static StructLayout captureStateLayout() {
            throw new UnsupportedOperationException("Stub!");
        }

        static Option isTrivial() {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}

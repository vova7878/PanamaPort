package com.v7878.foreign;

import com.v7878.unsafe.foreign.RawNativeLibraries;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Optional;

sealed abstract class _AbstractAndroidLinker implements Linker {
    static final class Impl extends _AbstractAndroidLinker {
        @Override
        public MethodHandle downcallHandle(MemorySegment address, FunctionDescriptor function, Option... options) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }

        @Override
        public MethodHandle downcallHandle(FunctionDescriptor function, Option... options) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }

        @Override
        public MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena, Option... options) {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }

        @Override
        public Map<String, MemoryLayout> canonicalLayouts() {
            //TODO
            throw new UnsupportedOperationException("Not supported yet");
        }
    }

    public static Linker getSystemLinker() {
        return new Impl();
    }

    @Override
    public SymbolLookup defaultLookup() {
        return name -> {
            long tmp = RawNativeLibraries.dlsym(RawNativeLibraries.RTLD_DEFAULT, name);
            return Optional.ofNullable(tmp == 0 ? null : MemorySegment.ofAddress(tmp));
        };
    }
}

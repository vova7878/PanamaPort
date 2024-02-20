package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_CHAR;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.RawNativeLibraries;

import java.lang.invoke.MethodHandle;
import java.util.Map;

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
    }

    public static Linker getSystemLinker() {
        return new Impl();
    }

    @Override
    public SymbolLookup defaultLookup() {
        return JavaForeignAccess.libraryLookup(RawNativeLibraries.DEFAULT, Arena.global());
    }

    @Override
    public Map<String, MemoryLayout> canonicalLayouts() {
        class Holder {
            static final Map<String, MemoryLayout> CANONICAL_LAYOUTS;

            static {
                MemoryLayout word = IS64BIT ? JAVA_LONG : JAVA_INT;

                CANONICAL_LAYOUTS = Map.ofEntries(
                        // specified canonical layouts
                        Map.entry("bool", JAVA_BOOLEAN),
                        Map.entry("char", JAVA_BYTE),
                        Map.entry("short", JAVA_SHORT),
                        Map.entry("int", JAVA_INT),
                        Map.entry("float", JAVA_FLOAT),
                        Map.entry("long", word),
                        Map.entry("long long", JAVA_LONG),
                        Map.entry("double", JAVA_DOUBLE),
                        Map.entry("void*", ADDRESS),
                        Map.entry("size_t", word),
                        //TODO?: Map.entry("wchar_t", ???),

                        // unspecified size-dependent layouts
                        Map.entry("int8_t", JAVA_BYTE),
                        Map.entry("int16_t", JAVA_SHORT),
                        Map.entry("int32_t", JAVA_INT),
                        Map.entry("int64_t", JAVA_LONG),
                        Map.entry("intptr_t", word),

                        // unspecified JNI layouts
                        Map.entry("jboolean", JAVA_BOOLEAN),
                        Map.entry("jchar", JAVA_CHAR),
                        Map.entry("jbyte", JAVA_BYTE),
                        Map.entry("jshort", JAVA_SHORT),
                        Map.entry("jint", JAVA_INT),
                        Map.entry("jlong", JAVA_LONG),
                        Map.entry("jfloat", JAVA_FLOAT),
                        Map.entry("jdouble", JAVA_DOUBLE)
                );
            }
        }

        return Holder.CANONICAL_LAYOUTS;
    }
}

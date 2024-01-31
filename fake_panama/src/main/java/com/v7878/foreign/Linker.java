/*
 * Copyright (c) 2024 Vladimir Kozelkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.v7878.foreign;

import java.lang.invoke.MethodHandle;
import java.util.Map;

public interface Linker {
    static Linker nativeLinker() {
        throw new UnsupportedOperationException("Stub!");
    }

    MethodHandle downcallHandle(MemorySegment address,
                                FunctionDescriptor function,
                                Option... options);

    MethodHandle downcallHandle(FunctionDescriptor function, Option... options);

    MemorySegment upcallStub(MethodHandle target,
                             FunctionDescriptor function,
                             Arena arena,
                             Linker.Option... options);

    SymbolLookup defaultLookup();

    Map<String, MemoryLayout> canonicalLayouts();

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

        static Option critical(boolean allowHeapAccess) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}

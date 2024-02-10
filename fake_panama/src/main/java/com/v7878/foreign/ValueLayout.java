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

import com.v7878.invoke.VarHandle;

import java.nio.ByteOrder;
import java.util.function.Supplier;

public interface ValueLayout extends MemoryLayout {

    ByteOrder order();

    ValueLayout withOrder(ByteOrder order);

    @Override
    ValueLayout withoutName();

    Class<?> carrier();

    @Override
    ValueLayout withName(String name);

    @Override
    ValueLayout withByteAlignment(long byteAlignment);

    VarHandle varHandle();

    interface OfBoolean extends ValueLayout {

        @Override
        OfBoolean withName(String name);

        @Override
        OfBoolean withoutName();

        @Override
        OfBoolean withByteAlignment(long byteAlignment);

        @Override
        OfBoolean withOrder(ByteOrder order);
    }

    interface OfByte extends ValueLayout {

        @Override
        OfByte withName(String name);

        @Override
        OfByte withoutName();

        @Override
        OfByte withByteAlignment(long byteAlignment);

        @Override
        OfByte withOrder(ByteOrder order);
    }

    interface OfChar extends ValueLayout {

        @Override
        OfChar withName(String name);

        @Override
        OfChar withoutName();

        @Override
        OfChar withByteAlignment(long byteAlignment);

        @Override
        OfChar withOrder(ByteOrder order);
    }

    interface OfShort extends ValueLayout {

        @Override
        OfShort withName(String name);

        @Override
        OfShort withoutName();

        @Override
        OfShort withByteAlignment(long byteAlignment);

        @Override
        OfShort withOrder(ByteOrder order);

    }

    interface OfInt extends ValueLayout {

        @Override
        OfInt withName(String name);

        @Override
        OfInt withoutName();

        @Override
        OfInt withByteAlignment(long byteAlignment);

        @Override
        OfInt withOrder(ByteOrder order);
    }

    interface OfFloat extends ValueLayout {

        @Override
        OfFloat withName(String name);

        @Override
        OfFloat withoutName();

        @Override
        OfFloat withByteAlignment(long byteAlignment);

        @Override
        OfFloat withOrder(ByteOrder order);
    }

    interface OfLong extends ValueLayout {

        @Override
        OfLong withName(String name);

        @Override
        OfLong withoutName();

        @Override
        OfLong withByteAlignment(long byteAlignment);

        @Override
        OfLong withOrder(ByteOrder order);
    }

    interface OfDouble extends ValueLayout {

        @Override
        OfDouble withName(String name);

        @Override
        OfDouble withoutName();

        @Override
        OfDouble withByteAlignment(long byteAlignment);

        @Override
        OfDouble withOrder(ByteOrder order);
    }

    AddressLayout ADDRESS = ((Supplier<AddressLayout>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfByte JAVA_BYTE = ((Supplier<OfByte>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfBoolean JAVA_BOOLEAN = ((Supplier<OfBoolean>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfChar JAVA_CHAR = ((Supplier<OfChar>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfShort JAVA_SHORT = ((Supplier<OfShort>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfInt JAVA_INT = ((Supplier<OfInt>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfLong JAVA_LONG = ((Supplier<OfLong>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfFloat JAVA_FLOAT = ((Supplier<OfFloat>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();
    OfDouble JAVA_DOUBLE = ((Supplier<OfDouble>) () -> {
        throw new UnsupportedOperationException("Stub!");
    }).get();

    AddressLayout ADDRESS_UNALIGNED = ADDRESS.withByteAlignment(1);
    OfChar JAVA_CHAR_UNALIGNED = JAVA_CHAR.withByteAlignment(1);
    OfShort JAVA_SHORT_UNALIGNED = JAVA_SHORT.withByteAlignment(1);
    OfInt JAVA_INT_UNALIGNED = JAVA_INT.withByteAlignment(1);
    OfLong JAVA_LONG_UNALIGNED = JAVA_LONG.withByteAlignment(1);
    OfFloat JAVA_FLOAT_UNALIGNED = JAVA_FLOAT.withByteAlignment(1);
    OfDouble JAVA_DOUBLE_UNALIGNED = JAVA_DOUBLE.withByteAlignment(1);
}

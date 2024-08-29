package com.v7878.unsafe.cpp_std;

import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;

import com.v7878.foreign.ValueLayout;

public class CTypes {
    public static final ValueLayout C_WCHAR_T = JAVA_INT;
    public static final OfLong C_LONG_LONG = JAVA_LONG.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofLongLong());
    public static final OfDouble C_DOUBLE = JAVA_DOUBLE.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofDouble());
}

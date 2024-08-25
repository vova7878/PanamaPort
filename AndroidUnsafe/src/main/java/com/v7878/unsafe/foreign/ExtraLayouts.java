package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.ValueLayout;

public class ExtraLayouts {
    public static final ValueLayout WORD = IS64BIT ? JAVA_LONG : JAVA_INT;
    public static final MemoryLayout JAVA_OBJECT = structLayout(JAVA_INT);
    public static final MemoryLayout JNI_OBJECT = structLayout(WORD);

    public static final ValueLayout C_WCHAR_T = JAVA_INT;
    public static final OfLong C_LONG_LONG = JAVA_LONG.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofLongLong());
    public static final OfDouble C_DOUBLE = JAVA_DOUBLE.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofDouble());
}

package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.paddingLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.ValueLayout;

public class ExtraLayouts {
    public static final ValueLayout C_LONG_LONG = JAVA_LONG.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofLongLong());
    public static final ValueLayout C_DOUBLE = JAVA_DOUBLE.withByteAlignment(
            CURRENT_INSTRUCTION_SET.alignofDouble());

    public static final ValueLayout WORD = IS64BIT ? C_LONG_LONG : JAVA_INT;
    public static final MemoryLayout JAVA_OBJECT = structLayout(JAVA_INT);
    public static final MemoryLayout JNI_OBJECT = structLayout(WORD);

    public static class std {
        public static final MemoryLayout string = structLayout(paddingLayout(ADDRESS_SIZE * 3L));
        public static final MemoryLayout shared_ptr = structLayout(paddingLayout(ADDRESS_SIZE * 2L));
        public static final MemoryLayout unique_ptr = structLayout(ADDRESS);
    }
}

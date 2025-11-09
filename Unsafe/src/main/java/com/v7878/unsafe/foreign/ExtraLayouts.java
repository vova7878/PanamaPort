package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.ValueLayout;

public class ExtraLayouts {
    public static final ValueLayout WORD = IS64BIT ? JAVA_LONG : JAVA_INT;
    public static final MemoryLayout JAVA_OBJECT = structLayout(JAVA_INT);
    public static final MemoryLayout JNI_OBJECT = structLayout(WORD);
}

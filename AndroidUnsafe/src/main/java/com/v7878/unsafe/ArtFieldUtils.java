package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.Reflection.getArtField;
import static com.v7878.unsafe.Utils.nothrows_run;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.paddedStructLayout;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings("Since15")
public class ArtFieldUtils {

    private static final GroupLayout art_field_layout = paddedStructLayout(
            JAVA_INT /*TODO: JAVA_OBJECT*/.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("field_dex_idx_"),
            JAVA_INT.withName("offset_")
    );

    public static final GroupLayout ARTFIELD_LAYOUT = nothrows_run(() -> {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 34) {
            return art_field_layout;
        } else {
            throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    });

    public static MemorySegment getArtFieldSegment(Field f) {
        return MemorySegment.ofAddress(getArtField(f)).reinterpret(ARTFIELD_LAYOUT.byteSize());
    }

    private static final long ACCESS_FLAGS_OFFSET = ARTFIELD_LAYOUT
            .byteOffset(groupElement("access_flags_"));

    public static int getFieldFlags(Field f) {
        return getIntN(getArtField(f) + ACCESS_FLAGS_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setFieldFlags(Field f, int flags) {
        putIntN(getArtField(f) + ACCESS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeFieldFlags(Field f, int remove_flags, int add_flags) {
        int flags = getFieldFlags(f) & ~remove_flags;
        setFieldFlags(f, flags | add_flags);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeFieldPublic(Field f) {
        changeFieldFlags(f, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE, Modifier.PUBLIC);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeFieldNonFinal(Field f) {
        changeFieldFlags(f, Modifier.FINAL, 0);
    }
}

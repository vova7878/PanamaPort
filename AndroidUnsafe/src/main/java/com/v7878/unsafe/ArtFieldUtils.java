package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Reflection.ART_FIELD_SIZE;
import static com.v7878.unsafe.Reflection.getArtField;
import static com.v7878.unsafe.Utils.check;
import static com.v7878.unsafe.Utils.unsupportedSDK;
import static com.v7878.unsafe.foreign.ExtraLayouts.JAVA_OBJECT;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public class ArtFieldUtils {
    private static final GroupLayout art_field_layout = paddedStructLayout(
            JAVA_OBJECT.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("field_dex_idx_"),
            JAVA_INT.withName("offset_")
    );

    @ApiSensitive
    public static final GroupLayout ARTFIELD_LAYOUT;

    static {
        if (ART_SDK_INT >= 26 && ART_SDK_INT <= 36) {
            ARTFIELD_LAYOUT = art_field_layout;
        } else {
            throw unsupportedSDK(ART_SDK_INT);
        }
        check(ARTFIELD_LAYOUT.byteSize() == ART_FIELD_SIZE, AssertionError::new);
    }

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
    public static void changeFieldFlags(Field f, IntUnaryOperator filter) {
        Objects.requireNonNull(filter);
        setFieldFlags(f, filter.applyAsInt(getFieldFlags(f)));
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeFieldPublic(Field f) {
        changeFieldFlags(f, Modifier.PROTECTED | Modifier.PRIVATE, Modifier.PUBLIC);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeFieldNonFinal(Field f) {
        changeFieldFlags(f, Modifier.FINAL, 0);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeFieldPublicApi(Field f) {
        changeFieldFlags(f, ArtModifiers::makePublicApi);
    }
}

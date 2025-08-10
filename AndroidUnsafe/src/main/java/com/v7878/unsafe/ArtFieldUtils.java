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
import static com.v7878.unsafe.Utils.dcheck;
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
        dcheck(ARTFIELD_LAYOUT.byteSize() == ART_FIELD_SIZE, AssertionError::new);
    }

    public static MemorySegment getArtFieldSegment(Field f) {
        return MemorySegment.ofAddress(getArtField(f)).reinterpret(ARTFIELD_LAYOUT.byteSize());
    }

    private static final long ACCESS_FLAGS_OFFSET = ARTFIELD_LAYOUT
            .byteOffset(groupElement("access_flags_"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static int getFieldFlags(long art_field) {
        return getIntN(art_field + ACCESS_FLAGS_OFFSET);
    }

    public static int getFieldFlags(Field f) {
        return getFieldFlags(getArtField(f));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setFieldFlags(long art_field, int flags) {
        putIntN(art_field + ACCESS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setFieldFlags(Field f, int flags) {
        setFieldFlags(getArtField(f), flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeFieldFlags(long art_field, int remove_flags, int add_flags) {
        int old_flags = getFieldFlags(art_field);
        int new_flags = (old_flags & ~remove_flags) | add_flags;
        if (new_flags != old_flags) {
            setFieldFlags(art_field, new_flags);
            fullFence();
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeFieldFlags(Field f, int remove_flags, int add_flags) {
        changeFieldFlags(getArtField(f), remove_flags, add_flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeFieldFlags(long art_field, IntUnaryOperator filter) {
        Objects.requireNonNull(filter);
        int old_flags = getFieldFlags(art_field);
        int new_flags = filter.applyAsInt(old_flags);
        if (new_flags != old_flags) {
            setFieldFlags(art_field, new_flags);
            fullFence();
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeFieldFlags(Field f, IntUnaryOperator filter) {
        changeFieldFlags(getArtField(f), filter);
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

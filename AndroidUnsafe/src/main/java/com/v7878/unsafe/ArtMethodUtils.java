package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemoryLayout.unionLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getWordN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.AndroidUnsafe.putWordN;
import static com.v7878.unsafe.Reflection.getArtMethod;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;

public class ArtMethodUtils {
    private static final GroupLayout art_method_14_12_layout = paddedStructLayout(
            JAVA_INT /*TODO: JAVA_OBJECT*/.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("dex_method_index_"),
            JAVA_SHORT.withName("method_index_"),
            unionLayout(
                    JAVA_SHORT.withName("hotness_count_"),
                    JAVA_SHORT.withName("imt_index_")
            ),
            structLayout(
                    ADDRESS.withName("data_"),
                    ADDRESS.withName("entry_point_from_quick_compiled_code_")
            ).withName("ptr_sized_fields_")
    );

    private static final GroupLayout art_method_11_10_layout = paddedStructLayout(
            JAVA_INT /*TODO: JAVA_OBJECT*/.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("dex_code_item_offset_"),
            JAVA_INT.withName("dex_method_index_"),
            JAVA_SHORT.withName("method_index_"),
            unionLayout(
                    JAVA_SHORT.withName("hotness_count_"),
                    JAVA_SHORT.withName("imt_index_")
            ),
            structLayout(
                    ADDRESS.withName("data_"),
                    ADDRESS.withName("entry_point_from_quick_compiled_code_")
            ).withName("ptr_sized_fields_")
    );

    private static final GroupLayout art_method_9_layout = paddedStructLayout(
            JAVA_INT /*TODO: JAVA_OBJECT*/.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("dex_code_item_offset_"),
            JAVA_INT.withName("dex_method_index_"),
            JAVA_SHORT.withName("method_index_"),
            JAVA_SHORT.withName("hotness_count_"),
            structLayout(
                    ADDRESS.withName("data_"),
                    ADDRESS.withName("entry_point_from_quick_compiled_code_")
            ).withName("ptr_sized_fields_")
    );

    private static final GroupLayout art_method_8xx_layout = paddedStructLayout(
            JAVA_INT /*TODO: JAVA_OBJECT*/.withName("declaring_class_"),
            JAVA_INT.withName("access_flags_"),
            JAVA_INT.withName("dex_code_item_offset_"),
            JAVA_INT.withName("dex_method_index_"),
            JAVA_SHORT.withName("method_index_"),
            JAVA_SHORT.withName("hotness_count_"),
            structLayout(
                    // ArtMethod** for oreo
                    // mirror::MethodDexCacheType for oreo mr 1
                    ADDRESS.withName("dex_cache_resolved_methods_"),
                    ADDRESS.withName("data_"),
                    ADDRESS.withName("entry_point_from_quick_compiled_code_")
            ).withName("ptr_sized_fields_")
    );

    public static final GroupLayout ARTMETHOD_LAYOUT = nothrows_run(() -> switch (CORRECT_SDK_INT) {
        case 34, // android 14
                33, // android 13
                32, // android 12L
                31  // android 12
                -> art_method_14_12_layout;
        case 30,    // android 11
                29  // android 10
                -> art_method_11_10_layout;
        case 28     // android 9
                -> art_method_9_layout;
        case 27,    // android 8.1
                26  // android 8
                -> art_method_8xx_layout;
        default -> throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    });

    public static MemorySegment getArtMethodSegment(Executable ex) {
        return MemorySegment.ofAddress(getArtMethod(ex)).reinterpret(ARTMETHOD_LAYOUT.byteSize());
    }

    private static final long DATA_OFFSET = ARTMETHOD_LAYOUT.byteOffset(
            groupElement("ptr_sized_fields_"), groupElement("data_"));

    public static long getExecutableData(long art_method) {
        return getWordN(art_method + DATA_OFFSET);
    }

    public static long getExecutableData(Executable ex) {
        return getExecutableData(getArtMethod(ex));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableData(long art_method, long data) {
        putWordN(art_method + DATA_OFFSET, data);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableData(Executable ex, long data) {
        setExecutableData(getArtMethod(ex), data);
    }

    private static final long ACCESS_FLAGS_OFFSET = ARTMETHOD_LAYOUT
            .byteOffset(groupElement("access_flags_"));

    public static int getExecutableFlags(Executable ex) {
        return getIntN(getArtMethod(ex) + ACCESS_FLAGS_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableFlags(Executable ex, int flags) {
        putIntN(getArtMethod(ex) + ACCESS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeExecutableFlags(Executable ex, int remove_flags, int add_flags) {
        int flags = getExecutableFlags(ex) & ~remove_flags;
        setExecutableFlags(ex, flags | add_flags);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeExecutablePublicNonFinal(Executable ex) {
        changeExecutableFlags(ex, Modifier.FINAL | Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE, Modifier.PUBLIC);
    }
}

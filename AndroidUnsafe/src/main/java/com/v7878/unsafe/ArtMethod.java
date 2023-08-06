package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getWordN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.AndroidUnsafe.putWordN;
import static com.v7878.unsafe.Reflection.getArtMethod;
import static com.v7878.unsafe.Utils.nothrows_run;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.paddedStructLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemoryLayout.unionLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;

@SuppressWarnings("Since15")
public class ArtMethod {
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

    public static final GroupLayout ARTMETHOD_LAYOUT = nothrows_run(() -> {
        switch (CORRECT_SDK_INT) {
            case 34: // android 14
            case 33: // android 13
            case 32: // android 12L
            case 31: // android 12
                return art_method_14_12_layout;
            case 30: // android 11
            case 29: // android 10
                return art_method_11_10_layout;
            case 28: // android 9
                return art_method_9_layout;
            case 27: // android 8.1
            case 26: // android 8
                return art_method_8xx_layout;
            default:
                throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
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

    public enum AccessModifier {
        PUBLIC(Modifier.PUBLIC),
        PROTECTED(Modifier.PROTECTED),
        PRIVATE(Modifier.PRIVATE),
        NONE(0);

        public final int value;

        AccessModifier(int value) {
            this.value = value;
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void replaceExecutableAccessModifier(Executable ex, AccessModifier modifier) {
        final int all = AccessModifier.PUBLIC.value |
                AccessModifier.PROTECTED.value | AccessModifier.PRIVATE.value;
        int flags = getExecutableFlags(ex) & ~all;
        setExecutableFlags(ex, flags | modifier.value);
        fullFence();
    }
}

package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemoryLayout.unionLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getShortN;
import static com.v7878.unsafe.AndroidUnsafe.getWordN;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.AndroidUnsafe.putWordN;
import static com.v7878.unsafe.ArtModifiers.kAccCompileDontBother;
import static com.v7878.unsafe.ArtModifiers.kAccIntrinsic;
import static com.v7878.unsafe.ArtModifiers.kAccPreCompiled;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Reflection.ART_METHOD_SIZE;
import static com.v7878.unsafe.Reflection.getArtMethod;
import static com.v7878.unsafe.Utils.dcheck;
import static com.v7878.unsafe.Utils.unsupportedSDK;
import static com.v7878.unsafe.foreign.ExtraLayouts.JAVA_OBJECT;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public class ArtMethodUtils {
    private static final GroupLayout art_method_16_12_layout = paddedStructLayout(
            JAVA_OBJECT.withName("declaring_class_"),
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
            JAVA_OBJECT.withName("declaring_class_"),
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
            JAVA_OBJECT.withName("declaring_class_"),
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
            JAVA_OBJECT.withName("declaring_class_"),
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

    @ApiSensitive
    public static final GroupLayout ARTMETHOD_LAYOUT = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/, 33 /*android 13*/,
             32 /*android 12L*/, 31 /*android 12*/ -> art_method_16_12_layout;
        case 30 /*android 11*/, 29 /*android 10*/ -> art_method_11_10_layout;
        case 28 /*android 9*/ -> art_method_9_layout;
        case 27 /*android 8.1*/, 26 /*android 8*/ -> art_method_8xx_layout;
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    static {
        dcheck(ARTMETHOD_LAYOUT.byteSize() == ART_METHOD_SIZE, AssertionError::new);
    }

    public static MemorySegment getArtMethodSegment(Executable ex) {
        return MemorySegment.ofAddress(getArtMethod(ex)).reinterpret(ARTMETHOD_LAYOUT.byteSize());
    }

    private static final long DATA_OFFSET = ARTMETHOD_LAYOUT.byteOffset(
            groupElement("ptr_sized_fields_"), groupElement("data_"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
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

    private static final long ENTRYPOINT_OFFSET = ARTMETHOD_LAYOUT.byteOffset(
            groupElement("ptr_sized_fields_"),
            groupElement("entry_point_from_quick_compiled_code_"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static long getExecutableEntryPoint(long art_method) {
        return getWordN(art_method + ENTRYPOINT_OFFSET);
    }

    public static long getExecutableEntryPoint(Executable ex) {
        return getExecutableEntryPoint(getArtMethod(ex));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableEntryPoint(long art_method, long entry_point) {
        putWordN(art_method + ENTRYPOINT_OFFSET, entry_point);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableEntryPoint(Executable ex, long entry_point) {
        setExecutableEntryPoint(getArtMethod(ex), entry_point);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void registerNativeMethod(Method m, long data) {
        Objects.requireNonNull(m);
        if (!Modifier.isNative(m.getModifiers())) {
            throw new IllegalArgumentException("only native methods allowed");
        }
        ClassUtils.ensureClassInitialized(m.getDeclaringClass());
        setExecutableData(m, data);
    }

    // Entry within a dispatch table for this method. For static/direct methods the index is into
    // the declaringClass.directMethods, for virtual methods the vtable and for interface methods the
    // ifTable.
    private static final long DISPATCH_TABLE_INDEX_OFFSET = ARTMETHOD_LAYOUT
            .byteOffset(groupElement("method_index_"));

    public static int getDispatchTableIndex(long art_method) {
        return getShortN(art_method + DISPATCH_TABLE_INDEX_OFFSET) & 0xffff;
    }

    public static int getDispatchTableIndex(Executable ex) {
        return getDispatchTableIndex(getArtMethod(ex));
    }

    private static final long ACCESS_FLAGS_OFFSET = ARTMETHOD_LAYOUT
            .byteOffset(groupElement("access_flags_"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static int getExecutableFlags(long art_method) {
        return getIntN(art_method + ACCESS_FLAGS_OFFSET);
    }

    public static int getExecutableFlags(Executable ex) {
        return getExecutableFlags(getArtMethod(ex));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableFlags(long art_method, int flags) {
        putIntN(art_method + ACCESS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setExecutableFlags(Executable ex, int flags) {
        setExecutableFlags(getArtMethod(ex), flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeExecutableFlags(long art_method, int remove_flags, int add_flags) {
        int old_flags = getExecutableFlags(art_method);
        int new_flags = (old_flags & ~remove_flags) | add_flags;
        if (new_flags != old_flags) {
            setExecutableFlags(art_method, new_flags);
            fullFence();
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeExecutableFlags(Executable ex, int remove_flags, int add_flags) {
        changeExecutableFlags(getArtMethod(ex), remove_flags, add_flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeExecutableFlags(long art_method, IntUnaryOperator filter) {
        Objects.requireNonNull(filter);
        int old_flags = getExecutableFlags(art_method);
        int new_flags = filter.applyAsInt(old_flags);
        if (new_flags != old_flags) {
            setExecutableFlags(art_method, new_flags);
            fullFence();
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeExecutableFlags(Executable ex, IntUnaryOperator filter) {
        changeExecutableFlags(getArtMethod(ex), filter);
    }

    private static final int VISIBILITY_MASK = Modifier.PROTECTED | Modifier.PRIVATE;

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeExecutablePublic(Executable ex) {
        changeExecutableFlags(ex, flags -> {
            if (ex instanceof Method && !Modifier.isStatic(flags) && Modifier.isPrivate(flags)) {
                throw new IllegalArgumentException("Can't make direct private method virtual: " + ex);
            }
            return (flags & ~VISIBILITY_MASK) | Modifier.PUBLIC;
        });
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeMethodInheritable(Method m) {
        changeExecutableFlags(m, flags -> {
            if (Modifier.isStatic(flags) || Modifier.isPrivate(flags)) {
                throw new IllegalArgumentException("Only virtual methods are supported: " + m);
            }
            return (flags & ~(Modifier.FINAL | VISIBILITY_MASK)) | Modifier.PUBLIC;
        });
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeExecutableNonCompilable(Executable ex) {
        changeExecutableFlags(ex, flags -> {
            if ((flags & kAccIntrinsic) != 0) {
                throw new IllegalArgumentException("Intrinsic executables are not supported: " + ex);
            }
            return (flags & ~kAccPreCompiled) | kAccCompileDontBother;
        });
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeExecutablePublicApi(Executable ex) {
        changeExecutableFlags(ex, flags -> {
            if ((flags & kAccIntrinsic) != 0) {
                throw new IllegalArgumentException("Intrinsic executables are not supported: " + ex);
            }
            return ArtModifiers.makePublicApi(flags);
        });
    }
}

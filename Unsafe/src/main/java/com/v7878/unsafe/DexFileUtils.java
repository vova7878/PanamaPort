package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A11;
import static com.v7878.unsafe.ArtVersion.A12;
import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.A16;
import static com.v7878.unsafe.ArtVersion.A16p1;
import static com.v7878.unsafe.ArtVersion.A8p0;
import static com.v7878.unsafe.ArtVersion.A8p1;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Utils.unsupportedART;
import static com.v7878.unsafe.cpp_std.basic_string.string;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.DexFileAccess;
import com.v7878.unsafe.cpp_std.shared_ptr;
import com.v7878.unsafe.cpp_std.unique_ptr;

import java.nio.ByteBuffer;
import java.util.Objects;

import dalvik.system.DexFile;

public class DexFileUtils {
    private static final GroupLayout array_ref_layout = paddedStructLayout(
            ADDRESS.withName("array_"),
            WORD.withName("size_")
    );

    private static final GroupLayout dex_file_16p1_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("unused_size_"),
            array_ref_layout.withName("data_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            shared_ptr.LAYOUT.withName("container_"),
            JAVA_BYTE.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_14_16_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"), // unused_size_ for android 15, 16
            array_ref_layout.withName("data_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            shared_ptr.LAYOUT.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BYTE.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_13_11_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"),
            ADDRESS.withName("data_begin_"),
            WORD.withName("data_size_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            unique_ptr.LAYOUT.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BYTE.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_10_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"),
            ADDRESS.withName("data_begin_"),
            WORD.withName("data_size_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            unique_ptr.LAYOUT.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_INT.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_9_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"),
            ADDRESS.withName("data_begin_"),
            WORD.withName("data_size_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("oat_dex_file_"),
            unique_ptr.LAYOUT.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BOOLEAN.withName("is_platform_dex_")
    );
    private static final GroupLayout dex_file_8xx_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"),
            string.LAYOUT.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            unique_ptr.LAYOUT.withName("mem_map_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            WORD.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            WORD.withName("num_call_site_ids_"),
            ADDRESS.withName("oat_dex_file_")
    );

    @ApiSensitive
    public static final GroupLayout DEXFILE_LAYOUT = switch (ART_INDEX) {
        case A16p1 -> dex_file_16p1_layout;
        case A16, A15, A14 -> dex_file_14_16_layout;
        case A13, A12, A11 -> dex_file_13_11_layout;
        case A10 -> dex_file_10_layout;
        case A9 -> dex_file_9_layout;
        case A8p1, A8p0 -> dex_file_8xx_layout;
        default -> throw unsupportedART(ART_INDEX);
    };

    public static Object getDexCache(Class<?> clazz) {
        class Holder {
            static final long DEX_CACHE_OFFSET = Reflection.
                    instanceFieldOffset(Class.class, "dexCache");
        }
        return AndroidUnsafe.getObject(Objects.requireNonNull(clazz), Holder.DEX_CACHE_OFFSET);
    }

    public static long getDexFileStruct(Class<?> clazz) {
        class Holder {
            static final long DEX_FILE_OFFSET = Reflection.instanceFieldOffset(
                    ClassUtils.sysClass("java.lang.DexCache"), "dexFile");
        }
        Object dexCache = Objects.requireNonNull(getDexCache(clazz));
        long address = AndroidUnsafe.getLongO(dexCache, Holder.DEX_FILE_OFFSET);
        if (address == 0) {
            throw new IllegalStateException("dexFile == 0");
        }
        return address;
    }

    public static MemorySegment getDexFileStructSegment(Class<?> clazz) {
        return MemorySegment.ofAddress(getDexFileStruct(clazz))
                .reinterpret(DEXFILE_LAYOUT.byteSize());
    }

    @ApiSensitive
    public static DexFile openDexFile(ByteBuffer data) {
        if (ART_INDEX <= A9) {
            return DexFileAccess.openDexFile(data);
        }
        return DexFileAccess.openDexFile(new ByteBuffer[]{data}, null);
    }

    public static DexFile openDexFile(byte[] data) {
        return openDexFile(ByteBuffer.wrap(data));
    }

    @ApiSensitive
    public static long[] openCookie(ByteBuffer data) {
        if (ART_INDEX <= A9) {
            return (long[]) DexFileAccess.openCookie(data);
        }
        return (long[]) DexFileAccess.openCookie(new ByteBuffer[]{data}, null);
    }

    public static long[] openCookie(byte[] data) {
        return openCookie(ByteBuffer.wrap(data));
    }

    private static class Holder {
        static final long COOKIE_OFFSET = Reflection.
                instanceFieldOffset(DexFile.class, "mCookie");
        static final long INTERNAL_COOKIE_OFFSET = Reflection.
                instanceFieldOffset(DexFile.class, "mInternalCookie");
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static long[] getCookie(DexFile dex) {
        return (long[]) AndroidUnsafe.getObject(Objects.requireNonNull(dex), Holder.COOKIE_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setCookie(DexFile dex, long[] cookie) {
        AndroidUnsafe.putObject(Objects.requireNonNull(dex), Holder.COOKIE_OFFSET, cookie);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static long[] getInternalCookie(DexFile dex) {
        return (long[]) AndroidUnsafe.getObject(Objects.requireNonNull(dex), Holder.INTERNAL_COOKIE_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setInternalCookie(DexFile dex, long[] cookie) {
        AndroidUnsafe.putObject(Objects.requireNonNull(dex), Holder.INTERNAL_COOKIE_OFFSET, cookie);
    }

    @ApiSensitive
    public static void setTrusted(long dexfile_struct) {
        if (ART_INDEX < A9) {
            return;
        }
        class Holder {
            static final long offset;

            static {
                if (ART_INDEX == A9) {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("is_platform_dex_"));
                } else {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("hiddenapi_domain_"));
                }
            }
        }
        if (ART_INDEX == A9) {
            AndroidUnsafe.putBooleanN(dexfile_struct + Holder.offset, true);
            return;
        }
        final int kCorePlatform = 0;
        if (ART_INDEX == A10) {
            AndroidUnsafe.putIntN(dexfile_struct + Holder.offset, kCorePlatform);
            return;
        }
        AndroidUnsafe.putByteN(dexfile_struct + Holder.offset, (byte) kCorePlatform);
    }

    @ApiSensitive
    public static void setTrusted(DexFile dex) {
        if (ART_INDEX < A9) {
            return;
        }
        long[] cookie = getCookie(dex);
        final int start = 1;
        for (int i = start; i < cookie.length; i++) {
            setTrusted(cookie[i]);
        }
    }

    public static void setTrusted(Class<?> clazz) {
        if (ART_INDEX < A9) {
            return;
        }
        setTrusted(getDexFileStruct(clazz));
    }

    public static Class<?> loadClass(DexFile dex, String name, ClassLoader loader) {
        class Holder {
            static final ClassLoader BOOT_CLASS_LOADER =
                    ClassLoader.getSystemClassLoader().getParent();
        }
        if (loader == Holder.BOOT_CLASS_LOADER) {
            loader = null;
        }
        return DexFileAccess.defineClassNative(
                name.replace('.', '/'),
                loader, getCookie(dex), dex);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static String[] getClassNameList(Object cookie) {
        return DexFileAccess.getClassNameList(cookie);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static String[] getClassNameList(long dexfile_struct) {
        return getClassNameList(new long[]{0, dexfile_struct});
    }
}

package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Reflection.arrayCast;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredConstructor;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.cpp_std.basic_string.string;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.Reflection.ClassMirror;
import com.v7878.unsafe.cpp_std.shared_ptr;
import com.v7878.unsafe.cpp_std.unique_ptr;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Objects;

import dalvik.system.DexFile;

public class DexFileUtils {

    private static final GroupLayout array_ref_layout = paddedStructLayout(
            ADDRESS.withName("array_"),
            WORD.withName("size_")
    );
    private static final GroupLayout dex_file_14_15_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            WORD.withName("size_"), // unused_size_ for android 15
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
    public static final GroupLayout DEXFILE_LAYOUT;

    static {
        DEXFILE_LAYOUT = switch (CORRECT_SDK_INT) {
            case 35 /*android 15*/, 34 /*android 14*/ -> dex_file_14_15_layout;
            case 33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
                 30 /*android 11*/ -> dex_file_13_11_layout;
            case 29 /*android 10*/ -> dex_file_10_layout;
            case 28 /*android 9*/ -> dex_file_9_layout;
            case 27 /*android 8.1*/, 26 /*android 8*/ -> dex_file_8xx_layout;
            default -> throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        };
    }

    private static final long dexFileOffset = fieldOffset(nothrows_run(() ->
            getDeclaredField(Class.forName("java.lang.DexCache"), "dexFile")));

    public static Object getDexCache(Class<?> clazz) {
        ClassMirror[] m = arrayCast(ClassMirror.class, clazz);
        return m[0].dexCache;
    }

    public static long getDexFile(Class<?> clazz) {
        Object dexCache = Objects.requireNonNull(getDexCache(clazz));
        long address = AndroidUnsafe.getLongO(dexCache, dexFileOffset);
        if (address == 0) {
            throw new IllegalStateException("dexFile == 0");
        }
        return address;
    }

    public static MemorySegment getDexFileSegment(Class<?> clazz) {
        return MemorySegment.ofAddress(getDexFile(clazz)).reinterpret(DEXFILE_LAYOUT.byteSize());
    }

    @ApiSensitive
    public static DexFile openDexFile(ByteBuffer data) {
        class Holder {
            static final MethodHandle new_dex_file;

            static {
                Constructor<DexFile> dex_constructor;
                if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 28) {
                    dex_constructor = getDeclaredConstructor(DexFile.class, ByteBuffer.class);
                } else if (CORRECT_SDK_INT >= 29 && CORRECT_SDK_INT <= 35) {
                    Class<?> dex_path_list_elements = nothrows_run(
                            () -> Class.forName("[Ldalvik.system.DexPathList$Element;"));
                    dex_constructor = getDeclaredConstructor(DexFile.class, ByteBuffer[].class,
                            ClassLoader.class, dex_path_list_elements);
                } else {
                    throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
                }
                new_dex_file = unreflect(dex_constructor);
            }
        }

        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 28) {
            return nothrows_run(() -> (DexFile) Holder.new_dex_file.invokeExact(data));
        } else if (CORRECT_SDK_INT >= 29 && CORRECT_SDK_INT <= 35) {
            return nothrows_run(() -> (DexFile) Holder.new_dex_file.invoke(
                    new ByteBuffer[]{data}, null, null));
        } else {
            throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    }

    public static DexFile openDexFile(byte[] data) {
        return openDexFile(ByteBuffer.wrap(data));
    }

    private static final long cookieOffset = fieldOffset(nothrows_run(() ->
            getDeclaredField(DexFile.class, "mCookie")));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static long[] getCookie(DexFile dex) {
        return (long[]) AndroidUnsafe.getObject(Objects.requireNonNull(dex), cookieOffset);
    }

    @ApiSensitive
    public static void setTrusted(long dexfile) {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 27) {
            return;
        }
        class Holder {
            static final long offset;

            static {
                if (CORRECT_SDK_INT == 28) {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("is_platform_dex_"));
                } else {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("hiddenapi_domain_"));
                }
            }
        }
        if (CORRECT_SDK_INT == 28) {
            AndroidUnsafe.putBooleanN(dexfile + Holder.offset, true);
            return;
        }
        final int kCorePlatform = 0;
        if (CORRECT_SDK_INT == 29) {
            AndroidUnsafe.putIntN(dexfile + Holder.offset, kCorePlatform);
            return;
        }
        if (CORRECT_SDK_INT >= 30 && CORRECT_SDK_INT <= 35) {
            AndroidUnsafe.putByteN(dexfile + Holder.offset, (byte) kCorePlatform);
            return;
        }
        throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    }

    @ApiSensitive
    public static void setTrusted(DexFile dex) {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 27) {
            return;
        }
        if (CORRECT_SDK_INT >= 28 && CORRECT_SDK_INT <= 35) {
            long[] cookie = getCookie(dex);
            final int start = 1;
            for (int i = start; i < cookie.length; i++) {
                setTrusted(cookie[i]);
            }
            return;
        }
        throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    }

    public static Class<?> loadClass(DexFile dex, String name, ClassLoader loader) {
        class Holder {
            static final MethodHandle defineClassNative = unreflect(
                    getDeclaredMethod(DexFile.class, "defineClassNative",
                            String.class, ClassLoader.class, Object.class, DexFile.class));
        }
        //noinspection RedundantCast
        return nothrows_run(() -> (Class<?>) Holder.defineClassNative.invokeExact(
                name.replace('.', '/'), loader, (Object) getCookie(dex), dex));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static String[] getClassNameList(Object cookie) {
        class Holder {
            static final MethodHandle getClassNameList = unreflect(
                    getDeclaredMethod(DexFile.class, "getClassNameList", Object.class));
        }
        return nothrows_run(() -> (String[]) Holder.getClassNameList.invokeExact(cookie));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static String[] getClassNameList(long dexfile) {
        return getClassNameList(new long[]{0, dexfile});
    }
}

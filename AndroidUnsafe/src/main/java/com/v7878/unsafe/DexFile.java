package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Reflection.arrayCast;
import static com.v7878.unsafe.Reflection.getDeclaredConstructor;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.paddedStructLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("Since15")
public class DexFile {
    private static class std {

        static final GroupLayout string = paddedStructLayout(
                ADDRESS /*TODO: WORD*/.withName("mLength"),
                ADDRESS /*TODO: WORD*/.withName("mCapacity"),
                ADDRESS.withName("mData")
        );
        static final GroupLayout shared_ptr = paddedStructLayout(
                ADDRESS.withName("__ptr_"),
                ADDRESS.withName("__cntrl_")
        );
        static final GroupLayout unique_ptr = paddedStructLayout(
                ADDRESS.withName("__ptr_")
        );
    }

    private static final GroupLayout array_ref_layout = paddedStructLayout(
            ADDRESS.withName("array_"),
            ADDRESS /*TODO: WORD*/.withName("size_")
    );
    private static final GroupLayout dex_file_14_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            ADDRESS /*TODO: WORD*/.withName("size_"),
            array_ref_layout.withName("data_"),
            std.string.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            ADDRESS /*TODO: WORD*/.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            ADDRESS /*TODO: WORD*/.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            std.shared_ptr.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BYTE.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_13_11_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            ADDRESS /*TODO: WORD*/.withName("size_"),
            ADDRESS.withName("data_begin_"),
            ADDRESS /*TODO: WORD*/.withName("data_size_"),
            std.string.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            ADDRESS /*TODO: WORD*/.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            ADDRESS /*TODO: WORD*/.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            std.unique_ptr.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BYTE.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_10_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            ADDRESS /*TODO: WORD*/.withName("size_"),
            ADDRESS.withName("data_begin_"),
            ADDRESS /*TODO: WORD*/.withName("data_size_"),
            std.string.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            ADDRESS /*TODO: WORD*/.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            ADDRESS /*TODO: WORD*/.withName("num_call_site_ids_"),
            ADDRESS.withName("hiddenapi_class_data_"),
            ADDRESS.withName("oat_dex_file_"),
            std.unique_ptr.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_INT.withName("hiddenapi_domain_")
    );
    private static final GroupLayout dex_file_9_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            ADDRESS /*TODO: WORD*/.withName("size_"),
            ADDRESS.withName("data_begin_"),
            ADDRESS /*TODO: WORD*/.withName("data_size_"),
            std.string.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            ADDRESS /*TODO: WORD*/.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            ADDRESS /*TODO: WORD*/.withName("num_call_site_ids_"),
            ADDRESS.withName("oat_dex_file_"),
            std.unique_ptr.withName("container_"),
            JAVA_BOOLEAN.withName("is_compact_dex_"),
            JAVA_BOOLEAN.withName("is_platform_dex_")
    );
    private static final GroupLayout dex_file_8xx_layout = paddedStructLayout(
            ADDRESS.withName("__cpp_virtual_data__"),
            ADDRESS.withName("begin_"),
            ADDRESS /*TODO: WORD*/.withName("size_"),
            std.string.withName("location_"),
            JAVA_INT.withName("location_checksum_"),
            std.unique_ptr.withName("mem_map_"),
            ADDRESS.withName("header_"),
            ADDRESS.withName("string_ids_"),
            ADDRESS.withName("type_ids_"),
            ADDRESS.withName("field_ids_"),
            ADDRESS.withName("method_ids_"),
            ADDRESS.withName("proto_ids_"),
            ADDRESS.withName("class_defs_"),
            ADDRESS.withName("method_handles_"),
            ADDRESS /*TODO: WORD*/.withName("num_method_handles_"),
            ADDRESS.withName("call_site_ids_"),
            ADDRESS /*TODO: WORD*/.withName("num_call_site_ids_"),
            ADDRESS.withName("oat_dex_file_")
    );

    public static final GroupLayout DEXFILE_LAYOUT = nothrows_run(() -> {
        switch (CORRECT_SDK_INT) {
            case 34: // android 14
                return dex_file_14_layout;
            case 33: // android 13
            case 32: // android 12L
            case 31: // android 12
            case 30: // android 11
                return dex_file_13_11_layout;
            case 29: // android 10
                return dex_file_10_layout;
            case 28: // android 9
                return dex_file_9_layout;
            case 27: // android 8.1
            case 26: // android 8
                return dex_file_8xx_layout;
            default:
                throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    });

    private static final Class<?> dexCacheClass
            = nothrows_run(() -> Class.forName("java.lang.DexCache"));
    private static final Field dexFile = nothrows_run(
            () -> getDeclaredField(dexCacheClass, "dexFile"));

    public static Object getDexCache(Class<?> clazz) {
        Reflection.ClassMirror[] m = arrayCast(Reflection.ClassMirror.class, clazz);
        return m[0].dexCache;
    }

    public static long getDexFile(Class<?> clazz) {
        Object dexCache = getDexCache(clazz);
        long address = nothrows_run(() -> dexFile.getLong(dexCache));
        assert_(address != 0, () -> new IllegalStateException("dexFile == 0"));
        return address;
    }

    public static MemorySegment getDexFileSegment(Class<?> clazz) {
        return MemorySegment.ofAddress(getDexFile(clazz)).reinterpret(DEXFILE_LAYOUT.byteSize());
    }

    private static final Supplier<Constructor<DexFile>> dex_constructor = runOnce(() -> {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 28) {
            return getDeclaredConstructor(DexFile.class, ByteBuffer.class);
        }
        if (CORRECT_SDK_INT >= 29 && CORRECT_SDK_INT <= 34) {
            Class<?> dex_path_list_elements = nothrows_run(
                    () -> Class.forName("[Ldalvik.system.DexPathList$Element;"));
            return getDeclaredConstructor(DexFile.class, ByteBuffer[].class,
                    ClassLoader.class, dex_path_list_elements);
        }
        throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    });

    public static DexFile openDexFile(ByteBuffer data) {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 28) {
            return nothrows_run(() -> dex_constructor.get().newInstance(data));
        } else if (CORRECT_SDK_INT >= 29 && CORRECT_SDK_INT <= 34) {
            return nothrows_run(() -> dex_constructor.get().newInstance(
                    new ByteBuffer[]{data}, null, null));
        } else {
            throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    }

    public static DexFile openDexFile(byte[] data) {
        return openDexFile(ByteBuffer.wrap(data));
    }

    private static final Field cookie = nothrows_run(
            () -> getDeclaredField(DexFile.class, "mCookie"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static long[] getCookie(DexFile dex) {
        return (long[]) nothrows_run(() -> cookie.get(dex));
    }

    public static void setTrusted(long dexfile) {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 27) {
            return;
        }
        if (CORRECT_SDK_INT == 28) {
            final long offset = DEXFILE_LAYOUT.byteOffset(groupElement("is_platform_dex_"));
            AndroidUnsafe.putBooleanN(dexfile + offset, true);
            return;
        }
        if (CORRECT_SDK_INT >= 29 && CORRECT_SDK_INT <= 34) {
            final long offset = DEXFILE_LAYOUT.byteOffset(groupElement("hiddenapi_domain_"));
            AndroidUnsafe.putByteN(dexfile + offset, /*kCorePlatform*/ (byte) 0);
            return;
        }
        throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    }

    public static void setTrusted(DexFile dex) {
        if (CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 27) {
            return;
        }
        if (CORRECT_SDK_INT >= 28 && CORRECT_SDK_INT <= 34) {
            long[] cookie = getCookie(dex);
            final int start = 1;
            for (int i = start; i < cookie.length; i++) {
                setTrusted(cookie[i]);
            }
            return;
        }
        throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    }

    private static final Supplier<MethodHandle> loadClassBinaryName = runOnce(
            () -> unreflectDirect(getDeclaredMethod(DexFile.class, "loadClassBinaryName",
                    String.class, ClassLoader.class, List.class)));

    public static Class<?> loadClass(DexFile dex, String name, ClassLoader loader) {
        List<Throwable> suppressed = new ArrayList<>();
        Class<?> out = (Class<?>) nothrows_run(() -> loadClassBinaryName.get()
                .invoke(dex, name.replace('.', '/'), loader, suppressed));
        if (!suppressed.isEmpty()) {
            RuntimeException err = new RuntimeException();
            for (Throwable tmp : suppressed) {
                err.addSuppressed(tmp);
            }
            throw err;
        }
        return out;
    }

}

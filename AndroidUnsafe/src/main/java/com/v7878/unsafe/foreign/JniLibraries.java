package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.instanceFieldOffset;
import static com.v7878.unsafe.Utils.unsupportedSDK;
import static com.v7878.unsafe.cpp_std.basic_string.string;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.ExtraLayouts.JNI_OBJECT;
import static com.v7878.unsafe.foreign.LibArt.ART;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.cpp_std.map;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.misc.Math;

import java.util.Objects;
import java.util.function.Function;

public class JniLibraries {
    public static final GroupLayout SHARED_LIBRARY = paddedStructLayout(
            string.LAYOUT.withName("path_"),
            ADDRESS.withName("handle_"),
            JAVA_BOOLEAN.withName("needs_native_bridge_"),
            JNI_OBJECT.withName("class_loader_"),
            ADDRESS.withName("class_loader_allocator_")
    );
    private static final AddressLayout SHARED_LIBRARY_PTR = ADDRESS.withTargetLayout(SHARED_LIBRARY);

    private static final map LIBS_MAP = new map(string.LAYOUT, ADDRESS);

    // TODO: there must be a better way to do this
    @ApiSensitive
    private static final long libraries_offset = switch (ART_SDK_INT) {
        case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/ -> {
            long tmp = ADDRESS_SIZE * 4L;
            tmp += 3;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 3L;

            //mem_map
            tmp += ADDRESS_SIZE * 7L;
            tmp += 6;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE;

            tmp += ADDRESS_SIZE;
            tmp += 4;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 3L;
            yield tmp;
        }
        case 33 /*android 13*/, 32 /*android 12L*/, 31 /*android 12*/,
             30 /*android 11*/, 29 /*android 10*/ -> {
            long tmp = ADDRESS_SIZE * 4L;
            tmp += 3;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 3L;

            tmp += 4;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);

            //mem_map
            tmp += ADDRESS_SIZE * 7L;
            tmp += 6;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE;

            tmp += ADDRESS_SIZE;
            tmp += 4;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 2L;
            tmp += 8;
            yield tmp;
        }
        case 28 /*android 9*/, 27 /*android 8.1*/, 26 /*android 8*/ -> {
            long tmp = ADDRESS_SIZE * 4L;
            tmp += 3;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 3L;

            tmp += 4;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 2L;
            tmp += 4;
            tmp = Math.roundUpL(tmp, ADDRESS_SIZE);
            tmp += ADDRESS_SIZE * 2L;
            tmp += 8;
            yield tmp;
        }
        default -> throw unsupportedSDK(ART_SDK_INT);
    };

    private static MemorySegment getLibraries() {
        class Holder {
            static final MemorySegment libraries;

            static {
                libraries = JNIUtils.getJavaVM().reinterpret(Long.MAX_VALUE)
                        .get(ADDRESS, libraries_offset).reinterpret(LIBS_MAP.LAYOUT.byteSize());
            }
        }
        return Holder.libraries;
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "_ZN3art5Mutex13ExclusiveLockEPNS_6ThreadE")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void ExclusiveLock(long mutex, long thread);

        @LibrarySymbol(name = "_ZN3art5Mutex15ExclusiveUnlockEPNS_6ThreadE")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void ExclusiveUnlock(long mutex, long thread);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, ART));
    }

    @SuppressWarnings("SameParameterValue")
    private static void MutexLock(long mutex, long thread) {
        Native.INSTANCE.ExclusiveLock(mutex, thread);
    }

    @SuppressWarnings("SameParameterValue")
    private static void MutexUnlock(long mutex, long thread) {
        Native.INSTANCE.ExclusiveUnlock(mutex, thread);
    }

    private static final long LIBRARIES_LOCK =
            ART.findOrThrow("_ZN3art5Locks19jni_libraries_lock_E").
                    reinterpret(ADDRESS_SIZE).get(ADDRESS, 0).nativeAddress();

    public static void forEachLibraries(Function<MemorySegment, Boolean> consumer) {
        Objects.requireNonNull(consumer);
        MemorySegment libs = getLibraries();

        long self = JNIUtils.getRawNativePeer(Thread.currentThread());
        MutexLock(LIBRARIES_LOCK, self);
        try {
            var map = LIBS_MAP.new impl(libs);
            var end = map.end();

            for (var iter = map.begin(); !iter.equals(end); iter = iter.next()) {
                if (consumer.apply(iter.second().get(SHARED_LIBRARY_PTR, 0))) {
                    break;
                }
            }
        } finally {
            MutexUnlock(LIBRARIES_LOCK, self);
        }
    }

    private static final long NNB_OFFSET = SHARED_LIBRARY
            .byteOffset(groupElement("needs_native_bridge_"));
    private static final long CLA_OFFSET = SHARED_LIBRARY
            .byteOffset(groupElement("class_loader_allocator_"));
    private static final long HANDLE_OFFSET = SHARED_LIBRARY
            .byteOffset(groupElement("handle_"));

    private static final long ALLOCATOR_OFFSET = instanceFieldOffset(
            getDeclaredField(ClassLoader.class, "allocator"));

    public static void forEachHandlesInClassLoader(
            ClassLoader loader, Function<MemorySegment, Boolean> consumer) {
        Objects.requireNonNull(loader);
        Objects.requireNonNull(consumer);

        long allocator = getLongO(loader, ALLOCATOR_OFFSET);
        forEachLibraries(library -> {
            if (!library.get(JAVA_BOOLEAN, NNB_OFFSET) &&
                    library.get(ADDRESS, CLA_OFFSET).nativeAddress() == allocator) {
                return consumer.apply(library.get(ADDRESS, HANDLE_OFFSET));
            }
            // skip
            return false;
        });
    }
}

package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.paddedStructLayout;
import static com.v7878.foreign.MemoryLayout.paddingLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.instanceFieldOffset;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;
import static com.v7878.unsafe.foreign.ExtraLayouts.JNI_OBJECT;
import static com.v7878.unsafe.foreign.LibArt.ART;

import androidx.annotation.Keep;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.misc.Math;
import com.v7878.unsafe.JNIUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import dalvik.annotation.optimization.CriticalNative;

public class JniLibraries {

    public static final GroupLayout SHARED_LIBRARY = paddedStructLayout(
            ExtraLayouts.std.string.withName("path_"),
            ADDRESS.withName("handle_"),
            JAVA_BOOLEAN.withName("needs_native_bridge_"),
            JNI_OBJECT.withName("class_loader_"),
            ADDRESS.withName("class_loader_allocator_")
    );

    private static MemorySegment unbound(MemorySegment ptr) {
        return ptr.reinterpret(Long.MAX_VALUE);
    }

    private static MemorySegment begin(MemorySegment map) {
        return unbound(map).get(ADDRESS, 0);
    }

    private static MemorySegment end(MemorySegment map) {
        return unbound(map).asSlice(ADDRESS_SIZE, 0);
    }

    private static MemorySegment getValue(MemorySegment iterator) {
        return unbound(iterator).get(ADDRESS, IS64BIT ? 0x38 : 0x1c);
    }

    private static final AddressLayout PTR =
            ADDRESS.withTargetLayout(paddingLayout(Long.MAX_VALUE));

    private static MemorySegment next(MemorySegment iterator) {
        MemorySegment tmp1 = unbound(iterator);
        MemorySegment tmp2 = tmp1.get(PTR, ADDRESS_SIZE);
        MemorySegment tmp3;

        if (tmp2.equals(MemorySegment.NULL)) {
            while (true) {
                tmp3 = tmp1.get(PTR, ADDRESS_SIZE * 2L);
                if (tmp3.get(ADDRESS, 0).equals(tmp1)) {
                    break;
                }
                tmp1 = tmp3;
            }
        } else {
            do {
                tmp3 = tmp2;
                tmp2 = tmp2.get(PTR, 0);
            } while (!tmp2.equals(MemorySegment.NULL));
        }
        return tmp3.reinterpret(0);
    }

    private static final long libraries_offset = nothrows_run(() -> switch (CORRECT_SDK_INT) {
        case 34 /*android 14*/ -> {
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
        default -> throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
    });

    private static final Supplier<MemorySegment> libraries = runOnce(() ->
            unbound(JNIUtils.getJavaVMPtr()).get(ADDRESS, libraries_offset));

    private static MemorySegment getLibraries() {
        return libraries.get();
    }

    @Keep
    @CriticalNative
    private static native void MutexLock32(int mutex, int thread);

    @Keep
    @CriticalNative
    private static native void MutexLock64(long mutex, long thread);

    @SuppressWarnings("SameParameterValue")
    private static void MutexLock(long mutex, long thread) {
        if (IS64BIT) {
            MutexLock64(mutex, thread);
        } else {
            MutexLock32((int) mutex, (int) thread);
        }
    }

    @Keep
    @CriticalNative
    private static native void MutexUnlock32(int mutex, int thread);

    @Keep
    @CriticalNative
    private static native void MutexUnlock64(long mutex, long thread);

    @SuppressWarnings("SameParameterValue")
    private static void MutexUnlock(long mutex, long thread) {
        if (IS64BIT) {
            MutexUnlock64(mutex, thread);
        } else {
            MutexUnlock32((int) mutex, (int) thread);
        }
    }

    static {
        String suffix = IS64BIT ? "64" : "32";
        Class<?> word = IS64BIT ? long.class : int.class;

        Method symbol = getDeclaredMethod(JniLibraries.class,
                "MutexLock" + suffix, word, word);
        registerNativeMethod(symbol, ART.find(
                "_ZN3art5Mutex13ExclusiveLockEPNS_6ThreadE").get().address());

        symbol = getDeclaredMethod(JniLibraries.class,
                "MutexUnlock" + suffix, word, word);
        registerNativeMethod(symbol, ART.find(
                "_ZN3art5Mutex15ExclusiveUnlockEPNS_6ThreadE").get().address());
    }

    private static final long LIBRARIES_LOCK =
            ART.find("_ZN3art5Locks19jni_libraries_lock_E")
                    .get().reinterpret(ADDRESS_SIZE).get(ADDRESS, 0).address();

    public static void forEachLibraries(Function<MemorySegment, Boolean> consumer) {
        Objects.requireNonNull(consumer);
        MemorySegment libs = getLibraries();

        long self = JNIUtils.getRawNativePeer(Thread.currentThread());
        MutexLock(LIBRARIES_LOCK, self);
        try {
            MemorySegment end = end(libs);

            for (var iter = begin(libs); !end.equals(iter); iter = next(iter)) {
                if (consumer.apply(getValue(iter))) {
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
            library = library.reinterpret(SHARED_LIBRARY.byteSize());
            if (!library.get(JAVA_BOOLEAN, NNB_OFFSET) &&
                    library.get(ADDRESS, CLA_OFFSET).address() == allocator) {
                return consumer.apply(library.get(ADDRESS, HANDLE_OFFSET));
            }
            // skip
            return false;
        });
    }
}

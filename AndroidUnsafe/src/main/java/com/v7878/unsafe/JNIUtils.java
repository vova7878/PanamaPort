package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("Since15")
public class JNIUtils {
    public static final GroupLayout JNI_NATIVE_INTERFACE_LAYOUT = structLayout(
            ADDRESS.withName("reserved0"),
            ADDRESS.withName("reserved1"),
            ADDRESS.withName("reserved2"),
            ADDRESS.withName("reserved3"),
            ADDRESS.withName("GetVersion"),
            ADDRESS.withName("DefineClass"),
            ADDRESS.withName("FindClass"),
            ADDRESS.withName("FromReflectedMethod"),
            ADDRESS.withName("FromReflectedField"),
            ADDRESS.withName("ToReflectedMethod"),
            ADDRESS.withName("GetSuperclass"),
            ADDRESS.withName("IsAssignableFrom"),
            ADDRESS.withName("ToReflectedField"),
            ADDRESS.withName("Throw"),
            ADDRESS.withName("ThrowNew"),
            ADDRESS.withName("ExceptionOccurred"),
            ADDRESS.withName("ExceptionDescribe"),
            ADDRESS.withName("ExceptionClear"),
            ADDRESS.withName("FatalError"),
            ADDRESS.withName("PushLocalFrame"),
            ADDRESS.withName("PopLocalFrame"),
            ADDRESS.withName("NewGlobalRef"),
            ADDRESS.withName("DeleteGlobalRef"),
            ADDRESS.withName("DeleteLocalRef"),
            ADDRESS.withName("IsSameObject"),
            ADDRESS.withName("NewLocalRef"),
            ADDRESS.withName("EnsureLocalCapacity"),
            ADDRESS.withName("AllocObject"),
            ADDRESS.withName("NewObject"),
            ADDRESS.withName("NewObjectV"),
            ADDRESS.withName("NewObjectA"),
            ADDRESS.withName("GetObjectClass"),
            ADDRESS.withName("IsInstanceOf"),
            ADDRESS.withName("GetMethodID"),
            ADDRESS.withName("CallObjectMethod"),
            ADDRESS.withName("CallObjectMethodV"),
            ADDRESS.withName("CallObjectMethodA"),
            ADDRESS.withName("CallBooleanMethod"),
            ADDRESS.withName("CallBooleanMethodV"),
            ADDRESS.withName("CallBooleanMethodA"),
            ADDRESS.withName("CallByteMethod"),
            ADDRESS.withName("CallByteMethodV"),
            ADDRESS.withName("CallByteMethodA"),
            ADDRESS.withName("CallCharMethod"),
            ADDRESS.withName("CallCharMethodV"),
            ADDRESS.withName("CallCharMethodA"),
            ADDRESS.withName("CallShortMethod"),
            ADDRESS.withName("CallShortMethodV"),
            ADDRESS.withName("CallShortMethodA"),
            ADDRESS.withName("CallIntMethod"),
            ADDRESS.withName("CallIntMethodV"),
            ADDRESS.withName("CallIntMethodA"),
            ADDRESS.withName("CallLongMethod"),
            ADDRESS.withName("CallLongMethodV"),
            ADDRESS.withName("CallLongMethodA"),
            ADDRESS.withName("CallFloatMethod"),
            ADDRESS.withName("CallFloatMethodV"),
            ADDRESS.withName("CallFloatMethodA"),
            ADDRESS.withName("CallDoubleMethod"),
            ADDRESS.withName("CallDoubleMethodV"),
            ADDRESS.withName("CallDoubleMethodA"),
            ADDRESS.withName("CallVoidMethod"),
            ADDRESS.withName("CallVoidMethodV"),
            ADDRESS.withName("CallVoidMethodA"),
            ADDRESS.withName("CallNonvirtualObjectMethod"),
            ADDRESS.withName("CallNonvirtualObjectMethodV"),
            ADDRESS.withName("CallNonvirtualObjectMethodA"),
            ADDRESS.withName("CallNonvirtualBooleanMethod"),
            ADDRESS.withName("CallNonvirtualBooleanMethodV"),
            ADDRESS.withName("CallNonvirtualBooleanMethodA"),
            ADDRESS.withName("CallNonvirtualByteMethod"),
            ADDRESS.withName("CallNonvirtualByteMethodV"),
            ADDRESS.withName("CallNonvirtualByteMethodA"),
            ADDRESS.withName("CallNonvirtualCharMethod"),
            ADDRESS.withName("CallNonvirtualCharMethodV"),
            ADDRESS.withName("CallNonvirtualCharMethodA"),
            ADDRESS.withName("CallNonvirtualShortMethod"),
            ADDRESS.withName("CallNonvirtualShortMethodV"),
            ADDRESS.withName("CallNonvirtualShortMethodA"),
            ADDRESS.withName("CallNonvirtualIntMethod"),
            ADDRESS.withName("CallNonvirtualIntMethodV"),
            ADDRESS.withName("CallNonvirtualIntMethodA"),
            ADDRESS.withName("CallNonvirtualLongMethod"),
            ADDRESS.withName("CallNonvirtualLongMethodV"),
            ADDRESS.withName("CallNonvirtualLongMethodA"),
            ADDRESS.withName("CallNonvirtualFloatMethod"),
            ADDRESS.withName("CallNonvirtualFloatMethodV"),
            ADDRESS.withName("CallNonvirtualFloatMethodA"),
            ADDRESS.withName("CallNonvirtualDoubleMethod"),
            ADDRESS.withName("CallNonvirtualDoubleMethodV"),
            ADDRESS.withName("CallNonvirtualDoubleMethodA"),
            ADDRESS.withName("CallNonvirtualVoidMethod"),
            ADDRESS.withName("CallNonvirtualVoidMethodV"),
            ADDRESS.withName("CallNonvirtualVoidMethodA"),
            ADDRESS.withName("GetFieldID"),
            ADDRESS.withName("GetObjectField"),
            ADDRESS.withName("GetBooleanField"),
            ADDRESS.withName("GetByteField"),
            ADDRESS.withName("GetCharField"),
            ADDRESS.withName("GetShortField"),
            ADDRESS.withName("GetIntField"),
            ADDRESS.withName("GetLongField"),
            ADDRESS.withName("GetFloatField"),
            ADDRESS.withName("GetDoubleField"),
            ADDRESS.withName("SetObjectField"),
            ADDRESS.withName("SetBooleanField"),
            ADDRESS.withName("SetByteField"),
            ADDRESS.withName("SetCharField"),
            ADDRESS.withName("SetShortField"),
            ADDRESS.withName("SetIntField"),
            ADDRESS.withName("SetLongField"),
            ADDRESS.withName("SetFloatField"),
            ADDRESS.withName("SetDoubleField"),
            ADDRESS.withName("GetStaticMethodID"),
            ADDRESS.withName("CallStaticObjectMethod"),
            ADDRESS.withName("CallStaticObjectMethodV"),
            ADDRESS.withName("CallStaticObjectMethodA"),
            ADDRESS.withName("CallStaticBooleanMethod"),
            ADDRESS.withName("CallStaticBooleanMethodV"),
            ADDRESS.withName("CallStaticBooleanMethodA"),
            ADDRESS.withName("CallStaticByteMethod"),
            ADDRESS.withName("CallStaticByteMethodV"),
            ADDRESS.withName("CallStaticByteMethodA"),
            ADDRESS.withName("CallStaticCharMethod"),
            ADDRESS.withName("CallStaticCharMethodV"),
            ADDRESS.withName("CallStaticCharMethodA"),
            ADDRESS.withName("CallStaticShortMethod"),
            ADDRESS.withName("CallStaticShortMethodV"),
            ADDRESS.withName("CallStaticShortMethodA"),
            ADDRESS.withName("CallStaticIntMethod"),
            ADDRESS.withName("CallStaticIntMethodV"),
            ADDRESS.withName("CallStaticIntMethodA"),
            ADDRESS.withName("CallStaticLongMethod"),
            ADDRESS.withName("CallStaticLongMethodV"),
            ADDRESS.withName("CallStaticLongMethodA"),
            ADDRESS.withName("CallStaticFloatMethod"),
            ADDRESS.withName("CallStaticFloatMethodV"),
            ADDRESS.withName("CallStaticFloatMethodA"),
            ADDRESS.withName("CallStaticDoubleMethod"),
            ADDRESS.withName("CallStaticDoubleMethodV"),
            ADDRESS.withName("CallStaticDoubleMethodA"),
            ADDRESS.withName("CallStaticVoidMethod"),
            ADDRESS.withName("CallStaticVoidMethodV"),
            ADDRESS.withName("CallStaticVoidMethodA"),
            ADDRESS.withName("GetStaticFieldID"),
            ADDRESS.withName("GetStaticObjectField"),
            ADDRESS.withName("GetStaticBooleanField"),
            ADDRESS.withName("GetStaticByteField"),
            ADDRESS.withName("GetStaticCharField"),
            ADDRESS.withName("GetStaticShortField"),
            ADDRESS.withName("GetStaticIntField"),
            ADDRESS.withName("GetStaticLongField"),
            ADDRESS.withName("GetStaticFloatField"),
            ADDRESS.withName("GetStaticDoubleField"),
            ADDRESS.withName("SetStaticObjectField"),
            ADDRESS.withName("SetStaticBooleanField"),
            ADDRESS.withName("SetStaticByteField"),
            ADDRESS.withName("SetStaticCharField"),
            ADDRESS.withName("SetStaticShortField"),
            ADDRESS.withName("SetStaticIntField"),
            ADDRESS.withName("SetStaticLongField"),
            ADDRESS.withName("SetStaticFloatField"),
            ADDRESS.withName("SetStaticDoubleField"),
            ADDRESS.withName("NewString"),
            ADDRESS.withName("GetStringLength"),
            ADDRESS.withName("GetStringChars"),
            ADDRESS.withName("ReleaseStringChars"),
            ADDRESS.withName("NewStringUTF"),
            ADDRESS.withName("GetStringUTFLength"),
            ADDRESS.withName("GetStringUTFChars"),
            ADDRESS.withName("ReleaseStringUTFChars"),
            ADDRESS.withName("GetArrayLength"),
            ADDRESS.withName("NewObjectArray"),
            ADDRESS.withName("GetObjectArrayElement"),
            ADDRESS.withName("SetObjectArrayElement"),
            ADDRESS.withName("NewBooleanArray"),
            ADDRESS.withName("NewByteArray"),
            ADDRESS.withName("NewCharArray"),
            ADDRESS.withName("NewShortArray"),
            ADDRESS.withName("NewIntArray"),
            ADDRESS.withName("NewLongArray"),
            ADDRESS.withName("NewFloatArray"),
            ADDRESS.withName("NewDoubleArray"),
            ADDRESS.withName("GetBooleanArrayElements"),
            ADDRESS.withName("GetByteArrayElements"),
            ADDRESS.withName("GetCharArrayElements"),
            ADDRESS.withName("GetShortArrayElements"),
            ADDRESS.withName("GetIntArrayElements"),
            ADDRESS.withName("GetLongArrayElements"),
            ADDRESS.withName("GetFloatArrayElements"),
            ADDRESS.withName("GetDoubleArrayElements"),
            ADDRESS.withName("ReleaseBooleanArrayElements"),
            ADDRESS.withName("ReleaseByteArrayElements"),
            ADDRESS.withName("ReleaseCharArrayElements"),
            ADDRESS.withName("ReleaseShortArrayElements"),
            ADDRESS.withName("ReleaseIntArrayElements"),
            ADDRESS.withName("ReleaseLongArrayElements"),
            ADDRESS.withName("ReleaseFloatArrayElements"),
            ADDRESS.withName("ReleaseDoubleArrayElements"),
            ADDRESS.withName("GetBooleanArrayRegion"),
            ADDRESS.withName("GetByteArrayRegion"),
            ADDRESS.withName("GetCharArrayRegion"),
            ADDRESS.withName("GetShortArrayRegion"),
            ADDRESS.withName("GetIntArrayRegion"),
            ADDRESS.withName("GetLongArrayRegion"),
            ADDRESS.withName("GetFloatArrayRegion"),
            ADDRESS.withName("GetDoubleArrayRegion"),
            ADDRESS.withName("SetBooleanArrayRegion"),
            ADDRESS.withName("SetByteArrayRegion"),
            ADDRESS.withName("SetCharArrayRegion"),
            ADDRESS.withName("SetShortArrayRegion"),
            ADDRESS.withName("SetIntArrayRegion"),
            ADDRESS.withName("SetLongArrayRegion"),
            ADDRESS.withName("SetFloatArrayRegion"),
            ADDRESS.withName("SetDoubleArrayRegion"),
            ADDRESS.withName("RegisterNatives"),
            ADDRESS.withName("UnregisterNatives"),
            ADDRESS.withName("MonitorEnter"),
            ADDRESS.withName("MonitorExit"),
            ADDRESS.withName("GetJavaVM"),
            ADDRESS.withName("GetStringRegion"),
            ADDRESS.withName("GetStringUTFRegion"),
            ADDRESS.withName("GetPrimitiveArrayCritical"),
            ADDRESS.withName("ReleasePrimitiveArrayCritical"),
            ADDRESS.withName("GetStringCritical"),
            ADDRESS.withName("ReleaseStringCritical"),
            ADDRESS.withName("NewWeakGlobalRef"),
            ADDRESS.withName("DeleteWeakGlobalRef"),
            ADDRESS.withName("ExceptionCheck"),
            ADDRESS.withName("NewDirectByteBuffer"),
            ADDRESS.withName("GetDirectBufferAddress"),
            ADDRESS.withName("GetDirectBufferCapacity"),
            ADDRESS.withName("GetObjectRefType")
    );

    public static final AddressLayout JNIEnv_LAYOUT =
            ADDRESS.withTargetLayout(JNI_NATIVE_INTERFACE_LAYOUT);

    public static final GroupLayout JNI_INVOKE_INTERFACE_LAYOUT = structLayout(
            ADDRESS.withName("reserved0"),
            ADDRESS.withName("reserved1"),
            ADDRESS.withName("reserved2"),
            ADDRESS.withName("DestroyJavaVM"),
            ADDRESS.withName("AttachCurrentThread"),
            ADDRESS.withName("DetachCurrentThread"),
            ADDRESS.withName("GetEnv"),
            ADDRESS.withName("AttachCurrentThreadAsDaemon")
    );
    public static final AddressLayout JavaVM_LAYOUT
            = ADDRESS.withTargetLayout(JNI_INVOKE_INTERFACE_LAYOUT);

    // TODO: get env from native and compute offset heuristically
    private static final long env_offset = nothrows_run(() -> {
        long tmp;
        switch (CORRECT_SDK_INT) {
            case 34: // android 14
                tmp = 21 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 33: // android 13
                tmp = 20 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 32: // android 12L
            case 31: // android 12
                tmp = 4; // StateAndFlags
                tmp += 21 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 30: // android 11
                tmp = 4; // StateAndFlags
                tmp += 22 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 29: // android 10
                tmp = 4; // StateAndFlags
                tmp += 20 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 28: // android 9
            case 27: // android 8.1
                tmp = 4; // StateAndFlags
                tmp += 17 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 26: // android 8
                tmp = 4; // StateAndFlags
                tmp += 15 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7 * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            default:
                throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    });

    private static final long nativePeerOffset = nothrows_run(
            () -> fieldOffset(getDeclaredField(Thread.class, "nativePeer")));

    public static MemorySegment getNativePeer(Thread thread) {
        Objects.requireNonNull(thread);
        long tmp = getLongO(thread, nativePeerOffset);
        assert_(tmp != 0, () -> new IllegalStateException("nativePeer == nullptr"));
        return MemorySegment.ofAddress(tmp);
    }

    public static MemorySegment getEnvPtr(Thread thread) {
        MemorySegment out = getNativePeer(thread).reinterpret(Long.MAX_VALUE)
                .get(ADDRESS.withTargetLayout(ADDRESS), env_offset);
        assert_(!NULL.equals(out), () -> new IllegalStateException("env == nullptr"));
        return out;
    }

    public static MemorySegment getCurrentEnvPtr() {
        return getEnvPtr(Thread.currentThread());
    }

    public static MemorySegment getJNINativeInterface() {
        // TODO: get raw functions
        return getCurrentEnvPtr().get(JNIEnv_LAYOUT, 0);
    }

    private static final Supplier<MemorySegment> javaVMPtr = runOnce(() -> {
        // TODO: cleanup code
        MemorySegment env = getCurrentEnvPtr();
        MethodHandle get_vm = Linker.nativeLinker().downcallHandle(
                getJNINativeInterface().get(ADDRESS,
                        JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement("GetJavaVM"))),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment jvm = arena.allocate(ADDRESS);
            int status = (int) nothrows_run(() -> get_vm.invoke(env, jvm));
            if (status != 0) {
                throw new IllegalStateException("can`t get JavaVM: " + status);
            }
            return jvm.get(ADDRESS.withTargetLayout(ADDRESS), 0);
        }
    });

    public static MemorySegment getJavaVMPtr() {
        return javaVMPtr.get();
    }

    public static MemorySegment getJNIInvokeInterface() {
        // TODO: get raw functions
        return getJavaVMPtr().get(JavaVM_LAYOUT, 0);
    }

    // TODO
    /*private static final Supplier<Pointer> runtimePtr = runOnce(() ->
            Linker.nativeLinker().lookup("_ZN3art7Runtime9instance_E").get(ADDRESS));

    public static Pointer getRuntimePtr() {
        return runtimePtr.get();
    }*/

    // TODO
    /*@Keep
    private static class RefUtils {
        @SuppressWarnings("JavaJniMissingFunction")
        @FastNative
        private native int NewGlobalRef32();

        @SuppressWarnings("JavaJniMissingFunction")
        @FastNative
        private native long NewGlobalRef64();

        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        private static native void DeleteGlobalRef32(int env, int ref);

        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        private static native void DeleteGlobalRef64(long env, long ref);
    }

    private static final Supplier<MethodHandle> newGlobalRef = runOnce(() -> {
        Class<?> word = IS64BIT ? long.class : int.class;
        String suffix = IS64BIT ? "64" : "32";

        Method m = getDeclaredMethod(RefUtils.class, "NewGlobalRef" + suffix);
        setExecutableData(m, getCurrentJNINativeInterface()
                .select(groupElement("NewGlobalRef")).get(ADDRESS, 0));
        MethodHandle out = unreflectDirect(m);

        MethodHandleMirror[] mirror = arrayCast(MethodHandleMirror.class, out);
        mirror[0].type = MethodType.methodType(word, Object.class);

        return out;
    });

    public static long NewGlobalRef(Object obj) {
        return nothrows_run(() -> {
            MethodHandle f = newGlobalRef.get();
            if (IS64BIT) {
                return (long) f.invokeExact(obj);
            } else {
                return ((int) f.invokeExact(obj)) & 0xffffffffL;
            }
        });
    }

    private static final Supplier<MethodHandle> deleteGlobalRef = runOnce(() -> {
        Class<?> word = IS64BIT ? long.class : int.class;
        String suffix = IS64BIT ? "64" : "32";

        Method m = getDeclaredMethod(RefUtils.class,
                "DeleteGlobalRef" + suffix, word, word);
        setExecutableData(m, getCurrentJNINativeInterface()
                .select(groupElement("DeleteGlobalRef")).get(ADDRESS, 0));

        return unreflect(m);
    });

    public static void DeleteGlobalRef(long ref) {
        nothrows_run(() -> {
            MethodHandle f = deleteGlobalRef.get();
            long env = getCurrentEnvPtr().address();
            if (IS64BIT) {
                f.invokeExact(env, ref);
            } else {
                f.invokeExact((int) env, (int)ref);
            }
        });
    }*/

    //TODO
    //public static class ScopedGlobalRef implements FineClosable {
    //}
}

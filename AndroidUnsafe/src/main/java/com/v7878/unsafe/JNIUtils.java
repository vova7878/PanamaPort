package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemorySegment.NULL;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.setMethodType;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.FAST_STATIC;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT_AS_ADDRESS;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.LibArt.ART;

import androidx.annotation.Keep;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;

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
    @ApiSensitive
    private static final long env_offset = nothrows_run(() -> {
        long tmp;
        switch (CORRECT_SDK_INT) {
            case 35 /*android 15*/ -> {
                tmp = 20 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 34 /*android 14*/ -> {
                tmp = 21 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 33 /*android 13*/ -> {
                tmp = 20 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 32 /*android 12L*/, 31 /*android 12*/ -> {
                tmp = 4; // StateAndFlags
                tmp += 21 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 30 /*android 11*/ -> {
                tmp = 4; // StateAndFlags
                tmp += 22 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 29 /*android 10*/ -> {
                tmp = 4; // StateAndFlags
                tmp += 20 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 28 /*android 9*/, 27 /*android 8.1*/ -> {
                tmp = 4; // StateAndFlags
                tmp += 17 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            case 26 /*android 8*/ -> {
                tmp = 4; // StateAndFlags
                tmp += 15 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            }
            default -> throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
        }
    });

    private static final long nativePeerOffset = nothrows_run(
            () -> fieldOffset(getDeclaredField(Thread.class, "nativePeer")));

    public static long getRawNativePeer(Thread thread) {
        Objects.requireNonNull(thread);
        long tmp = getLongO(thread, nativePeerOffset);
        assert_(tmp != 0, () -> new IllegalStateException("nativePeer == nullptr"));
        return tmp;
    }

    public static MemorySegment getNativePeer(Thread thread) {
        return MemorySegment.ofAddress(getRawNativePeer(thread));
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
        class Holder {
            static final MemorySegment jni_interface;

            static {
                //TODO: get unchecked functions?
                jni_interface = getCurrentEnvPtr().get(JNIEnv_LAYOUT, 0);
            }
        }
        return Holder.jni_interface;
    }

    public static MemorySegment getJNINativeInterfaceFunction(String name) {
        return getJNINativeInterface().get(ADDRESS,
                JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    public static MemorySegment getJavaVMPtr() {
        class Holder {
            static final MemorySegment jvm;

            @Keep
            @CriticalNative
            public static native int GetJavaVM32(int env, int jvm);

            @Keep
            @CriticalNative
            public static native int GetJavaVM64(long env, long jvm);

            static {
                Class<?> word = IS64BIT ? long.class : int.class;
                String suffix = IS64BIT ? "64" : "32";
                String name = "GetJavaVM";

                MemorySegment env = getCurrentEnvPtr();
                Method get_vm = getDeclaredMethod(Holder.class, name + suffix, word, word);
                registerNativeMethod(get_vm, getJNINativeInterfaceFunction(name).nativeAddress());
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment ptr = arena.allocate(ADDRESS);
                    int status = IS64BIT ? GetJavaVM64(env.nativeAddress(), ptr.nativeAddress()) :
                            GetJavaVM32((int) env.nativeAddress(), (int) ptr.nativeAddress());
                    if (status != 0) {
                        throw new IllegalStateException("can`t get JavaVM: " + status);
                    }
                    jvm = ptr.get(ADDRESS.withTargetLayout(ADDRESS), 0);
                }
            }
        }
        return Holder.jvm;
    }

    public static MemorySegment getJNIInvokeInterface() {
        class Holder {
            static final MemorySegment jni_interface;

            static {
                //TODO: get unchecked functions?
                jni_interface = getJavaVMPtr().get(JavaVM_LAYOUT, 0);
            }
        }
        return Holder.jni_interface;
    }

    public static MemorySegment getJNIInvokeInterfaceFunction(String name) {
        return getJNIInvokeInterface().get(ADDRESS,
                JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    //TODO: cache as much as possible
    @SuppressWarnings("unused")
    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @SymbolGenerator(method = "genDeleteGlobalRef")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void DeleteGlobalRef(long env, long ref);

        private static MemorySegment genDeleteGlobalRef() {
            return getJNINativeInterfaceFunction("DeleteGlobalRef");
        }

        @LibrarySymbol("_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, OBJECT_AS_ADDRESS})
        abstract long NewLocalRef(long env, Object obj);

        @LibrarySymbol("_ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void DeleteLocalRef(long env, long ref);

        @SuppressWarnings("SameParameterValue")
        @SymbolGenerator(method = "genPutRef")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG, LONG_AS_WORD})
        abstract void putRef(Object obj, long offset, long ref);

        private static MemorySegment genPutRef() {
            Method method = getDeclaredMethod(SunUnsafe.getUnsafeClass(), "putObject",
                    Object.class, long.class, Object.class);
            assert Modifier.isNative(method.getModifiers());
            return MemorySegment.ofAddress(getExecutableData(method));
        }

        @SymbolGenerator(method = "genPushLocalFrame")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void PushLocalFrame(long env, int capacity);

        private static MemorySegment genPushLocalFrame() {
            return JNIUtils.getJNINativeInterfaceFunction("PushLocalFrame");
        }

        @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
        @SymbolGenerator(method = "genPopLocalFrame")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long PopLocalFrame(long env, long survivor_ref);

        private static MemorySegment genPopLocalFrame() {
            return JNIUtils.getJNINativeInterfaceFunction("PopLocalFrame");
        }

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, ART));
    }

    // TODO: use BulkLinker
    @Keep
    abstract static class RefUtils {
        @FastNative
        @SuppressWarnings("unused")
        private native int NewGlobalRef32();

        @FastNative
        @SuppressWarnings("unused")
        private native long NewGlobalRef64();

        public static final MethodHandle newGlobalRef;

        static {
            Class<?> word = IS64BIT ? long.class : int.class;
            String suffix = IS64BIT ? "64" : "32";

            Method[] methods = getDeclaredMethods(RefUtils.class);

            Method ngr = searchMethod(methods, "NewGlobalRef" + suffix);
            registerNativeMethod(ngr, getJNINativeInterfaceFunction("NewGlobalRef").nativeAddress());

            newGlobalRef = unreflectDirect(ngr);
            setMethodType(newGlobalRef, MethodType.methodType(word, Object.class));
        }
    }

    public static long NewLocalRef(Object obj) {
        long env = getCurrentEnvPtr().nativeAddress();
        return Native.INSTANCE.NewLocalRef(env, obj);
    }

    public static void DeleteLocalRef(long ref) {
        long env = getCurrentEnvPtr().nativeAddress();
        Native.INSTANCE.DeleteLocalRef(env, ref);
    }

    public static void PushLocalFrame(int capacity) {
        long env = getCurrentEnvPtr().nativeAddress();
        Native.INSTANCE.PushLocalFrame(env, capacity);
    }

    public static void PopLocalFrame() {
        long env = getCurrentEnvPtr().nativeAddress();
        Native.INSTANCE.PopLocalFrame(env, 0);
    }

    public static long NewGlobalRef(Object obj) {
        return nothrows_run(() -> {
            MethodHandle h = RefUtils.newGlobalRef;
            if (IS64BIT) {
                return (long) h.invokeExact(obj);
            } else {
                return ((int) h.invokeExact(obj)) & 0xffffffffL;
            }
        });
    }

    public static void DeleteGlobalRef(long ref) {
        long env = getCurrentEnvPtr().nativeAddress();
        Native.INSTANCE.DeleteGlobalRef(env, ref);
    }

    public static long NewGlobalRef(Object obj, Arena arena) {
        Objects.requireNonNull(arena);
        long ref = NewGlobalRef(obj);
        JavaForeignAccess.addOrCleanupIfFail(arena.scope(), () -> DeleteGlobalRef(ref));
        return ref;
    }

    public static Object refToObject(long ref) {
        Object[] arr = new Object[1];
        Native.INSTANCE.putRef(arr, ARRAY_OBJECT_BASE_OFFSET, ref);
        return arr[0];
    }

    // TODO: use BulkLinker
    @Keep
    private static class IDUtils {
        @FastNative
        @SuppressWarnings("unused")
        private native int FromReflectedMethod32();

        @FastNative
        @SuppressWarnings("unused")
        private native long FromReflectedMethod64();

        @FastNative
        @SuppressWarnings("unused")
        private native int FromReflectedField32();

        @FastNative
        @SuppressWarnings("unused")
        private native long FromReflectedField64();

        public static final MethodHandle fromReflectedMethod;
        public static final MethodHandle fromReflectedField;

        static {
            Class<?> word = IS64BIT ? long.class : int.class;
            String suffix = IS64BIT ? "64" : "32";

            Method[] methods = getDeclaredMethods(IDUtils.class);

            Method frm = searchMethod(methods, "FromReflectedMethod" + suffix);
            registerNativeMethod(frm, getJNINativeInterfaceFunction("FromReflectedMethod").nativeAddress());

            fromReflectedMethod = unreflectDirect(frm);
            setMethodType(fromReflectedMethod, MethodType.methodType(word, Method.class));

            Method frf = searchMethod(methods, "FromReflectedField" + suffix);
            registerNativeMethod(frf, getJNINativeInterfaceFunction("FromReflectedField").nativeAddress());

            fromReflectedField = unreflectDirect(frf);
            setMethodType(fromReflectedField, MethodType.methodType(word, Field.class));
        }
    }

    public static long FromReflectedMethod(Method method) {
        return nothrows_run(() -> {
            MethodHandle h = IDUtils.fromReflectedMethod;
            if (IS64BIT) {
                return (long) h.invokeExact(method);
            } else {
                return ((int) h.invokeExact(method)) & 0xffffffffL;
            }
        });
    }

    public static long FromReflectedField(Field field) {
        return nothrows_run(() -> {
            MethodHandle h = IDUtils.fromReflectedField;
            if (IS64BIT) {
                return (long) h.invokeExact(field);
            } else {
                return ((int) h.invokeExact(field)) & 0xffffffffL;
            }
        });
    }

    public static MemorySegment getRuntimePtr() {
        class Holder {
            static final MemorySegment ptr;

            static {
                ptr = ART.find("_ZN3art7Runtime9instance_E").get()
                        .reinterpret(ADDRESS_SIZE).get(ADDRESS, 0);
            }
        }
        return Holder.ptr;
    }
}

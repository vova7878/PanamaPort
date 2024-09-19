package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.FAST_STATIC;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.FAST_VIRTUAL_REPLACE_THIS;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT_AS_ADDRESS;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.LibArt.ART;

import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.ASM;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;
import com.v7878.unsafe.foreign.BulkLinker.SymbolGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;

public class JNIUtils {
    @DoNotShrink
    private static final Arena SCOPE = Arena.ofAuto();

    public static final int JNIInvalidRefType = 0;
    public static final int JNILocalRefType = 1;
    public static final int JNIGlobalRefType = 2;
    public static final int JNIWeakGlobalRefType = 3;

    public static final int JNI_VERSION_1_1 = 0x00010001;
    public static final int JNI_VERSION_1_2 = 0x00010002;
    public static final int JNI_VERSION_1_4 = 0x00010004;
    public static final int JNI_VERSION_1_6 = 0x00010006;

    public static final int JNI_OK = 0;  /* no error */
    public static final int JNI_ERR = -1;  /* generic error */
    public static final int JNI_EDETACHED = -2;  /* thread detached from the VM */
    public static final int JNI_EVERSION = -3;  /* JNI version error */
    public static final int JNI_ENOMEM = -4;  /* Out of memory */
    public static final int JNI_EEXIST = -5;  /* VM already created */
    public static final int JNI_EINVAL = -6;  /* Invalid argument */

    public static final int JNI_COMMIT = 1;  /* copy content, do not free buffer */
    public static final int JNI_ABORT = 2;  /* free buffer w/o copying back */

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

    private static final long nativePeerOffset =
            fieldOffset(getDeclaredField(Thread.class, "nativePeer"));

    public static long getRawNativePeer(Thread thread) {
        Objects.requireNonNull(thread);
        long tmp = getLongO(thread, nativePeerOffset);
        assert_(tmp != 0, () -> new IllegalStateException("nativePeer == nullptr"));
        return tmp;
    }

    public static MemorySegment getNativePeer(Thread thread) {
        return MemorySegment.ofAddress(getRawNativePeer(thread));
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class ENV {
        @ASM(conditions = @Conditions(arch = X86_64), code = {72, -119, -8, -61})
        @ASM(conditions = @Conditions(arch = X86), code = {-117, 68, 36, 4, -61})
        @ASM(conditions = @Conditions(arch = ARM64), code = {-64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM), code = {30, -1, 47, -31})
        //TODO: @ASM(conditions = @Conditions(arch = RISCV64), code = {})
        @CallSignature(type = FAST_STATIC, ret = LONG_AS_WORD, args = {})
        abstract long current();

        static final ENV INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, ENV.class));
    }

    public static long getCurrentEnvPtr() {
        long env = ENV.INSTANCE.current();
        if (env == 0) {
            throw new IllegalStateException("env == nullptr");
        }
        return env;
    }

    public static MemorySegment getCurrentEnv() {
        return MemorySegment.ofAddress(getCurrentEnvPtr()).reinterpret(ADDRESS.byteSize());
    }

    //TODO: public static MemorySegment getEnvPtr(Thread thread) { }

    public static MemorySegment getJNINativeInterface() {
        class Holder {
            static final MemorySegment jni_interface;

            static {
                jni_interface = getCurrentEnv().get(JNIEnv_LAYOUT, 0);
            }
        }
        return Holder.jni_interface;
    }

    public static MemorySegment getJNINativeInterfaceFunction(String name) {
        return getJNINativeInterface().get(ADDRESS,
                JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    public static long getJNINativeInterfaceOffset(String name) {
        return JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name));
    }

    public static SymbolLookup getJNINativeInterfaceLookup() {
        return (name) -> {
            try {
                return Optional.of(getJNINativeInterfaceFunction(name));
            } catch (Throwable th) {
                return Optional.empty();
            }
        };
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class JVM {
        @LibrarySymbol(name = "GetJavaVM")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract int get_java_vm(long env, long jvm);

        static final JVM INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, JVM.class, getJNINativeInterfaceLookup()));
    }

    public static MemorySegment getJavaVM() {
        class Holder {
            static final MemorySegment jvm;

            static {
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment tmp = arena.allocate(ADDRESS);
                    int status = JVM.INSTANCE.get_java_vm(getCurrentEnvPtr(), tmp.nativeAddress());
                    if (status != JNI_OK) {
                        throw new IllegalStateException("Can`t get JavaVM: " + status);
                    }
                    jvm = tmp.get(ADDRESS.withTargetLayout(ADDRESS), 0);
                }
            }
        }
        return Holder.jvm;
    }

    public static long getJavaVMPtr() {
        return getJavaVM().nativeAddress();
    }

    public static MemorySegment getJNIInvokeInterface() {
        class Holder {
            static final MemorySegment jni_interface;

            static {
                jni_interface = getJavaVM().get(JavaVM_LAYOUT, 0);
            }
        }
        return Holder.jni_interface;
    }

    public static MemorySegment getJNIInvokeInterfaceFunction(String name) {
        return getJNIInvokeInterface().get(ADDRESS,
                JNI_INVOKE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    public static long getJNIInvokeInterfaceOffset(String name) {
        return JNI_INVOKE_INTERFACE_LAYOUT.byteOffset(groupElement(name));
    }

    public static SymbolLookup getJNIInvokeInterfaceLookup() {
        return (name) -> {
            try {
                return Optional.of(getJNIInvokeInterfaceFunction(name));
            } catch (Throwable th) {
                return Optional.empty();
            }
        };
    }

    public static MemorySegment getRuntime() {
        class Holder {
            static final MemorySegment ptr;

            static {
                ptr = ART.findOrThrow("_ZN3art7Runtime9instance_E")
                        .reinterpret(ADDRESS_SIZE).get(ADDRESS, 0);
            }
        }
        return Holder.ptr;
    }

    public static long getRuntimePtr() {
        return getRuntime().nativeAddress();
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @LibrarySymbol(name = "NewGlobalRef")
        @CallSignature(type = FAST_VIRTUAL_REPLACE_THIS, ret = LONG_AS_WORD, args = {OBJECT})
        abstract long NewGlobalRef(Object obj);

        @LibrarySymbol(name = "DeleteGlobalRef")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void DeleteGlobalRef(long env, long ref);

        @LibrarySymbol(name = "_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, OBJECT_AS_ADDRESS})
        abstract long NewLocalRef(long env, Object obj);

        @LibrarySymbol(name = "_ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void DeleteLocalRef(long env, long ref);

        @SuppressWarnings("SameParameterValue")
        @SymbolGenerator(method = "genPutRef")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG, LONG_AS_WORD})
        abstract void putRef(Object obj, long offset, long ref);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static MemorySegment genPutRef() {
            Method method = getDeclaredMethod(SunUnsafe.getUnsafeClass(), "putObject",
                    Object.class, long.class, Object.class);
            assert Modifier.isNative(method.getModifiers());
            return MemorySegment.ofAddress(getExecutableData(method));
        }

        @LibrarySymbol(name = "PushLocalFrame")
        @CallSignature(type = CRITICAL, ret = INT, args = {LONG_AS_WORD, INT})
        abstract int PushLocalFrame(long env, int capacity);

        @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
        @LibrarySymbol(name = "PopLocalFrame")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long PopLocalFrame(long env, long survivor_ref);

        @LibrarySymbol(name = "FromReflectedMethod")
        @CallSignature(type = FAST_VIRTUAL_REPLACE_THIS, ret = LONG_AS_WORD, args = {OBJECT})
        abstract long FromReflectedMethod(Object method);

        @SuppressWarnings("SameParameterValue")
        @LibrarySymbol(name = "ToReflectedMethod")
        // (JNIEnv* env, jclass ignored, jmethodID mid, jboolean ignored) -> jmethod
        @CallSignature(type = FAST_STATIC, ret = OBJECT, args = {LONG_AS_WORD, BOOL})
        abstract Object ToReflectedMethod(long mid, boolean ignored);

        @LibrarySymbol(name = "FromReflectedField")
        @CallSignature(type = FAST_VIRTUAL_REPLACE_THIS, ret = LONG_AS_WORD, args = {OBJECT})
        abstract long FromReflectedField(Object field);

        @SuppressWarnings("SameParameterValue")
        @LibrarySymbol(name = "ToReflectedField")
        // (JNIEnv* env, jclass ignored, jfieldID fid, jboolean ignored) -> jfield
        @CallSignature(type = FAST_STATIC, ret = OBJECT, args = {LONG_AS_WORD, BOOL})
        abstract Object ToReflectedField(long fid, boolean ignored);

        // TODO: GetObjectRefType

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, ART.or(getJNINativeInterfaceLookup())));
    }

    public static long NewLocalRef(Object obj) {
        long env = getCurrentEnvPtr();
        return Native.INSTANCE.NewLocalRef(env, obj);
    }

    public static void DeleteLocalRef(long ref) {
        long env = getCurrentEnvPtr();
        Native.INSTANCE.DeleteLocalRef(env, ref);
    }

    public static void PushLocalFrame(int capacity) {
        long env = getCurrentEnvPtr();
        int status = Native.INSTANCE.PushLocalFrame(env, capacity);
        // Note: if the return value != JNI_OK, then OutOfMemoryError is thrown
        assert status == JNI_OK;
    }

    public static void PopLocalFrame() {
        long env = getCurrentEnvPtr();
        Native.INSTANCE.PopLocalFrame(env, 0);
    }

    public static long NewGlobalRef(Object obj) {
        return Native.INSTANCE.NewGlobalRef(obj);
    }

    public static void DeleteGlobalRef(long ref) {
        long env = getCurrentEnvPtr();
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

    public static long FromReflectedMethod(Method method) {
        return Native.INSTANCE.FromReflectedMethod(method);
    }

    public static Method ToReflectedMethod(long mid) {
        return (Method) Native.INSTANCE.ToReflectedMethod(mid, /*unused*/ false);
    }

    //TODO: public static long ToArtMethod(long mid)

    public static long FromReflectedField(Field field) {
        return Native.INSTANCE.FromReflectedField(field);
    }

    public static Field ToReflectedField(long fid) {
        return (Field) Native.INSTANCE.ToReflectedField(fid, /*unused*/ false);
    }

    //TODO: public static long ToArtField(long mid)
}

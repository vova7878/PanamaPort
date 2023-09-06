package com.v7878.unsafe;

import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.RawUnOp.NEG_INT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.setMethodType;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;
import static com.v7878.unsafe.Utils.searchMethod;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import androidx.annotation.Keep;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.unsafe.access.JavaForeignAccess;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Supplier;

import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.DexFile;

@SuppressWarnings({"Since15", "deprecation"})
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
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 33: // android 13
                tmp = 20 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 32: // android 12L
            case 31: // android 12
                tmp = 4; // StateAndFlags
                tmp += 21 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 30: // android 11
                tmp = 4; // StateAndFlags
                tmp += 22 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 29: // android 10
                tmp = 4; // StateAndFlags
                tmp += 20 * 4; // tls32_
                tmp += 4; // padding
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 28: // android 9
            case 27: // android 8.1
                tmp = 4; // StateAndFlags
                tmp += 17 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
                return tmp;
            case 26: // android 8
                tmp = 4; // StateAndFlags
                tmp += 15 * 4; // tls32_
                tmp += 8 * 8; // tls64_
                tmp += 7L * ADDRESS_SIZE; // tlsPtr_
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

    public static MemorySegment getJNINativeInterfaceFunction(String name) {
        return getJNINativeInterface().get(ADDRESS,
                JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    private static final Supplier<MemorySegment> javaVMPtr = runOnce(() -> {
        MemorySegment env = getCurrentEnvPtr();
        MethodHandle get_vm = Linker.nativeLinker().downcallHandle(
                getJNINativeInterfaceFunction("GetJavaVM"),
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

    public static MemorySegment getJNIInvokeInterfaceFunction(String name) {
        return getJNIInvokeInterface().get(ADDRESS,
                JNI_NATIVE_INTERFACE_LAYOUT.byteOffset(groupElement(name)));
    }

    @Keep
    abstract static class RefUtils {
        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native int NewGlobalRef32();

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native long NewGlobalRef64();

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void DeleteGlobalRef32(int env, int ref);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void DeleteGlobalRef64(long env, long ref);

        abstract long NewLocalRef64(long env, Object obj);

        abstract int NewLocalRef32(int env, Object obj);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        static native long RawNewLocalRef64(long env, long ref_ptr);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        static native int RawNewLocalRef32(int env, int ref_ptr);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void DeleteLocalRef64(long env, long ref);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void DeleteLocalRef32(int env, int ref);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void PushLocalFrame64(long env, int capacity);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void PushLocalFrame32(int env, int capacity);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void PopLocalFrame64(long env);

        @CriticalNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void PopLocalFrame32(int env);

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void putRef64(Object obj, long offset, long ref);

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private static native void putRef32(Object obj, long offset, int ref);

        public static final MethodHandle newGlobalRef;

        static {
            Class<?> word = IS64BIT ? long.class : int.class;
            String suffix = IS64BIT ? "64" : "32";

            Method[] methods = getDeclaredMethods(RefUtils.class);

            Method put = getDeclaredMethod(SunUnsafe.getUnsafeClass(), "putObject",
                    Object.class, long.class, Object.class);
            assert_(Modifier.isNative(put.getModifiers()), AssertionError::new);
            setExecutableData(searchMethod(methods, "putRef" + suffix,
                    Object.class, long.class, word), getExecutableData(put));

            Method ngr = searchMethod(methods, "NewGlobalRef" + suffix);
            setExecutableData(ngr, getJNINativeInterfaceFunction("NewGlobalRef").address());

            newGlobalRef = unreflectDirect(ngr);
            setMethodType(newGlobalRef, MethodType.methodType(word, Object.class));

            Method dgr = searchMethod(methods, "DeleteGlobalRef" + suffix, word, word);
            setExecutableData(dgr, getJNINativeInterfaceFunction("DeleteGlobalRef").address());

            SymbolLookup art = Linker.nativeLinker().defaultLookup();

            MemorySegment nlr = art.find("_ZN3art9JNIEnvExt11NewLocalRefEPNS_6mirror6ObjectE").get();
            setExecutableData(searchMethod(methods, "RawNewLocalRef" + suffix, word, word), nlr.address());

            MemorySegment dlr = art.find("_ZN3art9JNIEnvExt14DeleteLocalRefEP8_jobject").get();
            setExecutableData(searchMethod(methods, "DeleteLocalRef" + suffix, word, word), dlr.address());

            // art.find("_ZN3art9JNIEnvExt9PushFrameEi").get();
            MemorySegment push = JNIUtils.getJNINativeInterfaceFunction("PushLocalFrame");
            setExecutableData(searchMethod(methods, "PushLocalFrame" + suffix, word, int.class), push.address());

            MemorySegment pop = art.find("_ZN3art9JNIEnvExt8PopFrameEv").get();
            setExecutableData(searchMethod(methods, "PopLocalFrame" + suffix, word), pop.address());
        }
    }

    private static final Supplier<RefUtils> localRefUtils = runOnce(() -> {
        //make sure kPoisonReferences is initialized
        boolean kPoisonReferences = VM.kPoisonReferences.get();

        Class<?> word = IS64BIT ? long.class : int.class;
        TypeId word_id = TypeId.of(word);
        String suffix = IS64BIT ? "64" : "32";

        String impl_name = RefUtils.class.getName() + "$Impl";
        TypeId impl_id = TypeId.of(impl_name);
        ClassDef impl_def = new ClassDef(impl_id);
        impl_def.setSuperClass(TypeId.of(RefUtils.class));

        MethodId raw_nlr_id = new MethodId(TypeId.of(RefUtils.class),
                new ProtoId(word_id, word_id, word_id), "RawNewLocalRef" + suffix);
        MethodId nlr_id = new MethodId(impl_id, new ProtoId(word_id,
                word_id, TypeId.of(Object.class)), "NewLocalRef" + suffix);

        // note: it's broken - object is cast to pointer
        if (IS64BIT) {
            // pointer 64-bit, object 32-bit -> fill upper 32 bits with zeros (l0 register)
            impl_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                    nlr_id, Modifier.PUBLIC).withCode(1, b -> b
                    .const_4(b.l(0), 0)
                    .if_(kPoisonReferences,
                            // TODO: how will GC react to this?
                            unused -> b.raw_unop(NEG_INT, b.p(2), b.p(2)),
                            unused -> b.nop()
                    )
                    .invoke(STATIC, raw_nlr_id, b.p(0), b.p(1), b.p(2), b.l(0))
                    .move_result_wide(b.v(0))
                    .return_wide(b.v(0))
            ));
        } else {
            impl_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                    nlr_id, Modifier.PUBLIC).withCode(0, b -> b
                    .if_(kPoisonReferences,
                            // TODO: how will GC react to this?
                            unused -> b.raw_unop(NEG_INT, b.p(1), b.p(1)),
                            unused -> b.nop()
                    )
                    .invoke(STATIC, raw_nlr_id, b.p(0), b.p(1))
                    .move_result(b.v(0))
                    .return_(b.v(0))
            ));
        }

        DexFile dex = openDexFile(new Dex(impl_def).compile());
        Class<?> utils = loadClass(dex, impl_name, RefUtils.class.getClassLoader());
        setClassStatus(utils, ClassUtils.ClassStatus.Verified);

        return (RefUtils) allocateInstance(utils);
    });

    public static long NewLocalRef(Object obj) {
        RefUtils utils = localRefUtils.get();
        long env = getCurrentEnvPtr().address();
        if (IS64BIT) {
            return utils.NewLocalRef64(env, obj);
        } else {
            return utils.NewLocalRef32((int) env, obj) & 0xffffffffL;
        }
    }

    public static void DeleteLocalRef(long ref) {
        long env = getCurrentEnvPtr().address();
        if (IS64BIT) {
            RefUtils.DeleteLocalRef64(env, ref);
        } else {
            RefUtils.DeleteLocalRef32((int) env, (int) ref);
        }
    }

    public static void PushLocalFrame(int capacity) {
        long env = getCurrentEnvPtr().address();
        if (IS64BIT) {
            RefUtils.PushLocalFrame64(env, capacity);
        } else {
            RefUtils.PushLocalFrame32((int) env, capacity);
        }
    }

    public static void PopLocalFrame() {
        long env = getCurrentEnvPtr().address();
        if (IS64BIT) {
            RefUtils.PopLocalFrame64(env);
        } else {
            RefUtils.PopLocalFrame32((int) env);
        }
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
        long env = getCurrentEnvPtr().address();
        if (IS64BIT) {
            RefUtils.DeleteGlobalRef64(env, ref);
        } else {
            RefUtils.DeleteGlobalRef32((int) env, (int) ref);
        }
    }

    public static long NewGlobalRef(Object obj, Arena arena) {
        Objects.requireNonNull(arena);
        long ref = NewGlobalRef(obj);
        JavaForeignAccess.addOrCleanupIfFail(arena.scope(), () -> DeleteGlobalRef(ref));
        return ref;
    }

    public static Object refToObject(long ref) {
        Object[] arr = new Object[1];
        final long offset = ARRAY_OBJECT_BASE_OFFSET;
        if (IS64BIT) {
            RefUtils.putRef64(arr, offset, ref);
        } else {
            RefUtils.putRef32(arr, offset, (int) ref);
        }
        return arr[0];
    }

    @Keep
    private static class IDUtils {
        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native int FromReflectedMethod32();

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native long FromReflectedMethod64();

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native int FromReflectedField32();

        @FastNative
        @SuppressWarnings("JavaJniMissingFunction")
        private native long FromReflectedField64();

        public static final MethodHandle fromReflectedMethod;
        public static final MethodHandle fromReflectedField;

        static {
            Class<?> word = IS64BIT ? long.class : int.class;
            String suffix = IS64BIT ? "64" : "32";

            Method[] methods = getDeclaredMethods(IDUtils.class);

            Method frm = searchMethod(methods, "FromReflectedMethod" + suffix);
            setExecutableData(frm, getJNINativeInterfaceFunction("FromReflectedMethod").address());

            fromReflectedMethod = unreflectDirect(frm);
            setMethodType(fromReflectedMethod, MethodType.methodType(word, Method.class));

            Method frf = searchMethod(methods, "FromReflectedField" + suffix);
            setExecutableData(frf, getJNINativeInterfaceFunction("FromReflectedField").address());

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

    private static final Supplier<MemorySegment> runtimePtr = runOnce(() ->
            Linker.nativeLinker().defaultLookup()
                    .find("_ZN3art7Runtime9instance_E")
                    .get().get(ADDRESS, 0));

    public static MemorySegment getRuntimePtr() {
        return runtimePtr.get();
    }
}

package com.v7878.unsafe;

import static com.v7878.misc.Math.isSigned32Bit;
import static com.v7878.misc.Math.roundUp;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.arrayBaseOffset;
import static com.v7878.unsafe.AndroidUnsafe.arrayIndexScale;
import static com.v7878.unsafe.AndroidUnsafe.getInt;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.getWordO;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.AndroidUnsafe.putWordO;
import static com.v7878.unsafe.Reflection.ClassMirror;
import static com.v7878.unsafe.Reflection.arrayCast;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.runOnce;

import androidx.annotation.Keep;

import com.v7878.misc.Checks;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Supplier;

public class VM {
    @Keep
    public static class ArrayMirror {

        public int length;
    }

    @Keep
    public static class StringMirror {

        public static final boolean COMPACT_STRINGS = nothrows_run(() -> {
            StringMirror[] test = arrayCast(StringMirror.class, "\uffff");
            if (test[0].count == 3) {
                return true;
            }
            if (test[0].count == 1) {
                return false;
            }
            throw new IllegalStateException("" + test[0].count);
        });

        public int count;
        public int hash;
    }

    public static final int OBJECT_ALIGNMENT_SHIFT = 3;
    public static final int OBJECT_ALIGNMENT = 1 << OBJECT_ALIGNMENT_SHIFT;
    public static final int OBJECT_INSTANCE_SIZE = objectSizeField(Object.class);

    public static final int OBJECT_FIELD_SIZE_SHIFT = 2;
    public static final int OBJECT_FIELD_SIZE = 1 << OBJECT_FIELD_SIZE_SHIFT;

    private static final Supplier<Field> shadow$_klass_ =
            runOnce(() -> getDeclaredField(Object.class, "shadow$_klass_"));
    private static final Supplier<Field> shadow$_monitor_ =
            runOnce(() -> getDeclaredField(Object.class, "shadow$_monitor_"));

    private static final Supplier<Class<?>> vmruntime_class = runOnce(() ->
            nothrows_run(() -> Class.forName("dalvik.system.VMRuntime")));
    private static final Supplier<Object> vmruntime = runOnce(() ->
            allocateInstance(vmruntime_class.get()));

    private static final Supplier<MethodHandle> newNonMovableArray =
            runOnce(() -> unreflectDirect(getDeclaredMethod(vmruntime_class.get(),
                    "newNonMovableArray", Class.class, int.class)));
    private static final Supplier<MethodHandle> addressOf =
            runOnce(() -> unreflectDirect(getDeclaredMethod(vmruntime_class.get(),
                    "addressOf", Object.class)));
    private static final Supplier<MethodHandle> vmLibrary =
            runOnce(() -> unreflectDirect(getDeclaredMethod(vmruntime_class.get(),
                    "vmLibrary")));

    private static final Supplier<MethodHandle> getCurrentInstructionSet =
            runOnce(() -> unreflect(getDeclaredMethod(vmruntime_class.get(),
                    "getCurrentInstructionSet")));

    private static final Supplier<MethodHandle> internalClone =
            runOnce(() -> unreflectDirect(getDeclaredMethod(Object.class, "internalClone")));

    static {
        assert_(ARRAY_OBJECT_INDEX_SCALE == OBJECT_FIELD_SIZE, AssertionError::new);
        assert_(ARRAY_INT_BASE_OFFSET == 12, AssertionError::new);
        assert_(OBJECT_INSTANCE_SIZE == 8, AssertionError::new);
    }

    public static String vmLibrary() {
        return (String) nothrows_run(() -> vmLibrary.get().invoke(vmruntime.get()));
    }

    public static String getCurrentInstructionSet() {
        return (String) nothrows_run(() -> getCurrentInstructionSet.get().invoke());
    }

    public static Field getShadowKlassField() {
        return shadow$_klass_.get();
    }

    public static Field getShadowMonitorField() {
        return shadow$_monitor_.get();
    }

    @SuppressWarnings("unchecked")
    public static <T> T internalClone(T obj) {
        return (T) nothrows_run(() -> internalClone.get().invoke(obj));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @SuppressWarnings("unchecked")
    public static <T> T setObjectClass(Object obj, Class<T> clazz) {
        Field sk = getShadowKlassField();
        nothrows_run(() -> sk.set(obj, clazz));
        return (T) obj;
    }

    public static Object newNonMovableArray(Class<?> componentType, int length) {
        return nothrows_run(() -> newNonMovableArray.get()
                .invoke(vmruntime.get(), componentType, length));
    }

    public static long addressOfNonMovableArrayData(Object array) {
        return (long) nothrows_run(() -> addressOf.get().invoke(vmruntime.get(), array));
    }

    public static long addressOfNonMovableArray(Object array) {
        return addressOfNonMovableArrayData(array) - arrayBaseOffset(array.getClass());
    }

    public static int getArrayLength(Object arr) {
        assert_(arr.getClass().isArray(), IllegalArgumentException::new);
        ArrayMirror[] clh = arrayCast(ArrayMirror.class, arr);
        return clh[0].length;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setArrayLength(Object arr, int length) {
        assert_(arr.getClass().isArray(), IllegalArgumentException::new);
        assert_(length >= 0, IllegalArgumentException::new);
        ArrayMirror[] clh = arrayCast(ArrayMirror.class, arr);
        clh[0].length = length;
    }

    public static int objectSizeField(Class<?> clazz) {
        ClassMirror[] clh = arrayCast(ClassMirror.class, clazz);
        int out = clh[0].objectSize;
        assert_(out != 0, IllegalArgumentException::new);
        return out;
    }

    public static int classSizeField(Class<?> clazz) {
        ClassMirror[] clh = arrayCast(ClassMirror.class, clazz);
        return clh[0].classSize;
    }

    public static int emptyClassSize() {
        return classSizeField(void.class);
    }

    public static boolean shouldHaveEmbeddedVTableAndImt(Class<?> clazz) {
        return clazz.isArray()
                || !(clazz.isInterface()
                || clazz.isPrimitive()
                || Modifier.isAbstract(clazz.getModifiers()));
    }

    public static int getEmbeddedVTableLength(Class<?> clazz) {
        assert_(shouldHaveEmbeddedVTableAndImt(clazz), IllegalArgumentException::new);
        return getIntO(clazz, emptyClassSize());
    }

    private static final long VTABLE_OFFSET = roundUp(emptyClassSize() + 4, ADDRESS_SIZE) + ADDRESS_SIZE;

    public static long getEmbeddedVTableEntry(Class<?> clazz, int index) {
        Checks.checkIndex(index, getEmbeddedVTableLength(clazz));
        return getWordO(clazz, VTABLE_OFFSET + (long) index * ADDRESS_SIZE);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setEmbeddedVTableEntry(Class<?> clazz, int index, long art_method) {
        Checks.checkIndex(index, getEmbeddedVTableLength(clazz));
        putWordO(clazz, VTABLE_OFFSET + (long) index * ADDRESS_SIZE, art_method);
    }

    public static boolean isCompressedString(String s) {
        StringMirror[] sm = arrayCast(StringMirror.class, s);
        return StringMirror.COMPACT_STRINGS && ((sm[0].count & 1) == 0);
    }

    public static int sizeOf(Object obj) {
        Objects.requireNonNull(obj);
        if (obj instanceof String sobj) {
            int data_size = sobj.length() * (isCompressedString(sobj) ? 1 : 2);
            return roundUp(objectSizeField(StringMirror.class) + data_size, OBJECT_ALIGNMENT);
        }
        if (obj instanceof Class) {
            return classSizeField((Class<?>) obj);
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return arrayBaseOffset(clazz) + arrayIndexScale(clazz) * getArrayLength(obj);
        }
        return objectSizeField(clazz);
    }

    public static int alignedSizeOf(Object obj) {
        return roundUp(sizeOf(obj), OBJECT_ALIGNMENT);
    }

    @DangerLevel(DangerLevel.POTENTIAL_GC_COLLISION)
    public static int rawObjectToInt(Object obj) {
        Object[] arr = new Object[1];
        arr[0] = obj;
        return getInt(arr, ARRAY_OBJECT_BASE_OFFSET);
    }

    @DangerLevel(DangerLevel.POTENTIAL_GC_COLLISION)
    public static void putObjectRaw(long address, Object value) {
        putIntN(address, rawObjectToInt(value));
    }

    @DangerLevel(DangerLevel.GC_COLLISION_MOVABLE_OBJECTS)
    public static Object rawIntToObject(int obj) {
        int[] arr = new int[1];
        arr[0] = obj;
        return getObject(arr, ARRAY_INT_BASE_OFFSET);
    }

    @DangerLevel(DangerLevel.GC_COLLISION_MOVABLE_OBJECTS)
    public static Object getObjectRaw(long address) {
        return rawIntToObject(getIntN(address));
    }

    protected static final Supplier<Boolean> kPoisonReferences = runOnce(() -> {
        Object test = newNonMovableArray(int.class, 0);
        long address = addressOfNonMovableArray(test);
        assert_(isSigned32Bit(address), AssertionError::new);
        int actual = (int) address;
        int raw = rawObjectToInt(test);
        if (actual == raw) {
            return false;
        } else if (actual == -raw) {
            return true;
        } else {
            throw new AssertionError(
                    "unknown type of poisoning: actual:" + actual + " raw:" + raw);
        }
    });

    @DangerLevel(DangerLevel.POTENTIAL_GC_COLLISION)
    public static int objectToInt(Object obj) {
        int out = rawObjectToInt(obj);
        return kPoisonReferences.get() ? -out : out;
    }

    @DangerLevel(DangerLevel.POTENTIAL_GC_COLLISION)
    public static long objectToLong(Object obj) {
        return objectToInt(obj) & 0xffffffffL;
    }

    @DangerLevel(DangerLevel.GC_COLLISION_MOVABLE_OBJECTS)
    public static Object intToObject(int obj) {
        return rawIntToObject(kPoisonReferences.get() ? -obj : obj);
    }
}

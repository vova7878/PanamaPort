package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.arrayBaseOffset;
import static com.v7878.unsafe.AndroidUnsafe.arrayIndexScale;
import static com.v7878.unsafe.AndroidUnsafe.getInt;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.getWordO;
import static com.v7878.unsafe.AndroidUnsafe.putIntN;
import static com.v7878.unsafe.AndroidUnsafe.putWordO;
import static com.v7878.unsafe.Reflection.fillArray;
import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.Reflection.unreflectDirect;
import static com.v7878.unsafe.Utils.check;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.access.AccessLinker.FieldAccess;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;
import static com.v7878.unsafe.misc.Math.isSigned32Bit;
import static com.v7878.unsafe.misc.Math.roundUpU;
import static com.v7878.unsafe.misc.Math.roundUpUL;

import android.annotation.TargetApi;
import android.os.Build;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.VMAccess;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class VM {
    @DoNotShrink
    @DoNotObfuscate
    private static class ArrayMirror {
        public int length;
    }

    @DoNotShrink
    @DoNotObfuscate
    @SuppressWarnings("unused")
    private static class StringMirror {
        public static final boolean COMPACT_STRINGS;

        static {
            var mirror = new StringMirror[1];
            fillArray(mirror, "\uffff");
            int test = mirror[0].count;
            COMPACT_STRINGS = switch (test) {
                case 3 -> true;
                case 1 -> false;
                default -> throw new IllegalStateException("Illegal test value: " + test);
            };
        }

        public int count;
        public int hash;
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.lang.Object", name = "shadow$_klass_")
        abstract void shadow$_klass_(Object instance, Class<?> value);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.Class", name = "vtable")
        abstract Object vtable(Class<?> instance);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.lang.Class", name = "vtable")
        abstract void vtable(Class<?> instance, Object value);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.Class", name = "ifTable")
        abstract Object[] ifTable(Class<?> instance);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.lang.Class", name = "ifTable")
        abstract void ifTable(Class<?> instance, Object[] value);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.Class", name = "dexClassDefIndex")
        abstract int dexClassDefIndex(Class<?> instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.Class", name = "objectSize")
        abstract int objectSize(Class<?> instance);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.Class", name = "classSize")
        abstract int classSize(Class<?> instance);

        public static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    public static final int OBJECT_ALIGNMENT_SHIFT = 3;
    public static final int OBJECT_ALIGNMENT = 1 << OBJECT_ALIGNMENT_SHIFT;
    public static final int OBJECT_INSTANCE_SIZE = objectSizeField(Object.class);

    public static final int OBJECT_FIELD_SIZE_SHIFT = 2;
    public static final int OBJECT_FIELD_SIZE = 1 << OBJECT_FIELD_SIZE_SHIFT;

    public static final int STRING_HEADER_SIZE = objectSizeField(StringMirror.class);

    static {
        check(ARRAY_OBJECT_INDEX_SCALE == OBJECT_FIELD_SIZE, AssertionError::new);
        check(ARRAY_INT_BASE_OFFSET == 12, AssertionError::new);
        check(OBJECT_INSTANCE_SIZE == 8, AssertionError::new);
    }

    @SuppressWarnings("unchecked")
    public static <T> T internalClone(T obj) {
        // TODO: move to AccessI
        class Holder {
            static final MethodHandle internalClone = unreflectDirect(
                    getHiddenMethod(Object.class, "internalClone"));
        }
        return (T) nothrows_run(() -> Holder.internalClone.invoke(obj));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @SuppressWarnings("unchecked")
    public static <T> T setObjectClass(Object obj, Class<T> clazz) {
        AccessI.INSTANCE.shadow$_klass_(obj, clazz);
        return (T) obj;
    }

    public static String[] properties() {
        return VMAccess.properties();
    }

    public static String bootClassPath() {
        return VMAccess.bootClassPath();
    }

    public static String classPath() {
        return VMAccess.classPath();
    }

    public static String vmLibrary() {
        return VMAccess.vmLibrary();
    }

    public static boolean isDebugVMLibrary() {
        return "libartd.so".equals(vmLibrary());
    }

    public static boolean isCheckJniEnabled() {
        return VMAccess.isCheckJniEnabled();
    }

    public static boolean isNativeDebuggable() {
        return VMAccess.isNativeDebuggable();
    }

    @TargetApi(Build.VERSION_CODES.P)
    public static boolean isJavaDebuggable() {
        return VMAccess.isJavaDebuggable();
    }

    public static String getCurrentInstructionSet() {
        return VMAccess.getCurrentInstructionSet();
    }

    public static Object newNonMovableArray(Class<?> componentType, int length) {
        return VMAccess.newNonMovableArray(componentType, length);
    }

    public static long addressOfNonMovableArrayData(Object array) {
        return VMAccess.addressOf(array);
    }

    public static long addressOfNonMovableArray(Object array) {
        return addressOfNonMovableArrayData(array) - arrayBaseOffset(array.getClass());
    }

    public static int getArrayLength(Object arr) {
        check(arr.getClass().isArray(), IllegalArgumentException::new);
        var mirror = new ArrayMirror[1];
        fillArray(mirror, arr);
        return mirror[0].length;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setArrayLength(Object arr, int length) {
        check(arr.getClass().isArray(), IllegalArgumentException::new);
        check(length >= 0, IllegalArgumentException::new);
        var mirror = new ArrayMirror[1];
        fillArray(mirror, arr);
        mirror[0].length = length;
    }

    public static int getDexClassDefIndex(Class<?> clazz) {
        return AccessI.INSTANCE.dexClassDefIndex(clazz);
    }

    public static int objectSizeField(Class<?> clazz) {
        return AccessI.INSTANCE.objectSize(clazz);
    }

    public static int classSizeField(Class<?> clazz) {
        return AccessI.INSTANCE.classSize(clazz);
    }

    public static int emptyClassSize() {
        class Holder {
            static final int size = classSizeField(void.class);
        }
        return Holder.size;
    }

    public static boolean shouldHaveEmbeddedVTableAndImt(Class<?> clazz) {
        return clazz.isArray()
                || !(clazz.isInterface()
                || clazz.isPrimitive()
                || Modifier.isAbstract(clazz.getModifiers()));
    }

    public static int getEmbeddedVTableLength(Class<?> clazz) {
        check(shouldHaveEmbeddedVTableAndImt(clazz), IllegalArgumentException::new);
        return getIntO(clazz, emptyClassSize());
    }

    private static final long IMTABLE_OFFSET = roundUpUL(emptyClassSize() + 4, ADDRESS_SIZE);

    public static long getIMTable(Class<?> clazz) {
        check(shouldHaveEmbeddedVTableAndImt(clazz), IllegalArgumentException::new);
        return getWordO(clazz, IMTABLE_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setIMTable(Class<?> clazz, long imtable) {
        check(shouldHaveEmbeddedVTableAndImt(clazz), IllegalArgumentException::new);
        putWordO(clazz, IMTABLE_OFFSET, imtable);
    }

    private static final long VTABLE_OFFSET = IMTABLE_OFFSET + ADDRESS_SIZE;

    public static long getEmbeddedVTableEntry(Class<?> clazz, int index) {
        Objects.checkIndex(index, getEmbeddedVTableLength(clazz));
        return getWordO(clazz, VTABLE_OFFSET + (long) index * ADDRESS_SIZE);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setEmbeddedVTableEntry(Class<?> clazz, int index, long art_method) {
        Objects.checkIndex(index, getEmbeddedVTableLength(clazz));
        putWordO(clazz, VTABLE_OFFSET + (long) index * ADDRESS_SIZE, art_method);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static /* PointerArray */ Object getVTable(Class<?> clazz) {
        return AccessI.INSTANCE.vtable(clazz);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setVTable(Class<?> clazz, Object /* PointerArray */ vtable) {
        AccessI.INSTANCE.vtable(clazz, vtable);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static /* IfTable */ Object[] getIFTable(Class<?> clazz) {
        return AccessI.INSTANCE.ifTable(clazz);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setIFTable(Class<?> clazz, Object[] /* IfTable */ ifTable) {
        AccessI.INSTANCE.ifTable(clazz, ifTable);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void copyTables(Class<?> from, Class<?> to) {
        int vtable_size = getEmbeddedVTableLength(to);
        for (int i = 0; i < vtable_size; i++) {
            setEmbeddedVTableEntry(to, i, getEmbeddedVTableEntry(from, i));
        }
        setVTable(to, getVTable(from));
        setIFTable(to, getIFTable(from));
        setIMTable(to, getIMTable(from));
    }

    public static boolean isCompressedString(String s) {
        var mirror = new StringMirror[1];
        fillArray(mirror, s);
        return StringMirror.COMPACT_STRINGS && ((mirror[0].count & 1) == 0);
    }

    public static int stringDataSize(String s) {
        return s.length() * (isCompressedString(s) ? 1 : 2);
    }

    public static int sizeOf(Object obj) {
        Objects.requireNonNull(obj);
        if (obj instanceof String sobj) {
            return roundUpU(STRING_HEADER_SIZE + stringDataSize(sobj), OBJECT_ALIGNMENT);
        }
        if (obj instanceof Class<?> cobj) {
            return classSizeField(cobj);
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return (int) arrayBaseOffset(clazz) + arrayIndexScale(clazz) * getArrayLength(obj);
        }
        return objectSizeField(clazz);
    }

    public static int alignedSizeOf(Object obj) {
        return roundUpU(sizeOf(obj), OBJECT_ALIGNMENT);
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static int rawObjectToInt(Object obj) {
        Object[] arr = new Object[1];
        arr[0] = obj;
        return getInt(arr, ARRAY_OBJECT_BASE_OFFSET);
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static void putObjectRaw(long address, Object value) {
        putIntN(address, rawObjectToInt(value));
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static Object rawIntToObject(int obj) {
        int[] arr = new int[1];
        arr[0] = obj;
        return getObject(arr, ARRAY_INT_BASE_OFFSET);
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static Object getObjectRaw(long address) {
        return rawIntToObject(getIntN(address));
    }

    public static boolean isPoisonReferences() {
        class Holder {
            static final boolean kPoisonReferences;

            static {
                Object test = newNonMovableArray(int.class, 0);
                long address = addressOfNonMovableArray(test);
                check(isSigned32Bit(address), AssertionError::new);
                int actual = (int) address;
                int raw = rawObjectToInt(test);
                if (actual == raw) {
                    kPoisonReferences = false;
                } else if (actual == -raw) {
                    kPoisonReferences = true;
                } else {
                    throw new AssertionError(
                            "unknown type of poisoning: actual:" + actual + " raw:" + raw);
                }
            }
        }
        return Holder.kPoisonReferences;
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static int objectToInt(Object obj) {
        int out = rawObjectToInt(obj);
        return isPoisonReferences() ? -out : out;
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static long objectToLong(Object obj) {
        return objectToInt(obj) & 0xffffffffL;
    }

    @DangerLevel(DangerLevel.ONLY_NONMOVABLE_OBJECTS)
    public static Object intToObject(int obj) {
        return rawIntToObject(isPoisonReferences() ? -obj : obj);
    }
}

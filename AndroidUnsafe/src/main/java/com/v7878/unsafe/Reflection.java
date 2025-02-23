package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Utils.check;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchConstructor;
import static com.v7878.unsafe.Utils.searchField;
import static com.v7878.unsafe.Utils.searchMethod;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class Reflection {
    static {
        // SDK version checks
        check(CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 36, AssertionError::new);
    }

    private static class Utils_16 {
        @DoNotShrink
        @DoNotObfuscate
        @ApiSensitive
        @SuppressWarnings("unused")
        private static class ClassMirror {
            public ClassLoader classLoader;
            public Class<?> componentType;
            public Object dexCache;
            public Object extData;
            public Object[] ifTable;
            public String name;
            public Class<?> superClass;
            public Object vtable;
            public long fields;
            public long methods;
            public int accessFlags;
            public int classFlags;
            public int classSize;
            public int clinitThreadId;
            public int dexClassDefIndex;
            public volatile int dexTypeIndex;
            public int numReferenceInstanceFields;
            public int numReferenceStaticFields;
            public int objectSize;
            public int objectSizeAllocFastPath;
            public int primitiveType;
            public int referenceInstanceOffsets;
            public int status;
            public short copiedMethodsOffset;
            public short virtualMethodsOffset;
        }

        public static long getMethods(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].methods;
        }

        public static int getCopiedMethodsOffset(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].copiedMethodsOffset;
        }

        public static int getVirtualMethodsOffset(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].virtualMethodsOffset;
        }

        public static long getFields(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].fields;
        }
    }

    private static class Utils_8_15 {
        @DoNotShrink
        @DoNotObfuscate
        @ApiSensitive
        @SuppressWarnings("unused")
        private static class ClassMirror {
            public ClassLoader classLoader;
            public Class<?> componentType;
            public Object dexCache;
            public Object extData;
            public Object[] ifTable;
            public String name;
            public Class<?> superClass;
            public Object vtable;
            public long iFields;
            public long methods;
            public long sFields;
            public int accessFlags;
            public int classFlags;
            public int classSize;
            public int clinitThreadId;
            public int dexClassDefIndex;
            public volatile int dexTypeIndex;
            public int numReferenceInstanceFields;
            public int numReferenceStaticFields;
            public int objectSize;
            public int objectSizeAllocFastPath;
            public int primitiveType;
            public int referenceInstanceOffsets;
            public int status;
            public short copiedMethodsOffset;
            public short virtualMethodsOffset;
        }

        public static long getMethods(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].methods;
        }

        public static int getCopiedMethodsOffset(Class<?> clazz) {
            Utils_16.ClassMirror[] mirror = arrayCast(Utils_16.ClassMirror.class, clazz);
            return mirror[0].copiedMethodsOffset;
        }

        public static int getVirtualMethodsOffset(Class<?> clazz) {
            Utils_16.ClassMirror[] mirror = arrayCast(Utils_16.ClassMirror.class, clazz);
            return mirror[0].virtualMethodsOffset;
        }

        public static long getStaticFields(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].sFields;
        }

        public static long getInstanceFields(Class<?> clazz) {
            ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
            return mirror[0].iFields;
        }
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    private static class AccessibleObjectMirror {
        public boolean override;
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    @SuppressWarnings("unused")
    private static class FieldMirror extends AccessibleObjectMirror {
        public int accessFlags;
        public Class<?> declaringClass;
        public int artFieldIndex;
        public int offset;
        public Class<?> type;
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    private static class ExecutableMirror extends AccessibleObjectMirror {
        public volatile boolean hasRealParameterData;
        @SuppressWarnings("VolatileArrayField")
        public volatile Parameter[] parameters;
        public int accessFlags;
        public long artMethod;
        public Class<?> declaringClass;
        public Class<?> declaringClassOfOverriddenMethod;
        public int dexMethodIndex;
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    @SuppressWarnings("unused")
    private static class MethodHandleMirror {
        public MethodType type;
        public Object different1;
        public Object different2;
        public int handleKind;
        public long artFieldOrMethod;
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    private static final class MethodHandleImplMirror extends MethodHandleMirror {
        public HandleInfoMirror info;
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    @SuppressWarnings("unused")
    private static final class HandleInfoMirror {
        public Member member;
        public MethodHandleImplMirror handle;
    }

    @DoNotShrink
    @DoNotObfuscate
    private static class Test {
        public static final Method am = nothrows_run(()
                -> Test.class.getDeclaredMethod("a"));
        public static final Method bm = nothrows_run(()
                -> Test.class.getDeclaredMethod("b"));

        public static final Field af = nothrows_run(()
                -> Test.class.getDeclaredField("sa"));
        public static final Field bf = nothrows_run(()
                -> Test.class.getDeclaredField("sb"));

        public static int sa, sb;

        @SuppressWarnings("EmptyMethod")
        public static void a() {
        }

        @SuppressWarnings("EmptyMethod")
        public static void b() {
        }
    }

    @SuppressWarnings("unchecked")
    private static final Class<MethodHandle> MethodHandleImplClass = nothrows_run(() ->
            (Class<MethodHandle>) Class.forName("java.lang.invoke.MethodHandleImpl"));

    public static final long ART_METHOD_SIZE;
    public static final long ART_METHOD_PADDING;
    public static final long ART_FIELD_SIZE;
    public static final long ART_FIELD_PADDING;

    private static final MethodHandle mGetArtField;

    static {
        final int length_field_size = 4;

        long am = getArtMethod(Test.am);
        long bm = getArtMethod(Test.bm);
        ART_METHOD_SIZE = Math.abs(bm - am);
        long methods = ART_SDK_INT >= 36 ? Utils_16.getMethods(Test.class)
                : Utils_8_15.getMethods(Test.class);
        ART_METHOD_PADDING = (am - methods - length_field_size)
                % ART_METHOD_SIZE + length_field_size;

        mGetArtField = unreflectDirect(getDeclaredVirtualMethod(Field.class, "getArtField"));

        long af = getArtField(Test.af);
        long bf = getArtField(Test.bf);
        ART_FIELD_SIZE = Math.abs(bf - af);
        long fields = ART_SDK_INT >= 36 ? Utils_16.getFields(Test.class)
                : Utils_8_15.getStaticFields(Test.class);
        ART_FIELD_PADDING = (af - fields - length_field_size)
                % ART_FIELD_SIZE + length_field_size;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    public static <T> T[] arrayCast(Class<T> clazz, Object... data) {
        assert !clazz.isPrimitive();
        //noinspection unchecked
        T[] out = (T[]) Array.newInstance(clazz, data.length);
        for (int i = 0; i < data.length; i++) {
            putObject(out, ARRAY_OBJECT_BASE_OFFSET +
                    (long) i * ARRAY_OBJECT_INDEX_SCALE, data[i]);
        }
        return out;
    }

    @AlwaysInline
    public static void setAccessible(AccessibleObject ao, boolean value) {
        if (ao.isAccessible()) return;
        AccessibleObjectMirror[] aob = arrayCast(AccessibleObjectMirror.class, ao);
        aob[0].override = value;
    }

    @AlwaysInline
    public static int fieldOffset(Field f) {
        FieldMirror[] fh = arrayCast(FieldMirror.class, f);
        return fh[0].offset;
    }

    @AlwaysInline
    public static long instanceFieldOffset(Field f) {
        check(!Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return fieldOffset(f);
    }

    @AlwaysInline
    public static long staticFieldOffset(Field f) {
        check(Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return fieldOffset(f);
    }

    @AlwaysInline
    public static Object staticFieldBase(Field f) {
        check(Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return f.getDeclaringClass();
    }

    @AlwaysInline
    public static long getArtMethod(Executable ex) {
        ExecutableMirror[] eh = arrayCast(ExecutableMirror.class, ex);
        return eh[0].artMethod;
    }

    @AlwaysInline
    public static long getArtField(Field f) {
        return nothrows_run(() -> (long) mGetArtField.invokeExact(f));
    }

    public static Executable toExecutable(long art_method) {
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        mhh[0].artFieldOrMethod = art_method;
        Executable tmp = MethodHandles.reflectAs(Executable.class, mh);
        setAccessible(tmp, true);
        return tmp;
    }

    public static Field toField(long art_field) {
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        mhh[0].artFieldOrMethod = art_field;
        mhh[0].handleKind = Integer.MAX_VALUE;
        Field tmp = MethodHandles.reflectAs(Field.class, mh);
        setAccessible(tmp, true);
        return tmp;
    }

    @SuppressWarnings("SameParameterValue")
    private static Field[] getFields0(long fields, int begin, int count) {
        Field[] out = new Field[count];
        if (out.length == 0) {
            return out;
        }
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        for (int i = 0; i < count; i++) {
            int index = begin + i;
            mhh[0].artFieldOrMethod = fields + ART_FIELD_PADDING + ART_FIELD_SIZE * index;
            mhh[0].info = null;
            mhh[0].handleKind = Integer.MAX_VALUE;
            Field tmp = MethodHandles.reflectAs(Field.class, mh);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
        return out;
    }

    public static Field[] getDeclaredInstanceFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            // TODO
            return Arrays.stream(getDeclaredFields(clazz))
                    .filter((field) -> !Modifier.isStatic(field.getModifiers()))
                    .toArray(Field[]::new);
        } else {
            long fields = Utils_8_15.getInstanceFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        }
    }

    public static Field getDeclaredInstanceField(Class<?> clazz, String name) {
        //TODO: improve performance
        return searchField(getDeclaredInstanceFields(clazz), name);
    }

    public static Field[] getDeclaredStaticFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            // TODO
            return Arrays.stream(getDeclaredFields(clazz))
                    .filter((field) -> Modifier.isStatic(field.getModifiers()))
                    .toArray(Field[]::new);
        } else {
            long fields = Utils_8_15.getStaticFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        }
    }

    public static Field getDeclaredStaticField(Class<?> clazz, String name) {
        //TODO: improve performance
        return searchField(getDeclaredStaticFields(clazz), name);
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            long fields = Utils_16.getFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        } else {
            Field[] out1 = getDeclaredInstanceFields(clazz);
            Field[] out2 = getDeclaredStaticFields(clazz);
            Field[] out = new Field[out1.length + out2.length];
            System.arraycopy(out1, 0, out, 0, out1.length);
            System.arraycopy(out2, 0, out, out1.length, out2.length);
            return out;
        }
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        //TODO: improve performance
        return searchField(getDeclaredFields(clazz), name);
    }

    private static void fillExecutables0(long methods, int begin, Executable[] out) {
        if (out.length == 0) {
            return;
        }
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        for (int i = 0; i < out.length; i++) {
            int index = begin + i;
            mhh[0].artFieldOrMethod = methods + ART_METHOD_PADDING + ART_METHOD_SIZE * index;
            mhh[0].info = null;
            Executable tmp = MethodHandles.reflectAs(Executable.class, mh);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
    }

    public static Executable[] getDeclaredExecutables(Class<?> clazz) {
        long methods = ART_SDK_INT >= 36 ? Utils_16.getMethods(clazz)
                : Utils_8_15.getMethods(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = ART_SDK_INT >= 36 ? Utils_16.getCopiedMethodsOffset(clazz)
                : Utils_8_15.getCopiedMethodsOffset(clazz);
        var out = new Executable[count];
        fillExecutables0(methods, 0, out);
        return out;
    }

    public static Executable[] getDeclaredDirectExecutables(Class<?> clazz) {
        long methods = ART_SDK_INT >= 36 ? Utils_16.getMethods(clazz)
                : Utils_8_15.getMethods(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = ART_SDK_INT >= 36 ? Utils_16.getVirtualMethodsOffset(clazz)
                : Utils_8_15.getVirtualMethodsOffset(clazz);
        var out = new Executable[count];
        fillExecutables0(methods, 0, out);
        return out;
    }

    // Note: only methods can be virtual
    public static Method[] getDeclaredVirtualMethods(Class<?> clazz) {
        long methods = ART_SDK_INT >= 36 ? Utils_16.getMethods(clazz)
                : Utils_8_15.getMethods(clazz);
        if (methods == 0) {
            return new Method[0];
        }
        int begin = ART_SDK_INT >= 36 ? Utils_16.getVirtualMethodsOffset(clazz)
                : Utils_8_15.getVirtualMethodsOffset(clazz);
        int end = ART_SDK_INT >= 36 ? Utils_16.getCopiedMethodsOffset(clazz)
                : Utils_8_15.getCopiedMethodsOffset(clazz);
        var out = new Method[end - begin];
        fillExecutables0(methods, begin, out);
        return out;
    }

    public static Method getDeclaredVirtualMethod(Class<?> clazz, String name, Class<?>... params) {
        return searchMethod(getDeclaredVirtualMethods(clazz), name, params);
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return Arrays.stream(getDeclaredExecutables(clazz))
                .filter((exec) -> exec instanceof Method)
                .toArray(Method[]::new);
    }

    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        //TODO: improve performance
        return searchMethod(getDeclaredMethods(clazz), name, params);
    }

    public static <T> Constructor<T>[] getDeclaredConstructors(Class<T> clazz) {
        //noinspection SuspiciousToArrayCall,unchecked
        return Arrays.stream(getDeclaredDirectExecutables(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && !Modifier.isStatic(exec.getModifiers()))
                .toArray(Constructor[]::new);
    }

    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... params) {
        //TODO: improve performance
        return searchConstructor(getDeclaredConstructors(clazz), params);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Method constructorToMethod(Constructor<?> ct) {
        Method out = allocateInstance(Method.class);
        ExecutableMirror[] eb = arrayCast(ExecutableMirror.class, ct, out);

        eb[1].override = eb[0].override;

        eb[1].hasRealParameterData = eb[0].hasRealParameterData;
        eb[1].parameters = eb[0].parameters;
        eb[1].accessFlags = eb[0].accessFlags;
        eb[1].artMethod = eb[0].artMethod;
        eb[1].declaringClass = eb[0].declaringClass;
        eb[1].declaringClassOfOverriddenMethod = eb[0].declaringClassOfOverriddenMethod;
        eb[1].dexMethodIndex = eb[0].dexMethodIndex;

        return out;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Method getStaticConstructor(Class<?> clazz) {
        var value = Arrays.stream(getDeclaredDirectExecutables(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && Modifier.isStatic(exec.getModifiers()))
                .findAny().orElse(null);
        if (value == null) {
            return null;
        }
        return constructorToMethod((Constructor<?>) value);
    }

    @AlwaysInline
    public static MethodHandle unreflect(Method m) {
        setAccessible(m, true);
        return nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));
    }

    @AlwaysInline
    public static MethodHandle unreflect(Constructor<?> c) {
        setAccessible(c, true);
        return nothrows_run(() -> MethodHandles.publicLookup().unreflectConstructor(c));
    }

    public static MethodHandle unreflectDirect(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("Only non-static and non-abstract methods allowed");
        }

        MethodHandle out = unreflect(m);
        setMethodHandleKind(out, /*INVOKE_DIRECT*/ 2);

        return out;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    // TODO: move to InvokeAccess
    public static void setMethodType(MethodHandle handle, MethodType type) {
        // TODO: android 8-12L nominalType
        MethodHandleMirror[] mirror = arrayCast(MethodHandleMirror.class, handle);
        mirror[0].type = type;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    // TODO: move to InvokeAccess
    public static void setMethodHandleKind(MethodHandle handle, int kind) {
        MethodHandleMirror[] mirror = arrayCast(MethodHandleMirror.class, handle);
        mirror[0].handleKind = kind;
    }
}

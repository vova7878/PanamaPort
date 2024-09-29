package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_OBJECT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Reflection {
    static {
        // SDK version checks
        check(CORRECT_SDK_INT >= 26 && CORRECT_SDK_INT <= 35, AssertionError::new);
    }

    @DoNotShrink
    @DoNotObfuscate
    @ApiSensitive
    @SuppressWarnings("unused")
    static class ClassMirror {
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

    public static final int ART_METHOD_SIZE;
    public static final int ART_METHOD_PADDING;
    public static final int ART_FIELD_SIZE;
    public static final int ART_FIELD_PADDING;

    private static final MethodHandle mGetArtField;

    static {
        ClassMirror[] tm = arrayCast(ClassMirror.class, Test.class);

        final int length_field_size = 4;

        long am = getArtMethod(Test.am);
        long bm = getArtMethod(Test.bm);
        ART_METHOD_SIZE = (int) Math.abs(bm - am);
        ART_METHOD_PADDING = (int) (am - tm[0].methods - length_field_size)
                % ART_METHOD_SIZE + length_field_size;

        mGetArtField = unreflectDirect(getDeclaredMethod(Field.class, "getArtField"));

        long af = getArtField(Test.af);
        long bf = getArtField(Test.bf);
        ART_FIELD_SIZE = (int) Math.abs(bf - af);
        ART_FIELD_PADDING = (int) (af - tm[0].sFields - length_field_size)
                % ART_FIELD_SIZE + length_field_size;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    public static <T> T[] arrayCast(Class<T> clazz, Object... data) {
        assert !clazz.isPrimitive();
        //noinspection unchecked
        T[] out = (T[]) Array.newInstance(clazz, data.length);
        for (int i = 0; i < data.length; i++) {
            putObject(out, ARRAY_OBJECT_BASE_OFFSET + (long) i * ARRAY_OBJECT_INDEX_SCALE, data[i]);
        }
        return out;
    }

    public static void setAccessible(AccessibleObject ao, boolean value) {
        AccessibleObjectMirror[] aob = arrayCast(AccessibleObjectMirror.class, ao);
        aob[0].override = value;
    }

    public static int fieldOffset(Field f) {
        FieldMirror[] fh = arrayCast(FieldMirror.class, f);
        return fh[0].offset;
    }

    public static long instanceFieldOffset(Field f) {
        check(!Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return fieldOffset(f);
    }

    public static long staticFieldOffset(Field f) {
        check(Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return fieldOffset(f);
    }

    public static Object staticFieldBase(Field f) {
        check(Modifier.isStatic(f.getModifiers()), IllegalArgumentException::new);
        return f.getDeclaringClass();
    }

    public static long getArtMethod(Executable ex) {
        ExecutableMirror[] eh = arrayCast(ExecutableMirror.class, ex);
        return eh[0].artMethod;
    }

    public static long getArtField(Field f) {
        return (long) nothrows_run(() -> mGetArtField.invoke(f));
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

    public static Executable[] getDeclaredExecutables0(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        ClassMirror[] clh = arrayCast(ClassMirror.class, clazz);
        long methods = clh[0].methods;
        if (methods == 0) {
            return new Executable[0];
        }
        int col = getIntN(methods);
        Executable[] out = new Executable[col];
        if (out.length == 0) {
            return out;
        }
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        for (int i = 0; i < col; i++) {
            mhh[0].artFieldOrMethod = methods + ART_METHOD_PADDING + (long) ART_METHOD_SIZE * i;
            mhh[0].info = null;
            Executable tmp = MethodHandles.reflectAs(Executable.class, mh);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
        return out;
    }

    public static Field[] getDeclaredFields0(Class<?> clazz, boolean s) {
        Objects.requireNonNull(clazz);
        ClassMirror[] clh = arrayCast(ClassMirror.class, clazz);
        long fields = s ? clh[0].sFields : clh[0].iFields;
        if (fields == 0) {
            return new Field[0];
        }
        int col = getIntN(fields);
        Field[] out = new Field[col];
        if (out.length == 0) {
            return out;
        }
        MethodHandle mh = allocateInstance(MethodHandleImplClass);
        MethodHandleImplMirror[] mhh = arrayCast(MethodHandleImplMirror.class, mh);
        for (int i = 0; i < col; i++) {
            mhh[0].artFieldOrMethod = fields + ART_FIELD_PADDING + (long) ART_FIELD_SIZE * i;
            mhh[0].info = null;
            mhh[0].handleKind = Integer.MAX_VALUE;
            Field tmp = MethodHandles.reflectAs(Field.class, mh);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
        return out;
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        Field[] out1 = getDeclaredFields0(clazz, true);
        Field[] out2 = getDeclaredFields0(clazz, false);
        Field[] out = new Field[out1.length + out2.length];
        System.arraycopy(out1, 0, out, 0, out1.length);
        System.arraycopy(out2, 0, out, out1.length, out2.length);
        return out;
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return Arrays.stream(getDeclaredExecutables0(clazz))
                .filter((exec) -> exec instanceof Method)
                .toArray(Method[]::new);
    }

    public static <T> Constructor<T>[] getDeclaredConstructors(Class<T> clazz) {
        //noinspection SuspiciousToArrayCall,unchecked
        return Arrays.stream(getDeclaredExecutables0(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && !Modifier.isStatic(exec.getModifiers()))
                .toArray(Constructor[]::new);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Method convertConstructorToMethod(Constructor<?> ct) {
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
    public static Method getDeclaredStaticConstructor(Class<?> clazz) {
        //noinspection SuspiciousToArrayCall,rawtypes
        Constructor[] out = Arrays.stream(getDeclaredExecutables0(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && Modifier.isStatic(exec.getModifiers()))
                .toArray(Constructor[]::new);
        if (out.length == 0) {
            return null;
        }
        check(out.length == 1, IllegalStateException::new);
        return convertConstructorToMethod(out[0]);
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        return searchField(getDeclaredFields(clazz), name, true);
    }

    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        return searchMethod(getDeclaredMethods(clazz), name, true, params);
    }

    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... params) {
        return searchConstructor(getDeclaredConstructors(clazz), true, params);
    }

    public static Field[] getFields(Class<?> c) {
        ArrayList<Field> out = new ArrayList<>();
        while (c != null) {
            Field[] af = getDeclaredFields(c);
            out.addAll(Arrays.asList(af));
            c = c.getSuperclass();
        }
        return out.toArray(new Field[0]);
    }

    public static Field[] getInstanceFields(Class<?> c) {
        ArrayList<Field> out = new ArrayList<>();
        while (c != null) {
            Field[] af = getDeclaredFields0(c, false);
            out.addAll(Arrays.asList(af));
            c = c.getSuperclass();
        }
        return out.toArray(new Field[0]);
    }

    public static Method[] getMethods(Class<?> c) {
        ArrayList<Method> out = new ArrayList<>();
        while (c != null) {
            Method[] af = getDeclaredMethods(c);
            out.addAll(Arrays.asList(af));
            c = c.getSuperclass();
        }
        return out.toArray(new Method[0]);
    }

    public static MethodHandle unreflect(Method m) {
        setAccessible(m, true);
        return nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));
    }

    public static MethodHandle unreflect(Constructor<?> c) {
        setAccessible(c, true);
        return nothrows_run(() -> MethodHandles.publicLookup().unreflectConstructor(c));
    }

    public static MethodHandle unreflectDirect(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("only non-static and non-abstract methods allowed");
        }

        setAccessible(m, true);
        MethodHandle out = nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));

        setMethodHandleKind(out, /*INVOKE_DIRECT*/ 2);

        return out;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setMethodType(MethodHandle handle, MethodType type) {
        Objects.requireNonNull(handle);
        Objects.requireNonNull(type);
        MethodHandleMirror[] mirror = arrayCast(MethodHandleMirror.class, handle);
        mirror[0].type = type;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setMethodHandleKind(MethodHandle handle, int kind) {
        Objects.requireNonNull(handle);
        MethodHandleMirror[] mirror = arrayCast(MethodHandleMirror.class, handle);
        mirror[0].handleKind = kind;
    }
}

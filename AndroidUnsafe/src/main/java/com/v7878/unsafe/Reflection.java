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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

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
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].methods;
        }

        public static int getCopiedMethodsOffset(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].copiedMethodsOffset & 0xffff;
        }

        public static int getVirtualMethodsOffset(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].virtualMethodsOffset & 0xffff;
        }

        public static long getFields(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
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
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].methods;
        }

        public static int getCopiedMethodsOffset(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].copiedMethodsOffset & 0xffff;
        }

        public static int getVirtualMethodsOffset(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].virtualMethodsOffset & 0xffff;
        }

        public static long getStaticFields(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].sFields;
        }

        public static long getInstanceFields(Class<?> clazz) {
            var mirror = new ClassMirror[1];
            fillArray(mirror, clazz);
            return mirror[0].iFields;
        }
    }

    @AlwaysInline
    private static long getMethodsPtr(Class<?> clazz) {
        return ART_SDK_INT >= 36 ? Utils_16.getMethods(clazz)
                : Utils_8_15.getMethods(clazz);
    }

    @AlwaysInline
    private static int getCopiedMethodsOffset(Class<?> clazz) {
        return ART_SDK_INT >= 36 ? Utils_16.getCopiedMethodsOffset(clazz)
                : Utils_8_15.getCopiedMethodsOffset(clazz);
    }

    @AlwaysInline
    private static int getVirtualMethodsOffset(Class<?> clazz) {
        return ART_SDK_INT >= 36 ? Utils_16.getVirtualMethodsOffset(clazz)
                : Utils_8_15.getVirtualMethodsOffset(clazz);
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

    private static final MethodHandle getArtField;

    static {
        final int length_field_size = 4;

        long am = getArtMethod(Test.am);
        long bm = getArtMethod(Test.bm);
        ART_METHOD_SIZE = Math.abs(bm - am);
        long methods = getMethodsPtr(Test.class);
        ART_METHOD_PADDING = (am - methods - length_field_size)
                % ART_METHOD_SIZE + length_field_size;

        getArtField = unreflectDirect(getHiddenVirtualMethod(
                Field.class, "getArtField"));

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
    public static void fillArray(Object[] array, Object... data) {
        assert array != null;
        assert array.length >= data.length;
        for (int i = 0; i < data.length; i++) {
            putObject(array, ARRAY_OBJECT_BASE_OFFSET +
                    (long) i * ARRAY_OBJECT_INDEX_SCALE, data[i]);
        }
    }

    @AlwaysInline
    public static void setAccessible(AccessibleObject ao, boolean value) {
        if (ao.isAccessible()) return;
        var aob = new AccessibleObjectMirror[1];
        fillArray(aob, ao);
        aob[0].override = value;
    }

    @AlwaysInline
    public static int fieldOffset(Field f) {
        var mirror = new FieldMirror[1];
        fillArray(mirror, f);
        return mirror[0].offset;
    }

    @AlwaysInline
    public static long getArtMethod(Executable ex) {
        var mirror = new ExecutableMirror[1];
        fillArray(mirror, ex);
        return mirror[0].artMethod;
    }

    @AlwaysInline
    public static long getArtField(Field f) {
        return nothrows_run(() -> (long) getArtField.invokeExact(f));
    }

    public static Executable toExecutable(long art_method) {
        MethodHandle impl = allocateInstance(MethodHandleImplClass);
        var mirror = new MethodHandleImplMirror[1];
        fillArray(mirror, impl);
        mirror[0].artFieldOrMethod = art_method;
        Executable tmp = MethodHandles.reflectAs(Executable.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    public static Field toField(long art_field) {
        MethodHandle impl = allocateInstance(MethodHandleImplClass);
        var mirror = new MethodHandleImplMirror[1];
        fillArray(mirror, impl);
        mirror[0].artFieldOrMethod = art_field;
        mirror[0].handleKind = Integer.MAX_VALUE;
        Field tmp = MethodHandles.reflectAs(Field.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    @SuppressWarnings("SameParameterValue")
    private static Field[] getFields0(long fields, int begin, int count) {
        Field[] out = new Field[count];
        if (out.length == 0) {
            return out;
        }
        MethodHandle impl = allocateInstance(MethodHandleImplClass);
        var mirror = new MethodHandleImplMirror[1];
        fillArray(mirror, impl);
        for (int i = 0; i < count; i++) {
            int index = begin + i;
            mirror[0].artFieldOrMethod = fields + ART_FIELD_PADDING + ART_FIELD_SIZE * index;
            mirror[0].info = null;
            mirror[0].handleKind = Integer.MAX_VALUE;
            Field tmp = MethodHandles.reflectAs(Field.class, impl);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
        return out;
    }

    public static Field[] getHiddenInstanceFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            // TODO
            return Arrays.stream(getHiddenFields(clazz))
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

    public static Field getHiddenInstanceField(Class<?> clazz, String name) {
        return searchField(getHiddenInstanceFields(clazz), name);
    }

    public static Field[] getHiddenStaticFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            // TODO
            return Arrays.stream(getHiddenFields(clazz))
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

    public static Field gethiddenStaticField(Class<?> clazz, String name) {
        return searchField(getHiddenStaticFields(clazz), name);
    }

    public static Field[] getHiddenFields(Class<?> clazz) {
        if (ART_SDK_INT >= 36) {
            long fields = Utils_16.getFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        } else {
            Field[] out1 = getHiddenInstanceFields(clazz);
            Field[] out2 = getHiddenStaticFields(clazz);
            Field[] out = new Field[out1.length + out2.length];
            System.arraycopy(out1, 0, out, 0, out1.length);
            System.arraycopy(out2, 0, out, out1.length, out2.length);
            return out;
        }
    }

    public static Field getHiddenField(Class<?> clazz, String name) {
        return searchField(getHiddenFields(clazz), name);
    }

    private static void fillExecutables0(long methods, int begin, Executable[] out) {
        if (out.length == 0) {
            return;
        }
        MethodHandle impl = allocateInstance(MethodHandleImplClass);
        var mirror = new MethodHandleImplMirror[1];
        fillArray(mirror, impl);
        for (int i = 0; i < out.length; i++) {
            int index = begin + i;
            mirror[0].artFieldOrMethod = methods + ART_METHOD_PADDING + ART_METHOD_SIZE * index;
            mirror[0].info = null;
            Executable tmp = MethodHandles.reflectAs(Executable.class, impl);
            setAccessible(tmp, true);
            out[i] = tmp;
        }
    }

    public static Executable[] getHiddenExecutables(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = getCopiedMethodsOffset(clazz);
        var out = new Executable[count];
        fillExecutables0(methods, 0, out);
        return out;
    }

    public static Executable[] getHiddenDirectExecutables(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = getVirtualMethodsOffset(clazz);
        var out = new Executable[count];
        fillExecutables0(methods, 0, out);
        return out;
    }

    // Note: only methods can be virtual
    public static Method[] getHiddenVirtualMethods(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Method[0];
        }
        int begin = getVirtualMethodsOffset(clazz);
        int end = getCopiedMethodsOffset(clazz);
        var out = new Method[end - begin];
        fillExecutables0(methods, begin, out);
        return out;
    }

    public static Method getHiddenVirtualMethod(Class<?> clazz, String name, Class<?>... params) {
        return searchMethod(getHiddenVirtualMethods(clazz), name, params);
    }

    public static Method[] getHiddenMethods(Class<?> clazz) {
        return Arrays.stream(getHiddenExecutables(clazz))
                .filter((exec) -> exec instanceof Method)
                .toArray(Method[]::new);
    }

    public static Method getHiddenMethod(Class<?> clazz, String name, Class<?>... params) {
        return searchMethod(getHiddenMethods(clazz), name, params);
    }

    public static <T> Constructor<T>[] getHiddenConstructors(Class<T> clazz) {
        //noinspection SuspiciousToArrayCall,unchecked
        return Arrays.stream(getHiddenDirectExecutables(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && !Modifier.isStatic(exec.getModifiers()))
                .toArray(Constructor[]::new);
    }

    public static <T> Constructor<T> getHiddenConstructor(Class<T> clazz, Class<?>... params) {
        return searchConstructor(getHiddenConstructors(clazz), params);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Method constructorToMethod(Constructor<?> constructor) {
        Method method = allocateInstance(Method.class);
        var mirror = new ExecutableMirror[2];
        fillArray(mirror, constructor, method);

        mirror[1].override = mirror[0].override;

        mirror[1].hasRealParameterData = mirror[0].hasRealParameterData;
        mirror[1].parameters = mirror[0].parameters;
        mirror[1].accessFlags = mirror[0].accessFlags;
        mirror[1].artMethod = mirror[0].artMethod;
        mirror[1].declaringClass = mirror[0].declaringClass;
        mirror[1].declaringClassOfOverriddenMethod = mirror[0].declaringClassOfOverriddenMethod;
        mirror[1].dexMethodIndex = mirror[0].dexMethodIndex;

        return method;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Method getStaticConstructor(Class<?> clazz) {
        var value = Arrays.stream(getHiddenDirectExecutables(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && Modifier.isStatic(exec.getModifiers()))
                .findAny().orElse(null);
        if (value == null) {
            return null;
        }
        return constructorToMethod((Constructor<?>) value);
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        try {
            var out = clazz.getDeclaredMethods();
            Stream.of(out).forEach(value -> setAccessible(value, true));
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            var out = clazz.getDeclaredMethod(name, params);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
        try {
            var out = clazz.getDeclaredConstructors();
            Stream.of(out).forEach(value -> setAccessible(value, true));
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... params) {
        try {
            var out = clazz.getDeclaredConstructor(params);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        try {
            var out = clazz.getDeclaredFields();
            Stream.of(out).forEach(value -> setAccessible(value, true));
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            var out = clazz.getDeclaredField(name);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static void initHandle(MethodHandle handle) {
        Objects.requireNonNull(handle);
        try {
            MethodHandles.publicLookup().revealDirect(handle);
        } catch (Throwable th) {
            // ignore
        }
    }

    @AlwaysInline
    public static MethodHandle unreflect(Method m) {
        setAccessible(m, true);
        var handle = nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));
        initHandle(handle);
        return handle;
    }

    @AlwaysInline
    public static MethodHandle unreflect(Constructor<?> c) {
        setAccessible(c, true);
        var handle = nothrows_run(() -> MethodHandles.publicLookup().unreflectConstructor(c));
        initHandle(handle);
        return handle;
    }

    public static MethodHandle unreflectDirect(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("Only non-static and non-abstract methods allowed");
        }

        MethodHandle out = unreflect(m);
        setMethodHandleKind(out, /*INVOKE_DIRECT*/ 2);

        initHandle(out);
        return out;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    // TODO: move to InvokeAccess
    public static void setMethodType(MethodHandle handle, MethodType type) {
        // TODO: android 8-12L nominalType
        var mirror = new MethodHandleMirror[1];
        fillArray(mirror, handle);
        mirror[0].type = type;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @AlwaysInline
    // TODO: move to InvokeAccess
    public static void setMethodHandleKind(MethodHandle handle, int kind) {
        var mirror = new MethodHandleMirror[1];
        fillArray(mirror, handle);
        mirror[0].handleKind = kind;
    }
}

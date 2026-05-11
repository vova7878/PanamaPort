package com.v7878.unsafe;

import static com.v7878.dex.DexConstants.ACC_DIRECT_MASK;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.ArtModifiers.kAccCopied;
import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A11;
import static com.v7878.unsafe.ArtVersion.A12;
import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.A16;
import static com.v7878.unsafe.ArtVersion.A16p1;
import static com.v7878.unsafe.ArtVersion.A17;
import static com.v7878.unsafe.ArtVersion.A8p0;
import static com.v7878.unsafe.ArtVersion.A8p1;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Utils.check;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchConstructor;
import static com.v7878.unsafe.Utils.searchField;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.unsupportedART;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;
import static com.v7878.unsafe.misc.Math.ulong;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

@ApiSensitive
public class Reflection {
    static {
        check(ART_INDEX >= A8p0 && ART_INDEX <= A17, AssertionError::new);
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class Class_16p1_17 {
        @AlwaysInline
        public static long getFields(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 40);
        }

        @AlwaysInline
        public static long getMethods(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 48);
        }
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class Class_16 {
        @AlwaysInline
        public static long getFields(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 40);
        }

        @AlwaysInline
        public static long getMethods(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 48);
        }

        @AlwaysInline
        public static int getCopiedMethodsOffset(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getShortO(clazz, 108) & 0xffff;
        }

        @AlwaysInline
        public static int getVirtualMethodsOffset(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getShortO(clazz, 110) & 0xffff;
        }
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class Class_8_15 {
        @AlwaysInline
        public static long getInstanceFields(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 40);
        }

        @AlwaysInline
        public static long getStaticFields(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 56);
        }

        @AlwaysInline
        public static long getMethods(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getLongO(clazz, 48);
        }

        @AlwaysInline
        public static int getCopiedMethodsOffset(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getShortO(clazz, 116) & 0xffff;
        }

        @AlwaysInline
        public static int getVirtualMethodsOffset(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return AndroidUnsafe.getShortO(clazz, 118) & 0xffff;
        }
    }

    @AlwaysInline
    private static long getMethodsPtr(Class<?> clazz) {
        return ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ?
                Class_16p1_17.getMethods(clazz) :
                Class_16.getMethods(clazz)) :
                Class_8_15.getMethods(clazz);
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class _MethodHandle {
        @AlwaysInline
        public static void setArt(MethodHandle mh, long art) {
            Objects.requireNonNull(mh);
            AndroidUnsafe.putLongO(mh, 24, art);
        }

        @AlwaysInline
        public static void setKind(MethodHandle mh, int kind) {
            Objects.requireNonNull(mh);
            AndroidUnsafe.putIntO(mh, 20, kind);
        }

        @AlwaysInline
        public static void setInfo(MethodHandle mh, Object info) {
            Objects.requireNonNull(mh);
            AndroidUnsafe.putObject(mh, 32, info);
        }
    }

    public static final long ART_METHOD_SIZE;
    public static final long ART_METHOD_PADDING;
    public static final long ART_FIELD_SIZE;
    public static final long ART_FIELD_PADDING;

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @ExecutableAccess(kind = VIRTUAL,
                klass = "java.lang.reflect.Field",
                name = "getArtField", args = {})
        abstract long getArtField(Field instance);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    static {
        ART_METHOD_SIZE = switch (ART_INDEX) {
            case A17, A16p1, A16, A15, A14, A13, A12 -> IS64BIT ? 32 : 24;
            case A11, A10, A9 -> IS64BIT ? 40 : 28;
            case A8p1, A8p0 -> IS64BIT ? 48 : 32;
            default -> throw unsupportedART(ART_INDEX);
        };
        ART_METHOD_PADDING = IS64BIT ? 8 : 4;

        ART_FIELD_SIZE = 16;
        ART_FIELD_PADDING = 4;
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static void setAccessible(AccessibleObject ao, boolean value) {
        if (ao.isAccessible()) return;
        AndroidUnsafe.putBooleanO(ao, 8, value);
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static long fieldOffset(Field f) {
        Objects.requireNonNull(f);
        return ulong(AndroidUnsafe.getIntO(f, 28));
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static long getArtMethod(Executable ex) {
        Objects.requireNonNull(ex);
        return AndroidUnsafe.getLongO(ex, 24);
    }

    @AlwaysInline
    public static long getArtField(Field f) {
        return AccessI.INSTANCE.getArtField(f);
    }

    private static class Holder {
        @SuppressWarnings("unchecked")
        static final Class<MethodHandle> MH_IMPL = (Class<MethodHandle>)
                ClassUtils.sysClass("java.lang.invoke.MethodHandleImpl");
    }

    public static Executable toExecutable(long art_method) {
        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setArt(impl, art_method);
        Executable tmp = MethodHandles.reflectAs(Executable.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    public static Field toField(long art_field) {
        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setArt(impl, art_field);
        _MethodHandle.setKind(impl, Integer.MAX_VALUE);
        Field tmp = MethodHandles.reflectAs(Field.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    private static Field[] getFields0(long fields, int begin, int count, IntPredicate filter) {
        Field[] out = new Field[count];
        if (out.length == 0) {
            return out;
        }

        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setKind(impl, Integer.MAX_VALUE);

        int array_count = 0;
        for (int i = 0; i < count; i++) {
            int index = begin + i;
            long art_field = fields + ART_FIELD_PADDING + ART_FIELD_SIZE * index;
            if (!filter.test(ArtFieldUtils.getFieldFlags(art_field))) {
                continue;
            }
            _MethodHandle.setArt(impl, art_field);
            _MethodHandle.setInfo(impl, null);
            Field tmp = MethodHandles.reflectAs(Field.class, impl);
            setAccessible(tmp, true);
            out[array_count++] = tmp;
        }
        return Arrays.copyOf(out, array_count);
    }

    @SuppressWarnings("SameParameterValue")
    @AlwaysInline
    private static Field[] getFields0(long fields, int begin, int count) {
        return getFields0(fields, begin, count, unused -> true);
    }

    public static Field[] getHiddenInstanceFields(Class<?> clazz) {
        if (ART_INDEX >= A16) {
            long fields = ART_INDEX >= A16p1 ? Class_16p1_17.getFields(clazz) : Class_16.getFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count,
                    flags -> !Modifier.isStatic(flags));
        } else {
            long fields = Class_8_15.getInstanceFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        }
    }

    public static Field getHiddenInstanceField(Class<?> clazz, String name) {
        // TODO: use binary search as fields are in alphabetical order
        return searchField(getHiddenInstanceFields(clazz), name);
    }

    public static long instanceFieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getHiddenInstanceField(clazz, name));
    }

    public static Field[] getHiddenStaticFields(Class<?> clazz) {
        if (ART_INDEX >= A16) {
            long fields = ART_INDEX >= A16p1 ? Class_16p1_17.getFields(clazz) : Class_16.getFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count, Modifier::isStatic);
        } else {
            long fields = Class_8_15.getStaticFields(clazz);
            if (fields == 0) {
                return new Field[0];
            }
            int count = getIntN(fields);
            return getFields0(fields, 0, count);
        }
    }

    public static Field getHiddenStaticField(Class<?> clazz, String name) {
        // TODO: use binary search as fields are in alphabetical order
        return searchField(getHiddenStaticFields(clazz), name);
    }

    public static long staticFieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getHiddenStaticField(clazz, name));
    }

    public static Field[] getHiddenFields(Class<?> clazz) {
        if (ART_INDEX >= A16) {
            long fields = ART_INDEX >= A16p1 ? Class_16p1_17.getFields(clazz) : Class_16.getFields(clazz);
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
        // TODO?: use binary search as fields are in alphabetical order
        return searchField(getHiddenFields(clazz), name);
    }

    public static long fieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getHiddenField(clazz, name));
    }

    // The order is same as in the dex file. There is no such thing for fields
    public static long[] getArtMethods(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new long[0];
        }
        int count = ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ?
                getIntN(methods) :
                Class_16.getCopiedMethodsOffset(clazz)) :
                Class_8_15.getCopiedMethodsOffset(clazz);
        var out = new long[count];
        int array_count = 0;
        for (int i = 0; i < count; i++) {
            long art_method = methods + ART_METHOD_PADDING + ART_METHOD_SIZE * i;
            if ((ArtMethodUtils.getExecutableFlags(art_method) & kAccCopied) != 0) {
                continue;
            }
            out[array_count++] = art_method;
        }
        return Arrays.copyOf(out, array_count);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Executable> T[] fillExecutables(
            T[] out, long methods, int begin, IntPredicate filter) {
        if (out.length == 0) {
            return out;
        }

        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setKind(impl, 0);

        int array_count = 0;
        for (int i = 0; i < out.length; i++) {
            int index = begin + i;
            long art_method = methods + ART_METHOD_PADDING + ART_METHOD_SIZE * index;
            if (!filter.test(ArtMethodUtils.getExecutableFlags(art_method))) {
                continue;
            }
            _MethodHandle.setArt(impl, art_method);
            _MethodHandle.setInfo(impl, null);
            Executable tmp = MethodHandles.reflectAs(Executable.class, impl);
            setAccessible(tmp, true);
            out[array_count++] = (T) tmp;
        }
        return Arrays.copyOf(out, array_count);
    }

    private static <T extends Executable> T[] fillExecutables(
            T[] out, long methods, int begin) {
        return fillExecutables(out, methods, begin, unused -> true);
    }

    public static Executable[] getHiddenExecutables(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ?
                getIntN(methods) :
                Class_16.getCopiedMethodsOffset(clazz)) :
                Class_8_15.getCopiedMethodsOffset(clazz);
        var out = new Executable[count];
        if (ART_INDEX >= A16p1) {
            return fillExecutables(out, methods, 0, flags -> (flags & kAccCopied) == 0);
        }
        return fillExecutables(out, methods, 0);
    }

    public static Executable[] getHiddenDirectExecutables(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Executable[0];
        }
        int count = ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ?
                getIntN(methods) :
                Class_16.getVirtualMethodsOffset(clazz)) :
                Class_8_15.getVirtualMethodsOffset(clazz);
        var out = new Executable[count];
        if (ART_INDEX >= A16p1) {
            return fillExecutables(out, methods, 0, flags -> {
                if ((flags & kAccCopied) != 0) {
                    return false;
                }
                return (flags & ACC_DIRECT_MASK) != 0;
            });
        }
        return fillExecutables(out, methods, 0);
    }

    // Note: only methods can be virtual
    public static Method[] getHiddenVirtualMethods(Class<?> clazz) {
        long methods = getMethodsPtr(clazz);
        if (methods == 0) {
            return new Method[0];
        }
        int begin = ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ? 0 :
                Class_16.getVirtualMethodsOffset(clazz)) :
                Class_8_15.getVirtualMethodsOffset(clazz);
        int end = ART_INDEX >= A16 ? (ART_INDEX >= A16p1 ?
                getIntN(methods) :
                Class_16.getCopiedMethodsOffset(clazz)) :
                Class_8_15.getCopiedMethodsOffset(clazz);
        var out = new Method[end - begin];
        if (ART_INDEX >= A16p1) {
            return fillExecutables(out, methods, begin, flags -> {
                if ((flags & kAccCopied) != 0) {
                    return false;
                }
                return (flags & ACC_DIRECT_MASK) == 0;
            });
        }
        return fillExecutables(out, methods, begin);
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

    @SuppressWarnings({"unchecked", "SuspiciousToArrayCall"})
    public static <T> Constructor<T>[] getHiddenConstructors(Class<T> clazz) {
        return Arrays.stream(getHiddenDirectExecutables(clazz))
                .filter((exec) -> exec instanceof Constructor
                        && !Modifier.isStatic(exec.getModifiers()))
                .toArray(Constructor[]::new);
    }

    public static <T> Constructor<T> getHiddenConstructor(Class<T> clazz, Class<?>... params) {
        return searchConstructor(getHiddenConstructors(clazz), params);
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static Method constructorToMethod(Constructor<?> constructor) {
        Method method = allocateInstance(Method.class);

        // override + hasRealParameterData + byte[2] padding
        AndroidUnsafe.putIntO(method, 8, AndroidUnsafe.getIntO(constructor, 8));
        // declaringClass
        AndroidUnsafe.putObject(method, 12, AndroidUnsafe.getObject(constructor, 12));
        // declaringClassOfOverriddenMethod
        AndroidUnsafe.putObject(method, 16, AndroidUnsafe.getObject(constructor, 16));
        // parameters
        AndroidUnsafe.putObject(method, 20, AndroidUnsafe.getObject(constructor, 20));
        // artMethod
        AndroidUnsafe.putLongO(method, 24, AndroidUnsafe.getLongO(constructor, 24));
        // accessFlags
        AndroidUnsafe.putIntO(method, 32, AndroidUnsafe.getIntO(constructor, 32));
        // dexMethodIndex
        AndroidUnsafe.putIntO(method, 36, AndroidUnsafe.getIntO(constructor, 36));

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
        var out = clazz.getDeclaredMethods();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return out;
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
        var out = clazz.getDeclaredConstructors();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return out;
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
        var out = clazz.getDeclaredFields();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return out;
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
    public static MethodHandle unreflect(Executable ex) {
        setAccessible(ex, true);
        MethodHandle handle;
        if (ex instanceof Method m) {
            handle = nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));
        } else {
            var c = (Constructor<?>) ex;
            handle = nothrows_run(() -> MethodHandles.publicLookup().unreflectConstructor(c));
        }
        initHandle(handle);
        return handle;
    }

    public static MethodHandle unreflectDirect(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("Only non-static and non-abstract methods allowed");
        }

        MethodHandle out = unreflect(m);
        InvokeAccess.setMethodHandleKind(out, InvokeAccess.kindInvokeDirect());

        initHandle(out);
        return out;
    }
}

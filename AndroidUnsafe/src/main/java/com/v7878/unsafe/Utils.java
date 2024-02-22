package com.v7878.unsafe;


import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Stack.getStackClass1;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Utils {

    public interface FineClosable extends AutoCloseable {
        void close();
    }

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }
        if (a2 == null) {
            return a1.length == 0;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static Field searchField(Field[] fields, String name, boolean thw) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return thw ? AndroidUnsafe.throwException(new NoSuchFieldException(name)) : null;
    }

    public static Field searchField(Field[] fields, String name) {
        return searchField(fields, name, true);
    }

    private static String methodToString(String name, Class<?>[] argTypes) {
        return name + ((argTypes == null || argTypes.length == 0)
                ? "()" : Arrays.stream(argTypes)
                .map(c -> c == null ? "null" : c.getName())
                .collect(Collectors.joining(",", "(", ")")));
    }

    public static Method searchMethod(Method[] methods, String name,
                                      boolean thw, Class<?>... parameterTypes) {
        for (Method m : methods) {
            if (m.getName().equals(name) && arrayContentsEq(
                    parameterTypes, m.getParameterTypes())) {
                return m;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString(name, parameterTypes))) : null;
    }

    public static Method searchMethod(Method[] methods, String name, Class<?>... parameterTypes) {
        return searchMethod(methods, name, true, parameterTypes);
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, boolean thw, Class<?>... parameterTypes) {
        for (Constructor<T> c : constructors) {
            if (arrayContentsEq(parameterTypes, c.getParameterTypes())) {
                return c;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString("<init>", parameterTypes))) : null;
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, Class<?>... parameterTypes) {
        return searchConstructor(constructors, true, parameterTypes);
    }

    public static <T extends Throwable> void assert_(boolean value, Supplier<T> th) {
        if (!value) {
            AndroidUnsafe.throwException(th.get());
        }
    }

    @FunctionalInterface
    public interface TRun<T> {

        T run() throws Throwable;
    }

    @FunctionalInterface
    public interface VTRun {

        void run() throws Throwable;
    }

    public static <T> T nothrows_run(TRun<T> r) {
        try {
            return r.run();
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    public static void nothrows_run(VTRun r) {
        try {
            r.run();
        } catch (Throwable th) {
            AndroidUnsafe.throwException(th);
        }
    }

    public static <T> Supplier<T> runOnce(Supplier<T> task) {
        return new Supplier<>() {
            volatile T value;

            @Override
            public T get() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.get();
                        }
                    }
                }
                return value;
            }
        };
    }

    public static BooleanSupplier runOnce(BooleanSupplier task) {
        return new BooleanSupplier() {
            volatile Boolean value;

            @Override
            public boolean getAsBoolean() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.getAsBoolean();
                        }
                    }
                }
                return value;
            }
        };
    }

    public static IntSupplier runOnce(IntSupplier task) {
        return new IntSupplier() {
            volatile Integer value;

            @Override
            public int getAsInt() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.getAsInt();
                        }
                    }
                }
                return value;
            }
        };
    }

    public static LongSupplier runOnce(LongSupplier task) {
        return new LongSupplier() {
            volatile Long value;

            @Override
            public long getAsLong() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.getAsLong();
                        }
                    }
                }
                return value;
            }
        };
    }

    public static DoubleSupplier runOnce(DoubleSupplier task) {
        return new DoubleSupplier() {
            volatile Double value;

            @Override
            public double getAsDouble() {
                if (value == null) {
                    synchronized (this) {
                        if (value == null) {
                            value = task.getAsDouble();
                        }
                    }
                }
                return value;
            }
        };
    }

    @SuppressLint("NewApi")
    public static void reachabilityFence(Object ref) {
        if (CORRECT_SDK_INT >= 28) {
            Reference.reachabilityFence(ref);
        } else {
            SinkHolder.sink = ref;
            // Leaving SinkHolder set to ref is unpleasant, since it keeps ref live until the next
            // reachabilityFence call. Clear it again in a way that's unlikely to be optimizable.
            // The fact that finalize_count is volatile makes it hard to move the test up.
            if (!SinkHolder.finalized) {
                SinkHolder.sink = null;
            }
        }
    }

    @Keep
    private static class SinkHolder {
        static volatile Object sink;
        // Ensure that sink looks live to even a reasonably clever compiler.
        private static volatile boolean finalized = false;
        @SuppressWarnings("unused")
        private static final Object sinkUser = new Object() {
            protected void finalize() {
                if (sink == null && finalized) {
                    throw shouldNotReachHere();
                }
                finalized = true;
            }
        };
    }

    public static final class SoftReferenceCache<K, V> {

        private final Map<K, Node<K, V>> cache = new ConcurrentHashMap<>();

        public V get(K key, Function<K, V> valueFactory) {
            return cache
                    .computeIfAbsent(key, k -> new Node<>()) // short lock (has to be according to ConcurrentHashMap)
                    .get(key, valueFactory); // long lock, but just for the particular key
        }

        private static class Node<K, V> {

            private volatile SoftReference<V> ref;

            public Node() {
            }

            public V get(K key, Function<K, V> valueFactory) {
                V result;
                if (ref == null || (result = ref.get()) == null) {
                    synchronized (this) { // don't let threads race on the valueFactory::apply call
                        if (ref == null || (result = ref.get()) == null) {
                            result = valueFactory.apply(key); // keep alive
                            ref = new SoftReference<>(result);
                        }
                    }
                }
                return result;
            }
        }
    }

    public static final class WeakReferenceCache<K, V> {

        private final Map<K, Node<K, V>> cache = new ConcurrentHashMap<>();

        public V get(K key, Function<K, V> valueFactory) {
            return cache
                    .computeIfAbsent(key, k -> new Node<>()) // short lock (has to be according to ConcurrentHashMap)
                    .get(key, valueFactory); // long lock, but just for the particular key
        }

        private static class Node<K, V> {

            private volatile WeakReference<V> ref;

            public Node() {
            }

            public V get(K key, Function<K, V> valueFactory) {
                V result;
                if (ref == null || (result = ref.get()) == null) {
                    synchronized (this) { // don't let threads race on the valueFactory::apply call
                        if (ref == null || (result = ref.get()) == null) {
                            result = valueFactory.apply(key); // keep alive
                            ref = new WeakReference<>(result);
                        }
                    }
                }
                return result;
            }
        }
    }

    public static boolean containsNullChars(String s) {
        return s.indexOf('\u0000') >= 0;
    }

    public static String toHexString(long value) {
        return "0x" + Long.toHexString(value);
    }

    public static ClassLoader newEmptyClassLoader(ClassLoader parent) {
        // new every time, needed for GC
        return new ClassLoader(parent) {
        };
    }

    public static ClassLoader newEmptyClassLoader() {
        return newEmptyClassLoader(getStackClass1().getClassLoader());
    }

    public static RuntimeException newIllegalArgumentException(String message) {
        return new IllegalArgumentException(message);
    }

    public static RuntimeException newIllegalArgumentException(String message, Object obj) {
        return new IllegalArgumentException(message + ": " + obj);
    }

    public static RuntimeException newIllegalArgumentException(String message, Object... objs) {
        return new IllegalArgumentException(message + Arrays.stream(objs).map(Objects::toString)
                .collect(Collectors.joining(", ", ": ", "")));
    }

    public static WrongMethodTypeException newWrongMethodTypeException(MethodType from, MethodType to) {
        return new WrongMethodTypeException("Cannot convert " + from + " to " + to);
    }

    public static AssertionError shouldNotReachHere() {
        throw new AssertionError("Should not reach here");
    }

    public static RuntimeException unexpectedType(Class<?> unexpectedType) {
        throw new InternalError("Unexpected type: " + unexpectedType);
    }

    public static RuntimeException badCast(Class<?> from, Class<?> to) {
        throw new ClassCastException("Cannot cast from " + from + " to " + to);
    }

    public static Class<?> boxedTypeAsPrimitiveType(Class<?> boxed) {
        if (boxed == Boolean.class) {
            return boolean.class;
        } else if (boxed == Byte.class) {
            return byte.class;
        } else if (boxed == Short.class) {
            return short.class;
        } else if (boxed == Character.class) {
            return char.class;
        } else if (boxed == Integer.class) {
            return int.class;
        } else if (boxed == Float.class) {
            return float.class;
        } else if (boxed == Long.class) {
            return long.class;
        } else if (boxed == Double.class) {
            return double.class;
        } else if (boxed == Void.class) {
            return void.class;
        }
        throw unexpectedType(boxed);
    }

    public static char boxedTypeAsPrimitiveChar(Class<?> boxed) {
        if (boxed == Boolean.class) {
            return 'Z';
        } else if (boxed == Byte.class) {
            return 'B';
        } else if (boxed == Short.class) {
            return 'S';
        } else if (boxed == Character.class) {
            return 'C';
        } else if (boxed == Integer.class) {
            return 'I';
        } else if (boxed == Float.class) {
            return 'F';
        } else if (boxed == Long.class) {
            return 'J';
        } else if (boxed == Double.class) {
            return 'D';
        } else if (boxed == Void.class) {
            return 'V';
        }
        throw unexpectedType(boxed);
    }

    public static Class<?> primitiveTypeAsBoxedType(Class<?> prim) {
        if (prim == boolean.class) {
            return Boolean.class;
        } else if (prim == byte.class) {
            return Byte.class;
        } else if (prim == short.class) {
            return Short.class;
        } else if (prim == char.class) {
            return Character.class;
        } else if (prim == int.class) {
            return Integer.class;
        } else if (prim == float.class) {
            return Float.class;
        } else if (prim == long.class) {
            return Long.class;
        } else if (prim == double.class) {
            return Double.class;
        } else if (prim == void.class) {
            return Void.class;
        }
        throw unexpectedType(prim);
    }

    public static Class<?> primitiveCharAsBoxedType(char prim) {
        return switch (prim) {
            case 'Z' -> Boolean.class;
            case 'B' -> Byte.class;
            case 'S' -> Short.class;
            case 'C' -> Character.class;
            case 'I' -> Integer.class;
            case 'J' -> Long.class;
            case 'F' -> Float.class;
            case 'D' -> Double.class;
            case 'V' -> Void.class;
            default -> throw new InternalError(
                    "Unexpected type char: " + prim);
        };
    }

    @Keep
    public static void ensureClassInitialized(Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }
}

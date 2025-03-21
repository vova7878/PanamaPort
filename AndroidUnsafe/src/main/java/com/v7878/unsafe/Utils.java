package com.v7878.unsafe;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.Stack.getStackClass1;

import android.annotation.SuppressLint;
import android.util.Log;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;

import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {
    public static final boolean DEBUG_BUILD = BuildConfig.DEBUG;

    public static final String LOG_TAG = "PANAMA";

    public interface FineClosable extends AutoCloseable {
        void close();
    }

    public static boolean arrayContentsEq(Object[] a1, Object[] a2) {
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

    public static MethodType methodTypeOf(Executable target) {
        List<Class<?>> args = new ArrayList<>(List.of(target.getParameterTypes()));
        Class<?> ret;
        if (target instanceof Method method) {
            if (!Modifier.isStatic(method.getModifiers())) {
                args.add(0, method.getDeclaringClass());
            }
            ret = method.getReturnType();
        } else {
            // constructor
            ret = target.getDeclaringClass();
        }
        return MethodType.methodType(ret, args);
    }

    @AlwaysInline
    public static int assertEq(int a, int b) {
        if (a == b) {
            return a;
        }
        throw new IllegalArgumentException("a(" + a + ") != b(" + b + ")");
    }

    @AlwaysInline
    public static long assertEq(long a, long b) {
        if (a == b) {
            return a;
        }
        throw new IllegalArgumentException("a(" + a + ") != b(" + b + ")");
    }

    @AlwaysInline
    public static <T extends Throwable> void check(boolean value, Supplier<T> th) {
        if (!value) {
            AndroidUnsafe.throwException(th.get());
        }
    }

    @FunctionalInterface
    public interface TRun<T> {
        T run() throws Throwable;
    }

    @AlwaysInline
    public static <T> T nothrows_run(TRun<T> r) {
        try {
            return r.run();
        } catch (Throwable th) {
            return AndroidUnsafe.throwException(th);
        }
    }

    @FunctionalInterface
    public interface VTRun {
        void run() throws Throwable;
    }

    @AlwaysInline
    public static void nothrows_run(VTRun r) {
        try {
            r.run();
        } catch (Throwable th) {
            AndroidUnsafe.throwException(th);
        }
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

    @DoNotShrink
    @DoNotOptimize
    //TODO: check if keep rules work correctly
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

    public static AssertionError shouldNotHappen(Throwable th) {
        throw new AssertionError("Should not happen", th);
    }

    public static RuntimeException unexpectedType(Class<?> unexpectedType) {
        throw new InternalError("Unexpected type: " + unexpectedType);
    }

    public static RuntimeException badCast(Class<?> from, Class<?> to) {
        throw new ClassCastException("Cannot cast from " + from + " to " + to);
    }

    public static RuntimeException unsupportedSDK(int sdk) {
        throw new IllegalStateException("Unsupported sdk: " + sdk);
    }

    @SuppressWarnings("finally")
    public static void handleUncaughtException(Throwable th) {
        if (th != null) {
            try {
                Log.wtf(LOG_TAG, "Unrecoverable uncaught exception encountered. The VM will now exit", th);
            } finally {
                System.exit(1);
            }
        }
    }

    public static MemorySegment allocateAddress(Arena scope, MemorySegment value) {
        MemorySegment out = scope.allocate(ADDRESS);
        out.set(ADDRESS, 0, value);
        return out;
    }

    public static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(hasNext);
        Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE,
                Spliterator.ORDERED | Spliterator.IMMUTABLE) {
            T prev;
            boolean started, finished;

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (finished)
                    return false;
                T t;
                if (started)
                    t = next.apply(prev);
                else {
                    t = seed;
                    started = true;
                }
                if (!hasNext.test(t)) {
                    prev = null;
                    finished = true;
                    return false;
                }
                action.accept(prev = t);
                return true;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (finished)
                    return;
                finished = true;
                T t = started ? next.apply(prev) : seed;
                prev = null;
                while (hasNext.test(t)) {
                    action.accept(t);
                    t = next.apply(t);
                }
            }
        };
        return StreamSupport.stream(spliterator, false);
    }
}

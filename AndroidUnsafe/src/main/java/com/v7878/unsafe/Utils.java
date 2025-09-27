package com.v7878.unsafe;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Process.FIRST_APPLICATION_UID;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.unsafe.Stack.getStackClass1;

import android.util.Log;

import com.v7878.dex.immutable.TypeId;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
        FineClosable NOP = () -> { /* nop */ };

        void close();
    }

    public static FineClosable linkClosables(FineClosable first, FineClosable second) {
        record LinkedClosable(FineClosable first, FineClosable second) implements FineClosable {
            @Override
            public void close() {
                try {
                    first.close();
                } finally {
                    second.close();
                }
            }
        }
        if (first == second) {
            return first;
        }
        if (first == FineClosable.NOP) {
            return second;
        }
        if (second == FineClosable.NOP) {
            return first;
        }
        return new LinkedClosable(first, second);
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
            if (!Objects.equals(a1[i], a2[i])) {
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

    private static String methodToString(String name, Class<?>[] params) {
        return name + ((params == null || params.length == 0)
                ? "()" : Arrays.stream(params)
                .map(c -> c == null ? "null" : c.getName())
                .collect(Collectors.joining(",", "(", ")")));
    }

    private static String methodToString(String name, String[] params) {
        return name + ((params == null || params.length == 0)
                ? "()" : Arrays.stream(params)
                .collect(Collectors.joining(",", "(", ")")));
    }

    public static Method searchMethod(Method[] methods, String name,
                                      boolean thw, Class<?>... params) {
        for (Method m : methods) {
            if (m.getName().equals(name) && arrayContentsEq(
                    params, m.getParameterTypes())) {
                return m;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString(name, params))) : null;
    }

    public static Method searchMethod(Method[] methods, String name, Class<?>... params) {
        return searchMethod(methods, name, true, params);
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, boolean thw, Class<?>... params) {
        for (Constructor<T> c : constructors) {
            if (arrayContentsEq(params, c.getParameterTypes())) {
                return c;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString("<init>", params))) : null;
    }

    public static <T> Constructor<T> searchConstructor(
            Constructor<T>[] constructors, Class<?>... params) {
        return searchConstructor(constructors, true, params);
    }

    public static String getExecutableName(Executable ex) {
        if (ex instanceof Method m) {
            return m.getName();
        }
        assert ex instanceof Constructor<?>;
        return Modifier.isStatic(ex.getModifiers()) ? "<clinit>" : "<init>";
    }

    public static Executable searchExecutable(
            Executable[] executables, String name, boolean thw, String... params) {
        for (var ex : executables) {
            if (getExecutableName(ex).equals(name) && arrayContentsEq(
                    Stream.of(params).map(TypeId::ofName).toArray(TypeId[]::new),
                    Stream.of(ex.getParameterTypes()).map(TypeId::of).toArray(TypeId[]::new))) {
                return ex;
            }
        }
        return thw ? AndroidUnsafe.throwException(
                new NoSuchMethodException(methodToString(name, params))) : null;
    }

    public static Executable searchExecutable(
            Executable[] executables, String name, String... params) {
        return searchExecutable(executables, name, true, params);
    }

    public static boolean checkClassExists(ClassLoader loader, String name) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException th) {
            return false;
        }
    }

    public static String generateClassName(ClassLoader loader, String base) {
        class Holder {
            static final Random RANDOM = new Random();
        }
        String name = null;
        while (name == null || checkClassExists(loader, name)) {
            name = String.format("%s_%016X", base, Holder.RANDOM.nextLong());
        }
        return name;
    }

    public static <T> boolean contains(T[] array, T value) {
        for (T j : array) if (j == value) return true;
        return false;
    }

    @AlwaysInline
    public static <T extends Throwable> void check(boolean value, Supplier<T> th) {
        if (!value) {
            AndroidUnsafe.throwException(th.get());
        }
    }

    @AlwaysInline
    public static <T extends Throwable> void dcheck(boolean value, Supplier<T> th) {
        if (!DEBUG_BUILD && !value) {
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

    public static void reachabilityFence(Object ref) {
        if (SDK_INT >= 28) {
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

    @DoNotShrink // caller-sensitive
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

    public static RuntimeException unsupportedART(int art) {
        throw new IllegalStateException("Unsupported art: " + art);
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

    public static int getAppId(int uid) {
        final int PER_USER_RANGE = 100000;
        return uid % PER_USER_RANGE;
    }

    public static boolean isCoreUid(int uid) {
        if (uid >= 0) {
            return getAppId(uid) < FIRST_APPLICATION_UID;
        } else {
            return false;
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

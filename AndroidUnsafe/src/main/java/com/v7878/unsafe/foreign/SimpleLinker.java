package com.v7878.unsafe.foreign;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.runOnce;

import com.v7878.dex.AnnotationItem;
import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.invoke.MethodHandlesFixes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import dalvik.system.DexFile;

public class SimpleLinker {
    private static class WORD {
    }

    private static class BOOL_AS_INT {
    }

    public static final Class<?> WORD_CLASS = WORD.class;

    public static final Class<?> BOOL_AS_INT_CLASS = BOOL_AS_INT.class;

    private static class SymbolInfo {
        public final String name;
        public final MethodType stub_type;
        public final MethodType handle_type;
        public final LongSupplier symbol;

        public SymbolInfo(String name, MethodType stub_type,
                          MethodType handle_type, LongSupplier symbol) {
            this.name = name;
            this.stub_type = stub_type;
            this.handle_type = handle_type;
            this.symbol = symbol;
        }
    }

    public static Supplier<MethodHandle> processSymbol(SymbolLookup lookup, Arena scope, String name, MethodType type) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);

        MethodType stub_type = stubType(type);
        MethodType handle_type = handleType(type);
        LongSupplier symbol = runOnce(() -> {
            Optional<MemorySegment> tmp = lookup.find(name);
            if (!tmp.isPresent()) {
                throw new IllegalArgumentException("Cannot find symbol: \"" + name + "\"");
            }
            return tmp.get().nativeAddress();
        });

        return processSymbol(scope, new SymbolInfo(name, stub_type, handle_type, symbol));
    }

    private static Supplier<MethodHandle> processSymbol(Arena scope, SymbolInfo info) {
        return runOnce(() -> {
            long symbol = info.symbol.getAsLong();

            String stub_name = SimpleLinker.class.getName() + "$Stub$" + info.name;
            TypeId stub_id = TypeId.of(stub_name);

            ClassDef stub_def = new ClassDef(stub_id);
            stub_def.setSuperClass(TypeId.of(Object.class));
            stub_def.setAccessFlags(ACC_PUBLIC | ACC_FINAL);

            EncodedMethod e_method = new EncodedMethod(new MethodId(stub_id,
                    ProtoId.of(info.stub_type), info.name),
                    ACC_PUBLIC | ACC_STATIC | ACC_NATIVE);
            e_method.getAnnotations().add(AnnotationItem.CriticalNative());
            stub_def.getClassData().getDirectMethods().add(e_method);

            DexFile dex = openDexFile(new Dex(stub_def).compile());
            ClassLoader loader = Utils.newEmptyClassLoader();

            Class<?> stub_class = loadClass(dex, stub_name, loader);

            Method method = getDeclaredMethod(stub_class, info.name, info.stub_type.parameterArray());
            registerNativeMethod(method, symbol);
            MethodHandle raw_handle = unreflect(method);
            //TODO: check scope in handle
            return MethodHandlesFixes.explicitCastArguments(raw_handle, info.handle_type);
        });
    }

    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(
            void.class, WORD_CLASS,
            byte.class, boolean.class,
            short.class, char.class,
            int.class, float.class, BOOL_AS_INT_CLASS,
            long.class, double.class
    );

    private static void checkSupported(Class<?> raw_class) {
        if (!SUPPORTED_TYPES.contains(raw_class)) {
            throw new IllegalArgumentException(raw_class + " type is not supported");
        }
    }

    private static Class<?> stubClass(Class<?> raw_class) {
        checkSupported(raw_class);
        if (raw_class == WORD_CLASS) {
            return ExtraLayouts.WORD.carrier();
        }
        if (raw_class == BOOL_AS_INT_CLASS) {
            return int.class;
        }
        return raw_class;
    }

    private static MethodType stubType(MethodType raw_type) {
        Objects.requireNonNull(raw_type);
        return MethodType.methodType(stubClass(raw_type.returnType()),
                raw_type.parameterList().stream()
                        .map(SimpleLinker::stubClass).toArray(Class<?>[]::new));
    }

    private static Class<?> handleClass(Class<?> raw_class) {
        checkSupported(raw_class);
        if (raw_class == WORD_CLASS) {
            return long.class;
        }
        if (raw_class == BOOL_AS_INT_CLASS) {
            return boolean.class;
        }
        return raw_class;
    }

    private static MethodType handleType(MethodType raw_type) {
        Objects.requireNonNull(raw_type);
        return MethodType.methodType(handleClass(raw_type.returnType()),
                raw_type.parameterList().stream()
                        .map(SimpleLinker::handleClass).toArray(Class<?>[]::new));
    }
}

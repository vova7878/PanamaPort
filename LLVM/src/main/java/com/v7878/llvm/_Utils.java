package com.v7878.llvm;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.WORD_CLASS;
import static com.v7878.unsafe.Utils.searchMethod;

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

import dalvik.system.DexFile;

final class _Utils {
    private static class WORD {
    }

    private static class BOOL_AS_INT {
    }

    public static final Class<?> WORD = WORD.class;

    public static final Class<?> BOOL_AS_INT = BOOL_AS_INT.class;

    public static final Class<?> VOID_PTR = WORD;

    public static final Class<?> CHAR_PTR = ptr(byte.class);
    public static final Class<?> CONST_CHAR_PTR = const_ptr(byte.class);

    public static final Class<?> SIZE_T = WORD;
    public static final Class<?> UINT64_T = long.class;
    public static final Class<?> UNSIGNED_INT = int.class;

    public static final Class<?> ENUM = int.class;

    public static Class<?> ptr(Class<?> ignored) {
        return VOID_PTR;
    }

    public static Class<?> const_ptr(Class<?> ignored) {
        return VOID_PTR;
    }

    public interface Symbol {
        String name();

        MethodType type();

        void setSymbol(long native_symbol);

        void setHandle(MethodHandle handle);
    }

    private static class TmpSymbolInfo {
        public final Symbol symbol;
        public final String name;
        public final MethodType stub_type;
        public final MethodType handle_type;
        public final long native_symbol;

        public TmpSymbolInfo(Symbol symbol, String name, MethodType stub_type,
                             MethodType handle_type, long native_symbol) {
            this.symbol = symbol;
            this.name = name;
            this.stub_type = stub_type;
            this.handle_type = handle_type;
            this.native_symbol = native_symbol;
        }
    }

    public static void processSymbols(SymbolLookup lookup, Arena scope, Symbol... symbols) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(symbols);

        if (symbols.length == 0) {
            return;
        }

        String stub_name = _Utils.class.getName() + "$Stub";
        TypeId stub_id = TypeId.of(stub_name);

        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));
        stub_def.setAccessFlags(ACC_PUBLIC | ACC_FINAL);

        TmpSymbolInfo[] infos = new TmpSymbolInfo[symbols.length];

        for (int i = 0; i < symbols.length; i++) {
            Symbol symbol = symbols[i];
            String name = symbol.name();
            MethodType raw_type = symbol.type();
            MethodType stub_type = stubType(raw_type);
            MethodType handle_type = handleType(raw_type);
            Optional<MemorySegment> tmp = lookup.find(name);
            if (!tmp.isPresent()) {
                throw new IllegalArgumentException("Cannot find symbol: \"" + name + "\"");
            }
            long native_symbol = tmp.get().address();
            infos[i] = new TmpSymbolInfo(symbol, name, stub_type, handle_type, native_symbol);

            EncodedMethod method = new EncodedMethod(new MethodId(stub_id,
                    ProtoId.of(stub_type), name), ACC_PUBLIC | ACC_STATIC | ACC_NATIVE);
            method.getAnnotations().add(AnnotationItem.CriticalNative());
            stub_def.getClassData().getDirectMethods().add(method);
        }

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        ClassLoader loader = Utils.newEmptyClassLoader();

        Class<?> stub_class = loadClass(dex, stub_name, loader);
        Method[] methods = getDeclaredMethods(stub_class);

        for (TmpSymbolInfo info : infos) {
            Method method = searchMethod(methods, info.name, info.stub_type.parameterArray());
            setExecutableData(method, info.native_symbol);
            MethodHandle raw_handle = unreflect(method);
            //TODO: check scope in handle
            MethodHandle handle = MethodHandlesFixes.explicitCastArguments(raw_handle, info.handle_type);

            info.symbol.setSymbol(info.native_symbol);
            info.symbol.setHandle(handle);
        }
    }

    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(
            void.class, VOID_PTR,
            byte.class, boolean.class,
            short.class, char.class,
            int.class, float.class, BOOL_AS_INT,
            long.class, double.class
    );

    private static void checkSupported(Class<?> raw_class) {
        if (!SUPPORTED_TYPES.contains(raw_class)) {
            throw new IllegalArgumentException(raw_class + "is not supported");
        }
    }

    private static Class<?> stubClass(Class<?> raw_class) {
        checkSupported(raw_class);
        if (raw_class == WORD) {
            return WORD_CLASS;
        }
        if (raw_class == BOOL_AS_INT) {
            return int.class;
        }
        return raw_class;
    }

    private static MethodType stubType(MethodType raw_type) {
        Objects.requireNonNull(raw_type);
        return MethodType.methodType(stubClass(raw_type.returnType()),
                raw_type.parameterList().stream()
                        .map(_Utils::stubClass).toArray(Class<?>[]::new));
    }

    private static Class<?> handleClass(Class<?> raw_class) {
        checkSupported(raw_class);
        if (raw_class == WORD) {
            return long.class;
        }
        if (raw_class == BOOL_AS_INT) {
            return boolean.class;
        }
        return raw_class;
    }

    private static MethodType handleType(MethodType raw_type) {
        Objects.requireNonNull(raw_type);
        return MethodType.methodType(handleClass(raw_type.returnType()),
                raw_type.parameterList().stream()
                        .map(_Utils::handleClass).toArray(Class<?>[]::new));
    }
}

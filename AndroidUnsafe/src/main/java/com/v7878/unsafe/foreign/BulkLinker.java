package com.v7878.unsafe.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.BinOp.AND_LONG;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.dex.bytecode.CodeBuilder.UnOp.INT_TO_LONG;
import static com.v7878.dex.bytecode.CodeBuilder.UnOp.LONG_TO_INT;
import static com.v7878.dex.bytecode.CodeBuilder.UnOp.NEG_INT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.RISCV64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static java.lang.annotation.ElementType.METHOD;

import androidx.annotation.Keep;

import com.v7878.dex.AnnotationItem;
import com.v7878.dex.AnnotationSet;
import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.foreign.Arena;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils.ClassStatus;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.InstructionSet;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.VM;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import dalvik.system.DexFile;

public class BulkLinker {
    public enum MapType {
        // normal types
        VOID(void.class),
        BYTE(byte.class),
        BOOL(boolean.class),
        SHORT(short.class),
        CHAR(char.class),
        INT(int.class),
        FLOAT(float.class),
        LONG(long.class),
        DOUBLE(double.class),

        OBJECT(Object.class),

        // extra types
        LONG_AS_WORD(IS64BIT ? long.class : int.class, long.class),
        BOOL_AS_INT(int.class, boolean.class),
        @DangerLevel(DangerLevel.VERY_CAREFUL)
        OBJECT_AS_RAW_INT(int.class, Object.class),
        @DangerLevel(DangerLevel.BAD_GC_COLLISION)
        OBJECT_AS_ADDRESS(IS64BIT ? long.class : int.class, Object.class);

        public final Class<?> forStub;
        public final Class<?> forImpl;

        MapType(Class<?> forStub, Class<?> forImpl) {
            this.forStub = forStub;
            this.forImpl = forImpl;
        }

        MapType(Class<?> type) {
            this(type, type);
        }
    }

    private static ProtoId stubProto(MapType ret, MapType[] args) {
        return new ProtoId(TypeId.of(ret.forStub), Arrays.stream(args)
                .map(lt -> TypeId.of(lt.forStub)).toArray(TypeId[]::new));
    }

    private static ProtoId implProto(MapType ret, MapType[] args) {
        return new ProtoId(TypeId.of(ret.forImpl), Arrays.stream(args)
                .map(lt -> TypeId.of(lt.forImpl)).toArray(TypeId[]::new));
    }

    private static Class<?>[] stubArgs(MapType[] args) {
        return Arrays.stream(args).map(lt -> lt.forStub).toArray(Class[]::new);
    }

    private static Class<?>[] implArgs(MapType[] args) {
        return Arrays.stream(args).map(lt -> lt.forImpl).toArray(Class[]::new);
    }

    private abstract static sealed class SymbolSource
            permits ASMSource, SegmentSource {
    }

    private static final class ASMSource extends SymbolSource {
        final Supplier<byte[]> generator;

        private ASMSource(Supplier<byte[]> generator) {
            this.generator = generator;
        }

        public static ASMSource of(Supplier<byte[]> generator) {
            Objects.requireNonNull(generator);
            return new ASMSource(generator);
        }
    }

    private static final class SegmentSource extends SymbolSource {
        final Supplier<MemorySegment> symbol;

        private SegmentSource(Supplier<MemorySegment> symbol) {
            this.symbol = symbol;
        }

        public static SegmentSource of(Supplier<MemorySegment> symbol) {
            Objects.requireNonNull(symbol);
            return new SegmentSource(symbol);
        }
    }

    public enum CallType {
        NATIVE_STATIC(true, ACC_NATIVE | ACC_STATIC, null),
        NATIVE_VIRTUAL(false, ACC_NATIVE, null),
        /*TODO: NATIVE_VIRTUAL_REPLACE_THIS,*/
        FAST_STATIC(true, ACC_NATIVE | ACC_STATIC, AnnotationItem.FastNative()),
        FAST_VIRTUAL(false, ACC_NATIVE, AnnotationItem.FastNative()),
        /*TODO: FAST_VIRTUAL_REPLACE_THIS,*/
        CRITICAL(true, ACC_NATIVE | ACC_STATIC, AnnotationItem.CriticalNative());

        final boolean isStatic;
        final int flags;
        final AnnotationSet annotations;

        CallType(boolean isStatic, int flags, AnnotationItem annotation) {
            this.isStatic = isStatic;
            this.flags = flags;
            this.annotations = annotation == null ? null : new AnnotationSet(annotation);
        }
    }

    private static class SymbolInfo {
        final String name;
        final MapType ret;
        final MapType[] args;
        final SymbolSource source;
        final CallType call_type;

        private SymbolInfo(String name, MapType ret, MapType[] args,
                           SymbolSource source, CallType call_type) {
            this.name = name;
            this.ret = ret;
            this.args = args;
            this.source = source;
            this.call_type = call_type;
        }

        public static SymbolInfo of(String name, CallType call_type, SymbolSource source,
                                    MapType ret, MapType... args) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(ret);
            Objects.requireNonNull(args);
            Objects.requireNonNull(source);
            Objects.requireNonNull(call_type);
            return new SymbolInfo(name, ret, args.clone(), source, call_type);
        }
    }

    private static final String prefix = "raw_";

    //TODO: use optimally sized move instructions
    private static byte[] generateStub(Class<?> parent, SymbolInfo[] infos) {
        if (parent == null) {
            parent = Object.class;
        }
        String impl_name = parent.getName() + "$Impl";
        TypeId impl_id = TypeId.of(impl_name);
        ClassDef impl_def = new ClassDef(impl_id);
        if (parent.isInterface()) {
            impl_def.setSuperClass(TypeId.of(Object.class));
            impl_def.getInterfaces().add(TypeId.of(parent));
        } else {
            impl_def.setSuperClass(TypeId.of(parent));
        }

        for (SymbolInfo info : infos) {
            ProtoId raw_proto = stubProto(info.ret, info.args);
            MethodId raw_method_id = new MethodId(impl_id, raw_proto, prefix + info.name);
            EncodedMethod raw_em = new EncodedMethod(raw_method_id, info.call_type.flags,
                    info.call_type.annotations, null, null);
            if (info.call_type.isStatic) {
                impl_def.getClassData().getDirectMethods().add(raw_em);
            } else {
                impl_def.getClassData().getVirtualMethods().add(raw_em);
            }

            ProtoId proto = implProto(info.ret, info.args);
            MethodId method_id = new MethodId(impl_id, proto, info.name);
            final int reserved = 4;
            int locals = reserved + raw_proto.getInputRegistersCount();
            int[] regs = {/* call args */ reserved, /* stub args */ 0};
            EncodedMethod em = new EncodedMethod(method_id, ACC_PUBLIC).withCode(locals, b -> {
                if (!info.call_type.isStatic) {
                    b.move_object_16(b.l(regs[0]++), b.this_());
                }
                for (MapType type : info.args) {
                    switch (type) {
                        case BYTE, BOOL, SHORT, CHAR, INT, FLOAT, BOOL_AS_INT ->
                                b.move_16(b.l(regs[0]++), b.p(regs[1]++));
                        case LONG, DOUBLE -> {
                            b.move_wide_16(b.l(regs[0]), b.p(regs[1]));
                            regs[0] += 2;
                            regs[1] += 2;
                        }
                        case LONG_AS_WORD -> {
                            if (IS64BIT) {
                                b.move_wide_16(b.l(regs[0]), b.p(regs[1]));
                                regs[0] += 2;
                            } else {
                                b.move_wide_16(b.l(0), b.p(regs[1]));
                                b.unop(LONG_TO_INT, b.l(0), b.l(0));
                                b.move_16(b.l(regs[0]), b.l(0));
                                regs[0] += 1;
                            }
                            regs[1] += 2;
                        }
                        case OBJECT, OBJECT_AS_RAW_INT ->
                                b.move_object_16(b.l(regs[0]++), b.p(regs[1]++));
                        case OBJECT_AS_ADDRESS -> {
                            // note: it's broken - object is cast to pointer
                            // TODO: check how will GC react to this?
                            b.move_object_16(b.l(0), b.p(regs[1]));
                            if (VM.isPoisonReferences()) {
                                b.unop(NEG_INT, b.l(0), b.l(0));
                            }
                            if (IS64BIT) {
                                b.const_4(b.l(1), 0);
                                b.move_wide_16(b.l(regs[0]), b.l(0));
                                regs[0] += 2;
                            } else {
                                b.move_16(b.l(regs[0]), b.l(0));
                                regs[0] += 1;
                            }
                            regs[1] += 1;
                        }
                        default -> throw shouldNotReachHere();
                    }
                }

                int call_regs = regs[0] - reserved;
                if (call_regs == 0) {
                    b.invoke(info.call_type.isStatic ? STATIC : VIRTUAL, raw_method_id);
                } else {
                    b.invoke_range(info.call_type.isStatic ? STATIC : VIRTUAL,
                            raw_method_id, call_regs, b.l(reserved));
                }

                switch (info.ret) {
                    case VOID -> b.return_void();
                    case BYTE, BOOL, SHORT, CHAR, INT, FLOAT, BOOL_AS_INT -> {
                        b.move_result(b.l(0));
                        b.return_(b.l(0));
                    }
                    case LONG, DOUBLE -> {
                        b.move_result_wide(b.l(0));
                        b.return_wide(b.l(0));
                    }
                    case LONG_AS_WORD -> {
                        if (IS64BIT) {
                            b.move_result_wide(b.l(0));
                            b.return_wide(b.l(0));
                        } else {
                            b.move_result(b.l(0));
                            b.unop(INT_TO_LONG, b.l(0), b.l(0));
                            b.const_wide(b.l(2), 0xffffffffL);
                            b.binop_2addr(AND_LONG, b.l(0), b.l(2));
                            b.return_wide(b.l(0));
                        }
                    }
                    case OBJECT, OBJECT_AS_RAW_INT -> {
                        b.move_result_object(b.l(0));
                        b.return_object(b.l(0));
                    }
                    case OBJECT_AS_ADDRESS -> {
                        // note: it's broken - pointer is cast to object
                        // TODO: check how will GC react to this?
                        b.move_result_wide(b.l(0));
                        if (VM.isPoisonReferences()) {
                            b.unop(NEG_INT, b.l(0), b.l(0));
                        }
                        b.return_object(b.l(0));
                    }
                    default -> throw shouldNotReachHere();
                }
            });
            impl_def.getClassData().getVirtualMethods().add(em);
        }

        return new Dex(impl_def).compile();
    }

    private static <T> Class<T> processSymbols(Arena scope, Class<T> parent, ClassLoader loader, SymbolInfo[] infos) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(loader);
        MemorySegment[] symbols = new MemorySegment[infos.length];
        {
            int count = 0;
            int[] map = new int[infos.length];
            byte[][] data = new byte[infos.length][];
            for (int i = 0; i < infos.length; i++) {
                SymbolInfo info = infos[i];
                if (info.source instanceof ASMSource gs) {
                    map[count] = i;
                    data[count] = Objects.requireNonNull(gs.generator.get());
                    count++;
                } else if (info.source instanceof SegmentSource ss) {
                    symbols[i] = Objects.requireNonNull(ss.symbol.get());
                } else {
                    shouldNotReachHere();
                }
            }
            if (count != 0) {
                MemorySegment[] blob = NativeCodeBlob.makeCodeBlob(scope, Arrays.copyOf(data, count));
                for (int i = 0; i < count; i++) {
                    symbols[map[i]] = blob[i];
                }
            }
        }
        Class<T> impl;
        {
            String name = parent.getName() + "$Impl";
            DexFile dex = openDexFile(generateStub(parent, infos));
            //noinspection unchecked
            impl = (Class<T>) loadClass(dex, name, loader);
            setClassStatus(impl, ClassStatus.Verified);

            Method[] methods = getDeclaredMethods(impl);
            for (int i = 0; i < infos.length; i++) {
                SymbolInfo info = infos[i];
                Method method = searchMethod(methods, prefix + info.name, stubArgs(info.args));
                registerNativeMethod(method, symbols[i].nativeAddress());
            }
        }
        return impl;
    }

    public enum Tristate {
        TRUE, FALSE, NO_MATTER
    }

    @Keep
    public @interface Conditions {
        InstructionSet[] arch() default {ARM, ARM64, X86, X86_64, RISCV64};

        @ApiSensitive
        int[] api() default {26, 27, 28, 29, 30, 31, 32, 33, 34, 35};

        Tristate poisoning() default Tristate.NO_MATTER;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(ASMs.class)
    @Keep
    public @interface ASM {
        Conditions conditions() default @Conditions();

        byte[] code();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface ASMs {
        ASM[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface ASMGenerator {
        Class<?> clazz() default /*search in current class*/ void.class;

        String method();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(LibrarySymbols.class)
    @Keep
    public @interface LibrarySymbol {
        Conditions conditions() default @Conditions();

        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface LibrarySymbols {
        LibrarySymbol[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface SymbolGenerator {
        Class<?> clazz() default /*search in current class*/ void.class;

        String method();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface CallSignature {
        CallType type();

        MapType ret();

        MapType[] args();
    }

    private static void checkSignature(Method method, CallSignature signature) {
        if (method.getReturnType() == signature.ret().forImpl) {
            if (Utils.arrayContentsEq(method.getParameterTypes(), implArgs(signature.args()))) {
                return;
            }
        }
        throw new IllegalStateException(method + " has wrong signature");
    }

    private static byte[] getCode(ASM asm) {
        byte[] code = asm.code();
        if (code.length == 0) {
            return null;
        }
        return code;
    }

    private static byte[] getCode(ASMGenerator generator, Class<?> clazz, Map<Class<?>, Method[]> cached_methods) {
        if (generator.clazz() != void.class) {
            clazz = generator.clazz();
        }
        Method method = searchMethod(cached_methods.computeIfAbsent(clazz, Reflection::getDeclaredMethods), generator.method());
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("asm generator method is not static: " + method);
        }
        if (method.getReturnType() != byte[].class) {
            throw new IllegalArgumentException("return type of asm generator method is not byte[]: " + method);
        }
        byte[] code = nothrows_run(() -> (byte[]) unreflect(method).invokeExact());
        if (code == null || code.length == 0) {
            return null;
        }
        return code;
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean contains(int[] array, int value) {
        for (int j : array) if (j == value) return true;
        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> boolean contains(T[] array, T value) {
        for (T j : array) if (j == value) return true;
        return false;
    }

    private static boolean checkPoisoning(Tristate poisoning) {
        return poisoning == Tristate.NO_MATTER ||
                ((poisoning == Tristate.TRUE) == VM.isPoisonReferences());
    }

    private static boolean checkConditions(Conditions cond) {
        return contains(cond.arch(), CURRENT_INSTRUCTION_SET) &&
                contains(cond.api(), CORRECT_SDK_INT) &&
                checkPoisoning(cond.poisoning());
    }

    private static ASMSource getASMSource(
            ASM[] asms, ASMGenerator generator, Class<?> clazz,
            Map<Class<?>, Method[]> cached_methods, Method method) {
        if (asms.length == 0 && generator == null) {
            return null;
        }
        return ASMSource.of(() -> {
            byte[] code = null;
            for (ASM asm : asms) {
                Conditions cond = asm.conditions();
                if (checkConditions(cond)) {
                    code = getCode(asm);
                    if (code != null) {
                        break;
                    }
                }
            }
            if (generator != null) {
                code = getCode(generator, clazz, cached_methods);
            }
            if (code == null) {
                throw new IllegalStateException("could not find code for method " + method);
            }
            return code;
        });
    }

    private static MemorySegment getSymbol(SymbolGenerator generator, Class<?> clazz,
                                           Map<Class<?>, Method[]> cached_methods) {
        if (generator.clazz() != void.class) {
            clazz = generator.clazz();
        }
        Method method = searchMethod(cached_methods.computeIfAbsent(clazz, Reflection::getDeclaredMethods), generator.method());
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("symbol generator method is not static: " + method);
        }
        if (method.getReturnType() != MemorySegment.class) {
            throw new IllegalArgumentException("return type of symbol generator method is not MemorySegment: " + method);
        }
        return nothrows_run(() -> (MemorySegment) unreflect(method).invokeExact());
    }

    private static SegmentSource getSegmentSource(
            LibrarySymbol[] syms, SymbolGenerator generator, SymbolLookup lookup,
            Class<?> clazz, Map<Class<?>, Method[]> cached_methods, Method method) {
        if (syms.length == 0 && generator == null) {
            return null;
        }
        return SegmentSource.of(() -> {
            MemorySegment symbol = null;
            for (LibrarySymbol sym : syms) {
                Conditions cond = sym.conditions();
                if (checkConditions(cond)) {
                    symbol = lookup.find(sym.name()).orElse(null);
                    if (symbol != null) {
                        break;
                    }
                }
            }
            if (generator != null) {
                symbol = getSymbol(generator, clazz, cached_methods);
            }
            if (symbol == null) {
                throw new IllegalStateException("could not find symbol for method " + method);
            }
            return symbol;
        });
    }

    public static <T> Class<T> processSymbols(Arena arena, Class<T> clazz) {
        return processSymbols(arena, clazz, Linker.nativeLinker().defaultLookup());
    }

    public static <T> Class<T> processSymbols(Arena arena, Class<T> clazz, SymbolLookup lookup) {
        return processSymbols(arena, clazz, clazz.getClassLoader(), lookup);
    }

    //TODO: add CachedStub annotation
    public static <T> Class<T> processSymbols(Arena arena, Class<T> clazz, ClassLoader loader, SymbolLookup lookup) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(loader);
        Objects.requireNonNull(lookup);

        Map<Class<?>, Method[]> cached_methods = new HashMap<>();
        Method[] clazz_methods = getDeclaredMethods(clazz);
        cached_methods.put(clazz, clazz_methods);
        List<SymbolInfo> infos = new ArrayList<>(clazz_methods.length);

        for (Method method : clazz_methods) {
            CallSignature signature = method.getDeclaredAnnotation(CallSignature.class);

            ASM[] asms = method.getDeclaredAnnotationsByType(ASM.class);
            ASMGenerator asm_generator = method.getDeclaredAnnotation(ASMGenerator.class);
            ASMSource asm_source = getASMSource(asms, asm_generator, clazz, cached_methods, method);

            LibrarySymbol[] syms = method.getDeclaredAnnotationsByType(LibrarySymbol.class);
            SymbolGenerator sym_generator = method.getDeclaredAnnotation(SymbolGenerator.class);
            SymbolSource sym_source = getSegmentSource(syms, sym_generator, lookup, clazz, cached_methods, method);

            if (asm_source == null && sym_source == null) {
                if (signature == null) {
                    continue; // skip, this method does not require processing
                } else {
                    throw new IllegalStateException("Signature is present, but ASM or Symbol sources aren`t for method" + method);
                }
            } else if (asm_source != null && sym_source != null) {
                throw new IllegalStateException("Both ASM and Symbol sources are present for method " + method);
            } else if (signature == null) {
                throw new IllegalStateException("ASM or Symbol sources are present, but signeture isn`t for method" + method);
            }

            checkSignature(method, signature);

            SymbolSource source = asm_source == null ? sym_source : asm_source;
            infos.add(SymbolInfo.of(method.getName(), signature.type(), source, signature.ret(), signature.args()));
        }

        return processSymbols(arena, clazz, loader, infos.toArray(new SymbolInfo[0]));
    }
}

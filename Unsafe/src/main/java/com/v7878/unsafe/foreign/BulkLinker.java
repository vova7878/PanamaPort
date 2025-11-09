package com.v7878.unsafe.foreign;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION.SDK_INT_FULL;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_INTERFACE;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.dex.builder.CodeBuilder.BinOp.AND_LONG;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_LONG;
import static com.v7878.dex.builder.CodeBuilder.UnOp.LONG_TO_INT;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
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
import static com.v7878.unsafe.Utils.DEBUG_BUILD;
import static com.v7878.unsafe.Utils.LOG_TAG;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchField;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.llvm.LLVMBuilder.buildAddressToRawObject;
import static com.v7878.unsafe.llvm.LLVMBuilder.buildRawObjectToAddress;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_call;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_const_load_ptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_intptr;
import static com.v7878.unsafe.llvm.LLVMTypes.double_t;
import static com.v7878.unsafe.llvm.LLVMTypes.float_t;
import static com.v7878.unsafe.llvm.LLVMTypes.function_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int16_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int1_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int64_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int8_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.generateFunctionCodeArray;
import static java.lang.annotation.ElementType.METHOD;

import android.util.Log;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.Annotation;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.ClassUtils;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import dalvik.system.DexFile;

public class BulkLinker {
    public enum MapType {
        // normal types
        VOID(false, void.class),
        BYTE(false, byte.class),
        BOOL(false, boolean.class),
        SHORT(false, short.class),
        CHAR(false, char.class),
        INT(false, int.class),
        FLOAT(false, float.class),
        LONG(false, long.class),
        DOUBLE(false, double.class),

        OBJECT(false, Object.class),

        // extra types
        LONG_AS_WORD(false, IS64BIT ? long.class : int.class, long.class),
        BOOL_AS_INT(false, int.class, boolean.class),

        @DangerLevel(DangerLevel.VERY_CAREFUL)
        OBJECT_AS_RAW_INT(false, int.class, Object.class),
        @DangerLevel(DangerLevel.VERY_CAREFUL)
        OBJECT_AS_ADDRESS(true, int.class, Object.class);
        //TODO: OBJECT_AS_ADDRESS_WITH_OFFSET(true, int.class, Object.class);

        final boolean requireNativeStub;
        final Class<?> forStub;
        final Class<?> forImpl;

        MapType(boolean requireNativeStub, Class<?> forStub, Class<?> forImpl) {
            this.requireNativeStub = requireNativeStub;
            this.forStub = forStub;
            this.forImpl = forImpl;
        }

        MapType(boolean requireNativeStub, Class<?> type) {
            this(requireNativeStub, type, type);
        }
    }

    private sealed interface SymbolSource
            permits ASMSource, SegmentSource {
    }

    private record ASMSource(byte[] code) implements SymbolSource {
        public static ASMSource of(byte[] code) {
            Objects.requireNonNull(code);
            return new ASMSource(code);
        }
    }

    private record SegmentSource(MemorySegment symbol) implements SymbolSource {
        public static SegmentSource of(MemorySegment symbol) {
            Objects.requireNonNull(symbol);
            return new SegmentSource(symbol);
        }
    }

    private enum EnvType {
        FULL_ENV, NO_ENV, OMIT_ENV
    }

    public enum CallType {
        NATIVE_STATIC(false, false, EnvType.FULL_ENV, false, true, ACC_NATIVE | ACC_STATIC),
        NATIVE_STATIC_OMIT_ENV(false, true, EnvType.OMIT_ENV, false, true, ACC_NATIVE | ACC_STATIC),
        NATIVE_VIRTUAL(false, false, EnvType.FULL_ENV, false, false, ACC_NATIVE),
        NATIVE_VIRTUAL_REPLACE_THIS(false, false, EnvType.FULL_ENV, true, false, ACC_NATIVE),
        FAST_STATIC(true, false, EnvType.FULL_ENV, false, true, ACC_NATIVE | ACC_STATIC, Annotation.FastNative()),
        FAST_STATIC_OMIT_ENV(true, true, EnvType.OMIT_ENV, false, true, ACC_NATIVE | ACC_STATIC, Annotation.FastNative()),
        FAST_VIRTUAL(true, false, EnvType.FULL_ENV, false, false, ACC_NATIVE, Annotation.FastNative()),
        FAST_VIRTUAL_REPLACE_THIS(true, false, EnvType.FULL_ENV, true, false, ACC_NATIVE, Annotation.FastNative()),
        CRITICAL(true, false, EnvType.NO_ENV, false, true, ACC_NATIVE | ACC_STATIC, Annotation.CriticalNative());

        final boolean allowObjPtrs;
        final boolean requireNativeStub;
        final EnvType envType;
        final boolean replaceThis;
        final boolean isStatic;
        final int flags;
        final Set<Annotation> annotations;

        CallType(boolean allowObjPtrs, boolean requireNativeStub, EnvType envType, boolean replaceThis,
                 boolean isStatic, int flags, Annotation... annotations) {
            this.allowObjPtrs = allowObjPtrs;
            this.requireNativeStub = requireNativeStub;
            this.envType = envType;
            this.replaceThis = replaceThis;
            this.isStatic = isStatic;
            this.flags = flags | ACC_PRIVATE;
            this.annotations = Set.of(annotations);
        }
    }

    private record SymbolInfo(String name, MapType ret, MapType[] args, SymbolSource source,
                              CallType call_type, boolean requireNativeStub) {

        static SymbolInfo of(Method method, CallSignature signature, SymbolSource source) {
            Objects.requireNonNull(method);
            Objects.requireNonNull(signature);
            Objects.requireNonNull(source);

            CallType call_type = signature.type();
            MapType ret = signature.ret();
            MapType[] args = signature.args();

            if (call_type.replaceThis && (args.length < 1 || args[0] != MapType.OBJECT)) {
                throw new IllegalArgumentException(String.format(
                        "Call type %s requires first object parameter", call_type));
            }
            if (call_type == CallType.CRITICAL) {
                if (Stream.concat(Stream.of(ret), Stream.of(args))
                        .anyMatch(value -> value == MapType.OBJECT)) {
                    throw new IllegalArgumentException("MapType.OBJECT incompatible with CallType.CRITICAL");
                }
            }
            if (!call_type.allowObjPtrs) {
                if (Stream.concat(Stream.of(ret), Stream.of(args))
                        .anyMatch(value -> value == MapType.OBJECT_AS_ADDRESS
                                || value == MapType.OBJECT_AS_RAW_INT)) {
                    throw new IllegalArgumentException(String.format(
                            "Call type %s incompatible with object pointers", call_type));
                }
            }
            boolean requireNativeStub = call_type.requireNativeStub || ret.requireNativeStub
                    || Arrays.stream(args).anyMatch(arg -> arg.requireNativeStub);

            var out = new SymbolInfo(method.getName(), ret, args, source, call_type, requireNativeStub);
            out.checkImplSignature(method);
            return out;
        }

        ProtoId stubProto() {
            return ProtoId.of(TypeId.of(ret.forStub),
                    Arrays.stream(args, call_type.replaceThis ? 1 : 0, args.length)
                            .map(lt -> TypeId.of(lt.forStub)).toArray(TypeId[]::new));
        }

        ProtoId implProto() {
            return ProtoId.of(TypeId.of(ret.forImpl), Arrays.stream(args)
                    .map(lt -> TypeId.of(lt.forImpl)).toArray(TypeId[]::new));
        }

        Class<?>[] stubArgs() {
            return Arrays.stream(args, call_type.replaceThis ? 1 : 0, args.length)
                    .map(lt -> lt.forStub).toArray(Class[]::new);
        }

        Class<?>[] implArgs() {
            return Arrays.stream(args).map(lt -> lt.forImpl).toArray(Class[]::new);
        }

        void checkImplSignature(Method method) {
            if (method.getReturnType() == ret.forImpl) {
                if (Utils.arrayContentsEq(method.getParameterTypes(), implArgs())) {
                    return;
                }
            }
            throw new IllegalStateException(method + " has wrong signature");
        }
    }

    private static final String prefix = "raw_";

    private static byte[] generateJavaStub(Class<?> parent, String impl_name, SymbolInfo[] infos) {
        TypeId impl_id = TypeId.ofName(impl_name);
        ClassDef impl_def = ClassBuilder.build(impl_id, cb -> cb
                .withSuperClass(TypeId.of(parent))
                .withFlags(ACC_PUBLIC)
                .commit(cb2 -> {
                    for (SymbolInfo info : infos) {
                        ProtoId raw_proto = info.stubProto();
                        var raw_id = MethodId.of(impl_id, prefix + info.name, raw_proto);
                        cb.withMethod(mb -> mb
                                .of(raw_id)
                                .withFlags(info.call_type.flags)
                                .withAnnotations(info.call_type.annotations)
                        );

                        ProtoId proto = info.implProto();
                        var id = MethodId.of(impl_id, info.name, proto);
                        final int reserved = 4;
                        int locals = reserved + raw_proto.countInputRegisters() +
                                /* this */ (info.call_type.isStatic ? 0 : 1);
                        int[] regs = {/* call args */ reserved, /* stub args */ 0};

                        cb.withMethod(mb -> mb
                                .of(id)
                                .withFlags(ACC_PUBLIC)
                                .withCode(locals, ib -> {
                                    ib.generate_lines();
                                    if (!info.call_type.isStatic && !info.call_type.replaceThis) {
                                        ib.move_object(ib.l(regs[0]++), ib.this_());
                                    }
                                    for (MapType type : info.args) {
                                        switch (type) {
                                            case BYTE, BOOL, SHORT, CHAR, INT, FLOAT, BOOL_AS_INT ->
                                                    ib.move(ib.l(regs[0]++), ib.p(regs[1]++));
                                            case LONG, DOUBLE -> {
                                                ib.move_wide(ib.l(regs[0]), ib.p(regs[1]));
                                                regs[0] += 2;
                                                regs[1] += 2;
                                            }
                                            case LONG_AS_WORD -> {
                                                if (IS64BIT) {
                                                    ib.move_wide(ib.l(regs[0]), ib.p(regs[1]));
                                                    regs[0] += 2;
                                                } else {
                                                    ib.move_wide(ib.l(0), ib.p(regs[1]));
                                                    ib.unop(LONG_TO_INT, ib.l(0), ib.l(0));
                                                    ib.move(ib.l(regs[0]), ib.l(0));
                                                    regs[0] += 1;
                                                }
                                                regs[1] += 2;
                                            }
                                            case OBJECT, OBJECT_AS_RAW_INT, OBJECT_AS_ADDRESS ->
                                                    ib.move_object(ib.l(regs[0]++), ib.p(regs[1]++));
                                            default -> throw shouldNotReachHere();
                                        }
                                    }

                                    int call_regs = regs[0] - reserved;
                                    var kind = info.call_type.isStatic ? STATIC : DIRECT;
                                    ib.invoke_range(kind, raw_id, call_regs, call_regs == 0 ? 0 : ib.l(reserved));

                                    switch (info.ret) {
                                        case VOID -> ib.return_void();
                                        case BYTE, BOOL, SHORT, CHAR, INT, FLOAT, BOOL_AS_INT -> {
                                            ib.move_result(ib.l(0));
                                            ib.return_(ib.l(0));
                                        }
                                        case LONG, DOUBLE -> {
                                            ib.move_result_wide(ib.l(0));
                                            ib.return_wide(ib.l(0));
                                        }
                                        case LONG_AS_WORD -> {
                                            if (IS64BIT) {
                                                ib.move_result_wide(ib.l(0));
                                                ib.return_wide(ib.l(0));
                                            } else {
                                                ib.move_result(ib.l(0));
                                                ib.unop(INT_TO_LONG, ib.l(0), ib.l(0));
                                                ib.const_wide(ib.l(2), 0xffffffffL);
                                                ib.binop_2addr(AND_LONG, ib.l(0), ib.l(2));
                                                ib.return_wide(ib.l(0));
                                            }
                                        }
                                        case OBJECT, OBJECT_AS_RAW_INT, OBJECT_AS_ADDRESS -> {
                                            ib.move_result_object(ib.l(0));
                                            ib.return_object(ib.l(0));
                                        }
                                        default -> throw shouldNotReachHere();
                                    }
                                })
                        );
                    }
                })
        );

        return DexIO.write(Dex.of(impl_def));
    }

    private static LLVMTypeRef toLLVMType(LLVMContextRef context, MapType type, boolean stub) {
        return switch (type) {
            case VOID -> void_t(context);
            case BOOL -> int1_t(context);
            case BYTE -> int8_t(context);
            case SHORT, CHAR -> int16_t(context);
            case INT, BOOL_AS_INT, OBJECT_AS_RAW_INT -> int32_t(context);
            case FLOAT -> float_t(context);
            case LONG -> int64_t(context);
            case DOUBLE -> double_t(context);
            case OBJECT, LONG_AS_WORD -> intptr_t(context);
            case OBJECT_AS_ADDRESS -> stub ? int32_t(context) : intptr_t(context);
            //noinspection UnnecessaryDefault
            default -> throw shouldNotReachHere();
        };
    }

    private static LLVMTypeRef toLLVMType(LLVMContextRef context, SymbolInfo info, boolean stub) {
        LLVMTypeRef retType = toLLVMType(context, info.ret, stub);
        List<LLVMTypeRef> argTypes = new ArrayList<>(info.args.length + 2);

        for (var type : info.args) {
            argTypes.add(toLLVMType(context, type, stub));
        }
        var call_type = info.call_type;
        if ((call_type.envType == EnvType.FULL_ENV) ||
                (stub && call_type.envType == EnvType.OMIT_ENV)) {
            argTypes.add(0, intptr_t(context)); // env
            if (!call_type.replaceThis) {
                argTypes.add(1, intptr_t(context)); // class or this
            }
        }

        return function_t(retType, argTypes.toArray(new LLVMTypeRef[0]));
    }

    private static byte[] generateNativeStub(SymbolInfo info, long symbol_ptr) {
        final String function_name = "stub";
        return generateFunctionCodeArray((context, module, builder) -> {
            LLVMTypeRef stub_type = toLLVMType(context, info, true);
            LLVMValueRef stub = LLVMAddFunction(module, function_name, stub_type);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(stub, ""));

            LLVMTypeRef target_type = toLLVMType(context, info, false);
            LLVMValueRef target_ptr = build_const_load_ptr(builder, ptr_t(target_type), symbol_ptr);

            LLVMValueRef[] args = LLVMGetParams(stub);
            var call_type = info.call_type;
            if (call_type.envType == EnvType.OMIT_ENV) {
                // drop JNIEnv* and (optional) class or this
                args = Arrays.copyOfRange(args,
                        call_type.replaceThis ? 1 : 2, args.length);
            }
            int index = call_type.envType == EnvType.FULL_ENV ?
                    (call_type.replaceThis ? 1 : 2) : 0;
            for (var type : info.args) {
                if (type == MapType.OBJECT_AS_ADDRESS) {
                    args[index] = buildRawObjectToAddress(builder,
                            args[index], const_intptr(context, 0));
                }
                index++;
            }
            LLVMValueRef ret_val = build_call(builder, target_ptr, args);
            if (info.ret == MapType.VOID) {
                LLVMBuildRetVoid(builder);
            } else {
                if (info.ret == MapType.OBJECT_AS_ADDRESS) {
                    ret_val = buildAddressToRawObject(
                            builder, ret_val, const_intptr(context, 0));
                }
                LLVMBuildRet(builder, ret_val);
            }
        }, function_name);
    }

    private static void processASMs(Arena scope, MemorySegment[] symbols, byte[][] code, int[] map) {
        MemorySegment[] blob = NativeCodeBlob.makeCodeBlob(scope, code);
        for (int i = 0; i < map.length; i++) {
            symbols[map[i]] = blob[i];
        }
    }

    private static void processNativeStubs(Arena scope, SymbolInfo[] infos, MemorySegment[] symbols, int[] map) {
        //TODO: indirect pointers may be unnecessary for some architectures
        MemorySegment pointers = scope.allocate(ADDRESS, map.length);
        byte[][] code = new byte[map.length][];
        for (int i = 0; i < map.length; i++) {
            long offset = i * ADDRESS.byteSize();
            pointers.set(ADDRESS, offset, symbols[map[i]]);
            code[i] = generateNativeStub(infos[map[i]], pointers.nativeAddress() + offset);
        }
        processASMs(scope, symbols, code, map);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> processSymbols(Arena scope, Class<T> parent, ClassLoader loader, SymbolInfo[] infos) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(loader);
        MemorySegment[] symbols = new MemorySegment[infos.length];
        {
            int asm_count = 0;
            int[] asm_map = new int[infos.length];
            byte[][] asm_data = new byte[infos.length][];

            int native_stubs_count = 0;
            int[] native_stubs_map = new int[infos.length];

            for (int i = 0; i < infos.length; i++) {
                SymbolInfo info = infos[i];
                if (info.requireNativeStub) native_stubs_map[native_stubs_count++] = i;
                if (info.source instanceof ASMSource(byte[] code)) {
                    asm_map[asm_count] = i;
                    asm_data[asm_count] = Objects.requireNonNull(code);
                    asm_count++;
                } else if (info.source instanceof SegmentSource(MemorySegment symbol)) {
                    symbols[i] = Objects.requireNonNull(symbol);
                } else {
                    shouldNotReachHere();
                }
            }
            if (asm_count != 0) {
                processASMs(scope, symbols, Arrays.copyOf(asm_data,
                        asm_count), Arrays.copyOf(asm_map, asm_count));
            }
            if (native_stubs_count != 0) {
                processNativeStubs(scope, infos, symbols,
                        Arrays.copyOf(native_stubs_map, native_stubs_count));
            }
        }
        Class<T> impl;
        {
            String impl_name = parent.getName() + "$Impl";
            DexFile dex = openDexFile(generateJavaStub(parent, impl_name, infos));
            impl = (Class<T>) loadClass(dex, impl_name, loader);
            ClassUtils.forceClassVerified(impl);

            Method[] methods = getDeclaredMethods(impl);
            for (int i = 0; i < infos.length; i++) {
                SymbolInfo info = infos[i];
                Method method = searchMethod(methods, prefix + info.name, info.stubArgs());
                registerNativeMethod(method, symbols[i].nativeAddress());
            }
        }
        return impl;
    }

    public enum Tristate {
        TRUE, FALSE, NO_MATTER
    }

    @DoNotShrink
    @DoNotShrinkType
    public @interface Conditions {
        InstructionSet[] arch() default {ARM, ARM64, X86, X86_64, RISCV64};

        @ApiSensitive
        int min_api() default 0;

        @ApiSensitive
        int max_api() default Integer.MAX_VALUE;

        @ApiSensitive
        int min_api_full() default 0;

        @ApiSensitive
        int max_api_full() default Integer.MAX_VALUE;

        @ApiSensitive
        int min_art() default 0;

        @ApiSensitive
        int max_art() default Integer.MAX_VALUE;

        Tristate poisoning() default Tristate.NO_MATTER;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(ASMs.class)
    @DoNotShrink
    @DoNotShrinkType
    public @interface ASM {
        Conditions conditions() default @Conditions();

        byte[] code();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface ASMs {
        ASM[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface ASMGenerator {
        Class<?> klass() default /*search in current class*/ void.class;

        String field() default "";

        String method() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(LibrarySymbols.class)
    @DoNotShrink
    @DoNotShrinkType
    public @interface LibrarySymbol {
        Conditions conditions() default @Conditions();

        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface LibrarySymbols {
        LibrarySymbol[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface SymbolGenerator {
        Class<?> klass() default /*search in current class*/ void.class;

        String field() default "";

        String method() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(CallSignatures.class)
    @DoNotShrink
    @DoNotShrinkType
    public @interface CallSignature {
        Conditions conditions() default @Conditions();

        CallType type();

        MapType ret();

        MapType[] args();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @DoNotShrink
    @DoNotShrinkType
    public @interface CallSignatures {
        CallSignature[] value();
    }

    private static boolean checkPoisoning(BulkLinker.Tristate poisoning) {
        return poisoning == BulkLinker.Tristate.NO_MATTER ||
                ((poisoning == BulkLinker.Tristate.TRUE) == VM.isPoisonReferences());
    }

    private static boolean checkConditions(BulkLinker.Conditions cond) {
        return Utils.contains(cond.arch(), CURRENT_INSTRUCTION_SET) &&
                (cond.min_api() <= SDK_INT && cond.max_api() >= SDK_INT) &&
                (cond.min_api_full() <= SDK_INT_FULL && cond.max_api_full() >= SDK_INT_FULL) &&
                (cond.min_art() <= ART_INDEX && cond.max_art() >= ART_INDEX) &&
                checkPoisoning(cond.poisoning());
    }

    private static byte[] getCode(ASM asm) {
        byte[] code = asm.code();
        if (code == null || code.length == 0) {
            return null;
        }
        return code;
    }

    // TODO: simplify
    private static byte[] getCode(ASMGenerator generator, Class<?> clazz, Map<Class<?>,
            Method[]> cached_methods, Map<Class<?>, Field[]> cached_fields) {
        if (generator.klass() != void.class) {
            clazz = generator.klass();
        }
        byte[] code = null;
        if (!generator.field().isEmpty()) {
            Field field = searchField(cached_fields.computeIfAbsent(clazz, Reflection::getDeclaredFields), generator.field());
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("asm field is not static: " + field);
            }
            if (field.getType() != byte[].class) {
                throw new IllegalArgumentException("Type of asm field is not byte[]: " + field);
            }
            code = nothrows_run(() -> (byte[]) field.get(null));
            if (code == null || code.length == 0) {
                code = null;
            }
        }
        if (code == null && !generator.method().isEmpty()) {
            Method method = searchMethod(cached_methods.computeIfAbsent(clazz, Reflection::getDeclaredMethods), generator.method());
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("asm generator method is not static: " + method);
            }
            if (method.getReturnType() != byte[].class) {
                throw new IllegalArgumentException("Return type of asm generator method is not byte[]: " + method);
            }
            code = nothrows_run(() -> (byte[]) unreflect(method).invokeExact());
            if (code == null || code.length == 0) {
                code = null;
            }
        }
        return code;
    }

    private static ASMSource getASMSource(
            ASM[] asms, ASMGenerator generator, Class<?> clazz, Method method,
            Map<Class<?>, Method[]> cached_methods, Map<Class<?>, Field[]> cached_fields) {
        byte[] code = null;
        for (ASM asm : asms) {
            if (checkConditions(asm.conditions())) {
                code = getCode(asm);
                if (code != null) {
                    break;
                }
            }
        }
        if (DEBUG_BUILD) {
            if (generator != null) {
                byte[] generated_code = getCode(generator, clazz, cached_methods, cached_fields);
                if (code != null && !Arrays.equals(code, generated_code)) {
                    Log.w(LOG_TAG, String.format("Code from ASM(%s) != code from generator(%s) for method %s",
                            Arrays.toString(code), Arrays.toString(generated_code), method));
                }
                code = generated_code;
            }
        } else {
            if (code == null && generator != null) {
                code = getCode(generator, clazz, cached_methods, cached_fields);
            }
        }
        if (code == null) {
            return null;
        }
        return ASMSource.of(code);
    }

    // TODO: simplify
    private static MemorySegment getSymbol(SymbolGenerator generator, Class<?> clazz,
                                           Map<Class<?>, Method[]> cached_methods,
                                           Map<Class<?>, Field[]> cached_fields) {
        if (generator.klass() != void.class) {
            clazz = generator.klass();
        }
        MemorySegment out = null;
        if (!generator.field().isEmpty()) {
            Field field = searchField(cached_fields.computeIfAbsent(clazz, Reflection::getDeclaredFields), generator.field());
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Symbol field is not static: " + field);
            }
            if (field.getType() != MemorySegment.class) {
                throw new IllegalArgumentException("Type of symbol field is not MemorySegment: " + field);
            }
            out = nothrows_run(() -> (MemorySegment) field.get(null));
        }
        if (out == null && !generator.method().isEmpty()) {
            Method method = searchMethod(cached_methods.computeIfAbsent(clazz, Reflection::getDeclaredMethods), generator.method());
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Symbol generator method is not static: " + method);
            }
            if (method.getReturnType() != MemorySegment.class) {
                throw new IllegalArgumentException("Return type of symbol generator method is not MemorySegment: " + method);
            }
            out = nothrows_run(() -> (MemorySegment) unreflect(method).invokeExact());
        }
        return out;
    }

    private static SegmentSource getSegmentSource(
            LibrarySymbol[] syms, SymbolGenerator generator, SymbolLookup lookup,
            Class<?> clazz, Map<Class<?>, Method[]> cached_methods,
            Map<Class<?>, Field[]> cached_fields) {
        MemorySegment symbol = null;
        for (LibrarySymbol sym : syms) {
            if (checkConditions(sym.conditions())) {
                symbol = lookup.find(sym.name()).orElse(null);
                if (symbol != null) {
                    break;
                }
            }
        }
        if (symbol == null && generator != null) {
            symbol = getSymbol(generator, clazz, cached_methods, cached_fields);
        }
        if (symbol == null) {
            return null;
        }
        return SegmentSource.of(symbol);
    }

    private static CallSignature getSignature(CallSignature[] signatures) {
        for (CallSignature signature : signatures) {
            if (checkConditions(signature.conditions())) {
                return signature;
            }
        }
        return null;
    }

    public static <T> Class<T> generateImplClass(Arena arena, Class<T> clazz) {
        return generateImplClass(arena, clazz, RawNativeLibraries.DEFAULT_LOOKUP);
    }

    public static <T> Class<T> generateImplClass(Arena arena, Class<T> clazz, SymbolLookup lookup) {
        return generateImplClass(arena, clazz, clazz.getClassLoader(), lookup);
    }

    public static <T> Class<T> generateImplClass(Arena arena, Class<T> clazz, ClassLoader loader, SymbolLookup lookup) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(loader);
        Objects.requireNonNull(lookup);

        if ((clazz.getModifiers() & (ACC_INTERFACE | ACC_FINAL)) != 0) {
            throw new IllegalArgumentException(
                    "Interfaces and final classes are not allowed" + clazz);
        }

        Map<Class<?>, Method[]> cached_methods = new HashMap<>();
        Map<Class<?>, Field[]> cached_fields = new HashMap<>();
        Method[] sketch_methods = getDeclaredMethods(clazz);
        cached_methods.put(clazz, sketch_methods);
        List<SymbolInfo> infos = new ArrayList<>(sketch_methods.length);

        for (Method method : sketch_methods) {
            CallSignature[] signatures = method.getDeclaredAnnotationsByType(CallSignature.class);
            CallSignature signature = getSignature(signatures);

            // Skip, this method does not require processing
            if (signature == null) continue;

            if (!Modifier.isAbstract(method.getModifiers())) {
                throw new IllegalStateException("Method must be abstract " + method);
            }

            LibrarySymbol[] syms = method.getDeclaredAnnotationsByType(LibrarySymbol.class);
            SymbolGenerator sym_generator = method.getDeclaredAnnotation(SymbolGenerator.class);
            SymbolSource source = getSegmentSource(syms, sym_generator, lookup, clazz, cached_methods, cached_fields);

            if (source == null) {
                ASM[] asms = method.getDeclaredAnnotationsByType(ASM.class);
                ASMGenerator asm_generator = method.getDeclaredAnnotation(ASMGenerator.class);
                source = getASMSource(asms, asm_generator, clazz, method, cached_methods, cached_fields);
            }

            if (source == null) {
                throw new IllegalStateException("Could not find source for method " + method);
            }

            infos.add(SymbolInfo.of(method, signature, source));
        }

        return processSymbols(arena, clazz, loader, infos.toArray(new SymbolInfo[0]));
    }

    public static <T> T generateImpl(Arena arena, Class<T> clazz) {
        return AndroidUnsafe.allocateInstance(generateImplClass(arena, clazz));
    }

    public static <T> T generateImpl(Arena arena, Class<T> clazz, SymbolLookup lookup) {
        return AndroidUnsafe.allocateInstance(generateImplClass(arena, clazz, lookup));
    }

    public static <T> T generateImpl(Arena arena, Class<T> clazz, ClassLoader loader, SymbolLookup lookup) {
        return AndroidUnsafe.allocateInstance(generateImplClass(arena, clazz, loader, lookup));
    }
}

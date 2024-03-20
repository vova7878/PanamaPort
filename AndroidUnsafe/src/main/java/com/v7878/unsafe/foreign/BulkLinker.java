package com.v7878.unsafe.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.VIRTUAL;
import static com.v7878.dex.bytecode.CodeBuilder.UnOp.LONG_TO_INT;
import static com.v7878.dex.bytecode.CodeBuilder.UnOp.NEG_INT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.dex.AnnotationItem;
import com.v7878.dex.AnnotationSet;
import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.ClassUtils.ClassStatus;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.VM;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

import dalvik.system.DexFile;

public class BulkLinker {
    public enum LinkType {
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

        // extra types
        LONG_AS_WORD(IS64BIT ? long.class : int.class, long.class),
        BOOL_AS_INT(int.class, boolean.class),
        @DangerLevel(DangerLevel.VERY_CAREFUL)
        OBJECT_AS_RAW_INT(int.class, Object.class),
        @DangerLevel(DangerLevel.BAD_GC_COLLISION)
        OBJECT_AS_ADDRESS(IS64BIT ? long.class : int.class, Object.class);

        public final Class<?> forStub;
        public final Class<?> forImpl;

        LinkType(Class<?> forStub, Class<?> forImpl) {
            this.forStub = forStub;
            this.forImpl = forImpl;
        }

        LinkType(Class<?> type) {
            this(type, type);
        }
    }

    private static ProtoId stubProto(LinkType ret, LinkType[] args) {
        return new ProtoId(TypeId.of(ret.forStub), Arrays.stream(args)
                .map(lt -> TypeId.of(lt.forStub)).toArray(TypeId[]::new));
    }

    private static ProtoId implProto(LinkType ret, LinkType[] args) {
        return new ProtoId(TypeId.of(ret.forImpl), Arrays.stream(args)
                .map(lt -> TypeId.of(lt.forImpl)).toArray(TypeId[]::new));
    }

    private static Class<?>[] stubArgs(LinkType[] args) {
        return Arrays.stream(args).map(lt -> lt.forStub).toArray(Class[]::new);
    }

    public abstract static sealed class SymbolSource
            permits GeneratorSource, SegmentSource {
    }

    public static final class GeneratorSource extends SymbolSource {
        final Supplier<byte[]> generator;

        private GeneratorSource(Supplier<byte[]> generator) {
            this.generator = generator;
        }

        public static GeneratorSource of(Supplier<byte[]> generator) {
            Objects.requireNonNull(generator);
            return new GeneratorSource(generator);
        }
    }

    public static final class SegmentSource extends SymbolSource {
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

    public static class SymbolInfo {
        final String name;
        final LinkType ret;
        final LinkType[] args;
        final SymbolSource source;
        final CallType call_type;

        private SymbolInfo(String name, LinkType ret, LinkType[] args,
                           SymbolSource source, CallType call_type) {
            this.name = name;
            this.ret = ret;
            this.args = args;
            this.source = source;
            this.call_type = call_type;
        }

        public static SymbolInfo of(String name, CallType call_type, SymbolSource source,
                                    LinkType ret, LinkType... args) {
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
    public static byte[] generateStub(Class<?> parent, SymbolInfo[] infos) {
        String impl_name = parent.getName() + "$Impl";
        TypeId impl_id = TypeId.of(impl_name);
        ClassDef impl_def = new ClassDef(impl_id);
        impl_def.setSuperClass(TypeId.of(parent));

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
            final int reserved = 2;
            int locals = reserved + raw_proto.getInputRegistersCount();
            int[] regs = {/* call args */ reserved, /* stub args */ 0};
            EncodedMethod em = new EncodedMethod(method_id, ACC_PUBLIC).withCode(locals, b -> {
                if (!info.call_type.isStatic) {
                    b.move_object_16(b.l(regs[0]++), b.this_());
                }
                for (LinkType type : info.args) {
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
                                regs[1] += 2;
                            } else {
                                b.move_wide_16(b.l(0), b.p(regs[1]));
                                b.unop(LONG_TO_INT, b.l(0), b.l(0));
                                b.move_16(b.l(regs[0]), b.l(0));
                                regs[0] += 1;
                                regs[1] += 2;
                            }
                        }
                        case OBJECT_AS_RAW_INT -> b.move_object_16(b.l(regs[0]++), b.p(regs[1]++));
                        /*TODO: case OBJECT_AS_ADDRESS -> { }*/
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
                            b.const_4(b.l(1), 0);
                            b.return_wide(b.l(0));
                        }
                    }
                    case OBJECT_AS_RAW_INT -> {
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

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static <T> Class<T> processSymbols(Arena scope, Class<T> parent, ClassLoader loader, SymbolInfo[] infos) {
        MemorySegment[] symbols = new MemorySegment[infos.length];
        {
            int count = 0;
            int[] map = new int[infos.length];
            byte[][] data = new byte[infos.length][];
            for (int i = 0; i < infos.length; i++) {
                SymbolInfo info = infos[i];
                if (info.source instanceof GeneratorSource gs) {
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

    public static <T> Class<T> processSymbols(Arena scope, Class<T> parent, SymbolInfo[] infos) {
        return processSymbols(scope, parent, parent.getClassLoader(), infos);
    }
}

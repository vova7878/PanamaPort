package java.lang.foreign;

import static com.v7878.dex.bytecode.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.staticFieldOffset;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.invoke.Transformers.makeTransformer;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.lang.foreign._Utils.checkSymbol;
import static java.lang.foreign._Utils.unboxSegment;

import com.v7878.dex.AnnotationItem;
import com.v7878.dex.AnnotationSet;
import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Utils.SoftReferenceCache;
import com.v7878.unsafe.foreign.RawNativeLibraries;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.Transformers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import dalvik.system.DexFile;

@SuppressWarnings("deprecation")
final class _AndroidLinkerImpl implements Linker {

    public static final _AndroidLinkerImpl INSTANCE = new _AndroidLinkerImpl();

    private _AndroidLinkerImpl() {
    }

    @Override
    public MethodHandle downcallHandle(
            MemorySegment symbol, FunctionDescriptor descriptor, Option... options) {
        return downcallHandle0(symbol, descriptor, options);
    }

    @Override
    public MethodHandle downcallHandle(FunctionDescriptor descriptor, Option... options) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public MemorySegment upcallStub(MethodHandle target, FunctionDescriptor descriptor,
                                    Arena arena, Option... options) {
        return upcallStub0(target, descriptor, arena, options);
    }

    @Override
    public SymbolLookup defaultLookup() {
        return name -> {
            long tmp = RawNativeLibraries.dlsym(RawNativeLibraries.RTLD_DEFAULT, name);
            return Optional.ofNullable(tmp == 0 ? null : MemorySegment.ofAddress(tmp));
        };
    }

    private static final Set<MemoryLayout> SUPPORTED_LAYOUTS = Set.of(
            JAVA_BOOLEAN,
            JAVA_BYTE,
            JAVA_CHAR,
            JAVA_SHORT,
            JAVA_INT,
            JAVA_FLOAT,
            JAVA_LONG,
            JAVA_DOUBLE,
            ADDRESS
    );

    private static class DowncallLinkRequest {
        public final long symbol;
        public final FunctionDescriptor descriptor;
        //TODO: public final LinkerOptions options

        private DowncallLinkRequest(long symbol, FunctionDescriptor descriptor) {
            this.symbol = symbol;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DowncallLinkRequest lr
                    && symbol == lr.symbol
                    && Objects.equals(descriptor, lr.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, descriptor);
        }
    }

    private static final SoftReferenceCache<DowncallLinkRequest, MethodHandle> DOWNCALL_CACHE = new SoftReferenceCache<>();

    private static class UpcallLinkRequest {
        public final MethodHandle target;
        public final FunctionDescriptor descriptor;
        public final Arena arena;
        //TODO: public final LinkerOptions options

        private UpcallLinkRequest(MethodHandle target, FunctionDescriptor descriptor, Arena arena) {
            this.target = target;
            this.descriptor = descriptor;
            this.arena = arena;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof UpcallLinkRequest lr
                    && Objects.equals(target, lr.target)
                    && Objects.equals(descriptor, lr.descriptor)
                    && Objects.equals(arena, lr.arena);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, descriptor, arena);
        }
    }

    private static final SoftReferenceCache<UpcallLinkRequest, MemorySegment> UPCALL_CACHE = new SoftReferenceCache<>();

    private static MethodHandle downcallHandle0(
            MemorySegment symbol, FunctionDescriptor descriptor, Option... options) {
        checkSymbol(symbol);
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(options);
        checkLayouts(descriptor);
        descriptor = stripNames(descriptor);
        assert_(options.length == 0, UnsupportedOperationException::new); // TODO

        return DOWNCALL_CACHE.get(new DowncallLinkRequest(unboxSegment(symbol), descriptor),
                request -> arrangeDowncall(request.symbol, request.descriptor));
    }

    private static MemorySegment upcallStub0(MethodHandle target, FunctionDescriptor descriptor,
                                             Arena arena, Option... options) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(target);
        Objects.requireNonNull(descriptor);
        checkLayouts(descriptor);
        descriptor = stripNames(descriptor);
        assert_(options.length == 0, UnsupportedOperationException::new); // TODO

        MethodType type = descriptor.toMethodType();
        if (!type.equals(target.type())) {
            throw new IllegalArgumentException("Wrong method handle type: " + target.type());
        }

        return UPCALL_CACHE.get(new UpcallLinkRequest(target, descriptor, arena), request ->
                arrangeUpcall(request.target, request.descriptor, request.arena));
    }

    private static String getStubName(long symbol, ProtoId proto) {
        return _AndroidLinkerImpl.class.getName() + "$$$Stub_"
                + Long.toHexString(symbol) + "_" + proto.getShorty();
    }

    private static Class<?> maybeSegmentToCarrier(Class<?> type) {
        if (type == MemorySegment.class) {
            return IS64BIT ? long.class : int.class;
        }
        return type;
    }

    private static MethodType fixStubCallType(MethodType handle_type) {
        Class<?> ret = maybeSegmentToCarrier(handle_type.returnType());
        Class<?>[] args = new Class[handle_type.parameterCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = maybeSegmentToCarrier(handle_type.parameterType(i));
        }
        return MethodType.methodType(ret, args);
    }

    private static ClassLoader getStubClassLoader() {
        // new every time, needed for GC
        return new ClassLoader(Linker.class.getClassLoader()) {
        };
    }

    //TODO: check segments scope in call
    private static class DowncallArranger implements Transformers.TransformerI {
        private final MethodHandle stub;
        private final Class<?>[] args;
        private final ValueLayout ret;

        public DowncallArranger(MethodHandle stub, FunctionDescriptor descriptor) {
            this.stub = stub;
            this.args = descriptor.argumentLayouts().stream()
                    .map(l -> ((ValueLayout) l).carrier()).toArray(Class[]::new);
            this.ret = (ValueLayout) descriptor.returnLayout().orElse(null);
        }

        private static void copyArg(StackFrameAccessor reader,
                                    StackFrameAccessor writer, Class<?> type) {
            if (type == MemorySegment.class) {
                long value = unboxSegment(reader.nextReference(MemorySegment.class));
                if (IS64BIT) {
                    writer.putNextLong(value);
                } else {
                    writer.putNextInt((int) value);
                }
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        private static void copyRet(StackFrameAccessor reader,
                                    StackFrameAccessor writer, ValueLayout layout) {
            Class<?> type = layout.carrier();
            if (type == MemorySegment.class) {
                MemoryLayout target = ((AddressLayout) layout).targetLayout().orElse(null);
                long size = target == null ? 0 : target.byteSize();
                long alignment = target == null ? 1 : target.byteAlignment();
                long value = IS64BIT ? reader.nextLong() : reader.nextInt() & 0xffffffffL;
                writer.putNextReference(_Utils.longToAddress(value, size, alignment), MemorySegment.class);
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();
            for (Class<?> arg : args) {
                copyArg(thiz_acc, stub_acc, arg);
            }
            invokeExactWithFrameNoChecks(stub, stub_frame);
            if (ret != null) {
                thiz_acc.moveToReturn();
                stub_acc.moveToReturn();
                copyRet(stub_acc, thiz_acc, ret);
            }
        }
    }

    private static MethodHandle arrangeDowncall(long symbol, FunctionDescriptor descriptor) {
        //TODO: check symbol scope in call
        MethodType handle_call_type = descriptor.toMethodType();
        MethodType stub_call_type = fixStubCallType(handle_call_type);

        ProtoId stub_proto = ProtoId.of(stub_call_type);
        String stub_name = getStubName(symbol, stub_proto);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));
        MethodId fid = new MethodId(stub_id, stub_proto, "function");
        stub_def.getClassData().getDirectMethods().add(new EncodedMethod(
                fid, Modifier.NATIVE | Modifier.STATIC,
                new AnnotationSet(
                        AnnotationItem.CriticalNative()
                ), null, null
        ));

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        Class<?> stub = loadClass(dex, stub_name, getStubClassLoader());
        Method function = getDeclaredMethod(stub, "function", stub_call_type.parameterArray());
        setExecutableData(function, symbol);

        MethodHandle handle = unreflect(function);

        if (!stub_call_type.equals(handle_call_type)) {
            handle = makeTransformer(handle_call_type, new DowncallArranger(handle, descriptor));
        }

        return handle;
    }

    private static class UpcallArranger implements Transformers.TransformerI {
        private final MethodHandle stub;
        private final ValueLayout[] args;
        private final Class<?> ret;

        public UpcallArranger(MethodHandle stub, FunctionDescriptor descriptor) {
            this.stub = stub;
            //noinspection SuspiciousToArrayCall
            this.args = descriptor.argumentLayouts().toArray(new ValueLayout[0]);
            //noinspection unchecked,rawtypes
            this.ret = descriptor.returnLayout()
                    .map(l -> ((ValueLayout) l).carrier())
                    .orElse((Class) void.class);
        }

        private static void copyArg(StackFrameAccessor reader,
                                    StackFrameAccessor writer, ValueLayout layout) {
            Class<?> type = layout.carrier();
            if (type == MemorySegment.class) {
                MemoryLayout target = ((AddressLayout) layout).targetLayout().orElse(null);
                long size = target == null ? 0 : target.byteSize();
                long alignment = target == null ? 1 : target.byteAlignment();
                long value = IS64BIT ? reader.nextLong() : reader.nextInt() & 0xffffffffL;
                writer.putNextReference(_Utils.longToAddress(value, size, alignment), MemorySegment.class);
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        private static void copyRet(StackFrameAccessor reader,
                                    StackFrameAccessor writer, Class<?> type) {
            if (type == MemorySegment.class) {
                long value = unboxSegment(reader.nextReference(MemorySegment.class));
                if (IS64BIT) {
                    writer.putNextLong(value);
                } else {
                    writer.putNextInt((int) value);
                }
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();
            for (ValueLayout arg : args) {
                copyArg(thiz_acc, stub_acc, arg);
            }
            invokeExactWithFrameNoChecks(stub, stub_frame);
            if (ret != void.class) {
                thiz_acc.moveToReturn();
                stub_acc.moveToReturn();
                copyRet(stub_acc, thiz_acc, ret);
            }
        }
    }

    private static String getStubName(MethodHandle target, ProtoId proto, Arena arena) {
        return _AndroidLinkerImpl.class.getName() + "$$$Stub_"
                + Integer.toHexString(target.hashCode()) + "_"
                + proto.getShorty() + "_" + Integer.toHexString(arena.hashCode());
    }

    private static MemorySegment arrangeUpcall(
            MethodHandle target, FunctionDescriptor descriptor, Arena arena) {
        MethodType stub_call_type = fixStubCallType(target.type());

        if (!stub_call_type.equals(target.type())) {
            target = makeTransformer(stub_call_type, new UpcallArranger(target, descriptor));
        }

        ProtoId stub_proto = ProtoId.of(stub_call_type);
        String stub_name = getStubName(target, stub_proto, arena);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));

        FieldId tid = new FieldId(stub_id, TypeId.of(MethodHandle.class), "target");
        stub_def.getClassData().getStaticFields()
                .add(new EncodedField(tid, Modifier.STATIC, null));

        MethodId invokeExact = new MethodId(TypeId.of(MethodHandle.class),
                new ProtoId(TypeId.of(Object.class), TypeId.of(Object[].class)), "invokeExact");

        MethodId fid = new MethodId(stub_id, stub_proto, "function");
        stub_def.getClassData().getDirectMethods().add(new EncodedMethod(
                fid, Modifier.STATIC).withCode(2 /* locals for wide result */, b -> {
                    b.sop(GET_OBJECT, b.l(1), tid);
                    b.invoke_polymorphic_range(invokeExact, stub_proto,
                            stub_proto.getInputRegistersCount() + /* target */ 1, b.l(1));
                    switch (stub_proto.getReturnType().getShorty()) {
                        case 'V' -> b.return_void();
                        case 'L' -> b.move_result_object(b.l(0))
                                .return_object(b.l(0));
                        case 'J', 'D' -> b.move_result_wide(b.l(0))
                                .return_wide(b.l(0));
                        default -> b.move_result(b.l(0)).
                                return_(b.l(0));
                    }
                }
        ));

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        Class<?> stub = loadClass(dex, stub_name, getStubClassLoader());

        putObject(stub, staticFieldOffset(getDeclaredField(stub, "target")), target);

        return makeUpCallStubNativeCode(
                getDeclaredMethod(stub, "function", stub_call_type.parameterArray()),
                stub_proto.getReturnType().getShorty(), arena);
    }

    private static byte[] long_to_arr(long v) {
        return new byte[]{
                (byte) (v),
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24),
                (byte) (v >> 32),
                (byte) (v >> 40),
                (byte) (v >> 48),
                (byte) (v >> 56)};
    }

    private static MemorySegment makeUpCallStubNativeCode(
            Method function, char r_shorty, Arena arena) {
        long clazz = JNIUtils.NewGlobalRef(function.getDeclaringClass(), arena);
        long method_id = JNIUtils.FromReflectedMethod(function);
        long jvm = JNIUtils.getJavaVMPtr().address();

        byte[] j = long_to_arr(jvm);
        byte[] c = long_to_arr(clazz);
        byte[] m = long_to_arr(method_id);

        byte[] code;

        //<type> stub(...) {
        //    JavaVM* vm = (JavaVM*) <j>;
        //    JNIEnv* env;
        //
        //    int res = (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6);
        //    if (res != JNI_OK) {
        //        exit(res);
        //    }
        //
        //    va_list args;
        //    va_start(args);
        //
        //    jclass clazz = (jclass) <c>;
        //    jmethodID m_id = (jmethodID) <m>;
        //    <type> r = (*env)->CallStatic<type>MethodV(env, clazz, m_id, args);
        //
        //    va_end(args);
        //    return r;
        //}

        switch (NativeCodeBlob.CURRENT_INSTRUCTION_SET) {
            case X86_64:
                byte[] call = switch (r_shorty) {
                    case 'Z' -> new byte[]{(byte) 0xb0, 0x03};
                    case 'B' -> new byte[]{(byte) 0xc8, 0x03};
                    case 'C' -> new byte[]{(byte) 0xe0, 0x03};
                    case 'S' -> new byte[]{(byte) 0xf8, 0x03};
                    case 'I' -> new byte[]{0x10, 0x04};
                    case 'F' -> new byte[]{0x40, 0x04};
                    case 'J' -> new byte[]{0x28, 0x04};
                    case 'D' -> new byte[]{0x58, 0x04};
                    case 'V' -> new byte[]{0x70, 0x04};
                    default -> throw new AssertionError("Unexpected return type: " + r_shorty);
                };
                code = new byte[]{
                        (byte) 0x48, (byte) 0x81, (byte) 0xec, (byte) 0xd8, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                        (byte) 0x48, (byte) 0x89, (byte) 0x7c, (byte) 0x24, (byte) 0x20,
                        (byte) 0x48, (byte) 0x89, (byte) 0x74, (byte) 0x24, (byte) 0x28,
                        (byte) 0x48, (byte) 0x89, (byte) 0x54, (byte) 0x24, (byte) 0x30,
                        (byte) 0x48, (byte) 0x89, (byte) 0x4c, (byte) 0x24, (byte) 0x38,
                        (byte) 0x4c, (byte) 0x89, (byte) 0x44, (byte) 0x24, (byte) 0x40,
                        (byte) 0x4c, (byte) 0x89, (byte) 0x4c, (byte) 0x24, (byte) 0x48,

                        (byte) 0x84, (byte) 0xc0,
                        (byte) 0x74, (byte) 0x37,

                        (byte) 0x0f, (byte) 0x29, (byte) 0x44, (byte) 0x24, (byte) 0x50,
                        (byte) 0x0f, (byte) 0x29, (byte) 0x4c, (byte) 0x24, (byte) 0x60,
                        (byte) 0x0f, (byte) 0x29, (byte) 0x54, (byte) 0x24, (byte) 0x70,
                        (byte) 0x0f, (byte) 0x29, (byte) 0x9c, (byte) 0x24, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x0f, (byte) 0x29, (byte) 0xa4, (byte) 0x24, (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x0f, (byte) 0x29, (byte) 0xac, (byte) 0x24, (byte) 0xa0, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x0f, (byte) 0x29, (byte) 0xb4, (byte) 0x24, (byte) 0xb0, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x0f, (byte) 0x29, (byte) 0xbc, (byte) 0x24, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                        (byte) 0x48, (byte) 0xbf, j[0], j[1], j[2], j[3], j[4], j[5], j[6], j[7],

                        (byte) 0x48, (byte) 0x8b, (byte) 0x07,
                        (byte) 0xba, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                        (byte) 0x48, (byte) 0x8d, (byte) 0x74, (byte) 0x24, (byte) 0x18,

                        (byte) 0xff, (byte) 0x50, (byte) 0x30,

                        (byte) 0x85, (byte) 0xc0,
                        (byte) 0x75, (byte) 0x53,
                        (byte) 0xc7, (byte) 0x04, (byte) 0x24, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0xc7, (byte) 0x44, (byte) 0x24, (byte) 0x04, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x48, (byte) 0x8d, (byte) 0x84, (byte) 0x24, (byte) 0xe0, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                        (byte) 0x48, (byte) 0x89, (byte) 0x44, (byte) 0x24, (byte) 0x08,
                        (byte) 0x48, (byte) 0x8d, (byte) 0x44, (byte) 0x24, (byte) 0x20,
                        (byte) 0x48, (byte) 0x89, (byte) 0x44, (byte) 0x24, (byte) 0x10,
                        (byte) 0x48, (byte) 0x8B, (byte) 0x7C, (byte) 0x24, (byte) 0x18,
                        (byte) 0x48, (byte) 0x89, (byte) 0xE1,
                        (byte) 0x48, (byte) 0x8B, (byte) 0x07,
                        (byte) 0x48, (byte) 0xBA, m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7],
                        (byte) 0x48, (byte) 0xBE, c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7],
                        (byte) 0xFF, (byte) 0x90, call[0], call[1], (byte) 0x00, (byte) 0x00,
                        (byte) 0x48, (byte) 0x81, (byte) 0xC4, (byte) 0xD8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0xC3,
                        (byte) 0x89, (byte) 0xC7,
                        (byte) 0xE8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        // TODO: exit pointer
                        0, 0, 0, 0, 0, 0, 0, 0
                };
                break;
            default:
                //TODO
                throw new UnsupportedOperationException("Unsupported yet");
        }
        return NativeCodeBlob.makeCodeBlob(arena, code)[0];
    }

    private static void checkLayouts(FunctionDescriptor descriptor) {
        descriptor.returnLayout().ifPresent(_AndroidLinkerImpl::checkLayout);
        descriptor.argumentLayouts().forEach(_AndroidLinkerImpl::checkLayout);
    }

    private static void checkLayout(MemoryLayout layout) {
        if (layout instanceof ValueLayout vl) {
            checkSupported(vl);
        } else {
            // TODO
            throw new IllegalArgumentException("Unsupported layout: " + layout);
        }
    }

    private static void checkSupported(ValueLayout valueLayout) {
        if (valueLayout instanceof AddressLayout addressLayout) {
            valueLayout = addressLayout.withoutTargetLayout();
        }
        if (!SUPPORTED_LAYOUTS.contains(valueLayout.withoutName())) {
            throw new IllegalArgumentException("Unsupported layout: " + valueLayout);
        }
    }

    private static MemoryLayout stripNames(MemoryLayout ml) {
        // we don't care about transferring alignment and byte order here
        // since the linker already restricts those such that they will always be the same
        if (ml instanceof AddressLayout al) {
            return al.targetLayout()
                    .map(tl -> al.withoutName().withTargetLayout(
                            MemoryLayout.paddingLayout(tl.byteSize())
                                    .withByteAlignment(tl.byteAlignment())))
                    .orElseGet(al::withoutName);
        }
        return ml.withoutName();
    }

    private static MemoryLayout[] stripNames(List<MemoryLayout> layouts) {
        return layouts.stream()
                .map(_AndroidLinkerImpl::stripNames)
                .toArray(MemoryLayout[]::new);
    }

    private static FunctionDescriptor stripNames(FunctionDescriptor function) {
        return function.returnLayout()
                .map(rl -> FunctionDescriptor.of(stripNames(rl), stripNames(function.argumentLayouts())))
                .orElseGet(() -> FunctionDescriptor.ofVoid(stripNames(function.argumentLayouts())));
    }
}

package java.lang.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.ArtMethodUtils.setExecutableData;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.invoke.Transformers.makeTransformer;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign._Utils.checkSymbol;
import static java.lang.foreign._Utils.unboxSegment;

import com.v7878.dex.AnnotationItem;
import com.v7878.dex.AnnotationSet;
import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
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
        // TODO
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public SymbolLookup defaultLookup() {
        return name -> {
            long tmp = RawNativeLibraries.dlsym(RawNativeLibraries.RTLD_DEFAULT, name);
            return Optional.ofNullable(tmp == 0 ? null : MemorySegment.ofAddress(tmp));
        };
    }

    private static final Set<MemoryLayout> SUPPORTED_LAYOUTS = Set.of(
            ValueLayout.JAVA_BOOLEAN,
            JAVA_BYTE,
            ValueLayout.JAVA_CHAR,
            ValueLayout.JAVA_SHORT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_DOUBLE,
            ValueLayout.ADDRESS
    );

    private static class LinkRequest {
        public final long symbol;
        public final FunctionDescriptor descriptor;
        //TODO: public final LinkerOptions options

        private LinkRequest(long symbol, FunctionDescriptor descriptor) {
            this.symbol = symbol;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LinkRequest lr
                    && symbol == lr.symbol
                    && Objects.equals(descriptor, lr.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, descriptor);
        }
    }

    private static final SoftReferenceCache<LinkRequest, MethodHandle> DOWNCALL_CACHE = new SoftReferenceCache<>();

    private static MethodHandle downcallHandle0(
            MemorySegment symbol, FunctionDescriptor descriptor, Option... options) {
        checkSymbol(symbol);
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(options);
        checkLayouts(descriptor);
        descriptor = stripNames(descriptor);
        assert_(options.length == 0, UnsupportedOperationException::new); // TODO

        return DOWNCALL_CACHE.get(new LinkRequest(unboxSegment(symbol), descriptor),
                request -> arrangeDowncall(request.symbol, request.descriptor));
    }

    private static String getStubName(long symbol, ProtoId proto) {
        return _AndroidLinkerImpl.class.getName() + "$$$Stub_"
                + symbol + "_" + proto.getShorty();
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
    private static class Arranger implements Transformers.TransformerI {
        private final MethodHandle stub;
        private final Class<?>[] args;
        private final ValueLayout ret;

        public Arranger(MethodHandle stub, FunctionDescriptor descriptor) {
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

        ProtoId proto = ProtoId.of(stub_call_type);
        String stub_name = getStubName(symbol, proto);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));
        MethodId fid = new MethodId(stub_id, proto, "function");
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
            handle = makeTransformer(handle_call_type, new Arranger(handle, descriptor));
        }

        return handle;
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

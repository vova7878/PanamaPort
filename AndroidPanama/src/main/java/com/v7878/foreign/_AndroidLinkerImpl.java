package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign._CapturableState.ERRNO;
import static com.v7878.foreign._Utils.moveArgument;
import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddInstrAttribute;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMAttribute.LLVMByValAttribute;
import static com.v7878.llvm.Core.LLVMAttribute.LLVMStructRetAttribute;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildNeg;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMBuildZExtOrBitCast;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetInstrParamAlignment;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.llvm.LLVMGlobals.double_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.float_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int16_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int1_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int32_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int64_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int8_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.intptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.newContext;
import static com.v7878.unsafe.llvm.LLVMGlobals.newDefaultMachine;
import static com.v7878.unsafe.llvm.LLVMGlobals.void_ptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.getFunctionCode;

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
import com.v7878.foreign._StorageDescriptor.LLVMStorage;
import com.v7878.foreign._StorageDescriptor.MemoryStorage;
import com.v7878.foreign._StorageDescriptor.NoStorage;
import com.v7878.foreign._StorageDescriptor.RawStorage;
import com.v7878.foreign._StorageDescriptor.WrapperStorage;
import com.v7878.invoke.VarHandle;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.unsafe.invoke.Transformers.TransformerI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import dalvik.system.DexFile;

final class _AndroidLinkerImpl extends _AbstractAndroidLinker {
    public static final Linker INSTANCE = new _AndroidLinkerImpl();
    public static final VarHandle VH_ERRNO = _CapturableState.LAYOUT.varHandle(groupElement("errno"));

    private static class DowncallArranger implements TransformerI {
        private final MethodHandle stub;
        private final Class<?>[] args;
        private final ValueLayout ret;
        private final boolean allowsHeapAccess;
        private final int capturedStateMask;
        private final int segment_params_count;

        public DowncallArranger(MethodHandle stub, Class<?>[] args, ValueLayout ret,
                                boolean allowsHeapAccess, int capturedStateMask) {
            this.stub = stub;
            this.args = args;
            this.ret = ret;
            this.allowsHeapAccess = allowsHeapAccess;
            this.capturedStateMask = capturedStateMask;
            this.segment_params_count = Arrays.stream(args)
                    .mapToInt(t -> t == MemorySegment.class ? 1 : 0)
                    .reduce(Integer::sum).orElse(0)
                    + (capturedStateMask < 0 ? 1 : 0);
        }

        private void copyArg(StackFrameAccessor reader, StackFrameAccessor writer,
                             Class<?> type, List<_MemorySessionImpl> acquiredSessions) {
            if (type == MemorySegment.class) {
                _AbstractMemorySegmentImpl segment = (_AbstractMemorySegmentImpl)
                        reader.nextReference(MemorySegment.class);
                segment.scope.acquire0();
                acquiredSessions.add(segment.scope);
                long value;
                if (allowsHeapAccess) {
                    Object base = segment.unsafeGetBase();
                    writer.putNextReference(base, Object.class);
                    value = segment.unsafeGetOffset();
                } else {
                    value = _Utils.unboxSegment(segment);
                }
                if (IS64BIT) {
                    writer.putNextLong(value);
                } else {
                    writer.putNextInt((int) value);
                }
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        private void copyRet(StackFrameAccessor reader, StackFrameAccessor writer,
                             ValueLayout layout) {
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

            List<_MemorySessionImpl> acquiredSessions = new ArrayList<>(segment_params_count);
            try {
                _AbstractMemorySegmentImpl capturedState = null;
                if (capturedStateMask < 0) {
                    capturedState = (_AbstractMemorySegmentImpl) thiz_acc.nextReference(MemorySegment.class);
                    capturedState.scope.acquire0();
                    acquiredSessions.add(capturedState.scope);
                }

                for (Class<?> arg : args) {
                    copyArg(thiz_acc, stub_acc, arg, acquiredSessions);
                }

                invokeExactWithFrameNoChecks(stub, stub_frame);

                if ((capturedStateMask & ERRNO.mask()) != 0) {
                    VH_ERRNO.set(capturedState, 0, Errno.errno());
                }

                if (ret != null) {
                    thiz_acc.moveToReturn();
                    stub_acc.moveToReturn();
                    copyRet(stub_acc, thiz_acc, ret);
                }
            } finally {
                for (_MemorySessionImpl session : acquiredSessions) {
                    session.release0();
                }
            }
        }
    }

    private static Class<?> carrierTypeFor(MemoryLayout layout) {
        if (layout instanceof ValueLayout valueLayout) {
            return valueLayout.carrier();
        } else if (layout instanceof GroupLayout || layout instanceof SequenceLayout) {
            return MemorySegment.class;
        } else {
            // Note: we should not worry about padding layouts, as they cannot be present in a function descriptor
            throw shouldNotReachHere();
        }
    }

    public static MethodType fdToHandleMethodType(_FunctionDescriptorImpl descriptor) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        Class<?> returnValue = retLayout != null ? carrierTypeFor(retLayout) : void.class;
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        Class<?>[] argCarriers = new Class<?>[argLayouts.size()];
        for (int i = 0; i < argCarriers.length; i++) {
            argCarriers[i] = carrierTypeFor(argLayouts.get(i));
        }
        return MethodType.methodType(returnValue, argCarriers);
    }

    public static MethodType fdToStubMethodType(_FunctionDescriptorImpl descriptor, boolean allowsHeapAccess) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        Class<?> returnValue = retLayout != null ? carrierTypeFor(retLayout) : void.class;
        if (returnValue == MemorySegment.class) {
            returnValue = WORD.carrier();
        }
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        List<Class<?>> argCarriers = new ArrayList<>(argLayouts.size());
        for (MemoryLayout tmp : argLayouts) {
            Class<?> carrier = carrierTypeFor(tmp);
            if (carrier == MemorySegment.class) {
                if (allowsHeapAccess) {
                    argCarriers.add(Object.class);
                }
                argCarriers.add(WORD.carrier());
            } else {
                argCarriers.add(carrier);
            }
        }
        return MethodType.methodType(returnValue, argCarriers);
    }

    private static MethodType replaceObjectParameters(MethodType stubType) {
        return MethodType.methodType(stubType.returnType(), stubType.parameterList().stream()
                .map(a -> a == Object.class ? int.class : a).toArray(Class[]::new));
    }

    private static String getStubName(ProtoId proto) {
        return _AndroidLinkerImpl.class.getName() + "$Stub_" + proto.getShorty();
    }

    private static MethodHandle generateJavaDowncallStub(
            MethodType stubType, _FunctionDescriptorImpl stub_descriptor,
            _StorageDescriptor target_descriptor, _LinkerOptions options) {
        final String method_name = "function";
        final String field_name = "scope";

        MethodType gType = replaceObjectParameters(stubType);
        ProtoId stub_proto = ProtoId.of(gType);
        String stub_name = getStubName(stub_proto);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));

        FieldId scope_id = new FieldId(stub_id, TypeId.of(Arena.class), field_name);
        stub_def.getClassData().getStaticFields().add(new EncodedField(
                scope_id, ACC_PRIVATE | ACC_STATIC, null));

        MethodId fid = new MethodId(stub_id, stub_proto, method_name);
        stub_def.getClassData().getDirectMethods().add(new EncodedMethod(
                fid, ACC_NATIVE | ACC_STATIC,
                options.isCritical() ? new AnnotationSet(AnnotationItem.CriticalNative()) : null,
                null, null
        ));

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        Class<?> stub_class = loadClass(dex, stub_name, Utils.newEmptyClassLoader());

        Arena scope = JavaForeignAccess.createImplicitHeapArena(stub_class);

        Field field = getDeclaredField(stub_class, field_name);
        putObject(stub_class, fieldOffset(field), scope);

        Method function = getDeclaredMethod(stub_class, method_name, gType.parameterArray());
        registerNativeMethod(function, generateNativeDowncallStub(scope, target_descriptor, stub_descriptor, options).nativeAddress());

        MethodHandle stub = unreflect(function);
        Reflection.setMethodType(stub, stubType);
        return stub;
    }

    // Note: all layouts already has natural alignment
    private static LLVMTypeRef layoutToLLVMType(LLVMContextRef context, MemoryLayout layout) {
        Objects.requireNonNull(layout);
        LLVMTypeRef out;
        if (layout instanceof ValueLayout.OfByte) {
            out = int8_t(context);
        } else if (layout instanceof ValueLayout.OfBoolean) {
            out = int1_t(context);
        } else if (layout instanceof ValueLayout.OfShort || layout instanceof ValueLayout.OfChar) {
            out = int16_t(context);
        } else if (layout instanceof ValueLayout.OfInt) {
            out = int32_t(context);
        } else if (layout instanceof ValueLayout.OfFloat) {
            out = float_t(context);
        } else if (layout instanceof ValueLayout.OfLong) {
            out = int64_t(context);
        } else if (layout instanceof ValueLayout.OfDouble) {
            out = double_t(context);
        } else if (layout instanceof AddressLayout addressLayout) {
            out = LLVMPointerType(addressLayout.targetLayout()
                    .map(target -> layoutToLLVMType(context, target))
                    .orElse(void_t(context)), 0);
        } else if (layout instanceof UnionLayout) {
            // TODO: it`s ok?
            out = LLVMArrayType(int8_t(context), Math.toIntExact(layout.byteSize()));
        } else if (layout instanceof SequenceLayout sequence) {
            out = LLVMArrayType(layoutToLLVMType(context, sequence.elementLayout()),
                    Math.toIntExact(sequence.elementCount()));
        } else if (layout instanceof StructLayout struct) {
            List<MemoryLayout> members = struct.memberLayouts();
            List<LLVMTypeRef> elements = new ArrayList<>(members.size());
            for (MemoryLayout member : members) {
                if (!(member instanceof PaddingLayout)) {
                    elements.add(layoutToLLVMType(context, member));
                }
            }
            out = LLVMStructTypeInContext(context, elements.toArray(new LLVMTypeRef[0]), false);
            // TODO: check offsets
        } else {
            throw shouldNotReachHere();
        }
        return out;
    }

    private static LLVMTypeRef getPointerType(LLVMContextRef context, MemoryLayout layout) {
        if (layout instanceof AddressLayout addressLayout) {
            return layoutToLLVMType(context, addressLayout);
        }
        if (layout instanceof GroupLayout) {
            return LLVMPointerType(layoutToLLVMType(context, layout), 0);
        }
        throw shouldNotReachHere();
    }

    private static LLVMTypeRef fdToStubLLVMType(LLVMContextRef context, _FunctionDescriptorImpl descriptor,
                                                boolean allowsHeapAccess, boolean isCritical) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        LLVMTypeRef returnType = retLayout == null ? void_t(context) : layoutToLLVMType(context, retLayout);
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());
        for (MemoryLayout layout : argLayouts) {
            if (layout instanceof AddressLayout || layout instanceof GroupLayout) {
                if (allowsHeapAccess) {
                    argTypes.add(int32_t(context));
                    argTypes.add(intptr_t(context));
                } else {
                    argTypes.add(getPointerType(context, layout));
                }
            } else if (layout instanceof ValueLayout) {
                argTypes.add(layoutToLLVMType(context, layout));
            } else {
                throw shouldNotReachHere();
            }
        }
        if (!isCritical) {
            argTypes.add(0, void_ptr_t(context)); // JNIEnv*
            argTypes.add(1, void_ptr_t(context)); // jclass
        }
        return LLVMFunctionType(returnType, argTypes.toArray(new LLVMTypeRef[0]), false);
    }

    private static LLVMTypeRef sdToTargetLLVMType(LLVMContextRef context, _StorageDescriptor descriptor, int firstVariadicArgIndex) {
        LLVMStorage retStorage = descriptor.returnStorage();
        LLVMStorage[] argStorages = descriptor.argumentStorages();
        if (firstVariadicArgIndex >= 0) {
            argStorages = Arrays.copyOfRange(argStorages, 0, firstVariadicArgIndex);
        }

        LLVMTypeRef retType;
        List<LLVMTypeRef> argTypes = new ArrayList<>(argStorages.length);

        if (retStorage instanceof NoStorage) {
            retType = void_t(context);
        } else if (retStorage instanceof RawStorage) {
            retType = layoutToLLVMType(context, retStorage.layout);
        } else if (retStorage instanceof WrapperStorage ws) {
            retType = layoutToLLVMType(context, ws.wrapper);
        } else if (retStorage instanceof MemoryStorage) {
            // pass as pointer argument with "sret" attribute
            retType = void_t(context);
            argTypes.add(layoutToLLVMType(context, ADDRESS.withTargetLayout(retStorage.layout)));
        } else {
            throw shouldNotReachHere();
        }

        for (LLVMStorage storage : argStorages) {
            if (storage instanceof NoStorage) {
                // just drop
            } else if (storage instanceof RawStorage) {
                argTypes.add(layoutToLLVMType(context, storage.layout));
            } else if (storage instanceof WrapperStorage ws) {
                argTypes.add(layoutToLLVMType(context, ws.wrapper));
            } else if (storage instanceof MemoryStorage) {
                // pass as pointer with "byval" attribute
                argTypes.add(layoutToLLVMType(context, ADDRESS.withTargetLayout(storage.layout)));
            } else {
                throw shouldNotReachHere();
            }
        }

        return LLVMFunctionType(retType, argTypes.toArray(new LLVMTypeRef[0]), firstVariadicArgIndex >= 0);
    }

    private static MemorySegment generateNativeDowncallStub(
            Arena scope, _StorageDescriptor target_descriptor,
            _FunctionDescriptorImpl stub_descriptor, _LinkerOptions options) {
        final String function_name = "stub";
        try (var context = newContext(); var builder = LLVMCreateBuilderInContext(context);
             var module = LLVMModuleCreateWithNameInContext("generic", context)) {

            LLVMTypeRef stub_type = fdToStubLLVMType(context, stub_descriptor, options.allowsHeapAccess(), options.isCritical());
            LLVMValueRef stub = LLVMAddFunction(module, function_name, stub_type);

            LLVMTypeRef target_type = sdToTargetLLVMType(context, target_descriptor, options.firstVariadicArgIndex());
            LLVMTypeRef target_type_ptr = LLVMPointerType(target_type, 0);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(stub, ""));

            LLVMValueRef[] stub_args;
            if (options.allowsHeapAccess()) {
                List<LLVMValueRef> out = new ArrayList<>();
                LLVMValueRef[] tmp = LLVMGetParams(stub);
                int t = 0;
                for (MemoryLayout layout : stub_descriptor.argumentLayouts()) {
                    if (layout instanceof GroupLayout || layout instanceof AddressLayout) {
                        LLVMValueRef base = tmp[t++];
                        LLVMValueRef offset = tmp[t++];
                        if (VM.isPoisonReferences()) {
                            base = LLVMBuildNeg(builder, base, "");
                        }
                        base = LLVMBuildZExtOrBitCast(builder, base, intptr_t(context), "");
                        base = LLVMBuildAdd(builder, base, offset, "");
                        out.add(LLVMBuildIntToPtr(builder, base, getPointerType(context, layout), ""));
                    } else if (layout instanceof ValueLayout) {
                        out.add(tmp[t++]);
                    } else {
                        throw shouldNotReachHere();
                    }
                }
                stub_args = out.toArray(new LLVMValueRef[0]);
            } else {
                stub_args = LLVMGetParams(stub);
                if (!options.isCritical()) {
                    // drop JNIEnv* and jclass
                    stub_args = Arrays.copyOfRange(stub_args, 2, stub_args.length);
                }
            }
            assert stub_args.length == stub_descriptor.argumentLayouts().size();

            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in stub_args[]
            LLVMValueRef target = LLVMBuildPointerCast(builder, stub_args[index++], target_type_ptr, "");
            LLVMValueRef[] target_args = new LLVMValueRef[stub_args.length - index];
            int[] attrs = new int[target_args.length];
            int[] aligns = new int[target_args.length];
            boolean retVoid;
            MemoryLayout retStore;
            {
                LLVMStorage retStorage = target_descriptor.returnStorage();
                if (retStorage instanceof NoStorage) {
                    retVoid = true;
                    retStore = null;
                    if (options.isReturnInMemory()) {
                        index++; // just drop
                    }
                } else if (retStorage instanceof RawStorage) {
                    retVoid = false;
                    retStore = null;
                } else if (retStorage instanceof WrapperStorage ws) {
                    retVoid = false;
                    // Note: store layout, not wrapper
                    retStore = ws.layout;
                    stub_args[index] = LLVMBuildPointerCast(builder, stub_args[index],
                            layoutToLLVMType(context, ADDRESS.withTargetLayout(ws.wrapper)), "");
                    index++;
                } else if (retStorage instanceof MemoryStorage) {
                    // pass as pointer argument with "sret" attribute
                    retVoid = true;
                    retStore = null;
                    attrs[count] = LLVMStructRetAttribute;
                    // Note: alignment from layout, not wrapper
                    aligns[count] = Math.toIntExact(retStorage.layout.byteAlignment());
                    target_args[count] = stub_args[index];
                    index++;
                    count++;
                } else {
                    throw shouldNotReachHere();
                }
            }
            {
                int start = index;
                while (index < stub_args.length) {
                    LLVMStorage storage = target_descriptor.argumentStorage(index - start);
                    if (storage instanceof NoStorage) {
                        index++; // just drop
                    } else if (storage instanceof RawStorage) {
                        target_args[count++] = stub_args[index++];
                    } else if (storage instanceof WrapperStorage ws) {
                        stub_args[index] = LLVMBuildPointerCast(builder, stub_args[index],
                                layoutToLLVMType(context, ADDRESS.withTargetLayout(ws.wrapper)), "");
                        target_args[count] = LLVMBuildLoad(builder, stub_args[index], "");
                        // Note: alignment from layout, not wrapper
                        LLVMSetAlignment(target_args[count], Math.toIntExact(ws.layout.byteAlignment()));
                        index++;
                        count++;
                    } else if (storage instanceof MemoryStorage) {
                        // pass as pointer with "byval" attribute
                        attrs[count] = LLVMByValAttribute;
                        aligns[count] = Math.toIntExact(storage.layout.byteAlignment());
                        target_args[count] = stub_args[index];
                        index++;
                        count++;
                    } else {
                        throw shouldNotReachHere();
                    }
                }
            }

            LLVMValueRef call = LLVMBuildCall(builder, target, Arrays.copyOf(target_args, count), "");

            final int offset = 1; // Note: first index is 1, not 0
            for (int i = 0; i < count; i++) {
                if (attrs[i] != 0) {
                    LLVMAddInstrAttribute(call, i + offset, attrs[i]);
                }
                if (aligns[i] != 0) {
                    LLVMSetInstrParamAlignment(call, i + offset, aligns[i]);
                }
            }

            if (retVoid) {
                LLVMBuildRetVoid(builder);
            } else {
                if (retStore == null) {
                    LLVMBuildRet(builder, call);
                } else {
                    final int ret_index = 1;
                    LLVMValueRef store = LLVMBuildStore(builder, call, stub_args[ret_index]);
                    LLVMSetAlignment(store, Math.toIntExact(retStore.byteAlignment()));
                    LLVMBuildRetVoid(builder);
                }
            }

            LLVMVerifyModule(module);

            try (var machine = newDefaultMachine()) {
                LLVMMemoryBufferRef buf = LLVMTargetMachineEmitToMemoryBuffer(
                        machine, module, LLVMObjectFile);
                try (var of = LLVMCreateObjectFile(buf)) {
                    byte[] code = getFunctionCode(of, function_name).toArray(JAVA_BYTE);
                    return NativeCodeBlob.makeCodeBlob(scope, code)[0];
                }
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    private static class ReturnWrapper implements TransformerI {
        private final MethodHandle handle;
        private final MemoryLayout ret_layout;
        private final int ret_index;

        public ReturnWrapper(MethodHandle handle, MemoryLayout ret_layout, int ret_index) {
            this.handle = handle;
            this.ret_layout = ret_layout;
            this.ret_index = ret_index;
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            Arena arena = thiz_acc.getReference(ret_index, Arena.class);
            MemorySegment ret = arena.allocate(ret_layout);
            MethodType cached_type = stack.type();
            stack.setType(handle.type());
            thiz_acc.setReference(ret_index, ret, MemorySegment.class);
            invokeExactWithFrameNoChecks(handle, stack);
            stack.setType(cached_type);
            thiz_acc.moveToReturn().putNextReference(ret, MemorySegment.class);
        }
    }

    @Override
    protected MethodHandle arrangeDowncall(FunctionDescriptor descriptor, _LinkerOptions options) {
        _FunctionDescriptorImpl descriptor_impl = (_FunctionDescriptorImpl) descriptor;
        _StorageDescriptor target_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
        MemoryLayout ret = null;
        _FunctionDescriptorImpl stub_descriptor = descriptor_impl;
        if (options.isReturnInMemory()) {
            ret = stub_descriptor.returnLayoutPlain();
            stub_descriptor = stub_descriptor.dropReturnLayout()
                    .insertArgumentLayouts(0, ADDRESS.withTargetLayout(ret));
        }
        // leading function pointer
        stub_descriptor = stub_descriptor.insertArgumentLayouts(0, ADDRESS);
        MethodType handleType = fdToHandleMethodType(stub_descriptor);
        MethodType stubType = fdToStubMethodType(stub_descriptor, options.allowsHeapAccess());
        MethodHandle stub = generateJavaDowncallStub(stubType, stub_descriptor, target_descriptor, options);
        int capturedStateMask = options.capturedCallState()
                .mapToInt(_CapturableState::mask)
                .reduce(0, (a, b) -> a | b);
        capturedStateMask |= options.hasCapturedCallState() ? 1 << 31 : 0;
        var arranger = new DowncallArranger(stub, handleType.parameterArray(),
                (ValueLayout) stub_descriptor.returnLayoutPlain(),
                options.allowsHeapAccess(), capturedStateMask);
        if (options.hasCapturedCallState()) {
            handleType = handleType.insertParameterTypes(0, MemorySegment.class);
        }
        if (options.isReturnInMemory()) {
            handleType = handleType.changeReturnType(MemorySegment.class);
        }
        MethodHandle handle = Transformers.makeTransformer(handleType, arranger);
        if (options.isReturnInMemory()) {
            int ret_index = options.hasCapturedCallState() ? 2 : 1;
            MethodType type = handleType.changeParameterType(ret_index, Arena.class);
            ReturnWrapper wrapper = new ReturnWrapper(handle, ret, ret_index);
            handle = Transformers.makeTransformer(type, wrapper);
        }
        if (options.hasCapturedCallState()) {
            int index = options.isReturnInMemory() ? 2 : 1;
            handle = moveArgument(handle, 0, index);
        }
        return handle;
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(MethodType targetType, FunctionDescriptor descriptor, _LinkerOptions options) {
        //TODO
        throw new UnsupportedOperationException("Unsuppurted yet!");
    }
}

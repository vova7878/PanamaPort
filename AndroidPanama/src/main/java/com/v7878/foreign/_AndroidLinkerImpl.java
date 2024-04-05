package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign._CapturableState.ERRNO;
import static com.v7878.foreign._Utils.moveArgument;
import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMAddAttribute;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAddInstrAttribute;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMAttribute.LLVMByValAttribute;
import static com.v7878.llvm.Core.LLVMAttribute.LLVMStructRetAttribute;
import static com.v7878.llvm.Core.LLVMAttributeIndex.LLVMAttributeFirstArgIndex;
import static com.v7878.llvm.Core.LLVMBuildAlloca;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildPtrToInt;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMBuildUnreachable;
import static com.v7878.llvm.Core.LLVMConstInt;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetInstrParamAlignment;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
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
import static com.v7878.unsafe.llvm.LLVMUtils.buildToJvmPointer;
import static com.v7878.unsafe.llvm.LLVMUtils.getBuilderContext;
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
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;
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

    private static MemorySegment readSegment(StackFrameAccessor reader, MemoryLayout layout) {
        long size = layout == null ? 0 : layout.byteSize();
        long alignment = layout == null ? 1 : layout.byteAlignment();
        long value = IS64BIT ? reader.nextLong() : reader.nextInt() & 0xffffffffL;
        return _Utils.longToAddress(value, size, alignment);
    }

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
                writer.putNextReference(readSegment(reader, target), MemorySegment.class);
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

    public static MethodType fdToRawMethodType(_FunctionDescriptorImpl descriptor, boolean allowsHeapAccess) {
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

    private static String getStubName(ProtoId proto, boolean downcall) {
        return _AndroidLinkerImpl.class.getName() + "$" +
                (downcall ? "Downcall" : "Upcall") + "Stub_" + proto.getShorty();
    }

    private static MethodHandle generateJavaDowncallStub(
            Arena scope, MemorySegment nativeStub, MethodType stubType, _LinkerOptions options) {
        final String method_name = "function";
        final String field_name = "scope";

        MethodType gType = replaceObjectParameters(stubType);
        ProtoId stub_proto = ProtoId.of(gType);
        String stub_name = getStubName(stub_proto, true);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));

        FieldId scope_id = new FieldId(stub_id, TypeId.of(Arena.class), field_name);
        stub_def.getClassData().getStaticFields().add(new EncodedField(
                scope_id, ACC_PRIVATE | ACC_STATIC | ACC_FINAL, null));

        MethodId fid = new MethodId(stub_id, stub_proto, method_name);
        stub_def.getClassData().getDirectMethods().add(new EncodedMethod(
                fid, ACC_NATIVE | ACC_STATIC,
                options.isCritical() ? new AnnotationSet(AnnotationItem.CriticalNative()) : null,
                null, null
        ));

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        Class<?> stub_class = loadClass(dex, stub_name, Utils.newEmptyClassLoader());

        Field field = getDeclaredField(stub_class, field_name);
        putObject(stub_class, fieldOffset(field), scope);

        Method function = getDeclaredMethod(stub_class, method_name, gType.parameterArray());
        registerNativeMethod(function, nativeStub.nativeAddress());

        MethodHandle stub = unreflect(function);
        Reflection.setMethodType(stub, stubType);
        return stub;
    }

    private static LLVMTypeRef getTargetType(LLVMContextRef context, AddressLayout addressLayout) {
        return addressLayout.targetLayout()
                .map(target -> layoutToLLVMType(context, target))
                .orElse(void_t(context));
    }

    // Note: all layouts already has natural alignment
    private static LLVMTypeRef layoutToLLVMType(LLVMContextRef context, MemoryLayout layout) {
        Objects.requireNonNull(layout);
        if (layout instanceof ValueLayout.OfByte) {
            return int8_t(context);
        } else if (layout instanceof ValueLayout.OfBoolean) {
            return int1_t(context);
        } else if (layout instanceof ValueLayout.OfShort || layout instanceof ValueLayout.OfChar) {
            return int16_t(context);
        } else if (layout instanceof ValueLayout.OfInt) {
            return int32_t(context);
        } else if (layout instanceof ValueLayout.OfFloat) {
            return float_t(context);
        } else if (layout instanceof ValueLayout.OfLong) {
            return int64_t(context);
        } else if (layout instanceof ValueLayout.OfDouble) {
            return double_t(context);
        } else if (layout instanceof AddressLayout addressLayout) {
            return LLVMPointerType(getTargetType(context, addressLayout), 0);
        } else if (layout instanceof UnionLayout) {
            // TODO: it`s ok?
            return LLVMArrayType(int8_t(context), Math.toIntExact(layout.byteSize()));
        } else if (layout instanceof SequenceLayout sequence) {
            return LLVMArrayType(layoutToLLVMType(context, sequence.elementLayout()),
                    Math.toIntExact(sequence.elementCount()));
        } else if (layout instanceof StructLayout struct) {
            List<MemoryLayout> members = struct.memberLayouts();
            List<LLVMTypeRef> elements = new ArrayList<>(members.size());
            for (MemoryLayout member : members) {
                if (!(member instanceof PaddingLayout)) {
                    elements.add(layoutToLLVMType(context, member));
                }
            }
            // TODO: check offsets if debug
            return LLVMStructTypeInContext(context, elements.toArray(new LLVMTypeRef[0]), false);
        }
        throw shouldNotReachHere();
    }

    private static LLVMTypeRef getPointeeType(LLVMContextRef context, MemoryLayout layout) {
        if (layout instanceof AddressLayout addressLayout) {
            return getTargetType(context, addressLayout);
        }
        if (layout instanceof GroupLayout) {
            return layoutToLLVMType(context, layout);
        }
        throw shouldNotReachHere();
    }

    private static LLVMTypeRef getPointerType(LLVMContextRef context, MemoryLayout layout) {
        return LLVMPointerType(getPointeeType(context, layout), 0);
    }

    private static LLVMTypeRef fdToLLVMType(LLVMContextRef context, _FunctionDescriptorImpl descriptor,
                                            boolean allowsHeapAccess, boolean fullEnv) {
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
        if (fullEnv) {
            argTypes.add(0, void_ptr_t(context)); // JNIEnv*
            argTypes.add(1, void_ptr_t(context)); // jclass
        }
        return LLVMFunctionType(returnType, argTypes.toArray(new LLVMTypeRef[0]), false);
    }

    private static LLVMTypeRef sdToLLVMType(LLVMContextRef context, _StorageDescriptor descriptor,
                                            int firstVariadicArgIndex) {
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
                /* just drop */
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

            LLVMTypeRef stub_type = fdToLLVMType(context, stub_descriptor,
                    options.allowsHeapAccess(), !options.isCritical());
            LLVMValueRef stub = LLVMAddFunction(module, function_name, stub_type);

            LLVMTypeRef target_type = sdToLLVMType(context, target_descriptor, options.firstVariadicArgIndex());
            LLVMTypeRef target_type_ptr = LLVMPointerType(target_type, 0);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(stub, ""));

            LLVMValueRef[] stub_args;
            if (options.allowsHeapAccess()) {
                List<LLVMValueRef> out = new ArrayList<>();
                LLVMValueRef[] tmp = LLVMGetParams(stub);
                int t = 0;
                for (MemoryLayout layout : stub_descriptor.argumentLayouts()) {
                    if (layout instanceof GroupLayout || layout instanceof AddressLayout) {
                        out.add(buildToJvmPointer(builder, tmp[t++], tmp[t++], getPointeeType(context, layout)));
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
                } else if (retStorage instanceof MemoryStorage ms) {
                    // pass as pointer argument with "sret" attribute
                    retVoid = true;
                    retStore = null;
                    target_args[count] = stub_args[index];
                    attrs[count] = LLVMStructRetAttribute;
                    aligns[count] = Math.toIntExact(ms.layout.byteAlignment());
                    index++;
                    count++;
                } else {
                    throw shouldNotReachHere();
                }
            }
            for (LLVMStorage storage : target_descriptor.argumentStorages()) {
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

            LLVMValueRef call = LLVMBuildCall(builder, target, Arrays.copyOf(target_args, count), "");

            final int offset = LLVMAttributeFirstArgIndex;
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
                    MemorySegment code = getFunctionCode(of, function_name);
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
        MethodType stubType = fdToRawMethodType(stub_descriptor, options.allowsHeapAccess());
        Arena scope = Arena.ofAuto();
        MemorySegment nativeStub = generateNativeDowncallStub(scope, target_descriptor, stub_descriptor, options);
        MethodHandle stub = generateJavaDowncallStub(scope, nativeStub, stubType, options);
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

    private static Method generateJavaUpcallStub(MethodType target_type) {
        final String method_name = "function";
        final TypeId mh = TypeId.of(MethodHandle.class);

        MethodType stub_type = target_type.insertParameterTypes(0, MethodHandle.class);

        ProtoId target_proto = ProtoId.of(target_type);
        ProtoId stub_proto = ProtoId.of(stub_type);

        String stub_name = getStubName(target_proto, false);
        TypeId stub_id = TypeId.of(stub_name);
        ClassDef stub_def = new ClassDef(stub_id);
        stub_def.setSuperClass(TypeId.of(Object.class));

        MethodId mh_invoke_id = new MethodId(mh, new ProtoId(TypeId.of(Object.class),
                TypeId.of(Object[].class)), "invokeExact");

        int regs = stub_proto.getInputRegistersCount();

        MethodId fid = new MethodId(stub_id, stub_proto, method_name);
        stub_def.getClassData().getDirectMethods().add(new EncodedMethod(
                fid, ACC_PRIVATE | ACC_STATIC).withCode(0, b -> b
                .invoke_polymorphic_range(mh_invoke_id, target_proto, regs, b.p(0))
                .return_void()
        ));

        DexFile dex = openDexFile(new Dex(stub_def).compile());
        Class<?> stub_class = loadClass(dex, stub_name, Utils.newEmptyClassLoader());

        return getDeclaredMethod(stub_class, method_name, stub_type.parameterArray());
    }

    private static LLVMValueRef const_intptr(LLVMContextRef context, long value) {
        return LLVMConstInt(intptr_t(context), value, false);
    }

    private static LLVMValueRef const_int(LLVMContextRef context, int value) {
        return LLVMConstInt(int32_t(context), value, false);
    }

    private static LLVMTypeRef ptr_t(LLVMTypeRef type) {
        return LLVMPointerType(type, 0);
    }

    private static MemorySegment generateNativeUpcallStub(
            Arena scope, _StorageDescriptor stub_descriptor,
            long arranger_id, long class_id, long function_id) {
        class Holder {
            private static final Arena SCOPE = Arena.ofAuto();
            private static final SymbolLookup LIBC = SymbolLookup.libraryLookup("libc.so", SCOPE);

            private static final MemorySegment functions = SCOPE.allocate(ADDRESS, 9);

            private enum Symbols {
                JVM(JNIUtils.getJavaVMPtr()),
                EXIT(LIBC.find("exit").orElseThrow(Utils::shouldNotReachHere)),
                GET_ENV(JNIUtils.getJNIInvokeInterfaceFunction("GetEnv")),
                ATTACH(JNIUtils.getJNIInvokeInterfaceFunction("AttachCurrentThreadAsDaemon")),
                CALL(JNIUtils.getJNINativeInterfaceFunction("CallStaticVoidMethod")),
                EXCEPTION_CHECK(JNIUtils.getJNINativeInterfaceFunction("ExceptionCheck")),
                FATAL_ERROR(JNIUtils.getJNINativeInterfaceFunction("FatalError")),
                UNCAUGHT_EXCEPTION_MESSAGE(SCOPE.allocateFrom("Uncaught exception in upcall stub"));

                final long value;

                Symbols(MemorySegment symbol) {
                    MemorySegment target = functions.asSlice((long) ADDRESS_SIZE * ordinal());
                    target.set(ADDRESS, 0, symbol);
                    value = target.nativeAddress();
                }
            }

            private static LLVMValueRef const_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value) {
                var context = getBuilderContext(builder);
                LLVMTypeRef ptr = LLVMPointerType(type, 0);
                return LLVMBuildIntToPtr(builder, const_intptr(context, value), ptr, "");
            }

            private static LLVMValueRef const_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value) {
                var ptr = const_ptr(builder, ptr_t(type), value);
                return LLVMBuildLoad(builder, ptr, "");
            }

            static LLVMValueRef jvm(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                return const_load_ptr(builder, void_t(context), Symbols.JVM.value);
            }

            static LLVMValueRef exit(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{int32_t(context)}, false);
                return const_load_ptr(builder, type, Symbols.EXIT.value);
            }

            static LLVMValueRef get_env(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int32_t(context), new LLVMTypeRef[]{void_ptr_t(context),
                        ptr_t(void_ptr_t(context)), int32_t(context)}, false);
                return const_load_ptr(builder, type, Symbols.GET_ENV.value);
            }

            static LLVMValueRef call(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var intptr = intptr_t(context);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{void_ptr_t(context), intptr, intptr}, true);
                return const_load_ptr(builder, type, Symbols.CALL.value);
            }

            static LLVMValueRef exceptionCheck(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int1_t(context), new LLVMTypeRef[]{void_ptr_t(context)}, false);
                return const_load_ptr(builder, type, Symbols.EXCEPTION_CHECK.value);
            }

            static LLVMValueRef fatalError(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{void_ptr_t(context),
                        ptr_t(int8_t(context))}, false);
                return const_load_ptr(builder, type, Symbols.FATAL_ERROR.value);
            }

            static LLVMValueRef uncaughtExceptionMessage(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                return const_load_ptr(builder, int8_t(context), Symbols.UNCAUGHT_EXCEPTION_MESSAGE.value);
            }
        }

        final String function_name = "stub";
        try (var context = newContext(); var builder = LLVMCreateBuilderInContext(context);
             var module = LLVMModuleCreateWithNameInContext("generic", context)) {

            LLVMValueRef jni_ok = const_int(context, 0);
            LLVMValueRef jni_version = const_int(context, /* JNI_VERSION_1_6 */ 0x00010006);

            LLVMTypeRef stub_type = sdToLLVMType(context, stub_descriptor, -1);
            LLVMValueRef stub = LLVMAddFunction(module, function_name, stub_type);

            var start = LLVMAppendBasicBlock(stub, "");
            var body = LLVMAppendBasicBlock(stub, "");
            var uncaught_exception = LLVMAppendBasicBlock(stub, "");
            var end = LLVMAppendBasicBlock(stub, "");
            var exit = LLVMAppendBasicBlock(stub, "");

            LLVMPositionBuilderAtEnd(builder, exit);
            var status = LLVMBuildPhi(builder, int32_t(context), "");
            LLVMBuildCall(builder, Holder.exit(builder), new LLVMValueRef[]{status}, "");
            LLVMBuildUnreachable(builder);

            LLVMPositionBuilderAtEnd(builder, start);
            var env_ptr = LLVMBuildAlloca(builder, void_ptr_t(context), "");
            var jni_status = LLVMBuildCall(builder, Holder.get_env(builder), new LLVMValueRef[]{
                    Holder.jvm(builder), env_ptr, jni_version}, "");
            var test_status = LLVMBuildICmp(builder, LLVMIntEQ, jni_status, jni_ok, "");
            // TODO: attach thread
            LLVMBuildCondBr(builder, test_status, body, exit);
            LLVMAddIncoming(status, jni_status, start);

            LLVMPositionBuilderAtEnd(builder, body);
            var env = LLVMBuildLoad(builder, env_ptr, "");
            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in stub_args[]
            LLVMValueRef[] stub_args = LLVMGetParams(stub);
            LLVMValueRef target = Holder.call(builder);
            LLVMValueRef[] target_args = new LLVMValueRef[stub_args.length + 5];
            target_args[count++] = env;
            target_args[count++] = const_intptr(context, class_id);
            target_args[count++] = const_intptr(context, function_id);
            target_args[count++] = const_intptr(context, arranger_id);
            LLVMValueRef ret = null;
            MemoryLayout retLoad = null;
            {
                LLVMStorage retStorage = stub_descriptor.returnStorage();
                if (retStorage instanceof NoStorage ns) {
                    if (ns.layout != null) {
                        var type = layoutToLLVMType(context, ns.layout);
                        target_args[count++] = LLVMBuildAlloca(builder, type, "");
                    }
                } else if (retStorage instanceof RawStorage rs) {
                    var type = layoutToLLVMType(context, retLoad = rs.layout);
                    target_args[count++] = ret = LLVMBuildAlloca(builder, type, "");
                } else if (retStorage instanceof WrapperStorage ws) {
                    // Note: load layout, not wrapper
                    var ltype = layoutToLLVMType(context, retLoad = ws.layout);
                    var wtype = layoutToLLVMType(context, ws.wrapper);
                    target_args[count++] = ret = LLVMBuildAlloca(builder, ltype, "");
                    ret = LLVMBuildPointerCast(builder, ret, ptr_t(wtype), "");
                } else if (retStorage instanceof MemoryStorage) {
                    // pass as pointer argument with "sret" attribute
                    var sret = stub_args[index++];
                    LLVMAddAttribute(sret, LLVMStructRetAttribute);
                    target_args[count++] = LLVMBuildPtrToInt(builder, sret, intptr_t(context), "");
                } else {
                    throw shouldNotReachHere();
                }
            }
            for (LLVMStorage storage : stub_descriptor.argumentStorages()) {
                if (storage instanceof NoStorage ns) {
                    assert ns.layout != null;
                    var type = layoutToLLVMType(context, ns.layout);
                    target_args[count++] = LLVMBuildAlloca(builder, type, "");
                } else if (storage instanceof RawStorage) {
                    target_args[count++] = stub_args[index++];
                } else if (storage instanceof WrapperStorage ws) {
                    var ltype = layoutToLLVMType(context, ws.layout);
                    var wtype = layoutToLLVMType(context, ws.wrapper);
                    var arg = target_args[count++] = LLVMBuildAlloca(builder, ltype, "");
                    arg = LLVMBuildPointerCast(builder, arg, ptr_t(wtype), "");
                    var store = LLVMBuildStore(builder, stub_args[index++], arg);
                    // Note: alignment from layout, not wrapper
                    LLVMSetAlignment(store, Math.toIntExact(ws.layout.byteAlignment()));
                } else if (storage instanceof MemoryStorage) {
                    // pass as pointer with "byval" attribute
                    var byval = stub_args[index++];
                    LLVMAddAttribute(byval, LLVMByValAttribute);
                    target_args[count++] = LLVMBuildPtrToInt(builder, byval, intptr_t(context), "");
                } else {
                    throw shouldNotReachHere();
                }
            }

            LLVMBuildCall(builder, target, Arrays.copyOf(target_args, count), "");
            var test_exceptions = LLVMBuildCall(builder,
                    Holder.exceptionCheck(builder), new LLVMValueRef[]{env}, "");
            LLVMBuildCondBr(builder, test_exceptions, uncaught_exception, end);

            LLVMPositionBuilderAtEnd(builder, uncaught_exception);
            LLVMBuildCall(builder, Holder.fatalError(builder), new LLVMValueRef[]{env,
                    Holder.uncaughtExceptionMessage(builder)}, "");
            LLVMBuildUnreachable(builder);

            // TODO: destroy env if needed

            LLVMPositionBuilderAtEnd(builder, end);
            if (ret == null) {
                LLVMBuildRetVoid(builder);
            } else {
                LLVMValueRef load = LLVMBuildLoad(builder, ret, "");
                LLVMSetAlignment(load, Math.toIntExact(retLoad.byteAlignment()));
                LLVMBuildRet(builder, load);
            }

            LLVMVerifyModule(module);

            try (var machine = newDefaultMachine()) {
                LLVMMemoryBufferRef buf = LLVMTargetMachineEmitToMemoryBuffer(
                        machine, module, LLVMObjectFile);
                try (var of = LLVMCreateObjectFile(buf)) {
                    MemorySegment code = getFunctionCode(of, function_name);
                    return NativeCodeBlob.makeCodeBlob(scope, code)[0];
                }
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    private static class UpcallArranger implements TransformerI {
        private final MethodHandle stub;
        private final MemoryLayout[] args;
        private final MemoryLayout ret;

        public UpcallArranger(MethodHandle stub, _FunctionDescriptorImpl descriptor) {
            this.stub = stub;
            this.args = descriptor.argumentLayouts().toArray(new MemoryLayout[0]);
            this.ret = descriptor.returnLayoutPlain();
        }

        private static void copyArg(StackFrameAccessor reader, StackFrameAccessor writer, MemoryLayout layout) {
            if (layout instanceof ValueLayout vl) {
                Class<?> type = vl.carrier();
                if (type == MemorySegment.class) {
                    MemoryLayout target = ((AddressLayout) layout).targetLayout().orElse(null);
                    writer.putNextReference(readSegment(reader, target), MemorySegment.class);
                    return;
                }
                EmulatedStackFrame.copyNext(reader, writer, type);
            } else if (layout instanceof GroupLayout gl) {
                writer.putNextReference(readSegment(reader, gl), MemorySegment.class);
            } else {
                throw shouldNotReachHere();
            }
        }

        private static void copyRet(StackFrameAccessor reader, MemorySegment ret, MemoryLayout layout) {
            if (layout instanceof ValueLayout.OfByte bl) {
                ret.set(bl, 0, reader.nextByte());
            } else if (layout instanceof ValueLayout.OfBoolean bl) {
                ret.set(bl, 0, reader.nextBoolean());
            } else if (layout instanceof ValueLayout.OfShort sl) {
                ret.set(sl, 0, reader.nextShort());
            } else if (layout instanceof ValueLayout.OfChar cl) {
                ret.set(cl, 0, reader.nextChar());
            } else if (layout instanceof ValueLayout.OfInt il) {
                ret.set(il, 0, reader.nextInt());
            } else if (layout instanceof ValueLayout.OfFloat fl) {
                ret.set(fl, 0, reader.nextFloat());
            } else if (layout instanceof ValueLayout.OfLong ll) {
                ret.set(ll, 0, reader.nextLong());
            } else if (layout instanceof ValueLayout.OfDouble dl) {
                ret.set(dl, 0, reader.nextDouble());
            } else if (layout instanceof AddressLayout al) {
                ret.set(al, 0, reader.nextReference(MemorySegment.class));
            } else if (layout instanceof GroupLayout gl) {
                MemorySegment arg = reader.nextReference(MemorySegment.class);
                MemorySegment.copy(arg, 0, ret, 0, gl.byteSize());
            } else {
                throw shouldNotReachHere();
            }
        }

        @Override
        public void transform(EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();
            MemorySegment retSeg = ret == null ? null : readSegment(thiz_acc, ret);
            for (MemoryLayout arg : args) {
                copyArg(thiz_acc, stub_acc, arg);
            }
            invokeExactWithFrameNoChecks(stub, stub_frame);
            if (ret != null) {
                stub_acc.moveToReturn();
                copyRet(stub_acc, retSeg, ret);
            }
        }
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(
            MethodType target_type, FunctionDescriptor descriptor, _LinkerOptions unused) {
        _FunctionDescriptorImpl descriptor_impl = (_FunctionDescriptorImpl) descriptor;
        _StorageDescriptor stub_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
        MemoryLayout ret = descriptor_impl.returnLayoutPlain();
        _FunctionDescriptorImpl arranger_descriptor = ret == null ? descriptor_impl :
                descriptor_impl.dropReturnLayout().insertArgumentLayouts(0, ADDRESS.withTargetLayout(ret));
        MethodType arranger_type = fdToRawMethodType(arranger_descriptor, false);
        Method stub = generateJavaUpcallStub(arranger_type);

        return (target, arena) -> {
            MethodHandle arranger;
            if (target.type().equals(arranger_type)) {
                arranger = target;
            } else {
                arranger = Transformers.makeTransformer(arranger_type,
                        new UpcallArranger(target, descriptor_impl));
            }

            long arranger_id = JNIUtils.NewGlobalRef(arranger, arena);
            long class_id = JNIUtils.NewGlobalRef(stub.getDeclaringClass(), arena);
            long function_id = JNIUtils.FromReflectedMethod(stub);

            return generateNativeUpcallStub(arena, stub_descriptor, arranger_id, class_id, function_id);
        };
    }
}

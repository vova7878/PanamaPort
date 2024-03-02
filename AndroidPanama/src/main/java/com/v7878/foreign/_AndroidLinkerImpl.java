package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign._CapturableState.ERRNO;
import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddInstrAttribute;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMAttribute.LLVMByValAttribute;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildNeg;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildZExtOrBitCast;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
import static com.v7878.llvm.Extra.getFunctionCode;
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
import static com.v7878.unsafe.Utils.assert_;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.foreign.LLVMGlobals.DEFAULT_CONTEXT;
import static com.v7878.unsafe.foreign.LLVMGlobals.DOUBLE_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.FLOAT_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT16_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT1_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT32_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT64_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INT8_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.INTPTR_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.VOID_PTR_T;
import static com.v7878.unsafe.foreign.LLVMGlobals.VOID_T;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;

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
import com.v7878.invoke.VarHandle;
import com.v7878.llvm.Core;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.ObjectFile;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.foreign.LLVMGlobals;
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
            } finally {
                for (_MemorySessionImpl session : acquiredSessions) {
                    session.release0();
                }
            }

            if (ret != null) {
                thiz_acc.moveToReturn();
                stub_acc.moveToReturn();
                copyRet(stub_acc, thiz_acc, ret);
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
            MethodType stubType, _FunctionDescriptorImpl f_descriptor,
            _FunctionDescriptorImpl stub_descriptor, _LinkerOptions options) {
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

        Arena scope = Arena.ofAuto();

        Field field = getDeclaredField(stub_class, field_name);
        putObject(stub_class, fieldOffset(field), scope);

        Method function = getDeclaredMethod(stub_class, method_name, gType.parameterArray());
        registerNativeMethod(function, generateNativeDowncallStub(scope, f_descriptor, stub_descriptor, options).nativeAddress());

        MethodHandle stub = unreflect(function);
        Reflection.setMethodType(stub, stubType);
        return stub;
    }

    // Note: all layouts already has natural alignment
    private static LLVMTypeRef layoutToLLVMType(MemoryLayout layout) {
        Objects.requireNonNull(layout);
        LLVMTypeRef out;
        if (layout instanceof ValueLayout.OfByte) {
            out = INT8_T;
        } else if (layout instanceof ValueLayout.OfBoolean) {
            out = INT1_T;
        } else if (layout instanceof ValueLayout.OfShort || layout instanceof ValueLayout.OfChar) {
            out = INT16_T;
        } else if (layout instanceof ValueLayout.OfInt) {
            out = INT32_T;
        } else if (layout instanceof ValueLayout.OfFloat) {
            out = FLOAT_T;
        } else if (layout instanceof ValueLayout.OfLong) {
            out = INT64_T;
        } else if (layout instanceof ValueLayout.OfDouble) {
            out = DOUBLE_T;
        } else if (layout instanceof AddressLayout addressLayout) {
            out = LLVMPointerType(addressLayout.targetLayout()
                    .map(_AndroidLinkerImpl::layoutToLLVMType)
                    .orElse(VOID_T), 0);
        } else if (layout instanceof UnionLayout) {
            // TODO: it`s ok?
            out = LLVMArrayType(INT8_T, Math.toIntExact(layout.byteSize()));
        } else if (layout instanceof SequenceLayout sequence) {
            out = LLVMArrayType(layoutToLLVMType(sequence.elementLayout()),
                    Math.toIntExact(sequence.elementCount()));
        } else if (layout instanceof StructLayout struct) {
            List<MemoryLayout> members = struct.memberLayouts();
            List<LLVMTypeRef> elements = new ArrayList<>(members.size());
            for (MemoryLayout member : members) {
                if (!(member instanceof PaddingLayout)) {
                    elements.add(layoutToLLVMType(member));
                }
            }
            out = LLVMStructTypeInContext(DEFAULT_CONTEXT, elements.toArray(new LLVMTypeRef[0]), false);
            // TODO: check offsets
        } else {
            throw shouldNotReachHere();
        }
        return out;
    }

    private static LLVMTypeRef getPointerType(MemoryLayout layout) {
        if (layout instanceof AddressLayout addressLayout) {
            return layoutToLLVMType(addressLayout);
        }
        if (layout instanceof GroupLayout) {
            return LLVMPointerType(layoutToLLVMType(layout), 0);
        }
        throw shouldNotReachHere();
    }

    private static LLVMTypeRef fdToStubLLVMType(_FunctionDescriptorImpl descriptor,
                                                boolean allowsHeapAccess, boolean isCritical) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        LLVMTypeRef returnType = retLayout == null ? VOID_T :
                layoutToLLVMType(retLayout);
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());
        for (MemoryLayout layout : argLayouts) {
            if (layout instanceof AddressLayout || layout instanceof GroupLayout) {
                if (allowsHeapAccess) {
                    argTypes.add(INT32_T);
                    argTypes.add(INTPTR_T);
                } else {
                    argTypes.add(getPointerType(layout));
                }
            } else if (layout instanceof ValueLayout) {
                argTypes.add(layoutToLLVMType(layout));
            } else {
                throw shouldNotReachHere();
            }
        }
        if (!isCritical) {
            argTypes.add(0, VOID_PTR_T); // JNIEnv*
            argTypes.add(1, VOID_PTR_T); // jobject or jclass
        }
        return LLVMFunctionType(returnType, argTypes.toArray(new LLVMTypeRef[0]), false);
    }

    private static LLVMTypeRef fdToTargetLLVMType(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        LLVMTypeRef returnType = retLayout == null ? VOID_T : layoutToLLVMType(retLayout);
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        if (options.isVariadicFunction()) {
            argLayouts = argLayouts.subList(0, options.firstVariadicArgIndex());
        }
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());
        for (MemoryLayout layout : argLayouts) {
            if (layout instanceof AddressLayout || layout instanceof GroupLayout) {
                argTypes.add(getPointerType(layout));
            } else {
                argTypes.add(layoutToLLVMType(layout));
            }
        }
        return LLVMFunctionType(returnType, argTypes.toArray(new LLVMTypeRef[0]), options.isVariadicFunction());
    }

    private static MemorySegment generateNativeDowncallStub(
            Arena scope, _FunctionDescriptorImpl f_descriptor,
            _FunctionDescriptorImpl stub_descriptor, _LinkerOptions options) {
        final String function_name = "stub";
        try (var builder = Utils.lock(LLVMCreateBuilderInContext(DEFAULT_CONTEXT), Core::LLVMDisposeBuilder);
             var module = Utils.lock(LLVMModuleCreateWithNameInContext("generic", DEFAULT_CONTEXT), Core::LLVMDisposeModule)) {

            LLVMTypeRef stub_type = fdToStubLLVMType(stub_descriptor, options.allowsHeapAccess(), options.isCritical());
            LLVMValueRef function = LLVMAddFunction(module.value(), function_name, stub_type);

            LLVMTypeRef target_type_ptr = LLVMPointerType(fdToTargetLLVMType(f_descriptor, options), 0);

            LLVMPositionBuilderAtEnd(builder.value(), LLVMAppendBasicBlock(function, ""));

            LLVMValueRef[] args;
            if (options.allowsHeapAccess()) {
                List<LLVMValueRef> out = new ArrayList<>();
                LLVMValueRef[] tmp = LLVMGetParams(function);
                int t = 0;
                for (MemoryLayout layout : stub_descriptor.argumentLayouts()) {
                    if (layout instanceof GroupLayout || layout instanceof AddressLayout) {
                        LLVMValueRef base = tmp[t++];
                        LLVMValueRef offset = tmp[t++];
                        if (VM.isPoisonReferences()) {
                            base = LLVMBuildNeg(builder.value(), base, "");
                        }
                        base = LLVMBuildZExtOrBitCast(builder.value(), base, INTPTR_T, "");
                        base = LLVMBuildAdd(builder.value(), base, offset, "");
                        out.add(LLVMBuildIntToPtr(builder.value(), base, getPointerType(layout), ""));
                    } else if (layout instanceof ValueLayout) {
                        out.add(tmp[t++]);
                    } else {
                        throw shouldNotReachHere();
                    }
                }
                args = out.toArray(new LLVMValueRef[0]);
            } else {
                args = LLVMGetParams(function);
                if (!options.isCritical()) {
                    args = Arrays.copyOfRange(args, 2, args.length);
                }
            }
            assert_(args.length == stub_descriptor.argumentLayouts().size(), AssertionError::new);

            LLVMValueRef target = LLVMBuildPointerCast(builder.value(), args[0], target_type_ptr, "");
            LLVMValueRef[] target_args = new LLVMValueRef[args.length - 1];
            boolean[] byval = new boolean[target_args.length];
            for (int i = 0; i < target_args.length; i++) {
                MemoryLayout layout = f_descriptor.argumentLayouts().get(i);
                if (layout instanceof GroupLayout) {
                    byval[i] = true;
                }
                target_args[i] = args[i + 1];
            }

            LLVMValueRef ret = LLVMBuildCall(builder.value(), target, target_args, "");

            for (int i = 0; i < byval.length; i++) {
                if (byval[i]) {
                    LLVMAddInstrAttribute(ret, i + /* first index is 1, not 0 */ 1, LLVMByValAttribute);
                }
            }

            if (f_descriptor.returnLayoutPlain() == null) {
                LLVMBuildRetVoid(builder.value());
            } else {
                LLVMBuildRet(builder.value(), ret);
            }

            LLVMVerifyModule(module.value());

            LLVMMemoryBufferRef buf = LLVMTargetMachineEmitToMemoryBuffer(
                    LLVMGlobals.DEFAULT_MACHINE, module.value(), LLVMObjectFile);
            try (var of = Utils.lock(LLVMCreateObjectFile(buf), ObjectFile::LLVMDisposeObjectFile);) {
                byte[] code = getFunctionCode(of.value(), function_name).toArray(JAVA_BYTE);
                return NativeCodeBlob.makeCodeBlob(scope, code)[0];
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    @Override
    protected MethodHandle arrangeDowncall(FunctionDescriptor descriptor, _LinkerOptions options) {
        if (options.isReturnInMemory()) {
            //TODO
            throw new UnsupportedOperationException("Unsuppurted yet!");
        }
        _FunctionDescriptorImpl f_descriptor = (_FunctionDescriptorImpl) descriptor;
        _FunctionDescriptorImpl stub_descriptor = f_descriptor.insertArgumentLayouts(0, ADDRESS); // leading function pointer
        MethodType handleType = fdToHandleMethodType(stub_descriptor);
        MethodType stubType = fdToStubMethodType(stub_descriptor, options.allowsHeapAccess());
        MethodHandle stub = generateJavaDowncallStub(stubType, f_descriptor, stub_descriptor, options);
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
        MethodHandle tmp = Transformers.makeTransformer(handleType, arranger);
        int target_index = (options.hasCapturedCallState() ? 1 : 0) + (options.isReturnInMemory() ? 1 : 0);
        if (target_index != 0) {
            tmp = _Utils.moveArgument(tmp, target_index, 0);
        }
        return tmp;
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(MethodType targetType, FunctionDescriptor descriptor, _LinkerOptions options) {
        //TODO
        throw new UnsupportedOperationException("Unsuppurted yet!");
    }
}

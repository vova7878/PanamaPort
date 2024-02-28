package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Extra.getFunctionCode;
import static com.v7878.llvm.Extra.layoutToLLVMTypeInContext;
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
import static com.v7878.unsafe.foreign.LLVMGlobals.INT32_T;
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
import com.v7878.llvm.Core;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.ObjectFile;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.Utils;
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

import dalvik.system.DexFile;

final class _AndroidLinkerImpl extends _AbstractAndroidLinker {
    public static final Linker INSTANCE = new _AndroidLinkerImpl();

    private static class DowncallArranger implements TransformerI {
        private final MethodHandle stub;
        private final Class<?>[] args;
        private final ValueLayout ret;
        private final boolean allowsHeapAccess;
        private final int segment_params_count;

        public DowncallArranger(MethodHandle stub, Class<?>[] args, ValueLayout ret, boolean allowsHeapAccess) {
            this.stub = stub;
            this.args = args;
            this.ret = ret;
            this.allowsHeapAccess = allowsHeapAccess;
            this.segment_params_count = Arrays.stream(args)
                    .mapToInt(t -> t == MemorySegment.class ? 1 : 0)
                    .reduce(Integer::sum).orElse(0);
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
                for (Class<?> arg : args) {
                    copyArg(thiz_acc, stub_acc, arg, acquiredSessions);
                }

                invokeExactWithFrameNoChecks(stub, stub_frame);
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
            MethodType handleType, MethodType stubType, _FunctionDescriptorImpl f_descriptor,
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
                // TODO: FastNative for allowsHeapAccess?
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

    public static LLVMTypeRef fdToStubLLVMType(_FunctionDescriptorImpl descriptor,
                                               boolean allowsHeapAccess, boolean isCritical) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        LLVMTypeRef returnType = retLayout == null ? VOID_T :
                layoutToLLVMTypeInContext(DEFAULT_CONTEXT, retLayout);
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());
        for (MemoryLayout tmp : argLayouts) {
            if (tmp instanceof AddressLayout || tmp instanceof GroupLayout) {
                if (allowsHeapAccess) {
                    argTypes.add(INT32_T);
                    argTypes.add(INTPTR_T);
                } else {
                    LLVMTypeRef target;
                    if (tmp instanceof AddressLayout addressLayout) {
                        // TODO: maybe just VOID_T?
                        MemoryLayout targetLayout = addressLayout.targetLayout().orElse(null);
                        target = targetLayout == null ? VOID_T :
                                layoutToLLVMTypeInContext(DEFAULT_CONTEXT, targetLayout);
                    } else {
                        target = layoutToLLVMTypeInContext(DEFAULT_CONTEXT, tmp);
                    }
                    argTypes.add(LLVMPointerType(target, 0));
                }
            } else if (tmp instanceof ValueLayout) {
                argTypes.add(layoutToLLVMTypeInContext(DEFAULT_CONTEXT, tmp));
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

    public static LLVMTypeRef fdToTargetLLVMType(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        MemoryLayout retLayout = descriptor.returnLayoutPlain();
        LLVMTypeRef returnType = retLayout == null ? VOID_T :
                layoutToLLVMTypeInContext(DEFAULT_CONTEXT, retLayout);
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        if (options.isVariadicFunction()) {
            argLayouts = argLayouts.subList(0, options.firstVariadicArgIndex());
        }
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());
        for (MemoryLayout tmp : argLayouts) {
            argTypes.add(layoutToLLVMTypeInContext(DEFAULT_CONTEXT, tmp));
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
                //TODO
                throw new UnsupportedOperationException("Unsuppurted yet!");
            } else {
                args = LLVMGetParams(function);
                if (!options.isCritical()) {
                    args = Arrays.copyOfRange(args, 2, args.length);
                }
            }
            assert_(args.length == stub_descriptor.argumentLayouts().size(), AssertionError::new);

            LLVMValueRef target = LLVMBuildPointerCast(builder.value(), args[0], target_type_ptr, "");
            LLVMValueRef[] target_args = new LLVMValueRef[args.length - 1];
            for (int i = 0; i < target_args.length; i++) {
                MemoryLayout tmp = f_descriptor.argumentLayouts().get(i);
                if (tmp instanceof GroupLayout) {
                    target_args[i] = LLVMBuildLoad(builder.value(), args[i + 1], "");
                    LLVMSetAlignment(target_args[i], Math.toIntExact(tmp.byteAlignment()));
                } else if (tmp instanceof ValueLayout) {
                    target_args[i] = args[i + 1];
                } else {
                    throw shouldNotReachHere();
                }
            }

            LLVMValueRef ret = LLVMBuildCall(builder.value(), target, target_args, "");
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
        if (options.hasCapturedCallState() || options.hasAllocatorParameter()) {
            //TODO
            throw new UnsupportedOperationException("Unsuppurted yet!");
        }
        _FunctionDescriptorImpl f_descriptor = (_FunctionDescriptorImpl) descriptor;
        _FunctionDescriptorImpl stub_descriptor = f_descriptor.insertArgumentLayouts(0, ADDRESS); // leading function pointer
        MethodType handleType = fdToHandleMethodType(stub_descriptor);
        MethodType stubType = fdToStubMethodType(stub_descriptor, options.allowsHeapAccess());
        MethodHandle stub = generateJavaDowncallStub(handleType, stubType, f_descriptor, stub_descriptor, options);
        var arranger = new DowncallArranger(stub, handleType.parameterArray(),
                (ValueLayout) stub_descriptor.returnLayoutPlain(), options.allowsHeapAccess());
        return Transformers.makeTransformer(handleType, arranger);
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(MethodType targetType, FunctionDescriptor descriptor, _LinkerOptions options) {
        //TODO
        throw new UnsupportedOperationException("Unsuppurted yet!");
    }
}

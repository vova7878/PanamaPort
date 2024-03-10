package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
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
import static com.v7878.llvm.Extra.getFunctionCode;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
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
import com.v7878.unsafe.AndroidUnsafe;
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
        LLVMTypeRef returnType = retLayout == null ? VOID_T : layoutToLLVMType(retLayout);
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
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        if (options.isVariadicFunction()) {
            argLayouts = argLayouts.subList(0, options.firstVariadicArgIndex());
        }

        LLVMTypeRef retType;
        List<LLVMTypeRef> argTypes = new ArrayList<>(argLayouts.size());

        if (retLayout == null) {
            retType = VOID_T;
        } else if (retLayout instanceof GroupLayout gl) {
            MemoryLayout wrapper = getGroupWrapper(gl, true);
            if (wrapper != null) {
                if (wrapper == VOID_WRAPPER) {
                    retType = VOID_T; // just drop
                } else {
                    retType = layoutToLLVMType(wrapper);
                }
            } else {
                // pass through stack as argument with "sret" attribute
                retType = VOID_T;
                argTypes.add(layoutToLLVMType(ADDRESS.withTargetLayout(retLayout)));
            }
        } else {
            retType = layoutToLLVMType(retLayout);
        }

        for (MemoryLayout layout : argLayouts) {
            if (layout instanceof AddressLayout) {
                argTypes.add(getPointerType(layout));
            } else if (layout instanceof GroupLayout gl) {
                MemoryLayout wrapper = getGroupWrapper(gl, false);
                if (wrapper != null) {
                    if (wrapper == VOID_WRAPPER) {
                        // just drop
                    } else {
                        argTypes.add(layoutToLLVMType(wrapper));
                    }
                } else {
                    // pass through stack with "byval" attribute
                    argTypes.add(getPointerType(gl));
                }
            } else {
                argTypes.add(layoutToLLVMType(layout));
            }
        }

        return LLVMFunctionType(retType, argTypes.toArray(new LLVMTypeRef[0]), options.isVariadicFunction());
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
            assert args.length == stub_descriptor.argumentLayouts().size();

            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in args[]
            LLVMValueRef target = LLVMBuildPointerCast(builder.value(), args[index++], target_type_ptr, "");
            LLVMValueRef[] target_args = new LLVMValueRef[args.length - 1];
            int[] attrs = new int[target_args.length];
            int[] aligns = new int[target_args.length];
            boolean retVoid = false;
            MemoryLayout retStore = null;
            {
                MemoryLayout retLayout = f_descriptor.returnLayoutPlain();
                if (retLayout == null) {
                    retVoid = true;
                } else if (retLayout instanceof GroupLayout gl) {
                    MemoryLayout retWrapper = getGroupWrapper(gl, true);
                    if (retWrapper != null) {
                        int i = index++;
                        if (retWrapper == VOID_WRAPPER) {
                            retVoid = true; // just drop
                        } else {
                            args[i] = LLVMBuildPointerCast(builder.value(), args[i],
                                    layoutToLLVMType(ADDRESS.withTargetLayout(retWrapper)), "");
                            retStore = gl;
                        }
                    } else {
                        // pass through stack as argument with "sret" attribute
                        int i = index++;
                        int t = count++;
                        retVoid = true;
                        attrs[t] = LLVMStructRetAttribute;
                        aligns[t] = Math.toIntExact(gl.byteAlignment());
                        target_args[t] = args[i];
                    }
                }
            }
            {
                int start = index;
                while (index < args.length) {
                    MemoryLayout layout = f_descriptor.argumentLayouts().get(index - start);
                    if (layout instanceof GroupLayout gl) {
                        MemoryLayout wrapper = getGroupWrapper(gl, false);
                        if (wrapper != null) {
                            if (wrapper == VOID_WRAPPER) {
                                index++; // just drop
                            } else {
                                int i = index++;
                                int t = count++;
                                args[i] = LLVMBuildPointerCast(builder.value(), args[i],
                                        layoutToLLVMType(ADDRESS.withTargetLayout(wrapper)), "");
                                target_args[t] = LLVMBuildLoad(builder.value(), args[i], "");
                                // Note: copy alignment from gl, not wrapper
                                LLVMSetAlignment(target_args[t], Math.toIntExact(gl.byteAlignment()));
                            }
                        } else {
                            int i = index++;
                            int t = count++;
                            // pass through stack with "byval" attribute
                            attrs[t] = LLVMByValAttribute;
                            aligns[t] = Math.toIntExact(gl.byteAlignment());
                            target_args[t] = args[i];
                        }
                    } else {
                        target_args[count++] = args[index++];
                    }
                }
            }

            LLVMValueRef ret = LLVMBuildCall(builder.value(), target, Arrays.copyOf(target_args, count), "");

            final int offset = 1; // Note: first index is 1, not 0
            for (int i = 0; i < count; i++) {
                if (attrs[i] != 0) {
                    LLVMAddInstrAttribute(ret, i + offset, attrs[i]);
                }
                if (aligns[i] != 0) {
                    LLVMSetInstrParamAlignment(ret, i + offset, aligns[i]);
                }
            }

            if (retVoid) {
                LLVMBuildRetVoid(builder.value());
            } else {
                if (retStore == null) {
                    LLVMBuildRet(builder.value(), ret);
                } else {
                    int ret_index = 1;
                    LLVMValueRef store = LLVMBuildStore(builder.value(), ret, args[ret_index]);
                    LLVMSetAlignment(store, Math.toIntExact(retStore.byteAlignment()));
                    LLVMBuildRetVoid(builder.value());
                }
            }

            LLVMVerifyModule(module.value());

            LLVMMemoryBufferRef buf = LLVMTargetMachineEmitToMemoryBuffer(
                    LLVMGlobals.DEFAULT_MACHINE, module.value(), LLVMObjectFile);
            try (var of = Utils.lock(LLVMCreateObjectFile(buf), ObjectFile::LLVMDisposeObjectFile)) {
                byte[] code = getFunctionCode(of.value(), function_name).toArray(JAVA_BYTE);
                return NativeCodeBlob.makeCodeBlob(scope, code)[0];
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
        _FunctionDescriptorImpl f_descriptor = (_FunctionDescriptorImpl) descriptor;
        MemoryLayout ret = null;
        _FunctionDescriptorImpl stub_descriptor = f_descriptor;
        if (options.isReturnInMemory()) {
            ret = stub_descriptor.returnLayoutPlain();
            stub_descriptor = stub_descriptor.dropReturnLayout()
                    .insertArgumentLayouts(0, ADDRESS.withTargetLayout(ret));
        }
        // leading function pointer
        stub_descriptor = stub_descriptor.insertArgumentLayouts(0, ADDRESS);
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

    private static class x86_64_calling_convention {

        private static void markWrapperType(WrapperType[] types, int start, int size, WrapperType type) {
            for (int i = 0; i < size; i++) {
                WrapperType oldType = types[start + i];
                if (oldType == null || oldType.ordinal() < type.ordinal()) {
                    types[start + i] = type;
                }
            }
        }

        private static void markWrapperType(MemoryLayout layout, int offset, WrapperType[] types) {
            if (layout instanceof StructLayout sl) {
                for (MemoryLayout member : sl.memberLayouts()) {
                    markWrapperType(member, offset, types);
                    offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
                }
            } else if (layout instanceof UnionLayout ul) {
                for (MemoryLayout member : ul.memberLayouts()) {
                    markWrapperType(member, offset, types);
                }
            } else if (layout instanceof SequenceLayout sl) {
                MemoryLayout el = sl.elementLayout();
                int count = Math.toIntExact(sl.elementCount());
                int size = Math.toIntExact(el.byteSize());
                for (int i = 0; i < count; i++) {
                    markWrapperType(el, offset, types);
                    offset = Math.addExact(offset, size);
                }
            } else if (layout instanceof ValueLayout vl) {
                WrapperType type;
                if (layout instanceof ValueLayout.OfFloat) {
                    type = WrapperType.FLOAT;
                } else if (layout instanceof ValueLayout.OfDouble) {
                    type = WrapperType.DOUBLE;
                } else {
                    type = WrapperType.INT;
                }
                markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
            } else if (layout instanceof PaddingLayout) {
                // skip
            } else {
                throw shouldNotReachHere();
            }
        }

        private static MemoryLayout getGroupWrapper(WrapperType[] types, int count) {
            WrapperType max = null;
            for (WrapperType type : types) {
                if (max == null) {
                    max = type;
                }
                if (type != null && type.ordinal() > max.ordinal()) {
                    max = type;
                }
            }
            return switch (count) {
                case 0 -> VOID_WRAPPER;
                case 1 -> JAVA_BYTE;
                case 2 -> JAVA_SHORT;
                case 3 -> JAVA_INT;
                case 4 -> max == WrapperType.INT ? JAVA_INT : JAVA_FLOAT;
                case 8 -> max == WrapperType.INT ? JAVA_LONG : (max == WrapperType.DOUBLE ?
                        JAVA_DOUBLE : structLayout(JAVA_FLOAT, JAVA_FLOAT));
                default -> JAVA_LONG;
            };
        }

        public static MemoryLayout getGroupWrapper(GroupLayout layout) {
            if (layout.byteSize() > 16 /* 128 bit */) return null;
            WrapperType[] types = new WrapperType[16];
            markWrapperType(layout, 0, types);
            int count = 16;
            while (count > 0 && types[count - 1] == null) {
                count--;
            }
            WrapperType[] upper_half = Arrays.copyOf(types, 8);
            if (count <= 8) {
                return getGroupWrapper(upper_half, count);
            }
            WrapperType[] lower_half = Arrays.copyOfRange(types, 8, 16);
            return structLayout(getGroupWrapper(upper_half, 8),
                    getGroupWrapper(lower_half, count - 8));
        }
    }

    private static class aarch64_calling_convention {
        private static void markWrapperType(WrapperType[] types, int start, int size, WrapperType type) {
            for (int i = 0; i < size; i++) {
                WrapperType oldType = types[start + i];
                if (oldType == null) {
                    types[start + i] = type;
                } else if (oldType.ordinal() != type.ordinal()) {
                    types[start + i] = WrapperType.INT;
                }
            }
        }

        private static void markWrapperType(MemoryLayout layout, int offset, WrapperType[] types) {
            if (layout instanceof StructLayout sl) {
                for (MemoryLayout member : sl.memberLayouts()) {
                    markWrapperType(member, offset, types);
                    offset = Math.addExact(offset, Math.toIntExact(member.byteSize()));
                }
            } else if (layout instanceof UnionLayout ul) {
                for (MemoryLayout member : ul.memberLayouts()) {
                    markWrapperType(member, offset, types);
                }
            } else if (layout instanceof SequenceLayout sl) {
                MemoryLayout el = sl.elementLayout();
                int count = Math.toIntExact(sl.elementCount());
                int size = Math.toIntExact(el.byteSize());
                for (int i = 0; i < count; i++) {
                    markWrapperType(el, offset, types);
                    offset = Math.addExact(offset, size);
                }
            } else if (layout instanceof ValueLayout vl) {
                WrapperType type;
                if (layout instanceof ValueLayout.OfFloat) {
                    type = WrapperType.FLOAT;
                } else if (layout instanceof ValueLayout.OfDouble) {
                    type = WrapperType.DOUBLE;
                } else {
                    type = WrapperType.INT;
                }
                markWrapperType(types, offset, Math.toIntExact(vl.byteSize()), type);
            } else if (layout instanceof PaddingLayout) {
                // skip
            } else {
                throw shouldNotReachHere();
            }
        }

        public static MemoryLayout getGroupWrapper(GroupLayout layout) {
            if (layout.byteSize() > 32 /* 256 bit */) return null;
            WrapperType[] types = new WrapperType[32];
            markWrapperType(layout, 0, types);
            int count = 32;
            while (count > 0 && types[count - 1] == null) {
                count--;
            }
            types = Arrays.copyOfRange(types, 0, count);
            WrapperType common = null;
            for (WrapperType type : types) {
                if (common == null) {
                    common = type;
                }
                if (common != type) {
                    common = WrapperType.INT;
                    break;
                }
            }
            common = common == null ? WrapperType.INT : common;
            if (count > 16 /* 128 bit */ && common != WrapperType.DOUBLE) {
                return null;
            }
            return switch (common) {
                case INT -> count <= 8 ? JAVA_LONG : structLayout(JAVA_LONG, JAVA_LONG);
                case DOUBLE -> sequenceLayout(count / 8, JAVA_DOUBLE);
                case FLOAT -> sequenceLayout(count / 4, JAVA_FLOAT);
                //noinspection UnnecessaryDefault
                default -> throw shouldNotReachHere();
            };
        }
    }

    private enum WrapperType {
        FLOAT, DOUBLE, INT
    }

    // Note: invalid layout
    public static final MemoryLayout VOID_WRAPPER = AndroidUnsafe.allocateInstance(_PaddingLayoutImpl.class);

    public static MemoryLayout getGroupWrapper(GroupLayout layout, boolean is_return) {
        if (CURRENT_INSTRUCTION_SET == X86) return null; // TODO: check
        if (CURRENT_INSTRUCTION_SET == X86_64) {
            return x86_64_calling_convention.getGroupWrapper(layout);
        }
        if (CURRENT_INSTRUCTION_SET == ARM64) {
            // TODO: use is_return
            // TODO: check
            return aarch64_calling_convention.getGroupWrapper(layout);
        }
        //TODO: arm, riscv64
        throw new UnsupportedOperationException("Not supported yet!");
    }
}

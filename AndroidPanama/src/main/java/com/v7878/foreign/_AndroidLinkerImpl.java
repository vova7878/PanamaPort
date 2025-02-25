package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.dex.builder.CodeBuilder.BinOp.AND_LONG;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_BYTE;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_LONG;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_SHORT;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.OfChar;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfFloat;
import static com.v7878.foreign.ValueLayout.OfInt;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.foreign._CapturableState.ERRNO;
import static com.v7878.foreign._Utils.moveArgument;
import static com.v7878.llvm.Core.LLVMAddAttributeAtIndex;
import static com.v7878.llvm.Core.LLVMAddCallSiteAttribute;
import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMAttributeIndex.LLVMAttributeFirstArgIndex;
import static com.v7878.llvm.Core.LLVMBuildAlloca;
import static com.v7878.llvm.Core.LLVMBuildBr;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildFPCast;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildPtrToInt;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildSExt;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMBuildUnreachable;
import static com.v7878.llvm.Core.LLVMBuildZExt;
import static com.v7878.llvm.Core.LLVMCreateEnumAttribute;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetInstrParamAlignment;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.JNIUtils.getJNINativeInterfaceOffset;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.handleUncaughtException;
import static com.v7878.unsafe.Utils.newEmptyClassLoader;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.llvm.LLVMBuilder.buildRawObjectToAddress;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_call;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_load_ptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_intptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.functionPointerFactory;
import static com.v7878.unsafe.llvm.LLVMBuilder.intptrFactory;
import static com.v7878.unsafe.llvm.LLVMBuilder.pointerFactory;
import static com.v7878.unsafe.llvm.LLVMTypes.double_t;
import static com.v7878.unsafe.llvm.LLVMTypes.float_t;
import static com.v7878.unsafe.llvm.LLVMTypes.function_ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.function_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int16_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int1_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int64_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int8_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.variadic_function_ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.void_ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.generateFunctionCodeSegment;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.builder.CodeBuilder;
import com.v7878.dex.immutable.Annotation;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.ProtoId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.foreign.ValueLayout.OfBoolean;
import com.v7878.foreign.ValueLayout.OfByte;
import com.v7878.foreign.ValueLayout.OfShort;
import com.v7878.foreign._LLVMStorageDescriptor.LLVMStorage;
import com.v7878.foreign._LLVMStorageDescriptor.MemoryStorage;
import com.v7878.foreign._LLVMStorageDescriptor.NoStorage;
import com.v7878.foreign._LLVMStorageDescriptor.RawStorage;
import com.v7878.foreign._LLVMStorageDescriptor.WrapperStorage;
import com.v7878.foreign._ValueLayouts.JavaValueLayout;
import com.v7878.invoke.VarHandle;
import com.v7878.llvm.Types.LLVMAttributeRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.foreign.ENVGetter;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.RelativeStackFrameAccessor;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.MethodHandlesFixes;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.unsafe.invoke.Transformers.AbstractTransformer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import dalvik.system.DexFile;

final class _AndroidLinkerImpl extends _AbstractAndroidLinker {

    public static final Linker INSTANCE = new _AndroidLinkerImpl();
    private static final VarHandle VH_ERRNO = _CapturableState.LAYOUT.varHandle(groupElement("errno"));

    private static MemorySegment nextSegment(RelativeStackFrameAccessor reader, MemoryLayout layout) {
        long size = layout == null ? 0 : layout.byteSize();
        long alignment = layout == null ? 1 : layout.byteAlignment();
        long value = IS64BIT ? reader.nextLong() : reader.nextInt() & 0xffffffffL;
        return _Utils.longToAddress(value, size, alignment);
    }

    private static class DowncallArranger extends AbstractTransformer {
        private final MethodHandle stub;
        private final MemoryLayout[] args;
        private final ValueLayout ret;
        private final boolean allowsHeapAccess;
        private final int capturedStateMask;
        private final int segment_params_count;

        public DowncallArranger(MethodHandle stub, List<MemoryLayout> argsList, ValueLayout ret,
                                boolean allowsHeapAccess, int capturedStateMask) {
            this.stub = stub;
            this.args = argsList.toArray(new MemoryLayout[0]);
            this.ret = ret;
            this.allowsHeapAccess = allowsHeapAccess;
            this.capturedStateMask = capturedStateMask;
            this.segment_params_count = argsList.stream()
                    .mapToInt(t -> t instanceof AddressLayout ? 1 : 0)
                    .reduce(Integer::sum).orElse(0)
                    + (capturedStateMask < 0 ? 1 : 0);
        }

        private void copyArg(RelativeStackFrameAccessor reader,
                             RelativeStackFrameAccessor writer,
                             MemoryLayout layout, Arena arena,
                             List<_MemorySessionImpl> acquiredSessions) {
            if (layout instanceof JavaValueLayout<?> valueLayout) {
                Class<?> carrier = valueLayout.carrier();
                EmulatedStackFrame.copyNextValue(reader, writer, carrier);
                return;
            }
            _AbstractMemorySegmentImpl segment = reader.nextReference();
            if (layout instanceof AddressLayout) {
                segment.scope.acquire0();
                acquiredSessions.add(segment.scope);
            } else {
                if (layout.byteSize() == 0) {
                    segment = (_AbstractMemorySegmentImpl) MemorySegment.NULL;
                } else {
                    // TODO: improve performance
                    MemorySegment tmp = arena.allocate(layout);
                    // by-value struct
                    MemorySegment.copy(segment, 0, tmp, 0, layout.byteSize());
                    segment = (_AbstractMemorySegmentImpl) tmp;
                }
            }
            long value;
            if (allowsHeapAccess) {
                Object base = segment.unsafeGetBase();
                writer.putNextReference(base);
                value = segment.unsafeGetOffset();
            } else {
                value = _Utils.unboxSegment(segment);
            }
            if (IS64BIT) {
                writer.putNextLong(value);
            } else {
                writer.putNextInt((int) value);
            }
        }

        private void copyRet(RelativeStackFrameAccessor reader,
                             RelativeStackFrameAccessor writer,
                             ValueLayout layout) {
            Class<?> type = layout.carrier();
            if (type == MemorySegment.class) {
                MemoryLayout target = ((AddressLayout) layout).targetLayout().orElse(null);
                writer.putNextReference(nextSegment(reader, target));
                return;
            }
            EmulatedStackFrame.copyNextValue(reader, writer, type);
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) throws Throwable {
            var thiz_acc = stack.relativeAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            var stub_acc = stub_frame.relativeAccessor();

            List<_MemorySessionImpl> acquiredSessions = new ArrayList<>(segment_params_count);
            try (Arena arena = Arena.ofConfined()) {
                _AbstractMemorySegmentImpl capturedState = null;
                if (capturedStateMask < 0) {
                    capturedState = thiz_acc.nextReference();
                    capturedState.scope.acquire0();
                    acquiredSessions.add(capturedState.scope);
                }

                for (MemoryLayout arg : args) {
                    copyArg(thiz_acc, stub_acc, arg, arena, acquiredSessions);
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

    public static MethodType fdToRawMethodType(_FunctionDescriptorImpl descriptor, boolean heap_pointers) {
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
                if (heap_pointers) {
                    argCarriers.add(Object.class);
                }
                argCarriers.add(WORD.carrier());
            } else {
                argCarriers.add(carrier);
            }
        }
        return MethodType.methodType(returnValue, argCarriers);
    }

    private static MethodType fixObjectParameters(MethodType stubType) {
        return MethodType.methodType(stubType.returnType(), stubType.parameterList().stream()
                .map(a -> a == Object.class ? int.class : a).toArray(Class[]::new));
    }

    private static String getStubName(ProtoId proto, boolean downcall) {
        return _AndroidLinkerImpl.class.getName() + "$" +
                (downcall ? "Downcall" : "Upcall") + "Stub_" + proto.computeShorty();
    }

    private static MethodHandle generateJavaDowncallStub(
            Arena scope, MemorySegment nativeStub, MethodType stubType, _LinkerOptions options) {
        final String method_name = "function";
        final String field_name = "scope";

        MethodType primitive_type = fixObjectParameters(stubType);
        ProtoId stub_proto = ProtoId.of(primitive_type);

        String stub_name = getStubName(stub_proto, true);
        TypeId stub_id = TypeId.ofName(stub_name);

        FieldId scope_id = FieldId.of(stub_id, field_name, TypeId.of(Arena.class));

        ClassDef stub_def = ClassBuilder.build(stub_id, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withField(fb -> fb
                        .of(scope_id)
                        .withFlags(ACC_PRIVATE | ACC_STATIC | ACC_FINAL)
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_NATIVE | ACC_STATIC)
                        .withName(method_name)
                        .withProto(stub_proto)
                        .if_(options.isCritical(), mb2 -> mb2
                                .withAnnotations(Annotation.CriticalNative())
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(stub_def)));
        Class<?> stub_class = loadClass(dex, stub_name, newEmptyClassLoader());

        Field field = getDeclaredField(stub_class, field_name);
        putObject(stub_class, fieldOffset(field), scope);

        Method function = getDeclaredMethod(stub_class, method_name, primitive_type.parameterArray());
        registerNativeMethod(function, nativeStub.nativeAddress());

        return MethodHandlesFixes.unreflectWithTransform(function, stubType);
    }

    // Note: all layouts already has natural alignment
    private static LLVMTypeRef layoutToLLVMType(LLVMContextRef context, MemoryLayout layout) {
        Objects.requireNonNull(layout);
        if (layout instanceof OfByte) {
            return int8_t(context);
        } else if (layout instanceof OfBoolean) {
            return int1_t(context);
        } else if (layout instanceof OfShort || layout instanceof OfChar) {
            return int16_t(context);
        } else if (layout instanceof OfInt) {
            return int32_t(context);
        } else if (layout instanceof OfFloat) {
            return float_t(context);
        } else if (layout instanceof OfLong) {
            return int64_t(context);
        } else if (layout instanceof OfDouble) {
            return double_t(context);
        } else if (layout instanceof AddressLayout) {
            return intptr_t(context);
        } else if (layout instanceof UnionLayout) {
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
                    argTypes.add(intptr_t(context));
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
        return function_t(returnType, argTypes.toArray(new LLVMTypeRef[0]));
    }

    private static LLVMTypeRef sdToLLVMType(
            LLVMContextRef context, _LLVMStorageDescriptor descriptor, int firstVariadicArgIndex) {
        LLVMStorage retStorage = descriptor.returnStorage();
        LLVMStorage[] argStorages = descriptor.argumentStorages();
        if (firstVariadicArgIndex >= 0) {
            argStorages = Arrays.copyOfRange(argStorages, 0, firstVariadicArgIndex);
        }

        LLVMTypeRef retType;
        List<LLVMTypeRef> argTypes = new ArrayList<>(argStorages.length);

        if (retStorage instanceof NoStorage) {
            retType = void_t(context);
        } else if (retStorage instanceof RawStorage rs) {
            retType = layoutToLLVMType(context, rs.layout);
        } else if (retStorage instanceof WrapperStorage ws) {
            retType = layoutToLLVMType(context, ws.wrapper);
        } else if (retStorage instanceof MemoryStorage ms) {
            // pass as pointer argument with "sret" attribute
            retType = void_t(context);
            argTypes.add(ptr_t(layoutToLLVMType(context, ms.layout)));
        } else {
            throw shouldNotReachHere();
        }

        for (LLVMStorage storage : argStorages) {
            if (storage instanceof NoStorage) {
                /* just drop */
            } else if (storage instanceof RawStorage rs) {
                argTypes.add(layoutToLLVMType(context, rs.layout));
            } else if (storage instanceof WrapperStorage ws) {
                argTypes.add(layoutToLLVMType(context, ws.wrapper));
            } else if (storage instanceof MemoryStorage ms) {
                // pass as pointer with "byval" attribute
                argTypes.add(ptr_t(layoutToLLVMType(context, ms.layout)));
            } else {
                throw shouldNotReachHere();
            }
        }

        return LLVMFunctionType(retType, argTypes.toArray(new LLVMTypeRef[0]), firstVariadicArgIndex >= 0);
    }

    private static MemorySegment generateNativeDowncallStub(
            Arena scope, _LLVMStorageDescriptor target_descriptor,
            _FunctionDescriptorImpl stub_descriptor, _LinkerOptions options) {
        final String function_name = "stub";
        return generateFunctionCodeSegment((context, module, builder) -> {
            var sret_attr = LLVMCreateEnumAttribute(context, "sret", 0);
            var byval_attr = LLVMCreateEnumAttribute(context, "byval", 0);

            var stub_type = fdToLLVMType(context, stub_descriptor,
                    options.allowsHeapAccess(), !options.isCritical());
            var stub = LLVMAddFunction(module, function_name, stub_type);

            var target_type = sdToLLVMType(context, target_descriptor,
                    options.firstVariadicArgIndex());
            var target_type_ptr = ptr_t(target_type);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(stub, ""));

            LLVMValueRef[] stub_args;
            if (options.allowsHeapAccess()) {
                List<LLVMValueRef> out = new ArrayList<>();
                var tmp = LLVMGetParams(stub);
                int t = 0;
                for (MemoryLayout layout : stub_descriptor.argumentLayouts()) {
                    if (layout instanceof GroupLayout || layout instanceof AddressLayout) {
                        out.add(buildRawObjectToAddress(builder, tmp[t++], tmp[t++]));
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
            var target = LLVMBuildIntToPtr(builder, stub_args[index++], target_type_ptr, "");
            var target_args = new LLVMValueRef[stub_args.length - index];
            var attrs = new LLVMAttributeRef[target_args.length];
            int[] aligns = new int[target_args.length];
            boolean retVoid = true;
            LLVMValueRef retStore = null;
            int retAlignment = 0;
            {
                LLVMStorage retStorage = target_descriptor.returnStorage();
                if (retStorage instanceof NoStorage) {
                    if (options.isReturnInMemory()) {
                        index++; // just drop
                    }
                } else if (retStorage instanceof RawStorage) {
                    retVoid = false;
                } else if (retStorage instanceof WrapperStorage ws) {
                    // Note: store layout, not wrapper
                    retAlignment = Math.toIntExact(ws.layout.byteAlignment());
                    retStore = LLVMBuildIntToPtr(builder, stub_args[index],
                            ptr_t(layoutToLLVMType(context, ws.wrapper)), "");
                    index++;
                } else if (retStorage instanceof MemoryStorage ms) {
                    // pass as pointer argument with "sret" attribute
                    attrs[count] = ms.add_attr ? sret_attr : null;
                    aligns[count] = Math.toIntExact(ms.layout.byteAlignment());
                    target_args[count] = LLVMBuildIntToPtr(builder, stub_args[index],
                            ptr_t(layoutToLLVMType(context, ms.layout)), "");
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
                    stub_args[index] = LLVMBuildIntToPtr(builder, stub_args[index],
                            ptr_t(layoutToLLVMType(context, ws.wrapper)), "");
                    target_args[count] = LLVMBuildLoad(builder, stub_args[index], "");
                    // Note: alignment from layout, not wrapper
                    LLVMSetAlignment(target_args[count], Math.toIntExact(ws.layout.byteAlignment()));
                    index++;
                    count++;
                } else if (storage instanceof MemoryStorage ms) {
                    // pass as pointer with "byval" attribute
                    attrs[count] = ms.add_attr ? byval_attr : null;
                    aligns[count] = Math.toIntExact(ms.layout.byteAlignment());
                    target_args[count] = LLVMBuildIntToPtr(builder, stub_args[index],
                            ptr_t(layoutToLLVMType(context, ms.layout)), "");
                    index++;
                    count++;
                } else {
                    throw shouldNotReachHere();
                }
            }

            var call = build_call(builder, target, Arrays.copyOf(target_args, count));

            final int offset = LLVMAttributeFirstArgIndex;
            for (int i = 0; i < count; i++) {
                if (attrs[i] != null) {
                    LLVMAddCallSiteAttribute(call, i + offset, attrs[i]);
                }
                if (aligns[i] != 0) {
                    LLVMSetInstrParamAlignment(call, i + offset, aligns[i]);
                }
            }

            if (retVoid) {
                if (retStore != null) {
                    LLVMValueRef store = LLVMBuildStore(builder, call, retStore);
                    LLVMSetAlignment(store, retAlignment);
                }
                LLVMBuildRetVoid(builder);
            } else {
                LLVMBuildRet(builder, call);
            }
        }, function_name, scope);
    }

    private static class ReturnWrapper extends AbstractTransformer {
        //TODO: maybe use AbstractTransformer instead of MethodHandle?
        private final MethodHandle handle;
        private final MemoryLayout ret_layout;
        private final int ret_index;

        public ReturnWrapper(MethodHandle handle, MemoryLayout ret_layout, int ret_index) {
            this.handle = handle;
            this.ret_layout = ret_layout;
            this.ret_index = ret_index;
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.accessor();
            SegmentAllocator allocator = thiz_acc.getReference(ret_index);
            MemorySegment ret = allocator.allocate(ret_layout);
            MethodType cached_type = stack.type();
            stack.setType(handle.type());
            thiz_acc.setReference(ret_index, ret);
            invokeExactWithFrameNoChecks(handle, stack);
            stack.setType(cached_type);
            thiz_acc.setReference(RETURN_VALUE_IDX, ret);
        }
    }

    @Override
    protected MethodHandle arrangeDowncall(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        _LLVMStorageDescriptor target_descriptor = _LLVMCallingConvention.computeStorages(descriptor);
        MemoryLayout ret = null;
        _FunctionDescriptorImpl stub_descriptor = descriptor;
        if (options.isReturnInMemory()) {
            ret = stub_descriptor.returnLayoutPlain();
            stub_descriptor = stub_descriptor.dropReturnLayout()
                    .insertArgumentLayouts(0, ADDRESS);
        }
        // leading function pointer
        stub_descriptor = stub_descriptor.insertArgumentLayouts(0, ADDRESS);
        MethodType stubType = fdToRawMethodType(stub_descriptor, options.allowsHeapAccess());
        Arena scope = Arena.ofAuto();
        MemorySegment nativeStub = generateNativeDowncallStub(scope, target_descriptor, stub_descriptor, options);
        MethodHandle stub = generateJavaDowncallStub(scope, nativeStub, stubType, options);
        int capturedStateMask = options.capturedCallState()
                .mapToInt(_CapturableState::mask)
                .reduce(0, (a, b) -> a | b);
        capturedStateMask |= options.hasCapturedCallState() ? 1 << 31 : 0;
        var arranger = new DowncallArranger(stub, stub_descriptor.argumentLayouts(),
                (ValueLayout) stub_descriptor.returnLayoutPlain(),
                options.allowsHeapAccess(), capturedStateMask);
        MethodType handleType = fdToHandleMethodType(stub_descriptor);
        if (options.hasCapturedCallState()) {
            handleType = handleType.insertParameterTypes(0, MemorySegment.class);
        }
        if (options.isReturnInMemory()) {
            handleType = handleType.changeReturnType(MemorySegment.class);
        }
        MethodHandle handle = Transformers.makeTransformer(handleType, arranger);
        if (options.isReturnInMemory()) {
            //TODO: optimize if return layout is empty
            int ret_index = options.hasCapturedCallState() ? 2 : 1;
            MethodType type = handleType.changeParameterType(ret_index, SegmentAllocator.class);
            ReturnWrapper wrapper = new ReturnWrapper(handle, ret, ret_index);
            handle = Transformers.makeTransformer(type, wrapper);
        }
        if (options.hasCapturedCallState()) {
            int index = options.isReturnInMemory() ? 2 : 1;
            handle = moveArgument(handle, 0, index);
        }
        return handle;
    }

    private static int computeEnvIndex(_LLVMStorageDescriptor stub_descriptor, _LinkerOptions options) {
        int env_index = options.getJNIEnvArgIndex();
        if (env_index >= 0) {
            final int tmp = env_index;
            for (int i = 0; i < tmp; i++) {
                if (stub_descriptor.argumentStorage(i) instanceof NoStorage) env_index--;
            }
        }
        return env_index;
    }

    private static MemorySegment generateNativeUpcallStub(
            Arena scope, _LLVMStorageDescriptor stub_descriptor, _LinkerOptions options,
            long target_id, long class_id, long function_id) {
        class Holder {
            @DoNotShrink
            private static final Arena SCOPE = Arena.ofAuto();

            private static final Function<LLVMContextRef, LLVMValueRef> UNCAUGHT_EXCEPTION_MSG =
                    intptrFactory(SCOPE.allocateFrom("Uncaught exception in upcall:"));

            private static final Function<LLVMBuilderRef, LLVMValueRef> INIT =
                    functionPointerFactory(SCOPE, ENVGetter.INSTANCE, context ->
                            function_ptr_t(intptr_t(context)));

            private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> CALL =
                    pointerFactory(getJNINativeInterfaceOffset("CallStaticVoidMethod"), context ->
                            variadic_function_ptr_t(void_t(context), intptr_t(context),
                                    intptr_t(context), intptr_t(context)));

            private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> EXCEPTION_CHECK =
                    pointerFactory(getJNINativeInterfaceOffset("ExceptionCheck"), context ->
                            function_ptr_t(int1_t(context), intptr_t(context)));

            private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> FATAL_ERROR =
                    pointerFactory(getJNINativeInterfaceOffset("FatalError"), context ->
                            function_ptr_t(void_t(context), intptr_t(context), intptr_t(context)));
        }

        final String function_name = "stub";
        return generateFunctionCodeSegment((context, module, builder) -> {
            var sret_attr = LLVMCreateEnumAttribute(context, "sret", 0);
            var byval_attr = LLVMCreateEnumAttribute(context, "byval", 0);

            var stub_type = sdToLLVMType(context, stub_descriptor, -1);
            var stub = LLVMAddFunction(module, function_name, stub_type);

            var body = LLVMAppendBasicBlock(stub, "");
            var normal_exit = LLVMAppendBasicBlock(stub, "");

            // TODO?: check thread state (must be native)

            LLVMPositionBuilderAtEnd(builder, body);
            var stub_args = LLVMGetParams(stub);
            var target_args = new LLVMValueRef[stub_args.length + 5];

            int env_index = computeEnvIndex(stub_descriptor, options);
            var env_ptr = env_index >= 0 ? stub_args[env_index] :
                    build_call(builder, Holder.INIT.apply(builder));
            var env_iface = build_load_ptr(builder, intptr_t(context), env_ptr);

            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in stub_args[]
            var target = Holder.CALL.apply(builder, env_iface);
            target_args[count++] = env_ptr;
            target_args[count++] = const_intptr(context, class_id);
            target_args[count++] = const_intptr(context, function_id);
            target_args[count++] = const_intptr(context, target_id);
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
                } else if (retStorage instanceof MemoryStorage ms) {
                    // pass as pointer argument with "sret" attribute
                    var sret = stub_args[index++];
                    // Note: attribute index = index of arg + 1 or 0 for return
                    if (ms.add_attr) {
                        LLVMAddAttributeAtIndex(stub, index, sret_attr);
                    }
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
                } else if (index == env_index) {
                    // just skip
                    index++;
                } else if (storage instanceof RawStorage rs) {
                    var arg = stub_args[index++];
                    // varargs rules
                    if (rs.layout instanceof OfByte || rs.layout instanceof OfShort) {
                        arg = LLVMBuildSExt(builder, arg, int32_t(context), "");
                    } else if (rs.layout instanceof OfBoolean || rs.layout instanceof OfChar) {
                        arg = LLVMBuildZExt(builder, arg, int32_t(context), "");
                    } else if (rs.layout instanceof OfFloat) {
                        arg = LLVMBuildFPCast(builder, arg, double_t(context), "");
                    }
                    target_args[count++] = arg;
                } else if (storage instanceof WrapperStorage ws) {
                    var ltype = layoutToLLVMType(context, ws.layout);
                    var wtype = layoutToLLVMType(context, ws.wrapper);
                    var arg = target_args[count++] = LLVMBuildAlloca(builder, ltype, "");
                    arg = LLVMBuildPointerCast(builder, arg, ptr_t(wtype), "");
                    var store = LLVMBuildStore(builder, stub_args[index++], arg);
                    // Note: alignment from layout, not wrapper
                    LLVMSetAlignment(store, Math.toIntExact(ws.layout.byteAlignment()));
                } else if (storage instanceof MemoryStorage ms) {
                    // pass as pointer with "byval" attribute
                    var byval = stub_args[index++];
                    // Note: attribute index = index of arg + 1 or 0 for return
                    if (ms.add_attr) {
                        LLVMAddAttributeAtIndex(stub, index, byval_attr);
                    }
                    target_args[count++] = LLVMBuildPtrToInt(builder, byval, intptr_t(context), "");
                } else {
                    throw shouldNotReachHere();
                }
            }

            build_call(builder, target, Arrays.copyOf(target_args, count));
            if (options.allowExceptions()) {
                LLVMBuildBr(builder, normal_exit);
            } else {
                var uncaught_exception = LLVMAppendBasicBlock(stub, "");
                var test_exceptions = build_call(builder,
                        Holder.EXCEPTION_CHECK.apply(builder, env_iface), env_ptr);
                LLVMBuildCondBr(builder, test_exceptions, uncaught_exception, normal_exit);

                LLVMPositionBuilderAtEnd(builder, uncaught_exception);
                build_call(builder, Holder.FATAL_ERROR.apply(builder, env_iface),
                        env_ptr, Holder.UNCAUGHT_EXCEPTION_MSG.apply(context));
                LLVMBuildUnreachable(builder);
            }

            LLVMPositionBuilderAtEnd(builder, normal_exit);
            if (ret == null) {
                LLVMBuildRetVoid(builder);
            } else {
                LLVMValueRef load = LLVMBuildLoad(builder, ret, "");
                LLVMSetAlignment(load, Math.toIntExact(retLoad.byteAlignment()));
                LLVMBuildRet(builder, load);
            }
        }, function_name, scope);
    }

    @DoNotObfuscate
    @DoNotShrink
    @SuppressWarnings("unused")
    static class UpcallHelper {
        static {
            ClassUtils.makeClassPublic(UpcallHelper.class);
        }

        public static Arena createArena() {
            return Arena.ofConfined();
        }

        public static void closeArena(Arena arena) {
            arena.close();
        }

        public static void handleException(Throwable th) {
            handleUncaughtException(th);
        }

        public static MemorySegment makeSegment(long addr, long size, long align, Arena arena) {
            return _Utils.longToAddress(addr, size, align, (_MemorySessionImpl) arena.scope());
        }

        public static void putByte(long address, byte data) {
            AndroidUnsafe.putByteN(address, data);
        }

        public static void putShort(long address, short data) {
            AndroidUnsafe.putShortN(address, data);
        }

        public static void putInt(long address, int data) {
            AndroidUnsafe.putIntN(address, data);
        }

        public static void putFloat(long address, float data) {
            AndroidUnsafe.putFloatN(address, data);
        }

        public static void putLong(long address, long data) {
            AndroidUnsafe.putLongN(address, data);
        }

        public static void putDouble(long address, double data) {
            AndroidUnsafe.putDoubleN(address, data);
        }

        public static void putAddress(long address, MemorySegment data) {
            AndroidUnsafe.putWordN(address, _Utils.unboxSegment(data));
        }

        public static void putSegment(long address, MemorySegment data, long size) {
            // TODO: maybe faster implementation?
            MemorySegment out = MemorySegment.ofAddress(address).reinterpret(size);
            MemorySegment.copy(data, 0, out, 0, size);
        }
    }

    private static Method generateJavaUpcallStub(_FunctionDescriptorImpl descriptor, boolean allowExceptions) {
        final String method_name = "function";
        final TypeId mh = TypeId.of(MethodHandle.class);
        final TypeId ms = TypeId.of(MemorySegment.class);

        MemoryLayout[] args = descriptor.argumentLayouts().toArray(new MemoryLayout[0]);
        MemoryLayout ret = descriptor.returnLayoutPlain();

        MethodType target_type = fdToHandleMethodType(descriptor);
        MethodType stub_type;
        {
            _FunctionDescriptorImpl stub_descriptor = ret == null ? descriptor :
                    descriptor.dropReturnLayout().insertArgumentLayouts(0, ADDRESS);
            stub_type = fdToRawMethodType(stub_descriptor, false)
                    .insertParameterTypes(0, MethodHandle.class);
        }

        ProtoId target_proto = ProtoId.of(target_type);
        ProtoId stub_proto = ProtoId.of(stub_type);

        String stub_name = getStubName(target_proto, false);
        TypeId stub_id = TypeId.ofName(stub_name);

        var helper_id = TypeId.of(UpcallHelper.class);
        var arena_id = TypeId.of(Arena.class);

        MethodId mh_invoke_id = MethodId.of(mh, "invokeExact",
                ProtoId.of(TypeId.OBJECT, TypeId.OBJECT.array()));
        MethodId create_arena_id = MethodId.of(helper_id, "createArena", arena_id);
        MethodId close_arena_id = MethodId.of(helper_id, "closeArena", TypeId.V, arena_id);
        MethodId handle_exception_id = MethodId.of(helper_id,
                "handleException", TypeId.V, TypeId.of(Throwable.class));
        MethodId make_segment_id = MethodId.of(helper_id, "makeSegment",
                ms, TypeId.J, TypeId.J, TypeId.J, arena_id);

        MethodId put_byte_id = MethodId.of(helper_id, "putByte", TypeId.V, TypeId.J, TypeId.B);
        MethodId put_short_id = MethodId.of(helper_id, "putShort", TypeId.V, TypeId.J, TypeId.S);
        MethodId put_int_id = MethodId.of(helper_id, "putInt", TypeId.V, TypeId.J, TypeId.I);
        MethodId put_float_id = MethodId.of(helper_id, "putFloat", TypeId.V, TypeId.J, TypeId.F);
        MethodId put_long_id = MethodId.of(helper_id, "putLong", TypeId.V, TypeId.J, TypeId.J);
        MethodId put_double_id = MethodId.of(helper_id, "putDouble", TypeId.V, TypeId.J, TypeId.D);
        MethodId put_address_id = MethodId.of(helper_id, "putAddress", TypeId.V, TypeId.J, ms);
        MethodId put_segment_id = MethodId.of(helper_id, "putSegment", TypeId.V, TypeId.J, ms, TypeId.J);

        int target_ins = target_proto.countInputRegisters();

        var check_exceptions = !allowExceptions;
        var has_arena = Stream.of(args).anyMatch(layout ->
                layout instanceof AddressLayout || layout instanceof GroupLayout);

        int[] reserved = {7};
        int arena_reg = has_arena ? reserved[0]++ : -1;
        int exception_reg = (check_exceptions || has_arena) ? reserved[0]++ : -1;

        int[] stub_ret_arg = {-1};
        int target_ret_arg = ret == null ? -1 : reserved[0];
        reserved[0] += ret == null ? 0 : (ret instanceof OfLong || ret instanceof OfDouble ? 2 : 1);

        int[] regs = {/* target args start */ reserved[0], /* stub args start */ 0};
        int locals = reserved[0] + target_ins + /* handle */ 1;

        Consumer<CodeBuilder> create_arena = ib -> ib
                .invoke(STATIC, create_arena_id)
                .move_result_object(ib.l(arena_reg));

        Consumer<CodeBuilder> close_arena = ib -> ib
                .invoke(STATIC, close_arena_id, ib.l(arena_reg));

        Consumer<CodeBuilder> handle_exception = ib -> ib
                .invoke(STATIC, handle_exception_id, ib.l(exception_reg))
                // Unreachable, but you need to explicitly
                //  tell the verifier that execution ends here
                .throw_(ib.l(exception_reg));

        ClassDef stub_def = ClassBuilder.build(stub_id, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withMethod(mb -> mb
                        .withFlags(ACC_PRIVATE | ACC_STATIC)
                        .withName(method_name)
                        .withProto(stub_proto)
                        .withCode(locals, ib -> ib
                                .label("try_1_start")
                                .if_(has_arena, create_arena)
                                .label("try_1_end")

                                .label("try_2_start")
                                // target handle
                                .move_object(ib.l(regs[0]++), ib.p(regs[1]++))
                                .commit(ib2 -> {
                                    if (ret != null) {
                                        stub_ret_arg[0] = regs[1];
                                        regs[1] += IS64BIT ? 2 : 1;
                                    }
                                    for (var arg : args) {
                                        if (arg instanceof OfByte || arg instanceof OfBoolean
                                                || arg instanceof OfShort | arg instanceof OfChar
                                                || arg instanceof OfInt || arg instanceof OfFloat) {
                                            ib2.move(ib2.l(regs[0]++), ib2.p(regs[1]++));
                                        } else if (arg instanceof OfLong || arg instanceof OfDouble) {
                                            ib2.move_wide(ib2.l(regs[0]), ib2.p(regs[1]));
                                            regs[0] += 2;
                                            regs[1] += 2;
                                        } else if (arg instanceof AddressLayout || arg instanceof GroupLayout) {
                                            if (IS64BIT) {
                                                ib2.move_wide(ib2.l(0), ib2.p(regs[1]));
                                                regs[1] += 2;
                                            } else {
                                                ib2.move(ib2.l(0), ib2.p(regs[1]++));
                                                ib2.unop(INT_TO_LONG, ib2.l(0), ib2.l(0));
                                                ib2.const_wide(ib2.l(2), 0xffffffffL);
                                                ib2.binop_2addr(AND_LONG, ib2.l(0), ib2.l(2));
                                            }
                                            long size, align;
                                            if (arg instanceof AddressLayout al) {
                                                MemoryLayout target = al.targetLayout().orElse(null);
                                                size = target == null ? 0 : target.byteSize();
                                                align = target == null ? 1 : target.byteAlignment();
                                            } else {
                                                size = arg.byteSize();
                                                align = arg.byteAlignment();
                                            }
                                            ib2.const_wide(ib2.l(2), size);
                                            ib2.const_wide(ib2.l(4), align);
                                            ib2.move_object(ib2.l(6), arena_reg);
                                            ib2.invoke_range(STATIC, make_segment_id, 7, ib2.l(0));
                                            ib2.move_result_object(ib2.l(0));
                                            ib2.move_object(ib2.l(regs[0]++), ib2.l(0));
                                        } else {
                                            throw shouldNotReachHere();
                                        }
                                    }
                                })

                                .invoke_polymorphic_range(mh_invoke_id, target_proto,
                                        target_ins + /* handle */ 1, ib.l(reserved[0]))
                                .if_(ret != null, ib2 -> {
                                    if (ret instanceof AddressLayout || ret instanceof GroupLayout) {
                                        ib2.move_result_object(ib2.l(target_ret_arg));
                                    } else if (ret instanceof OfLong || ret instanceof OfDouble) {
                                        ib2.move_result_wide(ib2.l(target_ret_arg));
                                    } else {
                                        ib2.move_result(ib2.l(target_ret_arg));
                                    }
                                })

                                .if_(ret != null, ib2 -> {
                                    if (IS64BIT) {
                                        ib2.move_wide(ib2.l(0), ib2.p(stub_ret_arg[0]));
                                    } else {
                                        ib2.move(ib2.l(0), ib2.p(stub_ret_arg[0]));
                                        ib2.unop(INT_TO_LONG, ib2.l(0), ib2.l(0));
                                        ib2.const_wide(ib2.l(2), 0xffffffffL);
                                        ib2.binop_2addr(AND_LONG, ib2.l(0), ib2.l(2));
                                    }

                                    if (ret instanceof AddressLayout || ret instanceof GroupLayout) {
                                        ib2.move_object(ib2.l(2), ib2.l(target_ret_arg));
                                    } else if (ret instanceof OfLong || ret instanceof OfDouble) {
                                        ib2.move_wide(ib2.l(2), ib2.l(target_ret_arg));
                                    } else {
                                        ib2.move(ib2.l(2), ib2.l(target_ret_arg));
                                    }

                                    if (ret instanceof OfByte || ret instanceof OfBoolean) {
                                        ib2.unop(INT_TO_BYTE, ib2.l(2), ib2.l(2));
                                        ib2.invoke_range(STATIC, put_byte_id, 3, ib2.l(0));
                                    } else if (ret instanceof OfShort | ret instanceof OfChar) {
                                        ib2.unop(INT_TO_SHORT, ib2.l(2), ib2.l(2));
                                        ib2.invoke_range(STATIC, put_short_id, 3, ib2.l(0));
                                    } else if (ret instanceof OfInt) {
                                        ib2.invoke_range(STATIC, put_int_id, 3, ib2.l(0));
                                    } else if (ret instanceof OfFloat) {
                                        ib2.invoke_range(STATIC, put_float_id, 3, ib2.l(0));
                                    } else if (ret instanceof OfLong) {
                                        ib2.invoke_range(STATIC, put_long_id, 4, ib2.l(0));
                                    } else if (ret instanceof OfDouble) {
                                        ib2.invoke_range(STATIC, put_double_id, 4, ib2.l(0));
                                    } else if (ret instanceof AddressLayout) {
                                        ib2.invoke_range(STATIC, put_address_id, 3, ib2.l(0));
                                    } else if (ret instanceof GroupLayout gl) {
                                        // TODO: optimize if return layout is empty
                                        ib2.const_wide(ib2.l(3), gl.byteSize());
                                        ib2.invoke_range(STATIC, put_segment_id, 5, ib2.l(0));
                                    } else {
                                        throw shouldNotReachHere();
                                    }
                                })
                                .label("try_2_end")

                                .label("try_3_start")
                                .if_(has_arena, close_arena)
                                .return_void()
                                .label("try_3_end")

                                .if_(has_arena, ib2 -> ib2
                                        .try_catch_all("try_2_start", "try_2_end")
                                        .label("try_4_start")
                                        .move_exception(ib2.l(exception_reg))
                                        .commit(close_arena)
                                        .throw_(ib2.l(exception_reg))
                                        .label("try_4_end")
                                )
                                .if_(check_exceptions, ib2 -> ib2
                                        .try_catch_all("try_1_start", "try_1_end")
                                        .if_(!has_arena, ib3 -> ib3
                                                .try_catch_all("try_2_start", "try_2_end")
                                        )
                                        .try_catch_all("try_3_start", "try_3_end")
                                        .if_(has_arena, ib3 -> ib3
                                                .try_catch_all("try_4_start", "try_4_end")
                                        )
                                        .move_exception(ib2.l(exception_reg))
                                        .commit(handle_exception)
                                )
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(stub_def)));
        Class<?> stub_class = loadClass(dex, stub_name, newEmptyClassLoader());

        return getDeclaredMethod(stub_class, method_name, stub_type.parameterArray());
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(
            MethodType target_type, _FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        var native_descriptor = !options.hasJNIEnvArg() ? descriptor :
                descriptor.insertArgumentLayouts(options.getJNIEnvArgIndex(), ADDRESS);
        _LLVMStorageDescriptor stub_descriptor = _LLVMCallingConvention.computeStorages(native_descriptor);
        Method stub = generateJavaUpcallStub(descriptor, options.allowExceptions());

        return (target, arena) -> {
            long target_id = JNIUtils.NewGlobalRef(target, arena);
            long class_id = JNIUtils.NewGlobalRef(stub.getDeclaringClass(), arena);
            long function_id = JNIUtils.FromReflectedMethod(stub);

            return generateNativeUpcallStub(arena, stub_descriptor, options,
                    target_id, class_id, function_id);
        };
    }
}

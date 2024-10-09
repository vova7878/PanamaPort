package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
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
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.foreign.ENVGetter;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
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
import java.util.function.Function;

import dalvik.system.DexFile;

final class _AndroidLinkerImpl extends _AbstractAndroidLinker {
    public static final Linker INSTANCE = new _AndroidLinkerImpl();
    public static final VarHandle VH_ERRNO = _CapturableState.LAYOUT.varHandle(groupElement("errno"));

    private static MemorySegment nextSegment(StackFrameAccessor reader, MemoryLayout layout) {
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

        private void copyArg(StackFrameAccessor reader, StackFrameAccessor writer,
                             MemoryLayout layout, Arena arena,
                             List<_MemorySessionImpl> acquiredSessions) {
            if (layout instanceof JavaValueLayout<?> valueLayout) {
                Class<?> carrier = valueLayout.carrier();
                EmulatedStackFrame.copyNext(reader, writer, carrier);
                return;
            }
            _AbstractMemorySegmentImpl segment = (_AbstractMemorySegmentImpl)
                    reader.nextReference(MemorySegment.class);
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
        }

        private void copyRet(StackFrameAccessor reader, StackFrameAccessor writer,
                             ValueLayout layout) {
            Class<?> type = layout.carrier();
            if (type == MemorySegment.class) {
                MemoryLayout target = ((AddressLayout) layout).targetLayout().orElse(null);
                writer.putNextReference(nextSegment(reader, target), MemorySegment.class);
                return;
            }
            EmulatedStackFrame.copyNext(reader, writer, type);
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) throws Throwable {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();

            List<_MemorySessionImpl> acquiredSessions = new ArrayList<>(segment_params_count);
            try (Arena arena = Arena.ofConfined()) {
                _AbstractMemorySegmentImpl capturedState = null;
                if (capturedStateMask < 0) {
                    capturedState = (_AbstractMemorySegmentImpl) thiz_acc.nextReference(MemorySegment.class);
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
                (downcall ? "Downcall" : "Upcall") + "Stub_" + proto.getShorty();
    }

    private static MethodHandle generateJavaDowncallStub(
            Arena scope, MemorySegment nativeStub, MethodType stubType, _LinkerOptions options) {
        final String method_name = "function";
        final String field_name = "scope";

        MethodType primitive_type = fixObjectParameters(stubType);
        ProtoId stub_proto = ProtoId.of(primitive_type);
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

    private static LLVMTypeRef sdToLLVMType(LLVMContextRef context, _LLVMStorageDescriptor descriptor,
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
            StackFrameAccessor thiz_acc = stack.createAccessor();
            SegmentAllocator allocator = thiz_acc.getReference(ret_index, SegmentAllocator.class);
            MemorySegment ret = allocator.allocate(ret_layout);
            MethodType cached_type = stack.type();
            thiz_acc.setType(handle.type());
            thiz_acc.setReference(ret_index, ret, MemorySegment.class);
            invokeExactWithFrameNoChecks(handle, stack);
            stack.setType(cached_type);
            thiz_acc.moveToReturn().putNextReference(ret, MemorySegment.class);
        }
    }

    @Override
    protected MethodHandle arrangeDowncall(FunctionDescriptor descriptor, _LinkerOptions options) {
        _FunctionDescriptorImpl descriptor_impl = (_FunctionDescriptorImpl) descriptor;
        _LLVMStorageDescriptor target_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
        MemoryLayout ret = null;
        _FunctionDescriptorImpl stub_descriptor = descriptor_impl;
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
            //TODO: optimize if return layout if empty
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
        Class<?> stub_class = loadClass(dex, stub_name, newEmptyClassLoader());

        return getDeclaredMethod(stub_class, method_name, stub_type.parameterArray());
    }

    private static MemorySegment generateNativeUpcallStub(
            Arena scope, _LLVMStorageDescriptor stub_descriptor,
            long arranger_id, long class_id, long function_id) {
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

            var init = LLVMAppendBasicBlock(stub, "");
            var body = LLVMAppendBasicBlock(stub, "");
            var uncaught_exception = LLVMAppendBasicBlock(stub, "");
            var normal_exit = LLVMAppendBasicBlock(stub, "");

            LLVMPositionBuilderAtEnd(builder, init);
            var env_ptr = build_call(builder, Holder.INIT.apply(builder));
            var env_iface = build_load_ptr(builder, intptr_t(context), env_ptr);
            LLVMBuildBr(builder, body);

            // TODO?: check thread state (must be native)

            LLVMPositionBuilderAtEnd(builder, body);
            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in stub_args[]
            var stub_args = LLVMGetParams(stub);
            var target = Holder.CALL.apply(builder, env_iface);
            var target_args = new LLVMValueRef[stub_args.length + 5];
            target_args[count++] = env_ptr;
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
            var test_exceptions = build_call(builder,
                    Holder.EXCEPTION_CHECK.apply(builder, env_iface), env_ptr);
            LLVMBuildCondBr(builder, test_exceptions, uncaught_exception, normal_exit);

            LLVMPositionBuilderAtEnd(builder, uncaught_exception);
            build_call(builder, Holder.FATAL_ERROR.apply(builder, env_iface),
                    env_ptr, Holder.UNCAUGHT_EXCEPTION_MSG.apply(context));
            LLVMBuildUnreachable(builder);

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

    private static class UpcallArranger extends AbstractTransformer {
        private final MethodHandle stub;
        private final MemoryLayout[] args;
        private final MemoryLayout ret;

        public UpcallArranger(MethodHandle stub, _FunctionDescriptorImpl descriptor) {
            this.stub = stub;
            this.args = descriptor.argumentLayouts().toArray(new MemoryLayout[0]);
            this.ret = descriptor.returnLayoutPlain();
        }

        private static void copyArg(StackFrameAccessor reader, StackFrameAccessor writer,
                                    MemoryLayout layout, Arena arena) {
            if (layout instanceof AddressLayout al) {
                MemoryLayout target = al.targetLayout().orElse(null);
                MemorySegment segment = nextSegment(reader, target);
                segment = segment.reinterpret(arena, null);
                writer.putNextReference(segment, MemorySegment.class);
            } else if (layout instanceof ValueLayout vl) {
                EmulatedStackFrame.copyNext(reader, writer, vl.carrier());
            } else if (layout instanceof GroupLayout gl) {
                MemorySegment segment = nextSegment(reader, gl);
                segment = segment.reinterpret(arena, null);
                writer.putNextReference(segment, MemorySegment.class);
            } else {
                throw shouldNotReachHere();
            }
        }

        private static void copyRet(StackFrameAccessor reader, MemorySegment ret, MemoryLayout layout) {
            if (layout instanceof OfByte bl) {
                ret.set(bl, 0, reader.nextByte());
            } else if (layout instanceof OfBoolean bl) {
                ret.set(bl, 0, reader.nextBoolean());
            } else if (layout instanceof OfShort sl) {
                ret.set(sl, 0, reader.nextShort());
            } else if (layout instanceof OfChar cl) {
                ret.set(cl, 0, reader.nextChar());
            } else if (layout instanceof OfInt il) {
                ret.set(il, 0, reader.nextInt());
            } else if (layout instanceof OfFloat fl) {
                ret.set(fl, 0, reader.nextFloat());
            } else if (layout instanceof OfLong ll) {
                ret.set(ll, 0, reader.nextLong());
            } else if (layout instanceof OfDouble dl) {
                ret.set(dl, 0, reader.nextDouble());
            } else if (layout instanceof AddressLayout al) {
                ret.set(al, 0, reader.nextReference(MemorySegment.class));
            } else if (layout instanceof GroupLayout gl) {
                //TODO: optimize if return layout if empty
                MemorySegment arg = reader.nextReference(MemorySegment.class);
                MemorySegment.copy(arg, 0, ret, 0, gl.byteSize());
            } else {
                throw shouldNotReachHere();
            }
        }

        @Override
        public void transform(MethodHandle ignored, EmulatedStackFrame stack) {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();
            MemorySegment retSeg = ret == null ? null : nextSegment(thiz_acc, ret);
            try (Arena arena = Arena.ofConfined()) {
                for (MemoryLayout arg : args) {
                    copyArg(thiz_acc, stub_acc, arg, arena);
                }
                try {
                    invokeExactWithFrameNoChecks(stub, stub_frame);
                } catch (Throwable th) {
                    handleUncaughtException(th);
                }
                if (ret != null) {
                    stub_acc.moveToReturn();
                    copyRet(stub_acc, retSeg, ret);
                }
            }
        }
    }

    @Override
    protected UpcallStubFactory arrangeUpcall(
            MethodType target_type, FunctionDescriptor descriptor, _LinkerOptions unused) {
        _FunctionDescriptorImpl descriptor_impl = (_FunctionDescriptorImpl) descriptor;
        _LLVMStorageDescriptor stub_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
        MemoryLayout ret = descriptor_impl.returnLayoutPlain();
        _FunctionDescriptorImpl arranger_descriptor = ret == null ? descriptor_impl :
                descriptor_impl.dropReturnLayout().insertArgumentLayouts(0, ADDRESS);
        MethodType arranger_type = fdToRawMethodType(arranger_descriptor, false);
        Method stub = generateJavaUpcallStub(arranger_type);

        return (target, arena) -> {
            MethodHandle arranger = Transformers.makeTransformer(arranger_type,
                    new UpcallArranger(target, descriptor_impl));

            long arranger_id = JNIUtils.NewGlobalRef(arranger, arena);
            long class_id = JNIUtils.NewGlobalRef(stub.getDeclaringClass(), arena);
            long function_id = JNIUtils.FromReflectedMethod(stub);

            return generateNativeUpcallStub(arena, stub_descriptor, arranger_id, class_id, function_id);
        };
    }
}

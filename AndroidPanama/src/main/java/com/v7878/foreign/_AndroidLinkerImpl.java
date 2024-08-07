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
import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMArrayType;
import static com.v7878.llvm.Core.LLVMAttributeIndex.LLVMAttributeFirstArgIndex;
import static com.v7878.llvm.Core.LLVMBuildAlloca;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildFPCast;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
import static com.v7878.llvm.Core.LLVMBuildPointerCast;
import static com.v7878.llvm.Core.LLVMBuildPtrToInt;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildSExt;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMBuildUnreachable;
import static com.v7878.llvm.Core.LLVMBuildZExt;
import static com.v7878.llvm.Core.LLVMConstAllOnes;
import static com.v7878.llvm.Core.LLVMConstNull;
import static com.v7878.llvm.Core.LLVMCreateEnumAttribute;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetInstrParamAlignment;
import static com.v7878.llvm.Core.LLVMStructTypeInContext;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.handleUncaughtException;
import static com.v7878.unsafe.Utils.newEmptyClassLoader;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
import static com.v7878.unsafe.foreign.LibArt.ART;
import static com.v7878.unsafe.invoke.Transformers.invokeExactWithFrameNoChecks;
import static com.v7878.unsafe.llvm.LLVMGlobals.double_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.float_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int16_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int1_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int32_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int64_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int8_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.intptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.ptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.void_ptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.buildRawObjectToAddress;
import static com.v7878.unsafe.llvm.LLVMUtils.const_int32;
import static com.v7878.unsafe.llvm.LLVMUtils.const_intptr;
import static com.v7878.unsafe.llvm.LLVMUtils.const_load_ptr;
import static com.v7878.unsafe.llvm.LLVMUtils.const_ptr;
import static com.v7878.unsafe.llvm.LLVMUtils.generateFunctionCodeSegment;
import static com.v7878.unsafe.llvm.LLVMUtils.getBuilderContext;

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
import com.v7878.foreign._StorageDescriptor.LLVMStorage;
import com.v7878.foreign._StorageDescriptor.MemoryStorage;
import com.v7878.foreign._StorageDescriptor.NoStorage;
import com.v7878.foreign._StorageDescriptor.RawStorage;
import com.v7878.foreign._StorageDescriptor.WrapperStorage;
import com.v7878.foreign._ValueLayouts.JavaValueLayout;
import com.v7878.invoke.VarHandle;
import com.v7878.llvm.Types.LLVMAttributeRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.foreign.Errno;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.EmulatedStackFrame.StackFrameAccessor;
import com.v7878.unsafe.invoke.MethodHandlesFixes;
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
                // TODO: improve performance
                MemorySegment tmp = arena.allocate(layout);
                // by-value struct
                MemorySegment.copy(segment, 0, tmp, 0, layout.byteSize());
                segment = (_AbstractMemorySegmentImpl) tmp;
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
        Class<?> stub_class = loadClass(dex, stub_name, newEmptyClassLoader());

        Field field = getDeclaredField(stub_class, field_name);
        putObject(stub_class, fieldOffset(field), scope);

        Method function = getDeclaredMethod(stub_class, method_name, gType.parameterArray());
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
            Arena scope, _StorageDescriptor target_descriptor,
            _FunctionDescriptorImpl stub_descriptor, _LinkerOptions options) {
        final String function_name = "stub";
        return generateFunctionCodeSegment((context, module, builder) -> {
            var sret_attr = LLVMCreateEnumAttribute(context, "sret", 0);
            var byval_attr = LLVMCreateEnumAttribute(context, "byval", 0);

            LLVMTypeRef stub_type = fdToLLVMType(context, stub_descriptor,
                    options.allowsHeapAccess(), !options.isCritical());
            LLVMValueRef stub = LLVMAddFunction(module, function_name, stub_type);

            LLVMTypeRef target_type = sdToLLVMType(context, target_descriptor, options.firstVariadicArgIndex());
            LLVMTypeRef target_type_ptr = ptr_t(target_type);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(stub, ""));

            LLVMValueRef[] stub_args;
            if (options.allowsHeapAccess()) {
                List<LLVMValueRef> out = new ArrayList<>();
                LLVMValueRef[] tmp = LLVMGetParams(stub);
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
            LLVMValueRef target = LLVMBuildIntToPtr(builder, stub_args[index++], target_type_ptr, "");
            LLVMValueRef[] target_args = new LLVMValueRef[stub_args.length - index];
            LLVMAttributeRef[] attrs = new LLVMAttributeRef[target_args.length];
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

            LLVMValueRef call = LLVMBuildCall(builder, target, Arrays.copyOf(target_args, count), "");

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

    @SuppressWarnings("ClassCanBeRecord")
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
        _StorageDescriptor target_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
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
            Arena scope, _StorageDescriptor stub_descriptor,
            long arranger_id, long class_id, long function_id) {
        // TODO: use pthread_key_create to init threadlocal JNIEnv
        class Holder {
            private static final Arena SCOPE = Arena.ofAuto();
            private static int f_ptrs_count = 0;

            private enum Symbols {
                LOG_ASSERT(true, ART.findOrThrow("__android_log_assert")),
                GET_ENV(true, JNIUtils.getJNIInvokeInterfaceFunction("GetEnv")),
                ATTACH(true, JNIUtils.getJNIInvokeInterfaceFunction("AttachCurrentThreadAsDaemon")),
                DETACH(true, JNIUtils.getJNIInvokeInterfaceFunction("DetachCurrentThread")),
                CALL(true, JNIUtils.getJNINativeInterfaceFunction("CallStaticVoidMethod")),
                EXCEPTION_CHECK(true, JNIUtils.getJNINativeInterfaceFunction("ExceptionCheck")),
                FATAL_ERROR(true, JNIUtils.getJNINativeInterfaceFunction("FatalError")),
                JVM(false, JNIUtils.getJavaVMPtr()),
                LOG_TAG(false, SCOPE.allocateFrom(Utils.LOG_TAG)),
                LOG_GET_ENV_MSG(false, SCOPE.allocateFrom("Could not get JNIEnv for upcall. JNI error code: %d")),
                LOG_ATTACH_MSG(false, SCOPE.allocateFrom("Could not attach thread for upcall. JNI error code: %d")),
                LOG_DETACH_MSG(false, SCOPE.allocateFrom("Could not detach current thread. JNI error code: %d")),
                UNCAUGHT_EXCEPTION_MSG(false, SCOPE.allocateFrom("Uncaught exception in upcall:"));

                private static final MemorySegment pointers;

                static {
                    pointers = SCOPE.allocate(ADDRESS, f_ptrs_count);
                    for (var symbol : values()) {
                        if (symbol.is_function) {
                            pointers.setAtIndex(ADDRESS, symbol.index, symbol.value);
                        }
                    }
                }

                private final MemorySegment value;
                private final boolean is_function;
                private final int index;

                Symbols(boolean is_function, MemorySegment value) {
                    this.value = value;
                    this.is_function = is_function;
                    this.index = is_function ? f_ptrs_count++ : -1;
                }

                public LLVMValueRef getFPtr(LLVMBuilderRef builder, LLVMTypeRef type) {
                    assert is_function;
                    return const_load_ptr(builder, type, pointers.nativeAddress(), (long) ADDRESS_SIZE * index);
                }

                public LLVMValueRef getValue(LLVMContextRef context, LLVMTypeRef type) {
                    assert !is_function;
                    return const_ptr(context, type, value.nativeAddress());
                }
            }

            static LLVMValueRef log_assert(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{intptr_t(context),
                        ptr_t(int8_t(context)), ptr_t(int8_t(context))}, true);
                return Symbols.LOG_ASSERT.getFPtr(builder, type);
            }

            static LLVMValueRef get_env(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int32_t(context), new LLVMTypeRef[]{void_ptr_t(context),
                        ptr_t(void_ptr_t(context)), int32_t(context)}, false);
                return Symbols.GET_ENV.getFPtr(builder, type);
            }

            static LLVMValueRef attach(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int32_t(context), new LLVMTypeRef[]{void_ptr_t(context),
                        ptr_t(void_ptr_t(context)), intptr_t(context)}, false);
                return Symbols.ATTACH.getFPtr(builder, type);
            }

            static LLVMValueRef detach(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int32_t(context), new LLVMTypeRef[]{void_ptr_t(context)}, false);
                return Symbols.DETACH.getFPtr(builder, type);
            }

            static LLVMValueRef call(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var intptr = intptr_t(context);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{
                        void_ptr_t(context), intptr, intptr}, true);
                return Symbols.CALL.getFPtr(builder, type);
            }

            static LLVMValueRef exceptionCheck(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(int1_t(context),
                        new LLVMTypeRef[]{void_ptr_t(context)}, false);
                return Symbols.EXCEPTION_CHECK.getFPtr(builder, type);
            }

            static LLVMValueRef fatalError(LLVMBuilderRef builder) {
                var context = getBuilderContext(builder);
                var type = LLVMFunctionType(void_t(context), new LLVMTypeRef[]{void_ptr_t(context),
                        ptr_t(int8_t(context))}, false);
                return Symbols.FATAL_ERROR.getFPtr(builder, type);
            }

            static LLVMValueRef jvm(LLVMContextRef context) {
                return Symbols.JVM.getValue(context, void_t(context));
            }

            static LLVMValueRef log_tag(LLVMContextRef context) {
                return Symbols.LOG_TAG.getValue(context, int8_t(context));
            }

            static LLVMValueRef log_get_env_msg(LLVMContextRef context) {
                return Symbols.LOG_GET_ENV_MSG.getValue(context, int8_t(context));
            }

            static LLVMValueRef log_attach_msg(LLVMContextRef context) {
                return Symbols.LOG_ATTACH_MSG.getValue(context, int8_t(context));
            }

            static LLVMValueRef log_detach_msg(LLVMContextRef context) {
                return Symbols.LOG_DETACH_MSG.getValue(context, int8_t(context));
            }

            static LLVMValueRef uncaught_exception_msg(LLVMContextRef context) {
                return Symbols.UNCAUGHT_EXCEPTION_MSG.getValue(context, int8_t(context));
            }
        }

        final String function_name = "stub";
        return generateFunctionCodeSegment((context, module, builder) -> {

            var jni_ok = const_int32(context, 0);
            var jni_edetached = const_int32(context, -2);
            var jni_version = const_int32(context, /* JNI_VERSION_1_6 */ 0x00010006);

            var jvm = Holder.jvm(context);

            var sret_attr = LLVMCreateEnumAttribute(context, "sret", 0);
            var byval_attr = LLVMCreateEnumAttribute(context, "byval", 0);

            var stub_type = sdToLLVMType(context, stub_descriptor, -1);
            var stub = LLVMAddFunction(module, function_name, stub_type);

            var get_env = LLVMAppendBasicBlock(stub, "");
            var check_detached = LLVMAppendBasicBlock(stub, "");
            var attach = LLVMAppendBasicBlock(stub, "");
            var body = LLVMAppendBasicBlock(stub, "");
            var uncaught_exception = LLVMAppendBasicBlock(stub, "");
            var check_attached = LLVMAppendBasicBlock(stub, "");
            var detach = LLVMAppendBasicBlock(stub, "");
            var exit = LLVMAppendBasicBlock(stub, "");
            var abort = LLVMAppendBasicBlock(stub, "");

            LLVMPositionBuilderAtEnd(builder, abort);
            var abort_msg = LLVMBuildPhi(builder, ptr_t(int8_t(context)), "");
            var status = LLVMBuildPhi(builder, int32_t(context), "");
            LLVMBuildCall(builder, Holder.log_assert(builder), new LLVMValueRef[]{
                    const_intptr(context, 0), Holder.log_tag(context), abort_msg, status}, "");
            LLVMBuildUnreachable(builder);

            LLVMPositionBuilderAtEnd(builder, get_env);
            var env_ptr = LLVMBuildAlloca(builder, void_ptr_t(context), "");
            var jni_status = LLVMBuildCall(builder, Holder.get_env(builder), new LLVMValueRef[]{
                    jvm, env_ptr, jni_version}, "");
            var test = LLVMBuildICmp(builder, LLVMIntEQ, jni_status, jni_ok, "");
            LLVMBuildCondBr(builder, test, body, attach);

            LLVMPositionBuilderAtEnd(builder, check_detached);
            test = LLVMBuildICmp(builder, LLVMIntEQ, jni_status, jni_edetached, "");
            LLVMAddIncoming(abort_msg, Holder.log_get_env_msg(context), check_detached);
            LLVMAddIncoming(status, jni_status, check_detached);
            LLVMBuildCondBr(builder, test, attach, abort);

            LLVMPositionBuilderAtEnd(builder, attach);
            jni_status = LLVMBuildCall(builder, Holder.attach(builder), new LLVMValueRef[]{
                    jvm, env_ptr, const_intptr(context, 0)}, "");
            test = LLVMBuildICmp(builder, LLVMIntEQ, jni_status, jni_ok, "");
            LLVMAddIncoming(abort_msg, Holder.log_attach_msg(context), attach);
            LLVMAddIncoming(status, jni_status, attach);
            LLVMBuildCondBr(builder, test, body, abort);

            // TODO?: check thread state (must be native)

            LLVMPositionBuilderAtEnd(builder, body);
            var test_attached = LLVMBuildPhi(builder, int1_t(context), "");
            LLVMAddIncoming(test_attached, LLVMConstNull(int1_t(context)), get_env);
            LLVMAddIncoming(test_attached, LLVMConstAllOnes(int1_t(context)), attach);
            var env = LLVMBuildLoad(builder, env_ptr, "");
            int count = 0; // current index in target_args[] and their count
            int index = 0; // current index in stub_args[]
            var stub_args = LLVMGetParams(stub);
            var target = Holder.call(builder);
            var target_args = new LLVMValueRef[stub_args.length + 5];
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

            LLVMBuildCall(builder, target, Arrays.copyOf(target_args, count), "");
            var test_exceptions = LLVMBuildCall(builder,
                    Holder.exceptionCheck(builder), new LLVMValueRef[]{env}, "");
            LLVMBuildCondBr(builder, test_exceptions, uncaught_exception, check_attached);

            LLVMPositionBuilderAtEnd(builder, uncaught_exception);
            LLVMBuildCall(builder, Holder.fatalError(builder), new LLVMValueRef[]{env,
                    Holder.uncaught_exception_msg(context)}, "");
            LLVMBuildUnreachable(builder);

            LLVMPositionBuilderAtEnd(builder, check_attached);
            LLVMBuildCondBr(builder, test_attached, detach, exit);

            LLVMPositionBuilderAtEnd(builder, detach);
            jni_status = LLVMBuildCall(builder, Holder.detach(builder), new LLVMValueRef[]{jvm}, "");
            test = LLVMBuildICmp(builder, LLVMIntEQ, jni_status, jni_ok, "");
            LLVMAddIncoming(abort_msg, Holder.log_detach_msg(context), detach);
            LLVMAddIncoming(status, jni_status, detach);
            LLVMBuildCondBr(builder, test, exit, abort);

            LLVMPositionBuilderAtEnd(builder, exit);
            if (ret == null) {
                LLVMBuildRetVoid(builder);
            } else {
                LLVMValueRef load = LLVMBuildLoad(builder, ret, "");
                LLVMSetAlignment(load, Math.toIntExact(retLoad.byteAlignment()));
                LLVMBuildRet(builder, load);
            }
        }, function_name, scope);
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
            if (layout instanceof AddressLayout al) {
                MemoryLayout target = al.targetLayout().orElse(null);
                writer.putNextReference(readSegment(reader, target), MemorySegment.class);
            } else if (layout instanceof ValueLayout vl) {
                EmulatedStackFrame.copyNext(reader, writer, vl.carrier());
            } else if (layout instanceof GroupLayout gl) {
                writer.putNextReference(readSegment(reader, gl), MemorySegment.class);
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
                MemorySegment arg = reader.nextReference(MemorySegment.class);
                MemorySegment.copy(arg, 0, ret, 0, gl.byteSize());
            } else {
                throw shouldNotReachHere();
            }
        }

        @Override
        public void transform(EmulatedStackFrame stack) {
            StackFrameAccessor thiz_acc = stack.createAccessor();
            EmulatedStackFrame stub_frame = EmulatedStackFrame.create(stub.type());
            StackFrameAccessor stub_acc = stub_frame.createAccessor();
            MemorySegment retSeg = ret == null ? null : readSegment(thiz_acc, ret);
            for (MemoryLayout arg : args) {
                copyArg(thiz_acc, stub_acc, arg);
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

    @Override
    protected UpcallStubFactory arrangeUpcall(
            MethodType target_type, FunctionDescriptor descriptor, _LinkerOptions unused) {
        _FunctionDescriptorImpl descriptor_impl = (_FunctionDescriptorImpl) descriptor;
        _StorageDescriptor stub_descriptor = _LLVMCallingConvention.computeStorages(descriptor_impl);
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

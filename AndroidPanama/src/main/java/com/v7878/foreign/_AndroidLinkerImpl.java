package com.v7878.foreign;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.dex.DexConstants.ACC_PRIVATE;
import static com.v7878.dex.DexConstants.ACC_STATIC;
import static com.v7878.dex.builder.CodeBuilder.BinOp.AND_LONG;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_BYTE;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_LONG;
import static com.v7878.dex.builder.CodeBuilder.UnOp.INT_TO_SHORT;
import static com.v7878.dex.builder.CodeBuilder.UnOp.LONG_TO_INT;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.OfChar;
import static com.v7878.foreign.ValueLayout.OfDouble;
import static com.v7878.foreign.ValueLayout.OfFloat;
import static com.v7878.foreign.ValueLayout.OfInt;
import static com.v7878.foreign.ValueLayout.OfLong;
import static com.v7878.foreign._CapturableState.ERRNO;
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
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.JNIUtils.getJNINativeInterfaceOffset;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.handleUncaughtException;
import static com.v7878.unsafe.Utils.newEmptyClassLoader;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.ExtraLayouts.WORD;
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
import com.v7878.foreign._MemorySessionImpl.ResourceList.ResourceCleanup;
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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
    static {
        ClassUtils.makeClassPublic(DowncallHelper.class);
        ClassUtils.makeClassPublic(UpcallHelper.class);
    }

    public static final Linker INSTANCE = new _AndroidLinkerImpl();
    private static final VarHandle VH_ERRNO = _CapturableState.LAYOUT.varHandle(groupElement("errno"));

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
                assert options.isCritical();
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

    private static String layoutName(MemoryLayout layout) {
        if (layout == null) {
            return "V";
        }
        if (layout instanceof AddressLayout al) {
            return "A" + al.targetLayout().map(target ->
                    "_size_" + target.byteSize() + "_align_" + target.byteAlignment()
            ).orElse("");
        }
        if (layout instanceof GroupLayout gl) {
            return (gl instanceof StructLayout ? "S" : "U") +
                    "_size_" + gl.byteSize() + "_align_" + gl.byteAlignment();
        }
        if (layout instanceof ValueLayout vl) {
            return Character.toString(TypeId.of(vl.carrier()).getShorty());
        }
        throw shouldNotReachHere();
    }

    private static String stubName(_FunctionDescriptorImpl descriptor,
                                   _LinkerOptions options, boolean downcall) {
        StringBuilder out = new StringBuilder();
        out.append(_AndroidLinkerImpl.class.getName());
        out.append('$');
        out.append(downcall ? "Downcall" : "Upcall");
        out.append("Stub_");
        out.append(layoutName(descriptor.returnLayoutPlain()));
        out.append("_");
        for (var layout : descriptor.argumentLayouts()) {
            out.append(layoutName(layout));
        }
        if (options.allowExceptions()) {
            out.append("_E");
        }
        int env_index = options.getJNIEnvArgIndex();
        if (env_index >= 0) {
            out.append("_ENV_");
            out.append(env_index);
        }
        int var_index = options.firstVariadicArgIndex();
        if (var_index >= 0) {
            out.append("_VAR_");
            out.append(var_index);
        }
        if (options.hasCapturedCallState()) {
            out.append("_state");
            options.capturedCallState().forEach(state -> {
                out.append("$");
                out.append(state.stateName());
            });
        }
        if (options.isCritical()) {
            out.append("_critical");
            out.append(options.allowsHeapAccess() ? "_heap" : "_noheap");
        }
        return out.toString();
    }

    @DoNotObfuscate
    @DoNotShrink
    @SuppressWarnings("unused")
    static class DowncallHelper {
        public static Arena createArena(long size) {
            if (size == 0) return null;
            return _Utils.newBoundedArena(size);
        }

        public static MemorySegment allocateSegment(SegmentAllocator allocator, long size, long align) {
            return allocator.allocate(size, align);
        }

        // Note: If the execution is without heap access, and a heap segment is received,
        //  it must be copied to a temporary native segment so that it can be passed on
        public static MemorySegment allocateCopy(MemorySegment segment, Arena arena, long size, long align) {
            var session = ((_AbstractMemorySegmentImpl) segment).sessionImpl();
            if (size == 0) {
                session.checkValidState();
                return MemorySegment.NULL;
            }
            assert arena != null;
            if (segment.isNative()) {
                var tmp_session = ((_MemorySessionImpl) arena.scope());
                session.acquire0();
                tmp_session.addOrCleanupIfFail(
                        ResourceCleanup.ofRunnable(session::release0));
                return segment;
            } else {
                MemorySegment tmp = arena.allocate(size, align);
                // by-value struct
                MemorySegment.copy(segment, 0, tmp, 0, size);
                return tmp;
            }
        }

        public static void closeArena(Arena arena) {
            if (arena == null) return;
            arena.close();
        }

        public static MemorySegment checkCaptureSegment(MemorySegment segment) {
            return _Utils.checkCaptureSegment(segment);
        }

        public static MemorySegment makeSegment(long addr, long size, long align) {
            return _Utils.makeSegment(addr, size, align);
        }

        public static long unboxSymbolSegment(MemorySegment segment) {
            _Utils.checkSymbol(segment);
            return _Utils.unboxSegment(segment);
        }

        public static long unboxSegment(MemorySegment segment) {
            return _Utils.unboxSegment(segment);
        }

        public static Object getBase(MemorySegment segment) {
            return ((_AbstractMemorySegmentImpl) segment).unsafeGetBase();
        }

        public static long getOffset(MemorySegment segment) {
            return ((_AbstractMemorySegmentImpl) segment).unsafeGetOffset();
        }

        public static void acquire(MemorySegment segment) {
            ((_AbstractMemorySegmentImpl) segment).scope.acquire0();
        }

        public static void release(MemorySegment segment) {
            ((_AbstractMemorySegmentImpl) segment).scope.release0();
        }

        public static void putErrno(MemorySegment capturedState) {
            VH_ERRNO.set(capturedState, 0, Errno.errno());
        }
    }

    private static String label_for_reg(String prefix, int reg, boolean start) {
        return prefix + "_" + reg + "_" + (start ? "start" : "end");
    }

    private static MethodHandle generateJavaDowncallStub(
            MemorySegment native_stub, MethodType native_stub_type,
            _FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        final String native_stub_name = "function_native";
        final String java_stub_name = "function_java";
        final String scope_field_name = "scope";
        final String init_scope_name = "initScope";

        final TypeId ms_id = TypeId.of(MemorySegment.class);
        final TypeId helper_id = TypeId.of(DowncallHelper.class);
        final TypeId sa_id = TypeId.of(SegmentAllocator.class);
        final TypeId arena_id = TypeId.of(Arena.class);

        boolean heap_access = options.allowsHeapAccess();

        int capturedStateMask = options.capturedCallState()
                .mapToInt(_CapturableState::mask)
                .reduce(0, (a, b) -> a | b)
                | (options.hasCapturedCallState() ? 1 << 31 : 0);

        MemoryLayout[] args = descriptor.argumentLayouts().toArray(new MemoryLayout[0]);
        MemoryLayout ret = descriptor.returnLayoutPlain();

        MethodType java_stub_type = fdToHandleMethodType(descriptor);
        if (capturedStateMask < 0) {
            java_stub_type = java_stub_type.insertParameterTypes(0, MemorySegment.class);
        }
        if (options.isReturnInMemory()) {
            java_stub_type = java_stub_type.insertParameterTypes(0, SegmentAllocator.class);
        }
        // leading function pointer
        java_stub_type = java_stub_type.insertParameterTypes(0, MemorySegment.class);

        ProtoId native_stub_proto = ProtoId.of(native_stub_type);
        ProtoId java_stub_proto = ProtoId.of(java_stub_type);

        String stub_name = stubName(descriptor, options, true);
        TypeId stub_id = TypeId.ofName(stub_name);

        FieldId scope_id = FieldId.of(stub_id, scope_field_name, TypeId.OBJECT);
        MethodId native_stub_id = MethodId.of(stub_id, native_stub_name, native_stub_proto);

        MethodId create_arena_id = MethodId.of(helper_id, "createArena", arena_id, TypeId.J);
        MethodId allocate_segment_id = MethodId.of(helper_id,
                "allocateSegment", ms_id, sa_id, TypeId.J, TypeId.J);
        MethodId allocate_copy_id = MethodId.of(helper_id,
                "allocateCopy", ms_id, ms_id, arena_id, TypeId.J, TypeId.J);
        MethodId close_arena_id = MethodId.of(helper_id, "closeArena", TypeId.V, arena_id);
        MethodId check_capture_segment_id = MethodId.of(helper_id, "checkCaptureSegment", ms_id, ms_id);
        MethodId make_segment_id = MethodId.of(helper_id,
                "makeSegment", ms_id, TypeId.J, TypeId.J, TypeId.J);
        MethodId unbox_symbol_segment_id = MethodId.of(helper_id,
                "unboxSymbolSegment", TypeId.J, ms_id);
        MethodId unbox_segment_id = MethodId.of(helper_id, "unboxSegment", TypeId.J, ms_id);
        MethodId get_base_id = MethodId.of(helper_id, "getBase", TypeId.OBJECT, ms_id);
        MethodId get_offset_id = MethodId.of(helper_id, "getOffset", TypeId.J, ms_id);
        MethodId acquire_id = MethodId.of(helper_id, "acquire", TypeId.V, ms_id);
        MethodId release_id = MethodId.of(helper_id, "release", TypeId.V, ms_id);
        MethodId put_errno_id = MethodId.of(helper_id, "putErrno", TypeId.V, ms_id);

        var has_arena = !heap_access && Stream.of(args)
                .anyMatch(layout -> layout instanceof GroupLayout);
        var max_arena_size = !has_arena ? -1 : Stream.of(args)
                .filter(layout -> layout instanceof GroupLayout)
                .mapToLong(MemoryLayout::byteSize).sum();

        int native_ins = native_stub_proto.countInputRegisters();

        int[] state_arg = {-1};
        int[] allocator_arg = {-1};

        int[] reserved = {6};
        int arena_reg = has_arena ? reserved[0]++ : -1;
        int exception_reg = reserved[0]++;

        var native_ret_type = native_stub_proto.getReturnType();
        char native_ret_shorty = native_ret_type.getShorty();
        int native_ret_reg = native_ret_shorty == 'V' ? -1 : reserved[0];
        reserved[0] += native_ret_type.getRegisterCount();

        int[] regs = {/* native args start */ reserved[0], /* java args start */ 0};
        int locals = reserved[0] + native_ins;

        List<Integer> acquired_segments = new ArrayList<>();

        Consumer<CodeBuilder> create_arena = ib -> ib
                .const_wide(ib.l(0), max_arena_size)
                .invoke_range(STATIC, create_arena_id, 2, ib.l(0))
                .move_result_object(ib.l(arena_reg));

        Consumer<CodeBuilder> close_arena = ib -> ib
                .invoke(STATIC, close_arena_id, ib.l(arena_reg));

        ClassDef stub_def = ClassBuilder.build(stub_id, cb -> cb
                .withSuperClass(TypeId.OBJECT)
                .withField(fb -> fb
                        .of(scope_id)
                        .withFlags(ACC_PRIVATE | ACC_STATIC)
                )
                .withMethod(mb -> mb
                        .withName(init_scope_name)
                        .withReturnType(TypeId.V)
                        .withParameterTypes(TypeId.OBJECT)
                        .withFlags(ACC_PRIVATE | ACC_STATIC)
                        .withCode(0, ib -> ib
                                .generate_lines()
                                .sop(PUT_OBJECT, ib.p(0), scope_id)
                                .return_void()
                        )
                )
                .withMethod(mb -> mb
                        .of(native_stub_id)
                        .withFlags(ACC_PRIVATE | ACC_STATIC | ACC_NATIVE)
                        .if_(options.isCritical(), mb2 -> mb2
                                .withAnnotations(Annotation.CriticalNative())
                        )
                )
                .withMethod(mb -> mb
                        .withFlags(ACC_PRIVATE | ACC_STATIC)
                        .withName(java_stub_name)
                        .withProto(java_stub_proto)
                        .withCode(locals, ib -> ib
                                .generate_lines()
                                .if_(has_arena, create_arena)

                                .label("try_arena_start")
                                .commit(ib2 -> {
                                    int symbol_arg = regs[1]++;

                                    ib2.invoke_range(STATIC, acquire_id,
                                            1, ib2.p(symbol_arg));
                                    ib2.label(label_for_reg("try_segment", symbol_arg, true));
                                    acquired_segments.add(symbol_arg);

                                    ib2.invoke_range(STATIC, unbox_symbol_segment_id,
                                            1, ib2.p(symbol_arg));
                                    ib2.move_result_wide(ib2.l(0));
                                    if (IS64BIT) {
                                        ib2.move_wide(ib2.l(regs[0]), ib2.l(0));
                                        regs[0] += 2;
                                    } else {
                                        ib2.unop(LONG_TO_INT, ib2.l(0), ib2.l(0));
                                        ib2.move(ib2.l(regs[0]++), ib2.l(0));
                                    }
                                })
                                .if_(options.isReturnInMemory(), ib2 -> {
                                    assert ret instanceof GroupLayout;

                                    allocator_arg[0] = regs[1]++;

                                    ib2.move_object(ib2.l(0), ib2.p(allocator_arg[0]));
                                    ib2.const_wide(ib2.l(1), ret.byteSize());
                                    ib2.const_wide(ib2.l(3), ret.byteAlignment());

                                    ib2.invoke_range(STATIC, allocate_segment_id,
                                            5, ib2.l(0));
                                    ib2.move_result_object(ib2.l(0));
                                    ib2.move_object(ib2.p(allocator_arg[0]), ib2.l(0));

                                    if (heap_access) {
                                        ib2.invoke_range(STATIC, get_base_id,
                                                1, ib2.p(allocator_arg[0]));
                                        ib2.move_result_object(ib2.l(0));
                                        ib2.move_object(ib2.l(regs[0]++), ib2.l(0));
                                    }
                                    ib2.invoke_range(STATIC,
                                            heap_access ? get_offset_id : unbox_segment_id,
                                            1, ib2.p(allocator_arg[0]));
                                    ib2.move_result_wide(ib2.l(0));
                                    if (IS64BIT) {
                                        ib2.move_wide(ib2.l(regs[0]), ib2.l(0));
                                        regs[0] += 2;
                                    } else {
                                        ib2.unop(LONG_TO_INT, ib2.l(0), ib2.l(0));
                                        ib2.move(ib2.l(regs[0]++), ib2.l(0));
                                    }
                                })
                                .if_(capturedStateMask < 0, ib2 -> {
                                    state_arg[0] = regs[1]++;

                                    ib2.invoke_range(STATIC, check_capture_segment_id,
                                            1, ib2.p(state_arg[0]));
                                    ib2.move_result_object(ib2.l(0));
                                    ib2.move_object(ib2.p(state_arg[0]), ib2.l(0));

                                    ib2.invoke_range(STATIC, acquire_id,
                                            1, ib2.p(state_arg[0]));
                                    ib2.label(label_for_reg("try_segment", state_arg[0], true));
                                    acquired_segments.add(state_arg[0]);
                                })
                                .commit(ib2 -> {
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
                                            var segment_reg = regs[1]++;

                                            if (!heap_access && arg instanceof GroupLayout gl) {
                                                ib2.move_object(ib2.l(0), ib2.p(segment_reg));
                                                ib2.move_object(ib2.l(1), ib2.l(arena_reg));
                                                ib2.const_wide(ib2.l(2), gl.byteSize());
                                                ib2.const_wide(ib2.l(4), gl.byteAlignment());

                                                ib2.invoke_range(STATIC, allocate_copy_id,
                                                        6, ib2.l(0));
                                                ib2.move_result_object(ib2.l(0));
                                                ib2.move_object(ib2.p(segment_reg), ib2.l(0));
                                            } else {
                                                ib2.invoke_range(STATIC, acquire_id,
                                                        1, ib2.p(segment_reg));
                                                ib2.label(label_for_reg("try_segment", segment_reg, true));
                                                acquired_segments.add(segment_reg);
                                            }

                                            if (heap_access) {
                                                ib2.invoke_range(STATIC, get_base_id,
                                                        1, ib2.p(segment_reg));
                                                ib2.move_result_object(ib2.l(0));
                                                ib2.move_object(ib2.l(regs[0]++), ib2.l(0));
                                            }
                                            ib2.invoke_range(STATIC,
                                                    heap_access ? get_offset_id : unbox_segment_id,
                                                    1, ib2.p(segment_reg));
                                            ib2.move_result_wide(ib2.l(0));
                                            if (IS64BIT) {
                                                ib2.move_wide(ib2.l(regs[0]), ib2.l(0));
                                                regs[0] += 2;
                                            } else {
                                                ib2.unop(LONG_TO_INT, ib2.l(0), ib2.l(0));
                                                ib2.move(ib2.l(regs[0]++), ib2.l(0));
                                            }
                                        } else {
                                            throw shouldNotReachHere();
                                        }
                                    }
                                })

                                .invoke_range(STATIC, native_stub_id,
                                        native_ins, ib.l(reserved[0]))
                                .if_(native_ret_reg != -1, ib2 -> ib2.
                                        move_result_shorty(native_ret_shorty,
                                                ib2.l(native_ret_reg))
                                )

                                .if_((capturedStateMask & ERRNO.mask()) != 0, ib2 -> ib2
                                        .invoke_range(STATIC, put_errno_id, 1, ib2.p(state_arg[0]))
                                )

                                .commit(ib2 -> {
                                    for (int i = acquired_segments.size() - 1; i >= 0; i--) {
                                        int segment_reg = acquired_segments.get(i);
                                        ib2.label(label_for_reg("try_segment", segment_reg, false));

                                        ib2.invoke_range(STATIC, release_id,
                                                1, ib2.p(segment_reg));
                                    }
                                })
                                .label("try_arena_end")

                                .if_(has_arena, close_arena)

                                .commit(ib2 -> {
                                    if (ret == null) {
                                        ib2.return_void();
                                    } else if (ret instanceof AddressLayout al) {
                                        if (IS64BIT) {
                                            ib2.move_wide(ib2.l(0), ib2.l(native_ret_reg));
                                        } else {
                                            ib2.move(ib2.l(0), ib2.l(native_ret_reg));
                                            ib2.unop(INT_TO_LONG, ib2.l(0), ib2.l(0));
                                            ib2.const_wide(ib2.l(2), 0xffffffffL);
                                            ib2.binop_2addr(AND_LONG, ib2.l(0), ib2.l(2));
                                        }
                                        MemoryLayout target = al.targetLayout().orElse(null);
                                        long size = target == null ? 0 : target.byteSize();
                                        long align = target == null ? 1 : target.byteAlignment();

                                        ib2.const_wide(ib2.l(2), size);
                                        ib2.const_wide(ib2.l(4), align);
                                        ib2.invoke_range(STATIC, make_segment_id, 6, ib2.l(0));
                                        ib2.move_result_object(ib2.l(0));
                                        ib2.return_object(ib2.l(0));
                                    } else if (ret instanceof GroupLayout) {
                                        ib2.move_object(ib2.l(0), ib2.p(allocator_arg[0]));
                                        ib2.return_object(ib2.l(0));
                                    } else {
                                        ib2.return_shorty(native_ret_shorty, ib2.l(native_ret_reg));
                                    }
                                })
                                // TODO: Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V
                                .commit(ib2 -> {
                                    Integer previous_reg = null;
                                    for (int i = acquired_segments.size() - 1; i >= 0; i--) {
                                        int segment_reg = acquired_segments.get(i);

                                        if (previous_reg == null) {
                                            ib2.try_catch_all(
                                                    label_for_reg("try_segment", segment_reg, true),
                                                    label_for_reg("try_segment", segment_reg, false)
                                            );
                                        } else {
                                            ib2.try_catch_all(
                                                    label_for_reg("try_segment", segment_reg, true),
                                                    label_for_reg("try_segment", previous_reg, true)
                                            );
                                            ib2.try_catch_all(
                                                    label_for_reg("try_segment", previous_reg, false),
                                                    label_for_reg("try_segment", segment_reg, false)
                                            );
                                            ib2.try_catch_all(
                                                    label_for_reg("catch_segment", previous_reg, true),
                                                    label_for_reg("catch_segment", previous_reg, false)
                                            );
                                        }

                                        ib2.label(label_for_reg("catch_segment", segment_reg, true));

                                        ib2.move_exception(ib2.l(exception_reg));
                                        ib2.invoke_range(STATIC, release_id,
                                                1, ib2.p(segment_reg));
                                        ib2.throw_(ib2.l(exception_reg));

                                        ib2.label(label_for_reg("catch_segment", segment_reg, false));

                                        previous_reg = segment_reg;
                                    }

                                    // At least one element - symbol segment
                                    assert previous_reg != null;

                                    if (has_arena) {
                                        ib2.try_catch_all(
                                                "try_arena_start",
                                                label_for_reg("try_segment", previous_reg, true)
                                        );
                                        ib2.try_catch_all(
                                                label_for_reg("try_segment", previous_reg, false),
                                                "try_arena_end"
                                        );
                                        ib2.try_catch_all(
                                                label_for_reg("catch_segment", previous_reg, true),
                                                label_for_reg("catch_segment", previous_reg, false)
                                        );
                                        ib2.move_exception(ib2.l(exception_reg));
                                        ib2.commit(close_arena);
                                        ib2.throw_(ib2.l(exception_reg));
                                    }
                                })
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(stub_def)));
        Class<?> stub_class = loadClass(dex, stub_name, newEmptyClassLoader());

        if (options.allowsHeapAccess()) {
            // reinterpret cast Object to int
            ClassUtils.forceClassVerified(stub_class);
        }

        var methods = getDeclaredMethods(stub_class);

        Method init_scope = searchMethod(methods, init_scope_name, Object.class);
        nothrows_run(() -> init_scope.invoke(null, native_stub.scope()));

        Method native_function = searchMethod(methods,
                native_stub_name, native_stub_type.parameterArray());
        registerNativeMethod(native_function, native_stub.nativeAddress());

        Method java_function = searchMethod(methods,
                java_stub_name, java_stub_type.parameterArray());
        return unreflect(java_function);
    }

    private static MethodType fixObjectParameters(MethodType stubType) {
        return MethodType.methodType(stubType.returnType(), stubType.parameterList().stream()
                .map(a -> a == Object.class ? int.class : a).toArray(Class[]::new));
    }

    @Override
    protected MethodHandle arrangeDowncall(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        _LLVMStorageDescriptor target_descriptor = _LLVMCallingConvention.computeStorages(descriptor);
        _FunctionDescriptorImpl stub_descriptor = descriptor;
        if (options.isReturnInMemory()) {
            stub_descriptor = stub_descriptor.dropReturnLayout()
                    .insertArgumentLayouts(0, ADDRESS);
        }
        // leading function pointer
        stub_descriptor = stub_descriptor.insertArgumentLayouts(0, WORD);
        MethodType stubType = fdToRawMethodType(stub_descriptor, options.allowsHeapAccess());

        MemorySegment nativeStub = generateNativeDowncallStub(
                Arena.ofAuto(), target_descriptor, stub_descriptor, options);
        return generateJavaDowncallStub(nativeStub, fixObjectParameters(stubType), descriptor, options);
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
        public static _MemorySessionImpl createScope() {
            return _MemorySessionImpl.createConfined(Thread.currentThread());
        }

        public static void closeScope(_MemorySessionImpl scope) {
            scope.close();
        }

        public static void handleException(Throwable th) {
            handleUncaughtException(th);
        }

        public static MemorySegment makeSegment(long addr, long size, long align, _MemorySessionImpl scope) {
            return _Utils.makeSegment(addr, size, align, scope);
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

    private static Method generateJavaUpcallStub(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
        final String method_name = "function";
        final TypeId mh_id = TypeId.of(MethodHandle.class);
        final TypeId ms_id = TypeId.of(MemorySegment.class);
        final TypeId helper_id = TypeId.of(UpcallHelper.class);
        final TypeId scope_id = TypeId.of(_MemorySessionImpl.class);

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

        String stub_name = stubName(descriptor, options, false);
        TypeId stub_id = TypeId.ofName(stub_name);

        MethodId mh_invoke_id = MethodId.of(mh_id, "invokeExact",
                ProtoId.of(TypeId.OBJECT, TypeId.OBJECT.array()));
        MethodId create_scope_id = MethodId.of(helper_id, "createScope", scope_id);
        MethodId close_scope_id = MethodId.of(helper_id, "closeScope", TypeId.V, scope_id);
        MethodId handle_exception_id = MethodId.of(helper_id,
                "handleException", TypeId.V, TypeId.of(Throwable.class));
        MethodId make_segment_id = MethodId.of(helper_id, "makeSegment",
                ms_id, TypeId.J, TypeId.J, TypeId.J, scope_id);

        MethodId put_byte_id = MethodId.of(helper_id, "putByte", TypeId.V, TypeId.J, TypeId.B);
        MethodId put_short_id = MethodId.of(helper_id, "putShort", TypeId.V, TypeId.J, TypeId.S);
        MethodId put_int_id = MethodId.of(helper_id, "putInt", TypeId.V, TypeId.J, TypeId.I);
        MethodId put_float_id = MethodId.of(helper_id, "putFloat", TypeId.V, TypeId.J, TypeId.F);
        MethodId put_long_id = MethodId.of(helper_id, "putLong", TypeId.V, TypeId.J, TypeId.J);
        MethodId put_double_id = MethodId.of(helper_id, "putDouble", TypeId.V, TypeId.J, TypeId.D);
        MethodId put_address_id = MethodId.of(helper_id, "putAddress", TypeId.V, TypeId.J, ms_id);
        MethodId put_segment_id = MethodId.of(helper_id, "putSegment", TypeId.V, TypeId.J, ms_id, TypeId.J);

        int target_ins = target_proto.countInputRegisters();

        var check_exceptions = !options.allowExceptions();
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
                .invoke(STATIC, create_scope_id)
                .move_result_object(ib.l(arena_reg));

        Consumer<CodeBuilder> close_arena = ib -> ib
                .invoke(STATIC, close_scope_id, ib.l(arena_reg));

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
                                .generate_lines()
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

                                // TODO: Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V
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
        Method stub = generateJavaUpcallStub(descriptor, options);

        return (target, arena) -> {
            long target_id = JNIUtils.NewGlobalRef(target, arena);
            long class_id = JNIUtils.NewGlobalRef(stub.getDeclaringClass(), arena);
            long function_id = JNIUtils.FromReflectedMethod(stub);

            return generateNativeUpcallStub(arena, stub_descriptor, options,
                    target_id, class_id, function_id);
        };
    }
}

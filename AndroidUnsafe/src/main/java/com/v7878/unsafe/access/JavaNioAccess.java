package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_CONSTRUCTOR;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.builder.CodeBuilder.InvokeKind.SUPER;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublicApi;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublicApi;
import static com.v7878.unsafe.ArtMethodUtils.makeMethodInheritable;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.ClassUtils.makeClassInheritable;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getHiddenConstructors;
import static com.v7878.unsafe.Reflection.getHiddenInstanceField;
import static com.v7878.unsafe.Reflection.getHiddenInstanceFields;
import static com.v7878.unsafe.Reflection.getHiddenMethods;
import static com.v7878.unsafe.Reflection.getHiddenVirtualMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.MethodId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.access.DirectSegmentByteBuffer.SegmentMemoryRef;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dalvik.system.DexFile;

@ApiSensitive
public class JavaNioAccess {
    // SegmentByteBuffers should not appear in JavaNioAccess as it breaks class loading order
    @DoNotOptimize
    private static class SegmentBufferAccess {
        public static ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, Scope scope) {
            var memref = new SegmentMemoryRef(addr, obj);
            var buffer = new DirectSegmentByteBuffer(memref,
                    -1, 0, cap, cap, 0, false, scope);
            JavaForeignAccess.addOrCleanupIfFail(scope, memref::free);
            return buffer;
        }

        public static ByteBuffer newMappedByteBuffer(UnmapperProxy unmapper, long addr,
                                                     int cap, Object obj, Scope scope) {
            ByteBuffer out = new DirectSegmentByteBuffer(new SegmentMemoryRef(addr, obj),
                    -1, 0, cap, cap, 0, false, scope);
            if (unmapper != null) {
                putObject(out, FD_OFFSET, unmapper.fileDescriptor());
            }
            return out;
        }

        public static ByteBuffer newHeapByteBuffer(byte[] buf, int off, int cap, Scope scope) {
            return new HeapSegmentByteBuffer(buf, -1, 0,
                    cap, cap, off, false, scope);
        }

        public static Scope getBufferScope(Buffer buffer) {
            if (buffer instanceof DirectSegmentByteBuffer) {
                return ((DirectSegmentByteBuffer) buffer).scope;
            }
            if (buffer instanceof HeapSegmentByteBuffer) {
                return ((HeapSegmentByteBuffer) buffer).scope;
            }
            return null;
        }
    }

    public interface UnmapperProxy {
        UnmapperProxy DUMMY = new UnmapperProxy() {
            @Override
            public long address() {
                return 0;
            }

            @Override
            public FileDescriptor fileDescriptor() {
                return null;
            }

            @Override
            public void unmap() {
            }
        };

        long address();

        FileDescriptor fileDescriptor();

        void unmap();
    }

    private static final MethodHandle attachment;

    // TODO: simpify
    static {
        Method m_attachment;

        String nio_direct_buf_name = "java.nio.DirectByteBuffer";
        TypeId nio_direct_buf_id = TypeId.ofName(nio_direct_buf_name);
        String nio_mem_ref_name = "java.nio.DirectByteBuffer$MemoryRef";
        TypeId nio_mem_ref_id = TypeId.ofName(nio_mem_ref_name);
        String nio_heap_buf_name = "java.nio.HeapByteBuffer";
        TypeId nio_heap_buf_id = TypeId.ofName(nio_heap_buf_name);

        Class<?> nio_mem_ref_class = nothrows_run(() -> Class.forName(nio_mem_ref_name));
        {
            makeClassInheritable(nio_mem_ref_class);

            Method[] methods = getHiddenMethods(nio_mem_ref_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            Constructor<?>[] constructors = getHiddenConstructors(nio_mem_ref_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }

            Field[] fields = getHiddenInstanceFields(nio_mem_ref_class);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        Class<?> nio_direct_buf_class = nothrows_run(() -> Class.forName(nio_direct_buf_name));
        {
            Method[] methods = getHiddenMethods(nio_direct_buf_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            m_attachment = searchMethod(methods, "attachment");
            attachment = unreflect(m_attachment);

            Constructor<?>[] constructors = getHiddenConstructors(nio_direct_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }

            Field[] fields = getHiddenInstanceFields(nio_direct_buf_class);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        Class<?> nio_heap_buf_class = nothrows_run(() -> Class.forName(nio_heap_buf_name));
        {
            makeClassInheritable(nio_heap_buf_class);

            Method[] methods = getHiddenMethods(nio_heap_buf_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            Constructor<?>[] constructors = getHiddenConstructors(nio_heap_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }
        }

        {
            // All virtual methods (including those from superclasses)
            {
                Class<?> clazz = MappedByteBuffer.class;
                while (clazz != null && clazz != Object.class) {
                    var methods = getHiddenVirtualMethods(clazz);
                    for (Method method : methods) {
                        int flags = method.getModifiers();
                        if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                            makeMethodInheritable(method);
                        }
                        makeExecutablePublicApi(method);
                    }
                    clazz = clazz.getSuperclass();
                }
            }

            Field[] fields = getHiddenInstanceFields(MappedByteBuffer.class);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        {
            Field[] fields = getHiddenInstanceFields(ByteBuffer.class);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        {
            Field[] fields = getHiddenInstanceFields(Buffer.class);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        String direct_buf_name = "com.v7878.unsafe.DirectByteBuffer";
        TypeId direct_buf_id = TypeId.ofName(direct_buf_name);
        String mem_ref_name = "com.v7878.unsafe.DirectByteBuffer$MemoryRef";
        TypeId mem_ref_id = TypeId.ofName(mem_ref_name);
        String heap_buf_name = "com.v7878.unsafe.HeapByteBuffer";
        TypeId heap_buf_id = TypeId.ofName(heap_buf_name);

        FieldId obo = FieldId.of(mem_ref_id, "originalBufferObject", TypeId.OBJECT);

        ClassDef mem_def = ClassBuilder.build(mem_ref_id, cb -> cb
                .withSuperClass(nio_mem_ref_id)
                .withFlags(ACC_PUBLIC)
                .if_(ART_SDK_INT == 26, cb2 -> cb2
                        // public final Object originalBufferObject;
                        .withField(fb -> fb
                                .of(obo)
                                .withFlags(ACC_PUBLIC | ACC_FINAL)
                        )
                )
                // public MemoryRef($args$) {
                //     super($args$);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_CONSTRUCTOR)
                        .withConstructorSignature()
                        .withParameterTypes(TypeId.J, TypeId.OBJECT)
                        .withCode(0, ib -> ib
                                .if_(ART_SDK_INT == 26,
                                        ib2 -> ib2
                                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id, TypeId.J),
                                                        ib.this_(), ib.p(0), ib.p(1))
                                                .iop(PUT_OBJECT, ib.p(2), ib.this_(), obo),
                                        ib2 -> ib2
                                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id,
                                                                TypeId.J, TypeId.OBJECT),
                                                        ib.this_(), ib.p(0), ib.p(1), ib.p(2))
                                )
                                .return_void()
                        )
                )
        );

        ClassDef direct_buf_def = ClassBuilder.build(direct_buf_id, cb -> cb
                .withSuperClass(nio_direct_buf_id)
                .withFlags(ACC_PUBLIC)
                // public DirectByteBuffer($args$) {
                //     super($args$);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_CONSTRUCTOR)
                        .withConstructorSignature()
                        .withParameterTypes(mem_ref_id, TypeId.I, TypeId.I,
                                TypeId.I, TypeId.I, TypeId.I, TypeId.Z)
                        .withCode(0, ib -> ib
                                .invoke_range(DIRECT, MethodId.constructor(nio_direct_buf_id, nio_mem_ref_id,
                                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                                        8, ib.this_())
                                .return_void()
                        )
                )
                // public MemoryRef attachment() {
                //     return (MemoryRef) super.attachment();
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC)
                        .withName("attachment")
                        .withReturnType(mem_ref_id)
                        .withParameters()
                        .withCode(1, ib -> ib
                                .invoke(SUPER, MethodId.of(m_attachment), ib.this_())
                                .move_result_object(ib.l(0))
                                .check_cast(ib.l(0), mem_ref_id)
                                .return_object(ib.l(0))
                        )
                )
        );

        ClassDef heap_buf_def = ClassBuilder.build(heap_buf_id, cb -> cb
                .withSuperClass(nio_heap_buf_id)
                .withFlags(ACC_PUBLIC)
                // public HeapByteBuffer($args$) {
                //     super($args$);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_CONSTRUCTOR)
                        .withConstructorSignature()
                        .withParameterTypes(TypeId.of(byte[].class), TypeId.I,
                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z)
                        .withCode(0, ib -> ib
                                .invoke_range(DIRECT, MethodId.constructor(nio_heap_buf_id, TypeId.of(byte[].class),
                                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                                        8, ib.this_())
                                .return_void()
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(mem_def, direct_buf_def, heap_buf_def)));
        setTrusted(dex);

        ClassLoader loader = JavaNioAccess.class.getClassLoader();

        loadClass(dex, mem_ref_name, loader);
        loadClass(dex, direct_buf_name, loader);
        loadClass(dex, heap_buf_name, loader);
    }

    public static ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, Scope scope) {
        Objects.requireNonNull(scope);
        return SegmentBufferAccess.newDirectByteBuffer(addr, cap, obj, scope);
    }

    public static ByteBuffer newMappedByteBuffer(UnmapperProxy unmapper, long addr,
                                                 int cap, Object obj, Scope scope) {
        Objects.requireNonNull(scope);
        return SegmentBufferAccess.newMappedByteBuffer(unmapper, addr, cap, obj, scope);
    }

    public static ByteBuffer newHeapByteBuffer(byte[] buffer, int offset, int capacity, Scope scope) {
        Objects.requireNonNull(scope);
        return SegmentBufferAccess.newHeapByteBuffer(buffer, offset, capacity, scope);
    }

    private static long assert_same(long v1, long v2) {
        if (v1 == v2) {
            return v1;
        }
        throw new AssertionError();
    }

    private static final Set<Class<?>> BUFFERS = Set.of(ByteBuffer.class,
            CharBuffer.class, ShortBuffer.class, IntBuffer.class,
            FloatBuffer.class, LongBuffer.class, DoubleBuffer.class);

    private static final Set<Class<?>> BUFFER_VIEWS = Stream
            .of("Char", "Short", "Int", "Float", "Long", "Double")
            .map(name -> String.format("java.nio.ByteBufferAs%sBuffer", name))
            .map(name -> nothrows_run(() -> Class.forName(name)))
            .collect(Collectors.toSet());

    private static boolean isBufferView(Buffer buffer) {
        return BUFFER_VIEWS.contains(buffer.getClass());
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static ByteBuffer getDerivedBuffer(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFER_VIEWS.stream()
                    .mapToLong(clazz -> fieldOffset(getHiddenInstanceField(clazz, "bb")))
                    .reduce(JavaNioAccess::assert_same).getAsLong();
        }
        return (ByteBuffer) getObject(buffer, Holder.OFFSET);
    }

    public static Scope getBufferScope(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (isBufferView(buffer)) {
            return getBufferScope(getDerivedBuffer(buffer));
        }
        return SegmentBufferAccess.getBufferScope(buffer);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static Object getBufferBase(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFERS.stream()
                    .mapToLong(clazz -> fieldOffset(getHiddenInstanceField(clazz, "hb")))
                    .reduce(JavaNioAccess::assert_same).getAsLong();
        }
        return getObject(buffer, Holder.OFFSET);
    }

    public static Object getBufferUnsafeBase(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (buffer.isDirect()) return null;
        buffer = isBufferView(buffer) ? getDerivedBuffer(buffer) : buffer;
        return getBufferBase(buffer);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static long getBufferAddress(Buffer buffer) {
        class Holder {
            static final long OFFSET = fieldOffset(
                    getHiddenInstanceField(Buffer.class, "address"));
        }
        return getLongO(buffer, Holder.OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static int getBufferOffset(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFERS.stream()
                    .mapToLong(clazz -> fieldOffset(getHiddenInstanceField(clazz, "offset")))
                    .reduce(JavaNioAccess::assert_same).getAsLong();
        }
        return getIntO(buffer, Holder.OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static int getBufferDerivedOffset(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFER_VIEWS.stream()
                    .mapToLong(clazz -> fieldOffset(getHiddenInstanceField(clazz,
                            ART_SDK_INT >= 35 ? "byteOffset" : "offset")))
                    .reduce(JavaNioAccess::assert_same).getAsLong();
        }
        return getIntO(buffer, Holder.OFFSET);
    }

    public static int scaleShifts(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (buffer instanceof ByteBuffer) {
            return 0;
        } else if (buffer instanceof CharBuffer || buffer instanceof ShortBuffer) {
            return 1;
        } else if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) {
            return 2;
        } else if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) {
            return 3;
        }
        throw shouldNotReachHere();
    }

    private static long getArrayBase(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return ARRAY_BYTE_BASE_OFFSET;
        } else if (buffer instanceof CharBuffer) {
            return ARRAY_CHAR_BASE_OFFSET;
        } else if (buffer instanceof ShortBuffer) {
            return ARRAY_SHORT_BASE_OFFSET;
        } else if (buffer instanceof IntBuffer) {
            return ARRAY_INT_BASE_OFFSET;
        } else if (buffer instanceof FloatBuffer) {
            return ARRAY_FLOAT_BASE_OFFSET;
        } else if (buffer instanceof LongBuffer) {
            return ARRAY_LONG_BASE_OFFSET;
        } else if (buffer instanceof DoubleBuffer) {
            return ARRAY_DOUBLE_BASE_OFFSET;
        }
        throw shouldNotReachHere();
    }

    public static long getBufferUnsafeOffset(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (buffer.isDirect()) {
            return getBufferAddress(buffer);
        }
        long offset = 0;
        if (isBufferView(buffer)) {
            offset += getBufferDerivedOffset(buffer);
            buffer = getDerivedBuffer(buffer);
        }
        offset += getArrayBase(buffer);
        return offset + getBufferOffset(buffer);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static FileDescriptor getBufferFD(MappedByteBuffer buffer) {
        return (FileDescriptor) getObject(buffer, FD_OFFSET);
    }

    static final long FD_OFFSET = fieldOffset(
            getHiddenInstanceField(MappedByteBuffer.class, "fd"));

    public static UnmapperProxy unmapper(Buffer buffer) {
        if (!(buffer instanceof MappedByteBuffer mapped)) {
            return null;
        }
        assert mapped.isDirect();
        FileDescriptor fd = getBufferFD(mapped);
        return fd == null ? null : new UnmapperProxy() {
            @Override
            public long address() {
                return getBufferAddress(buffer);
            }

            @Override
            public FileDescriptor fileDescriptor() {
                return fd;
            }

            @Override
            public void unmap() {
                //TODO: Unsafe.getUnsafe().invokeCleaner(mapped);
                throw new UnsupportedOperationException("Not supported yet!");
            }
        };
    }

    public static Object attachment(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not direct");
        }
        var tmp = isBufferView(buffer) ? getDerivedBuffer(buffer) : buffer;
        return nothrows_run(() -> attachment.invoke(tmp));
    }

    public static void force(FileDescriptor fd, long address, long offset, long length) {
        MappedMemoryUtils.force(fd, address + offset, length);
    }

    public static void load(long address, long length) {
        MappedMemoryUtils.load(address, length);
    }

    public static void unload(long address, long length) {
        MappedMemoryUtils.unload(address, length);
    }

    public static boolean isLoaded(long address, long length) {
        return MappedMemoryUtils.isLoaded(address, length);
    }
}

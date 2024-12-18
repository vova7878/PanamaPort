package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_CONSTRUCTOR;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.SUPER;
import static com.v7878.dex.bytecode.CodeBuilder.Op.PUT_OBJECT;
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
import static com.v7878.unsafe.Reflection.getDeclaredConstructors;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredFields0;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.getMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
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

    static {
        Method m_attachment;

        String nio_direct_buf_name = "java.nio.DirectByteBuffer";
        TypeId nio_direct_buf_id = TypeId.of(nio_direct_buf_name);
        String nio_mem_ref_name = "java.nio.DirectByteBuffer$MemoryRef";
        TypeId nio_mem_ref_id = TypeId.of(nio_mem_ref_name);
        String nio_heap_buf_name = "java.nio.HeapByteBuffer";
        TypeId nio_heap_buf_id = TypeId.of(nio_heap_buf_name);

        Class<?> nio_mem_ref_class = nothrows_run(() -> Class.forName(nio_mem_ref_name));
        {
            makeClassInheritable(nio_mem_ref_class);

            Method[] methods = getDeclaredMethods(nio_mem_ref_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            Constructor<?>[] constructors = getDeclaredConstructors(nio_mem_ref_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }

            Field[] fields = getDeclaredFields0(nio_mem_ref_class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        Class<?> nio_direct_buf_class = nothrows_run(() -> Class.forName(nio_direct_buf_name));
        {
            Method[] methods = getDeclaredMethods(nio_direct_buf_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            m_attachment = searchMethod(methods, "attachment");
            attachment = unreflect(m_attachment);

            Constructor<?>[] constructors = getDeclaredConstructors(nio_direct_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }

            Field[] fields = getDeclaredFields0(nio_direct_buf_class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        Class<?> nio_heap_buf_class = nothrows_run(() -> Class.forName(nio_heap_buf_name));
        {
            makeClassInheritable(nio_heap_buf_class);

            Method[] methods = getDeclaredMethods(nio_heap_buf_class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            Constructor<?>[] constructors = getDeclaredConstructors(nio_heap_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublic(constructor);
                makeExecutablePublicApi(constructor);
            }
        }

        {
            Method[] methods = getMethods(MappedByteBuffer.class);
            for (Method method : methods) {
                int flags = method.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
                    makeMethodInheritable(method);
                }
                makeExecutablePublicApi(method);
            }

            Field[] fields = getDeclaredFields0(MappedByteBuffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        {
            Field[] fields = getDeclaredFields0(ByteBuffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        {
            Field[] fields = getDeclaredFields0(Buffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
                makeFieldPublicApi(field);
            }
        }

        String direct_buf_name = "com.v7878.unsafe.DirectByteBuffer";
        TypeId direct_buf_id = TypeId.of(direct_buf_name);
        String mem_ref_name = "com.v7878.unsafe.DirectByteBuffer$MemoryRef";
        TypeId mem_ref_id = TypeId.of(mem_ref_name);
        String heap_buf_name = "com.v7878.unsafe.HeapByteBuffer";
        TypeId heap_buf_id = TypeId.of(heap_buf_name);

        ClassDef mem_def = new ClassDef(mem_ref_id);
        mem_def.setSuperClass(nio_mem_ref_id);
        mem_def.setAccessFlags(ACC_PUBLIC);

        FieldId obo = new FieldId(mem_ref_id, TypeId.of(Object.class), "originalBufferObject");
        if (ART_SDK_INT == 26) {
            // public final Object originalBufferObject;
            mem_def.getClassData().getInstanceFields().add(new EncodedField(obo,
                    ACC_PUBLIC | ACC_FINAL, null));
        }


        //public MemoryRef($args$) {
        //    super($args$);
        //}
        mem_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(mem_ref_id, TypeId.J, TypeId.of(Object.class)),
                ACC_PUBLIC | ACC_CONSTRUCTOR
        ).withCode(ART_SDK_INT == 26 ? 0 : 1, b -> b
                .if_(ART_SDK_INT == 26,
                        unused -> b
                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id, TypeId.J),
                                        b.this_(), b.p(0), b.p(1))
                                .iop(PUT_OBJECT, b.p(2), b.this_(), obo),
                        unused -> b
                                .const_4(b.l(0), 0)
                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id,
                                                TypeId.J, TypeId.of(Object.class)),
                                        b.this_(), b.p(0), b.p(1), b.p(2)))
                .return_void()
        ));

        ClassDef direct_buf_def = new ClassDef(direct_buf_id);
        direct_buf_def.setSuperClass(nio_direct_buf_id);
        direct_buf_def.setAccessFlags(ACC_PUBLIC);

        //public DirectByteBuffer($args$) {
        //    super($args$);
        //}
        direct_buf_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(direct_buf_id, mem_ref_id,
                        TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                ACC_PUBLIC | ACC_CONSTRUCTOR).withCode(0, b -> b
                .invoke_range(DIRECT, MethodId.constructor(nio_direct_buf_id, nio_mem_ref_id,
                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                        8, b.this_())
                .return_void()
        ));

        //public MemoryRef attachment() {
        //    return (MemoryRef) super.attachment();
        //}
        direct_buf_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                new MethodId(direct_buf_id, new ProtoId(mem_ref_id), "attachment"),
                ACC_PUBLIC).withCode(1, b -> b
                .invoke(SUPER, MethodId.of(m_attachment), b.this_())
                .move_result_object(b.l(0))
                .check_cast(b.l(0), mem_ref_id)
                .return_object(b.l(0))
        ));

        ClassDef heap_buf_def = new ClassDef(heap_buf_id);
        heap_buf_def.setSuperClass(nio_heap_buf_id);
        heap_buf_def.setAccessFlags(ACC_PUBLIC);

        //public HeapByteBuffer($args$) {
        //    super($args$);
        //}
        heap_buf_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(heap_buf_id, TypeId.of(byte[].class),
                        TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                ACC_PUBLIC | ACC_CONSTRUCTOR).withCode(0, b -> b
                .invoke_range(DIRECT, MethodId.constructor(nio_heap_buf_id, TypeId.of(byte[].class),
                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                        8, b.this_())
                .return_void()
        ));

        DexFile dex = openDexFile(new Dex(mem_def, direct_buf_def, heap_buf_def).compile());
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
                    .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz, "bb")))
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
                    .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz, "hb")))
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
                    getDeclaredField(Buffer.class, "address"));
        }
        return getLongO(buffer, Holder.OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static int getBufferOffset(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFERS.stream()
                    .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz, "offset")))
                    .reduce(JavaNioAccess::assert_same).getAsLong();
        }
        return getIntO(buffer, Holder.OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    private static int getBufferDerivedOffset(Buffer buffer) {
        class Holder {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            static final long OFFSET = BUFFER_VIEWS.stream()
                    .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz,
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

    private static int getArrayBase(Buffer buffer) {
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
            getDeclaredField(MappedByteBuffer.class, "fd"));

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

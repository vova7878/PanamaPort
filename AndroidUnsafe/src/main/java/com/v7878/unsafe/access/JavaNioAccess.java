package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_CONSTRUCTOR;
import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.SUPER;
import static com.v7878.dex.bytecode.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublicNonFinal;
import static com.v7878.unsafe.ClassUtils.makeClassPublicNonFinal;
import static com.v7878.unsafe.DexFileUtils.getDexFile;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredConstructors;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredFields0;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.searchMethod;
import static com.v7878.unsafe.access.JavaNioAccess.FD_OFFSET;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedField;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.FieldId;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.foreign.MemorySegment.Scope;
import com.v7878.unsafe.access.DirectSegmentByteBuffer.SegmentMemoryRef;
import com.v7878.unsafe.access.JavaNioAccess.UnmapperProxy;

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

// SegmentByteBuffers should not appear in JavaNioAccess as it breaks class loading order
class SegmentBufferAccess {
    static {
        setTrusted(getDexFile(DirectSegmentByteBuffer.class));
        setTrusted(getDexFile(HeapSegmentByteBuffer.class));
    }

    public static ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, Scope scope) {
        return new DirectSegmentByteBuffer(new SegmentMemoryRef(addr, obj),
                -1, 0, cap, cap, 0, false, scope);
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

public class JavaNioAccess {

    public interface UnmapperProxy {
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
            makeClassPublicNonFinal(nio_mem_ref_class);

            Constructor<?>[] constructors = getDeclaredConstructors(nio_mem_ref_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublicNonFinal(constructor);
            }

            Field[] fields = getDeclaredFields0(nio_mem_ref_class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
            }
        }

        Class<?> nio_direct_buf_class = nothrows_run(() -> Class.forName(nio_direct_buf_name));
        {
            Method[] methods = getDeclaredMethods(nio_direct_buf_class);
            for (Method method : methods) {
                if (!Modifier.isPrivate(method.getModifiers())) {
                    makeExecutablePublicNonFinal(method);
                }
            }

            m_attachment = searchMethod(methods, "attachment");
            attachment = unreflect(m_attachment);

            Constructor<?>[] constructors = getDeclaredConstructors(nio_direct_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublicNonFinal(constructor);
            }

            Field[] fields = getDeclaredFields0(nio_direct_buf_class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
            }
        }

        Class<?> nio_heap_buf_class = nothrows_run(() -> Class.forName(nio_heap_buf_name));
        {
            makeClassPublicNonFinal(nio_heap_buf_class);

            Method[] methods = getDeclaredMethods(nio_heap_buf_class);
            for (Method method : methods) {
                if (!Modifier.isPrivate(method.getModifiers())) {
                    makeExecutablePublicNonFinal(method);
                }
            }

            Constructor<?>[] constructors = getDeclaredConstructors(nio_heap_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublicNonFinal(constructor);
            }
        }

        {
            Field[] fields = getDeclaredFields0(MappedByteBuffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
            }
        }

        {
            Field[] fields = getDeclaredFields0(ByteBuffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
            }
        }

        {
            Field[] fields = getDeclaredFields0(Buffer.class, false);
            for (Field field : fields) {
                makeFieldPublic(field);
            }

            makeExecutablePublicNonFinal(getDeclaredMethod(Buffer.class, "markValue"));
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
        if (CORRECT_SDK_INT == 26) {
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
        ).withCode(CORRECT_SDK_INT == 26 ? 0 : 1, b -> b
                .if_(CORRECT_SDK_INT == 26,
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

    private static final long BASE_OFFSET = Stream.of(ByteBuffer.class, CharBuffer.class,
            ShortBuffer.class, IntBuffer.class, FloatBuffer.class, LongBuffer.class,
            DoubleBuffer.class).mapToLong(clazz -> fieldOffset(getDeclaredField(clazz,
            "hb"))).reduce(JavaNioAccess::assert_same).getAsLong();

    private static final Set<Class<?>> AS_BUFFERS = Stream
            .of("Char", "Short", "Int", "Float", "Long", "Double")
            .map(name -> String.format("java.nio.ByteBufferAs%sBuffer", name))
            .map(name -> nothrows_run(() -> Class.forName(name)))
            .collect(Collectors.toSet());

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static final long BB_OFFSET = AS_BUFFERS.stream()
            .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz, "bb")))
            .reduce(JavaNioAccess::assert_same).getAsLong();

    public static Object getBufferBase(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (AS_BUFFERS.contains(buffer.getClass())) {
            return getBufferBase((ByteBuffer) getObject(buffer, BB_OFFSET));
        }
        return getObject(buffer, BASE_OFFSET);
    }

    private static final long ADDRESS_OFFSET =
            fieldOffset(getDeclaredField(Buffer.class, "address"));

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // Yes, it is offset to offset from base (ugly name)
    private static final long OFFSET_OFFSET = AS_BUFFERS.stream()
            .mapToLong(clazz -> fieldOffset(getDeclaredField(clazz, "offset")))
            .reduce(JavaNioAccess::assert_same).getAsLong();

    public static long getBufferAddress(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (!buffer.isDirect() && AS_BUFFERS.contains(buffer.getClass())) {
            return getIntO(buffer, OFFSET_OFFSET);
        }
        return getLongO(buffer, ADDRESS_OFFSET);
    }

    public static Scope getBufferScope(Buffer buffer) {
        return SegmentBufferAccess.getBufferScope(buffer);
    }

    static final long FD_OFFSET =
            fieldOffset(getDeclaredField(MappedByteBuffer.class, "fd"));

    public static UnmapperProxy unmapper(Buffer buffer) {
        if (!(buffer instanceof MappedByteBuffer)) {
            return null;
        }
        FileDescriptor fd = (FileDescriptor) getObject(buffer, FD_OFFSET);
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
                //TODO
                throw new UnsupportedOperationException("Not supported yet!");
            }
        };
    }

    public static Object attachment(Buffer buffer) {
        Objects.requireNonNull(buffer);
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not direct");
        }
        var tmp = AS_BUFFERS.contains(buffer.getClass()) ?
                getObject(buffer, BB_OFFSET) : buffer;
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

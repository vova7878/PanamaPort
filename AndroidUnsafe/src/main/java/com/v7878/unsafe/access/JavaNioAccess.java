package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_ABSTRACT;
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
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getHiddenInstanceField;
import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;
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
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.DirectSegmentByteBuffer.SegmentMemoryRef;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
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
    // TODO: fix sun.nio.ch.IOUtil.read/write calls in
    //  sun.nio.ch.FileChannelImpl
    //  sun.nio.ch.SimpleAsynchronousFileChannelImpl
    //  sun.nio.ch.SocketChannelImpl
    //  sun.nio.ch.UnixAsynchronousSocketChannelImpl
    //  sun.nio.ch.DatagramChannelImpl
    //  sun.nio.ch.SinkChannelImpl
    //  sun.nio.ch.SourceChannelImpl

    // SegmentByteBuffers should not appear in JavaNioAccess as it breaks class loading order
    @DoNotOptimize
    private static class Helper {
        public static ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, Scope scope) {
            var memref = new SegmentMemoryRef(addr, obj);
            var buffer = new DirectSegmentByteBuffer(memref, null,
                    -1, 0, cap, cap, 0, false, scope);
            JavaForeignAccess.addOrCleanupIfFail(scope, memref::free);
            return buffer;
        }

        public static ByteBuffer newMappedByteBuffer(UnmapperProxy unmapper, long addr,
                                                     int cap, Object obj, Scope scope) {
            var fd = unmapper.fileDescriptor();
            return new DirectSegmentByteBuffer(new SegmentMemoryRef(addr, obj),
                    fd, -1, 0, cap, cap, 0, false, scope);
        }

        public static ByteBuffer newHeapByteBuffer(byte[] buf, int off, int cap, Scope scope) {
            return new HeapSegmentByteBuffer(buf, -1, 0,
                    cap, cap, off, false, scope);
        }

        public static Scope getBufferScope(Object buffer) {
            if (buffer instanceof DirectSegmentByteBuffer) {
                return ((DirectSegmentByteBuffer) buffer).scope;
            }
            if (buffer instanceof HeapSegmentByteBuffer) {
                return ((HeapSegmentByteBuffer) buffer).scope;
            }
            return null;
        }

        public static Class<?> getFileChannelHook() {
            return AsynchronousFileChannelHook.class;
        }

        public static Class<?> getSocketChannelHook() {
            return AsynchronousSocketChannelHook.class;
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
        Method attachment_method;

        String nio_direct_buf_name = "java.nio.DirectByteBuffer";
        TypeId nio_direct_buf_id = TypeId.ofName(nio_direct_buf_name);
        String nio_mem_ref_name = "java.nio.DirectByteBuffer$MemoryRef";
        TypeId nio_mem_ref_id = TypeId.ofName(nio_mem_ref_name);
        String nio_heap_buf_name = "java.nio.HeapByteBuffer";
        TypeId nio_heap_buf_id = TypeId.ofName(nio_heap_buf_name);

        Class<?> nio_mem_ref_class = ClassUtils.sysClass(nio_mem_ref_name);
        ClassUtils.openClass(nio_mem_ref_class);

        Class<?> nio_direct_buf_class = ClassUtils.sysClass(nio_direct_buf_name);
        ClassUtils.openClass(nio_direct_buf_class);
        attachment_method = getHiddenMethod(nio_direct_buf_class, "attachment");
        attachment = unreflect(attachment_method);

        Class<?> nio_heap_buf_class = ClassUtils.sysClass(nio_heap_buf_name);
        ClassUtils.openClass(nio_heap_buf_class);

        for (Class<?> clazz = MappedByteBuffer.class;
             clazz != null && clazz != Object.class;
             clazz = clazz.getSuperclass()) {
            ClassUtils.openClass(clazz);
        }

        String nio_file_channel_name = "sun.nio.ch.SimpleAsynchronousFileChannelImpl";
        TypeId nio_file_channel_id = TypeId.ofName(nio_file_channel_name);
        String nio_socket_channel_name = "sun.nio.ch.UnixAsynchronousSocketChannelImpl";
        TypeId nio_socket_channel_id = TypeId.ofName(nio_socket_channel_name);

        Class<?> nio_file_channel_class = ClassUtils.sysClass(nio_file_channel_name);
        for (Class<?> clazz = nio_file_channel_class;
             clazz != null && clazz != Object.class;
             clazz = clazz.getSuperclass()) {
            ClassUtils.openClass(clazz);
        }

        Class<?> nio_socket_channel_class = ClassUtils.sysClass(nio_socket_channel_name);
        for (Class<?> clazz = nio_socket_channel_class;
             clazz != null && clazz != Object.class;
             clazz = clazz.getSuperclass()) {
            ClassUtils.openClass(clazz);
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
                                .generate_lines()
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
                .withFlags(ACC_PUBLIC | ACC_ABSTRACT)
                // public DirectByteBuffer($args$) {
                //     super($args$);
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_CONSTRUCTOR)
                        .withConstructorSignature()
                        .withParameterTypes(mem_ref_id, TypeId.I, TypeId.I,
                                TypeId.I, TypeId.I, TypeId.I, TypeId.Z)
                        .withCode(0, ib -> ib
                                .generate_lines()
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
                                .generate_lines()
                                .invoke(SUPER, MethodId.of(attachment_method), ib.this_())
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
                                .generate_lines()
                                .invoke_range(DIRECT, MethodId.constructor(nio_heap_buf_id, TypeId.of(byte[].class),
                                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                                        8, ib.this_())
                                .return_void()
                        )
                )
        );

        String file_channel_name = "com.v7878.unsafe.AsynchronousFileChannelBase";
        TypeId file_channel_id = TypeId.ofName(file_channel_name);
        String socket_channel_name = "com.v7878.unsafe.AsynchronousSocketChannelBase";
        TypeId socket_channel_id = TypeId.ofName(socket_channel_name);

        ClassDef file_channel_def = ClassBuilder.build(file_channel_id, cb -> cb
                .withSuperClass(nio_file_channel_id)
                .withFlags(ACC_PUBLIC)
        );

        ClassDef socket_channel_def = ClassBuilder.build(socket_channel_id, cb -> cb
                .withSuperClass(nio_socket_channel_id)
                .withFlags(ACC_PUBLIC)
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(mem_def, direct_buf_def,
                heap_buf_def, file_channel_def, socket_channel_def)));
        setTrusted(dex);

        ClassLoader loader = JavaNioAccess.class.getClassLoader();

        loadClass(dex, mem_ref_name, loader);
        loadClass(dex, direct_buf_name, loader);
        loadClass(dex, heap_buf_name, loader);

        loadClass(dex, file_channel_name, loader);
        loadClass(dex, socket_channel_name, loader);

        VM.copyTables(Helper.getFileChannelHook(), nio_file_channel_class);
        VM.copyTables(Helper.getSocketChannelHook(), nio_socket_channel_class);
    }

    public static ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, Scope scope) {
        Objects.requireNonNull(scope);
        return Helper.newDirectByteBuffer(addr, cap, obj, scope);
    }

    public static ByteBuffer newMappedByteBuffer(UnmapperProxy unmapper, long addr,
                                                 int cap, Object obj, Scope scope) {
        Objects.requireNonNull(scope);
        return Helper.newMappedByteBuffer(unmapper, addr, cap, obj, scope);
    }

    public static ByteBuffer newHeapByteBuffer(byte[] buffer, int offset, int capacity, Scope scope) {
        Objects.requireNonNull(scope);
        return Helper.newHeapByteBuffer(buffer, offset, capacity, scope);
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
            .map(ClassUtils::sysClass)
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
        return Helper.getBufferScope(buffer);
    }

    public static FineClosable lockScope(ByteBuffer bb, boolean async) {
        var scope = getBufferScope(bb);
        if (async && scope != null && JavaForeignAccess.isThreadConfined(scope)) {
            throw new IllegalArgumentException("Buffer is thread confined");
        }
        return JavaForeignAccess.lock(scope);
    }

    public static void checkAsyncScope(ByteBuffer bb) {
        var scope = getBufferScope(bb);
        if (scope != null && JavaForeignAccess.isThreadConfined(scope)) {
            throw new IllegalArgumentException("Buffer is thread confined");
        }
    }

    public static void checkAsyncScope(ByteBuffer[] buffers) {
        for (var bb : buffers) {
            checkAsyncScope(bb);
        }
    }

    public static FineClosable lockScopes(ByteBuffer[] buffers, boolean async) {
        FineClosable releaser = FineClosable.NOP;
        for (var bb : buffers) {
            releaser = Utils.linkClosables(lockScope(bb, async), releaser);
        }
        return releaser;
    }

    public static FineClosable lockScopes(ByteBuffer buf, ByteBuffer[] buffers, boolean async) {
        if (buffers == null) {
            assert buf != null;

            return lockScope(buf, async);
        } else {
            assert buf == null;

            return lockScopes(buffers, async);
        }
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

    private static final long FD_OFFSET = fieldOffset(
            getHiddenInstanceField(MappedByteBuffer.class, "fd"));

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static FileDescriptor getBufferFD(MappedByteBuffer buffer) {
        return (FileDescriptor) getObject(Objects.requireNonNull(buffer), FD_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void putBufferFD(MappedByteBuffer buffer, FileDescriptor fd) {
        putObject(Objects.requireNonNull(buffer), FD_OFFSET, fd);
    }

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

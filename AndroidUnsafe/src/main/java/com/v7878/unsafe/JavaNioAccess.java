package com.v7878.unsafe;

import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.DIRECT;
import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublicNonFinal;
import static com.v7878.unsafe.ClassUtils.makeClassPublicNonFinal;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getDeclaredConstructors;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.TypeId;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import dalvik.system.DexFile;

// SegmentByteBuffer should not appear in JavaNioAccess as it breaks class loading order
class BufferFactory {
    public static ByteBuffer makeBuf(long address, int cap) {
        return new SegmentByteBuffer(new DirectByteBuffer.MemoryRef(address), -1, 0, cap, cap, 0, false);
    }
}

@SuppressWarnings("deprecation")
public class JavaNioAccess {

    static {
        String nio_direct_buf_name = "java.nio.DirectByteBuffer";
        TypeId nio_direct_buf_id = TypeId.of(nio_direct_buf_name);
        String nio_mem_ref_name = "java.nio.DirectByteBuffer$MemoryRef";
        TypeId nio_mem_ref_id = TypeId.of(nio_mem_ref_name);
        String direct_buf_name = "com.v7878.unsafe.DirectByteBuffer";
        TypeId direct_buf_id = TypeId.of(direct_buf_name);
        String mem_ref_name = "com.v7878.unsafe.DirectByteBuffer$MemoryRef";
        TypeId mem_ref_id = TypeId.of(mem_ref_name);

        ClassDef mem_def = new ClassDef(mem_ref_id);
        mem_def.setSuperClass(nio_mem_ref_id);
        mem_def.setAccessFlags(Modifier.PUBLIC);

        //public MemoryRef($args$) {
        //    super($args$);
        //}
        mem_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(mem_ref_id, TypeId.J),
                Modifier.PUBLIC | /*TODO: CONSTRUCTOR*/ 0x10000
        ).withCode(CORRECT_SDK_INT == 26 ? 0 : 1, b -> b
                .if_(CORRECT_SDK_INT == 26,
                        unused -> b
                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id, TypeId.J),
                                        b.this_(), b.p(0), b.p(1)),
                        unused -> b
                                .const_4(b.l(0), 0)
                                .invoke(DIRECT, MethodId.constructor(nio_mem_ref_id,
                                                TypeId.J, TypeId.of(Object.class)),
                                        b.this_(), b.p(0), b.p(1), b.l(0)))
                .return_void()
        ));

        ClassDef buf_def = new ClassDef(direct_buf_id);
        buf_def.setSuperClass(nio_direct_buf_id);
        buf_def.setAccessFlags(Modifier.PUBLIC);

        //public DirectByteBuffer($args$) {
        //    super($args$);
        //}
        buf_def.getClassData().getDirectMethods().add(new EncodedMethod(
                MethodId.constructor(direct_buf_id, mem_ref_id,
                        TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                Modifier.PUBLIC | /*TODO: CONSTRUCTOR*/ 0x10000
        ).withCode(0, b -> b
                .invoke_range(DIRECT, MethodId.constructor(nio_direct_buf_id, nio_mem_ref_id,
                                TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.I, TypeId.Z),
                        8, b.this_())
                .return_void()
        ));

        Class<?> nio_mem_ref_class = nothrows_run(() -> Class.forName(nio_mem_ref_name));
        {
            makeClassPublicNonFinal(nio_mem_ref_class);

            Constructor<?>[] constructors = getDeclaredConstructors(nio_mem_ref_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublicNonFinal(constructor);
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

            Constructor<?>[] constructors = getDeclaredConstructors(nio_direct_buf_class);
            for (Constructor<?> constructor : constructors) {
                makeExecutablePublicNonFinal(constructor);
            }
        }

        DexFile dex = openDexFile(new Dex(mem_def, buf_def).compile());
        setTrusted(dex);

        ClassLoader loader = JavaNioAccess.class.getClassLoader();

        loadClass(dex, mem_ref_name, loader);
        loadClass(dex, direct_buf_name, loader);
    }

    public static ByteBuffer makeBuf(long address, int cap) {
        return BufferFactory.makeBuf(address, cap);
    }
}

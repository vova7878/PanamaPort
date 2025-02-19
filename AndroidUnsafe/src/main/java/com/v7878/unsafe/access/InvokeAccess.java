package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.dex.DexIO;
import com.v7878.dex.builder.ClassBuilder;
import com.v7878.dex.immutable.ClassDef;
import com.v7878.dex.immutable.Dex;
import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.VM;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

import dalvik.system.DexFile;

public class InvokeAccess {
    private static final AccessI access;

    static {
        TypeId mh = TypeId.of(MethodHandle.class);
        TypeId mt = TypeId.of(MethodType.class);

        String access_name = InvokeAccess.class.getName() + "$Access";
        TypeId access_id = TypeId.ofName(access_name);

        // public final class Access extends AccessI {
        //     <...>
        // }
        ClassDef access_def = ClassBuilder.build(access_id, cb -> cb
                .withSuperClass(TypeId.of(AccessI.class))
                .withFlags(ACC_PUBLIC | ACC_FINAL)
                // public MethodType realType(MethodHandle handle) {
                //     return handle.type;
                // }
                .withMethod(mb -> mb
                        .withFlags(ACC_PUBLIC | ACC_FINAL)
                        .withName("realType")
                        .withReturnType(mt)
                        .withParameterTypes(mh)
                        .withCode(0, ib -> {
                            Field type = getDeclaredField(MethodHandle.class, "type");
                            makeFieldPublic(type);
                            ib.iop(GET_OBJECT, ib.p(0), ib.p(0), FieldId.of(type));
                            ib.return_object(ib.p(0));
                        })
                )
                .if_(ART_SDK_INT <= 32, cb2 -> cb2
                        // public MethodType nominalType(MethodHandle handle) {
                        //     return handle.nominalType;
                        // }
                        .withMethod(mb -> mb
                                .withFlags(ACC_PUBLIC | ACC_FINAL)
                                .withName("nominalType")
                                .withReturnType(mt)
                                .withParameterTypes(mh)
                                .withCode(0, ib -> {
                                    Field type = getDeclaredField(MethodHandle.class, "nominalType");
                                    makeFieldPublic(type);
                                    ib.iop(GET_OBJECT, ib.p(0), ib.p(0), FieldId.of(type));
                                    ib.return_object(ib.p(0));
                                })
                        )
                )
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(access_def)));
        setTrusted(dex);

        ClassLoader loader = InvokeAccess.class.getClassLoader();

        Class<?> invoker_class = loadClass(dex, access_name, loader);
        access = (AccessI) allocateInstance(invoker_class);
    }

    @DoNotShrink
    @DoNotObfuscate
    private abstract static class AccessI {
        abstract MethodType realType(MethodHandle handle);

        abstract MethodType nominalType(MethodHandle handle);
    }

    public static MethodType realType(MethodHandle handle) {
        return access.realType(handle);
    }

    public static MethodType nominalType(MethodHandle handle) {
        if (ART_SDK_INT >= 33) {
            Objects.requireNonNull(handle);
            return null;
        }
        return access.nominalType(handle);
    }

    public static boolean isConvertibleTo(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle isConvertibleTo = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodType.class, "isConvertibleTo", MethodType.class)));
        }
        // TODO: move to access
        return nothrows_run(() -> (boolean) Holder.isConvertibleTo.invokeExact(from, to));
    }

    public static boolean explicitCastEquivalentToAsType(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle explicitCastEquivalentToAsType = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodType.class, "explicitCastEquivalentToAsType", MethodType.class)));
        }
        // TODO: move to access
        return nothrows_run(() -> (boolean) Holder.explicitCastEquivalentToAsType.invokeExact(from, to));
    }

    public static MethodHandle getMethodHandleImpl(MethodHandle target) {
        class Holder {
            static final MethodHandle getMethodHandleImpl = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodHandles.class, "getMethodHandleImpl", MethodHandle.class)));
        }
        // TODO: move to access
        return nothrows_run(() -> (MethodHandle) Holder.getMethodHandleImpl.invoke(target));
    }

    public static Member getMemberInternal(MethodHandle target) {
        class Holder {
            static final Class<?> mhi = nothrows_run(() ->
                    Class.forName("java.lang.invoke.MethodHandleImpl"));
            static final MethodHandle getMemberInternal = nothrows_run(() ->
                    unreflect(getDeclaredMethod(mhi, "getMemberInternal")));
        }
        // TODO: move to access
        return nothrows_run(() -> (Member) Holder.getMemberInternal.invoke(target));
    }

    public static Member getMemberOrNull(MethodHandle target) {
        MethodHandle impl;
        try {
            impl = getMethodHandleImpl(target);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return getMemberInternal(impl);
    }

    public static Class<?>[] exceptionTypes(MethodHandle handle) {
        Member member = getMemberOrNull(handle);
        if (member instanceof Method method) {
            return method.getExceptionTypes();
        }
        if (member instanceof Constructor<?> constructor) {
            return constructor.getExceptionTypes();
        }
        if (member instanceof Field) {
            return new Class[0];
        }
        // unknown
        return null;
    }

    public static Class<?> rtype(MethodType type) {
        return type.returnType();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Class<?>[] ptypes(MethodType type) {
        class Holder {
            static final long PTYPES_OFFSET = fieldOffset(
                    getDeclaredField(MethodType.class, "ptypes"));
        }
        return (Class<?>[]) getObject(Objects.requireNonNull(type), Holder.PTYPES_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static MethodHandle duplicateHandle(MethodHandle handle) {
        Objects.requireNonNull(handle);
        return VM.internalClone(handle);
    }
}

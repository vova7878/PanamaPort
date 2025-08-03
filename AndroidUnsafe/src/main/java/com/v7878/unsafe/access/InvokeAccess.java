package com.v7878.unsafe.access;

import static com.v7878.dex.DexConstants.ACC_FINAL;
import static com.v7878.dex.DexConstants.ACC_PUBLIC;
import static com.v7878.dex.builder.CodeBuilder.Op.GET_OBJECT;
import static com.v7878.dex.builder.CodeBuilder.Op.PUT_OBJECT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldNonFinal;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getHiddenInstanceField;
import static com.v7878.unsafe.Reflection.getHiddenMethod;
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
import com.v7878.unsafe.ClassUtils;
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
    private static final AccessI ACCESS;

    @DoNotShrink
    @DoNotObfuscate
    private abstract static class AccessI {
        abstract MethodType realType(MethodHandle handle);

        abstract void realType(MethodHandle handle, MethodType type);

        abstract MethodType nominalType(MethodHandle handle);

        abstract void nominalType(MethodHandle handle, MethodType type);

        abstract Class<?>[] ptypes(MethodType type);
    }

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
                .commit(cb2 -> {
                    FieldId type_id;
                    {
                        Field type = getHiddenInstanceField(MethodHandle.class, "type");
                        makeFieldPublic(type);
                        makeFieldNonFinal(type);
                        type_id = FieldId.of(type);
                    }
                    // public MethodType realType(MethodHandle handle) {
                    //     return handle.type;
                    // }
                    cb2.withMethod(mb -> mb
                            .withFlags(ACC_PUBLIC | ACC_FINAL)
                            .withName("realType")
                            .withReturnType(mt)
                            .withParameterTypes(mh)
                            .withCode(0, ib -> ib
                                    .generate_lines()
                                    .iop(GET_OBJECT, ib.p(0), ib.p(0), type_id)
                                    .return_object(ib.p(0))
                            )
                    );
                    // public void realType(MethodHandle handle, MethodType type) {
                    //     handle.type = type;
                    // }
                    cb2.withMethod(mb -> mb
                            .withFlags(ACC_PUBLIC | ACC_FINAL)
                            .withName("realType")
                            .withReturnType(TypeId.V)
                            .withParameterTypes(mh, mt)
                            .withCode(0, ib -> ib
                                    .generate_lines()
                                    .iop(PUT_OBJECT, ib.p(1), ib.p(0), type_id)
                                    .return_void()
                            )
                    );
                })
                .if_(ART_SDK_INT <= 32, cb2 -> {
                            FieldId nominal_type_id;
                            {
                                Field nominal_type = getHiddenInstanceField(MethodHandle.class, "nominalType");
                                makeFieldPublic(nominal_type);
                                makeFieldNonFinal(nominal_type);
                                nominal_type_id = FieldId.of(nominal_type);
                            }
                            // public MethodType nominalType(MethodHandle handle) {
                            //     return handle.nominalType;
                            // }
                            cb2.withMethod(mb -> mb
                                    .withFlags(ACC_PUBLIC | ACC_FINAL)
                                    .withName("nominalType")
                                    .withReturnType(mt)
                                    .withParameterTypes(mh)
                                    .withCode(0, ib -> ib
                                            .generate_lines()
                                            .iop(GET_OBJECT, ib.p(0), ib.p(0), nominal_type_id)
                                            .return_object(ib.p(0))
                                    )
                            );
                            // public void nominalType(MethodHandle handle, MethodType type) {
                            //     handle.nominalType = type;
                            // }
                            cb2.withMethod(mb -> mb
                                    .withFlags(ACC_PUBLIC | ACC_FINAL)
                                    .withName("nominalType")
                                    .withReturnType(TypeId.V)
                                    .withParameterTypes(mh, mt)
                                    .withCode(0, ib -> ib
                                            .generate_lines()
                                            .iop(PUT_OBJECT, ib.p(1), ib.p(0), nominal_type_id)
                                            .return_void()
                                    )
                            );
                        }
                )
                .commit(cb2 -> {
                    FieldId ptypes_id;
                    {
                        Field ptypes = getHiddenInstanceField(MethodType.class, "ptypes");
                        makeFieldPublic(ptypes);
                        ptypes_id = FieldId.of(ptypes);
                    }
                    // public Class<?>[] ptypes(MethodType type) {
                    //     return type.ptypes;
                    // }
                    cb2.withMethod(mb -> mb
                            .withFlags(ACC_PUBLIC | ACC_FINAL)
                            .withName("ptypes")
                            .withReturnType(TypeId.of(Class[].class))
                            .withParameterTypes(mt)
                            .withCode(0, ib -> ib
                                    .generate_lines()
                                    .iop(GET_OBJECT, ib.p(0), ib.p(0), ptypes_id)
                                    .return_object(ib.p(0))
                            )
                    );
                })
        );

        DexFile dex = openDexFile(DexIO.write(Dex.of(access_def)));
        setTrusted(dex);

        ClassLoader loader = InvokeAccess.class.getClassLoader();

        Class<?> invoker_class = loadClass(dex, access_name, loader);
        ACCESS = (AccessI) allocateInstance(invoker_class);
    }

    public static MethodType realType(MethodHandle handle) {
        return ACCESS.realType(handle);
    }

    public static MethodType nominalType(MethodHandle handle) {
        if (ART_SDK_INT >= 33) {
            throw new UnsupportedOperationException();
        }
        return ACCESS.nominalType(handle);
    }

    public static void setRealType(MethodHandle handle, MethodType type) {
        ACCESS.realType(handle, type);
    }

    public static void setNominalType(MethodHandle handle, MethodType type) {
        if (ART_SDK_INT >= 33) {
            throw new UnsupportedOperationException();
        }
        ACCESS.nominalType(handle, type);
    }

    public static boolean isConvertibleTo(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle isConvertibleTo = nothrows_run(() -> unreflect(
                    getHiddenMethod(MethodType.class, "isConvertibleTo", MethodType.class)));
        }
        // TODO: move to AccessI
        return nothrows_run(() -> (boolean) Holder.isConvertibleTo.invokeExact(from, to));
    }

    public static boolean explicitCastEquivalentToAsType(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle explicitCastEquivalentToAsType = nothrows_run(() -> unreflect(
                    getHiddenMethod(MethodType.class, "explicitCastEquivalentToAsType", MethodType.class)));
        }
        // TODO: move to AccessI
        return nothrows_run(() -> (boolean) Holder.explicitCastEquivalentToAsType.invokeExact(from, to));
    }

    public static MethodHandle getMethodHandleImpl(MethodHandle target) {
        class Holder {
            static final MethodHandle getMethodHandleImpl = nothrows_run(() -> unreflect(
                    getHiddenMethod(MethodHandles.class, "getMethodHandleImpl", MethodHandle.class)));
        }
        // TODO: move to AccessI
        return nothrows_run(() -> (MethodHandle) Holder.getMethodHandleImpl.invoke(target));
    }

    public static Member getMemberInternal(MethodHandle target) {
        class Holder {
            static final Class<?> mhi = ClassUtils.sysClass("java.lang.invoke.MethodHandleImpl");
            static final MethodHandle getMemberInternal = nothrows_run(() ->
                    unreflect(getHiddenMethod(mhi, "getMemberInternal")));
        }
        // TODO: move to AccessI
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
        return ACCESS.ptypes(type);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static MethodHandle duplicateHandle(MethodHandle handle) {
        Objects.requireNonNull(handle);
        return VM.internalClone(handle);
    }
}

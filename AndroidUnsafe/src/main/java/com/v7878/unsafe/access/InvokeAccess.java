package com.v7878.unsafe.access;

import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.STATIC;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.ApiSensitive;
import com.v7878.unsafe.DangerLevel;
import com.v7878.unsafe.VM;
import com.v7878.unsafe.access.AccessLinker.Conditions;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import com.v7878.unsafe.access.AccessLinker.FieldAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

public class InvokeAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.invoke.MethodHandle", name = "type")
        abstract MethodType realType(MethodHandle handle);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.lang.invoke.MethodHandle", name = "type")
        abstract void realType(MethodHandle handle, MethodType type);

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(art_api = {26, 27, 28, 29, 30, 31, 32}),
                kind = INSTANCE_GETTER, klass = "java.lang.invoke.MethodHandle", name = "nominalType")
        abstract MethodType nominalType(MethodHandle handle);

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(art_api = {26, 27, 28, 29, 30, 31, 32}),
                kind = INSTANCE_SETTER, klass = "java.lang.invoke.MethodHandle", name = "nominalType")
        abstract void nominalType(MethodHandle handle, MethodType type);

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.invoke.MethodType", name = "ptypes")
        abstract Class<?>[] ptypes(MethodType type);

        @ExecutableAccess(kind = VIRTUAL, klass = "java.lang.invoke.MethodType",
                name = "isConvertibleTo", args = {"java.lang.invoke.MethodType"})
        abstract boolean isConvertibleTo(MethodType from, MethodType to);

        @ExecutableAccess(kind = VIRTUAL, klass = "java.lang.invoke.MethodType",
                name = "explicitCastEquivalentToAsType", args = {"java.lang.invoke.MethodType"})
        abstract boolean explicitCastEquivalentToAsType(MethodType from, MethodType to);

        @ExecutableAccess(kind = STATIC, klass = "java.lang.invoke.MethodHandles",
                name = "getMethodHandleImpl", args = {"java.lang.invoke.MethodHandle"})
        abstract MethodHandle getMethodHandleImpl(MethodHandle handle);

        @ExecutableAccess(kind = VIRTUAL, klass = "java.lang.invoke.MethodHandleImpl",
                name = "getMemberInternal", args = {})
        abstract Member getMemberInternal(MethodHandle handle);

        public static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    public static MethodType realType(MethodHandle handle) {
        return AccessI.INSTANCE.realType(handle);
    }

    public static MethodType nominalType(MethodHandle handle) {
        if (ART_SDK_INT >= 33) {
            throw new UnsupportedOperationException();
        }
        return AccessI.INSTANCE.nominalType(handle);
    }

    public static void setRealType(MethodHandle handle, MethodType type) {
        AccessI.INSTANCE.realType(handle, type);
    }

    public static void setNominalType(MethodHandle handle, MethodType type) {
        if (ART_SDK_INT >= 33) {
            throw new UnsupportedOperationException();
        }
        AccessI.INSTANCE.nominalType(handle, type);
    }

    public static boolean isConvertibleTo(MethodType from, MethodType to) {
        return AccessI.INSTANCE.isConvertibleTo(from, to);
    }

    public static boolean explicitCastEquivalentToAsType(MethodType from, MethodType to) {
        return AccessI.INSTANCE.explicitCastEquivalentToAsType(from, to);
    }

    public static MethodHandle getMethodHandleImpl(MethodHandle target) {
        return AccessI.INSTANCE.getMethodHandleImpl(target);
    }

    public static Member getMemberInternal(MethodHandle target) {
        return AccessI.INSTANCE.getMemberInternal(target);
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
        return AccessI.INSTANCE.ptypes(type);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static MethodHandle duplicateHandle(MethodHandle handle) {
        Objects.requireNonNull(handle);
        return VM.internalClone(handle);
    }
}

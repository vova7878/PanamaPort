package com.v7878.unsafe.access;

import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.STATIC;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_SETTER;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.STATIC_GETTER;

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
        @FieldAccess(conditions = @Conditions(max_art = 32),
                kind = INSTANCE_GETTER, klass = "java.lang.invoke.MethodHandle", name = "nominalType")
        abstract MethodType nominalType(MethodHandle handle);

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(max_art = 32),
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

        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.lang.invoke.MethodHandle", name = "handleKind")
        abstract int handleKind(MethodHandle handle);

        @FieldAccess(kind = INSTANCE_SETTER, klass = "java.lang.invoke.MethodHandle", name = "handleKind")
        abstract void handleKind(MethodHandle handle, int kind);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
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

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class HandleKindI {
        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_VIRTUAL")
        abstract int INVOKE_VIRTUAL();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_SUPER")
        abstract int INVOKE_SUPER();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_DIRECT")
        abstract int INVOKE_DIRECT();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_STATIC")
        abstract int INVOKE_STATIC();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_INTERFACE")
        abstract int INVOKE_INTERFACE();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_TRANSFORM")
        abstract int INVOKE_TRANSFORM();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "IGET")
        abstract int IGET();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "IPUT")
        abstract int IPUT();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "SGET")
        abstract int SGET();

        @FieldAccess(kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "SPUT")
        abstract int SPUT();

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(max_art = 32),
                kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_CALLSITE_TRANSFORM")
        abstract int INVOKE_CALLSITE_TRANSFORM();

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(min_art = 28),
                kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_VAR_HANDLE")
        abstract int INVOKE_VAR_HANDLE();

        @ApiSensitive
        @FieldAccess(conditions = @Conditions(min_art = 28),
                kind = STATIC_GETTER, klass = "java.lang.invoke.MethodHandle", name = "INVOKE_VAR_HANDLE_EXACT")
        abstract int INVOKE_VAR_HANDLE_EXACT();

        static final HandleKindI INSTANCE = AccessLinker.generateImpl(HandleKindI.class);
    }

    public static int kindInvokeVirtual() {
        return HandleKindI.INSTANCE.INVOKE_VIRTUAL();
    }

    public static int kindInvokeSuper() {
        return HandleKindI.INSTANCE.INVOKE_SUPER();
    }

    public static int kindInvokeDirect() {
        return HandleKindI.INSTANCE.INVOKE_DIRECT();
    }

    public static int kindInvokeStatic() {
        return HandleKindI.INSTANCE.INVOKE_STATIC();
    }

    public static int kindInvokeInterface() {
        return HandleKindI.INSTANCE.INVOKE_INTERFACE();
    }

    public static int kindInvokeTransform() {
        return HandleKindI.INSTANCE.INVOKE_TRANSFORM();
    }

    public static int kindIGet() {
        return HandleKindI.INSTANCE.IGET();
    }

    public static int kindIPut() {
        return HandleKindI.INSTANCE.IPUT();
    }

    public static int kindSGet() {
        return HandleKindI.INSTANCE.SGET();
    }

    public static int kindSPut() {
        return HandleKindI.INSTANCE.SPUT();
    }

    public static int kindInvokeCallsiteTransform() {
        return HandleKindI.INSTANCE.INVOKE_CALLSITE_TRANSFORM();
    }

    public static int kindInvokeVarHandle() {
        return HandleKindI.INSTANCE.INVOKE_VAR_HANDLE();
    }

    public static int kindInvokeVarHandleExact() {
        return HandleKindI.INSTANCE.INVOKE_VAR_HANDLE_EXACT();
    }

    public static int getMethodHandleKind(MethodHandle handle) {
        return AccessI.INSTANCE.handleKind(handle);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setMethodHandleKind(MethodHandle handle, int kind) {
        AccessI.INSTANCE.handleKind(handle, kind);
    }
}

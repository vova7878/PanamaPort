package com.v7878.unsafe.access;

import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.unsafe.DangerLevel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

public class InvokeAccess {
    public static boolean isConvertibleTo(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle isConvertibleTo = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodType.class, "isConvertibleTo", MethodType.class)));
        }
        return nothrows_run(() -> (boolean) Holder.isConvertibleTo.invokeExact(from, to));
    }

    public static boolean explicitCastEquivalentToAsType(MethodType from, MethodType to) {
        class Holder {
            static final MethodHandle explicitCastEquivalentToAsType = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodType.class, "explicitCastEquivalentToAsType", MethodType.class)));
        }
        return nothrows_run(() -> (boolean) Holder.explicitCastEquivalentToAsType.invokeExact(from, to));
    }

    public static MethodHandle getMethodHandleImpl(MethodHandle target) {
        class Holder {
            static final MethodHandle getMethodHandleImpl = nothrows_run(() -> unreflect(
                    getDeclaredMethod(MethodHandles.class, "getMethodHandleImpl", MethodHandle.class)));
        }
        return nothrows_run(() -> (MethodHandle) Holder.getMethodHandleImpl.invoke(target));
    }

    public static Member getMemberInternal(MethodHandle target) {
        class Holder {
            static final Class<?> mhi = nothrows_run(() ->
                    Class.forName("java.lang.invoke.MethodHandleImpl"));
            static final MethodHandle getMemberInternal = nothrows_run(() ->
                    unreflect(getDeclaredMethod(mhi, "getMemberInternal")));
        }
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
        class Holder {
            static final long RTYPE_OFFSET = fieldOffset(
                    getDeclaredField(MethodType.class, "rtype"));
        }
        return (Class<?>) getObject(Objects.requireNonNull(type), Holder.RTYPE_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static Class<?>[] ptypes(MethodType type) {
        class Holder {
            static final long PTYPES_OFFSET = fieldOffset(
                    getDeclaredField(MethodType.class, "ptypes"));
        }
        return (Class<?>[]) getObject(Objects.requireNonNull(type), Holder.PTYPES_OFFSET);
    }
}

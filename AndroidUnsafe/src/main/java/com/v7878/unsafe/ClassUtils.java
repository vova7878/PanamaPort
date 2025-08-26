package com.v7878.unsafe;

import static com.v7878.dex.DexConstants.ACC_NATIVE;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldNonFinal;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublic;
import static com.v7878.unsafe.ArtFieldUtils.makeFieldPublicApi;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublic;
import static com.v7878.unsafe.ArtMethodUtils.makeExecutablePublicApi;
import static com.v7878.unsafe.ArtMethodUtils.makeMethodInheritable;
import static com.v7878.unsafe.ArtModifiers.kAccSkipAccessChecks;
import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A11;
import static com.v7878.unsafe.ArtVersion.A12;
import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.A16;
import static com.v7878.unsafe.ArtVersion.A16p1;
import static com.v7878.unsafe.ArtVersion.A8p0;
import static com.v7878.unsafe.ArtVersion.A8p1;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Reflection.getHiddenExecutables;
import static com.v7878.unsafe.Reflection.getHiddenFields;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.unsupportedART;

import com.v7878.r8.annotations.DoNotShrink;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public class ClassUtils {
    @ApiSensitive
    public enum ClassStatus {
        NotReady,
        Retired,
        ErrorResolved,
        ErrorUnresolved,
        Idx,
        Loaded,
        Resolving,
        Resolved,
        Verifying,
        RetryVerificationAtRuntime,
        VerifyingAtRuntime,
        VerifiedNeedsAccessChecks,
        Verified,
        SuperclassValidated,
        Initializing,
        Initialized,
        VisiblyInitialized;

        static {
            switch (ART_INDEX) {
                // TODO: Review after android 16 qpr 2 becomes stable
                case A16p1, A16, A15, A14, A13, A12, A11 -> {
                    NotReady.value = 0;  // Zero-initialized Class object starts in this state.
                    Retired.value = 1;  // Retired, should not be used. Use the newly cloned one instead.
                    ErrorResolved.value = 2;
                    ErrorUnresolved.value = 3;
                    Idx.value = 4;  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
                    Loaded.value = 5;  // DEX idx values resolved.
                    Resolving.value = 6;  // Just cloned from temporary class object.
                    Resolved.value = 7;  // Part of linking.
                    Verifying.value = 8;  // In the process of being verified.
                    RetryVerificationAtRuntime.value = 9;  // Compile time verification failed, retry at runtime.
                    VerifiedNeedsAccessChecks.value = 10;  // Compile time verification only failed for access checks.
                    Verified.value = 11;  // Logically part of linking; done pre-init.
                    SuperclassValidated.value = 12;  // Superclass validation part of init done.
                    Initializing.value = 13;  // Class init in progress.
                    Initialized.value = 14;  // Ready to go.
                    VisiblyInitialized.value = 15;  // Initialized and visible to all threads.
                }
                case A10, A9 -> {
                    NotReady.value = 0;  // Zero-initialized Class object starts in this state.
                    Retired.value = 1;  // Retired, should not be used. Use the newly cloned one instead.
                    ErrorResolved.value = 2;
                    ErrorUnresolved.value = 3;
                    Idx.value = 4;  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
                    Loaded.value = 5;  // DEX idx values resolved.
                    Resolving.value = 6;  // Just cloned from temporary class object.
                    Resolved.value = 7;  // Part of linking.
                    Verifying.value = 8;  // In the process of being verified.
                    RetryVerificationAtRuntime.value = 9;  // Compile time verification failed, retry at runtime.
                    VerifyingAtRuntime.value = 10;  // Retrying verification at runtime.
                    Verified.value = 11;  // Logically part of linking; done pre-init.
                    SuperclassValidated.value = 12;  // Superclass validation part of init done.
                    Initializing.value = 13;  // Class init in progress.
                    Initialized.value = 14;  // Ready to go.
                }
                case A8p1 -> {
                    Retired.value = -3;  // Retired, should not be used. Use the newly cloned one instead.
                    ErrorResolved.value = -2;
                    ErrorUnresolved.value = -1;
                    NotReady.value = 0;
                    Idx.value = 1;  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
                    Loaded.value = 2;  // DEX idx values resolved.
                    Resolving.value = 3;  // Just cloned from temporary class object.
                    Resolved.value = 4;  // Part of linking.
                    Verifying.value = 5;  // In the process of being verified.
                    RetryVerificationAtRuntime.value = 6;  // Compile time verification failed, retry at runtime.
                    VerifyingAtRuntime.value = 7;  // Retrying verification at runtime.
                    Verified.value = 8;  // Logically part of linking; done pre-init.
                    SuperclassValidated.value = 9;  // Superclass validation part of init done.
                    Initializing.value = 10;  // Class init in progress.
                    Initialized.value = 11;  // Ready to go.
                }
                case A8p0 -> {
                    Retired.value = -3;  // Retired, should not be used. Use the newly cloned one instead.
                    ErrorResolved.value = -2;
                    ErrorUnresolved.value = -1;
                    NotReady.value = 0;
                    Idx.value = 1;  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
                    Loaded.value = 2;  // DEX idx values resolved.
                    Resolving.value = 3;  // Just cloned from temporary class object.
                    Resolved.value = 4;  // Part of linking.
                    Verifying.value = 5;  // In the process of being verified.
                    RetryVerificationAtRuntime.value = 6;  // Compile time verification failed, retry at runtime.
                    VerifyingAtRuntime.value = 7;  // Retrying verification at runtime.
                    Verified.value = 8;  // Logically part of linking; done pre-init.
                    Initializing.value = 9;  // Class init in progress.
                    Initialized.value = 10;  // Ready to go.
                }
                default -> throw unsupportedART(ART_INDEX);
            }
        }

        private static final int NOT_VALID = Integer.MIN_VALUE;

        private int value = NOT_VALID;

        public int rawValue() {
            if (value == NOT_VALID) {
                throw new IllegalStateException("status " + this + " does not exists");
            }
            return value;
        }

        public boolean isValid() {
            return value != NOT_VALID;
        }

        public static ClassStatus fromRawValue(int value) {
            for (ClassStatus tmp : ClassStatus.values()) {
                if (tmp.isValid() && tmp.value == value) {
                    return tmp;
                }
            }
            throw new IllegalStateException("unknown raw class status: " + value);
        }
    }

    private static class Holder {
        static final long CLASS_STATUS_OFFSET = Reflection.
                instanceFieldOffset(Class.class, "status");
        static final long CLASS_FLAGS_OFFSET = Reflection.
                instanceFieldOffset(Class.class, "accessFlags");
    }

    @ApiSensitive
    public static int getRawClassStatus(Class<?> clazz) {
        int value = AndroidUnsafe.getIntO(Objects.requireNonNull(clazz), Holder.CLASS_STATUS_OFFSET);
        return ART_INDEX <= A8p1 ? value : (value >>> 32 - 4);
    }

    public static ClassStatus getClassStatus(Class<?> clazz) {
        return ClassStatus.fromRawValue(getRawClassStatus(clazz));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @ApiSensitive
    public static void setRawClassStatus(Class<?> clazz, int status) {
        Objects.requireNonNull(clazz);
        if (ART_INDEX > A8p1) {
            int value = AndroidUnsafe.getIntO(clazz, Holder.CLASS_STATUS_OFFSET);
            status = (value & ~0 >>> 4) | (status << 32 - 4);
        }
        AndroidUnsafe.putIntO(clazz, Holder.CLASS_STATUS_OFFSET, status);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassStatus(Class<?> clazz, ClassStatus status) {
        setRawClassStatus(clazz, status.rawValue());
    }

    public static int getClassFlags(Class<?> clazz) {
        return AndroidUnsafe.getIntO(Objects.requireNonNull(clazz), Holder.CLASS_FLAGS_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassFlags(Class<?> clazz, int flags) {
        AndroidUnsafe.putIntO(Objects.requireNonNull(clazz), Holder.CLASS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeClassFlags(Class<?> clazz, int remove_flags, int add_flags) {
        Objects.requireNonNull(clazz);
        int flags = AndroidUnsafe.getIntO(clazz, Holder.CLASS_FLAGS_OFFSET);
        flags &= ~remove_flags;
        flags |= add_flags;
        AndroidUnsafe.putIntO(clazz, Holder.CLASS_FLAGS_OFFSET, flags);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeClassFlags(Class<?> clazz, IntUnaryOperator filter) {
        Objects.requireNonNull(clazz);
        int flags = AndroidUnsafe.getIntO(clazz, Holder.CLASS_FLAGS_OFFSET);
        flags = filter.applyAsInt(flags);
        AndroidUnsafe.putIntO(clazz, Holder.CLASS_FLAGS_OFFSET, flags);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeClassPublic(Class<?> clazz) {
        changeClassFlags(clazz, 0, Modifier.PUBLIC);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeClassInheritable(Class<?> clazz) {
        changeClassFlags(clazz, Modifier.FINAL, Modifier.PUBLIC);
    }

    public static boolean isClassInitialized(Class<?> clazz) {
        return getRawClassStatus(clazz) >= ClassStatus.Initialized.rawValue();
    }

    public static boolean isClassVisiblyInitialized(Class<?> clazz) {
        int value = ART_INDEX <= A10 ?
                ClassStatus.Initialized.rawValue() :
                ClassStatus.VisiblyInitialized.rawValue();
        return getRawClassStatus(clazz) == value;
    }

    public static void ensureClassInitialized(Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static void ensureClassVisiblyInitialized(Class<?> clazz) {
        //TODO: maybe this can be done better?
        ensureClassInitialized(clazz);
        if (!isClassVisiblyInitialized(clazz)) {
            AndroidUnsafe.allocateInstance(clazz);
        }
    }

    public static void forceClassVerified(Class<?> clazz) {
        if (getClassStatus(clazz).ordinal() < ClassStatus.Verified.ordinal()) {
            setClassStatus(clazz, ClassStatus.Verified);

            for (var method : Reflection.getArtMethods(clazz)) {
                ArtMethodUtils.changeExecutableFlags(method, flags -> {
                    if ((flags & ACC_NATIVE) == 0) {
                        return flags | kAccSkipAccessChecks;
                    }
                    return flags;
                });
            }
        }
    }

    public static Class<?> forName(String name, boolean initialize, ClassLoader loader) {
        return nothrows_run(() -> Class.forName(name, initialize, loader));
    }

    public static Class<?> forName(String name, ClassLoader loader) {
        return forName(name, false, loader);
    }

    private static ClassLoader getClassLoader(Class<?> caller) {
        if (caller == null) {
            return null;
        }
        return caller.getClassLoader();
    }

    @DoNotShrink // caller-sensitive
    public static Class<?> forName(String name) {
        return forName(name, false, getClassLoader(Stack.getStackClass1()));
    }

    public static Class<?> sysClass(String name) {
        return forName(name, false, null);
    }

    public static void openClass(Class<?> clazz, boolean unfinal_fields) {
        makeClassInheritable(clazz);

        var executables = getHiddenExecutables(clazz);
        for (var executable : executables) {
            int flags = executable.getModifiers();
            if (executable instanceof Constructor<?> || Modifier.isStatic(flags)) {
                makeExecutablePublic(executable);
            } else if (!Modifier.isPrivate(flags)) {
                makeMethodInheritable((Method) executable);
            } else {
                // private non-static method is direct. Only constructors have the same call type,
                // so you can't change modifiers without changing the call type
            }
            makeExecutablePublicApi(executable);
        }

        var fields = getHiddenFields(clazz);
        for (var field : fields) {
            makeFieldPublic(field);
            makeFieldPublicApi(field);
            if (unfinal_fields) {
                makeFieldNonFinal(field);
            }
        }
    }

    public static void openClass(Class<?> clazz) {
        openClass(clazz, false);
    }
}

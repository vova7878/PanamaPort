package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.ArtVersion.ART_SDK_INT;
import static com.v7878.unsafe.Reflection.fieldOffset;
import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Utils.unsupportedSDK;

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
            switch (ART_SDK_INT) {
                case 36 /*android 16*/, 35 /*android 15*/, 34 /*android 14*/, 33 /*android 13*/,
                     32 /*android 12L*/, 31 /*android 12*/, 30 /*android 11*/ -> {
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
                case 29 /*android 10*/, 28 /*android 9*/ -> {
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
                case 27 /*android 8.1*/ -> {
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
                case 26 /*android 8*/ -> {
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
                default -> throw unsupportedSDK(ART_SDK_INT);
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

    private static final long CLASS_STATUS_OFFSET = fieldOffset(
            getDeclaredField(Class.class, "status"));
    private static final long CLASS_FLAGS_OFFSET = fieldOffset(
            getDeclaredField(Class.class, "accessFlags"));

    @ApiSensitive
    public static int getRawClassStatus(Class<?> clazz) {
        int value = AndroidUnsafe.getIntO(Objects.requireNonNull(clazz), CLASS_STATUS_OFFSET);
        return ART_SDK_INT <= 27 ? value : (value >>> 32 - 4);
    }

    public static ClassStatus getClassStatus(Class<?> clazz) {
        return ClassStatus.fromRawValue(getRawClassStatus(clazz));
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @ApiSensitive
    public static void setRawClassStatus(Class<?> clazz, int status) {
        Objects.requireNonNull(clazz);
        if (ART_SDK_INT > 27) {
            int value = AndroidUnsafe.getIntO(clazz, CLASS_STATUS_OFFSET);
            status = (value & ~0 >>> 4) | (status << 32 - 4);
        }
        AndroidUnsafe.putIntO(clazz, CLASS_STATUS_OFFSET, status);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassStatus(Class<?> clazz, ClassStatus status) {
        setRawClassStatus(clazz, status.rawValue());
    }

    public static int getClassFlags(Class<?> clazz) {
        return AndroidUnsafe.getIntO(Objects.requireNonNull(clazz), CLASS_FLAGS_OFFSET);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassFlags(Class<?> clazz, int flags) {
        AndroidUnsafe.putIntO(Objects.requireNonNull(clazz), CLASS_FLAGS_OFFSET, flags);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeClassFlags(Class<?> clazz, int remove_flags, int add_flags) {
        Objects.requireNonNull(clazz);
        int flags = AndroidUnsafe.getIntO(clazz, CLASS_FLAGS_OFFSET);
        flags &= ~remove_flags;
        flags |= add_flags;
        AndroidUnsafe.putIntO(clazz, CLASS_FLAGS_OFFSET, flags);
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeClassFlags(Class<?> clazz, IntUnaryOperator filter) {
        Objects.requireNonNull(clazz);
        int flags = AndroidUnsafe.getIntO(clazz, CLASS_FLAGS_OFFSET);
        flags = filter.applyAsInt(flags);
        AndroidUnsafe.putIntO(clazz, CLASS_FLAGS_OFFSET, flags);
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
        int value = ART_SDK_INT <= 29 ?
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
}

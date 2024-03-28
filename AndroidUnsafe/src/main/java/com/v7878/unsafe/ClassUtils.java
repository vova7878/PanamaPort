package com.v7878.unsafe;

import static com.v7878.misc.Version.CORRECT_SDK_INT;
import static com.v7878.unsafe.AndroidUnsafe.fullFence;
import static com.v7878.unsafe.Reflection.arrayCast;

import com.v7878.unsafe.Reflection.ClassMirror;

import java.lang.reflect.Modifier;

public class ClassUtils {
    //TODO: add all statuses
    @ApiSensitive
    public enum ClassStatus {
        NotReady,  // Zero-initialized Class object starts in this state.
        Retired,  // Retired, should not be used. Use the newly cloned one instead.
        ErrorResolved,
        ErrorUnresolved,
        Idx,  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
        Loaded,  // DEX idx values resolved.
        Resolving,  // Just cloned from temporary class object.
        Resolved,  // Part of linking.
        Verifying,  // In the process of being verified.
        RetryVerificationAtRuntime,  // Compile time verification failed, retry at runtime.
        Verified,  // Logically part of linking; done pre-init.
        Initializing,  // Class init in progress.
        Initialized;  // Ready to go.

        static {
            switch (CORRECT_SDK_INT) {
                case 35 /*android 15*/, 34 /*android 14*/, 33 /*android 13*/, 32 /*android 12L*/,
                        31 /*android 12*/, 30 /*android 11*/, 29 /*android 10*/, 28  /*android 9*/ -> {
                    NotReady.value = 0;
                    Retired.value = 1;
                    ErrorResolved.value = 2;
                    ErrorUnresolved.value = 3;
                    Idx.value = 4;
                    Loaded.value = 5;
                    Resolving.value = 6;
                    Resolved.value = 7;
                    Verifying.value = 8;
                    RetryVerificationAtRuntime.value = 9;
                    Verified.value = 11;
                    Initializing.value = 13;
                    Initialized.value = 14;
                }
                case 27 /*android 8.1*/ -> {
                    NotReady.value = 0;
                    Retired.value = -3;
                    ErrorResolved.value = -2;
                    ErrorUnresolved.value = -1;
                    Idx.value = 1;
                    Loaded.value = 2;
                    Resolving.value = 3;
                    Resolved.value = 4;
                    Verifying.value = 5;
                    RetryVerificationAtRuntime.value = 6;
                    Verified.value = 8;
                    Initializing.value = 10;
                    Initialized.value = 11;
                }
                case 26 /*android 8*/ -> {
                    NotReady.value = 0;
                    Retired.value = -3;
                    ErrorResolved.value = -2;
                    ErrorUnresolved.value = -1;
                    Idx.value = 1;
                    Loaded.value = 2;
                    Resolving.value = 3;
                    Resolved.value = 4;
                    Verifying.value = 5;
                    RetryVerificationAtRuntime.value = 6;
                    Verified.value = 8;
                    Initializing.value = 9;
                    Initialized.value = 10;
                }
                default -> throw new IllegalStateException("unsupported sdk: " + CORRECT_SDK_INT);
            }
        }

        private int value;

        public int rawValue() {
            return value;
        }
    }

    @ApiSensitive
    public static int getRawClassStatus(Class<?> clazz) {
        ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
        return CORRECT_SDK_INT <= 27 ? mirror[0].status : (mirror[0].status >>> 32 - 4);
    }

    public static ClassStatus getClassStatus(Class<?> clazz) {
        int status = getRawClassStatus(clazz);
        for (ClassStatus tmp : ClassStatus.values()) {
            if (tmp.value == status) {
                return tmp;
            }
        }
        throw new IllegalStateException("unknown raw class status: " + status);
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    @ApiSensitive
    public static void setRawClassStatus(Class<?> clazz, int status) {
        ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
        if (CORRECT_SDK_INT <= 27) {
            mirror[0].status = status;
        } else {
            mirror[0].status = (mirror[0].status & ~0 >>> 4) | (status << 32 - 4);
        }
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassStatus(Class<?> clazz, ClassStatus status) {
        setRawClassStatus(clazz, status.value);
    }

    public static int getClassFlags(Class<?> clazz) {
        ClassMirror[] mirror = arrayCast(ClassMirror.class);
        return mirror[0].accessFlags;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void setClassFlags(Class<?> clazz, int flags) {
        ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
        mirror[0].accessFlags = flags;
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void changeClassFlags(Class<?> clazz, int remove_flags, int add_flags) {
        ClassMirror[] mirror = arrayCast(ClassMirror.class, clazz);
        mirror[0].accessFlags &= ~remove_flags;
        mirror[0].accessFlags |= add_flags;
        fullFence();
    }

    @DangerLevel(DangerLevel.VERY_CAREFUL)
    public static void makeClassPublicNonFinal(Class<?> clazz) {
        changeClassFlags(clazz, Modifier.FINAL | Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE, Modifier.PUBLIC);
    }

    public static void ensureClassInitialized(Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static boolean isClassInitialized(Class<?> clazz) {
        return getRawClassStatus(clazz) >= ClassStatus.Initialized.rawValue();
    }
}

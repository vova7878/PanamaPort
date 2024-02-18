package com.v7878.llvm;

import static com.v7878.llvm._Utils.BOOL_AS_INT;
import static com.v7878.llvm._Utils.VOID_PTR;

import com.v7878.foreign.MemorySegment;

public final class Types {
    private Types() {
    }

    static class AddressValue {
        private final long value;

        AddressValue(long value) {
            this.value = value;
        }

        long value() {
            return value;
        }

        @Override
        public String toString() {
            return getClass().getName() + "{" +
                    "value=" + value +
                    '}';
        }
    }

    static final Class<?> LLVMBool = BOOL_AS_INT;
    static final Class<?> LLVMMemoryBufferRef = VOID_PTR;
    static final Class<?> LLVMContextRef = VOID_PTR;
    static final Class<?> LLVMModuleRef = VOID_PTR;
    static final Class<?> LLVMTypeRef = VOID_PTR;
    static final Class<?> LLVMValueRef = VOID_PTR;
    static final Class<?> LLVMBasicBlockRef = VOID_PTR;
    static final Class<?> LLVMBuilderRef = VOID_PTR;
    static final Class<?> LLVMModuleProviderRef = VOID_PTR;
    static final Class<?> LLVMPassManagerRef = VOID_PTR;
    static final Class<?> LLVMPassRegistryRef = VOID_PTR;
    static final Class<?> LLVMUseRef = VOID_PTR;
    static final Class<?> LLVMAttributeRef = VOID_PTR;
    static final Class<?> LLVMDiagnosticInfoRef = VOID_PTR;

    public static final class LLVMMemoryBufferRef extends AddressValue {

        LLVMMemoryBufferRef(long value) {
            super(value);
        }
    }

    public static final class LLVMContextRef extends AddressValue {
        LLVMContextRef(long value) {
            super(value);
        }
    }

    public static final class LLVMModuleRef extends AddressValue {
        LLVMModuleRef(long value) {
            super(value);
        }
    }

    public static final class LLVMTypeRef extends AddressValue {
        LLVMTypeRef(long value) {
            super(value);
        }
    }

    public static final class LLVMValueRef extends AddressValue {
        LLVMValueRef(long value) {
            super(value);
        }
    }

    public static final class LLVMBasicBlockRef extends AddressValue {
        LLVMBasicBlockRef(long value) {
            super(value);
        }
    }

    public static final class LLVMBuilderRef extends AddressValue {
        LLVMBuilderRef(long value) {
            super(value);
        }
    }

    public static final class LLVMModuleProviderRef extends AddressValue {
        LLVMModuleProviderRef(long value) {
            super(value);
        }
    }

    public static final class LLVMPassManagerRef extends AddressValue {
        LLVMPassManagerRef(long value) {
            super(value);
        }
    }

    public static final class LLVMPassRegistryRef extends AddressValue {
        LLVMPassRegistryRef(long value) {
            super(value);
        }
    }

    public static final class LLVMUseRef extends AddressValue {
        LLVMUseRef(long value) {
            super(value);
        }
    }

    public static final class LLVMAttributeRef extends AddressValue {
        LLVMAttributeRef(long value) {
            super(value);
        }
    }

    public static final class LLVMDiagnosticInfoRef extends AddressValue {

        LLVMDiagnosticInfoRef(long value) {
            super(value);
        }
    }

    // Port-added
    public static final class LLVMString extends AddressValue {

        LLVMString(long value) {
            super(value);
        }

        public String getValue() {
            return MemorySegment.ofAddress(value()).reinterpret(Long.MAX_VALUE).getString(0);
        }
    }
}

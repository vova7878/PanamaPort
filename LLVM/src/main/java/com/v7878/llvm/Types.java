package com.v7878.llvm;

import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.unsafe.foreign.SimpleBulkLinker.BOOL_AS_INT_CLASS;

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

    static final Class<?> LLVMBool = BOOL_AS_INT_CLASS;
    static final Class<?> cLLVMMemoryBufferRef = VOID_PTR;
    static final Class<?> cLLVMContextRef = VOID_PTR;
    static final Class<?> cLLVMModuleRef = VOID_PTR;
    static final Class<?> cLLVMTypeRef = VOID_PTR;
    static final Class<?> cLLVMValueRef = VOID_PTR;
    static final Class<?> cLLVMBasicBlockRef = VOID_PTR;
    static final Class<?> cLLVMBuilderRef = VOID_PTR;
    static final Class<?> cLLVMModuleProviderRef = VOID_PTR;
    static final Class<?> cLLVMPassManagerRef = VOID_PTR;
    static final Class<?> cLLVMPassRegistryRef = VOID_PTR;
    static final Class<?> cLLVMUseRef = VOID_PTR;
    static final Class<?> cLLVMAttributeRef = VOID_PTR;
    static final Class<?> cLLVMDiagnosticInfoRef = VOID_PTR;

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
}

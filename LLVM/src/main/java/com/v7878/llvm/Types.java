package com.v7878.llvm;

import static com.v7878.llvm.Core.LLVMContextDispose;
import static com.v7878.llvm.Core.LLVMDisposeBuilder;
import static com.v7878.llvm.Core.LLVMDisposeMemoryBuffer;
import static com.v7878.llvm.Core.LLVMDisposeModule;
import static com.v7878.llvm.Core.LLVMDisposeModuleProvider;
import static com.v7878.llvm.Core.LLVMDisposePassManager;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.unsafe.Utils.FineClosable;

import java.util.Objects;

public final class Types {
    private Types() {
    }

    static class AddressValue {
        private final long value;

        AddressValue(long value) {
            if (value == 0) {
                throw shouldNotReachHere();
            }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddressValue that = (AddressValue) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), value);
        }
    }

    public static final class LLVMMemoryBufferRef extends AddressValue implements FineClosable {

        private LLVMMemoryBufferRef(long value) {
            super(value);
        }

        public static LLVMMemoryBufferRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMMemoryBufferRef of 0");
            }
            return new LLVMMemoryBufferRef(value);
        }

        public static LLVMMemoryBufferRef ofNullable(long value) {
            return value == 0 ? null : new LLVMMemoryBufferRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeMemoryBuffer(this);
        }
    }

    public static final class LLVMContextRef extends AddressValue implements FineClosable {
        private LLVMContextRef(long value) {
            super(value);
        }

        public static LLVMContextRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMContextRef of 0");
            }
            return new LLVMContextRef(value);
        }

        public static LLVMContextRef ofNullable(long value) {
            return value == 0 ? null : new LLVMContextRef(value);
        }

        @Override
        public void close() {
            LLVMContextDispose(this);
        }
    }

    public static final class LLVMModuleRef extends AddressValue implements FineClosable {
        private LLVMModuleRef(long value) {
            super(value);
        }

        public static LLVMModuleRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMModuleRef of 0");
            }
            return new LLVMModuleRef(value);
        }

        public static LLVMModuleRef ofNullable(long value) {
            return value == 0 ? null : new LLVMModuleRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeModule(this);
        }
    }

    public static final class LLVMTypeRef extends AddressValue {
        private LLVMTypeRef(long value) {
            super(value);
        }

        public static LLVMTypeRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMTypeRef of 0");
            }
            return new LLVMTypeRef(value);
        }

        public static LLVMTypeRef ofNullable(long value) {
            return value == 0 ? null : new LLVMTypeRef(value);
        }
    }

    public static final class LLVMValueRef extends AddressValue {
        private LLVMValueRef(long value) {
            super(value);
        }

        public static LLVMValueRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMValueRef of 0");
            }
            return new LLVMValueRef(value);
        }

        public static LLVMValueRef ofNullable(long value) {
            return value == 0 ? null : new LLVMValueRef(value);
        }
    }

    public static final class LLVMBasicBlockRef extends AddressValue {
        private LLVMBasicBlockRef(long value) {
            super(value);
        }

        public static LLVMBasicBlockRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMBasicBlockRef of 0");
            }
            return new LLVMBasicBlockRef(value);
        }

        public static LLVMBasicBlockRef ofNullable(long value) {
            return value == 0 ? null : new LLVMBasicBlockRef(value);
        }
    }

    public static final class LLVMBuilderRef extends AddressValue implements FineClosable {
        private LLVMBuilderRef(long value) {
            super(value);
        }

        public static LLVMBuilderRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMBuilderRef of 0");
            }
            return new LLVMBuilderRef(value);
        }

        public static LLVMBuilderRef ofNullable(long value) {
            return value == 0 ? null : new LLVMBuilderRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeBuilder(this);
        }
    }

    public static final class LLVMModuleProviderRef extends AddressValue implements FineClosable {
        private LLVMModuleProviderRef(long value) {
            super(value);
        }

        public static LLVMModuleProviderRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMModuleProviderRef of 0");
            }
            return new LLVMModuleProviderRef(value);
        }

        public static LLVMModuleProviderRef ofNullable(long value) {
            return value == 0 ? null : new LLVMModuleProviderRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeModuleProvider(this);
        }
    }

    public static final class LLVMPassManagerRef extends AddressValue implements FineClosable {
        private LLVMPassManagerRef(long value) {
            super(value);
        }

        public static LLVMPassManagerRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMPassManagerRef of 0");
            }
            return new LLVMPassManagerRef(value);
        }

        public static LLVMPassManagerRef ofNullable(long value) {
            return value == 0 ? null : new LLVMPassManagerRef(value);
        }

        @Override
        public void close() {
            LLVMDisposePassManager(this);
        }
    }

    public static final class LLVMPassRegistryRef extends AddressValue {
        private LLVMPassRegistryRef(long value) {
            super(value);
        }

        public static LLVMPassRegistryRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMPassRegistryRef of 0");
            }
            return new LLVMPassRegistryRef(value);
        }

        public static LLVMPassRegistryRef ofNullable(long value) {
            return value == 0 ? null : new LLVMPassRegistryRef(value);
        }
    }

    public static final class LLVMUseRef extends AddressValue {
        private LLVMUseRef(long value) {
            super(value);
        }

        public static LLVMUseRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMUseRef of 0");
            }
            return new LLVMUseRef(value);
        }

        public static LLVMUseRef ofNullable(long value) {
            return value == 0 ? null : new LLVMUseRef(value);
        }
    }

    public static final class LLVMAttributeRef extends AddressValue {
        private LLVMAttributeRef(long value) {
            super(value);
        }

        public static LLVMAttributeRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMAttributeRef of 0");
            }
            return new LLVMAttributeRef(value);
        }

        public static LLVMAttributeRef ofNullable(long value) {
            return value == 0 ? null : new LLVMAttributeRef(value);
        }
    }

    public static final class LLVMDiagnosticInfoRef extends AddressValue {
        private LLVMDiagnosticInfoRef(long value) {
            super(value);
        }

        public static LLVMDiagnosticInfoRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMDiagnosticInfoRef of 0");
            }
            return new LLVMDiagnosticInfoRef(value);
        }

        public static LLVMDiagnosticInfoRef ofNullable(long value) {
            return value == 0 ? null : new LLVMDiagnosticInfoRef(value);
        }
    }
}

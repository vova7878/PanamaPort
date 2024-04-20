package com.v7878.llvm;

import static com.v7878.llvm.Core.LLVMContextDispose;
import static com.v7878.llvm.Core.LLVMDisposeBuilder;
import static com.v7878.llvm.Core.LLVMDisposeMemoryBuffer;
import static com.v7878.llvm.Core.LLVMDisposeModule;
import static com.v7878.llvm.Core.LLVMDisposeModuleProvider;
import static com.v7878.llvm.Core.LLVMDisposePassManager;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import android.annotation.SuppressLint;

import com.v7878.unsafe.Utils.FineClosable;

import java.util.Objects;

/*===-- llvm-c/Support.h - C Interface Types declarations ---------*- C -*-===*\
|*                                                                            *|
|* This file defines types used by the the C interface to LLVM.               *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
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

        static LLVMMemoryBufferRef of(long value) {
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

        static LLVMContextRef of(long value) {
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

        static LLVMModuleRef of(long value) {
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

        static LLVMTypeRef of(long value) {
            return value == 0 ? null : new LLVMTypeRef(value);
        }
    }

    public static final class LLVMValueRef extends AddressValue {
        private LLVMValueRef(long value) {
            super(value);
        }

        static LLVMValueRef of(long value) {
            return value == 0 ? null : new LLVMValueRef(value);
        }
    }

    public static final class LLVMBasicBlockRef extends AddressValue {
        private LLVMBasicBlockRef(long value) {
            super(value);
        }

        static LLVMBasicBlockRef of(long value) {
            return value == 0 ? null : new LLVMBasicBlockRef(value);
        }
    }

    public static final class LLVMBuilderRef extends AddressValue implements FineClosable {
        private LLVMBuilderRef(long value) {
            super(value);
        }

        static LLVMBuilderRef of(long value) {
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

        static LLVMModuleProviderRef of(long value) {
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

        static LLVMPassManagerRef of(long value) {
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

        static LLVMPassRegistryRef of(long value) {
            return value == 0 ? null : new LLVMPassRegistryRef(value);
        }
    }

    public static final class LLVMUseRef extends AddressValue {
        private LLVMUseRef(long value) {
            super(value);
        }

        static LLVMUseRef of(long value) {
            return value == 0 ? null : new LLVMUseRef(value);
        }
    }

    public static final class LLVMAttributeRef extends AddressValue {
        private LLVMAttributeRef(long value) {
            super(value);
        }

        static LLVMAttributeRef of(long value) {
            return value == 0 ? null : new LLVMAttributeRef(value);
        }
    }

    public static final class LLVMDiagnosticInfoRef extends AddressValue {
        private LLVMDiagnosticInfoRef(long value) {
            super(value);
        }

        static LLVMDiagnosticInfoRef of(long value) {
            return value == 0 ? null : new LLVMDiagnosticInfoRef(value);
        }
    }
}

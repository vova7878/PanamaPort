package com.v7878.foreign;

import java.util.Objects;

final class _StorageDescriptor {
    public static sealed class LLVMStorage {
        public final MemoryLayout layout;

        LLVMStorage(MemoryLayout layout) {
            this.layout = layout;
        }
    }

    // void or empty struct
    public static final class NoStorage extends LLVMStorage {
        NoStorage(MemoryLayout layout) {
            super(layout);
        }
    }

    // primitive types
    public static final class RawStorage extends LLVMStorage {
        RawStorage(MemoryLayout layout) {
            super(Objects.requireNonNull(layout));
            assert layout instanceof ValueLayout;
        }
    }

    // struct with 'byval' or 'sret' flags
    public static final class MemoryStorage extends LLVMStorage {
        MemoryStorage(MemoryLayout layout) {
            super(Objects.requireNonNull(layout));
            assert layout instanceof GroupLayout;
        }
    }

    // struct in registers
    public static final class WrapperStorage extends LLVMStorage {
        public final MemoryLayout wrapper;

        WrapperStorage(MemoryLayout layout, MemoryLayout wrapper) {
            super(Objects.requireNonNull(layout));
            assert layout instanceof GroupLayout;
            this.wrapper = Objects.requireNonNull(wrapper);
        }
    }

    private final LLVMStorage ret;
    private final LLVMStorage[] args;

    _StorageDescriptor(LLVMStorage ret, LLVMStorage[] args) {
        this.ret = ret;
        this.args = args;
    }

    public LLVMStorage returnStorage() {
        return ret;
    }

    public LLVMStorage argumentStorage(int index) {
        return args[index];
    }

    public LLVMStorage[] argumentStorages() {
        return args.clone();
    }
}

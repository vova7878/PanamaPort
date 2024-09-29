package com.v7878.unsafe.cpp_std;

import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.InstructionSet.RISCV64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import com.v7878.foreign.Arena;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

public class MemoryOperators {
    private static final int __STDCPP_DEFAULT_NEW_ALIGNMENT__ =
            CURRENT_INSTRUCTION_SET.stdcppDefaultNewAlignment();

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        // operator new[](unsigned int, std::align_val_t, std::nothrow_t const&)
        @LibrarySymbol(conditions = @Conditions(arch = {ARM, X86}),
                name = "_ZnajSt11align_val_tRKSt9nothrow_t")
        // operator new[](unsigned long, std::align_val_t, std::nothrow_t const&)
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64, X86_64, RISCV64}),
                name = "_ZnamSt11align_val_tRKSt9nothrow_t")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract long new_(long count, long alignment);

        // operator new[](unsigned int, std::nothrow_t const&)
        @LibrarySymbol(conditions = @Conditions(arch = {ARM, X86}),
                name = "_ZnajRKSt9nothrow_t")
        // operator new[](unsigned long, std::nothrow_t const&)
        @LibrarySymbol(conditions = @Conditions(arch = {ARM64, X86_64, RISCV64}),
                name = "_ZnamRKSt9nothrow_t")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long new_(long count);

        // operator delete[](void*, std::align_val_t)
        @LibrarySymbol(name = "_ZdaPvSt11align_val_t")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void delete(long ptr, long align);

        // operator delete[](void*)
        @LibrarySymbol(name = "_ZdaPv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void delete(long ptr);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LibCpp.CPP));
    }

    @AlwaysInline
    private static long new0(long count, long alignment) {
        if (alignment > __STDCPP_DEFAULT_NEW_ALIGNMENT__) {
            return Native.INSTANCE.new_(count, alignment);
        }
        return Native.INSTANCE.new_(count);
    }

    public static long new_(long count, long alignment) {
        checkSize(count);
        checkAlignment(alignment);
        if (count == 0) {
            return 0;
        }
        var ptr = new0(count, alignment);
        if (ptr == 0) {
            throw new OutOfMemoryError("Unable to allocate " +
                    count + " bytes with alignment " + alignment);
        }
        return ptr;
    }

    @AlwaysInline
    private static void delete0(long ptr, long alignment) {
        if (alignment > __STDCPP_DEFAULT_NEW_ALIGNMENT__) {
            Native.INSTANCE.delete(ptr, alignment);
        } else {
            Native.INSTANCE.delete(ptr);
        }
    }

    public static void delete(long ptr, long alignment) {
        checkNativeAddress(ptr);
        if (ptr == 0) {
            return;
        }
        delete0(ptr, alignment);
    }

    @AlwaysInline
    private static void checkNativeAddress(long address) {
        if (ADDRESS_SIZE == 4) {
            // Accept both zero and sign extended pointers. A valid
            // pointer will, after the +1 below, either have produced
            // the value 0x0 or 0x1. Masking off the low bit allows
            // for testing against 0.
            if ((((address >> 32) + 1) & ~1) != 0) {
                throw invalidInput("address", address);
            }
        }
    }

    @AlwaysInline
    private static boolean is32BitClean(long value) {
        return value >>> 32 == 0;
    }

    @AlwaysInline
    private static void checkSize(long size) {
        if (ADDRESS_SIZE == 4) {
            // Note: this will also check for negative sizes
            if (!is32BitClean(size)) {
                throw invalidInput("size", size);
            }
        } else if (size < 0) {
            throw invalidInput("size", size);
        }
    }

    @AlwaysInline
    private static void checkAlignment(long alignment) {
        if (alignment <= 0 || (alignment & alignment - 1) != 0L) {
            throw invalidInput("alignment", alignment);
        }
        if (ADDRESS_SIZE == 4) {
            if (!is32BitClean(alignment)) {
                throw invalidInput("alignment", alignment);
            }
        }
    }

    @AlwaysInline
    private static RuntimeException invalidInput(String name, long value) {
        return new IllegalArgumentException(String.format("Invalid %s: %s", name, value));
    }
}

package com.v7878.unsafe.cpp_std;

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

    public static long new_(long count, long alignment) {
        if (alignment > __STDCPP_DEFAULT_NEW_ALIGNMENT__) {
            return Native.INSTANCE.new_(count, alignment);
        }
        return Native.INSTANCE.new_(count);
    }

    public static void delete(long ptr, long alignment) {
        if (alignment > __STDCPP_DEFAULT_NEW_ALIGNMENT__) {
            Native.INSTANCE.delete(ptr, alignment);
        } else {
            Native.INSTANCE.delete(ptr);
        }
    }
}

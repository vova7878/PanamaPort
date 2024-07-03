package com.v7878.unsafe.cpp_std;

import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

public class MemoryOperators {
    private static final int __STDCPP_DEFAULT_NEW_ALIGNMENT__ =
            CURRENT_INSTRUCTION_SET.stdcppDefaultNewAlignment();

    @Keep
    private abstract static class Native {
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "_ZnamSt11align_val_tRKSt9nothrow_t")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD, LONG_AS_WORD})
        // operator new[](unsigned long, std::align_val_t, std::nothrow_t const&)
        abstract long new_(long count, long alignment);

        @LibrarySymbol(name = "_ZnamRKSt9nothrow_t")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        // operator new[](unsigned long, std::nothrow_t const&)
        abstract long new_(long count);

        @LibrarySymbol(name = "_ZdaPvSt11align_val_t")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        // operator delete[](void*, std::align_val_t)
        abstract void delete(long ptr, long align);

        @LibrarySymbol(name = "_ZdaPv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        // operator delete[](void*)
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

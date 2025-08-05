package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm.Core.LLVMCreateMemoryBufferWithSegment;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMMemoryBufferRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.allocString;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.util.function.Consumer;

/*===-- llvm-c/IRReader.h - IR Reader C Interface -----------------*- C -*-===*\
|*                                                                            *|
|* This file defines the C interface to the IR Reader.                        *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class IRReader {
    private IRReader() {
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMParseIRInContext")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMParseIRInContext(long ContextRef, long MemBuf, long OutM, long OutMessage);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, LLVM);
    }

    /**
     * Read LLVM IR from a memory buffer and convert it into an in-memory
     * Module object. Returns 0 on success. Optionally returns a
     * human-readable description of any errors that occurred during
     * parsing IR. OutMessage must be disposed with LLVMDisposeMessage.
     */
    /* package-private */
    static boolean nLLVMParseIRInContext(
            LLVMContextRef ContextRef, LLVMMemoryBufferRef MemBuf,
            Consumer<LLVMModuleRef> OutM, Consumer<String> OutMessage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_OutM = arena.allocate(ADDRESS);
            MemorySegment c_OutMessage = arena.allocate(ADDRESS);
            boolean err = Native.INSTANCE.LLVMParseIRInContext(ContextRef.value(),
                    MemBuf.value(), c_OutM.nativeAddress(), c_OutMessage.nativeAddress());
            if (!err) {
                OutM.accept(LLVMModuleRef.of(c_OutM.get(ADDRESS, 0).nativeAddress()));
            } else {
                OutMessage.accept(addressToLLVMString(c_OutMessage.get(ADDRESS, 0).nativeAddress()));
            }
            return err;
        }
    }

    /**
     * Read LLVM IR from a memory buffer and convert it into an in-memory Module object.
     */
    // Port-added
    public static LLVMModuleRef LLVMParseIRInContext(
            LLVMContextRef ContextRef, LLVMMemoryBufferRef MemBuf) throws LLVMException {
        String[] err = new String[1];
        LLVMModuleRef[] out = new LLVMModuleRef[1];
        if (nLLVMParseIRInContext(ContextRef, MemBuf, O -> out[0] = O, E -> err[0] = E)) {
            throw new LLVMException(err[0]);
        }
        return out[0];
    }

    /**
     * Read LLVM IR from a string and convert it into an in-memory Module object.
     */
    // Port-added
    public static LLVMModuleRef LLVMParseIRInContext(
            LLVMContextRef ContextRef, String Data) throws LLVMException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c_Data = allocString(arena, Data);
            LLVMMemoryBufferRef MemBuf = LLVMCreateMemoryBufferWithSegment(
                    c_Data, "", true);
            return LLVMParseIRInContext(ContextRef, MemBuf);
        }
    }
}

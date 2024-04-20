package com.v7878.llvm;

import static com.v7878.llvm.Types.LLVMPassRegistryRef;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===-- llvm-c/Initialization.h - Initialization C Interface ------*- C -*-===*\
|*                                                                            *|
|* This header declares the C interface to LLVM initialization routines,      *|
|* which must be called before you can use the functionality provided by      *|
|* the corresponding LLVM library.                                            *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class Initialization {
    private Initialization() {
    }

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMInitializeCore")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeCore(long R);

        @LibrarySymbol(name = "LLVMInitializeTransformUtils")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeTransformUtils(long R);

        @LibrarySymbol(name = "LLVMInitializeScalarOpts")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeScalarOpts(long R);

        @LibrarySymbol(name = "LLVMInitializeObjCARCOpts")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeObjCARCOpts(long R);

        @LibrarySymbol(name = "LLVMInitializeVectorization")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeVectorization(long R);

        @LibrarySymbol(name = "LLVMInitializeInstCombine")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeInstCombine(long R);

        @LibrarySymbol(name = "LLVMInitializeIPO")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeIPO(long R);

        @LibrarySymbol(name = "LLVMInitializeInstrumentation")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeInstrumentation(long R);

        @LibrarySymbol(name = "LLVMInitializeAnalysis")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeAnalysis(long R);

        @LibrarySymbol(name = "LLVMInitializeIPA")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeIPA(long R);

        @LibrarySymbol(name = "LLVMInitializeCodeGen")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeCodeGen(long R);

        @LibrarySymbol(name = "LLVMInitializeTarget")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInitializeTarget(long R);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    /*
     * @defgroup LLVMCInitialization Initialization Routines
     * @ingroup LLVMC
     *
     * This module contains routines used to initialize the LLVM system.
     */

    public static void LLVMInitializeCore(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeCore(R.value());
    }

    public static void LLVMInitializeTransformUtils(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeTransformUtils(R.value());
    }

    public static void LLVMInitializeScalarOpts(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeScalarOpts(R.value());
    }

    public static void LLVMInitializeObjCARCOpts(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeObjCARCOpts(R.value());
    }

    public static void LLVMInitializeVectorization(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeVectorization(R.value());
    }

    public static void LLVMInitializeInstCombine(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeInstCombine(R.value());
    }

    public static void LLVMInitializeIPO(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeIPO(R.value());
    }

    public static void LLVMInitializeInstrumentation(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeInstrumentation(R.value());
    }

    public static void LLVMInitializeAnalysis(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeAnalysis(R.value());
    }

    public static void LLVMInitializeIPA(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeIPA(R.value());
    }

    public static void LLVMInitializeCodeGen(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeCodeGen(R.value());
    }

    public static void LLVMInitializeTarget(LLVMPassRegistryRef R) {
        Native.INSTANCE.LLVMInitializeTarget(R.value());
    }
}

package com.v7878.llvm;

import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMPassManagerRef;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

/*===-- llvm-c/Transform/PassManagerBuilder.h - PMB C Interface ---*- C -*-===*\
|*                                                                            *|
|* This header declares the C interface to the PassManagerBuilder class.      *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class PassManagerBuilder {
    private PassManagerBuilder() {
    }

    /*
     * @defgroup LLVMCTransformsPassManagerBuilder Pass manager builder
     * @ingroup LLVMCTransforms
     */

    public static final class LLVMPassManagerBuilderRef extends AddressValue implements FineClosable {

        private LLVMPassManagerBuilderRef(long value) {
            super(value);
        }

        static LLVMPassManagerBuilderRef of(long value) {
            return value == 0 ? null : new LLVMPassManagerBuilderRef(value);
        }

        @Override
        public void close() {
            LLVMPassManagerBuilderDispose(this);
        }
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMPassManagerBuilderCreate")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {})
        abstract long LLVMPassManagerBuilderCreate();

        @LibrarySymbol(name = "LLVMPassManagerBuilderDispose")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMPassManagerBuilderDispose(long PMB);

        @LibrarySymbol(name = "LLVMPassManagerBuilderSetOptLevel")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMPassManagerBuilderSetOptLevel(long PMB, int OptLevel);

        @LibrarySymbol(name = "LLVMPassManagerBuilderSetSizeLevel")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMPassManagerBuilderSetSizeLevel(long PMB, int SizeLevel);

        @LibrarySymbol(name = "LLVMPassManagerBuilderSetDisableUnitAtATime")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMPassManagerBuilderSetDisableUnitAtATime(long PMB, boolean Value);

        @LibrarySymbol(name = "LLVMPassManagerBuilderSetDisableUnrollLoops")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMPassManagerBuilderSetDisableUnrollLoops(long PMB, boolean Value);

        @LibrarySymbol(name = "LLVMPassManagerBuilderSetDisableSimplifyLibCalls")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, BOOL_AS_INT})
        abstract void LLVMPassManagerBuilderSetDisableSimplifyLibCalls(long PMB, boolean Value);

        @LibrarySymbol(name = "LLVMPassManagerBuilderUseInlinerWithThreshold")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void LLVMPassManagerBuilderUseInlinerWithThreshold(long PMB, int Threshold);

        @LibrarySymbol(name = "LLVMPassManagerBuilderPopulateFunctionPassManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMPassManagerBuilderPopulateFunctionPassManager(long PMB, long PM);

        @LibrarySymbol(name = "LLVMPassManagerBuilderPopulateModulePassManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMPassManagerBuilderPopulateModulePassManager(long PMB, long PM);

        @LibrarySymbol(name = "LLVMPassManagerBuilderPopulateLTOPassManager")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD, BOOL_AS_INT, BOOL_AS_INT})
        abstract void LLVMPassManagerBuilderPopulateLTOPassManager(long PMB, long PM, boolean Internalize, boolean RunInliner);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, LLVM);
    }

    public static LLVMPassManagerBuilderRef LLVMPassManagerBuilderCreate() {
        return LLVMPassManagerBuilderRef.of(Native.INSTANCE.LLVMPassManagerBuilderCreate());
    }

    public static void LLVMPassManagerBuilderDispose(LLVMPassManagerBuilderRef PMB) {
        Native.INSTANCE.LLVMPassManagerBuilderDispose(PMB.value());
    }

    public static void LLVMPassManagerBuilderSetOptLevel(LLVMPassManagerBuilderRef PMB, int /* unsigned */ OptLevel) {
        Native.INSTANCE.LLVMPassManagerBuilderSetOptLevel(PMB.value(), OptLevel);
    }

    public static void LLVMPassManagerBuilderSetSizeLevel(LLVMPassManagerBuilderRef PMB, int /* unsigned */ SizeLevel) {
        Native.INSTANCE.LLVMPassManagerBuilderSetSizeLevel(PMB.value(), SizeLevel);
    }

    public static void LLVMPassManagerBuilderSetDisableUnitAtATime(LLVMPassManagerBuilderRef PMB, boolean Value) {
        Native.INSTANCE.LLVMPassManagerBuilderSetDisableUnitAtATime(PMB.value(), Value);
    }

    public static void LLVMPassManagerBuilderSetDisableUnrollLoops(LLVMPassManagerBuilderRef PMB, boolean Value) {
        Native.INSTANCE.LLVMPassManagerBuilderSetDisableUnrollLoops(PMB.value(), Value);
    }

    public static void LLVMPassManagerBuilderSetDisableSimplifyLibCalls(LLVMPassManagerBuilderRef PMB, boolean Value) {
        Native.INSTANCE.LLVMPassManagerBuilderSetDisableSimplifyLibCalls(PMB.value(), Value);
    }

    public static void LLVMPassManagerBuilderUseInlinerWithThreshold(LLVMPassManagerBuilderRef PMB, int /* unsigned */ Threshold) {
        Native.INSTANCE.LLVMPassManagerBuilderUseInlinerWithThreshold(PMB.value(), Threshold);
    }

    public static void LLVMPassManagerBuilderPopulateFunctionPassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMPassManagerBuilderPopulateFunctionPassManager(PMB.value(), PM.value());
    }

    public static void LLVMPassManagerBuilderPopulateModulePassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM) {
        Native.INSTANCE.LLVMPassManagerBuilderPopulateModulePassManager(PMB.value(), PM.value());
    }

    public static void LLVMPassManagerBuilderPopulateLTOPassManager(LLVMPassManagerBuilderRef PMB, LLVMPassManagerRef PM, boolean Internalize, boolean RunInliner) {
        Native.INSTANCE.LLVMPassManagerBuilderPopulateLTOPassManager(PMB.value(), PM.value(), Internalize, RunInliner);
    }
}
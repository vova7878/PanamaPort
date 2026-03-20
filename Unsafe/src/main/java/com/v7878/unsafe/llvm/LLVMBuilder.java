package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAddTargetDependentFunctionAttr;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildAnd;
import static com.v7878.llvm.Core.LLVMBuildBr;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
import static com.v7878.llvm.Core.LLVMBuildZExtOrBitCast;
import static com.v7878.llvm.Core.LLVMConstInt;
import static com.v7878.llvm.Core.LLVMConstIntOfArbitraryPrecision;
import static com.v7878.llvm.Core.LLVMConstIntToPtr;
import static com.v7878.llvm.Core.LLVMGetBasicBlockParent;
import static com.v7878.llvm.Core.LLVMGetGlobalParent;
import static com.v7878.llvm.Core.LLVMGetInsertBlock;
import static com.v7878.llvm.Core.LLVMGetModuleContext;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMModuleRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.unsafe.llvm.LLVMTypes.int128_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int16_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int1_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int64_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int8_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.ptr_t;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;

import java.util.function.BiFunction;
import java.util.function.Function;

public class LLVMBuilder {
    public static LLVMModuleRef builderModule(LLVMBuilderRef builder) {
        return LLVMGetGlobalParent(LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder)));
    }

    public static LLVMContextRef moduleContext(LLVMModuleRef module) {
        return LLVMGetModuleContext(module);
    }

    public static LLVMContextRef builderContext(LLVMBuilderRef builder) {
        return LLVMGetModuleContext(builderModule(builder));
    }

    public static LLVMValueRef local_jobj_to_intptr(
            LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        var current_block = LLVMGetInsertBlock(builder);
        var function = LLVMGetBasicBlockParent(current_block);

        var context = builderContext(builder);

        var zeroptr = const_intptr(context, 0);

        var non_zero = LLVMAppendBasicBlock(function, "");
        var continuation = LLVMAppendBasicBlock(function, "");

        var test = LLVMBuildICmp(builder, LLVMIntEQ, base, zeroptr, "");
        LLVMBuildCondBr(builder, test, continuation, non_zero);

        LLVMPositionBuilderAtEnd(builder, non_zero);
        base = LLVMBuildAnd(builder, base, const_intptr(context, 0xfffffffffffffffcL), "");
        base = LLVMBuildIntToPtr(builder, base, ptr_t(int32_t(context)), "");
        base = LLVMBuildLoad(builder, base, "");
        base = LLVMBuildZExtOrBitCast(builder, base, intptr_t(context), "");
        base = LLVMBuildAdd(builder, base, offset, "");
        LLVMBuildBr(builder, continuation);

        LLVMPositionBuilderAtEnd(builder, continuation);
        var out = LLVMBuildPhi(builder, intptr_t(context), "");
        LLVMAddIncoming(out, offset, current_block);
        LLVMAddIncoming(out, base, non_zero);

        return out;
    }

    public static LLVMValueRef local_jobj_to_ptr(
            LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset, LLVMTypeRef type) {
        return LLVMBuildIntToPtr(builder, local_jobj_to_intptr(builder, base, offset), ptr_t(type), "");
    }

    public static void no_frame_pointer_elim(LLVMValueRef function) {
        LLVMAddTargetDependentFunctionAttr(function, "no-frame-pointer-elim", "true");
        LLVMAddTargetDependentFunctionAttr(function, "frame-pointer", "all");
    }

    public static LLVMValueRef const_int128(LLVMContextRef context, long low, long high) {
        return LLVMConstIntOfArbitraryPrecision(int128_t(context), low, high);
    }

    public static LLVMValueRef const_intptr(LLVMContextRef context, long value) {
        return LLVMConstInt(intptr_t(context), value, false);
    }

    public static LLVMValueRef const_int64(LLVMContextRef context, long value) {
        return LLVMConstInt(int64_t(context), value, false);
    }

    public static LLVMValueRef const_int32(LLVMContextRef context, int value) {
        return LLVMConstInt(int32_t(context), value, false);
    }

    public static LLVMValueRef const_int16(LLVMContextRef context, short value) {
        return LLVMConstInt(int16_t(context), value, false);
    }

    public static LLVMValueRef const_int8(LLVMContextRef context, byte value) {
        return LLVMConstInt(int8_t(context), value, false);
    }

    public static LLVMValueRef const_int1(LLVMContextRef context, boolean value) {
        return LLVMConstInt(int1_t(context), value ? 1 : 0, false);
    }

    public static LLVMValueRef const_ptr(LLVMContextRef context, LLVMTypeRef type, long value) {
        return LLVMConstIntToPtr(const_intptr(context, value), ptr_t(type));
    }

    public static LLVMValueRef const_ptr(LLVMContextRef context, LLVMTypeRef type, long value, long offset) {
        return const_ptr(context, type, value + offset);
    }

    public static LLVMValueRef load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value) {
        var ptr = LLVMBuildIntToPtr(builder, value, ptr_t(type), "");
        return LLVMBuildLoad(builder, ptr, "");
    }

    public static LLVMValueRef load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value, LLVMValueRef offset) {
        return load_ptr(builder, type, LLVMBuildAdd(builder, value, offset, ""));
    }

    public static LLVMValueRef load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value, long offset) {
        var context = builderContext(builder);
        return load_ptr(builder, type, value, const_intptr(context, offset));
    }

    public static LLVMValueRef call(LLVMBuilderRef builder, LLVMValueRef target, LLVMValueRef... args) {
        return LLVMBuildCall(builder, target, args, "");
    }

    public static BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> ptr_factory(
            long offset, Function<LLVMContextRef, LLVMTypeRef> type) {
        return (builder, base) -> load_ptr(builder,
                ptr_t(type.apply(builderContext(builder))), base, offset);
    }

    public static Function<LLVMContextRef, LLVMValueRef> fnptr_factory(
            Arena scope, MemorySegment value, Function<LLVMContextRef, LLVMTypeRef> type) {
        return context -> const_ptr(context, type.apply(context), value.nativeAddress());
    }

    public static Function<LLVMContextRef, LLVMValueRef> intptr_factory(MemorySegment value) {
        return context -> const_intptr(context, value.nativeAddress());
    }
}

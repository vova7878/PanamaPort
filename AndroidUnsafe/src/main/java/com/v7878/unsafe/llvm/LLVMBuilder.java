package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildNeg;
import static com.v7878.llvm.Core.LLVMBuildPtrToInt;
import static com.v7878.llvm.Core.LLVMBuildTrunc;
import static com.v7878.llvm.Core.LLVMBuildZExtOrBitCast;
import static com.v7878.llvm.Core.LLVMConstInt;
import static com.v7878.llvm.Core.LLVMConstIntOfArbitraryPrecision;
import static com.v7878.llvm.Core.LLVMConstIntToPtr;
import static com.v7878.llvm.Core.LLVMGetBasicBlockParent;
import static com.v7878.llvm.Core.LLVMGetGlobalParent;
import static com.v7878.llvm.Core.LLVMGetInsertBlock;
import static com.v7878.llvm.Core.LLVMGetModuleContext;
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
import com.v7878.unsafe.Utils;
import com.v7878.unsafe.VM;

import java.util.function.BiFunction;
import java.util.function.Function;

public class LLVMBuilder {
    public static LLVMModuleRef getBuilderModule(LLVMBuilderRef builder) {
        return LLVMGetGlobalParent(LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder)));
    }

    public static LLVMContextRef getModuleContext(LLVMModuleRef module) {
        return LLVMGetModuleContext(module);
    }

    public static LLVMContextRef getBuilderContext(LLVMBuilderRef builder) {
        return LLVMGetModuleContext(getBuilderModule(builder));
    }

    public static LLVMValueRef buildRawObjectToAddress(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        if (VM.isPoisonReferences()) {
            base = LLVMBuildNeg(builder, base, "");
        }
        base = LLVMBuildZExtOrBitCast(builder, base, intptr_t(getBuilderContext(builder)), "");
        return LLVMBuildAdd(builder, base, offset, "");
    }

    public static LLVMValueRef buildRawObjectToPointer(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset, LLVMTypeRef type) {
        return LLVMBuildIntToPtr(builder, buildRawObjectToAddress(builder, base, offset), ptr_t(type), "");
    }

    public static LLVMValueRef buildAddressToRawObject(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        base = LLVMBuildAdd(builder, base, offset, "");
        base = LLVMBuildTrunc(builder, base, int32_t(getBuilderContext(builder)), "");
        if (VM.isPoisonReferences()) {
            base = LLVMBuildNeg(builder, base, "");
        }
        return base;
    }

    public static LLVMValueRef buildPointerToRawObject(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        base = LLVMBuildPtrToInt(builder, base, intptr_t(getBuilderContext(builder)), "");
        return buildAddressToRawObject(builder, base, offset);
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

    public static LLVMValueRef build_const_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value) {
        var context = getBuilderContext(builder);
        var ptr = const_ptr(context, type, value);
        return LLVMBuildLoad(builder, ptr, "");
    }

    public static LLVMValueRef build_const_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value, long offset) {
        return build_const_load_ptr(builder, type, value + offset);
    }

    public static LLVMValueRef build_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value) {
        var ptr = LLVMBuildIntToPtr(builder, value, ptr_t(type), "");
        return LLVMBuildLoad(builder, ptr, "");
    }

    public static LLVMValueRef build_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value, LLVMValueRef offset) {
        return build_load_ptr(builder, type, LLVMBuildAdd(builder, value, offset, ""));
    }

    public static LLVMValueRef build_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, LLVMValueRef value, long offset) {
        var context = getBuilderContext(builder);
        return build_load_ptr(builder, type, value, const_intptr(context, offset));
    }

    public static BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> pointerFactory(
            long offset, Function<LLVMContextRef, LLVMTypeRef> type) {
        return (builder, base) -> build_load_ptr(builder,
                type.apply(getBuilderContext(builder)), base, offset);
    }

    public static Function<LLVMBuilderRef, LLVMValueRef> functionPointerFactory(
            Arena scope, MemorySegment value, Function<LLVMContextRef, LLVMTypeRef> type) {
        //TODO: indirect pointers may be unnecessary for some architectures
        MemorySegment holder = Utils.allocateAddress(scope, value);
        return builder -> build_const_load_ptr(builder,
                type.apply(getBuilderContext(builder)), holder.nativeAddress());
    }

    public static Function<LLVMContextRef, LLVMValueRef> intptrFactory(MemorySegment value) {
        return context -> const_intptr(context, value.nativeAddress());
    }
}

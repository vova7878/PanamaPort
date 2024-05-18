package com.v7878.unsafe;

import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpAnd;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpOr;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXchg;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXor;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildAtomicCmpXchg;
import static com.v7878.llvm.Core.LLVMBuildAtomicRMW;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildExtractValue;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildInBoundsGEP;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMBuildSub;
import static com.v7878.llvm.Core.LLVMConstInt;
import static com.v7878.llvm.Core.LLVMConstNull;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntULT;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetOrdering;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.misc.Math.convEndian;
import static com.v7878.misc.Math.d2l;
import static com.v7878.misc.Math.f2i;
import static com.v7878.misc.Math.i2f;
import static com.v7878.misc.Math.l2d;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_INDEX_SCALE;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BYTE;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT_AS_RAW_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.SHORT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.BulkLinker.Tristate.FALSE;
import static com.v7878.unsafe.llvm.LLVMGlobals.int16_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int1_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int32_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int64_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int8_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.intptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.buildToJvmPointer;
import static com.v7878.unsafe.llvm.LLVMUtils.getFunctionCode;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.ValueLayout;
import com.v7878.llvm.Core.LLVMAtomicRMWBinOp;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.Types.LLVMBasicBlockRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.ASM;
import com.v7878.unsafe.foreign.BulkLinker.ASMGenerator;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.llvm.LLVMGlobals;
import com.v7878.unsafe.llvm.LLVMUtils;
import com.v7878.unsafe.llvm.LLVMUtils.Generator;

import java.util.function.Function;

public class ExtraMemoryAccess {

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        private static byte[] gen(Generator generator, String name) {
            try {
                var buf = LLVMUtils.generateModuleToBuffer(generator);
                try (var of = LLVMCreateObjectFile(buf)) {
                    return getFunctionCode(of, name).toArray(ValueLayout.JAVA_BYTE);
                }
            } catch (LLVMException e) {
                throw shouldNotHappen(e);
            }
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -8, 72, -123, -46, 116, 19, 72, 1, -16, 102, 15, 31, 68, 0, 0, -120, 8, 72, -1, -64, 72, -1, -54, 117, -10, -61})
        @ASMGenerator(method = "gen_memset")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_memset() {
            final String name = "memset";
            return gen((context, module, builder) -> {
                LLVMValueRef one = LLVMConstInt(intptr_t(context), 1, false);
                LLVMValueRef zero = LLVMConstNull(intptr_t(context));

                LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), intptr_t(context), int8_t(context)};
                LLVMTypeRef type = LLVMFunctionType(void_t(context), arg_types, false);
                LLVMValueRef function = LLVMAddFunction(module, name, type);
                LLVMValueRef[] args = LLVMGetParams(function);

                LLVMBasicBlockRef start = LLVMAppendBasicBlock(function, "");
                LLVMBasicBlockRef body = LLVMAppendBasicBlock(function, "");
                LLVMBasicBlockRef end = LLVMAppendBasicBlock(function, "");

                LLVMPositionBuilderAtEnd(builder, start);
                LLVMValueRef pointer = buildToJvmPointer(builder, args[0], args[1], int8_t(context));
                LLVMValueRef length = args[2];
                LLVMValueRef test_zero = LLVMBuildICmp(builder, LLVMIntEQ, length, zero, "");
                LLVMBuildCondBr(builder, test_zero, end, body);

                LLVMPositionBuilderAtEnd(builder, body);
                LLVMValueRef counter = LLVMBuildPhi(builder, intptr_t(context), "");
                LLVMAddIncoming(counter, zero, start);
                LLVMValueRef ptr = LLVMBuildInBoundsGEP(builder, pointer, new LLVMValueRef[]{counter}, "");
                LLVMValueRef value = args[3];
                LLVMValueRef store = LLVMBuildStore(builder, value, ptr);
                LLVMSetAlignment(store, 1);
                LLVMValueRef next_counter = LLVMBuildAdd(builder, counter, one, "");
                LLVMAddIncoming(counter, next_counter, body);
                LLVMValueRef test_end = LLVMBuildICmp(builder, LLVMIntEQ, next_counter, length, "");
                LLVMBuildCondBr(builder, test_end, end, body);

                LLVMPositionBuilderAtEnd(builder, end);
                LLVMBuildRetVoid(builder);
            }, name);
        }

        @SuppressWarnings("SameParameterValue")
        private static void gen_memmove_modify(
                LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder, String name,
                LLVMTypeRef element_type, int align, Function<LLVMValueRef, LLVMValueRef> action) {
            LLVMValueRef one = LLVMConstInt(intptr_t(context), 1, false);
            LLVMValueRef zero = LLVMConstNull(intptr_t(context));

            LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), int32_t(context), intptr_t(context), intptr_t(context)};
            LLVMTypeRef type = LLVMFunctionType(void_t(context), arg_types, false);
            LLVMValueRef function = LLVMAddFunction(module, name, type);
            LLVMValueRef[] args = LLVMGetParams(function);

            LLVMBasicBlockRef start = LLVMAppendBasicBlock(function, "");
            LLVMBasicBlockRef body = LLVMAppendBasicBlock(function, "");
            LLVMBasicBlockRef forward = LLVMAppendBasicBlock(function, "");
            LLVMBasicBlockRef backward = LLVMAppendBasicBlock(function, "");
            LLVMBasicBlockRef end = LLVMAppendBasicBlock(function, "");

            LLVMPositionBuilderAtEnd(builder, start);
            LLVMValueRef length = args[4];
            LLVMValueRef test_zero = LLVMBuildICmp(builder, LLVMIntEQ, length, zero, "");
            LLVMBuildCondBr(builder, test_zero, end, body);

            LLVMPositionBuilderAtEnd(builder, body);
            LLVMValueRef langth_m1 = LLVMBuildSub(builder, length, one, "");
            LLVMValueRef dst = buildToJvmPointer(builder, args[0], args[1], element_type);
            LLVMValueRef src = buildToJvmPointer(builder, args[2], args[3], element_type);
            LLVMValueRef test_order = LLVMBuildICmp(builder, LLVMIntULT, dst, src, "");
            LLVMBuildCondBr(builder, test_order, forward, backward);

            {
                LLVMPositionBuilderAtEnd(builder, forward);
                LLVMValueRef counter = LLVMBuildPhi(builder, intptr_t(context), "");
                LLVMAddIncoming(counter, zero, body);
                LLVMValueRef src_element = LLVMBuildInBoundsGEP(builder, src, new LLVMValueRef[]{counter}, "");
                LLVMValueRef dst_element = LLVMBuildInBoundsGEP(builder, dst, new LLVMValueRef[]{counter}, "");
                LLVMValueRef load = LLVMBuildLoad(builder, src_element, "");
                LLVMSetAlignment(load, align);
                LLVMValueRef value = action.apply(load);
                LLVMValueRef store = LLVMBuildStore(builder, value, dst_element);
                LLVMSetAlignment(store, align);
                LLVMValueRef next_counter = LLVMBuildAdd(builder, counter, one, "");
                LLVMAddIncoming(counter, next_counter, forward);
                LLVMValueRef test_end = LLVMBuildICmp(builder, LLVMIntEQ, next_counter, length, "");
                LLVMBuildCondBr(builder, test_end, end, forward);
            }
            {
                LLVMPositionBuilderAtEnd(builder, backward);
                LLVMValueRef counter = LLVMBuildPhi(builder, intptr_t(context), "");
                LLVMAddIncoming(counter, langth_m1, body);
                LLVMValueRef src_element = LLVMBuildInBoundsGEP(builder, src, new LLVMValueRef[]{counter}, "");
                LLVMValueRef dst_element = LLVMBuildInBoundsGEP(builder, dst, new LLVMValueRef[]{counter}, "");
                LLVMValueRef load = LLVMBuildLoad(builder, src_element, "");
                LLVMSetAlignment(load, align);
                LLVMValueRef value = action.apply(load);
                LLVMValueRef store = LLVMBuildStore(builder, value, dst_element);
                LLVMSetAlignment(store, align);
                LLVMValueRef next_counter = LLVMBuildSub(builder, counter, one, "");
                LLVMAddIncoming(counter, next_counter, backward);
                LLVMValueRef test_end = LLVMBuildICmp(builder, LLVMIntEQ, counter, zero, "");
                LLVMBuildCondBr(builder, test_end, end, backward);
            }

            LLVMPositionBuilderAtEnd(builder, end);
            LLVMBuildRetVoid(builder);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                77, -123, -64, 116, 75, -119, -8, 72, 1, -16, -119, -46, 72, 1, -54, 72, 57, -48, 115, 44, 102, 102, 102, 46, 15, 31, -124,
                0, 0, 0, 0, 0, 15, -74, 10, -120, 8, 72, -1, -64, 72, -1, -62, 73, -1, -56, 117, -16, -21, 30, 102, 102, 102, 102, 102, 46,
                15, 31, -124, 0, 0, 0, 0, 0, 66, 15, -74, 76, 2, -1, 66, -120, 76, 0, -1, 73, -1, -56, 117, -16, -61})
        @ASMGenerator(method = "gen_memmove")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @SuppressWarnings("unused")
        private static byte[] gen_memmove() {
            final String name = "memmove";
            return gen((context, module, builder) -> gen_memmove_modify(context, module, builder, name, int8_t(context), 1, value -> value), name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                77, -123, -64, 116, 80, -119, -8, 72, 1, -16, -119, -46, 72, 1, -54, 72, 57, -48, 115, 44, 102, 102, 102, 46, 15, 31, -124,
                0, 0, 0, 0, 0, 15, -73, 10, 102, -63, -63, 8, 102, -119, 8, 72, -125, -64, 2, 72, -125, -62, 2, 73, -1, -56, 117, -23, -21,
                28, 15, 31, -128, 0, 0, 0, 0, 66, 15, -73, 76, 66, -2, 102, -63, -63, 8, 102, 66, -119, 76, 64, -2, 73, -1, -56, 117, -21, -61})
        @ASMGenerator(method = "gen_memmove_swap_shorts")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_shorts(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_shorts() {
            final String name = "memmove_swap_shorts";
            return gen((context, module, builder) -> {
                LLVMTypeRef[] bswap16_args = {int16_t(context)};
                LLVMTypeRef bswap16_type = LLVMFunctionType(int16_t(context), bswap16_args, false);
                LLVMValueRef bswap16 = LLVMAddFunction(module, "llvm.bswap.i16", bswap16_type);

                gen_memmove_modify(context, module, builder, name, int16_t(context), 1,
                        value -> LLVMBuildCall(builder, bswap16, new LLVMValueRef[]{value}, ""));
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                77, -123, -64, 116, 76, -119, -8, 72, 1, -16, -119, -46, 72, 1, -54, 72, 57, -48, 115, 44, 102, 102, 102, 46, 15, 31, -124,
                0, 0, 0, 0, 0, -117, 10, 15, -55, -119, 8, 72, -125, -64, 4, 72, -125, -62, 4, 73, -1, -56, 117, -19, -21, 28, 102, 102, 46,
                15, 31, -124, 0, 0, 0, 0, 0, 66, -117, 76, -126, -4, 15, -55, 66, -119, 76, -128, -4, 73, -1, -56, 117, -17, -61})
        @ASMGenerator(method = "gen_memmove_swap_ints")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_ints(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_ints() {
            final String name = "memmove_swap_ints";
            return gen((context, module, builder) -> {
                LLVMTypeRef[] bswap32_args = {int32_t(context)};
                LLVMTypeRef bswap32_type = LLVMFunctionType(int32_t(context), bswap32_args, false);
                LLVMValueRef bswap32 = LLVMAddFunction(module, "llvm.bswap.i32", bswap32_type);

                gen_memmove_modify(context, module, builder, name, int32_t(context), 1,
                        value -> LLVMBuildCall(builder, bswap32, new LLVMValueRef[]{value}, ""));
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                77, -123, -64, 116, 77, -119, -8, 72, 1, -16, -119, -46, 72, 1, -54, 72, 57, -48, 115, 44, 102, 102, 102, 46, 15, 31, -124,
                0, 0, 0, 0, 0, 72, -117, 10, 72, 15, -55, 72, -119, 8, 72, -125, -64, 8, 72, -125, -62, 8, 73, -1, -56, 117, -22, -21, 26,
                15, 31, -124, 0, 0, 0, 0, 0, 74, -117, 76, -62, -8, 72, 15, -55, 74, -119, 76, -64, -8, 73, -1, -56, 117, -18, -61})
        @ASMGenerator(method = "gen_memmove_swap_longs")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_longs(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_longs() {
            final String name = "memmove_swap_longs";
            return gen((context, module, builder) -> {
                LLVMTypeRef[] bswap64_args = {int64_t(context)};
                LLVMTypeRef bswap64_type = LLVMFunctionType(int64_t(context), bswap64_args, false);
                LLVMValueRef bswap64 = LLVMAddFunction(module, "llvm.bswap.i64", bswap64_type);

                gen_memmove_modify(context, module, builder, name, int64_t(context), 1,
                        value -> LLVMBuildCall(builder, bswap64, new LLVMValueRef[]{value}, ""));
            }, name);
        }

        private static byte[] gen_load_atomic(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, int alignment) {
            return gen((context, module, builder) -> {
                LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context)};
                LLVMTypeRef var_type = type.apply(context);
                LLVMTypeRef f_type = LLVMFunctionType(var_type, arg_types, false);
                LLVMValueRef function = LLVMAddFunction(module, name, f_type);
                LLVMValueRef[] args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                LLVMValueRef pointer = buildToJvmPointer(builder, args[0], args[1], var_type);
                LLVMValueRef load = LLVMBuildLoad(builder, pointer, "");
                LLVMSetAlignment(load, alignment);
                LLVMSetOrdering(load, LLVMAtomicOrderingSequentiallyConsistent);

                LLVMBuildRet(builder, load);
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -118, 4, 48, -61})
        @ASMGenerator(method = "gen_load_byte_atomic")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_byte_atomic() {
            return gen_load_atomic("load_byte_atomic", LLVMGlobals::int8_t, 1);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -117, 4, 48, -61})
        @ASMGenerator(method = "gen_load_short_atomic")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_short_atomic() {
            return gen_load_atomic("load_short_atomic", LLVMGlobals::int16_t, 2);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -117, 4, 48, -61})
        @ASMGenerator(method = "gen_load_int_atomic")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_int_atomic() {
            return gen_load_atomic("load_int_atomic", LLVMGlobals::int32_t, 4);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -117, 4, 48, -61})
        @ASMGenerator(method = "gen_load_long_atomic")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract long load_long_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_long_atomic() {
            return gen_load_atomic("load_long_atomic", LLVMGlobals::int64_t, 8);
        }

        private static byte[] gen_store_atomic(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, int alignment) {
            return gen((context, module, builder) -> {
                LLVMTypeRef var_type = type.apply(context);
                LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), var_type};
                LLVMTypeRef f_type = LLVMFunctionType(void_t(context), arg_types, false);
                LLVMValueRef function = LLVMAddFunction(module, name, f_type);
                LLVMValueRef[] args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                LLVMValueRef pointer = buildToJvmPointer(builder, args[0], args[1], var_type);
                LLVMValueRef store = LLVMBuildStore(builder, args[2], pointer);
                LLVMSetAlignment(store, alignment);
                LLVMSetOrdering(store, LLVMAtomicOrderingSequentiallyConsistent);

                LLVMBuildRetVoid(builder);
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -122, 20, 48, -61})
        @ASMGenerator(method = "gen_store_byte_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_byte_atomic() {
            return gen_store_atomic("store_byte_atomic", LLVMGlobals::int8_t, 1);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -121, 20, 48, -61})
        @ASMGenerator(method = "gen_store_short_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_short_atomic() {
            return gen_store_atomic("store_short_atomic", LLVMGlobals::int16_t, 2);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -121, 20, 48, -61})
        @ASMGenerator(method = "gen_store_int_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_int_atomic() {
            return gen_store_atomic("store_int_atomic", LLVMGlobals::int32_t, 4);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -121, 20, 48, -61})
        @ASMGenerator(method = "gen_store_long_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract void store_long_atomic(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_long_atomic() {
            return gen_store_atomic("store_long_atomic", LLVMGlobals::int64_t, 8);
        }

        private static byte[] gen_atomic_rmw(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, LLVMAtomicRMWBinOp op) {
            return gen((context, module, builder) -> {
                LLVMTypeRef var_type = type.apply(context);
                LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), var_type};
                LLVMTypeRef f_type = LLVMFunctionType(var_type, arg_types, false);
                LLVMValueRef function = LLVMAddFunction(module, name, f_type);
                LLVMValueRef[] args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                LLVMValueRef pointer = buildToJvmPointer(builder, args[0], args[1], var_type);
                LLVMValueRef rmw = LLVMBuildAtomicRMW(builder, op, pointer, args[2],
                        LLVMAtomicOrderingSequentiallyConsistent, false);

                LLVMBuildRet(builder, rmw);
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -122, 20, 48, -119, -48, -61})
        @ASMGenerator(method = "gen_atomic_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_byte() {
            return gen_atomic_rmw("atomic_exchange_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -121, 20, 48, -119, -48, -61})
        @ASMGenerator(method = "gen_atomic_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_short() {
            return gen_atomic_rmw("atomic_exchange_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -121, 20, 48, -119, -48, -61})
        @ASMGenerator(method = "gen_atomic_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_int() {
            return gen_atomic_rmw("atomic_exchange_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -121, 20, 48, 72, -119, -48, -61})
        @ASMGenerator(method = "gen_atomic_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_long() {
            return gen_atomic_rmw("atomic_exchange_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 32, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_and_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_byte() {
            return gen_atomic_rmw("atomic_fetch_and_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 33, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASMGenerator(method = "gen_atomic_fetch_and_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_short() {
            return gen_atomic_rmw("atomic_fetch_and_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 33, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_and_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_int() {
            return gen_atomic_rmw("atomic_fetch_and_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 33, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASMGenerator(method = "gen_atomic_fetch_add_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_add_long() {
            return gen_atomic_rmw("atomic_fetch_and_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 8, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_or_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_byte() {
            return gen_atomic_rmw("atomic_fetch_or_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 9, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASMGenerator(method = "gen_atomic_fetch_or_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_short() {
            return gen_atomic_rmw("atomic_fetch_or_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 9, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_or_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_int() {
            return gen_atomic_rmw("atomic_fetch_or_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 9, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASMGenerator(method = "gen_atomic_fetch_or_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_long() {
            return gen_atomic_rmw("atomic_fetch_or_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 48, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_xor_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_byte() {
            return gen_atomic_rmw("atomic_fetch_xor_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 49, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASMGenerator(method = "gen_atomic_fetch_xor_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_short() {
            return gen_atomic_rmw("atomic_fetch_xor_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 49, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASMGenerator(method = "gen_atomic_fetch_xor_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_int() {
            return gen_atomic_rmw("atomic_fetch_xor_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 49, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASMGenerator(method = "gen_atomic_fetch_xor_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_xor_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_long() {
            return gen_atomic_rmw("atomic_fetch_xor_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpXor);
        }

        //TODO: weak version?
        private static byte[] gen_atomic_compare_and_exchange(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, boolean ret_value) {
            return gen((context, module, builder) -> {
                LLVMTypeRef var_type = type.apply(context);
                LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), var_type, var_type};
                LLVMTypeRef r_type = ret_value ? var_type : int1_t(context);
                LLVMTypeRef f_type = LLVMFunctionType(r_type, arg_types, false);
                LLVMValueRef function = LLVMAddFunction(module, name, f_type);
                LLVMValueRef[] args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                LLVMValueRef pointer = buildToJvmPointer(builder, args[0], args[1], var_type);
                LLVMValueRef cmpxchg = LLVMBuildAtomicCmpXchg(builder, pointer, args[2],
                        args[3], LLVMAtomicOrderingSequentiallyConsistent,
                        LLVMAtomicOrderingSequentiallyConsistent, false);
                LLVMValueRef ret = LLVMBuildExtractValue(builder, cmpxchg, ret_value ? 0 : 1, "");

                LLVMBuildRet(builder, ret);
            }, name);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -80, 12, 55, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_byte", LLVMGlobals::int8_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, 102, -16, 15, -79, 12, 55, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_short", LLVMGlobals::int16_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -79, 12, 55, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_int", LLVMGlobals::int32_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, 72, -119, -48, -16, 72, 15, -79, 12, 55, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_long", LLVMGlobals::int64_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -80, 12, 55, 15, -108, -64, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_set_byte")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_byte", LLVMGlobals::int8_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, 102, -16, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_set_short")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_short", LLVMGlobals::int16_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_set_int")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_int", LLVMGlobals::int32_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, 72, -119, -48, -16, 72, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASMGenerator(method = "gen_atomic_compare_and_set_long")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG, LONG})
        abstract boolean atomic_compare_and_set_long(Object base, long offset, long expected, long desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_long", LLVMGlobals::int64_t, false);
        }

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class));
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }

        if (ClassUtils.isClassInitialized(LLVMGlobals.class) && Native.INSTANCE != null) {
            Native.INSTANCE.memset(base, offset, bytes, value);
        } else {
            AndroidUnsafe.setMemory(base, offset, bytes, value);
        }
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }

        if (ClassUtils.isClassInitialized(LLVMGlobals.class) && Native.INSTANCE != null) {
            Native.INSTANCE.memmove(destBase, destOffset, srcBase, srcOffset, bytes);
        } else {
            AndroidUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    public static void swapShorts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap_shorts(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapInts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap_ints(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapLongs(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap_longs(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void copySwapMemory(Object srcBase, long srcOffset, Object destBase,
                                      long destOffset, long bytes, long elemSize) {
        if (bytes == 0) {
            return;
        }

        switch ((int) elemSize) {
            case 2 -> swapShorts(srcBase, srcOffset, destBase, destOffset, bytes / 2);
            case 4 -> swapInts(srcBase, srcOffset, destBase, destOffset, bytes / 4);
            case 8 -> swapLongs(srcBase, srcOffset, destBase, destOffset, bytes / 8);
            default -> throw new IllegalArgumentException("Illegal element size: " + elemSize);
        }
    }

    public static byte loadByteAtomic(Object base, long offset) {
        return Native.INSTANCE.load_byte_atomic(base, offset);
    }

    public static short loadShortAtomic(Object base, long offset) {
        return Native.INSTANCE.load_short_atomic(base, offset);
    }

    public static int loadIntAtomic(Object base, long offset) {
        return Native.INSTANCE.load_int_atomic(base, offset);
    }

    public static long loadLongAtomic(Object base, long offset) {
        return Native.INSTANCE.load_long_atomic(base, offset);
    }

    public static void storeByteAtomic(Object base, long offset, byte value) {
        Native.INSTANCE.store_byte_atomic(base, offset, value);
    }

    public static void storeShortAtomic(Object base, long offset, short value) {
        Native.INSTANCE.store_short_atomic(base, offset, value);
    }

    public static void storeIntAtomic(Object base, long offset, int value) {
        Native.INSTANCE.store_int_atomic(base, offset, value);
    }

    public static void storeLongAtomic(Object base, long offset, long value) {
        Native.INSTANCE.store_long_atomic(base, offset, value);
    }

    public static byte atomicExchangeByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_exchange_byte(base, offset, value);
    }

    public static short atomicExchangeShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_exchange_short(base, offset, value);
    }

    public static int atomicExchangeInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_exchange_int(base, offset, value);
    }

    public static long atomicExchangeLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_exchange_long(base, offset, value);
    }

    public static byte atomicFetchAddByteWithCAS(Object base, long offset, byte delta) {
        byte expectedValue;
        do {
            expectedValue = loadByteAtomic(base, offset);
        } while (/* TODO: weak? */!atomicCompareAndSetByte(base, offset,
                expectedValue, (byte) (expectedValue + delta)));
        return expectedValue;
    }

    public static short atomicFetchAddShortWithCAS(Object base, long offset, short delta, boolean swap) {
        short nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadShortAtomic(base, offset);
            expectedValue = convEndian(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetShort(base, offset,
                nativeExpectedValue, convEndian((short) (expectedValue + delta), swap)));
        return expectedValue;
    }

    public static int atomicFetchAddIntWithCAS(Object base, long offset, int delta, boolean swap) {
        int nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadIntAtomic(base, offset);
            expectedValue = convEndian(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetInt(base, offset,
                nativeExpectedValue, convEndian(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static float atomicFetchAddFloatWithCAS(Object base, long offset, float delta, boolean swap) {
        int nativeExpectedValue;
        float expectedValue;
        do {
            nativeExpectedValue = loadIntAtomic(base, offset);
            expectedValue = i2f(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetInt(base, offset,
                nativeExpectedValue, f2i(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static long atomicFetchAddLongWithCAS(Object base, long offset, long delta, boolean swap) {
        long nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadLongAtomic(base, offset);
            expectedValue = convEndian(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetLong(base, offset,
                nativeExpectedValue, convEndian(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static double atomicFetchAddDoubleWithCAS(Object base, long offset, double delta, boolean swap) {
        long nativeExpectedValue;
        double expectedValue;
        do {
            nativeExpectedValue = loadLongAtomic(base, offset);
            expectedValue = l2d(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetLong(base, offset,
                nativeExpectedValue, d2l(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static byte atomicFetchAndByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_and_byte(base, offset, value);
    }

    public static short atomicFetchAndShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_and_short(base, offset, value);
    }

    public static int atomicFetchAndInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_and_int(base, offset, value);
    }

    public static long atomicFetchAndLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_and_long(base, offset, value);
    }

    public static byte atomicFetchOrByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_or_byte(base, offset, value);
    }

    public static short atomicFetchOrShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_or_short(base, offset, value);
    }

    public static int atomicFetchOrInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_or_int(base, offset, value);
    }

    public static long atomicFetchOrLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_or_long(base, offset, value);
    }

    public static byte atomicFetchXorByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_xor_byte(base, offset, value);
    }

    public static short atomicFetchXorShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_xor_short(base, offset, value);
    }

    public static int atomicFetchXorInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_xor_int(base, offset, value);
    }

    public static long atomicFetchXorLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_xor_long(base, offset, value);
    }

    public static byte atomicCompareAndExchangeByte(Object base, long offset, byte expected, byte desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_byte(base, offset, expected, desired);
    }

    public static short atomicCompareAndExchangeShort(Object base, long offset, short expected, short desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_short(base, offset, expected, desired);
    }

    public static int atomicCompareAndExchangeInt(Object base, long offset, int expected, int desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_int(base, offset, expected, desired);
    }

    public static long atomicCompareAndExchangeLong(Object base, long offset, long expected, long desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_long(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetByte(Object base, long offset, byte expected, byte desired) {
        return Native.INSTANCE.atomic_compare_and_set_byte(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetShort(Object base, long offset, short expected, short desired) {
        return Native.INSTANCE.atomic_compare_and_set_short(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetInt(Object base, long offset, int expected, int desired) {
        return Native.INSTANCE.atomic_compare_and_set_int(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetLong(Object base, long offset, long expected, long desired) {
        return Native.INSTANCE.atomic_compare_and_set_long(base, offset, expected, desired);
    }

    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    public static final int LOG2_ARRAY_BOOLEAN_INDEX_SCALE = exactLog2(ARRAY_BOOLEAN_INDEX_SCALE);
    public static final int LOG2_ARRAY_BYTE_INDEX_SCALE = exactLog2(ARRAY_BYTE_INDEX_SCALE);
    public static final int LOG2_ARRAY_CHAR_INDEX_SCALE = exactLog2(ARRAY_CHAR_INDEX_SCALE);
    public static final int LOG2_ARRAY_SHORT_INDEX_SCALE = exactLog2(ARRAY_SHORT_INDEX_SCALE);
    public static final int LOG2_ARRAY_INT_INDEX_SCALE = exactLog2(ARRAY_INT_INDEX_SCALE);
    public static final int LOG2_ARRAY_LONG_INDEX_SCALE = exactLog2(ARRAY_LONG_INDEX_SCALE);
    public static final int LOG2_ARRAY_FLOAT_INDEX_SCALE = exactLog2(ARRAY_FLOAT_INDEX_SCALE);
    public static final int LOG2_ARRAY_DOUBLE_INDEX_SCALE = exactLog2(ARRAY_DOUBLE_INDEX_SCALE);

    private static final int LOG2_BYTE_BIT_SIZE = exactLog2(Byte.SIZE);

    private static int exactLog2(int scale) {
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        return Integer.numberOfTrailingZeros(scale);
    }

    public static int vectorizedMismatch(Object a, long aOffset,
                                         Object b, long bOffset,
                                         int length,
                                         int log2ArrayIndexScale) {
        // assert a.getClass().isArray();
        // assert b.getClass().isArray();
        // assert 0 <= length <= sizeOf(a)
        // assert 0 <= length <= sizeOf(b)
        // assert 0 <= log2ArrayIndexScale <= 3

        int log2ValuesPerWidth = LOG2_ARRAY_LONG_INDEX_SCALE - log2ArrayIndexScale;
        int wi = 0;
        for (; wi < length >> log2ValuesPerWidth; wi++) {
            long bi = ((long) wi) << LOG2_ARRAY_LONG_INDEX_SCALE;
            long av = AndroidUnsafe.getLongUnaligned(a, aOffset + bi);
            long bv = AndroidUnsafe.getLongUnaligned(b, bOffset + bi);
            if (av != bv) {
                long x = av ^ bv;
                int o = AndroidUnsafe.IS_BIG_ENDIAN
                        ? Long.numberOfLeadingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale)
                        : Long.numberOfTrailingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale);
                return (wi << log2ValuesPerWidth) + o;
            }
        }

        // Calculate the tail of remaining elements to check
        int tail = length - (wi << log2ValuesPerWidth);

        if (log2ArrayIndexScale < LOG2_ARRAY_INT_INDEX_SCALE) {
            int wordTail = 1 << (LOG2_ARRAY_INT_INDEX_SCALE - log2ArrayIndexScale);
            // Handle 4 bytes or 2 chars in the tail using int width
            if (tail >= wordTail) {
                long bi = ((long) wi) << LOG2_ARRAY_LONG_INDEX_SCALE;
                int av = AndroidUnsafe.getIntUnaligned(a, aOffset + bi);
                int bv = AndroidUnsafe.getIntUnaligned(b, bOffset + bi);
                if (av != bv) {
                    int x = av ^ bv;
                    int o = AndroidUnsafe.IS_BIG_ENDIAN
                            ? Integer.numberOfLeadingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale)
                            : Integer.numberOfTrailingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale);
                    return (wi << log2ValuesPerWidth) + o;
                }
                tail -= wordTail;
            }
        }
        return ~tail;
    }
}

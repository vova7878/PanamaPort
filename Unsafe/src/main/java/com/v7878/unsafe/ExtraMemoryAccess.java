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
import static com.v7878.llvm.Core.LLVMGetInsertBlock;
import static com.v7878.llvm.Core.LLVMGetParams;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntULT;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Core.LLVMSetAlignment;
import static com.v7878.llvm.Core.LLVMSetOrdering;
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BYTE;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.SHORT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.llvm.LLVMBuilder.call;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_intptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.local_jobj_to_ptr;
import static com.v7878.unsafe.llvm.LLVMTypes.fn_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int16_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int1_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int64_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.generateFunctionCodeArray;
import static com.v7878.unsafe.misc.Math.convEndian16;
import static com.v7878.unsafe.misc.Math.convEndian32;
import static com.v7878.unsafe.misc.Math.convEndian64;
import static com.v7878.unsafe.misc.Math.d2l;
import static com.v7878.unsafe.misc.Math.f2i;
import static com.v7878.unsafe.misc.Math.i2f;
import static com.v7878.unsafe.misc.Math.l2d;

import com.v7878.foreign.Arena;
import com.v7878.llvm.Core.LLVMAtomicRMWBinOp;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.ASM;
import com.v7878.unsafe.foreign.BulkLinker.ASMGenerator;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.llvm.LLVMTypes;

import java.util.Optional;
import java.util.function.Function;

public class ExtraMemoryAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class EarlyNative {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        /*
        extern "C" void memset(uintptr obj, uintptr off, uintptr bytes, char value) {
            auto ptr = (uint32*)(obj & (~3L));
            uintptr data = ptr ? *ptr : 0;
            auto memory = (char*)(data + off);
            for (uintptr i = 0; i < bytes; i++) {
                memory[i] = value;
            }
        }
        */
        @ASM(conditions = @Conditions(arch = X86_64), code = {
                72, -125, -25, -4, 116, 9, -117, 7, 72, -123, -46, 117, 9, -21, 23, 49,
                -64, 72, -123, -46, 116, 16, 72, 1, -16, 49, -10, -120, 12, 48, 72, -1,
                -58, 72, 57, -14, 117, -11, -61
        })
        @ASM(conditions = @Conditions(arch = X86), code = {
                86, -117, 68, 36, 16, -117, 76, 36, 8, -125, -31, -4, 116, 8, -117, 9,
                -123, -64, 117, 8, -21, 25, 49, -55, -123, -64, 116, 19, 15, -74, 84, 36,
                20, 3, 76, 36, 12, 49, -10, -120, 20, 49, 70, 57, -16, 117, -8, 94, -61
        })
        @ASM(conditions = @Conditions(arch = ARM64), code = {
                8, -12, 126, -14, 64, 0, 0, 84, 8, 1, 64, -71, -94, 0, 0, -76,
                8, 1, 1, -117, 66, 4, 0, -15, 3, 21, 0, 56, -63, -1, -1, 84,
                -64, 3, 95, -42
        })
        @ASM(conditions = @Conditions(arch = ARM), code = {
                3, 0, -48, -29, 0, 0, -112, 21, 0, 0, -96, 3, 0, 72, 45, -23,
                13, -80, -96, -31, 0, 0, 82, -29, 0, -120, -67, 8, 1, 0, -128, -32,
                1, 48, -64, -28, 1, 32, 82, -30, -4, -1, -1, 26, 0, -120, -67, -24
        })
        // TODO: RISCV64
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        /*
        extern "C" void memmove(uintptr dst_obj, uintptr dst_off, uintptr src_obj,
                                uintptr src_off, uintptr bytes) {
            auto dst_ptr = (uint32*)(dst_obj & (~3L));
            uintptr dst_data = ((uintptr)(dst_ptr ? *dst_ptr : 0)) + dst_off;
            auto dst_memory = (char*)dst_data;
            auto src_ptr = (uint32*)(src_obj & (~3L));
            uintptr src_data = ((uintptr)(src_ptr ? *src_ptr : 0)) + src_off;
            auto src_memory = (char*)src_data;
            if (src_data < dst_data) {
                for (uintptr i = 0; i < bytes; i++) {
                    int index = bytes - 1 - i;
                    dst_memory[index] = src_memory[index];
                }
            } else {
                for (uintptr i = 0; i < bytes; i++) {
                    dst_memory[i] = src_memory[i];
                }
            }
        }
         */
        @ASM(conditions = @Conditions(arch = X86_64), code = {
                72, -125, -25, -4, 116, 4, -117, 7, -21, 2, 49, -64, 72, 1, -16, 72,
                -125, -30, -4, 116, 4, -117, 18, -21, 2, 49, -46, 72, 1, -54, 72, 57,
                -62, 115, 51, 77, -123, -64, 116, 69, 72, -71, 0, 0, 0, 0, -1, -1,
                -1, -1, 76, -119, -58, 72, -63, -26, 32, 72, 1, -50, 72, -119, -9, 72,
                -63, -1, 32, 68, 15, -74, 12, 58, 68, -120, 12, 56, 72, 1, -50, 73,
                -1, -56, 117, -24, -21, 23, 77, -123, -64, 116, 18, 49, -55, 15, -74, 52,
                10, 64, -120, 52, 8, 72, -1, -63, 73, 57, -56, 117, -16, -61
        })
        @ASM(conditions = @Conditions(arch = X86), code = {
                83, 87, 86, -117, 84, 36, 24, -117, 116, 36, 20, -117, 124, 36, 16, 49,
                -64, -71, 0, 0, 0, 0, -125, -25, -4, 116, 2, -117, 15, 1, -15, -117,
                116, 36, 28, -125, -30, -4, 116, 2, -117, 2, 1, -16, -117, 84, 36, 32,
                57, -56, 115, 18, -123, -46, 116, 32, 15, -74, 92, 16, -1, -120, 92, 17,
                -1, 74, 117, -12, -21, 18, -123, -46, 116, 14, 49, -10, 15, -74, 28, 48,
                -120, 28, 49, 70, 57, -14, 117, -12, 94, 95, 91, -61
        })
        @ASM(conditions = @Conditions(arch = ARM64), code = {
                8, -12, 126, -14, 64, 0, 0, 84, 8, 1, 64, -71, 73, -12, 126, -14,
                8, 1, 1, -117, 64, 0, 0, 84, 41, 1, 64, -71, 41, 1, 3, -117,
                63, 1, 8, -21, 98, 1, 0, 84, -28, 1, 0, -76, -22, 127, 96, -78,
                75, -127, 4, -117, 108, -3, 96, -109, -124, 4, 0, -15, 107, 1, 10, -117,
                45, 105, 108, 56, 13, 105, 44, 56, 97, -1, -1, 84, 6, 0, 0, 20,
                -92, 0, 0, -76, 42, 21, 64, 56, -124, 4, 0, -15, 10, 21, 0, 56,
                -95, -1, -1, 84, -64, 3, 95, -42
        })
        @ASM(conditions = @Conditions(arch = ARM), code = {
                0, 72, 45, -23, 13, -80, -96, -31, 3, -32, -48, -29, 0, -64, -96, -29,
                0, 0, -96, -29, 0, 0, -98, 21, 1, 0, -128, -32, 3, 16, -46, -29,
                0, -64, -111, 21, 8, 16, -101, -27, 3, 32, -116, -32, 0, 0, 82, -31,
                8, 0, 0, 42, 0, 0, 81, -29, 0, -120, -67, 8, 1, 32, 66, -30,
                1, 0, 64, -30, 1, 48, -46, -25, 1, 48, -64, -25, 1, 16, 81, -30,
                -5, -1, -1, 26, 5, 0, 0, -22, 0, 0, 81, -29, 3, 0, 0, 10,
                1, 48, -46, -28, 1, 48, -64, -28, 1, 16, 81, -30, -5, -1, -1, 26,
                0, -120, -67, -24
        })
        // TODO: RISCV64
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        static final EarlyNative INSTANCE = BulkLinker.generateImpl(SCOPE,
                EarlyNative.class, name -> Optional.empty());
    }

    public static boolean isEarlyNativeInitialized() {
        return ClassUtils.isClassInitialized(EarlyNative.class);
    }

    @DoNotShrinkType
    @DoNotOptimize
    // TODO: cache
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @SuppressWarnings("SameParameterValue")
        private static LLVMValueRef gen_memmove_modify(
                LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder, String name,
                LLVMTypeRef element_type, int align, Function<LLVMValueRef, LLVMValueRef> action) {
            var one = const_intptr(context, 1);
            var zero = const_intptr(context, 0);

            var type = fn_t(void_t(context), intptr_t(context),
                    intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context));
            var function = LLVMAddFunction(module, name, type);
            var args = LLVMGetParams(function);

            var start = LLVMAppendBasicBlock(function, "");
            var body = LLVMAppendBasicBlock(function, "");
            var forward = LLVMAppendBasicBlock(function, "");
            var backward = LLVMAppendBasicBlock(function, "");
            var end = LLVMAppendBasicBlock(function, "");

            LLVMPositionBuilderAtEnd(builder, start);
            var length = args[4];
            var test_zero = LLVMBuildICmp(builder, LLVMIntEQ, length, zero, "");
            LLVMBuildCondBr(builder, test_zero, end, body);

            LLVMPositionBuilderAtEnd(builder, body);
            var langth_m1 = LLVMBuildSub(builder, length, one, "");
            var dst = local_jobj_to_ptr(builder, args[0], args[1], element_type);
            var src = local_jobj_to_ptr(builder, args[2], args[3], element_type);
            body = LLVMGetInsertBlock(builder);

            var test_order = LLVMBuildICmp(builder, LLVMIntULT, dst, src, "");
            LLVMBuildCondBr(builder, test_order, forward, backward);

            {
                LLVMPositionBuilderAtEnd(builder, forward);
                var counter = LLVMBuildPhi(builder, intptr_t(context), "");
                LLVMAddIncoming(counter, zero, body);
                var src_element = LLVMBuildInBoundsGEP(builder, src, new LLVMValueRef[]{counter}, "");
                var dst_element = LLVMBuildInBoundsGEP(builder, dst, new LLVMValueRef[]{counter}, "");
                var load = LLVMBuildLoad(builder, src_element, "");
                LLVMSetAlignment(load, align);
                var value = action.apply(load);
                var store = LLVMBuildStore(builder, value, dst_element);
                LLVMSetAlignment(store, align);
                var next_counter = LLVMBuildAdd(builder, counter, one, "");
                LLVMAddIncoming(counter, next_counter, forward);
                var test_end = LLVMBuildICmp(builder, LLVMIntEQ, next_counter, length, "");
                LLVMBuildCondBr(builder, test_end, end, forward);
            }
            {
                LLVMPositionBuilderAtEnd(builder, backward);
                var counter = LLVMBuildPhi(builder, intptr_t(context), "");
                LLVMAddIncoming(counter, langth_m1, body);
                var src_element = LLVMBuildInBoundsGEP(builder, src, new LLVMValueRef[]{counter}, "");
                var dst_element = LLVMBuildInBoundsGEP(builder, dst, new LLVMValueRef[]{counter}, "");
                var load = LLVMBuildLoad(builder, src_element, "");
                LLVMSetAlignment(load, align);
                var value = action.apply(load);
                var store = LLVMBuildStore(builder, value, dst_element);
                LLVMSetAlignment(store, align);
                var next_counter = LLVMBuildSub(builder, counter, one, "");
                LLVMAddIncoming(counter, next_counter, backward);
                var test_end = LLVMBuildICmp(builder, LLVMIntEQ, counter, zero, "");
                LLVMBuildCondBr(builder, test_end, end, backward);
            }

            LLVMPositionBuilderAtEnd(builder, end);
            LLVMBuildRetVoid(builder);

            return function;
        }

        @ASMGenerator(method = "gen_memmove_swap_shorts")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_shorts(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_shorts() {
            final String name = "memmove_swap_shorts";
            return generateFunctionCodeArray((context, module, builder) -> {
                var bswap16_type = fn_t(int16_t(context), int16_t(context));
                var bswap16 = LLVMAddFunction(module, "llvm.bswap.i16", bswap16_type);
                return gen_memmove_modify(context, module, builder, name, int16_t(context), 1,
                        value -> call(builder, bswap16, value));
            });
        }

        @ASMGenerator(method = "gen_memmove_swap_ints")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_ints(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_ints() {
            final String name = "memmove_swap_ints";
            return generateFunctionCodeArray((context, module, builder) -> {
                var bswap32_type = fn_t(int32_t(context), int32_t(context));
                var bswap32 = LLVMAddFunction(module, "llvm.bswap.i32", bswap32_type);
                return gen_memmove_modify(context, module, builder, name, int32_t(context), 1,
                        value -> call(builder, bswap32, value));
            });
        }

        @ASMGenerator(method = "gen_memmove_swap_longs")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap_longs(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_memmove_swap_longs() {
            final String name = "memmove_swap_longs";
            return generateFunctionCodeArray((context, module, builder) -> {
                var bswap64_type = fn_t(int64_t(context), int64_t(context));
                var bswap64 = LLVMAddFunction(module, "llvm.bswap.i64", bswap64_type);
                return gen_memmove_modify(context, module, builder, name, int64_t(context), 1,
                        value -> call(builder, bswap64, value));
            });
        }

        private static byte[] gen_load_atomic(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, int alignment) {
            return generateFunctionCodeArray((context, module, builder) -> {
                var var_type = type.apply(context);
                var f_type = fn_t(var_type, intptr_t(context), intptr_t(context));
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                var pointer = local_jobj_to_ptr(builder, args[0], args[1], var_type);
                var load = LLVMBuildLoad(builder, pointer, "");
                LLVMSetAlignment(load, alignment);
                LLVMSetOrdering(load, LLVMAtomicOrderingSequentiallyConsistent);

                LLVMBuildRet(builder, load);

                return function;
            });
        }

        @ASMGenerator(method = "gen_load_byte_atomic")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_byte_atomic() {
            return gen_load_atomic("load_byte_atomic", LLVMTypes::int8_t, 1);
        }

        @ASMGenerator(method = "gen_load_short_atomic")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_short_atomic() {
            return gen_load_atomic("load_short_atomic", LLVMTypes::int16_t, 2);
        }

        @ASMGenerator(method = "gen_load_int_atomic")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_int_atomic() {
            return gen_load_atomic("load_int_atomic", LLVMTypes::int32_t, 4);
        }

        @ASMGenerator(method = "gen_load_long_atomic")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD})
        abstract long load_long_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_long_atomic() {
            return gen_load_atomic("load_long_atomic", LLVMTypes::int64_t, 8);
        }

        private static byte[] gen_store_atomic(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, int alignment) {
            return generateFunctionCodeArray((context, module, builder) -> {
                var var_type = type.apply(context);
                var f_type = fn_t(void_t(context), intptr_t(context), intptr_t(context), var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                var pointer = local_jobj_to_ptr(builder, args[0], args[1], var_type);
                var store = LLVMBuildStore(builder, args[2], pointer);
                LLVMSetAlignment(store, alignment);
                LLVMSetOrdering(store, LLVMAtomicOrderingSequentiallyConsistent);

                LLVMBuildRetVoid(builder);

                return function;
            });
        }

        @ASMGenerator(method = "gen_store_byte_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_byte_atomic() {
            return gen_store_atomic("store_byte_atomic", LLVMTypes::int8_t, 1);
        }

        @ASMGenerator(method = "gen_store_short_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_short_atomic() {
            return gen_store_atomic("store_short_atomic", LLVMTypes::int16_t, 2);
        }

        @ASMGenerator(method = "gen_store_int_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_int_atomic() {
            return gen_store_atomic("store_int_atomic", LLVMTypes::int32_t, 4);
        }

        @ASMGenerator(method = "gen_store_long_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract void store_long_atomic(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_long_atomic() {
            return gen_store_atomic("store_long_atomic", LLVMTypes::int64_t, 8);
        }

        private static byte[] gen_atomic_rmw(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, LLVMAtomicRMWBinOp op) {
            return generateFunctionCodeArray((context, module, builder) -> {
                var var_type = type.apply(context);
                var f_type = fn_t(var_type, intptr_t(context), intptr_t(context), var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                var pointer = local_jobj_to_ptr(builder, args[0], args[1], var_type);
                var rmw = LLVMBuildAtomicRMW(builder, op, pointer, args[2],
                        LLVMAtomicOrderingSequentiallyConsistent, false);

                LLVMBuildRet(builder, rmw);

                return function;
            });
        }

        @ASMGenerator(method = "gen_atomic_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_byte() {
            return gen_atomic_rmw("atomic_exchange_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_short() {
            return gen_atomic_rmw("atomic_exchange_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_int() {
            return gen_atomic_rmw("atomic_exchange_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_long() {
            return gen_atomic_rmw("atomic_exchange_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_byte() {
            return gen_atomic_rmw("atomic_fetch_and_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_short() {
            return gen_atomic_rmw("atomic_fetch_and_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_int() {
            return gen_atomic_rmw("atomic_fetch_and_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_add_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_add_long() {
            return gen_atomic_rmw("atomic_fetch_and_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_byte() {
            return gen_atomic_rmw("atomic_fetch_or_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_short() {
            return gen_atomic_rmw("atomic_fetch_or_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_int() {
            return gen_atomic_rmw("atomic_fetch_or_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_long() {
            return gen_atomic_rmw("atomic_fetch_or_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_byte() {
            return gen_atomic_rmw("atomic_fetch_xor_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_short() {
            return gen_atomic_rmw("atomic_fetch_xor_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_int() {
            return gen_atomic_rmw("atomic_fetch_xor_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_xor_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_long() {
            return gen_atomic_rmw("atomic_fetch_xor_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpXor);
        }

        //TODO: weak version?
        private static byte[] gen_atomic_compare_and_exchange(
                String name, Function<LLVMContextRef, LLVMTypeRef> type, boolean ret_value) {
            return generateFunctionCodeArray((context, module, builder) -> {
                var var_type = type.apply(context);
                var r_type = ret_value ? var_type : int1_t(context);
                var f_type = fn_t(r_type, intptr_t(context), intptr_t(context), var_type, var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                var pointer = local_jobj_to_ptr(builder, args[0], args[1], var_type);
                var cmpxchg = LLVMBuildAtomicCmpXchg(builder, pointer, args[2],
                        args[3], LLVMAtomicOrderingSequentiallyConsistent,
                        LLVMAtomicOrderingSequentiallyConsistent, false);
                var ret = LLVMBuildExtractValue(builder, cmpxchg, ret_value ? 0 : 1, "");

                LLVMBuildRet(builder, ret);

                return function;
            });
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_byte", LLVMTypes::int8_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_short", LLVMTypes::int16_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_int", LLVMTypes::int32_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_long", LLVMTypes::int64_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_byte")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_byte", LLVMTypes::int8_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_short")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_short", LLVMTypes::int16_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_int")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_int", LLVMTypes::int32_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_long")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract boolean atomic_compare_and_set_long(Object base, long offset, long expected, long desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_long", LLVMTypes::int64_t, false);
        }

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class);
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }
        EarlyNative.INSTANCE.memset(base, offset, bytes, value);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }
        EarlyNative.INSTANCE.memmove(destBase, destOffset, srcBase, srcOffset, bytes);
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
            expectedValue = convEndian16(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetShort(base, offset,
                nativeExpectedValue, convEndian16((short) (expectedValue + delta), swap)));
        return expectedValue;
    }

    public static int atomicFetchAddIntWithCAS(Object base, long offset, int delta, boolean swap) {
        int nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadIntAtomic(base, offset);
            expectedValue = convEndian32(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetInt(base, offset,
                nativeExpectedValue, convEndian32(expectedValue + delta, swap)));
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
            expectedValue = convEndian64(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetLong(base, offset,
                nativeExpectedValue, convEndian64(expectedValue + delta, swap)));
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
}

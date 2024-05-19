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
import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.X86;
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 68, 36, 12, -123, -64, 116, 30, -118, 76, 36, 16, -117, 84, 36, 4, 3, 84, 36, 8, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -120, 10, 66, 72, 117, -6, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                -94, 0, 0, -76, 40, 64, 32, -117, 3, 21, 0, 56, 66, 4, 0, -47, -62, -1, -1, -75, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, 0, 82, -29, 30, -1, 47, 1, 1, 0, -128, -32, 1, 48, -64, -28, 1, 32, 82, -30, -4, -1, -1, 26, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, -117, 68, 36, 24, -123, -64, 116, 51, -117, 76, 36, 20, -117, 84, 36, 12, 3, 84, 36, 8,
                3, 76, 36, 16, 57, -54, 115, 19, -112, -112, -112, 15, -74, 25, -120, 26, 66, 65, 72, 117, -10,
                -21, 16, -112, -112, -112, -112, 15, -74, 92, 1, -1, -120, 92, 2, -1, 72, 117, -12, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                4, 2, 0, -76, 40, 64, 32, -117, 105, 64, 34, -117, 31, 1, 9, -21, -62, 0, 0, 84, 42, 21, 64,
                56, 10, 21, 0, 56, -124, 4, 0, -47, -92, -1, -1, -75, 7, 0, 0, 20, 42, 1, 4, -117, 74, -15,
                95, 56, 11, 1, 4, -117, 106, -15, 31, 56, -124, 4, 0, -47, 100, -1, -1, -75, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, -64, -99, -27, 0, 0, 92, -29, 30, -1, 47, 1, 3, 32, -126, -32, 1, 0, -128, -32, 2, 0, 80, -31, 4, 0,
                0, 42, 1, 16, -46, -28, 1, -64, 92, -30, 1, 16, -64, -28, -5, -1, -1, 26, 5, 0, 0, -22, 1, 16, 66, -30,
                1, 0, 64, -30, 12, 32, -47, -25, 12, 32, -64, -25, 1, -64, 92, -30, -5, -1, -1, 26, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 68, 36, 24, -123, -64, 116, 72, -117, 76, 36, 20, -117, 84, 36, 12, 3, 84, 36, 8, 3, 76, 36, 16, 57, -54, 115, 35, -112,
                -112, -112, 15, -73, 49, 102, -63, -58, 8, 102, -119, 50, -125, -62, 2, -125, -63, 2, 72, 117, -19, -21, 28, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, 15, -73, 116, 65, -2, 102, -63, -58, 8, 102, -119, 116, 66, -2, 72, 117, -17, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                -92, 2, 0, -76, 40, 64, 32, -117, 105, 64, 34, -117, 31, 1, 9, -21, 2, 1, 0, 84, 42, 37, 64,
                120, 74, 9, -64, 90, 74, 125, 16, 83, 10, 37, 0, 120, -124, 4, 0, -47, 100, -1, -1, -75, 10,
                0, 0, 20, -118, 4, 0, -47, 75, -7, 127, -45, 44, 105, 107, 120, 74, 5, 0, -47, -116, 9, -64,
                90, -116, 125, 16, 83, 12, 105, 43, 120, 95, 5, 0, -79, 33, -1, -1, 84, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, -64, -99, -27, 0, 0, 92, -29, 30, -1, 47, 1, 3, 32, -126, -32, 1, 0, -128, -32, 2, 0, 80, -31, 6, 0,
                0, 42, -78, 16, -46, -32, 1, -64, 92, -30, 49, 31, -65, -26, 33, 24, -96, -31, -78, 16, -64, -32, -7, -1,
                -1, 26, 8, 0, 0, -22, 1, 48, 76, -30, -125, 16, -126, -32, -125, 0, -128, -32, -78, 32, 81, -32, 1, -64,
                92, -30, 50, 47, -65, -26, 34, 40, -96, -31, -78, 32, 64, -32, -7, -1, -1, 26, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 68, 36, 24, -123, -64, 116, 68, -117, 76, 36, 20, -117, 84, 36, 12, 3, 84, 36, 8, 3, 76, 36, 16, 57, -54, 115, 35, -112,
                -112, -112, -117, 49, 15, -50, -119, 50, -125, -62, 4, -125, -63, 4, 72, 117, -15, -21, 28, -112, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -112, -117, 116, -127, -4, 15, -50, -119, 116, -126, -4, 72, 117, -13, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                100, 2, 0, -76, 40, 64, 32, -117, 105, 64, 34, -117, 31, 1, 9, -21, -30, 0, 0, 84, 42, 69, 64, -72, 74, 9, -64, 90, 10, 69,
                0, -72, -124, 4, 0, -47, -124, -1, -1, -75, 9, 0, 0, 20, -118, 4, 0, -47, 75, -11, 126, -45, 44, 105, 107, -72, 74, 5, 0,
                -47, -116, 9, -64, 90, 12, 105, 43, -72, 95, 5, 0, -79, 65, -1, -1, 84, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, -64, -99, -27, 0, 0, 92, -29, 30, -1, 47, 1, 3, 32, -126, -32, 1, 0, -128, -32, 2, 0, 80, -31, 5, 0, 0, 42, 4, 16,
                -110, -28, 1, -64, 92, -30, 49, 31, -65, -26, 4, 16, -128, -28, -6, -1, -1, 26, 6, 0, 0, -22, 4, 16, 66, -30, 4, 0,
                64, -30, 12, 33, -111, -25, 50, 47, -65, -26, 12, 33, -128, -25, 1, -64, 92, -30, -6, -1, -1, 26, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                87, 86, -117, 68, 36, 28, -123, -64, 116, 77, -117, 76, 36, 24, -117, 84, 36, 16, 3, 84, 36, 12, 3, 76, 36,
                20, 57, -54, 115, 34, -112, -112, -117, 49, -117, 121, 4, 15, -49, 15, -50, -119, 114, 4, -119, 58, -125,
                -62, 8, -125, -63, 8, 72, 117, -23, -21, 30, -112, -112, -112, -112, -112, -112, -112, -117, 116, -63, -8,
                -117, 124, -63, -4, 15, -49, 15, -50, -119, 116, -62, -4, -119, 124, -62, -8, 72, 117, -23, 94, 95, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                100, 2, 0, -76, 40, 64, 32, -117, 105, 64, 34, -117, 31, 1, 9, -21, -30, 0, 0, 84, 42, -123, 64, -8, 74, 13, -64,
                -38, 10, -123, 0, -8, -124, 4, 0, -47, -124, -1, -1, -75, 9, 0, 0, 20, -118, 4, 0, -47, 75, -15, 125, -45, 44, 105,
                107, -8, 74, 5, 0, -47, -116, 13, -64, -38, 12, 105, 43, -8, 95, 5, 0, -79, 65, -1, -1, 84, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, -64, -99, -27, 0, 0, 92, -29, 30, -1, 47, 1, 3, 32, -126, -32, 1, 0, -128, -32, 2, 0, 80, -31, 10, 0, 0, 42,
                0, 16, -110, -27, 4, 48, -110, -27, 8, 32, -126, -30, 1, -64, 92, -30, 49, 31, -65, -26, 4, 16, -128, -27, 51,
                31, -65, -26, 0, 16, -128, -27, 8, 0, -128, -30, -11, -1, -1, 26, 12, 0, 0, -22, 1, 48, 76, -30, -125, 17, -126,
                -32, -125, 1, -128, -32, 0, 32, -111, -27, 4, 48, -111, -27, 8, 16, 65, -30, 1, -64, 92, -30, 50, 47, -65, -26,
                4, 32, -128, -27, 51, 47, -65, -26, 0, 32, -128, -27, 8, 0, 64, -30, -11, -1, -1, 26, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {-117, 68, 36, 8, -117, 76, 36, 4, -118, 4, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 0, -3, -33, 8, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {1, 0, -48, -25, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_load_byte_atomic")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_byte_atomic() {
            return gen_load_atomic("load_byte_atomic", LLVMGlobals::int8_t, 1);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -117, 4, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {-117, 68, 36, 8, -117, 76, 36, 4, 102, -117, 4, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 0, -3, -33, 72, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {1, 0, -128, -32, -80, 0, -48, -31, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_load_short_atomic")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_short_atomic() {
            return gen_load_atomic("load_short_atomic", LLVMGlobals::int16_t, 2);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -117, 4, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {-117, 68, 36, 8, -117, 76, 36, 4, -117, 4, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 0, -3, -33, -120, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {1, 0, -112, -25, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_load_int_atomic")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        @SuppressWarnings("unused")
        private static byte[] gen_load_int_atomic() {
            return gen_load_atomic("load_int_atomic", LLVMGlobals::int32_t, 4);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -117, 4, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, 87, 86, -117, 116, 36, 20, -117, 124, 36, 16, 49, -64, 49, -46, 49, -55, 49, -37, -16, 15, -57, 12, 55, 94, 95, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 0, -3, -33, -56, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 0, -128, -32, -97, 15, -80, -31, 31, -16, 127, -11, 91, -16, 127, -11, 30, -1, 47, -31})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 68, 36, 8, -117, 76, 36, 4, -118, 84, 36, 12, -122, 20, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 2, -3, -97, 8, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, 32, -64, -25, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_store_byte_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_byte_atomic() {
            return gen_store_atomic("store_byte_atomic", LLVMGlobals::int8_t, 1);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -121, 20, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 68, 36, 8, -117, 76, 36, 4, 15, -73, 84, 36, 12, 102, -121, 20, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 2, -3, -97, 72, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 0, -128, -32, 91, -16, 127, -11, -80, 32, -64, -31, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_store_short_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_short_atomic() {
            return gen_store_atomic("store_short_atomic", LLVMGlobals::int16_t, 2);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -121, 20, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 68, 36, 8, -117, 76, 36, 4, -117, 84, 36, 12, -121, 20, 1, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 2, -3, -97, -120, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, 32, -128, -25, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_store_int_atomic")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_store_int_atomic() {
            return gen_store_atomic("store_int_atomic", LLVMGlobals::int32_t, 4);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -121, 20, 48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, 87, 86, -117, 76, 36, 28, -117, 92, 36, 24, -117, 84, 36, 20, -117, 124, 36, 16, -115, 52, 23,
                -117, 4, 23, -117, 84, 23, 4, -112, -112, -112, -16, 15, -57, 14, 117, -6, 94, 95, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {40, 64, 32, -117, 2, -3, -97, -56, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                48, 72, 45, -23, 91, -16, 127, -11, 1, 0, -128, -32, -97, 79, -80, -31, -110, 31,
                -96, -31, 0, 0, 81, -29, -5, -1, -1, 26, 91, -16, 127, -11, 48, -120, -67, -24})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 76, 36, 8, -117, 84, 36, 4, -118, 68, 36, 12, -122, 4, 10, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 8, 2, -3, 9, 8, -55, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, 16, -128, -32, -97, 15, -47, -31, -110, 63, -63,
                -31, 0, 0, 83, -29, -5, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_byte() {
            return gen_atomic_rmw("atomic_exchange_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 102, -121, 20, 48, -119, -48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 76, 36, 8, -117, 84, 36, 4, 15, -73, 68, 36, 12, 102, -121, 4, 10, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 72, 2, -3, 9, 72, -55, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, 16, -128, -32, -97, 15, -15, -31, -110, 63, -31,
                -31, 0, 0, 83, -29, -5, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_short() {
            return gen_atomic_rmw("atomic_exchange_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, -121, 20, 48, -119, -48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                -117, 76, 36, 8, -117, 84, 36, 4, -117, 68, 36, 12, -121, 4, 10, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -120, 2, -3, 9, -120, -55, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, 16, -128, -32, -97, 15, -111, -31, -110, 63, -127,
                -31, 0, 0, 83, -29, -5, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_int() {
            return gen_atomic_rmw("atomic_exchange_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -8, 72, -121, 20, 48, 72, -119, -48, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, 87, 86, -117, 76, 36, 28, -117, 92, 36, 24, -117, 84, 36, 20, -117, 124, 36, 16, -115, 52,
                23, -117, 4, 23, -117, 84, 23, 4, -112, -112, -112, -16, 15, -57, 14, 117, -6, 94, 95, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -56, 2, -3, 9, -56, -55, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                0, 72, 45, -23, 91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -68, -31, -110, -17,
                -84, -31, 0, 0, 94, -29, -5, -1, -1, 26, 91, -16, 127, -11, 0, -120, -67, -24})
        @ASMGenerator(method = "gen_atomic_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_long() {
            return gen_atomic_rmw("atomic_exchange_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 32, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -118, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -118, 4, 6, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -120, -60, 32, -52, -16, 15, -80, 34, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 8, 9, 0, 2, 10, 9, -3, 10, 8, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -36, -31, 2, 48, 0, -32, -109, 31,
                -52, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_and_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_byte() {
            return gen_atomic_rmw("atomic_fetch_and_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 33, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, 102, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, 102, -117, 4, 6, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 33, -50, 102, -16, 15, -79, 50, 117, -11, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 72, 9, 0, 2, 10, 9, -3, 10, 72, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -4, -31, 2, 48, 0, -32, -109, 31,
                -20, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_and_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_short() {
            return gen_atomic_rmw("atomic_fetch_and_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 33, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -117, 4, 6, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 33, -50, -16, 15, -79, 50, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -120, 9, 0, 2, 10, 9, -3, 10, -120, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -100, -31, 2, 48, 0, -32, -109, 31,
                -116, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_and_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_int() {
            return gen_atomic_rmw("atomic_fetch_and_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 33, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                85, 83, 87, 86, -117, 116, 36, 32, -117, 124, 36, 28, -117, 76, 36, 24, -117, 84, 36, 20, -115, 44, 10, -117, 4, 10,
                -117, 84, 10, 4, -112, -112, -119, -61, 33, -5, -119, -47, 33, -15, -16, 15, -57, 77, 0, 117, -15, 94, 95, 91, 93, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -56, 9, 0, 2, -118, 9, -3, 10, -56, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                48, 72, 45, -23, 91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -68, -31, 3, 80, 1, -32, 2, 64,
                0, -32, -108, -17, -84, -31, 0, 0, 94, -29, -7, -1, -1, 26, 91, -16, 127, -11, 48, -120, -67, -24})
        @ASMGenerator(method = "gen_atomic_fetch_add_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_add_long() {
            return gen_atomic_rmw("atomic_fetch_and_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 8, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -118, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -118, 4, 6, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -120, -60, 8, -52, -16, 15, -80, 34, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 8, 9, 0, 2, 42, 9, -3, 10, 8, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -36, -31, 2, 48, -128, -31, -109, 31,
                -52, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_or_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_byte() {
            return gen_atomic_rmw("atomic_fetch_or_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 9, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, 102, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, 102, -117, 4, 6, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 9, -50, 102, -16, 15, -79, 50, 117, -11, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 72, 9, 0, 2, 42, 9, -3, 10, 72, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -4, -31, 2, 48, -128, -31, -109, 31,
                -20, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_or_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_short() {
            return gen_atomic_rmw("atomic_fetch_or_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 9, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -117, 4, 6, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 9, -50, -16, 15, -79, 50, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -120, 9, 0, 2, 42, 9, -3, 10, -120, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -100, -31, 2, 48, -128, -31, -109,
                31, -116, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_or_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_int() {
            return gen_atomic_rmw("atomic_fetch_or_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 9, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                85, 83, 87, 86, -117, 116, 36, 32, -117, 124, 36, 28, -117, 76, 36, 24, -117, 84, 36, 20, -115, 44, 10, -117, 4, 10,
                -117, 84, 10, 4, -112, -112, -119, -61, 9, -5, -119, -47, 9, -15, -16, 15, -57, 77, 0, 117, -15, 94, 95, 91, 93, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -56, 9, 0, 2, -86, 9, -3, 10, -56, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                48, 72, 45, -23, 91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -68, -31, 3, 80, -127, -31, 2, 64,
                -128, -31, -108, -17, -84, -31, 0, 0, 94, -29, -7, -1, -1, 26, 91, -16, 127, -11, 48, -120, -67, -24})
        @ASMGenerator(method = "gen_atomic_fetch_or_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_long() {
            return gen_atomic_rmw("atomic_fetch_or_long", LLVMGlobals::int64_t, LLVMAtomicRMWBinOpOr);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -118, 4, 49, 72, -115, 52, 49, 15, 31, -128, 0, 0, 0, 0, -119, -63, 48, -47, -16, 15, -80, 14, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -118, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -118, 4, 6, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -120, -60, 48, -52, -16, 15, -80, 34, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 8, 9, 0, 2, 74, 9, -3, 10, 8, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -36, -31, 2, 48, 32, -32, -109, 31,
                -52, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_xor_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_byte() {
            return gen_atomic_rmw("atomic_fetch_xor_byte", LLVMGlobals::int8_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 102, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, -119, -58, 49, -42, 102, -16, 15, -79, 49, 117, -11, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, 102, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, 102, -117, 4, 6, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 49, -50, 102, -16, 15, -79, 50, 117, -11, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 72, 9, 0, 2, 74, 9, -3, 10, 72, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -4, -31, 2, 48, 32, -32, -109, 31,
                -20, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_xor_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_short() {
            return gen_atomic_rmw("atomic_fetch_xor_short", LLVMGlobals::int16_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, -117, 4, 49, 72, -115, 12, 49, 15, 31, -128, 0, 0, 0, 0, -119, -58, 49, -42, -16, 15, -79, 49, 117, -10, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 76, 36, 16, -117, 68, 36, 12, -117, 116, 36, 8, -115, 20, 6, -117, 4, 6, -112, -112, -112, -112, -112,
                -112, -112, -112, -112, -112, -112, -112, -112, -119, -58, 49, -50, -16, 15, -79, 50, 117, -10, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -120, 9, 0, 2, 74, 9, -3, 10, -120, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -100, -31, 2, 48, 32, -32, -109,
                31, -116, -31, 0, 0, 81, -29, -6, -1, -1, 26, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_fetch_xor_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_int() {
            return gen_atomic_rmw("atomic_fetch_xor_int", LLVMGlobals::int32_t, LLVMAtomicRMWBinOpXor);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {
                -119, -7, 72, -117, 4, 49, 72, -115, 12, 49, 102, 15, 31, 68, 0, 0, 72, -119, -58, 72, 49, -42, -16, 72, 15, -79, 49, 117, -13, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                85, 83, 87, 86, -117, 116, 36, 32, -117, 124, 36, 28, -117, 76, 36, 24, -117, 84, 36, 20, -115, 44, 10, -117, 4, 10,
                -117, 84, 10, 4, -112, -112, -119, -61, 49, -5, -119, -47, 49, -15, -16, 15, -57, 77, 0, 117, -15, 94, 95, 91, 93, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -56, 9, 0, 2, -54, 9, -3, 10, -56, -86, -1, -1, 53, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                48, 72, 45, -23, 91, -16, 127, -11, 1, -64, -128, -32, -97, 15, -68, -31, 3, 80, 33, -32, 2, 64,
                32, -32, -108, -17, -84, -31, 0, 0, 94, -29, -7, -1, -1, 26, 91, -16, 127, -11, 48, -120, -67, -24})
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
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -118, 68, 36, 16, -118, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, -16, 15, -80, 12, 22, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 8, 31, 0, 34, 107, -127, 0, 0, 84, 3, -3,
                9, 8, -119, -1, -1, 53, 2, 0, 0, 20, 95, 63, 3, -43, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 16, -128, -32, 114, -64, -17, -26, -97, 15, -47, -31, 12, 0, 80, -31, 6, 0, 0, 26,
                91, -16, 127, -11, -109, 47, -63, -31, 0, 0, 82, -29, 3, 0, 0, 10, -97, 15, -47, -31,
                12, 0, 80, -31, -7, -1, -1, 10, 31, -16, 127, -11, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_byte")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_byte", LLVMGlobals::int8_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, 102, -16, 15, -79, 12, 55, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, 15, -73, 68, 36, 16, 15, -73, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, 102, -16, 15, -79, 12, 22, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, 72, 31, 32, 34, 107, -127, 0, 0, 84, 3, -3,
                9, 72, -119, -1, -1, 53, 2, 0, 0, 20, 95, 63, 3, -43, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 16, -128, -32, 114, -64, -1, -26, -97, 15, -15, -31, 12, 0, 80, -31, 6, 0, 0, 26,
                91, -16, 127, -11, -109, 47, -31, -31, 0, 0, 82, -29, 3, 0, 0, 10, -97, 15, -15, -31,
                12, 0, 80, -31, -7, -1, -1, 10, 31, -16, 127, -11, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_short")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_short", LLVMGlobals::int16_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -79, 12, 55, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 68, 36, 16, -117, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, -16, 15, -79, 12, 22, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -120, 31, 0, 2, 107, -127, 0, 0, 84, 3, -3,
                9, -120, -119, -1, -1, 53, 2, 0, 0, 20, 95, 63, 3, -43, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, -64, -128, -32, -97, 15, -100, -31, 2, 0, 80, -31, 6, 0, 0, 26, 91, -16, 127,
                -11, -109, 31, -116, -31, 0, 0, 81, -29, 3, 0, 0, 10, -97, 15, -100, -31, 2, 0,
                80, -31, -7, -1, -1, 10, 31, -16, 127, -11, 91, -16, 127, -11, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_int")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_int", LLVMGlobals::int32_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, 72, -119, -48, -16, 72, 15, -79, 12, 55, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, 87, 86, -117, 68, 36, 24, -117, 84, 36, 28, -117, 92, 36, 32, -117, 76, 36,
                36, -117, 116, 36, 20, -117, 124, 36, 16, -16, 15, -57, 12, 55, 94, 95, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 0, -3, 95, -56, 31, 0, 2, -21, -127, 0, 0, 84, 3, -3,
                9, -56, -119, -1, -1, 53, 2, 0, 0, 20, 95, 63, 3, -43, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                -16, 72, 45, -23, 1, -64, -128, -32, -97, 15, -68, -31, 3, 80, 33, -32, 2, 64, 32, -32, 5,
                80, -108, -31, 10, 0, 0, 26, 28, 112, -99, -27, 91, -16, 127, -11, 24, 96, -99, -27, -106,
                79, -84, -31, 0, 0, 84, -29, 5, 0, 0, 10, -97, 15, -68, -31, 3, 80, 33, -32, 2, 64, 32, -32,
                5, 80, -108, -31, -9, -1, -1, 10, 31, -16, 127, -11, 91, -16, 127, -11, -16, -120, -67, -24})
        @ASMGenerator(method = "gen_atomic_compare_and_exchange_long")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_long", LLVMGlobals::int64_t, true);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -80, 12, 55, 15, -108, -64, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -118, 68, 36, 16, -118, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, -16, 15, -80, 12, 22, 15, -108, -64, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 9, -3, 95, 8, 63, 1, 34, 107, -95, 0, 0, 84, 3, -3, 9, 8, -119, -1, -1,
                53, -32, 3, 0, 50, -64, 3, 95, -42, 95, 63, 3, -43, -32, 3, 31, 42, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 0, -128, -32, 114, 16, -17, -26, -97, -49, -48, -31, 1, 0, 92, -31, 6, 0, 0, 26, 91, -16, 127, -11, -109,
                47, -64, -31, 0, 0, 82, -29, 6, 0, 0, 10, -97, 47, -48, -31, 1, 0, 82, -31, -7, -1, -1, 10, 31, -16, 127, -11,
                0, 0, -96, -29, 91, -16, 127, -11, 30, -1, 47, -31, 91, -16, 127, -11, 1, 0, -96, -29, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_set_byte")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_byte", LLVMGlobals::int8_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, 102, -16, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, 15, -73, 68, 36, 16, 15, -73, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, 102, -16, 15, -79, 12, 22, 15, -108, -64, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 9, -3, 95, 72, 63, 33, 34, 107, -95, 0, 0, 84, 3, -3, 9, 72, -119, -1, -1,
                53, -32, 3, 0, 50, -64, 3, 95, -42, 95, 63, 3, -43, -32, 3, 31, 42, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 0, -128, -32, 114, 16, -1, -26, -97, -49, -16, -31, 1, 0, 92, -31, 6, 0, 0, 26, 91, -16, 127, -11, -109,
                47, -32, -31, 0, 0, 82, -29, 6, 0, 0, 10, -97, 47, -16, -31, 1, 0, 82, -31, -7, -1, -1, 10, 31, -16, 127,
                -11, 0, 0, -96, -29, 91, -16, 127, -11, 30, -1, 47, -31, 91, -16, 127, -11, 1, 0, -96, -29, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_set_short")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_short", LLVMGlobals::int16_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, -119, -48, -16, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                86, -117, 68, 36, 16, -117, 76, 36, 20, -117, 84, 36, 12, -117, 116, 36, 8, -16, 15, -79, 12, 22, 15, -108, -64, 94, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 9, -3, 95, -120, 63, 1, 2, 107, -95, 0, 0, 84, 3, -3, 9, -120, -119, -1, -1,
                53, -32, 3, 0, 50, -64, 3, 95, -42, 95, 63, 3, -43, -32, 3, 31, 42, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                1, 0, -128, -32, -97, 31, -112, -31, 2, 0, 81, -31, 6, 0, 0, 26, 91, -16, 127, -11, -109, 31, -128, -31,
                0, 0, 81, -29, 6, 0, 0, 10, -97, 31, -112, -31, 2, 0, 81, -31, -7, -1, -1, 10, 31, -16, 127, -11, 0, 0,
                -96, -29, 91, -16, 127, -11, 30, -1, 47, -31, 91, -16, 127, -11, 1, 0, -96, -29, 30, -1, 47, -31})
        @ASMGenerator(method = "gen_atomic_compare_and_set_int")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT_AS_RAW_INT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_int", LLVMGlobals::int32_t, false);
        }

        @ASM(conditions = @Conditions(arch = X86_64, poisoning = FALSE), code = {-119, -1, 72, -119, -48, -16, 72, 15, -79, 12, 55, 15, -108, -64, -61})
        @ASM(conditions = @Conditions(arch = X86, poisoning = FALSE), code = {
                83, 87, 86, -117, 68, 36, 24, -117, 84, 36, 28, -117, 92, 36, 32, -117, 76, 36, 36, -117,
                116, 36, 20, -117, 124, 36, 16, -16, 15, -57, 12, 55, 15, -108, -64, 94, 95, 91, -61})
        @ASM(conditions = @Conditions(arch = ARM64, poisoning = FALSE), code = {
                40, 64, 32, -117, 9, -3, 95, -56, 63, 1, 2, -21, -95, 0, 0, 84, 3, -3, 9, -56, -119, -1, -1,
                53, -32, 3, 0, 50, -64, 3, 95, -42, 95, 63, 3, -43, -32, 3, 31, 42, -64, 3, 95, -42})
        @ASM(conditions = @Conditions(arch = ARM, poisoning = FALSE), code = {
                -16, 72, 45, -23, 1, 0, -128, -32, -97, 79, -80, -31, 3, 16, 37, -32, 2, 80, 36, -32, 1, 16, -107, -31, 10, 0,
                0, 26, 28, 80, -99, -27, 91, -16, 127, -11, 24, 64, -99, -27, -108, 31, -96, -31, 0, 0, 81, -29, 8, 0, 0, 10,
                -97, 111, -80, -31, 3, 16, 39, -32, 2, 112, 38, -32, 1, 16, -105, -31, -9, -1, -1, 10, 31, -16, 127, -11, 0, 0,
                -96, -29, 91, -16, 127, -11, -16, -120, -67, -24, 91, -16, 127, -11, 1, 0, -96, -29, -16, -120, -67, -24})
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

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
import static com.v7878.unsafe.foreign.BulkLinker.CallType.FAST_STATIC;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class ExtraMemoryAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class EarlyNative {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        /*
        extern "C" void memset(uintptr, uintptr, uintptr obj, uintptr off, uintptr bytes, char value) {
            auto ptr = (uint32*)(obj & (~3L));
            uintptr data = ptr ? *ptr : 0;
            auto memory = (char*)(data + off);
            for (uintptr i = 0; i < bytes; i++) {
                memory[i] = value;
            }
        }
        */
        @ASM(conditions = @Conditions(arch = X86_64), code = {
                72, -125, -30, -4, 116, 12, -117, 2, 77, -123, -64, 117, 16, -23, -114, 0, 0, 0, 49,
                -64, 77, -123, -64, 15, -124, -125, 0, 0, 0, 72, 1, -56, 73, -125, -8, 4, 115, 4, 49,
                -55, -21, 106, 65, 15, -74, -47, 73, -125, -8, 32, 115, 4, 49, -55, -21, 53, 76, -119,
                -63, 72, -125, -31, -32, 102, 15, 110, -62, 102, 15, -17, -55, 102, 15, 56, 0, -63, 49,
                -10, -13, 15, 127, 4, 48, -13, 15, 127, 68, 48, 16, 72, -125, -58, 32, 72, 57, -15, 117,
                -20, 73, 57, -56, 116, 57, 65, -10, -64, 28, 116, 39, 72, -119, -50, 76, -119, -63, 72,
                -125, -31, -4, 102, 15, 110, -62, 102, 15, 96, -64, -14, 15, 112, -64, 0, 102, 15, 126,
                4, 48, 72, -125, -58, 4, 72, 57, -15, 117, -14, -21, 7, 68, -120, 12, 8, 72, -1, -63,
                73, 57, -56, 117, -12, -61
        })
        @ASM(conditions = @Conditions(arch = X86), code = {
                83, 87, 86, -117, 68, 36, 32, -117, 76, 36, 24, -125, -31, -4, 116, 8, -117, 9, -123,
                -64, 117, 8, -21, 120, 49, -55, -123, -64, 116, 114, 15, -74, 84, 36, 36, 3, 76, 36,
                28, 49, -10, -125, -8, 4, 114, 90, 49, -10, 15, -74, -6, -125, -8, 32, 114, 46, -119,
                -58, -125, -26, -32, 102, 15, 110, -57, 102, 15, -17, -55, 102, 15, 56, 0, -63, 49,
                -37, -13, 15, 127, 4, 25, -13, 15, 127, 68, 25, 16, -125, -61, 32, 57, -34, 117, -18,
                57, -16, 116, 46, -88, 28, 116, 34, -119, -13, -119, -58, -125, -26, -4, 102, 15, 110,
                -57, 102, 15, 96, -64, -14, 15, 112, -64, 0, 102, 15, 126, 4, 25, -125, -61, 4, 57,
                -34, 117, -12, -21, 4, -120, 20, 49, 70, 57, -16, 117, -8, 94, 95, 91, -61
        })
        @ASM(conditions = @Conditions(arch = ARM64), code = {
                72, -12, 126, -14, 64, 0, 0, 84, 8, 1, 64, -71, -92, 4, 0, -76, -97, 32, 0, -15, 8,
                1, 3, -117, 98, 0, 0, 84, -23, 3, 31, -86, 27, 0, 0, 20, -97, -128, 0, -15, 98, 0,
                0, 84, -23, 3, 31, -86, 13, 0, 0, 20, -96, 12, 1, 78, -118, 4, 125, -110, -119, -24,
                123, -110, 11, 65, 0, -111, -116, -24, 123, -110, -116, -127, 0, -15, 96, -127, 63,
                -83, 107, -127, 0, -111, -95, -1, -1, 84, -97, 0, 9, -21, 32, 2, 0, 84, 106, 1, 0,
                -76, -96, 12, 1, 14, -21, 3, 9, -86, -119, -16, 125, -110, 106, 1, 9, -53, 11, 1,
                11, -117, 74, 33, 0, -79, 96, -123, 0, -4, -63, -1, -1, 84, -97, 0, 9, -21, -64, 0,
                0, 84, -118, 0, 9, -53, 8, 1, 9, -117, 74, 5, 0, -15, 5, 21, 0, 56, -63, -1, -1,
                84, -64, 3, 95, -42
        })
        @ASM(conditions = @Conditions(arch = ARM), code = {
                0, 72, 45, -23, 13, -80, -96, -31, 3, 0, -46, -29, 8, -64, -101, -27, 0, 32, -112,
                21, 0, 32, -96, 3, 0, 0, 92, -29, 18, 0, 0, 10, 12, 16, -101, -27, 3, -32, -126,
                -32, 0, 0, -96, -29, 16, 0, 92, -29, 8, 0, 0, 58, 15, 0, -52, -29, -112, 27, -32,
                -18, 14, 32, -96, -31, 0, 48, -96, -31, 13, 10, 66, -12, 16, 48, 83, -30, -4, -1,
                -1, 26, 0, 0, 92, -31, 0, -120, -67, 8, 0, 32, -114, -32, 0, 0, 76, -32, 1, 16, -62,
                -28, 1, 0, 80, -30, -4, -1, -1, 26, 0, -120, -67, -24
        })
        //TODO: RISCV64
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        /*
        extern "C" void memmove(uintptr, uintptr, uintptr dst_obj, uintptr dst_off,
                                uintptr src_obj, uintptr src_off, uintptr bytes) {
            auto dst_ptr = (uint32*)(dst_obj & (~3L));
            uintptr dst_data = ((uintptr)(dst_ptr ? *dst_ptr : 0)) + dst_off;
            auto dst_memory = (char*)dst_data;    auto src_ptr = (uint32*)(src_obj & (~3L));
            uintptr src_data = ((uintptr)(src_ptr ? *src_ptr : 0)) + src_off;
            auto src_memory = (char*)src_data;    if (src_data < dst_data) {
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
                72, -125, -30, -4, 116, 4, -117, 2, -21, 2, 49, -64, 72, 1, -56, 73,
                -125, -32, -4, 116, 5, 65, -117, 8, -21, 2, 49, -55, 72, -117, 84, 36,
                8, 76, 1, -55, 72, -119, -49, 72, 41, -57, 15, -125, -102, 0, 0, 0,
                72, -123, -46, 15, -124, -67, 1, 0, 0, 49, -10, 72, -125, -6, 8, 15,
                -125, -76, 0, 0, 0, 72, -119, -9, 72, -125, -49, 1, -10, -62, 1, 116,
                19, -9, -42, 1, -42, 72, 99, -10, 68, 15, -74, 4, 49, 68, -120, 4,
                48, 72, -119, -2, 72, 57, -6, 15, -124, -119, 1, 0, 0, 72, -65, 0,
                0, 0, 0, -2, -1, -1, -1, 73, -119, -48, 73, -63, -32, 32, 73, 1,
                -8, 73, -119, -15, 73, -63, -31, 32, 77, 41, -56, 73, -119, -47, 73, 41,
                -15, -9, -42, 1, -42, 72, -63, -26, 32, 72, -119, -14, 72, -63, -6, 32,
                68, 15, -74, 20, 17, 68, -120, 20, 16, 76, -119, -62, 72, -63, -6, 32,
                68, 15, -74, 20, 17, 68, -120, 20, 16, 73, 1, -8, 72, 1, -2, 73,
                -125, -63, -2, 117, -44, -23, 44, 1, 0, 0, 72, -123, -46, 15, -124, 35,
                1, 0, 0, 49, -10, 72, -125, -6, 4, 15, -126, -73, 0, 0, 0, 72,
                -119, -57, 72, 41, -49, 72, -125, -1, 32, 15, -126, -89, 0, 0, 0, 72,
                -125, -6, 32, 115, 79, 49, -10, -21, 125, 65, -119, -48, 65, -1, -56, 15,
                -120, 64, -1, -1, -1, 73, -71, 0, 0, 0, 0, -1, -1, -1, -1, 78,
                -115, 4, 10, 73, -1, -56, 77, 57, -56, 15, -126, 38, -1, -1, -1, 72,
                -125, -1, 32, 15, -126, 28, -1, -1, -1, 72, -65, -32, -1, -1, -1, 1,
                0, 0, 0, 72, -125, -6, 32, 15, -125, -70, 0, 0, 0, 49, -10, -23,
                6, 1, 0, 0, 72, -119, -42, 72, -125, -26, -32, 49, -1, 15, 16, 4,
                57, 15, 16, 76, 57, 16, 15, 17, 4, 56, 15, 17, 76, 56, 16, 72,
                -125, -57, 32, 72, 57, -2, 117, -27, 72, 57, -14, 15, -124, -123, 0, 0,
                0, -10, -62, 28, 116, 32, 72, -119, -9, 72, -119, -42, 72, -125, -26, -4,
                68, -117, 4, 57, 68, -119, 4, 56, 72, -125, -57, 4, 72, 57, -2, 117,
                -17, 72, 57, -14, 116, 96, 73, -119, -48, 72, -119, -9, 73, -125, -32, 3,
                116, 20, 72, -119, -9, 68, 15, -74, 12, 57, 68, -120, 12, 56, 72, -1,
                -57, 73, -1, -56, 117, -17, 72, 41, -42, 72, -125, -2, -4, 119, 55, 72,
                -125, -64, 3, 72, -125, -63, 3, 15, -74, 116, 57, -3, 64, -120, 116, 56,
                -3, 15, -74, 116, 57, -2, 64, -120, 116, 56, -2, 15, -74, 116, 57, -1,
                64, -120, 116, 56, -1, 15, -74, 52, 57, 64, -120, 52, 56, 72, -125, -57,
                4, 72, 57, -6, 117, -47, -61, 72, -119, -42, 72, 33, -2, 73, -119, -48,
                73, -63, -32, 32, 77, 1, -56, 73, -71, 0, 0, 0, 0, -32, -1, -1,
                -1, 73, -119, -14, 77, -119, -61, 73, -63, -5, 32, 66, 15, 16, 68, 25,
                -31, 66, 15, 16, 76, 25, -15, 66, 15, 17, 76, 24, -15, 66, 15, 17,
                68, 24, -31, 77, 1, -56, 73, -125, -62, -32, 117, -40, 72, 57, -14, 116,
                -75, -10, -62, 24, 15, -124, -5, -3, -1, -1, 73, -119, -16, 72, -125, -57,
                24, 72, -119, -2, 72, 33, -42, 76, -119, -57, 72, 41, -9, 65, -9, -48,
                65, 1, -48, 73, -63, -32, 32, 73, -71, 0, 0, 0, 0, -8, -1, -1,
                -1, 77, -119, -62, 73, -63, -6, 32, 78, -117, 92, 17, -7, 78, -119, 92,
                16, -7, 77, 1, -56, 72, -125, -57, 8, 117, -26, 72, 57, -14, 15, -123,
                -79, -3, -1, -1, -23, 93, -1, -1, -1
        })
        @ASM(conditions = @Conditions(arch = X86), code = {
                85, 83, 87, 86, -125, -20, 8, -117, 84, 36, 44, -117, 116, 36, 40, -117,
                124, 36, 36, 49, -19, -71, 0, 0, 0, 0, -125, -25, -4, 116, 2, -117,
                15, 1, -15, -117, 116, 36, 48, -125, -30, -4, 116, 2, -117, 42, 1, -11,
                -117, 84, 36, 52, -119, -18, 41, -50, 15, -125, 6, 1, 0, 0, -123, -46,
                15, -124, -82, 1, 0, 0, 49, -1, -125, -6, 8, 15, -126, -127, 0, 0,
                0, -125, -2, 32, 114, 124, 49, -1, -125, -6, 32, 114, 65, -119, -45, -125,
                -29, -32, -119, 28, 36, -9, -37, -115, 60, 17, -125, -57, -16, -115, 4, 42,
                -125, -64, -16, 49, -10, 15, 16, 68, 48, -16, 15, 16, 12, 48, 15, 17,
                12, 55, 15, 17, 68, 55, -16, -125, -58, -32, 57, -13, 117, -25, -117, 60,
                36, 57, -6, 15, -124, 91, 1, 0, 0, -10, -62, 24, 116, 52, -119, -45,
                -125, -29, -8, -119, 28, 36, -9, -37, -9, -33, -115, 52, 17, -125, -58, -8,
                -115, 4, 42, -125, -64, -8, -14, 15, 16, 4, 56, -14, 15, 17, 4, 62,
                -125, -57, -8, 57, -5, 117, -17, -117, 60, 36, 57, -6, 15, -124, 34, 1,
                0, 0, -119, -48, -119, -5, -125, -32, 3, -119, 68, 36, 4, 116, 41, -119,
                60, 36, -119, -5, -9, -45, 1, -45, -115, 60, 25, 1, -21, -9, 92, 36,
                4, 49, -10, 15, -74, 4, 51, -120, 4, 55, 78, 57, 116, 36, 4, 117,
                -14, -117, 60, 36, -119, -5, 41, -13, 41, -41, -125, -1, -4, 15, -121, -31,
                0, 0, 0, 77, 41, -38, 73, 15, -74, 68, 21, 0, -120, 4, 17, 15,
                -74, 68, 21, -1, -120, 68, 17, -1, 15, -74, 68, 21, -2, -120, 68, 17,
                -2, 15, -74, 68, 21, -3, -120, 68, 17, -3, -125, -62, -4, 117, -40, -23,
                -80, 0, 0, 0, -123, -46, 15, -124, -88, 0, 0, 0, 49, -10, -125, -6,
                4, 114, 83, -119, -49, 41, -17, -125, -1, 32, 114, 74, 49, -10, -125, -6,
                32, 114, 42, -119, -42, -125, -26, -32, 49, -1, 15, 16, 68, 61, 0, 15,
                16, 76, 61, 16, 15, 17, 4, 57, 15, 17, 76, 57, 16, -125, -57, 32,
                57, -2, 117, -26, 57, -14, 116, 108, -10, -62, 28, 116, 25, -119, -9, -119,
                -42, -125, -26, -4, -117, 92, 61, 0, -119, 28, 57, -125, -57, 4, 57, -2,
                117, -14, 57, -14, 116, 78, -119, -45, -119, -9, -125, -29, 3, 116, 14, -119,
                -9, 15, -74, 68, 61, 0, -120, 4, 57, 71, 75, 117, -12, 41, -42, -125,
                -2, -4, 119, 48, -125, -63, 3, -125, -59, 3, 15, -74, 68, 61, -3, -120,
                68, 57, -3, 15, -74, 68, 61, -2, -120, 68, 57, -2, 15, -74, 68, 61,
                -1, -120, 68, 57, -1, 15, -74, 68, 61, 0, -120, 4, 57, -125, -57, 4,
                57, -6, 117, -42, -125, -60, 8, 94, 95, 91, 93, -61
        })
        @ASM(conditions = @Conditions(arch = ARM64), code = {
                72, -12, 126, -14, 64, 0, 0, 84, 8, 1, 64, -71, -119, -12, 126, -14,
                8, 1, 3, -117, 64, 0, 0, 84, 41, 1, 64, -71, 41, 1, 5, -117,
                43, 1, 8, -21, 34, 2, 0, 84, 38, 9, 0, -76, -33, 32, 0, -15,
                34, 3, 0, 84, -22, 3, 31, -86, -21, 3, 42, 42, -54, 0, 10, -53,
                -20, 127, 96, -78, 107, 1, 6, 11, 107, 125, 96, -45, 109, -3, 96, -109,
                74, 5, 0, -15, 107, 1, 12, -117, 46, 105, 109, 56, 14, 105, 45, 56,
                97, -1, -1, 84, 58, 0, 0, 20, 38, 7, 0, -76, -33, 32, 0, -15,
                -22, 3, 31, -86, -29, 5, 0, 84, 11, 1, 9, -53, 127, -127, 0, -15,
                -125, 5, 0, 84, -33, -128, 0, -15, 34, 2, 0, 84, -22, 3, 31, -86,
                29, 0, 0, 20, -52, 4, 0, 81, -22, 3, 31, -86, -20, -4, -1, 55,
                44, 0, -64, -110, -19, 127, 96, -78, -52, 0, 12, -117, -97, 1, 13, -21,
                67, -4, -1, 84, 127, -127, 0, -15, 3, -4, -1, 84, -33, -128, 0, -15,
                -126, 4, 0, 84, -22, 3, 31, -86, 53, 0, 0, 20, -53, 4, 125, -110,
                -54, -24, 123, -110, 12, 65, 0, -111, 45, 65, 0, -111, -50, -24, 123, -110,
                -96, -123, 127, -83, -50, -127, 0, -15, -83, -127, 0, -111, -128, -123, 63, -83,
                -116, -127, 0, -111, 97, -1, -1, 84, -33, 0, 10, -21, -128, 2, 0, 84,
                -117, 1, 0, -76, -19, 3, 10, -86, -54, -16, 125, -110, -85, 1, 10, -53,
                12, 1, 13, -117, 45, 1, 13, -117, -96, -123, 64, -4, 107, 33, 0, -79,
                -128, -123, 0, -4, -95, -1, -1, 84, -33, 0, 10, -21, 0, 1, 0, 84,
                -53, 0, 10, -53, 8, 1, 10, -117, 41, 1, 10, -117, 42, 21, 64, 56,
                107, 5, 0, -15, 10, 21, 0, 56, -95, -1, -1, 84, -64, 3, 95, -42,
                -22, 127, 96, -78, -53, 4, 125, -110, -19, 107, 91, -78, 76, -127, 6, -117,
                -54, 108, 123, -110, -50, 108, 123, -110, -113, -3, 96, -109, -50, -127, 0, -15,
                -116, 1, 13, -117, 48, 1, 15, -117, 15, 1, 15, -117, 0, 18, -33, 60,
                1, 18, -34, 60, -32, 17, -97, 60, -31, 17, -98, 60, -31, -2, -1, 84,
                -33, 0, 10, -21, -64, -3, -1, 84, 11, -11, -1, -76, -20, 3, 10, -86,
                -54, 116, 125, -110, -19, 115, 93, -78, -21, 3, 44, 42, -116, 1, 10, -53,
                107, 1, 6, 11, 107, 125, 96, -45, 110, -3, 96, -109, -116, 33, 0, -79,
                107, 1, 13, -117, 47, 1, 14, -117, 14, 1, 14, -117, -32, -111, 95, -4,
                -64, -111, 31, -4, 33, -1, -1, 84, -33, 0, 10, -21, -31, -14, -1, 84,
                -37, -1, -1, 23
        })
        @ASM(conditions = @Conditions(arch = ARM), code = {
                48, 72, 45, -23, 8, -80, -115, -30, 8, 0, -101, -27, 3, 32, -46, -29,
                0, 80, -96, -29, 0, 16, -96, -29, 0, 80, -110, 21, 3, 0, -48, -29,
                12, 32, -101, -27, 0, 16, -112, 21, 3, 80, -123, -32, 16, -64, -101, -27,
                2, 32, -127, -32, 5, 0, 82, -31, 26, 0, 0, 42, 0, 0, 92, -29,
                38, 0, 0, 10, 16, 0, 92, -29, 0, -32, -96, -29, 5, 0, 66, 32,
                16, 0, 80, 35, 11, 0, 0, 58, 16, 0, 76, -30, 15, -32, -52, -29,
                0, 16, -126, -32, 0, 48, -123, -32, 15, 64, -32, -29, 14, 0, -96, -31,
                4, 10, 97, -12, 16, 0, 80, -30, 4, 10, 67, -12, -5, -1, -1, 26,
                14, 0, 92, -31, 48, -120, -67, 8, 1, 16, 66, -30, 14, 0, 76, -32,
                1, 32, 69, -30, 0, 48, -47, -25, 0, 48, -62, -25, 1, 0, 80, -30,
                -5, -1, -1, 26, 13, 0, 0, -22, 0, 0, 92, -29, 11, 0, 0, 10,
                16, 0, 92, -29, 0, 64, -96, -29, 2, 0, 69, 32, 16, 0, 80, 35,
                7, 0, 0, 42, 4, 16, -126, -32, 4, 0, -123, -32, 4, 32, 76, -32,
                1, 48, -47, -28, 1, 32, 82, -30, 1, 48, -64, -28, -5, -1, -1, 26,
                48, -120, -67, -24, 15, 64, -52, -29, 2, 16, -96, -31, 5, 0, -96, -31,
                4, 48, -96, -31, 13, 10, 97, -12, 16, 48, 83, -30, 13, 10, 64, -12,
                -5, -1, -1, 26, 4, 0, 92, -31, -19, -1, -1, 26, -13, -1, -1, -22
        })
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        //TODO: RISCV64
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

            var type = fn_t(void_t(context), intptr_t(context), intptr_t(context), intptr_t(context),
                    intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context));
            var function = LLVMAddFunction(module, name, type);
            var args = LLVMGetParams(function);
            args = Arrays.copyOfRange(args, 2, args.length);

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
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
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
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
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
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
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
                var f_type = fn_t(var_type, intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context));
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);
                args = Arrays.copyOfRange(args, 2, args.length);

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
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_byte_atomic() {
            return gen_load_atomic("load_byte_atomic", LLVMTypes::int8_t, 1);
        }

        @ASMGenerator(method = "gen_load_short_atomic")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_short_atomic() {
            return gen_load_atomic("load_short_atomic", LLVMTypes::int16_t, 2);
        }

        @ASMGenerator(method = "gen_load_int_atomic")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_load_int_atomic() {
            return gen_load_atomic("load_int_atomic", LLVMTypes::int32_t, 4);
        }

        @ASMGenerator(method = "gen_load_long_atomic")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD})
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
                var f_type = fn_t(void_t(context), intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context), var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);
                args = Arrays.copyOfRange(args, 2, args.length);

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
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_byte_atomic() {
            return gen_store_atomic("store_byte_atomic", LLVMTypes::int8_t, 1);
        }

        @ASMGenerator(method = "gen_store_short_atomic")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_short_atomic() {
            return gen_store_atomic("store_short_atomic", LLVMTypes::int16_t, 2);
        }

        @ASMGenerator(method = "gen_store_int_atomic")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_store_int_atomic() {
            return gen_store_atomic("store_int_atomic", LLVMTypes::int32_t, 4);
        }

        @ASMGenerator(method = "gen_store_long_atomic")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG})
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
                var f_type = fn_t(var_type, intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context), var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);
                args = Arrays.copyOfRange(args, 2, args.length);

                LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
                var pointer = local_jobj_to_ptr(builder, args[0], args[1], var_type);
                var rmw = LLVMBuildAtomicRMW(builder, op, pointer, args[2],
                        LLVMAtomicOrderingSequentiallyConsistent, false);

                LLVMBuildRet(builder, rmw);

                return function;
            });
        }

        @ASMGenerator(method = "gen_atomic_exchange_byte")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_byte() {
            return gen_atomic_rmw("atomic_exchange_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_short")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_short() {
            return gen_atomic_rmw("atomic_exchange_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_int")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_int() {
            return gen_atomic_rmw("atomic_exchange_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_exchange_long")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_exchange_long() {
            return gen_atomic_rmw("atomic_exchange_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpXchg);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_byte")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_byte() {
            return gen_atomic_rmw("atomic_fetch_and_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_short")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_short() {
            return gen_atomic_rmw("atomic_fetch_and_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_and_int")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_and_int() {
            return gen_atomic_rmw("atomic_fetch_and_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_add_long")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_add_long() {
            return gen_atomic_rmw("atomic_fetch_and_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpAnd);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_byte")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_byte() {
            return gen_atomic_rmw("atomic_fetch_or_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_short")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_short() {
            return gen_atomic_rmw("atomic_fetch_or_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_int")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_int() {
            return gen_atomic_rmw("atomic_fetch_or_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_or_long")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_or_long() {
            return gen_atomic_rmw("atomic_fetch_or_long", LLVMTypes::int64_t, LLVMAtomicRMWBinOpOr);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_byte")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_byte() {
            return gen_atomic_rmw("atomic_fetch_xor_byte", LLVMTypes::int8_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_short")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_short() {
            return gen_atomic_rmw("atomic_fetch_xor_short", LLVMTypes::int16_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_int")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_fetch_xor_int() {
            return gen_atomic_rmw("atomic_fetch_xor_int", LLVMTypes::int32_t, LLVMAtomicRMWBinOpXor);
        }

        @ASMGenerator(method = "gen_atomic_fetch_xor_long")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
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
                var f_type = fn_t(r_type, intptr_t(context), intptr_t(context), intptr_t(context), intptr_t(context), var_type, var_type);
                var function = LLVMAddFunction(module, name, f_type);
                var args = LLVMGetParams(function);
                args = Arrays.copyOfRange(args, 2, args.length);

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
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_byte", LLVMTypes::int8_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_short")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_short", LLVMTypes::int16_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_int")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_int", LLVMTypes::int32_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_exchange_long")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_exchange_long() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_exchange_long", LLVMTypes::int64_t, true);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_byte")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_byte() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_byte", LLVMTypes::int8_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_short")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_short() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_short", LLVMTypes::int16_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_int")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static byte[] gen_atomic_compare_and_set_int() {
            return gen_atomic_compare_and_exchange("atomic_compare_and_set_int", LLVMTypes::int32_t, false);
        }

        @ASMGenerator(method = "gen_atomic_compare_and_set_long")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
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

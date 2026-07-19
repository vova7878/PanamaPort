package com.v7878.unsafe;

import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpAnd;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpOr;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXchg;
import static com.v7878.llvm.Core.LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXor;
import static com.v7878.llvm.Core.LLVMBuildAtomicCmpXchg;
import static com.v7878.llvm.Core.LLVMBuildAtomicRMW;
import static com.v7878.llvm.Core.LLVMBuildExtractValue;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildStore;
import static com.v7878.llvm.Core.LLVMGetParams;
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
import static com.v7878.unsafe.llvm.LLVMBuilder.local_jobj_to_ptr;
import static com.v7878.unsafe.llvm.LLVMTypes.fn_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int1_t;
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
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMTypeRef;
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

// Compiled by clang with flags: "-O1 -ffreestanding --target=<arch>-linux-android26"
//
// inline uintptr obj_ptr(uintptr obj, uintptr off) {
//     auto ptr = (uint32*)(obj & (~3L));
//     uintptr data = ptr ? *ptr : 0;
//     return data + off;
// }
// TODO: RISCV64
public class ExtraMemoryAccess {
    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class EarlyNative {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        // extern "C" void memset(uintptr obj, uintptr off, uintptr bytes, char value) {
        //     auto dst = (char*)obj_ptr(obj, off);
        //     for (uintptr i = 0; i < bytes; i++) {
        //         dst[i] = value;
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQJiwdIhdJ1CescMcBIhdJ0FUgB8DH2Dx9EAACIDDBI_8ZIOfJ19cM=")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotEJBCLTCQIg-H8dAiLCYXAdQjrIjHJhcB0HA-2VCQUA0wkDDH2kJCQkJCQkJCQiBQxRjnwdfheww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5ogAAtAgBAYtCBADxAxUAOMH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEAAFLjAIi9CAEAgOABMMDkASBS4vz__xoAiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        // extern "C" void memmove(uintptr dst_obj, uintptr dst_off, uintptr src_obj,
        //                         uintptr src_off, uintptr bytes) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (char*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (char*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = bytes; i > 0; i--) {
        //             dst[i - 1] = src[i - 1];
        //         }
        //     } else {
        //         for (uintptr i = 0; i < bytes; i++) {
        //             dst[i] = src[i];
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrA" +
                "jHASAHwSIPi_HQEixLrAjHSSAHKSDnCcyJNhcB0OEyJwQ8fRAAASP_JQg-2d" +
                "AL_Qoh0AP9Jich17esbTYXAdBYxyQ8fQAAPtjQKQIg0CEj_wUk5yHXwww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "U1dWi1QkGIt0JBSLfCQQMc" +
                "C5AAAAAIPn_HQCiw8B8Yt0JByD4vx0AosCAfCLVCQgOchzHIXSdDSJ1pCQkJCQkE4P" +
                "tlwQ_4hcEf-J8nXy6xyF0nQYMfaQkJCQkJCQkJCQD7YcMIgcMUY58nX0Xl9bww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC" +
                "5SfR-8ggBAYtAAABUKQFAuSkBA4s_AQjrQgEAVMQBALQpBQDRCAUA0eoDBKor" +
                "aWQ4hAQA8QtpKjiB__9UBgAAFKQAALQqFUA4hAQA8QoVADih__9UwANf1g==")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wA" +
                "AoOMAAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhCAAAKgAAUOMAiL0IARBB4gEgQu" +
                "IAMNLnADDB5wEAUOL7__8aBQAA6gAAUOMDAAAKATDS5AEwweQBAFDi-___GgCIveg=")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap16(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint16*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint16*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap16(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap16(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrAjHASAHwS" +
                "IPi_HQEixLrAjHSSAHKSDnCcyxmZmZmLg8fhAAAAAAATYXAdD9CD7dMQv5mwcEIZkKJT" +
                "ED-Sf_ITYXAdejrJU2FwHQgMclmLg8fhAAAAAAAD7c0SmbBxghmiTRISP_BSTnIdezD")
        @ASM(conditions = @Conditions(arch = X86), base64 = "V1aLVCQUi3QkEIt8JAwxwLk" +
                "AAAAAg-f8dAKLDwHxi3QkGIPi_HQCiwIB8ItUJBw5yHMghdJ0OpCQkJCQkJCQkA-3dF" +
                "D-ZsHGCGaJdFH-SnXv6x6F0nQaMfaQkJCQkJCQD7c8cGbBxwhmiTxxRjnyde9eX8M=")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5SfR-8g" +
                "gBAYtAAABUKQFAuSkBA4s_AQjrggEAVEQCALQpCQDRCAkA0St5ZHjqAwSqhAQA8WsJwF" +
                "prfRBTC3kqeEH__1QIAAAU5AAAtColQHiEBADxSgnAWkp9EFMKJQB4Yf__VMADX9Y=")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wAAoOMAAJ4VAR" +
                "CA4AMA0uMAwJAVCACb5QMgjOABAFLhDAAAKgAAUOMAiL0IATDg44Awg-ADEIHgAyCC4LIwUuAzP7_m" +
                "Izig4bIwQeABAFDi-f__GgcAAOoAAFDjBQAACrIw0uAzP7_mIzig4bIwweABAFDi-f__GgCIveg=")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap16(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap32(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint32*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint32*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap32(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap32(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrA" +
                "jHASAHwSIPi_HQEixLrAjHSSAHKSDnCcyNNhcB0OEyJwQ8fRAAASP_JQot0g" +
                "vwPzkKJdID8SYnIdezrGk2FwHQVMckPHwCLNIoPzok0iEj_wUk5yHXwww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "V1aLVCQUi3QkEIt8JAwxwL" +
                "kAAAAAg-f8dAKLDwHxi3QkGIPi_HQCiwIB8ItUJBw5yHMehdJ0NonWkJCQkJCQkE6L" +
                "fJD8D8-JfJH8ifJ18eschdJ0GDH2kJCQkJCQkJCQizywD8-JPLFGOfJ1815fww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5Sf" +
                "R-8ggBAYtAAABUKQFAuSkBA4s_AQjrYgEAVAQCALQpEQDRCBEA0St5ZLjqAwSqhA" +
                "QA8WsJwFoLeSq4Yf__VAcAABTEAAC0KkVAuIQEAPFKCcBaCkUAuIH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wAAoOM" +
                "AAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhCQAAKgAAUOMAiL0IBBBB4gQgQuIAMZLnMz" +
                "-_5gAxgecBAFDi-v__GgYAAOoAAFDjBAAACgQwkuQzP7_mBDCB5AEAUOL6__8aAIi96A==")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap32(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap64(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint64*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint64*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap64(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap64(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrAj" +
                "HASAHwSIPi_HQEixLrAjHSSAHKSDnCcyRNhcB0O0yJwQ8fRAAASP_JSot0wvh" +
                "ID85KiXTA-EmJyHXr6xxNhcB0FzHJZpBIizTKSA_OSIk0yEj_wUk5yHXtww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "U1dWi1QkGIt0JBSLfCQQMcC5AAAAAIPn_H" +
                "QCiw8B8Yt0JByD4vx0AosCAfCLVCQgOchzJ4XSdE-J1pCQkJCQkE6LfND4i1zQ_A_LD8-JfNH8iVzR" +
                "-InydefrLIXSdCgx9pCQkJCQkJCQkJCQkJCQkIs88Itc8AQPyw_PiXzxBIkc8UY58nXpXl9bww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5Sf" +
                "R-8ggBAYtAAABUKQFAuSkBA4s_AQjrYgEAVAQCALQpIQDRCCEA0St5ZPjqAwSqhA" +
                "QA8WsNwNoLeSr4Yf__VAcAABTEAAC0KoVA-IQEAPFKDcDaCoUA-IH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "8Egt6RCwjeID4NDjAMC" +
                "g4wAAoOMAAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhDgAAKgAAUOMWAAAKBz" +
                "Dg44Axg-ADEIHgAyCC4NBAwuE1b7_mNH-_5vBgweEIEEHiCCBC4gEAUOL3__8aC" +
                "QAA6gAAUOMHAAAK0EDC4TVvv-Y0f7_m8GDB4QggguIIEIHiAQBQ4vf__xrwiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap64(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

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
        EarlyNative.INSTANCE.memmove_swap16(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapInts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        EarlyNative.INSTANCE.memmove_swap32(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapLongs(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        EarlyNative.INSTANCE.memmove_swap64(destBase, destOffset, srcBase, srcOffset, elements);
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

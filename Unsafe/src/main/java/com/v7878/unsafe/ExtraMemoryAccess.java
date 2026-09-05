package com.v7878.unsafe;

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
import static com.v7878.unsafe.misc.Math.convEndian16;
import static com.v7878.unsafe.misc.Math.convEndian32;
import static com.v7878.unsafe.misc.Math.convEndian64;
import static com.v7878.unsafe.misc.Math.d2l;
import static com.v7878.unsafe.misc.Math.f2i;
import static com.v7878.unsafe.misc.Math.i2f;
import static com.v7878.unsafe.misc.Math.l2d;

import com.v7878.foreign.Arena;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.ASM;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;

import java.util.Optional;

// Compiled by clang with flags: "-O1 -ffreestanding --target=<arch>-linux-android26"
// For aarch64: -mno-outline-atomics -mbranch-protection=none
// For i686: 64-bit atomics use __sync_* builtins (lock cmpxchg8b) to avoid library calls
//
// inline uintptr obj_ptr(uintptr obj, uintptr off) {
//     auto ptr = (uint32*)(obj & (~3L));
//     uintptr data = ptr ? *ptr : 0;
//     return data + off;
// }
// TODO: RISCV64
public class ExtraMemoryAccess {
    public static boolean isInitialized() {
        return ClassUtils.isClassInitialized(Native.class);
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        // extern "C" void memset(uintptr env, uintptr clazz, uintptr obj, uintptr off, uintptr bytes, uint8_t value) {
        //     auto dst = (uint8_t*)obj_ptr(obj, off);
        //     for (uintptr i = 0; i < bytes; i++) {
        //         dst[i] = value;
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQJiwJNhcB1CesdMcBNhcB0FkgByDHJDx9EAABEiAwISP_BSTnIdfTDDx8A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotEJBiLTCQQg-H8dAiLCYXAdQjrIjHJhcB0HA-2VCQcA0wkFDH2kJCQkJCQkJCQiBQxRjnwdfhew5CQkJCQkA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5pAAAtAgBA4uEBADxBRUAOMH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIAJvlAxDS4wAgkRUAIKADAABQ4wCIvQgMEJvlAyCC4AEQwuQBAFDi_P__GgCIveg")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        // extern "C" void memmove(uintptr env, uintptr clazz, uintptr dst_obj, uintptr dst_off, uintptr src_obj,
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
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEiwLrAjHASAHISYPg_HQFQYsI6wIxyUiLVCQITAHJSDnBcytIhdJ0QkiJ1mZmZmZmZi4PH4QAAAAAAEj_zg-2fBH_QIh8EP9IifJ17uscSIXSdBcx9g8fRAAAD7Y8MUCIPDBI_8ZIOfJ18MNmZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi1QkIIt0JByLfCQYMcC5AAAAAIPn_HQCiw8B8Yt0JCSD4vx0AosCAfCLVCQoOchzHIXSdDSJ1pCQkJCQkE4PtlwQ_4hcEf-J8nXy6xyF0nQYMfaQkJCQkJCQkJCQD7YcMIgcMUY58nX0Xl9bww")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5ifR-8ggBA4tAAABUKQFAuSkBBYs_AQjrQgEAVMYBALQpBQDRCAUA0eoDBqoraWY4xgQA8QtpKjiB__9UBgAAFKYAALQqFUA4xgQA8QoVADih__9UwANf1g")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEI4JvlAwDS4wAgoOMAEKDjABCQFQzAm-UDEIHgAwDe4wAgkBUQAJvlDCCC4AEAUuEIAAAqAABQ4wCIvQgBEEHiASBC4gAw0ucAMMHnAQBQ4vv__xoFAADqAABQ4wMAAAoBMNLkATDB5AEAUOL7__8aAIi96A")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap16(uintptr env, uintptr clazz, uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off, uintptr count) {
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
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEiwLrAjHASAHISYPg_HQFQYsI6wIxyUiLVCQITAHJSDnBcyQPH4AAAAAASIXSdD8Pt3RR_mbBxghmiXRQ_kj_ykiF0nXq6ydIhdJ0IjH2ZmZmLg8fhAAAAAAAD7c8cWbBxwhmiTxwSP_GSDnydezDZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLVCQci3QkGIt8JBQxwLkAAAAAg-f8dAKLDwHxi3QkIIPi_HQCiwIB8ItUJCQ5yHMghdJ0OpCQkJCQkJCQkA-3dFD-ZsHGCGaJdFH-SnXv6x6F0nQaMfaQkJCQkJCQD7c8cGbBxwhmiTxxRjnyde9eX8OQkJCQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5ifR-8ggBA4tAAABUKQFAuSkBBYs_AQjrggEAVEYCALQpCQDRCAkA0St5ZnjqAwaqxgQA8WsJwFprfRBTC3kqeEH__1QIAAAU5gAAtColQHjGBADxSgnAWkp9EFMKJQB4Yf__VMADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEI4JvlAwDS4wAgoOMAEKDjABCQFQzAm-UDEIHgAwDe4wAgkBUQAJvlDCCC4AEAUuEMAAAqAABQ4wCIvQgBMODjgDCD4AMQgeADIILgsjBS4DM_v-YjOKDhsjBB4AEAUOL5__8aBwAA6gAAUOMFAAAKsjDS4DM_v-YjOKDhsjDB4AEAUOL5__8aAIi96A")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap16(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap32(uintptr env, uintptr clazz, uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off, uintptr count) {
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
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEiwLrAjHASAHISYPg_HQFQYsI6wIxyUiLVCQITAHJSDnBcytIhdJ0QkiJ1mZmZmZmZi4PH4QAAAAAAEj_zot8kfwPz4l8kPxIifJ17uscSIXSdBcx9g8fRAAAizyxD8-JPLBI_8ZIOfJ18MNmZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLVCQci3QkGIt8JBQxwLkAAAAAg-f8dAKLDwHxi3QkIIPi_HQCiwIB8ItUJCQ5yHMehdJ0NonWkJCQkJCQkE6LfJD8D8-JfJH8ifJ18eschdJ0GDH2kJCQkJCQkJCQizywD8-JPLFGOfJ1815fww")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5ifR-8ggBA4tAAABUKQFAuSkBBYs_AQjrYgEAVAYCALQpEQDRCBEA0St5ZrjqAwaqxgQA8WsJwFoLeSq4Yf__VAcAABTGAAC0KkVAuMYEAPFKCcBaCkUAuIH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEI4JvlAwDS4wAgoOMAEKDjABCQFQzAm-UDEIHgAwDe4wAgkBUQAJvlDCCC4AEAUuEJAAAqAABQ4wCIvQgEEEHiBCBC4gAxkuczP7_mADGB5wEAUOL6__8aBgAA6gAAUOMEAAAKBDCS5DM_v-YEMIHkAQBQ4vr__xoAiL3o")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap32(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap64(uintptr env, uintptr clazz, uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off, uintptr count) {
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
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEiwLrAjHASAHISYPg_HQFQYsI6wIxyUiLVCQITAHJSDnBcy5IhdJ0RUiJ1mZmZmZmZi4PH4QAAAAAAEj_zkiLfNH4SA_PSIl80PhIifJ16-scSIXSdBcx9maQSIs88UgPz0iJPPBI_8ZIOfJ17cNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi1QkIIt0JByLfCQYMcC5AAAAAIPn_HQCiw8B8Yt0JCSD4vx0AosCAfCLVCQoOchzJ4XSdE-J1pCQkJCQkE6LfND4i1zQ_A_LD8-JfNH8iVzR-InydefrLIXSdCgx9pCQkJCQkJCQkJCQkJCQkIs88Itc8AQPyw_PiXzxBIkc8UY58nXpXl9bw5CQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5ifR-8ggBA4tAAABUKQFAuSkBBYs_AQjrYgEAVAYCALQpIQDRCCEA0St5ZvjqAwaqxgQA8WsNwNoLeSr4Yf__VAcAABTGAAC0KoVA-MYEAPFKDcDaCoUA-IH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "8Egt6RCwjeII4JvlAwDS4wAgoOMAEKDjABCQFQzAm-UDEIHgAwDe4wAgkBUQAJvlDCCC4AEAUuEOAAAqAABQ4xYAAAoHMODjgDGD4AMQgeADIILg0EDC4TVvv-Y0f7_m8GDB4QgQQeIIIELiAQBQ4vf__xoJAADqAABQ4wcAAArQQMLhNW-_5jR_v-bwYMHhCCCC4ggQgeIBAFDi9___GvCIveg")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap64(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);


        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        // extern "C" uint8_t load_byte_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwIPtgQIwzHAD7YECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkEItMJAyD4fx0B4sJD7YEAcMxyQ-2BAHDkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD93wjAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANLjAACQFQAAoAMDANDnW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        // extern "C" uint16_t load_short_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwIPtwQIwzHAD7cECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkEItMJAyD4fx0B4sJD7cEAcMxyQ-3BAHDkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD930jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANLjAACQFQAAoAMDAIDgsADQ4Vvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        // extern "C" uint32_t load_int_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQGiwKLBAjDMcCLBAjDZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkEItMJAyD4fx0BosJiwQBwzHJiwQBw5CQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD934jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANLjAACQFQAAoAMDAJDnW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        // extern "C" uint64_t load_long_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_fetch_and_add(ptr, 0)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwJIiwQIwzHASIsECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi3QkHItEJBiD4Px0BIs46wIx_4sEN4tUNwSQkJCJ0YnD8A_HDDd19V5fW8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD938jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANLjAACQFQAAoAMDAIDgnw-w4R_wf_Vb8H_1AIi96A")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD})
        abstract long load_long_atomic(Object base, long offset);

        // extern "C" void store_byte_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwJEhgQIwzHARIYECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7ZEJBSLTCQQi1QkDIPi_HQGixKGBArDMdKGBArDkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwT9nwjAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIAJvlAxDS4wAQkRUAEKADW_B_9QMAwedb8H_1AIi96A")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        // extern "C" void store_short_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQIiwJmRIcECMMxwGZEhwQIw2YuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7dEJBSLTCQQi1QkDIPi_HQHixJmhwQKwzHSZocECsM")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwT9n0jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIAJvlAxDS4wAQkRUAEKADAxCB4Fvwf_WwAMHhW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        // extern "C" void store_int_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwJEhwQIwzHARIcECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkFItMJBCLVCQMg-L8dAaLEocECsMx0ocECsOQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwT9n4jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIAJvlAxDS4wAQkRUAEKADW_B_9QMAgedb8H_1AIi96A")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        // extern "C" void store_long_atomic(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_lock_test_and_set(ptr, value)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQHiwJMhwQIwzHATIcECMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi3QkHItEJBiD4Px0BIs46wIx_4tMJCSLXCQgiwQ3i1Q3BJCQkJCQkJCQkJCQ8A_HDDd1-V5fW8OQkJCQkA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwT9n8jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "MEgt6QiwjeIMEJvlCACb5QMg0uMAIJIVACCgAwMgguBb8H_1n0-y4ZA_ouEAAFPj-___Glvwf_UwiL3o")
        @CallSignature(type = FAST_STATIC, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract void store_long_atomic(Object base, long offset, long value);

        // extern "C" uint8_t atomic_exchange_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQGixKGBArDMdKGBArDZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7ZEJBSLTCQQi1QkDIPi_HQGixKGBArDMdKGBArDkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9XwgE_QkIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIEJvlAyCA4Fvwf_WfD9LhkT_C4QAAU-P7__8aW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_exchange_short(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQHixJmhwQKwzHSZocECsNmDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7dEJBSLTCQQi1QkDIPi_HQHixJmhwQKwzHSZocECsM")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X0gE_QlIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIEJvlAyCA4Fvwf_WfD_LhkT_i4QAAU-P7__8aW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_exchange_int(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQGixKHBArDMdKHBArDZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkFItMJBCLVCQMg-L8dAaLEocECsMx0ocECsOQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X4gE_QmIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIEJvlAyCA4Fvwf_WfD5LhkT-C4QAAU-P7__8aW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_exchange_long(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_lock_test_and_set(ptr, value)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "TInASIPi_HQHixJIhwQKwzHSSIcECsNmDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi3QkHItEJBiD4Px0BIs46wIx_4tMJCSLXCQgiwQ3i1Q3BJCQkJCQkJCQkJCQ8A_HDDd1-V5fW8OQkJCQkA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X8gE_QnIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "MEgt6QiwjeIMUJvlCECb5QMA0uMAAJAVAACgAwMggOBb8H_1nw-y4ZQ_ouEAAFPj-___Glvwf_UwiL3o")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_and_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7YEConGRCDG8EAPsDQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJBSLRCQQg-D8dASLEOsCMdKKZCQYigQKkJCQkJCJwyDj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9XwgJAAQKCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD9LhDDAA4JMfwuEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_and_short(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7cEConGRCHGZvAPsTQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkHItUJBiLRCQUg-D8dASLMOsCMfYPtwQWkJCJxyHPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X0gJAAQKCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD_LhDDAA4JMf4uEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_and_int(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSiwQKkInGRCHG8A-xNAp19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQYi0QkFIPg_HQEixDrAjHSi3QkHIsECpCQkJCJxyH38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X4gJAAQKCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD5LhDDAA4JMfguEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_and_long(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_fetch_and_and(ptr, value)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSSIsECkiJxkwhxvBID7E0CnXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJCCLRCQcg-D8dASLOOsCMf-LdCQkiwQvi1QvBJCQkJCQkJCQkJCQkJCQicMh84nRI0wkKPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X8gJAASKCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADMEgt6QiwjeIMwJvlCOCb5QMwgOBb8H_1nw-z4Q5AAOAMUAHglC-j4QAAUuP5__8aW_B_9TCIveg")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_or_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7YEConGRAjG8EAPsDQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJBSLRCQQg-D8dASLEOsCMdKKZCQYigQKkJCQkJCJwwjj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9XwgJAAQqCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD9LhDDCA4ZMfwuEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_or_short(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7cEConGRAnGZvAPsTQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkHItUJBiLRCQUg-D8dASLMOsCMfYPtwQWkJCJxwnPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X0gJAAQqCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD_LhDDCA4ZMf4uEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_or_int(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSiwQKkInGRAnG8A-xNAp19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQYi0QkFIPg_HQEixDrAjHSi3QkHIsECpCQkJCJxwn38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X4gJAAQqCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD5LhDDCA4ZMfguEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_or_long(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_fetch_and_or(ptr, value)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSSIsECkiJxkwJxvBID7E0CnXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJCCLRCQcg-D8dASLOOsCMf-LdCQkiwQvi1QvBJCQkJCQkJCQkJCQkJCQicMJ84nRC0wkKPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X8gJAASqCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADMEgt6QiwjeIMwJvlCOCb5QMwgOBb8H_1nw-z4Q5AgOEMUIHhlC-j4QAAUuP5__8aW_B_9TCIveg")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_xor_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7YEConGRDDG8EAPsDQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJBSLRCQQg-D8dASLEOsCMdKKZCQYigQKkJCQkJCJwzDj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9XwgJAARKCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD9LhDDAg4JMfwuEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_xor_short(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSD7cEConGRDHGZvAPsTQKdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkHItUJBiLRCQUg-D8dASLMOsCMfYPtwQWkJCJxzHPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X0gJAARKCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD_LhDDAg4JMf4uEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_xor_int(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSiwQKkInGRDHG8A-xNAp19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQYi0QkFIPg_HQEixDrAjHSi3QkHIsECpCQkJCJxzH38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X4gJAARKCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADAEgt6Q2woOEIwJvlAyCA4Fvwf_WfD5LhDDAg4JMfguEAAFHj-v__Glvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_xor_long(uintptr env, uintptr clazz, uintptr obj, uintptr off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_fetch_and_xor(ptr, value)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPi_HQEixLrAjHSSIsECkiJxkwxxvBID7E0CnXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJCCLRCQcg-D8dASLOOsCMf-LdCQkiwQvi1QvBJCQkJCQkJCQkJCQkJCQicMx84nRM0wkKPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X8gJAATKCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDS4wAAkBUAAKADMEgt6QiwjeIMwJvlCOCb5QMwgOBb8H_1nw-z4Q5AIOAMUCHglC-j4QAAUuP5__8aW_B_9TCIveg")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_xor_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_compare_and_exchange_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                                     uint8_t expected, uint8_t desired) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     uint8_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQJixLwRA-wDArDMdLwRA-wDArDDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-2TCQcD7ZEJBiLVCQUi3QkEIPm_HQEizbrAjH28A-wDBZew5CQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8mAAAFQJAUC5AgAAFOkDH6qIHAASKQEDiyD9XwgfAAhrgQAAVCX9CgiK__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAxDS4wAQkRUAEKADAyCB4J8f0uEMAFHhBwAAGgwwm-Vb8H_1kw_C4QAAUOMDAAAKnx_S4QwAUeH5__8KH_B_9XEA7-Zb8H_1AIi96A")
        @CallSignature(type = FAST_STATIC, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        // extern "C" uint16_t atomic_compare_and_exchange_short(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                                       uint16_t expected, uint16_t desired) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     uint16_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQKixJm8EQPsQwKwzHSZvBED7EMCsMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-3TCQcD7dEJBiLVCQUi3QkEIPm_HQEizbrAjH2ZvAPsQwWXsOQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8mAAAFQJAUC5AgAAFOkDH6qIPAASKQEDiyD9X0gfAAhrgQAAVCX9CkiK__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAxDS4wAQkRUAEKADAyCB4J8f8uEMAFHhBwAAGgwwm-Vb8H_1kw_i4QAAUOMDAAAKnx_y4QwAUeH5__8KH_B_9XEA_-Zb8H_1AIi96A")
        @CallSignature(type = FAST_STATIC, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        // extern "C" uint32_t atomic_compare_and_exchange_int(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                                     uint32_t expected, uint32_t desired) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     uint32_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQJixLwRA-xDArDMdLwRA-xDArDDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotMJByLRCQYi1QkFIt0JBCD5vx0BIs26wIx9vAPsQwWXsOQkJCQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X4gfAARrgQAAVAX9CYiJ__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAwDS4wAAkBUAAKADAyCA4J8PkuEMAFDhBwAAGgwwm-Vb8H_1kx-C4QAAUeMDAAAKnw-S4QwAUOH5__8KH_B_9Vvwf_UAiL3o")
        @CallSignature(type = FAST_STATIC, ret = INT, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        // extern "C" uint64_t atomic_compare_and_exchange_long(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                                      uint64_t expected, uint64_t desired) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     uint64_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        // For i686: __sync_val_compare_and_swap(ptr, expected, desired)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "TInASIPi_HQJixLwTA-xDArDMdLwTA-xDArDDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi0wkLItcJCiLVCQki0QkIIt0JByLfCQYg-f8dASLP-sCMf_wD8cMN15fW8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwD9X8gfAATrgQAAVAX9CciJ__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "cEwt6RCwjeIMwJvlCOCb5QMA0uMAAJAVAACgAwMggOCfD7LhDDAh4A5AIOADMJThCgAAGhRQm-UQQJvlW_B_9ZQ_ouEAAFPjBQAACp8PsuEOMCDgDGAh4AYwk-H3__8KH_B_9Vvwf_VwjL3o")
        @CallSignature(type = FAST_STATIC, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        // extern "C" bool atomic_compare_and_set_byte(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                             uint8_t expected, uint8_t desired) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     uint8_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQEixLrAjHS8EQPsAwKD5TAww8fgAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-2TCQcD7ZEJBiLVCQUi3QkEIPm_HQEizbrAjH28A-wDBYPlMBew5CQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8mAAAFQJAUC5AgAAFOkDH6qIHAASKQEDiyr9XwhfAQhroQAAVCX9CgiK__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAwDS4wAAkBUAAKADAyCA4J8P0uEMAFDhCAAAGgwwm-Vb8H_1kx_C4QEAoOMAAFHjBAAACp8P0uEMAFDh-P__Ch_wf_UAAKDjW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        // extern "C" bool atomic_compare_and_set_short(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                              uint16_t expected, uint16_t desired) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     uint16_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQEixLrAjHSZvBED7EMCg-UwMNmDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-3TCQcD7dEJBiLVCQUi3QkEIPm_HQEizbrAjH2ZvAPsQwWD5TAXsOQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8mAAAFQJAUC5AgAAFOkDH6qIPAASKQEDiyr9X0hfAQhroQAAVCX9CkiK__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAwDS4wAAkBUAAKADAyCA4J8P8uEMAFDhCAAAGgwwm-Vb8H_1kx_i4QEAoOMAAFHjBAAACp8P8uEMAFDh-P__Ch_wf_UAAKDjW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        // extern "C" bool atomic_compare_and_set_int(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                            uint32_t expected, uint32_t desired) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     uint32_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "RInASIPi_HQEixLrAjHS8EQPsQwKD5TAww8fgAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotMJByLRCQYi1QkFIt0JBCD5vx0BIs26wIx9vAPsQwWD5TAXsOQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwn9X4g_AQRroQAAVAX9CYiJ__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEIwJvlAwDS4wAAkBUAAKADAyCA4J8PkuEMAFDhCAAAGgwwm-Vb8H_1kx-C4QEAoOMAAFHjBAAACp8PkuEMAFDh-P__Ch_wf_UAAKDjW_B_9QCIveg")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        // extern "C" bool atomic_compare_and_set_long(uintptr env, uintptr clazz, uintptr obj, uintptr off,
        //                                             uint64_t expected, uint64_t desired) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     uint64_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        // For i686: __sync_bool_compare_and_swap(ptr, expected, desired)
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "TInASIPi_HQEixLrAjHS8EwPsQwKD5TAww8fADHAww")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi0wkLItcJCiLVCQki0QkIIt0JByLfCQYg-f8dASLP-sCMf_wD8cMNw-UwF5fW8OQkDHAww")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "SPR-8kAAAFQIAUC5CAEDiwn9X8g_AQTroQAAVAX9CciJ__81IACAUsADX9bgAx8qXz8D1cADX9bgAx8qwANf1g")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "8Egt6RCwjeIMwJvlCBCb5QMA0uMAAJAVAACgAwMggOCfT7LhDAAl4AEwJOAAAJPhCwAAGhRQm-UQQJvlW_B_9ZQ_ouEBAKDjAABT4wYAAAqfb7LhAQAm4AwwJ-ADAJDh9v__Ch_wf_UAAKDjW_B_9fCIvegASC3pDbCg4QAAoOMAiL3o")
        @CallSignature(type = FAST_STATIC, ret = BOOL, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract boolean atomic_compare_and_set_long(Object base, long offset, long expected, long desired);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE,
                Native.class, name -> Optional.empty());
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }
        Native.INSTANCE.memset(base, offset, bytes, value);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }
        Native.INSTANCE.memmove(destBase, destOffset, srcBase, srcOffset, bytes);
    }

    public static void swapShorts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap16(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapInts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap32(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapLongs(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap64(destBase, destOffset, srcBase, srcOffset, elements);
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

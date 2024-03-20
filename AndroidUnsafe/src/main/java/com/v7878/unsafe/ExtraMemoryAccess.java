package com.v7878.unsafe;

import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildCall;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildInBoundsGEP;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
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
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_INDEX_SCALE;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.LinkType.BYTE;
import static com.v7878.unsafe.foreign.BulkLinker.LinkType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.LinkType.OBJECT_AS_RAW_INT;
import static com.v7878.unsafe.foreign.BulkLinker.LinkType.VOID;
import static com.v7878.unsafe.llvm.LLVMGlobals.int16_t;
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
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.Types.LLVMBasicBlockRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.GeneratorSource;
import com.v7878.unsafe.foreign.BulkLinker.SymbolInfo;
import com.v7878.unsafe.llvm.LLVMGlobals;
import com.v7878.unsafe.llvm.LLVMUtils;
import com.v7878.unsafe.llvm.LLVMUtils.Generator;

import java.util.function.Function;
import java.util.function.Supplier;

public class ExtraMemoryAccess {

    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @Keep
        abstract void memset(Object base, long offset, long bytes, byte value);

        private static void generate_memset(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
            LLVMValueRef one = LLVMConstInt(intptr_t(context), 1, false);
            LLVMValueRef zero = LLVMConstNull(intptr_t(context));

            LLVMTypeRef[] arg_types = {int32_t(context), intptr_t(context), intptr_t(context), int8_t(context)};
            LLVMTypeRef type = LLVMFunctionType(void_t(context), arg_types, false);
            LLVMValueRef function = LLVMAddFunction(module, "memset", type);
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
        }

        @SuppressWarnings("SameParameterValue")
        private static void generate_memmove_modify(
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

        @Keep
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        private static void generate_memmove(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
            generate_memmove_modify(context, module, builder, "memmove", int8_t(context), 1, value -> value);
        }

        @Keep
        abstract void memmove_swap_shorts(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        private static void generate_memmove_swap_shorts(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
            LLVMTypeRef[] bswap16_args = {int16_t(context)};
            LLVMTypeRef bswap16_type = LLVMFunctionType(int16_t(context), bswap16_args, false);
            LLVMValueRef bswap16 = LLVMAddFunction(module, "llvm.bswap.i16", bswap16_type);

            generate_memmove_modify(context, module, builder, "memmove_swap_shorts", int16_t(context), 1,
                    value -> LLVMBuildCall(builder, bswap16, new LLVMValueRef[]{value}, ""));
        }

        @Keep
        abstract void memmove_swap_ints(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        private static void generate_memmove_swap_ints(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
            LLVMTypeRef[] bswap32_args = {int32_t(context)};
            LLVMTypeRef bswap32_type = LLVMFunctionType(int32_t(context), bswap32_args, false);
            LLVMValueRef bswap32 = LLVMAddFunction(module, "llvm.bswap.i32", bswap32_type);

            generate_memmove_modify(context, module, builder, "memmove_swap_ints", int32_t(context), 1,
                    value -> LLVMBuildCall(builder, bswap32, new LLVMValueRef[]{value}, ""));
        }

        @Keep
        abstract void memmove_swap_longs(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        private static void generate_memmove_swap_longs(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
            LLVMTypeRef[] bswap64_args = {int64_t(context)};
            LLVMTypeRef bswap64_type = LLVMFunctionType(int64_t(context), bswap64_args, false);
            LLVMValueRef bswap64 = LLVMAddFunction(module, "llvm.bswap.i64", bswap64_type);

            generate_memmove_modify(context, module, builder, "memmove_swap_longs", int64_t(context), 1,
                    value -> LLVMBuildCall(builder, bswap64, new LLVMValueRef[]{value}, ""));
        }

        private static Supplier<byte[]> gen(Generator generator, String name) {
            return () -> {
                try {
                    var buf = LLVMUtils.generateModuleToBuffer(generator);
                    try (var of = LLVMCreateObjectFile(buf)) {
                        return getFunctionCode(of, name).toArray(ValueLayout.JAVA_BYTE);
                    }
                } catch (LLVMException e) {
                    throw shouldNotHappen(e);
                }
            };
        }

        @Keep
        private static final Native INSTANCE = nothrows_run(() -> {
            SymbolInfo[] functions = new SymbolInfo[]{
                    SymbolInfo.of("memset", CRITICAL, GeneratorSource.of(gen(Native::generate_memset, "memset")), VOID, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD, BYTE),
                    SymbolInfo.of("memmove", CRITICAL, GeneratorSource.of(gen(Native::generate_memmove, "memmove")), VOID, OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD),
                    SymbolInfo.of("memmove_swap_shorts", CRITICAL, GeneratorSource.of(gen(Native::generate_memmove_swap_shorts, "memmove_swap_shorts")), VOID, OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD),
                    SymbolInfo.of("memmove_swap_ints", CRITICAL, GeneratorSource.of(gen(Native::generate_memmove_swap_ints, "memmove_swap_ints")), VOID, OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD),
                    SymbolInfo.of("memmove_swap_longs", CRITICAL, GeneratorSource.of(gen(Native::generate_memmove_swap_longs, "memmove_swap_longs")), VOID, OBJECT_AS_RAW_INT, LONG_AS_WORD, OBJECT_AS_RAW_INT, LONG_AS_WORD, LONG_AS_WORD)
            };

            Class<Native> impl = BulkLinker.processSymbols(SCOPE, Native.class, functions);
            return AndroidUnsafe.allocateInstance(impl);
        });
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }

        if (LLVMGlobals.HOST_TARGET != null && Native.INSTANCE != null) {
            Native.INSTANCE.memset(base, offset, bytes, value);
        } else {
            AndroidUnsafe.setMemory(base, offset, bytes, value);
        }
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }

        if (LLVMGlobals.HOST_TARGET != null && Native.INSTANCE != null) {
            Native.INSTANCE.memmove(destBase, destOffset, srcBase, srcOffset, bytes);
        } else {
            AndroidUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    public static void swapShorts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        assert Native.INSTANCE != null;
        Native.INSTANCE.memmove_swap_shorts(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapInts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        assert Native.INSTANCE != null;
        Native.INSTANCE.memmove_swap_ints(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapLongs(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        assert Native.INSTANCE != null;
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

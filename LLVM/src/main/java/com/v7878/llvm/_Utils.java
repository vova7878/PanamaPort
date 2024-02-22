package com.v7878.llvm;

import static com.v7878.llvm.Core.LLVMDisposeMessage;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.foreign.SimpleBulkLinker.WORD_CLASS;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.llvm.Types.AddressValue;

final class _Utils {

    public static final Class<?> VOID_PTR = WORD_CLASS;

    public static final Class<?> CHAR_PTR = ptr(byte.class);
    public static final Class<?> CONST_CHAR_PTR = const_ptr(byte.class);

    public static final Class<?> SIZE_T = WORD_CLASS;

    public static final Class<?> DOUBLE = double.class;
    public static final Class<?> UINT8_T = byte.class;
    public static final Class<?> UINT16_T = short.class;
    public static final Class<?> UINT32_T = int.class;
    public static final Class<?> UINT64_T = long.class;
    public static final Class<?> INT = int.class;
    public static final Class<?> UNSIGNED_INT = int.class;
    public static final Class<?> LONG_LONG = long.class;
    public static final Class<?> UNSIGNED_LONG_LONG = long.class;

    public static final Class<?> ENUM = int.class;

    public static Class<?> ptr(Class<?> ignored) {
        return VOID_PTR;
    }

    public static Class<?> const_ptr(Class<?> ignored) {
        return VOID_PTR;
    }

    public static String addressToString(long address) {
        if (address == 0) return null;
        return MemorySegment.ofAddress(address).reinterpret(Long.MAX_VALUE).getString(0);
    }

    public static String addressToLLVMString(long address) {
        if (address == 0) throw shouldNotReachHere();
        String out = addressToString(address);
        LLVMDisposeMessage(address);
        return out;
    }

    public static String addressToString(long address, long length) {
        if (address == 0) {
            if (length != 0) {
                throw new IllegalArgumentException("null string with non-zero length");
            }
            return null;
        }
        MemorySegment tmp = MemorySegment.ofAddress(address).reinterpret(length);
        return new String(tmp.toArray(ValueLayout.JAVA_BYTE));
    }

    public static MemorySegment allocString(Arena scope, String value) {
        return value == null ? MemorySegment.NULL : scope.allocateFrom(value);
    }

    public static MemorySegment allocArray(Arena scope, AddressValue... values) {
        if (values == null || values.length == 0) {
            return MemorySegment.NULL;
        }
        if (IS64BIT) {
            long[] tmp = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                tmp[i] = values[i].value();
            }
            return scope.allocateFrom(ValueLayout.JAVA_LONG, tmp);
        } else {
            int[] tmp = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                tmp[i] = (int) values[i].value();
            }
            return scope.allocateFrom(ValueLayout.JAVA_INT, tmp);
        }
    }

    public static int arrayLength(AddressValue... values) {
        return values == null ? 0 : values.length;
    }

    public static MemorySegment allocArray(Arena scope, long... values) {
        if (values == null || values.length == 0) {
            return MemorySegment.NULL;
        }
        return scope.allocateFrom(ValueLayout.JAVA_LONG, values);
    }

    public static int arrayLength(long... values) {
        return values == null ? 0 : values.length;
    }
}

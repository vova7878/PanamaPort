package com.v7878.llvm;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.llvm.Core.nLLVMDisposeMessage;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;

import java.lang.reflect.Array;
import java.util.function.LongFunction;

final class _Utils {
    private _Utils() {
    }

    public static String addressToString(long address) {
        if (address == 0) return null;
        return MemorySegment.ofAddress(address).reinterpret(Long.MAX_VALUE).getString(0);
    }

    public static String addressToLLVMString(long address) {
        if (address == 0) throw shouldNotReachHere();
        String out = addressToString(address);
        nLLVMDisposeMessage(address);
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
        return new String(tmp.toArray(JAVA_BYTE));
    }

    public static MemorySegment allocString(Arena scope, String value) {
        class Holder {
            static final Arena SCOPE = Arena.ofAuto();
            static final MemorySegment EMPTY_STRING = SCOPE.allocateFrom("");
        }
        return value == null ? MemorySegment.NULL : (value.isEmpty() ?
                Holder.EMPTY_STRING : scope.allocateFrom(value));
    }

    public static long stringLength(MemorySegment string) {
        return MemorySegment.NULL.equals(string) ? 0 : string.byteSize() - 1;
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
            return scope.allocateFrom(JAVA_LONG, tmp);
        } else {
            int[] tmp = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                tmp[i] = (int) values[i].value();
            }
            return scope.allocateFrom(JAVA_INT, tmp);
        }
    }

    public static int arrayLength(AddressValue... values) {
        return values == null ? 0 : values.length;
    }

    public static MemorySegment allocPointerArray(Arena scope, int size) {
        return scope.allocate(ADDRESS, size);
    }

    public static <R extends AddressValue> R[] readPointerArray(
            MemorySegment array, Class<R> clazz, LongFunction<R> generator) {
        if (IS64BIT) {
            long[] tmp = array.toArray(JAVA_LONG);
            @SuppressWarnings("unchecked")
            R[] out = (R[]) Array.newInstance(clazz, tmp.length);
            for (int i = 0; i < tmp.length; i++) {
                out[i] = generator.apply(tmp[i]);
            }
            return out;
        } else {
            int[] tmp = array.toArray(JAVA_INT);
            @SuppressWarnings("unchecked")
            R[] out = (R[]) Array.newInstance(clazz, tmp.length);
            for (int i = 0; i < tmp.length; i++) {
                out[i] = generator.apply(tmp[i]);
            }
            return out;
        }
    }

    public static int[] readIntArray(long address, int count) {
        if (count == 0) return new int[0];
        if (address == 0) throw shouldNotReachHere();
        var data = MemorySegment.ofAddress(address).reinterpret(JAVA_INT.byteSize() * count);
        return data.toArray(JAVA_INT);
    }

    public static MemorySegment allocArray(Arena scope, long... values) {
        if (values == null || values.length == 0) {
            return MemorySegment.NULL;
        }
        return scope.allocateFrom(JAVA_LONG, values);
    }

    public static int arrayLength(long... values) {
        return values == null ? 0 : values.length;
    }

    public static MemorySegment allocArray(Arena scope, int... values) {
        if (values == null || values.length == 0) {
            return MemorySegment.NULL;
        }
        return scope.allocateFrom(JAVA_INT, values);
    }

    public static int arrayLength(int... values) {
        return values == null ? 0 : values.length;
    }

    public static long getWord(MemorySegment ms, long offset) {
        return IS64BIT ? ms.get(JAVA_LONG, offset) : ms.get(JAVA_INT, offset) & 0xffffffffL;
    }
}

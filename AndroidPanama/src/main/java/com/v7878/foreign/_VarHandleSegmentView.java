package com.v7878.foreign;

import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.shouldNotReachHere;

import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;
import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.unsafe.invoke.VarHandleImpl;
import com.v7878.unsafe.invoke.Wrapper;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

final class _VarHandleSegmentView {

    /**
     * Creates a memory segment view var handle.
     * <p>
     * The resulting var handle will take a memory segment as first argument (the segment to be dereferenced),
     * and a {@code long} as second argument (the offset into the segment).
     * <p>
     * Note: the returned var handle does not perform any size or alignment check. It is up to clients
     * to adapt the returned var handle and insert the appropriate checks.
     *
     * @param carrier       the Java carrier type.
     * @param alignmentMask alignment requirement to be checked upon access. In bytes. Expressed as a mask.
     * @return the created VarHandle.
     */
    public static VarHandle rawMemorySegmentViewHandle(Class<?> carrier, long alignmentMask, boolean swap) {
        if (!carrier.isPrimitive() || carrier == void.class || carrier == boolean.class) {
            throw new IllegalArgumentException("Invalid carrier: " + carrier.getName());
        }

        long min_align_mask = Wrapper.forPrimitiveType(carrier).byteWidth() - 1;
        boolean allowAtomicAccess = (alignmentMask & min_align_mask) == min_align_mask;
        int modesMask = VarHandleImpl.accessModesBitMask(carrier, allowAtomicAccess);

        Class<?> handle_class;
        if (carrier == byte.class) {
            swap = false;
            handle_class = _VarHandleSegmentAsBytes.class;
        } else if (carrier == short.class) {
            handle_class = _VarHandleSegmentAsShorts.class;
        } else if (carrier == char.class) {
            handle_class = _VarHandleSegmentAsChars.class;
        } else if (carrier == int.class) {
            handle_class = _VarHandleSegmentAsInts.class;
        } else if (carrier == float.class) {
            handle_class = _VarHandleSegmentAsFloats.class;
        } else if (carrier == long.class) {
            handle_class = _VarHandleSegmentAsLongs.class;
        } else if (carrier == double.class) {
            handle_class = _VarHandleSegmentAsDoubles.class;
        } else {
            throw shouldNotReachHere();
        }
        var lookup = MethodHandles.lookup();

        String suffix = swap ? "Swap" : "";
        return VarHandleImpl.newVarHandle(modesMask, (mode, type) -> {
            var name = methodName(mode) + suffix;
            return nothrows_run(() -> lookup.findStatic(handle_class, name, type));
        }, carrier, MemorySegment.class, MemoryLayout.class, long.class, long.class);
    }

    private static String methodName(AccessMode mode) {
        return switch (mode) {
            case GET -> "get";
            case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> "getVolatile";
            case SET -> "set";
            case SET_VOLATILE, SET_RELEASE, SET_OPAQUE -> "setVolatile";
            case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE -> "getAndSet";
            case COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE,
                 COMPARE_AND_EXCHANGE_RELEASE -> "compareAndExchange";
            case COMPARE_AND_SET, WEAK_COMPARE_AND_SET_PLAIN, WEAK_COMPARE_AND_SET,
                 WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_RELEASE -> "compareAndSet";
            case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_RELEASE,
                 GET_AND_BITWISE_AND_ACQUIRE -> "compareAndBitwiseAnd";
            case GET_AND_BITWISE_OR, GET_AND_BITWISE_OR_RELEASE,
                 GET_AND_BITWISE_OR_ACQUIRE -> "compareAndBitwiseOr";
            case GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_RELEASE,
                 GET_AND_BITWISE_XOR_ACQUIRE -> "compareAndBitwiseXor";
            case GET_AND_ADD, GET_AND_ADD_RELEASE, GET_AND_ADD_ACQUIRE -> "compareAndAdd";
            //noinspection UnnecessaryDefault
            default -> throw shouldNotReachHere();
        };
    }

    @AlwaysInline
    static void checkSegment(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, boolean ro) {
        Objects.requireNonNull(ms).checkEnclosingLayout(base, encl, ro);
    }

    @AlwaysInline
    static long getOffset(_AbstractMemorySegmentImpl bb, long base, long offset) {
        long segment_base = bb.unsafeGetOffset();
        return segment_base + base + offset;
    }
}

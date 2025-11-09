package com.v7878.foreign;

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
     * Creates a memory segment view var handle accessing a {@code carrier} element.
     * It has access coordinates {@code (MS, ML, long, (validated) long)}.
     * <p>
     * The resulting var handle will take a memory segment as first argument (the segment to be dereferenced),
     * and a {@code long} as third argument (the offset into the segment). Both arguments are checked.
     * The second argument is enclosing layout to perform bound and alignment checks against
     * <p>
     * The resulting var handle will take a pre-validated additional
     * offset as fourth argument, and caller must ensure that passed value is valid.
     *
     * @param carrier       the Java carrier type of the element
     * @param alignmentMask alignment of this accessed element in the enclosing layout
     * @param swap          true if byte order change is needed
     * @return the created var handle
     */
    static VarHandle memorySegmentViewHandle(Class<?> carrier, long alignmentMask, boolean swap) {
        if (!carrier.isPrimitive() || carrier == void.class) {
            throw new IllegalArgumentException("Invalid carrier: " + carrier.getName());
        }

        long min_align_mask = Wrapper.forPrimitiveType(carrier).byteWidth() - 1;
        boolean aligned = (alignmentMask & min_align_mask) == min_align_mask;
        int modesMask = VarHandleImpl.accessModesBitMask(carrier, aligned);

        Class<?> handle_class;
        if (carrier == boolean.class) {
            assert !swap;
            assert aligned;
            handle_class = _VarHandleSegmentAsBooleans.class;
        } else if (carrier == byte.class) {
            assert !swap;
            assert aligned;
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

        // TODO: cache handles
        var lookup = MethodHandles.lookup();
        return VarHandleImpl.newVarHandle(modesMask, (mode, type) -> {
            var name = methodName(mode, swap, aligned);
            return _MhUtil.findStatic(lookup, handle_class, name, type);
        }, carrier, MemorySegment.class, MemoryLayout.class, long.class, long.class);
    }

    private static String methodName(AccessMode mode, boolean swap, boolean aligned) {
        String suffix = swap ? "Swap" : "";
        String suffix2 = aligned ? "Aligned" : "";
        return switch (mode) {
            case GET -> "get" + suffix2;
            case GET_VOLATILE, GET_ACQUIRE, GET_OPAQUE -> "getVolatile";
            case SET -> "set" + suffix2;
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
        } + suffix;
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

package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;
import static com.v7878.unsafe.misc.Math.d2l;
import static com.v7878.unsafe.misc.Math.l2d;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsDoubles {
    @AlwaysInline
    public static double get(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            return l2d(_ScopedMemoryAccess.getLong(ms.sessionImpl(), heap_base, offset), swap);
        } else {
            return _ScopedMemoryAccess.getDoubleUnaligned(ms.sessionImpl(), heap_base, offset, swap);
        }
    }

    @AlwaysInline
    public static double getVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return l2d(_ScopedMemoryAccess.getLongVolatile(ms.sessionImpl(), heap_base, offset), swap);
    }

    @AlwaysInline
    public static void set(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            _ScopedMemoryAccess.putLong(ms.sessionImpl(), heap_base, offset, d2l(value, swap));
        } else {
            _ScopedMemoryAccess.putDoubleUnaligned(ms.sessionImpl(), heap_base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static void setVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putLongVolatile(ms.sessionImpl(), heap_base, offset, d2l(value, swap));
    }

    @AlwaysInline
    public static double getAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return l2d(_ScopedMemoryAccess.getAndSetLong(ms.sessionImpl(), heap_base, offset, d2l(value, swap)), swap);
    }

    @AlwaysInline
    public static double compareAndExchange(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return l2d(_ScopedMemoryAccess.compareAndExchangeLong(ms.sessionImpl(), heap_base, offset, d2l(expected, swap), d2l(desired, swap)), swap);
    }

    @AlwaysInline
    public static boolean compareAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetLong(ms.sessionImpl(), heap_base, offset, d2l(expected, swap), d2l(desired, swap));
    }

    @AlwaysInline
    public static double compareAndAdd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, double value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndAddDoubleWithCAS(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getAligned(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        set(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAligned(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        set(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        setVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        return getAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        return compareAndExchange(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        return compareAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        return compareAndAdd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        set(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        set(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        setVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double getAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        return getAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double compareAndExchangeSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        return compareAndExchange(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double expected, double desired) {
        return compareAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static double compareAndAddSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, double value) {
        return compareAndAdd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

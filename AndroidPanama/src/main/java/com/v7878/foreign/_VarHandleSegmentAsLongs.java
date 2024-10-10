package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;
import static com.v7878.misc.Math.convEndian;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsLongs {
    @AlwaysInline
    public static long get(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getLongUnaligned(ms.sessionImpl(), heap_base, offset, swap);
    }

    @AlwaysInline
    public static long getVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getLongVolatile(ms.sessionImpl(), heap_base, offset), swap);
    }

    @AlwaysInline
    public static void set(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putLongUnaligned(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @AlwaysInline
    public static void setVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putLongVolatile(ms.sessionImpl(), heap_base, offset, convEndian(value, swap));
    }

    @AlwaysInline
    public static long getAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndSetLong(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static long compareAndExchange(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.compareAndExchangeLong(ms.sessionImpl(), heap_base, offset, convEndian(expected, swap), convEndian(desired, swap)), swap);
    }

    @AlwaysInline
    public static boolean compareAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetLong(ms.sessionImpl(), heap_base, offset, convEndian(expected, swap), convEndian(desired, swap));
    }

    @AlwaysInline
    public static long compareAndBitwiseAnd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseAndLong(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static long compareAndBitwiseOr(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseOrLong(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static long compareAndBitwiseXor(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseXorLong(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static long compareAndAdd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, long value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndAddLongWithCAS(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        set(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        setVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return getAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        return compareAndExchange(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        return compareAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseAnd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseOr(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseXor(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndAdd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long getSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long getVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        set(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        setVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long getAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return getAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndExchangeSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        return compareAndExchange(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long expected, long desired) {
        return compareAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseAndSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseAnd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseOrSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseOr(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndBitwiseXorSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndBitwiseXor(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static long compareAndAddSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, long value) {
        return compareAndAdd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}
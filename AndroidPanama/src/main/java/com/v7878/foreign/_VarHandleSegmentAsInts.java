package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;
import static com.v7878.unsafe.misc.Math.convEndian;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsInts {
    @AlwaysInline
    public static int get(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getIntUnaligned(ms.sessionImpl(), heap_base, offset, swap);
    }

    @AlwaysInline
    public static int getVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getIntVolatile(ms.sessionImpl(), heap_base, offset), swap);
    }

    @AlwaysInline
    public static void set(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putIntUnaligned(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @AlwaysInline
    public static void setVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putIntVolatile(ms.sessionImpl(), heap_base, offset, convEndian(value, swap));
    }

    @AlwaysInline
    public static int getAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndSetInt(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static int compareAndExchange(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.compareAndExchangeInt(ms.sessionImpl(), heap_base, offset, convEndian(expected, swap), convEndian(desired, swap)), swap);
    }

    @AlwaysInline
    public static boolean compareAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetInt(ms.sessionImpl(), heap_base, offset, convEndian(expected, swap), convEndian(desired, swap));
    }

    @AlwaysInline
    public static int compareAndBitwiseAnd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseAndInt(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static int compareAndBitwiseOr(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseOrInt(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static int compareAndBitwiseXor(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian(_ScopedMemoryAccess.getAndBitwiseXorInt(ms.sessionImpl(), heap_base, offset, convEndian(value, swap)), swap);
    }

    @AlwaysInline
    public static int compareAndAdd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, int value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndAddIntWithCAS(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        set(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        setVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return getAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        return compareAndExchange(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        return compareAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseAnd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseOr(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseXor(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndAdd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int getSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int getVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        set(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        setVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int getAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return getAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndExchangeSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        return compareAndExchange(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int expected, int desired) {
        return compareAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseAndSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseAnd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseOrSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseOr(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndBitwiseXorSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndBitwiseXor(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static int compareAndAddSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, int value) {
        return compareAndAdd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

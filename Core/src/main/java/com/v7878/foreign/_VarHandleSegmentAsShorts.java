package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;
import static com.v7878.unsafe.misc.Math.convEndian16;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsShorts {
    @AlwaysInline
    public static short get(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            return convEndian16(_ScopedMemoryAccess.getShort(ms.sessionImpl(), heap_base, offset), swap);
        } else {
            return _ScopedMemoryAccess.getShortUnaligned(ms.sessionImpl(), heap_base, offset, swap);
        }
    }

    @AlwaysInline
    public static short getVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.getShortVolatile(ms.sessionImpl(), heap_base, offset), swap);
    }

    @AlwaysInline
    public static void set(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            _ScopedMemoryAccess.putShort(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap));
        } else {
            _ScopedMemoryAccess.putShortUnaligned(ms.sessionImpl(), heap_base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static void setVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putShortVolatile(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap));
    }

    @AlwaysInline
    public static short getAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.getAndSetShort(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap)), swap);
    }

    @AlwaysInline
    public static short compareAndExchange(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.compareAndExchangeShort(ms.sessionImpl(), heap_base, offset, convEndian16(expected, swap), convEndian16(desired, swap)), swap);
    }

    @AlwaysInline
    public static boolean compareAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetShort(ms.sessionImpl(), heap_base, offset, convEndian16(expected, swap), convEndian16(desired, swap));
    }

    @AlwaysInline
    public static short compareAndBitwiseAnd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.getAndBitwiseAndShort(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap)), swap);
    }

    @AlwaysInline
    public static short compareAndBitwiseOr(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.getAndBitwiseOrShort(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap)), swap);
    }

    @AlwaysInline
    public static short compareAndBitwiseXor(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return convEndian16(_ScopedMemoryAccess.getAndBitwiseXorShort(ms.sessionImpl(), heap_base, offset, convEndian16(value, swap)), swap);
    }

    @AlwaysInline
    public static short compareAndAdd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, short value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndAddShortWithCAS(ms.sessionImpl(), heap_base, offset, value, swap);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getAligned(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        set(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAligned(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        set(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        setVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return getAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        return compareAndExchange(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        return compareAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseAnd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseOr(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseXor(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndAdd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        set(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        set(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        setVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short getAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return getAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndExchangeSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        return compareAndExchange(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short expected, short desired) {
        return compareAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseAndSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseAnd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseOrSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseOr(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndBitwiseXorSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndBitwiseXor(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static short compareAndAddSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, short value) {
        return compareAndAdd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;
import static com.v7878.unsafe.misc.Math.convEndian16;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsChars {
    @AlwaysInline
    public static char get(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            return (char) convEndian16(_ScopedMemoryAccess.getShort(ms.sessionImpl(), heap_base, offset), swap);
        } else {
            return _ScopedMemoryAccess.getCharUnaligned(ms.sessionImpl(), heap_base, offset, swap);
        }
    }

    @AlwaysInline
    public static char getVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.getShortVolatile(ms.sessionImpl(), heap_base, offset), swap);
    }

    @AlwaysInline
    public static void set(boolean aligned, boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        if (aligned) {
            _ScopedMemoryAccess.putShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap));
        } else {
            _ScopedMemoryAccess.putCharUnaligned(ms.sessionImpl(), heap_base, offset, value, swap);
        }
    }

    @AlwaysInline
    public static void setVolatile(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putShortVolatile(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap));
    }

    @AlwaysInline
    public static char getAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.getAndSetShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap)), swap);
    }

    @AlwaysInline
    public static char compareAndExchange(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.compareAndExchangeShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) expected, swap), convEndian16((short) desired, swap)), swap);
    }

    @AlwaysInline
    public static boolean compareAndSet(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) expected, swap), convEndian16((short) desired, swap));
    }

    @AlwaysInline
    public static char compareAndBitwiseAnd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.getAndBitwiseAndShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap)), swap);
    }

    @AlwaysInline
    public static char compareAndBitwiseOr(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.getAndBitwiseOrShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap)), swap);
    }

    @AlwaysInline
    public static char compareAndBitwiseXor(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) convEndian16(_ScopedMemoryAccess.getAndBitwiseXorShort(ms.sessionImpl(), heap_base, offset, convEndian16((short) value, swap)), swap);
    }

    @AlwaysInline
    public static char compareAndAdd(boolean swap, _AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, char value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return (char) _ScopedMemoryAccess.getAndAddShortWithCAS(ms.sessionImpl(), heap_base, offset, (short) value, swap);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getAligned(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        set(false, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAligned(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        set(true, false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        setVolatile(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return getAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        return compareAndExchange(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        return compareAndSet(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseAnd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseOr(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseXor(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndAdd(false, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        set(false, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAlignedSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        set(true, true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatileSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        setVolatile(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char getAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return getAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndExchangeSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        return compareAndExchange(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSetSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char expected, char desired) {
        return compareAndSet(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseAndSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseAnd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseOrSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseOr(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndBitwiseXorSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndBitwiseXor(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static char compareAndAddSwap(MemorySegment ms, MemoryLayout encl, long base, long offset, char value) {
        return compareAndAdd(true, (_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsBooleans {
    @AlwaysInline
    public static boolean get(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getBoolean(ms.sessionImpl(), heap_base, offset);
    }

    @AlwaysInline
    public static boolean getVolatile(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getBooleanVolatile(ms.sessionImpl(), heap_base, offset);
    }

    @AlwaysInline
    public static void set(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putBoolean(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static void setVolatile(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putBooleanVolatile(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static boolean getAndSet(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndSetBoolean(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static boolean compareAndExchange(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean expected, boolean desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndExchangeBoolean(ms.sessionImpl(), heap_base, offset, expected, desired);
    }

    @AlwaysInline
    public static boolean compareAndSet(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean expected, boolean desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetBoolean(ms.sessionImpl(), heap_base, offset, expected, desired);
    }

    @AlwaysInline
    public static boolean compareAndBitwiseAnd(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseAndBoolean(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static boolean compareAndBitwiseOr(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseOrBoolean(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static boolean compareAndBitwiseXor(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, boolean value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseXorBoolean(ms.sessionImpl(), heap_base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean get(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get((_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile((_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void set(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        set((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        setVolatile((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        return getAndSet((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean expected, boolean desired) {
        return compareAndExchange((_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean expected, boolean desired) {
        return compareAndSet((_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        return compareAndBitwiseAnd((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        return compareAndBitwiseOr((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, boolean value) {
        return compareAndBitwiseXor((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

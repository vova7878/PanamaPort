package com.v7878.foreign;

import static com.v7878.foreign._VarHandleSegmentView.checkSegment;
import static com.v7878.foreign._VarHandleSegmentView.getOffset;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

@SuppressWarnings("unused")
final class _VarHandleSegmentAsBytes {
    @AlwaysInline
    public static byte get(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getByte(ms.sessionImpl(), heap_base, offset);
    }

    @AlwaysInline
    public static byte getVolatile(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getByteVolatile(ms.sessionImpl(), heap_base, offset);
    }

    @AlwaysInline
    public static void set(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putByte(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static void setVolatile(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        _ScopedMemoryAccess.putByteVolatile(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static byte getAndSet(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndSetByte(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static byte compareAndExchange(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte expected, byte desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndExchangeByte(ms.sessionImpl(), heap_base, offset, expected, desired);
    }

    @AlwaysInline
    public static boolean compareAndSet(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte expected, byte desired) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.compareAndSetByte(ms.sessionImpl(), heap_base, offset, expected, desired);
    }

    @AlwaysInline
    public static byte compareAndBitwiseAnd(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseAndByte(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static byte compareAndBitwiseOr(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseOrByte(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static byte compareAndBitwiseXor(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndBitwiseXorByte(ms.sessionImpl(), heap_base, offset, value);
    }

    @AlwaysInline
    public static byte compareAndAdd(_AbstractMemorySegmentImpl ms, MemoryLayout encl, long base, long offset, byte value) {
        checkSegment(ms, encl, base, true);
        offset = getOffset(ms, base, offset);
        Object heap_base = ms.unsafeGetBase();
        return _ScopedMemoryAccess.getAndAddByteWithCAS(ms.sessionImpl(), heap_base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte getAligned(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return get((_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte getVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset) {
        return getVolatile((_AbstractMemorySegmentImpl) ms, encl, base, offset);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setAligned(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        set((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static void setVolatile(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        setVolatile((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte getAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        return getAndSet((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte compareAndExchange(MemorySegment ms, MemoryLayout encl, long base, long offset, byte expected, byte desired) {
        return compareAndExchange((_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static boolean compareAndSet(MemorySegment ms, MemoryLayout encl, long base, long offset, byte expected, byte desired) {
        return compareAndSet((_AbstractMemorySegmentImpl) ms, encl, base, offset, expected, desired);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte compareAndBitwiseAnd(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        return compareAndBitwiseAnd((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte compareAndBitwiseOr(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        return compareAndBitwiseOr((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte compareAndBitwiseXor(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        return compareAndBitwiseXor((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }

    @DoNotShrink
    @DoNotObfuscate
    public static byte compareAndAdd(MemorySegment ms, MemoryLayout encl, long base, long offset, byte value) {
        return compareAndAdd((_AbstractMemorySegmentImpl) ms, encl, base, offset, value);
    }
}

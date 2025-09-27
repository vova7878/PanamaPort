package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.JavaForeignAccess;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// TODO: refactor?
public class EarlyNativeUtils {
    public static MemorySegment allocString(Arena arena, String value) {
        if (value == null) return MemorySegment.NULL;
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            return arena.allocateFrom(value);
        }

        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        data = Arrays.copyOf(data, data.length + 1);
        MemorySegment out = JavaForeignAccess.allocateNoInit(data.length, 1, arena);
        AndroidUnsafe.copyMemory(data, ARRAY_BYTE_BASE_OFFSET,
                null, out.nativeAddress(), data.length);
        return out;
    }

    public static String segmentToString(MemorySegment segment) {
        if (MemorySegment.NULL.equals(segment)) return null;
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            return segment.reinterpret(Long.MAX_VALUE).getString(0);
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        var base = JavaForeignAccess.unsafeGetBase(segment);
        var offset = JavaForeignAccess.unsafeGetOffset(segment);

        for (int i = 0; ; i++) {
            byte value = AndroidUnsafe.getByte(base, offset + i);
            if (value == 0) break;
            data.write(value);
        }

        return data.toString();
    }

    public static void copy(MemorySegment srcSegment, long srcOffset,
                            MemorySegment dstSegment, long dstOffset, long bytes) {
        if (bytes == 0) {
            return;
        }
        if (ExtraMemoryAccess.isEarlyNativeInitialized()) {
            MemorySegment.copy(srcSegment, srcOffset, dstSegment, dstOffset, bytes);
        } else {
            Object src_base = JavaForeignAccess.unsafeGetBase(srcSegment);
            long src_offset = JavaForeignAccess.unsafeGetOffset(srcSegment) + srcOffset;
            Object dst_base = JavaForeignAccess.unsafeGetBase(dstSegment);
            long dst_offset = JavaForeignAccess.unsafeGetOffset(dstSegment) + dstOffset;
            AndroidUnsafe.copyMemory(src_base, src_offset, dst_base, dst_offset, bytes);
        }
    }
}

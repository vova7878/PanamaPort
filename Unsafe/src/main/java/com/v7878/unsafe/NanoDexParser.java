package com.v7878.unsafe;

import static com.v7878.dex.DexOffsets.FIELD_ID_SIZE;
import static com.v7878.dex.DexOffsets.STRING_ID_SIZE;
import static com.v7878.dex.DexOffsets.TYPE_ID_SIZE;
import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.unsafe.AndroidUnsafe.ADDRESS_SIZE;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.DexFileUtils.DEXFILE_LAYOUT;

import com.v7878.dex.immutable.FieldId;
import com.v7878.dex.immutable.TypeId;
import com.v7878.r8.annotations.AlwaysInline;

@ApiSensitive
public final class NanoDexParser {
    private static final long DEX_DATA_OFFSET = (ART_INDEX >= A9 ? 3L : 1L) * ADDRESS_SIZE;
    private static final long DEX_STRINGS_OFFSET = DEXFILE_LAYOUT.byteOffset(groupElement("string_ids_"));
    private static final long DEX_TYPES_OFFSET = DEXFILE_LAYOUT.byteOffset(groupElement("type_ids_"));
    private static final long DEX_FIELDS_OFFSET = DEXFILE_LAYOUT.byteOffset(groupElement("field_ids_"));

    @AlwaysInline
    private static int read_ubyte(long[] ptr) {
        return AndroidUnsafe.getByteN(ptr[0]++) & 0xff;
    }

    @AlwaysInline
    private static int read_uleb128(long[] ptr) {
        int value = 0;
        int i = 0;
        do {
            int tmp = read_ubyte(ptr);
            value |= (tmp & 0x7f) << (i * 7);
            i++;
            if ((tmp & 0x80) != 0x80) {
                break;
            }
        } while (i < 5);
        return value;
    }

    private static String read_mutf8(long j) {
        var ptr = new long[]{j};
        var chars = new char[read_uleb128(ptr)];

        for (int i = 0; ; i++) {
            int b1 = read_ubyte(ptr);
            if (b1 == '\0') {
                return new String(chars, b1, i);
            }
            if (b1 < 128) {
                chars[i] = (char) b1;
            } else if ((b1 & 0xe0) == 0xc0) {
                int b2 = read_ubyte(ptr);
                chars[i] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f));
            } else {
                int b2 = read_ubyte(ptr);
                int b3 = read_ubyte(ptr);
                chars[i] = (char) (((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f));
            }
        }
    }

    public static String getString(long dexfile, int index) {
        if (dexfile == 0) throw new NullPointerException();
        var string_ids = AndroidUnsafe.getWordN(DEX_STRINGS_OFFSET + dexfile);
        var dex_data = AndroidUnsafe.getWordN(DEX_DATA_OFFSET + dexfile);
        var data_offset = AndroidUnsafe.getIntN(string_ids + ((long) index * STRING_ID_SIZE));
        return read_mutf8(dex_data + data_offset);
    }

    public static TypeId getTypeId(long dexfile, int index) {
        if (dexfile == 0) throw new NullPointerException();
        var type_ids = AndroidUnsafe.getWordN(DEX_TYPES_OFFSET + dexfile);
        var string_id = AndroidUnsafe.getIntN(type_ids + ((long) index * TYPE_ID_SIZE));
        return TypeId.of(getString(dexfile, string_id));
    }

    public static FieldId getFieldId(long dexfile, int index) {
        if (dexfile == 0) throw new NullPointerException();
        var field_ids = AndroidUnsafe.getWordN(dexfile + DEX_FIELDS_OFFSET);
        var field_id = field_ids + ((long) index * FIELD_ID_SIZE);

        var class_idx = AndroidUnsafe.getShortN(field_id) & 0xffff;
        var type_idx = AndroidUnsafe.getShortN(field_id + 2) & 0xffff;
        var name_idx = AndroidUnsafe.getIntN(field_id + 4);
        return FieldId.of(
                getTypeId(dexfile, class_idx),
                getString(dexfile, name_idx),
                getTypeId(dexfile, type_idx)
        );
    }
}

/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// Port-changed: Extensive modifications made throughout the class for Android.

package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;
import static com.v7878.unsafe.ExtraMemoryAccess.SOFT_MAX_ARRAY_LENGTH;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.VM;

import java.nio.charset.Charset;

/**
 * Miscellaneous functions to read and write strings, in various charsets.
 */
final class _StringSupport {

    private _StringSupport() {
    }

    private static final long LONG_MASK = ~7L; // The last three bits are zero

    public static String read(_AbstractMemorySegmentImpl segment, long offset, Charset charset) {
        return switch (CharsetKind.of(charset)) {
            case SINGLE_BYTE -> readByte(segment, offset, charset);
            case DOUBLE_BYTE -> readShort(segment, offset, charset);
            case QUAD_BYTE -> readInt(segment, offset, charset);
            //noinspection UnnecessaryDefault
            default -> throw shouldNotReachHere();
        };
    }

    public static void write(_AbstractMemorySegmentImpl segment, long offset, Charset charset, String string) {
        switch (CharsetKind.of(charset)) {
            case SINGLE_BYTE -> writeByte(segment, offset, charset, string);
            case DOUBLE_BYTE -> writeShort(segment, offset, charset, string);
            case QUAD_BYTE -> writeInt(segment, offset, charset, string);
            default -> throw shouldNotReachHere();
        }
    }

    private static String readByte(_AbstractMemorySegmentImpl segment, long offset, Charset charset) {
        final int len = strlenByte(segment, offset, segment.byteSize());
        final byte[] bytes = new byte[len];
        MemorySegment.copy(segment, JAVA_BYTE, offset, bytes, 0, len);
        return new String(bytes, charset);
    }

    private static void writeByte(_AbstractMemorySegmentImpl segment, long offset, Charset charset, String string) {
        int bytes = copyBytes(string, segment, charset, offset);
        segment.set(JAVA_BYTE, offset + bytes, (byte) 0);
    }

    private static String readShort(_AbstractMemorySegmentImpl segment, long offset, Charset charset) {
        int len = strlenShort(segment, offset, segment.byteSize());
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, JAVA_BYTE, offset, bytes, 0, len);
        return new String(bytes, charset);
    }

    private static void writeShort(_AbstractMemorySegmentImpl segment, long offset, Charset charset, String string) {
        int bytes = copyBytes(string, segment, charset, offset);
        segment.set(JAVA_SHORT_UNALIGNED, offset + bytes, (short) 0);
    }

    private static String readInt(_AbstractMemorySegmentImpl segment, long offset, Charset charset) {
        int len = strlenInt(segment, offset, segment.byteSize());
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, JAVA_BYTE, offset, bytes, 0, len);
        return new String(bytes, charset);
    }

    private static void writeInt(_AbstractMemorySegmentImpl segment, long offset, Charset charset, String string) {
        int bytes = copyBytes(string, segment, charset, offset);
        segment.set(JAVA_INT_UNALIGNED, offset + bytes, 0);
    }

    /**
     * {@return the index of the first zero byte beginning at the provided
     * {@code fromOffset} to the encountering of a zero byte in the provided
     * {@code segment} checking bytes before the {@code toOffset}}
     * <p>
     * The method is using a heuristic method to determine if a long word contains a
     * zero byte. The method might have false positives but never false negatives.
     * <p>
     * This method is inspired by the `glibc/string/strlen.c` implementation
     *
     * @param segment    to examine
     * @param fromOffset from where examination shall begin (inclusive)
     * @param toOffset   to where examination shall end (exclusive)
     * @throws IllegalArgumentException if the examined region contains no zero bytes
     *                                  within a length that can be accepted by a String
     */
    @DoNotShrink // TODO: DoNotInline
    public static int strlenByte(_AbstractMemorySegmentImpl segment,
                                 long fromOffset, long toOffset) {
        final long length = toOffset - fromOffset;
        segment.checkBounds(fromOffset, length);
        if (length < Byte.BYTES) {
            // There can be no null terminator present
            segment.scope.checkValidState();
            throw nullNotFound(segment, fromOffset, toOffset);
        }

        // TODO: use aligned memory access
        try (var ignored = _ScopedMemoryAccess.lock(segment.sessionImpl())) {
            var u_base = segment.unsafeGetBase();
            var u_offset = segment.unsafeGetOffset();

            final long longBytes = length & LONG_MASK;
            final long longLimit = fromOffset + longBytes;
            long offset = fromOffset;
            for (; offset < longLimit; offset += Long.BYTES) {
                long val = _ScopedMemoryAccess.getLongUnaligned(null, u_base, u_offset + offset, false);
                if (mightContainZeroByte(val)) {
                    for (int j = 0; j < Long.BYTES; j++) {
                        if ((val & 0xffL) == 0L) {
                            return requireWithinStringSize(offset + j - fromOffset, segment, fromOffset, toOffset);
                        }
                        val >>>= 8;
                    }
                }
            }
            // Handle the tail
            for (; offset < toOffset; offset++) {
                byte val = _ScopedMemoryAccess.getByte(null, u_base, u_offset + offset);
                if (val == 0) {
                    return requireWithinStringSize(offset - fromOffset, segment, fromOffset, toOffset);
                }
            }
        }
        throw nullNotFound(segment, fromOffset, toOffset);
    }

    @DoNotShrink // TODO: DoNotInline
    public static int strlenShort(_AbstractMemorySegmentImpl segment,
                                  long fromOffset, long toOffset) {
        final long length = toOffset - fromOffset;
        segment.checkBounds(fromOffset, length);
        if (length < Short.BYTES) {
            // There can be no null terminator present
            segment.scope.checkValidState();
            throw nullNotFound(segment, fromOffset, toOffset);
        }

        // TODO: use aligned memory access
        try (var ignored = _ScopedMemoryAccess.lock(segment.sessionImpl())) {
            var u_base = segment.unsafeGetBase();
            var u_offset = segment.unsafeGetOffset();

            final long longBytes = length & LONG_MASK;
            final long longLimit = fromOffset + longBytes;
            long offset = fromOffset;
            for (; offset < longLimit; offset += Long.BYTES) {
                long val = _ScopedMemoryAccess.getLongUnaligned(null, u_base, u_offset + offset, false);
                if (mightContainZeroShort(val)) {
                    for (int j = 0; j < Long.BYTES; j += Short.BYTES) {
                        if ((val & 0xffffL) == 0L) {
                            return requireWithinStringSize(offset + j - fromOffset, segment, fromOffset, toOffset);
                        }
                        val >>>= 16;
                    }
                }
            }
            // Handle the tail
            // Prevent over scanning as we step by 2
            final long endScan = toOffset & ~1; // The last bit is zero
            for (; offset < endScan; offset += Short.BYTES) {
                short val = _ScopedMemoryAccess.getShortUnaligned(null, u_base, u_offset + offset, false);
                if (val == 0) {
                    return requireWithinStringSize(offset - fromOffset, segment, fromOffset, toOffset);
                }
            }
        }
        throw nullNotFound(segment, fromOffset, toOffset);
    }

    @DoNotShrink // TODO: DoNotInline
    public static int strlenInt(_AbstractMemorySegmentImpl segment,
                                long fromOffset, long toOffset) {
        final long length = toOffset - fromOffset;
        segment.checkBounds(fromOffset, length);
        if (length < Integer.BYTES) {
            // There can be no null terminator present
            segment.scope.checkValidState();
            throw nullNotFound(segment, fromOffset, toOffset);
        }

        // TODO: use aligned memory access
        try (var ignored = _ScopedMemoryAccess.lock(segment.sessionImpl())) {
            var u_base = segment.unsafeGetBase();
            var u_offset = segment.unsafeGetOffset();

            final long longBytes = length & LONG_MASK;
            final long longLimit = fromOffset + longBytes;
            long offset = fromOffset;
            for (; offset < longLimit; offset += Long.BYTES) {
                long val = _ScopedMemoryAccess.getLongUnaligned(null, u_base, u_offset + offset, false);
                if (mightContainZeroInt(val)) {
                    for (int j = 0; j < Long.BYTES; j += Integer.BYTES) {
                        if ((val & 0xffffffffL) == 0L) {
                            return requireWithinStringSize(offset + j - fromOffset, segment, fromOffset, toOffset);
                        }
                        val >>>= 32;
                    }
                }
            }
            // Handle the tail
            // Prevent over scanning as we step by 4
            final long endScan = toOffset & ~3; // The last two bit are zero
            for (; offset < endScan; offset += Integer.BYTES) {
                int val = _ScopedMemoryAccess.getIntUnaligned(null, u_base, u_offset + offset, false);
                if (val == 0) {
                    return requireWithinStringSize(offset - fromOffset, segment, fromOffset, toOffset);
                }
            }
        }
        throw nullNotFound(segment, fromOffset, toOffset);
    }

    /*
    Bits 63 and N * 8 (N = 1..7) of this number are zero.  Call these bits
    the "holes".  Note that there is a hole just to the left of
    each byte, with an extra at the end:

    bits:  01111110 11111110 11111110 11111110 11111110 11111110 11111110 11111111
    bytes: AAAAAAAA BBBBBBBB CCCCCCCC DDDDDDDD EEEEEEEE FFFFFFFF GGGGGGGG HHHHHHHH

    The 1-bits make sure that carries propagate to the next 0-bit.
    The 0-bits provide holes for carries to fall into.
    */
    private static final long HIMAGIC_FOR_BYTES = 0x8080_8080_8080_8080L;
    private static final long LOMAGIC_FOR_BYTES = 0x0101_0101_0101_0101L;

    private static boolean mightContainZeroByte(long l) {
        return ((l - LOMAGIC_FOR_BYTES) & (~l) & HIMAGIC_FOR_BYTES) != 0;
    }

    private static final long HIMAGIC_FOR_SHORTS = 0x8000_8000_8000_8000L;
    private static final long LOMAGIC_FOR_SHORTS = 0x0001_0001_0001_0001L;

    private static boolean mightContainZeroShort(long l) {
        return ((l - LOMAGIC_FOR_SHORTS) & (~l) & HIMAGIC_FOR_SHORTS) != 0;
    }

    private static final long HIMAGIC_FOR_INTS = 0x8000_0000_8000_0000L;
    private static final long LOMAGIC_FOR_INTS = 0x0000_0001_0000_0001L;

    private static boolean mightContainZeroInt(long l) {
        return ((l - LOMAGIC_FOR_INTS) & (~l) & HIMAGIC_FOR_INTS) != 0;
    }

    private static int requireWithinStringSize(long size, _AbstractMemorySegmentImpl segment,
                                               long fromOffset, long toOffset) {
        if (size > SOFT_MAX_ARRAY_LENGTH) {
            throw stringTooLarge(segment, fromOffset, toOffset);
        }
        return (int) size;
    }

    private static IllegalArgumentException stringTooLarge(_AbstractMemorySegmentImpl segment,
                                                           long fromOffset, long toOffset) {
        return new IllegalArgumentException("String too large: " + exceptionInfo(segment, fromOffset, toOffset));
    }

    private static IndexOutOfBoundsException nullNotFound(_AbstractMemorySegmentImpl segment,
                                                          long fromOffset, long toOffset) {
        return new IndexOutOfBoundsException("No null terminator found: " + exceptionInfo(segment, fromOffset, toOffset));
    }

    private static String exceptionInfo(_AbstractMemorySegmentImpl segment,
                                        long fromOffset, long toOffset) {
        return segment + " using region [" + fromOffset + ", " + toOffset + ")";
    }

    public enum CharsetKind {
        SINGLE_BYTE(1),
        DOUBLE_BYTE(2),
        QUAD_BYTE(4);

        private static final Charset UTF_32LE = Charset.forName("UTF-32LE");
        private static final Charset UTF_32BE = Charset.forName("UTF-32BE");
        private static final Charset UTF_32 = Charset.forName("UTF-32");

        final int terminatorCharSize;

        CharsetKind(int terminatorCharSize) {
            this.terminatorCharSize = terminatorCharSize;
        }

        public int terminatorCharSize() {
            return terminatorCharSize;
        }

        public static CharsetKind of(Charset charset) {
            if (UTF_8.equals(charset) ||
                    ISO_8859_1.equals(charset) ||
                    US_ASCII.equals(charset)) {
                return SINGLE_BYTE;
            } else if (UTF_16LE.equals(charset) ||
                    UTF_16BE.equals(charset) ||
                    UTF_16.equals(charset)) {
                return DOUBLE_BYTE;
            } else if (UTF_32LE.equals(charset) ||
                    UTF_32BE.equals(charset) ||
                    UTF_32.equals(charset)) {
                return QUAD_BYTE;
            } else {
                throw new IllegalArgumentException("Unsupported charset: " + charset);
            }
        }
    }

    public static boolean bytesCompatible(String string, Charset charset) {
        // Port-changed
        //if (string.isLatin1()) {
        //    if (charset == ISO_8859_1.INSTANCE) {
        //        return true; // ok, same encoding
        //    } else if (charset == UTF_8.INSTANCE || charset == US_ASCII.INSTANCE) {
        //        byte[] value = string.value;
        //        return !StringCoding.hasNegatives(value, 0, value.length); // ok, if ASCII-compatible
        //    }
        //}
        //return false;

        // On Android, compressed strings only contain characters 0x01-0x7f
        return VM.isCompressedString(string) && (charset == ISO_8859_1 || charset == UTF_8 || charset == US_ASCII);
    }

    public static int copyBytes(String string, MemorySegment segment, Charset charset, long offset) {
        if (bytesCompatible(string, charset)) {
            copyToSegmentRaw(string, segment, offset);
            return string.length();
        } else {
            byte[] bytes = string.getBytes(charset);
            MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, offset, bytes.length);
            return bytes.length;
        }
    }

    public static void copyToSegmentRaw(String string, MemorySegment segment, long offset) {
        // Port-changed
        MemorySegment.copy(_SegmentFactories.fromObject(string), VM.STRING_HEADER_SIZE,
                segment, offset, VM.stringDataSize(string));
    }
}

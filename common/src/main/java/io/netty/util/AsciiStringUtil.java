/*
 * Copyright 2024 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SWARUtil;

/**
 * A collection of utility methods that is related with handling {@link AsciiString}.
 */
final class AsciiStringUtil {

    /**
     * Convert the {@link AsciiString} to a lower case.
     *
     * @param string the {@link AsciiString} to convert
     * @return the new {@link AsciiString} in lower case
     */
    static AsciiString toLowerCase(final AsciiString string) {
        final byte[] byteArray = string.array();
        final int offset = string.arrayOffset();
        final int length = string.length();
        if (!containsUpperCase(byteArray, offset, length)) {
            return string;
        }
        final byte[] newByteArray = PlatformDependent.allocateUninitializedArray(length);
        toLowerCase(byteArray, offset, newByteArray);
        return new AsciiString(newByteArray, false);
    }

    private static boolean containsUpperCase(final byte[] byteArray, int offset, final int length) {
        if (!PlatformDependent.isUnaligned()) {
            return linearContainsUpperCase(byteArray, offset, length);
        }

        final int longCount = length >>> 3;
        for (int i = 0; i < longCount; ++i) {
            final long word = PlatformDependent.getLong(byteArray, offset);
            if (SWARUtil.containsUpperCase(word)) {
                return true;
            }
            offset += Long.BYTES;
        }
        return unrolledContainsUpperCase(byteArray, offset, length & 7);
    }

    private static boolean linearContainsUpperCase(final byte[] byteArray, final int offset, final int length) {
        final int end = offset + length;
        for (int idx = offset; idx < end; ++idx) {
            if (isUpperCase(byteArray[idx])) {
                return true;
            }
        }
        return false;
    }

    private static boolean unrolledContainsUpperCase(final byte[] byteArray, int offset, final int byteCount) {
        assert byteCount >= 0 && byteCount < 8;
        if ((byteCount & Integer.BYTES) != 0) {
            final int word = PlatformDependent.getInt(byteArray, offset);
            if (SWARUtil.containsUpperCase(word)) {
                return true;
            }
            offset += Integer.BYTES;
        }
        if ((byteCount & Short.BYTES) != 0) {
            if (isUpperCase(PlatformDependent.getByte(byteArray, offset))) {
                return true;
            }
            if (isUpperCase(PlatformDependent.getByte(byteArray, offset + 1))) {
                return true;
            }
            offset += Short.BYTES;
        }
        if ((byteCount & Byte.BYTES) != 0) {
            return isUpperCase(PlatformDependent.getByte(byteArray, offset));
        }
        return false;
    }

    private static void toLowerCase(final byte[] src, final int srcOffset, final byte[] dst) {
        if (!PlatformDependent.isUnaligned()) {
            linearToLowerCase(src, srcOffset, dst);
            return;
        }

        final int length = dst.length;
        final int longCount = length >>> 3;
        int offset = 0;
        for (int i = 0; i < longCount; ++i) {
            final long word = PlatformDependent.getLong(src, srcOffset + offset);
            PlatformDependent.putLong(dst, offset, SWARUtil.toLowerCase(word));
            offset += Long.BYTES;
        }
        unrolledToLowerCase(src, srcOffset + offset, dst, offset, length & 7);
    }

    private static void linearToLowerCase(final byte[] src, final int srcOffset, final byte[] dst) {
        for (int i = 0; i < dst.length; ++i) {
            dst[i] = toLowerCase(src[srcOffset + i]);
        }
    }

    private static void unrolledToLowerCase(final byte[] src, int srcPos,
                                            final byte[] dst, int dstOffset, final int byteCount) {
        assert byteCount >= 0 && byteCount < 8;
        int offset = 0;
        if ((byteCount & Integer.BYTES) != 0) {
            final int word = PlatformDependent.getInt(src, srcPos + offset);
            PlatformDependent.putInt(dst, dstOffset + offset, SWARUtil.toLowerCase(word));
            offset += Integer.BYTES;
        }

        if ((byteCount & Short.BYTES) != 0) {
            final short word = PlatformDependent.getShort(src, srcPos + offset);
            final short result = (short) ((toLowerCase((byte) (word >>> 8)) << 8) | toLowerCase((byte) word));
            PlatformDependent.putShort(dst, dstOffset + offset, result);
            offset += Short.BYTES;
        }

        // this is equivalent to byteCount >= Byte.BYTES (i.e. whether byteCount is odd)
        if ((byteCount & Byte.BYTES) != 0) {
            PlatformDependent.putByte(dst, dstOffset + offset,
                                      toLowerCase(PlatformDependent.getByte(src, srcPos + offset)));
        }
    }

    /**
     * Convert the {@link AsciiString} to a upper case.
     *
     * @param string the {@link AsciiString} to convert
     * @return the {@link AsciiString} in upper case
     */
    static AsciiString toUpperCase(final AsciiString string) {
        final byte[] byteArray = string.array();
        final int offset = string.arrayOffset();
        final int length = string.length();
        if (!containsLowerCase(byteArray, offset, length)) {
            return string;
        }
        final byte[] newByteArray = PlatformDependent.allocateUninitializedArray(length);
        toUpperCase(byteArray, offset, newByteArray);
        return new AsciiString(newByteArray, false);
    }

    private static boolean containsLowerCase(final byte[] byteArray, int offset, final int length) {
        if (!PlatformDependent.isUnaligned()) {
            return linearContainsLowerCase(byteArray, offset, length);
        }

        final int longCount = length >>> 3;
        for (int i = 0; i < longCount; ++i) {
            final long word = PlatformDependent.getLong(byteArray, offset);
            if (SWARUtil.containsLowerCase(word)) {
                return true;
            }
            offset += Long.BYTES;
        }
        return unrolledContainsLowerCase(byteArray, offset, length & 7);
    }

    private static boolean linearContainsLowerCase(final byte[] byteArray, final int offset, final int length) {
        final int end = offset + length;
        for (int idx = offset; idx < end; ++idx) {
            if (isLowerCase(byteArray[idx])) {
                return true;
            }
        }
        return false;
    }

    private static boolean unrolledContainsLowerCase(final byte[] byteArray, int offset, final int byteCount) {
        assert byteCount >= 0 && byteCount < 8;
        if ((byteCount & Integer.BYTES) != 0) {
            final int word = PlatformDependent.getInt(byteArray, offset);
            if (SWARUtil.containsLowerCase(word)) {
                return true;
            }
            offset += Integer.BYTES;
        }
        if ((byteCount & Short.BYTES) != 0) {
            if (isLowerCase(PlatformDependent.getByte(byteArray, offset))) {
                return true;
            }
            if (isLowerCase(PlatformDependent.getByte(byteArray, offset + 1))) {
                return true;
            }
            offset += Short.BYTES;
        }
        if ((byteCount & Byte.BYTES) != 0) {
            return isLowerCase(PlatformDependent.getByte(byteArray, offset));
        }
        return false;
    }

    private static void toUpperCase(final byte[] src, final int srcOffset, final byte[] dst) {
        if (!PlatformDependent.isUnaligned()) {
            linearToUpperCase(src, srcOffset, dst);
            return;
        }

        final int length = dst.length;
        final int longCount = length >>> 3;
        int offset = 0;
        for (int i = 0; i < longCount; ++i) {
            final long word = PlatformDependent.getLong(src, srcOffset + offset);
            PlatformDependent.putLong(dst, offset, SWARUtil.toUpperCase(word));
            offset += Long.BYTES;
        }
        unrolledToUpperCase(src, srcOffset + offset, dst, offset, length & 7);
    }

    private static void linearToUpperCase(final byte[] src, final int srcOffset, final byte[] dst) {
        for (int i = 0; i < dst.length; ++i) {
            dst[i] = toUpperCase(src[srcOffset + i]);
        }
    }

    private static void unrolledToUpperCase(final byte[] src, int srcOffset,
                                            final byte[] dst, int dstOffset, final int byteCount) {
        assert byteCount >= 0 && byteCount < 8;
        int offset = 0;
        if ((byteCount & Integer.BYTES) != 0) {
            final int word = PlatformDependent.getInt(src, srcOffset + offset);
            PlatformDependent.putInt(dst, dstOffset + offset, SWARUtil.toUpperCase(word));
            offset += Integer.BYTES;
        }
        if ((byteCount & Short.BYTES) != 0) {
            final short word = PlatformDependent.getShort(src, srcOffset + offset);
            final short result = (short) ((toUpperCase((byte) (word >>> 8)) << 8) | toUpperCase((byte) word));
            PlatformDependent.putShort(dst, dstOffset + offset, result);
            offset += Short.BYTES;
        }

        if ((byteCount & Byte.BYTES) != 0) {
            PlatformDependent.putByte(dst, dstOffset + offset,
                                      toUpperCase(PlatformDependent.getByte(src, srcOffset + offset)));
        }
    }

    static boolean contentEqualsIgnoreCase(final AsciiString lhs, final AsciiString rhs) {
        assert lhs.length() == rhs.length();
        if (!PlatformDependent.isUnaligned()) {
            return linearContentEqualsIgnoreCase(lhs, rhs);
        }
        final byte[] lhsArray = lhs.array();
        final int lhsOffset = lhs.arrayOffset();
        final byte[] rhsArray = rhs.array();
        final int rhsOffset = rhs.arrayOffset();
        final int lenght = lhs.length();
        final int byteCount = lenght & 7;
        final int longCount = lenght >>> 3;
        int offset = 0;
        for (; offset < longCount; offset += Long.BYTES) {
            final long lhsWord = PlatformDependent.getLong(lhsArray, lhsOffset + offset);
            final long rhsWord = PlatformDependent.getLong(rhsArray, rhsOffset + offset);
            if (!equalsIgnoreCase(lhsWord, rhsWord)) {
                return false;
            }
        }
        return unrolledContentEqualsIgnoreCase(lhsArray, lhsOffset + offset,
                                               rhsArray, rhsOffset + offset, byteCount);
    }

    private static boolean linearContentEqualsIgnoreCase(final AsciiString lhs, final AsciiString rhs) {
        final byte[] lhsArray = lhs.array();
        final int lhsOffset = lhs.arrayOffset();
        final int lhsLength = lhs.length();
        final int rhsOffset = rhs.arrayOffset();
        final int rhsLength = rhs.length();
        final byte[] rhsArray = rhs.array();
        if (lhsOffset == 0 && rhsOffset == 0 && lhsLength == rhsLength) {
            for (int i = 0; i < lhsLength; ++i) {
                if (!equalsIgnoreCase(lhsArray[i], rhsArray[i])) {
                    return false;
                }
            }
            return true;
        }
        return misalginedEqualsIgnoreCase(lhsArray, lhsOffset, rhsArray, rhsOffset, lhsLength);
    }

    private static boolean misalginedEqualsIgnoreCase(final byte[] lhsArray, final int lhsOffset,
                                                      final byte[] rhsArray, final int rhsOffset, final int length) {
        for (int i = lhsOffset, j = rhsOffset, end = lhsOffset + length; i < end; ++i, ++j) {
            if (!equalsIgnoreCase(lhsArray[i], rhsArray[j])) {
                return false;
            }
        }
        return true;
    }

    private static boolean unrolledContentEqualsIgnoreCase(final byte[] lhsArray, int lhsOffset,
                                                           final byte[] rhsArray, int rhsOffset, final int byteCount) {
        assert byteCount >= 0 && byteCount < 8;
        switch (byteCount) {
        case 1:
            return equalsIgnoreCase(PlatformDependent.getByte(rhsArray, lhsOffset),
                                    PlatformDependent.getByte(rhsArray, rhsOffset));
        case 2:
            final short lWord = PlatformDependent.getShort(lhsArray, lhsOffset);
            final short rWord = PlatformDependent.getShort(rhsArray, rhsOffset);
            if (equalsIgnoreCase((byte) (lWord >>> 8), (byte) (rWord >>> 8))) {
                return equalsIgnoreCase((byte) lWord, (byte) rWord);
            }
            return false;
        case 3:
            if (equalsIgnoreCase(PlatformDependent.getByte(lhsArray, lhsOffset),
                                 PlatformDependent.getByte(rhsArray, rhsOffset))) {
                final short lWord2 = PlatformDependent.getShort(lhsArray, lhsOffset + 1);
                final short rWord2 = PlatformDependent.getShort(rhsArray, rhsOffset + 1);
                return equalsIgnoreCase((byte) lWord2, (byte) rWord2);
            }
            return false;
        case 4:
            return equalsIgnoreCase(PlatformDependent.getInt(lhsArray, lhsOffset),
                                    PlatformDependent.getInt(rhsArray, rhsOffset));
        case 5:
            if (equalsIgnoreCase(PlatformDependent.getByte(lhsArray, lhsOffset),
                                 PlatformDependent.getByte(rhsArray, rhsOffset))) {
                return equalsIgnoreCase(PlatformDependent.getInt(lhsArray, lhsOffset + 1),
                                        PlatformDependent.getInt(rhsArray, rhsOffset + 1));
            }
            return false;

        case 6:
            final short lWord3 = PlatformDependent.getShort(lhsArray, lhsOffset);
            final short rWord3 = PlatformDependent.getShort(rhsArray, rhsOffset);
            if (equalsIgnoreCase((byte) (lWord3 >>> 8), (byte) (rWord3 >>> 8)) &&
                equalsIgnoreCase((byte) lWord3, (byte) rWord3)) {
                return equalsIgnoreCase(PlatformDependent.getInt(lhsArray, lhsOffset + 2),
                                        PlatformDependent.getInt(rhsArray, rhsOffset + 2));
            }
            return false;
        case 7:
            long lWord4 = PlatformDependent.getInt(lhsArray, lhsOffset);
            long rWord4 = PlatformDependent.getInt(rhsArray, rhsOffset);
            lWord4 |= (long) PlatformDependent.getInt(lhsArray, lhsOffset + 3) << 32;
            rWord4 |= (long) PlatformDependent.getInt(rhsArray, rhsOffset + 3) << 32;
            return equalsIgnoreCase(lWord4, rWord4);
        }
        return true;
    }

    private static boolean isLowerCase(final byte value) {
        return value >= 'a' && value <= 'z';
    }

    /**
     * Check if the given byte is upper case.
     *
     * @param value the byte to check
     * @return {@code true} if the byte is upper case, {@code false} otherwise.
     */
    static boolean isUpperCase(final byte value) {
        return value >= 'A' && value <= 'Z';
    }

    /**
     * Convert the given byte to lower case.
     *
     * @param value the byte to convert
     * @return the lower case byte
     */
    static byte toLowerCase(final byte value) {
        return isUpperCase(value)? (byte) (value + 32) : value;
    }

    /**
     * Convert the given byte to upper case.
     *
     * @param value the byte to convert
     * @return the upper case byte
     */
    static byte toUpperCase(final byte value) {
        return isLowerCase(value)? (byte) (value - 32) : value;
    }

    private static boolean equalsIgnoreCase(final long a, final long b) {
        return a == b || SWARUtil.toLowerCase(a) == SWARUtil.toLowerCase(b);
    }

    private static boolean equalsIgnoreCase(final int a, final int b) {
        return a == b || SWARUtil.toLowerCase(a) == SWARUtil.toLowerCase(b);
    }

    private static boolean equalsIgnoreCase(final byte a, final byte b) {
        return a == b || toLowerCase(a) == toLowerCase(b);
    }

    private AsciiStringUtil() {
        // Utility
    }
}

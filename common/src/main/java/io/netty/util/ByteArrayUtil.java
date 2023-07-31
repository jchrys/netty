/*
 * Copyright 2012 The Netty Project
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

import io.netty.util.internal.MathUtil;
import io.netty.util.internal.PlatformDependent;

import java.nio.ByteOrder;

public final class ByteArrayUtil {

    private ByteArrayUtil() {
    }

    private static final int INDEX_NOT_FOUND = -1;

    public static int firstIndexOf(byte[] array, int fromIndex, int toIndex, byte value) {
        final int length = toIndex - fromIndex;
        final int capacity = array.length;
        if (capacity == 0) {
            return INDEX_NOT_FOUND;
        }
        if (MathUtil.isOutOfBounds(fromIndex, length, capacity)) {
            throw new IndexOutOfBoundsException(); // TODO: exception details
        }
        if (!PlatformDependent.isUnaligned()) {
            return linearFirstIndexOf(array, fromIndex, toIndex, value);
        }
        int offset = fromIndex;
        final int byteCount = length & 7;
        if (byteCount > 0) {
            final int index = unrolledFirstIndexOf(array, offset, byteCount, value);
            if (index != INDEX_NOT_FOUND) {
                return index;
            }
            offset += byteCount;
            if (offset == toIndex) {
                return INDEX_NOT_FOUND;
            }
        }
        final long pattern = compilePattern(value);
        for (;offset < length; offset += Long.BYTES) {
            final long word = PlatformDependent.getLong(array, offset);
            final int index = firstAnyPattern(word, pattern, PlatformDependent.BIG_ENDIAN_NATIVE_ORDER);
            if (index < Long.BYTES) {
                return offset + index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    private static int linearFirstIndexOf(byte[] array, int fromIndex, int toIndex, byte value) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    private static int unrolledFirstIndexOf(byte[] array, int fromIndex, int byteCount, byte value) {
        assert byteCount > 0 && byteCount < 8;
        if (array[fromIndex] == value) {
            return fromIndex;
        }
        if (byteCount == 1) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 1] == value) {
            return fromIndex + 1;
        }
        if (byteCount == 2) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 2] == value) {
            return fromIndex + 2;
        }
        if (byteCount == 3) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 3] == value) {
            return fromIndex + 3;
        }
        if (byteCount == 4) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 4] == value) {
            return fromIndex + 4;
        }
        if (byteCount == 5) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 5] == value) {
            return fromIndex + 5;
        }
        if (byteCount == 6) {
            return INDEX_NOT_FOUND;
        }
        if (array[fromIndex + 6] == value) {
            return fromIndex + 6;
        }
        return INDEX_NOT_FOUND;
    }

    private static long compilePattern(byte byteToFind) {
        return (byteToFind & 0xFFL) * 0x101010101010101L;
    }

    private static int firstAnyPattern(long word, long pattern, boolean leading) {
        long input = word ^ pattern;
        long tmp = (input & 0x7F7F7F7F7F7F7F7FL) + 0x7F7F7F7F7F7F7F7FL;
        tmp = ~(tmp | input | 0x7F7F7F7F7F7F7F7FL);
        final int binaryPosition = leading? Long.numberOfLeadingZeros(tmp) : Long.numberOfTrailingZeros(tmp);
        return binaryPosition >>> 3;
    }


}

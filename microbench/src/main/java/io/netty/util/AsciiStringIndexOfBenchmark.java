/*
 * Copyright 2023 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.util;

import io.netty.microbench.util.AbstractMicrobenchmark;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

@Threads(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@State(Scope.Benchmark)
public class AsciiStringIndexOfBenchmark extends AbstractMicrobenchmark {

    public static Object blackhole;
    // from 1 to 128
    @Param({
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
            "51", "52", "53", "54", "55", "56", "57", "58", "59", "60",
            "61", "62", "63", "64", "65", "66", "67", "68", "69", "70",
            "71", "72", "73", "74", "75", "76", "77", "78", "79", "80",
            "81", "82", "83", "84", "85", "86", "87", "88", "89", "90",
            "91", "92", "93", "94", "95", "96", "97", "98", "99", "100",
            "101", "102", "103", "104", "105", "106", "107", "108", "109", "110",
            "111", "112", "113", "114", "115", "116", "117", "118", "119", "120",
            "121", "122", "123", "124", "125", "126", "127", "128"
    })
    int size;
    @Param({ "4", "7", "11", "14", "17" })
    int logPermutations;

    @Param({ "1" })
    int seed;

    int permutations;
    AsciiString[] data;
    private int i;

    @Param({ "0" })
    private byte needleByte;

    @Param({ "false" })
    private boolean noUnsafe;

    @Param({ "8", "32", "999" })
    private int bytePosition;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        System.setProperty("io.netty.noUnsafe", Boolean.valueOf(noUnsafe).toString());
        blackhole = PlatformDependent.isUnaligned();
        SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        data = new AsciiString[permutations];
        for (int i = 0; i < permutations; ++i) {
            byte[] byteArray = new byte[size];
            for (int j = 0; j < size; j++) {
                int value = random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE + 1);
                // turn any found value into something different
                if (value == needleByte) {
                    if (needleByte != 1) {
                        value = 1;
                    } else {
                        value = 0;
                    }
                }
                byteArray[j] = (byte) value;
            }
            final int foundIndex = random.nextInt(Math.max(0, size - bytePosition), size);
            byteArray[foundIndex] = needleByte;
            data[i] = new AsciiString(byteArray);
            blackhole = data[i].toString(); // cache
        }
    }

    private AsciiString getData() {
        return data[i++ & permutations - 1];
    }

    @Benchmark
    public int asciiStringIndexOf() {
        return getData().indexOf((char) needleByte, 0);
    }

    @Benchmark
    public int stringIndexOf() {
        return getData().toString().indexOf((char) needleByte);
    }
}

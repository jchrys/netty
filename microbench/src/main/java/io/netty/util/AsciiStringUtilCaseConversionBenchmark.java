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
public class AsciiStringUtilCaseConversionBenchmark extends AbstractMicrobenchmark {

    @Param({ "7", "16", "23", "32" })
    int size;
    @Param({ "11" })
    int logPermutations;

    @Param({ "1" })
    int seed;

    int permutations;
    byte[][] upperCaseData;
    private int i;

    @Param({ "true", "false" })
    private boolean noUnsafe;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        System.setProperty("io.netty.noUnsafe", Boolean.valueOf(noUnsafe).toString());
        SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        upperCaseData = new byte[permutations][size];
        for (int i = 0; i < permutations; ++i) {
            final int foundIndex = random.nextInt(Math.max(0, size - 8), size);
            byte[] byteArray = new byte[size];
            int j = 0;
            for (; j < size; j++) {
                int value = random.nextInt(0, Byte.MAX_VALUE + 1);
                // turn any found value into something different
                if (j < foundIndex) {
                    if (value >= 'A' && value <= 'Z') {
                        value |= 32;
                    }
                }
                if (j == foundIndex) {
                    value = 'N';
                }
                byteArray[j] = (byte) value;
            }
            upperCaseData[i] = byteArray;
        }
    }

    private byte[] getData() {
        return upperCaseData[i++ & permutations - 1];
    }

    @Benchmark
    public byte[] toLowerCase() {
        byte[] ret = new byte[size];
        AsciiStringUtil.toLowerCase(getData(), 0, ret, 0, size);
        return ret;
    }

    @Benchmark
    public byte[] toLowerCasePolly() {
        byte[] ret = new byte[size];
        AsciiStringUtil.toLowerCasePolly(getData(), 0, ret, 0, size);
        return ret;
    }

}

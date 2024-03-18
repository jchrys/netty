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
public class AsciiStringCaseConversionBenchmark extends AbstractMicrobenchmark {

    @Param({ "7", "16", "23", "32" })
    int size;

    @Param({ "11" })
    int logPermutations;

    @Param({ "0" })
    int seed;

    int permutations;

    AsciiString[] data;

    AsciiString[] copiedData;


    private int i;

    @Param({ "false" })
    private boolean noUnsafe;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        System.setProperty("io.netty.noUnsafe", Boolean.valueOf(noUnsafe).toString());
        SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        data = new AsciiString[permutations];
        copiedData = new AsciiString[permutations];
        for (int i = 0; i < permutations; ++i) {
            final byte[] byteArray = new byte[size];
            final byte[] byteArrayCopy = new byte[size];
            for (int j = 0; j < size; j++) {
                final byte asciiValue = (byte) random.nextInt(0, Byte.MAX_VALUE + 1);
                byteArray[j] = asciiValue;
            }
            data[i] = new AsciiString(byteArray);

            System.arraycopy(byteArray, 0, byteArrayCopy, 0, size);
            final int difIdx = random.nextInt(0, size);
            byteArrayCopy[difIdx] = (byte) (byteArrayCopy[difIdx] ^ 0x1);
            copiedData[i] = new AsciiString(byteArrayCopy);
        }
    }

    private AsciiString getData() {
        return data[i++ & permutations - 1];
    }

    private AsciiString getCopiedData() {
        return copiedData[i & permutations - 1];
    }

    @Benchmark
    public boolean isEqualsIgnoreCase() {
        final AsciiString s1 = getCopiedData();
        final AsciiString s2 = getData();
        return s1.contentEqualsIgnoreCase(s2);
    }

    @Benchmark
    public boolean isEqualsIgnoreCaseOld() {
        final AsciiString s1 = getCopiedData();
        final AsciiString s2 = getData();
        return s1.contentEqualsIgnoreCaseOld(s2);
    }

}

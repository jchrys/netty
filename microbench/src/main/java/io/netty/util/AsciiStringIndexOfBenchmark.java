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
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.CompilerControl.Mode;
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
import org.openjdk.jmh.infra.Blackhole;

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
    @Param({
            "63"
    })
    int size;
    @Param({ "11" })
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
            final int foundIndex = random.nextInt(Math.max(0, size - 8), size);
            byteArray[foundIndex] = needleByte;
            data[i] = new AsciiString(byteArray);
            blackhole = data[i].toString(); // cache
        }
    }

    private AsciiString getData() {
        return data[i++ & permutations - 1];
    }

    @Benchmark
    @CompilerControl(Mode.DONT_INLINE)
    public void stringIndexOf(Blackhole blackhole) {
        final AsciiString data = getData();
        for (int i = 0; i < 1000; ++i) {
            data.toString().indexOf((char) needleByte);
        }
    }
}

package io.netty.microbenchmark.common;

import io.netty.microbench.util.AbstractMicrobenchmark;
import io.netty.util.AsciiString;
import io.netty.util.internal.SuppressJava6Requirement;
import org.openjdk.jmh.annotations.Benchmark;
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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 8, time = 1)
@State(Scope.Benchmark)
public class AsciiStringIndexOfBenchmark extends AbstractMicrobenchmark {
    @Param({ "7", "16", "23", "32" })
    int size;
    @Param({ "4", "11" })
    int logPermutations;

    @Param({ "1" })
    int seed;

    int permutations; // uniquenessness
    AsciiString[] data;
    private int i;

    @Param({ "0" })
    private byte needleByte;

    @Param({ "true", "false" })
    private boolean noUnsafe;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        System.setProperty("io.netty.noUnsafe", Boolean.valueOf(noUnsafe).toString());
        SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        this.data = new AsciiString[permutations];
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
        }
    }

    private AsciiString getData() {
        return data[i++ & permutations - 1];
    }

    @Benchmark
    public int indexOf() {
        return getData().indexOf((char) needleByte, 0);
    }

    @Benchmark
    public int swarindexOf() {
        return getData().swarIndexOf((char) needleByte, 0);
    }

}

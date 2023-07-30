package io.netty.microbenchmark.common;

import io.netty.microbench.util.AbstractMicrobenchmark;
import io.netty.util.AsciiString;
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

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Threads(1)
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 8, time = 1)
public class AsciiStringCaseConversionBenchmark extends AbstractMicrobenchmark {

    @Param({ "7", "23", "47", "97"})
    public int size;
    @Param({ "5", "11", "23" })
    int logPermutations;
    @Param({ "1" })
    public int seed;

    int permutations;

    private int i;

    private byte[][] data;

    private byte[] result;


    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void setup() {
        final SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        data = new byte[permutations][size];
        result = new byte[size];

        for (int i = 0; i < permutations; ++i) {
            for (int j = 0; j < size; ++j) {
                data[i][j] = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE + 1);
            }
        }
    }

    private byte[] getData() {
        return data[i++ & (permutations - 1)];
    }

    @Benchmark
    public byte[] toLowerCase() {
        final byte[] data = getData();
        for (int i = 0; i < size; ++i) {
            result[i] = AsciiString.toLowerCase(data[i]);
        }
        return result;
    }

    @Benchmark
    public byte[] toLowerCaseNew() {
        final byte[] data = getData();
        for (int i = 0; i < size; ++i) {
            result[i] = AsciiString.toLowerCaseNew(data[i]);
        }
        return result;
    }

    @Benchmark
    public byte[] toUpperCase() {
        final byte[] data = getData();
        for (int i = 0; i < size; ++i) {
            result[i] = AsciiString.toUpperCase(data[i]);
        }
        return result;
    }

    @Benchmark
    public byte[] toUpperCaseNew() {
        final byte[] data = getData();
        for (int i = 0; i < size; ++i) {
            result[i] = AsciiString.toUpperCaseNew(data[i]);
        }
        return result;
    }
}

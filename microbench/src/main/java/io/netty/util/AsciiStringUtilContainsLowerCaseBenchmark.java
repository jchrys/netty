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
public class AsciiStringUtilContainsLowerCaseBenchmark extends AbstractMicrobenchmark {

    @Param({ "11" })
    int logPermutations;

    @Param({ "1" })
    int seed;

    int permutations;
    long[] data;
    private int i;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        data = new long[permutations];
        byte[] longBytes = new byte[8];
        for (int i = 0; i < permutations; ++i) {
            data[i] = random.nextLong();
        }
    }

    private long getData() {
        return data[i++ & permutations - 1];
    }

    @Benchmark
    public boolean containsLowerCase() {
        return AsciiStringUtil.containsLowerCase(getData());
    }

    @Benchmark
    public boolean containsLowerCasePolly() {
        return AsciiStringUtil.containsLowerCasePolly(getData());
    }

}

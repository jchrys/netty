package io.netty.util;

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
public class AsciiStringEqualsIgnoreCaseBenchmark {

    private static final byte[] ALPHA_NUMERIC = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };


    @Param({ "1", "2", "3", "4", "5", "6", "7", "8", "16", "23", "32" })
    int size;

    @Param({ "4", "11" })
    int logPermutations;

    @Param({ "0" })
    int seed;

    int permutations;

    AsciiString[] data;


    byte[] ret;
    private int i;

    @Param({ "true", "false" })
    private boolean noUnsafe;

    @Setup(Level.Trial)
    @SuppressJava6Requirement(reason = "using SplittableRandom to reliably produce data")
    public void init() {
        System.setProperty("io.netty.noUnsafe", Boolean.valueOf(noUnsafe).toString());
        final SplittableRandom random = new SplittableRandom(seed);
        permutations = 1 << logPermutations;
        ret = new byte[size];
        data = new AsciiString[permutations << 2];
        for (int i = 0; i < permutations; i += 2) {
            final byte[] byteArray = new byte[size];
            final byte[] byteArray2 = new byte[size];
            for (int j = 0; j < size; j++) {
                byte value = ALPHA_NUMERIC[random.nextInt(0, ALPHA_NUMERIC.length)];
                // turn any found value into something different
                byteArray[j] = value;
                byteArray2[j] = AsciiStringUtil.isUpperCase(value)? AsciiStringUtil.toLowerCase(value) :
                        AsciiStringUtil.toUpperCase(value);
            }
            data[i] = new AsciiString(byteArray, false);
            data[i + 1] = new AsciiString(byteArray2, false);
        }
    }

    private int getIdx() {
        int ret = i & permutations - 1;
        i += 2;
        return ret;
    }

    private AsciiString getData(int idx) {
        return data[idx];
    }

    @Benchmark
    public boolean equalsIgnoreCase() {
        final int idx = getIdx();
        final AsciiString lhs = getData(idx);
        final AsciiString rhs = getData(idx ^ 1);
        return lhs.contentEqualsIgnoreCase(rhs);
    }

}

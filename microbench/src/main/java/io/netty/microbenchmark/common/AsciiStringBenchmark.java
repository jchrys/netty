/*
 * Copyright 2015 The Netty Project
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
package io.netty.microbenchmark.common;

import io.netty.microbench.util.AbstractMicrobenchmark;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.NewAsciiString;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

@Threads(1)
@Measurement(iterations = 5)
@Warmup(iterations = 5)
public class AsciiStringBenchmark extends AbstractMicrobenchmark {

    @Param({ "10", "100", "1000", "10000" })
    public int size;

    private boolean adder;

    private AsciiString asciiString;
    private String string;
    private AsciiString alphabeticAsciiString;
    private NewAsciiString alphabeticNewAsciiString;
    private byte[] alphabeticBytes;
    private static final Random random = new Random();

    @Setup(Level.Trial)
    public void setup() {
        adder = true;
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        asciiString = new AsciiString(bytes, false);
        alphabeticBytes = new byte[size];
        nextAlphabeticBytes(alphabeticBytes);
        alphabeticAsciiString = new AsciiString(alphabeticBytes, true);
        alphabeticNewAsciiString = new NewAsciiString(alphabeticBytes, true);
        string = new String(bytes, CharsetUtil.US_ASCII);
    }

    private static void nextAlphabeticBytes(byte[] bytes) {
        // This function fills the given 'bytes' array with random alphabetic characters.
        // The input 'bytes' should have enough space to store the random alphabetic characters.

        Random random = new Random();

        // Loop through the byte array.
        bytes[0] = 'Z';
        for (int i = 1; i < bytes.length; i++) {
            // Generate a random integer between 0 and 51 (inclusive) to represent the alphabetic characters.
            int randomValue = random.nextInt(52);

            // Map the random value to the appropriate ASCII value for an alphabetic character.
            if (randomValue < 26) {
                // Random uppercase letter (A to Z) - ASCII values: 65 to 90.
                bytes[i] = (byte) (randomValue + 'A');
            } else {
                // Random lowercase letter (a to z) - ASCII values: 97 to 122.
                bytes[i] = (byte) (randomValue - 26 + 'a');
            }
        }
    }

    @Benchmark
    public void oldToLowerCase(Blackhole blackhole) {
        blackhole.consume(alphabeticAsciiString.toLowerCase());
    }

    @Benchmark
    public void toLowerCase(Blackhole blackhole) {
        blackhole.consume(alphabeticNewAsciiString.toLowerCase());
    }

    @Benchmark
    public void oldEqualsIgnoreCase(Blackhole blackhole) {
        for (int i = 0; i < size; ++i) {
            blackhole.consume(AsciiString.equalsIgnoreCase(alphabeticBytes[i], alphabeticBytes[(i + 5) % size]));
        }
    }

    @Benchmark
    public void equalsIgnoreCase(Blackhole blackhole) {
        for (int i = 0; i < size; ++i) {
            blackhole.consume(NewAsciiString.equalsIgnoreCase(alphabeticBytes[i], alphabeticBytes[(i + 5) % size]));
        }
    }

    @Benchmark
    public void equalsIgnoreCase2(Blackhole blackhole) {
        for (int i = 0; i < size; ++i) {
            blackhole.consume(NewAsciiString.equalsIgnoreCase2(alphabeticBytes[i], alphabeticBytes[(i + 5) % size]));
        }
    }
}


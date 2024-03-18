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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Threads(1)
@Measurement(iterations = 5, time = 200, timeUnit = MILLISECONDS)
@Warmup(iterations = 5, time = 200, timeUnit = MILLISECONDS)
public class AsciiStringBenchmark extends AbstractMicrobenchmark {


    private AsciiString asciiString;
    private String string;
    private static final Random random = new Random();
    private AsciiString connection;
    private AsciiString Connection;

    @Setup(Level.Trial)
    public void setup() {
        connection = AsciiString.cached("connection");
        Connection = AsciiString.cached("Connection");
    }

    @Benchmark
    public boolean equalsIgnoreCaseBench() {
        return Connection.contentEqualsIgnoreCase(connection);
    }

    @Benchmark
    public boolean equalsIgnoreCaseOld() {
        return Connection.contentEqualsIgnoreCaseOld(connection);
    }

}

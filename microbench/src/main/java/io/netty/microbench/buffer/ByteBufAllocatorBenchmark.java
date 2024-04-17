/*
 * Copyright 2012 The Netty Project
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
package io.netty.microbench.buffer;

import io.netty.buffer.AdaptiveByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.microbench.util.AbstractMicrobenchmark;
import io.netty.util.NettyRuntime;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This class benchmarks different allocators with different allocation sizes.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(8)
public class ByteBufAllocatorBenchmark extends AbstractMicrobenchmark {
    static {
        NettyRuntime.setAvailableProcessors(8);
    }

    private static final ByteBufAllocator unpooledAllocator = new UnpooledByteBufAllocator(true);
    private static final ByteBufAllocator pooledAllocator =
            new PooledByteBufAllocator(true, PooledByteBufAllocator.defaultNumHeapArena(), PooledByteBufAllocator.defaultNumDirectArena(), 8192, 11, 0, 0, 0, true, 0); // Disable thread-local cache
    private static final ByteBufAllocator adaptiveAllocator = new AdaptiveByteBufAllocator();

    private static final int MAX_LIVE_BUFFERS = 8192;

    @State(Scope.Thread)
    public static class ThreadState {
        final Random rand = new Random();
        final ByteBuf[] unpooledHeapBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] unpooledDirectBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] pooledHeapBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] pooledDirectBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] defaultPooledHeapBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] defaultPooledDirectBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] adaptiveHeapBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
        final ByteBuf[] adaptiveDirectBuffers = new ByteBuf[MAX_LIVE_BUFFERS];
    }

    @Param({ "00000", "00256", "01024", "04096", "16384" })
    public int size;

    @Benchmark
    public void unpooledHeapAllocAndFree(ThreadState state) {
        int idx = state.rand.nextInt(state.unpooledHeapBuffers.length);
        ByteBuf oldBuf = state.unpooledHeapBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.unpooledHeapBuffers[idx] = unpooledAllocator.heapBuffer(size);
    }

    @Benchmark
    public void unpooledDirectAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.unpooledDirectBuffers.length);
        ByteBuf oldBuf = state.unpooledDirectBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.unpooledDirectBuffers[idx] = unpooledAllocator.directBuffer(size);
    }

    @Benchmark
    public void pooledHeapAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.pooledHeapBuffers.length);
        ByteBuf oldBuf = state.pooledHeapBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.pooledHeapBuffers[idx] = pooledAllocator.heapBuffer(size);
    }

    @Benchmark
    public void pooledDirectAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.pooledDirectBuffers.length);
        ByteBuf oldBuf = state.pooledDirectBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.pooledDirectBuffers[idx] = pooledAllocator.directBuffer(size);
    }

    @Benchmark
    public void defaultPooledHeapAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.defaultPooledHeapBuffers.length);
        ByteBuf oldBuf = state.defaultPooledHeapBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.defaultPooledHeapBuffers[idx] = PooledByteBufAllocator.DEFAULT.heapBuffer(size);
    }

    @Benchmark
    public void defaultPooledDirectAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.defaultPooledDirectBuffers.length);
        ByteBuf oldBuf = state.defaultPooledDirectBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.defaultPooledDirectBuffers[idx] = PooledByteBufAllocator.DEFAULT.directBuffer(size);
    }

    @Benchmark
    public void adaptiveHeapAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.adaptiveHeapBuffers.length);
        ByteBuf oldBuf = state.adaptiveHeapBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.adaptiveHeapBuffers[idx] = adaptiveAllocator.heapBuffer(size);
    }

    @Benchmark
    public void adaptiveDirectAllocAndFree(final ThreadState state) {
        int idx = state.rand.nextInt(state.adaptiveDirectBuffers.length);
        ByteBuf oldBuf = state.adaptiveDirectBuffers[idx];
        if (oldBuf != null) {
            oldBuf.release();
        }
        state.adaptiveDirectBuffers[idx] = adaptiveAllocator.directBuffer(size);
    }
}

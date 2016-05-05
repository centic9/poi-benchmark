package org.apache.poi.benchmark.suite;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.io.IOException;

public class BuildBenchmarks extends BaseBenchmark {
    @Setup
    public void setUp() throws IOException {
        clean();
    }

    @Benchmark
    public void benchmarkCompileAll() throws IOException {
        compileAll();
    }
}

package org.apache.poi.benchmark.suite;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SSPerformanceBenchmarks extends BaseBenchmark {
    @Setup
    public void setUp() throws IOException {
        compileAll();
    }

    @Benchmark
    public void benchmarkHSSFPerformance() throws IOException {
        runPOIApplication("org.apache.poi.ss.examples.SSPerformanceTest", TimeUnit.HOURS.toMillis(1), "HSSF", "2000", "20", "0");
    }

    @Benchmark
    public void benchmarkXSSFPerformance() throws IOException {
        runPOIApplication("org.apache.poi.ss.examples.SSPerformanceTest", TimeUnit.HOURS.toMillis(1), "XSSF", "20000", "20", "0");
    }

    @Benchmark
    public void benchmarkSXSSFPerformance() throws IOException {
        runPOIApplication("org.apache.poi.ss.examples.SSPerformanceTest", TimeUnit.HOURS.toMillis(1), "SXSSF", "20000", "20", "0");
    }
}

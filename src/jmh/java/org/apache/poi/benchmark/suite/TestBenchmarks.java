package org.apache.poi.benchmark.suite;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestBenchmarks extends BaseBenchmark {
    @Setup
    public void setUp() throws IOException {
        compileAll();
    }

    @Benchmark
    public void benchmarkTestMain() throws IOException {
        testMain();
    }

    @Benchmark
    public void benchmarkTestScratchpad() throws IOException {
        testScratchpad();
    }

    @Benchmark
    public void benchmarkTestOOXML() throws IOException {
        testOOXML();
    }

    @Benchmark
    public void benchmarkTestOOXMLLite() throws IOException {
        testOOXMLLite();
    }

    @Benchmark
    public void benchmarkTestExcelant() throws IOException {
        testExcelant();
    }

    // set the timeout to a high value as integration tests do not print out anything for quite some time
    @Timeout(time = 3, timeUnit = TimeUnit.HOURS)
    @Benchmark
    public void benchmarkTestIntegration() throws IOException {
        testIntegration();
    }
}

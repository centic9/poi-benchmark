package org.apache.poi.benchmark.suite;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.io.IOException;

public class SSPerformanceBenchmarks extends BaseBenchmark {
    @Setup
    public void setUp() throws IOException {
        compileAll();
    }

    @Benchmark
    public void benchmarkHSSFPerformance() throws IOException {
        /*
        export JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk-amd64
        export ANT_OPTS="-Xmx1024m -XX:MaxPermSize=256m"

        for i in build/dist/maven/poi* /
        *.jar build/*classes ooxml-lib/xmlbeans-2.3.0.jar ooxml-lib/dom4j-1.6.1.jar;do
	JARS="$JARS:$i"
done

ROWS=25000
COLS=25

AGENT=-agentpath:/opt/dynaTrace/dynatrace-6.1.0/agent/lib64/libdtagent.so=name=ApachePOI,server=localhost

echo CP: $JARS
java -cp $JARS -Xmx1024m $AGENT org.apache.poi.ss.examples.SSPerformanceTest HSSF $ROWS $COLS 0 && \
java -cp $JARS -Xmx1024m $AGENT org.apache.poi.ss.examples.SSPerformanceTest XSSF $ROWS $COLS 0 && \
java -cp $JARS -Xmx1024m $AGENT org.apache.poi.ss.examples.SSPerformanceTest SXSSF $ROWS $COLS 0 && \
echo All done
        */

    }

    @Benchmark
    public void benchmarkXSSFPerformance() throws IOException {

    }

    @Benchmark
    public void benchmarkSXSSFPerformance() throws IOException {

    }
}

package org.apache.poi.benchmark.suite;

import com.google.common.base.Preconditions;
import org.apache.commons.exec.CommandLine;
import org.dstadler.commons.exec.BufferingLogOutputStream;
import org.dstadler.commons.exec.ExecutionHelper;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 0, warmups = 0)
public abstract class BaseBenchmark {
    protected final File srcDir = new File("sources");

    @Setup
    public final void baseSetUp() throws IOException {
        // ensure directories
        if(!srcDir.exists()) {
            Preconditions.checkState(srcDir.mkdir(), "Could not create directory " + srcDir.getAbsolutePath());
        }

        svnCheckout();
    }

    private void svnCheckout() throws IOException {
        // svn checkout/update
        try (OutputStream out = new BufferingLogOutputStream()) {
            CommandLine cmd = new CommandLine("svn");
            if(new File(srcDir, ".svn").exists()) {
                cmd.addArgument("up");
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, 60000, out);
            } else {
                cmd.addArgument("co");
                cmd.addArgument("https://svn.apache.org/repos/asf/poi/trunk");
                cmd.addArgument(srcDir.getName());
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir.getParentFile(), 0, 60000, out);
            }
        }
    }

    protected void clean() throws IOException {
        runAntTarget("clean", TimeUnit.MINUTES.toMillis(10));
    }

    protected void compileAll() throws IOException {
        runAntTarget("compile-all", TimeUnit.HOURS.toMillis(1));
    }

    protected void testMain() throws IOException {
        runAntTarget("test-main", TimeUnit.HOURS.toMillis(1));
    }

    protected void testScratchpad() throws IOException {
        runAntTarget("test-scratchpad", TimeUnit.HOURS.toMillis(1));
    }

    protected void testOOXML() throws IOException {
        runAntTarget("test-ooxml", TimeUnit.HOURS.toMillis(1));
    }

    protected void testOOXMLLite() throws IOException {
        runAntTarget("test-ooxml-lite", TimeUnit.HOURS.toMillis(1));
    }

    protected void testExcelant() throws IOException {
        runAntTarget("test-excelant", TimeUnit.HOURS.toMillis(1));
    }

    protected void testIntegration() throws IOException {
        runAntTarget("test-integration", TimeUnit.HOURS.toMillis(1), "-Dorg.apache.poi.util.POILogger=org.apache.poi.util.SystemOutLogger");
    }

    private void runAntTarget(String target, long timeout, String... args) throws IOException {
        try (OutputStream out = new BufferingLogOutputStream()) {
            CommandLine cmd = new CommandLine("ant");
            cmd.addArgument(target);
            cmd.addArguments(args);
            ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out);
        }
    }
}

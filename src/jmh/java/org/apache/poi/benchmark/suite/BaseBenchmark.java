package org.apache.poi.benchmark.suite;

import com.google.common.base.Preconditions;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.dstadler.commons.arrays.ArrayUtils;
import org.dstadler.commons.exec.BufferingLogOutputStream;
import org.dstadler.commons.exec.ExecutionHelper;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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

        svnCleanup();
        svnCheckout();
        svnStatus();
    }

    private void svnCleanup() throws IOException {
        runSVN("cleanup", 60000);
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

    private void svnStatus() throws IOException {
        runSVN("status", TimeUnit.MINUTES.toMillis(1));
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
        // need to clean one file here to avoid not doing anything if the code
        // was already compiled before
        final File testfile = new File(srcDir, "build/ooxml-lite-testokfile.txt");
        if(testfile.exists()) {
            if(!testfile.delete()) {
                throw new IOException("Could not delete file " + testfile);
            }
        }

        runAntTarget("test-ooxml-lite", TimeUnit.HOURS.toMillis(1));
    }

    protected void testExcelant() throws IOException {
        runAntTarget("test-excelant", TimeUnit.HOURS.toMillis(1));
    }

    protected void testIntegration() throws IOException {
        runAntTarget("test-integration", TimeUnit.HOURS.toMillis(2)/*, "-Dorg.apache.poi.util.POILogger=org.apache.poi.util.SystemOutLogger"*/);
    }

    private void runAntTarget(String target, long timeout, String... args) throws IOException {
        try (OutputStream out = new BufferingLogOutputStream()) {
            CommandLine cmd = new CommandLine("ant");
            cmd.addArgument(target);
            cmd.addArguments(args);
            ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out);
        }
    }

    private void runSVN(String command, long timeout, String... args) throws IOException {
        try (OutputStream out = new BufferingLogOutputStream()) {
            CommandLine cmd = new CommandLine("svn");
            cmd.addArgument(command);
            cmd.addArguments(args);
            ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out);
        }
    }

    protected void runPOIApplication(@SuppressWarnings("SameParameterValue") String clazz, long timeout, String... args) throws IOException {
        List<String> jars = new ArrayList<>();
        addJarsFromDir(jars, "lib");
        addJarsFromDir(jars, "compile-lib");
        addJarsFromDir(jars, "ooxml-lib");
        addClassesDir(jars, "build");
        try (OutputStream out = new BufferingLogOutputStream()) {
            CommandLine cmd = new CommandLine("java");
            cmd.addArgument("-cp");
            cmd.addArgument(ArrayUtils.toString(jars.toArray(), ":", "", ""));
            cmd.addArgument(clazz);
            cmd.addArguments(args);
            ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addClassesDir(List<String> jars, String dir) {
        File[] files = new File(srcDir, dir).listFiles((FileFilter)
                        new SuffixFileFilter("classes"));
        Preconditions.checkNotNull(files);
        for(File file : files) {
            jars.add(file.getAbsolutePath());
        }
    }

    private void addJarsFromDir(List<String> jars, String dir) {
        File[] files = new File(srcDir, dir).listFiles((FileFilter)
                new AndFileFilter(
                    new SuffixFileFilter(".jar"),
                    new NotFileFilter(new AndFileFilter(
                            new SuffixFileFilter("-sources.jar"),
                            new SuffixFileFilter("xmlbeans-2.3.0.jar")))));
        Preconditions.checkNotNull(files);
        for(File file : files) {
            jars.add(file.getAbsolutePath());
        }
    }
}

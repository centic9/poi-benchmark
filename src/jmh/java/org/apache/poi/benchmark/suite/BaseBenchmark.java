package org.apache.poi.benchmark.suite;

import com.google.common.base.Preconditions;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.poi.benchmark.util.TailLogOutputStream;
import org.dstadler.commons.arrays.ArrayUtils;
import org.dstadler.commons.exec.ExecutionHelper;
import org.dstadler.commons.logging.jdk.DefaultFormatter;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@SuppressWarnings("JmhInspections")
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 0, warmups = 0)
public abstract class BaseBenchmark {
    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final long TEN_MINUTES = TimeUnit.MINUTES.toMillis(10);
    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2);

    // Apache POI requires a newer Ant now, but the jmh plugin does not re-use the one from the
    // commandline therefore we resort to setting it manually here for now
    private static final String ANT_HOME = "/opt/apache-ant-1.10.8";
    private static final String ANT_OPTS = "-Xmx512m";
    private static final Map<String, String> ENVIRONMENT = new HashMap<>();
    private static final int TAIL_LINES = 100;

    static {
        ENVIRONMENT.put("ANT_HOME", ANT_HOME);
        ENVIRONMENT.put("ANT_OPTS", ANT_OPTS);
        ENVIRONMENT.put("PATH", ANT_HOME + "/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/snap/bin:"
                + System.getenv("PATH"));
    }

    static {
        // set up logging configuration
        configureLoggingFramework();
    }

    private static final Logger log = LoggerFactory.make();

    protected final File srcDir = new File("sources");

    private static void configureLoggingFramework() {
        try {
            LoggerFactory.sendCommonsLogToJDKLog();

            try (InputStream resource = new FileInputStream("src/jmh/resources/logging.properties")) {
                // apply configuration
                try {
                    LogManager.getLogManager().readConfiguration(resource);
                } finally {
                    resource.close();
                }

                // apply a default format to the log handlers here before throwing an exception further down
                Logger log = Logger.getLogger("");    // NOSONAR - local logger used on purpose here
                for (Handler handler : log.getHandlers()) {
                    handler.setFormatter(new DefaultFormatter());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Current directory: " + new File(".").getAbsolutePath(), e);
        }
    }

    @Setup
    public final void baseSetUp() throws IOException {
        // ensure directories exist
        if(!srcDir.exists()) {
            Preconditions.checkState(srcDir.mkdir(), "Could not create directory " + srcDir.getAbsolutePath());
        }

        // clean up checkout
        if(new File(srcDir, ".svn").exists()) {
            svnCleanup();
        }

        svnCheckout();
        svnStatus();
        printEnvironment();
    }

    private void svnCleanup() throws IOException {
        runSVN("cleanup");
    }

    private void svnCheckout() throws IOException {
        // svn checkout/update
        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            try {
                CommandLine cmd = new CommandLine("svn");
                if (new File(srcDir, ".svn").exists()) {
                    cmd.addArgument("up");
                    ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, ONE_MINUTE, out, ENVIRONMENT);
                } else {
                    cmd.addArgument("co");
                    cmd.addArgument("https://svn.apache.org/repos/asf/poi/trunk");
                    cmd.addArgument(srcDir.getName());
                    ExecutionHelper.getCommandResultIntoStream(cmd, srcDir.getParentFile(), 0, ONE_MINUTE,
                            out, ENVIRONMENT);
                }
            } catch (IOException e) {
                throw new IOException("Log-Tail: " + out.getLines(), e);
            }
        }
    }

    private void svnStatus() throws IOException {
        runSVN("status");
    }

    protected void clean() throws IOException {
        runAntTarget("clean", TEN_MINUTES);
    }

    private void printEnvironment() throws IOException {
        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("bash");
            cmd.addArgument("-c");
            cmd.addArguments("set");
            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, ONE_MINUTE, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to print the environment variables", e);
                throw new IOException("Log-Tail: " + out.getLines(), e);
            }
        }
    }

    protected void compileAll() throws IOException {
        runAntTarget("compile", ONE_HOUR);
    }

    protected void testMain() throws IOException {
        runAntTarget("test-main", ONE_HOUR);
    }

    protected void testScratchpad() throws IOException {
        runAntTarget("test-scratchpad", ONE_HOUR);
    }

    protected void testOOXML() throws IOException {
        runAntTarget("test-ooxml", ONE_HOUR);
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

        runAntTarget("test-ooxml-lite", ONE_HOUR);
    }

    protected void testExcelant() throws IOException {
        runAntTarget("test-excelant", ONE_HOUR);
    }

    protected void testIntegration() throws IOException {
        runAntTarget("test-integration", TWO_HOURS/*, "-Dorg.apache.poi.util.POILogger=org.apache.poi.util.SystemOutLogger"*/);
    }

    private void runAntTarget(String target, long timeout, String... args) throws IOException {
        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("ant");
            cmd.addArgument(target);
            cmd.addArguments(args);
            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to run Ant with target " + target +
                        " and args: " + Arrays.toString(args), e);
                throw new IOException("Log-Tail: " + out.getLines(), e);
            }
        }
    }

    private void runSVN(String command, String... args) throws IOException {
        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("svn");
            cmd.addArgument(command);
            cmd.addArguments(args);
            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, ONE_MINUTE, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to run SVN with command " + command +
                        " and args: " + Arrays.toString(args), e);
                throw new IOException("Log-Tail: " + out.getLines(), e);
            }
        }
    }

    protected void runPOIApplication(@SuppressWarnings("SameParameterValue") String clazz, long timeout, String... args) throws IOException {
        List<String> jars = new ArrayList<>();

        // Collect third-party jar-files
        addJarsFromDir(jars, "lib/excelant");
        addJarsFromDir(jars, "lib/main");
        addJarsFromDir(jars, "lib/main-tests");
        addJarsFromDir(jars, "lib/ooxml");
        addJarsFromDir(jars, "lib/ooxml-provided");
        addJarsFromDir(jars, "lib/ooxml-tests");
        addJarsFromDir(jars, "lib/util");

        // Collect complied classes for Apache POI itself
        addClassesDir(jars, "build");

        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("java");
            cmd.addArgument("-cp");
            cmd.addArgument(ArrayUtils.toString(jars.toArray(), ":", "", ""));
            cmd.addArgument(clazz);
            cmd.addArguments(args);

            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to run POI application " + clazz + "" +
                        " and args: " + Arrays.toString(args), e);
                throw new IOException("Log-Tail: " + out.getLines(), e);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addClassesDir(List<String> jars, String dir) {
        File[] files = new File(srcDir, dir).listFiles((FileFilter)
                        new SuffixFileFilter("classes"));
        Preconditions.checkNotNull(files,
                "Sub-Directory %s in %s does not exist",
                dir, srcDir.getAbsolutePath());
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
        Preconditions.checkNotNull(files,
                "Sub-Directory %s in %s does not exist",
                dir, srcDir.getAbsolutePath());
        for(File file : files) {
            jars.add(file.getAbsolutePath());
        }
    }
}

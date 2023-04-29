package org.apache.poi.benchmark.suite;

import com.google.common.base.Preconditions;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
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

    private static final Map<String, String> ENVIRONMENT = new HashMap<>();
    static {
        // we had some strange failures on one of the test-vms, so let's try
        // to run with less parallelism when running tests
        ENVIRONMENT.put("CI_BUILD", "TRUE");
    }

    private static final int TAIL_LINES = 100;

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

    @SuppressWarnings("unused")
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
		patchTestExecution();
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
                throw new IOException("Log-Tail: \n" + String.join("\n    ", out.getLines()), e);
            }
        }
    }

	private void patchTestExecution() throws IOException {
		String content = FileUtils.readFileToString(new File(srcDir, "build.gradle"), "UTF-8");

		// use the "Marlin" rendering engine as the default "pisces" causes endless loops for some integration-test-files
        boolean changed = false;
		if (!content.contains("strategy=dynamic','-Xbootclasspath")) {
            content = content.replace("strategy=dynamic',",
                    "strategy=dynamic',"
                            + "'-Xbootclasspath/p:" + srcDir.getAbsoluteFile().getParentFile().getAbsolutePath() + "/marlin-0.9.4.5-Unsafe.jar',"
                            + "'-Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine',");

            changed = true;
        }

        if (!content.contains("strategy=fixed','-Xbootclasspath")) {
            content = content.replace("strategy=fixed',",
                    "strategy=fixed',"
                            + "'-Xbootclasspath/p:" + srcDir.getAbsoluteFile().getParentFile().getAbsolutePath() + "/marlin-0.9.4.5-Unsafe.jar',"
                            + "'-Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine',");

            changed = true;
        }

        if (!content.contains("1536m")) {
            // this can be removed again as soon as the file in POI itself is updated with a bit more memory
            content = content.replace(
                    "maxHeapSize = \"1G\"",
                    "maxHeapSize = \"1536m\"");

            changed = true;
        }

        if (changed) {
			FileUtils.writeStringToFile(new File(srcDir, "build.gradle"), content, "UTF-8");
		}
	}

    private void svnStatus() throws IOException {
        runSVN("status");
    }

    protected void clean() throws IOException {
		// these files are modified locally, we want to avoid conflicts
		runSVN("revert",
				"poi-examples/src/main/java9/module-info.class",
				"poi-excelant/src/main/java9/module-info.class",
				"poi-excelant/src/test/java9/module-info.class",
				"poi-integration/src/test/java9/module-info.class",
				"poi-ooxml-full/src/main/java9/module-info.class",
				"poi-ooxml-lite-agent/src/main/java9/module-info.class",
				"poi-ooxml-lite/src/main/java9/module-info.class",
				"poi-ooxml/src/main/java9/module-info.class",
				"poi-ooxml/src/test/java9/module-info.class",
				"poi-scratchpad/src/main/java9/module-info.class",
				"poi-scratchpad/src/test/java9/module-info.class",
				"poi/src/main/java9/module-info.class",
				"poi/src/test/java9/module-info.class",

				"poi-examples/src/main/java9/module-info.java",
				"poi-excelant/src/main/java9/module-info.java",
				"poi-excelant/src/test/java9/module-info.java",
				"poi-integration/src/test/java9/module-info.java",
				"poi-ooxml-full/src/main/java9/module-info.java",
				"poi-ooxml-lite-agent/src/main/java9/module-info.java",
				"poi-ooxml-lite/src/main/java9/module-info.java",
				"poi-ooxml/src/main/java9/module-info.java",
				"poi-ooxml/src/test/java9/module-info.java",
				"poi-scratchpad/src/main/java9/module-info.java",
				"poi-scratchpad/src/test/java9/module-info.java",
				"poi/src/main/java9/module-info.java",
				"poi/src/test/java9/module-info.java",

                "src/resources/ooxml-lite-report.clazz",
                "src/resources/ooxml-lite-report.xsb",
				"build.gradle");

		runGradleTarget("clean", TEN_MINUTES);
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
                throw new IOException("Log-Tail: \n" + String.join("\n    ", out.getLines()), e);
            }
        }
    }

    protected void compileAll() throws IOException {
        runGradleTarget("compileJava", ONE_HOUR,
				// let's run more than one target in one go
				"compileTestJava",  "getDeps");
    }

    protected void testMain() throws IOException {
        runGradleTarget(":poi:check", ONE_HOUR);
    }

    protected void testScratchpad() throws IOException {
        runGradleTarget(":poi-scratchpad:check", ONE_HOUR);
    }

    protected void testOOXML() throws IOException {
        runGradleTarget(":poi-ooxml:check", ONE_HOUR);
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

        runGradleTarget(":poi-ooxml-lite:check", ONE_HOUR);
    }

    protected void testExcelant() throws IOException {
        runGradleTarget(":poi-excelant:check", ONE_HOUR);
    }

    protected void testIntegration() throws IOException {
        runGradleTarget(":poi-integration:check", TWO_HOURS);
    }

    private void runGradleTarget(String target, long timeout, String... args) throws IOException {
        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("bash");
            cmd.addArgument("./gradlew");
            cmd.addArgument("--no-daemon");
            cmd.addArgument("--console");
            cmd.addArgument("plain");
            cmd.addArgument(target);
            cmd.addArguments(args);
            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to run Gradle with target: '" + target + "' and args: " +
                        ArrayUtils.toString(args, " ", "", ""), e);
                throw new IOException("Log-Tail: \n" + String.join("\n    ", out.getLines()), e);
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
                throw new IOException("Log-Tail: \n" + String.join("\n    ", out.getLines()), e);
            }
        }
    }

    protected void runPOIApplication(@SuppressWarnings("SameParameterValue") String clazz, long timeout, String... args) throws IOException {
        List<String> jars = new ArrayList<>();

		addJarsFromDir(jars, "poi/build/runtime");
		addJarsFromDir(jars, "poi-examples/build/runtime");
		//addJarsFromDir(jars, "poi-excelant/build/runtime");
		addJarsFromDir(jars, "poi-ooxml/build/runtime");
		//addJarsFromDir(jars, "poi-ooxml-full/build/runtime");
		//addJarsFromDir(jars, "poi-ooxml-lite-agent/build/runtime");
		//addJarsFromDir(jars, "poi-ooxml-lite/build/runtime");
		addJarsFromDir(jars, "poi-scratchpad/build/runtime");

        // Collect third-party jar-files (only available after running Ant, replaced by "build/runtime" above)
        /*addJarsFromDir(jars, "lib/excelant");
        addJarsFromDir(jars, "lib/main");
        addJarsFromDir(jars, "lib/main-tests");
        addJarsFromDir(jars, "lib/ooxml");
        addJarsFromDir(jars, "lib/ooxml-provided");
        addJarsFromDir(jars, "lib/ooxml-tests");
        addJarsFromDir(jars, "lib/util");*/

        // Collect complied classes for Apache POI itself
        addClassesDir(jars, "build");

        // new directories after starting move to Gradle
        addClassesDir(jars, "poi/build");
        addClassesDir(jars, "poi-examples/build");
        addClassesDir(jars, "poi-excelant/build");
        addClassesDir(jars, "poi-integration/build");
        addClassesDir(jars, "poi-ooxml/build");
        addClassesDir(jars, "poi-ooxml-full/build");
        addClassesDir(jars, "poi-scratchpad/build");

        try (TailLogOutputStream out = new TailLogOutputStream(TAIL_LINES)) {
            CommandLine cmd = new CommandLine("java");
			cmd.addArgument("-Djava.io.tmpdir=build");
            cmd.addArgument("-cp");
            cmd.addArgument(ArrayUtils.toString(jars.toArray(), ":", "", ""));
            cmd.addArgument(clazz);
            cmd.addArguments(args);

            try {
                ExecutionHelper.getCommandResultIntoStream(cmd, srcDir, 0, timeout, out, ENVIRONMENT);
            } catch (ExecuteException e) {
                log.log(Level.WARNING, "Failed to run POI application " + clazz +
                        " and args: " + Arrays.toString(args), e);
                throw new IOException("Log-Tail: \n" + String.join("\n    ", out.getLines()), e);
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
			if (new File(file.getAbsolutePath(), "java/main").exists()) {
				jars.add(file.getAbsolutePath() + "/java/main");
			}
			if (new File(file.getAbsolutePath(), "ant/java").exists()) {
				jars.add(file.getAbsolutePath() + "/ant/java");
			}
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

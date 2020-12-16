package org.apache.poi.benchmark.util;

import org.dstadler.commons.exec.BufferingLogOutputStream;
import org.dstadler.commons.testing.TestHelpers;
import org.dstadler.commons.testing.ThreadTestHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TailLogOutputStreamTest {
    private static final int NUMBER_OF_THREADS = 10;
    private static final int NUMBER_OF_TESTS = 1000;

    @Test
    public void test() throws IOException {
        try (TailLogOutputStream stream = new TailLogOutputStream(10)) {

            // sends everything to Level.INFO
            stream.processLine("some line", 0);
            stream.processLine("some line", 0);

            // test null and empty string
            stream.processLine(null, 0);
            stream.processLine("", 0);
            stream.processLine(" ", 0);

            stream.processLine("some line", 0);

            Collection<String> lines = stream.getLines();
            assertNotNull(lines);
            assertEquals(4, lines.size());
        }
    }

    @Test
    public void testLargeData() {
        TestHelpers.runTestWithDifferentLogLevel(() -> {
            try (TailLogOutputStream stream = new TailLogOutputStream(10)) {

                // sends everything to Level.INFO
                for (int i = 0; i < 1000; i++) {
                    stream.processLine("some line", 0);
                }

                stream.close();

                // try closing twice, should not fail
                stream.close();

                Collection<String> lines = stream.getLines();
                assertNotNull(lines);
                assertTrue(lines.size() > 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }, BufferingLogOutputStream.class.getName(), Level.WARNING);
    }

    @Test
    public void testMultipleThreads() {
        TestHelpers.runTestWithDifferentLogLevel(() -> {
            try {
                ThreadTestHelper helper =
                        new ThreadTestHelper(NUMBER_OF_THREADS, NUMBER_OF_TESTS);

                try (final TailLogOutputStream stream = new TailLogOutputStream(10)) {
                    helper.executeTest(new ThreadTestHelper.TestRunnable() {

                        @Override
                        public void doEnd(int threadNum) {
                            // do stuff at the end ...
                        }

                        @Override
                        public void run(int threadNum, int it) {
                            for (int i = 0; i < 100; i++) {
                                stream.processLine("some line", 0);
                            }
                        }
                    });

                    Collection<String> lines = stream.getLines();
                    assertNotNull(lines);
                    assertTrue(lines.size() > 0);
                }
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }, BufferingLogOutputStream.class.getName(), Level.WARNING);
    }

    @Test
    public void testOverride() throws IOException {
        final AtomicBoolean seen = new AtomicBoolean();
        try (TailLogOutputStream stream = new TailLogOutputStream(10) {
            @Override
            protected void processLine(String line, int level) {
                if(line != null && line.equals("some other line")) {
                    seen.set(true);
                }
                super.processLine(line, level);
            }
        }) {

            // sends everything to Level.INFO
            stream.processLine("some line", 0);
            stream.processLine("some line", 0);
            stream.processLine("some other line", 0);

            // test null and empty string
            stream.processLine(null, 0);
            stream.processLine("", 0);

            stream.processLine("some line", 0);

            Collection<String> lines = stream.getLines();
            assertNotNull(lines);
            assertTrue(lines.size() > 0);
        }

        assertTrue(seen.get());
    }
}
package org.apache.poi.benchmark;

import com.google.common.collect.EvictingQueue;
import org.dstadler.commons.exec.BufferingLogOutputStream;

import java.util.Collection;
import java.util.Queue;

/**
 * An extension to {@link BufferingLogOutputStream} which additionally
 * keeps the last few lines of output in a buffer so they can be retrieved
 * even when all the lines were sent to the logging system already and thus
 * cannot be retrieved any more.
 */
public class TailLogOutputStream extends BufferingLogOutputStream {
    private final Queue<String> lastLines;

    public TailLogOutputStream(int capacity) {
        //noinspection UnstableApiUsage
        lastLines = EvictingQueue.create(capacity);
    }

    @Override
    protected void processLine(String line, int level) {
        synchronized (lastLines) {
            lastLines.add(line);
        }

        super.processLine(line, level);
    }

    /**
     * Return the last lines that were sent to the logging system
     *
     * @return A collection of log-lines up to the limit
     *      that is specified in the constructor.
     */
    public Collection<String> getLines() {
        return lastLines;
    }
}

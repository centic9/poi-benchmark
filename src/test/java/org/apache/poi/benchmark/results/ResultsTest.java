package org.apache.poi.benchmark.results;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
class ResultsTest {
    @Test
    public void testOneFile() throws IOException {
        Results results = new Results();

        File resultsDir = new File("results");
        File[] files = resultsDir.listFiles((FilenameFilter) new SuffixFileFilter("-results.json"));
        assertNotNull(files);
        assertTrue(files.length > 0);

        String date = results.readFiles(new File[]{files[0], files[1]});
        assertNotNull(date);

        Map<String, Map<String, Double>> values = results.getValues();
        assertNotNull(values);
        assertTrue(values.size() > 0);
    }
}
package org.apache.poi.benchmark.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read all json-files and prepare a map of benchmarks with their
 * daily results.
 */
public class Results {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Map<String, Double>> values = new TreeMap<>();

    /**
     * Read results from the given list of json-files.
     *
     * @param files An array of .json files with results
     * @return The highest found date
     * @throws IOException If reading a file fails
     */
    public String readFiles(File[] files) throws IOException {
        String maxDateStr = null;
        for(File file : files) {
            String date = file.getName().replace("-results.json", "");

            //noinspection unchecked
            Map<String,Object>[] userData = mapper.readValue(file, Map[].class);
            for(Map<String,Object> data : userData) {
                //noinspection unchecked
                Map<String, Object> primaryMetric = (Map<String, Object>) data.get("primaryMetric");

                String benchmark = data.get("benchmark").toString();
                double value = Double.parseDouble(primaryMetric.get("score").toString());
                //System.out.println("File " + file + ": Found: " + benchmark + ": " + value);

                Map<String,Double> benchmarkValues = values.get(benchmark);
                if(benchmarkValues == null) {
                    benchmarkValues = new HashMap<>();
                }
                benchmarkValues.put(date, value);
                values.put(benchmark, benchmarkValues);

                if(maxDateStr == null || maxDateStr.compareTo(date) <= 0) {
                    maxDateStr = date;
                }
            }
        }

        Preconditions.checkNotNull(maxDateStr, "Should have a max date now!");

        return maxDateStr;
    }

    public Map<String, Map<String, Double>> getValues() {
        return values;
    }
}

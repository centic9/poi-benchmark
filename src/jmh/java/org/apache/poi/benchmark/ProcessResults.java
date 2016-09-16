package org.apache.poi.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class ProcessResults {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Date START_DATE;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    static {
        try {
            START_DATE = DATE_FORMAT.parse("2016-04-27");
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse date", e);
        }
    }

    private static final String TEMPLATE =
        "<html>\n" +
        "<head>" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/dygraph/1.1.1/dygraph-combined.js\"></script>\n" +
                "<style>#graphdiv { position: absolute; left: 10px; right: 10px; top: 10px; bottom: 10px; }</style>\n" +
        "</head>\n" +
        "<body>" +
                "<div id=\"graphdiv\"></div>\n" +
                "<script type=\"text/javascript\">\n" +
                "  g = new Dygraph(\n" +
                "\n" +
                "    // containing div\n" +
                "    document.getElementById(\"graphdiv\"),\n" +
                "\n" +
                "    // CSV or path to a CSV file.\n" +
                "    \"${dataheader}\\n\" +\n" +
                "   ${data},\n" +
                "    {\n" +

                // http://dygraphs.com/tutorial.html
                // http://dygraphs.com/options.html

                "       title: 'Results for \\'${benchmark}\\'',\n" +

                // Step-Chart
//                "       stepPlot: true,\n" +
                "       fillGraph: true,\n" +

                // Dot-Chart
                "       drawPoints: true,\n" +
                //"       strokeWidth: 0.0,\n" +

                "       includeZero: true,\n" +
                "       xRangePad: 20,\n" +

                "       yAxisLabelWidth: 60,\n" +
                //"       yLabelWidth: 100,\n" +

                "   axes: {\n" +
                "       y: {\n" +
                "                valueFormatter: function(y) {\n" +
                "                  return parseFloat(Math.round(y * 100) / 100).toFixed(2) + 's';\n" +
                "                },\n" +
                "                axisLabelFormatter: function(y) {\n" +
                "                  return y + 's';\n" +
                "                }\n" +
                "              }\n" +
                "   },\n" +

                //"      rollPeriod: 7,\n" +
                //"      showRoller: true,\n" +
                //"connectSeparatedPoints: true,\n" +
                //"       drawPoints: true\n" +
                "colors: ['#000000', '#ff0000', '#ff8000', '#ffff00', '#40ff00', '#0040ff', '#ff00ff', '#757e83', '#75c5d5', '#663300'],\n" +
                "    }\n" +
                "\n" +
                "  );\n" +
                "\n" +
                "  g.ready(function() {\n" +
                "    g.setAnnotations([\n" +
                "    {series: \"Test.TestOOXMLLite\",x: \"2016-08-01\",shortText: \"A\",text: \"OOXMLLite build change\"},\n" +
                "    {series: \"Test.TestIntegration\",x: \"2016-09-15\",shortText: \"B\",text: \"Server upgrade\",attachAtBottom: true},\n" +
                "    {series: \"Test.TestOOXMLLite\",x: \"2016-09-17\",shortText: \"C\",text: \"OOXMLLite enabled again\"},\n" +
                "    ]);\n" +
                "  });\n" +
                "</script>\n" +
        "</body>\n" +
        "</html>\n";

    public static void main(String[] args) throws IOException, ParseException {
        //<script src="//cdnjs.cloudflare.com/ajax/libs/dygraph/1.1.1/dygraph-combined.js"></script>
        File[] files = new File("results").listFiles((FilenameFilter) new SuffixFileFilter("-results.json"));

        Map<String, Map<String,Double>> values = new TreeMap<>();
        String maxDateStr = readFiles(files, values);

        generateHtmlFiles(values, maxDateStr);
    }

    private static String readFiles(File[] files, Map<String, Map<String, Double>> values) throws IOException {
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
                System.out.println("File " + file + ": Found: " + benchmark + ": " + value);

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

    private static String getBenchmarkName(String benchmark) {
        return StringUtils.removeStart(benchmark, "org.apache.poi.benchmark.suite.").replace("Benchmarks.benchmark", ".");
    }

    private static String getBenchmarkNames(Set<String> names) {
        StringBuilder benchmarkNames = new StringBuilder();
        for(String benchmark : names) {
            benchmarkNames.append(getBenchmarkName(benchmark)).append(",");
        }

        // remove last trailing ","
        benchmarkNames.setLength(benchmarkNames.length() - 1);

        return benchmarkNames.toString();
    }

    private static void generateHtmlFiles(Map<String, Map<String, Double>> values, String maxDateStr) throws ParseException, IOException {
        Set<String> dates = new TreeSet<>();

        StringBuilder overviewHtml = new StringBuilder("<html><body><h1>Available Benchmarks for Apache POI</h1><br/>\n");
        overviewHtml.append("Having data from ").append(DATE_FORMAT.format(START_DATE)).
                append(" to ").append(maxDateStr).append("<br/><br/><br/>");

        // one file per benchmark
        for(String benchmark : values.keySet()) {
            Map<String, Double> dateItems = values.get(benchmark);

            StringBuilder data = new StringBuilder();
            Date date = START_DATE;
            while(date.compareTo(DATE_FORMAT.parse(maxDateStr)) <= 0) {
                String dateStr = DATE_FORMAT.format(date);
                Double value = dateItems.get(dateStr);

                // Format: "    \"2008-05-07,75\\n\" +\n" +
                data.append("\"").append(dateStr).append(",").append(formatValue(value)).append("\\n\" + \n");

                date = DateUtils.addDays(date, 1);

                // keep for combined file
                dates.add(dateStr);
            }

            // remove last trailing "+"
            data.setLength(data.length() - 3);

            String html = TEMPLATE.replace("${data}", data);
            html = html.replace("${dataheader}", "Date,Time");
            html = html.replace("${benchmark}", getBenchmarkName(benchmark));

            FileUtils.writeStringToFile(new File("results", benchmark + ".html"), html);

            overviewHtml.append("<a href=\"").append(benchmark).append(".html\">").
                    append(getBenchmarkName(benchmark)).append("</a><br/>\n");
        }

        StringBuilder combinedData = new StringBuilder();
        for(String dateStr : dates) {
            combinedData.append("\"").append(dateStr);
            for(String benchmark : values.keySet()) {
                Double value = values.get(benchmark).get(dateStr);
                combinedData.append(",").append(formatValue(value));
            }
            combinedData.append("\\n\" + \n");
        }

        // remove last trailing "+"
        combinedData.setLength(combinedData.length() - 3);

        String html = TEMPLATE.replace("${data}", combinedData);
        html = html.replace("${dataheader}", "Date," + getBenchmarkNames(values.keySet()));
        html = html.replace("${benchmark}", "Combined");

        FileUtils.writeStringToFile(new File("results", "combined.html"), html);
        overviewHtml.append("<br/><a href=\"combined.html\">Combined</a><br/>");

        overviewHtml.append("</body></html>");

        FileUtils.writeStringToFile(new File("results", "results.html"), overviewHtml.toString());
    }

    private static String formatValue(Double value) {
        return value == null ? "" : "" + value/1000;
    }
}

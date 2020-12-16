package org.apache.poi.benchmark;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.benchmark.results.Results;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

public class ProcessResults {
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private enum ResultType {
        all, last90Days
    }

    private static final String TEMPLATE =
        "<html>\n" +
        "<head>\n" +
                "    <!-- downloaded from https://cdnjs.cloudflare.com/ajax/libs/dygraph/2.0.0/dygraph.min.css -->\n" +
                "    <link rel=\"stylesheet\" href=\"dygraph.min.css\">\n" +
                '\n' +
                "    <!-- downloaded from https://cdnjs.cloudflare.com/ajax/libs/dygraph/2.0.0/dygraph.min.js -->\n" +
                "    <script src=\"dygraph.min.js\"></script>\n" +
                "    <style>#graphdiv { position: absolute; left: 10px; right: 10px; top: 10px; bottom: 10px; }</style>\n" +
        "</head>\n" +
        "<body>\n" +
                "<div id=\"graphdiv\"></div>\n" +
                "<script type=\"text/javascript\">\n" +
                "  g = new Dygraph(\n" +
                '\n' +
                "    // containing div\n" +
                "    document.getElementById(\"graphdiv\"),\n" +
                '\n' +
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
                "       legend: 'always',\n" +

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
                '\n' +
                "  );\n" +
                '\n' +
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
        File resultsDir = new File("results");
        File[] files = resultsDir.listFiles((FilenameFilter) new SuffixFileFilter("-results.json"));
        Preconditions.checkNotNull(files, "Directory %s does not exist",
                resultsDir.getAbsolutePath());

        System.out.println("Found " + files.length + " file to process in directory " + resultsDir.getAbsolutePath());

        Results results = new Results();
        String maxDateStr = results.readFiles(files);

        Map<String, Map<String, Double>> values = results.getValues();
        generateHtmlFiles(values, DATE_FORMAT.parse("2016-04-27"), maxDateStr, resultsDir, ResultType.all);

        // produce charts for only the last 30 days
        generateHtmlFiles(values, DateUtils.addDays(new Date(), -90), maxDateStr, new File(resultsDir, "month"), ResultType.last90Days);
    }

    private static String getBenchmarkName(String benchmark) {
        return StringUtils.removeStart(benchmark, "org.apache.poi.benchmark.suite.").
                replace("Benchmarks.benchmark", ".").
                replace("SSPerformance.", "");
    }

    private static String getBenchmarkNames(Set<String> names, Predicate<String> isIncluded) {
        StringBuilder benchmarkNames = new StringBuilder();
        for(String benchmark : names) {
            if(!isIncluded.test(benchmark)) {
                continue;
            }
            benchmarkNames.append(getBenchmarkName(benchmark)).append(",");
        }

        // remove last trailing ","
        benchmarkNames.setLength(benchmarkNames.length() - 1);

        return benchmarkNames.toString();
    }

    private static void generateHtmlFiles(Map<String, Map<String, Double>> values, Date startDate, String maxDateStr,
                                          File resultsDir, ResultType resultType) throws ParseException, IOException {
        Set<String> dates = new TreeSet<>();

        StringBuilder overviewHtml = new StringBuilder("<html><body><h1>Available Benchmarks for Apache POI" +
                (resultType == ResultType.last90Days ? " for last 90 days" : "") + "</h1><br/>\n");
        overviewHtml.append("Having data from ").append(DATE_FORMAT.format(startDate)).
                append(" to ").append(maxDateStr).append("<br/><br/><br/>");

        // one file per benchmark
        for(String benchmark : values.keySet()) {
            Map<String, Double> dateItems = values.get(benchmark);

            StringBuilder data = new StringBuilder();
            Date date = startDate;
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
            if(data.length() >= 3) {
                data.setLength(data.length() - 3);
            }

            overviewHtml.append("<a href=\"").append(benchmark).append(".html\">").
                    append(getBenchmarkName(benchmark)).append("</a><br/>\n");

            writeHtml(data, "Date,Time", getBenchmarkName(benchmark), benchmark, resultsDir);
        }

        writeCombined(values, dates, overviewHtml, "combined", "Combined", s -> true, resultsDir);
        writeCombined(values, dates, overviewHtml, "ssperformance", "SSPerformance", input -> input.contains("SSPerformance"), resultsDir);

        if(resultType == ResultType.all) {
            overviewHtml.append("<br/><a href=\"month/results.html\">Last 90 days</a>");
        } else {
            overviewHtml.append("<br/><a href=\"../results.html\">Full time range</a>");
        }
        overviewHtml.append("</body></html>");

        System.out.println("Writing overview to result.html");
        FileUtils.writeStringToFile(new File(resultsDir, "results.html"), overviewHtml.toString(), "UTF-8");
    }

    private static void writeCombined(Map<String, Map<String, Double>> values, Set<String> dates,
                                      StringBuilder overviewHtml, String fileName, String groupName,
                                      Predicate<String> isIncluded, File resultsDir) throws IOException {
        StringBuilder combinedData = new StringBuilder();
        for(String dateStr : dates) {
            combinedData.append("\"").append(dateStr);
            for(String benchmark : values.keySet()) {
                if(!isIncluded.test(benchmark)) {
                    continue;
                }
                Double value = values.get(benchmark).get(dateStr);
                combinedData.append(",").append(formatValue(value));
            }
            combinedData.append("\\n\" + \n");
        }

        // remove last trailing "+"
        if(combinedData.length() >= 3) {
            combinedData.setLength(combinedData.length() - 3);
        }

        writeHtml(combinedData, "Date," + getBenchmarkNames(values.keySet(), isIncluded), groupName, fileName, resultsDir);

        overviewHtml.append("<br/><a href=\"").append(fileName).append(".html\">").append(groupName).append("</a><br/>\n");
    }

    private static void writeHtml(StringBuilder data, String dataHeader, String benchmark, String fileName, File resultsDir) throws IOException {
        String html = TEMPLATE.replace("${data}", data);
        html = html.replace("${dataheader}", dataHeader);
        html = html.replace("${benchmark}", benchmark);

        System.out.println("Writing report to " + fileName + ".html for " + benchmark);
        FileUtils.writeStringToFile(new File(resultsDir, fileName + ".html"), html, "UTF-8");
    }

    private static String formatValue(Double value) {
        return value == null ? "" : "" + value/1000;
    }
}

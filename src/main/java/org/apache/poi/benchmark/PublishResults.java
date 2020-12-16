package org.apache.poi.benchmark;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.benchmark.email.EmailSender;
import org.apache.poi.benchmark.email.PropertyAccess;
import org.dstadler.commons.email.EmailConfig;
import org.dstadler.commons.email.MailserverConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PublishResults {
    private static final File REPORTS_DIR = new File("build/reports/jmh");
    private static final File RESULTS_DIR = new File("results");

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final String TODAY = DATE_FORMAT.format(new Date());
    private static final File LOG_FILE = new File("poi-benchmark.0.0.log");
    private static final File OUTPUT_FILE = new File("benchmark.log");

    public static void main(String[] args) throws IOException {
        // read mail-config
        MailserverConfig config = getMailserverConfig();

        if(!REPORTS_DIR.exists()) {
            sendReportNotFound(config);

            return;
        }

        sendReport(config);

        copyReport();
    }

    private static void copyReport() throws IOException {
        File[] files = REPORTS_DIR.listFiles();
        Preconditions.checkNotNull(files, "Did not find files at %s", REPORTS_DIR.getAbsolutePath());

        for(File file : files) {
            File destFile = new File(RESULTS_DIR, TODAY + "-" + file.getName());

            Preconditions.checkState(!destFile.exists(), "Should not have the destination file %s, but it already exists!", destFile.getAbsolutePath());
            System.out.println("Copying file from " + file + " to " + destFile);

            FileUtils.copyFile(file, destFile);
        }
    }

    private static void sendReport(MailserverConfig config) throws IOException {
        EmailSender sender = new EmailSender();

        File[] dirFiles = REPORTS_DIR.listFiles();
        Preconditions.checkNotNull(dirFiles, "Did not find directory " + REPORTS_DIR.getAbsolutePath());        List<File> files = new ArrayList<>(Arrays.asList(dirFiles));

        // Add the Java log file
        if(LOG_FILE.exists()) {
            files.add(LOG_FILE);
        }

        // Add the logfile produced via redirection of commands in the shell-script
        if(OUTPUT_FILE.exists()) {
            files.add(OUTPUT_FILE);
        }

        Preconditions.checkNotNull(files, "Should have the directory " + REPORTS_DIR);

        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom("benchmark@poi.apache.org");
        email.setSubject("Apache POI benchmark results");

        List<String> lines = FileUtils.readLines(new File(REPORTS_DIR, "human.txt"), "UTF-8");
        Iterator<String> it = lines.iterator();
        boolean prevWasNewline = false;

        // remove lines starting with "# " and remove duplicate empty lines
        while(it.hasNext()) {
            String line = it.next();
            if(line.startsWith("# ") || line.startsWith("Iteration   1: ")) {
                it.remove();
            } else {
                if(line.isEmpty()) {
                    if(prevWasNewline) {
                        it.remove();
                    } else {
                        prevWasNewline = true;
                    }
                } else {
                    prevWasNewline = false;
                }
            }
        }

        String report = String.join("\n", lines);
        String msg = "Nightly benchmarks for Apache POI have finished, see the attached files for details.<br/><br/>" +
                "Charts are available <a href=\"https://rawgit.com/centic9/poi-benchmark/master/results/results.html\">here</a>, " +
                "see also <a href=\"https://github.com/centic9/poi-benchmark\">https://github.com/centic9/poi-benchmark</a><br/><br/>" +
                "<pre>" + report + "</pre>";
        System.out.println("Sending email to " + email + " with content: " + msg + " and config " + config);
        sender.sendAttachmentEmail(files, config, email, msg);
    }

    private static void sendReportNotFound(MailserverConfig config) throws IOException {
        EmailSender sender = new EmailSender();

        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom("benchmark@poi.apache.org");
        email.setSubject("No JMH reports found");

        sender.sendAttachmentEmail(Collections.emptyList(), config, email,
                "Could not find any results in directory " + REPORTS_DIR + " - " + REPORTS_DIR.getAbsolutePath());
    }

    protected static MailserverConfig getMailserverConfig() {
        MailserverConfig config = new MailserverConfig();
        config.setBounce(PropertyAccess.getProperty("mail.bounce"));
        config.setDebug(Boolean.parseBoolean(PropertyAccess.getProperty("mail.debug")));
        config.setUserId(PropertyAccess.getProperty("mail.user"));
        config.setPassword(PropertyAccess.getProperty("mail.password"));
        config.setServerAddress(PropertyAccess.getProperty("mail.server"));
        config.setServerPort(Integer.parseInt(PropertyAccess.getProperty("mail.port")));
        config.setSSLEnabled(Boolean.parseBoolean(PropertyAccess.getProperty("mail.ssl")));
        config.setSubjectPrefix("[POI-Benchmark] ");
        return config;
    }
}

package org.apache.poi.benchmark;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.poi.benchmark.email.EmailConfig;
import org.apache.poi.benchmark.email.EmailSender;
import org.apache.poi.benchmark.email.MailserverConfig;
import org.apache.poi.benchmark.email.PropertyAccess;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PublishResults {
    private static final File REPORTS_DIR = new File("build/reports/jmh");

    public static void main(String[] args) throws IOException {
        // read mail-config
        MailserverConfig config = getMailserverConfig();

        if(!REPORTS_DIR.exists()) {
            sendReportNotFound(config);

            return;
        }

        sendReport(config);
    }

    private static void sendReport(MailserverConfig config) throws IOException {
        EmailSender sender = new EmailSender();

        File[] files = REPORTS_DIR.listFiles();
        Preconditions.checkNotNull(files, "Should have the directory " + REPORTS_DIR);

        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom("benchmark@poi.apache.org");
        email.setSubject("Apache POI benchmark results");

        List<String> lines = FileUtils.readLines(new File(REPORTS_DIR, "human.txt"));
        Iterator<String> it = lines.iterator();
        boolean prevWasNewline = false;

        // remove lines starting with "# " and remove duplicate empty lines
        while(it.hasNext()) {
            String line = it.next();
            if(line.startsWith("# ")) {
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
        sender.sendAttachmentEmail(Arrays.asList(files), config, email,
                "Nightly benchmarks for Apache POI have finished, see the attached files for details.<br/><br/>" +
                "<pre>" + report + "</pre>");
    }

    private static void sendReportNotFound(MailserverConfig config) throws IOException {
        EmailSender sender = new EmailSender();

        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom("benchmark@poi.apache.org");
        email.setSubject("No JMH reports found");

        sender.sendAttachmentEmail(Collections.emptyList(), config, email,
                "Could not find any results in directory " + REPORTS_DIR);
    }

    private static MailserverConfig getMailserverConfig() {
        MailserverConfig config = new MailserverConfig();
        config.setBounce(PropertyAccess.getProperty("mail.bounce"));
        config.setDebug(Boolean.parseBoolean(PropertyAccess.getProperty("mail.debug")));
        config.setUserId(PropertyAccess.getProperty("mail.user"));
        config.setPassword(PropertyAccess.getProperty("mail.password"));
        config.setServerAddress(PropertyAccess.getProperty("mail.server"));
        config.setServerPort(Integer.parseInt(PropertyAccess.getProperty("mail.port")));
        config.setSSLEnabled(Boolean.parseBoolean(PropertyAccess.getProperty("mail.ssl")));
        return config;
    }
}

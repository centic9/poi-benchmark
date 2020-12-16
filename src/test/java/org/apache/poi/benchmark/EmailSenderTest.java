package org.apache.poi.benchmark;

import org.apache.poi.benchmark.email.EmailSender;
import org.apache.poi.benchmark.email.PropertyAccess;
import org.dstadler.commons.email.EmailConfig;
import org.dstadler.commons.email.MailserverConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

class EmailSenderTest {
    @Disabled("Just used for local testing")
    @Test
    public void sendEmail() throws IOException {
        // read mail-config
        MailserverConfig config = PublishResults.getMailserverConfig();

        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom("benchmark@poi.apache.org");
        email.setSubject("Apache POI benchmark results - Test Email");

        EmailSender sender = new EmailSender();
        sender.sendAttachmentEmail(Collections.emptyList(), config, email, "<html>test</html>");

        System.out.println("Sent an email: " + email + " with config " + config);
    }
}

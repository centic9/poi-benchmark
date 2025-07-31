package org.apache.poi.benchmark;

import org.apache.poi.benchmark.email.EmailSender;
import org.apache.poi.benchmark.email.PropertyAccess;
import org.dstadler.commons.email.EmailConfig;
import org.dstadler.commons.email.MailserverConfig;
import org.dstadler.commons.email.MockSMTPServer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailSenderTest {
    @BeforeAll
    static void setUp() {
        Assumptions.assumeTrue(new File("src/main/resources/benchmark.properties").exists(),
                "Need 'benchmark.properties' to run this test");
    }

    @Test
    void testMissingConfig() {
        EmailSender sender = new EmailSender();
        assertThrows(IOException.class, () ->
                sender.sendAttachmentEmail(Collections.emptyList(), null, null, null));

        assertThrows(IOException.class, () ->
                sender.sendAttachmentEmail(Collections.emptyList(), new MailserverConfig(),
                        null, null));

        assertThrows(IOException.class, () ->
                sender.sendAttachmentEmail(Collections.emptyList(), null,
                        getEmailConfig(), null));

        assertThrows(IOException.class, () ->
                sender.sendAttachmentEmail(Collections.emptyList(), new MailserverConfig(),
                        getEmailConfig(), null));

        assertThrows(IOException.class, () ->
                sender.sendAttachmentEmail(null, new MailserverConfig(),
                        getEmailConfig(), null));
    }

    @Test
    void testSending() throws IOException {
        MockSMTPServer server = new MockSMTPServer();

        server.start();

        try {
            MailserverConfig config = new MailserverConfig();
            config.setServerPort(server.getPort());
            config.setServerAddress("localhost");

            sendAndCheck(Collections.emptyList(), server, config);
        } finally {
            server.stop();
        }
    }

    @Test
    void testSendingWithAuth() throws IOException {
        MockSMTPServer server = new MockSMTPServer();

        server.start();

        try {
            MailserverConfig config = new MailserverConfig();
            config.setServerPort(server.getPort());
            config.setServerAddress("localhost");
            config.setUserId("user");
            config.setPassword("pwd");

            sendAndCheck(Collections.emptyList(), server, config);
        } finally {
            server.stop();
        }
    }

    @Test
    void testSendingWithAttachments() throws IOException {
        MockSMTPServer server = new MockSMTPServer();

        server.start();

        try {
            MailserverConfig config = new MailserverConfig();
            config.setServerPort(server.getPort());
            config.setServerAddress("localhost");

            sendAndCheck(Collections.singletonList(new File("build.gradle")), server, config);
        } finally {
            server.stop();
        }
    }

    @Test
    void testSendingWithInvalidHostname() throws IOException {
        MockSMTPServer server = new MockSMTPServer();

        server.start();

        try {
            MailserverConfig config = new MailserverConfig();
            config.setServerPort(server.getPort());
            config.setServerAddress("not-existing-host");

            assertThrows(IOException.class, () ->
                sendAndCheck(Collections.emptyList(), server, config));
        } finally {
            server.stop();
        }
    }

    @Disabled("Cannot send SSL mail to MockSMTPServer")
    @Test
    void testWithSSL() throws IOException {
        MockSMTPServer server = new MockSMTPServer();

        server.start();

        try {
            MailserverConfig config = new MailserverConfig();
            config.setServerPort(server.getPort());
            config.setServerAddress("localhost");
            config.setSSLEnabled(true);

            sendAndCheck(Collections.emptyList(), server, config);
        } finally {
            server.stop();
        }
    }

    @Disabled("Just used for local testing")
    @Test
    void sendEmail() throws IOException {
        // read mail-config
        MailserverConfig config = PublishResults.getMailserverConfig();

        EmailConfig email = getEmailConfig();

        EmailSender sender = new EmailSender();
        sender.sendAttachmentEmail(Collections.emptyList(), config, email, "<html>test</html>");

        System.out.println("Sent an email: " + email + " with config " + config);
    }

    private void sendAndCheck(List<File> attachments, MockSMTPServer server, MailserverConfig config) throws IOException {
        EmailSender sender = new EmailSender();

        sender.sendAttachmentEmail(attachments, config, getEmailConfig(), "test message sending");

        assertEquals(1, server.getMessageCount());
        assertTrue(server.getMessages().next().contains("test message sending"));
    }

    private EmailConfig getEmailConfig() {
        EmailConfig email = new EmailConfig();
        email.setTo(Collections.singletonList(PropertyAccess.getProperty("mail.to")));
        email.setFrom(PropertyAccess.getProperty("mail.from"));
        email.setSubject("Apache POI benchmark results - Test Email");
        return email;
    }
}

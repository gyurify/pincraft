package com.pinbuttonmaker.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SmtpMailSender {
    private static final String CRLF = "\r\n";

    private final SmtpSettings settings;

    public SmtpMailSender(SmtpSettings settings) {
        this.settings = settings;
    }

    public SendResult sendPasswordResetCode(String recipientEmail, String resetCode, Duration validFor) {
        if (settings == null || !settings.isConfigured()) {
            return SendResult.failure(settings == null
                ? "SMTP is not configured."
                : settings.getConfigurationErrorMessage());
        }

        long validMinutes = Math.max(1L, validFor.toMinutes());
        String body = "We received a request to reset your PinCraft password." + System.lineSeparator()
            + System.lineSeparator()
            + "Reset code: " + resetCode + System.lineSeparator()
            + "This code is valid for about " + validMinutes + " minute(s)." + System.lineSeparator()
            + System.lineSeparator()
            + "Return to the PinCraft app, enter this code, and choose a new password." + System.lineSeparator()
            + "If you did not request this change, you can ignore this email.";

        return sendTextEmail(recipientEmail, "PinCraft password reset code", body);
    }

    public SendResult sendTextEmail(String recipientEmail, String subject, String body) {
        if (settings == null || !settings.isConfigured()) {
            return SendResult.failure(settings == null
                ? "SMTP is not configured."
                : settings.getConfigurationErrorMessage());
        }

        try (SmtpConnection connection = openConnection()) {
            connection.expect(220);
            connection.sendCommand("EHLO pincraft.local", 250);

            if (settings.getSecurityMode() == SmtpSettings.SecurityMode.STARTTLS) {
                connection.sendCommand("STARTTLS", 220);
                connection.upgradeToTls(settings.getHost(), settings.getPort());
                connection.sendCommand("EHLO pincraft.local", 250);
            }

            if (settings.hasAuthentication()) {
                connection.sendCommand("AUTH LOGIN", 334);
                connection.sendCommand(encodeBase64(settings.getUsername()), 334);
                connection.sendCommand(encodeBase64(settings.getPassword()), 235);
            }

            connection.sendCommand("MAIL FROM:<" + settings.getFromAddress() + ">", 250);
            connection.sendCommand("RCPT TO:<" + recipientEmail + ">", 250, 251);
            connection.sendCommand("DATA", 354);
            connection.sendData(buildMessage(recipientEmail, subject, body));
            connection.expect(250);
            connection.sendCommand("QUIT", 221);
            return SendResult.success("Password reset email sent.");
        } catch (Exception exception) {
            return SendResult.failure("Unable to send reset email: " + exception.getMessage());
        }
    }

    private SmtpConnection openConnection() throws IOException {
        Socket socket;
        if (settings.getSecurityMode() == SmtpSettings.SecurityMode.SSL) {
            SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(settings.getHost(), settings.getPort());
            sslSocket.setSoTimeout(10000);
            sslSocket.startHandshake();
            socket = sslSocket;
        } else {
            socket = new Socket();
            socket.connect(new InetSocketAddress(settings.getHost(), settings.getPort()), 10000);
            socket.setSoTimeout(10000);
        }
        return new SmtpConnection(socket);
    }

    private String buildMessage(String recipientEmail, String subject, String body) {
        StringBuilder builder = new StringBuilder();
        builder.append("From: ").append(formatFromHeader()).append(CRLF);
        builder.append("To: <").append(sanitizeHeaderValue(recipientEmail)).append(">").append(CRLF);
        builder.append("Subject: ").append(sanitizeHeaderValue(subject)).append(CRLF);
        builder.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))).append(CRLF);
        builder.append("MIME-Version: 1.0").append(CRLF);
        builder.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
        builder.append("Content-Transfer-Encoding: 8bit").append(CRLF);
        builder.append(CRLF);

        String normalizedBody = body == null ? "" : body.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedBody.split("\n", -1);
        for (String line : lines) {
            if (line.startsWith(".")) {
                builder.append('.');
            }
            builder.append(line).append(CRLF);
        }

        builder.append('.').append(CRLF);
        return builder.toString();
    }

    private String formatFromHeader() {
        String fromAddress = sanitizeHeaderValue(settings.getFromAddress());
        String fromName = sanitizeHeaderValue(settings.getFromName());
        if (fromName.isEmpty()) {
            return "<" + fromAddress + ">";
        }
        return "\"" + fromName + "\" <" + fromAddress + ">";
    }

    private String sanitizeHeaderValue(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "").trim();
    }

    private String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static final class SendResult {
        private final boolean success;
        private final String message;

        private SendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static SendResult success(String message) {
            return new SendResult(true, message);
        }

        public static SendResult failure(String message) {
            return new SendResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class SmtpConnection implements AutoCloseable {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        private SmtpConnection(Socket socket) throws IOException {
            attach(socket);
        }

        private void attach(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private void upgradeToTls(String host, int port) throws IOException {
            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(socket, host, port, true);
            sslSocket.setSoTimeout(10000);
            sslSocket.startHandshake();
            attach(sslSocket);
        }

        private void sendCommand(String command, int... expectedCodes) throws IOException {
            writer.write(command);
            writer.write(CRLF);
            writer.flush();
            expect(expectedCodes);
        }

        private void sendData(String messageData) throws IOException {
            writer.write(messageData);
            writer.flush();
        }

        private void expect(int... expectedCodes) throws IOException {
            SmtpResponse response = readResponse();
            for (int expectedCode : expectedCodes) {
                if (response.code == expectedCode) {
                    return;
                }
            }

            throw new IOException("SMTP " + response.code + ": " + response.message);
        }

        private SmtpResponse readResponse() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("SMTP server closed the connection unexpectedly.");
            }

            if (line.length() < 3) {
                throw new IOException("Malformed SMTP response: " + line);
            }

            int code = Integer.parseInt(line.substring(0, 3));
            StringBuilder message = new StringBuilder(line);

            while (line.length() > 3 && line.charAt(3) == '-') {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("SMTP server closed the connection unexpectedly.");
                }
                message.append(" ").append(line);
            }

            return new SmtpResponse(code, message.toString());
        }

        @Override
        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private static final class SmtpResponse {
        private final int code;
        private final String message;

        private SmtpResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}

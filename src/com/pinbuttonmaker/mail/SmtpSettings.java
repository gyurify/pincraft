package com.pinbuttonmaker.mail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SmtpSettings {
    private static final Path ENV_FILE = Path.of(".env").toAbsolutePath().normalize();

    public enum SecurityMode {
        SSL,
        STARTTLS,
        NONE
    }

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String fromName;
    private final SecurityMode securityMode;

    private SmtpSettings(
        String host,
        int port,
        String username,
        String password,
        String fromAddress,
        String fromName,
        SecurityMode securityMode
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.securityMode = securityMode;
    }

    public static SmtpSettings fromEnvironment() {
        Map<String, String> envFileValues = loadEnvFileValues();
        SecurityMode securityMode = parseSecurityMode(readSetting("pincraft.smtp.security", "PINCRAFT_SMTP_SECURITY", "ssl", envFileValues));
        int defaultPort = securityMode == SecurityMode.STARTTLS ? 587 : (securityMode == SecurityMode.NONE ? 25 : 465);
        int port = parsePort(readSetting("pincraft.smtp.port", "PINCRAFT_SMTP_PORT", String.valueOf(defaultPort), envFileValues), defaultPort);

        return new SmtpSettings(
            readSetting("pincraft.smtp.host", "PINCRAFT_SMTP_HOST", "", envFileValues),
            port,
            readSetting("pincraft.smtp.username", "PINCRAFT_SMTP_USERNAME", "", envFileValues),
            readSetting("pincraft.smtp.password", "PINCRAFT_SMTP_PASSWORD", "", envFileValues),
            readSetting("pincraft.smtp.from", "PINCRAFT_SMTP_FROM", "", envFileValues),
            readSetting("pincraft.smtp.fromName", "PINCRAFT_SMTP_FROM_NAME", "PinCraft", envFileValues),
            securityMode
        );
    }

    public boolean isConfigured() {
        return getMissingSettings().isEmpty();
    }

    public boolean hasAuthentication() {
        return !username.isEmpty();
    }

    public String getConfigurationErrorMessage() {
        List<String> missing = getMissingSettings();
        if (missing.isEmpty()) {
            return "";
        }

        return "SMTP is not configured. Set " + String.join(", ", missing) + ".";
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    private List<String> getMissingSettings() {
        List<String> missing = new ArrayList<>();
        if (host.isEmpty()) {
            missing.add("PINCRAFT_SMTP_HOST or pincraft.smtp.host");
        }
        if (fromAddress.isEmpty()) {
            missing.add("PINCRAFT_SMTP_FROM or pincraft.smtp.from");
        }
        if (hasAuthentication() && password.isEmpty()) {
            missing.add("PINCRAFT_SMTP_PASSWORD or pincraft.smtp.password");
        }
        return missing;
    }

    private static SecurityMode parseSecurityMode(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ENGLISH);
        if ("starttls".equals(normalized) || "tls".equals(normalized)) {
            return SecurityMode.STARTTLS;
        }
        if ("none".equals(normalized) || "plain".equals(normalized)) {
            return SecurityMode.NONE;
        }
        return SecurityMode.SSL;
    }

    private static int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(normalize(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String readSetting(String propertyKey, String envKey, String fallback, Map<String, String> envFileValues) {
        String propertyValue = normalize(System.getProperty(propertyKey));
        if (!propertyValue.isEmpty()) {
            return propertyValue;
        }

        String environmentValue = normalize(System.getenv(envKey));
        if (!environmentValue.isEmpty()) {
            return environmentValue;
        }

        String envFileValue = normalize(envFileValues.get(envKey));
        if (!envFileValue.isEmpty()) {
            return envFileValue;
        }

        return fallback;
    }

    private static Map<String, String> loadEnvFileValues() {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.exists(ENV_FILE)) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(ENV_FILE)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                values.put(key, stripMatchingQuotes(value));
            }
        } catch (IOException ignored) {
            // Ignore malformed or unreadable local env files and fall back to system environment variables.
        }

        return values;
    }

    private static String stripMatchingQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

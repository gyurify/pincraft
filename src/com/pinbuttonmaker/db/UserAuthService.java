package com.pinbuttonmaker.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class UserAuthService {
    private final DatabaseManager databaseManager;

    public UserAuthService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public AuthResult register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = password == null ? "" : password.trim();

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.failure("Enter a valid email address.");
        }
        if (normalizedPassword.isEmpty()) {
            return AuthResult.failure("Password is required.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            if (userExists(connection, normalizedEmail)) {
                return AuthResult.failure("That email is already registered.");
            }

            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (email, password_hash) VALUES (?, ?)"
            )) {
                insert.setString(1, normalizedEmail);
                insert.setString(2, hashPassword(normalizedPassword));
                insert.executeUpdate();
            }

            return AuthResult.success(normalizedEmail, "Registration successful.");
        } catch (SQLException exception) {
            return AuthResult.failure("Registration failed: " + exception.getMessage());
        }
    }

    public AuthResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = password == null ? "" : password.trim();

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.failure("Enter a valid email address.");
        }
        if (normalizedPassword.isEmpty()) {
            return AuthResult.failure("Password is required.");
        }

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement query = connection.prepareStatement(
                 "SELECT password_hash FROM users WHERE email = ?"
             )) {
            query.setString(1, normalizedEmail);

            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next()) {
                    return AuthResult.failure("No account found for that email.");
                }

                String storedHash = resultSet.getString("password_hash");
                String suppliedHash = hashPassword(normalizedPassword);
                if (!suppliedHash.equals(storedHash)) {
                    return AuthResult.failure("Incorrect password.");
                }
            }

            return AuthResult.success(normalizedEmail, "Login successful.");
        } catch (SQLException exception) {
            return AuthResult.failure("Login failed: " + exception.getMessage());
        }
    }

    public boolean isDatabaseReady() {
        return databaseManager.isAvailable();
    }

    public String getDatabaseStatusMessage() {
        return databaseManager.getStatusMessage();
    }

    private boolean userExists(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT 1 FROM users WHERE email = ? LIMIT 1"
        )) {
            query.setString(1, email);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ENGLISH);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password.", exception);
        }
    }

    public static final class AuthResult {
        private final boolean success;
        private final String userEmail;
        private final String message;

        private AuthResult(boolean success, String userEmail, String message) {
            this.success = success;
            this.userEmail = userEmail;
            this.message = message;
        }

        public static AuthResult success(String userEmail, String message) {
            return new AuthResult(true, userEmail, message);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public String getMessage() {
            return message;
        }
    }
}

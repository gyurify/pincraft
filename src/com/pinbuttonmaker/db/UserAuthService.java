package com.pinbuttonmaker.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import com.pinbuttonmaker.mail.SmtpMailSender;

public class UserAuthService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final Duration PASSWORD_RESET_WINDOW = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager databaseManager;
    private final SmtpMailSender mailSender;

    public UserAuthService(DatabaseManager databaseManager, SmtpMailSender mailSender) {
        this.databaseManager = databaseManager;
        this.mailSender = mailSender;
    }

    public AuthResult register(String email, String password) {
        //create a new account after validation.
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password);

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
            if (findUserRecord(connection, normalizedEmail) != null) {
                return AuthResult.failure("That email is already registered.");
            }

            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (email, password_hash) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )) {
                insert.setString(1, normalizedEmail);
                insert.setString(2, hashPassword(normalizedPassword));
                insert.executeUpdate();

                long userId = readGeneratedUserId(insert);
                return AuthResult.success(userId, normalizedEmail, "Registration successful.");
            }
        } catch (SQLException exception) {
            return AuthResult.failure("Registration failed: " + exception.getMessage());
        }
    }

    public AuthResult login(String email, String password) {
        //check the email and password against the stored hash.
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password);

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
            UserRecord user = findUserRecord(connection, normalizedEmail);
            if (user == null) {
                return AuthResult.failure("No account found for that email.");
            }

            String suppliedHash = hashPassword(normalizedPassword);
            if (!suppliedHash.equals(user.passwordHash)) {
                return AuthResult.failure("Incorrect password.");
            }

            return AuthResult.success(user.id, user.email, "Login successful.");
        } catch (SQLException exception) {
            return AuthResult.failure("Login failed: " + exception.getMessage());
        }
    }

    public AuthResult signInWithMockProvider(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.failure("Enter a valid email address.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            UserRecord existingUser = findUserRecord(connection, normalizedEmail);
            if (existingUser != null) {
                return AuthResult.success(existingUser.id, existingUser.email, "Login successful.");
            }

            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (email, password_hash) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )) {
                insert.setString(1, normalizedEmail);
                insert.setString(2, hashPassword(UUID.randomUUID().toString()));
                insert.executeUpdate();
                return AuthResult.success(readGeneratedUserId(insert), normalizedEmail, "Login successful.");
            }
        } catch (SQLException exception) {
            return AuthResult.failure("Mock sign-in failed: " + exception.getMessage());
        }
    }

    public AuthResult requestPasswordReset(String email) {
        //create and email a short-lived reset code.
        String normalizedEmail = normalizeEmail(email);

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.failure("Enter a valid email address.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            UserRecord user = findUserRecord(connection, normalizedEmail);
            if (user == null) {
                return AuthResult.failure("No account found for that email.");
            }

            String resetCode = generateResetCode();
            clearPasswordResetCodes(connection, user.id);
            savePasswordResetCode(connection, user.id, resetCode);

            SmtpMailSender.SendResult sendResult = mailSender.sendPasswordResetCode(
                normalizedEmail,
                resetCode,
                PASSWORD_RESET_WINDOW
            );

            if (!sendResult.isSuccess()) {
                clearPasswordResetCodes(connection, user.id);
                return AuthResult.failure(sendResult.getMessage());
            }

            return AuthResult.success(user.id, normalizedEmail, "A reset code was sent to " + normalizedEmail + ".");
        } catch (SQLException exception) {
            return AuthResult.failure("Unable to start password reset: " + exception.getMessage());
        }
    }

    public AuthResult resetPassword(String email, String resetCode, String newPassword) {
        //verify the reset code before changing the password hash.
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = resetCode == null ? "" : resetCode.trim();
        String normalizedPassword = normalizePassword(newPassword);

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.failure("Enter a valid email address.");
        }
        if (normalizedCode.isEmpty()) {
            return AuthResult.failure("Enter the reset code from your email.");
        }
        if (normalizedPassword.isEmpty()) {
            return AuthResult.failure("Enter a new password.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            UserRecord user = findUserRecord(connection, normalizedEmail);
            if (user == null) {
                return AuthResult.failure("No account found for that email.");
            }

            PasswordResetCode storedCode = findLatestPasswordResetCode(connection, user.id);
            if (storedCode == null) {
                return AuthResult.failure("Request a new reset code first.");
            }
            if (storedCode.isExpired()) {
                clearPasswordResetCodes(connection, user.id);
                return AuthResult.failure("That reset code has expired. Request a new one.");
            }
            if (!storedCode.codeHash.equals(hashPassword(normalizedCode))) {
                return AuthResult.failure("Incorrect reset code.");
            }

            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE users SET password_hash = ? WHERE id = ?"
            )) {
                update.setString(1, hashPassword(normalizedPassword));
                update.setLong(2, user.id);
                update.executeUpdate();
            }

            clearPasswordResetCodes(connection, user.id);
            return AuthResult.success(user.id, normalizedEmail, "Password updated. You can sign in now.");
        } catch (SQLException exception) {
            return AuthResult.failure("Unable to reset the password: " + exception.getMessage());
        }
    }

    public AuthResult changePassword(long userId, String currentPassword, String newPassword) {
        //require the current password before allowing a password change.
        String normalizedCurrentPassword = normalizePassword(currentPassword);
        String normalizedNewPassword = normalizePassword(newPassword);

        if (!databaseManager.isAvailable()) {
            return AuthResult.failure(databaseManager.getStatusMessage());
        }
        if (userId <= 0) {
            return AuthResult.failure("Sign in again to change your password.");
        }
        if (normalizedCurrentPassword.isEmpty()) {
            return AuthResult.failure("Enter your current password.");
        }
        if (normalizedNewPassword.isEmpty()) {
            return AuthResult.failure("Enter a new password.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            UserRecord user = findUserRecordById(connection, userId);
            if (user == null) {
                return AuthResult.failure("Sign in again to change your password.");
            }

            if (!hashPassword(normalizedCurrentPassword).equals(user.passwordHash)) {
                return AuthResult.failure("Current password is incorrect.");
            }

            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE users SET password_hash = ? WHERE id = ?"
            )) {
                update.setString(1, hashPassword(normalizedNewPassword));
                update.setLong(2, user.id);
                update.executeUpdate();
            }

            clearPasswordResetCodes(connection, user.id);
            return AuthResult.success(user.id, user.email, "Password changed successfully.");
        } catch (SQLException exception) {
            return AuthResult.failure("Unable to change the password: " + exception.getMessage());
        }
    }

    public boolean isDatabaseReady() {
        return databaseManager.isAvailable();
    }

    public String getDatabaseStatusMessage() {
        return databaseManager.getStatusMessage();
    }

    private UserRecord findUserRecord(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT id, email, password_hash FROM users WHERE email = ? LIMIT 1"
        )) {
            query.setString(1, email);
            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new UserRecord(
                    resultSet.getLong("id"),
                    resultSet.getString("email"),
                    resultSet.getString("password_hash")
                );
            }
        }
    }

    private UserRecord findUserRecordById(Connection connection, long userId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT id, email, password_hash FROM users WHERE id = ? LIMIT 1"
        )) {
            query.setLong(1, userId);
            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new UserRecord(
                    resultSet.getLong("id"),
                    resultSet.getString("email"),
                    resultSet.getString("password_hash")
                );
            }
        }
    }

    private PasswordResetCode findLatestPasswordResetCode(Connection connection, long userId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "SELECT code_hash, expires_at FROM password_reset_codes WHERE user_id = ? "
                + "ORDER BY created_at DESC, id DESC LIMIT 1"
        )) {
            query.setLong(1, userId);
            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new PasswordResetCode(
                    resultSet.getString("code_hash"),
                    Instant.parse(resultSet.getString("expires_at"))
                );
            }
        }
    }

    private void savePasswordResetCode(Connection connection, long userId, String resetCode) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO password_reset_codes (user_id, code_hash, expires_at) VALUES (?, ?, ?)"
        )) {
            insert.setLong(1, userId);
            insert.setString(2, hashPassword(resetCode));
            insert.setString(3, TIMESTAMP_FORMATTER.format(Instant.now().plus(PASSWORD_RESET_WINDOW)));
            insert.executeUpdate();
        }
    }

    private void clearPasswordResetCodes(Connection connection, long userId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
            "DELETE FROM password_reset_codes WHERE user_id = ?"
        )) {
            delete.setLong(1, userId);
            delete.executeUpdate();
        }
    }

    private long readGeneratedUserId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Unable to determine the created user id.");
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

    private String normalizePassword(String password) {
        return password == null ? "" : password.trim();
    }

    private String generateResetCode() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String hashPassword(String password) {
        try {
            //store hashes instead of plain text passwords.
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
        private final Long userId;
        private final String userEmail;
        private final String message;

        private AuthResult(boolean success, Long userId, String userEmail, String message) {
            this.success = success;
            this.userId = userId;
            this.userEmail = userEmail;
            this.message = message;
        }

        public static AuthResult success(Long userId, String userEmail, String message) {
            return new AuthResult(true, userId, userEmail, message);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, null, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class UserRecord {
        private final long id;
        private final String email;
        private final String passwordHash;

        private UserRecord(long id, String email, String passwordHash) {
            this.id = id;
            this.email = email;
            this.passwordHash = passwordHash;
        }
    }

    private static final class PasswordResetCode {
        private final String codeHash;
        private final Instant expiresAt;

        private PasswordResetCode(String codeHash, Instant expiresAt) {
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return expiresAt == null || Instant.now().isAfter(expiresAt);
        }
    }
}

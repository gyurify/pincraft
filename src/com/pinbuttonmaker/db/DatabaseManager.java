package com.pinbuttonmaker.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_PATH = DATA_DIRECTORY.resolve("pincraft.db").toAbsolutePath().normalize();

    private final String jdbcUrl;
    private boolean available;
    private String statusMessage;

    public DatabaseManager() {
        this.jdbcUrl = "jdbc:sqlite:" + DATABASE_PATH;
        this.available = false;
        this.statusMessage = "Database is not initialized.";

        initialize();
    }

    private void initialize() {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Class.forName(SQLITE_DRIVER);

            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "email TEXT NOT NULL UNIQUE, "
                        + "password_hash TEXT NOT NULL, "
                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")"
                );
            }

            available = true;
            statusMessage = "SQLite database ready at " + DATABASE_PATH;
        } catch (ClassNotFoundException exception) {
            available = false;
            statusMessage = "SQLite JDBC driver not found. Add sqlite-jdbc.jar to the project classpath.";
        } catch (Exception exception) {
            available = false;
            statusMessage = "Database initialization failed: " + exception.getMessage();
        }
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public boolean isAvailable() {
        return available;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}

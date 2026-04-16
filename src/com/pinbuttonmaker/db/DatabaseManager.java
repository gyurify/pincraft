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

        //prepare the sqlite file and tables when the app starts.
        initialize();
    }

    private void initialize() {
        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            Class.forName(SQLITE_DRIVER);

            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                //store registered users here.
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "email TEXT NOT NULL UNIQUE, "
                        + "password_hash TEXT NOT NULL, "
                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")"
                );

                //store one-time reset codes for forgot password.
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS password_reset_codes ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "user_id INTEGER NOT NULL, "
                        + "code_hash TEXT NOT NULL, "
                        + "expires_at TEXT NOT NULL, "
                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                        + ")"
                );

                //store one row per saved project.
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS projects ("
                        + "id TEXT PRIMARY KEY, "
                        + "user_id INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "button_diameter_mm REAL NOT NULL, "
                        + "background_argb INTEGER NOT NULL, "
                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                        + ")"
                );

                //store every layer that belongs to a saved project.
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS project_layers ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "project_id TEXT NOT NULL, "
                        + "layer_order INTEGER NOT NULL, "
                        + "layer_name TEXT NOT NULL, "
                        + "layer_kind TEXT NOT NULL, "
                        + "visible INTEGER NOT NULL, "
                        + "text_content TEXT, "
                        + "text_color_argb INTEGER, "
                        + "font_family TEXT, "
                        + "font_size INTEGER NOT NULL, "
                        + "text_offset_x INTEGER NOT NULL, "
                        + "text_offset_y INTEGER NOT NULL, "
                        + "size_percent INTEGER NOT NULL, "
                        + "rotation_degrees INTEGER NOT NULL, "
                        + "stretch_percent INTEGER NOT NULL, "
                        + "transparency_percent INTEGER NOT NULL, "
                        + "bend_percent INTEGER NOT NULL, "
                        + "photo_image BLOB, "
                        + "photo_offset_x INTEGER NOT NULL, "
                        + "photo_offset_y INTEGER NOT NULL, "
                        + "photo_scale_percent INTEGER NOT NULL, "
                        + "crop_applied INTEGER NOT NULL, "
                        + "crop_x INTEGER NOT NULL, "
                        + "crop_y INTEGER NOT NULL, "
                        + "crop_width INTEGER NOT NULL, "
                        + "crop_height INTEGER NOT NULL, "
                        + "FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE"
                        + ")"
                );

                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_password_reset_codes_user ON password_reset_codes(user_id)");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_projects_user_updated ON projects(user_id, updated_at)");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_project_layers_project_order ON project_layers(project_id, layer_order)");
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
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            //enforce foreign key rules in sqlite for every connection.
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
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

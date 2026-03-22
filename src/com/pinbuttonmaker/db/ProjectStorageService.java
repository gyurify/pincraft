package com.pinbuttonmaker.db;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.pinbuttonmaker.data.LayerData;
import com.pinbuttonmaker.data.ProjectData;

public class ProjectStorageService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final DatabaseManager databaseManager;

    public ProjectStorageService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public StorageResult<List<ProjectData>> loadProjectsForUser(Long userId) {
        if (!databaseManager.isAvailable()) {
            return StorageResult.failure(databaseManager.getStatusMessage());
        }
        if (userId == null) {
            return StorageResult.success(new ArrayList<>(), "No signed-in account.");
        }

        String query = "SELECT id, name, button_diameter_mm, background_argb "
            + "FROM projects WHERE user_id = ? ORDER BY updated_at DESC, name COLLATE NOCASE ASC";

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);

            List<ProjectData> projects = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(readProject(connection, resultSet));
                }
            }

            return StorageResult.success(projects, "Projects loaded.");
        } catch (SQLException | IOException exception) {
            return StorageResult.failure("Unable to load saved projects: " + exception.getMessage());
        }
    }

    public StorageResult<ProjectData> loadProjectForUser(Long userId, String projectId) {
        if (!databaseManager.isAvailable()) {
            return StorageResult.failure(databaseManager.getStatusMessage());
        }
        if (userId == null) {
            return StorageResult.failure("Sign in to open saved projects.");
        }
        if (projectId == null || projectId.trim().isEmpty()) {
            return StorageResult.failure("Select a saved project to load.");
        }

        String query = "SELECT id, name, button_diameter_mm, background_argb "
            + "FROM projects WHERE user_id = ? AND id = ? LIMIT 1";

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setString(2, projectId.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return StorageResult.failure("That saved project was not found for this account.");
                }

                ProjectData project = readProject(connection, resultSet);
                return StorageResult.success(project, "Project loaded.");
            }
        } catch (SQLException | IOException exception) {
            return StorageResult.failure("Unable to load the selected project: " + exception.getMessage());
        }
    }

    public StorageResult<Void> saveProject(Long userId, ProjectData project) {
        if (!databaseManager.isAvailable()) {
            return StorageResult.failure(databaseManager.getStatusMessage());
        }
        if (userId == null) {
            return StorageResult.failure("Sign in to save designs to your account.");
        }
        if (project == null) {
            return StorageResult.failure("There is no project to save.");
        }

        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);

            try {
                Long existingOwnerId = findProjectOwnerId(connection, project.getProjectId());
                if (existingOwnerId == null) {
                    insertProject(connection, userId, project);
                } else if (!existingOwnerId.equals(userId)) {
                    connection.rollback();
                    return StorageResult.failure("That project id belongs to a different account.");
                } else {
                    updateProject(connection, userId, project);
                }

                deleteProjectLayers(connection, project.getProjectId());
                insertProjectLayers(connection, project);

                connection.commit();
                return StorageResult.success(null, "Project saved to your account.");
            } catch (SQLException | IOException exception) {
                connection.rollback();
                return StorageResult.failure("Unable to save the project: " + exception.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            return StorageResult.failure("Unable to save the project: " + exception.getMessage());
        }
    }

    public StorageResult<Void> deleteProject(Long userId, String projectId) {
        if (!databaseManager.isAvailable()) {
            return StorageResult.failure(databaseManager.getStatusMessage());
        }
        if (userId == null) {
            return StorageResult.failure("Sign in to manage saved designs.");
        }
        if (projectId == null || projectId.trim().isEmpty()) {
            return StorageResult.failure("Select a saved project to remove.");
        }

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM projects WHERE user_id = ? AND id = ?"
             )) {
            statement.setLong(1, userId);
            statement.setString(2, projectId.trim());

            int deleted = statement.executeUpdate();
            if (deleted == 0) {
                return StorageResult.failure("That saved project was not found for this account.");
            }

            return StorageResult.success(null, "Project removed from your account.");
        } catch (SQLException exception) {
            return StorageResult.failure("Unable to remove the project: " + exception.getMessage());
        }
    }

    private ProjectData readProject(Connection connection, ResultSet resultSet) throws SQLException, IOException {
        String projectId = resultSet.getString("id");
        String projectName = resultSet.getString("name");

        ProjectData project = ProjectData.restore(projectId, projectName);
        project.clearLayers();
        project.setButtonDiameterMm(resultSet.getDouble("button_diameter_mm"));
        project.setButtonBackgroundColor(new Color(resultSet.getInt("background_argb"), true));

        for (LayerData layer : loadProjectLayers(connection, projectId)) {
            project.addLayer(layer);
        }

        return project;
    }

    private List<LayerData> loadProjectLayers(Connection connection, String projectId) throws SQLException, IOException {
        List<LayerData> layers = new ArrayList<>();
        String query = "SELECT layer_name, layer_kind, visible, text_content, text_color_argb, font_family, "
            + "font_size, text_offset_x, text_offset_y, size_percent, rotation_degrees, stretch_percent, "
            + "transparency_percent, bend_percent, photo_image, photo_offset_x, photo_offset_y, "
            + "photo_scale_percent, crop_applied, crop_x, crop_y, crop_width, crop_height "
            + "FROM project_layers WHERE project_id = ? ORDER BY layer_order ASC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, projectId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    layers.add(readLayer(resultSet));
                }
            }
        }

        return layers;
    }

    private LayerData readLayer(ResultSet resultSet) throws SQLException, IOException {
        LayerData.LayerKind layerKind = parseLayerKind(resultSet.getString("layer_kind"));
        LayerData layer = new LayerData(resultSet.getString("layer_name"), layerKind);

        layer.setVisible(resultSet.getInt("visible") == 1);
        layer.setTextContent(resultSet.getString("text_content"));

        int textColorArgb = resultSet.getInt("text_color_argb");
        if (resultSet.wasNull()) {
            layer.setColor(null);
        } else {
            layer.setColor(new Color(textColorArgb, true));
        }

        layer.setFontFamily(resultSet.getString("font_family"));
        layer.setFontSize(resultSet.getInt("font_size"));
        layer.setTextOffsetX(resultSet.getInt("text_offset_x"));
        layer.setTextOffsetY(resultSet.getInt("text_offset_y"));
        layer.setSizePercent(resultSet.getInt("size_percent"));
        layer.setRotationDegrees(resultSet.getInt("rotation_degrees"));
        layer.setStretchPercent(resultSet.getInt("stretch_percent"));
        layer.setTransparencyPercent(resultSet.getInt("transparency_percent"));
        layer.setBendPercent(resultSet.getInt("bend_percent"));
        layer.setPhotoImage(readImage(resultSet.getBytes("photo_image")));
        layer.setPhotoOffsetX(resultSet.getInt("photo_offset_x"));
        layer.setPhotoOffsetY(resultSet.getInt("photo_offset_y"));
        layer.setPhotoScalePercent(resultSet.getInt("photo_scale_percent"));

        boolean cropApplied = resultSet.getInt("crop_applied") == 1;
        if (cropApplied) {
            layer.setCropData(new Rectangle(
                resultSet.getInt("crop_x"),
                resultSet.getInt("crop_y"),
                resultSet.getInt("crop_width"),
                resultSet.getInt("crop_height")
            ));
        } else {
            layer.clearCropData();
        }

        return layer;
    }

    private void insertProject(Connection connection, Long userId, ProjectData project) throws SQLException {
        String timestamp = currentTimestamp();
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO projects (id, user_id, name, button_diameter_mm, background_argb, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
            statement.setString(1, project.getProjectId());
            statement.setLong(2, userId);
            statement.setString(3, resolveProjectName(project));
            statement.setDouble(4, project.getButtonDiameterMm());
            statement.setInt(5, project.getButtonBackgroundColor().getRGB());
            statement.setString(6, timestamp);
            statement.setString(7, timestamp);
            statement.executeUpdate();
        }
    }

    private void updateProject(Connection connection, Long userId, ProjectData project) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE projects SET name = ?, button_diameter_mm = ?, background_argb = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?"
        )) {
            statement.setString(1, resolveProjectName(project));
            statement.setDouble(2, project.getButtonDiameterMm());
            statement.setInt(3, project.getButtonBackgroundColor().getRGB());
            statement.setString(4, currentTimestamp());
            statement.setString(5, project.getProjectId());
            statement.setLong(6, userId);
            statement.executeUpdate();
        }
    }

    private void deleteProjectLayers(Connection connection, String projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM project_layers WHERE project_id = ?"
        )) {
            statement.setString(1, projectId);
            statement.executeUpdate();
        }
    }

    private void insertProjectLayers(Connection connection, ProjectData project) throws SQLException, IOException {
        String insertSql = "INSERT INTO project_layers (project_id, layer_order, layer_name, layer_kind, visible, "
            + "text_content, text_color_argb, font_family, font_size, text_offset_x, text_offset_y, size_percent, "
            + "rotation_degrees, stretch_percent, transparency_percent, bend_percent, photo_image, photo_offset_x, "
            + "photo_offset_y, photo_scale_percent, crop_applied, crop_x, crop_y, crop_width, crop_height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            int order = 0;
            for (LayerData layer : project.getLayers()) {
                statement.setString(1, project.getProjectId());
                statement.setInt(2, order++);
                statement.setString(3, layer.getLayerName());
                statement.setString(4, layer.getLayerKind().name());
                statement.setInt(5, layer.isVisible() ? 1 : 0);
                statement.setString(6, layer.getTextContent());

                if (layer.getColor() == null) {
                    statement.setNull(7, Types.INTEGER);
                } else {
                    statement.setInt(7, layer.getColor().getRGB());
                }

                statement.setString(8, layer.getFontFamily());
                statement.setInt(9, layer.getFontSize());
                statement.setInt(10, layer.getTextOffsetX());
                statement.setInt(11, layer.getTextOffsetY());
                statement.setInt(12, layer.getSizePercent());
                statement.setInt(13, layer.getRotationDegrees());
                statement.setInt(14, layer.getStretchPercent());
                statement.setInt(15, layer.getTransparencyPercent());
                statement.setInt(16, layer.getBendPercent());

                byte[] imageBytes = writeImage(layer.getPhotoImage());
                if (imageBytes == null) {
                    statement.setNull(17, Types.BLOB);
                } else {
                    statement.setBytes(17, imageBytes);
                }

                statement.setInt(18, layer.getPhotoOffsetX());
                statement.setInt(19, layer.getPhotoOffsetY());
                statement.setInt(20, layer.getPhotoScalePercent());

                Rectangle cropBounds = layer.getCropBounds();
                statement.setInt(21, cropBounds == null ? 0 : 1);
                statement.setInt(22, cropBounds == null ? 0 : cropBounds.x);
                statement.setInt(23, cropBounds == null ? 0 : cropBounds.y);
                statement.setInt(24, cropBounds == null ? 0 : cropBounds.width);
                statement.setInt(25, cropBounds == null ? 0 : cropBounds.height);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private Long findProjectOwnerId(Connection connection, String projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT user_id FROM projects WHERE id = ? LIMIT 1"
        )) {
            statement.setString(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getLong("user_id");
            }
        }
    }

    private LayerData.LayerKind parseLayerKind(String value) {
        if (value == null) {
            return LayerData.LayerKind.TEXT;
        }

        try {
            return LayerData.LayerKind.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return LayerData.LayerKind.TEXT;
        }
    }

    private byte[] writeImage(BufferedImage image) throws IOException {
        if (image == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private BufferedImage readImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    private String resolveProjectName(ProjectData project) {
        String projectName = project.getProjectName();
        if (projectName == null || projectName.trim().isEmpty()) {
            return "Untitled Project";
        }
        return projectName.trim();
    }

    private String currentTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }

    public static final class StorageResult<T> {
        private final boolean success;
        private final T data;
        private final String message;

        private StorageResult(boolean success, T data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }

        public static <T> StorageResult<T> success(T data, String message) {
            return new StorageResult<>(true, data, message);
        }

        public static <T> StorageResult<T> failure(String message) {
            return new StorageResult<>(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }
    }
}

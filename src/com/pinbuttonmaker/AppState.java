package com.pinbuttonmaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.db.DatabaseManager;
import com.pinbuttonmaker.db.ProjectStorageService;
import com.pinbuttonmaker.db.UserAuthService;
import com.pinbuttonmaker.mail.SmtpMailSender;
import com.pinbuttonmaker.mail.SmtpSettings;
import com.pinbuttonmaker.util.Utils;

public class AppState {
    public enum ThemeMode {
        DARK,
        LIGHT
    }

    private Long currentUserId;
    private String currentUser;
    private ProjectData currentProject;
    private ThemeMode themeMode;
    private final List<ProjectData> savedProjects;
    private final DatabaseManager databaseManager;
    private final UserAuthService userAuthService;
    private final ProjectStorageService projectStorageService;

    public AppState() {
        this.currentUserId = null;
        this.currentUser = "Guest";
        this.currentProject = new ProjectData("Untitled Project");
        this.themeMode = ThemeMode.LIGHT;
        this.savedProjects = new ArrayList<>();
        this.databaseManager = new DatabaseManager();
        this.userAuthService = new UserAuthService(
            databaseManager,
            new SmtpMailSender(SmtpSettings.fromEnvironment())
        );
        this.projectStorageService = new ProjectStorageService(databaseManager);
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUserId != null;
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public boolean isDarkMode() {
        return themeMode == ThemeMode.DARK;
    }

    public void setThemeMode(ThemeMode themeMode) {
        this.themeMode = themeMode == null ? ThemeMode.LIGHT : themeMode;
    }

    public void setAuthenticatedUser(Long userId, String userEmail) {
        boolean switchedAccounts = currentUserId != null && userId != null && !currentUserId.equals(userId);

        this.currentUserId = userId;
        this.currentUser = Utils.normalizeOrDefault(userEmail, "Guest");

        if (switchedAccounts) {
            this.currentProject = new ProjectData("Untitled Project");
        }

        refreshSavedProjects();
    }

    public void logout() {
        this.currentUserId = null;
        this.currentUser = "Guest";
        this.currentProject = new ProjectData("Untitled Project");
        this.savedProjects.clear();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserAuthService getUserAuthService() {
        return userAuthService;
    }

    public ProjectData getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(ProjectData currentProject) {
        this.currentProject = currentProject == null ? new ProjectData("Untitled Project") : currentProject;
    }

    public List<ProjectData> getSavedProjects() {
        return Collections.unmodifiableList(savedProjects);
    }

    public ProjectStorageService.StorageResult<List<ProjectData>> refreshSavedProjects() {
        if (!isAuthenticated()) {
            savedProjects.clear();
            return ProjectStorageService.StorageResult.success(new ArrayList<>(), "No signed-in account.");
        }

        ProjectStorageService.StorageResult<List<ProjectData>> result = projectStorageService.loadProjectsForUser(currentUserId);
        if (result.isSuccess()) {
            savedProjects.clear();
            savedProjects.addAll(result.getData());
        }
        return result;
    }

    public ProjectStorageService.StorageResult<Void> saveCurrentProject() {
        if (currentProject == null) {
            return ProjectStorageService.StorageResult.failure("There is no project to save.");
        }
        return saveProject(currentProject);
    }

    public ProjectStorageService.StorageResult<Void> saveProject(ProjectData project) {
        if (!isAuthenticated()) {
            return ProjectStorageService.StorageResult.failure("Sign in to save designs to your account.");
        }

        ProjectStorageService.StorageResult<Void> result = projectStorageService.saveProject(currentUserId, project.copy());
        if (result.isSuccess()) {
            refreshSavedProjects();
        }
        return result;
    }

    public ProjectStorageService.StorageResult<ProjectData> loadProjectAsCurrent(String projectId) {
        if (!isAuthenticated()) {
            return ProjectStorageService.StorageResult.failure("Sign in to open saved projects.");
        }

        ProjectStorageService.StorageResult<ProjectData> result = projectStorageService.loadProjectForUser(currentUserId, projectId);
        if (!result.isSuccess()) {
            return result;
        }

        currentProject = result.getData().copy();
        return ProjectStorageService.StorageResult.success(currentProject, result.getMessage());
    }

    public ProjectStorageService.StorageResult<Void> removeSavedProject(String projectId) {
        if (!isAuthenticated()) {
            return ProjectStorageService.StorageResult.failure("Sign in to manage saved designs.");
        }

        ProjectStorageService.StorageResult<Void> result = projectStorageService.deleteProject(currentUserId, projectId);
        if (result.isSuccess()) {
            refreshSavedProjects();
        }
        return result;
    }
}

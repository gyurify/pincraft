package com.pinbuttonmaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.db.DatabaseManager;
import com.pinbuttonmaker.db.UserAuthService;

public class AppState {
    private String currentUser;
    private ProjectData currentProject;
    private final List<ProjectData> savedProjects;
    private final DatabaseManager databaseManager;
    private final UserAuthService userAuthService;

    public AppState() {
        this.currentUser = "Guest";
        this.currentProject = new ProjectData("Untitled Project");
        this.savedProjects = new ArrayList<>();
        this.databaseManager = new DatabaseManager();
        this.userAuthService = new UserAuthService(databaseManager);
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
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
        this.currentProject = currentProject;
    }

    public List<ProjectData> getSavedProjects() {
        return Collections.unmodifiableList(savedProjects);
    }

    public void saveCurrentProject() {
        if (currentProject == null) {
            return;
        }
        saveProject(currentProject);
    }

    public void saveProject(ProjectData project) {
        if (project == null) {
            return;
        }

        ProjectData snapshot = project.copy();
        int existingIndex = findSavedProjectIndex(snapshot.getProjectId());
        if (existingIndex >= 0) {
            savedProjects.remove(existingIndex);
        }
        savedProjects.add(0, snapshot);
    }

    public ProjectData loadProjectAsCurrent(String projectId) {
        ProjectData saved = getSavedProjectById(projectId);
        if (saved == null) {
            return null;
        }

        currentProject = saved.copy();
        return currentProject;
    }

    public ProjectData getSavedProjectById(String projectId) {
        if (projectId == null) {
            return null;
        }

        for (ProjectData project : savedProjects) {
            if (projectId.equals(project.getProjectId())) {
                return project;
            }
        }
        return null;
    }

    public boolean removeSavedProject(String projectId) {
        int index = findSavedProjectIndex(projectId);
        if (index < 0) {
            return false;
        }

        savedProjects.remove(index);
        return true;
    }

    private int findSavedProjectIndex(String projectId) {
        if (projectId == null) {
            return -1;
        }

        for (int i = 0; i < savedProjects.size(); i++) {
            if (projectId.equals(savedProjects.get(i).getProjectId())) {
                return i;
            }
        }
        return -1;
    }
}

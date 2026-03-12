package com.pinbuttonmaker.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProjectData {
    private static final Color DEFAULT_BUTTON_BG = new Color(247, 249, 253);

    private final String projectId;
    private String projectName;
    private final List<LayerData> layers;
    private double buttonDiameterMm;
    private Color buttonBackgroundColor;

    public ProjectData(String projectName) {
        this(UUID.randomUUID().toString(), projectName, true);
    }

    public ProjectData(ProjectData source) {
        this(source.projectId, source.projectName, false);
        this.buttonDiameterMm = source.buttonDiameterMm;
        this.buttonBackgroundColor = source.buttonBackgroundColor == null ? null : new Color(source.buttonBackgroundColor.getRGB(), true);

        for (LayerData layer : source.layers) {
            this.layers.add(layer.copy());
        }
    }

    private ProjectData(String projectId, String projectName, boolean initializeDefaults) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.layers = new ArrayList<>();
        this.buttonDiameterMm = 57.15;
        this.buttonBackgroundColor = new Color(DEFAULT_BUTTON_BG.getRGB(), true);

        if (initializeDefaults) {
            initializeDefaultLayers();
        }
    }

    private void initializeDefaultLayers() {
        LayerData text1 = new LayerData("Text 1", LayerData.LayerKind.TEXT);
        text1.setTextContent("Text 1");
        text1.setFontSize(30);

        LayerData text2 = new LayerData("Text 2", LayerData.LayerKind.TEXT);
        text2.setTextContent("Text 2");
        text2.setFontSize(26);

        LayerData photo = new LayerData("Photo", LayerData.LayerKind.PHOTO);

        layers.add(photo);
        layers.add(text1);
        layers.add(text2);
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<LayerData> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public void addLayer(LayerData layer) {
        layers.add(layer);
    }

    public void insertLayer(int index, LayerData layer) {
        int target = Math.max(0, Math.min(index, layers.size()));
        layers.add(target, layer);
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            layers.remove(index);
        }
    }

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size() || toIndex < 0 || toIndex >= layers.size() || fromIndex == toIndex) {
            return;
        }

        LayerData layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);
    }

    public Color getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    public void setButtonBackgroundColor(Color buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor == null ? new Color(DEFAULT_BUTTON_BG.getRGB(), true) : new Color(buttonBackgroundColor.getRGB(), true);
    }

    public double getButtonDiameterMm() {
        return buttonDiameterMm;
    }

    public void setButtonDiameterMm(double buttonDiameterMm) {
        this.buttonDiameterMm = buttonDiameterMm;
    }

    public ProjectData copy() {
        return new ProjectData(this);
    }

    @Override
    public String toString() {
        String name = projectName == null || projectName.trim().isEmpty() ? "Untitled Project" : projectName.trim();
        String shortId = projectId.length() > 8 ? projectId.substring(0, 8) : projectId;
        return name + " (" + shortId + ")";
    }
}

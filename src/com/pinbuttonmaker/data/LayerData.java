package com.pinbuttonmaker.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class LayerData {
    public enum LayerKind {
        TEXT,
        PHOTO
    }

    private String layerName;
    private LayerKind layerKind;
    private boolean visible;
    private String textContent;
    private Color color;
    private String fontFamily;
    private int fontSize;
    private int textOffsetX;
    private int textOffsetY;

    private int sizePercent;
    private int rotationDegrees;
    private int stretchPercent;
    private int transparencyPercent;
    private int bendPercent;

    private BufferedImage photoImage;
    private int photoOffsetX;
    private int photoOffsetY;
    private int photoScalePercent;

    private boolean cropApplied;
    private int cropX;
    private int cropY;
    private int cropWidth;
    private int cropHeight;

    public LayerData(String layerName) {
        this(layerName, LayerKind.TEXT);
    }

    public LayerData(String layerName, LayerKind layerKind) {
        this.layerName = layerName;
        this.layerKind = layerKind;
        this.visible = true;
        this.textContent = "";
        this.color = new Color(40, 46, 58);
        this.fontFamily = "SansSerif";
        this.fontSize = 28;
        this.textOffsetX = 0;
        this.textOffsetY = 0;

        this.sizePercent = 100;
        this.rotationDegrees = 0;
        this.stretchPercent = 100;
        this.transparencyPercent = 100;
        this.bendPercent = 0;

        this.photoImage = null;
        this.photoOffsetX = 0;
        this.photoOffsetY = 0;
        this.photoScalePercent = 100;

        clearCropData();
    }

    public LayerData(LayerData source) {
        this.layerName = source.layerName;
        this.layerKind = source.layerKind;
        this.visible = source.visible;
        this.textContent = source.textContent;
        this.color = source.color == null ? null : new Color(source.color.getRGB(), true);
        this.fontFamily = source.fontFamily;
        this.fontSize = source.fontSize;
        this.textOffsetX = source.textOffsetX;
        this.textOffsetY = source.textOffsetY;

        this.sizePercent = source.sizePercent;
        this.rotationDegrees = source.rotationDegrees;
        this.stretchPercent = source.stretchPercent;
        this.transparencyPercent = source.transparencyPercent;
        this.bendPercent = source.bendPercent;

        this.photoImage = copyImage(source.photoImage);
        this.photoOffsetX = source.photoOffsetX;
        this.photoOffsetY = source.photoOffsetY;
        this.photoScalePercent = source.photoScalePercent;

        this.cropApplied = source.cropApplied;
        this.cropX = source.cropX;
        this.cropY = source.cropY;
        this.cropWidth = source.cropWidth;
        this.cropHeight = source.cropHeight;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public LayerKind getLayerKind() {
        return layerKind;
    }

    public void setLayerKind(LayerKind layerKind) {
        this.layerKind = layerKind;
    }

    public boolean isTextLayer() {
        return layerKind == LayerKind.TEXT;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, fontSize);
    }

    public int getTextOffsetX() {
        return textOffsetX;
    }

    public void setTextOffsetX(int textOffsetX) {
        this.textOffsetX = textOffsetX;
    }

    public int getTextOffsetY() {
        return textOffsetY;
    }

    public void setTextOffsetY(int textOffsetY) {
        this.textOffsetY = textOffsetY;
    }

    public int getSizePercent() {
        return sizePercent;
    }

    public void setSizePercent(int sizePercent) {
        this.sizePercent = clamp(sizePercent, 50, 200);
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(int rotationDegrees) {
        this.rotationDegrees = clamp(rotationDegrees, -180, 180);
    }

    public int getStretchPercent() {
        return stretchPercent;
    }

    public void setStretchPercent(int stretchPercent) {
        this.stretchPercent = clamp(stretchPercent, 50, 200);
    }

    public int getTransparencyPercent() {
        return transparencyPercent;
    }

    public void setTransparencyPercent(int transparencyPercent) {
        this.transparencyPercent = clamp(transparencyPercent, 0, 100);
    }

    public int getBendPercent() {
        return bendPercent;
    }

    public void setBendPercent(int bendPercent) {
        this.bendPercent = clamp(bendPercent, -180, 180);
    }

    public BufferedImage getPhotoImage() {
        return photoImage;
    }

    public void setPhotoImage(BufferedImage photoImage) {
        this.photoImage = photoImage;
    }

    public boolean hasPhotoImage() {
        return photoImage != null;
    }

    public int getPhotoOffsetX() {
        return photoOffsetX;
    }

    public void setPhotoOffsetX(int photoOffsetX) {
        this.photoOffsetX = photoOffsetX;
    }

    public int getPhotoOffsetY() {
        return photoOffsetY;
    }

    public void setPhotoOffsetY(int photoOffsetY) {
        this.photoOffsetY = photoOffsetY;
    }

    public int getPhotoScalePercent() {
        return photoScalePercent;
    }

    public void setPhotoScalePercent(int photoScalePercent) {
        this.photoScalePercent = clamp(photoScalePercent, 50, 200);
    }

    public boolean isCropApplied() {
        return cropApplied;
    }

    public void clearCropData() {
        this.cropApplied = false;
        this.cropX = 0;
        this.cropY = 0;
        this.cropWidth = 0;
        this.cropHeight = 0;
    }

    public void setCropData(Rectangle cropBounds) {
        if (cropBounds == null || cropBounds.width <= 0 || cropBounds.height <= 0) {
            clearCropData();
            return;
        }

        this.cropApplied = true;
        this.cropX = Math.max(0, cropBounds.x);
        this.cropY = Math.max(0, cropBounds.y);
        this.cropWidth = Math.max(1, cropBounds.width);
        this.cropHeight = Math.max(1, cropBounds.height);
    }

    public Rectangle getCropBounds() {
        if (!cropApplied) {
            return null;
        }
        return new Rectangle(cropX, cropY, cropWidth, cropHeight);
    }

    public void centerPhoto() {
        this.photoOffsetX = 0;
        this.photoOffsetY = 0;
    }

    public float getOpacity() {
        return transparencyPercent / 100f;
    }

    public boolean isPrintable() {
        return visible && transparencyPercent > 0;
    }

    public LayerData copy() {
        return new LayerData(this);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private BufferedImage copyImage(BufferedImage source) {
        if (source == null) {
            return null;
        }

        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }
}

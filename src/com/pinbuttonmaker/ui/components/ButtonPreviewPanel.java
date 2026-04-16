package com.pinbuttonmaker.ui.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import com.pinbuttonmaker.data.LayerData;
import com.pinbuttonmaker.data.ProjectData;

public class ButtonPreviewPanel extends JPanel {
    private static final double DEFAULT_BUTTON_TO_OUTER_RATIO = 0.92;
    private static final double DEFAULT_SAFE_TO_OUTER_RATIO = 0.76;
    private static final double SAFE_TO_BUTTON_RATIO = DEFAULT_SAFE_TO_OUTER_RATIO / DEFAULT_BUTTON_TO_OUTER_RATIO;

    public interface LayerSelectionListener {
        void onLayerSelected(int layerIndex);
    }

    private static final int BLEED_MARGIN = 36;

    private static final Color BLEED_LINE_COLOR = new Color(95, 125, 175);
    private static final Color BUTTON_OUTLINE = new Color(165, 176, 194);
    private static final Color SAFE_ZONE_COLOR = new Color(58, 74, 100);

    private static final Color DEFAULT_TEXT_COLOR = new Color(38, 46, 60);
    private static final Color PHOTO_FILL = new Color(223, 229, 238);
    private static final Color PHOTO_BORDER = new Color(134, 146, 166);

    private static final Color ACTIVE_GUIDE_BORDER = new Color(56, 122, 232, 210);
    private static final Color ACTIVE_GUIDE_FILL = new Color(56, 122, 232, 235);
    private static final Color DRAG_SNAP_GUIDE = new Color(52, 133, 255, 185);
    private static final int DRAG_SNAP_THRESHOLD = 6;
    private static final int RESIZE_HANDLE_SIZE = 10;
    private static final int RESIZE_HANDLE_HIT_PADDING = 6;

    private ProjectData projectData;
    private int activeLayerIndex = -1;

    private LayerSelectionListener layerSelectionListener;
    private boolean showGuides;
    private boolean showButtonOutline;
    private boolean showPhotoBorder;
    private int outerMargin;
    private double buttonToOuterRatio;

    private boolean draggingLayer;
    private boolean resizingPhoto;
    private int lastDragX;
    private int lastDragY;
    private boolean snapToCenterX;
    private boolean snapToCenterY;

    public ButtonPreviewPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(520, 520));
        setMinimumSize(new Dimension(260, 260));
        this.draggingLayer = false;
        this.resizingPhoto = false;
        this.snapToCenterX = false;
        this.snapToCenterY = false;
        this.showGuides = true;
        this.showButtonOutline = true;
        this.showPhotoBorder = true;
        this.outerMargin = BLEED_MARGIN;
        this.buttonToOuterRatio = DEFAULT_BUTTON_TO_OUTER_RATIO;

        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                PreviewGeometry geometry = getPreviewGeometry();
                if (!geometry.isValid()) {
                    draggingLayer = false;
                    resizingPhoto = false;
                    return;
                }

                snapToCenterX = false;
                snapToCenterY = false;

                int hitLayerIndex = findLayerAtPoint(event.getX(), event.getY(), geometry);
                if (hitLayerIndex < 0) {
                    LayerData active = getActiveLayer();
                    if (active != null && active.isPrintable()) {
                        hitLayerIndex = activeLayerIndex;
                    }
                }

                if (hitLayerIndex < 0) {
                    draggingLayer = false;
                    resizingPhoto = false;
                    return;
                }

                if (hitLayerIndex != activeLayerIndex) {
                    activeLayerIndex = hitLayerIndex;
                    if (layerSelectionListener != null) {
                        layerSelectionListener.onLayerSelected(hitLayerIndex);
                    }
                    repaint();
                }

                LayerData layer = getActiveLayer();
                if (layer == null || !layer.isPrintable()) {
                    draggingLayer = false;
                    resizingPhoto = false;
                    return;
                }

                if (!layer.isTextLayer() && isPointOnPhotoResizeBorder(layer, event.getX(), event.getY(), geometry)) {
                    draggingLayer = false;
                    resizingPhoto = true;
                    lastDragX = event.getX();
                    lastDragY = event.getY();
                    return;
                }

                draggingLayer = true;
                resizingPhoto = false;
                lastDragX = event.getX();
                lastDragY = event.getY();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (resizingPhoto) {
                    LayerData layer = getActiveLayer();
                    if (layer == null || layer.isTextLayer()) {
                        resizingPhoto = false;
                        return;
                    }

                    PreviewGeometry geometry = getPreviewGeometry();
                    resizePhotoLayerFromPoint(layer, event.getX(), event.getY(), geometry);
                    lastDragX = event.getX();
                    lastDragY = event.getY();
                    repaint();
                    return;
                }

                if (!draggingLayer) {
                    return;
                }

                LayerData layer = getActiveLayer();
                if (layer == null) {
                    draggingLayer = false;
                    resizingPhoto = false;
                    snapToCenterX = false;
                    snapToCenterY = false;
                    return;
                }

                PreviewGeometry geometry = getPreviewGeometry();
                int dx = event.getX() - lastDragX;
                int dy = event.getY() - lastDragY;

                if (layer.isTextLayer()) {
                    int nextX = layer.getTextOffsetX() + dx;
                    int nextY = layer.getTextOffsetY() + dy;
                    int centerSnapOffsetY = computeTextCenterSnapOffsetY(layer.getLayerName(), geometry.buttonDiameter);

                    snapToCenterX = Math.abs(nextX) <= DRAG_SNAP_THRESHOLD;
                    snapToCenterY = Math.abs(nextY - centerSnapOffsetY) <= DRAG_SNAP_THRESHOLD;

                    layer.setTextOffsetX(snapToCenterX ? 0 : nextX);
                    layer.setTextOffsetY(snapToCenterY ? centerSnapOffsetY : nextY);
                } else {
                    int nextX = layer.getPhotoOffsetX() + dx;
                    int nextY = layer.getPhotoOffsetY() + dy;

                    snapToCenterX = Math.abs(nextX) <= DRAG_SNAP_THRESHOLD;
                    snapToCenterY = Math.abs(nextY) <= DRAG_SNAP_THRESHOLD;

                    layer.setPhotoOffsetX(snapToCenterX ? 0 : nextX);
                    layer.setPhotoOffsetY(snapToCenterY ? 0 : nextY);
                }

                lastDragX = event.getX();
                lastDragY = event.getY();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                boolean wasSnapped = snapToCenterX || snapToCenterY;
                draggingLayer = false;
                resizingPhoto = false;
                snapToCenterX = false;
                snapToCenterY = false;
                if (wasSnapped) {
                    repaint();
                }
            }
        };

        addMouseListener(dragAdapter);
        addMouseMotionListener(dragAdapter);
    }

    public static BufferedImage createPreviewImage(ProjectData projectData, int size, boolean showGuides) {
        int dimension = Math.max(96, size);
        ButtonPreviewPanel panel = new ButtonPreviewPanel();
        panel.showGuides = showGuides;
        panel.setProjectData(projectData == null ? null : projectData.copy());
        panel.setActiveLayerIndex(-1);
        panel.setSize(dimension, dimension);
        panel.setPreferredSize(new Dimension(dimension, dimension));
        panel.doLayout();

        BufferedImage image = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        return image;
    }

    public static BufferedImage createPrintableArtworkImage(ProjectData projectData, int cutSize, double cutDiameterInches, double bleedExtraInches) {
        int targetCutSize = Math.max(96, cutSize);
        double safeCutDiameterInches = Math.max(0.1, cutDiameterInches);
        double safeBleedExtraInches = Math.max(0.0, bleedExtraInches);

        ButtonPreviewPanel referencePanel = new ButtonPreviewPanel();
        int referencePanelSize = referencePanel.getPreferredSize().width;
        referencePanel.setSize(referencePanelSize, referencePanelSize);
        referencePanel.setPreferredSize(new Dimension(referencePanelSize, referencePanelSize));
        referencePanel.doLayout();
        PreviewGeometry referenceGeometry = referencePanel.getPreviewGeometry();

        double targetOuterToButtonRatio = safeCutDiameterInches / (safeCutDiameterInches + safeBleedExtraInches);
        double safeRatio = Math.max(0.1, Math.min(1.0, targetOuterToButtonRatio));
        int renderOuterSize = Math.max(1, (int) Math.ceil(referenceGeometry.buttonDiameter / safeRatio));

        ButtonPreviewPanel panel = new ButtonPreviewPanel();
        panel.showGuides = false;
        panel.showButtonOutline = false;
        panel.showPhotoBorder = false;
        panel.outerMargin = 0;
        panel.buttonToOuterRatio = safeRatio;
        panel.setProjectData(projectData == null ? null : projectData.copy());
        panel.setActiveLayerIndex(-1);
        panel.setSize(renderOuterSize, renderOuterSize);
        panel.setPreferredSize(new Dimension(renderOuterSize, renderOuterSize));
        panel.doLayout();

        PreviewGeometry geometry = panel.getPreviewGeometry();
        double scale = targetCutSize / (double) geometry.buttonDiameter;
        int scaledCanvasSize = Math.max(1, (int) Math.ceil(renderOuterSize * scale));

        BufferedImage scaledCanvas = new BufferedImage(scaledCanvasSize, scaledCanvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaledCanvas.createGraphics();
        graphics.scale(scale, scale);
        panel.paint(graphics);
        graphics.dispose();

        int cropSize = Math.max(1, (int) Math.round(geometry.bleedDiameter * scale));
        int cropX = Math.max(0, (int) Math.round((geometry.centerX - (geometry.bleedDiameter / 2.0)) * scale));
        int cropY = Math.max(0, (int) Math.round((geometry.centerY - (geometry.bleedDiameter / 2.0)) * scale));
        cropX = Math.min(cropX, Math.max(0, scaledCanvasSize - cropSize));
        cropY = Math.min(cropY, Math.max(0, scaledCanvasSize - cropSize));

        BufferedImage image = new BufferedImage(cropSize, cropSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D croppedGraphics = image.createGraphics();
        croppedGraphics.drawImage(
            scaledCanvas,
            0,
            0,
            cropSize,
            cropSize,
            cropX,
            cropY,
            cropX + cropSize,
            cropY + cropSize,
            null
        );
        croppedGraphics.dispose();
        return image;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
        repaint();
    }

    public void setActiveLayerIndex(int activeLayerIndex) {
        this.activeLayerIndex = activeLayerIndex;
        repaint();
    }

    public void setLayerSelectionListener(LayerSelectionListener layerSelectionListener) {
        this.layerSelectionListener = layerSelectionListener;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        PreviewGeometry geometry = getPreviewGeometry();
        if (!geometry.isValid()) {
            g2.dispose();
            return;
        }

        int bleedX = geometry.centerX - geometry.bleedDiameter / 2;
        int bleedY = geometry.centerY - geometry.bleedDiameter / 2;

        int buttonX = geometry.centerX - geometry.buttonDiameter / 2;
        int buttonY = geometry.centerY - geometry.buttonDiameter / 2;

        int safeX = geometry.centerX - geometry.safeDiameter / 2;
        int safeY = geometry.centerY - geometry.safeDiameter / 2;

        Color buttonFill = getButtonFillColor();
        if (buttonFill != null) {
            g2.setColor(buttonFill);
            g2.fillOval(buttonX, buttonY, geometry.buttonDiameter, geometry.buttonDiameter);
        }

        drawLayers(g2, geometry);

        if (showButtonOutline) {
            g2.setColor(BUTTON_OUTLINE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(buttonX, buttonY, geometry.buttonDiameter, geometry.buttonDiameter);
        }

        if (showGuides) {
            g2.setColor(BLEED_LINE_COLOR);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {8f, 7f}, 0f));
            g2.drawOval(bleedX, bleedY, geometry.bleedDiameter, geometry.bleedDiameter);

            g2.setColor(SAFE_ZONE_COLOR);
            g2.setStroke(new BasicStroke(2.25f));
            g2.drawOval(safeX, safeY, geometry.safeDiameter, geometry.safeDiameter);

            drawDragSnapGuides(g2, geometry, buttonX, buttonY);
        }

        g2.dispose();
    }
    private void drawDragSnapGuides(Graphics2D g2, PreviewGeometry geometry, int buttonX, int buttonY) {
        if (!draggingLayer || (!snapToCenterX && !snapToCenterY)) {
            return;
        }

        Stroke oldStroke = g2.getStroke();
        g2.setColor(DRAG_SNAP_GUIDE);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {6f, 4f}, 0f));

        if (snapToCenterX) {
            g2.drawLine(geometry.centerX, buttonY, geometry.centerX, buttonY + geometry.buttonDiameter);
        }

        if (snapToCenterY) {
            g2.drawLine(buttonX, geometry.centerY, buttonX + geometry.buttonDiameter, geometry.centerY);
        }

        g2.setStroke(oldStroke);
    }

    private void drawLayers(Graphics2D g2, PreviewGeometry geometry) {
        if (projectData == null) {
            return;
        }

        List<LayerData> layers = projectData.getLayers();
        int baseCurveRadius = (int) (geometry.safeDiameter * 0.52);

        for (int index = 0; index < layers.size(); index++) {
            LayerData layer = layers.get(index);

            if (!layer.isPrintable()) {
                continue;
            }

            Graphics2D layerGraphics = (Graphics2D) g2.create();
            layerGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity()));
            applyLayerTransform(layerGraphics, layer, geometry.centerX, geometry.centerY);

            if (layer.isTextLayer()) {
                drawTextLayer(layerGraphics, layer, geometry.centerX, geometry.centerY, geometry.buttonDiameter, baseCurveRadius);
            } else {
                drawPhotoLayer(layerGraphics, layer, geometry.centerX, geometry.centerY, geometry.buttonDiameter, geometry.safeDiameter);
            }

            layerGraphics.dispose();
        }

        LayerData activeLayer = getActiveLayer();
        if (showGuides && activeLayer != null) {
            Graphics2D overlayGraphics = (Graphics2D) g2.create();
            applyLayerTransform(overlayGraphics, activeLayer, geometry.centerX, geometry.centerY);
            drawActiveLayerGuide(overlayGraphics, activeLayer, geometry, baseCurveRadius);
            overlayGraphics.dispose();
        }
    }

    private int findLayerAtPoint(int x, int y, PreviewGeometry geometry) {
        if (projectData == null) {
            return -1;
        }

        List<LayerData> layers = projectData.getLayers();
        int baseCurveRadius = (int) (geometry.safeDiameter * 0.52);

        for (int i = layers.size() - 1; i >= 0; i--) {
            LayerData layer = layers.get(i);
            if (!layer.isPrintable()) {
                continue;
            }

            if (isPointInsideLayer(layer, x, y, geometry, baseCurveRadius)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isPointInsideLayer(LayerData layer, int x, int y, PreviewGeometry geometry, int baseCurveRadius) {
        Rectangle guideBounds = computeLayerGuideBounds(layer, geometry, baseCurveRadius);
        if (guideBounds == null) {
            return false;
        }

        Rectangle interactiveBounds = new Rectangle(guideBounds);
        interactiveBounds.grow(10, 10);

        AffineTransform layerTransform = createLayerTransform(layer, geometry.centerX, geometry.centerY);
        try {
            AffineTransform inverse = layerTransform.createInverse();
            Point2D localPoint = inverse.transform(new Point2D.Double(x, y), null);
            return interactiveBounds.contains(localPoint);
        } catch (NoninvertibleTransformException exception) {
            return false;
        }
    }

    private void drawTextLayer(Graphics2D g2, LayerData layer, int centerX, int centerY, int buttonDiameter, int baseCurveRadius) {
        String text = layer.getTextContent() == null ? "" : layer.getTextContent().trim();
        if (text.isEmpty()) {
            return;
        }

        Color color = layer.getColor() == null ? DEFAULT_TEXT_COLOR : layer.getColor();
        Font font = new Font(layer.getFontFamily(), Font.BOLD, layer.getFontSize());

        g2.setFont(font);
        g2.setColor(color);

        int textCenterX = centerX + layer.getTextOffsetX();
        int bendDegrees = layer.getBendPercent();
        String name = layer.getLayerName();
        int straightBaseline = getStraightBaseline(name, centerY, buttonDiameter) + layer.getTextOffsetY();
        boolean topArc = bendDegrees > 0;

        if (bendDegrees != 0) {
            int arcDegrees = Math.min(180, Math.abs(bendDegrees));
            drawCurvedText(g2, text, textCenterX, straightBaseline, baseCurveRadius, arcDegrees, topArc);
            return;
        }

        drawCenteredText(g2, text, textCenterX, straightBaseline);
    }

    private int getStraightBaseline(String layerName, int centerY, int buttonDiameter) {
        if ("Text 1".equals(layerName) || "Text 2".equals(layerName)) {
            return centerY;
        }
        return calculateTextBaseline(layerName, centerY);
    }

    private int computeTextCenterSnapOffsetY(String layerName, int buttonDiameter) {
        if ("Text 1".equals(layerName) || "Text 2".equals(layerName)) {
            return 0;
        }
        return 0;
    }

    private boolean isTopArcLayer(String layerName, int baselineY, int centerY) {
        if (layerName != null && layerName.startsWith("Text ")) {
            try {
                int number = Integer.parseInt(layerName.substring(5).trim());
                return (number % 2) != 0;
            } catch (NumberFormatException ignored) {
                // Fall back to baseline position.
            }
        }
        return baselineY <= centerY;
    }

    private int calculateTextBaseline(String layerName, int centerY) {
        if (layerName != null && layerName.startsWith("Text ")) {
            try {
                int number = Integer.parseInt(layerName.substring(5).trim());
                if (number >= 3) {
                    return centerY;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to center baseline.
            }
        }
        return centerY;
    }

    private void drawPhotoLayer(Graphics2D g2, LayerData layer, int centerX, int centerY, int buttonDiameter, int safeDiameter) {
        int buttonX = centerX - buttonDiameter / 2;
        int buttonY = centerY - buttonDiameter / 2;

        Shape oldClip = g2.getClip();
        g2.clip(new Ellipse2D.Double(buttonX, buttonY, buttonDiameter, buttonDiameter));

        if (layer.hasPhotoImage()) {
            BufferedImage image = layer.getPhotoImage();
            double coverScale = Math.max((double) buttonDiameter / image.getWidth(), (double) buttonDiameter / image.getHeight());
            double userScale = layer.getPhotoScalePercent() / 100.0;
            double scale = coverScale * userScale;

            int drawWidth = (int) Math.round(image.getWidth() * scale);
            int drawHeight = (int) Math.round(image.getHeight() * scale);

            int drawX = centerX - drawWidth / 2 + layer.getPhotoOffsetX();
            int drawY = centerY - drawHeight / 2 + layer.getPhotoOffsetY();

            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        }

        g2.setClip(oldClip);

        if (showPhotoBorder) {
            g2.setColor(PHOTO_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(buttonX, buttonY, buttonDiameter, buttonDiameter);
        }
    }

    private void applyLayerTransform(Graphics2D g2, LayerData layer, int centerX, int centerY) {
        g2.transform(createLayerTransform(layer, centerX, centerY));
    }

    private AffineTransform createLayerTransform(LayerData layer, int centerX, int centerY) {
        double sizeScale = layer.isTextLayer() ? layer.getSizePercent() / 100.0 : 1.0;
        double stretchScale = layer.getStretchPercent() / 100.0;
        double scaleX = sizeScale * stretchScale;
        double scaleY = sizeScale;

        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(Math.toRadians(layer.getRotationDegrees()));
        transform.scale(scaleX, scaleY);
        transform.translate(-centerX, -centerY);
        return transform;
    }

    private void drawCenteredText(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics metrics = g2.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        g2.drawString(text, centerX - textWidth / 2, baselineY);
    }
    private void drawCurvedText(Graphics2D g2, String text, int centerX, int baselineY, int baseRadius, int arcDegrees, boolean top) {
        if (text.isEmpty()) {
            return;
        }

        FontMetrics metrics = g2.getFontMetrics();
        double totalAdvance = 0;
        for (int i = 0; i < text.length(); i++) {
            totalAdvance += metrics.charWidth(text.charAt(i));
        }
        if (totalAdvance <= 0) {
            return;
        }

        double arcRadians = Math.toRadians(Math.max(1, arcDegrees));
        double computedRadius = totalAdvance / arcRadians;

        // Use direct geometric radius so bend remains smooth across the full 0..360 range.
        double minRadius = Math.max(4.0, metrics.getHeight() * 0.12);
        double maxRadius = Math.max(baseRadius * 35.0, minRadius + 1.0);
        double radius = Math.max(minRadius, Math.min(maxRadius, computedRadius));

        double circleCenterY = top ? baselineY + radius : baselineY - radius;

        double centerAngle = top ? -90.0 : 90.0;
        double direction = top ? 1.0 : -1.0;

        double runningAdvance = -totalAdvance / 2.0;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isWhitespace(character)) {
                runningAdvance += metrics.charWidth(character);
                continue;
            }

            int charWidth = metrics.charWidth(character);
            runningAdvance += charWidth / 2.0;

            double angleOffsetDegrees = Math.toDegrees(runningAdvance / radius);
            double angleDegrees = centerAngle + (direction * angleOffsetDegrees);
            double radians = Math.toRadians(angleDegrees);

            int x = centerX + (int) Math.round(radius * Math.cos(radians));
            int y = (int) Math.round(circleCenterY + (radius * Math.sin(radians)));

            AffineTransform oldTransform = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(top ? angleDegrees + 90 : angleDegrees - 90));

            String glyph = String.valueOf(character);
            int glyphWidth = metrics.stringWidth(glyph);
            int ascent = metrics.getAscent();
            g2.drawString(glyph, -glyphWidth / 2, ascent / 2);

            g2.setTransform(oldTransform);
            runningAdvance += charWidth / 2.0;
        }
    }

    private void drawActiveLayerGuide(Graphics2D g2, LayerData layer, PreviewGeometry geometry, int baseCurveRadius) {
        Rectangle guideBounds = computeLayerGuideBounds(layer, geometry, baseCurveRadius);
        if (guideBounds == null) {
            return;
        }

        int buttonX = geometry.centerX - geometry.buttonDiameter / 2;
        int buttonY = geometry.centerY - geometry.buttonDiameter / 2;

        Shape oldClip = g2.getClip();
        if (layer.isTextLayer()) {
            g2.clip(new Ellipse2D.Double(buttonX, buttonY, geometry.buttonDiameter, geometry.buttonDiameter));
        }

        g2.setColor(ACTIVE_GUIDE_BORDER);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {7f, 5f}, 0f));
        g2.drawRect(guideBounds.x, guideBounds.y, guideBounds.width, guideBounds.height);

        if (!layer.isTextLayer()) {
            drawPhotoResizeHandles(g2, guideBounds);
        }

        g2.setClip(oldClip);
    }

    private Rectangle computeLayerGuideBounds(LayerData layer, PreviewGeometry geometry, int baseCurveRadius) {
        if (layer == null) {
            return null;
        }

        if (layer.isTextLayer()) {
            String sample = layer.getTextContent() == null ? "" : layer.getTextContent().trim();
            if (sample.isEmpty()) {
                sample = layer.getLayerName() == null ? "Text" : layer.getLayerName();
            }

            Font textFont = new Font(layer.getFontFamily(), Font.BOLD, layer.getFontSize());
            FontMetrics metrics = getFontMetrics(textFont);

            int baseline = getStraightBaseline(layer.getLayerName(), geometry.centerY, geometry.buttonDiameter) + layer.getTextOffsetY();
            int textWidth = Math.max(1, metrics.stringWidth(sample));
            int rectWidth = Math.max(1, textWidth);
            int rectHeight = Math.max(1, metrics.getHeight());
            int rectCenterX = geometry.centerX + layer.getTextOffsetX();
            int rectCenterY = baseline - metrics.getAscent() + (metrics.getHeight() / 2);

            int bendDegrees = layer.getBendPercent();
            if (bendDegrees != 0) {
                int absBendDegrees = Math.abs(bendDegrees);
                int arcHeight = (int) Math.round((absBendDegrees / 180.0) * baseCurveRadius * 0.9);
                int arcWidth = (int) Math.round(arcHeight * 0.45);
                rectWidth += arcWidth;
                rectHeight += arcHeight;
                boolean topArc = bendDegrees > 0;
                int shift = (int) Math.round((absBendDegrees / 180.0) * rectHeight * 0.22);
                rectCenterY += topArc ? shift : -shift;
            }

            int x = rectCenterX - (rectWidth / 2);
            int y = rectCenterY - (rectHeight / 2);
            return new Rectangle(x, y, rectWidth, rectHeight);
        }

        int squareCenterX = geometry.centerX;
        int squareCenterY = geometry.centerY;
        int squareSize;

        Rectangle photoBounds = computePhotoGuideBounds(layer, geometry);
        if (photoBounds != null) {
            return photoBounds;
        }

        squareSize = (int) Math.round(geometry.safeDiameter * 0.55);
        squareCenterX = geometry.centerX + layer.getPhotoOffsetX();
        squareCenterY = geometry.centerY + layer.getPhotoOffsetY();

        int maxSquare = Math.max(60, (int) Math.round(geometry.buttonDiameter * 0.94));
        squareSize = clamp(squareSize, 56, maxSquare);

        int x = squareCenterX - squareSize / 2;
        int y = squareCenterY - squareSize / 2;
        return new Rectangle(x, y, squareSize, squareSize);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resizePhotoLayerFromPoint(LayerData layer, int mouseX, int mouseY, PreviewGeometry geometry) {
        Point2D localPoint = toLayerLocalPoint(layer, mouseX, mouseY, geometry);
        if (localPoint == null) {
            return;
        }

        PhotoDrawBounds baseBounds = computePhotoDrawBounds(layer, geometry, 100);
        if (baseBounds == null || baseBounds.width <= 0 || baseBounds.height <= 0) {
            return;
        }

        double halfWidth = Math.max(12.0, Math.abs(localPoint.getX() - geometry.centerX));
        double halfHeight = Math.max(12.0, Math.abs(localPoint.getY() - geometry.centerY));

        double widthRatio = (halfWidth * 2.0) / baseBounds.width;
        double heightRatio = (halfHeight * 2.0) / baseBounds.height;
        int nextScalePercent = (int) Math.round(Math.max(widthRatio, heightRatio) * 100.0);
        layer.setPhotoScalePercent(nextScalePercent);
    }

    private boolean isPointOnPhotoResizeBorder(LayerData layer, int x, int y, PreviewGeometry geometry) {
        Rectangle guideBounds = computePhotoGuideBounds(layer, geometry);
        if (guideBounds == null) {
            return false;
        }

        Point2D localPoint = toLayerLocalPoint(layer, x, y, geometry);
        if (localPoint == null) {
            return false;
        }

        if (isPointInsideResizeHandle(guideBounds, localPoint)) {
            return true;
        }

        Rectangle outerBounds = new Rectangle(guideBounds);
        outerBounds.grow(RESIZE_HANDLE_HIT_PADDING, RESIZE_HANDLE_HIT_PADDING);

        Rectangle innerBounds = new Rectangle(guideBounds);
        int shrinkBy = Math.max(6, RESIZE_HANDLE_SIZE);
        innerBounds.grow(-shrinkBy, -shrinkBy);

        return outerBounds.contains(localPoint) && !innerBounds.contains(localPoint);
    }

    private boolean isPointInsideResizeHandle(Rectangle guideBounds, Point2D localPoint) {
        for (Rectangle handle : getResizeHandles(guideBounds)) {
            Rectangle hitBounds = new Rectangle(handle);
            hitBounds.grow(RESIZE_HANDLE_HIT_PADDING, RESIZE_HANDLE_HIT_PADDING);
            if (hitBounds.contains(localPoint)) {
                return true;
            }
        }
        return false;
    }

    private Point2D toLayerLocalPoint(LayerData layer, int x, int y, PreviewGeometry geometry) {
        AffineTransform layerTransform = createLayerTransform(layer, geometry.centerX, geometry.centerY);
        try {
            AffineTransform inverse = layerTransform.createInverse();
            return inverse.transform(new Point2D.Double(x, y), null);
        } catch (NoninvertibleTransformException exception) {
            return null;
        }
    }

    private Rectangle computePhotoGuideBounds(LayerData layer, PreviewGeometry geometry) {
        PhotoDrawBounds photoBounds = computePhotoDrawBounds(layer, geometry, layer.getPhotoScalePercent());
        if (photoBounds == null) {
            return null;
        }
        return new Rectangle(photoBounds.x, photoBounds.y, photoBounds.width, photoBounds.height);
    }

    private PhotoDrawBounds computePhotoDrawBounds(LayerData layer, PreviewGeometry geometry, int scalePercent) {
        if (layer == null) {
            return null;
        }

        if (!layer.hasPhotoImage()) {
            int fallbackSize = (int) Math.round(geometry.safeDiameter * 0.55);
            int fallbackX = geometry.centerX - (fallbackSize / 2) + layer.getPhotoOffsetX();
            int fallbackY = geometry.centerY - (fallbackSize / 2) + layer.getPhotoOffsetY();
            return new PhotoDrawBounds(fallbackX, fallbackY, fallbackSize, fallbackSize);
        }

        BufferedImage image = layer.getPhotoImage();
        double coverScale = Math.max((double) geometry.buttonDiameter / image.getWidth(), (double) geometry.buttonDiameter / image.getHeight());
        double userScale = Math.max(0.1, scalePercent / 100.0);
        double scale = coverScale * userScale;

        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int drawX = geometry.centerX - (drawWidth / 2) + layer.getPhotoOffsetX();
        int drawY = geometry.centerY - (drawHeight / 2) + layer.getPhotoOffsetY();
        return new PhotoDrawBounds(drawX, drawY, drawWidth, drawHeight);
    }

    private void drawPhotoResizeHandles(Graphics2D g2, Rectangle guideBounds) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.2f));

        for (Rectangle handle : getResizeHandles(guideBounds)) {
            g2.setColor(ACTIVE_GUIDE_FILL);
            g2.fillRect(handle.x, handle.y, handle.width, handle.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(handle.x, handle.y, handle.width, handle.height);
        }

        g2.setStroke(oldStroke);
    }

    private Rectangle[] getResizeHandles(Rectangle guideBounds) {
        int halfHandle = RESIZE_HANDLE_SIZE / 2;
        return new Rectangle[] {
            new Rectangle(guideBounds.x - halfHandle, guideBounds.y - halfHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
            new Rectangle(guideBounds.x + guideBounds.width - halfHandle, guideBounds.y - halfHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
            new Rectangle(guideBounds.x - halfHandle, guideBounds.y + guideBounds.height - halfHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
            new Rectangle(guideBounds.x + guideBounds.width - halfHandle, guideBounds.y + guideBounds.height - halfHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)
        };
    }


    private Color getButtonFillColor() {
        if (projectData != null && projectData.getButtonBackgroundColor() != null) {
            return projectData.getButtonBackgroundColor();
        }
        return null;
    }
    private LayerData getActiveLayer() {
        if (projectData == null || activeLayerIndex < 0 || activeLayerIndex >= projectData.getLayers().size()) {
            return null;
        }
        return projectData.getLayers().get(activeLayerIndex);
    }

    private boolean isInsideButtonCircle(int x, int y, PreviewGeometry geometry) {
        int radius = geometry.buttonDiameter / 2;
        int dx = x - geometry.centerX;
        int dy = y - geometry.centerY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    public boolean isPointInsideButtonCircle(int x, int y) {
        PreviewGeometry geometry = getPreviewGeometry();
        return geometry.isValid() && isInsideButtonCircle(x, y, geometry);
    }

    private PreviewGeometry getPreviewGeometry() {
        int outerDiameter = Math.min(getWidth(), getHeight()) - outerMargin;
        int buttonDiameter = (int) Math.round(outerDiameter * buttonToOuterRatio);
        int safeDiameter = (int) Math.round(buttonDiameter * SAFE_TO_BUTTON_RATIO);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        return new PreviewGeometry(centerX, centerY, outerDiameter, buttonDiameter, safeDiameter);
    }

    private static final class PreviewGeometry {
        private final int centerX;
        private final int centerY;
        private final int bleedDiameter;
        private final int buttonDiameter;
        private final int safeDiameter;

        private PreviewGeometry(int centerX, int centerY, int bleedDiameter, int buttonDiameter, int safeDiameter) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.bleedDiameter = bleedDiameter;
            this.buttonDiameter = buttonDiameter;
            this.safeDiameter = safeDiameter;
        }

        private boolean isValid() {
            return bleedDiameter > 0 && buttonDiameter > 0 && safeDiameter > 0;
        }
    }

    private static final class PhotoDrawBounds {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private PhotoDrawBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}


















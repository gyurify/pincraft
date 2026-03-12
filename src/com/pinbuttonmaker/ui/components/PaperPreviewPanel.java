package com.pinbuttonmaker.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

public class PaperPreviewPanel extends JPanel {
    private static final Color WORKSPACE_BG = new Color(237, 242, 249);
    private static final Color PAGE_SHADOW = new Color(214, 221, 232, 90);
    private static final Color PAGE_FILL = Color.WHITE;
    private static final Color PAGE_BORDER = new Color(189, 201, 220);
    private static final Color SLOT_FILL = new Color(225, 234, 247, 120);
    private static final Color SLOT_SOFT_BORDER = new Color(206, 216, 232, 170);
    private static final Color CUT_LINE_COLOR = new Color(90, 128, 184, 155);
    private static final Color CAPTION_COLOR = new Color(100, 112, 132);

    private String paperLabel;
    private double paperWidthInches;
    private double paperHeightInches;

    private String buttonLabel;
    private double buttonDiameterInches;
    private double horizontalMarginInches;
    private double verticalMarginInches;
    private double horizontalGapInches;
    private double verticalGapInches;

    private boolean showCutLines;
    private LayoutInfo layoutInfo;

    public PaperPreviewPanel() {
        setOpaque(true);
        setBackground(WORKSPACE_BG);
        setPreferredSize(new Dimension(560, 640));
        setMinimumSize(new Dimension(260, 320));

        paperLabel = "A4";
        paperWidthInches = 8.27;
        paperHeightInches = 11.69;

        buttonLabel = "2.25\"";
        buttonDiameterInches = 2.25;
        horizontalMarginInches = 0.36;
        verticalMarginInches = 0.62;
        horizontalGapInches = 0.30;
        verticalGapInches = 0.80;

        showCutLines = true;
        layoutInfo = calculateLayout();
    }

    public void setPaperSize(String paperLabel, double paperWidthInches, double paperHeightInches) {
        this.paperLabel = paperLabel == null ? "Paper" : paperLabel;
        this.paperWidthInches = Math.max(1.0, paperWidthInches);
        this.paperHeightInches = Math.max(1.0, paperHeightInches);
        refreshLayout();
    }

    public void setButtonLayout(String buttonLabel, double buttonDiameterInches, double horizontalMarginInches, double verticalMarginInches, double horizontalGapInches, double verticalGapInches) {
        this.buttonLabel = buttonLabel == null ? "Button" : buttonLabel;
        this.buttonDiameterInches = Math.max(0.1, buttonDiameterInches);
        this.horizontalMarginInches = Math.max(0.0, horizontalMarginInches);
        this.verticalMarginInches = Math.max(0.0, verticalMarginInches);
        this.horizontalGapInches = Math.max(0.0, horizontalGapInches);
        this.verticalGapInches = Math.max(0.0, verticalGapInches);
        refreshLayout();
    }

    public void setShowCutLines(boolean showCutLines) {
        this.showCutLines = showCutLines;
        repaint();
    }

    public LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    private void refreshLayout() {
        layoutInfo = calculateLayout();
        repaint();
    }

    private LayoutInfo calculateLayout() {
        double usableWidth = Math.max(0.0, paperWidthInches - (horizontalMarginInches * 2.0));
        double usableHeight = Math.max(0.0, paperHeightInches - (verticalMarginInches * 2.0));

        int columns = (int) Math.floor((usableWidth + horizontalGapInches) / (buttonDiameterInches + horizontalGapInches));
        int rows = (int) Math.floor((usableHeight + verticalGapInches) / (buttonDiameterInches + verticalGapInches));

        columns = Math.max(0, columns);
        rows = Math.max(0, rows);

        double occupiedWidth = columns == 0 ? 0.0 : (columns * buttonDiameterInches) + ((columns - 1) * horizontalGapInches);
        double occupiedHeight = rows == 0 ? 0.0 : (rows * buttonDiameterInches) + ((rows - 1) * verticalGapInches);

        double startX = horizontalMarginInches + Math.max(0.0, (usableWidth - occupiedWidth) / 2.0);
        double startY = verticalMarginInches + Math.max(0.0, (usableHeight - occupiedHeight) / 2.0);

        return new LayoutInfo(columns, rows, columns * rows, startX, startY);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int workspaceMargin = 24;

        int availableWidth = Math.max(1, panelWidth - (workspaceMargin * 2));
        int availableHeight = Math.max(1, panelHeight - (workspaceMargin * 2));

        double paperRatio = paperWidthInches / paperHeightInches;
        int paperWidth = availableWidth;
        int paperHeight = (int) Math.round(paperWidth / paperRatio);
        if (paperHeight > availableHeight) {
            paperHeight = availableHeight;
            paperWidth = (int) Math.round(paperHeight * paperRatio);
        }

        int paperX = (panelWidth - paperWidth) / 2;
        int paperY = (panelHeight - paperHeight) / 2;

        drawPaper(g2, paperX, paperY, paperWidth, paperHeight);
        drawButtonSlots(g2, paperX, paperY, paperWidth, paperHeight);
        drawCaption(g2, paperX, paperY);

        g2.dispose();
    }

    private void drawPaper(Graphics2D g2, int paperX, int paperY, int paperWidth, int paperHeight) {
        RoundRectangle2D shadow = new RoundRectangle2D.Double(paperX + 6, paperY + 8, paperWidth, paperHeight, 14, 14);
        RoundRectangle2D page = new RoundRectangle2D.Double(paperX, paperY, paperWidth, paperHeight, 14, 14);

        g2.setColor(PAGE_SHADOW);
        g2.fill(shadow);

        g2.setColor(PAGE_FILL);
        g2.fill(page);

        g2.setColor(PAGE_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(page);
    }

    private void drawButtonSlots(Graphics2D g2, int paperX, int paperY, int paperWidth, int paperHeight) {
        if (layoutInfo.getTotalPins() == 0) {
            return;
        }

        double scale = Math.min(paperWidth / paperWidthInches, paperHeight / paperHeightInches);
        double diameter = buttonDiameterInches * scale;

        Stroke oldStroke = g2.getStroke();
        Stroke cutLineStroke = new BasicStroke(1.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {6f, 5f}, 0f);

        for (int row = 0; row < layoutInfo.getRows(); row++) {
            for (int col = 0; col < layoutInfo.getColumns(); col++) {
                double slotX = paperX + ((layoutInfo.getStartXInches() + (col * (buttonDiameterInches + horizontalGapInches))) * scale);
                double slotY = paperY + ((layoutInfo.getStartYInches() + (row * (buttonDiameterInches + verticalGapInches))) * scale);

                slotX = clamp(slotX, paperX + 1.0, (paperX + paperWidth) - diameter - 1.0);
                slotY = clamp(slotY, paperY + 1.0, (paperY + paperHeight) - diameter - 1.0);

                Ellipse2D slot = new Ellipse2D.Double(slotX, slotY, diameter, diameter);

                g2.setColor(SLOT_FILL);
                g2.fill(slot);

                g2.setStroke(oldStroke);
                g2.setColor(SLOT_SOFT_BORDER);
                g2.draw(slot);

                if (showCutLines) {
                    g2.setStroke(cutLineStroke);
                    g2.setColor(CUT_LINE_COLOR);
                    g2.draw(slot);
                }
            }
        }

        g2.setStroke(oldStroke);
    }

    private void drawCaption(Graphics2D g2, int paperX, int paperY) {
        g2.setColor(CAPTION_COLOR);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(paperLabel + " Preview | " + buttonLabel + " | " + layoutInfo.getTotalPins() + " pins", paperX + 12, paperY + 20);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class LayoutInfo {
        private final int columns;
        private final int rows;
        private final int totalPins;
        private final double startXInches;
        private final double startYInches;

        private LayoutInfo(int columns, int rows, int totalPins, double startXInches, double startYInches) {
            this.columns = columns;
            this.rows = rows;
            this.totalPins = totalPins;
            this.startXInches = startXInches;
            this.startYInches = startYInches;
        }

        public int getColumns() {
            return columns;
        }

        public int getRows() {
            return rows;
        }

        public int getTotalPins() {
            return totalPins;
        }

        public double getStartXInches() {
            return startXInches;
        }

        public double getStartYInches() {
            return startYInches;
        }
    }
}

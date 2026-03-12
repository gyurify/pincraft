package com.pinbuttonmaker.ui.dialogs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ImageCropDialog extends JDialog {
    private final CropCanvas cropCanvas;
    private BufferedImage croppedImage;
    private Rectangle cropBoundsInSource;

    public ImageCropDialog(Window owner, BufferedImage sourceImage) {
        super(owner, "Crop Photo", ModalityType.APPLICATION_MODAL);
        this.cropCanvas = new CropCanvas(sourceImage);
        this.croppedImage = null;
        this.cropBoundsInSource = null;

        buildUi();
    }

    public BufferedImage getCroppedImage() {
        return croppedImage;
    }

    public Rectangle getCropBoundsInSource() {
        if (cropBoundsInSource == null) {
            return null;
        }
        return new Rectangle(cropBoundsInSource);
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 10));
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Square crop ready. Drag inside to move, use corners to resize.", SwingConstants.LEFT);
        title.setBorder(new EmptyBorder(0, 2, 0, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));

        JButton resetButton = createFooterButton("Reset");
        resetButton.addActionListener(event -> cropCanvas.clearSelection());

        JButton cancelButton = createFooterButton("Cancel");
        cancelButton.addActionListener(event -> {
            croppedImage = null;
            cropBoundsInSource = null;
            dispose();
        });

        JButton applyButton = createFooterButton("Apply Crop");
        applyButton.addActionListener(event -> {
            cropBoundsInSource = cropCanvas.getSelectionInImageCoordinates();
            croppedImage = cropCanvas.createCroppedImage();
            dispose();
        });

        footer.add(resetButton);
        footer.add(Box.createHorizontalStrut(8));
        footer.add(Box.createHorizontalGlue());
        footer.add(cancelButton);
        footer.add(Box.createHorizontalStrut(8));
        footer.add(applyButton);

        add(header, BorderLayout.NORTH);
        add(cropCanvas, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(780, 620));
        pack();
        setResizable(true);
        setLocationRelativeTo(getOwner());
    }

    private JButton createFooterButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        return button;
    }

    private static final class CropCanvas extends JPanel {
        private static final int PADDING = 14;
        private static final int HANDLE_SIZE = 10;
        private static final int HANDLE_HIT_PADDING = 5;
        private static final int MIN_SELECTION_SIZE = 8;

        private static final Color CANVAS_BG = new Color(52, 57, 66);
        private static final Color IMAGE_BG = new Color(28, 32, 38);
        private static final Color SELECTION_FILL = new Color(64, 139, 255, 60);
        private static final Color SELECTION_BORDER = new Color(95, 169, 255);
        private static final Color GRID_LINE = new Color(255, 255, 255, 120);
        private static final Color HANDLE_FILL = new Color(247, 250, 255);

        private enum DragMode {
            NONE,
            MOVE,
            RESIZE_NW,
            RESIZE_NE,
            RESIZE_SW,
            RESIZE_SE
        }

        private final BufferedImage image;
        private Rectangle selection;
        private Rectangle selectionAtDragStart;
        private Point dragStart;
        private DragMode dragMode;

        private CropCanvas(BufferedImage image) {
            this.image = image;
            this.selection = null;
            this.selectionAtDragStart = null;
            this.dragStart = null;
            this.dragMode = DragMode.NONE;

            setBorder(BorderFactory.createLineBorder(new Color(120, 128, 141)));
            setBackground(CANVAS_BG);
            setPreferredSize(new Dimension(760, 520));

            MouseAdapter dragHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    Rectangle viewport = getImageViewport();
                    if (!viewport.contains(event.getPoint())) {
                        clearDragState();
                        return;
                    }

                    ensureDefaultSelection(viewport);

                    Point clamped = clampToViewport(event.getPoint(), viewport);
                    DragMode handleMode = getHandleAtPoint(clamped);
                    if (handleMode != DragMode.NONE) {
                        dragMode = handleMode;
                        dragStart = clamped;
                        selectionAtDragStart = new Rectangle(selection);
                        return;
                    }

                    if (selection != null && selection.contains(clamped)) {
                        dragMode = DragMode.MOVE;
                        dragStart = clamped;
                        selectionAtDragStart = new Rectangle(selection);
                        return;
                    }

                    if (selection != null) {
                        int side = selection.width;
                        int x = clamp(clamped.x - side / 2, viewport.x, viewport.x + viewport.width - side);
                        int y = clamp(clamped.y - side / 2, viewport.y, viewport.y + viewport.height - side);
                        selection = new Rectangle(x, y, side, side);
                        dragMode = DragMode.MOVE;
                        dragStart = clamped;
                        selectionAtDragStart = new Rectangle(selection);
                        repaint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (dragMode == DragMode.NONE || dragStart == null || selectionAtDragStart == null) {
                        return;
                    }

                    Rectangle viewport = getImageViewport();
                    Point current = clampToViewport(event.getPoint(), viewport);

                    if (dragMode == DragMode.MOVE) {
                        int dx = current.x - dragStart.x;
                        int dy = current.y - dragStart.y;
                        Rectangle moved = new Rectangle(selectionAtDragStart);
                        moved.translate(dx, dy);
                        selection = clampRectangleToViewport(moved, viewport);
                    } else {
                        selection = resizeSquareSelection(selectionAtDragStart, current, dragMode, viewport);
                    }

                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    clearDragState();
                    repaint();
                }
            };

            addMouseListener(dragHandler);
            addMouseMotionListener(dragHandler);
        }

        private void clearSelection() {
            selection = null;
            clearDragState();
            repaint();
        }

        private void clearDragState() {
            dragMode = DragMode.NONE;
            dragStart = null;
            selectionAtDragStart = null;
        }

        private BufferedImage createCroppedImage() {
            Rectangle sourceRect = getSelectionInImageCoordinates();
            if (sourceRect == null) {
                sourceRect = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            }

            BufferedImage result = new BufferedImage(sourceRect.width, sourceRect.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = result.createGraphics();
            g2.drawImage(
                image,
                0,
                0,
                sourceRect.width,
                sourceRect.height,
                sourceRect.x,
                sourceRect.y,
                sourceRect.x + sourceRect.width,
                sourceRect.y + sourceRect.height,
                null
            );
            g2.dispose();
            return result;
        }

        Rectangle getSelectionInImageCoordinates() {
            Rectangle viewport = getImageViewport();
            ensureDefaultSelection(viewport);

            if (selection == null || selection.width < 2 || selection.height < 2) {
                return null;
            }

            double scale = getScale(viewport);
            if (scale <= 0.0) {
                return null;
            }

            int x = (int) Math.floor((selection.x - viewport.x) / scale);
            int y = (int) Math.floor((selection.y - viewport.y) / scale);
            int w = (int) Math.ceil(selection.width / scale);
            int h = (int) Math.ceil(selection.height / scale);

            x = clamp(x, 0, image.getWidth() - 1);
            y = clamp(y, 0, image.getHeight() - 1);
            w = clamp(w, 1, image.getWidth() - x);
            h = clamp(h, 1, image.getHeight() - y);

            return new Rectangle(x, y, w, h);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.setColor(IMAGE_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Rectangle viewport = getImageViewport();
            ensureDefaultSelection(viewport);
            g2.drawImage(image, viewport.x, viewport.y, viewport.width, viewport.height, null);

            if (selection != null && selection.width > 0 && selection.height > 0) {
                g2.setColor(SELECTION_FILL);
                g2.fillRect(selection.x, selection.y, selection.width, selection.height);

                g2.setColor(SELECTION_BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(selection.x, selection.y, selection.width, selection.height);

                drawGrid(g2);
                drawCornerHandles(g2);
            }

            g2.dispose();
        }

        private void ensureDefaultSelection(Rectangle viewport) {
            if (selection == null || selection.width <= 0 || selection.height <= 0) {
                selection = createDefaultSquare(viewport);
                return;
            }

            int maxSide = Math.max(1, Math.min(viewport.width, viewport.height));
            int side = Math.max(1, Math.min(Math.min(selection.width, selection.height), maxSide));
            int x = clamp(selection.x, viewport.x, viewport.x + viewport.width - side);
            int y = clamp(selection.y, viewport.y, viewport.y + viewport.height - side);
            selection = new Rectangle(x, y, side, side);
        }

        private Rectangle createDefaultSquare(Rectangle viewport) {
            int maxSide = Math.max(1, Math.min(viewport.width, viewport.height));
            int side = Math.max(MIN_SELECTION_SIZE, (int) Math.round(maxSide * 0.62));
            side = Math.min(side, maxSide);
            int x = viewport.x + (viewport.width - side) / 2;
            int y = viewport.y + (viewport.height - side) / 2;
            return new Rectangle(x, y, side, side);
        }

        private void drawGrid(Graphics2D g2) {
            if (selection == null) {
                return;
            }

            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(GRID_LINE);

            for (int i = 1; i <= 2; i++) {
                int x = selection.x + (selection.width * i / 3);
                int y = selection.y + (selection.height * i / 3);
                g2.drawLine(x, selection.y, x, selection.y + selection.height);
                g2.drawLine(selection.x, y, selection.x + selection.width, y);
            }

            g2.setStroke(oldStroke);
        }

        private void drawCornerHandles(Graphics2D g2) {
            for (DragMode mode : new DragMode[] {DragMode.RESIZE_NW, DragMode.RESIZE_NE, DragMode.RESIZE_SW, DragMode.RESIZE_SE}) {
                Rectangle handle = getHandleBounds(mode);
                if (handle == null) {
                    continue;
                }

                g2.setColor(HANDLE_FILL);
                g2.fillRect(handle.x, handle.y, handle.width, handle.height);
                g2.setColor(SELECTION_BORDER);
                g2.drawRect(handle.x, handle.y, handle.width, handle.height);
            }
        }

        private DragMode getHandleAtPoint(Point point) {
            if (selection == null || selection.width <= 0 || selection.height <= 0) {
                return DragMode.NONE;
            }

            DragMode[] handles = {DragMode.RESIZE_NW, DragMode.RESIZE_NE, DragMode.RESIZE_SW, DragMode.RESIZE_SE};
            for (DragMode mode : handles) {
                Rectangle bounds = getHandleBounds(mode);
                if (bounds == null) {
                    continue;
                }

                Rectangle hit = new Rectangle(bounds);
                hit.grow(HANDLE_HIT_PADDING, HANDLE_HIT_PADDING);
                if (hit.contains(point)) {
                    return mode;
                }
            }

            return DragMode.NONE;
        }

        private Rectangle getHandleBounds(DragMode mode) {
            if (selection == null) {
                return null;
            }

            int centerX;
            int centerY;
            switch (mode) {
                case RESIZE_NW:
                    centerX = selection.x;
                    centerY = selection.y;
                    break;
                case RESIZE_NE:
                    centerX = selection.x + selection.width;
                    centerY = selection.y;
                    break;
                case RESIZE_SW:
                    centerX = selection.x;
                    centerY = selection.y + selection.height;
                    break;
                case RESIZE_SE:
                    centerX = selection.x + selection.width;
                    centerY = selection.y + selection.height;
                    break;
                default:
                    return null;
            }

            return new Rectangle(centerX - HANDLE_SIZE / 2, centerY - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        }

        private Rectangle resizeSquareSelection(Rectangle baseSelection, Point current, DragMode mode, Rectangle viewport) {
            Point anchor;
            switch (mode) {
                case RESIZE_NW:
                    anchor = new Point(baseSelection.x + baseSelection.width, baseSelection.y + baseSelection.height);
                    break;
                case RESIZE_NE:
                    anchor = new Point(baseSelection.x, baseSelection.y + baseSelection.height);
                    break;
                case RESIZE_SW:
                    anchor = new Point(baseSelection.x + baseSelection.width, baseSelection.y);
                    break;
                case RESIZE_SE:
                    anchor = new Point(baseSelection.x, baseSelection.y);
                    break;
                default:
                    return new Rectangle(baseSelection);
            }

            int side = Math.max(Math.abs(current.x - anchor.x), Math.abs(current.y - anchor.y));
            int maxSide = Math.max(1, getMaxSideFromAnchor(anchor, viewport, mode));
            int minSide = Math.min(MIN_SELECTION_SIZE, maxSide);
            side = clamp(side, minSide, maxSide);

            int x;
            int y;
            switch (mode) {
                case RESIZE_NW:
                    x = anchor.x - side;
                    y = anchor.y - side;
                    break;
                case RESIZE_NE:
                    x = anchor.x;
                    y = anchor.y - side;
                    break;
                case RESIZE_SW:
                    x = anchor.x - side;
                    y = anchor.y;
                    break;
                case RESIZE_SE:
                    x = anchor.x;
                    y = anchor.y;
                    break;
                default:
                    return new Rectangle(baseSelection);
            }

            return new Rectangle(x, y, side, side);
        }

        private int getMaxSideFromAnchor(Point anchor, Rectangle viewport, DragMode mode) {
            switch (mode) {
                case RESIZE_NW:
                    return Math.min(anchor.x - viewport.x, anchor.y - viewport.y);
                case RESIZE_NE:
                    return Math.min(viewport.x + viewport.width - anchor.x, anchor.y - viewport.y);
                case RESIZE_SW:
                    return Math.min(anchor.x - viewport.x, viewport.y + viewport.height - anchor.y);
                case RESIZE_SE:
                    return Math.min(viewport.x + viewport.width - anchor.x, viewport.y + viewport.height - anchor.y);
                default:
                    return 0;
            }
        }

        private Rectangle clampRectangleToViewport(Rectangle rectangle, Rectangle viewport) {
            Rectangle clamped = new Rectangle(rectangle);

            if (clamped.width > viewport.width) {
                clamped.width = viewport.width;
            }
            if (clamped.height > viewport.height) {
                clamped.height = viewport.height;
            }

            int minX = viewport.x;
            int minY = viewport.y;
            int maxX = viewport.x + viewport.width - clamped.width;
            int maxY = viewport.y + viewport.height - clamped.height;

            clamped.x = clamp(clamped.x, minX, maxX);
            clamped.y = clamp(clamped.y, minY, maxY);
            return clamped;
        }

        private Rectangle getImageViewport() {
            int availableWidth = Math.max(1, getWidth() - (PADDING * 2));
            int availableHeight = Math.max(1, getHeight() - (PADDING * 2));

            double scale = Math.min(
                availableWidth / (double) image.getWidth(),
                availableHeight / (double) image.getHeight()
            );
            scale = Math.max(0.0001, scale);

            int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));

            int x = (getWidth() - drawWidth) / 2;
            int y = (getHeight() - drawHeight) / 2;

            return new Rectangle(x, y, drawWidth, drawHeight);
        }

        private double getScale(Rectangle viewport) {
            return viewport.width / (double) image.getWidth();
        }

        private Point clampToViewport(Point point, Rectangle viewport) {
            int x = clamp(point.x, viewport.x, viewport.x + viewport.width);
            int y = clamp(point.y, viewport.y, viewport.y + viewport.height);
            return new Point(x, y);
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}

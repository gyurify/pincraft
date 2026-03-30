package com.pinbuttonmaker.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

public class PaperPreviewPanel extends JPanel {
    private static final double BLEED_EXTRA_INCHES = 1.0 / 2.54;
    private static final float CUT_LINE_WIDTH_PX = 2.0f;

    private static final Color WORKSPACE_BG = new Color(237, 242, 249);
    private static final Color PAGE_SHADOW = new Color(214, 221, 232, 90);
    private static final Color PAGE_FILL = Color.WHITE;
    private static final Color PAGE_BORDER = new Color(189, 201, 220);
    private static final Color SLOT_FILL = new Color(225, 234, 247, 120);
    private static final Color SLOT_SOFT_BORDER = new Color(206, 216, 232, 170);
    private static final Color CUT_LINE_COLOR = Color.BLACK;
    private static final Color CAPTION_COLOR = new Color(100, 112, 132);
    private static final Color EMPTY_SLOT_LABEL = new Color(141, 152, 171);

    private static final String PAYLOAD_GALLERY = "gallery";
    private static final String PAYLOAD_SLOT = "slot";
    private static final String PAYLOAD_SEPARATOR = "::";

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

    private final Map<String, PrintableItem> availableItemsById;
    private final List<String> slotAssignments;

    private int dragSourceSlotIndex;
    private boolean dragExportTriggered;

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

        this.availableItemsById = new LinkedHashMap<>();
        this.slotAssignments = new ArrayList<>();
        this.dragSourceSlotIndex = -1;
        this.dragExportTriggered = false;

        ensureSlotAssignmentCapacity(layoutInfo.getTotalPins());

        setTransferHandler(new SlotTransferHandler());
        installSlotDragHandler();
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

    public void setAvailableItems(List<PrintableItem> items) {
        availableItemsById.clear();

        if (items != null) {
            for (PrintableItem item : items) {
                if (item == null || item.getItemId() == null) {
                    continue;
                }
                availableItemsById.put(item.getItemId(), item);
            }
        }

        removeUnavailableAssignments();
        repaint();
    }

    public LayoutInfo getLayoutInfo() {
        return layoutInfo;
    }

    public ExportSnapshot buildExportSnapshot() {
        List<PlacedSlot> slots = new ArrayList<>();
        int index = 0;

        for (int row = 0; row < layoutInfo.getRows(); row++) {
            for (int col = 0; col < layoutInfo.getColumns(); col++) {
                double xInches = layoutInfo.getStartXInches() + (col * (buttonDiameterInches + getEffectiveHorizontalGapInches()));
                double yInches = layoutInfo.getStartYInches() + (row * (buttonDiameterInches + getEffectiveVerticalGapInches()));

                BufferedImage image = null;
                if (index >= 0 && index < slotAssignments.size()) {
                    String assignedId = slotAssignments.get(index);
                    if (assignedId != null) {
                        PrintableItem item = availableItemsById.get(assignedId);
                        image = item == null ? null : item.getPreviewImage();
                    }
                }

                slots.add(new PlacedSlot(
                    index,
                    xInches,
                    yInches,
                    buttonDiameterInches,
                    calculateBleedDiameterInches(buttonDiameterInches),
                    image
                ));
                index++;
            }
        }

        return new ExportSnapshot(
            paperLabel,
            paperWidthInches,
            paperHeightInches,
            showCutLines,
            slots
        );
    }

    public static String buildGalleryDragPayload(String itemId) {
        return PAYLOAD_GALLERY + PAYLOAD_SEPARATOR + sanitizePayloadPart(itemId);
    }

    private void refreshLayout() {
        layoutInfo = calculateLayout();
        ensureSlotAssignmentCapacity(layoutInfo.getTotalPins());
        repaint();
    }

    private LayoutInfo calculateLayout() {
        double effectiveHorizontalMargin = getEffectiveHorizontalMarginInches();
        double effectiveVerticalMargin = getEffectiveVerticalMarginInches();
        double effectiveHorizontalGap = getEffectiveHorizontalGapInches();
        double effectiveVerticalGap = getEffectiveVerticalGapInches();

        double usableWidth = Math.max(0.0, paperWidthInches - (effectiveHorizontalMargin * 2.0));
        double usableHeight = Math.max(0.0, paperHeightInches - (effectiveVerticalMargin * 2.0));

        int columns = (int) Math.floor((usableWidth + effectiveHorizontalGap) / (buttonDiameterInches + effectiveHorizontalGap));
        int rows = (int) Math.floor((usableHeight + effectiveVerticalGap) / (buttonDiameterInches + effectiveVerticalGap));

        columns = Math.max(0, columns);
        rows = Math.max(0, rows);

        double occupiedWidth = columns == 0 ? 0.0 : (columns * buttonDiameterInches) + ((columns - 1) * effectiveHorizontalGap);
        double occupiedHeight = rows == 0 ? 0.0 : (rows * buttonDiameterInches) + ((rows - 1) * effectiveVerticalGap);

        double startX = effectiveHorizontalMargin + Math.max(0.0, (usableWidth - occupiedWidth) / 2.0);
        double startY = effectiveVerticalMargin + Math.max(0.0, (usableHeight - occupiedHeight) / 2.0);

        return new LayoutInfo(columns, rows, columns * rows, startX, startY);
    }

    private double getEffectiveHorizontalMarginInches() {
        return Math.max(horizontalMarginInches, BLEED_EXTRA_INCHES / 2.0);
    }

    private double getEffectiveVerticalMarginInches() {
        return Math.max(verticalMarginInches, BLEED_EXTRA_INCHES / 2.0);
    }

    private double getEffectiveHorizontalGapInches() {
        return Math.max(horizontalGapInches, BLEED_EXTRA_INCHES);
    }

    private double getEffectiveVerticalGapInches() {
        return Math.max(verticalGapInches, BLEED_EXTRA_INCHES);
    }

    private void ensureSlotAssignmentCapacity(int totalPins) {
        while (slotAssignments.size() < totalPins) {
            slotAssignments.add(null);
        }
        while (slotAssignments.size() > totalPins) {
            slotAssignments.remove(slotAssignments.size() - 1);
        }
    }

    private void removeUnavailableAssignments() {
        for (int i = 0; i < slotAssignments.size(); i++) {
            String assignedId = slotAssignments.get(i);
            if (assignedId != null && !availableItemsById.containsKey(assignedId)) {
                slotAssignments.set(i, null);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        PaperRect paperRect = calculatePaperRect();
        drawPaper(g2, paperRect.x, paperRect.y, paperRect.width, paperRect.height);

        List<SlotBounds> slots = computeSlotBounds(paperRect);
        drawButtonSlots(g2, slots);
        drawCaption(g2, paperRect.x, paperRect.y);

        g2.dispose();
    }

    private PaperRect calculatePaperRect() {
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
        return new PaperRect(paperX, paperY, paperWidth, paperHeight);
    }

    private List<SlotBounds> computeSlotBounds(PaperRect paperRect) {
        List<SlotBounds> slots = new ArrayList<>();

        if (layoutInfo.getTotalPins() == 0) {
            return slots;
        }

        double scale = Math.min(paperRect.width / paperWidthInches, paperRect.height / paperHeightInches);
        double diameter = buttonDiameterInches * scale;

        int index = 0;
        for (int row = 0; row < layoutInfo.getRows(); row++) {
            for (int col = 0; col < layoutInfo.getColumns(); col++) {
                double slotX = paperRect.x + ((layoutInfo.getStartXInches() + (col * (buttonDiameterInches + getEffectiveHorizontalGapInches()))) * scale);
                double slotY = paperRect.y + ((layoutInfo.getStartYInches() + (row * (buttonDiameterInches + getEffectiveVerticalGapInches()))) * scale);

                slotX = clamp(slotX, paperRect.x + 1.0, (paperRect.x + paperRect.width) - diameter - 1.0);
                slotY = clamp(slotY, paperRect.y + 1.0, (paperRect.y + paperRect.height) - diameter - 1.0);

                slots.add(new SlotBounds(index, slotX, slotY, diameter));
                index++;
            }
        }

        ensureSlotAssignmentCapacity(slots.size());
        return slots;
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

    private void drawButtonSlots(Graphics2D g2, List<SlotBounds> slots) {
        Stroke oldStroke = g2.getStroke();
        Stroke cutLineStroke = new BasicStroke(CUT_LINE_WIDTH_PX, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for (SlotBounds slot : slots) {
            Ellipse2D circle = slot.toEllipse();

            g2.setColor(SLOT_FILL);
            g2.fill(circle);

            boolean hasAssignedPreview = drawAssignedPreview(g2, slot);

            if (!hasAssignedPreview || !showCutLines) {
                g2.setStroke(oldStroke);
                g2.setColor(SLOT_SOFT_BORDER);
                g2.draw(circle);
            }

            if (showCutLines && hasAssignedPreview) {
                g2.setStroke(cutLineStroke);
                g2.setColor(CUT_LINE_COLOR);
                g2.draw(createOuterCutLineEllipse(slot, CUT_LINE_WIDTH_PX));
            }

            if (!hasAssignedPreview) {
                drawEmptySlotHint(g2, slot);
            }
        }

        g2.setStroke(oldStroke);
    }

    private boolean drawAssignedPreview(Graphics2D g2, SlotBounds slot) {
        int index = slot.getIndex();
        if (index < 0 || index >= slotAssignments.size()) {
            return false;
        }

        String itemId = slotAssignments.get(index);
        if (itemId == null) {
            return false;
        }

        PrintableItem item = availableItemsById.get(itemId);
        if (item == null || item.getPreviewImage() == null) {
            return false;
        }

        BufferedImage image = item.getPreviewImage();
        Ellipse2D cutCircle = slot.toEllipse();
        Shape oldClip = g2.getClip();
        g2.clip(cutCircle);

        double scale = Math.max(cutCircle.getWidth() / image.getWidth(), cutCircle.getHeight() / image.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int drawX = (int) Math.round(cutCircle.getX() + (cutCircle.getWidth() - drawWidth) / 2.0);
        int drawY = (int) Math.round(cutCircle.getY() + (cutCircle.getHeight() - drawHeight) / 2.0);

        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        g2.setClip(oldClip);
        return true;
    }

    private Ellipse2D createBleedEllipse(SlotBounds slot) {
        double scaleFactor = getBleedScaleFactor();
        double bleedDiameter = slot.getDiameter() * scaleFactor;
        double inset = (bleedDiameter - slot.getDiameter()) / 2.0;
        return new Ellipse2D.Double(
            slot.getX() - inset,
            slot.getY() - inset,
            bleedDiameter,
            bleedDiameter
        );
    }

    private Ellipse2D createOuterCutLineEllipse(SlotBounds slot, double strokeWidth) {
        double inset = strokeWidth / 2.0;
        return new Ellipse2D.Double(
            slot.getX() - inset,
            slot.getY() - inset,
            slot.getDiameter() + strokeWidth,
            slot.getDiameter() + strokeWidth
        );
    }

    private double getBleedScaleFactor() {
        if (buttonDiameterInches <= 0.0) {
            return 1.0;
        }
        return calculateBleedDiameterInches(buttonDiameterInches) / buttonDiameterInches;
    }

    private static double calculateBleedDiameterInches(double cutDiameterInches) {
        return cutDiameterInches + BLEED_EXTRA_INCHES;
    }

    private void drawEmptySlotHint(Graphics2D g2, SlotBounds slot) {
        g2.setColor(EMPTY_SLOT_LABEL);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

        String hint = "Drop";
        int textWidth = g2.getFontMetrics().stringWidth(hint);
        int x = (int) Math.round(slot.getX() + (slot.getDiameter() - textWidth) / 2.0);
        int y = (int) Math.round(slot.getY() + (slot.getDiameter() / 2.0) + 4);
        g2.drawString(hint, x, y);
    }

    private void drawCaption(Graphics2D g2, int paperX, int paperY) {
        g2.setColor(CAPTION_COLOR);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(paperLabel + " Preview | " + buttonLabel + " | " + layoutInfo.getTotalPins() + " pins", paperX + 12, paperY + 20);
    }

    private void installSlotDragHandler() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                int slotIndex = getSlotIndexAtPoint(event.getPoint());
                if (slotIndex < 0 || slotIndex >= slotAssignments.size() || slotAssignments.get(slotIndex) == null) {
                    dragSourceSlotIndex = -1;
                    dragExportTriggered = false;
                    return;
                }

                dragSourceSlotIndex = slotIndex;
                dragExportTriggered = false;
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragSourceSlotIndex < 0 || dragExportTriggered) {
                    return;
                }

                String assignedId = slotAssignments.get(dragSourceSlotIndex);
                if (assignedId == null) {
                    dragSourceSlotIndex = -1;
                    return;
                }

                TransferHandler transferHandler = getTransferHandler();
                if (transferHandler != null) {
                    dragExportTriggered = true;
                    transferHandler.exportAsDrag(PaperPreviewPanel.this, event, TransferHandler.MOVE);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragSourceSlotIndex = -1;
                dragExportTriggered = false;
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private int getSlotIndexAtPoint(Point point) {
        PaperRect paperRect = calculatePaperRect();
        List<SlotBounds> slots = computeSlotBounds(paperRect);

        for (SlotBounds slot : slots) {
            if (slot.toEllipse().contains(point)) {
                return slot.getIndex();
            }
        }

        return -1;
    }

    private boolean hasItemId(String itemId) {
        return itemId != null && availableItemsById.containsKey(itemId);
    }

    private void assignItemToSlot(int slotIndex, String itemId) {
        if (slotIndex < 0 || slotIndex >= slotAssignments.size() || !hasItemId(itemId)) {
            return;
        }
        slotAssignments.set(slotIndex, itemId);
    }

    private void moveOrSwapSlotItem(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= slotAssignments.size() || toIndex < 0 || toIndex >= slotAssignments.size()) {
            return;
        }

        if (fromIndex == toIndex) {
            return;
        }

        String fromItem = slotAssignments.get(fromIndex);
        if (!hasItemId(fromItem)) {
            return;
        }

        String toItem = slotAssignments.get(toIndex);
        slotAssignments.set(toIndex, fromItem);

        if (toItem != null && hasItemId(toItem)) {
            slotAssignments.set(fromIndex, toItem);
        } else {
            slotAssignments.set(fromIndex, null);
        }
    }

    private String safeTransferText(Transferable transferable) {
        try {
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.stringFlavor);
                if (data instanceof String) {
                    return (String) data;
                }
            }
        } catch (Exception ignored) {
            // Ignore invalid payloads.
        }
        return null;
    }

    private static String sanitizePayloadPart(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(PAYLOAD_SEPARATOR, "").trim();
    }

    private DragPayload parsePayload(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String[] parts = raw.split(PAYLOAD_SEPARATOR);
        if (parts.length < 2) {
            return null;
        }

        String type = parts[0];
        if (PAYLOAD_GALLERY.equals(type)) {
            String itemId = parts[1];
            if (!hasItemId(itemId)) {
                return null;
            }
            return DragPayload.forGallery(itemId);
        }

        if (PAYLOAD_SLOT.equals(type) && parts.length >= 3) {
            int sourceIndex;
            try {
                sourceIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                return null;
            }

            String itemId = parts[2];
            if (!hasItemId(itemId)) {
                return null;
            }
            return DragPayload.forSlot(itemId, sourceIndex);
        }

        return null;
    }

    private final class SlotTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            if (dragSourceSlotIndex < 0 || dragSourceSlotIndex >= slotAssignments.size()) {
                return null;
            }

            String itemId = slotAssignments.get(dragSourceSlotIndex);
            if (!hasItemId(itemId)) {
                return null;
            }

            String payload = PAYLOAD_SLOT + PAYLOAD_SEPARATOR + dragSourceSlotIndex + PAYLOAD_SEPARATOR + sanitizePayloadPart(itemId);
            return new StringSelection(payload);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }

            Point dropPoint = support.getDropLocation().getDropPoint();
            int targetIndex = getSlotIndexAtPoint(dropPoint);
            if (targetIndex < 0) {
                return false;
            }

            String raw = safeTransferText(support.getTransferable());
            return parsePayload(raw) != null;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            Point dropPoint = support.getDropLocation().getDropPoint();
            int targetIndex = getSlotIndexAtPoint(dropPoint);
            if (targetIndex < 0) {
                return false;
            }

            String raw = safeTransferText(support.getTransferable());
            DragPayload payload = parsePayload(raw);
            if (payload == null) {
                return false;
            }

            if (payload.getType() == DragPayload.Type.SLOT) {
                moveOrSwapSlotItem(payload.getSourceSlotIndex(), targetIndex);
            } else {
                assignItemToSlot(targetIndex, payload.getItemId());
            }

            repaint();
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            dragSourceSlotIndex = -1;
            dragExportTriggered = false;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class PrintableItem {
        private final String itemId;
        private final String displayName;
        private final BufferedImage previewImage;

        public PrintableItem(String itemId, String displayName, BufferedImage previewImage) {
            this.itemId = itemId;
            this.displayName = displayName == null ? "Item" : displayName;
            this.previewImage = previewImage;
        }

        public String getItemId() {
            return itemId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public BufferedImage getPreviewImage() {
            return previewImage;
        }
    }

    public static final class ExportSnapshot {
        private final String paperLabel;
        private final double paperWidthInches;
        private final double paperHeightInches;
        private final boolean showCutLines;
        private final List<PlacedSlot> slots;

        private ExportSnapshot(
            String paperLabel,
            double paperWidthInches,
            double paperHeightInches,
            boolean showCutLines,
            List<PlacedSlot> slots
        ) {
            this.paperLabel = paperLabel;
            this.paperWidthInches = paperWidthInches;
            this.paperHeightInches = paperHeightInches;
            this.showCutLines = showCutLines;
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }

        public String getPaperLabel() {
            return paperLabel;
        }

        public double getPaperWidthInches() {
            return paperWidthInches;
        }

        public double getPaperHeightInches() {
            return paperHeightInches;
        }

        public boolean isShowCutLines() {
            return showCutLines;
        }

        public List<PlacedSlot> getSlots() {
            return slots;
        }
    }

    public static final class PlacedSlot {
        private final int index;
        private final double xInches;
        private final double yInches;
        private final double diameterInches;
        private final double bleedDiameterInches;
        private final BufferedImage previewImage;

        private PlacedSlot(int index, double xInches, double yInches, double diameterInches, double bleedDiameterInches, BufferedImage previewImage) {
            this.index = index;
            this.xInches = xInches;
            this.yInches = yInches;
            this.diameterInches = diameterInches;
            this.bleedDiameterInches = bleedDiameterInches;
            this.previewImage = previewImage;
        }

        public int getIndex() {
            return index;
        }

        public double getXInches() {
            return xInches;
        }

        public double getYInches() {
            return yInches;
        }

        public double getDiameterInches() {
            return diameterInches;
        }

        public double getBleedDiameterInches() {
            return bleedDiameterInches;
        }

        public BufferedImage getPreviewImage() {
            return previewImage;
        }
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

    private static final class PaperRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private PaperRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class SlotBounds {
        private final int index;
        private final double x;
        private final double y;
        private final double diameter;

        private SlotBounds(int index, double x, double y, double diameter) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.diameter = diameter;
        }

        private int getIndex() {
            return index;
        }

        private double getX() {
            return x;
        }

        private double getY() {
            return y;
        }

        private double getDiameter() {
            return diameter;
        }

        private Ellipse2D toEllipse() {
            return new Ellipse2D.Double(x, y, diameter, diameter);
        }
    }

    private static final class DragPayload {
        private enum Type {
            GALLERY,
            SLOT
        }

        private final Type type;
        private final String itemId;
        private final int sourceSlotIndex;

        private DragPayload(Type type, String itemId, int sourceSlotIndex) {
            this.type = type;
            this.itemId = itemId;
            this.sourceSlotIndex = sourceSlotIndex;
        }

        private static DragPayload forGallery(String itemId) {
            return new DragPayload(Type.GALLERY, itemId, -1);
        }

        private static DragPayload forSlot(String itemId, int sourceSlotIndex) {
            return new DragPayload(Type.SLOT, itemId, sourceSlotIndex);
        }

        private Type getType() {
            return type;
        }

        private String getItemId() {
            return itemId;
        }

        private int getSourceSlotIndex() {
            return sourceSlotIndex;
        }
    }
}

package com.pinbuttonmaker.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.data.LayerData;
import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.ui.UIStyles;
import com.pinbuttonmaker.ui.components.CustomButton;
import com.pinbuttonmaker.ui.components.PaperPreviewPanel;
import com.pinbuttonmaker.util.Utils;

public class PrintPage extends JPanel {
    private static final Color PAGE_BG = new Color(243, 246, 251);
    private static final Color PANEL_BG = new Color(250, 252, 255);
    private static final Color BORDER = new Color(212, 220, 233);
    private static final Color TEXT_PRIMARY = new Color(35, 46, 66);
    private static final Color TEXT_SECONDARY = new Color(100, 112, 132);
    private static final Color PREVIEW_BOX_BG = new Color(245, 248, 252);

    private static final PaperSizeOption[] PAPER_SIZE_OPTIONS = {
        new PaperSizeOption("A4", "210 x 297 mm", 8.27, 11.69),
        new PaperSizeOption("Letter", "216 x 279 mm", 8.50, 11.00)
    };

    private static final ButtonSizeOption[] BUTTON_SIZE_OPTIONS = {
        new ButtonSizeOption("1\"", 1.00, 96, 0.30, 0.35, 0.18, 0.20),
        new ButtonSizeOption("1.25\"", 1.25, 120, 0.32, 0.38, 0.20, 0.24),
        new ButtonSizeOption("1.5\"", 1.50, 144, 0.34, 0.42, 0.24, 0.28),
        new ButtonSizeOption("2\"", 2.00, 192, 0.35, 0.50, 0.28, 0.42),
        new ButtonSizeOption("2.25\"", 2.25, 216, 0.36, 0.62, 0.30, 0.80),
        new ButtonSizeOption("2.5\"", 2.50, 240, 0.38, 0.65, 0.32, 0.85),
        new ButtonSizeOption("3\"", 3.00, 288, 0.42, 0.72, 0.36, 1.00),
        new ButtonSizeOption("6\"", 6.00, 576, 0.55, 0.90, 0.50, 1.10)
    };

    private final AppRouter router;
    private final AppState appState;

    private JCheckBox showCutLinesCheck;
    private JComboBox<PaperSizeOption> paperSizeCombo;
    private JComboBox<ButtonSizeOption> buttonSizeCombo;

    private JPanel galleryListPanel;
    private PaperPreviewPanel paperPreviewPanel;
    private JLabel previewTitleLabel;
    private JLabel fitSummaryLabel;

    public PrintPage(AppRouter router, AppState appState) {
        this.router = router;
        this.appState = appState;

        buildLayout();
        syncButtonSizeFromCurrentProject();
        refreshPreviewState();
        registerLifecycleRefresh();
        refreshGallery();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(16, 0));
        setBackground(PAGE_BG);
        UIStyles.applyPagePadding(this);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        splitPane.setResizeWeight(0.66);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerSize(8);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setBackground(PAGE_BG);

        previewTitleLabel = new JLabel("Paper Preview");
        previewTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        previewTitleLabel.setForeground(TEXT_PRIMARY);

        JPanel previewCard = new JPanel(new BorderLayout());
        previewCard.setBackground(PANEL_BG);
        previewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        paperPreviewPanel = new PaperPreviewPanel();
        previewCard.add(paperPreviewPanel, BorderLayout.CENTER);

        leftPanel.add(previewTitleLabel, BorderLayout.NORTH);
        leftPanel.add(previewCard, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 14));
        rightPanel.setBackground(PAGE_BG);
        rightPanel.setPreferredSize(new Dimension(360, 0));
        rightPanel.setMinimumSize(new Dimension(320, 0));

        JPanel controlsCard = new JPanel();
        controlsCard.setLayout(new BoxLayout(controlsCard, BoxLayout.Y_AXIS));
        controlsCard.setBackground(PANEL_BG);
        controlsCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel settingsTitle = new JLabel("Print Settings");
        settingsTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        settingsTitle.setForeground(TEXT_PRIMARY);

        paperSizeCombo = new JComboBox<>(PAPER_SIZE_OPTIONS);
        paperSizeCombo.addActionListener(event -> refreshPreviewState());

        buttonSizeCombo = new JComboBox<>(BUTTON_SIZE_OPTIONS);
        buttonSizeCombo.addActionListener(event -> {
            applySelectedButtonSizeToProject();
            refreshPreviewState();
            refreshGallery();
        });

        showCutLinesCheck = new JCheckBox("Show Cut Lines", true);
        showCutLinesCheck.setBackground(PANEL_BG);
        showCutLinesCheck.setForeground(TEXT_PRIMARY);
        showCutLinesCheck.setFocusPainted(false);
        showCutLinesCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        showCutLinesCheck.addActionListener(event -> refreshPreviewState());

        fitSummaryLabel = new JLabel();
        fitSummaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        fitSummaryLabel.setForeground(TEXT_SECONDARY);
        fitSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        CustomButton backButton = new CustomButton("Back");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        backButton.setPreferredSize(new Dimension(160, 48));
        backButton.setMaximumSize(new Dimension(160, 48));
        backButton.addActionListener(event -> router.showEditor());

        CustomButton downloadPdfButton = new CustomButton("Download PDF");
        downloadPdfButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        downloadPdfButton.setPreferredSize(new Dimension(160, 48));
        downloadPdfButton.setMaximumSize(new Dimension(160, 48));
        downloadPdfButton.addActionListener(event -> Utils.showInfo(this, "PDF export is prototype-only in this build."));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionRow.add(backButton);
        actionRow.add(downloadPdfButton);

        controlsCard.add(settingsTitle);
        controlsCard.add(Box.createVerticalStrut(12));
        controlsCard.add(createField("Paper Size", paperSizeCombo));
        controlsCard.add(Box.createVerticalStrut(10));
        controlsCard.add(createField("Button Size", buttonSizeCombo));
        controlsCard.add(Box.createVerticalStrut(10));
        controlsCard.add(showCutLinesCheck);
        controlsCard.add(Box.createVerticalStrut(8));
        controlsCard.add(fitSummaryLabel);
        controlsCard.add(Box.createVerticalStrut(14));
        controlsCard.add(actionRow);

        JPanel galleryCard = new JPanel(new BorderLayout(0, 10));
        galleryCard.setBackground(PANEL_BG);
        galleryCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel galleryTitle = new JLabel("Printable Items");
        galleryTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        galleryTitle.setForeground(TEXT_PRIMARY);

        galleryListPanel = new JPanel();
        galleryListPanel.setLayout(new BoxLayout(galleryListPanel, BoxLayout.Y_AXIS));
        galleryListPanel.setBackground(PANEL_BG);

        JScrollPane galleryScroll = new JScrollPane(galleryListPanel);
        galleryScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        galleryScroll.getVerticalScrollBar().setUnitIncrement(14);
        galleryScroll.getViewport().setBackground(PANEL_BG);

        galleryCard.add(galleryTitle, BorderLayout.NORTH);
        galleryCard.add(galleryScroll, BorderLayout.CENTER);

        rightPanel.add(controlsCard, BorderLayout.NORTH);
        rightPanel.add(galleryCard, BorderLayout.CENTER);
        return rightPanel;
    }

    private JPanel createField(String labelText, JComboBox<?> comboBox) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 6));
        fieldPanel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(TEXT_SECONDARY);

        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        fieldPanel.add(label, BorderLayout.NORTH);
        fieldPanel.add(comboBox, BorderLayout.CENTER);
        return fieldPanel;
    }

    private void syncButtonSizeFromCurrentProject() {
        double targetMm = appState.getCurrentProject().getButtonDiameterMm();
        int bestIndex = 0;
        double bestDiff = Double.MAX_VALUE;

        for (int i = 0; i < BUTTON_SIZE_OPTIONS.length; i++) {
            double diff = Math.abs(BUTTON_SIZE_OPTIONS[i].toMillimeters() - targetMm);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        }

        buttonSizeCombo.setSelectedIndex(bestIndex);
        applySelectedButtonSizeToProject();
    }

    private void applySelectedButtonSizeToProject() {
        ButtonSizeOption selected = getSelectedButtonOption();
        appState.getCurrentProject().setButtonDiameterMm(selected.toMillimeters());
    }

    private void refreshPreviewState() {
        PaperSizeOption paper = getSelectedPaperOption();
        ButtonSizeOption button = getSelectedButtonOption();

        paperPreviewPanel.setPaperSize(paper.paperLabel, paper.widthInches, paper.heightInches);
        paperPreviewPanel.setButtonLayout(
            button.displayLabel,
            button.inches,
            button.horizontalMarginInches,
            button.verticalMarginInches,
            button.horizontalGapInches,
            button.verticalGapInches
        );
        paperPreviewPanel.setShowCutLines(showCutLinesCheck.isSelected());

        PaperPreviewPanel.LayoutInfo layout = paperPreviewPanel.getLayoutInfo();
        previewTitleLabel.setText(paper.paperLabel + " Paper Preview");
        fitSummaryLabel.setText("Fits " + layout.getTotalPins() + " pins (" + layout.getColumns() + " x " + layout.getRows() + ") at " + button.displayLabel);
    }

    private void registerLifecycleRefresh() {
        addHierarchyListener(event -> {
            long flags = event.getChangeFlags();
            if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshGallery();
                refreshPreviewState();
            }
        });
    }

    private void refreshGallery() {
        galleryListPanel.removeAll();

        ProjectData currentProject = appState.getCurrentProject();
        List<LayerData> printableLayers = getEligibleGalleryLayers(currentProject);

        if (currentProject == null || printableLayers.isEmpty()) {
            JLabel empty = new JLabel("No printable layers in the current project.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(TEXT_SECONDARY);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            galleryListPanel.add(empty);
        } else {
            JLabel sourceLabel = new JLabel("Source: " + getDisplayProjectName(currentProject));
            sourceLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            sourceLabel.setForeground(TEXT_SECONDARY);
            sourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
            galleryListPanel.add(sourceLabel);

            for (LayerData layer : printableLayers) {
                galleryListPanel.add(createGalleryItem(layer));
                galleryListPanel.add(Box.createVerticalStrut(8));
            }
        }

        galleryListPanel.revalidate();
        galleryListPanel.repaint();
    }

    private List<LayerData> getEligibleGalleryLayers(ProjectData project) {
        List<LayerData> printableLayers = new ArrayList<>();
        if (project == null) {
            return printableLayers;
        }

        for (LayerData layer : project.getLayers()) {
            if (isEligibleGalleryLayer(layer)) {
                printableLayers.add(layer);
            }
        }
        return printableLayers;
    }

    private boolean isEligibleGalleryLayer(LayerData layer) {
        if (layer == null || !layer.isPrintable()) {
            return false;
        }

        if (layer.isTextLayer()) {
            String text = layer.getTextContent();
            return text != null && !text.trim().isEmpty();
        }

        return layer.hasPhotoImage();
    }

    private JPanel createGalleryItem(LayerData layer) {
        JPanel item = new JPanel(new BorderLayout(0, 8));
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel nameLabel = new JLabel(getLayerDisplayName(layer));
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        nameLabel.setForeground(TEXT_PRIMARY);

        JPanel previewBox = new JPanel(new BorderLayout());
        previewBox.setOpaque(true);
        previewBox.setBackground(PREVIEW_BOX_BG);
        previewBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 241)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        previewBox.add(createGalleryContentPreview(layer), BorderLayout.CENTER);

        JLabel detailsLabel = new JLabel(buildLayerDetails(layer));
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailsLabel.setForeground(TEXT_SECONDARY);

        item.add(nameLabel, BorderLayout.NORTH);
        item.add(previewBox, BorderLayout.CENTER);
        item.add(detailsLabel, BorderLayout.SOUTH);
        return item;
    }

    private JComponent createGalleryContentPreview(LayerData layer) {
        if (layer.isTextLayer()) {
            JLabel previewLabel = new JLabel(getLayerTextPreview(layer));
            previewLabel.setFont(new Font(layer.getFontFamily(), Font.BOLD, Math.max(12, Math.min(18, layer.getFontSize()))));
            previewLabel.setForeground(layer.getColor() == null ? TEXT_PRIMARY : layer.getColor());
            previewLabel.setVerticalAlignment(SwingConstants.CENTER);
            return previewLabel;
        }

        BufferedImage image = layer.getPhotoImage();
        JLabel photoLabel = new JLabel();
        photoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        photoLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (image != null) {
            photoLabel.setIcon(new ImageIcon(createScaledPreviewImage(image, 84)));
        }

        return photoLabel;
    }

    private Image createScaledPreviewImage(BufferedImage image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        double scale = Math.min((double) maxSize / Math.max(1, width), (double) maxSize / Math.max(1, height));
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        return image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }

    private String getLayerDisplayName(LayerData layer) {
        if (layer == null) {
            return "Layer";
        }

        String layerName = layer.getLayerName();
        if (layerName == null || layerName.trim().isEmpty()) {
            return layer.isTextLayer() ? "Text Layer" : "Photo Layer";
        }
        return layerName.trim();
    }

    private String getLayerTextPreview(LayerData layer) {
        String text = layer.getTextContent();
        if (text == null || text.trim().isEmpty()) {
            return "(Empty text)";
        }
        return text.trim();
    }

    private String buildLayerDetails(LayerData layer) {
        String kindText = layer.isTextLayer() ? "Text" : "Photo";
        String opacityText = layer.getTransparencyPercent() + "% opacity";

        if (layer.isTextLayer()) {
            return kindText + " | " + opacityText + " | " + layer.getFontFamily();
        }

        BufferedImage image = layer.getPhotoImage();
        if (image != null) {
            return kindText + " | " + opacityText + " | " + image.getWidth() + " x " + image.getHeight();
        }

        return kindText + " | " + opacityText;
    }

    private String getDisplayProjectName(ProjectData project) {
        if (project == null || project.getProjectName() == null || project.getProjectName().trim().isEmpty()) {
            return "Untitled Project";
        }
        return project.getProjectName().trim();
    }

    private String resolveProjectButtonSizeText(double diameterMm) {
        ButtonSizeOption bestMatch = BUTTON_SIZE_OPTIONS[0];
        double bestDiff = Double.MAX_VALUE;
        for (ButtonSizeOption option : BUTTON_SIZE_OPTIONS) {
            double diff = Math.abs(option.toMillimeters() - diameterMm);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestMatch = option;
            }
        }
        return bestMatch.displayLabel;
    }

    private PaperSizeOption getSelectedPaperOption() {
        PaperSizeOption selected = (PaperSizeOption) paperSizeCombo.getSelectedItem();
        return selected == null ? PAPER_SIZE_OPTIONS[0] : selected;
    }

    private ButtonSizeOption getSelectedButtonOption() {
        ButtonSizeOption selected = (ButtonSizeOption) buttonSizeCombo.getSelectedItem();
        return selected == null ? BUTTON_SIZE_OPTIONS[4] : selected;
    }

    private static final class PaperSizeOption {
        private final String paperLabel;
        private final String metricLabel;
        private final double widthInches;
        private final double heightInches;

        private PaperSizeOption(String paperLabel, String metricLabel, double widthInches, double heightInches) {
            this.paperLabel = paperLabel;
            this.metricLabel = metricLabel;
            this.widthInches = widthInches;
            this.heightInches = heightInches;
        }

        @Override
        public String toString() {
            return paperLabel + " (" + metricLabel + ")";
        }
    }

    private static final class ButtonSizeOption {
        private final String displayLabel;
        private final double inches;
        private final int pixels;
        private final double horizontalMarginInches;
        private final double verticalMarginInches;
        private final double horizontalGapInches;
        private final double verticalGapInches;

        private ButtonSizeOption(String displayLabel, double inches, int pixels, double horizontalMarginInches, double verticalMarginInches, double horizontalGapInches, double verticalGapInches) {
            this.displayLabel = displayLabel;
            this.inches = inches;
            this.pixels = pixels;
            this.horizontalMarginInches = horizontalMarginInches;
            this.verticalMarginInches = verticalMarginInches;
            this.horizontalGapInches = horizontalGapInches;
            this.verticalGapInches = verticalGapInches;
        }

        private double toMillimeters() {
            return inches * 25.4;
        }

        @Override
        public String toString() {
            return displayLabel;
        }
    }
}

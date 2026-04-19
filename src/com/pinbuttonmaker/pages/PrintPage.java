package com.pinbuttonmaker.pages;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.data.LayerData;
import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.ui.UIStyles;
import com.pinbuttonmaker.ui.components.ButtonPreviewPanel;
import com.pinbuttonmaker.ui.components.CustomButton;
import com.pinbuttonmaker.ui.components.PaperPreviewPanel;
import com.pinbuttonmaker.util.PdfExporter;
import com.pinbuttonmaker.util.Utils;

public class PrintPage extends JPanel {
    private static final int PRINTABLE_PREVIEW_RESOLUTION = 720;
    private static final double PRINT_SIZE_EXTRA_INCHES = 1.0 / 2.54;

    private static final Color PAGE_BG = UIStyles.SHELL_BG;
    private static final Color PANEL_BG = UIStyles.PANEL_BG;
    private static final Color BORDER = UIStyles.PANEL_BORDER;
    private static final Color TEXT_PRIMARY = UIStyles.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = UIStyles.TEXT_MUTED;
    private static final Color PREVIEW_BOX_BG = UIStyles.PANEL_ALT_BG;

    private static final Color LIGHT_PAGE_BG = new Color(243, 246, 251);
    private static final Color LIGHT_PANEL_BG = new Color(250, 252, 255);
    private static final Color LIGHT_BORDER = new Color(212, 220, 233);
    private static final Color LIGHT_TEXT_PRIMARY = new Color(35, 46, 66);
    private static final Color LIGHT_TEXT_SECONDARY = new Color(100, 112, 132);
    private static final Color LIGHT_PREVIEW_BOX_BG = new Color(245, 248, 252);

    private static final PaperSizeOption[] PAPER_SIZE_OPTIONS = {
        new PaperSizeOption("A4", "210 x 297 mm", 210.0 / 25.4, 297.0 / 25.4),
        new PaperSizeOption("Letter", "216 x 279 mm", 8.50, 11.00)
    };

    private static final ButtonSizeOption[] BUTTON_SIZE_OPTIONS = {
        // Tuned for a 1 cm bleed so the spacing on Short/Letter paper matches the reference export more closely.
        new ButtonSizeOption("1\"", 1.00, 96, 0.20, 0.26, 0.48, 0.56),
        new ButtonSizeOption("1.25\"", 1.25, 120, 0.20, 0.26, 0.50, 0.60),
        new ButtonSizeOption("1.5\"", 1.50, 144, 0.20, 0.28, 0.53, 0.66),
        new ButtonSizeOption("2\"", 2.00, 192, 0.22, 0.30, 0.56, 0.72),
        new ButtonSizeOption("2.25\"", 2.25, 216, 0.24, 0.32, 0.58, 0.78),
        new ButtonSizeOption("2.5\"", 2.50, 240, 0.24, 0.34, 0.62, 0.86),
        new ButtonSizeOption("3\"", 3.00, 288, 0.26, 0.36, 0.68, 0.96),
        new ButtonSizeOption("6\"", 6.00, 576, 0.30, 0.40, 0.80, 1.20)
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

    private final List<PaperPreviewPanel.PrintableItem> galleryItems;

    public PrintPage(AppRouter router, AppState appState) {
        this.router = router;
        this.appState = appState;
        this.galleryItems = new ArrayList<>();

        buildLayout();
        syncButtonSizeFromCurrentProject();
        refreshPreviewState();
        registerLifecycleRefresh();
        refreshGallery();
    }

    private Color pageBg() {
        return appState.isDarkMode() ? PAGE_BG : LIGHT_PAGE_BG;
    }

    private Color panelBg() {
        return appState.isDarkMode() ? PANEL_BG : LIGHT_PANEL_BG;
    }

    private Color borderColor() {
        return appState.isDarkMode() ? BORDER : LIGHT_BORDER;
    }

    private Color primaryText() {
        return appState.isDarkMode() ? TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
    }

    private Color secondaryText() {
        return appState.isDarkMode() ? TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;
    }

    private Color previewBoxBg() {
        return appState.isDarkMode() ? PREVIEW_BOX_BG : LIGHT_PREVIEW_BOX_BG;
    }

    private void buildLayout() {
        setLayout(new BorderLayout(16, 0));
        setBackground(pageBg());
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
        leftPanel.setBackground(pageBg());

        previewTitleLabel = new JLabel("Paper Preview");
        previewTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        previewTitleLabel.setForeground(primaryText());

        JPanel previewCard = new JPanel(new BorderLayout());
        previewCard.setBackground(panelBg());
        previewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor()),
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
        rightPanel.setBackground(pageBg());
        rightPanel.setPreferredSize(new Dimension(360, 0));
        rightPanel.setMinimumSize(new Dimension(320, 0));

        JPanel controlsCard = new JPanel();
        controlsCard.setLayout(new BoxLayout(controlsCard, BoxLayout.Y_AXIS));
        controlsCard.setBackground(panelBg());
        controlsCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor()),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel settingsTitle = new JLabel("Print Settings");
        settingsTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        settingsTitle.setForeground(primaryText());
        settingsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsTitle.setHorizontalAlignment(SwingConstants.CENTER);

        paperSizeCombo = new JComboBox<>(PAPER_SIZE_OPTIONS);
        styleComboBox(paperSizeCombo);
        paperSizeCombo.addActionListener(event -> {
            refreshPreviewState();
            refreshGallery();
        });

        buttonSizeCombo = new JComboBox<>(BUTTON_SIZE_OPTIONS);
        styleComboBox(buttonSizeCombo);
        buttonSizeCombo.addActionListener(event -> {
            applySelectedButtonSizeToProject();
            refreshPreviewState();
            refreshGallery();
        });

        showCutLinesCheck = new JCheckBox("Show Cut Lines", true);
        showCutLinesCheck.setBackground(panelBg());
        showCutLinesCheck.setForeground(primaryText());
        showCutLinesCheck.setFocusPainted(false);
        showCutLinesCheck.setOpaque(true);
        showCutLinesCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        showCutLinesCheck.addActionListener(event -> refreshPreviewState());

        fitSummaryLabel = new JLabel();
        fitSummaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        fitSummaryLabel.setForeground(secondaryText());
        fitSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //back button.
        CustomButton backButton = new CustomButton("Back");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        backButton.setPreferredSize(new Dimension(160, 48));
        backButton.setMaximumSize(new Dimension(160, 48));
        backButton.setBackground(UIStyles.ACTION_GREY);
        backButton.setForeground(UIStyles.TEXT_PRIMARY);
        //return to the editor without changing the placed print items.
        backButton.addActionListener(event -> router.showEditor());

        //download pdf button.
        CustomButton downloadPdfButton = new CustomButton("Download PDF");
        downloadPdfButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        downloadPdfButton.setPreferredSize(new Dimension(160, 48));
        downloadPdfButton.setMaximumSize(new Dimension(160, 48));
        downloadPdfButton.setBackground(UIStyles.ACTION_BLUE);
        downloadPdfButton.setForeground(UIStyles.TEXT_PRIMARY);
        downloadPdfButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyles.ACTION_BLUE_HOVER),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        //build the current print layout and export it as a pdf file.
        downloadPdfButton.addActionListener(event -> exportCurrentLayoutAsPdf());

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
        galleryCard.setBackground(panelBg());
        galleryCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor()),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel galleryTitle = new JLabel("Printable Items");
        galleryTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        galleryTitle.setForeground(primaryText());

        galleryListPanel = new JPanel();
        galleryListPanel.setLayout(new BoxLayout(galleryListPanel, BoxLayout.Y_AXIS));
        galleryListPanel.setBackground(panelBg());

        JScrollPane galleryScroll = new JScrollPane(galleryListPanel);
        galleryScroll.setBorder(BorderFactory.createLineBorder(borderColor()));
        galleryScroll.getVerticalScrollBar().setUnitIncrement(14);
        galleryScroll.getViewport().setBackground(panelBg());

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
        label.setForeground(secondaryText());

        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        fieldPanel.add(label, BorderLayout.NORTH);
        fieldPanel.add(comboBox, BorderLayout.CENTER);
        return fieldPanel;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(appState.isDarkMode() ? UIStyles.PANEL_ALT_BG : Color.WHITE);
        comboBox.setForeground(primaryText());
        comboBox.setBorder(BorderFactory.createLineBorder(borderColor()));
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
        double printCutDiameterInches = getPrintCutDiameterInches(button);

        paperPreviewPanel.setPaperSize(paper.paperLabel, paper.widthInches, paper.heightInches);
        paperPreviewPanel.setButtonLayout(
            button.displayLabel + " + 1 cm",
            printCutDiameterInches,
            button.horizontalMarginInches,
            button.verticalMarginInches,
            button.horizontalGapInches,
            button.verticalGapInches
        );
        paperPreviewPanel.setShowCutLines(showCutLinesCheck.isSelected());

        PaperPreviewPanel.LayoutInfo layout = paperPreviewPanel.getLayoutInfo();
        previewTitleLabel.setText(paper.paperLabel + " Paper Preview");
        fitSummaryLabel.setText(
            "Fits "
                + layout.getTotalPins()
                + " pins ("
                + layout.getColumns()
                + " x "
                + layout.getRows()
                + ") at "
                + button.displayLabel
                + ""
        );
    }

    private void registerLifecycleRefresh() {
        addHierarchyListener(event -> {
            long flags = event.getChangeFlags();
            if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshPreviewState();
                refreshGallery();
            }
        });
    }

    private void refreshGallery() {
        galleryListPanel.removeAll();
        galleryItems.clear();

        List<ProjectData> printableProjects = getPrintableProjectsForGallery();
        if (printableProjects.isEmpty()) {
            JLabel empty = new JLabel("No printable designs yet. Add visible text or photo layers in Editor and save.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(secondaryText());
            empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            galleryListPanel.add(empty);
        } else {
            JPanel cardsWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            cardsWrap.setOpaque(false);

            for (ProjectData project : printableProjects) {
                PaperPreviewPanel.PrintableItem item = createPrintableItem(project);
                galleryItems.add(item);
                cardsWrap.add(createGalleryItem(item, project));
            }

            galleryListPanel.add(cardsWrap);
        }

        paperPreviewPanel.setAvailableItems(galleryItems);

        galleryListPanel.revalidate();
        galleryListPanel.repaint();
    }

    private void exportCurrentLayoutAsPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File("pincraft-print-layout.pdf"));

        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null) {
            return;
        }

        File outputFile = selected.getName().toLowerCase().endsWith(".pdf")
            ? selected
            : new File(selected.getParentFile(), selected.getName() + ".pdf");

        try {
            PaperPreviewPanel.ExportSnapshot snapshot = paperPreviewPanel.buildExportSnapshot();
            PdfExporter.exportPdf(outputFile, snapshot);
            Utils.showInfo(
                this,
                "PDF exported to:\n"
                    + outputFile.getAbsolutePath()
                    + "\n\nPrint with Actual size / 100% scaling for exact button diameters."
            );
        } catch (Exception exception) {
            Utils.showInfo(this, "PDF export failed: " + exception.getMessage());
        }
    }

    private List<ProjectData> getPrintableProjectsForGallery() {
        List<ProjectData> projects = new ArrayList<>();

        for (ProjectData saved : appState.getSavedProjects()) {
            if (saved == null || getRenderableLayers(saved).isEmpty()) {
                continue;
            }

            projects.add(saved);
        }

        return projects;
    }

    private List<LayerData> getRenderableLayers(ProjectData project) {
        List<LayerData> layers = new ArrayList<>();
        if (project == null) {
            return layers;
        }

        for (LayerData layer : project.getLayers()) {
            if (!isRenderableLayer(layer)) {
                continue;
            }
            layers.add(layer);
        }
        return layers;
    }

    private boolean isRenderableLayer(LayerData layer) {
        if (layer == null || !layer.isPrintable()) {
            return false;
        }

        if (layer.isTextLayer()) {
            String text = layer.getTextContent();
            return text != null && !text.trim().isEmpty();
        }

        return layer.hasPhotoImage();
    }

    private PaperPreviewPanel.PrintableItem createPrintableItem(ProjectData project) {
        String itemId = buildPrintableItemId(project);
        String label = buildProjectCardTitle(project);
        BufferedImage previewImage = createProjectPinPreview(project, PRINTABLE_PREVIEW_RESOLUTION);
        return new PaperPreviewPanel.PrintableItem(itemId, label, previewImage);
    }

    private String buildPrintableItemId(ProjectData project) {
        String projectId = project == null || project.getProjectId() == null ? "project" : project.getProjectId();
        return "project-" + projectId;
    }

    private JPanel createGalleryItem(PaperPreviewPanel.PrintableItem itemData, ProjectData project) {
        JPanel item = new JPanel(new BorderLayout(0, 0));
        item.setBackground(panelBg());
        item.setPreferredSize(new Dimension(154, 154));
        item.setMinimumSize(new Dimension(154, 154));
        item.setMaximumSize(new Dimension(154, 154));
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor()),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIStyles.TOP_BAR_BG);
        titleBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel nameLabel = new JLabel(itemData.getDisplayName());
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        nameLabel.setForeground(UIStyles.TEXT_PRIMARY);
        titleBar.add(nameLabel, BorderLayout.CENTER);

        JPanel previewBox = new JPanel(new BorderLayout(0, 4));
        previewBox.setOpaque(true);
        previewBox.setBackground(previewBoxBg());
        previewBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        previewBox.add(createGalleryContentPreview(itemData), BorderLayout.CENTER);

        JLabel detailsLabel = new JLabel(resolveProjectButtonSizeText(project.getButtonDiameterMm()), SwingConstants.CENTER);
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailsLabel.setForeground(secondaryText());
        previewBox.add(detailsLabel, BorderLayout.SOUTH);

        item.add(titleBar, BorderLayout.NORTH);
        item.add(previewBox, BorderLayout.CENTER);

        installGalleryDrag(item, itemData.getItemId());
        installGalleryClickActions(item, itemData.getItemId());

        return item;
    }

    private JComponent createGalleryContentPreview(PaperPreviewPanel.PrintableItem item) {
        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);

        BufferedImage image = item.getPreviewImage();
        if (image != null) {
            previewLabel.setIcon(new ImageIcon(createScaledPreviewImage(image, 72)));
        } else {
            previewLabel.setText(item.getDisplayName());
            previewLabel.setForeground(secondaryText());
            previewLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        return previewLabel;
    }

    private void installGalleryDrag(JComponent root, String itemId) {
        GalleryItemTransferHandler transferHandler = new GalleryItemTransferHandler(itemId);
        MouseAdapter dragStarter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                JComponent source = (JComponent) event.getSource();
                TransferHandler handler = source.getTransferHandler();
                if (handler != null) {
                    handler.exportAsDrag(source, event, TransferHandler.COPY);
                }
            }
        };

        applyDragSupportRecursive(root, transferHandler, dragStarter);
    }

    private void installGalleryClickActions(JComponent root, String itemId) {
        MouseAdapter galleryClickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }

                if (event.getClickCount() >= 2) {
                    paperPreviewPanel.fillAllSlotsWithItem(itemId);
                    return;
                }

                if (event.getClickCount() == 1) {
                    paperPreviewPanel.clearAssignmentsForItem(itemId);
                }
            }
        };

        applyMouseListenerRecursive(root, galleryClickHandler);
    }

    private void applyDragSupportRecursive(Component component, TransferHandler transferHandler, MouseAdapter dragStarter) {
        if (component instanceof JComponent) {
            JComponent dragComponent = (JComponent) component;
            dragComponent.setTransferHandler(transferHandler);
            dragComponent.addMouseListener(dragStarter);
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyDragSupportRecursive(child, transferHandler, dragStarter);
            }
        }
    }

    private void applyMouseListenerRecursive(Component component, MouseAdapter mouseAdapter) {
        component.addMouseListener(mouseAdapter);

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyMouseListenerRecursive(child, mouseAdapter);
            }
        }
    }

    private BufferedImage createProjectPinPreview(ProjectData project, int size) {
        ButtonSizeOption button = getSelectedButtonOption();
        return ButtonPreviewPanel.createPrintableArtworkImage(
            project,
            size,
            getPrintCutDiameterInches(button),
            paperPreviewPanel.getBleedExtraInches()
        );
    }

    private double getPrintCutDiameterInches(ButtonSizeOption button) {
        if (button == null) {
            return PRINT_SIZE_EXTRA_INCHES;
        }
        return button.inches + PRINT_SIZE_EXTRA_INCHES;
    }

    private void drawTextLayerPreview(Graphics2D g2, LayerData layer, int x, int y, int diameter) {
        String text = getLayerTextPreview(layer);
        int fontSize = Math.max(14, Math.min(40, (int) Math.round(layer.getFontSize() * (diameter / 220.0))));

        Font font = new Font(layer.getFontFamily(), Font.BOLD, fontSize);
        g2.setFont(font);
        g2.setColor(layer.getColor() == null ? new Color(35, 46, 66) : layer.getColor());

        FontMetrics metrics = g2.getFontMetrics(font);
        int textWidth = metrics.stringWidth(text);

        int centerX = x + (diameter / 2) + layer.getTextOffsetX();
        int baseline = y + (diameter / 2) + (metrics.getAscent() - metrics.getDescent()) / 2 + layer.getTextOffsetY();
        g2.drawString(text, centerX - (textWidth / 2), baseline);
    }

    private void drawPhotoLayerPreview(Graphics2D g2, LayerData layer, int x, int y, int diameter) {
        BufferedImage photo = layer.getPhotoImage();
        if (photo == null) {
            g2.setColor(new Color(222, 229, 238));
            g2.fillRect(x, y, diameter, diameter);
            return;
        }

        double coverScale = Math.max((double) diameter / Math.max(1, photo.getWidth()), (double) diameter / Math.max(1, photo.getHeight()));
        double userScale = layer.getPhotoScalePercent() / 100.0;
        double scale = coverScale * userScale;

        int drawWidth = Math.max(1, (int) Math.round(photo.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(photo.getHeight() * scale));
        int drawX = x + (diameter - drawWidth) / 2 + layer.getPhotoOffsetX();
        int drawY = y + (diameter - drawHeight) / 2 + layer.getPhotoOffsetY();

        g2.drawImage(photo, drawX, drawY, drawWidth, drawHeight, null);
    }

    private Image createScaledPreviewImage(BufferedImage image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        double scale = Math.min((double) maxSize / Math.max(1, width), (double) maxSize / Math.max(1, height));
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        return image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }

    private String getLayerTextPreview(LayerData layer) {
        String text = layer.getTextContent();
        if (text == null || text.trim().isEmpty()) {
            return "(Empty text)";
        }
        return text.trim();
    }

    private String getDisplayProjectName(ProjectData project) {
        if (project == null || project.getProjectName() == null || project.getProjectName().trim().isEmpty()) {
            return "Untitled Project";
        }
        return project.getProjectName().trim();
    }

    private String buildProjectCardTitle(ProjectData project) {
        String baseName = getDisplayProjectName(project).replace(' ', '_');
        if (baseName.length() > 16) {
            baseName = baseName.substring(0, 16);
        }
        return baseName + "_" + resolveProjectButtonSizeText(project.getButtonDiameterMm());
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

    private static final class GalleryItemTransferHandler extends TransferHandler {
        private final String itemId;

        private GalleryItemTransferHandler(String itemId) {
            this.itemId = itemId;
        }

        @Override
        protected java.awt.datatransfer.Transferable createTransferable(JComponent component) {
            return new java.awt.datatransfer.StringSelection(PaperPreviewPanel.buildGalleryDragPayload(itemId));
        }

        @Override
        public int getSourceActions(JComponent component) {
            return COPY;
        }
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

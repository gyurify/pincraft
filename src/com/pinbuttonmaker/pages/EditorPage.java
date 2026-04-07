package com.pinbuttonmaker.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.data.LayerData;
import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.db.ProjectStorageService;
import com.pinbuttonmaker.ui.components.ButtonPreviewPanel;
import com.pinbuttonmaker.ui.dialogs.ImageCropDialog;

public class EditorPage extends JPanel {
    private static final String[] FONT_CHOICES = {
        "SansSerif",
        "Serif",
        "Monospaced",
        "Dialog"
    };

    private static final Color APP_BG = new Color(30, 34, 40);
    private static final Color TOP_BAR_BG = new Color(39, 44, 51);
    private static final Color TOP_BAR_BORDER = new Color(58, 65, 75);

    private static final Color BUTTON_BG = new Color(63, 71, 82);
    private static final Color BUTTON_BORDER = new Color(93, 103, 118);
    private static final Color BUTTON_TEXT = new Color(234, 238, 245);

    private static final Color WORKSPACE_BG = new Color(210, 215, 224);
    private static final Color WORKSPACE_BORDER = new Color(179, 186, 198);

    private static final Color SIDEBAR_BG = new Color(39, 44, 52);
    private static final Color SIDEBAR_SECTION_BG = new Color(46, 52, 61);
    private static final Color SIDEBAR_SECTION_BORDER = new Color(70, 78, 91);

    private static final Color TEXT_PRIMARY = new Color(240, 244, 250);
    private static final Color TEXT_MUTED = new Color(188, 197, 211);
    private static final Color FIELD_BG = new Color(57, 65, 76);
    private static final Color FIELD_BORDER = new Color(84, 95, 111);
    private static final Color FIELD_TEXT = TEXT_PRIMARY;
    private static final Color SOFT_BUTTON_BG = new Color(64, 72, 84);
    private static final Color HINT_TEXT = new Color(150, 160, 175);
    private static final Color BUTTON_FACE = new Color(247, 249, 253);
    private static final Color PREVIEW_FRAME = new Color(198, 204, 214);
    private static final Color DEEP_TEXT = new Color(30, 34, 40);

    private static final Color TAB_BG = new Color(43, 50, 60);
    private static final Color TAB_BORDER = new Color(56, 65, 77);
    private static final Color TAB_ACTIVE_BG = new Color(44, 108, 232);
    private static final Color TAB_HIDDEN_BG = new Color(35, 41, 49);
    private static final Color TAB_HIDDEN_TEXT = new Color(122, 132, 149);

    private final AppState appState;
    private ProjectData projectData;

    private final ButtonPreviewPanel previewPanel;
    private JSplitPane centerSplitPane;

    private JButton topUploadButton;

    private JTextField textInputField;
    private JComboBox<String> fontDropdown;
    private JButton colorPickerButton;
    private JLabel activeLayerLabel;
    private JLabel layerInfoLabel;

    private JSlider bendSlider;
    private JLabel bendValueLabel;
    private JPanel bendSliderRow;
    private JButton bendResetButton;
    private JButton resetTextPositionButton;

    private JSlider sizeSlider;
    private JSlider rotateSlider;
    private JSlider stretchSlider;
    private JSlider transparencySlider;

    private JLabel sizeValueLabel;
    private JLabel rotateValueLabel;
    private JLabel stretchValueLabel;
    private JLabel transparencyValueLabel;

    private JPanel sizeSliderRow;
    private JButton sizeResetButton;
    private JButton rotateResetButton;
    private JButton stretchResetButton;
    private JButton transparencyResetButton;
    private JButton uploadPhotoButton;
    private JButton centerHorizontalTextButton;
    private JButton centerVerticalTextButton;
    private JPanel photoCenterButtonsRow;
    private JButton centerHorizontalPhotoButton;
    private JButton centerVerticalPhotoButton;
    private JButton backgroundColorButton;

    private JButton addTextLayerButton;
    private JButton addPhotoLayerButton;
    private JButton removeLayerButton;

    private JPanel layerTabsPanel;
    private final List<JButton> layerTabButtons;
    private int draggingLayerIndex;
    private int layerDragStartX;
    private int layerDragStartY;
    private boolean layerTabDragActive;

    private int activeLayerIndex;
    private boolean controlsUpdating;

    public EditorPage(AppRouter router, AppState appState) {
        this.appState = appState;
        this.previewPanel = new ButtonPreviewPanel();
        this.previewPanel.setLayerSelectionListener(this::handlePreviewLayerSelection);
        installPreviewImageDropSupport();
        this.layerTabButtons = new ArrayList<>();
        this.draggingLayerIndex = -1;
        this.layerDragStartX = 0;
        this.layerDragStartY = 0;
        this.layerTabDragActive = false;
        this.activeLayerIndex = -1;
        this.controlsUpdating = false;

        setLayout(new BorderLayout(0, 0));
        setBackground(APP_BG);
        setPreferredSize(new Dimension(980, 680));
        setMinimumSize(new Dimension(760, 560));

        add(createTopBar(router), BorderLayout.NORTH);
        add(createCenterArea(), BorderLayout.CENTER);
        add(createBottomTabs(), BorderLayout.SOUTH);

        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshProjectBinding();
            }
        });

        refreshProjectBinding();
    }

    private JPanel createTopBar(AppRouter router) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(TOP_BAR_BG);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TOP_BAR_BORDER));
        topBar.setPreferredSize(new Dimension(0, 52));

        JPanel strip = new JPanel(new GridLayout(1, 7, 8, 0));
        strip.setOpaque(false);
        strip.setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton saveButton = createTopButton("Save");
        saveButton.addActionListener(event -> saveCurrentProjectToMemory());
        strip.add(saveButton);

        JButton loadButton = createTopButton("Load");
        loadButton.addActionListener(event -> loadProjectFromMemory());
        strip.add(loadButton);

        topUploadButton = createTopButton("Upload Image");
        topUploadButton.addActionListener(event -> uploadPhotoFromToolbar());
        strip.add(topUploadButton);

        JButton addPhotoTopButton = createTopButton("Add Photo");
        addPhotoTopButton.addActionListener(event -> addPhotoLayer());
        strip.add(addPhotoTopButton);

        JButton addTextTopButton = createTopButton("Add Text");
        addTextTopButton.addActionListener(event -> addTextLayer());
        strip.add(addTextTopButton);

        JButton homeButton = createTopButton("Home");
        homeButton.addActionListener(event -> router.showHome());
        strip.add(homeButton);

        JButton printButton = createTopButton("Print ->");
        printButton.addActionListener(event -> router.showPrint());
        strip.add(printButton);

        topBar.add(strip, BorderLayout.CENTER);
        return topBar;
    }

    private JButton createTopButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(BUTTON_TEXT);
        button.setBackground(BUTTON_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BUTTON_BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createTransformActionButton(String text) {
        JButton button = createTopButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return button;
    }

    private JPanel createCenterArea() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(APP_BG);
        center.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane toolsScrollPane = new JScrollPane(
            createRightSidebar(),
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        toolsScrollPane.setBorder(BorderFactory.createLineBorder(SIDEBAR_SECTION_BORDER));
        toolsScrollPane.setBackground(SIDEBAR_BG);
        toolsScrollPane.getViewport().setBackground(SIDEBAR_BG);
        toolsScrollPane.getVerticalScrollBar().setUnitIncrement(14);

        centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createWorkspacePanel(), toolsScrollPane);
        centerSplitPane.setResizeWeight(0.6);
        centerSplitPane.setDividerSize(6);
        centerSplitPane.setContinuousLayout(true);
        centerSplitPane.setBorder(null);
        centerSplitPane.setOpaque(false);

        center.add(centerSplitPane, BorderLayout.CENTER);
        return center;
    }

    private JPanel createWorkspacePanel() {
        JPanel workspace = new JPanel(new GridBagLayout());
        workspace.setBackground(WORKSPACE_BG);
        workspace.setBorder(BorderFactory.createLineBorder(WORKSPACE_BORDER));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        JPanel previewHolder = new JPanel(new BorderLayout());
        previewHolder.setOpaque(false);
        previewHolder.setBorder(new EmptyBorder(14, 14, 14, 14));
        previewHolder.add(previewPanel, BorderLayout.CENTER);

        workspace.add(previewHolder, constraints);
        return workspace;
    }

    private JPanel createRightSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(8, 8, 8, 8));
        sidebar.setMinimumSize(new Dimension(220, 0));

        sidebar.add(createTextSection());
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createTransformSection());
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createLayerSection());
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JPanel createTextSection() {
        JPanel section = createSectionContainer("Text section");

        activeLayerLabel = createMutedLabel("Active Layer: -");

        JLabel textLabel = createMutedLabel("Text");
        textInputField = createInputField();

        JLabel fontLabel = createMutedLabel("Font");
        fontDropdown = createFontDropdown();

        JLabel colorLabel = createMutedLabel("Color");
        colorPickerButton = createColorPickerButton();

        bendSlider = createTransformSlider(0, 360, 0);
        bendValueLabel = createTransformValueLabel("0 deg");
        bendSliderRow = addSliderControlRow(section, "Bend", bendSlider, bendValueLabel, button -> bendResetButton = button, this::resetBendForActiveLayer);

        section.add(activeLayerLabel);
        section.add(Box.createVerticalStrut(8));
        section.add(textLabel);
        section.add(Box.createVerticalStrut(4));
        section.add(textInputField);
        section.add(Box.createVerticalStrut(8));
        section.add(fontLabel);
        section.add(Box.createVerticalStrut(4));
        section.add(fontDropdown);
        section.add(Box.createVerticalStrut(8));
        section.add(colorLabel);
        section.add(Box.createVerticalStrut(4));
        section.add(colorPickerButton);
        section.add(Box.createVerticalStrut(8));
        section.add(bendSliderRow);
        section.add(Box.createVerticalStrut(8));

        JPanel textCenterRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        textCenterRow.setOpaque(false);
        textCenterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCenterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        resetTextPositionButton = createTransformActionButton("Reset Text Position");
        resetTextPositionButton.addActionListener(event -> resetTextPositionForActiveLayer());

        centerHorizontalTextButton = createAxisCenterButton("\u2194", "Center text horizontally");
        centerHorizontalTextButton.addActionListener(event -> centerActiveLayerHorizontally());

        centerVerticalTextButton = createAxisCenterButton("\u2195", "Center text vertically");
        centerVerticalTextButton.addActionListener(event -> centerActiveLayerVertically());

        textCenterRow.add(resetTextPositionButton);
        textCenterRow.add(centerHorizontalTextButton);
        textCenterRow.add(centerVerticalTextButton);
        section.add(textCenterRow);

        wireTextControls();
        return section;
    }

    private JPanel createTransformSection() {
        JPanel section = createSectionContainer("Transform section");

        sizeSlider = createTransformSlider(50, 200, 100);
        rotateSlider = createTransformSlider(-180, 180, 0);
        stretchSlider = createTransformSlider(50, 200, 100);
        transparencySlider = createTransformSlider(0, 100, 100);

        sizeValueLabel = createTransformValueLabel("100%");
        rotateValueLabel = createTransformValueLabel("0 deg");
        stretchValueLabel = createTransformValueLabel("100%");
        transparencyValueLabel = createTransformValueLabel("100%");

        sizeSliderRow = addSliderControlRow(section, "Size", sizeSlider, sizeValueLabel, button -> sizeResetButton = button, this::resetSizeForActiveLayer);
        section.add(sizeSliderRow);
        section.add(Box.createVerticalStrut(8));
        section.add(addSliderControlRow(section, "Rotate", rotateSlider, rotateValueLabel, button -> rotateResetButton = button, this::resetRotateForActiveLayer));
        section.add(Box.createVerticalStrut(8));
        section.add(addSliderControlRow(section, "Stretch", stretchSlider, stretchValueLabel, button -> stretchResetButton = button, this::resetStretchForActiveLayer));
        section.add(Box.createVerticalStrut(8));
        section.add(addSliderControlRow(section, "Transparency", transparencySlider, transparencyValueLabel, button -> transparencyResetButton = button, this::resetTransparencyForActiveLayer));

        section.add(Box.createVerticalStrut(8));
        JLabel backgroundLabel = createMutedLabel("Button Background");
        backgroundColorButton = createColorPickerButton();
        backgroundColorButton.addActionListener(event -> chooseButtonBackgroundColor());
        section.add(backgroundLabel);
        section.add(Box.createVerticalStrut(4));
        section.add(backgroundColorButton);

        section.add(Box.createVerticalStrut(10));
        uploadPhotoButton = createTransformActionButton("Upload Photo");
        uploadPhotoButton.addActionListener(event -> uploadPhotoForActiveLayer());
        section.add(uploadPhotoButton);

        section.add(Box.createVerticalStrut(6));
        photoCenterButtonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        photoCenterButtonsRow.setOpaque(false);
        photoCenterButtonsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoCenterButtonsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        centerHorizontalPhotoButton = createAxisCenterButton("\u2194", "Center photo horizontally");
        centerHorizontalPhotoButton.addActionListener(event -> centerActiveLayerHorizontally());

        centerVerticalPhotoButton = createAxisCenterButton("\u2195", "Center photo vertically");
        centerVerticalPhotoButton.addActionListener(event -> centerActiveLayerVertically());

        photoCenterButtonsRow.add(centerHorizontalPhotoButton);
        photoCenterButtonsRow.add(centerVerticalPhotoButton);
        section.add(photoCenterButtonsRow);

        wireTransformControls();
        return section;
    }
    private JPanel createLayerSection() {
        JPanel section = createSectionContainer("Layer section");

        addTextLayerButton = createTransformActionButton("Add Text Layer");
        addTextLayerButton.addActionListener(event -> addTextLayer());

        addPhotoLayerButton = createTransformActionButton("Add Photo Layer");
        addPhotoLayerButton.addActionListener(event -> addPhotoLayer());

        removeLayerButton = createTransformActionButton("Delete Layer");
        removeLayerButton.addActionListener(event -> removeActiveLayer());

        section.add(createMutedLabel("Use tabs below to select, drag, or hide layers."));
        section.add(Box.createVerticalStrut(8));
        section.add(addTextLayerButton);
        section.add(Box.createVerticalStrut(6));
        section.add(addPhotoLayerButton);
        section.add(Box.createVerticalStrut(6));
        section.add(removeLayerButton);

        return section;
    }

    private JPanel createSectionContainer(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(SIDEBAR_SECTION_BG);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SIDEBAR_SECTION_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(titleLabel);
        section.add(Box.createVerticalStrut(8));
        return section;
    }

    private JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(FIELD_TEXT);
        field.setBackground(FIELD_BG);
        field.setCaretColor(FIELD_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JComboBox<String> createFontDropdown() {
        JComboBox<String> dropdown = new JComboBox<>(FONT_CHOICES);
        dropdown.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dropdown.setForeground(FIELD_TEXT);
        dropdown.setBackground(FIELD_BG);
        dropdown.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        return dropdown;
    }

    private JButton createColorPickerButton() {
        JButton button = new JButton("Choose Color");
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(FIELD_TEXT);
        button.setBackground(FIELD_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private JSlider createTransformSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setOpaque(false);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setFocusable(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        return slider;
    }

    private JLabel createTransformValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(58, 20));
        return label;
    }

    private JButton createSliderResetButton(String tooltip) {
        JButton button = new JButton("\u21BA");
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(BUTTON_TEXT);
        button.setBackground(SOFT_BUTTON_BG);
        button.setBorder(BorderFactory.createLineBorder(BUTTON_BORDER));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Dimension size = new Dimension(24, 22);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private JButton createAxisCenterButton(String symbol, String tooltip) {
        JButton button = new JButton(symbol);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(BUTTON_TEXT);
        button.setBackground(SOFT_BUTTON_BG);
        button.setBorder(BorderFactory.createLineBorder(BUTTON_BORDER));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Dimension size = new Dimension(30, 30);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private interface ButtonConsumer {
        void accept(JButton button);
    }

    private JPanel addSliderControlRow(
        JPanel parentSection,
        String labelText,
        JSlider slider,
        JLabel valueLabel,
        ButtonConsumer resetConsumer,
        Runnable resetAction
    ) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(createMutedLabel(labelText));
        wrapper.add(Box.createVerticalStrut(2));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(valueLabel);
        right.add(Box.createHorizontalStrut(6));

        JButton resetButton = createSliderResetButton("Reset " + labelText);
        resetButton.addActionListener(event -> resetAction.run());
        right.add(resetButton);
        resetConsumer.accept(resetButton);

        row.add(slider, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        wrapper.add(row);

        return wrapper;
    }

    private JPanel createBottomTabs() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(TOP_BAR_BG);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TOP_BAR_BORDER));
        bottom.setPreferredSize(new Dimension(0, 90));

        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 12, 10, 12));

        layerTabsPanel = new JPanel();
        layerTabsPanel.setLayout(new BoxLayout(layerTabsPanel, BoxLayout.X_AXIS));
        layerTabsPanel.setOpaque(false);
        layerTabsPanel.setBorder(new EmptyBorder(2, 0, 2, 0));

        JScrollPane tabsScrollPane = new JScrollPane(
            layerTabsPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        tabsScrollPane.setBorder(null);
        tabsScrollPane.setOpaque(false);
        tabsScrollPane.getViewport().setOpaque(false);
        tabsScrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        tabsScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateLayerTabButtonSizes();
            }
        });

        layerInfoLabel = new JLabel("Drag tabs to reorder | Double-click to hide | Click x to remove");
        layerInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        layerInfoLabel.setForeground(HINT_TEXT);
        layerInfoLabel.setBorder(new EmptyBorder(0, 4, 0, 2));

        content.add(tabsScrollPane, BorderLayout.CENTER);
        content.add(layerInfoLabel, BorderLayout.EAST);
        bottom.add(content, BorderLayout.CENTER);
        return bottom;
    }

    private LayerTabButton createLayerTabButton(String text, int tabWidth) {
        LayerTabButton button = new LayerTabButton(text);
        button.putClientProperty("layer_role", "tab");
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(TAB_BG);

        Dimension size = new Dimension(tabWidth, 44);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);

        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TAB_BORDER, 1, true),
            BorderFactory.createEmptyBorder(9, 14, 9, 34)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        return button;
    }

    private static final class LayerTabButton extends JButton {
        private static final int REMOVE_SIZE = 14;
        private static final int REMOVE_RIGHT = 8;
        private static final int REMOVE_TOP = 5;

        private final Rectangle removeBounds;

        private LayerTabButton(String text) {
            super(text);
            this.removeBounds = new Rectangle();
        }

        private boolean isRemoveHit(Point point) {
            return point != null && getRemoveBounds().contains(point);
        }

        private Rectangle getRemoveBounds() {
            int x = Math.max(0, getWidth() - REMOVE_RIGHT - REMOVE_SIZE);
            int y = REMOVE_TOP;
            removeBounds.setBounds(x, y, REMOVE_SIZE, REMOVE_SIZE);
            return removeBounds;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Rectangle bounds = getRemoveBounds();
            String glyph = "x";
            Font removeFont = getFont().deriveFont(Font.BOLD, 11f);
            g2.setFont(removeFont);
            g2.setColor(getForeground());

            FontMetrics metrics = g2.getFontMetrics();
            int x = bounds.x + (bounds.width - metrics.stringWidth(glyph)) / 2;
            int y = bounds.y + (bounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(glyph, x, y);

            g2.dispose();
        }
    }

    private void wireTextControls() {
        textInputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                onTextChanged();
            }
        });

        fontDropdown.addActionListener(event -> {
            if (controlsUpdating) {
                return;
            }

            LayerData layer = getActiveLayer();
            if (layer == null || !layer.isTextLayer()) {
                return;
            }

            String font = (String) fontDropdown.getSelectedItem();
            if (font != null) {
                layer.setFontFamily(font);
                previewPanel.repaint();
            }
        });

        colorPickerButton.addActionListener(event -> {
            LayerData layer = getActiveLayer();
            if (layer == null || !layer.isTextLayer()) {
                return;
            }

            Color chosenColor = JColorChooser.showDialog(this, "Choose Text Color", layer.getColor());
            if (chosenColor != null) {
                layer.setColor(chosenColor);
                applyColorButtonStyle(chosenColor);
                previewPanel.repaint();
            }
        });

        bendSlider.addChangeListener(event -> onBendChanged());
    }

    private void wireTransformControls() {
        sizeSlider.addChangeListener(event -> onTransformChanged());
        rotateSlider.addChangeListener(event -> onTransformChanged());
        stretchSlider.addChangeListener(event -> onTransformChanged());
        transparencySlider.addChangeListener(event -> onTransformChanged());
    }

    private void onTextChanged() {
        if (controlsUpdating) {
            return;
        }

        LayerData layer = getActiveLayer();
        if (layer == null || !layer.isTextLayer()) {
            return;
        }

        layer.setTextContent(textInputField.getText());
        previewPanel.repaint();
    }

    private void onBendChanged() {
        if (controlsUpdating) {
            return;
        }

        LayerData layer = getActiveLayer();
        if (layer == null || !layer.isTextLayer()) {
            return;
        }

        layer.setBendPercent(bendSlider.getValue());
        bendValueLabel.setText(layer.getBendPercent() + " deg");
        previewPanel.repaint();
    }

    private void onTransformChanged() {
        if (controlsUpdating) {
            return;
        }

        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        if (layer.isTextLayer()) {
            layer.setSizePercent(sizeSlider.getValue());
        } else if (isPhotoLayer(layer)) {
            layer.setPhotoScalePercent(sizeSlider.getValue());
        }

        layer.setRotationDegrees(rotateSlider.getValue());
        layer.setStretchPercent(stretchSlider.getValue());
        layer.setTransparencyPercent(transparencySlider.getValue());

        updateTransformValueLabels(layer);
        refreshLayerTabStyles();
        previewPanel.repaint();
    }
    private void resetBendForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null || !layer.isTextLayer()) {
            return;
        }

        layer.setBendPercent(0);
        refreshControlsFromActiveLayer();
        previewPanel.repaint();
    }
    private void resetTextPositionForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null || !layer.isTextLayer()) {
            return;
        }

        layer.setTextOffsetX(0);
        layer.setTextOffsetY(0);
        previewPanel.repaint();
    }

    private void centerActiveLayerHorizontally() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        if (layer.isTextLayer()) {
            layer.setTextOffsetX(0);
        } else if (isPhotoLayer(layer)) {
            layer.setPhotoOffsetX(0);
        } else {
            return;
        }

        previewPanel.repaint();
    }

    private void centerActiveLayerVertically() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        if (layer.isTextLayer()) {
            layer.setTextOffsetY(0);
        } else if (isPhotoLayer(layer)) {
            layer.setPhotoOffsetY(0);
        } else {
            return;
        }

        previewPanel.repaint();
    }

    private void resetSizeForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        if (layer.isTextLayer()) {
            layer.setSizePercent(100);
        } else if (isPhotoLayer(layer)) {
            layer.setPhotoScalePercent(100);
        }

        refreshAfterTransformReset();
    }

    private void resetRotateForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        layer.setRotationDegrees(0);
        refreshAfterTransformReset();
    }

    private void resetStretchForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        layer.setStretchPercent(100);
        refreshAfterTransformReset();
    }

    private void resetTransparencyForActiveLayer() {
        LayerData layer = getActiveLayer();
        if (layer == null) {
            return;
        }

        layer.setTransparencyPercent(100);
        refreshAfterTransformReset();
    }

    private void refreshAfterTransformReset() {
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
        previewPanel.repaint();
    }


    private void chooseButtonBackgroundColor() {
        if (projectData == null) {
            return;
        }

        Color initial = projectData.getButtonBackgroundColor() == null ? BUTTON_FACE : projectData.getButtonBackgroundColor();
        Color chosen = JColorChooser.showDialog(this, "Choose Button Background", initial);
        if (chosen == null) {
            return;
        }

        projectData.setButtonBackgroundColor(chosen);
        applyBackgroundColorButtonStyle(chosen);
        previewPanel.repaint();
    }

    private void applyBackgroundColorButtonStyle(Color color) {
        Color appliedColor = color == null ? BUTTON_FACE : color;
        backgroundColorButton.setBackground(appliedColor);

        int luminance = (appliedColor.getRed() * 299 + appliedColor.getGreen() * 587 + appliedColor.getBlue() * 114) / 1000;
        backgroundColorButton.setForeground(luminance < 140 ? Color.WHITE : DEEP_TEXT);

        String hex = String.format("#%02X%02X%02X", appliedColor.getRed(), appliedColor.getGreen(), appliedColor.getBlue());
        backgroundColorButton.setText("Background: " + hex);
    }
    private void uploadPhotoFromToolbar() {
        if (projectData == null) {
            return;
        }

        int photoIndex = findFirstPhotoLayerIndex();
        if (photoIndex < 0) {
            addPhotoLayer();
            photoIndex = findFirstPhotoLayerIndex();
        }

        if (photoIndex >= 0) {
            switchActiveLayer(photoIndex);
            uploadPhotoForActiveLayer();
        }
    }

    private void uploadPhotoForActiveLayer() {
        LayerData photoLayer = ensureActivePhotoLayerForImport();
        if (!isPhotoLayer(photoLayer)) {
            JOptionPane.showMessageDialog(this, "Unable to prepare a Photo layer for upload.", "Upload Image", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = createImageChooserWithPreview();
        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            BufferedImage image = ImageIO.read(selectedFile);
            if (image == null) {
                JOptionPane.showMessageDialog(this, "The selected file is not a supported image.", "Upload Image", JOptionPane.ERROR_MESSAGE);
                return;
            }
            applyImageToPhotoLayer(photoLayer, image);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Unable to read image: " + exception.getMessage(), "Upload Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installPreviewImageDropSupport() {
        previewPanel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (support == null || !support.isDrop() || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }

                Point dropPoint = support.getDropLocation().getDropPoint();
                return dropPoint != null && previewPanel.isPointInsideButtonCircle(dropPoint.x, dropPoint.y);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(data instanceof List<?>)) {
                        return false;
                    }

                    List<File> files = (List<File>) data;
                    BufferedImage image = null;
                    File sourceFile = null;

                    for (File file : files) {
                        if (file == null || !file.isFile()) {
                            continue;
                        }

                        BufferedImage candidate = ImageIO.read(file);
                        if (candidate != null) {
                            image = candidate;
                            sourceFile = file;
                            break;
                        }
                    }

                    if (image == null) {
                        JOptionPane.showMessageDialog(
                            EditorPage.this,
                            "Dropped file is not a supported image.",
                            "Drop Image",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return false;
                    }

                    LayerData targetLayer = ensureActivePhotoLayerForImport();
                    if (targetLayer == null) {
                        return false;
                    }

                    applyImageToPhotoLayer(targetLayer, image);
                    if (sourceFile != null) {
                        previewPanel.repaint();
                    }
                    return true;
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(
                        EditorPage.this,
                        "Unable to import dropped image: " + exception.getMessage(),
                        "Drop Image",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
            }
        });
    }

    private LayerData ensureActivePhotoLayerForImport() {
        LayerData activeLayer = getActiveLayer();
        if (isPhotoLayer(activeLayer)) {
            return activeLayer;
        }

        int photoIndex = findFirstPhotoLayerIndex();
        if (photoIndex < 0) {
            addPhotoLayer();
            photoIndex = findFirstPhotoLayerIndex();
        }

        if (photoIndex < 0) {
            return null;
        }

        switchActiveLayer(photoIndex);
        return getActiveLayer();
    }

    private void applyImageToPhotoLayer(LayerData photoLayer, BufferedImage image) {
        if (photoLayer == null || image == null) {
            return;
        }

        PhotoProcessResult processed = maybeCropImage(image);
        photoLayer.setPhotoImage(processed.image);
        if (processed.cropApplied) {
            photoLayer.setCropData(processed.cropBounds);
        } else {
            photoLayer.clearCropData();
        }
        photoLayer.centerPhoto();
        photoLayer.setVisible(true);
        if (photoLayer.getTransparencyPercent() == 0) {
            photoLayer.setTransparencyPercent(100);
        }

        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
        previewPanel.repaint();
    }

    private JFileChooser createImageChooserWithPreview() {
        JFileChooser chooser = new JFileChooser(resolveInitialImageDirectory());
        chooser.setDialogTitle("Select Photo");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp", "gif", "webp"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setPreferredSize(new Dimension(900, 580));
        chooser.setAccessory(new ImageChooserPreview(chooser));
        return chooser;
    }

    private File resolveInitialImageDirectory() {
        File picturesFolder = new File(System.getProperty("user.home"), "Pictures");
        if (picturesFolder.isDirectory()) {
            return picturesFolder;
        }

        File homeDirectory = FileSystemView.getFileSystemView().getHomeDirectory();
        if (homeDirectory != null && homeDirectory.isDirectory()) {
            return homeDirectory;
        }

        File defaultDirectory = FileSystemView.getFileSystemView().getDefaultDirectory();
        if (defaultDirectory != null && defaultDirectory.isDirectory()) {
            return defaultDirectory;
        }

        return new File(".");
    }

    private PhotoProcessResult maybeCropImage(BufferedImage sourceImage) {
        int response = JOptionPane.showConfirmDialog(
            this,
            "Do you want to crop this image before placing it on the Photo layer?",
            "Crop Photo",
            JOptionPane.YES_NO_OPTION
        );

        if (response != JOptionPane.YES_OPTION) {
            return new PhotoProcessResult(sourceImage, null, false);
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        ImageCropDialog cropDialog = new ImageCropDialog(owner, sourceImage);
        cropDialog.setVisible(true);

        BufferedImage cropped = cropDialog.getCroppedImage();
        if (cropped == null) {
            return new PhotoProcessResult(sourceImage, null, false);
        }

        Rectangle cropBounds = cropDialog.getCropBoundsInSource();
        boolean cropApplied = cropBounds != null;
        return new PhotoProcessResult(cropped, cropBounds, cropApplied);
    }

    private void saveCurrentProjectToMemory() {
        if (projectData == null) {
            return;
        }

        String projectName = promptForProjectNameBeforeSave();
        if (projectName == null) {
            return;
        }
        projectData.setProjectName(projectName);

        ProjectStorageService.StorageResult<Void> result = appState.saveCurrentProject();
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                this,
                result.getMessage(),
                "Save Project",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            result.getMessage(),
            "Save Project",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String promptForProjectNameBeforeSave() {
        String currentName = projectData.getProjectName() == null ? "" : projectData.getProjectName().trim();
        String suggestedName = currentName.isEmpty() || "Untitled Project".equalsIgnoreCase(currentName)
            ? "PinCraft Project " + (appState.getSavedProjects().size() + 1)
            : currentName;

        while (true) {
            String enteredName = (String) JOptionPane.showInputDialog(
                this,
                "Enter a project name before saving:",
                "Save Project",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                suggestedName
            );

            if (enteredName == null) {
                return null;
            }

            enteredName = enteredName.trim();
            if (!enteredName.isEmpty()) {
                return enteredName;
            }

            JOptionPane.showMessageDialog(
                this,
                "Project name cannot be empty.",
                "Save Project",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadProjectFromMemory() {
        if (!appState.isAuthenticated()) {
            JOptionPane.showMessageDialog(
                this,
                "Sign in to load saved projects.",
                "Load Project",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        List<ProjectData> savedProjects = appState.getSavedProjects();
        if (savedProjects.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No saved projects available for this account.",
                "Load Project",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        ProjectData selected = (ProjectData) JOptionPane.showInputDialog(
            this,
            "Select a project to load:",
            "Load Project",
            JOptionPane.PLAIN_MESSAGE,
            null,
            savedProjects.toArray(),
            savedProjects.get(0)
        );

        if (selected == null) {
            return;
        }

        ProjectStorageService.StorageResult<ProjectData> result = appState.loadProjectAsCurrent(selected.getProjectId());
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                this,
                result.getMessage(),
                "Load Project",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        refreshProjectBinding();

        JOptionPane.showMessageDialog(
            this,
            "Loaded: " + result.getData().getProjectName(),
            "Load Project",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static final class ImageChooserPreview extends JPanel implements PropertyChangeListener {
        private static final int PREVIEW_WIDTH = 172;
        private static final int PREVIEW_HEIGHT = 132;

        private final JLabel previewLabel;
        private final JLabel captionLabel;

        private ImageChooserPreview(JFileChooser chooser) {
            setOpaque(false);
            setPreferredSize(new Dimension(190, 170));
            setLayout(new BorderLayout(0, 6));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PREVIEW_FRAME),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
            ));

            previewLabel = new JLabel("No image", SwingConstants.CENTER);
            previewLabel.setOpaque(true);
            previewLabel.setBackground(BUTTON_FACE);
            previewLabel.setForeground(TEXT_MUTED);
            previewLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            previewLabel.setBorder(BorderFactory.createLineBorder(PREVIEW_FRAME));

            captionLabel = new JLabel("Select an image", SwingConstants.CENTER);
            captionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            captionLabel.setForeground(TEXT_MUTED);

            add(previewLabel, BorderLayout.CENTER);
            add(captionLabel, BorderLayout.SOUTH);

            chooser.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (!JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(event.getPropertyName())) {
                return;
            }

            File selected = (File) event.getNewValue();
            updatePreview(selected);
        }

        private void updatePreview(File selected) {
            if (selected == null || !selected.isFile()) {
                previewLabel.setIcon(null);
                previewLabel.setText("No image");
                captionLabel.setText("Select an image");
                return;
            }

            try {
                BufferedImage image = ImageIO.read(selected);
                if (image == null) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Unsupported");
                    captionLabel.setText(selected.getName());
                    return;
                }

                BufferedImage scaled = scaleImageToFit(image, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                previewLabel.setIcon(new ImageIcon(scaled));
                previewLabel.setText("");
                captionLabel.setText(selected.getName() + "  (" + image.getWidth() + "x" + image.getHeight() + ")");
            } catch (IOException exception) {
                previewLabel.setIcon(null);
                previewLabel.setText("Preview error");
                captionLabel.setText(selected.getName());
            }
        }

        private BufferedImage scaleImageToFit(BufferedImage source, int maxWidth, int maxHeight) {
            int srcWidth = Math.max(1, source.getWidth());
            int srcHeight = Math.max(1, source.getHeight());
            double scale = Math.min((double) maxWidth / srcWidth, (double) maxHeight / srcHeight);
            scale = Math.max(0.01, Math.min(1.0, scale));

            int targetWidth = Math.max(1, (int) Math.round(srcWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(srcHeight * scale));

            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            g2.dispose();
            return scaled;
        }
    }

    private static final class PhotoProcessResult {
        private final BufferedImage image;
        private final Rectangle cropBounds;
        private final boolean cropApplied;

        private PhotoProcessResult(BufferedImage image, Rectangle cropBounds, boolean cropApplied) {
            this.image = image;
            this.cropBounds = cropBounds == null ? null : new Rectangle(cropBounds);
            this.cropApplied = cropApplied;
        }
    }

    private void addTextLayer() {
        addLayerOfKind(LayerData.LayerKind.TEXT);
    }

    private void addPhotoLayer() {
        addLayerOfKind(LayerData.LayerKind.PHOTO);
    }

    private void addLayerOfKind(LayerData.LayerKind kind) {
        if (projectData == null) {
            return;
        }

        LayerData layer;
        if (kind == LayerData.LayerKind.TEXT) {
            String name = generateNextTextName();
            layer = new LayerData(name, LayerData.LayerKind.TEXT);
            layer.setTextContent(name);
        } else {
            String name = generateNextPhotoName();
            layer = new LayerData(name, LayerData.LayerKind.PHOTO);
        }

        projectData.addLayer(layer);
        activeLayerIndex = projectData.getLayers().size() - 1;

        previewPanel.setActiveLayerIndex(activeLayerIndex);
        rebuildLayerTabs();
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
        previewPanel.repaint();
    }

    private void removeActiveLayer() {
        removeLayerAt(activeLayerIndex);
    }

    private void removeLayerAt(int layerIndex) {
        if (projectData == null || layerIndex < 0 || layerIndex >= projectData.getLayers().size()) {
            return;
        }

        if (projectData.getLayers().size() == 1) {
            JOptionPane.showMessageDialog(this, "Keep at least one layer in the project.", "Remove Layer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LayerData layer = projectData.getLayers().get(layerIndex);
        int response = JOptionPane.showConfirmDialog(
            this,
            "Remove layer '" + layer.getLayerName() + "'?",
            "Remove Layer",
            JOptionPane.YES_NO_OPTION
        );

        if (response != JOptionPane.YES_OPTION) {
            return;
        }

        projectData.removeLayer(layerIndex);
        if (activeLayerIndex == layerIndex) {
            if (activeLayerIndex >= projectData.getLayers().size()) {
                activeLayerIndex = projectData.getLayers().size() - 1;
            }
        } else if (layerIndex < activeLayerIndex) {
            activeLayerIndex--;
        }

        previewPanel.setActiveLayerIndex(activeLayerIndex);
        rebuildLayerTabs();
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
        previewPanel.repaint();
    }

    private String generateNextTextName() {
        int max = 0;
        for (LayerData layer : projectData.getLayers()) {
            String name = layer.getLayerName();
            if (name != null && name.startsWith("Text ")) {
                try {
                    int number = Integer.parseInt(name.substring(5).trim());
                    max = Math.max(max, number);
                } catch (NumberFormatException ignored) {
                    // Keep scanning.
                }
            }
        }
        return "Text " + (max + 1);
    }

    private String generateNextPhotoName() {
        boolean hasBasePhoto = false;
        int max = 1;

        for (LayerData layer : projectData.getLayers()) {
            String name = layer.getLayerName();
            if ("Photo".equals(name)) {
                hasBasePhoto = true;
                continue;
            }

            if (name != null && name.startsWith("Photo ")) {
                try {
                    int number = Integer.parseInt(name.substring(6).trim());
                    max = Math.max(max, number);
                } catch (NumberFormatException ignored) {
                    // Keep scanning.
                }
            }
        }

        if (!hasBasePhoto) {
            return "Photo";
        }
        return "Photo " + (max + 1);
    }

    private int findFirstPhotoLayerIndex() {
        if (projectData == null) {
            return -1;
        }

        for (int i = 0; i < projectData.getLayers().size(); i++) {
            if (isPhotoLayer(projectData.getLayers().get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void refreshProjectBinding() {
        projectData = appState.getCurrentProject();
        if (projectData == null) {
            projectData = new ProjectData("Untitled Project");
            appState.setCurrentProject(projectData);
        }

        removeLegacyRoundTextLayers();

        if (projectData.getLayers().isEmpty()) {
            projectData.addLayer(new LayerData("Photo", LayerData.LayerKind.PHOTO));
        }

        if (activeLayerIndex < 0 || activeLayerIndex >= projectData.getLayers().size()) {
            activeLayerIndex = projectData.getLayers().size() - 1;
        }

        previewPanel.setProjectData(projectData);
        previewPanel.setActiveLayerIndex(activeLayerIndex);

        rebuildLayerTabs();
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
    }
    private void removeLegacyRoundTextLayers() {
        if (projectData == null) {
            return;
        }

        List<LayerData> layers = projectData.getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            LayerData layer = layers.get(i);
            String name = layer.getLayerName();
            if ("Text Round Top".equals(name) || "Text Round Bottom".equals(name)) {
                projectData.removeLayer(i);
                if (activeLayerIndex > i) {
                    activeLayerIndex--;
                }
            }
        }

        if (activeLayerIndex >= projectData.getLayers().size()) {
            activeLayerIndex = projectData.getLayers().size() - 1;
        }
    }

    private void rebuildLayerTabs() {
        if (layerTabsPanel == null) {
            return;
        }

        layerTabsPanel.removeAll();
        layerTabButtons.clear();

        if (projectData == null) {
            updateLayerTabButtonSizes();
            layerTabsPanel.revalidate();
            layerTabsPanel.repaint();
            return;
        }

        List<LayerData> layers = projectData.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            final int layerIndex = i;
            int tabWidth = calculateLayerTabWidth(layers.size());
            LayerTabButton tab = createLayerTabButton(getLayerTabText(layers.get(i)), tabWidth);
            final boolean[] removePressed = new boolean[] {false};

            MouseAdapter tabDragHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (tab.isRemoveHit(event.getPoint())) {
                        removePressed[0] = true;
                        draggingLayerIndex = -1;
                        layerTabDragActive = false;
                        return;
                    }

                    removePressed[0] = false;
                    draggingLayerIndex = layerIndex;
                    layerDragStartX = event.getXOnScreen();
                    layerDragStartY = event.getYOnScreen();
                    layerTabDragActive = false;
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (removePressed[0]) {
                        return;
                    }

                    int dx = Math.abs(event.getXOnScreen() - layerDragStartX);
                    int dy = Math.abs(event.getYOnScreen() - layerDragStartY);
                    if (dx > 4 || dy > 4) {
                        layerTabDragActive = true;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (removePressed[0]) {
                        if (tab.isRemoveHit(event.getPoint())) {
                            removeLayerAt(layerIndex);
                        }
                        removePressed[0] = false;
                        draggingLayerIndex = -1;
                        layerTabDragActive = false;
                        return;
                    }

                    if (layerTabDragActive) {
                        handleLayerDrop(event, layerIndex);
                    }
                    draggingLayerIndex = -1;
                    layerTabDragActive = false;
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (tab.isRemoveHit(event.getPoint())) {
                        return;
                    }

                    if (event.getClickCount() == 2) {
                        toggleLayerVisibility(layerIndex);
                    } else if (event.getClickCount() == 1) {
                        switchActiveLayer(layerIndex);
                    }
                }
            };

            tab.addMouseListener(tabDragHandler);
            tab.addMouseMotionListener(tabDragHandler);

            if (i > 0) {
                layerTabsPanel.add(Box.createHorizontalStrut(10));
            }

            layerTabButtons.add(tab);
            layerTabsPanel.add(tab);
        }

        updateLayerTabButtonSizes();
        layerTabsPanel.revalidate();
        layerTabsPanel.repaint();
    }
    private int calculateLayerTabWidth(int layerCount) {
        int safeCount = Math.max(1, layerCount);

        int viewportWidth = 0;
        if (layerTabsPanel != null && layerTabsPanel.getParent() != null) {
            viewportWidth = layerTabsPanel.getParent().getWidth();
        }

        if (viewportWidth <= 0) {
            return 220;
        }

        int spacing = (safeCount - 1) * 10;
        int availableWidth = Math.max(0, viewportWidth - spacing - 8);
        int width = availableWidth / safeCount;

        return Math.max(150, Math.min(260, width));
    }

    private void updateLayerTabButtonSizes() {
        if (layerTabButtons == null || layerTabButtons.isEmpty()) {
            return;
        }

        int tabWidth = calculateLayerTabWidth(layerTabButtons.size());
        Dimension size = new Dimension(tabWidth, 44);

        for (JButton tabButton : layerTabButtons) {
            tabButton.setPreferredSize(size);
            tabButton.setMinimumSize(size);
            tabButton.setMaximumSize(size);
        }
    }
    private void handleLayerDrop(MouseEvent event, int fallbackSourceIndex) {
        if (projectData == null || projectData.getLayers().size() < 2) {
            draggingLayerIndex = -1;
            return;
        }

        int fromIndex = draggingLayerIndex >= 0 ? draggingLayerIndex : fallbackSourceIndex;
        draggingLayerIndex = -1;

        Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), layerTabsPanel);
        int toIndex = findDropLayerIndex(point.x);

        if (toIndex < 0 || toIndex == fromIndex || toIndex >= projectData.getLayers().size()) {
            return;
        }

        projectData.moveLayer(fromIndex, toIndex);

        if (activeLayerIndex == fromIndex) {
            activeLayerIndex = toIndex;
        } else if (fromIndex < activeLayerIndex && toIndex >= activeLayerIndex) {
            activeLayerIndex--;
        } else if (fromIndex > activeLayerIndex && toIndex <= activeLayerIndex) {
            activeLayerIndex++;
        }

        previewPanel.setActiveLayerIndex(activeLayerIndex);
        rebuildLayerTabs();
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
        previewPanel.repaint();
    }

    private int findDropLayerIndex(int xInTabsPanel) {
        int buttonIndex = 0;

        for (Component component : layerTabsPanel.getComponents()) {
            if (!(component instanceof JButton)) {
                continue;
            }

            Object role = ((JButton) component).getClientProperty("layer_role");
            if (!"tab".equals(role)) {
                continue;
            }

            int middle = component.getX() + (component.getWidth() / 2);
            if (xInTabsPanel < middle) {
                return buttonIndex;
            }

            buttonIndex++;
        }

        return buttonIndex > 0 ? buttonIndex - 1 : -1;
    }

    private void switchActiveLayer(int layerIndex) {
        if (projectData == null || layerIndex < 0 || layerIndex >= projectData.getLayers().size()) {
            return;
        }

        activeLayerIndex = layerIndex;
        previewPanel.setActiveLayerIndex(activeLayerIndex);
        refreshControlsFromActiveLayer();
        refreshLayerTabStyles();
    }

    private void handlePreviewLayerSelection(int layerIndex) {
        if (projectData == null || layerIndex < 0 || layerIndex >= projectData.getLayers().size()) {
            return;
        }

        switchActiveLayer(layerIndex);
    }

    private void toggleLayerVisibility(int layerIndex) {
        if (projectData == null || layerIndex < 0 || layerIndex >= projectData.getLayers().size()) {
            return;
        }

        LayerData layer = projectData.getLayers().get(layerIndex);
        layer.setVisible(!layer.isVisible());
        refreshLayerTabStyles();
        previewPanel.repaint();
    }

    private void refreshControlsFromActiveLayer() {
        controlsUpdating = true;

        LayerData activeLayer = getActiveLayer();
        boolean hasLayer = activeLayer != null;
        boolean textEditable = hasLayer && activeLayer.isTextLayer();
        boolean photoEditable = isPhotoLayer(activeLayer);

        if (!hasLayer) {
            activeLayerLabel.setText("Active Layer: -");
            textInputField.setText("");
            fontDropdown.setSelectedIndex(0);
            applyColorButtonStyle(FIELD_BG);
            bendSlider.setValue(0);
            bendValueLabel.setText("0 deg");

            sizeSlider.setValue(100);
            rotateSlider.setValue(0);
            stretchSlider.setValue(100);
            transparencySlider.setValue(100);

            updateTransformValueLabels(null);
        } else {
            activeLayerLabel.setText("Active Layer: " + getLayerDisplayLabel(activeLayer));

            if (textEditable) {
                textInputField.setText(activeLayer.getTextContent() == null ? "" : activeLayer.getTextContent());
                fontDropdown.setSelectedItem(activeLayer.getFontFamily());
                applyColorButtonStyle(activeLayer.getColor());
                bendSlider.setValue(activeLayer.getBendPercent());
                bendValueLabel.setText(activeLayer.getBendPercent() + " deg");
            } else {
                textInputField.setText("Photo layer is not text-editable.");
                fontDropdown.setSelectedIndex(0);
                applyColorButtonStyle(FIELD_BG);
                bendSlider.setValue(0);
                bendValueLabel.setText("0 deg");
            }

            sizeSlider.setValue(activeLayer.isTextLayer() ? activeLayer.getSizePercent() : activeLayer.getPhotoScalePercent());
            rotateSlider.setValue(activeLayer.getRotationDegrees());
            stretchSlider.setValue(activeLayer.getStretchPercent());
            transparencySlider.setValue(activeLayer.getTransparencyPercent());

            updateTransformValueLabels(activeLayer);
        }

        textInputField.setEnabled(textEditable);
        fontDropdown.setEnabled(textEditable);
        colorPickerButton.setEnabled(textEditable);

        bendSliderRow.setVisible(textEditable);
        bendSlider.setEnabled(textEditable);
        bendResetButton.setVisible(textEditable);
        bendResetButton.setEnabled(textEditable);
        resetTextPositionButton.setVisible(textEditable);
        resetTextPositionButton.setEnabled(textEditable);

        sizeSliderRow.setVisible(hasLayer);
        sizeSlider.setEnabled(hasLayer);
        sizeResetButton.setVisible(hasLayer);
        sizeResetButton.setEnabled(hasLayer);

        rotateSlider.setEnabled(hasLayer);
        stretchSlider.setEnabled(hasLayer);
        transparencySlider.setEnabled(hasLayer);
        rotateResetButton.setEnabled(hasLayer);
        stretchResetButton.setEnabled(hasLayer);
        transparencyResetButton.setEnabled(hasLayer);

        uploadPhotoButton.setVisible(photoEditable);
        uploadPhotoButton.setEnabled(photoEditable);

        if (photoCenterButtonsRow != null) {
            photoCenterButtonsRow.setVisible(photoEditable);
        }
        if (centerHorizontalPhotoButton != null) {
            centerHorizontalPhotoButton.setEnabled(photoEditable);
        }
        if (centerVerticalPhotoButton != null) {
            centerVerticalPhotoButton.setEnabled(photoEditable);
        }

        if (centerHorizontalTextButton != null) {
            centerHorizontalTextButton.setVisible(textEditable);
            centerHorizontalTextButton.setEnabled(textEditable);
        }
        if (centerVerticalTextButton != null) {
            centerVerticalTextButton.setVisible(textEditable);
            centerVerticalTextButton.setEnabled(textEditable);
        }

        removeLayerButton.setEnabled(hasLayer);
        topUploadButton.setEnabled(projectData != null);
        if (backgroundColorButton != null) {
            Color currentBackground = projectData == null ? BUTTON_FACE : projectData.getButtonBackgroundColor();
            applyBackgroundColorButtonStyle(currentBackground);
            backgroundColorButton.setEnabled(projectData != null);
        }

        if (sizeSliderRow.getParent() != null) {
            sizeSliderRow.getParent().revalidate();
            sizeSliderRow.getParent().repaint();
        }

        controlsUpdating = false;
    }

    private void updateTransformValueLabels(LayerData layer) {
        if (layer == null) {
            sizeValueLabel.setText("100%");
            rotateValueLabel.setText("0 deg");
            stretchValueLabel.setText("100%");
            transparencyValueLabel.setText("100%");
            return;
        }

        int sizeValue = layer.isTextLayer() ? layer.getSizePercent() : layer.getPhotoScalePercent();
        sizeValueLabel.setText(sizeValue + "%");
        rotateValueLabel.setText(layer.getRotationDegrees() + " deg");
        stretchValueLabel.setText(layer.getStretchPercent() + "%");
        transparencyValueLabel.setText(layer.getTransparencyPercent() + "%");
    }

    private void applyColorButtonStyle(Color color) {
        Color appliedColor = color == null ? FIELD_BG : color;
        colorPickerButton.setBackground(appliedColor);

        int luminance = (appliedColor.getRed() * 299 + appliedColor.getGreen() * 587 + appliedColor.getBlue() * 114) / 1000;
        colorPickerButton.setForeground(luminance < 140 ? Color.WHITE : DEEP_TEXT);

        String hex = String.format("#%02X%02X%02X", appliedColor.getRed(), appliedColor.getGreen(), appliedColor.getBlue());
        colorPickerButton.setText("Color: " + hex);
    }

    private void refreshLayerTabStyles() {
        if (projectData == null) {
            return;
        }

        List<LayerData> layers = projectData.getLayers();
        int buttonCount = Math.min(layerTabButtons.size(), layers.size());

        for (int i = 0; i < buttonCount; i++) {
            JButton tabButton = layerTabButtons.get(i);
            LayerData layer = layers.get(i);

            boolean active = (i == activeLayerIndex);
            boolean hidden = isLayerHidden(layer);

            if (active) {
                tabButton.setBackground(TAB_ACTIVE_BG);
                tabButton.setForeground(Color.WHITE);
            } else if (hidden) {
                tabButton.setBackground(TAB_HIDDEN_BG);
                tabButton.setForeground(TAB_HIDDEN_TEXT);
            } else {
                tabButton.setBackground(TAB_BG);
                tabButton.setForeground(TEXT_PRIMARY);
            }

            tabButton.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 12));
            tabButton.setText(getLayerTabText(layer));
        }

        if (layerInfoLabel != null) {
            layerInfoLabel.setText("Drag tabs to reorder | Double-click to hide | Click x to remove");
        }
    }

    private String getLayerTabText(LayerData layer) {
        return getLayerDisplayLabel(layer);
    }

    private String getLayerDisplayLabel(LayerData layer) {
        String displayName = getLayerDisplayName(layer.getLayerName());
        return isLayerHidden(layer) ? displayName + " (hidden)" : displayName;
    }

    private boolean isLayerHidden(LayerData layer) {
        return layer != null && !layer.isPrintable();
    }

    private String getLayerDisplayName(String name) {
        if ("Picture".equals(name)) {
            return "Photo";
        }
        return name;
    }

    private boolean isPhotoLayer(LayerData layer) {
        return layer != null && layer.getLayerKind() == LayerData.LayerKind.PHOTO;
    }

    private LayerData getActiveLayer() {
        if (projectData == null || activeLayerIndex < 0 || activeLayerIndex >= projectData.getLayers().size()) {
            return null;
        }
        return projectData.getLayers().get(activeLayerIndex);
    }
}































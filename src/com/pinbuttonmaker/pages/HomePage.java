package com.pinbuttonmaker.pages;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.db.ProjectStorageService;
import com.pinbuttonmaker.ui.components.ButtonPreviewPanel;
import com.pinbuttonmaker.ui.components.CustomButton;

public class HomePage extends JPanel {
    private static final Color PAGE_BG = new Color(245, 247, 251);
    private static final Color HEADER_BG = Color.WHITE;
    private static final Color HEADER_BORDER = new Color(226, 230, 238);

    private static final Color HERO_BG = new Color(236, 238, 248);
    private static final Color HERO_BORDER = new Color(222, 226, 236);

    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(223, 228, 237);
    private static final Color PRIMARY_BLUE = new Color(46, 103, 231);

    private final AppRouter router;
    private final AppState appState;

    private final JPanel recentGridPanel;

    private final JLabel heroTitleLabel;
    private final JLabel heroSubtitleLabel;
    private final JLabel recentTitleLabel;
    private final JLabel recentSubtitleLabel;

    private final RoundedPanel heroCard;
    private final CustomButton startButton;
    private final JButton profileButton;

    public HomePage(AppRouter router, AppState appState) {
        this.router = router;
        this.appState = appState;
        this.recentGridPanel = new JPanel();
        this.profileButton = createProfileButton();

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        add(createHeaderPanel(), BorderLayout.NORTH);

        heroTitleLabel = new JLabel("Create Your Custom Pin Button");
        heroSubtitleLabel = new JLabel("Design clean, printable layouts with PinCraft's editor.");
        recentTitleLabel = new JLabel("Recent Designs");
        recentSubtitleLabel = new JLabel("Continue working on your saved projects");

        startButton = createStartProjectButton();
        heroCard = createHeroSection();

        add(createBodyPanel(), BorderLayout.CENTER);

        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshHomeState();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsiveLayout();
            }
        });

        SwingUtilities.invokeLater(() -> {
            applyResponsiveLayout();
            refreshHomeState();
        });
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new MatteBorder(0, 0, 1, 0, HEADER_BORDER));
        header.setPreferredSize(new Dimension(0, 60));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftGroup.setOpaque(false);
        leftGroup.setBorder(new EmptyBorder(0, 14, 0, 0));

        JLabel titleLabel = new JLabel("PinCraft");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 23));
        titleLabel.setForeground(new Color(23, 29, 41));

        leftGroup.add(new LogoBadge());
        leftGroup.add(titleLabel);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        rightGroup.setOpaque(false);
        rightGroup.setBorder(new EmptyBorder(0, 0, 0, 8));
        rightGroup.add(profileButton);

        header.add(leftGroup, BorderLayout.WEST);
        header.add(rightGroup, BorderLayout.EAST);
        return header;
    }

    private JButton createProfileButton() {
        JButton button = new JButton("U");
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(new Color(33, 43, 60));
        button.setBackground(new Color(238, 242, 249));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 214, 228)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> router.showLogin());
        return button;
    }

    private JScrollPane createBodyPanel() {
        JPanel content = new JPanel();
        content.setBackground(PAGE_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(14, 18, 18, 18));

        content.add(heroCard);
        content.add(Box.createVerticalStrut(16));
        content.add(createRecentSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PAGE_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private RoundedPanel createHeroSection() {
        RoundedPanel panel = new RoundedPanel(18, HERO_BG, HERO_BORDER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroTitleLabel.setForeground(new Color(24, 31, 46));
        heroTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroSubtitleLabel.setForeground(new Color(92, 101, 121));
        heroSubtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        startButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(heroTitleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(heroSubtitleLabel);
        panel.add(Box.createVerticalStrut(14));
        panel.add(startButton);

        return panel;
    }

    private CustomButton createStartProjectButton() {
        CustomButton button = new CustomButton("Start New Project");
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.addActionListener(event -> startNewProject());
        return button;
    }

    private JPanel createRecentSection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        recentTitleLabel.setForeground(new Color(24, 31, 46));
        recentSubtitleLabel.setForeground(new Color(98, 107, 126));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(recentTitleLabel);
        heading.add(Box.createVerticalStrut(2));
        heading.add(recentSubtitleLabel);

        recentGridPanel.setOpaque(false);
        recentGridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(heading);
        section.add(Box.createVerticalStrut(12));
        section.add(recentGridPanel);
        return section;
    }

    private void startNewProject() {
        String projectName = "PinCraft Project " + (appState.getSavedProjects().size() + 1);
        ProjectData newProject = new ProjectData(projectName);
        appState.setCurrentProject(newProject);
        router.showEditor();
    }

    private void handleRemoveProject(ProjectData project) {
        String projectName = project.getProjectName();
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Remove \"" + projectName + "\" from recent designs?",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        ProjectStorageService.StorageResult<Void> result = appState.removeSavedProject(project.getProjectId());
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                this,
                result.getMessage(),
                "Remove Project",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        refreshRecentDesigns();

        JOptionPane.showMessageDialog(
            this,
            result.getMessage(),
            "Removed",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void refreshHomeState() {
        appState.refreshSavedProjects();
        updateProfileButton();
        refreshRecentDesigns();
    }

    private void updateProfileButton() {
        String user = appState.getCurrentUser() == null ? "U" : appState.getCurrentUser().trim();
        String initial = user.isEmpty() ? "U" : user.substring(0, 1).toUpperCase();
        profileButton.setText(initial);
        profileButton.setToolTipText(appState.isAuthenticated() ? appState.getCurrentUser() : "Guest");
    }

    private void refreshRecentDesigns() {
        recentGridPanel.removeAll();
        recentGridPanel.setLayout(new GridLayout(0, calculateGridColumns(), 10, 10));

        List<ProjectData> savedProjects = appState.getSavedProjects();
        if (savedProjects.isEmpty()) {
            recentGridPanel.add(createEmptyStateCard());
        } else {
            int maxCards = Math.min(savedProjects.size(), 6);
            for (int i = 0; i < maxCards; i++) {
                recentGridPanel.add(createProjectCard(savedProjects.get(i)));
            }
        }

        recentGridPanel.revalidate();
        recentGridPanel.repaint();
    }


    private int calculateGridColumns() {
        int width = recentGridPanel.getWidth();
        if (width <= 0) {
            width = Math.max(360, getWidth() - 48);
        }

        if (width < 560) {
            return 1;
        }
        if (width < 900) {
            return 2;
        }
        return 3;
    }

    private JPanel createEmptyStateCard() {
        RoundedPanel panel = new RoundedPanel(14, new Color(250, 251, 253), new Color(214, 220, 231));
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setPreferredSize(new Dimension(220, 130));

        JLabel label = new JLabel("No recent designs yet");
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setForeground(new Color(108, 117, 136));
        panel.add(label);

        return panel;
    }

    private JPanel createProjectCard(ProjectData project) {
        RoundedPanel card = new RoundedPanel(14, CARD_BG, CARD_BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setPreferredSize(new Dimension(220, 246));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JPanel previewBox = createProjectPreviewBox(project);

        JLabel title = new JLabel(project.getProjectName());
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(new Color(26, 33, 48));

        JLabel details = new JLabel("Layers: " + project.getLayers().size());
        details.setFont(new Font("SansSerif", Font.PLAIN, 12));
        details.setForeground(new Color(96, 106, 126));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);

        JButton openButton = createProjectActionButton("Open", new Color(38, 96, 225));
        openButton.addActionListener(event -> {
            ProjectStorageService.StorageResult<ProjectData> result = appState.loadProjectAsCurrent(project.getProjectId());
            if (!result.isSuccess()) {
                JOptionPane.showMessageDialog(
                    this,
                    result.getMessage(),
                    "Open Project",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            router.showEditor();
        });

        JButton removeButton = createProjectActionButton("Remove", new Color(189, 61, 61));
        removeButton.addActionListener(event -> handleRemoveProject(project));

        actions.add(openButton);
        actions.add(removeButton);

        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 10, 0);
        card.add(previewBox, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 6, 0);
        card.add(title, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 8, 0);
        card.add(details, constraints);

        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 0, 0);
        card.add(actions, constraints);

        return card;
    }

    private JPanel createProjectPreviewBox(ProjectData project) {
        RoundedPanel previewBox = new RoundedPanel(12, new Color(247, 249, 253), new Color(229, 233, 241));
        previewBox.setLayout(new GridBagLayout());
        previewBox.setPreferredSize(new Dimension(180, 132));

        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);

        BufferedImage previewImage = ButtonPreviewPanel.createPreviewImage(project, 120, false);
        Image scaledImage = previewImage.getScaledInstance(108, 108, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaledImage));

        previewBox.add(previewLabel);
        return previewBox;
    }

    private JButton createProjectActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(color);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private void applyResponsiveLayout() {
        int width = Math.max(420, getWidth());

        int heroTitleSize = width < 700 ? 22 : 27;
        int heroSubtitleSize = width < 700 ? 13 : 14;
        int sectionTitleSize = width < 700 ? 20 : 24;
        int sectionSubtitleSize = width < 700 ? 12 : 13;
        int buttonFontSize = width < 700 ? 13 : 14;

        heroTitleLabel.setFont(new Font("SansSerif", Font.BOLD, heroTitleSize));
        heroSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, heroSubtitleSize));
        recentTitleLabel.setFont(new Font("SansSerif", Font.BOLD, sectionTitleSize));
        recentSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, sectionSubtitleSize));
        startButton.setFont(new Font("SansSerif", Font.BOLD, buttonFontSize));

        int heroHeight = width < 700 ? 138 : 156;
        heroCard.setPreferredSize(new Dimension(Math.max(320, width - 52), heroHeight));
        heroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, heroHeight));

        refreshRecentDesigns();
    }

    private static final class LogoBadge extends JPanel {
        private LogoBadge() {
            setOpaque(false);
            setPreferredSize(new Dimension(22, 22));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(44, 104, 233));
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            int inset = 6;
            g2.drawOval(inset, inset, getWidth() - inset * 2 - 1, getHeight() - inset * 2 - 1);
            g2.dispose();
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fillColor;
        private final Color borderColor;

        private RoundedPanel(int arc, Color fillColor, Color borderColor) {
            this.arc = arc;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(graphics);
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }
}

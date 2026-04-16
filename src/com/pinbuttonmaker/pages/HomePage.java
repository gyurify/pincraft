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
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.data.ProjectData;
import com.pinbuttonmaker.db.ProjectStorageService;
import com.pinbuttonmaker.db.UserAuthService;
import com.pinbuttonmaker.ui.UIStyles;
import com.pinbuttonmaker.ui.components.ButtonPreviewPanel;
import com.pinbuttonmaker.ui.components.CustomButton;

public class HomePage extends JPanel {
    private static final Color PAGE_BG = UIStyles.SHELL_BG;
    private static final Color HEADER_BG = UIStyles.TOP_BAR_BG;
    private static final Color HEADER_BORDER = UIStyles.PANEL_BORDER;

    private static final Color HERO_BG = UIStyles.PANEL_BG;
    private static final Color HERO_BORDER = UIStyles.PANEL_BORDER;

    private static final Color CARD_BG = UIStyles.PANEL_BG;
    private static final Color CARD_BORDER = UIStyles.PANEL_BORDER;
    private static final Color TITLE_TEXT = UIStyles.TEXT_PRIMARY;
    private static final Color MUTED_TEXT = UIStyles.TEXT_MUTED;
    private static final Color PRIMARY_BLUE = UIStyles.ACTION_BLUE;
    private static final Color OPEN_BUTTON_BG = UIStyles.ACTION_BLUE;
    private static final Color OPEN_BUTTON_BORDER = UIStyles.ACTION_BLUE_HOVER;
    private static final Color REMOVE_BUTTON_BG = new Color(120, 55, 55);
    private static final Color REMOVE_BUTTON_TEXT = new Color(255, 235, 235);
    private static final Color REMOVE_BUTTON_BORDER = new Color(158, 82, 82);
    private static final Color PREVIEW_SURFACE_BG = UIStyles.CANVAS_BG;
    private static final Color PREVIEW_SURFACE_BORDER = UIStyles.CANVAS_BORDER;

    private static final Color LIGHT_PAGE_BG = new Color(245, 247, 251);
    private static final Color LIGHT_HEADER_BG = Color.WHITE;
    private static final Color LIGHT_HEADER_BORDER = new Color(226, 230, 238);
    private static final Color LIGHT_HERO_BG = new Color(236, 238, 248);
    private static final Color LIGHT_HERO_BORDER = new Color(222, 226, 236);
    private static final Color LIGHT_CARD_BG = Color.WHITE;
    private static final Color LIGHT_CARD_BORDER = new Color(223, 228, 237);
    private static final Color LIGHT_TITLE_TEXT = new Color(24, 31, 46);
    private static final Color LIGHT_MUTED_TEXT = new Color(98, 107, 126);
    private static final Color LIGHT_REMOVE_BUTTON_BG = new Color(246, 232, 232);
    private static final Color LIGHT_REMOVE_BUTTON_TEXT = new Color(170, 53, 53);
    private static final Color LIGHT_REMOVE_BUTTON_BORDER = new Color(232, 205, 205);
    private static final Color LIGHT_PREVIEW_SURFACE_BG = new Color(247, 249, 253);
    private static final Color LIGHT_PREVIEW_SURFACE_BORDER = new Color(229, 233, 241);
    private static final int RECENT_PREVIEW_RENDER_SIZE = 520;

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
        setBackground(pageBg());

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

    private Color pageBg() {
        return appState.isDarkMode() ? PAGE_BG : LIGHT_PAGE_BG;
    }

    private Color headerBg() {
        return appState.isDarkMode() ? HEADER_BG : LIGHT_HEADER_BG;
    }

    private Color headerBorder() {
        return appState.isDarkMode() ? HEADER_BORDER : LIGHT_HEADER_BORDER;
    }

    private Color heroBg() {
        return appState.isDarkMode() ? HERO_BG : LIGHT_HERO_BG;
    }

    private Color heroBorder() {
        return appState.isDarkMode() ? HERO_BORDER : LIGHT_HERO_BORDER;
    }

    private Color cardBg() {
        return appState.isDarkMode() ? CARD_BG : LIGHT_CARD_BG;
    }

    private Color cardBorder() {
        return appState.isDarkMode() ? CARD_BORDER : LIGHT_CARD_BORDER;
    }

    private Color titleText() {
        return appState.isDarkMode() ? TITLE_TEXT : LIGHT_TITLE_TEXT;
    }

    private Color mutedText() {
        return appState.isDarkMode() ? MUTED_TEXT : LIGHT_MUTED_TEXT;
    }

    private Color removeButtonBg() {
        return appState.isDarkMode() ? REMOVE_BUTTON_BG : LIGHT_REMOVE_BUTTON_BG;
    }

    private Color removeButtonText() {
        return appState.isDarkMode() ? REMOVE_BUTTON_TEXT : LIGHT_REMOVE_BUTTON_TEXT;
    }

    private Color removeButtonBorder() {
        return appState.isDarkMode() ? REMOVE_BUTTON_BORDER : LIGHT_REMOVE_BUTTON_BORDER;
    }

    private Color previewSurfaceBg() {
        return appState.isDarkMode() ? PREVIEW_SURFACE_BG : LIGHT_PREVIEW_SURFACE_BG;
    }

    private Color previewSurfaceBorder() {
        return appState.isDarkMode() ? PREVIEW_SURFACE_BORDER : LIGHT_PREVIEW_SURFACE_BORDER;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerBg());
        header.setBorder(new MatteBorder(0, 0, 1, 0, headerBorder()));
        header.setPreferredSize(new Dimension(0, 60));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftGroup.setOpaque(false);
        leftGroup.setBorder(new EmptyBorder(0, 14, 0, 0));

        JLabel titleLabel = new JLabel("PinCraft");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 23));
        titleLabel.setForeground(titleText());

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
        button.setForeground(titleText());
        button.setBackground(appState.isDarkMode() ? UIStyles.ACTION_GREY : new Color(238, 242, 249));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(appState.isDarkMode() ? UIStyles.PANEL_BORDER : new Color(206, 214, 228)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> showProfileMenu());
        return button;
    }

    private void showProfileMenu() {
        if (!appState.isAuthenticated()) {
            router.showLogin();
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(appState.isDarkMode() ? UIStyles.PANEL_BG : Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(appState.isDarkMode() ? UIStyles.PANEL_BORDER : new Color(206, 214, 228)));

        JMenuItem accountItem = createProfileMenuItem(appState.getCurrentUser(), false);
        JMenuItem changePasswordItem = createProfileMenuItem("Change Password", true);
        JMenuItem logoutItem = createProfileMenuItem("Logout", true);

        //open the password change dialog for the signed-in user.
        changePasswordItem.addActionListener(event -> handleChangePassword());

        //clear the current session and go back to login.
        logoutItem.addActionListener(event -> handleLogout());

        menu.add(accountItem);
        menu.addSeparator();
        menu.add(changePasswordItem);
        menu.add(logoutItem);
        menu.show(profileButton, 0, profileButton.getHeight());
    }

    private JMenuItem createProfileMenuItem(String text, boolean enabled) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        item.setFont(new Font("SansSerif", enabled ? Font.BOLD : Font.PLAIN, 13));
        item.setOpaque(true);
        item.setBackground(appState.isDarkMode() ? UIStyles.PANEL_BG : Color.WHITE);
        item.setForeground(enabled ? titleText() : mutedText());
        return item;
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Log out of " + appState.getCurrentUser() + "?",
            "Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        appState.logout();
        router.showLogin();
    }

    private void handleChangePassword() {
        Long currentUserId = appState.getCurrentUserId();
        if (currentUserId == null) {
            router.showLogin();
            return;
        }

        JPasswordField currentPasswordField = new JPasswordField();
        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JCheckBox showPasswordsCheck = new JCheckBox("Show passwords");

        char currentPasswordEcho = currentPasswordField.getEchoChar();
        showPasswordsCheck.setOpaque(false);
        showPasswordsCheck.addActionListener(event -> {
            char echoChar = showPasswordsCheck.isSelected() ? (char) 0 : currentPasswordEcho;
            currentPasswordField.setEchoChar(echoChar);
            newPasswordField.setEchoChar(echoChar);
            confirmPasswordField.setEchoChar(echoChar);
        });

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 0);

        panel.add(new JLabel("Current Password"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        currentPasswordField.setPreferredSize(new Dimension(220, 30));
        panel.add(currentPasswordField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.insets = new Insets(10, 0, 6, 0);
        panel.add(new JLabel("New Password"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        newPasswordField.setPreferredSize(new Dimension(220, 30));
        panel.add(newPasswordField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.insets = new Insets(10, 0, 6, 0);
        panel.add(new JLabel("Confirm New Password"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        confirmPasswordField.setPreferredSize(new Dimension(220, 30));
        panel.add(confirmPasswordField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        constraints.insets = new Insets(10, 0, 0, 0);
        panel.add(showPasswordsCheck, constraints);

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Change Password",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            String currentPassword = new String(currentPasswordField.getPassword()).trim();
            String newPassword = new String(newPasswordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

            if (currentPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter your current password.", "Change Password", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a new password.", "Change Password", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "The new password and confirmation do not match.", "Change Password", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            UserAuthService.AuthResult result = appState.getUserAuthService().changePassword(currentUserId, currentPassword, newPassword);
            if (!result.isSuccess()) {
                JOptionPane.showMessageDialog(this, result.getMessage(), "Change Password", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            JOptionPane.showMessageDialog(this, result.getMessage(), "Change Password", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }

    private JScrollPane createBodyPanel() {
        JPanel content = new JPanel();
        content.setBackground(pageBg());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(14, 18, 18, 18));

        content.add(heroCard);
        content.add(Box.createVerticalStrut(16));
        content.add(createRecentSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(pageBg());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private RoundedPanel createHeroSection() {
        RoundedPanel panel = new RoundedPanel(18, heroBg(), heroBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroTitleLabel.setForeground(titleText());
        heroTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroSubtitleLabel.setForeground(mutedText());
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
        //start new project button.
        CustomButton button = new CustomButton("Start New Project");
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(UIStyles.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyles.ACTION_BLUE_HOVER),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setFocusPainted(false);

        //create a fresh project and move the user to the editor.
        button.addActionListener(event -> startNewProject());
        return button;
    }

    private JPanel createRecentSection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        recentTitleLabel.setForeground(titleText());
        recentSubtitleLabel.setForeground(mutedText());

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(recentTitleLabel);
        heading.add(Box.createVerticalStrut(2));
        heading.add(recentSubtitleLabel);

        recentGridPanel.setOpaque(false);
        recentGridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setOpaque(false);
        gridHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        gridHolder.add(recentGridPanel, BorderLayout.NORTH);

        section.add(heading);
        section.add(Box.createVerticalStrut(12));
        section.add(gridHolder);
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
        RoundedPanel panel = new RoundedPanel(14, cardBg(), cardBorder());
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setPreferredSize(new Dimension(220, 130));

        JLabel label = new JLabel("No recent designs yet");
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setForeground(mutedText());
        panel.add(label);

        return panel;
    }

    private JPanel createProjectCard(ProjectData project) {
        int gridColumns = calculateGridColumns();
        boolean singleColumn = gridColumns == 1;
        boolean compactCard = gridColumns >= 3;
        int cardHeight = singleColumn ? 384 : (compactCard ? 314 : 336);
        int titleFontSize = singleColumn ? 21 : (compactCard ? 17 : 19);
        int detailsFontSize = singleColumn ? 16 : (compactCard ? 13 : 15);
        int actionGap = singleColumn ? 12 : 10;
        int actionTopInset = singleColumn ? 8 : 6;
        int buttonFontSize = singleColumn ? 15 : 14;
        int buttonHeight = singleColumn ? 46 : 42;

        RoundedPanel card = new RoundedPanel(14, cardBg(), cardBorder());
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(singleColumn ? 16 : 14, 16, singleColumn ? 16 : 14, 16));
        card.setPreferredSize(new Dimension(220, cardHeight));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JPanel previewBox = createProjectPreviewBox(project, gridColumns);

        JLabel title = new JLabel(project.getProjectName());
        title.setFont(new Font("SansSerif", Font.BOLD, titleFontSize));
        title.setForeground(titleText());

        JLabel details = new JLabel("Layers: " + project.getLayers().size());
        details.setFont(new Font("SansSerif", Font.PLAIN, detailsFontSize));
        details.setForeground(mutedText());

        JPanel actions = new JPanel(new GridLayout(1, 2, actionGap, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(actionTopInset, 0, 0, 0));

        JButton openButton = createProjectActionButton(
            "Open",
            OPEN_BUTTON_BG,
            Color.WHITE,
            OPEN_BUTTON_BORDER,
            buttonFontSize,
            buttonHeight
        );

        //open button.
        openButton.addActionListener(event -> {
            //load the selected saved project into the editor.
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

        JButton removeButton = createProjectActionButton(
            "Remove",
            removeButtonBg(),
            removeButtonText(),
            removeButtonBorder(),
            buttonFontSize,
            buttonHeight
        );

        //remove button.
        //delete the selected project from the signed-in account.
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

    private JPanel createProjectPreviewBox(ProjectData project, int gridColumns) {
        boolean singleColumn = gridColumns == 1;
        boolean compactCard = gridColumns >= 3;
        int previewHeight = singleColumn ? 244 : (compactCard ? 176 : 206);
        int previewIconSize = singleColumn ? 210 : (compactCard ? 148 : 176);

        RoundedPanel previewBox = new RoundedPanel(12, previewSurfaceBg(), previewSurfaceBorder());
        previewBox.setLayout(new GridBagLayout());
        previewBox.setBorder(new EmptyBorder(singleColumn ? 16 : 14, 14, singleColumn ? 16 : 14, 14));
        previewBox.setPreferredSize(new Dimension(220, previewHeight));

        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);

        BufferedImage previewImage = ButtonPreviewPanel.createPreviewImage(project, RECENT_PREVIEW_RENDER_SIZE, false);
        Image scaledImage = previewImage.getScaledInstance(previewIconSize, previewIconSize, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaledImage));

        previewBox.add(previewLabel);
        return previewBox;
    }

    private JButton createProjectActionButton(String text, Color background, Color foreground, Color borderColor, int fontSize, int buttonHeight) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1, true),
            BorderFactory.createEmptyBorder(11, 12, 11, 12)
        ));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, buttonHeight));
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

            g2.setColor(UIStyles.ACTION_BLUE);
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

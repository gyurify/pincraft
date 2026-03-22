package com.pinbuttonmaker.pages;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.pinbuttonmaker.AppRouter;
import com.pinbuttonmaker.AppState;
import com.pinbuttonmaker.db.UserAuthService;
import com.pinbuttonmaker.ui.UIStyles;
import com.pinbuttonmaker.ui.components.CustomButton;
import com.pinbuttonmaker.util.Utils;

public class LoginPage extends JPanel {
    private static final Color PAGE_BG = new Color(236, 238, 242);
    private static final Color CARD_BG = new Color(246, 247, 250);
    private static final Color CARD_BORDER = new Color(211, 215, 222);

    private static final Color SEGMENT_BG = new Color(226, 228, 233);
    private static final Color TAB_SELECTED_BG = Color.WHITE;
    private static final Color TAB_SELECTED_TEXT = new Color(20, 23, 31);
    private static final Color TAB_IDLE_TEXT = new Color(100, 110, 133);

    private static final Color INPUT_BG = new Color(241, 243, 247);
    private static final Color INPUT_BORDER = new Color(208, 213, 223);
    private static final Color INPUT_ICON = new Color(111, 121, 141);
    private static final Color INPUT_TEXT = new Color(33, 40, 55);
    private static final Color INPUT_PLACEHOLDER = new Color(125, 133, 151);

    private static final Color MUTED_TEXT = new Color(95, 104, 124);
    private static final Color LINK_COLOR = new Color(27, 90, 244);
    private static final Color PRIMARY_BLUE = new Color(47, 103, 232);
    private static final Color PRIMARY_BLUE_HOVER = new Color(39, 93, 213);
    private static final Color STATUS_SUCCESS = new Color(47, 122, 84);
    private static final Color STATUS_ERROR = new Color(181, 62, 62);

    private static final String EMAIL_PLACEHOLDER = "Enter your email";
    private static final String PASSWORD_PLACEHOLDER = "Enter your password";
    private static final String CONFIRM_PASSWORD_PLACEHOLDER = "Confirm your password";
    private static final String FORM_AUTH = "auth";
    private static final String FORM_RESET = "reset";

    private final AppRouter router;
    private final AppState appState;

    private final RoundedPanel cardPanel;
    private final JComponent segmentControlPanel;
    private final CardLayout formCardLayout;
    private final JPanel formContentPanel;
    private final JPanel authContentPanel;
    private final JPanel resetContentPanel;
    private final JButton signInTabButton;
    private final JButton registerTabButton;
    private final JLabel authStatusLabel;

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final RoundedPanel emailRow;
    private final RoundedPanel passwordRow;
    private final RoundedPanel confirmPasswordRow;
    private final JLabel confirmPasswordLabel;
    private final Component confirmPasswordTopSpacer;
    private final Component confirmPasswordFieldSpacer;

    private final JButton passwordToggleButton;
    private final JButton forgotPasswordButton;
    private final JPanel forgotPasswordRow;

    private final JLabel resetStatusLabel;
    private final JTextField resetEmailField;
    private final JTextField resetCodeField;
    private final JPasswordField resetNewPasswordField;
    private final JPasswordField resetConfirmPasswordField;
    private final RoundedPanel resetEmailRow;
    private final RoundedPanel resetCodeRow;
    private final RoundedPanel resetNewPasswordRow;
    private final RoundedPanel resetConfirmPasswordRow;
    private final JLabel resetCodeLabel;
    private final JLabel resetNewPasswordLabel;
    private final JLabel resetConfirmPasswordLabel;
    private final Component resetCodeTopSpacer;
    private final Component resetCodeFieldSpacer;
    private final Component resetNewPasswordTopSpacer;
    private final Component resetNewPasswordFieldSpacer;
    private final Component resetConfirmPasswordTopSpacer;
    private final Component resetConfirmPasswordFieldSpacer;
    private final RoundedPrimaryButton resetActionButton;
    private final JButton resetBackButton;

    private final RoundedPrimaryButton submitButton;
    private final JButton footerActionButton;
    private final JLabel footerPrefixLabel;
    private final JPanel footerPanel;

    private boolean registerMode;
    private boolean resetModeActive;
    private boolean resetCodeRequested;
    private boolean passwordVisible;
    private boolean emailPlaceholderActive;
    private boolean passwordPlaceholderActive;
    private boolean confirmPasswordPlaceholderActive;
    private final char defaultPasswordEcho;

    public LoginPage(AppRouter router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new GridBagLayout());
        setBackground(PAGE_BG);
        UIStyles.applyPagePadding(this);

        JPanel content = createPageContent();
        add(content, createCenterConstraints());

        cardPanel = createCardPanel();
        content.add(cardPanel);
        formCardLayout = new CardLayout();
        formContentPanel = new JPanel(formCardLayout);
        formContentPanel.setOpaque(false);

        signInTabButton = createSegmentTabButton("Sign In", false);
        registerTabButton = createSegmentTabButton("Register", true);
        segmentControlPanel = createSegmentControl();
        authStatusLabel = createStatusLabel();

        emailField = createEmailField();
        passwordField = createPasswordField();
        confirmPasswordField = createPasswordField();
        defaultPasswordEcho = passwordField.getEchoChar();

        passwordToggleButton = createPasswordToggleButton();
        forgotPasswordButton = createLinkButton("Forgot password?");
        forgotPasswordRow = createForgotPasswordRow();

        resetStatusLabel = createStatusLabel();
        resetEmailField = createEmailField();
        resetCodeField = createEmailField();
        resetNewPasswordField = createPasswordField();
        resetConfirmPasswordField = createPasswordField();
        resetEmailRow = createInputRow("@", resetEmailField, null);
        resetCodeRow = createInputRow("#", resetCodeField, null);
        resetNewPasswordRow = createInputRow("#", resetNewPasswordField, createDialogPasswordToggleButton(resetNewPasswordField));
        resetConfirmPasswordRow = createInputRow("#", resetConfirmPasswordField, createDialogPasswordToggleButton(resetConfirmPasswordField));
        resetCodeLabel = createFieldLabel("Reset Code");
        resetNewPasswordLabel = createFieldLabel("New Password");
        resetConfirmPasswordLabel = createFieldLabel("Confirm New Password");
        resetCodeTopSpacer = Box.createVerticalStrut(10);
        resetCodeFieldSpacer = Box.createVerticalStrut(6);
        resetNewPasswordTopSpacer = Box.createVerticalStrut(10);
        resetNewPasswordFieldSpacer = Box.createVerticalStrut(6);
        resetConfirmPasswordTopSpacer = Box.createVerticalStrut(10);
        resetConfirmPasswordFieldSpacer = Box.createVerticalStrut(6);
        resetActionButton = createResetActionButton();
        resetBackButton = createResetBackButton();

        submitButton = createSubmitButton();

        footerPrefixLabel = new JLabel();
        footerActionButton = createFooterActionButton();
        footerPanel = new JPanel();

        emailRow = createInputRow("@", emailField, null);
        passwordRow = createInputRow("#", passwordField, passwordToggleButton);
        confirmPasswordLabel = createFieldLabel("Confirm Password");
        confirmPasswordRow = createInputRow("#", confirmPasswordField, null);
        confirmPasswordTopSpacer = Box.createVerticalStrut(10);
        confirmPasswordFieldSpacer = Box.createVerticalStrut(6);

        authContentPanel = createAuthContentPanel();
        resetContentPanel = createResetContentPanel();
        layoutCard();
        layoutFooter(content);

        installEmailPlaceholder();
        installPasswordPlaceholder();
        installConfirmPasswordPlaceholder();
        resetModeActive = false;
        resetCodeRequested = false;
        setRegisterMode(false);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsiveLayout();
            }
        });

        SwingUtilities.invokeLater(this::applyResponsiveLayout);
    }

    private GridBagConstraints createCenterConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        return constraints;
    }

    private JPanel createPageContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 16, 16, 16));
        content.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));

        JLabel title = new JLabel("Pin Button Maker");
        title.setFont(new Font("SansSerif", Font.BOLD, 48));
        title.setForeground(new Color(36, 43, 60));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Sign in to save designs per account and reset passwords by email.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(subtitle);

        content.add(Box.createVerticalStrut(16));
        return content;
    }

    private RoundedPanel createCardPanel() {
        RoundedPanel panel = new RoundedPanel(18, CARD_BG, CARD_BORDER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 20, 16, 20));
        panel.setPreferredSize(new Dimension(590, 420));
        panel.setMaximumSize(new Dimension(650, 650));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private JButton createSegmentTabButton(String text, boolean registerTab) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> setRegisterMode(registerTab));
        return button;
    }

    private JTextField createEmailField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        styleTextField(field);
        return field;
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        field.setOpaque(false);
        field.setForeground(INPUT_TEXT);
    }

    private RoundedPanel createInputRow(String iconText, JComponent field, JButton trailing) {
        RoundedPanel row = new RoundedPanel(11, INPUT_BG, INPUT_BORDER);
        row.setLayout(new BorderLayout(8, 0));
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(iconText, SwingConstants.CENTER);
        icon.setForeground(INPUT_ICON);
        icon.setPreferredSize(new Dimension(22, 22));

        row.add(icon, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);

        if (trailing != null) {
            row.add(trailing, BorderLayout.EAST);
        }

        return row;
    }

    private JButton createPasswordToggleButton() {
        JButton button = createLinkButton("Show");
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.addActionListener(event -> togglePasswordVisibility());
        return button;
    }

    private JButton createDialogPasswordToggleButton(JPasswordField field) {
        char defaultEchoChar = field.getEchoChar();
        JButton button = createLinkButton("Show");
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.addActionListener(event -> {
            boolean visible = field.getEchoChar() == (char) 0;
            field.setEchoChar(visible ? defaultEchoChar : (char) 0);
            button.setText(visible ? "Show" : "Hide");
        });
        return button;
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private RoundedPrimaryButton createSubmitButton() {
        RoundedPrimaryButton button = new RoundedPrimaryButton("Sign In  ->");
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_BLUE);
        button.setHoverBackground(PRIMARY_BLUE_HOVER);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> handleEmailAuth());
        return button;
    }

    private JButton createFooterActionButton() {
        JButton button = createLinkButton("Sign up");
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.addActionListener(event -> setRegisterMode(!registerMode));
        return button;
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(STATUS_ERROR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setVisible(false);
        return label;
    }

    private RoundedPrimaryButton createResetActionButton() {
        RoundedPrimaryButton button = new RoundedPrimaryButton("Send Reset Code  ->");
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_BLUE);
        button.setHoverBackground(PRIMARY_BLUE_HOVER);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> handleResetAction());
        return button;
    }

    private JButton createResetBackButton() {
        JButton button = createLinkButton("Back to sign in");
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> showAuthFlow(false));
        return button;
    }

    private JPanel createAuthContentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(authStatusLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(createFieldLabel("Email"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(emailRow);
        panel.add(Box.createVerticalStrut(8));
        panel.add(createFieldLabel("Password"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(passwordRow);
        panel.add(confirmPasswordTopSpacer);
        panel.add(confirmPasswordLabel);
        panel.add(confirmPasswordFieldSpacer);
        panel.add(confirmPasswordRow);
        panel.add(Box.createVerticalStrut(4));
        panel.add(forgotPasswordRow);
        panel.add(Box.createVerticalStrut(8));
        panel.add(submitButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createResetContentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Reset Your Password");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(24, 31, 46));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel help = new JLabel("Use the same form to request a reset code and set a new password.");
        help.setFont(new Font("SansSerif", Font.PLAIN, 13));
        help.setForeground(MUTED_TEXT);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(help);
        panel.add(Box.createVerticalStrut(10));
        panel.add(resetStatusLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(createFieldLabel("Email"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(resetEmailRow);
        panel.add(resetCodeTopSpacer);
        panel.add(resetCodeLabel);
        panel.add(resetCodeFieldSpacer);
        panel.add(resetCodeRow);
        panel.add(resetNewPasswordTopSpacer);
        panel.add(resetNewPasswordLabel);
        panel.add(resetNewPasswordFieldSpacer);
        panel.add(resetNewPasswordRow);
        panel.add(resetConfirmPasswordTopSpacer);
        panel.add(resetConfirmPasswordLabel);
        panel.add(resetConfirmPasswordFieldSpacer);
        panel.add(resetConfirmPasswordRow);
        panel.add(Box.createVerticalStrut(12));
        panel.add(resetActionButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(resetBackButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void layoutCard() {
        formContentPanel.add(authContentPanel, FORM_AUTH);
        formContentPanel.add(resetContentPanel, FORM_RESET);
        formContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardPanel.add(segmentControlPanel);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(formContentPanel);

        forgotPasswordButton.addActionListener(event -> handleForgotPassword());
    }

    private void layoutFooter(JPanel contentPanel) {
        footerPanel.setOpaque(false);
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        footerPrefixLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        footerPrefixLabel.setForeground(MUTED_TEXT);

        footerPanel.add(footerPrefixLabel);
        footerPanel.add(Box.createHorizontalStrut(4));
        footerPanel.add(footerActionButton);

        contentPanel.add(Box.createVerticalStrut(14));
        contentPanel.add(footerPanel);
    }

    private JComponent createSegmentControl() {
        RoundedPanel segmentPanel = new RoundedPanel(12, SEGMENT_BG, SEGMENT_BG);
        segmentPanel.setLayout(new GridLayout(1, 2, 6, 0));
        segmentPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        segmentPanel.add(signInTabButton);
        segmentPanel.add(registerTabButton);
        segmentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return segmentPanel;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(30, 35, 46));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createForgotPasswordRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(forgotPasswordButton, BorderLayout.EAST);
        return row;
    }

    private void setRegisterMode(boolean registerMode) {
        this.registerMode = registerMode;
        if (resetModeActive) {
            return;
        }

        applyTabStyle(signInTabButton, !registerMode);
        applyTabStyle(registerTabButton, registerMode);

        submitButton.setText(registerMode ? "Register  ->" : "Sign In  ->");
        forgotPasswordButton.setVisible(!registerMode);
        forgotPasswordButton.setEnabled(!registerMode);
        confirmPasswordTopSpacer.setVisible(registerMode);
        confirmPasswordLabel.setVisible(registerMode);
        confirmPasswordFieldSpacer.setVisible(registerMode);
        confirmPasswordRow.setVisible(registerMode);

        if (registerMode) {
            footerPrefixLabel.setText("Already have an account?");
            footerActionButton.setText("Sign in");
        } else {
            footerPrefixLabel.setText("Don't have an account?");
            footerActionButton.setText("Sign up");
        }

        applyResponsiveLayout();
    }

    private void showAuthFlow(boolean showResetSuccessMessage) {
        resetModeActive = false;
        formCardLayout.show(formContentPanel, FORM_AUTH);
        segmentControlPanel.setVisible(true);
        footerPanel.setVisible(true);
        setRegisterMode(false);
        clearResetStatusMessage();

        if (showResetSuccessMessage) {
            showAuthStatusMessage("Password updated. Sign in with your new password.", STATUS_SUCCESS);
        } else {
            clearAuthStatusMessage();
        }
    }

    private void showResetFlow(String prefilledEmail) {
        resetModeActive = true;
        resetCodeRequested = false;
        formCardLayout.show(formContentPanel, FORM_RESET);
        segmentControlPanel.setVisible(false);
        footerPanel.setVisible(false);

        resetEmailField.setText(prefilledEmail == null ? "" : prefilledEmail.trim());
        resetCodeField.setText("");
        resetNewPasswordField.setText("");
        resetConfirmPasswordField.setText("");
        resetNewPasswordField.setEchoChar(defaultPasswordEcho);
        resetConfirmPasswordField.setEchoChar(defaultPasswordEcho);
        resetActionButton.setText("Send Reset Code  ->");
        updateResetStepVisibility();
        clearResetStatusMessage();
        clearAuthStatusMessage();
        applyResponsiveLayout();
    }

    private void updateResetStepVisibility() {
        resetEmailField.setEnabled(!resetCodeRequested);
        resetCodeTopSpacer.setVisible(resetCodeRequested);
        resetCodeLabel.setVisible(resetCodeRequested);
        resetCodeFieldSpacer.setVisible(resetCodeRequested);
        resetCodeRow.setVisible(resetCodeRequested);
        resetNewPasswordTopSpacer.setVisible(resetCodeRequested);
        resetNewPasswordLabel.setVisible(resetCodeRequested);
        resetNewPasswordFieldSpacer.setVisible(resetCodeRequested);
        resetNewPasswordRow.setVisible(resetCodeRequested);
        resetConfirmPasswordTopSpacer.setVisible(resetCodeRequested);
        resetConfirmPasswordLabel.setVisible(resetCodeRequested);
        resetConfirmPasswordFieldSpacer.setVisible(resetCodeRequested);
        resetConfirmPasswordRow.setVisible(resetCodeRequested);
        resetActionButton.setText(resetCodeRequested ? "Change Password  ->" : "Send Reset Code  ->");
    }

    private void showAuthStatusMessage(String message, Color color) {
        authStatusLabel.setText(message);
        authStatusLabel.setForeground(color);
        authStatusLabel.setVisible(message != null && !message.trim().isEmpty());
    }

    private void clearAuthStatusMessage() {
        authStatusLabel.setText(" ");
        authStatusLabel.setVisible(false);
    }

    private void showResetStatusMessage(String message, Color color) {
        resetStatusLabel.setText(message);
        resetStatusLabel.setForeground(color);
        resetStatusLabel.setVisible(message != null && !message.trim().isEmpty());
    }

    private void clearResetStatusMessage() {
        resetStatusLabel.setText(" ");
        resetStatusLabel.setVisible(false);
    }

    private void applyTabStyle(JButton button, boolean selected) {
        button.setForeground(selected ? TAB_SELECTED_TEXT : TAB_IDLE_TEXT);
        button.setBackground(selected ? TAB_SELECTED_BG : SEGMENT_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(selected ? new Color(213, 218, 228) : SEGMENT_BG),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void applyResponsiveLayout() {
        int availableWidth = Math.max(400, getWidth());
        int sidePadding = availableWidth < 760 ? 44 : 220;
        int cardWidth = Math.max(340, Math.min(590, availableWidth - sidePadding));
        Dimension segmentSize = new Dimension(cardWidth - 44, cardWidth < 390 ? 44 : 52);
        segmentControlPanel.setPreferredSize(segmentSize);
        segmentControlPanel.setMaximumSize(segmentSize);

        int fieldHeight = cardWidth < 390 ? 38 : 44;
        Dimension rowSize = new Dimension(cardWidth - 44, fieldHeight);
        emailRow.setPreferredSize(rowSize);
        emailRow.setMaximumSize(rowSize);
        passwordRow.setPreferredSize(rowSize);
        passwordRow.setMaximumSize(rowSize);
        confirmPasswordRow.setPreferredSize(rowSize);
        confirmPasswordRow.setMaximumSize(rowSize);
        resetEmailRow.setPreferredSize(rowSize);
        resetEmailRow.setMaximumSize(rowSize);
        resetCodeRow.setPreferredSize(rowSize);
        resetCodeRow.setMaximumSize(rowSize);
        resetNewPasswordRow.setPreferredSize(rowSize);
        resetNewPasswordRow.setMaximumSize(rowSize);
        resetConfirmPasswordRow.setPreferredSize(rowSize);
        resetConfirmPasswordRow.setMaximumSize(rowSize);

        Dimension forgotSize = new Dimension(cardWidth - 44, cardWidth < 390 ? 20 : 22);
        forgotPasswordRow.setPreferredSize(forgotSize);
        forgotPasswordRow.setMaximumSize(forgotSize);
        forgotPasswordRow.setMinimumSize(forgotSize);

        int submitHeight = cardWidth < 390 ? 40 : 44;
        Dimension submitSize = new Dimension(cardWidth - 44, submitHeight);
        submitButton.setPreferredSize(submitSize);
        submitButton.setMaximumSize(submitSize);
        resetActionButton.setPreferredSize(submitSize);
        resetActionButton.setMaximumSize(submitSize);

        int tabFontSize = cardWidth < 390 ? 13 : 15;
        int inputFontSize = cardWidth < 390 ? 12 : 14;
        int labelFontSize = cardWidth < 390 ? 13 : 15;
        int buttonFontSize = cardWidth < 390 ? 13 : 15;

        signInTabButton.setFont(new Font("SansSerif", Font.BOLD, tabFontSize));
        registerTabButton.setFont(new Font("SansSerif", Font.BOLD, tabFontSize));
        emailField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        confirmPasswordField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        resetEmailField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        resetCodeField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        resetNewPasswordField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        resetConfirmPasswordField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        forgotPasswordButton.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize - 1));
        submitButton.setFont(new Font("SansSerif", Font.BOLD, buttonFontSize));
        resetActionButton.setFont(new Font("SansSerif", Font.BOLD, buttonFontSize));
        resetStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize - 1));
        authStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize - 1));
        resetBackButton.setFont(new Font("SansSerif", Font.BOLD, inputFontSize - 1));
        footerPrefixLabel.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        footerActionButton.setFont(new Font("SansSerif", Font.BOLD, inputFontSize));

        JPanel activeFormPanel = resetModeActive ? resetContentPanel : authContentPanel;
        Dimension activeFormSize = activeFormPanel.getPreferredSize();
        int formWidth = cardWidth - 44;
        int formHeight = activeFormSize.height;
        formContentPanel.setPreferredSize(new Dimension(formWidth, formHeight));
        formContentPanel.setMaximumSize(new Dimension(formWidth, formHeight));

        int availableHeight = Math.max(540, getHeight());
        int segmentHeight = segmentControlPanel.isVisible() ? segmentSize.height + 10 : 0;
        Insets cardInsets = cardPanel.getInsets();
        int preferredCardHeight = cardInsets.top + cardInsets.bottom + segmentHeight + formHeight;
        int minCardHeight = resetModeActive
            ? (resetCodeRequested ? 420 : 280)
            : (registerMode ? 390 : 300);
        int maxCardHeight = Math.max(minCardHeight, availableHeight - 140);
        int cardHeight = Math.max(minCardHeight, Math.min(preferredCardHeight, maxCardHeight));
        cardPanel.setPreferredSize(new Dimension(cardWidth, cardHeight));

        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private void installEmailPlaceholder() {
        setEmailPlaceholder();

        emailField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (emailPlaceholderActive) {
                    emailField.setText("");
                    emailField.setForeground(INPUT_TEXT);
                    emailPlaceholderActive = false;
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (emailField.getText().trim().isEmpty()) {
                    setEmailPlaceholder();
                }
            }
        });
    }

    private void setEmailPlaceholder() {
        emailField.setText(EMAIL_PLACEHOLDER);
        emailField.setForeground(INPUT_PLACEHOLDER);
        emailPlaceholderActive = true;
    }

    private void installPasswordPlaceholder() {
        setPasswordPlaceholder();

        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (passwordPlaceholderActive) {
                    passwordField.setText("");
                    passwordField.setForeground(INPUT_TEXT);
                    passwordPlaceholderActive = false;
                    applyPasswordEchoState(passwordField, false);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (new String(passwordField.getPassword()).trim().isEmpty()) {
                    setPasswordPlaceholder();
                }
            }
        });

        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }
        });
    }

    private void setPasswordPlaceholder() {
        passwordField.setText(PASSWORD_PLACEHOLDER);
        passwordField.setForeground(INPUT_PLACEHOLDER);
        passwordPlaceholderActive = true;
        passwordVisible = false;
        applyPasswordEchoState(passwordField, true);
        updatePasswordToggleText();
    }

    private void installConfirmPasswordPlaceholder() {
        setConfirmPasswordPlaceholder();

        confirmPasswordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (confirmPasswordPlaceholderActive) {
                    confirmPasswordField.setText("");
                    confirmPasswordField.setForeground(INPUT_TEXT);
                    confirmPasswordPlaceholderActive = false;
                    applyPasswordEchoState(confirmPasswordField, false);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (new String(confirmPasswordField.getPassword()).trim().isEmpty()) {
                    setConfirmPasswordPlaceholder();
                }
            }
        });

        confirmPasswordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePasswordToggleText();
            }
        });
    }

    private void setConfirmPasswordPlaceholder() {
        confirmPasswordField.setText(CONFIRM_PASSWORD_PLACEHOLDER);
        confirmPasswordField.setForeground(INPUT_PLACEHOLDER);
        confirmPasswordPlaceholderActive = true;
        applyPasswordEchoState(confirmPasswordField, true);
        updatePasswordToggleText();
    }

    private void togglePasswordVisibility() {
        if (passwordPlaceholderActive && (!registerMode || confirmPasswordPlaceholderActive)) {
            return;
        }

        passwordVisible = !passwordVisible;
        applyPasswordEchoState(passwordField, passwordPlaceholderActive);
        applyPasswordEchoState(confirmPasswordField, confirmPasswordPlaceholderActive);
        updatePasswordToggleText();
    }

    private void applyPasswordEchoState(JPasswordField field, boolean placeholderActive) {
        field.setEchoChar(placeholderActive || passwordVisible ? (char) 0 : defaultPasswordEcho);
    }

    private void updatePasswordToggleText() {
        boolean mainPasswordFilled = !passwordPlaceholderActive && !new String(passwordField.getPassword()).trim().isEmpty();
        boolean confirmPasswordFilled = registerMode
            && !confirmPasswordPlaceholderActive
            && !new String(confirmPasswordField.getPassword()).trim().isEmpty();

        if (!mainPasswordFilled && !confirmPasswordFilled) {
            passwordToggleButton.setText("Show");
            return;
        }

        passwordToggleButton.setText(passwordVisible ? "Hide" : "Show");
    }

    private String readEmailValue() {
        if (emailPlaceholderActive) {
            return "";
        }
        return Utils.normalizeOrDefault(emailField.getText(), "");
    }

    private String readPasswordValue() {
        if (passwordPlaceholderActive) {
            return "";
        }
        return new String(passwordField.getPassword()).trim();
    }

    private String readConfirmPasswordValue() {
        if (confirmPasswordPlaceholderActive) {
            return "";
        }
        return new String(confirmPasswordField.getPassword()).trim();
    }

    private void handleForgotPassword() {
        showResetFlow(readEmailValue());
    }

    private void handleResetAction() {
        UserAuthService authService = appState.getUserAuthService();

        if (!resetCodeRequested) {
            UserAuthService.AuthResult requestResult = authService.requestPasswordReset(resetEmailField.getText());
            if (!requestResult.isSuccess()) {
                showResetStatusMessage(requestResult.getMessage(), STATUS_ERROR);
                return;
            }

            resetCodeRequested = true;
            resetEmailField.setText(requestResult.getUserEmail());
            updateResetStepVisibility();
            showResetStatusMessage("Reset code sent. Enter it below and choose a new password.", STATUS_SUCCESS);
            applyResponsiveLayout();
            resetCodeField.requestFocusInWindow();
            return;
        }

        String email = Utils.normalizeOrDefault(resetEmailField.getText(), "");
        String resetCode = Utils.normalizeOrDefault(resetCodeField.getText(), "");
        String newPassword = new String(resetNewPasswordField.getPassword()).trim();
        String confirmedPassword = new String(resetConfirmPasswordField.getPassword()).trim();

        if (resetCode.isEmpty()) {
            showResetStatusMessage("Enter the reset code from your email.", STATUS_ERROR);
            return;
        }
        if (newPassword.isEmpty()) {
            showResetStatusMessage("Enter a new password.", STATUS_ERROR);
            return;
        }
        if (!newPassword.equals(confirmedPassword)) {
            showResetStatusMessage("The new password and confirmation do not match.", STATUS_ERROR);
            return;
        }

        UserAuthService.AuthResult resetResult = authService.resetPassword(email, resetCode, newPassword);
        if (!resetResult.isSuccess()) {
            showResetStatusMessage(resetResult.getMessage(), STATUS_ERROR);
            return;
        }

        setEmailFieldValue(email);
        setPasswordPlaceholder();
        showAuthFlow(true);
    }

    private void setEmailFieldValue(String value) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            setEmailPlaceholder();
            return;
        }

        emailField.setText(normalizedValue);
        emailField.setForeground(INPUT_TEXT);
        emailPlaceholderActive = false;
    }

    private void handleEmailAuth() {
        String email = readEmailValue();
        String password = readPasswordValue();
        UserAuthService authService = appState.getUserAuthService();

        if (registerMode) {
            String confirmedPassword = readConfirmPasswordValue();
            if (confirmedPassword.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Confirm your password before registering.",
                    "Register",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if (!password.equals(confirmedPassword)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Password and confirm password do not match.",
                    "Register",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        UserAuthService.AuthResult result = registerMode
            ? authService.register(email, password)
            : authService.login(email, password);

        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                this,
                result.getMessage(),
                registerMode ? "Register" : "Login",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        appState.setAuthenticatedUser(result.getUserId(), result.getUserEmail());
        Utils.showInfo(this, registerMode ? "Registered: " + result.getUserEmail() : "Signed in: " + result.getUserEmail());
        router.showHome();
    }

    private static final class CircleBadge extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(56, 56);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 1;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setColor(new Color(47, 103, 232));
            g2.fillOval(x, y, size, size);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            int inset = 16;
            g2.drawOval(x + inset, y + inset, size - inset * 2, size - inset * 2);
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

    private static final class RoundedPrimaryButton extends CustomButton {
        private Color hoverBackground = PRIMARY_BLUE;
        private boolean hovered;

        private RoundedPrimaryButton(String text) {
            super(text);
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setHoverBackground(Color hoverBackground) {
            this.hoverBackground = hoverBackground;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? hoverBackground : getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}

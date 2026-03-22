package com.pinbuttonmaker.pages;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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

    private static final String EMAIL_PLACEHOLDER = "Enter your email";
    private static final String PASSWORD_PLACEHOLDER = "Enter your password";

    private final AppRouter router;
    private final AppState appState;

    private final RoundedPanel cardPanel;
    private final JButton signInTabButton;
    private final JButton registerTabButton;

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final RoundedPanel emailRow;
    private final RoundedPanel passwordRow;

    private final JButton passwordToggleButton;
    private final JButton forgotPasswordButton;
    private final JPanel forgotPasswordRow;

    private final RoundedPrimaryButton submitButton;
    private final JButton googleButton;
    private final JButton footerActionButton;
    private final JLabel footerPrefixLabel;

    private boolean registerMode;
    private boolean passwordVisible;
    private boolean emailPlaceholderActive;
    private boolean passwordPlaceholderActive;
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

        signInTabButton = createSegmentTabButton("Sign In", false);
        registerTabButton = createSegmentTabButton("Register", true);

        emailField = createEmailField();
        passwordField = createPasswordField();
        defaultPasswordEcho = passwordField.getEchoChar();

        passwordToggleButton = createPasswordToggleButton();
        forgotPasswordButton = createLinkButton("Forgot password?");
        forgotPasswordRow = createForgotPasswordRow();

        submitButton = createSubmitButton();
        googleButton = createGoogleButton();

        footerPrefixLabel = new JLabel();
        footerActionButton = createFooterActionButton();

        emailRow = createInputRow("@", emailField, null);
        passwordRow = createInputRow("#", passwordField, passwordToggleButton);

        layoutCard();
        layoutFooter(content);

        installEmailPlaceholder();
        installPasswordPlaceholder();
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
        content.setBorder(new EmptyBorder(6, 8, 6, 8));

        content.add(createBadge());
        content.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("Pin Button Maker");
        title.setFont(new Font("SansSerif", Font.BOLD, 45));
        title.setForeground(new Color(33, 38, 51));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("PROTOTYPE. (you can press login w/o registering)");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(subtitle);

        content.add(Box.createVerticalStrut(16));
        return content;
    }

    private JComponent createBadge() {
        return new CircleBadge();
    }

    private RoundedPanel createCardPanel() {
        RoundedPanel panel = new RoundedPanel(18, CARD_BG, CARD_BORDER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        panel.setPreferredSize(new Dimension(500, 500));
        panel.setMaximumSize(new Dimension(560, 620));
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

    private JButton createGoogleButton() {
        JButton button = new JButton("Google");
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(37, 44, 59));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(208, 213, 223)),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> handleGoogleAuth());
        return button;
    }

    private JButton createFooterActionButton() {
        JButton button = createLinkButton("Sign up");
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.addActionListener(event -> setRegisterMode(!registerMode));
        return button;
    }

    private void layoutCard() {
        cardPanel.add(createSegmentControl());
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(createFieldLabel("Email"));
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(emailRow);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(createFieldLabel("Password"));
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(passwordRow);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(forgotPasswordRow);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(submitButton);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(createDividerRow());
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(googleButton);

        forgotPasswordButton.addActionListener(event -> Utils.showInfo(this, "Password recovery is not implemented in this prototype."));
    }

    private void layoutFooter(JPanel contentPanel) {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        footerPrefixLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        footerPrefixLabel.setForeground(MUTED_TEXT);

        footer.add(footerPrefixLabel);
        footer.add(Box.createHorizontalStrut(4));
        footer.add(footerActionButton);

        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(footer);
    }

    private RoundedPanel createSegmentControl() {
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

    private JPanel createDividerRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftLine = createDividerLine();
        JPanel rightLine = createDividerLine();

        JLabel text = new JLabel("or continue with");
        text.setFont(new Font("SansSerif", Font.PLAIN, 13));
        text.setForeground(MUTED_TEXT);

        row.add(leftLine);
        row.add(Box.createHorizontalStrut(10));
        row.add(text);
        row.add(Box.createHorizontalStrut(10));
        row.add(rightLine);

        return row;
    }

    private JPanel createDividerLine() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(new Color(209, 214, 224));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.setPreferredSize(new Dimension(40, 1));
        return panel;
    }

    private void setRegisterMode(boolean registerMode) {
        this.registerMode = registerMode;
        applyTabStyle(signInTabButton, !registerMode);
        applyTabStyle(registerTabButton, registerMode);

        submitButton.setText(registerMode ? "Register  ->" : "Sign In  ->");
        forgotPasswordButton.setVisible(!registerMode);
        forgotPasswordButton.setEnabled(!registerMode);

        if (registerMode) {
            footerPrefixLabel.setText("Already have an account?");
            footerActionButton.setText("Sign in");
        } else {
            footerPrefixLabel.setText("Don't have an account?");
            footerActionButton.setText("Sign up");
        }

        applyResponsiveLayout();
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
        int availableWidth = Math.max(380, getWidth());
        int sidePadding = availableWidth < 640 ? 34 : 70;
        int cardWidth = Math.max(330, Math.min(520, availableWidth - sidePadding));

        int availableHeight = Math.max(520, getHeight());
        int desiredHeight = 500;
        int minHeight = 450;
        int cardHeight = Math.max(minHeight, Math.min(desiredHeight, availableHeight - 170));

        cardPanel.setPreferredSize(new Dimension(cardWidth, cardHeight));

        int fieldHeight = cardWidth < 400 ? 40 : 46;
        Dimension rowSize = new Dimension(cardWidth - 40, fieldHeight);
        emailRow.setPreferredSize(rowSize);
        emailRow.setMaximumSize(rowSize);
        passwordRow.setPreferredSize(rowSize);
        passwordRow.setMaximumSize(rowSize);

        Dimension forgotSize = new Dimension(cardWidth - 40, cardWidth < 400 ? 22 : 24);
        forgotPasswordRow.setPreferredSize(forgotSize);
        forgotPasswordRow.setMaximumSize(forgotSize);
        forgotPasswordRow.setMinimumSize(forgotSize);

        int submitHeight = cardWidth < 400 ? 42 : 48;
        Dimension submitSize = new Dimension(cardWidth - 40, submitHeight);
        submitButton.setPreferredSize(submitSize);
        submitButton.setMaximumSize(submitSize);

        Dimension googleSize = new Dimension(cardWidth - 40, cardWidth < 400 ? 40 : 44);
        googleButton.setPreferredSize(googleSize);
        googleButton.setMaximumSize(googleSize);

        int tabFontSize = cardWidth < 400 ? 14 : 16;
        int inputFontSize = cardWidth < 400 ? 13 : 15;
        int labelFontSize = cardWidth < 400 ? 14 : 16;
        int buttonFontSize = cardWidth < 400 ? 14 : 16;

        signInTabButton.setFont(new Font("SansSerif", Font.BOLD, tabFontSize));
        registerTabButton.setFont(new Font("SansSerif", Font.BOLD, tabFontSize));
        emailField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize));
        forgotPasswordButton.setFont(new Font("SansSerif", Font.PLAIN, inputFontSize - 1));
        submitButton.setFont(new Font("SansSerif", Font.BOLD, buttonFontSize));

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
                    passwordField.setEchoChar(defaultPasswordEcho);
                    passwordPlaceholderActive = false;
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
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PASSWORD_PLACEHOLDER);
        passwordField.setForeground(INPUT_PLACEHOLDER);
        passwordPlaceholderActive = true;
        passwordVisible = false;
        updatePasswordToggleText();
    }

    private void togglePasswordVisibility() {
        if (passwordPlaceholderActive) {
            return;
        }

        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : defaultPasswordEcho);
        updatePasswordToggleText();
    }

    private void updatePasswordToggleText() {
        if (passwordPlaceholderActive || new String(passwordField.getPassword()).trim().isEmpty()) {
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

    private void handleEmailAuth() {
        String email = readEmailValue();
        String password = readPasswordValue();
        UserAuthService authService = appState.getUserAuthService();

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

        appState.setCurrentUser(result.getUserEmail());
        Utils.showInfo(this, registerMode ? "Registered: " + result.getUserEmail() : "Signed in: " + result.getUserEmail());
        router.showHome();
    }

    private void handleGoogleAuth() {
        appState.setCurrentUser("google.user@pincraft.local");
        Utils.showInfo(this, "Google sign-in successful (mock).");
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



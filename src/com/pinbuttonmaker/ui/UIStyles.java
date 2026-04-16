package com.pinbuttonmaker.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

public final class UIStyles {
    public static final Color SHELL_BG = new Color(30, 34, 40);
    public static final Color TOP_BAR_BG = new Color(44, 49, 58);
    public static final Color PANEL_BG = new Color(46, 52, 61);
    public static final Color PANEL_ALT_BG = new Color(65, 74, 87);
    public static final Color PANEL_BORDER = new Color(90, 102, 120);
    public static final Color TEXT_PRIMARY = new Color(243, 246, 251);
    public static final Color TEXT_MUTED = new Color(188, 197, 211);
    public static final Color ACTION_GREY = new Color(65, 74, 87);
    public static final Color ACTION_GREY_HOVER = new Color(77, 88, 112);
    public static final Color ACTION_BLUE = new Color(59, 130, 246);
    public static final Color ACTION_BLUE_HOVER = new Color(37, 99, 235);
    public static final Color ACTION_GREEN = new Color(34, 197, 94);
    public static final Color ACTION_GREEN_HOVER = new Color(22, 163, 74);
    public static final Color CANVAS_BG = new Color(210, 215, 224);
    public static final Color CANVAS_BORDER = new Color(179, 186, 198);

    private UIStyles() {
        // Utility class
    }

    public static JLabel createPageTitle(String text) {
        JLabel titleLabel = new JLabel(text);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        return titleLabel;
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(ACTION_GREY);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PANEL_BORDER),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    public static void applyPagePadding(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(
            UIConstants.PADDING,
            UIConstants.PADDING,
            UIConstants.PADDING,
            UIConstants.PADDING
        ));
    }
}

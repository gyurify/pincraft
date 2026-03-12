package com.pinbuttonmaker.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

public final class UIStyles {
    private static final Color PRIMARY_BUTTON_BG = new Color(40, 92, 143);
    private static final Color PRIMARY_BUTTON_FG = Color.WHITE;

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
        button.setBackground(PRIMARY_BUTTON_BG);
        button.setForeground(PRIMARY_BUTTON_FG);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
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

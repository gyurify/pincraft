package com.pinbuttonmaker.util;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JOptionPane;

public final class Utils {
    private Utils() {
        // Utility class
    }

    public static void centerWindow(Window window) {
        window.setLocationRelativeTo(null);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "PinCraft", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String normalizeOrDefault(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}

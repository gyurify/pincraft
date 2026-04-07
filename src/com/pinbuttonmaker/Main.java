package com.pinbuttonmaker;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppFrame appFrame = new AppFrame();
            appFrame.setVisible(true);
        });
    }
}

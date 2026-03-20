package com.pinbuttonmaker.ui.components;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class FadablePanel extends JPanel {
    private float alpha;

    public FadablePanel(Component content) {
        super(new BorderLayout());
        this.alpha = 1.0f;
        setOpaque(false);
        if (content != null) {
            add(content, BorderLayout.CENTER);
        }
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = clamp(alpha);
        repaint();
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paint(g2);
        g2.dispose();
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

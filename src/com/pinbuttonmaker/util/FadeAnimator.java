package com.pinbuttonmaker.util;

import javax.swing.Timer;

import com.pinbuttonmaker.ui.components.FadablePanel;

public final class FadeAnimator {
    private static final String TIMER_KEY = "fade.timer";
    private static final int DEFAULT_DURATION_MS = 170;
    private static final int FRAME_DELAY_MS = 16;

    private FadeAnimator() {
        // Utility class
    }

    public static void fadeIn(FadablePanel panel) {
        fadeIn(panel, DEFAULT_DURATION_MS);
    }

    public static void fadeIn(FadablePanel panel, int durationMs) {
        animate(panel, 0.85f, 1.0f, durationMs);
    }

    public static void fadeOut(FadablePanel panel, int durationMs) {
        animate(panel, panel == null ? 1.0f : panel.getAlpha(), 0.85f, durationMs);
    }

    private static void animate(FadablePanel panel, float from, float to, int durationMs) {
        if (panel == null) {
            return;
        }

        stopExisting(panel);
        panel.setAlpha(from);

        final int safeDuration = Math.max(80, durationMs);
        final long startTime = System.currentTimeMillis();

        Timer timer = new Timer(FRAME_DELAY_MS, null);
        timer.addActionListener(event -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, elapsed / (float) safeDuration);
            float eased = easeOutCubic(progress);
            float next = from + ((to - from) * eased);
            panel.setAlpha(next);

            if (progress >= 1.0f) {
                panel.setAlpha(to);
                timer.stop();
                panel.putClientProperty(TIMER_KEY, null);
            }
        });

        panel.putClientProperty(TIMER_KEY, timer);
        timer.start();
    }

    private static void stopExisting(FadablePanel panel) {
        Object value = panel.getClientProperty(TIMER_KEY);
        if (value instanceof Timer) {
            ((Timer) value).stop();
            panel.putClientProperty(TIMER_KEY, null);
        }
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - (inv * inv * inv);
    }
}

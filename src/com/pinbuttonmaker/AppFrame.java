package com.pinbuttonmaker;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

import com.pinbuttonmaker.ui.UIConstants;
import com.pinbuttonmaker.util.Utils;

public class AppFrame extends JFrame {
    private static final double MIN_DEFAULT_RATIO = (double) UIConstants.FRAME_WIDTH / UIConstants.FRAME_HEIGHT;

    private final AppState appState;
    private final AppRouter appRouter;

    private boolean enforcingBounds;

    public AppFrame() {
        super(UIConstants.APP_TITLE);

        this.appState = new AppState();
        this.appRouter = new AppRouter(appState);

        initializeFrame();
    }

    private void initializeFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(UIConstants.FRAME_WIDTH, UIConstants.FRAME_HEIGHT);
        setMinimumSize(UIConstants.FRAME_MIN_SIZE);
        setLayout(new BorderLayout());

        add(appRouter.getMainPanel(), BorderLayout.CENTER);
        appRouter.showLogin();

        attachResizeGuards();
        Utils.centerWindow(this);
    }

    private void attachResizeGuards() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                enforceDefaultMinimumRatio();
            }
        });
    }

    private void enforceDefaultMinimumRatio() {
        if (enforcingBounds) {
            return;
        }

        int currentWidth = getWidth();
        int currentHeight = getHeight();
        if (currentWidth <= 0 || currentHeight <= 0) {
            return;
        }

        int targetHeight = Math.max(currentHeight, UIConstants.FRAME_MIN_SIZE.height);
        int minimumWidthFromRatio = (int) Math.ceil(targetHeight * MIN_DEFAULT_RATIO);
        int targetWidth = Math.max(currentWidth, Math.max(UIConstants.FRAME_MIN_SIZE.width, minimumWidthFromRatio));

        if (targetWidth == currentWidth && targetHeight == currentHeight) {
            return;
        }

        enforcingBounds = true;
        setSize(targetWidth, targetHeight);
        enforcingBounds = false;
    }

    public AppState getAppState() {
        return appState;
    }

    public AppRouter getAppRouter() {
        return appRouter;
    }
}

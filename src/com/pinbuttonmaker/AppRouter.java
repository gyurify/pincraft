package com.pinbuttonmaker;

import java.awt.CardLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.pinbuttonmaker.pages.EditorPage;
import com.pinbuttonmaker.pages.HomePage;
import com.pinbuttonmaker.pages.LoginPage;
import com.pinbuttonmaker.pages.PrintPage;
import com.pinbuttonmaker.ui.components.FadablePanel;
import com.pinbuttonmaker.util.FadeAnimator;

public class AppRouter {
    public static final String ROUTE_LOGIN = "login";
    public static final String ROUTE_HOME = "home";
    public static final String ROUTE_EDITOR = "editor";
    public static final String ROUTE_PRINT = "print";

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final AppState appState;
    private final Map<String, FadablePanel> routePanels;

    public AppRouter(AppState appState) {
        this.appState = appState;
        this.cardLayout = new CardLayout();
        this.mainPanel = new JPanel(cardLayout);
        this.routePanels = new LinkedHashMap<>();

        registerPages();
    }

    private void registerPages() {
        registerRoute(ROUTE_LOGIN, new LoginPage(this, appState));
        registerRoute(ROUTE_HOME, new HomePage(this, appState));
        registerRoute(ROUTE_EDITOR, new EditorPage(this, appState));
        registerRoute(ROUTE_PRINT, new PrintPage(this, appState));
    }

    private void registerRoute(String route, JPanel content) {
        FadablePanel wrapper = new FadablePanel(content);
        routePanels.put(route, wrapper);
        mainPanel.add(wrapper, route);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void navigate(String route) {
        cardLayout.show(mainPanel, route);
        FadablePanel activePanel = routePanels.get(route);
        if (activePanel != null) {
            FadeAnimator.fadeIn(activePanel);
        }
    }

    public void showLogin() {
        navigate(ROUTE_LOGIN);
    }

    public void showHome() {
        navigate(ROUTE_HOME);
    }

    public void showEditor() {
        navigate(ROUTE_EDITOR);
    }

    public void showPrint() {
        navigate(ROUTE_PRINT);
    }
}

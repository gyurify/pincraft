package com.pinbuttonmaker;

import java.awt.CardLayout;

import javax.swing.JPanel;

import com.pinbuttonmaker.pages.EditorPage;
import com.pinbuttonmaker.pages.HomePage;
import com.pinbuttonmaker.pages.LoginPage;
import com.pinbuttonmaker.pages.PrintPage;

public class AppRouter {
    public static final String ROUTE_LOGIN = "login";
    public static final String ROUTE_HOME = "home";
    public static final String ROUTE_EDITOR = "editor";
    public static final String ROUTE_PRINT = "print";

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final AppState appState;

    public AppRouter(AppState appState) {
        this.appState = appState;
        this.cardLayout = new CardLayout();
        this.mainPanel = new JPanel(cardLayout);

        registerPages();
    }

    private void registerPages() {
        mainPanel.add(new LoginPage(this, appState), ROUTE_LOGIN);
        mainPanel.add(new HomePage(this, appState), ROUTE_HOME);
        mainPanel.add(new EditorPage(this, appState), ROUTE_EDITOR);
        mainPanel.add(new PrintPage(this, appState), ROUTE_PRINT);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void navigate(String route) {
        cardLayout.show(mainPanel, route);
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

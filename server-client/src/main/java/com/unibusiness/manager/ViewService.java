package com.unibusiness.manager;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Fachada de navegação para o conteúdo interno do Dashboard.
 *
 * Abstrai o uso direto do ViewManager para os controllers,
 * mantendo o acoplamento baixo.
 */
public class ViewService {

    private static BorderPane mainLayout;

    private ViewService() {}

    public static void setMainLayout(BorderPane layout) {
        mainLayout = layout;
    }

    public static void navigateTo(String fxmlPath) {
        requireLayout();
        ViewManager.switchContentTo(mainLayout.centerProperty(), fxmlPath);
    }

    public static <T> void navigateTo(String fxmlPath, T data) {
        requireLayout();
        ViewManager.switchContentTo(mainLayout.centerProperty(), fxmlPath, data);
    }

    public static void navigateToSidebar(String fxmlPath) {
        requireLayout();
        ViewManager.switchContentTo(mainLayout.leftProperty(), fxmlPath);
    }

    public static void navigateToHeader(String fxmlPath) {
        requireLayout();
        ViewManager.switchContentTo(mainLayout.topProperty(), fxmlPath);
    }

    public static void navigateToFooter(String fxmlPath) {
        requireLayout();
        ViewManager.switchContentTo(mainLayout.bottomProperty(), fxmlPath);
    }

    public static Node getCurrentContent() {
        return mainLayout != null ? mainLayout.getCenter() : null;
    }

    public static void clearContent() {
        if (mainLayout != null) mainLayout.setCenter(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getController(String fxmlPath) {
        return (T) ViewManager.getController(fxmlPath);
    }

    private static void requireLayout() {
        if (mainLayout == null)
            throw new IllegalStateException("MainLayout não definido. Chame setMainLayout() primeiro.");
    }
}

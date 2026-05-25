package com.unibusiness.manager;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class ViewService {

    private static BorderPane mainLayout;

    public static void setMainLayout(BorderPane layout) {
        mainLayout = layout;
    }

    public static void navigateTo(String fxmlPath) {
        if (mainLayout == null) {
            throw new IllegalStateException("MainLayout não foi definido. Chame setMainLayout() primeiro.");
        }
        ViewManager.switchContentTo(mainLayout.centerProperty(), fxmlPath);
    }

    public static <T> void navigateTo(String fxmlPath, T data) {
        if (mainLayout == null) {
            throw new IllegalStateException("MainLayout não foi definido. Chame setMainLayout() primeiro.");
        }
        ViewManager.switchContentTo(mainLayout.centerProperty(), fxmlPath, data);
    }

    public static void navigateToSidebar(String fxmlPath) {
        if (mainLayout == null) {
            throw new IllegalStateException("MainLayout não foi definido. Chame setMainLayout() primeiro.");
        }
        ViewManager.switchContentTo(mainLayout.leftProperty(), fxmlPath);
    }

    public static void navigateToHeader(String fxmlPath) {
        if (mainLayout == null) {
            throw new IllegalStateException("MainLayout não foi definido. Chame setMainLayout() primeiro.");
        }
        ViewManager.switchContentTo(mainLayout.topProperty(), fxmlPath);
    }

    public static void navigateToFooter(String fxmlPath) {
        if (mainLayout == null) {
            throw new IllegalStateException("MainLayout não foi definido. Chame setMainLayout() primeiro.");
        }
        ViewManager.switchContentTo(mainLayout.bottomProperty(), fxmlPath);
    }

    public static Node getCurrentContent() {
        if (mainLayout == null) {
            return null;
        }
        return mainLayout.getCenter();
    }

    public static void clearContent() {
        if (mainLayout != null) {
            mainLayout.setCenter(null);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getController(String fxmlPath) {
        return (T) ViewManager.getController(fxmlPath);
    }
}
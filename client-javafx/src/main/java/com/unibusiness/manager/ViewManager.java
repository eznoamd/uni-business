package com.unibusiness.manager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class ViewManager {

    private static Stage primaryStage;
    private static Stage secondaryStage;
    
    private static final Map<String, Node> viewCache = new HashMap<>();
    
    private static final Map<String, Object> controllerCache = new HashMap<>();
    
    private static Consumer<String> onViewChangeListener;

    private static boolean firstStage = true;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setSecondaryStage(Stage stage) {
        secondaryStage = stage;
    }

    public static void switchPrimaryTo(String fxmlPath) {
        switchSceneTo(primaryStage, fxmlPath);
    }

    public static void switchSecondaryTo(String fxmlPath) {
        switchSceneTo(secondaryStage, fxmlPath);
    }

    public static void switchContentTo(javafx.beans.property.ObjectProperty<Node> container, String fxmlPath) {
        try {
            Node view = loadView(fxmlPath);
            container.set(view);
            notifyViewChange(fxmlPath);
        } catch (IOException e) {
            handleError("Erro ao carregar view: " + fxmlPath, e);
        }
    }

    public static <T> void switchContentTo(
            javafx.beans.property.ObjectProperty<Node> container, 
            String fxmlPath, 
            T data) {
        try {
            Node view = loadView(fxmlPath);
            Object controller = controllerCache.get(fxmlPath);
            
            // Se o controller implementa DataReceiver, passa os dados
            if (controller instanceof DataReceiver) {
                ((DataReceiver<T>) controller).receiveData(data);
            }
            
            container.set(view);
            notifyViewChange(fxmlPath);
        } catch (IOException e) {
            handleError("Erro ao carregar view: " + fxmlPath, e);
        }
    }

    public static Node loadViewOnly(String fxmlPath) throws IOException {
        return loadView(fxmlPath);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getController(String fxmlPath) {
        return (T) controllerCache.get(fxmlPath);
    }

    public static void clearViewCache(String fxmlPath) {
        viewCache.remove(fxmlPath);
        controllerCache.remove(fxmlPath);
    }

    public static void clearAllCache() {
        viewCache.clear();
        controllerCache.clear();
    }

    public static void setOnViewChangeListener(Consumer<String> listener) {
        onViewChangeListener = listener;
    }

    private static void switchSceneTo(Stage stage, String fxmlPath) {
        try {
            Node view = loadView(fxmlPath);
            Scene scene = new Scene((Parent) view);
            stage.setScene(scene);
            stage.show();
            if (firstStage) {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                stage.setMaximized(true);
                
                firstStage = false;
            }
            notifyViewChange(fxmlPath);
        } catch (IOException e) {
            handleError("Erro ao carregar cena: " + fxmlPath, e);
        }
    }

    private static Node loadView(String fxmlPath) throws IOException {
        if (viewCache.containsKey(fxmlPath)) {
            return viewCache.get(fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmlPath));
        Node view = loader.load();
        
        viewCache.put(fxmlPath, view);
        controllerCache.put(fxmlPath, loader.getController());
        
        return view;
    }

    private static void notifyViewChange(String fxmlPath) {
        if (onViewChangeListener != null) {
            onViewChangeListener.accept(fxmlPath);
        }
    }

    private static void handleError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
    }

    public interface DataReceiver<T> {
        void receiveData(T data);
    }
}
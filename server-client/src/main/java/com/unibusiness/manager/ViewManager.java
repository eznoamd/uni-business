package com.unibusiness.manager;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Gerencia a navegação entre views FXML.
 *
 * Cache: views estáticas (sidebar, dashboard) são cacheadas para performance.
 * Views com estado (chat, tarefas, home) nunca são cacheadas para garantir
 * que os dados sejam sempre recarregados ao navegar.
 *
 * Regra: qualquer view que carrega dados do servidor NÃO deve ser cacheada.
 */
public class ViewManager {

    /** Views que NUNCA devem ser cacheadas (têm estado / carregam dados). */
    private static final Set<String> NO_CACHE_VIEWS = Set.of(
        "/views/aba/chat.fxml",
        "/views/aba/home.fxml",
        "/views/aba/minhas-tarefas.fxml",
        "/views/aba/tarefas.fxml",
        "/views/aba/estoque.fxml",
        "/views/aba/financeiro.fxml",
        "/views/aba/clientes.fxml",
        "/views/aba/fornecedores.fxml",
        "/views/aba/funcionarios.fxml",
        "/views/aba/equipes.fxml"
    );

    private static Stage primaryStage;
    private static boolean firstShow = true;

    private static final Map<String, Node>   viewCache       = new HashMap<>();
    private static final Map<String, Object> controllerCache = new HashMap<>();

    private static Consumer<String> onViewChangeListener;

    private ViewManager() {}

    // ── Configuração de stage ─────────────────────────────────────────────────

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    // ── Navegação de cena completa ────────────────────────────────────────────

    public static void switchPrimaryTo(String fxmlPath) {
        try {
            Node view = loadView(fxmlPath);
            Scene scene = new Scene((Parent) view);
            primaryStage.setScene(scene);
            primaryStage.show();

            if (firstShow) {
                maximizarJanela();
                firstShow = false;
            }

            notifyViewChange(fxmlPath);
        } catch (IOException e) {
            handleError("Erro ao carregar cena: " + fxmlPath, e);
        }
    }

    // ── Navegação de conteúdo interno ─────────────────────────────────────────

    public static void switchContentTo(
            javafx.beans.property.ObjectProperty<Node> container,
            String fxmlPath) {
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
            if (controller instanceof DataReceiver<?>) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> receiver = (DataReceiver<T>) controller;
                receiver.receiveData(data);
            }
            container.set(view);
            notifyViewChange(fxmlPath);
        } catch (IOException e) {
            handleError("Erro ao carregar view: " + fxmlPath, e);
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

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

    // ── Interno ───────────────────────────────────────────────────────────────

    private static Node loadView(String fxmlPath) throws IOException {
        // Views com estado nunca são cacheadas
        boolean useCache = !NO_CACHE_VIEWS.contains(fxmlPath);

        if (useCache && viewCache.containsKey(fxmlPath)) {
            return viewCache.get(fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(fxmlPath));
        Node view = loader.load();

        if (useCache) {
            viewCache.put(fxmlPath, view);
            controllerCache.put(fxmlPath, loader.getController());
        } else {
            // Ainda armazena o controller para acesso por getController(),
            // mas não cacheia a view
            controllerCache.put(fxmlPath, loader.getController());
        }

        return view;
    }

    private static void maximizarJanela() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setMaximized(true);
    }

    private static void notifyViewChange(String fxmlPath) {
        if (onViewChangeListener != null) onViewChangeListener.accept(fxmlPath);
    }

    private static void handleError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
    }

    // ── Interface de dados ────────────────────────────────────────────────────

    public interface DataReceiver<T> {
        void receiveData(T data);
    }
}

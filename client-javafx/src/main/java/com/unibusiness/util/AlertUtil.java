package com.unibusiness.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Utilitário de alertas padronizados para o cliente JavaFX.
 *
 * Todos os métodos são thread-safe: podem ser chamados de qualquer thread,
 * pois usam Platform.runLater() quando necessário.
 */
public final class AlertUtil {

    private AlertUtil() {}

    public static void erro(String mensagem) {
        mostrar(AlertType.ERROR, "Erro", mensagem);
    }

    public static void erro(String titulo, String mensagem) {
        mostrar(AlertType.ERROR, titulo, mensagem);
    }

    public static void aviso(String mensagem) {
        mostrar(AlertType.WARNING, "Atenção", mensagem);
    }

    public static void aviso(String titulo, String mensagem) {
        mostrar(AlertType.WARNING, titulo, mensagem);
    }

    public static void info(String mensagem) {
        mostrar(AlertType.INFORMATION, "Informação", mensagem);
    }

    public static void info(String titulo, String mensagem) {
        mostrar(AlertType.INFORMATION, titulo, mensagem);
    }

    // ── Interno ───────────────────────────────────────────────────────────────

    private static void mostrar(AlertType tipo, String titulo, String mensagem) {
        Runnable show = () -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }
}

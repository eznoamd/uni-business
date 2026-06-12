package com.unibusiness;

import com.unibusiness.core.config.DatabasePopulate;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.manager.ViewManager;
import com.unibusiness.service.AuthService;
import com.unibusiness.session.SessionManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Antes: só cuidava da navegação JavaFX; a conexão com o servidor TCP era
 * aberta sob demanda pelo LoginController.
 *
 * Agora: também inicializa o banco (JPA) ao subir, e garante que o usuário
 * é marcado como offline ao fechar a janela (presença = campo no banco).
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        PersistenceManager.getEntityManagerFactory();
        DatabasePopulate.run();

        stage.setOnCloseRequest(e -> {
            if (SessionManager.getInstance().isLogado()) {
                new AuthService().logout();
            }
            PersistenceManager.close();
        });

        ViewManager.setPrimaryStage(stage);
        ViewManager.switchPrimaryTo("/views/login.fxml");
    }

    public static void main(String[] args) {
        launch();
    }
}

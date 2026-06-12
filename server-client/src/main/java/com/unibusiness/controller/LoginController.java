package com.unibusiness.controller;

import com.unibusiness.manager.ViewManager;
import com.unibusiness.service.AuthService;
import com.unibusiness.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

/**
 * Antes: conectava no TcpClient (host/porta do servidor) e chamava AuthService.login()
 * via socket.
 * Agora: AuthService.login() consulta o banco direto (JPA). Mantemos o Task em
 * background só para não travar a UI durante a consulta ao banco.
 */
public class LoginController {

    @FXML private TextField     contaField;
    @FXML private PasswordField senhaField;
    @FXML private Button        loginButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private StackPane     loadingOverlay;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        loginButton.setDefaultButton(true);
    }

    @FXML
    private void login() {
        String email = contaField.getText().trim();
        String senha = senhaField.getText();

        if (email.isBlank() || senha.isBlank()) {
            AlertUtil.aviso("Preencha e-mail e senha.");
            return;
        }

        setLoading(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.login(email, senha);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue()) {
                ViewManager.switchPrimaryTo("/views/dashboard.fxml");
            } else {
                AlertUtil.erro("Falha na autenticação", authService.getUltimoErro());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            AlertUtil.erro("Erro ao acessar o banco de dados",
                "Verifique se o Postgres está rodando e acessível.");
        });

        new Thread(task, "login-thread").start();
    }

    private void setLoading(boolean active) {
        loadingOverlay.setVisible(active);
        loadingOverlay.setManaged(active);
        contaField.setDisable(active);
        senhaField.setDisable(active);
        loginButton.setDisable(active);
    }
}

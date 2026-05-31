package com.unibusiness.controller;

import com.unibusiness.manager.ViewManager;
import com.unibusiness.network.TcpClient;
import com.unibusiness.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller da tela de login.
 * Substitui a verificação hardcoded por autenticação TCP real.
 */
public class LoginController {

    // ── Configuração da conexão ───────────────────────────────────────────────
    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 7777;

    @FXML private TextField     contaField;
    @FXML private PasswordField senhaField;
    @FXML private Button        loginButton;
    @FXML private ProgressIndicator loadingIndicator; // opcional no FXML

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
            mostrarErro("Preencha e-mail e senha.");
            return;
        }

        loginButton.setDisable(true);
        setLoading(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // 1. Conecta ao servidor se ainda não conectado
                if (!TcpClient.getInstance().isConnected()) {
                    TcpClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
                }
                // 2. Autentica
                return authService.login(email, senha);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            loginButton.setDisable(false);

            if (task.getValue()) {
                // Login ok → vai para o dashboard
                ViewManager.switchPrimaryTo("/views/dashboard.fxml");
            } else {
                mostrarErro(authService.getUltimoErro());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            loginButton.setDisable(false);
            mostrarErro("Não foi possível conectar ao servidor.\nVerifique se o servidor está rodando em "
                + SERVER_HOST + ":" + SERVER_PORT);
        });

        new Thread(task, "login-thread").start();
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Login");
        alert.setHeaderText("Falha na autenticação");
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void setLoading(boolean active) {
        // Se você adicionar um ProgressIndicator no FXML com fx:id="loadingIndicator"
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(active);
            loadingIndicator.setManaged(active);
        }
    }
}

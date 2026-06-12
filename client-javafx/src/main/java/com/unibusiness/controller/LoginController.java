package com.unibusiness.controller;

import com.unibusiness.manager.ViewManager;
import com.unibusiness.network.TcpClient;
import com.unibusiness.service.AuthService;
import com.unibusiness.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class LoginController {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 7777;

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
            protected Boolean call() throws Exception {
                if (!TcpClient.getInstance().isConnected()) {
                    TcpClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
                }
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
            AlertUtil.erro("Conexão falhou",
                "Não foi possível conectar ao servidor.\n" +
                "Verifique se o servidor está rodando em " + SERVER_HOST + ":" + SERVER_PORT);
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

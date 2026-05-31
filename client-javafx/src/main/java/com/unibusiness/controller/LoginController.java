package com.unibusiness.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import com.unibusiness.manager.ViewManager;

public class LoginController {

    @FXML
    private TextField contaField;

    @FXML
    private PasswordField senhaField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private void initialize() {
        loginButton.setDefaultButton(true);
    }

    @FXML
    private void login() {
        String conta = contaField.getText();
        String senha = senhaField.getText();

        if (conta.equals("teste") && senha.equals("1234")) {
            System.out.println("Login bem-sucedido!");
            ViewManager.switchPrimaryTo("/views/dashboard.fxml");
            return;
        }

        // Feedback para login inválido
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Login");
        alert.setHeaderText("Credenciais inválidas");
        alert.setContentText("Usuário ou senha incorretos. Tente novamente.");
        alert.showAndWait();
    }
}
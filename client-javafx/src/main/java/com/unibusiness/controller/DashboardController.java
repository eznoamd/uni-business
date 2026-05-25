package com.unibusiness.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import com.unibusiness.manager.ViewService;

public class DashboardController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private void initialize() {
        ViewService.setMainLayout(mainLayout);
        ViewService.navigateTo("/views/aba/home.fxml");
    }

    @FXML
    private void switchToHome() {
        ViewService.navigateTo("/views/aba/home.fxml");
    }

    @FXML
    private void switchToMinhasTarefas() {
        ViewService.navigateTo("/views/aba/minhas-tarefas.fxml");
    }

    @FXML
    private void switchToChat() {
        ViewService.navigateTo("/views/aba/chat.fxml");
    }

    @FXML 
    private void switchToEstoque() {
        ViewService.navigateTo("/views/aba/estoque.fxml");
    }

    @FXML
    private void switchToFinanceiro() {
        ViewService.navigateTo("/views/aba/financeiro.fxml");
    }

    @FXML
    private void switchToClientes() {
        ViewService.navigateTo("/views/aba/clientes.fxml");
    }

    @FXML
    private void switchToFornecedores() {
        ViewService.navigateTo("/views/aba/fornecedores.fxml");
    }

    @FXML
    private void switchToFuncionarios() {
        ViewService.navigateTo("/views/aba/funcionarios.fxml");
    }

    @FXML
    private void switchToTarefas() {
        ViewService.navigateTo("/views/aba/tarefas.fxml");
    }

    @FXML
    private void switchToEquipes() {
        ViewService.navigateTo("/views/aba/equipes.fxml");
    }
}
package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.UsuarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class FuncionariosController {

    @FXML private TableView<Dto.Usuario>         tabela;
    @FXML private TableColumn<Dto.Usuario, Integer> colId;
    @FXML private TableColumn<Dto.Usuario, String>  colNome;
    @FXML private TableColumn<Dto.Usuario, String>  colEmail;
    @FXML private TableColumn<Dto.Usuario, Boolean> colAtivo;

    @FXML private TextField buscaField;
    @FXML private Label     lblTotal;
    @FXML private Label     lblAtivos;

    @FXML private StackPane loadingOverlay;
    @FXML private StackPane customSpinner;

    private final UsuarioService usuarioService = new UsuarioService();
    private final ObservableList<Dto.Usuario> usuarios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabela();
        iniciarAnimacaoSpinner();
        buscaField.textProperty().addListener((obs, o, n) -> filtrar(n));
        carregarFuncionarios();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAtivo.setCellValueFactory(new PropertyValueFactory<>("ativo"));

        colAtivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item ? "Ativo" : "Inativo");
                l.getStyleClass().add(item ? "badge-green" : "badge-gray");
                setGraphic(l); setText(null);
            }
        });

        tabela.setItems(usuarios);
        tabela.setPlaceholder(new Label("Nenhum funcionário encontrado."));
    }

    private void filtrar(String termo) {
        if (termo == null || termo.isBlank()) { tabela.setItems(usuarios); return; }
        String t = termo.toLowerCase();
        tabela.setItems(usuarios.filtered(u ->
            u.getNome().toLowerCase().contains(t) ||
            u.getEmail().toLowerCase().contains(t)
        ));
    }

    private void carregarFuncionarios() {
        setLoading(true);
        Task<List<Dto.Usuario>> task = new Task<>() {
            @Override protected List<Dto.Usuario> call() { return usuarioService.listarUsuarios(); }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            usuarios.setAll(task.getValue());
            lblTotal.setText(String.valueOf(usuarios.size()));
            long ativos = usuarios.stream().filter(Dto.Usuario::isAtivo).count();
            lblAtivos.setText(String.valueOf(ativos));
        });
        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-funcionarios").start();
    }

    @FXML
    private void recarregar() { carregarFuncionarios(); }

    private void setLoading(boolean active) {
        Platform.runLater(() -> {
            loadingOverlay.setVisible(active);
            loadingOverlay.setManaged(active);
        });
    }

    private void iniciarAnimacaoSpinner() {
        javafx.animation.RotateTransition r =
            new javafx.animation.RotateTransition(javafx.util.Duration.seconds(1), customSpinner);
        r.setByAngle(360);
        r.setCycleCount(javafx.animation.Animation.INDEFINITE);
        r.setInterpolator(javafx.animation.Interpolator.LINEAR);
        r.play();
    }
}

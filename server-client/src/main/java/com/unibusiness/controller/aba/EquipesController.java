package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.EquipeService;
import com.unibusiness.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class EquipesController {

    @FXML private TableView<Dto.Equipe>         tabela;
    @FXML private TableColumn<Dto.Equipe, Integer> colId;
    @FXML private TableColumn<Dto.Equipe, String>  colNome;
    @FXML private TableColumn<Dto.Equipe, Void>    colAcoes;

    @FXML private Label     lblTotal;
    @FXML private StackPane formOverlay;
    @FXML private TextField fNome;
    @FXML private Label     formTitulo;

    @FXML private StackPane loadingOverlay;
    @FXML private StackPane customSpinner;

    private final EquipeService equipeService = new EquipeService();
    private final ObservableList<Dto.Equipe> equipes = FXCollections.observableArrayList();
    private Dto.Equipe editando;

    @FXML
    public void initialize() {
        configurarTabela();
        iniciarAnimacaoSpinner();
        carregarEquipes();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        colAcoes.setCellFactory(col -> new TableCell<>() {
            final Button btnEdit = new Button("✏ Editar");
            final Button btnDel  = new Button("✕");
            final HBox box = new HBox(8, btnEdit, btnDel);

            {
                btnEdit.getStyleClass().add("btn-ghost");
                btnDel.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> abrirEdicao(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e  -> confirmarDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tabela.setItems(equipes);
        tabela.setPlaceholder(new Label("Nenhuma equipe cadastrada."));
    }

    private void carregarEquipes() {
        setLoading(true);
        Task<List<Dto.Equipe>> task = new Task<>() {
            @Override protected List<Dto.Equipe> call() { return equipeService.listar(); }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            equipes.setAll(task.getValue());
            lblTotal.setText(String.valueOf(equipes.size()));
        });
        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-equipes").start();
    }

    @FXML
    private void abrirFormNovo() {
        editando = null;
        formTitulo.setText("Nova Equipe");
        fNome.clear();
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    private void abrirEdicao(Dto.Equipe e) {
        editando = e;
        formTitulo.setText("Editar Equipe");
        fNome.setText(e.getNome());
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    @FXML
    private void fecharForm() {
        formOverlay.setVisible(false);
        formOverlay.setManaged(false);
    }

    @FXML
    private void salvar() {
        String nome = fNome.getText().trim();
        if (nome.isBlank()) { AlertUtil.aviso("Informe o nome da equipe."); return; }

        fecharForm();
        setLoading(true);
        Dto.Equipe ref = editando;

        new Thread(() -> {
            boolean ok;
            if (ref == null) {
                ok = equipeService.criar(nome) != null;
            } else {
                ok = equipeService.atualizar(ref.getId(), nome);
            }
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível salvar.");
                carregarEquipes();
            });
        }, "salvar-equipe").start();
    }

    private void confirmarDelete(Dto.Equipe e) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover equipe \"" + e.getNome() + "\"?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    boolean ok = equipeService.deletar(e.getId());
                    Platform.runLater(() -> {
                        if (!ok) AlertUtil.aviso("Não foi possível remover.");
                        carregarEquipes();
                    });
                }, "delete-equipe").start();
            }
        });
    }

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

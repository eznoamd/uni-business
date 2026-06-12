package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.TarefaService;
import com.unibusiness.service.UsuarioService;
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

public class TarefasController {

    @FXML private TableView<Dto.Tarefa>         tabela;
    @FXML private TableColumn<Dto.Tarefa, Integer> colId;
    @FXML private TableColumn<Dto.Tarefa, String>  colTitulo;
    @FXML private TableColumn<Dto.Tarefa, String>  colStatus;
    @FXML private TableColumn<Dto.Tarefa, String>  colPrioridade;
    @FXML private TableColumn<Dto.Tarefa, String>  colResponsavel;
    @FXML private TableColumn<Dto.Tarefa, String>  colDataFim;
    @FXML private TableColumn<Dto.Tarefa, Void>    colAcoes;

    @FXML private Label lblTotal;
    @FXML private Label lblPendentes;
    @FXML private Label lblAndamento;
    @FXML private Label lblConcluidas;

    @FXML private ComboBox<String> filtroStatus;
    @FXML private TextField        buscaField;

    // Form
    @FXML private StackPane   formOverlay;
    @FXML private TextField   fTitulo;
    @FXML private TextArea    fDescricao;
    @FXML private ComboBox<String> fPrioridade;
    @FXML private TextField   fDataFim;
    @FXML private ComboBox<String> fResponsavel;

    @FXML private StackPane   loadingOverlay;
    @FXML private StackPane   customSpinner;

    private final TarefaService  tarefaService  = new TarefaService();
    private final UsuarioService usuarioService = new UsuarioService();

    private final ObservableList<Dto.Tarefa>  tarefas  = FXCollections.observableArrayList();
    private List<Dto.Usuario> usuarios;

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltros();
        iniciarAnimacaoSpinner();
        carregarTarefas();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item.replace("_", " "));
                String cls = switch (item) {
                    case "CONCLUIDA"   -> "badge-green";
                    case "EM_ANDAMENTO" -> "badge-blue";
                    default -> "badge-orange";
                };
                l.getStyleClass().add(cls);
                setGraphic(l); setText(null);
            }
        });

        colPrioridade.setCellValueFactory(new PropertyValueFactory<>("prioridade"));
        colPrioridade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                String cls = switch (item) {
                    case "ALTA"  -> "badge-red";
                    case "MEDIA" -> "badge-orange";
                    default      -> "badge-gray";
                };
                l.getStyleClass().add(cls);
                setGraphic(l); setText(null);
            }
        });

        colResponsavel.setCellValueFactory(new PropertyValueFactory<>("responsavel"));
        colDataFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));

        colAcoes.setCellFactory(col -> new TableCell<>() {
            final Button btnConcluir = criarBtn("✓ Concluir", "btn-success");
            final Button btnAndamento = criarBtn("▶ Iniciar", "btn-warning");
            final Button btnDel = criarBtn("✕", "btn-danger");
            final HBox box = new HBox(6, btnAndamento, btnConcluir, btnDel);

            {
                btnAndamento.setOnAction(e -> alterarStatus(
                    getTableView().getItems().get(getIndex()), "EM_ANDAMENTO"));
                btnConcluir.setOnAction(e -> alterarStatus(
                    getTableView().getItems().get(getIndex()), "CONCLUIDA"));
                btnDel.setOnAction(e -> confirmarDelete(
                    getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tabela.setItems(tarefas);
        tabela.setPlaceholder(new Label("Nenhuma tarefa encontrada."));
    }

    private Button criarBtn(String text, String cls) {
        Button b = new Button(text);
        b.getStyleClass().add(cls);
        return b;
    }

    private void configurarFiltros() {
        filtroStatus.setItems(FXCollections.observableArrayList(
            "Todos", "PENDENTE", "EM_ANDAMENTO", "CONCLUIDA"));
        filtroStatus.getSelectionModel().selectFirst();
        filtroStatus.valueProperty().addListener((obs, o, n) -> aplicarFiltro());
        buscaField.textProperty().addListener((obs, o, n) -> aplicarFiltro());
    }

    private void aplicarFiltro() {
        String status = filtroStatus.getValue();
        String busca  = buscaField.getText().toLowerCase();

        tabela.setItems(tarefas.filtered(t -> {
            boolean okStatus = "Todos".equals(status) || status.equals(t.getStatus());
            boolean okBusca  = busca.isBlank() ||
                t.getTitulo().toLowerCase().contains(busca) ||
                (t.getResponsavel() != null && t.getResponsavel().toLowerCase().contains(busca));
            return okStatus && okBusca;
        }));
    }

    private void carregarTarefas() {
        setLoading(true);
        Task<List<Dto.Tarefa>> task = new Task<>() {
            @Override protected List<Dto.Tarefa> call() { return tarefaService.listar(); }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            tarefas.setAll(task.getValue());
            atualizarKpis();
            aplicarFiltro();
        });
        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-tarefas").start();
    }

    private void atualizarKpis() {
        lblTotal.setText(String.valueOf(tarefas.size()));
        lblPendentes.setText(String.valueOf(tarefas.stream().filter(t -> "PENDENTE".equals(t.getStatus())).count()));
        lblAndamento.setText(String.valueOf(tarefas.stream().filter(t -> "EM_ANDAMENTO".equals(t.getStatus())).count()));
        lblConcluidas.setText(String.valueOf(tarefas.stream().filter(t -> "CONCLUIDA".equals(t.getStatus())).count()));
    }

    @FXML
    private void abrirFormNovo() {
        carregarUsuariosForm();
        fTitulo.clear(); fDescricao.clear(); fDataFim.clear();
        fPrioridade.getSelectionModel().selectFirst();
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    private void carregarUsuariosForm() {
        new Thread(() -> {
            usuarios = usuarioService.listarUsuarios();
            List<String> nomes = usuarios.stream().map(u -> u.getNome()).toList();
            Platform.runLater(() -> {
                fResponsavel.setItems(FXCollections.observableArrayList(nomes));
                fResponsavel.getSelectionModel().selectFirst();
            });
        }, "load-usuarios-form").start();
    }

    @FXML
    private void fecharForm() {
        formOverlay.setVisible(false);
        formOverlay.setManaged(false);
    }

    @FXML
    private void salvarTarefa() {
        String titulo = fTitulo.getText().trim();
        if (titulo.isBlank()) { AlertUtil.aviso("Informe o título."); return; }

        String prioridade = fPrioridade.getValue();
        String dataFim    = fDataFim.getText().trim();
        String descricao  = fDescricao.getText().trim();

        Integer respId = null;
        int idx = fResponsavel.getSelectionModel().getSelectedIndex();
        if (usuarios != null && idx >= 0 && idx < usuarios.size()) {
            respId = usuarios.get(idx).getId();
        }

        fecharForm();
        setLoading(true);
        Integer respIdFinal = respId;

        new Thread(() -> {
            Dto.Tarefa t = tarefaService.criar(titulo, descricao, prioridade,
                dataFim.isBlank() ? null : dataFim, respIdFinal);
            Platform.runLater(() -> {
                setLoading(false);
                if (t == null) AlertUtil.aviso("Não foi possível criar a tarefa.");
                carregarTarefas();
            });
        }, "criar-tarefa").start();
    }

    private void alterarStatus(Dto.Tarefa t, String novoStatus) {
        setLoading(true);
        new Thread(() -> {
            boolean ok = tarefaService.atualizar(t.getId(), novoStatus, t.getPrioridade());
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível atualizar.");
                carregarTarefas();
            });
        }, "update-tarefa").start();
    }

    private void confirmarDelete(Dto.Tarefa t) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover tarefa \"" + t.getTitulo() + "\"?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    boolean ok = tarefaService.deletar(t.getId());
                    Platform.runLater(() -> {
                        if (!ok) AlertUtil.aviso("Não foi possível remover.");
                        carregarTarefas();
                    });
                }, "delete-tarefa").start();
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

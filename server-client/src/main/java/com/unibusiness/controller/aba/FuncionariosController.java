package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
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

public class FuncionariosController {

    @FXML private TableView<Dto.Usuario>         tabela;
    @FXML private TableColumn<Dto.Usuario, Integer> colId;
    @FXML private TableColumn<Dto.Usuario, String>  colNome;
    @FXML private TableColumn<Dto.Usuario, String>  colEmail;
    @FXML private TableColumn<Dto.Usuario, Boolean> colAtivo;

    @FXML private TextField buscaField;
    @FXML private Label     lblTotal;
    @FXML private Label     lblAtivos;

    // Form "Novo Funcionário"
    @FXML private StackPane     formOverlay;
    @FXML private TextField     fNome;
    @FXML private TextField     fEmail;
    @FXML private PasswordField fSenha;
    @FXML private ComboBox<String> fCargo;
    @FXML private CheckBox      fAtivo;

    @FXML private StackPane loadingOverlay;
    @FXML private StackPane customSpinner;

    private final UsuarioService usuarioService = new UsuarioService();
    private final ObservableList<Dto.Usuario> usuarios = FXCollections.observableArrayList();

    /** Cargos disponíveis, na mesma ordem mostrada no ComboBox. */
    private List<Dto.Cargo> cargosDisponiveis;

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

    // ── Novo funcionário ─────────────────────────────────────────────────────

    @FXML
    private void abrirFormNovo() {
        fNome.clear();
        fEmail.clear();
        fSenha.clear();
        fAtivo.setSelected(true);
        fCargo.getItems().clear();
        carregarCargos();
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    private void carregarCargos() {
        Task<List<Dto.Cargo>> task = new Task<>() {
            @Override protected List<Dto.Cargo> call() { return usuarioService.listarCargos(); }
        };
        task.setOnSucceeded(e -> {
            cargosDisponiveis = task.getValue();
            List<String> nomes = cargosDisponiveis.stream().map(Dto.Cargo::getNome).toList();
            fCargo.setItems(FXCollections.observableArrayList(nomes));
            if (!nomes.isEmpty()) fCargo.getSelectionModel().selectFirst();
        });
        new Thread(task, "load-cargos").start();
    }

    @FXML
    private void fecharForm() {
        formOverlay.setVisible(false);
        formOverlay.setManaged(false);
    }

    @FXML
    private void salvar() {
        String nome  = fNome.getText().trim();
        String email = fEmail.getText().trim();
        String senha = fSenha.getText();

        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            AlertUtil.aviso("Preencha nome, e-mail e senha.");
            return;
        }
        if (senha.length() < 6) {
            AlertUtil.aviso("A senha deve ter pelo menos 6 caracteres.");
            return;
        }

        Integer cargoId = null;
        int idx = fCargo.getSelectionModel().getSelectedIndex();
        if (cargosDisponiveis != null && idx >= 0 && idx < cargosDisponiveis.size()) {
            cargoId = cargosDisponiveis.get(idx).getId();
        }
        final Integer cargoIdFinal = cargoId;
        boolean ativo = fAtivo.isSelected();

        fecharForm();
        setLoading(true);

        Task<Dto.Usuario> task = new Task<>() {
            @Override protected Dto.Usuario call() {
                return usuarioService.criar(nome, email, senha, cargoIdFinal, ativo);
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue() == null) {
                AlertUtil.aviso("Não foi possível criar o funcionário. O e-mail já pode estar cadastrado.");
            }
            carregarFuncionarios();
        });
        task.setOnFailed(e -> {
            setLoading(false);
            AlertUtil.erro("Erro ao criar funcionário.");
            carregarFuncionarios();
        });
        new Thread(task, "criar-funcionario").start();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

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

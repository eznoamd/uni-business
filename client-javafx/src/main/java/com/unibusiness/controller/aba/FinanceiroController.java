package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.CaixaService;
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

/**
 * Controller do Financeiro.
 *
 * CORREÇÕES:
 * - Usa CaixaService.getCaixaAtual() (sem id) que internamente chama CAIXA_GET_ATUAL.
 * - fecharCaixa() não passa mais id (o service guarda internamente).
 * - movimentar() não passa mais caixaId (o service guarda internamente).
 * - listarMovimentacoes() não passa mais caixaId.
 */
public class FinanceiroController {

    @FXML private Label lblStatus;
    @FXML private Label lblSaldoAtual;
    @FXML private Label lblSaldoInicial;
    @FXML private Label lblAbertura;

    @FXML private Button btnAbrirFechar;
    @FXML private HBox   painelCaixaAberto;

    @FXML private TableView<Dto.MovimentacaoCaixa>         tabelaMov;
    @FXML private TableColumn<Dto.MovimentacaoCaixa, String>  colTipo;
    @FXML private TableColumn<Dto.MovimentacaoCaixa, Double>  colValor;
    @FXML private TableColumn<Dto.MovimentacaoCaixa, String>  colDesc;
    @FXML private TableColumn<Dto.MovimentacaoCaixa, String>  colData;

    @FXML private StackPane   movOverlay;
    @FXML private ComboBox<String> movTipo;
    @FXML private TextField   movValor;
    @FXML private TextField   movDesc;

    @FXML private StackPane   abrirOverlay;
    @FXML private TextField   saldoInicialField;

    @FXML private StackPane   loadingOverlay;
    @FXML private StackPane   customSpinner;

    private final CaixaService caixaService = new CaixaService();
    private final ObservableList<Dto.MovimentacaoCaixa> movimentacoes = FXCollections.observableArrayList();
    private Dto.Caixa caixaAtual;

    @FXML
    public void initialize() {
        configurarTabela();
        iniciarAnimacaoSpinner();
        carregarCaixa();
    }

    private void configurarTabela() {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.getStyleClass().add("ENTRADA".equals(item) ? "badge-green" : "badge-red");
                setGraphic(l); setText(null);
            }
        });

        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "R$ " + String.format("%.2f", item));
            }
        });

        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colData.setCellValueFactory(new PropertyValueFactory<>("realizadaEm"));

        tabelaMov.setItems(movimentacoes);
        tabelaMov.setPlaceholder(new Label("Nenhuma movimentação registrada."));
    }

    // ── Carregar caixa ────────────────────────────────────────────────────────

    private void carregarCaixa() {
        setLoading(true);
        Task<Dto.Caixa> task = new Task<>() {
            @Override protected Dto.Caixa call() {
                // CORREÇÃO: getCaixaAtual() usa CAIXA_GET_ATUAL — sem id
                return caixaService.getCaixaAtual();
            }
        };
        task.setOnSucceeded(e -> {
            caixaAtual = task.getValue();
            atualizarUIcaixa();
            if (caixaAtual != null) {
                carregarMovimentacoes();
            } else {
                setLoading(false);
            }
        });
        task.setOnFailed(e -> {
            setLoading(false);
            atualizarUIcaixa();
        });
        new Thread(task, "load-caixa").start();
    }

    private void atualizarUIcaixa() {
        Platform.runLater(() -> {
            boolean aberto = caixaAtual != null && "ABERTO".equals(caixaAtual.getStatus());

            lblStatus.setText(aberto ? "● Aberto" : "● Fechado");
            lblStatus.setStyle(aberto
                ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

            if (aberto) {
                lblSaldoAtual.setText("R$ " + String.format("%.2f", caixaAtual.getSaldoAtual()));
                lblSaldoInicial.setText("R$ " + String.format("%.2f", caixaAtual.getSaldoInicial()));
                lblAbertura.setText(caixaAtual.getAberturaEm() != null
                    ? caixaAtual.getAberturaEm().substring(0, 16) : "—");
                btnAbrirFechar.setText("Fechar Caixa");
                btnAbrirFechar.getStyleClass().setAll("btn-danger");
            } else {
                lblSaldoAtual.setText("—");
                lblSaldoInicial.setText("—");
                lblAbertura.setText("—");
                btnAbrirFechar.setText("Abrir Caixa");
                btnAbrirFechar.getStyleClass().setAll("btn-primary");
            }

            painelCaixaAberto.setVisible(aberto);
            painelCaixaAberto.setManaged(aberto);
        });
    }

    private void carregarMovimentacoes() {
        Task<List<Dto.MovimentacaoCaixa>> task = new Task<>() {
            @Override protected List<Dto.MovimentacaoCaixa> call() {
                // CORREÇÃO: listarMovimentacoes() usa caixaAtualId interno
                return caixaService.listarMovimentacoes();
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            movimentacoes.setAll(task.getValue());
        });
        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-mov").start();
    }

    // ── Abrir / Fechar caixa ──────────────────────────────────────────────────

    @FXML
    private void toggleCaixa() {
        if (caixaAtual != null && "ABERTO".equals(caixaAtual.getStatus())) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Fechar o caixa atual?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) executarFecharCaixa();
            });
        } else {
            saldoInicialField.clear();
            abrirOverlay.setVisible(true);
            abrirOverlay.setManaged(true);
        }
    }

    @FXML
    private void fecharAbrirOverlay() {
        abrirOverlay.setVisible(false);
        abrirOverlay.setManaged(false);
    }

    @FXML
    private void confirmarAbrirCaixa() {
        double saldo;
        try {
            saldo = Double.parseDouble(saldoInicialField.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertUtil.aviso("Saldo inicial inválido.");
            return;
        }

        fecharAbrirOverlay();
        setLoading(true);
        new Thread(() -> {
            // CORREÇÃO: abrirCaixa captura internamente o id retornado pelo server
            boolean ok = caixaService.abrirCaixa(saldo);
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível abrir o caixa.");
                carregarCaixa();
            });
        }, "abrir-caixa").start();
    }

    private void executarFecharCaixa() {
        setLoading(true);
        new Thread(() -> {
            // CORREÇÃO: fecharCaixa() usa caixaAtualId interno — não precisa passar id
            boolean ok = caixaService.fecharCaixa();
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível fechar o caixa.");
                carregarCaixa();
            });
        }, "fechar-caixa").start();
    }

    // ── Movimentação ──────────────────────────────────────────────────────────

    @FXML
    private void abrirMovimentar() {
        movTipo.setItems(FXCollections.observableArrayList("ENTRADA", "SAIDA"));
        movTipo.getSelectionModel().selectFirst();
        movValor.clear();
        movDesc.clear();
        movOverlay.setVisible(true);
        movOverlay.setManaged(true);
    }

    @FXML
    private void fecharMovOverlay() {
        movOverlay.setVisible(false);
        movOverlay.setManaged(false);
    }

    @FXML
    private void confirmarMovimentacao() {
        String tipo = movTipo.getValue();
        String desc = movDesc.getText().trim();
        double valor;
        try {
            valor = Double.parseDouble(movValor.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertUtil.aviso("Valor inválido.");
            return;
        }
        if (desc.isBlank()) {
            AlertUtil.aviso("Informe a descrição.");
            return;
        }

        fecharMovOverlay();
        setLoading(true);
        new Thread(() -> {
            // CORREÇÃO: movimentar() passa caixaId internamente
            boolean ok = caixaService.movimentar(tipo, valor, desc);
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível registrar a movimentação.");
                carregarCaixa();
            });
        }, "mov-caixa").start();
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
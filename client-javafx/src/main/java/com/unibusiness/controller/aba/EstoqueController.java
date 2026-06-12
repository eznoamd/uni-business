package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.ProdutoService;
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

public class EstoqueController {

    @FXML private TableView<Dto.Produto>         tabelaProdutos;
    @FXML private TableColumn<Dto.Produto, Integer> colId;
    @FXML private TableColumn<Dto.Produto, String>  colNome;
    @FXML private TableColumn<Dto.Produto, String>  colDescricao;
    @FXML private TableColumn<Dto.Produto, Integer> colQtd;
    @FXML private TableColumn<Dto.Produto, Float>   colPreco;
    @FXML private TableColumn<Dto.Produto, Void>    colAcoes;

    @FXML private TextField  buscaField;
    @FXML private Label      lblTotal;
    @FXML private Label      lblBaixoEstoque;

    // Formulário
    @FXML private StackPane  formOverlay;
    @FXML private TextField  fNome;
    @FXML private TextField  fDescricao;
    @FXML private TextField  fQtd;
    @FXML private TextField  fPreco;
    @FXML private Label      formTitulo;

    // Movimentar
    @FXML private StackPane  movOverlay;
    @FXML private Label      movProdutoNome;
    @FXML private ComboBox<String> movTipo;
    @FXML private TextField  movQtd;

    @FXML private StackPane  loadingOverlay;
    @FXML private StackPane  customSpinner;

    private final ProdutoService produtoService = new ProdutoService();
    private ObservableList<Dto.Produto> produtos = FXCollections.observableArrayList();
    private Dto.Produto produtoEditando = null;
    private Dto.Produto produtoMovimentando = null;

    @FXML
    public void initialize() {
        configurarTabela();
        iniciarAnimacaoSpinner();
        configurarBusca();
        carregarProdutos();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));

        colPreco.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "R$ " + String.format("%.2f", item));
            }
        });

        colQtd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label l = new Label(String.valueOf(item));
                l.getStyleClass().add(item < 5 ? "badge-red" : item < 20 ? "badge-orange" : "badge-green");
                setGraphic(l);
                setText(null);
            }
        });

        colAcoes.setCellFactory(col -> new TableCell<>() {
            final Button btnMov  = criarBotao("↕ Mov.", "btn-warning");
            final Button btnEdit = criarBotao("✏ Editar", "btn-ghost");
            final Button btnDel  = criarBotao("✕", "btn-danger");
            final HBox box = new HBox(6, btnMov, btnEdit, btnDel);

            {
                btnMov.setOnAction(e  -> abrirMovimentar(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> abrirFormEdicao(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e  -> confirmarDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tabelaProdutos.setItems(produtos);
        tabelaProdutos.setPlaceholder(new Label("Nenhum produto cadastrado."));
    }

    private Button criarBotao(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        return b;
    }

    private void configurarBusca() {
        buscaField.textProperty().addListener((obs, o, n) -> filtrarProdutos(n));
    }

    private void filtrarProdutos(String termo) {
        if (termo == null || termo.isBlank()) {
            tabelaProdutos.setItems(produtos);
        } else {
            String t = termo.toLowerCase();
            tabelaProdutos.setItems(produtos.filtered(p ->
                p.getNome().toLowerCase().contains(t) ||
                (p.getDescricao() != null && p.getDescricao().toLowerCase().contains(t))
            ));
        }
    }

    private void carregarProdutos() {
        setLoading(true);
        Task<List<Dto.Produto>> task = new Task<>() {
            @Override protected List<Dto.Produto> call() {
                return produtoService.listar();
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            produtos.setAll(task.getValue());
            atualizarKpis();
        });
        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-estoque").start();
    }

    private void atualizarKpis() {
        int total      = produtos.size();
        long baixo     = produtos.stream().filter(p -> p.getQuantidade() < 5).count();
        lblTotal.setText(String.valueOf(total));
        lblBaixoEstoque.setText(String.valueOf(baixo));
    }

    // ── Formulário novo/editar ────────────────────────────────────────────────

    @FXML
    private void abrirFormNovo() {
        produtoEditando = null;
        formTitulo.setText("Novo Produto");
        fNome.clear(); fDescricao.clear(); fQtd.clear(); fPreco.clear();
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    private void abrirFormEdicao(Dto.Produto p) {
        produtoEditando = p;
        formTitulo.setText("Editar Produto");
        fNome.setText(p.getNome());
        fDescricao.setText(p.getDescricao() != null ? p.getDescricao() : "");
        fQtd.setText(String.valueOf(p.getQuantidade()));
        fPreco.setText(String.format("%.2f", p.getPrecoUnitario()).replace(",", "."));
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
    }

    @FXML
    private void fecharForm() {
        formOverlay.setVisible(false);
        formOverlay.setManaged(false);
    }

    @FXML
    private void salvarProduto() {
        String nome = fNome.getText().trim();
        String desc = fDescricao.getText().trim();
        if (nome.isBlank()) { AlertUtil.aviso("Informe o nome do produto."); return; }

        int qtd; float preco;
        try { qtd   = Integer.parseInt(fQtd.getText().trim()); }
        catch (NumberFormatException e) { AlertUtil.aviso("Quantidade inválida."); return; }
        try { preco = Float.parseFloat(fPreco.getText().trim().replace(",", ".")); }
        catch (NumberFormatException e) { AlertUtil.aviso("Preço inválido."); return; }

        setLoading(true);
        fecharForm();

        Dto.Produto editRef = produtoEditando;
        Task<Boolean> task;

        if (editRef == null) {
            task = new Task<>() {
                @Override protected Boolean call() {
                    return produtoService.criar(nome, desc, qtd, preco) != null;
                }
            };
        } else {
            int id = editRef.getId();
            task = new Task<>() {
                @Override protected Boolean call() {
                    return produtoService.atualizar(id, nome, desc, preco);
                }
            };
        }

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (!task.getValue()) AlertUtil.aviso("Não foi possível salvar.");
            carregarProdutos();
        });
        task.setOnFailed(e -> { setLoading(false); AlertUtil.erro("Erro ao salvar produto."); });
        new Thread(task, "salvar-produto").start();
    }

    // ── Movimentar ────────────────────────────────────────────────────────────

    private void abrirMovimentar(Dto.Produto p) {
        produtoMovimentando = p;
        movProdutoNome.setText(p.getNome());
        movTipo.setItems(FXCollections.observableArrayList("ENTRADA", "SAIDA"));
        movTipo.getSelectionModel().selectFirst();
        movQtd.clear();
        movOverlay.setVisible(true);
        movOverlay.setManaged(true);
    }

    @FXML
    private void fecharMov() {
        movOverlay.setVisible(false);
        movOverlay.setManaged(false);
    }

    @FXML
    private void confirmarMovimentacao() {
        String tipo = movTipo.getValue();
        int qtd;
        try { qtd = Integer.parseInt(movQtd.getText().trim()); }
        catch (NumberFormatException e) { AlertUtil.aviso("Quantidade inválida."); return; }
        if (qtd <= 0) { AlertUtil.aviso("Quantidade deve ser maior que zero."); return; }

        setLoading(true);
        fecharMov();
        int pid = produtoMovimentando.getId();

        new Thread(() -> {
            boolean ok = produtoService.movimentarEstoque(pid, tipo, qtd);
            Platform.runLater(() -> {
                setLoading(false);
                if (!ok) AlertUtil.aviso("Não foi possível registrar a movimentação.");
                carregarProdutos();
            });
        }, "movimentar").start();
    }

    private void confirmarDelete(Dto.Produto p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover \"" + p.getNome() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    boolean ok = produtoService.deletar(p.getId());
                    Platform.runLater(() -> {
                        if (!ok) AlertUtil.aviso("Não foi possível remover o produto.");
                        carregarProdutos();
                    });
                }, "delete-produto").start();
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

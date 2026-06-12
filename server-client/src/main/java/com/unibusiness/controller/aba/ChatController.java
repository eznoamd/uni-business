package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.service.ConversaService;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.session.SessionManager;
import com.unibusiness.util.AlertUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller do Chat.
 *
 * ONLINE/OFFLINE E NOVAS MENSAGENS — sem servidor TCP / sem pushes:
 *
 * Antes, o servidor mantinha um socket aberto por usuário e enviava
 * PUSH_MENSAGEM / PUSH_STATUS_USUARIO em tempo real (PushListener/PushRouter/
 * PresenceCache).
 *
 * Agora cada instância do app fala direto com o banco. "online" é só um
 * campo (UsuarioEntity.online) atualizado no login/logout. Para refletir
 * mudanças feitas por OUTRA instância do app (outro usuário enviando
 * mensagem, ficando online/offline), esta tela faz POLLING:
 *
 *   - a cada poucos segundos, recarrega a lista de conversas (atualiza
 *     dots online/offline e contagem de não lidas);
 *   - se uma conversa estiver aberta, recarrega as mensagens e renderiza
 *     só as novas.
 *
 * É uma solução simples (sem sockets, sem listeners globais) — atualiza
 * com um pequeno atraso (POLL_INTERVAL), o que é aceitável para o escopo
 * do trabalho.
 */
public class ChatController {

    private static final Duration POLL_INTERVAL = Duration.seconds(4);

    // ── FXML — lista ─────────────────────────────────────────────────────────
    @FXML private ScrollPane chatListView;
    @FXML private VBox       conversasContainer;

    // ── FXML — criar conversa ─────────────────────────────────────────────────
    @FXML private ScrollPane criarConversaView;
    @FXML private Button     btnPessoal;
    @FXML private Button     btnEquipe;
    @FXML private VBox       nomeGrupoBox;
    @FXML private TextField  nomeGrupoField;
    @FXML private Label      labelParticipantes;
    @FXML private ListView<String> participantesListView;

    // ── FXML — conversa aberta ────────────────────────────────────────────────
    @FXML private HBox       chatTopbar;
    @FXML private Label      chatTitle;
    @FXML private Label      chatStatusLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox       mensagensContainer;
    @FXML private HBox       chatInputBar;
    @FXML private TextField  messageField;

    // ── FXML — loading ────────────────────────────────────────────────────────
    @FXML private StackPane loadingOverlay;
    @FXML private StackPane customSpinner;

    // ── Estado ───────────────────────────────────────────────────────────────
    private final ConversaService conversaService = new ConversaService();
    private final UsuarioService  usuarioService  = new UsuarioService();

    private Integer           usuarioLogadoId;
    private Dto.Conversa      conversaAtual;
    private int                mensagensRenderizadas = 0;
    private List<Dto.Usuario> usuariosDisponiveis;
    private String modoNovaConversa = "PRIVADA";

    private Timeline pollTimeline;

    // ── Inicialização ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        usuarioLogadoId = SessionManager.getInstance().getUsuarioId();

        configurarAutoScroll();
        iniciarAnimacaoSpinner();
        participantesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        carregarConversas();
        iniciarPolling();
    }

    private void configurarAutoScroll() {
        mensagensContainer.heightProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0))
        );
    }

    /**
     * Recarrega periodicamente conversas (dots online/badges) e, se uma
     * conversa estiver aberta, as mensagens dela. Para automaticamente
     * quando a tela não está mais visível (nó sem Scene), evitando
     * polling acumulado ao navegar entre abas.
     */
    private void iniciarPolling() {
        pollTimeline = new Timeline(new KeyFrame(POLL_INTERVAL, e -> {
            if (conversasContainer.getScene() == null) {
                pollTimeline.stop();
                return;
            }
            if (conversaAtual != null) {
                atualizarMensagensNovas();
            } else if (chatListView.isVisible()) {
                carregarConversas();
            }
        }));
        pollTimeline.setCycleCount(Animation.INDEFINITE);
        pollTimeline.play();
    }

    // ── Lista de conversas ────────────────────────────────────────────────────

    private void carregarConversas() {
        setLoading(true);

        Task<List<Dto.Conversa>> task = new Task<>() {
            @Override protected List<Dto.Conversa> call() {
                return conversaService.listarConversas();
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            conversasContainer.getChildren().clear();
            List<Dto.Conversa> lista = task.getValue();
            if (lista == null || lista.isEmpty()) {
                renderizarEstadoVazio();
            } else {
                lista.forEach(this::adicionarItemConversa);
            }
        });

        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-conversas").start();
    }

    private void renderizarEstadoVazio() {
        VBox vbox = new VBox(8);
        vbox.setStyle("-fx-alignment: center; -fx-padding: 40;");
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 32px;");
        Label t = new Label("Nenhuma conversa");
        t.getStyleClass().add("empty-state-title");
        Label s = new Label("Clique em \"+ Nova\" para começar");
        s.getStyleClass().add("empty-state-subtitle");
        vbox.getChildren().addAll(icon, t, s);
        conversasContainer.getChildren().add(vbox);
    }

    private void adicionarItemConversa(Dto.Conversa conversa) {
        boolean isPrivada = "PRIVADA".equals(conversa.tipo);
        boolean online = isPrivada && conversa.online;

        VBox item = new VBox(5);
        item.getStyleClass().add("chat-item");

        HBox header = new HBox(7);
        header.setStyle("-fx-alignment: CENTER_LEFT;");

        if (isPrivada) {
            Circle dot = new Circle(5);
            dot.getStyleClass().add(online ? "dot-online" : "dot-offline");
            header.getChildren().add(dot);
        }

        Label nomeLabel = new Label(resolverNome(conversa));
        nomeLabel.getStyleClass().add("chat-item-title");
        HBox.setHgrow(nomeLabel, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(nomeLabel);

        if (conversa.naoLidas > 0) {
            Label badge = new Label(String.valueOf(conversa.naoLidas));
            badge.getStyleClass().add("chat-item-badge");
            header.getChildren().add(badge);
        }

        HBox sub = new HBox(6);
        sub.setStyle("-fx-alignment: CENTER_LEFT;");

        Label tipoTag = new Label(isPrivada ? "Pessoal" : "Equipe");
        tipoTag.getStyleClass().add(isPrivada ? "tag-pessoal" : "tag-equipe");
        sub.getChildren().add(tipoTag);

        if (isPrivada) {
            Label statusDot = new Label(online ? "● online" : "● offline");
            statusDot.setStyle(online
                ? "-fx-text-fill: #10b981; -fx-font-size: 11px;"
                : "-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            sub.getChildren().add(statusDot);
        }

        item.getChildren().addAll(header, sub);
        item.setOnMouseClicked(e -> abrirConversa(conversa));
        conversasContainer.getChildren().add(item);
    }

    private String resolverNome(Dto.Conversa c) {
        if (c.nome != null && !c.nome.isBlank()) return c.nome;
        return "Conversa #" + c.id;
    }

    // ── Abrir conversa ────────────────────────────────────────────────────────

    private void abrirConversa(Dto.Conversa conversa) {
        this.conversaAtual = conversa;
        this.mensagensRenderizadas = 0;
        mensagensContainer.getChildren().clear();

        chatTitle.setText(resolverNome(conversa));
        atualizarStatusTopbar(conversa);

        mostrarView(Painel.CONVERSA);

        Task<List<Dto.Mensagem>> task = new Task<>() {
            @Override protected List<Dto.Mensagem> call() {
                List<Dto.Mensagem> historico = conversaService.listarMensagens(conversa.id);
                conversaService.marcarComoLida(conversa.id);
                return historico;
            }
        };

        task.setOnSucceeded(e -> {
            renderizarNovasMensagens(task.getValue());
            Platform.runLater(() -> messageField.requestFocus());
        });

        new Thread(task, "load-mensagens").start();
    }

    /**
     * Chamada pelo polling enquanto uma conversa está aberta: busca o
     * histórico de novo e renderiza só as mensagens que ainda não foram
     * exibidas, e marca a conversa como lida.
     */
    private void atualizarMensagensNovas() {
        if (conversaAtual == null) return;
        int conversaId = conversaAtual.id;

        Task<List<Dto.Mensagem>> task = new Task<>() {
            @Override protected List<Dto.Mensagem> call() {
                List<Dto.Mensagem> historico = conversaService.listarMensagens(conversaId);
                conversaService.marcarComoLida(conversaId);
                return historico;
            }
        };
        task.setOnSucceeded(e -> {
            if (conversaAtual == null || conversaAtual.id != conversaId) return;
            renderizarNovasMensagens(task.getValue());
        });
        new Thread(task, "poll-mensagens").start();
    }

    private void renderizarNovasMensagens(List<Dto.Mensagem> historico) {
        if (historico.size() <= mensagensRenderizadas) return;
        historico.subList(mensagensRenderizadas, historico.size()).forEach(msg -> {
            boolean isMe = msg.remetenteId.equals(usuarioLogadoId);
            renderizarMensagem(msg.conteudo, isMe, msg.remetente);
        });
        mensagensRenderizadas = historico.size();
    }

    private void atualizarStatusTopbar(Dto.Conversa conversa) {
        boolean isPrivada = "PRIVADA".equals(conversa.tipo);
        if (!isPrivada) {
            chatStatusLabel.setText("Grupo");
            chatStatusLabel.getStyleClass().setAll("chat-subtitle-offline");
            return;
        }
        boolean online = conversa.online;
        chatStatusLabel.setText(online ? "● online" : "● offline");
        chatStatusLabel.getStyleClass().setAll(online ? "chat-subtitle" : "chat-subtitle-offline");
    }

    // ── Enviar mensagem ───────────────────────────────────────────────────────

    @FXML
    private void enviarMensagem() {
        String texto = messageField.getText();
        if (texto == null || texto.isBlank() || conversaAtual == null) return;

        messageField.clear();
        renderizarMensagem(texto, true, SessionManager.getInstance().getNome());
        mensagensRenderizadas++;

        int convId = conversaAtual.id;
        new Thread(() -> {
            Integer id = conversaService.enviarMensagem(convId, texto);
            if (id == null) {
                Platform.runLater(() ->
                    renderizarMensagem("⚠️ Falha ao enviar.", false, "Sistema"));
            }
        }, "send-msg").start();
    }

    // ── Criar conversa ────────────────────────────────────────────────────────

    @FXML
    private void abrirCriarConversa() {
        participantesListView.getItems().clear();
        nomeGrupoField.clear();
        selecionarModoPessoal();
        carregarUsuariosParaSelecao();
        mostrarView(Painel.CRIAR);
    }

    @FXML private void fecharCriarConversa() { mostrarView(Painel.LISTA); }

    @FXML
    private void selecionarModoPessoal() {
        modoNovaConversa = "PRIVADA";
        btnPessoal.getStyleClass().setAll("tipo-toggle-ativo");
        btnEquipe.getStyleClass().setAll("tipo-toggle-btn");
        nomeGrupoBox.setVisible(false);
        nomeGrupoBox.setManaged(false);
        labelParticipantes.setText("Selecione 1 pessoa");
        participantesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        participantesListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void selecionarModoEquipe() {
        modoNovaConversa = "GRUPO";
        btnEquipe.getStyleClass().setAll("tipo-toggle-ativo");
        btnPessoal.getStyleClass().setAll("tipo-toggle-btn");
        nomeGrupoBox.setVisible(true);
        nomeGrupoBox.setManaged(true);
        labelParticipantes.setText("Selecione os participantes (Ctrl+clique)");
        participantesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        participantesListView.getSelectionModel().clearSelection();
    }

    private void carregarUsuariosParaSelecao() {
        Task<List<Dto.Usuario>> task = new Task<>() {
            @Override protected List<Dto.Usuario> call() {
                return usuarioService.listarUsuarios();
            }
        };
        task.setOnSucceeded(e -> {
            usuariosDisponiveis = task.getValue().stream()
                .filter(u -> !u.id.equals(usuarioLogadoId))
                .collect(Collectors.toList());
            List<String> nomes = usuariosDisponiveis.stream()
                .map(u -> u.nome + "  ·  " + u.email)
                .collect(Collectors.toList());
            Platform.runLater(() ->
                participantesListView.setItems(FXCollections.observableArrayList(nomes))
            );
        });
        new Thread(task, "load-usuarios").start();
    }

    @FXML
    private void criarConversa() {
        List<Integer> selecionados = participantesListView
            .getSelectionModel().getSelectedIndices().stream()
            .map(i -> usuariosDisponiveis.get(i).id)
            .collect(Collectors.toList());

        if ("PRIVADA".equals(modoNovaConversa)) {
            if (selecionados.size() != 1) {
                AlertUtil.aviso("Selecione exatamente 1 pessoa para conversa pessoal.");
                return;
            }
        } else {
            if (selecionados.isEmpty()) { AlertUtil.aviso("Selecione ao menos 1 participante."); return; }
            if (nomeGrupoField.getText().trim().isBlank()) { AlertUtil.aviso("Digite o nome do grupo."); return; }
        }

        setLoading(true);
        String nomeGrupo = nomeGrupoField.getText().trim();
        String tipo = modoNovaConversa;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                return conversaService.criarConversa(tipo, selecionados,
                    "GRUPO".equals(tipo) ? nomeGrupo : null);
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue() != null) { mostrarView(Painel.LISTA); carregarConversas(); }
            else AlertUtil.aviso("Não foi possível criar a conversa.");
        });
        task.setOnFailed(e -> { setLoading(false); AlertUtil.erro("Erro ao criar conversa."); });
        new Thread(task, "criar-conversa").start();
    }

    @FXML
    private void voltarParaLista() {
        mostrarView(Painel.LISTA);
        carregarConversas();
    }

    // ── Navegação de painéis ──────────────────────────────────────────────────

    private enum Painel { LISTA, CRIAR, CONVERSA }

    private void mostrarView(Painel p) {
        chatListView.setVisible(p == Painel.LISTA);
        chatListView.setManaged(p == Painel.LISTA);
        criarConversaView.setVisible(p == Painel.CRIAR);
        criarConversaView.setManaged(p == Painel.CRIAR);
        boolean conv = p == Painel.CONVERSA;
        chatTopbar.setVisible(conv);         chatTopbar.setManaged(conv);
        messagesScrollPane.setVisible(conv); messagesScrollPane.setManaged(conv);
        chatInputBar.setVisible(conv);       chatInputBar.setManaged(conv);
        if (!conv) {
            conversaAtual = null;
            mensagensRenderizadas = 0;
        }
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private void renderizarMensagem(String texto, boolean isMe, String remetente) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().addAll("message", isMe ? "message-me" : "message-other");

        if (!isMe && remetente != null) {
            Label nome = new Label(remetente);
            nome.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                          "-fx-text-fill: #05AD98; -fx-opacity: 0.85;");
            bubble.getChildren().add(nome);
        }

        Label label = new Label(texto);
        label.setWrapText(true);
        bubble.getChildren().add(label);

        HBox wrapper = new HBox();
        wrapper.getStyleClass().add(isMe ? "message-wrapper-me" : "message-wrapper-other");
        wrapper.getChildren().add(bubble);

        Platform.runLater(() -> mensagensContainer.getChildren().add(wrapper));
    }

    private void setLoading(boolean active) {
        Platform.runLater(() -> {
            loadingOverlay.setVisible(active);
            loadingOverlay.setManaged(active);
        });
    }

    private void iniciarAnimacaoSpinner() {
        javafx.animation.RotateTransition rotate =
            new javafx.animation.RotateTransition(javafx.util.Duration.seconds(1), customSpinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(javafx.animation.Animation.INDEFINITE);
        rotate.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rotate.play();
    }
}

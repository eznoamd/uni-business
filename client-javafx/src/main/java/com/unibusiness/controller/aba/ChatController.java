package com.unibusiness.controller.aba;

import com.unibusiness.dto.Dto;
import com.unibusiness.network.PresenceCache;
import com.unibusiness.network.PushListener;
import com.unibusiness.network.PushRouter;
import com.unibusiness.service.ConversaService;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.session.SessionManager;
import com.unibusiness.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller do Chat.
 *
 * CORREÇÃO ONLINE/OFFLINE:
 *
 * Havia dois bugs relacionados:
 *
 * Bug 1 (TcpClient): Pushes de PUSH_STATUS_USUARIO chegavam logo após o login,
 * mas ANTES do DashboardController registrar o PushRouter. Resultado: pushes
 * descartados silenciosamente, PresenceCache nunca populado.
 * -> Corrigido em TcpClient com buffer de pushes pendentes.
 *
 * Bug 2 (ChatController): Mesmo com o TcpClient corrigido, se o usuário nunca
 * recebeu push de um contato (ex: contato estava online ANTES do usuário logar
 * e o snapshot foi perdido), o PresenceCache não tem entrada e usa o fallback
 * conversa.online do CONVERSA_LIST — que pode estar desatualizado.
 * -> Corrigido aqui: ao carregar conversas, populamos o PresenceCache com
 *    o valor retornado pelo servidor via conversa.online, garantindo que
 *    pelo menos o estado no momento do CONVERSA_LIST seja refletido.
 *    Pushes subsequentes continuam atualizando normalmente.
 */
public class ChatController implements PushListener {

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
    private List<Dto.Usuario> usuariosDisponiveis;
    private String modoNovaConversa = "PRIVADA";

    // ── Inicialização ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        PushRouter.getInstance().setCurrentListener(this);

        configurarAutoScroll();
        iniciarAnimacaoSpinner();
        participantesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        carregarConversas();
    }

    private void configurarAutoScroll() {
        mensagensContainer.heightProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0))
        );
    }

    // ── Lista de conversas ────────────────────────────────────────────────────

    private void carregarConversas() {
        conversasContainer.getChildren().clear();
        setLoading(true);

        Task<List<Dto.Conversa>> task = new Task<>() {
            @Override protected List<Dto.Conversa> call() {
                return conversaService.listarConversas();
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            List<Dto.Conversa> lista = task.getValue();
            if (lista == null || lista.isEmpty()) {
                renderizarEstadoVazio();
            } else {
                // CORREÇÃO Bug 2: popula o PresenceCache com o estado vindo do
                // servidor antes de renderizar, para que o fallback seja preciso.
                // Pushes de PUSH_STATUS_USUARIO continuam sobrescrevendo conforme chegam.
                popularPresenceCacheComListaConversas(lista);
                lista.forEach(this::adicionarItemConversa);
            }
        });

        task.setOnFailed(e -> setLoading(false));
        new Thread(task, "load-conversas").start();
    }

    /**
     * CORREÇÃO: Popula o PresenceCache com os valores de conversa.online
     * retornados pelo CONVERSA_LIST. Isso garante que mesmo se os pushes
     * de presença do login foram perdidos (race condition), o cache tem
     * pelo menos o estado atual conforme o servidor.
     *
     * Nota: só atualizamos o cache se NÃO há entrada prévia — pushes
     * recebidos via PUSH_STATUS_USUARIO são mais recentes e têm prioridade.
     */
    private void popularPresenceCacheComListaConversas(List<Dto.Conversa> lista) {
        PresenceCache cache = PresenceCache.getInstance();
        for (Dto.Conversa c : lista) {
            if ("PRIVADA".equals(c.tipo) && c.outroUsuarioId != null) {
                // Só popula se não há entrada — preserva o que foi atualizado por pushes
                // O método isOnline já usa o server fallback, então populamos explicitamente
                // para que futuras consultas não dependam do fallback
                cache.setIfAbsent(c.outroUsuarioId, c.online);
            }
        }
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

        // Lê do PresenceCache global — agora populado tanto por pushes quanto
        // pelo popularPresenceCacheComListaConversas() acima.
        boolean online = false;
        if (isPrivada && conversa.outroUsuarioId != null) {
            online = PresenceCache.getInstance().isOnline(conversa.outroUsuarioId, conversa.online);
        }

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

        final boolean onlineFinal = online;
        item.getChildren().addAll(header, sub);
        item.setOnMouseClicked(e -> abrirConversa(conversa, onlineFinal));
        conversasContainer.getChildren().add(item);
    }

    private String resolverNome(Dto.Conversa c) {
        if (c.nome != null && !c.nome.isBlank()) return c.nome;
        return "Conversa #" + c.id;
    }

    // ── Abrir conversa ────────────────────────────────────────────────────────

    private void abrirConversa(Dto.Conversa conversa, boolean onlineAtual) {
        this.conversaAtual = conversa;
        mensagensContainer.getChildren().clear();

        chatTitle.setText(resolverNome(conversa));
        atualizarStatusTopbar(conversa, onlineAtual);

        mostrarView(Painel.CONVERSA);

        Task<List<Dto.Mensagem>> task = new Task<>() {
            @Override protected List<Dto.Mensagem> call() {
                List<Dto.Mensagem> historico = conversaService.listarMensagens(conversa.id);
                conversaService.marcarComoLida(conversa.id);
                return historico;
            }
        };

        task.setOnSucceeded(e -> {
            task.getValue().forEach(msg -> {
                boolean isMe = msg.remetenteId.equals(usuarioLogadoId);
                renderizarMensagem(msg.conteudo, isMe, msg.remetente);
            });
            Platform.runLater(() -> messageField.requestFocus());
        });

        new Thread(task, "load-mensagens").start();
    }

    private void atualizarStatusTopbar(Dto.Conversa conversa, boolean online) {
        boolean isPrivada = "PRIVADA".equals(conversa.tipo);
        if (!isPrivada) {
            chatStatusLabel.setText("Grupo");
            chatStatusLabel.getStyleClass().setAll("chat-subtitle-offline");
            return;
        }
        if (conversa.outroUsuarioId != null) {
            online = PresenceCache.getInstance().isOnline(conversa.outroUsuarioId, online);
        }
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

    // ── Push ─────────────────────────────────────────────────────────────────

    @Override
    public void onNovaMensagem(Dto.PushMensagem push) {
        if (conversaAtual != null && push.conversaId.equals(conversaAtual.id)) {
            boolean isMe = push.remetenteId.equals(usuarioLogadoId);
            Platform.runLater(() -> renderizarMensagem(push.conteudo, isMe, push.remetente));
        } else {
            Platform.runLater(this::carregarConversas);
        }
    }

    @Override
    public void onNaoLidas(Dto.PushNaoLidas push) {
        Platform.runLater(this::carregarConversas);
    }

    @Override
    public void onMensagemLida(Dto.PushMensagemLida push) {}

    /**
     * O DashboardController (listener global) já atualizou o PresenceCache.
     * Aqui apenas atualizamos a UI do chat se ele estiver visível.
     */
    @Override
    public void onStatusUsuario(Dto.PushStatusUsuario push) {
        Platform.runLater(() -> {
            // 1. Atualiza topbar se a conversa aberta for com esse usuário
            if (conversaAtual != null
                    && "PRIVADA".equals(conversaAtual.tipo)
                    && push.usuarioId.equals(conversaAtual.outroUsuarioId)) {
                conversaAtual.online = push.online;
                chatStatusLabel.setText(push.online ? "● online" : "● offline");
                chatStatusLabel.getStyleClass().setAll(
                    push.online ? "chat-subtitle" : "chat-subtitle-offline");
            }
            // 2. Recarrega lista de conversas se estiver visível (atualiza os dots)
            if (chatListView.isVisible()) {
                carregarConversas();
            }
        });
    }

    @Override
    public void onDigitando(Dto.PushDigitando push) {
        if (conversaAtual == null || !push.conversaId.equals(conversaAtual.id)) return;
        if (push.usuarioId.equals(usuarioLogadoId)) return;

        Platform.runLater(() -> {
            if (push.digitando) {
                chatStatusLabel.setText(push.nome + " está digitando...");
                chatStatusLabel.getStyleClass().setAll("chat-typing-label");
            } else {
                boolean online = conversaAtual.outroUsuarioId != null
                    ? PresenceCache.getInstance().isOnline(conversaAtual.outroUsuarioId, conversaAtual.online)
                    : false;
                chatStatusLabel.setText(online ? "● online" : "● offline");
                chatStatusLabel.getStyleClass().setAll(online ? "chat-subtitle" : "chat-subtitle-offline");
            }
        });
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
        if (!conv) conversaAtual = null;
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
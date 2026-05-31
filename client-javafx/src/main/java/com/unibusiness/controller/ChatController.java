package com.unibusiness.controller;

import com.unibusiness.dto.Dto;
import com.unibusiness.network.PushListener;
import com.unibusiness.network.TcpClient;
import com.unibusiness.service.ConversaService;
import com.unibusiness.session.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

public class ChatController implements PushListener {

    @FXML private VBox      conversasContainer;
    @FXML private VBox      chatListView;
    @FXML private VBox      chatConversationView;
    @FXML private VBox      mensagensContainer;
    @FXML private TextField messageField;
    @FXML private Label     chatTitle;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox      loadingOverlay;
    @FXML private StackPane customSpinner;

    private final ConversaService conversaService = new ConversaService();
    private final TcpClient       client          = TcpClient.getInstance();

    private Integer usuarioLogadoId;

    private Dto.Conversa conversaAtual;

    @FXML
    public void initialize() {
        usuarioLogadoId = SessionManager.getInstance().getUsuarioId();

        client.setPushListener(this);

        setupAutoScroll();
        iniciarAnimacaoDoSpinner();
        carregarConversas();
    }

    private void setupAutoScroll() {
        mensagensContainer.heightProperty().addListener((obs, oldV, newV) ->
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0))
        );
    }

    private void carregarConversas() {
        conversasContainer.getChildren().clear();
        setLoading(true);

        Task<List<Dto.Conversa>> task = new Task<>() {
            @Override
            protected List<Dto.Conversa> call() {
                return conversaService.listarConversas();
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            for (Dto.Conversa conversa : task.getValue()) {
                adicionarItemConversa(conversa);
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            System.err.println("Erro ao carregar conversas: " + task.getException().getMessage());
        });

        new Thread(task, "load-conversas").start();
    }

    private void adicionarItemConversa(Dto.Conversa conversa) {
        VBox item = new VBox();
        item.getStyleClass().add("chat-item");

        Label titulo = new Label(conversa.tipo + " #" + conversa.id);
        titulo.getStyleClass().add("chat-item-title");

        String subtexto = conversa.naoLidas > 0
            ? conversa.naoLidas + " mensagem(ns) não lida(s)"
            : "Nenhuma mensagem nova";

        Label sub = new Label(subtexto);
        sub.getStyleClass().add("chat-item-subtitle");

        // Badge de não lidas
        if (conversa.naoLidas > 0) {
            sub.setStyle("-fx-text-fill: #05AD98; -fx-font-weight: bold;");
        }

        item.getChildren().addAll(titulo, sub);
        item.setOnMouseClicked(e -> abrirConversa(conversa));
        conversasContainer.getChildren().add(item);
    }

    private void abrirConversa(Dto.Conversa conversa) {
        this.conversaAtual = conversa;
        chatTitle.setText(conversa.tipo + " #" + conversa.id);
        mensagensContainer.getChildren().clear();

        chatListView.setVisible(false);
        chatListView.setManaged(false);
        chatConversationView.setVisible(true);
        chatConversationView.setManaged(true);

        Task<List<Dto.Mensagem>> task = new Task<>() {
            @Override
            protected List<Dto.Mensagem> call() {
                List<Dto.Mensagem> historico = conversaService.listarMensagens(conversa.id);
                conversaService.marcarComoLida(conversa.id);
                return historico;
            }
        };

        task.setOnSucceeded(e -> {
            for (Dto.Mensagem msg : task.getValue()) {
                boolean isMe = msg.remetenteId.equals(usuarioLogadoId);
                renderizarMensagem(msg.conteudo, isMe, msg.remetente);
            }
            Platform.runLater(() -> messageField.requestFocus());
        });

        new Thread(task, "load-mensagens").start();
    }

    @FXML
    private void enviarMensagem() {
        String texto = messageField.getText();
        if (texto == null || texto.isBlank() || conversaAtual == null) return;

        messageField.clear();
        renderizarMensagem(texto, true, SessionManager.getInstance().getNome());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Integer msgId = conversaService.enviarMensagem(conversaAtual.id, texto);
                if (msgId == null) {
                    Platform.runLater(() ->
                        renderizarMensagem("⚠️ Falha ao enviar. Tente novamente.", false, "Sistema")
                    );
                }
                return null;
            }
        };

        new Thread(task, "send-msg").start();
    }


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
    public void onMensagemLida(Dto.PushMensagemLida push) {
        // Aqui você pode exibir "✓✓ lido por [nome]" na UI se quiser
        // Por ora apenas loga
        System.out.println("[LIDO] " + push.usuario + " leu a conversa " + push.conversaId);
    }

    @FXML
    private void voltar() {
        conversaAtual = null;
        chatConversationView.setVisible(false);
        chatConversationView.setManaged(false);
        chatListView.setVisible(true);
        chatListView.setManaged(true);
        carregarConversas();
    }

    private void renderizarMensagem(String texto, boolean isMe, String remetente) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().addAll("message", isMe ? "message-me" : "message-other");

        if (!isMe && remetente != null) {
            Label nomeLabel = new Label(remetente);
            nomeLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-opacity: 0.7;");
            bubble.getChildren().add(nomeLabel);
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
        loadingOverlay.setVisible(active);
        loadingOverlay.setManaged(active);
    }

    private void iniciarAnimacaoDoSpinner() {
        javafx.animation.RotateTransition rotate = new javafx.animation.RotateTransition(
            javafx.util.Duration.seconds(1), customSpinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(javafx.animation.Animation.INDEFINITE);
        rotate.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rotate.play();
    }
}

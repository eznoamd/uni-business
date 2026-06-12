package com.unibusiness.controller;

import com.unibusiness.manager.ViewService;
import com.unibusiness.service.ConversaService;
import com.unibusiness.session.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Map;

/**
 * Controller do Dashboard.
 *
 * Antes era o listener GLOBAL do PushRouter (presença online/offline via push).
 * Agora não há mais pushes: presença é um campo no banco (UsuarioEntity.online)
 * atualizado no login/logout (AuthService) e lido com polling onde for exibido
 * (ver ChatController).
 *
 * Também faz polling do total de mensagens não lidas para mostrar a bolinha
 * no item "Chat" da sidebar — assim o usuário vê que tem mensagem nova mesmo
 * sem estar na aba de Chat.
 */
public class DashboardController {

    private static final Duration POLL_INTERVAL = Duration.seconds(4);

    @FXML private BorderPane mainLayout;
    @FXML private Label      profileName;
    @FXML private Label      profileInitial;
    @FXML private Label      profileRole;
    @FXML private Label      chatBadge;
    @FXML private VBox       adminSection;

    private final ConversaService conversaService = new ConversaService();

    @FXML
    private void initialize() {
        ViewService.setMainLayout(mainLayout);
        ViewService.navigateTo("/views/aba/home.fxml");
        popularPerfil();
        aplicarPermissoes();

        atualizarBadgeChat();
        iniciarPollingNaoLidas();
    }

    private void popularPerfil() {
        SessionManager sm = SessionManager.getInstance();
        String nome = sm.getNome();
        if (nome != null && !nome.isBlank()) {
            profileName.setText(nome);
            profileInitial.setText(String.valueOf(nome.charAt(0)).toUpperCase());
        }
        profileRole.setText(sm.isAdmin() ? "Administrador" : "Colaborador");
    }

    /**
     * Esconde a seção "Administração" (Funcionários, Tarefas, Equipes) da
     * sidebar para usuários sem permissão ADMIN_TOTAL. A permissão é
     * verificada uma vez no login (SessionManager.isAdmin()).
     */
    private void aplicarPermissoes() {
        boolean admin = SessionManager.getInstance().isAdmin();
        adminSection.setVisible(admin);
        adminSection.setManaged(admin);
    }

    // ── Badge de mensagens não lidas ─────────────────────────────────────────

    /**
     * O DashboardController fica "vivo" durante toda a sessão (a view é
     * cacheada pelo ViewManager), então um único Timeline aqui é suficiente
     * — não precisa parar/recriar ao navegar entre abas.
     */
    private void iniciarPollingNaoLidas() {
        Timeline timeline = new Timeline(new KeyFrame(POLL_INTERVAL, e -> atualizarBadgeChat()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void atualizarBadgeChat() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() {
                Map<String, Long> naoLidas = conversaService.contarNaoLidas();
                return naoLidas.values().stream().mapToLong(Long::longValue).sum();
            }
        };
        task.setOnSucceeded(e -> {
            long total = task.getValue();
            Platform.runLater(() -> {
                if (total > 0) {
                    chatBadge.setText(total > 99 ? "99+" : String.valueOf(total));
                    chatBadge.setVisible(true);
                    chatBadge.setManaged(true);
                } else {
                    chatBadge.setVisible(false);
                    chatBadge.setManaged(false);
                }
            });
        });
        new Thread(task, "poll-nao-lidas").start();
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    @FXML private void switchToHome()          { ViewService.navigateTo("/views/aba/home.fxml"); }
    @FXML private void switchToMinhasTarefas() { ViewService.navigateTo("/views/aba/minhas-tarefas.fxml"); }
    @FXML private void switchToChat()          { atualizarBadgeChat(); ViewService.navigateTo("/views/aba/chat.fxml"); }
    @FXML private void switchToEstoque()       { ViewService.navigateTo("/views/aba/estoque.fxml"); }
    @FXML private void switchToFinanceiro()    { ViewService.navigateTo("/views/aba/financeiro.fxml"); }
    @FXML private void switchToClientes()      { ViewService.navigateTo("/views/aba/clientes.fxml"); }
    @FXML private void switchToFornecedores()  { ViewService.navigateTo("/views/aba/fornecedores.fxml"); }
    @FXML private void switchToFuncionarios()  { if (exigirAdmin()) ViewService.navigateTo("/views/aba/funcionarios.fxml"); }
    @FXML private void switchToTarefas()       { if (exigirAdmin()) ViewService.navigateTo("/views/aba/tarefas.fxml"); }
    @FXML private void switchToEquipes()       { if (exigirAdmin()) ViewService.navigateTo("/views/aba/equipes.fxml"); }

    /** Segunda barreira além de esconder os botões — garante que a tela de
     *  administração não é aberta por engano caso este método seja chamado
     *  de outro lugar. */
    private boolean exigirAdmin() {
        return SessionManager.getInstance().isAdmin();
    }
}

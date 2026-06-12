package com.unibusiness.controller;

import com.unibusiness.dto.Dto;
import com.unibusiness.manager.ViewService;
import com.unibusiness.network.PresenceCache;
import com.unibusiness.network.PushListener;
import com.unibusiness.network.PushRouter;
import com.unibusiness.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * Controller do Dashboard.
 *
 * É o listener GLOBAL do PushRouter — recebe TODOS os pushes
 * independentemente de qual aba está visível.
 *
 * Responsabilidade de presença:
 *   - onStatusUsuario() atualiza o PresenceCache singleton.
 *   - O ChatController lê do PresenceCache ao renderizar a lista,
 *     portanto dots de online/offline ficam corretos mesmo após navegar
 *     para outras abas e voltar.
 */
public class DashboardController implements PushListener {

    @FXML private BorderPane mainLayout;
    @FXML private Label      profileName;
    @FXML private Label      profileInitial;
    @FXML private Label      profileRole;

    @FXML
    private void initialize() {
        ViewService.setMainLayout(mainLayout);
        ViewService.navigateTo("/views/aba/home.fxml");
        popularPerfil();

        // Registra como listener global — permanece ativo durante toda a sessão
        PushRouter.getInstance().setGlobalListener(this);
    }

    private void popularPerfil() {
        SessionManager sm = SessionManager.getInstance();
        String nome = sm.getNome();
        if (nome != null && !nome.isBlank()) {
            profileName.setText(nome);
            profileInitial.setText(String.valueOf(nome.charAt(0)).toUpperCase());
        }
    }

    // ── PushListener global ───────────────────────────────────────────────────

    @Override
    public void onNovaMensagem(Dto.PushMensagem push) {}

    @Override
    public void onNaoLidas(Dto.PushNaoLidas push) {}

    @Override
    public void onMensagemLida(Dto.PushMensagemLida push) {}

    /**
     * CORREÇÃO PRINCIPAL de online/offline:
     * Atualiza o PresenceCache global a cada push de presença.
     * Quando o ChatController for recriado (ao navegar de volta ao chat),
     * ele lê o estado correto do cache — não o estado desatualizado do servidor.
     */
    @Override
    public void onStatusUsuario(Dto.PushStatusUsuario push) {
        PresenceCache.getInstance().set(push.usuarioId, push.online);
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    @FXML private void switchToHome()          { clearChatListener(); ViewService.navigateTo("/views/aba/home.fxml"); }
    @FXML private void switchToMinhasTarefas() { clearChatListener(); ViewService.navigateTo("/views/aba/minhas-tarefas.fxml"); }

    @FXML
    private void switchToChat() {
        ViewService.navigateTo("/views/aba/chat.fxml");
    }

    @FXML private void switchToEstoque()      { clearChatListener(); ViewService.navigateTo("/views/aba/estoque.fxml"); }
    @FXML private void switchToFinanceiro()   { clearChatListener(); ViewService.navigateTo("/views/aba/financeiro.fxml"); }
    @FXML private void switchToClientes()     { clearChatListener(); ViewService.navigateTo("/views/aba/clientes.fxml"); }
    @FXML private void switchToFornecedores() { clearChatListener(); ViewService.navigateTo("/views/aba/fornecedores.fxml"); }
    @FXML private void switchToFuncionarios() { clearChatListener(); ViewService.navigateTo("/views/aba/funcionarios.fxml"); }
    @FXML private void switchToTarefas()      { clearChatListener(); ViewService.navigateTo("/views/aba/tarefas.fxml"); }
    @FXML private void switchToEquipes()      { clearChatListener(); ViewService.navigateTo("/views/aba/equipes.fxml"); }

    private void clearChatListener() {
        PushRouter.getInstance().clearCurrentListener();
    }
}
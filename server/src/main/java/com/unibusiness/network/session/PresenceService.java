package com.unibusiness.network.session;

import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.response.Response;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Centraliza a lógica de presença (online / offline).
 *
 * CORREÇÕES:
 *  - onLogin: o snapshot enviado ao novo usuário não inclui ele mesmo (já era correto),
 *    mas agora o snapshot é capturado ANTES de registrar a sessão — o register()
 *    é feito no AuthHandler antes de chamar onLogin, então filtramos pelo id.
 *  - onLogout: sem mudanças de lógica, mas deve ser chamado ANTES de sessions.remove().
 *    O cleanup() do ClientHandler agora verifica sessions.isValid() para evitar
 *    chamar onLogout duas vezes quando houve logout explícito.
 */
public final class PresenceService {

    private static final Logger LOG = Logger.getLogger(PresenceService.class.getName());

    private static final PresenceService INSTANCE = new PresenceService();

    private final SessionStore sessions = SessionStore.getInstance();

    private PresenceService() {}

    public static PresenceService getInstance() { return INSTANCE; }

    /**
     * Chamado pelo AuthHandler após registrar a sessão.
     *
     * 1. Envia ao novo usuário quem já está online (exclui ele mesmo).
     * 2. Notifica todos os outros que esse usuário ficou online.
     */
    public void onLogin(ClientSession newSession) {
        Integer myId = newSession.getUsuario().getId();

        // 1. Informa o novo usuário quem está online (snapshot atual excluindo ele)
        sessions.allSessions().stream()
            .filter(s -> !s.getUsuario().getId().equals(myId) && s.isConnected())
            .forEach(s -> newSession.send(buildStatusPush(s, true)));

        // 2. Informa todos os outros que o novo usuário ficou online
        Response myStatus = buildStatusPush(newSession, true);
        sessions.allSessions().stream()
            .filter(s -> !s.getUsuario().getId().equals(myId) && s.isConnected())
            .forEach(s -> {
                try {
                    s.send(myStatus);
                } catch (Exception e) {
                    LOG.warning("Falha ao notificar login: " + e.getMessage());
                }
            });
    }

    /**
     * Chamado pelo ClientHandler (cleanup) ou AuthHandler (logout explícito).
     * Deve ser chamado ANTES de sessions.remove().
     */
    public void onLogout(ClientSession session) {
        if (session == null || session.getUsuario() == null) return;

        Response push = buildStatusPush(session, false);
        Integer myId  = session.getUsuario().getId();

        sessions.allSessions().stream()
            .filter(s -> !s.getUsuario().getId().equals(myId) && s.isConnected())
            .forEach(s -> {
                try {
                    s.send(push);
                } catch (Exception e) {
                    LOG.warning("Falha ao notificar logout: " + e.getMessage());
                }
            });
    }

    private Response buildStatusPush(ClientSession session, boolean online) {
        return Response.push(Actions.PUSH_STATUS_USUARIO, Map.of(
            "usuarioId", session.getUsuario().getId(),
            "nome",      session.getUsuario().getNome(),
            "online",    online
        ));
    }
}

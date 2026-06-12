package com.unibusiness.network.handler;

import com.unibusiness.network.PushService;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.PresenceService;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.ConversaService;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.service.impl.ConversaServiceImpl;
import com.unibusiness.service.impl.UsuarioServiceImpl;
import com.unibusiness.util.JsonUtil;
import com.unibusiness.util.PasswordUtil;

import java.util.Map;

/**
 * Handler de autenticação.
 *
 * LOGIN:
 *  1. Valida credenciais.
 *  2. Registra sessão no SessionStore.
 *  3. Dispara (em virtual thread) presença + push de não lidas.
 *
 * LOGOUT:
 *  1. Chama onLogout (notifica offline) ANTES de remover do store.
 *  2. Remove a sessão do store.
 *  O ClientHandler.cleanup() detectará que o token foi removido e não
 *  repetirá o onLogout quando a conexão fechar.
 */
public class AuthHandler implements ActionHandler {

    private final UsuarioService  usuarioService  = new UsuarioServiceImpl();
    private final ConversaService conversaService = new ConversaServiceImpl();
    private final SessionStore    sessions        = SessionStore.getInstance();
    private final PresenceService presence        = PresenceService.getInstance();

    @Override
    public Response handle(Request request, ClientSession session) {
        return switch (request.getAction()) {
            case Actions.LOGIN  -> login(request, session);
            case Actions.LOGOUT -> logout(request, session);
            default -> Response.error(request.getAction(), "Action não suportada por AuthHandler.");
        };
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    private Response login(Request req, ClientSession tempSession) {
        String email = req.getString("email");
        String senha = req.getString("senha");

        if (email == null || email.isBlank())
            return Response.error(Actions.LOGIN, "Campo 'email' obrigatório.");
        if (senha == null || senha.isBlank())
            return Response.error(Actions.LOGIN, "Campo 'senha' obrigatório.");

        return usuarioService.findByEmail(email).map(usuario -> {
            if (!PasswordUtil.checkPassword(senha, usuario.getSenhaHash()))
                return Response.error(Actions.LOGIN, "Senha incorreta.");

            if (!usuario.getAtivo())
                return Response.error(Actions.LOGIN, "Usuário inativo.");

            ClientSession realSession = new ClientSession(
                tempSession.getSocket(),
                tempSession.getOut(),
                usuario,
                JsonUtil.gson()
            );
            String token = sessions.register(realSession);

            Response loginOk = Response.ok(Actions.LOGIN, "Login realizado com sucesso.", Map.of(
                "token",     token,
                "usuarioId", usuario.getId(),
                "nome",      usuario.getNome(),
                "email",     usuario.getEmail()
            ));

            // Pushes pós-login em virtual thread para não bloquear o envio da resposta
            Thread.ofVirtual().start(() -> {
                Thread.yield();
                presence.onLogin(realSession);
                MensagemHandler.enviarPushNaoLidasAposLogin(realSession, conversaService);
            });

            return loginOk;

        }).orElse(Response.error(Actions.LOGIN, "Usuário não encontrado."));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * CORREÇÃO: onLogout é chamado ANTES de sessions.remove().
     * O onLogout itera allSessions() para notificar os outros usuários —
     * se removermos primeiro, a sessão sai do mapa e a notificação ainda funciona
     * (pois iteramos byUsuario.values() sem o usuário que saiu), mas a ordem
     * correta é: notificar → depois remover. Mantemos assim por clareza.
     */
    private Response logout(Request req, ClientSession session) {
        if (session != null && session.getToken() != null) {
            presence.onLogout(session);
            sessions.remove(session.getToken());
        } else if (req.getToken() != null) {
            sessions.remove(req.getToken());
        }
        return Response.ok(Actions.LOGOUT, "Logout realizado.", null);
    }
}

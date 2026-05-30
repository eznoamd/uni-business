package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.UsuarioRepository;
import com.unibusiness.util.JsonUtil;

import javax.persistence.EntityManager;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;

/**
 * Handles: LOGIN, LOGOUT
 *
 * LOGIN payload:  { "email": "...", "senha": "..." }
 * LOGOUT payload: nenhum (usa token)
 *
 * No LOGIN, o ClientHandler passa o socket e o out para que este handler
 * possa criar um ClientSession real (com UsuarioEntity) e registrá-lo
 * no SessionStore. O token gerado é retornado ao cliente.
 *
 * O ClientHandler recupera o socket do ClientSession temporário via
 * session.getSocket() e session.getOut() — veja como o ClientHandler
 * injeta esses valores antes de chamar o dispatcher.
 */
public class AuthHandler implements ActionHandler {

    private final SessionStore sessions = SessionStore.getInstance();

    @Override
    public Response handle(Request request, ClientSession session) {
        return switch (request.getAction()) {
            case Actions.LOGIN  -> login(request, session);
            case Actions.LOGOUT -> logout(request);
            default -> Response.error(request.getAction(), "Action não suportada por AuthHandler.");
        };
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    private Response login(Request req, ClientSession tempSession) {
        String email = req.getString("email");
        String senha  = req.getString("senha");

        if (email == null || senha == null) {
            return Response.error(Actions.LOGIN, "Campos 'email' e 'senha' obrigatórios.");
        }

        EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager();
        try {
            Optional<UsuarioEntity> opt = new UsuarioRepository(em).findByEmail(email);

            if (opt.isEmpty()) {
                return Response.error(Actions.LOGIN, "Usuário não encontrado.");
            }

            UsuarioEntity usuario = opt.get();

            if (!usuario.getSenhaHash().equals(senha)) {
                return Response.error(Actions.LOGIN, "Senha incorreta.");
            }

            if (!usuario.getAtivo()) {
                return Response.error(Actions.LOGIN, "Usuário inativo.");
            }

            ClientSession realSession = new ClientSession(
                tempSession.getSocket(),
                tempSession.getOut(),
                usuario,
                JsonUtil.gson()
            );

            String token = sessions.register(realSession);

            return Response.ok(Actions.LOGIN, "Login realizado com sucesso.",
                    Map.of(
                        "token",     token,
                        "usuarioId", usuario.getId(),
                        "nome",      usuario.getNome(),
                        "email",     usuario.getEmail()
                    ));
        } finally {
            em.close();
        }
    }

    private Response logout(Request req) {
        sessions.remove(req.getToken());
        return Response.ok(Actions.LOGOUT, "Logout realizado.", null);
    }
}

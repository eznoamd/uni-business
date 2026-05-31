package com.unibusiness.network.handler;

import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.service.impl.UsuarioServiceImpl;
import com.unibusiness.util.JsonUtil;

import java.util.Map;

public class AuthHandler implements ActionHandler {

    private final UsuarioService service  = new UsuarioServiceImpl();
    private final SessionStore   sessions = SessionStore.getInstance();

    @Override
    public Response handle(Request request, ClientSession session) {
        return switch (request.getAction()) {
            case Actions.LOGIN  -> login(request, session);
            case Actions.LOGOUT -> logout(request);
            default -> Response.error(request.getAction(), "Action não suportada por AuthHandler.");
        };
    }

    private Response login(Request req, ClientSession tempSession) {
        String email = req.getString("email");
        String senha = req.getString("senha");

        if (email == null || senha == null)
            return Response.error(Actions.LOGIN, "Campos 'email' e 'senha' obrigatórios.");

        return service.findByEmail(email).map(usuario -> {

            // NOTA: substituir por BCrypt.checkpw(senha, usuario.getSenhaHash()) em produção
            if (!usuario.getSenhaHash().equals(senha))
                return Response.error(Actions.LOGIN, "Senha incorreta.");

            if (!usuario.getAtivo())
                return Response.error(Actions.LOGIN, "Usuário inativo.");

            // Cria sessão real reutilizando o socket/writer já abertos
            ClientSession realSession = new ClientSession(
                tempSession.getSocket(),
                tempSession.getOut(),
                usuario,
                JsonUtil.gson()
            );
            String token = sessions.register(realSession);

            return Response.ok(Actions.LOGIN, "Login realizado com sucesso.", Map.of(
                "token",     token,
                "usuarioId", usuario.getId(),
                "nome",      usuario.getNome(),
                "email",     usuario.getEmail()
            ));

        }).orElse(Response.error(Actions.LOGIN, "Usuário não encontrado."));
    }

    private Response logout(Request req) {
        sessions.remove(req.getToken());
        return Response.ok(Actions.LOGOUT, "Logout realizado.", null);
    }
}

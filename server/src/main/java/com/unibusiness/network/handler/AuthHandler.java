package com.unibusiness.network.handler;

import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
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

public class AuthHandler implements ActionHandler {

    private final UsuarioService  usuarioService  = new UsuarioServiceImpl();
    private final ConversaService conversaService = new ConversaServiceImpl();
    private final SessionStore    sessions        = SessionStore.getInstance();

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

            Thread.ofVirtual().start(() -> {
                Thread.yield();
                MensagemHandler.enviarPushNaoLidasAposLogin(realSession, conversaService);
            });

            return loginOk;

        }).orElse(Response.error(Actions.LOGIN, "Usuário não encontrado."));
    }

    private Response logout(Request req) {
        sessions.remove(req.getToken());
        return Response.ok(Actions.LOGOUT, "Logout realizado.", null);
    }
}
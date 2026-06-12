package com.unibusiness.network.handler;

import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.service.impl.UsuarioServiceImpl;
import com.unibusiness.util.PasswordUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UsuarioHandler implements ActionHandler {

    private final UsuarioService service = new UsuarioServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.USUARIO_CREATE -> create(req);
            case Actions.USUARIO_LIST   -> list();
            case Actions.USUARIO_GET    -> get(req);
            case Actions.USUARIO_UPDATE -> update(req);
            case Actions.USUARIO_DELETE -> delete(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response create(Request req) {
        String nome  = req.getString("nome");
        String email = req.getString("email");
        String senha = req.getString("senha");

        if (nome == null || email == null || senha == null)
            return Response.error(Actions.USUARIO_CREATE, "Campos 'nome', 'email' e 'senha' obrigatórios.");

        String senhaHash = PasswordUtil.hashPassword(senha);
        UsuarioEntity saved = service.create(new UsuarioEntity(nome, email, senhaHash));
        return Response.ok(Actions.USUARIO_CREATE, toMap(saved));
    }

    private Response list() {
        List<Map<String, Object>> lista = service.listAll().stream()
            .map(this::toMap).collect(Collectors.toList());
        return Response.ok(Actions.USUARIO_LIST, lista);
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(u -> Response.ok(Actions.USUARIO_GET, toMap(u)))
            .orElse(Response.error(Actions.USUARIO_GET, "Usuário não encontrado."));
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_UPDATE, "Campo 'id' obrigatório.");

        return service.findById(id).map(u -> {
            if (req.getString("nome")  != null) u.setNome(req.getString("nome"));
            if (req.getString("email") != null) u.setEmail(req.getString("email"));
            if (req.getString("senha") != null) u.setSenhaHash(PasswordUtil.hashPassword(req.getString("senha")));
            if (req.get("ativo")       != null) u.setAtivo(Boolean.parseBoolean(req.getString("ativo")));
            return Response.ok(Actions.USUARIO_UPDATE, toMap(service.update(u)));
        }).orElse(Response.error(Actions.USUARIO_UPDATE, "Usuário não encontrado."));
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_DELETE, "Campo 'id' obrigatório.");

        return service.findById(id).map(u -> {
            u.setAtivo(false);
            service.update(u);
            return Response.ok(Actions.USUARIO_DELETE, "Usuário desativado.", null);
        }).orElse(Response.error(Actions.USUARIO_DELETE, "Usuário não encontrado."));
    }

    private Map<String, Object> toMap(UsuarioEntity u) {
        return Map.of(
            "id",    u.getId(),
            "nome",  u.getNome(),
            "email", u.getEmail(),
            "ativo", u.getAtivo()
        );
    }
}

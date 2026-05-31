package com.unibusiness.network.handler;

import com.unibusiness.model.EquipeEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.EquipeService;
import com.unibusiness.service.impl.EquipeServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EquipeHandler implements ActionHandler {

    private final EquipeService service = new EquipeServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.EQUIPE_CREATE -> create(req);
            case Actions.EQUIPE_LIST   -> list();
            case Actions.EQUIPE_GET    -> get(req);
            case Actions.EQUIPE_UPDATE -> update(req);
            case Actions.EQUIPE_DELETE -> delete(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response create(Request req) {
        String nome = req.getString("nome");
        if (nome == null) return Response.error(Actions.EQUIPE_CREATE, "Campo 'nome' obrigatório.");
        return Response.ok(Actions.EQUIPE_CREATE, toMap(service.create(new EquipeEntity(nome))));
    }

    private Response list() {
        List<Map<String, Object>> lista = service.listAll().stream().map(this::toMap).collect(Collectors.toList());
        return Response.ok(Actions.EQUIPE_LIST, lista);
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(e -> Response.ok(Actions.EQUIPE_GET, toMap(e)))
            .orElse(Response.error(Actions.EQUIPE_GET, "Equipe não encontrada."));
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_UPDATE, "Campo 'id' obrigatório.");
        return service.findById(id).map(e -> {
            if (req.getString("nome") != null) e.setNome(req.getString("nome"));
            return Response.ok(Actions.EQUIPE_UPDATE, toMap(service.update(e)));
        }).orElse(Response.error(Actions.EQUIPE_UPDATE, "Equipe não encontrada."));
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_DELETE, "Campo 'id' obrigatório.");
        try {
            service.delete(id);
            return Response.ok(Actions.EQUIPE_DELETE, "Equipe removida.", null);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.EQUIPE_DELETE, e.getMessage());
        }
    }

    private Map<String, Object> toMap(EquipeEntity e) {
        return Map.of("id", e.getId(), "nome", e.getNome());
    }
}

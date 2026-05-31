package com.unibusiness.network.handler;

import com.unibusiness.model.CargoEntity;
import com.unibusiness.model.PermissaoEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.CargoService;
import com.unibusiness.service.impl.CargoServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CargoHandler implements ActionHandler {

    private final CargoService service = new CargoServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CARGO_CREATE     -> createCargo(req);
            case Actions.CARGO_LIST       -> listCargo();
            case Actions.CARGO_GET        -> getCargo(req);
            case Actions.CARGO_DELETE     -> deleteCargo(req);
            case Actions.PERMISSAO_CREATE -> createPermissao(req);
            case Actions.PERMISSAO_LIST   -> listPermissao();
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response createCargo(Request req) {
        String nome = req.getString("nome");
        if (nome == null) return Response.error(Actions.CARGO_CREATE, "Campo 'nome' obrigatório.");
        CargoEntity c = service.create(new CargoEntity(nome));
        return Response.ok(Actions.CARGO_CREATE, Map.of("id", c.getId(), "nome", c.getNome()));
    }

    private Response listCargo() {
        List<Map<String, Object>> lista = service.listAll().stream()
            .map(c -> Map.<String, Object>of("id", c.getId(), "nome", c.getNome()))
            .collect(Collectors.toList());
        return Response.ok(Actions.CARGO_LIST, lista);
    }

    private Response getCargo(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CARGO_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(c -> Response.ok(Actions.CARGO_GET, Map.of("id", c.getId(), "nome", c.getNome())))
            .orElse(Response.error(Actions.CARGO_GET, "Cargo não encontrado."));
    }

    private Response deleteCargo(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CARGO_DELETE, "Campo 'id' obrigatório.");
        try {
            service.delete(id);
            return Response.ok(Actions.CARGO_DELETE, "Cargo removido.", null);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CARGO_DELETE, e.getMessage());
        }
    }

    private Response createPermissao(Request req) {
        String nome = req.getString("nome");
        if (nome == null) return Response.error(Actions.PERMISSAO_CREATE, "Campo 'nome' obrigatório.");
        PermissaoEntity p = service.createPermissao(new PermissaoEntity(nome));
        return Response.ok(Actions.PERMISSAO_CREATE, Map.of("id", p.getId(), "nome", p.getNome()));
    }

    private Response listPermissao() {
        List<Map<String, Object>> lista = service.listAllPermissoes().stream()
            .map(p -> Map.<String, Object>of("id", p.getId(), "nome", p.getNome()))
            .collect(Collectors.toList());
        return Response.ok(Actions.PERMISSAO_LIST, lista);
    }
}

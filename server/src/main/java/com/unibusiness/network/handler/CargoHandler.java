package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.CargoEntity;
import com.unibusiness.model.PermissaoEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.GenericRepository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CargoHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CARGO_CREATE      -> createCargo(req);
            case Actions.CARGO_LIST        -> listCargo();
            case Actions.CARGO_GET         -> getCargo(req);
            case Actions.CARGO_DELETE      -> deleteCargo(req);
            case Actions.PERMISSAO_CREATE  -> createPermissao(req);
            case Actions.PERMISSAO_LIST    -> listPermissao();
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response createCargo(Request req) {
        String nome = req.getString("nome");
        if (nome == null) return Response.error(Actions.CARGO_CREATE, "Campo 'nome' obrigatório.");
        EntityManager em = em();
        try {
            CargoEntity c = new GenericRepository<>(CargoEntity.class, em).save(new CargoEntity(nome));
            return Response.ok(Actions.CARGO_CREATE, Map.of("id", c.getId(), "nome", c.getNome()));
        } finally { em.close(); }
    }

    private Response listCargo() {
        EntityManager em = em();
        try {
            List<Map<String,Object>> l = new GenericRepository<>(CargoEntity.class, em)
                .findAll().stream()
                .map(c -> Map.<String,Object>of("id", c.getId(), "nome", c.getNome()))
                .collect(Collectors.toList());
            return Response.ok(Actions.CARGO_LIST, l);
        } finally { em.close(); }
    }

    private Response getCargo(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CARGO_GET, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            CargoEntity c = em.find(CargoEntity.class, id);
            if (c == null) return Response.error(Actions.CARGO_GET, "Cargo não encontrado.");
            return Response.ok(Actions.CARGO_GET, Map.of("id", c.getId(), "nome", c.getNome()));
        } finally { em.close(); }
    }

    private Response deleteCargo(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CARGO_DELETE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<CargoEntity> repo = new GenericRepository<>(CargoEntity.class, em);
            CargoEntity c = repo.findById(id);
            if (c == null) return Response.error(Actions.CARGO_DELETE, "Cargo não encontrado.");
            repo.delete(c);
            return Response.ok(Actions.CARGO_DELETE, "Cargo removido.", null);
        } finally { em.close(); }
    }

    private Response createPermissao(Request req) {
        String nome = req.getString("nome");
        if (nome == null) return Response.error(Actions.PERMISSAO_CREATE, "Campo 'nome' obrigatório.");
        EntityManager em = em();
        try {
            PermissaoEntity p = new GenericRepository<>(PermissaoEntity.class, em).save(new PermissaoEntity(nome));
            return Response.ok(Actions.PERMISSAO_CREATE, Map.of("id", p.getId(), "nome", p.getNome()));
        } finally { em.close(); }
    }

    private Response listPermissao() {
        EntityManager em = em();
        try {
            List<Map<String,Object>> l = new GenericRepository<>(PermissaoEntity.class, em)
                .findAll().stream()
                .map(p -> Map.<String,Object>of("id", p.getId(), "nome", p.getNome()))
                .collect(Collectors.toList());
            return Response.ok(Actions.PERMISSAO_LIST, l);
        } finally { em.close(); }
    }

    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }
}

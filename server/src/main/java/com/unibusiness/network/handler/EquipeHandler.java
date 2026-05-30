package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.EquipeEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.GenericRepository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EquipeHandler implements ActionHandler {

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
        EntityManager em = em();
        try {
            EquipeEntity e = new GenericRepository<>(EquipeEntity.class, em).save(new EquipeEntity(nome));
            return Response.ok(Actions.EQUIPE_CREATE, toMap(e));
        } finally { em.close(); }
    }

    private Response list() {
        EntityManager em = em();
        try {
            List<Map<String,Object>> l = new GenericRepository<>(EquipeEntity.class, em)
                .findAll().stream().map(this::toMap).collect(Collectors.toList());
            return Response.ok(Actions.EQUIPE_LIST, l);
        } finally { em.close(); }
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_GET, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            EquipeEntity e = em.find(EquipeEntity.class, id);
            if (e == null) return Response.error(Actions.EQUIPE_GET, "Equipe não encontrada.");
            return Response.ok(Actions.EQUIPE_GET, toMap(e));
        } finally { em.close(); }
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_UPDATE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<EquipeEntity> repo = new GenericRepository<>(EquipeEntity.class, em);
            EquipeEntity e = repo.findById(id);
            if (e == null) return Response.error(Actions.EQUIPE_UPDATE, "Equipe não encontrada.");
            if (req.getString("nome") != null) e.setNome(req.getString("nome"));
            return Response.ok(Actions.EQUIPE_UPDATE, toMap(repo.save(e)));
        } finally { em.close(); }
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.EQUIPE_DELETE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<EquipeEntity> repo = new GenericRepository<>(EquipeEntity.class, em);
            EquipeEntity e = repo.findById(id);
            if (e == null) return Response.error(Actions.EQUIPE_DELETE, "Equipe não encontrada.");
            repo.delete(e);
            return Response.ok(Actions.EQUIPE_DELETE, "Equipe removida.", null);
        } finally { em.close(); }
    }

    private Map<String,Object> toMap(EquipeEntity e) { return Map.of("id", e.getId(), "nome", e.getNome()); }
    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }
}

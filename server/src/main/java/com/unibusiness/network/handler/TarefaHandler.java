package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.*;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.GenericRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles: TAREFA_CREATE, TAREFA_LIST, TAREFA_GET, TAREFA_UPDATE, TAREFA_DELETE
 *
 * TAREFA_CREATE payload:
 *   { "titulo": "...", "status": "ABERTA", "prioridade": "ALTA",
 *     "dataInicio": "2024-01-01T08:00:00", "descricao": "..." }
 */
public class TarefaHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.TAREFA_CREATE -> create(req, session);
            case Actions.TAREFA_LIST   -> list();
            case Actions.TAREFA_GET    -> get(req);
            case Actions.TAREFA_UPDATE -> update(req);
            case Actions.TAREFA_DELETE -> delete(req);
            default -> Response.error(req.getAction(), "Action não suportada por TarefaHandler.");
        };
    }

    private Response create(Request req, ClientSession session) {
        String titulo    = req.getString("titulo");
        String status    = req.getString("status");
        String prioridade = req.getString("prioridade");
        String dataInicioStr = req.getString("dataInicio");

        if (titulo == null || status == null || prioridade == null || dataInicioStr == null)
            return Response.error(Actions.TAREFA_CREATE, "Campos 'titulo', 'status', 'prioridade' e 'dataInicio' obrigatórios.");

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UsuarioEntity criador = em.find(UsuarioEntity.class, session.getUsuario().getId());
            LocalDateTime dataInicio = LocalDateTime.parse(dataInicioStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            TarefaEntity t = new TarefaEntity(titulo, status, prioridade, dataInicio, criador);
            if (req.getString("descricao") != null) t.setDescricao(req.getString("descricao"));
            em.persist(t);
            tx.commit();
            return Response.ok(Actions.TAREFA_CREATE, toMap(t));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.TAREFA_CREATE, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response list() {
        EntityManager em = em();
        try {
            List<Map<String,Object>> l = new GenericRepository<>(TarefaEntity.class, em)
                .findAll().stream().map(this::toMap).collect(Collectors.toList());
            return Response.ok(Actions.TAREFA_LIST, l);
        } finally { em.close(); }
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_GET, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            TarefaEntity t = em.find(TarefaEntity.class, id);
            if (t == null) return Response.error(Actions.TAREFA_GET, "Tarefa não encontrada.");
            return Response.ok(Actions.TAREFA_GET, toMap(t));
        } finally { em.close(); }
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_UPDATE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            TarefaEntity t = em.find(TarefaEntity.class, id);
            if (t == null) { tx.rollback(); return Response.error(Actions.TAREFA_UPDATE, "Tarefa não encontrada."); }
            if (req.getString("titulo")    != null) t.setTitulo(req.getString("titulo"));
            if (req.getString("status")    != null) t.setStatus(req.getString("status"));
            if (req.getString("prioridade")!= null) t.setPrioridade(req.getString("prioridade"));
            if (req.getString("descricao") != null) t.setDescricao(req.getString("descricao"));
            if (req.getString("dataFim")   != null)
                t.setDataFim(LocalDateTime.parse(req.getString("dataFim"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            em.merge(t);
            tx.commit();
            return Response.ok(Actions.TAREFA_UPDATE, toMap(t));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.TAREFA_UPDATE, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_DELETE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<TarefaEntity> repo = new GenericRepository<>(TarefaEntity.class, em);
            TarefaEntity t = repo.findById(id);
            if (t == null) return Response.error(Actions.TAREFA_DELETE, "Tarefa não encontrada.");
            repo.delete(t);
            return Response.ok(Actions.TAREFA_DELETE, "Tarefa removida.", null);
        } finally { em.close(); }
    }

    private Map<String,Object> toMap(TarefaEntity t) {
        Map<String,Object> m = new java.util.HashMap<>();
        m.put("id",        t.getId());
        m.put("titulo",    t.getTitulo());
        m.put("status",    t.getStatus());
        m.put("prioridade",t.getPrioridade());
        m.put("descricao", t.getDescricao());
        m.put("dataInicio",t.getDataInicio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("dataFim",   t.getDataFim() != null ? t.getDataFim().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return m;
    }

    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }
}

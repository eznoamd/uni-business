package com.unibusiness.network.handler;

import com.unibusiness.model.TarefaEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.TarefaService;
import com.unibusiness.service.impl.TarefaServiceImpl;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TarefaHandler implements ActionHandler {

    private final TarefaService service = new TarefaServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.TAREFA_CREATE -> create(req, session);
            case Actions.TAREFA_LIST   -> list();
            case Actions.TAREFA_GET    -> get(req);
            case Actions.TAREFA_UPDATE -> update(req);
            case Actions.TAREFA_DELETE -> delete(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response create(Request req, ClientSession session) {
        String titulo     = req.getString("titulo");
        String status     = req.getString("status");
        String prioridade = req.getString("prioridade");
        String dataStr    = req.getString("dataInicio");

        if (titulo == null || status == null || prioridade == null || dataStr == null)
            return Response.error(Actions.TAREFA_CREATE, "Campos 'titulo', 'status', 'prioridade' e 'dataInicio' obrigatórios.");

        try {
            TarefaEntity t = new TarefaEntity(
                titulo, status, prioridade,
                LocalDateTime.parse(dataStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                session.getUsuario()
            );
            if (req.getString("descricao") != null) t.setDescricao(req.getString("descricao"));
            return Response.ok(Actions.TAREFA_CREATE, toMap(service.create(t)));
        } catch (Exception e) {
            return Response.error(Actions.TAREFA_CREATE, "Erro: " + e.getMessage());
        }
    }

    private Response list() {
        List<Map<String, Object>> lista = service.listAll().stream().map(this::toMap).collect(Collectors.toList());
        return Response.ok(Actions.TAREFA_LIST, lista);
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(t -> Response.ok(Actions.TAREFA_GET, toMap(t)))
            .orElse(Response.error(Actions.TAREFA_GET, "Tarefa não encontrada."));
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_UPDATE, "Campo 'id' obrigatório.");
        return service.findById(id).map(t -> {
            if (req.getString("titulo")     != null) t.setTitulo(req.getString("titulo"));
            if (req.getString("status")     != null) t.setStatus(req.getString("status"));
            if (req.getString("prioridade") != null) t.setPrioridade(req.getString("prioridade"));
            if (req.getString("descricao")  != null) t.setDescricao(req.getString("descricao"));
            if (req.getString("dataFim")    != null)
                t.setDataFim(LocalDateTime.parse(req.getString("dataFim"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return Response.ok(Actions.TAREFA_UPDATE, toMap(service.update(t)));
        }).orElse(Response.error(Actions.TAREFA_UPDATE, "Tarefa não encontrada."));
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_DELETE, "Campo 'id' obrigatório.");
        try {
            service.delete(id);
            return Response.ok(Actions.TAREFA_DELETE, "Tarefa removida.", null);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.TAREFA_DELETE, e.getMessage());
        }
    }

    private Map<String, Object> toMap(TarefaEntity t) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id",         t.getId());
        m.put("titulo",     t.getTitulo());
        m.put("status",     t.getStatus());
        m.put("prioridade", t.getPrioridade());
        m.put("descricao",  t.getDescricao());
        m.put("dataInicio", t.getDataInicio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("dataFim",    t.getDataFim() != null ? t.getDataFim().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return m;
    }
}

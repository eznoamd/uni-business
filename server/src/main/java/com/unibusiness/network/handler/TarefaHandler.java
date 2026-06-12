package com.unibusiness.network.handler;

import com.unibusiness.model.TarefaEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.TarefaService;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.service.impl.TarefaServiceImpl;
import com.unibusiness.service.impl.UsuarioServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler de Tarefas.
 *
 * CORREÇÕES:
 *
 * 1. create():
 *    - "dataInicio" agora usa LocalDateTime.now() se não enviado (em vez de
 *      retornar erro 400). O cliente novo envia dataInicio, mas garantimos
 *      compatibilidade.
 *    - "status" agora usa "PENDENTE" como padrão se não enviado.
 *    - "dataFim" aceita "AAAA-MM-DD" além de ISO datetime completo.
 *    - "responsavelId" é processado: busca o usuário e adiciona à tarefa.
 *
 * 2. toMap():
 *    - Inclui "responsavel" (nome) e "responsavelId" no retorno para que
 *      o Dto.Tarefa do cliente exiba o responsável na tabela.
 *    - "dataFim" retorna apenas a data (AAAA-MM-DD) para o cliente.
 */
public class TarefaHandler implements ActionHandler {

    private static final DateTimeFormatter ISO      = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TarefaService  service        = new TarefaServiceImpl();
    private final UsuarioService usuarioService = new UsuarioServiceImpl();

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

    // ── create ────────────────────────────────────────────────────────────────

    private Response create(Request req, ClientSession session) {
        String titulo = req.getString("titulo");
        if (titulo == null || titulo.isBlank())
            return Response.error(Actions.TAREFA_CREATE, "Campo 'titulo' obrigatório.");

        // CORREÇÃO: status e dataInicio têm valores padrão
        String status     = req.getString("status") != null ? req.getString("status") : "PENDENTE";
        String prioridade = req.getString("prioridade") != null ? req.getString("prioridade") : "BAIXA";

        // CORREÇÃO: dataInicio usa now() se não enviado
        LocalDateTime dataInicio;
        String dataInicioStr = req.getString("dataInicio");
        if (dataInicioStr != null && !dataInicioStr.isBlank()) {
            try {
                dataInicio = LocalDateTime.parse(dataInicioStr, ISO);
            } catch (Exception e) {
                dataInicio = LocalDateTime.now();
            }
        } else {
            dataInicio = LocalDateTime.now();
        }

        try {
            TarefaEntity t = new TarefaEntity(titulo, status, prioridade, dataInicio, session.getUsuario());

            if (req.getString("descricao") != null) t.setDescricao(req.getString("descricao"));

            // CORREÇÃO: aceita dataFim em formato "AAAA-MM-DD" ou ISO datetime
            String dataFimStr = req.getString("dataFim");
            if (dataFimStr != null && !dataFimStr.isBlank()) {
                LocalDateTime dataFim = parseDatetime(dataFimStr);
                if (dataFim != null) t.setDataFim(dataFim);
            }

            TarefaEntity saved = service.create(t);

            // CORREÇÃO: associa responsável se informado
            Integer responsavelId = req.getInteger("responsavelId");
            if (responsavelId != null) {
                usuarioService.findById(responsavelId).ifPresent(u -> {
                    // Adiciona à lista de usuáriosAtribuídos
                    saved.getUsuariosAtribuidos().add(u);
                    service.update(saved);
                });
            }

            return Response.ok(Actions.TAREFA_CREATE, toMap(saved));
        } catch (Exception e) {
            return Response.error(Actions.TAREFA_CREATE, "Erro: " + e.getMessage());
        }
    }

    // ── list ──────────────────────────────────────────────────────────────────

    private Response list() {
        List<Map<String, Object>> lista = service.listAll().stream()
            .map(this::toMap)
            .collect(Collectors.toList());
        return Response.ok(Actions.TAREFA_LIST, lista);
    }

    // ── get ───────────────────────────────────────────────────────────────────

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(t -> Response.ok(Actions.TAREFA_GET, toMap(t)))
            .orElse(Response.error(Actions.TAREFA_GET, "Tarefa não encontrada."));
    }

    // ── update ────────────────────────────────────────────────────────────────

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.TAREFA_UPDATE, "Campo 'id' obrigatório.");

        return service.findById(id).map(t -> {
            if (req.getString("titulo")     != null) t.setTitulo(req.getString("titulo"));
            if (req.getString("status")     != null) t.setStatus(req.getString("status"));
            if (req.getString("prioridade") != null) t.setPrioridade(req.getString("prioridade"));
            if (req.getString("descricao")  != null) t.setDescricao(req.getString("descricao"));

            // CORREÇÃO: aceita "AAAA-MM-DD" ou ISO datetime
            String dataFimStr = req.getString("dataFim");
            if (dataFimStr != null && !dataFimStr.isBlank()) {
                LocalDateTime dataFim = parseDatetime(dataFimStr);
                if (dataFim != null) t.setDataFim(dataFim);
            }

            return Response.ok(Actions.TAREFA_UPDATE, toMap(service.update(t)));
        }).orElse(Response.error(Actions.TAREFA_UPDATE, "Tarefa não encontrada."));
    }

    // ── delete ────────────────────────────────────────────────────────────────

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

    // ── toMap ─────────────────────────────────────────────────────────────────

    /**
     * CORREÇÃO: inclui "responsavel" (nome) e "responsavelId" para o cliente.
     * "dataFim" retorna apenas "AAAA-MM-DD" (sem horário) para o cliente exibir.
     */
    private Map<String, Object> toMap(TarefaEntity t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",         t.getId());
        m.put("titulo",     t.getTitulo());
        m.put("status",     t.getStatus());
        m.put("prioridade", t.getPrioridade());
        m.put("descricao",  t.getDescricao());
        m.put("dataInicio", t.getDataInicio().format(ISO));
        m.put("dataFim",    t.getDataFim() != null
            ? t.getDataFim().format(DATE_FMT)   // CORREÇÃO: apenas data para o cliente
            : null);

        // CORREÇÃO: popula responsável a partir de usuáriosAtribuídos
        if (!t.getUsuariosAtribuidos().isEmpty()) {
            UsuarioEntity resp = t.getUsuariosAtribuidos().iterator().next();
            m.put("responsavelId", resp.getId());
            m.put("responsavel",   resp.getNome());
        } else {
            // Criador como fallback
            m.put("responsavelId", t.getCriadoPor().getId());
            m.put("responsavel",   t.getCriadoPor().getNome());
        }

        return m;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Aceita "AAAA-MM-DD" ou "AAAA-MM-DDTHH:mm:ss".
     */
    private LocalDateTime parseDatetime(String s) {
        try {
            if (s.length() == 10) {
                return LocalDateTime.parse(s + "T00:00:00", ISO);
            }
            return LocalDateTime.parse(s, ISO);
        } catch (Exception e) {
            return null;
        }
    }
}
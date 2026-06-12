package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de tarefas — cliente.
 *
 * CORREÇÕES:
 *
 * 1. criar():
 *    O TarefaHandler no servidor exige os campos:
 *      - titulo     (obrigatório)
 *      - status     (obrigatório) — o client não enviava; padrão "PENDENTE"
 *      - prioridade (obrigatório)
 *      - dataInicio (obrigatório, formato ISO: "2025-01-15T00:00:00") — não enviado antes
 *
 *    O campo "dataFim" é OPCIONAL e deve ser ISO datetime, não apenas data.
 *
 * 2. atualizar():
 *    O TarefaHandler.update() chama service.update(t) mas o TAREFA_UPDATE
 *    do server aceita: id, titulo, status, prioridade, descricao, dataFim.
 *    O client enviava apenas id, status, prioridade → ok, mas "dataFim"
 *    exige formato ISO datetime ("2025-12-31T00:00:00"), não apenas "2025-12-31".
 *
 * 3. Dto.Tarefa:
 *    O servidor retorna "responsavel" como null (não existe responsavelNome no
 *    TarefaHandler.toMap()). O campo "responsavel" em Dto.Tarefa permanece null
 *    — isso é esperado com o servidor atual.
 */
public class TarefaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TcpClient client = TcpClient.getInstance();
    private final Gson      gson   = client.getGson();

    // ── Listar ────────────────────────────────────────────────────────────────

    public List<Dto.Tarefa> listar() {
        ServerResponse resp = client.send("TAREFA_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.Tarefa>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }

    // ── Criar ─────────────────────────────────────────────────────────────────

    /**
     * Cria uma tarefa nova.
     *
     * @param titulo       título da tarefa (obrigatório)
     * @param descricao    descrição (opcional, pode ser null ou vazio)
     * @param prioridade   "BAIXA", "MEDIA" ou "ALTA"
     * @param dataFim      prazo no formato "AAAA-MM-DD" (opcional, pode ser null)
     * @param responsavelId id do usuário responsável (opcional, pode ser null)
     *
     * O servidor exige "status" e "dataInicio" — preenchemos automaticamente.
     */
    public Dto.Tarefa criar(String titulo, String descricao, String prioridade,
                            String dataFim, Integer responsavelId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("titulo",     titulo);
        payload.put("status",     "PENDENTE");                          // CORREÇÃO: campo obrigatório
        payload.put("prioridade", prioridade != null ? prioridade : "BAIXA");
        payload.put("dataInicio", LocalDateTime.now().format(ISO));     // CORREÇÃO: campo obrigatório

        if (descricao != null && !descricao.isBlank()) {
            payload.put("descricao", descricao);
        }

        // CORREÇÃO: dataFim deve ser ISO datetime, não apenas data
        if (dataFim != null && !dataFim.isBlank()) {
            String isoDataFim = converterParaIsoDatetime(dataFim);
            if (isoDataFim != null) payload.put("dataFim", isoDataFim);
        }

        if (responsavelId != null) {
            // Nota: TAREFA_CREATE no servidor não processa responsavelId ainda
            // (TarefaHandler.create não lê esse campo), mas incluímos para futuro
            payload.put("responsavelId", responsavelId);
        }

        ServerResponse resp = client.send("TAREFA_CREATE", payload);
        if (resp.isError() || resp.getData() == null) return null;
        return gson.fromJson(resp.getData(), Dto.Tarefa.class);
    }

    // ── Atualizar ─────────────────────────────────────────────────────────────

    /**
     * Atualiza status e prioridade de uma tarefa existente.
     *
     * CORREÇÃO: não envia dataFim aqui pois o TarefasController não coleta
     * nova data no fluxo de atualização rápida (iniciar/concluir).
     */
    public boolean atualizar(int id, String status, String prioridade) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id",        id);
        payload.put("status",    status);
        payload.put("prioridade", prioridade);

        ServerResponse resp = client.send("TAREFA_UPDATE", payload);
        return resp.isOk();
    }

    // ── Deletar ───────────────────────────────────────────────────────────────

    public boolean deletar(int id) {
        ServerResponse resp = client.send("TAREFA_DELETE", Map.of("id", id));
        return resp.isOk();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converte "AAAA-MM-DD" → "AAAA-MM-DDTOO:00:00" para o servidor.
     * Retorna null se o formato for inválido.
     */
    private String converterParaIsoDatetime(String data) {
        try {
            // Aceita "AAAA-MM-DD" ou já está em formato ISO datetime
            if (data.length() == 10) {
                return data + "T00:00:00";
            }
            // Valida se já é ISO datetime
            LocalDateTime.parse(data, ISO);
            return data;
        } catch (Exception e) {
            return null;
        }
    }
}
package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Serviço de conversas e mensagens do client.
 *
 * Todos os métodos fazem chamadas síncronas ao TcpClient e
 * devem ser chamados dentro de um javafx.concurrent.Task,
 * nunca diretamente na thread do JavaFX.
 *
 * Exemplo no ChatController:
 *
 *   Task<List<Dto.Conversa>> task = new Task<>() {
 *       protected List<Dto.Conversa> call() {
 *           return new ConversaService().listarConversas();
 *       }
 *   };
 *   task.setOnSucceeded(e -> renderizar(task.getValue()));
 *   new Thread(task).start();
 */
public class ConversaService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson gson = client.getGson();

    // ── CONVERSA_LIST ─────────────────────────────────────────────────────────

    /** Lista todas as conversas do usuário logado, com contador de não lidas. */
    public List<Dto.Conversa> listarConversas() {
        ServerResponse resp = client.send("CONVERSA_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();

        Type listType = new TypeToken<List<Dto.Conversa>>(){}.getType();
        return gson.fromJson(resp.getData(), listType);
    }

    // ── MENSAGEM_LIST ─────────────────────────────────────────────────────────

    /** Lista o histórico de mensagens de uma conversa. */
    public List<Dto.Mensagem> listarMensagens(Integer conversaId) {
        ServerResponse resp = client.send("MENSAGEM_LIST", Map.of("conversaId", conversaId));
        if (resp.isError() || resp.getData() == null) return List.of();

        Type listType = new TypeToken<List<Dto.Mensagem>>(){}.getType();
        return gson.fromJson(resp.getData(), listType);
    }

    // ── MENSAGEM_SEND ─────────────────────────────────────────────────────────

    /**
     * Envia uma mensagem para uma conversa.
     * @return id da mensagem criada, ou null se falhou
     */
    public Integer enviarMensagem(Integer conversaId, String conteudo) {
        ServerResponse resp = client.send("MENSAGEM_SEND", Map.of(
            "conversaId", conversaId,
            "conteudo",   conteudo
        ));
        if (resp.isError() || resp.getData() == null) return null;

        try {
            return gson.fromJson(resp.getData(), Map.class)
                .get("mensagemId") instanceof Number n ? n.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ── MENSAGEM_MARCAR_LIDA ──────────────────────────────────────────────────

    /**
     * Marca todas as mensagens de uma conversa como lidas.
     * Chame ao abrir uma conversa.
     */
    public void marcarComoLida(Integer conversaId) {
        client.send("MENSAGEM_MARCAR_LIDA", Map.of("conversaId", conversaId));
    }

    // ── MENSAGEM_NAO_LIDAS ────────────────────────────────────────────────────

    /** Retorna mapa conversaId → quantidade de não lidas. */
    public Map<String, Long> contarNaoLidas() {
        ServerResponse resp = client.send("MENSAGEM_NAO_LIDAS");
        if (resp.isError() || resp.getData() == null) return Map.of();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(resp.getData(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Long> conversas = (Map<String, Long>) data.get("conversas");
            return conversas != null ? conversas : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ── CONVERSA_CREATE ───────────────────────────────────────────────────────

    /**
     * Cria uma nova conversa.
     * @param tipo            "PRIVADO", "GRUPO" ou "BROADCAST"
     * @param participanteIds IDs dos participantes (exceto o próprio usuário)
     * @return id da conversa criada, ou null se falhou
     */
    public Integer criarConversa(String tipo, List<Integer> participanteIds) {
        ServerResponse resp = client.send("CONVERSA_CREATE", Map.of(
            "tipo",             tipo,
            "participanteIds",  participanteIds
        ));
        if (resp.isError() || resp.getData() == null) return null;

        try {
            return gson.fromJson(resp.getData(), Map.class)
                .get("id") instanceof Number n ? n.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

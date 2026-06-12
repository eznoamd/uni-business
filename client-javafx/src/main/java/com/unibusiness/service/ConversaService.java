package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversaService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson      gson   = client.getGson();

    public List<Dto.Conversa> listarConversas() {
        ServerResponse resp = client.send("CONVERSA_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.Conversa>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }

    public List<Dto.Mensagem> listarMensagens(Integer conversaId) {
        ServerResponse resp = client.send("MENSAGEM_LIST", Map.of("conversaId", conversaId));
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.Mensagem>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }

    public Integer enviarMensagem(Integer conversaId, String conteudo) {
        ServerResponse resp = client.send("MENSAGEM_SEND", Map.of(
            "conversaId", conversaId,
            "conteudo",   conteudo
        ));
        if (resp.isError() || resp.getData() == null) return null;
        try {
            Object id = gson.fromJson(resp.getData(), Map.class).get("mensagemId");
            return id instanceof Number n ? n.intValue() : null;
        } catch (Exception e) { return null; }
    }

    public void marcarComoLida(Integer conversaId) {
        client.send("MENSAGEM_MARCAR_LIDA", Map.of("conversaId", conversaId));
    }

    public Map<String, Long> contarNaoLidas() {
        ServerResponse resp = client.send("MENSAGEM_NAO_LIDAS");
        if (resp.isError() || resp.getData() == null) return Map.of();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(resp.getData(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Long> c = (Map<String, Long>) data.get("conversas");
            return c != null ? c : Map.of();
        } catch (Exception e) { return Map.of(); }
    }

    /**
     * Cria conversa.
     * tipo: "PRIVADA" para 1-a-1, "GRUPO" para grupos.
     * nome: obrigatório apenas para GRUPO.
     */
    public Integer criarConversa(String tipo, List<Integer> participanteIds, String nome) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", tipo);
        payload.put("participanteIds", participanteIds);
        if (nome != null && !nome.isBlank()) payload.put("nome", nome);

        ServerResponse resp = client.send("CONVERSA_CREATE", payload);
        if (resp.isError() || resp.getData() == null) return null;
        try {
            Object id = gson.fromJson(resp.getData(), Map.class).get("id");
            return id instanceof Number n ? n.intValue() : null;
        } catch (Exception e) { return null; }
    }

    /** Compat com código antigo sem nome */
    public Integer criarConversa(String tipo, List<Integer> participanteIds) {
        return criarConversa(tipo, participanteIds, null);
    }
}

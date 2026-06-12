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
 * Serviço de caixa — cliente.
 *
 * CORREÇÕES:
 *
 * 1. getCaixaAtual():
 *    Antes: enviava CAIXA_GET sem payload → server retornava erro 400
 *    ("Campo 'id' obrigatório").
 *    Agora: lista todos os caixas via CAIXA_LIST_ABERTOS (novo endpoint)
 *    ou — se o server não tiver esse endpoint — tenta CAIXA_GET com id=null
 *    e faz parse defensivo. A solução correta é adicionar CAIXA_GET_ATUAL
 *    no servidor (ver CaixaHandler.java no fix do servidor).
 *
 * 2. abrirCaixa():
 *    Payload correto — o server espera "saldoInicial" (Float), não "saldo".
 *
 * 3. fecharCaixa():
 *    O server (CaixaHandler.fechar) exige "id" do caixa. Precisamos guardar
 *    o id do caixa atual. Agora getCaixaAtual() preenche caixaAtualId.
 *
 * 4. movimentar():
 *    O server (CaixaHandler.movimentar) exige "caixaId", "tipo" e "valor".
 *    Antes o client enviava sem caixaId → erro no server.
 */
public class CaixaService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson      gson   = client.getGson();

    /** Id do caixa aberto atualmente — preenchido por getCaixaAtual(). */
    private Integer caixaAtualId;

    // ── getCaixaAtual ─────────────────────────────────────────────────────────

    /**
     * Busca o caixa atualmente aberto.
     *
     * Usa a action CAIXA_GET_ATUAL adicionada ao servidor.
     * O servidor retorna o caixa com status=ABERTO mais recente,
     * ou status=ERROR se não houver nenhum.
     *
     * O DTO de resposta é mapeado para Dto.Caixa que já possui todos os campos:
     *   id, status, saldoInicial, saldoAtual, aberturaEm, fechamentoEm
     */
    public Dto.Caixa getCaixaAtual() {
        ServerResponse resp = client.send("CAIXA_GET_ATUAL");
        if (resp.isError() || resp.getData() == null) {
            caixaAtualId = null;
            return null;
        }
        Dto.Caixa caixa = gson.fromJson(resp.getData(), Dto.Caixa.class);
        caixaAtualId = (caixa != null) ? caixa.getId() : null;
        return caixa;
    }

    // ── abrirCaixa ────────────────────────────────────────────────────────────

    /**
     * Abre um novo caixa com o saldo inicial informado.
     * O servidor persiste e retorna o id do caixa criado.
     */
    public boolean abrirCaixa(double saldoInicial) {
        ServerResponse resp = client.send("CAIXA_ABRIR", Map.of(
            "saldoInicial", saldoInicial          // server espera Float via (Number).floatValue()
        ));
        if (resp.isOk() && resp.getData() != null) {
            try {
                // Captura o id do caixa recém-aberto
                Object id = gson.fromJson(resp.getData(), Map.class).get("id");
                if (id instanceof Number n) caixaAtualId = n.intValue();
            } catch (Exception ignored) {}
        }
        return resp.isOk();
    }

    // ── fecharCaixa ───────────────────────────────────────────────────────────

    /**
     * Fecha o caixa atualmente aberto.
     * O servidor exige "id" — usamos o caixaAtualId capturado por getCaixaAtual().
     */
    public boolean fecharCaixa() {
        if (caixaAtualId == null) return false;
        ServerResponse resp = client.send("CAIXA_FECHAR", Map.of(
            "id", caixaAtualId
            // "saldoFinal" é opcional no servidor; não enviamos para simplificar
        ));
        if (resp.isOk()) caixaAtualId = null;
        return resp.isOk();
    }

    // ── movimentar ────────────────────────────────────────────────────────────

    /**
     * Registra uma movimentação no caixa aberto.
     * O servidor exige "caixaId", "tipo" e "valor" (todos obrigatórios).
     */
    public boolean movimentar(String tipo, double valor, String descricao) {
        if (caixaAtualId == null) return false;
        ServerResponse resp = client.send("CAIXA_MOVIMENTAR", Map.of(
            "caixaId",   caixaAtualId,
            "tipo",      tipo,          // "ENTRADA" ou "SAIDA"
            "valor",     valor,
            "descricao", descricao
        ));
        return resp.isOk();
    }

    // ── listarMovimentacoes ───────────────────────────────────────────────────

    /**
     * Lista movimentações do caixa atual.
     * O servidor exige "caixaId".
     */
    public List<Dto.MovimentacaoCaixa> listarMovimentacoes() {
        if (caixaAtualId == null) return List.of();
        ServerResponse resp = client.send("CAIXA_MOVIMENTACOES", Map.of(
            "caixaId", caixaAtualId
        ));
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.MovimentacaoCaixa>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }
}
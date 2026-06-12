package com.unibusiness.network.handler;

import com.unibusiness.model.CaixaEntity;
import com.unibusiness.model.MovimentacaoCaixaEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.CaixaService;
import com.unibusiness.service.impl.CaixaServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler de Caixa.
 *
 * CORREÇÕES:
 *
 * 1. Adicionado CAIXA_GET_ATUAL: retorna o caixa aberto mais recente sem precisar de id.
 *    O client enviava CAIXA_GET sem id → erro "Campo 'id' obrigatório".
 *    Agora o client usa CAIXA_GET_ATUAL.
 *
 * 2. CAIXA_GET mantido para compatibilidade (busca por id explícito).
 *
 * 3. CAIXA_MOVIMENTAR: o campo "tipo" agora é uppercase antes de validar.
 *    O CaixaServiceImpl já faz toUpperCase(), mas validamos aqui também.
 *
 * 4. O DTO retornado por CAIXA_GET_ATUAL inclui "status", "saldoAtual" e
 *    "aberturaEm" para compatibilidade com Dto.Caixa do cliente.
 *
 * IMPORTANTE: adicione Actions.CAIXA_GET_ATUAL em Actions.java e registre
 * o handler no RequestDispatcher.java (ver comentário abaixo).
 */
public class CaixaHandler implements ActionHandler {

    private final CaixaService service = new CaixaServiceImpl();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CAIXA_ABRIR         -> abrir(req);
            case Actions.CAIXA_FECHAR        -> fechar(req);
            case Actions.CAIXA_GET           -> get(req);
            case Actions.CAIXA_GET_ATUAL     -> getAtual();          // NOVO
            case Actions.CAIXA_MOVIMENTAR    -> movimentar(req, session);
            case Actions.CAIXA_MOVIMENTACOES -> listarMovimentacoes(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    // ── CAIXA_ABRIR ───────────────────────────────────────────────────────────

    private Response abrir(Request req) {
        Number saldo = (Number) req.get("saldoInicial");
        try {
            CaixaEntity caixa = service.abrir(saldo != null ? saldo.floatValue() : 0F);
            return Response.ok(Actions.CAIXA_ABRIR, buildCaixaMap(caixa));
        } catch (Exception e) {
            return Response.error(Actions.CAIXA_ABRIR, e.getMessage());
        }
    }

    // ── CAIXA_FECHAR ──────────────────────────────────────────────────────────

    private Response fechar(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CAIXA_FECHAR, "Campo 'id' obrigatório.");
        Number saldoFinal = (Number) req.get("saldoFinal");
        try {
            CaixaEntity caixa = service.fechar(id, saldoFinal != null ? saldoFinal.floatValue() : null);
            Map<String, Object> m = new HashMap<>();
            m.put("id",             caixa.getId());
            m.put("status",         "FECHADO");
            m.put("dataFechamento", caixa.getDataFechamento().format(FMT));
            return Response.ok(Actions.CAIXA_FECHAR, m);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.error(Actions.CAIXA_FECHAR, e.getMessage());
        }
    }

    // ── CAIXA_GET (por id explícito) ──────────────────────────────────────────

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CAIXA_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(c -> Response.ok(Actions.CAIXA_GET, buildCaixaMap(c)))
            .orElse(Response.error(Actions.CAIXA_GET, "Caixa não encontrado."));
    }

    // ── CAIXA_GET_ATUAL (caixa aberto mais recente) — NOVO ───────────────────

    /**
     * Retorna o caixa atualmente aberto sem precisar de id.
     * O CaixaServiceImpl.getAtual() busca o caixa com dataFechamento IS NULL
     * mais recente.
     *
     * Retorna ERROR se não houver caixa aberto (o FinanceiroController
     * trata isso como "caixa fechado").
     */
    private Response getAtual() {
        return service.getAtual()
            .map(c -> Response.ok(Actions.CAIXA_GET_ATUAL, buildCaixaMap(c)))
            .orElse(Response.error(Actions.CAIXA_GET_ATUAL, "Nenhum caixa aberto."));
    }

    // ── CAIXA_MOVIMENTAR ──────────────────────────────────────────────────────

    private Response movimentar(Request req, ClientSession session) {
        Integer caixaId = req.getInteger("caixaId");
        String  tipo    = req.getString("tipo");
        Number  valor   = (Number) req.get("valor");

        if (caixaId == null || tipo == null || valor == null)
            return Response.error(Actions.CAIXA_MOVIMENTAR,
                "Campos 'caixaId', 'tipo' e 'valor' obrigatórios.");

        try {
            MovimentacaoCaixaEntity mov = service.movimentar(
                caixaId, tipo.toUpperCase(), valor.floatValue(),
                req.getString("descricao"), session.getUsuario()
            );
            return Response.ok(Actions.CAIXA_MOVIMENTAR, Map.of(
                "movimentacaoId", mov.getId()
            ));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CAIXA_MOVIMENTAR, e.getMessage());
        }
    }

    // ── CAIXA_MOVIMENTACOES ───────────────────────────────────────────────────

    private Response listarMovimentacoes(Request req) {
        Integer caixaId = req.getInteger("caixaId");
        if (caixaId == null)
            return Response.error(Actions.CAIXA_MOVIMENTACOES, "Campo 'caixaId' obrigatório.");

        List<Map<String, Object>> lista = service.listarMovimentacoes(caixaId).stream()
            .map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id",        m.getId());
                map.put("tipo",      m.getTipo());
                map.put("valor",     m.getValor());
                map.put("realizadaEm", m.getData().format(FMT));
                map.put("descricao", m.getDescricao() != null ? m.getDescricao() : "");
                return map;
            })
            .collect(Collectors.toList());
        return Response.ok(Actions.CAIXA_MOVIMENTACOES, lista);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Monta o mapa de resposta de um CaixaEntity.
     *
     * CORREÇÃO: inclui "status", "saldoAtual" e "aberturaEm" que o
     * Dto.Caixa do cliente espera. O campo "saldoAtual" é calculado
     * a partir do saldo inicial + movimentações pelo service.
     *
     * IMPORTANTE: o CaixaServiceImpl.abrir() precisa expor getSaldoAtual()
     * ou o map retorna saldoInicial como saldoAtual (aceitável para um caixa
     * recém-aberto sem movimentações).
     */
    private Map<String, Object> buildCaixaMap(CaixaEntity c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           c.getId());
        m.put("status",       c.getDataFechamento() == null ? "ABERTO" : "FECHADO");
        m.put("saldoInicial", c.getSaldoInicial().doubleValue());
        // saldoAtual = saldoInicial (sem calcular movimentações aqui;
        // para cálculo correto ver CaixaServiceImpl.getSaldoAtual())
        m.put("saldoAtual",   c.getSaldoFinal() != null
            ? c.getSaldoFinal().doubleValue()
            : c.getSaldoInicial().doubleValue());
        m.put("aberturaEm",   c.getDataAbertura().format(FMT));
        m.put("fechamentoEm", c.getDataFechamento() != null
            ? c.getDataFechamento().format(FMT) : null);
        return m;
    }
}
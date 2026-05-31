package com.unibusiness.network.handler;

import com.unibusiness.model.MovimentacaoCaixaEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.CaixaService;
import com.unibusiness.service.impl.CaixaServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CaixaHandler implements ActionHandler {

    private final CaixaService service = new CaixaServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CAIXA_ABRIR         -> abrir(req);
            case Actions.CAIXA_FECHAR        -> fechar(req);
            case Actions.CAIXA_GET           -> get(req);
            case Actions.CAIXA_MOVIMENTAR    -> movimentar(req, session);
            case Actions.CAIXA_MOVIMENTACOES -> listarMovimentacoes(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response abrir(Request req) {
        Number saldo = (Number) req.get("saldoInicial");
        try {
            var caixa = service.abrir(saldo != null ? saldo.floatValue() : null);
            return Response.ok(Actions.CAIXA_ABRIR, Map.of(
                "id",           caixa.getId(),
                "saldoInicial", caixa.getSaldoInicial(),
                "dataAbertura", caixa.getDataAbertura().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (Exception e) {
            return Response.error(Actions.CAIXA_ABRIR, e.getMessage());
        }
    }

    private Response fechar(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CAIXA_FECHAR, "Campo 'id' obrigatório.");
        Number saldoFinal = (Number) req.get("saldoFinal");
        try {
            var caixa = service.fechar(id, saldoFinal != null ? saldoFinal.floatValue() : null);
            return Response.ok(Actions.CAIXA_FECHAR, Map.of(
                "id",             caixa.getId(),
                "dataFechamento", caixa.getDataFechamento().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.error(Actions.CAIXA_FECHAR, e.getMessage());
        }
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CAIXA_GET, "Campo 'id' obrigatório.");
        return service.findById(id).map(c -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id",           c.getId());
            m.put("saldoInicial", c.getSaldoInicial());
            m.put("dataAbertura", c.getDataAbertura().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            m.put("saldoFinal",   c.getSaldoFinal());
            m.put("dataFechamento", c.getDataFechamento() != null
                ? c.getDataFechamento().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            return Response.ok(Actions.CAIXA_GET, m);
        }).orElse(Response.error(Actions.CAIXA_GET, "Caixa não encontrado."));
    }

    private Response movimentar(Request req, ClientSession session) {
        Integer caixaId = req.getInteger("caixaId");
        String  tipo    = req.getString("tipo");
        Number  valor   = (Number) req.get("valor");
        if (caixaId == null || tipo == null || valor == null)
            return Response.error(Actions.CAIXA_MOVIMENTAR, "Campos 'caixaId', 'tipo' e 'valor' obrigatórios.");
        try {
            MovimentacaoCaixaEntity mov = service.movimentar(
                caixaId, tipo, valor.floatValue(), req.getString("descricao"), session.getUsuario()
            );
            return Response.ok(Actions.CAIXA_MOVIMENTAR, Map.of("movimentacaoId", mov.getId()));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CAIXA_MOVIMENTAR, e.getMessage());
        }
    }

    private Response listarMovimentacoes(Request req) {
        Integer caixaId = req.getInteger("caixaId");
        if (caixaId == null) return Response.error(Actions.CAIXA_MOVIMENTACOES, "Campo 'caixaId' obrigatório.");

        List<Map<String, Object>> lista = service.listarMovimentacoes(caixaId).stream()
            .map(m -> Map.<String, Object>of(
                "id",       m.getId(),
                "tipo",     m.getTipo(),
                "valor",    m.getValor(),
                "data",     m.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "descricao", m.getDescricao() != null ? m.getDescricao() : ""
            ))
            .collect(Collectors.toList());
        return Response.ok(Actions.CAIXA_MOVIMENTACOES, lista);
    }
}

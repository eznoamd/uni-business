package com.unibusiness.network.handler;

import com.unibusiness.model.MovimentacaoEstoqueEntity;
import com.unibusiness.model.ProdutoEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.ProdutoService;
import com.unibusiness.service.impl.ProdutoServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProdutoHandler implements ActionHandler {

    private final ProdutoService service = new ProdutoServiceImpl();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.PRODUTO_CREATE        -> create(req);
            case Actions.PRODUTO_LIST          -> list();
            case Actions.PRODUTO_GET           -> get(req);
            case Actions.PRODUTO_UPDATE        -> update(req);
            case Actions.PRODUTO_DELETE        -> delete(req);
            case Actions.ESTOQUE_MOVIMENTAR    -> movimentar(req, session);
            case Actions.ESTOQUE_MOVIMENTACOES -> listarMovimentacoes(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response create(Request req) {
        String nome  = req.getString("nome");
        Number preco = (Number) req.get("precoUnitario");
        if (nome == null || preco == null)
            return Response.error(Actions.PRODUTO_CREATE, "Campos 'nome' e 'precoUnitario' obrigatórios.");

        ProdutoEntity p = new ProdutoEntity(nome, preco.floatValue());
        if (req.getString("descricao") != null) p.setDescricao(req.getString("descricao"));

        return Response.ok(Actions.PRODUTO_CREATE, toMap(service.create(p)));
    }

    private Response list() {
        List<Map<String, Object>> lista = service.listAll().stream().map(this::toMap).collect(Collectors.toList());
        return Response.ok(Actions.PRODUTO_LIST, lista);
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_GET, "Campo 'id' obrigatório.");
        return service.findById(id)
            .map(p -> Response.ok(Actions.PRODUTO_GET, toMap(p)))
            .orElse(Response.error(Actions.PRODUTO_GET, "Produto não encontrado."));
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_UPDATE, "Campo 'id' obrigatório.");

        return service.findById(id).map(p -> {
            if (req.getString("nome")      != null) p.setNome(req.getString("nome"));
            if (req.getString("descricao") != null) p.setDescricao(req.getString("descricao"));
            if (req.get("precoUnitario")   != null) p.setPrecoUnitario(((Number) req.get("precoUnitario")).floatValue());
            if (req.get("quantidade")      != null) p.setQuantidade(req.getInteger("quantidade"));
            return Response.ok(Actions.PRODUTO_UPDATE, toMap(service.update(p)));
        }).orElse(Response.error(Actions.PRODUTO_UPDATE, "Produto não encontrado."));
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_DELETE, "Campo 'id' obrigatório.");
        try {
            service.delete(id);
            return Response.ok(Actions.PRODUTO_DELETE, "Produto removido.", null);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.PRODUTO_DELETE, e.getMessage());
        }
    }

    private Response movimentar(Request req, ClientSession session) {
        Integer produtoId  = req.getInteger("produtoId");
        String  tipo       = req.getString("tipo");
        Integer quantidade = req.getInteger("quantidade");

        if (produtoId == null || tipo == null || quantidade == null)
            return Response.error(Actions.ESTOQUE_MOVIMENTAR, "Campos 'produtoId', 'tipo' e 'quantidade' obrigatórios.");

        try {
            MovimentacaoEstoqueEntity mov = service.movimentar(produtoId, tipo, quantidade, session.getUsuario());
            return Response.ok(Actions.ESTOQUE_MOVIMENTAR, Map.of(
                "movimentacaoId", mov.getId(),
                "estoqueAtual",   mov.getProduto().getQuantidade()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.error(Actions.ESTOQUE_MOVIMENTAR, e.getMessage());
        }
    }

    private Response listarMovimentacoes(Request req) {
        Integer produtoId = req.getInteger("produtoId");
        if (produtoId == null) return Response.error(Actions.ESTOQUE_MOVIMENTACOES, "Campo 'produtoId' obrigatório.");

        List<Map<String, Object>> lista = service.listarMovimentacoes(produtoId).stream()
            .map(m -> Map.<String, Object>of(
                "id",         m.getId(),
                "tipo",       m.getTipo(),
                "quantidade", m.getQuantidade(),
                "data",       m.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
            .collect(Collectors.toList());
        return Response.ok(Actions.ESTOQUE_MOVIMENTACOES, lista);
    }

    private Map<String, Object> toMap(ProdutoEntity p) {
        return Map.of(
            "id",            p.getId(),
            "nome",          p.getNome(),
            "descricao",     p.getDescricao() != null ? p.getDescricao() : "",
            "quantidade",    p.getQuantidade(),
            "precoUnitario", p.getPrecoUnitario()
        );
    }
}

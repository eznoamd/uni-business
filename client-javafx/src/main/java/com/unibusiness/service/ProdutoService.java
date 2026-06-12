package com.unibusiness.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ProdutoService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson      gson   = client.getGson();

    public List<Dto.Produto> listar() {
        ServerResponse resp = client.send("PRODUTO_LIST");
        if (resp.isError() || resp.getData() == null) return List.of();
        Type t = new TypeToken<List<Dto.Produto>>(){}.getType();
        return gson.fromJson(resp.getData(), t);
    }

    public Dto.Produto criar(String nome, String descricao, int quantidade, float preco) {
        ServerResponse resp = client.send("PRODUTO_CREATE", Map.of(
            "nome", nome,
            "descricao", descricao,
            "quantidade", quantidade,
            "precoUnitario", preco
        ));
        if (resp.isError() || resp.getData() == null) return null;
        return gson.fromJson(resp.getData(), Dto.Produto.class);
    }

    public boolean atualizar(int id, String nome, String descricao, float preco) {
        ServerResponse resp = client.send("PRODUTO_UPDATE", Map.of(
            "id", id,
            "nome", nome,
            "descricao", descricao,
            "precoUnitario", preco
        ));
        return resp.isOk();
    }

    public boolean deletar(int id) {
        ServerResponse resp = client.send("PRODUTO_DELETE", Map.of("id", id));
        return resp.isOk();
    }

    public boolean movimentarEstoque(int produtoId, String tipo, int quantidade) {
        ServerResponse resp = client.send("ESTOQUE_MOVIMENTAR", Map.of(
            "produtoId", produtoId,
            "tipo", tipo,
            "quantidade", quantidade
        ));
        return resp.isOk();
    }
}

package com.unibusiness.service;

import com.unibusiness.core.model.ProdutoEntity;
import com.unibusiness.core.service.impl.ProdutoServiceImpl;
import com.unibusiness.dto.Dto;
import com.unibusiness.session.SessionManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de produtos/estoque — antes via TCP (PRODUTO_*, ESTOQUE_MOVIMENTAR),
 * agora chamando ProdutoService (JPA) direto.
 */
public class ProdutoService {

    private final com.unibusiness.core.service.ProdutoService produtoService = new ProdutoServiceImpl();

    public List<Dto.Produto> listar() {
        return produtoService.listAll().stream().map(ProdutoService::toDto).collect(Collectors.toList());
    }

    public Dto.Produto criar(String nome, String descricao, int quantidade, float preco) {
        ProdutoEntity p = new ProdutoEntity(nome, preco);
        p.setDescricao(descricao);
        p.setQuantidade(quantidade);
        try {
            return toDto(produtoService.create(p));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean atualizar(int id, String nome, String descricao, float preco) {
        return produtoService.findById(id).map(p -> {
            p.setNome(nome);
            p.setDescricao(descricao);
            p.setPrecoUnitario(preco);
            produtoService.update(p);
            return true;
        }).orElse(false);
    }

    public boolean deletar(int id) {
        try {
            produtoService.delete(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean movimentarEstoque(int produtoId, String tipo, int quantidade) {
        try {
            produtoService.movimentar(produtoId, tipo, quantidade, SessionManager.getInstance().getUsuario());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Dto.Produto toDto(ProdutoEntity p) {
        Dto.Produto dto = new Dto.Produto();
        dto.id = p.getId();
        dto.nome = p.getNome();
        dto.descricao = p.getDescricao();
        dto.quantidade = p.getQuantidade();
        dto.precoUnitario = p.getPrecoUnitario();
        return dto;
    }
}

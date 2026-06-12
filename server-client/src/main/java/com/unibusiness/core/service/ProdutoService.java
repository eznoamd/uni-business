package com.unibusiness.core.service;
import com.unibusiness.core.model.MovimentacaoEstoqueEntity;
import com.unibusiness.core.model.ProdutoEntity;
import com.unibusiness.core.model.UsuarioEntity;
import java.util.List;
import java.util.Optional;
public interface ProdutoService {
    ProdutoEntity create(ProdutoEntity produto);
    List<ProdutoEntity> listAll();
    Optional<ProdutoEntity> findById(Integer id);
    ProdutoEntity update(ProdutoEntity produto);
    void delete(Integer id);
    MovimentacaoEstoqueEntity movimentar(Integer produtoId, String tipo, Integer quantidade, UsuarioEntity usuario);
    List<MovimentacaoEstoqueEntity> listarMovimentacoes(Integer produtoId);
}
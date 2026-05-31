package com.unibusiness.service;

import com.unibusiness.model.CaixaEntity;
import com.unibusiness.model.MovimentacaoCaixaEntity;
import com.unibusiness.model.UsuarioEntity;

import java.util.List;
import java.util.Optional;

public interface CaixaService {
    CaixaEntity abrir(Float saldoInicial);
    CaixaEntity fechar(Integer id, Float saldoFinal);
    Optional<CaixaEntity> findById(Integer id);

    MovimentacaoCaixaEntity movimentar(Integer caixaId, String tipo, Float valor, String descricao, UsuarioEntity usuario);
    List<MovimentacaoCaixaEntity> listarMovimentacoes(Integer caixaId);
}

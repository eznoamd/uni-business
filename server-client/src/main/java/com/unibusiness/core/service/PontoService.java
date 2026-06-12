package com.unibusiness.core.service;
import com.unibusiness.core.model.RegistroPontoEntity;
import java.util.List;
public interface PontoService {
    RegistroPontoEntity registrarEntrada(Integer usuarioId);
    RegistroPontoEntity registrarSaida(Integer usuarioId);
    List<RegistroPontoEntity> listar(Integer usuarioId);
}
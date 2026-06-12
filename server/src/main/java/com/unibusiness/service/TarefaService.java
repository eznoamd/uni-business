package com.unibusiness.service;
import com.unibusiness.model.TarefaEntity;
import java.util.List;
import java.util.Optional;
public interface TarefaService {
    TarefaEntity create(TarefaEntity tarefa);
    List<TarefaEntity> listAll();
    Optional<TarefaEntity> findById(Integer id);
    TarefaEntity update(TarefaEntity tarefa);
    void delete(Integer id);
}
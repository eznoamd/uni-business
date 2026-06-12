package com.unibusiness.core.service;
import com.unibusiness.core.model.EquipeEntity;
import java.util.List;
import java.util.Optional;
public interface EquipeService {
    EquipeEntity create(EquipeEntity equipe);
    List<EquipeEntity> listAll();
    Optional<EquipeEntity> findById(Integer id);
    EquipeEntity update(EquipeEntity equipe);
    void delete(Integer id);
}
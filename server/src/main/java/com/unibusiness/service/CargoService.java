package com.unibusiness.service;

import com.unibusiness.model.CargoEntity;
import com.unibusiness.model.PermissaoEntity;

import java.util.List;
import java.util.Optional;

public interface CargoService {
    CargoEntity create(CargoEntity cargo);
    List<CargoEntity> listAll();
    Optional<CargoEntity> findById(Integer id);
    void delete(Integer id);

    PermissaoEntity createPermissao(PermissaoEntity permissao);
    List<PermissaoEntity> listAllPermissoes();
}

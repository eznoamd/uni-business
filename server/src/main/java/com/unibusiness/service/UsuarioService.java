package com.unibusiness.service;

import com.unibusiness.model.UsuarioEntity;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    UsuarioEntity create(UsuarioEntity usuario);
    UsuarioEntity update(UsuarioEntity usuario);
    Optional<UsuarioEntity> findById(Integer id);
    Optional<UsuarioEntity> findByEmail(String email);
    List<UsuarioEntity> listAll();
}
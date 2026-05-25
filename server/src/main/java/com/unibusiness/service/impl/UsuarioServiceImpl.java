package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.repository.UsuarioRepository;
import com.unibusiness.service.UsuarioService;
import com.unibusiness.model.UsuarioEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

public class UsuarioServiceImpl implements UsuarioService {

    private final EntityManagerFactory emf;

    public UsuarioServiceImpl() {
        this.emf = PersistenceManager.getEntityManagerFactory();
    }

    private EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    @Override
    public UsuarioEntity create(UsuarioEntity usuario) {
        EntityManager em = createEntityManager();
        try {
            return new UsuarioRepository(em).save(usuario);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<UsuarioEntity> findById(Integer id) {
        EntityManager em = createEntityManager();
        try {
            return Optional.ofNullable(new UsuarioRepository(em).findById(id));
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<UsuarioEntity> findByEmail(String email) {
        EntityManager em = createEntityManager();
        try {
            return new UsuarioRepository(em).findByEmail(email);
        } finally {
            em.close();
        }
    }

    @Override
    public List<UsuarioEntity> listAll() {
        EntityManager em = createEntityManager();
        try {
            return new UsuarioRepository(em).findAll();
        } finally {
            em.close();
        }
    }
}

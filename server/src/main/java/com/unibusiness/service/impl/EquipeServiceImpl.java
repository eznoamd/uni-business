package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.EquipeEntity;
import com.unibusiness.repository.GenericRepository;
import com.unibusiness.service.EquipeService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

public class EquipeServiceImpl implements EquipeService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public EquipeEntity create(EquipeEntity equipe) {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(EquipeEntity.class, em).save(equipe); }
        finally { em.close(); }
    }

    @Override
    public List<EquipeEntity> listAll() {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(EquipeEntity.class, em).findAll(); }
        finally { em.close(); }
    }

    @Override
    public Optional<EquipeEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try { return Optional.ofNullable(em.find(EquipeEntity.class, id)); }
        finally { em.close(); }
    }

    @Override
    public EquipeEntity update(EquipeEntity equipe) {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(EquipeEntity.class, em).save(equipe); }
        finally { em.close(); }
    }

    @Override
    public void delete(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            GenericRepository<EquipeEntity> repo = new GenericRepository<>(EquipeEntity.class, em);
            EquipeEntity e = repo.findById(id);
            if (e == null) throw new IllegalArgumentException("Equipe não encontrada: " + id);
            repo.delete(e);
        } finally { em.close(); }
    }
}

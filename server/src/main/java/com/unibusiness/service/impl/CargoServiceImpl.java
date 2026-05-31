package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.CargoEntity;
import com.unibusiness.model.PermissaoEntity;
import com.unibusiness.repository.GenericRepository;
import com.unibusiness.service.CargoService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

public class CargoServiceImpl implements CargoService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public CargoEntity create(CargoEntity cargo) {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(CargoEntity.class, em).save(cargo); }
        finally { em.close(); }
    }

    @Override
    public List<CargoEntity> listAll() {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(CargoEntity.class, em).findAll(); }
        finally { em.close(); }
    }

    @Override
    public Optional<CargoEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try { return Optional.ofNullable(em.find(CargoEntity.class, id)); }
        finally { em.close(); }
    }

    @Override
    public void delete(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            GenericRepository<CargoEntity> repo = new GenericRepository<>(CargoEntity.class, em);
            CargoEntity c = repo.findById(id);
            if (c == null) throw new IllegalArgumentException("Cargo não encontrado: " + id);
            repo.delete(c);
        } finally { em.close(); }
    }

    @Override
    public PermissaoEntity createPermissao(PermissaoEntity permissao) {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(PermissaoEntity.class, em).save(permissao); }
        finally { em.close(); }
    }

    @Override
    public List<PermissaoEntity> listAllPermissoes() {
        EntityManager em = emf.createEntityManager();
        try { return new GenericRepository<>(PermissaoEntity.class, em).findAll(); }
        finally { em.close(); }
    }
}

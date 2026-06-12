package com.unibusiness.core.service.impl;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.model.CargoEntity;
import com.unibusiness.core.model.PermissaoEntity;
import com.unibusiness.core.repository.GenericRepository;
import com.unibusiness.core.service.CargoService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
public class CargoServiceImpl implements CargoService {
    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
    public CargoEntity create(CargoEntity cargo) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(CargoEntity.class, em).save(cargo); } finally { em.close(); } }
    public List<CargoEntity> listAll() { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(CargoEntity.class, em).findAll(); } finally { em.close(); } }
    public Optional<CargoEntity> findById(Integer id) { EntityManager em = emf.createEntityManager(); try { return Optional.ofNullable(em.find(CargoEntity.class, id)); } finally { em.close(); } }
    public void delete(Integer id) { EntityManager em = emf.createEntityManager(); try { GenericRepository<CargoEntity> repo = new GenericRepository<>(CargoEntity.class, em); CargoEntity c = repo.findById(id); if (c == null) throw new IllegalArgumentException("Cargo não encontrado: " + id); repo.delete(c); } finally { em.close(); } }
    public PermissaoEntity createPermissao(PermissaoEntity permissao) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(PermissaoEntity.class, em).save(permissao); } finally { em.close(); } }
    public List<PermissaoEntity> listAllPermissoes() { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(PermissaoEntity.class, em).findAll(); } finally { em.close(); } }
}
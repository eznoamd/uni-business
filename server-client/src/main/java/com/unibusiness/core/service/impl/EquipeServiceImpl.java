package com.unibusiness.core.service.impl;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.model.EquipeEntity;
import com.unibusiness.core.repository.GenericRepository;
import com.unibusiness.core.service.EquipeService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
public class EquipeServiceImpl implements EquipeService {
    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
    public EquipeEntity create(EquipeEntity equipe) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(EquipeEntity.class, em).save(equipe); } finally { em.close(); } }
    public List<EquipeEntity> listAll() { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(EquipeEntity.class, em).findAll(); } finally { em.close(); } }
    public Optional<EquipeEntity> findById(Integer id) { EntityManager em = emf.createEntityManager(); try { return Optional.ofNullable(em.find(EquipeEntity.class, id)); } finally { em.close(); } }
    public EquipeEntity update(EquipeEntity equipe) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(EquipeEntity.class, em).save(equipe); } finally { em.close(); } }
    public void delete(Integer id) { EntityManager em = emf.createEntityManager(); try { GenericRepository<EquipeEntity> repo = new GenericRepository<>(EquipeEntity.class, em); EquipeEntity e = repo.findById(id); if (e == null) throw new IllegalArgumentException("Equipe não encontrada: " + id); repo.delete(e); } finally { em.close(); } }
}
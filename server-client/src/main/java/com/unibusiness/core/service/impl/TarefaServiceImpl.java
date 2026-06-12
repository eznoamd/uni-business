package com.unibusiness.core.service.impl;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.model.TarefaEntity;
import com.unibusiness.core.repository.GenericRepository;
import com.unibusiness.core.service.TarefaService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
public class TarefaServiceImpl implements TarefaService {
    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
    public TarefaEntity create(TarefaEntity tarefa) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(TarefaEntity.class, em).save(tarefa); } finally { em.close(); } }
    public List<TarefaEntity> listAll() { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(TarefaEntity.class, em).findAll(); } finally { em.close(); } }
    public Optional<TarefaEntity> findById(Integer id) { EntityManager em = emf.createEntityManager(); try { return Optional.ofNullable(em.find(TarefaEntity.class, id)); } finally { em.close(); } }
    public TarefaEntity update(TarefaEntity tarefa) { EntityManager em = emf.createEntityManager(); try { return new GenericRepository<>(TarefaEntity.class, em).save(tarefa); } finally { em.close(); } }
    public void delete(Integer id) { EntityManager em = emf.createEntityManager(); try { GenericRepository<TarefaEntity> repo = new GenericRepository<>(TarefaEntity.class, em); TarefaEntity t = repo.findById(id); if (t == null) throw new IllegalArgumentException("Tarefa não encontrada: " + id); repo.delete(t); } finally { em.close(); } }
}
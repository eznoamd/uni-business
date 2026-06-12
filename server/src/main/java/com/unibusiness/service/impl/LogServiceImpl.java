package com.unibusiness.service.impl;
import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.LogSistemaEntity;
import com.unibusiness.service.LogService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
public class LogServiceImpl implements LogService {
    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
    public List<LogSistemaEntity> listar(Integer usuarioId, Integer limit) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = usuarioId != null ? "SELECT l FROM LogSistemaEntity l WHERE l.usuario.id = :uid ORDER BY l.data DESC" : "SELECT l FROM LogSistemaEntity l ORDER BY l.data DESC";
            var query = em.createQuery(jpql, LogSistemaEntity.class);
            if (usuarioId != null) query.setParameter("uid", usuarioId);
            query.setMaxResults(limit != null ? limit : 100);
            return query.getResultList();
        } finally { em.close(); }
    }
}
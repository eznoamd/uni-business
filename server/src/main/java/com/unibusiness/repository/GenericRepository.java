package com.unibusiness.repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

public class GenericRepository<T> {

    protected final EntityManager em;
    private final Class<T> entityClass;

    public GenericRepository(Class<T> entityClass, EntityManager em) {
        this.entityClass = entityClass;
        this.em = em;
    }

    public T findById(Integer id) {
        return em.find(entityClass, id);
    }

    public List<T> findAll() {
        CriteriaQuery<T> criteria = em.getCriteriaBuilder().createQuery(entityClass);
        criteria.select(criteria.from(entityClass));
        return em.createQuery(criteria).getResultList();
    }

    public T save(T entity) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T merged = em.merge(entity);
            tx.commit();
            return merged;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public void delete(T entity) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.remove(em.contains(entity) ? entity : em.merge(entity));
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        }
    }
}

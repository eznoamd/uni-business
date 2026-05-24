package com.unibusiness.repository;

import com.unibusiness.model.MensagemEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class MensagemRepository extends GenericRepository<MensagemEntity> {

    public MensagemRepository(EntityManager em) {
        super(MensagemEntity.class, em);
    }

    public List<MensagemEntity> findByConversaId(Integer conversaId) {
        TypedQuery<MensagemEntity> query = em.createQuery(
                "SELECT m FROM MensagemEntity m WHERE m.conversa.id = :conversaId ORDER BY m.enviadoEm",
                MensagemEntity.class);
        query.setParameter("conversaId", conversaId);
        return query.getResultList();
    }
}

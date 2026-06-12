package com.unibusiness.core.service.impl;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.repository.MensagemRepository;
import com.unibusiness.core.service.MensagemService;
import com.unibusiness.core.model.MensagemEntity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
public class MensagemServiceImpl implements MensagemService {
    private final EntityManagerFactory emf;
    public MensagemServiceImpl() { this.emf = PersistenceManager.getEntityManagerFactory(); }
    public MensagemEntity create(MensagemEntity mensagem) { EntityManager em = emf.createEntityManager(); try { return new MensagemRepository(em).save(mensagem); } finally { em.close(); } }
    public List<MensagemEntity> findByConversaId(Integer conversaId) { EntityManager em = emf.createEntityManager(); try { return new MensagemRepository(em).findByConversaId(conversaId); } finally { em.close(); } }
}
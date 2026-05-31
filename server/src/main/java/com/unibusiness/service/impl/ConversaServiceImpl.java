package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.repository.MensagemRepository;
import com.unibusiness.repository.UsuarioRepository;
import com.unibusiness.service.ConversaService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConversaServiceImpl implements ConversaService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public ConversaEntity criar(String tipo, Integer criadorId, Set<Integer> participanteIds) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ConversaEntity conversa = new ConversaEntity(tipo.toUpperCase());
            UsuarioRepository repo = new UsuarioRepository(em);

            Set<UsuarioEntity> participantes = new HashSet<>();
            UsuarioEntity criador = repo.findById(criadorId);
            if (criador == null) throw new IllegalArgumentException("Criador não encontrado.");
            participantes.add(criador);

            for (Integer id : participanteIds) {
                UsuarioEntity p = repo.findById(id);
                if (p != null) participantes.add(p);
            }
            conversa.setParticipantes(participantes);
            em.persist(conversa);

            for (UsuarioEntity p : participantes) {
                em.merge(p).getConversas().add(conversa);
            }

            tx.commit();
            return conversa;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public List<ConversaEntity> listarPorUsuario(Integer usuarioId) {
        EntityManager em = emf.createEntityManager();
        try {
            UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
            if (usuario == null) throw new IllegalArgumentException("Usuário não encontrado.");
            // força carregamento lazy dentro da sessão JPA aberta
            return List.copyOf(usuario.getConversas());
        } finally { em.close(); }
    }

    @Override
    public MensagemEntity enviarMensagem(Integer conversaId, Integer remetenteId, String conteudo) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ConversaEntity conversa = em.find(ConversaEntity.class, conversaId);
            if (conversa == null) throw new IllegalArgumentException("Conversa não encontrada.");
            UsuarioEntity remetente = em.find(UsuarioEntity.class, remetenteId);
            if (remetente == null) throw new IllegalArgumentException("Remetente não encontrado.");

            MensagemEntity msg = new MensagemEntity(conversa, remetente, conteudo);
            em.persist(msg);
            tx.commit();
            return msg;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public List<MensagemEntity> listarMensagens(Integer conversaId) {
        EntityManager em = emf.createEntityManager();
        try {
            return new MensagemRepository(em).findByConversaId(conversaId);
        } finally { em.close(); }
    }
}

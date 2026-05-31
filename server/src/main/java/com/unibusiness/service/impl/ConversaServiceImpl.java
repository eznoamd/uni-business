package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.*;
import com.unibusiness.repository.MensagemRepository;
import com.unibusiness.repository.MensagemStatusRepository;
import com.unibusiness.repository.UsuarioRepository;
import com.unibusiness.service.ConversaService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.*;
import java.util.stream.Collectors;

public class ConversaServiceImpl implements ConversaService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public ConversaEntity criar(String tipo, Integer criadorId, Set<Integer> participanteIds) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            ConversaEntity conversa = new ConversaEntity(tipo.toUpperCase());
            em.persist(conversa);

            UsuarioRepository repo = new UsuarioRepository(em);
            UsuarioEntity criador = repo.findById(criadorId);
            if (criador == null) throw new IllegalArgumentException("Criador não encontrado.");

            Set<Integer> todos = new HashSet<>(participanteIds);
            todos.add(criadorId);

            for (Integer id : todos) {
                UsuarioEntity p = repo.findById(id);
                if (p != null) {
                    p.getConversas().add(conversa);
                    em.merge(p);
                }
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
            return em.createQuery(
                "SELECT DISTINCT c FROM ConversaEntity c " +
                "JOIN c.participantes p " +
                "LEFT JOIN FETCH c.participantes " +
                "WHERE p.id = :uid",
                ConversaEntity.class)
                .setParameter("uid", usuarioId)
                .getResultList();
        } finally { em.close(); }
    }

    @Override
    public MensagemEntity enviarMensagem(Integer conversaId, Integer remetenteId, String conteudo) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            ConversaEntity conversa = em.createQuery(
                "SELECT c FROM ConversaEntity c " +
                "LEFT JOIN FETCH c.participantes " +
                "WHERE c.id = :id",
                ConversaEntity.class)
                .setParameter("id", conversaId)
                .getResultStream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada."));

            UsuarioEntity remetente = em.find(UsuarioEntity.class, remetenteId);
            if (remetente == null) throw new IllegalArgumentException("Remetente não encontrado.");

            MensagemEntity msg = new MensagemEntity(conversa, remetente, conteudo);
            em.persist(msg);
            for (UsuarioEntity participante : conversa.getParticipantes()) {
                if (!participante.getId().equals(remetenteId)) {
                    em.persist(new MensagemStatusEntity(msg, participante));
                }
            }

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
            return em.createQuery(
                "SELECT m FROM MensagemEntity m " +
                "JOIN FETCH m.remetente " +
                "JOIN FETCH m.conversa " +
                "WHERE m.conversa.id = :cid " +
                "ORDER BY m.enviadoEm",
                MensagemEntity.class)
                .setParameter("cid", conversaId)
                .getResultList();
        } finally { em.close(); }
    }

    @Override
    public int marcarConversaComoLida(Integer usuarioId, Integer conversaId) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int atualizadas = new MensagemStatusRepository(em)
                .marcarConversaComoLida(usuarioId, conversaId);
            tx.commit();
            return atualizadas;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public Map<Integer, Long> contarNaoLidasPorConversa(Integer usuarioId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<Object[]> rows = new MensagemStatusRepository(em)
                .contarNaoLidasPorConversa(usuarioId);

            return rows.stream().collect(Collectors.toMap(
                row -> (Integer) row[0],
                row -> (Long)    row[1]
            ));
        } finally { em.close(); }
    }
}
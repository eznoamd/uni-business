package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.CaixaEntity;
import com.unibusiness.model.MovimentacaoCaixaEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.service.CaixaService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CaixaServiceImpl implements CaixaService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public CaixaEntity abrir(Float saldoInicial) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = new CaixaEntity(saldoInicial != null ? saldoInicial : 0F);
            em.persist(caixa);
            tx.commit();
            return caixa;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public CaixaEntity fechar(Integer id, Float saldoFinal) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = em.find(CaixaEntity.class, id);
            if (caixa == null) throw new IllegalArgumentException("Caixa não encontrado: " + id);
            if (caixa.getDataFechamento() != null) throw new IllegalStateException("Caixa já está fechado.");
            caixa.setDataFechamento(LocalDateTime.now());
            if (saldoFinal != null) caixa.setSaldoFinal(saldoFinal);
            em.merge(caixa);
            tx.commit();
            return caixa;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public Optional<CaixaEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(em.find(CaixaEntity.class, id));
        } finally { em.close(); }
    }

    @Override
    public MovimentacaoCaixaEntity movimentar(Integer caixaId, String tipo, Float valor, String descricao, UsuarioEntity usuario) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = em.find(CaixaEntity.class, caixaId);
            if (caixa == null) throw new IllegalArgumentException("Caixa não encontrado: " + caixaId);
            UsuarioEntity usuarioManaged = em.find(UsuarioEntity.class, usuario.getId());
            MovimentacaoCaixaEntity mov = new MovimentacaoCaixaEntity(caixa, tipo.toUpperCase(), valor, usuarioManaged);
            mov.setDescricao(descricao);
            em.persist(mov);
            tx.commit();
            return mov;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public List<MovimentacaoCaixaEntity> listarMovimentacoes(Integer caixaId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT m FROM MovimentacaoCaixaEntity m WHERE m.caixa.id = :cid ORDER BY m.data",
                MovimentacaoCaixaEntity.class)
                .setParameter("cid", caixaId)
                .getResultList();
        } finally { em.close(); }
    }
}

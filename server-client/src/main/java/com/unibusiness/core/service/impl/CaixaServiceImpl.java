package com.unibusiness.core.service.impl;

import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.model.CaixaEntity;
import com.unibusiness.core.model.MovimentacaoCaixaEntity;
import com.unibusiness.core.model.UsuarioEntity;
import com.unibusiness.core.service.CaixaService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de CaixaService.
 *
 * CORREÇÕES:
 *
 * 1. Adicionado getAtual(): retorna o caixa aberto (dataFechamento IS NULL)
 *    mais recente. Usado pelo novo endpoint CAIXA_GET_ATUAL.
 *
 * 2. buildCaixaMap() no handler precisava de "saldoAtual" calculado.
 *    Adicionado getSaldoAtual(caixa, em) que soma as movimentações de entrada
 *    e subtrai as de saída a partir do saldo inicial.
 *
 * 3. O método abrir() agora retorna o id correto (já estava correto, mantido).
 */
public class CaixaServiceImpl implements CaixaService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    // ── abrir ─────────────────────────────────────────────────────────────────

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

    // ── fechar ────────────────────────────────────────────────────────────────

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
            if (saldoFinal != null) {
                caixa.setSaldoFinal(saldoFinal);
            } else {
                // Calcula saldo final a partir das movimentações
                caixa.setSaldoFinal(calcularSaldoAtual(caixa, em));
            }
            em.merge(caixa);
            tx.commit();
            return caixa;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Override
    public Optional<CaixaEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(em.find(CaixaEntity.class, id));
        } finally { em.close(); }
    }

    // ── getAtual — NOVO ───────────────────────────────────────────────────────

    /**
     * Retorna o caixa aberto mais recente (dataFechamento IS NULL).
     * Ordenado por dataAbertura DESC, pega o primeiro.
     */
    @Override
    public Optional<CaixaEntity> getAtual() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT c FROM CaixaEntity c " +
                "WHERE c.dataFechamento IS NULL " +
                "ORDER BY c.dataAbertura DESC",
                CaixaEntity.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
        } finally { em.close(); }
    }

    // ── getUltimoFechado — NOVO ───────────────────────────────────────────────

    /**
     * Retorna o caixa fechado mais recente (dataFechamento IS NOT NULL).
     * Usado para mostrar o saldo final quando não há caixa aberto no momento.
     */
    @Override
    public Optional<CaixaEntity> getUltimoFechado() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT c FROM CaixaEntity c " +
                "WHERE c.dataFechamento IS NOT NULL " +
                "ORDER BY c.dataFechamento DESC",
                CaixaEntity.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
        } finally { em.close(); }
    }

    // ── movimentar ────────────────────────────────────────────────────────────

    @Override
    public MovimentacaoCaixaEntity movimentar(Integer caixaId, String tipo, Float valor,
                                               String descricao, UsuarioEntity usuario) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = em.find(CaixaEntity.class, caixaId);
            if (caixa == null) throw new IllegalArgumentException("Caixa não encontrado: " + caixaId);
            if (caixa.getDataFechamento() != null) throw new IllegalStateException("Caixa já está fechado.");

            UsuarioEntity usuarioManaged = em.find(UsuarioEntity.class, usuario.getId());
            MovimentacaoCaixaEntity mov = new MovimentacaoCaixaEntity(caixa, tipo, valor, usuarioManaged);
            mov.setDescricao(descricao);
            em.persist(mov);
            tx.commit();
            return mov;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    // ── listarMovimentacoes ───────────────────────────────────────────────────

    @Override
    public List<MovimentacaoCaixaEntity> listarMovimentacoes(Integer caixaId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT m FROM MovimentacaoCaixaEntity m " +
                "WHERE m.caixa.id = :cid ORDER BY m.data",
                MovimentacaoCaixaEntity.class)
                .setParameter("cid", caixaId)
                .getResultList();
        } finally { em.close(); }
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    /**
     * Calcula o saldo atual do caixa somando entradas e subtraindo saídas
     * a partir do saldo inicial.
     */
    private float calcularSaldoAtual(CaixaEntity caixa, EntityManager em) {
        List<MovimentacaoCaixaEntity> movs = em.createQuery(
            "SELECT m FROM MovimentacaoCaixaEntity m WHERE m.caixa.id = :cid",
            MovimentacaoCaixaEntity.class)
            .setParameter("cid", caixa.getId())
            .getResultList();

        float saldo = caixa.getSaldoInicial();
        for (MovimentacaoCaixaEntity m : movs) {
            if ("ENTRADA".equals(m.getTipo())) {
                saldo += m.getValor();
            } else if ("SAIDA".equals(m.getTipo())) {
                saldo -= m.getValor();
            }
        }
        return saldo;
    }
}
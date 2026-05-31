package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.RegistroPontoEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.service.PontoService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PontoServiceImpl implements PontoService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public RegistroPontoEntity registrarEntrada(Integer usuarioId) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
            if (usuario == null) throw new IllegalArgumentException("Usuário não encontrado: " + usuarioId);

            LocalDate hoje = LocalDate.now();
            List<RegistroPontoEntity> existentes = buscarRegistroHoje(em, usuarioId, hoje);

            if (!existentes.isEmpty() && existentes.get(0).getHoraEntrada() != null)
                throw new IllegalStateException("Entrada já registrada hoje.");

            RegistroPontoEntity ponto = existentes.isEmpty()
                ? new RegistroPontoEntity(usuario, hoje)
                : existentes.get(0);
            ponto.setHoraEntrada(LocalDateTime.now());
            em.merge(ponto);
            tx.commit();
            return ponto;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public RegistroPontoEntity registrarSaida(Integer usuarioId) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            LocalDate hoje = LocalDate.now();
            List<RegistroPontoEntity> existentes = buscarRegistroHoje(em, usuarioId, hoje);

            if (existentes.isEmpty() || existentes.get(0).getHoraEntrada() == null)
                throw new IllegalStateException("Nenhuma entrada registrada hoje.");

            RegistroPontoEntity ponto = existentes.get(0);
            if (ponto.getHoraSaida() != null)
                throw new IllegalStateException("Saída já registrada hoje.");

            ponto.setHoraSaida(LocalDateTime.now());
            em.merge(ponto);
            tx.commit();
            return ponto;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public List<RegistroPontoEntity> listar(Integer usuarioId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT r FROM RegistroPontoEntity r WHERE r.usuario.id = :uid ORDER BY r.data DESC",
                RegistroPontoEntity.class)
                .setParameter("uid", usuarioId)
                .getResultList();
        } finally { em.close(); }
    }

    private List<RegistroPontoEntity> buscarRegistroHoje(EntityManager em, Integer usuarioId, LocalDate data) {
        return em.createQuery(
            "SELECT r FROM RegistroPontoEntity r WHERE r.usuario.id = :uid AND r.data = :data",
            RegistroPontoEntity.class)
            .setParameter("uid", usuarioId)
            .setParameter("data", data)
            .getResultList();
    }
}

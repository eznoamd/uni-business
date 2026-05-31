package com.unibusiness.service.impl;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.MovimentacaoEstoqueEntity;
import com.unibusiness.model.ProdutoEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.repository.GenericRepository;
import com.unibusiness.service.ProdutoService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;

public class ProdutoServiceImpl implements ProdutoService {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public ProdutoEntity create(ProdutoEntity produto) {
        EntityManager em = emf.createEntityManager();
        try {
            return new GenericRepository<>(ProdutoEntity.class, em).save(produto);
        } finally { em.close(); }
    }

    @Override
    public List<ProdutoEntity> listAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return new GenericRepository<>(ProdutoEntity.class, em).findAll();
        } finally { em.close(); }
    }

    @Override
    public Optional<ProdutoEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(new GenericRepository<>(ProdutoEntity.class, em).findById(id));
        } finally { em.close(); }
    }

    @Override
    public ProdutoEntity update(ProdutoEntity produto) {
        EntityManager em = emf.createEntityManager();
        try {
            return new GenericRepository<>(ProdutoEntity.class, em).save(produto);
        } finally { em.close(); }
    }

    @Override
    public void delete(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            GenericRepository<ProdutoEntity> repo = new GenericRepository<>(ProdutoEntity.class, em);
            ProdutoEntity p = repo.findById(id);
            if (p == null) throw new IllegalArgumentException("Produto não encontrado: " + id);
            repo.delete(p);
        } finally { em.close(); }
    }

    @Override
    public MovimentacaoEstoqueEntity movimentar(Integer produtoId, String tipo, Integer quantidade, UsuarioEntity usuario) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProdutoEntity produto = em.find(ProdutoEntity.class, produtoId);
            if (produto == null) throw new IllegalArgumentException("Produto não encontrado: " + produtoId);

            if ("SAIDA".equalsIgnoreCase(tipo) && produto.getQuantidade() < quantidade)
                throw new IllegalStateException("Estoque insuficiente.");

            int delta = "ENTRADA".equalsIgnoreCase(tipo) ? quantidade : -quantidade;
            produto.setQuantidade(produto.getQuantidade() + delta);
            em.merge(produto);

            UsuarioEntity usuarioManaged = em.find(UsuarioEntity.class, usuario.getId());
            MovimentacaoEstoqueEntity mov = new MovimentacaoEstoqueEntity(produto, tipo.toUpperCase(), quantidade, usuarioManaged);
            em.persist(mov);
            tx.commit();
            return mov;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    @Override
    public List<MovimentacaoEstoqueEntity> listarMovimentacoes(Integer produtoId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT m FROM MovimentacaoEstoqueEntity m WHERE m.produto.id = :pid ORDER BY m.data",
                MovimentacaoEstoqueEntity.class)
                .setParameter("pid", produtoId)
                .getResultList();
        } finally { em.close(); }
    }
}

package com.unibusiness.core.service.impl;
import com.unibusiness.core.config.PersistenceManager;
import com.unibusiness.core.repository.UsuarioRepository;
import com.unibusiness.core.service.UsuarioService;
import com.unibusiness.core.model.UsuarioEntity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
public class UsuarioServiceImpl implements UsuarioService {
    private final EntityManagerFactory emf;
    public UsuarioServiceImpl() { this.emf = PersistenceManager.getEntityManagerFactory(); }
    private EntityManager createEntityManager() { return emf.createEntityManager(); }
    public UsuarioEntity create(UsuarioEntity usuario) { EntityManager em = createEntityManager(); try { return new UsuarioRepository(em).save(usuario); } finally { em.close(); } }
    public UsuarioEntity update(UsuarioEntity usuario) { EntityManager em = createEntityManager(); try { return new UsuarioRepository(em).save(usuario); } finally { em.close(); } }
    public Optional<UsuarioEntity> findById(Integer id) { EntityManager em = createEntityManager(); try { return Optional.ofNullable(new UsuarioRepository(em).findById(id)); } finally { em.close(); } }
    public Optional<UsuarioEntity> findByEmail(String email) { EntityManager em = createEntityManager(); try { return new UsuarioRepository(em).findByEmail(email); } finally { em.close(); } }
    public List<UsuarioEntity> listAll() { EntityManager em = createEntityManager(); try { return new UsuarioRepository(em).findAll(); } finally { em.close(); } }

    @Override
    public void setOnline(Integer usuarioId, boolean online) {
        EntityManager em = createEntityManager();
        javax.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
            if (usuario != null) {
                usuario.setOnline(online);
                usuario.setUltimoAcessoEm(java.time.LocalDateTime.now());
                em.merge(usuario);
            }
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally { em.close(); }
    }

    /**
     * Verifica se o usuario tem a permissao "ADMIN_TOTAL" atraves de algum
     * dos seus cargos. Feito com uma query propria (em vez de navegar pelas
     * colecoes lazy de UsuarioEntity), pois a entidade pode estar "detached"
     * (EntityManager ja fechado) quando essa checagem acontece.
     */
    @Override
    public boolean isAdmin(Integer usuarioId) {
        EntityManager em = createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM UsuarioEntity u " +
                "JOIN u.cargos c JOIN c.permissoes p " +
                "WHERE u.id = :uid AND p.nome = 'ADMIN_TOTAL'", Long.class)
                .setParameter("uid", usuarioId)
                .getSingleResult();
            return count != null && count > 0;
        } finally { em.close(); }
    }

    @Override
    public UsuarioEntity criarComCargo(UsuarioEntity usuario, Integer cargoId) {
        EntityManager em = createEntityManager();
        javax.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(usuario);
            if (cargoId != null) {
                com.unibusiness.core.model.CargoEntity cargo =
                    em.find(com.unibusiness.core.model.CargoEntity.class, cargoId);
                if (cargo != null) usuario.getCargos().add(cargo);
            }
            tx.commit();
            return usuario;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally { em.close(); }
    }
}
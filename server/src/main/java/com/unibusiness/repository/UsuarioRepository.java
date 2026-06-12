package com.unibusiness.repository;
import com.unibusiness.model.UsuarioEntity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;
public class UsuarioRepository extends GenericRepository<UsuarioEntity> {
    public UsuarioRepository(EntityManager em) { super(UsuarioEntity.class, em); }
    public Optional<UsuarioEntity> findByEmail(String email) {
        TypedQuery<UsuarioEntity> query = em.createQuery("SELECT u FROM UsuarioEntity u WHERE u.email = :email", UsuarioEntity.class);
        query.setParameter("email", email);
        return query.getResultStream().findFirst();
    }
}
package com.unibusiness.core.repository;
import com.unibusiness.core.model.MensagemStatusEntity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
public class MensagemStatusRepository extends GenericRepository<MensagemStatusEntity> {
    public MensagemStatusRepository(EntityManager em) { super(MensagemStatusEntity.class, em); }
    public Optional<MensagemStatusEntity> findByMensagemAndUsuario(Integer mensagemId, Integer usuarioId) {
        TypedQuery<MensagemStatusEntity> q = em.createQuery("SELECT s FROM MensagemStatusEntity s WHERE s.mensagem.id = :mid AND s.usuario.id = :uid", MensagemStatusEntity.class);
        q.setParameter("mid", mensagemId); q.setParameter("uid", usuarioId);
        return q.getResultStream().findFirst();
    }
    public List<MensagemStatusEntity> findNaoLidasPorConversa(Integer usuarioId, Integer conversaId) {
        return em.createQuery("SELECT s FROM MensagemStatusEntity s WHERE s.usuario.id = :uid AND s.mensagem.conversa.id = :cid AND s.lida = false ORDER BY s.mensagem.enviadoEm", MensagemStatusEntity.class)
            .setParameter("uid", usuarioId).setParameter("cid", conversaId).getResultList();
    }
    public List<Object[]> contarNaoLidasPorConversa(Integer usuarioId) {
        return em.createQuery("SELECT s.mensagem.conversa.id, COUNT(s) FROM MensagemStatusEntity s WHERE s.usuario.id = :uid AND s.lida = false GROUP BY s.mensagem.conversa.id", Object[].class)
            .setParameter("uid", usuarioId).getResultList();
    }
    public Long contarTotalNaoLidas(Integer usuarioId) {
        return em.createQuery("SELECT COUNT(s) FROM MensagemStatusEntity s WHERE s.usuario.id = :uid AND s.lida = false", Long.class)
            .setParameter("uid", usuarioId).getSingleResult();
    }
    public int marcarConversaComoLida(Integer usuarioId, Integer conversaId) {
        return em.createQuery("UPDATE MensagemStatusEntity s SET s.lida = true, s.lidaEm = CURRENT_TIMESTAMP WHERE s.usuario.id = :uid AND s.mensagem.conversa.id = :cid AND s.lida = false")
            .setParameter("uid", usuarioId).setParameter("cid", conversaId).executeUpdate();
    }
}
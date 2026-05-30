package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.RegistroPontoEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles: PONTO_REGISTRAR_ENTRADA, PONTO_REGISTRAR_SAIDA, PONTO_LIST
 *
 * PONTO_REGISTRAR_ENTRADA / PONTO_REGISTRAR_SAIDA: usa o usuário autenticado, data de hoje
 * PONTO_LIST payload: { "usuarioId": 1 }  (opcional, sem payload lista do próprio usuário)
 */
public class PontoHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.PONTO_REGISTRAR_ENTRADA -> registrarEntrada(req, session);
            case Actions.PONTO_REGISTRAR_SAIDA   -> registrarSaida(req, session);
            case Actions.PONTO_LIST              -> listar(req, session);
            default -> Response.error(req.getAction(), "Action não suportada por PontoHandler.");
        };
    }

    private Response registrarEntrada(Request req, ClientSession session) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UsuarioEntity usuario = em.find(UsuarioEntity.class, session.getUsuario().getId());
            LocalDate hoje = LocalDate.now();

            // Verifica se já existe registro para hoje
            List<RegistroPontoEntity> existentes = em.createQuery(
                "SELECT r FROM RegistroPontoEntity r WHERE r.usuario.id = :uid AND r.data = :data",
                RegistroPontoEntity.class)
                .setParameter("uid", usuario.getId())
                .setParameter("data", hoje)
                .getResultList();

            if (!existentes.isEmpty() && existentes.get(0).getHoraEntrada() != null) {
                tx.rollback();
                return Response.error(Actions.PONTO_REGISTRAR_ENTRADA, "Entrada já registrada hoje.");
            }

            RegistroPontoEntity ponto = existentes.isEmpty()
                ? new RegistroPontoEntity(usuario, hoje)
                : existentes.get(0);
            ponto.setHoraEntrada(LocalDateTime.now());
            em.merge(ponto);
            tx.commit();

            return Response.ok(Actions.PONTO_REGISTRAR_ENTRADA, Map.of(
                "id",          ponto.getId(),
                "horaEntrada", ponto.getHoraEntrada().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.PONTO_REGISTRAR_ENTRADA, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response registrarSaida(Request req, ClientSession session) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            LocalDate hoje = LocalDate.now();
            List<RegistroPontoEntity> existentes = em.createQuery(
                "SELECT r FROM RegistroPontoEntity r WHERE r.usuario.id = :uid AND r.data = :data",
                RegistroPontoEntity.class)
                .setParameter("uid", session.getUsuario().getId())
                .setParameter("data", hoje)
                .getResultList();

            if (existentes.isEmpty() || existentes.get(0).getHoraEntrada() == null)
                { tx.rollback(); return Response.error(Actions.PONTO_REGISTRAR_SAIDA, "Nenhuma entrada registrada hoje."); }

            RegistroPontoEntity ponto = existentes.get(0);
            if (ponto.getHoraSaida() != null)
                { tx.rollback(); return Response.error(Actions.PONTO_REGISTRAR_SAIDA, "Saída já registrada hoje."); }

            ponto.setHoraSaida(LocalDateTime.now());
            em.merge(ponto);
            tx.commit();

            return Response.ok(Actions.PONTO_REGISTRAR_SAIDA, Map.of(
                "id",        ponto.getId(),
                "horaSaida", ponto.getHoraSaida().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.PONTO_REGISTRAR_SAIDA, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response listar(Request req, ClientSession session) {
        Integer uid = req.getInteger("usuarioId");
        if (uid == null) uid = session.getUsuario().getId();
        final int usuarioId = uid;

        EntityManager em = em();
        try {
            List<Map<String,Object>> list = em.createQuery(
                "SELECT r FROM RegistroPontoEntity r WHERE r.usuario.id = :uid ORDER BY r.data DESC",
                RegistroPontoEntity.class)
                .setParameter("uid", usuarioId)
                .getResultList().stream()
                .map(r -> {
                    Map<String,Object> m = new java.util.HashMap<>();
                    m.put("id",          r.getId());
                    m.put("data",        r.getData().toString());
                    m.put("horaEntrada", r.getHoraEntrada() != null ? r.getHoraEntrada().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                    m.put("horaSaida",   r.getHoraSaida()  != null ? r.getHoraSaida().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)  : null);
                    return m;
                })
                .collect(Collectors.toList());
            return Response.ok(Actions.PONTO_LIST, list);
        } finally { em.close(); }
    }

    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }
}

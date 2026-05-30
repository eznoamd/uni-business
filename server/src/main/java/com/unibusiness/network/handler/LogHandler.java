package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.LogSistemaEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

import javax.persistence.EntityManager;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles: LOG_LIST
 * payload opcional: { "usuarioId": 1, "limit": 50 }
 */
public class LogHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        Integer uid   = req.getInteger("usuarioId");
        Integer limit = req.getInteger("limit");
        if (limit == null) limit = 100;

        EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager();
        try {
            String jpql = uid != null
                ? "SELECT l FROM LogSistemaEntity l WHERE l.usuario.id = :uid ORDER BY l.data DESC"
                : "SELECT l FROM LogSistemaEntity l ORDER BY l.data DESC";

            var query = em.createQuery(jpql, LogSistemaEntity.class);
            if (uid != null) query.setParameter("uid", uid);
            query.setMaxResults(limit);

            List<Map<String,Object>> list = query.getResultList().stream()
                .map(l -> Map.<String,Object>of(
                    "id",       l.getId(),
                    "acao",     l.getAcao(),
                    "data",     l.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "detalhes", l.getDetalhes() != null ? l.getDetalhes() : ""
                ))
                .collect(Collectors.toList());

            return Response.ok(Actions.LOG_LIST, list);
        } finally { em.close(); }
    }
}

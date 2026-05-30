package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.CaixaEntity;
import com.unibusiness.model.MovimentacaoCaixaEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CaixaHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CAIXA_ABRIR         -> abrir(req, session);
            case Actions.CAIXA_FECHAR        -> fechar(req);
            case Actions.CAIXA_GET           -> get(req);
            case Actions.CAIXA_MOVIMENTAR    -> movimentar(req, session);
            case Actions.CAIXA_MOVIMENTACOES -> listarMovimentacoes(req);
            default -> Response.error(req.getAction(), "Action não suportada por CaixaHandler.");
        };
    }

    private Response abrir(Request req, ClientSession session) {
        Number saldo = (Number) req.get("saldoInicial");
        float saldoInicial = saldo != null ? saldo.floatValue() : 0F;

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = new CaixaEntity(saldoInicial);
            em.persist(caixa);
            tx.commit();
            return Response.ok(Actions.CAIXA_ABRIR, Map.of(
                "id",           caixa.getId(),
                "saldoInicial", caixa.getSaldoInicial(),
                "dataAbertura", caixa.getDataAbertura().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.CAIXA_ABRIR, "Erro ao abrir caixa: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response fechar(Request req) {
        Integer id = req.getInteger("id");
        Number saldoFinal = (Number) req.get("saldoFinal");
        if (id == null) return Response.error(Actions.CAIXA_FECHAR, "Campo 'id' obrigatório.");

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = em.find(CaixaEntity.class, id);
            if (caixa == null) { tx.rollback(); return Response.error(Actions.CAIXA_FECHAR, "Caixa não encontrado."); }
            if (caixa.getDataFechamento() != null) { tx.rollback(); return Response.error(Actions.CAIXA_FECHAR, "Caixa já está fechado."); }
            caixa.setDataFechamento(LocalDateTime.now());
            if (saldoFinal != null) caixa.setSaldoFinal(saldoFinal.floatValue());
            em.merge(caixa);
            tx.commit();
            return Response.ok(Actions.CAIXA_FECHAR, Map.of("id", caixa.getId(), "dataFechamento",
                caixa.getDataFechamento().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.CAIXA_FECHAR, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.CAIXA_GET, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            CaixaEntity c = em.find(CaixaEntity.class, id);
            if (c == null) return Response.error(Actions.CAIXA_GET, "Caixa não encontrado.");
            return Response.ok(Actions.CAIXA_GET, toMap(c));
        } finally { em.close(); }
    }

    private Response movimentar(Request req, ClientSession session) {
        Integer caixaId = req.getInteger("caixaId");
        String  tipo    = req.getString("tipo");
        Number  valor   = (Number) req.get("valor");
        if (caixaId == null || tipo == null || valor == null)
            return Response.error(Actions.CAIXA_MOVIMENTAR, "Campos 'caixaId', 'tipo' e 'valor' obrigatórios.");

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CaixaEntity caixa = em.find(CaixaEntity.class, caixaId);
            if (caixa == null) { tx.rollback(); return Response.error(Actions.CAIXA_MOVIMENTAR, "Caixa não encontrado."); }
            UsuarioEntity usuario = em.find(UsuarioEntity.class, session.getUsuario().getId());
            MovimentacaoCaixaEntity mov = new MovimentacaoCaixaEntity(caixa, tipo.toUpperCase(), valor.floatValue(), usuario);
            mov.setDescricao(req.getString("descricao"));
            em.persist(mov);
            tx.commit();
            return Response.ok(Actions.CAIXA_MOVIMENTAR, Map.of("movimentacaoId", mov.getId()));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.CAIXA_MOVIMENTAR, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response listarMovimentacoes(Request req) {
        Integer caixaId = req.getInteger("caixaId");
        if (caixaId == null) return Response.error(Actions.CAIXA_MOVIMENTACOES, "Campo 'caixaId' obrigatório.");
        EntityManager em = em();
        try {
            List<Map<String,Object>> list = em.createQuery(
                "SELECT m FROM MovimentacaoCaixaEntity m WHERE m.caixa.id = :cid ORDER BY m.data",
                MovimentacaoCaixaEntity.class)
                .setParameter("cid", caixaId)
                .getResultList().stream()
                .map(m -> Map.<String,Object>of(
                    "id",    m.getId(),
                    "tipo",  m.getTipo(),
                    "valor", m.getValor(),
                    "data",  m.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "descricao", m.getDescricao() != null ? m.getDescricao() : ""
                ))
                .collect(Collectors.toList());
            return Response.ok(Actions.CAIXA_MOVIMENTACOES, list);
        } finally { em.close(); }
    }

    private Map<String,Object> toMap(CaixaEntity c) {
        Map<String,Object> m = new java.util.HashMap<>();
        m.put("id",           c.getId());
        m.put("saldoInicial", c.getSaldoInicial());
        m.put("dataAbertura", c.getDataAbertura().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("saldoFinal",   c.getSaldoFinal());
        m.put("dataFechamento", c.getDataFechamento() != null
            ? c.getDataFechamento().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return m;
    }

    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }
}

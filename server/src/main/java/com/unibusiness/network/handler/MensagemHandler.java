package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.GenericRepository;
import com.unibusiness.repository.MensagemRepository;
import com.unibusiness.repository.UsuarioRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles: CONVERSA_CREATE, CONVERSA_LIST, MENSAGEM_SEND, MENSAGEM_LIST
 *
 * CONVERSA_CREATE payload:
 *   { "tipo": "PRIVADO|GRUPO|BROADCAST", "participanteIds": [1,2,...] }
 *
 * MENSAGEM_SEND payload:
 *   { "conversaId": 5, "conteudo": "Olá!" }
 *   → persiste e faz PUSH_MENSAGEM para todos os participantes online
 *
 * MENSAGEM_LIST payload:
 *   { "conversaId": 5 }
 *
 * CONVERSA_LIST: lista conversas do usuário autenticado
 */
public class MensagemHandler implements ActionHandler {

    private final SessionStore sessions = SessionStore.getInstance();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CONVERSA_CREATE -> criarConversa(req, session);
            case Actions.CONVERSA_LIST   -> listarConversas(req, session);
            case Actions.MENSAGEM_SEND   -> enviarMensagem(req, session);
            case Actions.MENSAGEM_LIST   -> listarMensagens(req);
            default -> Response.error(req.getAction(), "Action não suportada por MensagemHandler.");
        };
    }

    // ── CONVERSA_CREATE ───────────────────────────────────────────────────────

    private Response criarConversa(Request req, ClientSession session) {
        String tipo = req.getString("tipo");
        if (tipo == null) return Response.error(Actions.CONVERSA_CREATE, "Campo 'tipo' obrigatório.");

        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) req.get("participanteIds");
        if (ids == null || ids.isEmpty()) {
            return Response.error(Actions.CONVERSA_CREATE, "Campo 'participanteIds' obrigatório.");
        }

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            ConversaEntity conversa = new ConversaEntity(tipo.toUpperCase());
            UsuarioRepository usuarioRepo = new UsuarioRepository(em);

            // Adiciona o próprio usuário autenticado
            Set<UsuarioEntity> participantes = new HashSet<>();
            participantes.add(session.getUsuario());
            for (Number id : ids) {
                UsuarioEntity p = usuarioRepo.findById(id.intValue());
                if (p != null) participantes.add(p);
            }
            conversa.setParticipantes(participantes);

            em.persist(conversa);

            // Atualiza o lado do usuário (dono da JoinTable)
            for (UsuarioEntity p : participantes) {
                UsuarioEntity managed = em.merge(p);
                managed.getConversas().add(conversa);
            }

            tx.commit();

            return Response.ok(Actions.CONVERSA_CREATE, Map.of(
                "id",   conversa.getId(),
                "tipo", conversa.getTipo()
            ));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.CONVERSA_CREATE, "Erro ao criar conversa: " + e.getMessage());
        } finally { em.close(); }
    }

    // ── CONVERSA_LIST ─────────────────────────────────────────────────────────

    private Response listarConversas(Request req, ClientSession session) {
        EntityManager em = em();
        try {
            UsuarioEntity usuario = em.find(UsuarioEntity.class, session.getUsuario().getId());
            if (usuario == null) return Response.error(Actions.CONVERSA_LIST, "Usuário não encontrado.");

            // Força carregamento lazy da coleção de conversas
            List<Map<String, Object>> conversas = usuario.getConversas().stream()
                .map(c -> Map.<String, Object>of(
                    "id",   c.getId(),
                    "tipo", c.getTipo()
                ))
                .collect(Collectors.toList());

            return Response.ok(Actions.CONVERSA_LIST, conversas);
        } finally { em.close(); }
    }

    // ── MENSAGEM_SEND ─────────────────────────────────────────────────────────

    private Response enviarMensagem(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        String  conteudo   = req.getString("conteudo");

        if (conversaId == null || conteudo == null || conteudo.isBlank()) {
            return Response.error(Actions.MENSAGEM_SEND, "Campos 'conversaId' e 'conteudo' obrigatórios.");
        }

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            ConversaEntity conversa = em.find(ConversaEntity.class, conversaId);
            if (conversa == null) {
                tx.rollback();
                return Response.error(Actions.MENSAGEM_SEND, "Conversa não encontrada.");
            }

            UsuarioEntity remetente = em.find(UsuarioEntity.class, session.getUsuario().getId());

            MensagemEntity msg = new MensagemEntity(conversa, remetente, conteudo);
            em.persist(msg);
            tx.commit();

            // ── Push em tempo real ────────────────────────────────────────────
            Map<String, Object> pushPayload = Map.of(
                "mensagemId",  msg.getId(),
                "conversaId",  conversaId,
                "remetenteId", remetente.getId(),
                "remetente",   remetente.getNome(),
                "conteudo",    conteudo,
                "enviadoEm",   msg.getEnviadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            Response push = Response.push(Actions.PUSH_MENSAGEM, pushPayload);

            // Carrega participantes e notifica cada um que estiver online
            conversa.getParticipantes().forEach(participante -> {
                ClientSession dest = sessions.getByUsuarioId(participante.getId());
                if (dest != null && dest.isConnected()) {
                    dest.send(push);
                }
            });

            return Response.ok(Actions.MENSAGEM_SEND, "Mensagem enviada.", Map.of("mensagemId", msg.getId()));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.MENSAGEM_SEND, "Erro ao enviar mensagem: " + e.getMessage());
        } finally { em.close(); }
    }

    // ── MENSAGEM_LIST ─────────────────────────────────────────────────────────

    private Response listarMensagens(Request req) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null) return Response.error(Actions.MENSAGEM_LIST, "Campo 'conversaId' obrigatório.");

        EntityManager em = em();
        try {
            List<Map<String, Object>> lista = new MensagemRepository(em)
                .findByConversaId(conversaId)
                .stream()
                .map(m -> Map.<String, Object>of(
                    "id",          m.getId(),
                    "conversaId",  m.getConversa().getId(),
                    "remetenteId", m.getRemetente().getId(),
                    "remetente",   m.getRemetente().getNome(),
                    "conteudo",    m.getConteudo(),
                    "enviadoEm",   m.getEnviadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());

            return Response.ok(Actions.MENSAGEM_LIST, lista);
        } finally { em.close(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EntityManager em() {
        return PersistenceManager.getEntityManagerFactory().createEntityManager();
    }
}

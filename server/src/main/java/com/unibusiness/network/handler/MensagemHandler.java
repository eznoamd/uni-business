package com.unibusiness.network.handler;

import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.service.ConversaService;
import com.unibusiness.service.impl.ConversaServiceImpl;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MensagemHandler implements ActionHandler {

    private final ConversaService service  = new ConversaServiceImpl();
    private final SessionStore    sessions = SessionStore.getInstance();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CONVERSA_CREATE      -> criarConversa(req, session);
            case Actions.CONVERSA_LIST        -> listarConversas(session);
            case Actions.MENSAGEM_SEND        -> enviarMensagem(req, session);
            case Actions.MENSAGEM_LIST        -> listarMensagens(req);
            case Actions.MENSAGEM_MARCAR_LIDA -> marcarLida(req, session);
            case Actions.MENSAGEM_NAO_LIDAS   -> naoLidas(session);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    // ── CONVERSA_CREATE ───────────────────────────────────────────────────────

    private Response criarConversa(Request req, ClientSession session) {
        String tipo = req.getString("tipo");
        if (tipo == null) return Response.error(Actions.CONVERSA_CREATE, "Campo 'tipo' obrigatório.");

        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) req.get("participanteIds");
        if (ids == null || ids.isEmpty())
            return Response.error(Actions.CONVERSA_CREATE, "Campo 'participanteIds' obrigatório.");

        try {
            Set<Integer> participanteIds = ids.stream()
                .map(Number::intValue)
                .collect(Collectors.toCollection(HashSet::new));

            ConversaEntity conversa = service.criar(tipo, session.getUsuario().getId(), participanteIds);
            return Response.ok(Actions.CONVERSA_CREATE, Map.of(
                "id",   conversa.getId(),
                "tipo", conversa.getTipo()
            ));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CONVERSA_CREATE, e.getMessage());
        }
    }

    // ── CONVERSA_LIST ─────────────────────────────────────────────────────────

    private Response listarConversas(ClientSession session) {
        try {
            // Inclui contador de não lidas em cada conversa da listagem
            Map<Integer, Long> naoLidas = service.contarNaoLidasPorConversa(session.getUsuario().getId());

            List<Map<String, Object>> lista = service.listarPorUsuario(session.getUsuario().getId())
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",        c.getId());
                    m.put("tipo",      c.getTipo());
                    m.put("naoLidas",  naoLidas.getOrDefault(c.getId(), 0L));
                    return m;
                })
                .collect(Collectors.toList());
            return Response.ok(Actions.CONVERSA_LIST, lista);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CONVERSA_LIST, e.getMessage());
        }
    }

    // ── MENSAGEM_SEND ─────────────────────────────────────────────────────────

    private Response enviarMensagem(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        String  conteudo   = req.getString("conteudo");

        if (conversaId == null || conteudo == null || conteudo.isBlank())
            return Response.error(Actions.MENSAGEM_SEND, "Campos 'conversaId' e 'conteudo' obrigatórios.");

        try {
            MensagemEntity msg = service.enviarMensagem(
                conversaId, session.getUsuario().getId(), conteudo
            );

            // Push em tempo real para participantes online (exceto remetente)
            Map<String, Object> pushPayload = Map.of(
                "mensagemId",  msg.getId(),
                "conversaId",  conversaId,
                "remetenteId", msg.getRemetente().getId(),
                "remetente",   msg.getRemetente().getNome(),
                "conteudo",    conteudo,
                "enviadoEm",   msg.getEnviadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            Response push = Response.push(Actions.PUSH_MENSAGEM, pushPayload);

            msg.getConversa().getParticipantes().forEach(participante -> {
                if (participante.getId().equals(session.getUsuario().getId())) return; // pula remetente
                ClientSession dest = sessions.getByUsuarioId(participante.getId());
                if (dest != null && dest.isConnected()) {
                    dest.send(push);
                }
            });

            return Response.ok(Actions.MENSAGEM_SEND, "Mensagem enviada.", Map.of("mensagemId", msg.getId()));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.MENSAGEM_SEND, e.getMessage());
        }
    }

    // ── MENSAGEM_LIST ─────────────────────────────────────────────────────────

    private Response listarMensagens(Request req) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null) return Response.error(Actions.MENSAGEM_LIST, "Campo 'conversaId' obrigatório.");

        List<Map<String, Object>> lista = service.listarMensagens(conversaId).stream()
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
    }

    // ── MENSAGEM_MARCAR_LIDA ──────────────────────────────────────────────────

    /**
     * O client chama isso quando o usuário abre uma conversa.
     * Marca todas as mensagens da conversa como lidas para aquele usuário
     * e notifica os outros participantes online com PUSH_MENSAGEM_LIDA.
     *
     * payload: { "conversaId": 1 }
     */
    private Response marcarLida(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null) return Response.error(Actions.MENSAGEM_MARCAR_LIDA, "Campo 'conversaId' obrigatório.");

        try {
            int marcadas = service.marcarConversaComoLida(session.getUsuario().getId(), conversaId);

            // Notifica outros participantes online que este usuário leu as mensagens
            // (útil para mostrar "✓✓ lido" no client de quem enviou)
            if (marcadas > 0) {
                Response pushLida = Response.push(Actions.PUSH_MENSAGEM_LIDA, Map.of(
                    "conversaId", conversaId,
                    "usuarioId",  session.getUsuario().getId(),
                    "usuario",    session.getUsuario().getNome()
                ));

                service.listarPorUsuario(session.getUsuario().getId()).stream()
                    .filter(c -> c.getId().equals(conversaId))
                    .findFirst()
                    .ifPresent(conversa -> conversa.getParticipantes().forEach(participante -> {
                        if (participante.getId().equals(session.getUsuario().getId())) return;
                        ClientSession dest = sessions.getByUsuarioId(participante.getId());
                        if (dest != null && dest.isConnected()) {
                            dest.send(pushLida);
                        }
                    }));
            }

            return Response.ok(Actions.MENSAGEM_MARCAR_LIDA,
                "Mensagens marcadas como lidas.",
                Map.of("marcadas", marcadas)
            );
        } catch (Exception e) {
            return Response.error(Actions.MENSAGEM_MARCAR_LIDA, e.getMessage());
        }
    }

    // ── MENSAGEM_NAO_LIDAS ────────────────────────────────────────────────────

    /**
     * Retorna contadores de não lidas por conversa para o usuário autenticado.
     * Resposta: { "conversas": { "1": 3, "5": 1 } }
     *
     * payload: (nenhum)
     */
    private Response naoLidas(ClientSession session) {
        Map<Integer, Long> contadores = service.contarNaoLidasPorConversa(session.getUsuario().getId());
        return Response.ok(Actions.MENSAGEM_NAO_LIDAS, Map.of("conversas", contadores));
    }

    // ── Método estático chamado pelo AuthHandler após LOGIN ───────────────────

    /**
     * Envia o push PUSH_NAOLIDADAS para uma sessão recém autenticada.
     * Deve ser chamado pelo AuthHandler logo após registrar a sessão.
     */
    public static void enviarPushNaoLidasAposLogin(ClientSession session, ConversaService conversaService) {
        Map<Integer, Long> contadores = conversaService.contarNaoLidasPorConversa(session.getUsuario().getId());

        // Só envia o push se tiver pelo menos uma mensagem não lida
        if (contadores.isEmpty()) return;

        long total = contadores.values().stream().mapToLong(Long::longValue).sum();

        Response push = Response.push(Actions.PUSH_NAOLIDADAS, Map.of(
            "total",     total,
            "conversas", contadores   // { conversaId: count, ... }
        ));
        session.send(push);
    }
}

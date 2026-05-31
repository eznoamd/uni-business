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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MensagemHandler implements ActionHandler {

    private final ConversaService service  = new ConversaServiceImpl();
    private final SessionStore    sessions = SessionStore.getInstance();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CONVERSA_CREATE -> criarConversa(req, session);
            case Actions.CONVERSA_LIST   -> listarConversas(session);
            case Actions.MENSAGEM_SEND   -> enviarMensagem(req, session);
            case Actions.MENSAGEM_LIST   -> listarMensagens(req);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

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

    private Response listarConversas(ClientSession session) {
        try {
            List<Map<String, Object>> lista = service.listarPorUsuario(session.getUsuario().getId())
                .stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "tipo", c.getTipo()))
                .collect(Collectors.toList());
            return Response.ok(Actions.CONVERSA_LIST, lista);
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CONVERSA_LIST, e.getMessage());
        }
    }

    private Response enviarMensagem(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        String  conteudo   = req.getString("conteudo");

        if (conversaId == null || conteudo == null || conteudo.isBlank())
            return Response.error(Actions.MENSAGEM_SEND, "Campos 'conversaId' e 'conteudo' obrigatórios.");

        try {
            MensagemEntity msg = service.enviarMensagem(
                conversaId, session.getUsuario().getId(), conteudo
            );

            // Push em tempo real para participantes online
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
}

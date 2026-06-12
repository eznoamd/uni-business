package com.unibusiness.network.handler;

import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.PushService;
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

    private static final Set<String> TIPOS_GRUPO_NEGOCIO =
        Set.of("SUPORTE", "PROJETO", "EQUIPE", "GERAL");

    private final ConversaService service  = new ConversaServiceImpl();
    private final SessionStore    sessions = SessionStore.getInstance();
    private final PushService     push     = PushService.getInstance();

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.CONVERSA_CREATE      -> criarConversa(req, session);
            case Actions.CONVERSA_LIST        -> listarConversas(session);
            case Actions.MENSAGEM_SEND        -> enviarMensagem(req, session);
            case Actions.MENSAGEM_LIST        -> listarMensagens(req);
            case Actions.MENSAGEM_MARCAR_LIDA -> marcarLida(req, session);
            case Actions.MENSAGEM_NAO_LIDAS   -> naoLidas(session);
            case Actions.USUARIO_DIGITANDO    -> usuarioDigitando(req, session);
            default -> Response.error(req.getAction(), "Action não suportada.");
        };
    }

    private Response criarConversa(Request req, ClientSession session) {
        String tipoStr = req.getString("tipo");
        if (tipoStr == null || tipoStr.isBlank())
            return Response.error(Actions.CONVERSA_CREATE, "Campo 'tipo' obrigatório.");

        tipoStr = tipoStr.toUpperCase();

        ConversaEntity.Tipo tipo;
        String nome;

        if (TIPOS_GRUPO_NEGOCIO.contains(tipoStr)) {
            tipo = ConversaEntity.Tipo.GRUPO;
            nome = req.getString("nome") != null ? req.getString("nome") : tipoStr;
        } else if ("DIRETO".equals(tipoStr) || "PRIVADA".equals(tipoStr)) {
            tipo = ConversaEntity.Tipo.PRIVADA;
            nome = null;
        } else if ("GRUPO".equals(tipoStr)) {
            tipo = ConversaEntity.Tipo.GRUPO;
            nome = req.getString("nome");
            if (nome == null || nome.isBlank())
                return Response.error(Actions.CONVERSA_CREATE, "Grupos precisam de um 'nome'.");
        } else {
            return Response.error(Actions.CONVERSA_CREATE,
                "Tipo inválido: " + tipoStr + ". Use PRIVADA, GRUPO, SUPORTE, PROJETO, EQUIPE, DIRETO ou GERAL.");
        }

        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) req.get("participanteIds");
        if (ids == null || ids.isEmpty())
            return Response.error(Actions.CONVERSA_CREATE, "Campo 'participanteIds' obrigatório.");

        try {
            Set<Integer> participanteIds = ids.stream()
                .map(Number::intValue)
                .collect(Collectors.toCollection(HashSet::new));

            ConversaEntity conversa = service.criar(tipo, nome, session.getUsuario().getId(), participanteIds);
            return Response.ok(Actions.CONVERSA_CREATE, Map.of(
                "id",   conversa.getId(),
                "tipo", conversa.getTipo().name(),
                "nome", resolveNome(conversa, session.getUsuario())
            ));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.CONVERSA_CREATE, e.getMessage());
        }
    }

    private Response listarConversas(ClientSession session) {
        try {
            Map<Integer, Long> naoLidas = service.contarNaoLidasPorConversa(session.getUsuario().getId());

            List<Map<String, Object>> lista = service.listarPorUsuario(session.getUsuario().getId())
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",       c.getId());
                    m.put("tipo",     c.getTipo().name());
                    m.put("nome",     resolveNome(c, session.getUsuario()));
                    m.put("naoLidas", naoLidas.getOrDefault(c.getId(), 0L));
                    if (c.getTipo() == ConversaEntity.Tipo.PRIVADA) {
                        outroParticipante(c, session.getUsuario()).ifPresent(outro -> {
                            m.put("outroUsuarioId", outro.getId());
                            m.put("online", sessions.getByUsuarioId(outro.getId()) != null);
                        });
                    }
                    return m;
                })
                .collect(Collectors.toList());
            return Response.ok(Actions.CONVERSA_LIST, lista);
        } catch (Exception e) {
            return Response.error(Actions.CONVERSA_LIST, e.getMessage());
        }
    }

    private Response enviarMensagem(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        String  conteudo   = req.getString("conteudo");

        if (conversaId == null)
            return Response.error(Actions.MENSAGEM_SEND, "Campo 'conversaId' obrigatório.");
        if (conteudo == null || conteudo.isBlank())
            return Response.error(Actions.MENSAGEM_SEND, "Campo 'conteudo' obrigatório.");

        try {
            MensagemEntity msg = service.enviarMensagem(
                conversaId, session.getUsuario().getId(), conteudo
            );

            Response pushMsg = Response.push(Actions.PUSH_MENSAGEM, Map.of(
                "mensagemId",  msg.getId(),
                "conversaId",  conversaId,
                "remetenteId", msg.getRemetente().getId(),
                "remetente",   msg.getRemetente().getNome(),
                "conteudo",    conteudo,
                "enviadoEm",   msg.getEnviadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));

            push.broadcastToConversa(msg.getConversa(), pushMsg, session.getUsuario().getId());

            return Response.ok(Actions.MENSAGEM_SEND, "Mensagem enviada.", Map.of("mensagemId", msg.getId()));
        } catch (IllegalArgumentException e) {
            return Response.error(Actions.MENSAGEM_SEND, e.getMessage());
        }
    }

    private Response listarMensagens(Request req) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null)
            return Response.error(Actions.MENSAGEM_LIST, "Campo 'conversaId' obrigatório.");

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

    private Response marcarLida(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null)
            return Response.error(Actions.MENSAGEM_MARCAR_LIDA, "Campo 'conversaId' obrigatório.");

        try {
            int marcadas = service.marcarConversaComoLida(session.getUsuario().getId(), conversaId);

            if (marcadas > 0) {
                Response pushLida = Response.push(Actions.PUSH_MENSAGEM_LIDA, Map.of(
                    "conversaId", conversaId,
                    "usuarioId",  session.getUsuario().getId(),
                    "usuario",    session.getUsuario().getNome()
                ));
                service.listarPorUsuario(session.getUsuario().getId()).stream()
                    .filter(c -> c.getId().equals(conversaId))
                    .findFirst()
                    .ifPresent(c -> push.broadcastToConversa(c, pushLida, session.getUsuario().getId()));
            }

            return Response.ok(Actions.MENSAGEM_MARCAR_LIDA, "Mensagens marcadas como lidas.",
                Map.of("marcadas", marcadas));
        } catch (Exception e) {
            return Response.error(Actions.MENSAGEM_MARCAR_LIDA, e.getMessage());
        }
    }

    private Response naoLidas(ClientSession session) {
        Map<Integer, Long> contadores = service.contarNaoLidasPorConversa(session.getUsuario().getId());
        return Response.ok(Actions.MENSAGEM_NAO_LIDAS, Map.of("conversas", contadores));
    }

    private Response usuarioDigitando(Request req, ClientSession session) {
        Integer conversaId = req.getInteger("conversaId");
        if (conversaId == null)
            return Response.error(Actions.USUARIO_DIGITANDO, "Campo 'conversaId' obrigatório.");

        boolean digitando = req.get("digitando") == null
            || Boolean.parseBoolean(req.getString("digitando"));

        Response pushDigitando = Response.push(Actions.PUSH_DIGITANDO, Map.of(
            "conversaId", conversaId,
            "usuarioId",  session.getUsuario().getId(),
            "nome",       session.getUsuario().getNome(),
            "digitando",  digitando
        ));

        service.listarPorUsuario(session.getUsuario().getId()).stream()
            .filter(c -> c.getId().equals(conversaId))
            .findFirst()
            .ifPresent(c -> push.broadcastToConversa(c, pushDigitando, session.getUsuario().getId()));

        return Response.ok(Actions.USUARIO_DIGITANDO, "Sinal enviado.", null);
    }

    private String resolveNome(ConversaEntity conversa, UsuarioEntity viewer) {
        if (conversa.getTipo() == ConversaEntity.Tipo.GRUPO) {
            return conversa.getNome() != null ? conversa.getNome() : "Grupo";
        }
        return conversa.getParticipantes().stream()
            .filter(p -> !p.getId().equals(viewer.getId()))
            .map(UsuarioEntity::getNome)
            .findFirst()
            .orElse("Conversa");
    }

    private Optional<UsuarioEntity> outroParticipante(ConversaEntity conversa, UsuarioEntity viewer) {
        return conversa.getParticipantes().stream()
            .filter(p -> !p.getId().equals(viewer.getId()))
            .findFirst();
    }

    public static void enviarPushNaoLidasAposLogin(ClientSession session, ConversaService conversaService) {
        Map<Integer, Long> contadores = conversaService.contarNaoLidasPorConversa(
            session.getUsuario().getId()
        );
        if (contadores.isEmpty()) return;

        long total = contadores.values().stream().mapToLong(Long::longValue).sum();
        Response pushNaoLidas = Response.push(Actions.PUSH_NAOLIDADAS, Map.of(
            "total",     total,
            "conversas", contadores
        ));
        session.send(pushNaoLidas);
    }
}

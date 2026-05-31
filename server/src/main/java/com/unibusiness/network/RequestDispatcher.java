package com.unibusiness.network;

import com.unibusiness.network.handler.*;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());

    private static final Set<String> PUBLIC_ACTIONS = Set.of(Actions.LOGIN);

    private final Map<String, ActionHandler> handlers = new HashMap<>();
    private final SessionStore sessionStore = SessionStore.getInstance();

    public RequestDispatcher() {
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(Actions.LOGIN,  new AuthHandler());
        handlers.put(Actions.LOGOUT, new AuthHandler());

        UsuarioHandler uh = new UsuarioHandler();
        handlers.put(Actions.USUARIO_CREATE,          uh);
        handlers.put(Actions.USUARIO_LIST,            uh);
        handlers.put(Actions.USUARIO_GET,             uh);
        handlers.put(Actions.USUARIO_UPDATE,          uh);
        handlers.put(Actions.USUARIO_DELETE,          uh);

        ProdutoHandler ph = new ProdutoHandler();
        handlers.put(Actions.PRODUTO_CREATE,          ph);
        handlers.put(Actions.PRODUTO_LIST,            ph);
        handlers.put(Actions.PRODUTO_GET,             ph);
        handlers.put(Actions.PRODUTO_UPDATE,          ph);
        handlers.put(Actions.PRODUTO_DELETE,          ph);
        handlers.put(Actions.ESTOQUE_MOVIMENTAR,      ph);
        handlers.put(Actions.ESTOQUE_MOVIMENTACOES,   ph);

        CaixaHandler ch = new CaixaHandler();
        handlers.put(Actions.CAIXA_ABRIR,             ch);
        handlers.put(Actions.CAIXA_FECHAR,            ch);
        handlers.put(Actions.CAIXA_GET,               ch);
        handlers.put(Actions.CAIXA_MOVIMENTAR,        ch);
        handlers.put(Actions.CAIXA_MOVIMENTACOES,     ch);

        TarefaHandler th = new TarefaHandler();
        handlers.put(Actions.TAREFA_CREATE,           th);
        handlers.put(Actions.TAREFA_LIST,             th);
        handlers.put(Actions.TAREFA_GET,              th);
        handlers.put(Actions.TAREFA_UPDATE,           th);
        handlers.put(Actions.TAREFA_DELETE,           th);

        EquipeHandler eh = new EquipeHandler();
        handlers.put(Actions.EQUIPE_CREATE,           eh);
        handlers.put(Actions.EQUIPE_LIST,             eh);
        handlers.put(Actions.EQUIPE_GET,              eh);
        handlers.put(Actions.EQUIPE_UPDATE,           eh);
        handlers.put(Actions.EQUIPE_DELETE,           eh);

        CargoHandler cargo = new CargoHandler();
        handlers.put(Actions.CARGO_CREATE,            cargo);
        handlers.put(Actions.CARGO_LIST,              cargo);
        handlers.put(Actions.CARGO_GET,               cargo);
        handlers.put(Actions.CARGO_DELETE,            cargo);
        handlers.put(Actions.PERMISSAO_CREATE,        cargo);
        handlers.put(Actions.PERMISSAO_LIST,          cargo);

        PontoHandler ponto = new PontoHandler();
        handlers.put(Actions.PONTO_REGISTRAR_ENTRADA, ponto);
        handlers.put(Actions.PONTO_REGISTRAR_SAIDA,   ponto);
        handlers.put(Actions.PONTO_LIST,              ponto);

        // Mensagens — inclui novas actions de leitura
        MensagemHandler msg = new MensagemHandler();
        handlers.put(Actions.CONVERSA_CREATE,         msg);
        handlers.put(Actions.CONVERSA_LIST,           msg);
        handlers.put(Actions.MENSAGEM_SEND,           msg);
        handlers.put(Actions.MENSAGEM_LIST,           msg);
        handlers.put(Actions.MENSAGEM_MARCAR_LIDA,    msg);
        handlers.put(Actions.MENSAGEM_NAO_LIDAS,      msg);

        handlers.put(Actions.LOG_LIST, new LogHandler());
    }

    public Response dispatch(Request request, ClientSession session) {
        String action = request.getAction();

        if (action == null || action.isBlank())
            return Response.error("UNKNOWN", "Campo 'action' obrigatório.");

        if (!PUBLIC_ACTIONS.contains(action)) {
            if (session == null || !sessionStore.isValid(request.getToken()))
                return Response.error(action, "Não autenticado. Faça LOGIN primeiro.");
        }

        ActionHandler handler = handlers.get(action);
        if (handler == null) {
            LOG.warning("Action desconhecida: " + action);
            return Response.error(action, "Action desconhecida: " + action);
        }

        try {
            return handler.handle(request, session);
        } catch (Exception e) {
            LOG.severe("Erro ao processar action " + action + ": " + e.getMessage());
            return Response.error(action, "Erro interno: " + e.getMessage());
        }
    }
}

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

/**
 * Dispatcher de actions.
 *
 * CORREÇÃO: registra Actions.CAIXA_GET_ATUAL no CaixaHandler.
 */
public class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());

    private static final Set<String> PUBLIC_ACTIONS = Set.of(Actions.LOGIN);

    private final Map<String, ActionHandler> handlers = new HashMap<>();
    private final SessionStore sessionStore = SessionStore.getInstance();

    public RequestDispatcher() {
        registerHandlers();
    }

    private void registerHandlers() {
        AuthHandler auth = new AuthHandler();
        handlers.put(Actions.LOGIN,  auth);
        handlers.put(Actions.LOGOUT, auth);

        UsuarioHandler usuario = new UsuarioHandler();
        handlers.put(Actions.USUARIO_CREATE, usuario);
        handlers.put(Actions.USUARIO_LIST,   usuario);
        handlers.put(Actions.USUARIO_GET,    usuario);
        handlers.put(Actions.USUARIO_UPDATE, usuario);
        handlers.put(Actions.USUARIO_DELETE, usuario);

        ProdutoHandler produto = new ProdutoHandler();
        handlers.put(Actions.PRODUTO_CREATE,        produto);
        handlers.put(Actions.PRODUTO_LIST,          produto);
        handlers.put(Actions.PRODUTO_GET,           produto);
        handlers.put(Actions.PRODUTO_UPDATE,        produto);
        handlers.put(Actions.PRODUTO_DELETE,        produto);
        handlers.put(Actions.ESTOQUE_MOVIMENTAR,    produto);
        handlers.put(Actions.ESTOQUE_MOVIMENTACOES, produto);

        CaixaHandler caixa = new CaixaHandler();
        handlers.put(Actions.CAIXA_ABRIR,         caixa);
        handlers.put(Actions.CAIXA_FECHAR,        caixa);
        handlers.put(Actions.CAIXA_GET,           caixa);
        handlers.put(Actions.CAIXA_GET_ATUAL,     caixa);   // NOVO
        handlers.put(Actions.CAIXA_MOVIMENTAR,    caixa);
        handlers.put(Actions.CAIXA_MOVIMENTACOES, caixa);

        TarefaHandler tarefa = new TarefaHandler();
        handlers.put(Actions.TAREFA_CREATE, tarefa);
        handlers.put(Actions.TAREFA_LIST,   tarefa);
        handlers.put(Actions.TAREFA_GET,    tarefa);
        handlers.put(Actions.TAREFA_UPDATE, tarefa);
        handlers.put(Actions.TAREFA_DELETE, tarefa);

        EquipeHandler equipe = new EquipeHandler();
        handlers.put(Actions.EQUIPE_CREATE, equipe);
        handlers.put(Actions.EQUIPE_LIST,   equipe);
        handlers.put(Actions.EQUIPE_GET,    equipe);
        handlers.put(Actions.EQUIPE_UPDATE, equipe);
        handlers.put(Actions.EQUIPE_DELETE, equipe);

        CargoHandler cargo = new CargoHandler();
        handlers.put(Actions.CARGO_CREATE,     cargo);
        handlers.put(Actions.CARGO_LIST,       cargo);
        handlers.put(Actions.CARGO_GET,        cargo);
        handlers.put(Actions.CARGO_DELETE,     cargo);
        handlers.put(Actions.PERMISSAO_CREATE, cargo);
        handlers.put(Actions.PERMISSAO_LIST,   cargo);

        PontoHandler ponto = new PontoHandler();
        handlers.put(Actions.PONTO_REGISTRAR_ENTRADA, ponto);
        handlers.put(Actions.PONTO_REGISTRAR_SAIDA,   ponto);
        handlers.put(Actions.PONTO_LIST,              ponto);

        MensagemHandler mensagem = new MensagemHandler();
        handlers.put(Actions.CONVERSA_CREATE,      mensagem);
        handlers.put(Actions.CONVERSA_LIST,        mensagem);
        handlers.put(Actions.MENSAGEM_SEND,        mensagem);
        handlers.put(Actions.MENSAGEM_LIST,        mensagem);
        handlers.put(Actions.MENSAGEM_MARCAR_LIDA, mensagem);
        handlers.put(Actions.MENSAGEM_NAO_LIDAS,   mensagem);
        handlers.put(Actions.USUARIO_DIGITANDO,    mensagem);

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
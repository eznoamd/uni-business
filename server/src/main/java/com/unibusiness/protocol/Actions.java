package com.unibusiness.protocol;

public final class Actions {
    private Actions() {}

    public static final String LOGIN  = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    public static final String USUARIO_CREATE = "USUARIO_CREATE";
    public static final String USUARIO_LIST   = "USUARIO_LIST";
    public static final String USUARIO_GET    = "USUARIO_GET";
    public static final String USUARIO_UPDATE = "USUARIO_UPDATE";
    public static final String USUARIO_DELETE = "USUARIO_DELETE";

    public static final String PRODUTO_CREATE        = "PRODUTO_CREATE";
    public static final String PRODUTO_LIST          = "PRODUTO_LIST";
    public static final String PRODUTO_GET           = "PRODUTO_GET";
    public static final String PRODUTO_UPDATE        = "PRODUTO_UPDATE";
    public static final String PRODUTO_DELETE        = "PRODUTO_DELETE";
    public static final String ESTOQUE_MOVIMENTAR    = "ESTOQUE_MOVIMENTAR";
    public static final String ESTOQUE_MOVIMENTACOES = "ESTOQUE_MOVIMENTACOES";

    public static final String CAIXA_ABRIR         = "CAIXA_ABRIR";
    public static final String CAIXA_FECHAR        = "CAIXA_FECHAR";
    public static final String CAIXA_GET           = "CAIXA_GET";
    public static final String CAIXA_GET_ATUAL     = "CAIXA_GET_ATUAL";    // NOVO
    public static final String CAIXA_MOVIMENTAR    = "CAIXA_MOVIMENTAR";
    public static final String CAIXA_MOVIMENTACOES = "CAIXA_MOVIMENTACOES";

    public static final String TAREFA_CREATE = "TAREFA_CREATE";
    public static final String TAREFA_LIST   = "TAREFA_LIST";
    public static final String TAREFA_GET    = "TAREFA_GET";
    public static final String TAREFA_UPDATE = "TAREFA_UPDATE";
    public static final String TAREFA_DELETE = "TAREFA_DELETE";

    public static final String EQUIPE_CREATE = "EQUIPE_CREATE";
    public static final String EQUIPE_LIST   = "EQUIPE_LIST";
    public static final String EQUIPE_GET    = "EQUIPE_GET";
    public static final String EQUIPE_UPDATE = "EQUIPE_UPDATE";
    public static final String EQUIPE_DELETE = "EQUIPE_DELETE";

    public static final String CARGO_CREATE     = "CARGO_CREATE";
    public static final String CARGO_LIST       = "CARGO_LIST";
    public static final String CARGO_GET        = "CARGO_GET";
    public static final String CARGO_DELETE     = "CARGO_DELETE";
    public static final String PERMISSAO_CREATE = "PERMISSAO_CREATE";
    public static final String PERMISSAO_LIST   = "PERMISSAO_LIST";

    public static final String PONTO_REGISTRAR_ENTRADA = "PONTO_REGISTRAR_ENTRADA";
    public static final String PONTO_REGISTRAR_SAIDA   = "PONTO_REGISTRAR_SAIDA";
    public static final String PONTO_LIST              = "PONTO_LIST";

    public static final String CONVERSA_CREATE      = "CONVERSA_CREATE";
    public static final String CONVERSA_LIST        = "CONVERSA_LIST";
    public static final String MENSAGEM_SEND        = "MENSAGEM_SEND";
    public static final String MENSAGEM_LIST        = "MENSAGEM_LIST";
    public static final String MENSAGEM_MARCAR_LIDA = "MENSAGEM_MARCAR_LIDA";
    public static final String MENSAGEM_NAO_LIDAS   = "MENSAGEM_NAO_LIDAS";
    public static final String USUARIO_DIGITANDO    = "USUARIO_DIGITANDO";

    public static final String PUSH_MENSAGEM      = "PUSH_MENSAGEM";
    public static final String PUSH_NAOLIDADAS    = "PUSH_NAOLIDADAS";
    public static final String PUSH_MENSAGEM_LIDA = "PUSH_MENSAGEM_LIDA";
    public static final String PUSH_STATUS_USUARIO = "PUSH_STATUS_USUARIO";
    public static final String PUSH_DIGITANDO     = "PUSH_DIGITANDO";

    public static final String LOG_LIST = "LOG_LIST";
}
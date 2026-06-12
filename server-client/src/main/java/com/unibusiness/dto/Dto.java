package com.unibusiness.dto;

/**
 * DTOs usados pelas telas (TableView, ListView, etc).
 *
 * Antes eram desserializados de JSON vindo do servidor TCP. Agora são
 * montados pelos services em com.unibusiness.service.* a partir das
 * entidades JPA (com.unibusiness.core.model.*).
 */
public final class Dto {

    private Dto() {}

    public static class Conversa {
        public Integer id;
        public String  tipo;
        public String  nome;
        public long    naoLidas;
        public Integer outroUsuarioId;
        public boolean online;
    }

    public static class Mensagem {
        public Integer id;
        public Integer conversaId;
        public Integer remetenteId;
        public String  remetente;
        public String  conteudo;
        public String  enviadoEm;
    }

    public static class Usuario {
        public Integer id;
        public String  nome;
        public String  email;
        public boolean ativo;

        public Integer getId()    { return id; }
        public String  getNome()  { return nome; }
        public String  getEmail() { return email; }
        public boolean isAtivo()  { return ativo; }
    }

    public static class Produto {
        public Integer id;
        public String  nome;
        public String  descricao;
        public Integer quantidade;
        public Float   precoUnitario;

        public Integer getId()            { return id; }
        public String  getNome()          { return nome; }
        public String  getDescricao()     { return descricao; }
        public Integer getQuantidade()    { return quantidade; }
        public Float   getPrecoUnitario() { return precoUnitario; }
    }

    public static class Tarefa {
        public Integer id;
        public String  titulo;
        public String  descricao;
        public String  status;
        public String  prioridade;
        public String  dataInicio;
        public String  dataFim;
        public Integer responsavelId;
        public String  responsavel;

        public Integer getId()            { return id; }
        public String  getTitulo()        { return titulo; }
        public String  getDescricao()     { return descricao; }
        public String  getStatus()        { return status; }
        public String  getPrioridade()    { return prioridade; }
        public String  getDataInicio()    { return dataInicio; }
        public String  getDataFim()       { return dataFim; }
        public Integer getResponsavelId() { return responsavelId; }
        public String  getResponsavel()   { return responsavel; }
    }

    public static class Equipe {
        public Integer id;
        public String  nome;

        public Integer getId()   { return id; }
        public String  getNome() { return nome; }
    }

    public static class Cargo {
        public Integer id;
        public String  nome;

        public Integer getId()   { return id; }
        public String  getNome() { return nome; }
    }

    public static class Caixa {
        public Integer id;
        public String  status;
        public Double  saldoInicial;
        public Double  saldoAtual;
        public String  aberturaEm;
        public String  fechamentoEm;

        public Integer getId()           { return id; }
        public String  getStatus()       { return status; }
        public Double  getSaldoInicial() { return saldoInicial != null ? saldoInicial : 0.0; }
        public Double  getSaldoAtual()   { return saldoAtual  != null ? saldoAtual  : 0.0; }
        public String  getAberturaEm()   { return aberturaEm; }
        public String  getFechamentoEm() { return fechamentoEm; }
    }

    public static class MovimentacaoCaixa {
        public Integer id;
        public String  tipo;
        public Double  valor;
        public String  descricao;
        public String  realizadaEm;

        public Integer getId()          { return id; }
        public String  getTipo()        { return tipo; }
        public Double  getValor()       { return valor; }
        public String  getDescricao()   { return descricao; }
        public String  getRealizadaEm() { return realizadaEm; }
    }

    public static class MovimentacaoEstoque {
        public Integer id;
        public Integer produtoId;
        public String  produto;
        public String  tipo;
        public Integer quantidade;
        public String  realizadaEm;

        public Integer getId()          { return id; }
        public Integer getProdutoId()   { return produtoId; }
        public String  getProduto()     { return produto; }
        public String  getTipo()        { return tipo; }
        public Integer getQuantidade()  { return quantidade; }
        public String  getRealizadaEm() { return realizadaEm; }
    }
}

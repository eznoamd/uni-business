package com.unibusiness.dto;

/**
 * Espelha os campos retornados pelo servidor em cada action.
 * Todos os campos são Strings/primitivos — sem entidades JPA no client.
 */
public final class Dto {

    private Dto() {}

    // ── Autenticação ──────────────────────────────────────────────────────────

    public static class UsuarioLogado {
        public String token;
        public Integer usuarioId;
        public String nome;
        public String email;
    }

    // ── Conversas ─────────────────────────────────────────────────────────────

    public static class Conversa {
        public Integer id;
        public String tipo;
        public long naoLidas;   // contador de não lidas (vem no CONVERSA_LIST)
    }

    // ── Mensagens ─────────────────────────────────────────────────────────────

    public static class Mensagem {
        public Integer id;
        public Integer conversaId;
        public Integer remetenteId;
        public String remetente;
        public String conteudo;
        public String enviadoEm;
    }

    // ── Push: nova mensagem ───────────────────────────────────────────────────

    public static class PushMensagem {
        public Integer mensagemId;
        public Integer conversaId;
        public Integer remetenteId;
        public String remetente;
        public String conteudo;
        public String enviadoEm;
    }

    // ── Push: contadores de não lidas após login ──────────────────────────────

    public static class PushNaoLidas {
        public long total;
        /** conversaId (como String pois JSON deserializa chaves como String) → count */
        public java.util.Map<String, Long> conversas;
    }

    // ── Push: conversa marcada como lida por outro participante ───────────────

    public static class PushMensagemLida {
        public Integer conversaId;
        public Integer usuarioId;
        public String usuario;
    }

    // ── Usuário ───────────────────────────────────────────────────────────────

    public static class Usuario {
        public Integer id;
        public String nome;
        public String email;
        public boolean ativo;
    }

    // ── Produto ───────────────────────────────────────────────────────────────

    public static class Produto {
        public Integer id;
        public String nome;
        public String descricao;
        public Integer quantidade;
        public Float precoUnitario;
    }
}

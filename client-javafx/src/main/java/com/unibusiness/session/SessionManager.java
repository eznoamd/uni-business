package com.unibusiness.session;

import com.unibusiness.dto.Dto;

/**
 * Singleton que guarda o estado da sessão autenticada do usuário local.
 *
 * Preenchido pelo AuthService após login bem-sucedido.
 * Limpado no logout ou ao fechar o app.
 *
 * O TcpClient consulta getToken() automaticamente em cada requisição.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String token;
    private Dto.UsuarioLogado usuario;

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    // ── Preenchimento ─────────────────────────────────────────────────────────

    public void iniciar(String token, Dto.UsuarioLogado usuario) {
        this.token   = token;
        this.usuario = usuario;
    }

    public void encerrar() {
        this.token   = null;
        this.usuario = null;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public String getToken()               { return token; }
    public Dto.UsuarioLogado getUsuario()  { return usuario; }
    public Integer getUsuarioId()          { return usuario != null ? usuario.usuarioId : null; }
    public String getNome()                { return usuario != null ? usuario.nome : null; }
    public boolean isLogado()              { return token != null; }
}
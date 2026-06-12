package com.unibusiness.session;

import com.unibusiness.core.model.UsuarioEntity;

/**
 * Singleton que guarda o estado da sessão do usuário logado.
 *
 * Antes guardava um token + Dto.UsuarioLogado vindos do servidor TCP.
 * Agora, como tudo roda no mesmo processo e fala direto com o banco,
 * guardamos a UsuarioEntity logada + se ela é administrador (cargo com
 * permissão ADMIN_TOTAL), calculado uma vez no login.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private UsuarioEntity usuario;
    private boolean admin;

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    // ── Preenchimento ─────────────────────────────────────────────────────────

    public void iniciar(UsuarioEntity usuario, boolean admin) {
        this.usuario = usuario;
        this.admin = admin;
    }

    public void encerrar() {
        this.usuario = null;
        this.admin = false;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public UsuarioEntity getUsuario()      { return usuario; }
    public Integer getUsuarioId()          { return usuario != null ? usuario.getId() : null; }
    public String getNome()                { return usuario != null ? usuario.getNome() : null; }
    public boolean isLogado()              { return usuario != null; }
    public boolean isAdmin()               { return admin; }
}

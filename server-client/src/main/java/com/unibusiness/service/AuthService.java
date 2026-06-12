package com.unibusiness.service;

import com.unibusiness.core.model.UsuarioEntity;
import com.unibusiness.core.service.UsuarioService;
import com.unibusiness.core.service.impl.UsuarioServiceImpl;
import com.unibusiness.core.util.PasswordUtil;
import com.unibusiness.session.SessionManager;

import java.util.Optional;

/**
 * Serviço de autenticação do client.
 *
 * Antes: enviava LOGIN/LOGOUT para o servidor TCP e guardava um token.
 * Agora: consulta o UsuarioEntity direto no banco via UsuarioService (JPA)
 * e guarda o usuário logado no SessionManager.
 *
 * "Online": como não há mais conexões TCP para indicar presença, o status
 * online/offline agora é um campo (online + ultimoAcessoEm) na própria
 * UsuarioEntity, atualizado aqui no login/logout. As outras instâncias do
 * app descobrem essa mudança via polling (ver ChatController).
 */
public class AuthService {

    private final UsuarioService usuarioService = new UsuarioServiceImpl();
    private String ultimoErro;

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * Autentica contra o banco e preenche o SessionManager em caso de sucesso.
     * @return true se login ok, false caso contrário (ver getUltimoErro())
     */
    public boolean login(String email, String senha) {
        Optional<UsuarioEntity> usuarioOpt = usuarioService.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            ultimoErro = "Usuário não encontrado.";
            return false;
        }

        UsuarioEntity usuario = usuarioOpt.get();

        if (!PasswordUtil.checkPassword(senha, usuario.getSenhaHash())) {
            ultimoErro = "Senha incorreta.";
            return false;
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            ultimoErro = "Usuário inativo.";
            return false;
        }

        SessionManager.getInstance().iniciar(usuario, usuarioService.isAdmin(usuario.getId()));
        usuarioService.setOnline(usuario.getId(), true);
        return true;
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    public void logout() {
        Integer id = SessionManager.getInstance().getUsuarioId();
        if (id != null) {
            usuarioService.setOnline(id, false);
        }
        SessionManager.getInstance().encerrar();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getUltimoErro() { return ultimoErro; }
}

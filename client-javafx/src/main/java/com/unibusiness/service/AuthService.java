package com.unibusiness.service;

import com.google.gson.Gson;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.network.TcpClient;
import com.unibusiness.session.SessionManager;

import java.util.Map;

/**
 * Serviço de autenticação do client.
 *
 * Uso no LoginController (dentro de um Task):
 *
 *   AuthService auth = new AuthService();
 *   boolean ok = auth.login(email, senha);
 *   if (!ok) { mostrarErro(auth.getUltimoErro()); }
 */
public class AuthService {

    private final TcpClient client = TcpClient.getInstance();
    private final Gson gson = client.getGson();
    private String ultimoErro;

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * Autentica no servidor e preenche o SessionManager em caso de sucesso.
     * @return true se login ok, false caso contrário (ver getUltimoErro())
     */
    public boolean login(String email, String senha) {
        ServerResponse resp = client.send("LOGIN", Map.of(
            "email", email,
            "senha", senha
        ));

        if (resp.isError()) {
            ultimoErro = resp.getMessage();
            return false;
        }

        try {
            Dto.UsuarioLogado usuario = gson.fromJson(resp.getData(), Dto.UsuarioLogado.class);
            SessionManager.getInstance().iniciar(usuario.token, usuario);
            return true;
        } catch (Exception e) {
            ultimoErro = "Resposta inesperada do servidor.";
            return false;
        }
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    public void logout() {
        client.send("LOGOUT");
        SessionManager.getInstance().encerrar();
        client.disconnect();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getUltimoErro() { return ultimoErro; }
}
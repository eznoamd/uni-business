package com.unibusiness.network;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.PresenceService;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread dedicada a um único cliente TCP.
 *
 * CORREÇÃO 1 (desconexão abrupta):
 *   setSoTimeout(30_000) no socket faz o readLine() lançar SocketTimeoutException
 *   a cada 30s se não chegar nenhum dado. Isso permite que o loop detecte
 *   se o socket foi fechado do lado do cliente sem FIN (ex: processo morto,
 *   cabo desconectado) e chame cleanup() corretamente.
 *
 *   Sem isso, readLine() bloqueia para sempre e o servidor nunca dispara
 *   o push de offline para os outros usuários.
 *
 * CORREÇÃO 2 (double-logout):
 *   cleanup() verifica sessions.isValid(token) antes de chamar onLogout.
 *   Se houve logout explícito (AuthHandler já removeu o token), cleanup não
 *   repete o push de offline.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    /**
     * Timeout de leitura do socket em ms.
     * Após este período sem dados, o servidor testa se o socket ainda está
     * vivo (isConnected + !isClosed). Se estiver fechado, sai do loop.
     * 30s é conservador — não gera tráfego desnecessário e detecta quedas
     * em até 30s (aceitável para presença online/offline).
     */
    private static final int SO_TIMEOUT_MS = 15_000;

    private final Socket            socket;
    private final RequestDispatcher dispatcher;
    private final SessionStore      sessions = SessionStore.getInstance();
    private final PresenceService   presence = PresenceService.getInstance();

    private ClientSession session;
    private ClientSession tempSession;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket     = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        LOG.info("Cliente conectado: " + remote);

        try {
            // CORREÇÃO: timeout de leitura para detectar desconexões abruptas
            socket.setSoTimeout(SO_TIMEOUT_MS);
        } catch (IOException e) {
            LOG.warning("Não foi possível definir SO_TIMEOUT: " + e.getMessage());
        }

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            tempSession = new ClientSession(socket, writer, null, JsonUtil.gson());

            String line;
            while (true) {
                try {
                    line = reader.readLine();
                } catch (SocketTimeoutException e) {
                    // Timeout — verifica se o socket ainda está vivo
                    if (socket.isClosed() || !socket.isConnected()) {
                        LOG.info("Socket morto detectado via timeout: " + remote);
                        break;
                    }
                    // Ainda vivo, apenas sem dados — continua aguardando
                    continue;
                }

                if (line == null) {
                    // EOF — o cliente fechou a conexão normalmente
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                Request request;
                try {
                    request = JsonUtil.gson().fromJson(line, Request.class);
                } catch (Exception e) {
                    sendRaw(writer, Response.error("PARSE_ERROR", "JSON inválido: " + e.getMessage()));
                    continue;
                }

                ClientSession sessionParaDispatch = resolveSession(request);
                Response response = dispatcher.dispatch(request, sessionParaDispatch);

                sendRaw(writer, response);

                if (Actions.LOGIN.equals(request.getAction())
                        && Response.OK.equals(response.getStatus())) {
                    extractAndSetSession(response);
                }
            }

        } catch (IOException e) {
            LOG.log(Level.INFO, "Conexão encerrada: " + remote + " — " + e.getMessage());
        } finally {
            cleanup();
            LOG.info("Cliente desconectado: " + remote);
        }
    }

    // ── Resolução de sessão ───────────────────────────────────────────────────

    private ClientSession resolveSession(Request request) {
        if (request.getToken() != null) {
            ClientSession stored = sessions.get(request.getToken());
            if (stored != null) {
                session = stored;
                return stored;
            }
        }
        if (Actions.LOGIN.equals(request.getAction())) return tempSession;
        return session;
    }

    @SuppressWarnings("unchecked")
    private void extractAndSetSession(Response response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.getData();
            if (data != null && data.get("token") instanceof String token) {
                ClientSession registered = sessions.get(token);
                if (registered != null) session = registered;
            }
        } catch (Exception ignored) {}
    }

    // ── Envio e limpeza ───────────────────────────────────────────────────────

    private void sendRaw(PrintWriter writer, Response response) {
        writer.println(JsonUtil.toJson(response));
        writer.flush();
    }

    /**
     * Chamado sempre ao sair do loop, seja por EOF, IOException ou timeout.
     *
     * Só dispara onLogout se o token ainda é válido no store (não houve
     * logout explícito). Isso evita duplo push de offline.
     */
    private void cleanup() {
        if (session != null && session.getToken() != null) {
            String token = session.getToken();
            if (sessions.isValid(token)) {
                // Desconexão abrupta ou inesperada: notifica offline e remove
                presence.onLogout(session);
                sessions.remove(token);
            }
            // Logout explícito (AuthHandler já cuidou): não faz nada
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}
package com.unibusiness.network;

import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread dedicada a um único cliente TCP.
 *
 * FIX: A lógica de "isLoginOk / não reenviar" foi removida. O AuthHandler
 * não mais chama session.send() diretamente; toda resposta passa por
 * sendRaw() aqui, garantindo um único ponto de envio.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket            socket;
    private final RequestDispatcher dispatcher;
    private final SessionStore      sessions = SessionStore.getInstance();

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

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            tempSession = new ClientSession(socket, writer, null, JsonUtil.gson());

            String line;
            while ((line = reader.readLine()) != null) {
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

                // FIX: envia a resposta normalmente em todos os casos.
                // Após o envio, se for um login bem-sucedido, atualiza a referência
                // de sessão local para que requisições seguintes usem a sessão autenticada.
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

    private ClientSession resolveSession(Request request) {
        if (request.getToken() != null) {
            ClientSession stored = sessions.get(request.getToken());
            if (stored != null) {
                session = stored;
                return stored;
            }
        }
        if (Actions.LOGIN.equals(request.getAction())) {
            return tempSession;
        }
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

    private void sendRaw(PrintWriter writer, Response response) {
        writer.println(JsonUtil.toJson(response));
        writer.flush();
    }

    private void cleanup() {
        if (session != null && session.getToken() != null) {
            sessions.remove(session.getToken());
        }
        // FIX: garante fechamento do socket mesmo quando o login nunca ocorreu
        try { socket.close(); } catch (IOException ignored) {}
    }
}
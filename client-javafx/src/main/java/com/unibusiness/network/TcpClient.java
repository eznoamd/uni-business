package com.unibusiness.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.session.SessionManager;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cliente TCP singleton para o UniBusiness Server.
 *
 * Arquitetura de duas threads:
 *
 *   Thread de leitura (push-reader):
 *     Fica em loop lendo linhas do socket.
 *     Se a action for um PUSH_*, entrega ao PushListener.
 *     Se for uma resposta normal, coloca na responseQueue.
 *
 *   Thread principal (JavaFX / Task):
 *     Chama send() e depois aguarda responseQueue.poll() com timeout.
 *     Isso garante que envio e recebimento sejam sincronizados sem bloquear a UI.
 *
 * Uso básico:
 *
 *   TcpClient client = TcpClient.getInstance();
 *   client.connect("localhost", 7777);
 *   client.setPushListener(meuController);
 *
 *   ServerResponse resp = client.send("LOGIN", Map.of("email","...","senha","..."));
 *   if (resp.isOk()) { ... }
 */
public class TcpClient {

    private static final Logger LOG = Logger.getLogger(TcpClient.class.getName());
    private static final int TIMEOUT_SEGUNDOS = 10;

    private static TcpClient instance;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private Socket socket;
    private PrintWriter writer;
    private Thread readerThread;

    /** Fila onde a thread de leitura deposita respostas síncronas. */
    private final BlockingQueue<ServerResponse> responseQueue = new ArrayBlockingQueue<>(1);

    /** Listener que recebe pushes assíncronos do servidor. */
    private volatile PushListener pushListener;

    private volatile boolean connected = false;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private TcpClient() {}

    public static synchronized TcpClient getInstance() {
        if (instance == null) instance = new TcpClient();
        return instance;
    }

    // ── Conexão ───────────────────────────────────────────────────────────────

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        connected = true;
        LOG.info("Conectado ao servidor " + host + ":" + port);
        startReaderThread();
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
        LOG.info("Desconectado do servidor.");
    }

    // ── Push listener ─────────────────────────────────────────────────────────

    public void setPushListener(PushListener listener) {
        this.pushListener = listener;
    }

    public void removePushListener() {
        this.pushListener = null;
    }

    // ── Envio e recebimento síncrono ──────────────────────────────────────────

    /**
     * Envia uma requisição e aguarda a resposta do servidor.
     * Deve ser chamado fora da thread JavaFX (dentro de um Task).
     *
     * @param action  action do protocolo (ex: "USUARIO_LIST")
     * @param payload mapa com os campos do payload (pode ser null)
     * @return resposta do servidor, ou resposta de erro em caso de timeout
     */
    public synchronized ServerResponse send(String action, Map<String, Object> payload) {
        if (!isConnected()) {
            return errorResponse(action, "Não conectado ao servidor.");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("action", action);

        String token = SessionManager.getInstance().getToken();
        if (token != null) request.put("token", token);

        request.put("payload", payload != null ? payload : Map.of());

        // Limpa fila antes de enviar para não pegar resposta de outra requisição
        responseQueue.clear();

        writer.println(gson.toJson(request));
        writer.flush();

        try {
            ServerResponse response = responseQueue.poll(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (response == null) {
                return errorResponse(action, "Timeout: servidor não respondeu em " + TIMEOUT_SEGUNDOS + "s.");
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResponse(action, "Requisição interrompida.");
        }
    }

    /** Atalho para envio sem payload. */
    public synchronized ServerResponse send(String action) {
        return send(action, null);
    }

    // ── Thread de leitura ─────────────────────────────────────────────────────

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {

                String line;
                while (connected && (line = reader.readLine()) != null) {
                    processLine(line);
                }
            } catch (IOException e) {
                if (connected) {
                    LOG.warning("Conexão com servidor perdida: " + e.getMessage());
                    connected = false;
                }
            }
        }, "tcp-push-reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Processa cada linha JSON recebida do servidor.
     * Pushes → PushListener.
     * Respostas normais → responseQueue.
     */
    private void processLine(String line) {
        ServerResponse response;
        try {
            response = gson.fromJson(line, ServerResponse.class);
        } catch (Exception e) {
            LOG.warning("JSON inválido recebido: " + line);
            return;
        }

        String action = response.getAction();
        if (action == null) return;

        switch (action) {
            case "PUSH_MENSAGEM"    -> handlePushMensagem(response);
            case "PUSH_NAOLIDADAS" -> handlePushNaoLidas(response);
            case "PUSH_MENSAGEM_LIDA" -> handlePushMensagemLida(response);
            default                 -> {
                // Resposta síncrona normal — deposita na fila
                try {
                    responseQueue.put(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── Handlers de push ──────────────────────────────────────────────────────

    private void handlePushMensagem(ServerResponse response) {
        PushListener listener = pushListener;
        if (listener == null || response.getData() == null) return;
        try {
            Dto.PushMensagem push = gson.fromJson(response.getData(), Dto.PushMensagem.class);
            listener.onNovaMensagem(push);
        } catch (Exception e) {
            LOG.warning("Erro ao processar PUSH_MENSAGEM: " + e.getMessage());
        }
    }

    private void handlePushNaoLidas(ServerResponse response) {
        PushListener listener = pushListener;
        if (listener == null || response.getData() == null) return;
        try {
            Dto.PushNaoLidas push = gson.fromJson(response.getData(), Dto.PushNaoLidas.class);
            listener.onNaoLidas(push);
        } catch (Exception e) {
            LOG.warning("Erro ao processar PUSH_NAOLIDADAS: " + e.getMessage());
        }
    }

    private void handlePushMensagemLida(ServerResponse response) {
        PushListener listener = pushListener;
        if (listener == null || response.getData() == null) return;
        try {
            Dto.PushMensagemLida push = gson.fromJson(response.getData(), Dto.PushMensagemLida.class);
            listener.onMensagemLida(push);
        } catch (Exception e) {
            LOG.warning("Erro ao processar PUSH_MENSAGEM_LIDA: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerResponse errorResponse(String action, String message) {
        // Cria uma ServerResponse de erro sem ir ao servidor
        String json = gson.toJson(Map.of(
            "status",  "ERROR",
            "action",  action,
            "message", message
        ));
        return gson.fromJson(json, ServerResponse.class);
    }

    public Gson getGson() { return gson; }
}

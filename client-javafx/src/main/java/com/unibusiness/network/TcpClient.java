package com.unibusiness.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unibusiness.dto.Dto;
import com.unibusiness.dto.ServerResponse;
import com.unibusiness.session.SessionManager;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cliente TCP singleton para o UniBusiness Server.
 *
 * CORREÇÃO do bug de presença online/offline:
 *
 * O problema era uma race condition:
 *   1. Client envia LOGIN
 *   2. Server responde com o token
 *   3. Server lança virtual thread que envia PUSH_STATUS_USUARIO (snapshot de quem está online)
 *   4. Client recebe o response e navega para o Dashboard
 *   5. DashboardController registra o PushRouter como listener
 *
 * Se o passo 3 chegasse ANTES do passo 5 (muito comum, pois JavaFX leva
 * alguns milissegundos para renderizar o Dashboard), os pushes de presença
 * eram descartados silenciosamente (pushListener == null).
 *
 * SOLUÇÃO: Buffer de pushes pendentes.
 * Pushes recebidos antes de um listener estar registrado são acumulados em
 * pendingPushes. Quando setPushListener() é chamado, todos os pushes
 * pendentes são entregues imediatamente ao novo listener.
 */
public class TcpClient {

    private static final Logger LOG = Logger.getLogger(TcpClient.class.getName());
    private static final int TIMEOUT_SEGUNDOS = 10;

    private static TcpClient instance;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private Socket       socket;
    private PrintWriter  writer;
    private Thread       readerThread;

    private final BlockingQueue<ServerResponse> responseQueue = new ArrayBlockingQueue<>(1);
    private volatile PushListener pushListener;
    private volatile boolean      connected = false;

    /**
     * Buffer de pushes recebidos antes do listener estar registrado.
     * Protegido por lock no próprio objeto.
     */
    private final List<ServerResponse> pendingPushes = new ArrayList<>();

    private TcpClient() {}

    public static synchronized TcpClient getInstance() {
        if (instance == null) instance = new TcpClient();
        return instance;
    }

    // ── Conexão ───────────────────────────────────────────────────────────────

    public void connect(String host, int port) throws IOException {
        socket  = new Socket(host, port);
        writer  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        connected = true;
        LOG.info("Conectado ao servidor " + host + ":" + port);
        startReaderThread();
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
        synchronized (pendingPushes) {
            pendingPushes.clear();
        }
    }

    // ── Push listener ─────────────────────────────────────────────────────────

    /**
     * Registra o listener de pushes.
     *
     * CORREÇÃO: entrega imediatamente quaisquer pushes que chegaram antes
     * do listener ser registrado (race condition no login).
     */
    public void setPushListener(PushListener listener) {
        this.pushListener = listener;

        if (listener == null) return;

        // Drena os pushes pendentes para o novo listener
        List<ServerResponse> pending;
        synchronized (pendingPushes) {
            if (pendingPushes.isEmpty()) return;
            pending = new ArrayList<>(pendingPushes);
            pendingPushes.clear();
        }

        LOG.info("Entregando " + pending.size() + " pushes pendentes ao listener.");
        for (ServerResponse push : pending) {
            dispatchPush(push);
        }
    }

    public void removePushListener() { this.pushListener = null; }

    // ── Envio e recebimento síncrono ──────────────────────────────────────────

    public synchronized ServerResponse send(String action, Map<String, Object> payload) {
        if (!isConnected()) return errorResponse(action, "Não conectado ao servidor.");

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("action", action);

        String token = SessionManager.getInstance().getToken();
        if (token != null) request.put("token", token);
        request.put("payload", payload != null ? payload : Map.of());

        responseQueue.clear();
        writer.println(gson.toJson(request));
        writer.flush();

        try {
            ServerResponse response = responseQueue.poll(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (response == null)
                return errorResponse(action, "Timeout: servidor não respondeu em " + TIMEOUT_SEGUNDOS + "s.");
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResponse(action, "Requisição interrompida.");
        }
    }

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
            case "PUSH_MENSAGEM",
                 "PUSH_NAOLIDADAS",
                 "PUSH_MENSAGEM_LIDA",
                 "PUSH_STATUS_USUARIO",
                 "PUSH_DIGITANDO" -> handlePush(response);
            default -> {
                try {
                    responseQueue.put(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Trata um push recebido.
     *
     * CORREÇÃO: se o listener ainda não está registrado, armazena o push
     * no buffer pendingPushes em vez de descartá-lo.
     */
    private void handlePush(ServerResponse response) {
        PushListener l = pushListener;
        if (l == null) {
            // Listener ainda não registrado — buffer o push para entrega posterior
            synchronized (pendingPushes) {
                pendingPushes.add(response);
                LOG.fine("Push " + response.getAction() + " bufferizado (listener não registrado ainda).");
            }
            return;
        }
        dispatchPush(response);
    }

    /**
     * Despacha um push para o listener registrado.
     */
    private void dispatchPush(ServerResponse response) {
        PushListener l = pushListener;
        if (l == null || response.getData() == null) return;

        String action = response.getAction();
        try {
            switch (action) {
                case "PUSH_MENSAGEM" ->
                    l.onNovaMensagem(gson.fromJson(response.getData(), Dto.PushMensagem.class));
                case "PUSH_NAOLIDADAS" ->
                    l.onNaoLidas(gson.fromJson(response.getData(), Dto.PushNaoLidas.class));
                case "PUSH_MENSAGEM_LIDA" ->
                    l.onMensagemLida(gson.fromJson(response.getData(), Dto.PushMensagemLida.class));
                case "PUSH_STATUS_USUARIO" ->
                    l.onStatusUsuario(gson.fromJson(response.getData(), Dto.PushStatusUsuario.class));
                case "PUSH_DIGITANDO" ->
                    l.onDigitando(gson.fromJson(response.getData(), Dto.PushDigitando.class));
            }
        } catch (Exception e) {
            LOG.warning("Erro ao processar " + action + ": " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerResponse errorResponse(String action, String message) {
        String json = gson.toJson(Map.of(
            "status",  "ERROR",
            "action",  action,
            "message", message
        ));
        return gson.fromJson(json, ServerResponse.class);
    }

    public Gson getGson() { return gson; }
}
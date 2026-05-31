package com.unibusiness.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Cliente TCP para o UniBusiness Server.
 *
 * FIX (race condition no reader): a thread de push e o método sendAndReceive()
 * competiam pelo mesmo BufferedReader sem sincronização adequada.
 *
 * Solução: uma única thread de leitura (readerThread) consome TODAS as linhas
 * do socket. Respostas síncronas (request/response) são enfileiradas em
 * responseQueue. Pushes assíncronos (action começa com "PUSH_") são tratados
 * diretamente no listener. Assim, nunca há duas threads lendo o mesmo stream.
 */
public class TcpClient {

    private static final String HOST           = "localhost";
    private static final int    PORT           = 7777;
    private static final long   TIMEOUT_MS     = 10_000;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private Socket        socket;
    private BufferedReader reader;
    private PrintWriter   writer;
    private String        token;

    // FIX: fila que desacopla leitura síncrona de pushes assíncronos
    private final BlockingQueue<Map<String, Object>> responseQueue = new LinkedBlockingQueue<>();

    // ── Conexão ───────────────────────────────────────────────────────────────

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        System.out.println("[cliente] Conectado a " + HOST + ":" + PORT);

        // FIX: UMA única thread lê o socket; despacha para fila ou handler de push
        Thread readerThread = new Thread(this::readLoop, "socket-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void disconnect() throws IOException {
        if (token != null) send(Map.of("action", "LOGOUT", "token", token));
        socket.close();
    }

    // ── Autenticação ──────────────────────────────────────────────────────────

    public boolean login(String email, String senha) throws IOException, InterruptedException {
        Map<String, Object> req = Map.of(
            "action",  "LOGIN",
            "payload", Map.of("email", email, "senha", senha)
        );
        Map<String, Object> resp = sendAndReceive(req);

        if ("OK".equals(resp.get("status"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            token = (String) data.get("token");
            System.out.println("[cliente] Login OK. Token: " + token);
            return true;
        } else {
            System.out.println("[cliente] Login FALHOU: " + resp.get("message"));
            return false;
        }
    }

    // ── Requisições autenticadas ──────────────────────────────────────────────

    public void listarUsuarios() throws IOException, InterruptedException {
        Map<String, Object> req = Map.of(
            "action",  "USUARIO_LIST",
            "token",   token,
            "payload", Map.of()
        );
        Map<String, Object> resp = sendAndReceive(req);
        System.out.println("[cliente] Usuários: " + gson.toJson(resp.get("data")));
    }

    public void enviarMensagem(int conversaId, String conteudo) throws IOException, InterruptedException {
        Map<String, Object> req = Map.of(
            "action",  "MENSAGEM_SEND",
            "token",   token,
            "payload", Map.of("conversaId", conversaId, "conteudo", conteudo)
        );
        Map<String, Object> resp = sendAndReceive(req);
        System.out.println("[cliente] Mensagem enviada: " + resp.get("message"));
    }

    // ── I/O ───────────────────────────────────────────────────────────────────

    private synchronized void send(Map<String, Object> request) {
        writer.println(gson.toJson(request));
        writer.flush();
    }

    /**
     * FIX: envia a requisição e aguarda a próxima RESPOSTA na fila,
     * sem competir com a thread de leitura.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request)
            throws IOException, InterruptedException {
        send(request);
        Map<String, Object> resp = responseQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (resp == null) throw new IOException("Timeout aguardando resposta do servidor.");
        return resp;
    }

    /**
     * FIX: loop único que lê o socket. Pushes (action = "PUSH_*") vão para
     * handlePush(); todo o resto vai para responseQueue e é consumido por
     * sendAndReceive().
     */
    @SuppressWarnings("unchecked")
    private void readLoop() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> msg = gson.fromJson(line, Map.class);
                String action = (String) msg.get("action");

                if (action != null && action.startsWith("PUSH_")) {
                    handlePush(action, msg);
                } else {
                    responseQueue.put(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("[cliente] Conexão com servidor encerrada.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePush(String action, Map<String, Object> msg) {
        switch (action) {
            case "PUSH_MENSAGEM" -> {
                Map<String, Object> data = (Map<String, Object>) msg.get("data");
                System.out.printf("[PUSH] Nova mensagem de %s na conversa %s: %s%n",
                    data.get("remetente"),
                    data.get("conversaId"),
                    data.get("conteudo"));
            }
            case "PUSH_NAOLIDADAS" -> {
                Map<String, Object> data = (Map<String, Object>) msg.get("data");
                System.out.printf("[PUSH] Você tem %s mensagens não lidas.%n", data.get("total"));
            }
            case "PUSH_MENSAGEM_LIDA" -> {
                Map<String, Object> data = (Map<String, Object>) msg.get("data");
                System.out.printf("[PUSH] %s leu as mensagens da conversa %s.%n",
                    data.get("usuario"), data.get("conversaId"));
            }
            default -> System.out.println("[PUSH] " + action + ": " + msg.get("message"));
        }
    }

    // ── Main de demonstração ──────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        TcpClient client = new TcpClient();
        client.connect();

        if (client.login("teste", "1234")) {
            client.listarUsuarios();
            client.enviarMensagem(1, "Olá, equipe!");

            Thread.sleep(10_000);
        }

        client.disconnect();
    }
}
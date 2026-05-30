package com.unibusiness.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Exemplo de cliente TCP para o UniBusiness Server.
 *
 * Demonstra:
 *  1. Conexão
 *  2. LOGIN
 *  3. Envio de requisição CRUD (ex: USUARIO_LIST)
 *  4. Envio de mensagem (MENSAGEM_SEND)
 *  5. Recebimento de push assíncrono (PUSH_MENSAGEM) em thread separada
 *
 * Use este arquivo como referência para implementar o seu client real.
 * Adapte para Swing, JavaFX, console, etc.
 */
public class TcpClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 7777;

    private final Gson       gson = new GsonBuilder().serializeNulls().create();
    private Socket           socket;
    private BufferedReader   reader;
    private PrintWriter      writer;
    private String           token;

    // ── Conexão ───────────────────────────────────────────────────────────────

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        System.out.println("[cliente] Conectado a " + HOST + ":" + PORT);

        // Thread que fica ouvindo pushes assíncronos do servidor
        Thread listenerThread = new Thread(this::listenForPushes, "push-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnect() throws IOException {
        if (token != null) send(Map.of("action", "LOGOUT", "token", token));
        socket.close();
    }

    // ── Autenticação ──────────────────────────────────────────────────────────

    public boolean login(String email, String senha) throws IOException {
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

    /**
     * Lista todos os usuários.
     */
    public void listarUsuarios() throws IOException {
        Map<String, Object> req = Map.of(
            "action",  "USUARIO_LIST",
            "token",   token,
            "payload", Map.of()
        );
        Map<String, Object> resp = sendAndReceive(req);
        System.out.println("[cliente] Usuários: " + gson.toJson(resp.get("data")));
    }

    /**
     * Envia uma mensagem para uma conversa.
     * O servidor fará push para todos os participantes online.
     */
    public void enviarMensagem(int conversaId, String conteudo) throws IOException {
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
     * Envia e aguarda a próxima linha do servidor.
     * ATENÇÃO: isso funciona em fluxo síncrono request/response.
     * Pushes assíncronos são tratados em listenForPushes().
     *
     * Em um cliente real, use uma fila para correlacionar respostas.
     */
    @SuppressWarnings("unchecked")
    private synchronized Map<String, Object> sendAndReceive(Map<String, Object> request) throws IOException {
        send(request);
        String line = reader.readLine();
        return gson.fromJson(line, Map.class);
    }

    /**
     * Loop em thread separada que processa pushes assíncronos (ex: PUSH_MENSAGEM).
     * Em um cliente real, dispare eventos de UI aqui.
     */
    @SuppressWarnings("unchecked")
    private void listenForPushes() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> msg = gson.fromJson(line, Map.class);
                String action = (String) msg.get("action");

                if ("PUSH_MENSAGEM".equals(action)) {
                    Map<String, Object> data = (Map<String, Object>) msg.get("data");
                    System.out.printf("[PUSH] Nova mensagem de %s na conversa %s: %s%n",
                        data.get("remetente"),
                        data.get("conversaId"),
                        data.get("conteudo"));
                } else {
                    // Outras respostas não-solicitadas (ex: notificações futuras)
                    System.out.println("[PUSH] " + action + ": " + msg.get("message"));
                }
            }
        } catch (IOException e) {
            System.out.println("[cliente] Conexão com servidor encerrada.");
        }
    }

    // ── Main de demonstração ──────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        TcpClient client = new TcpClient();
        client.connect();

        if (client.login("admin@empresa.com", "senha123")) {
            client.listarUsuarios();
            client.enviarMensagem(1, "Olá, equipe!");

            // Mantém o cliente aberto para receber pushes
            Thread.sleep(10_000);
        }

        client.disconnect();
    }
}

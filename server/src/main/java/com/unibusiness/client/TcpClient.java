package com.unibusiness.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TcpClient {

    private static final String HOST       = "localhost";
    private static final int    PORT       = 7777;
    private static final long   TIMEOUT_MS = 10_000;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private Socket        socket;
    private BufferedReader reader;
    private PrintWriter   writer;
    private String        token;

    private final BlockingQueue<Map<String, Object>> responseQueue = new LinkedBlockingQueue<>();

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        System.out.println("[cliente] Conectado a " + HOST + ":" + PORT);

        Thread readerThread = new Thread(this::readLoop, "socket-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void disconnect() throws IOException {
        if (token != null) send(Map.of("action", "LOGOUT", "token", token));
        socket.close();
    }

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

    public void listarConversas() throws IOException, InterruptedException {
        Map<String, Object> resp = sendAndReceive(Map.of(
            "action", "CONVERSA_LIST", "token", token, "payload", Map.of()
        ));
        System.out.println("[cliente] Conversas: " + gson.toJson(resp.get("data")));
    }

    public void enviarMensagem(int conversaId, String conteudo) throws IOException, InterruptedException {
        Map<String, Object> resp = sendAndReceive(Map.of(
            "action",  "MENSAGEM_SEND",
            "token",   token,
            "payload", Map.of("conversaId", conversaId, "conteudo", conteudo)
        ));
        System.out.println("[cliente] Mensagem enviada: " + resp.get("message"));
    }

    public void digitando(int conversaId) {
        send(Map.of(
            "action",  "USUARIO_DIGITANDO",
            "token",   token,
            "payload", Map.of("conversaId", conversaId, "digitando", true)
        ));
    }

    public void pararDigitando(int conversaId) {
        send(Map.of(
            "action",  "USUARIO_DIGITANDO",
            "token",   token,
            "payload", Map.of("conversaId", conversaId, "digitando", false)
        ));
    }

    private synchronized void send(Map<String, Object> request) {
        writer.println(gson.toJson(request));
        writer.flush();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request)
            throws IOException, InterruptedException {
        send(request);
        Map<String, Object> resp = responseQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (resp == null) throw new IOException("Timeout aguardando resposta do servidor.");
        return resp;
    }

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
        Map<String, Object> data = (Map<String, Object>) msg.get("data");
        switch (action) {
            case "PUSH_MENSAGEM" -> System.out.printf(
                "[PUSH] Nova mensagem de %s na conversa %s: %s%n",
                data.get("remetente"), data.get("conversaId"), data.get("conteudo"));
            case "PUSH_NAOLIDADAS" -> System.out.printf(
                "[PUSH] Você tem %s mensagens não lidas.%n", data.get("total"));
            case "PUSH_MENSAGEM_LIDA" -> System.out.printf(
                "[PUSH] %s leu as mensagens da conversa %s.%n",
                data.get("usuario"), data.get("conversaId"));
            case "PUSH_STATUS_USUARIO" -> {
                boolean online = Boolean.TRUE.equals(data.get("online"));
                System.out.printf("[PUSH] %s está %s.%n",
                    data.get("nome"), online ? "online" : "offline");
            }
            case "PUSH_DIGITANDO" -> {
                boolean digitando = Boolean.TRUE.equals(data.get("digitando"));
                if (digitando) {
                    System.out.printf("[PUSH] %s está digitando na conversa %s…%n",
                        data.get("nome"), data.get("conversaId"));
                } else {
                    System.out.printf("[PUSH] %s parou de digitar na conversa %s.%n",
                        data.get("nome"), data.get("conversaId"));
                }
            }
            default -> System.out.println("[PUSH] " + action + ": " + msg.get("message"));
        }
    }

    public static void main(String[] args) throws Exception {
        TcpClient client = new TcpClient();
        client.connect();

        if (client.login("a@a", "asasas")) {
            client.listarConversas();
            client.digitando(1);
            Thread.sleep(1500);
            client.enviarMensagem(1, "Olá, equipe!");
            client.pararDigitando(1);
            Thread.sleep(10_000);
        }

        client.disconnect();
    }
}

package com.unibusiness.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpServer {

    private static final Logger LOG = Logger.getLogger(TcpServer.class.getName());

    private final int               port;
    private final ExecutorService   pool;
    private final RequestDispatcher dispatcher;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public TcpServer(int port, int maxClients) {
        this.port       = port;
        this.pool       = Executors.newFixedThreadPool(maxClients);
        this.dispatcher = new RequestDispatcher();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        LOG.info("═══════════════════════════════════════════");
        LOG.info("  UniBusiness TCP Server rodando na porta " + port);
        LOG.info("═══════════════════════════════════════════");

        while (running) {
            try {
                Socket client = serverSocket.accept();
                client.setKeepAlive(true);
                // CORREÇÃO: removido setSoTimeout(0) que anulava o timeout
                // definido pelo ClientHandler. O ClientHandler define
                // setSoTimeout(15_000) para detectar desconexões abruptas.
                pool.submit(new ClientHandler(client, dispatcher));
            } catch (IOException e) {
                if (running) {
                    LOG.log(Level.WARNING, "Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            LOG.warning("Erro ao fechar ServerSocket: " + e.getMessage());
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Servidor encerrado.");
    }
}
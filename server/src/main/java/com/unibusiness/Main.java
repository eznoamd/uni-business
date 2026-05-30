package com.unibusiness;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.network.TcpServer;

import java.io.IOException;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        int port       = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "7777"));
        int maxClients = Integer.parseInt(System.getenv().getOrDefault("MAX_CLIENTS", "100"));

        PersistenceManager.getEntityManagerFactory();

        TcpServer server = new TcpServer(port, maxClients);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Encerrando servidor...");
            server.shutdown();
            PersistenceManager.close();
        }));

        try {
            server.start();
        } catch (IOException e) {
            LOG.severe("Falha ao iniciar servidor: " + e.getMessage());
            System.exit(1);
        }
    }
}

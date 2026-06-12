package com.unibusiness.network.session;

import com.google.gson.Gson;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.protocol.response.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {

    private final Socket        socket;
    private final PrintWriter   out;
    private final UsuarioEntity usuario;
    private final Gson          gson;
    private String              token;

    public ClientSession(Socket socket, PrintWriter out, UsuarioEntity usuario, Gson gson) {
        this.socket  = socket;
        this.out     = out;
        this.usuario = usuario;
        this.gson    = gson;
    }

    public synchronized void send(Response response) {
        if (socket.isClosed()) return;
        out.println(gson.toJson(response));
        out.flush();
    }

    public boolean isConnected() {
        return !socket.isClosed() && socket.isConnected();
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public Socket        getSocket()            { return socket; }
    public PrintWriter   getOut()               { return out; }
    public UsuarioEntity getUsuario()           { return usuario; }
    public String        getToken()             { return token; }
    public void          setToken(String token) { this.token = token; }
}

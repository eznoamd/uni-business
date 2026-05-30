package com.unibusiness.network.session;

import com.unibusiness.model.UsuarioEntity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionStore {

    private static final SessionStore INSTANCE = new SessionStore();

    private final Map<String, ClientSession> byToken   = new ConcurrentHashMap<>();
    private final Map<Integer, ClientSession> byUsuario = new ConcurrentHashMap<>();

    private SessionStore() {}

    public static SessionStore getInstance() { return INSTANCE; }

    public String register(ClientSession session) {
        String token = UUID.randomUUID().toString();
        session.setToken(token);
        byToken.put(token, session);
        byUsuario.put(session.getUsuario().getId(), session);
        return token;
    }

    public void remove(String token) {
        ClientSession session = byToken.remove(token);
        if (session != null) {
            byUsuario.remove(session.getUsuario().getId());
        }
    }

    public ClientSession get(String token) {
        return byToken.get(token);
    }

    public ClientSession getByUsuarioId(Integer id) {
        return byUsuario.get(id);
    }

    public boolean isValid(String token) {
        return token != null && byToken.containsKey(token);
    }

    public Collection<ClientSession> allSessions() {
        return byToken.values();
    }

    public UsuarioEntity getUsuario(String token) {
        ClientSession s = byToken.get(token);
        return s != null ? s.getUsuario() : null;
    }
}

package com.unibusiness.network.session;

import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.protocol.response.Response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class SessionStore {

    private static final Logger LOG = Logger.getLogger(SessionStore.class.getName());

    private static final long TOKEN_TTL_HOURS = 8;

    private static final SessionStore INSTANCE = new SessionStore();

    private final Map<String,  SessionEntry>  byToken   = new ConcurrentHashMap<>();
    private final Map<Integer, ClientSession> byUsuario = new ConcurrentHashMap<>();

    private SessionStore() {}

    public static SessionStore getInstance() { return INSTANCE; }

    // ── Registro / remoção ────────────────────────────────────────────────────

    public synchronized String register(ClientSession session) {
        Integer uid = session.getUsuario().getId();
        // Remove sessão anterior do mesmo usuário (re-login)
        ClientSession old = byUsuario.get(uid);
        if (old != null && old.getToken() != null) {
            byToken.remove(old.getToken());
        }

        String token = UUID.randomUUID().toString();
        session.setToken(token);
        byToken.put(token, new SessionEntry(session, Instant.now()));
        byUsuario.put(uid, session);
        return token;
    }

    public synchronized void remove(String token) {
        if (token == null) return;
        SessionEntry entry = byToken.remove(token);
        if (entry != null && entry.session().getUsuario() != null) {
            // Remove do byUsuario apenas se o token ainda é o ativo para aquele usuário
            Integer uid = entry.session().getUsuario().getId();
            ClientSession current = byUsuario.get(uid);
            if (current != null && token.equals(current.getToken())) {
                byUsuario.remove(uid);
            }
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public ClientSession get(String token) {
        if (token == null) return null;
        SessionEntry entry = byToken.get(token);
        if (entry == null) return null;
        if (isExpired(entry)) {
            LOG.info("Token expirado removido: " + token);
            remove(token);
            return null;
        }
        return entry.session();
    }

    public ClientSession getByUsuarioId(Integer id) {
        return byUsuario.get(id);
    }

    public boolean isValid(String token) {
        return token != null && get(token) != null;
    }

    public Collection<ClientSession> allSessions() {
        return byUsuario.values();
    }

    public UsuarioEntity getUsuario(String token) {
        ClientSession s = get(token);
        return s != null ? s.getUsuario() : null;
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    public void broadcastExcept(Response response, Integer excludeUsuarioId) {
        allSessions().forEach(s -> {
            if (!s.getUsuario().getId().equals(excludeUsuarioId) && s.isConnected()) {
                s.send(response);
            }
        });
    }

    public void broadcastAll(Response response) {
        allSessions().forEach(s -> {
            if (s.isConnected()) s.send(response);
        });
    }

    // ── Interno ───────────────────────────────────────────────────────────────

    private boolean isExpired(SessionEntry entry) {
        return entry.createdAt()
            .plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS)
            .isBefore(Instant.now());
    }

    private record SessionEntry(ClientSession session, Instant createdAt) {}
}

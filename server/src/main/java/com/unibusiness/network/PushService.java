package com.unibusiness.network;

import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.network.session.SessionStore;
import com.unibusiness.protocol.response.Response;

import java.util.Collection;
import java.util.logging.Logger;

public final class PushService {

    private static final Logger LOG = Logger.getLogger(PushService.class.getName());

    private static final PushService INSTANCE = new PushService();

    private final SessionStore sessions = SessionStore.getInstance();

    private PushService() {}

    public static PushService getInstance() { return INSTANCE; }

    public void broadcastToConversa(ConversaEntity conversa, Response push, Integer excludeUsuarioId) {
        for (UsuarioEntity participante : conversa.getParticipantes()) {
            if (participante.getId().equals(excludeUsuarioId)) continue;
            sendToUser(participante.getId(), push);
        }
    }

    public void sendToUser(Integer usuarioId, Response push) {
        ClientSession dest = sessions.getByUsuarioId(usuarioId);
        if (dest != null && dest.isConnected()) {
            try {
                dest.send(push);
            } catch (Exception e) {
                LOG.warning("Falha ao enviar push para usuário " + usuarioId + ": " + e.getMessage());
            }
        }
    }

    public void broadcastExcept(Response push, Integer excludeUsuarioId) {
        sessions.allSessions().forEach(s -> {
            if (!s.getUsuario().getId().equals(excludeUsuarioId) && s.isConnected()) {
                try {
                    s.send(push);
                } catch (Exception e) {
                    LOG.warning("Falha no broadcast: " + e.getMessage());
                }
            }
        });
    }

    public void broadcastAll(Response push) {
        sessions.allSessions().forEach(s -> {
            if (s.isConnected()) {
                try {
                    s.send(push);
                } catch (Exception e) {
                    LOG.warning("Falha no broadcastAll: " + e.getMessage());
                }
            }
        });
    }

    public void send(Collection<ClientSession> targets, Response push) {
        targets.forEach(s -> {
            if (s.isConnected()) {
                try {
                    s.send(push);
                } catch (Exception e) {
                    LOG.warning("Falha ao enviar push: " + e.getMessage());
                }
            }
        });
    }
}

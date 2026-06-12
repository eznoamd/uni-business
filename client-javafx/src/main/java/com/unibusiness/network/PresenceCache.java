package com.unibusiness.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Cache global de presença (online/offline).
 *
 * CORREÇÃO: adicionado setIfAbsent() para popular o cache com o fallback
 * do servidor sem sobrescrever valores já atualizados por pushes recentes.
 *
 * O fluxo de atualização agora é:
 *   1. Login -> TcpClient buffer pushes pendentes
 *   2. DashboardController registra no PushRouter
 *   3. TcpClient drena pushes pendentes -> PresenceCache.set() (mais recente, sempre sobrescreve)
 *   4. ChatController carrega CONVERSA_LIST -> PresenceCache.setIfAbsent() (só popula se vazio)
 *   5. Novos pushes -> PresenceCache.set() (sempre sobrescreve)
 */
public final class PresenceCache {

    private static final PresenceCache INSTANCE = new PresenceCache();

    /** usuarioId → true=online, false=offline */
    private final Map<Integer, Boolean> cache = new ConcurrentHashMap<>();

    private PresenceCache() {}

    public static PresenceCache getInstance() { return INSTANCE; }

    /**
     * Atualiza o status de um usuário.
     * Chamado pelo DashboardController em cada PUSH_STATUS_USUARIO.
     * Sempre sobrescreve — pushes são a fonte de verdade mais recente.
     */
    public void set(Integer usuarioId, boolean online) {
        cache.put(usuarioId, online);
    }

    /**
     * Popula o cache somente se não há entrada para o usuário.
     * Chamado pelo ChatController ao carregar CONVERSA_LIST, como fallback
     * para o caso de pushes do login terem sido perdidos.
     * Não sobrescreve valores já definidos por pushes.
     */
    public void setIfAbsent(Integer usuarioId, boolean online) {
        cache.putIfAbsent(usuarioId, online);
    }

    /**
     * Consulta o status de um usuário.
     * Se o usuário nunca foi visto via push nem via setIfAbsent,
     * retorna o valor padrão (fallback do servidor no momento da consulta).
     */
    public boolean isOnline(Integer usuarioId, boolean serverFallback) {
        Boolean cached = cache.get(usuarioId);
        return cached != null ? cached : serverFallback;
    }

    /** Limpa o cache (chamado no logout). */
    public void clear() {
        cache.clear();
    }
}
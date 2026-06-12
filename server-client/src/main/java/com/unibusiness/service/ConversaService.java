package com.unibusiness.service;

import com.unibusiness.core.model.ConversaEntity;
import com.unibusiness.core.model.MensagemEntity;
import com.unibusiness.core.model.UsuarioEntity;
import com.unibusiness.core.service.impl.ConversaServiceImpl;
import com.unibusiness.dto.Dto;
import com.unibusiness.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço de conversas/mensagens — antes via TCP (CONVERSA_*, MENSAGEM_*),
 * agora via ConversaService (JPA) direto.
 *
 * "online" do outro participante (conversas PRIVADA) é lido direto do campo
 * UsuarioEntity.online — populado pelo AuthService no login/logout e
 * atualizado via polling pelo ChatController.
 */
public class ConversaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final com.unibusiness.core.service.ConversaService conversaService = new ConversaServiceImpl();

    public List<Dto.Conversa> listarConversas() {
        Integer usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        if (usuarioLogadoId == null) return List.of();

        Map<Integer, Long> naoLidas = conversaService.contarNaoLidasPorConversa(usuarioLogadoId);

        return conversaService.listarPorUsuario(usuarioLogadoId).stream()
            .map(c -> toDto(c, usuarioLogadoId, naoLidas))
            .collect(Collectors.toList());
    }

    public List<Dto.Mensagem> listarMensagens(Integer conversaId) {
        return conversaService.listarMensagens(conversaId).stream()
            .map(ConversaService::toDto)
            .collect(Collectors.toList());
    }

    public Integer enviarMensagem(Integer conversaId, String conteudo) {
        Integer usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        try {
            MensagemEntity msg = conversaService.enviarMensagem(conversaId, usuarioLogadoId, conteudo);
            return msg.getId();
        } catch (Exception e) {
            return null;
        }
    }

    public void marcarComoLida(Integer conversaId) {
        Integer usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        if (usuarioLogadoId == null) return;
        conversaService.marcarConversaComoLida(usuarioLogadoId, conversaId);
    }

    public Map<String, Long> contarNaoLidas() {
        Integer usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        if (usuarioLogadoId == null) return Map.of();
        Map<Integer, Long> contagem = conversaService.contarNaoLidasPorConversa(usuarioLogadoId);
        Map<String, Long> result = new HashMap<>();
        contagem.forEach((convId, qtd) -> result.put(String.valueOf(convId), qtd));
        return result;
    }

    /**
     * Cria conversa.
     * tipo: "PRIVADA" para 1-a-1, "GRUPO" para grupos.
     * nome: obrigatório apenas para GRUPO.
     */
    public Integer criarConversa(String tipo, List<Integer> participanteIds, String nome) {
        Integer usuarioLogadoId = SessionManager.getInstance().getUsuarioId();
        if (usuarioLogadoId == null) return null;

        Set<Integer> participantes = new HashSet<>(participanteIds);
        ConversaEntity.Tipo tipoEnum = "GRUPO".equals(tipo) ? ConversaEntity.Tipo.GRUPO : ConversaEntity.Tipo.PRIVADA;

        try {
            ConversaEntity conversa = conversaService.criar(tipoEnum, nome, usuarioLogadoId, participantes);
            return conversa.getId();
        } catch (Exception e) {
            return null;
        }
    }

    /** Compat com código antigo sem nome */
    public Integer criarConversa(String tipo, List<Integer> participanteIds) {
        return criarConversa(tipo, participanteIds, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Dto.Conversa toDto(ConversaEntity c, Integer usuarioLogadoId, Map<Integer, Long> naoLidas) {
        Dto.Conversa dto = new Dto.Conversa();
        dto.id = c.getId();
        dto.tipo = c.getTipo().name();
        dto.naoLidas = naoLidas.getOrDefault(c.getId(), 0L);

        if (c.getTipo() == ConversaEntity.Tipo.PRIVADA) {
            UsuarioEntity outro = c.getParticipantes().stream()
                .filter(p -> !p.getId().equals(usuarioLogadoId))
                .findFirst()
                .orElse(null);

            if (outro != null) {
                dto.outroUsuarioId = outro.getId();
                dto.nome = outro.getNome();
                dto.online = Boolean.TRUE.equals(outro.getOnline());
            }
        } else {
            dto.nome = c.getNome();
        }

        return dto;
    }

    private static Dto.Mensagem toDto(MensagemEntity m) {
        Dto.Mensagem dto = new Dto.Mensagem();
        dto.id = m.getId();
        dto.conversaId = m.getConversa().getId();
        dto.remetenteId = m.getRemetente().getId();
        dto.remetente = m.getRemetente().getNome();
        dto.conteudo = m.getConteudo();
        dto.enviadoEm = m.getEnviadoEm() != null ? m.getEnviadoEm().format(ISO) : null;
        return dto;
    }
}

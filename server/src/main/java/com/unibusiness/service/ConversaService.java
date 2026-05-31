package com.unibusiness.service;

import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConversaService {

    ConversaEntity criar(String tipo, Integer criadorId, Set<Integer> participanteIds);

    List<ConversaEntity> listarPorUsuario(Integer usuarioId);

    /**
     * Persiste a mensagem e cria um MensagemStatusEntity (lida=false)
     * para cada participante da conversa, exceto o próprio remetente.
     */
    MensagemEntity enviarMensagem(Integer conversaId, Integer remetenteId, String conteudo);

    List<MensagemEntity> listarMensagens(Integer conversaId);

    /**
     * Marca todas as mensagens não lidas de uma conversa como lidas para o usuário.
     * Retorna quantas foram marcadas.
     */
    int marcarConversaComoLida(Integer usuarioId, Integer conversaId);

    /**
     * Retorna um mapa conversaId → quantidade de mensagens não lidas
     * para o usuário informado. Usado no PUSH_NAOLIDADAS após login.
     */
    Map<Integer, Long> contarNaoLidasPorConversa(Integer usuarioId);
}

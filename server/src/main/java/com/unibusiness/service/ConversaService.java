package com.unibusiness.service;
import com.unibusiness.model.ConversaEntity;
import com.unibusiness.model.MensagemEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
public interface ConversaService {
    ConversaEntity criar(ConversaEntity.Tipo tipo, String nome, Integer criadorId, Set<Integer> participanteIds);
    List<ConversaEntity> listarPorUsuario(Integer usuarioId);
    MensagemEntity enviarMensagem(Integer conversaId, Integer remetenteId, String conteudo);
    List<MensagemEntity> listarMensagens(Integer conversaId);
    int marcarConversaComoLida(Integer usuarioId, Integer conversaId);
    Map<Integer, Long> contarNaoLidasPorConversa(Integer usuarioId);
}
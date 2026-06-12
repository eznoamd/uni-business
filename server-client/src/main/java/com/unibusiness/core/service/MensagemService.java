package com.unibusiness.core.service;
import com.unibusiness.core.model.MensagemEntity;
import java.util.List;
public interface MensagemService {
    MensagemEntity create(MensagemEntity mensagem);
    List<MensagemEntity> findByConversaId(Integer conversaId);
}
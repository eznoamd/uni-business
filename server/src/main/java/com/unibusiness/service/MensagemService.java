package com.unibusiness.service;
import com.unibusiness.model.MensagemEntity;
import java.util.List;
public interface MensagemService {
    MensagemEntity create(MensagemEntity mensagem);
    List<MensagemEntity> findByConversaId(Integer conversaId);
}
package model.mapper;

import model.entity.MensagemEntity;
import model.dto.MensagemDTO;

public class MensagemMapper implements ModelMapper<MensagemEntity, MensagemDTO> {

    @Override
    public MensagemEntity toEntity(MensagemDTO dto) {
        if (dto == null) {
            return null;
        }

        MensagemEntity entity = new MensagemEntity();
        entity.setId(dto.getId());
        entity.setConteudo(dto.getConteudo());

        return entity;
    }

    @Override
    public MensagemDTO toDto(MensagemEntity entity) {
        if (entity == null) {
            return null;
        }

        MensagemDTO dto = new MensagemDTO();
        dto.setId(entity.getId());
        dto.setConteudo(entity.getConteudo());
        dto.setEnviadoEm(entity.getEnviadoEm());

        if (entity.getConversa() != null) {
            dto.setConversaId(entity.getConversa().getId());
        }

        if (entity.getRemetente() != null) {
            dto.setRemetenteId(entity.getRemetente().getId());
            dto.setRemetenteNome(entity.getRemetente().getNome());
        }

        return dto;
    }
}

package model.mapper;

import model.entity.ConversaEntity;
import model.dto.ConversaDTO;

public class ConversaMapper implements ModelMapper<ConversaEntity, ConversaDTO> {

    @Override
    public ConversaEntity toEntity(ConversaDTO dto) {
        if (dto == null) {
            return null;
        }

        ConversaEntity entity = new ConversaEntity();
        entity.setId(dto.getId());
        entity.setTipo(dto.getTipo());

        return entity;
    }

    @Override
    public ConversaDTO toDto(ConversaEntity entity) {
        if (entity == null) {
            return null;
        }

        ConversaDTO dto = new ConversaDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());

        return dto;
    }
}

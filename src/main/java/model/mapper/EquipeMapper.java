package model.mapper;

import model.entity.EquipeEntity;
import model.dto.EquipeDTO;

public class EquipeMapper implements ModelMapper<EquipeEntity, EquipeDTO> {

    @Override
    public EquipeEntity toEntity(EquipeDTO dto) {
        if (dto == null) {
            return null;
        }

        EquipeEntity entity = new EquipeEntity();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());

        return entity;
    }

    @Override
    public EquipeDTO toDto(EquipeEntity entity) {
        if (entity == null) {
            return null;
        }

        EquipeDTO dto = new EquipeDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());

        return dto;
    }
}

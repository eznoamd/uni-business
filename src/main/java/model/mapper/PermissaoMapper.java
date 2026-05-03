package model.mapper;

import model.entity.PermissaoEntity;
import model.dto.PermissaoDTO;

public class PermissaoMapper implements ModelMapper<PermissaoEntity, PermissaoDTO> {

    @Override
    public PermissaoEntity toEntity(PermissaoDTO dto) {
        if (dto == null) {
            return null;
        }

        PermissaoEntity entity = new PermissaoEntity();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());

        return entity;
    }

    @Override
    public PermissaoDTO toDto(PermissaoEntity entity) {
        if (entity == null) {
            return null;
        }

        PermissaoDTO dto = new PermissaoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());

        return dto;
    }
}

package model.mapper;

import model.entity.UsuarioEntity;
import model.dto.UsuarioDTO;

public class UsuarioMapper implements ModelMapper<UsuarioEntity, UsuarioDTO> {

    @Override
    public UsuarioEntity toEntity(UsuarioDTO dto) {
        if (dto == null) {
            return null;
        }

        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setAtivo(dto.getAtivo());

        return entity;
    }

    @Override
    public UsuarioDTO toDto(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }

        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setAtivo(entity.getAtivo());
        dto.setCriadoEm(entity.getCriadoEm());

        return dto;
    }
}

package model.mapper;

import model.entity.LogSistemaEntity;
import model.dto.LogSistemaDTO;

public class LogSistemaMapper implements ModelMapper<LogSistemaEntity, LogSistemaDTO> {

    @Override
    public LogSistemaEntity toEntity(LogSistemaDTO dto) {
        if (dto == null) {
            return null;
        }

        LogSistemaEntity entity = new LogSistemaEntity();
        entity.setId(dto.getId());
        entity.setAcao(dto.getAcao());
        entity.setDetalhes(dto.getDetalhes());

        return entity;
    }

    @Override
    public LogSistemaDTO toDto(LogSistemaEntity entity) {
        if (entity == null) {
            return null;
        }

        LogSistemaDTO dto = new LogSistemaDTO();
        dto.setId(entity.getId());
        dto.setAcao(entity.getAcao());
        dto.setData(entity.getData());
        dto.setDetalhes(entity.getDetalhes());

        if (entity.getUsuario() != null) {
            dto.setUsuarioId(entity.getUsuario().getId());
            dto.setUsuarioNome(entity.getUsuario().getNome());
        }

        return dto;
    }
}

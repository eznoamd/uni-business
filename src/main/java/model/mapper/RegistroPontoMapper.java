package model.mapper;

import model.entity.RegistroPontoEntity;
import model.dto.RegistroPontoDTO;

public class RegistroPontoMapper implements ModelMapper<RegistroPontoEntity, RegistroPontoDTO> {

    @Override
    public RegistroPontoEntity toEntity(RegistroPontoDTO dto) {
        if (dto == null) {
            return null;
        }

        RegistroPontoEntity entity = new RegistroPontoEntity();
        entity.setId(dto.getId());
        entity.setData(dto.getData());
        entity.setHoraEntrada(dto.getHoraEntrada());
        entity.setHoraSaida(dto.getHoraSaida());
        entity.setObservacao(dto.getObservacao());

        return entity;
    }

    @Override
    public RegistroPontoDTO toDto(RegistroPontoEntity entity) {
        if (entity == null) {
            return null;
        }

        RegistroPontoDTO dto = new RegistroPontoDTO();
        dto.setId(entity.getId());
        dto.setData(entity.getData());
        dto.setHoraEntrada(entity.getHoraEntrada());
        dto.setHoraSaida(entity.getHoraSaida());
        dto.setObservacao(entity.getObservacao());

        if (entity.getUsuario() != null) {
            dto.setUsuarioId(entity.getUsuario().getId());
            dto.setUsuarioNome(entity.getUsuario().getNome());
        }

        return dto;
    }
}

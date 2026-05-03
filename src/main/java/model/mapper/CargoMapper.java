package model.mapper;

import model.entity.CargoEntity;
import model.dto.CargoDTO;

public class CargoMapper implements ModelMapper<CargoEntity, CargoDTO> {

    @Override
    public CargoEntity toEntity(CargoDTO dto) {
        if (dto == null) {
            return null;
        }

        CargoEntity entity = new CargoEntity();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());

        return entity;
    }

    @Override
    public CargoDTO toDto(CargoEntity entity) {
        if (entity == null) {
            return null;
        }

        CargoDTO dto = new CargoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());

        return dto;
    }
}

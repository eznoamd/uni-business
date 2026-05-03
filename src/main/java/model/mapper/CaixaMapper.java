package model.mapper;

import model.entity.CaixaEntity;
import model.dto.CaixaDTO;

public class CaixaMapper implements ModelMapper<CaixaEntity, CaixaDTO> {

    @Override
    public CaixaEntity toEntity(CaixaDTO dto) {
        if (dto == null) {
            return null;
        }

        CaixaEntity entity = new CaixaEntity();
        entity.setId(dto.getId());
        entity.setSaldoInicial(dto.getSaldoInicial());
        entity.setDataFechamento(dto.getDataFechamento());
        entity.setSaldoFinal(dto.getSaldoFinal());

        return entity;
    }

    @Override
    public CaixaDTO toDto(CaixaEntity entity) {
        if (entity == null) {
            return null;
        }

        CaixaDTO dto = new CaixaDTO();
        dto.setId(entity.getId());
        dto.setDataAbertura(entity.getDataAbertura());
        dto.setDataFechamento(entity.getDataFechamento());
        dto.setSaldoInicial(entity.getSaldoInicial());
        dto.setSaldoFinal(entity.getSaldoFinal());

        return dto;
    }
}

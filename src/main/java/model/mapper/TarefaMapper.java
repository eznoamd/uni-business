package model.mapper;

import model.entity.TarefaEntity;
import model.dto.TarefaDTO;

public class TarefaMapper implements ModelMapper<TarefaEntity, TarefaDTO> {

    @Override
    public TarefaEntity toEntity(TarefaDTO dto) {
        if (dto == null) {
            return null;
        }

        TarefaEntity entity = new TarefaEntity();
        entity.setId(dto.getId());
        entity.setTitulo(dto.getTitulo());
        entity.setDescricao(dto.getDescricao());
        entity.setStatus(dto.getStatus());
        entity.setPrioridade(dto.getPrioridade());
        entity.setDataInicio(dto.getDataInicio());
        entity.setDataFim(dto.getDataFim());

        return entity;
    }

    @Override
    public TarefaDTO toDto(TarefaEntity entity) {
        if (entity == null) {
            return null;
        }

        TarefaDTO dto = new TarefaDTO();
        dto.setId(entity.getId());
        dto.setTitulo(entity.getTitulo());
        dto.setDescricao(entity.getDescricao());
        dto.setStatus(entity.getStatus());
        dto.setPrioridade(entity.getPrioridade());
        dto.setDataInicio(entity.getDataInicio());
        dto.setDataFim(entity.getDataFim());

        if (entity.getCriadoPor() != null) {
            dto.setCriadoPorId(entity.getCriadoPor().getId());
            dto.setCriadoPorNome(entity.getCriadoPor().getNome());
        }

        return dto;
    }
}

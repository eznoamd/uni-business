package model.mapper;

import model.entity.MovimentacaoCaixaEntity;
import model.dto.MovimentacaoCaixaDTO;

public class MovimentacaoCaixaMapper implements ModelMapper<MovimentacaoCaixaEntity, MovimentacaoCaixaDTO> {

    @Override
    public MovimentacaoCaixaEntity toEntity(MovimentacaoCaixaDTO dto) {
        if (dto == null) {
            return null;
        }

        MovimentacaoCaixaEntity entity = new MovimentacaoCaixaEntity();
        entity.setId(dto.getId());
        entity.setTipo(dto.getTipo());
        entity.setValor(dto.getValor());
        entity.setDescricao(dto.getDescricao());

        return entity;
    }

    @Override
    public MovimentacaoCaixaDTO toDto(MovimentacaoCaixaEntity entity) {
        if (entity == null) {
            return null;
        }

        MovimentacaoCaixaDTO dto = new MovimentacaoCaixaDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValor(entity.getValor());
        dto.setDescricao(entity.getDescricao());
        dto.setData(entity.getData());

        if (entity.getCaixa() != null) {
            dto.setCaixaId(entity.getCaixa().getId());
        }

        if (entity.getUsuario() != null) {
            dto.setUsuarioId(entity.getUsuario().getId());
            dto.setUsuarioNome(entity.getUsuario().getNome());
        }

        return dto;
    }
}

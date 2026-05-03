package model.mapper;

import model.entity.MovimentacaoEstoqueEntity;
import model.dto.MovimentacaoEstoqueDTO;

public class MovimentacaoEstoqueMapper implements ModelMapper<MovimentacaoEstoqueEntity, MovimentacaoEstoqueDTO> {

    @Override
    public MovimentacaoEstoqueEntity toEntity(MovimentacaoEstoqueDTO dto) {
        if (dto == null) {
            return null;
        }

        MovimentacaoEstoqueEntity entity = new MovimentacaoEstoqueEntity();
        entity.setId(dto.getId());
        entity.setTipo(dto.getTipo());
        entity.setQuantidade(dto.getQuantidade());

        return entity;
    }

    @Override
    public MovimentacaoEstoqueDTO toDto(MovimentacaoEstoqueEntity entity) {
        if (entity == null) {
            return null;
        }

        MovimentacaoEstoqueDTO dto = new MovimentacaoEstoqueDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setQuantidade(entity.getQuantidade());
        dto.setData(entity.getData());

        if (entity.getProduto() != null) {
            dto.setProdutoId(entity.getProduto().getId());
            dto.setProdutoNome(entity.getProduto().getNome());
        }

        if (entity.getUsuario() != null) {
            dto.setUsuarioId(entity.getUsuario().getId());
            dto.setUsuarioNome(entity.getUsuario().getNome());
        }

        return dto;
    }
}

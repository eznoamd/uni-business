package model.mapper;

import model.entity.ProdutoEntity;
import model.dto.ProdutoDTO;

public class ProdutoMapper implements ModelMapper<ProdutoEntity, ProdutoDTO> {

    @Override
    public ProdutoEntity toEntity(ProdutoDTO dto) {
        if (dto == null) {
            return null;
        }

        ProdutoEntity entity = new ProdutoEntity();
        entity.setId(dto.getId());
        entity.setNome(dto.getNome());
        entity.setDescricao(dto.getDescricao());
        entity.setQuantidade(dto.getQuantidade());
        entity.setPrecoUnitario(dto.getPrecoUnitario());

        return entity;
    }

    @Override
    public ProdutoDTO toDto(ProdutoEntity entity) {
        if (entity == null) {
            return null;
        }

        ProdutoDTO dto = new ProdutoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setQuantidade(entity.getQuantidade());
        dto.setPrecoUnitario(entity.getPrecoUnitario());

        return dto;
    }
}

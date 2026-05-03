package model.mapper;

public interface ModelMapper<E, D> {
    E toEntity(D dto);
    D toDto(E entity);
}
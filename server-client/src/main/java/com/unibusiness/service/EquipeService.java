package com.unibusiness.service;

import com.unibusiness.core.model.EquipeEntity;
import com.unibusiness.core.service.impl.EquipeServiceImpl;
import com.unibusiness.dto.Dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de equipes — antes via TCP (EQUIPE_*), agora via EquipeService (JPA) direto.
 */
public class EquipeService {

    private final com.unibusiness.core.service.EquipeService equipeService = new EquipeServiceImpl();

    public List<Dto.Equipe> listar() {
        return equipeService.listAll().stream().map(EquipeService::toDto).collect(Collectors.toList());
    }

    public Dto.Equipe criar(String nome) {
        try {
            return toDto(equipeService.create(new EquipeEntity(nome)));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean atualizar(int id, String nome) {
        return equipeService.findById(id).map(e -> {
            e.setNome(nome);
            equipeService.update(e);
            return true;
        }).orElse(false);
    }

    public boolean deletar(int id) {
        try {
            equipeService.delete(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Dto.Equipe toDto(EquipeEntity e) {
        Dto.Equipe dto = new Dto.Equipe();
        dto.id = e.getId();
        dto.nome = e.getNome();
        return dto;
    }
}

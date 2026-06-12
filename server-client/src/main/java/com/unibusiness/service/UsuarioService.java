package com.unibusiness.service;

import com.unibusiness.core.model.UsuarioEntity;
import com.unibusiness.core.service.impl.CargoServiceImpl;
import com.unibusiness.core.service.impl.UsuarioServiceImpl;
import com.unibusiness.core.util.PasswordUtil;
import com.unibusiness.dto.Dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de usuários — usado para popular a lista de participantes
 * na criação de conversas, exibição de funcionários e cadastro de novos
 * funcionários (aba "Funcionários", admin).
 *
 * Antes: enviava USUARIO_LIST para o servidor TCP e desserializava JSON.
 * Agora: consulta direto via UsuarioService/CargoService (JPA) e converte
 * para Dto.Usuario/Dto.Cargo.
 */
public class UsuarioService {

    private final com.unibusiness.core.service.UsuarioService usuarioService = new UsuarioServiceImpl();
    private final com.unibusiness.core.service.CargoService   cargoService   = new CargoServiceImpl();

    /**
     * Lista todos os usuários do sistema (ativos e inativos) — a tela de
     * Funcionários precisa dos dois para calcular "Total" vs "Ativos".
     */
    public List<Dto.Usuario> listarUsuarios() {
        return usuarioService.listAll().stream()
            .map(UsuarioService::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Lista todos os cargos cadastrados (ex: "Gerente", "Operador") — usado
     * para o ComboBox de cargo ao criar um novo funcionário.
     */
    public List<Dto.Cargo> listarCargos() {
        return cargoService.listAll().stream().map(c -> {
            Dto.Cargo dto = new Dto.Cargo();
            dto.id = c.getId();
            dto.nome = c.getNome();
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Cria um novo funcionário (usuário) com senha já hasheada e, se
     * informado, associado a um cargo.
     *
     * @return o usuário criado, ou {@code null} se o e-mail já existir ou
     *         ocorrer outro erro (ex: violação de unicidade no banco).
     */
    public Dto.Usuario criar(String nome, String email, String senha, Integer cargoId, boolean ativo) {
        if (usuarioService.findByEmail(email).isPresent()) {
            return null;
        }

        UsuarioEntity usuario = new UsuarioEntity(nome, email, PasswordUtil.hashPassword(senha));
        usuario.setAtivo(ativo);

        try {
            return toDto(usuarioService.criarComCargo(usuario, cargoId));
        } catch (Exception e) {
            return null;
        }
    }

    public static Dto.Usuario toDto(UsuarioEntity u) {
        Dto.Usuario dto = new Dto.Usuario();
        dto.id = u.getId();
        dto.nome = u.getNome();
        dto.email = u.getEmail();
        dto.ativo = Boolean.TRUE.equals(u.getAtivo());
        return dto;
    }
}

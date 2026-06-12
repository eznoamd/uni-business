package com.unibusiness.core.service;
import com.unibusiness.core.model.UsuarioEntity;
import java.util.List;
import java.util.Optional;
public interface UsuarioService {
    UsuarioEntity create(UsuarioEntity usuario);
    UsuarioEntity update(UsuarioEntity usuario);
    Optional<UsuarioEntity> findById(Integer id);
    Optional<UsuarioEntity> findByEmail(String email);
    List<UsuarioEntity> listAll();
    /** Marca o usuario como online/offline e atualiza ultimoAcessoEm. */
    void setOnline(Integer usuarioId, boolean online);
    /** true se algum dos cargos do usuario tiver a permissao ADMIN_TOTAL. */
    boolean isAdmin(Integer usuarioId);
    /**
     * Persiste um novo usuario e, se cargoId != null, associa o cargo
     * correspondente — tudo na mesma transacao (evita problemas de merge
     * de entidades "detached" em relacoes ManyToMany).
     */
    UsuarioEntity criarComCargo(UsuarioEntity usuario, Integer cargoId);
}
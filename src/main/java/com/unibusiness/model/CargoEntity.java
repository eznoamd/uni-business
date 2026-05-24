package com.unibusiness.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cargos")
public class CargoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nome;

    @ManyToMany(mappedBy = "cargos", fetch = FetchType.LAZY)
    private Set<UsuarioEntity> usuarios = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cargo_permissao",
            joinColumns = @JoinColumn(name = "cargo_id"),
            inverseJoinColumns = @JoinColumn(name = "permissao_id")
    )
    private Set<PermissaoEntity> permissoes = new HashSet<>();

    // Construtores
    public CargoEntity() {}

    public CargoEntity(String nome) {
        this.nome = nome;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Set<UsuarioEntity> getUsuarios() { return usuarios; }
    public void setUsuarios(Set<UsuarioEntity> usuarios) { this.usuarios = usuarios; }
    public Set<PermissaoEntity> getPermissoes() { return permissoes; }
    public void setPermissoes(Set<PermissaoEntity> permissoes) { this.permissoes = permissoes; }
}

package com.unibusiness.core.model;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "permissoes")
public class PermissaoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true) private String nome;
    @ManyToMany(mappedBy = "permissoes", fetch = FetchType.LAZY) private Set<CargoEntity> cargos = new HashSet<>();
    public PermissaoEntity() {}
    public PermissaoEntity(String nome) { this.nome = nome; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Set<CargoEntity> getCargos() { return cargos; }
    public void setCargos(Set<CargoEntity> cargos) { this.cargos = cargos; }
}